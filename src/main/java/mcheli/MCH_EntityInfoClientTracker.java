package mcheli;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side entity snapshot tracker.
 *
 * The client only relies on periodic “full snapshot heartbeats” for incremental or full updates;
 * deletions are handled entirely via local expiration cleanup.
 *
 * Behavior and safeguards:
 * - **Out-of-order protection:** Only accepts snapshot packets where `snapshotSeq >= lastAppliedSeq`.
 * - **Expiration policy:** Uses a dual condition — entities expire when both the time threshold
 *   and the missing-sequence threshold are exceeded.
 * - **Timer mechanism:** Uses the client tick loop instead of a separate timer to avoid
 *   cross-thread issues in Minecraft’s single-threaded environment.
 */
public class MCH_EntityInfoClientTracker {

    /** Configurable: maximum heartbeat absence in milliseconds before expiration (e.g., 1000ms = 1s). */
    public static long EXPIRATION_MS = 1_000L;

    /** Configurable: maximum missing snapshot sequence count before expiration (e.g., 20 ticks ≈ 1s at 20TPS). */
    public static long MISSING_SEQ_THRESHOLD = 20L;

    /** Configurable: how often cleanup runs, measured in client ticks (e.g., every 10 ticks). */
    public static int CLEANUP_TICK_INTERVAL = 10;

    /** Internal structure to track entity info and last-seen timestamps. */
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

    private static final Map<Integer, Tracked> tracked = new ConcurrentHashMap<>();
    private static volatile long lastAppliedSeq = -1L;    // Latest applied snapshot sequence number
    private static volatile long latestSeqObserved = -1L; // Highest sequence number received (for gap detection)
    private static int clientTickCounter = 0;

    static {
        // Register client tick listener — runs once when the class is first loaded
        FMLCommonHandler.instance().bus().register(new ClientTicker());
    }

    /**
     * Called by network packet handler:
     * Applies a batch of entity snapshots and records the associated sequence number.
     */
    public static void updateEntities(List<MCH_EntityInfo> infos, long snapshotSeq) {
        // Out-of-order or late packet protection — ignore older snapshots
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
     * Legacy compatibility: the old server-side REMOVE packet is no longer used.
     * This method is kept only to maintain backward compatibility for any external calls.
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
     * Periodic cleanup: removes entities that have been absent for too long,
     * based on either elapsed time or missing sequence count.
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

    /**
     * Note: Must be declared public so that ASMEventHandler can access it.
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
