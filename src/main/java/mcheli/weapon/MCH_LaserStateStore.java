package mcheli.weapon;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Phase-A infrastructure only.
 * Stores per-owner laser designator state by source channel without changing existing gameplay logic.
 */
public final class MCH_LaserStateStore {

    public static final int SOURCE_HANDHELD = 1;
    public static final int SOURCE_AIRCRAFT = 2;

    public static final int DEFAULT_TTL_TICKS = 40;
    public static final long NO_NETWORK_UPDATE = -1L;

    public static class LaserState {
        public int ownerId;
        public int sourceType;
        public double x;
        public double y;
        public double z;
        public boolean active;
        public long sequence;
        public long lastUpdateTick;
        private boolean published;
        private boolean lastPublishedActive;
        private long lastPublishedTick;
    }

    private static final Map<Long, LaserState> SERVER_STATES = new HashMap<Long, LaserState>();
    private static final Map<Long, LaserState> CLIENT_STATES = new HashMap<Long, LaserState>();

    private MCH_LaserStateStore() {
    }

    public static boolean isValidSourceType(int sourceType) {
        return sourceType == SOURCE_HANDHELD || sourceType == SOURCE_AIRCRAFT;
    }

    public static synchronized void upsertServerState(int ownerId, int sourceType, double x, double y, double z,
                                                      boolean active, long sequence, long worldTick) {
        if (!isValidSourceType(sourceType)) {
            return;
        }
        long key = buildKey(ownerId, sourceType);
        LaserState old = SERVER_STATES.get(key);
        if (old != null && sequence < old.sequence) {
            // Drop out-of-order update.
            return;
        }
        LaserState st = old != null ? old : new LaserState();
        st.ownerId = ownerId;
        st.sourceType = sourceType;
        st.x = x;
        st.y = y;
        st.z = z;
        st.active = active;
        st.sequence = sequence;
        st.lastUpdateTick = worldTick;
        SERVER_STATES.put(key, st);
    }

    public static synchronized LaserState getServerState(int ownerId, int sourceType) {
        return SERVER_STATES.get(buildKey(ownerId, sourceType));
    }

    public static synchronized LaserState getServerState(int ownerId, int sourceType, long worldTick) {
        long key = buildKey(ownerId, sourceType);
        LaserState state = SERVER_STATES.get(key);
        if (state != null && (worldTick < state.lastUpdateTick
            || worldTick - state.lastUpdateTick > DEFAULT_TTL_TICKS)) {
            SERVER_STATES.remove(key);
            return null;
        }
        return state;
    }

    public static synchronized void upsertClientState(int ownerId, int sourceType, double x, double y, double z,
                                                      boolean active, long sequence, long worldTick) {
        if (!isValidSourceType(sourceType)) {
            return;
        }
        long key = buildKey(ownerId, sourceType);
        LaserState old = CLIENT_STATES.get(key);
        if (old != null && sequence < old.sequence) {
            return;
        }
        LaserState st = old != null ? old : new LaserState();
        st.ownerId = ownerId;
        st.sourceType = sourceType;
        st.x = x;
        st.y = y;
        st.z = z;
        st.active = active;
        st.sequence = sequence;
        st.lastUpdateTick = worldTick;
        CLIENT_STATES.put(key, st);
    }

    /**
     * Updates the local presentation state and returns a sequence only when a packet is needed.
     * State transitions are immediate; active coordinates are capped to the requested interval.
     */
    public static synchronized long updateClientStateForNetwork(int ownerId, int sourceType,
                                                                 double x, double y, double z, boolean active,
                                                                 long worldTick, int activeIntervalTicks) {
        if (!isValidSourceType(sourceType)) {
            return NO_NETWORK_UPDATE;
        }
        long key = buildKey(ownerId, sourceType);
        LaserState state = CLIENT_STATES.get(key);
        if (state == null) {
            state = new LaserState();
            state.ownerId = ownerId;
            state.sourceType = sourceType;
            CLIENT_STATES.put(key, state);
        }

        int interval = Math.max(1, activeIntervalTicks);
        boolean transition = !state.published || state.lastPublishedActive != active;
        boolean activeUpdateDue = active && (worldTick < state.lastPublishedTick
            || worldTick - state.lastPublishedTick >= interval);

        state.x = x;
        state.y = y;
        state.z = z;
        state.active = active;
        state.lastUpdateTick = worldTick;

        if (!transition && !activeUpdateDue) {
            return NO_NETWORK_UPDATE;
        }

        state.sequence++;
        state.published = true;
        state.lastPublishedActive = active;
        state.lastPublishedTick = worldTick;
        return state.sequence;
    }

    public static synchronized LaserState getClientState(int ownerId, int sourceType) {
        return CLIENT_STATES.get(buildKey(ownerId, sourceType));
    }

    public static synchronized long nextClientSequence(int ownerId, int sourceType) {
        if (!isValidSourceType(sourceType)) {
            return 0L;
        }
        // Sequence ownership follows the shared owner/source channel, not an individual weapon hardpoint.
        LaserState old = CLIENT_STATES.get(buildKey(ownerId, sourceType));
        return old != null ? old.sequence + 1L : 1L;
    }

    public static synchronized void expireServerStates(long worldTick, int ttlTicks) {
        int ttl = Math.max(1, ttlTicks);
        Iterator<Map.Entry<Long, LaserState>> it = SERVER_STATES.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Long, LaserState> e = it.next();
            LaserState st = e.getValue();
            if (st == null || worldTick - st.lastUpdateTick > ttl) {
                it.remove();
            }
        }
    }

    public static synchronized void expireClientStates(long worldTick, int ttlTicks) {
        int ttl = Math.max(1, ttlTicks);
        Iterator<Map.Entry<Long, LaserState>> it = CLIENT_STATES.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Long, LaserState> e = it.next();
            LaserState st = e.getValue();
            if (st == null || worldTick - st.lastUpdateTick > ttl) {
                it.remove();
            }
        }
    }

    public static synchronized void clearOwner(int ownerId) {
        clearOwnerFromMap(SERVER_STATES, ownerId);
        clearOwnerFromMap(CLIENT_STATES, ownerId);
    }

    private static void clearOwnerFromMap(Map<Long, LaserState> map, int ownerId) {
        Iterator<Map.Entry<Long, LaserState>> it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Long, LaserState> e = it.next();
            LaserState st = e.getValue();
            if (st != null && st.ownerId == ownerId) {
                it.remove();
            }
        }
    }

    private static long buildKey(int ownerId, int sourceType) {
        return ((long) ownerId << 32) | (sourceType & 0xFFFFFFFFL);
    }
}
