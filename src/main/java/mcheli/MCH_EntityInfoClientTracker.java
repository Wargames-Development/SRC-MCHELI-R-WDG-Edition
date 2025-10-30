package mcheli;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 客户端仅依赖“全量快照心跳”进行增量/覆盖；删除完全靠本地过期清理。
 * - 乱序保护：仅接受 snapshotSeq >= lastAppliedSeq 的数据。
 * - 过期策略：同时依据毫秒超时与缺席序号阈值做双重判定。
 * - 定时器：使用 ClientTick（避免 Timer 的跨线程问题）。
 */
public class MCH_EntityInfoClientTracker {

    private static final Map<Integer, Tracked> tracked = new ConcurrentHashMap<>();
    /**
     * 可调：心跳缺席的毫秒阈值（例如 5s）
     */
    public static long EXPIRATION_MS = 1_000L;
    /**
     * 可调：心跳缺席的序号阈值（以服务器 tick 计数，20TPS 下 100≈5s）
     */
    public static long MISSING_SEQ_THRESHOLD = 20L;
    /**
     * 可调：清理扫描的 Tick 周期（例如每 10 个客户端 Tick 扫描一次）
     */
    public static int CLEANUP_TICK_INTERVAL = 10;
    private static volatile long lastAppliedSeq = -1L;    // 已应用的最新快照序号
    private static volatile long latestSeqObserved = -1L; // 最近接收到的最大序号（用于缺席判断）
    private static int clientTickCounter = 0;

    static {
        // 注册客户端 Tick 监听（类被首次引用时完成注册）
        FMLCommonHandler.instance().bus().register(new ClientTicker());
    }

    /**
     * 由网络包回调调用：应用一批实体并记录快照序号
     */
    public static void updateEntities(List<MCH_EntityInfo> infos, long snapshotSeq) {
        // 乱序/迟到包保护
        if (snapshotSeq < lastAppliedSeq) {
            return;
        }

        long now = System.currentTimeMillis();
        latestSeqObserved = Math.max(latestSeqObserved, snapshotSeq);

        for (MCH_EntityInfo info : infos) {
            Tracked t = tracked.get(info.entityId);
            if (t == null) {
                tracked.put(info.entityId, new Tracked(info, now, snapshotSeq));
            } else {
                t.info = info;
                t.lastSeenMillis = now;
                t.lastSeenSeq = snapshotSeq;
            }
        }

        lastAppliedSeq = snapshotSeq;
    }

    /**
     * 兼容旧接口：不再使用服务端 REMOVE 包，这里保留以防外部调用
     */
    @Deprecated
    public static void removeEntities(List<MCH_EntityInfo> infos) {
        for (MCH_EntityInfo info : infos) {
            tracked.remove(info.entityId);
        }
    }

    public static MCH_EntityInfo getEntityInfo(int entityId) {
        Tracked t = tracked.get(entityId);
        return t == null ? null : t.info;
    }

    public static Collection<MCH_EntityInfo> getAllTrackedEntities() {
        List<MCH_EntityInfo> out = new ArrayList<>(tracked.size());
        for (Tracked t : tracked.values()) {
            out.add(t.info);
        }
        return Collections.unmodifiableCollection(out);
    }

    /**
     * 定期扫描：缺席过久（时间或序号）则删除
     */
    private static void cleanupExpired() {
        if (tracked.isEmpty()) return;

        long now = System.currentTimeMillis();
        long seqNow = latestSeqObserved;

        Iterator<Map.Entry<Integer, Tracked>> it = tracked.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, Tracked> e = it.next();
            Tracked t = e.getValue();

            boolean timeExpired = (now - t.lastSeenMillis) > EXPIRATION_MS;
            boolean seqExpired = (seqNow - t.lastSeenSeq) > MISSING_SEQ_THRESHOLD;

            if (timeExpired || seqExpired) {
                it.remove();
            }
        }
    }

    private static final class Tracked {
        MCH_EntityInfo info;
        long lastSeenMillis;
        long lastSeenSeq;

        Tracked(MCH_EntityInfo info, long now, long seq) {
            this.info = info;
            this.lastSeenMillis = now;
            this.lastSeenSeq = seq;
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
            }
        }
    }
}
