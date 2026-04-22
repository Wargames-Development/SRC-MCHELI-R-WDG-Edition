package mcheli;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public final class MCH_RWRThreatClientTracker {

    private static final ConcurrentHashMap<Integer, MCH_RWRThreatTable> TABLES = new ConcurrentHashMap<Integer, MCH_RWRThreatTable>();

    private MCH_RWRThreatClientTracker() {
    }

    public static void updateTable(MCH_RWRThreatTable incoming) {
        if (incoming == null || incoming.receiverEntityId <= 0) {
            return;
        }
        MCH_RWRThreatTable prev = TABLES.get(incoming.receiverEntityId);
        if (prev != null && incoming.snapshotSeq < prev.snapshotSeq) {
            return;
        }
        TABLES.put(incoming.receiverEntityId, incoming);
    }

    public static MCH_RWRThreatTable getTable(int receiverEntityId) {
        return TABLES.get(receiverEntityId);
    }

    public static List<MCH_RWRThreatEvent> getEvents(int receiverEntityId) {
        MCH_RWRThreatTable table = TABLES.get(receiverEntityId);
        if (table == null || table.events == null || table.events.isEmpty()) {
            return new ArrayList<MCH_RWRThreatEvent>();
        }
        return new ArrayList<MCH_RWRThreatEvent>(table.events);
    }

    public static void clear(int receiverEntityId) {
        TABLES.remove(receiverEntityId);
    }
}
