package mcheli;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.aircraft.MCH_PacketStatusRequest;
import mcheli.helicopter.MCH_EntityHeli;
import mcheli.plane.MCP_EntityPlane;
import mcheli.tank.MCH_EntityTank;
import mcheli.vehicle.MCH_EntityVehicle;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 客户端仅依赖“全量快照心跳”进行增量/覆盖；删除完全靠本地过期清理。
 * - 乱序保护：仅接受 snapshotSeq >= lastAppliedSeq 的数据。
 * - 过期策略：同时依据毫秒超时与缺席序号阈值做双重判定。
 * - 定时器：使用 ClientTick（避免 Timer 的跨线程问题）。
 */
public class MCH_EntityInfoClientTracker {

    private static final long RESYNC_ENTITY_COOLDOWN_MS = 2_000L;
    private static final long RESYNC_GLOBAL_COOLDOWN_MS = 2_000L;
    private static final double RESYNC_MAX_HORIZONTAL_DISTANCE_SQ = 256.0D * 256.0D;
    private static final long AIRCRAFT_RENDER_MISSING_GRACE_MS = 3_000L;

    // Accessed only by client render calls. Weak keys release state with the local entity.
    private static final Map<UUID, AircraftRenderState> aircraftRenderStates = new WeakHashMap<>();

    private static final Map<Integer, Tracked> tracked = new ConcurrentHashMap<>();
    private static volatile Collection<MCH_EntityInfo> renderSnapshot = Collections.emptyList();
    private static volatile Set<Integer> latestSnapshotEntityIds = Collections.emptySet();
    private static volatile Map<Integer, Set<Integer>> radarTerrain = Collections.emptyMap();
    /**
     * 可调：心跳缺席的毫秒阈值（例如 5s）
     */
    public static long EXPIRATION_MS = 3_000L;
    /**
     * 可调：心跳缺席的序号阈值（以服务器 tick 计数，20TPS 下 100≈5s）
     */
    public static long MISSING_SEQ_THRESHOLD = 60L;
    /**
     * 可调：清理扫描的 Tick 周期（例如每 10 个客户端 Tick 扫描一次）
     */
    public static int CLEANUP_TICK_INTERVAL = 20;
    private static volatile long lastAppliedSeq = -1L;    // 已应用的最新快照序号
    private static volatile long latestSeqObserved = -1L; // 最近接收到的最大序号（用于缺席判断）
    private static int clientTickCounter = 0;
    private static long lastTrackerResyncRequestMillis;

    static {
        // 注册客户端 Tick 监听（类被首次引用时完成注册）
        FMLCommonHandler.instance().bus().register(new ClientTicker());
    }

    /**
     * 由网络包回调调用：应用一批实体并记录快照序号
     */
    public static void updateEntities(List<MCH_EntityInfo> infos, long snapshotSeq) {
        updateEntities(infos, snapshotSeq, 0, Collections.<Integer>emptyList());
    }

    public static void updateEntities(List<MCH_EntityInfo> infos, long snapshotSeq, int emitterId, List<Integer> blockedIds) {
        // 乱序/迟到包保护
        if (snapshotSeq < lastAppliedSeq) {
            return;
        }

        long now = System.currentTimeMillis();
        latestSeqObserved = Math.max(latestSeqObserved, snapshotSeq);
        Set<Integer> snapshotEntityIds = new HashSet<Integer>();

        for (MCH_EntityInfo info : infos) {
            snapshotEntityIds.add(Integer.valueOf(info.entityId));
            Tracked t = tracked.get(info.entityId);
            if (t == null) {
                tracked.put(info.entityId, new Tracked(info, now, snapshotSeq));
            } else {
                t.info = info;
                t.lastSeenMillis = now;
                t.lastSeenSeq = snapshotSeq;
            }
        }

        latestSnapshotEntityIds = Collections.unmodifiableSet(snapshotEntityIds);
        radarTerrain = emitterId > 0
            ? Collections.singletonMap(emitterId, Collections.unmodifiableSet(new HashSet<Integer>(blockedIds)))
            : Collections.<Integer, Set<Integer>>emptyMap();
        lastAppliedSeq = snapshotSeq;
        publishRenderSnapshot();
    }

