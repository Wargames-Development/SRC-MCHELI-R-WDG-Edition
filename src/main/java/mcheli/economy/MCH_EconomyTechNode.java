package mcheli.economy;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public final class MCH_EconomyTechNode {

    public enum NodeType {
        RP_UNLOCK,
        SL_PURCHASE,
        GE_EXCHANGE
    }

    public final String id;
    public final NodeType type;
    public final int costSL;
    public final int costGE;
    public final int costRP;
    public final int grantSL;
    public final int grantGE;
    public final int grantRP;
    public final Set<String> prerequisites;
    public final String npcItemName;

    private MCH_EconomyTechNode(
        String id,
        NodeType type,
        int costSL,
        int costGE,
        int costRP,
        int grantSL,
        int grantGE,
        int grantRP,
        Set<String> prerequisites,
        String npcItemName
    ) {
        this.id = id;
        this.type = type;
        this.costSL = Math.max(0, costSL);
        this.costGE = Math.max(0, costGE);
        this.costRP = Math.max(0, costRP);
        this.grantSL = Math.max(0, grantSL);
        this.grantGE = Math.max(0, grantGE);
        this.grantRP = Math.max(0, grantRP);
        this.prerequisites = prerequisites == null ? Collections.<String>emptySet() : Collections.unmodifiableSet(prerequisites);
        this.npcItemName = npcItemName == null ? "" : npcItemName;
    }

    public static MCH_EconomyTechNode rpUnlock(String id, int costRP, Set<String> prerequisites) {
        return new MCH_EconomyTechNode(id, NodeType.RP_UNLOCK, 0, 0, costRP, 0, 0, 0, copySet(prerequisites), "");
    }

    public static MCH_EconomyTechNode slPurchase(String id, int costSL, Set<String> prerequisites, String npcItemName) {
        return new MCH_EconomyTechNode(id, NodeType.SL_PURCHASE, costSL, 0, 0, 0, 0, 0, copySet(prerequisites), npcItemName);
    }

    public static MCH_EconomyTechNode geExchange(String id, int costGE, int grantSL, int grantRP, Set<String> prerequisites) {
        return new MCH_EconomyTechNode(id, NodeType.GE_EXCHANGE, 0, costGE, 0, grantSL, 0, grantRP, copySet(prerequisites), "");
    }

    private static Set<String> copySet(Set<String> src) {
        if (src == null || src.isEmpty()) {
            return new LinkedHashSet<String>();
        }
        return new LinkedHashSet<String>(src);
    }
}
