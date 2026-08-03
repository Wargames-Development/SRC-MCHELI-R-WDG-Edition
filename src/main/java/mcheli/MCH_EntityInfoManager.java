package mcheli;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.ReflectionHelper;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.flare.MCH_EntityChaff;
import mcheli.network.packets.PacketEntityInfoSync;
import mcheli.weapon.MCH_IEntityLockChecker;
import mcheli.weapon.MCH_IMissile;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityTracker;
import net.minecraft.entity.EntityTrackerEntry;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.IntHashMap;
import net.minecraft.world.WorldServer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class MCH_EntityInfoManager {

    private static final long TRACKER_RESYNC_REQUEST_INTERVAL_MS = 2_000L;
    private static final int MAX_TRACKER_RESYNCS_PER_TICK = 16;
    public static final double ENTITY_INFO_SYNC_RANGE = 4096.0D;
    private static final double ENTITY_INFO_SYNC_RANGE_SQ = ENTITY_INFO_SYNC_RANGE * ENTITY_INFO_SYNC_RANGE;

    // 服务器侧仅用于收集/去重，不再依赖“删除发包”
    public static final Map<Integer, MCH_EntityInfo> serverEntities = new ConcurrentHashMap<>();

    private int tickCounter;
    private long snapshotSeq = 0L; // 递增的全局快照序号
    private final Queue<TrackerResyncRequest> trackerResyncRequests = new ConcurrentLinkedQueue<>();
    private final Map<EntityPlayerMP, Long> lastTrackerResyncRequest = new WeakHashMap<>();

    public MCH_EntityInfoManager() {
        FMLCommonHandler.instance().bus().register(this);
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            tickCounter++;
            snapshotSeq++; // 每个服务端 Tick 递增一次
            processTrackerResyncRequests();
            serverTick();
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
        for (int i = 0; i < MAX_TRACKER_RESYNCS_PER_TICK; ++i) {
            TrackerResyncRequest request = this.trackerResyncRequests.poll();
            if (request == null) {
                return;
            }
            refreshTrackerEntry(request.player, request.entityId);
        }
    }

    private void refreshTrackerEntry(EntityPlayerMP player, int entityId) {
        if (player == null || player.isDead || !(player.worldObj instanceof WorldServer)) {
            return;
        }

        Entity entity = player.worldObj.getEntityByID(entityId);
        if (!(entity instanceof MCH_EntityAircraft) || entity.isDead) {
            return;
        }

        WorldServer world = (WorldServer) player.worldObj;
        EntityTracker tracker = world.getEntityTracker();
        try {
            IntHashMap entries = ReflectionHelper.getPrivateValue(EntityTracker.class, tracker,
                new String[]{"trackedEntityIDs", "field_72794_c"});
            EntityTrackerEntry entry = entries != null ? (EntityTrackerEntry) entries.lookup(entityId) : null;
            if (entry == null) {
                return;
            }

            double dx = player.posX - entity.posX;
            double dz = player.posZ - entity.posZ;
            if (Math.abs(dx) > entry.blocksDistanceThreshold || Math.abs(dz) > entry.blocksDistanceThreshold) {
                return;
            }

            // This method also checks that the player watches the entity's chunk,
            // so the recovery does not force chunk loads or bypass normal limits.
            entry.removePlayerFromTracker(player);
            entry.tryStartWachingThis(player);
            if (entry.trackingPlayers.contains(player)) {
                MCH_Lib.Log(entity, "[EntitySync] Refreshed tracker entry for player=%s, id=%d, type=%s",
                    player.getCommandSenderName(), Integer.valueOf(entityId), ((MCH_EntityAircraft) entity).getTypeName());
            }
        } catch (RuntimeException ex) {
            MCH_Lib.Log(entity, "[EntitySync] Failed to refresh tracker entry: player=%s, id=%d, error=%s",
                player.getCommandSenderName(), Integer.valueOf(entityId), ex.getMessage());
        }
    }

    private static final class TrackerResyncRequest {
        private final EntityPlayerMP player;
        private final int entityId;

        private TrackerResyncRequest(EntityPlayerMP player, int entityId) {
            this.player = player;
            this.entityId = entityId;
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
                List<MCH_EntityInfo> visibleEntities = new ArrayList<MCH_EntityInfo>();
                for (MCH_EntityInfo info : worldEntities) {
                    if (info.getDistanceSqToEntity(player) <= ENTITY_INFO_SYNC_RANGE_SQ) {
                        visibleEntities.add(info);
                    }
                }
                MCH_MOD.getPacketHandler().sendTo(new PacketEntityInfoSync(visibleEntities, snapshotSeq), player);
            }
        }
    }

    private boolean shouldTrack(WorldServer w, Entity entity) {
        if (entity.isDead) {
            return false;
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
}
