package mcheli;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.internal.FMLNetworkHandler;
import cpw.mods.fml.relauncher.ReflectionHelper;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.aircraft.MCH_EntitySeat;
import mcheli.flare.MCH_EntityChaff;
import mcheli.network.packets.PacketEntityInfoSync;
import mcheli.uav.MCH_EntityUavStation;
import mcheli.weapon.MCH_IEntityLockChecker;
import mcheli.weapon.MCH_IMissile;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityTracker;
import net.minecraft.entity.EntityTrackerEntry;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.Packet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.IntHashMap;
import net.minecraft.world.WorldServer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class MCH_EntityInfoManager {

    private static final long TRACKER_RESYNC_REQUEST_INTERVAL_MS = 2_000L;
    private static final int MAX_TRACKER_RESYNCS_PER_TICK = 16;
    private static final int ACTIVE_SYNC_INTERVAL_TICKS = 2;
    private static final int EMPTY_HEARTBEAT_INTERVAL_TICKS = 20;
    public static final double ENTITY_INFO_SYNC_RANGE = 4096.0D;
    private static final double ENTITY_INFO_SYNC_RANGE_SQ = ENTITY_INFO_SYNC_RANGE * ENTITY_INFO_SYNC_RANGE;

    // 服务器侧仅用于收集/去重，不再依赖“删除发包”
    public static final Map<Integer, MCH_EntityInfo> serverEntities = new ConcurrentHashMap<>();

    private long tickCounter;
    private long snapshotSeq = 0L; // 递增的全局快照序号
    private final Queue<TrackerResyncRequest> trackerResyncRequests = new ConcurrentLinkedQueue<>();
    private final List<TrackerResyncRequest> pendingTrackerRestarts = new ArrayList<>();
    private final Map<EntityPlayerMP, Long> lastTrackerResyncRequest = new WeakHashMap<>();
    private final Map<EntityPlayerMP, PlayerSyncState> playerSyncStates = new WeakHashMap<>();

    public MCH_EntityInfoManager() {
        FMLCommonHandler.instance().bus().register(this);
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            tickCounter++;
            snapshotSeq++; // 每个服务端 Tick 递增一次
            processTrackerResyncRequests();
            if (tickCounter % ACTIVE_SYNC_INTERVAL_TICKS == 0L) {
                serverTick();
            }
        }
    }

    /**
     * Called from the network handler. The tracker mutation is deferred to the
     * server tick rather than modifying EntityTracker collections on a Netty thread.
     */
    public synchronized void queueTrackerResync(EntityPlayerMP player, int entityId) {
        if (player == null || entityId <= 0 || this.trackerResyncRequests.size() >= 256) {
            return;
        }

        long now = System.currentTimeMillis();
        Long lastRequest = this.lastTrackerResyncRequest.get(player);
        if (lastRequest != null && now - lastRequest.longValue() < TRACKER_RESYNC_REQUEST_INTERVAL_MS) {
            return;
        }

        this.lastTrackerResyncRequest.put(player, Long.valueOf(now));
        this.trackerResyncRequests.offer(new TrackerResyncRequest(player, entityId));
    }

    private void processTrackerResyncRequests() {
        for (int i = this.pendingTrackerRestarts.size() - 1; i >= 0; --i) {
            TrackerResyncRequest request = this.pendingTrackerRestarts.get(i);
            if (request.restartAfterTick <= this.tickCounter) {
                restartTrackerEntry(request);
                this.pendingTrackerRestarts.remove(i);
            }
        }

        for (int i = 0; i < MAX_TRACKER_RESYNCS_PER_TICK; ++i) {
            TrackerResyncRequest request = this.trackerResyncRequests.poll();
            if (request == null) {
                return;
            }
            resendTrackerEntry(request);
        }
    }

    private void resendTrackerEntry(TrackerResyncRequest request) {
        EntityPlayerMP player = request.player;
        if (player == null || player.isDead || player.dimension != request.dimension
            || !(player.worldObj instanceof WorldServer)) {
            return;
        }

        Entity entity = player.worldObj.getEntityByID(request.entityId);
        if (!(entity instanceof MCH_EntityAircraft) || entity.isDead) {
            return;
        }

        MCH_EntityAircraft aircraft = (MCH_EntityAircraft)entity;
        if (isRequesterCurrentAircraft(player, aircraft)) {
            MCH_Lib.DbgTrace(player.worldObj,
                "event=tracker_resync_rejected reason=current_aircraft player=%s playerId=%d aircraftId=%d aircraftObj=%d type=%s mount=%s",
                player.getCommandSenderName(), Integer.valueOf(player.getEntityId()), Integer.valueOf(aircraft.getEntityId()),
                Integer.valueOf(System.identityHashCode(aircraft)), aircraft.getTypeName(), describeEntity(player.ridingEntity));
            return;
        }

        EntityTracker tracker = ((WorldServer)player.worldObj).getEntityTracker();
        try {
            IntHashMap entries = ReflectionHelper.getPrivateValue(EntityTracker.class, tracker,
                new String[]{"trackedEntityIDs", "field_72794_c"});
            EntityTrackerEntry entry = entries != null ? (EntityTrackerEntry)entries.lookup(request.entityId) : null;
            if (entry == null) {
                return;
            }

            if (!entry.trackingPlayers.contains(player)) {
                entry.tryStartWachingThis(player);
                MCH_Lib.DbgTrace(player.worldObj,
                    "event=tracker_resync_transition action=start_watching player=%s aircraftId=%d aircraftObj=%d type=%s watching=%s",
                    player.getCommandSenderName(), Integer.valueOf(aircraft.getEntityId()),
                    Integer.valueOf(System.identityHashCode(aircraft)), aircraft.getTypeName(),
                    Boolean.valueOf(entry.trackingPlayers.contains(player)));
                return;
            }

            double dx = player.posX - entity.posX;
            double dz = player.posZ - entity.posZ;
            if (Math.abs(dx) > entry.blocksDistanceThreshold || Math.abs(dz) > entry.blocksDistanceThreshold) {
                MCH_Lib.DbgTrace(player.worldObj,
                    "event=tracker_resync_rejected reason=out_of_range player=%s aircraftId=%d aircraftObj=%d type=%s dx=%.2f dz=%.2f threshold=%d",
                    player.getCommandSenderName(), Integer.valueOf(aircraft.getEntityId()),
                    Integer.valueOf(System.identityHashCode(aircraft)), aircraft.getTypeName(),
                    Double.valueOf(dx), Double.valueOf(dz), Integer.valueOf(entry.blocksDistanceThreshold));
                return;
            }

            Packet spawnPacket = FMLNetworkHandler.getEntitySpawningPacket(entity);
            if (spawnPacket != null) {
                player.playerNetServerHandler.sendPacket(spawnPacket);
                MCH_Lib.DbgTrace(player.worldObj,
                    "event=tracker_resync_transition action=targeted_spawn player=%s aircraftId=%d aircraftObj=%d type=%s packet=%s",
                    player.getCommandSenderName(), Integer.valueOf(aircraft.getEntityId()),
                    Integer.valueOf(System.identityHashCode(aircraft)), aircraft.getTypeName(),
                    spawnPacket.getClass().getSimpleName());
                return;
            }

            // Preserve the delayed remove/restart recovery for entities which do
            // not expose an FML spawn packet. Recheck before mutating the tracker.
            if (isRequesterCurrentAircraft(player, aircraft)) {
                MCH_Lib.DbgTrace(player.worldObj,
                    "event=tracker_resync_rejected reason=current_aircraft_before_fallback player=%s aircraftId=%d aircraftObj=%d type=%s",
                    player.getCommandSenderName(), Integer.valueOf(aircraft.getEntityId()),
                    Integer.valueOf(System.identityHashCode(aircraft)), aircraft.getTypeName());
                return;
            }
            if (removeTrackerEntry(player, request.entityId)) {
                request.restartAfterTick = this.tickCounter + 1L;
                this.pendingTrackerRestarts.add(request);
                MCH_Lib.DbgTrace(player.worldObj,
                    "event=tracker_resync_transition action=delayed_restart_queued player=%s aircraftId=%d aircraftObj=%d type=%s restartTick=%d",
                    player.getCommandSenderName(), Integer.valueOf(aircraft.getEntityId()),
                    Integer.valueOf(System.identityHashCode(aircraft)), aircraft.getTypeName(),
                    Long.valueOf(request.restartAfterTick));
            }
        } catch (RuntimeException ex) {
            MCH_Lib.Log(entity, "[EntitySync] Failed tracker resend: player=%s, id=%d, error=%s",
                player.getCommandSenderName(), Integer.valueOf(request.entityId), ex.getMessage());
        }
    }

    private boolean isRequesterCurrentAircraft(EntityPlayerMP player, MCH_EntityAircraft aircraft) {
        MCH_EntityAircraft controlled = MCH_EntityAircraft.getAircraft_RiddenOrControl(player);
        return controlled == aircraft
            || controlled != null && controlled.getEntityId() == aircraft.getEntityId()
            || aircraft.isMountedEntity(player);
    }

    private static String describeEntity(Entity entity) {
        return entity == null ? "null" : entity.getClass().getSimpleName() + "#" + entity.getEntityId()
            + "@" + System.identityHashCode(entity);
    }

    private boolean removeTrackerEntry(EntityPlayerMP player, int entityId) {
        if (player == null || player.isDead || !(player.worldObj instanceof WorldServer)) {
            return false;
        }

        Entity entity = player.worldObj.getEntityByID(entityId);
        if (!(entity instanceof MCH_EntityAircraft) || entity.isDead) {
            return false;
        }

        MCH_EntityAircraft aircraft = (MCH_EntityAircraft)entity;
        if (isRequesterCurrentAircraft(player, aircraft)) {
            MCH_Lib.DbgTrace(player.worldObj,
                "event=tracker_resync_rejected reason=current_aircraft_in_remove player=%s aircraftId=%d aircraftObj=%d type=%s",
                player.getCommandSenderName(), Integer.valueOf(aircraft.getEntityId()),
                Integer.valueOf(System.identityHashCode(aircraft)), aircraft.getTypeName());
            return false;
        }

        WorldServer world = (WorldServer) player.worldObj;
        EntityTracker tracker = world.getEntityTracker();
        try {
            IntHashMap entries = ReflectionHelper.getPrivateValue(EntityTracker.class, tracker,
                new String[]{"trackedEntityIDs", "field_72794_c"});
            EntityTrackerEntry entry = entries != null ? (EntityTrackerEntry) entries.lookup(entityId) : null;
            if (entry == null) {
                return false;
            }

            double dx = player.posX - entity.posX;
            double dz = player.posZ - entity.posZ;
            if (Math.abs(dx) > entry.blocksDistanceThreshold || Math.abs(dz) > entry.blocksDistanceThreshold) {
                return false;
            }

            // EntityPlayerMP queues destroy IDs until its next update. Restarting
            // here would send the new spawn before that queued destroy is flushed.
            entry.removePlayerFromTracker(player);
            return true;
        } catch (RuntimeException ex) {
            MCH_Lib.Log(entity, "[EntitySync] Failed to remove tracker entry: player=%s, id=%d, error=%s",
                player.getCommandSenderName(), Integer.valueOf(entityId), ex.getMessage());
            return false;
        }
    }

    private void restartTrackerEntry(TrackerResyncRequest request) {
        EntityPlayerMP player = request.player;
        int entityId = request.entityId;
        if (player == null || player.isDead || player.dimension != request.dimension
            || !(player.worldObj instanceof WorldServer)) {
            return;
        }

        Entity entity = player.worldObj.getEntityByID(entityId);
        if (!(entity instanceof MCH_EntityAircraft) || entity.isDead) {
            return;
        }

        EntityTracker tracker = ((WorldServer) player.worldObj).getEntityTracker();
        try {
            IntHashMap entries = ReflectionHelper.getPrivateValue(EntityTracker.class, tracker,
                new String[]{"trackedEntityIDs", "field_72794_c"});
            EntityTrackerEntry entry = entries != null ? (EntityTrackerEntry) entries.lookup(entityId) : null;
            if (entry == null) {
                return;
            }

            // This also checks chunk visibility and normal tracking distance.
            entry.tryStartWachingThis(player);
            if (entry.trackingPlayers.contains(player)) {
                MCH_Lib.Log(entity, "[EntitySync] Restarted tracker entry for player=%s, id=%d, type=%s",
                    player.getCommandSenderName(), Integer.valueOf(entityId), ((MCH_EntityAircraft) entity).getTypeName());
            }
        } catch (RuntimeException ex) {
            MCH_Lib.Log(entity, "[EntitySync] Failed to restart tracker entry: player=%s, id=%d, error=%s",
                player.getCommandSenderName(), Integer.valueOf(entityId), ex.getMessage());
        }
    }

    private static final class TrackerResyncRequest {
        private final EntityPlayerMP player;
        private final int entityId;
        private final int dimension;
        private long restartAfterTick;

        private TrackerResyncRequest(EntityPlayerMP player, int entityId) {
            this.player = player;
            this.entityId = entityId;
            this.dimension = player.dimension;
        }
    }

    public void serverTick() {
        serverEntities.clear();
        for (WorldServer world : MinecraftServer.getServer().worldServers) {
            List<MCH_EntityInfo> worldEntities = new ArrayList<MCH_EntityInfo>();
            @SuppressWarnings("unchecked")
            List<Entity> loaded = world.loadedEntityList;
            for (Entity entity : loaded) {
                if (shouldTrack(world, entity)) {
                    MCH_EntityInfo info = MCH_EntityInfo.createInfo(entity, world.getTotalWorldTime());
                    serverEntities.put(entity.getEntityId(), info);
                    worldEntities.add(info);
                }
            }

            @SuppressWarnings("unchecked")
            List<EntityPlayerMP> players = world.playerEntities;
            for (EntityPlayerMP player : players) {
                List<MCH_EntityInfo> visibleEntities = null;
                for (MCH_EntityInfo info : worldEntities) {
                    if (info.getDistanceSqToEntity(player) <= ENTITY_INFO_SYNC_RANGE_SQ
                        && canSyncToPlayer(info, player)) {
                        if (visibleEntities == null) {
                            visibleEntities = new ArrayList<MCH_EntityInfo>();
                        }
                        visibleEntities.add(info);
                    }
                }
                if (visibleEntities == null) {
                    visibleEntities = Collections.emptyList();
                }
                PlayerSyncState state = this.playerSyncStates.get(player);
                if (state == null || state.dimension != player.dimension) {
                    state = new PlayerSyncState(player.dimension);
                    this.playerSyncStates.put(player, state);
                }
                boolean membershipChanged = state.hasMembershipChanged(visibleEntities);
                int interval = visibleEntities.isEmpty() ? EMPTY_HEARTBEAT_INTERVAL_TICKS : ACTIVE_SYNC_INTERVAL_TICKS;
                if (!state.hasSent || membershipChanged || tickCounter - state.lastSendTick >= interval) {
                    MCH_MOD.getPacketHandler().sendTo(new PacketEntityInfoSync(visibleEntities, snapshotSeq), player);
                    if (!state.hasSent || membershipChanged) {
                        state.rememberMembership(visibleEntities);
                    }
                    state.lastSendTick = tickCounter;
                    state.hasSent = true;
                }
            }
        }
    }

    private static final class PlayerSyncState {
        final int dimension;
        Set<Integer> visibleEntityIds = Collections.emptySet();
        long lastSendTick;
        boolean hasSent;

        PlayerSyncState(int dimension) {
            this.dimension = dimension;
        }

        boolean hasMembershipChanged(List<MCH_EntityInfo> entities) {
            if (this.visibleEntityIds.size() != entities.size()) {
                return true;
            }
            for (MCH_EntityInfo info : entities) {
                if (!this.visibleEntityIds.contains(Integer.valueOf(info.entityId))) {
                    return true;
                }
            }
            return false;
        }

        void rememberMembership(List<MCH_EntityInfo> entities) {
            if (entities.isEmpty()) {
                this.visibleEntityIds = Collections.emptySet();
                return;
            }
            Set<Integer> ids = new HashSet<Integer>();
            for (MCH_EntityInfo info : entities) {
                ids.add(Integer.valueOf(info.entityId));
            }
            this.visibleEntityIds = ids;
        }
    }

    private boolean shouldTrack(WorldServer w, Entity entity) {
        if (entity.isDead) {
            return false;
        }
        // Same-team players use this stream for HUD markers beyond vanilla tracking range.
        // Per-recipient filtering prevents disclosure of untracked enemy player positions.
        if (entity instanceof EntityPlayerMP) {
            return !isPlayerMountedInAircraft(w, entity);
        }
        // Visual contacts are sensor-independent: every live aircraft must remain continuous.
        if (entity instanceof MCH_EntityAircraft) {
            return true;
        }
        // Track missiles explicitly to keep radar contacts synced even if lock-checker paths change.
        if (MCH_FMURUtil.isSoldier(entity) || entity instanceof MCH_IEntityLockChecker || entity instanceof MCH_IMissile) {
            if (entity instanceof MCH_EntityChaff
                && entity.posY - w.getHeightValue((int)entity.posX, (int)entity.posZ) < 0) {
                return false;
            }
            return true;
        }
        return false;
    }

    private boolean isPlayerMountedInAircraft(WorldServer world, Entity player) {
        if (player.ridingEntity instanceof MCH_EntityAircraft
            || player.ridingEntity instanceof MCH_EntitySeat
            || player.ridingEntity instanceof MCH_EntityUavStation) {
            return true;
        }
        for (Object obj : world.loadedEntityList) {
            if (obj instanceof MCH_EntityAircraft && ((MCH_EntityAircraft)obj).isMountedEntity(player)) {
                return true;
            }
        }
        return false;
    }

    private boolean canSyncToPlayer(MCH_EntityInfo info, EntityPlayerMP player) {
        if (!EntityPlayerMP.class.getName().equals(info.entityClassName)) {
            return true;
        }
        return player.getTeam() != null && info.teamName != null
            && player.getTeam().getRegisteredName().equals(info.teamName);
    }
}
