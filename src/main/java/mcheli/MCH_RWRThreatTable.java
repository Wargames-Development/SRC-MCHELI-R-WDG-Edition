package mcheli;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MCH_RWRThreatTable {

    public int receiverEntityId;
    public long snapshotSeq;
    public List<MCH_RWRThreatEvent> events;

    public MCH_RWRThreatTable() {
        this(0, 0L, new ArrayList<MCH_RWRThreatEvent>());
    }

    public MCH_RWRThreatTable(int receiverEntityId, long snapshotSeq, List<MCH_RWRThreatEvent> events) {
        this.receiverEntityId = receiverEntityId;
        this.snapshotSeq = snapshotSeq;
        this.events = events != null ? events : new ArrayList<MCH_RWRThreatEvent>();
    }

    public List<MCH_RWRThreatEvent> immutableEvents() {
        return Collections.unmodifiableList(this.events);
    }
}