    /**
     * 兼容旧接口：不再使用服务端 REMOVE 包，这里保留以防外部调用
     */
    @Deprecated
    public static void removeEntities(List<MCH_EntityInfo> infos) {
        for (MCH_EntityInfo info : infos) {
            tracked.remove(info.entityId);
        }
        publishRenderSnapshot();
    }

    public static MCH_EntityInfo getEntityInfo(int entityId) {
        Tracked t = tracked.get(entityId);
        return t == null ? null : t.info;
    }

    public static boolean isEntityInLatestSnapshot(int entityId) {
        return latestSnapshotEntityIds.contains(Integer.valueOf(entityId));
    }

    public static boolean isRadarTerrainBlocked(int emitterId, int targetId) {
        Set<Integer> blocked = radarTerrain.get(emitterId);
        return blocked != null && blocked.contains(targetId);
    }

    public static Collection<MCH_EntityInfo> getAllTrackedEntities() {
        return renderSnapshot;
    }

    /** A loaded client copy is not proof that the aircraft still exists on the server. */
    public static boolean shouldSuppressAircraftRender(MCH_EntityAircraft aircraft) {
        if (aircraft.isDead) return true;
        UUID localUuid = aircraft.getUniqueID();
        AircraftRenderState state = aircraftRenderStates.get(localUuid);
        if (state == null) {
            state = new AircraftRenderState();
            aircraftRenderStates.put(localUuid, state);
        }
        int entityId = aircraft.getEntityId();
        return state.shouldSuppress(getEntityInfo(entityId), isEntityInLatestSnapshot(entityId),
            aircraft.isDestroyed(), lastAppliedSeq, System.currentTimeMillis());
    }

    static final class AircraftRenderState {
        private UUID serverUuid;
        private long missingSinceMillis = -1L;
        private long missingSinceSeq;
        private boolean serverDestroyed;

        boolean shouldSuppress(MCH_EntityInfo info, boolean inSnapshot, boolean localDestroyed,
                long snapshotSeq, long now) {
            if (snapshotSeq < 0L) return false;
            if (info != null && inSnapshot) {
                // A reused entity ID must not validate the previous vehicle's client copy.
                // Forge 1.7.10 does not send the UUID in this aircraft's spawn data;
                // bind the server identity from snapshots, never from the local UUID.
                if (info.aircraftUuid != null) {
                    if (this.serverUuid != null && !info.aircraftUuid.equals(this.serverUuid)) return true;
                    this.serverUuid = info.aircraftUuid;
                }
                this.missingSinceMillis = -1L;
                this.serverDestroyed = info.destroyed;
                // Preserve the normal dark wreck while its server entity still exists.
                return this.serverDestroyed && !localDestroyed;
            }
            if (this.missingSinceMillis < 0L) {
                this.missingSinceMillis = now;
                this.missingSinceSeq = snapshotSeq;
            }
            // Retain destruction after the short-lived tombstone expires. Absence alone
            // needs a newer full snapshot and a grace window for spawn/chunk handoffs.
            return this.serverDestroyed && !localDestroyed
                || snapshotSeq > this.missingSinceSeq
                    && now - this.missingSinceMillis >= AIRCRAFT_RENDER_MISSING_GRACE_MS;
        }
    }

    /** Packet/tick publication owns the copy; render readers reuse it without allocating. */
    private static void publishRenderSnapshot() {
        List<MCH_EntityInfo> out = new ArrayList<MCH_EntityInfo>(tracked.size());
        for (Tracked entry : tracked.values()) out.add(entry.info);
        renderSnapshot = Collections.unmodifiableList(out);
    }

    /**
     * 定期扫描：缺席过久（时间或序号）则删除
     */
    private static void cleanupExpired() {
        if (tracked.isEmpty()) return;

        long now = System.currentTimeMillis();
        long seqNow = latestSeqObserved;

        Iterator<Map.Entry<Integer, Tracked>> it = tracked.entrySet().iterator();
        boolean changed = false;
        while (it.hasNext()) {
            Map.Entry<Integer, Tracked> e = it.next();
            Tracked t = e.getValue();

            boolean timeExpired = (now - t.lastSeenMillis) > EXPIRATION_MS;
            boolean seqExpired = (seqNow - t.lastSeenSeq) > MISSING_SEQ_THRESHOLD;

            if (timeExpired || seqExpired) {
                it.remove();
                changed = true;
            }
        }
        if (changed) publishRenderSnapshot();
    }

    private static void requestMissingAircraftResync() {
        Entity clientPlayer = MCH_MOD.proxy.getClientPlayer();
        if (!(clientPlayer instanceof EntityPlayer) || clientPlayer.worldObj == null) {
            return;
        }
        EntityPlayer player = (EntityPlayer) clientPlayer;

        long now = System.currentTimeMillis();
        for (Tracked trackedEntity : tracked.values()) {
            MCH_EntityInfo info = trackedEntity.info;
            if (!isAircraftInfo(info)) {
                continue;
            }

            if (info.getHorizonalDistanceSqToEntity(player) > RESYNC_MAX_HORIZONTAL_DISTANCE_SQ) {
                continue;
            }

            Entity localEntity = player.worldObj.getEntityByID(info.entityId);
            if (localEntity instanceof MCH_EntityAircraft && !localEntity.isDead
                && player.worldObj.loadedEntityList.contains(localEntity)) {
                continue;
            }

            if (now - trackedEntity.lastResyncRequestMillis < RESYNC_ENTITY_COOLDOWN_MS
                || now - lastTrackerResyncRequestMillis < RESYNC_GLOBAL_COOLDOWN_MS) {
                continue;
            }

            trackedEntity.lastResyncRequestMillis = now;
            lastTrackerResyncRequestMillis = now;
            MCH_Lib.Log(player, "[EntitySync] Requesting missing aircraft tracker resend: id=%d, type=%s, distance=%.1f",
                Integer.valueOf(info.entityId), info.entityName, Double.valueOf(info.getDistanceToEntity(player)));
            MCH_PacketStatusRequest.requestTrackerResync(info.entityId);
            return;
        }
    }

    private static boolean isAircraftInfo(MCH_EntityInfo info) {
        if (info == null || info.destroyed || info.entityClassName == null) {
            return false;
        }
        String className = info.entityClassName;
        return className.equals(MCP_EntityPlane.class.getName())
            || className.equals(MCH_EntityHeli.class.getName())
            || className.equals(MCH_EntityTank.class.getName())
            || className.equals(MCH_EntityVehicle.class.getName());
    }

    public static void resetTracker() {
        tracked.clear();
        aircraftRenderStates.clear();
        renderSnapshot = Collections.emptyList();
        latestSnapshotEntityIds = Collections.emptySet();
        radarTerrain = Collections.emptyMap();
        lastAppliedSeq = -1L;
        latestSeqObserved = -1L;
        clientTickCounter = 0;
        lastTrackerResyncRequestMillis = 0L;
    }

    private static final class Tracked {
        volatile MCH_EntityInfo info;
        long lastSeenMillis;
        long lastSeenSeq;
        long lastResyncRequestMillis;

        Tracked(MCH_EntityInfo info, long now, long seq) {
            this.info = info;
            this.lastSeenMillis = now;
            this.lastSeenSeq = seq;
            this.lastResyncRequestMillis = 0L;
        }
    }

    /**
     * ※ 注意：必须是 public 才能被 ASMEventHandler 访问
     */
    public static class ClientTicker {
        @SubscribeEvent
        public void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            clientTickCounter++;
            if (clientTickCounter % CLEANUP_TICK_INTERVAL == 0) {
                cleanupExpired();
                requestMissingAircraftResync();
            }
        }
    }
}
