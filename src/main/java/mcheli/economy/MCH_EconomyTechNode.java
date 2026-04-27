package mcheli.economy;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public final class MCH_EconomyTechNode {

    public enum NodeType {
        RP_UNLOCK,
        GE_UNLOCK,
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
    public final int purchaseCostSL;
    public final String treeId;

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
        String npcItemName,
        int purchaseCostSL,
        String treeId
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
        this.purchaseCostSL = Math.max(0, purchaseCostSL);
        this.treeId = MCH_EconomyTechRegistry.normalizeId(treeId);
    }

    public static MCH_EconomyTechNode rpUnlock(String id, int costRP, Set<String> prerequisites) {
        return rpUnlock(id, costRP, prerequisites, "");
    }

    public static MCH_EconomyTechNode rpUnlock(String id, int costRP, Set<String> prerequisites, String treeId) {
        return rpUnlock(id, costRP, prerequisites, "", 0, treeId);
    }

    public static MCH_EconomyTechNode rpUnlock(String id, int costRP, Set<String> prerequisites, String npcItemName, int purchaseCostSL, String treeId) {
        return new MCH_EconomyTechNode(id, NodeType.RP_UNLOCK, 0, 0, costRP, 0, 0, 0, copySet(prerequisites), npcItemName, purchaseCostSL, treeId);
    }

    public static MCH_EconomyTechNode geUnlock(String id, int costGE, Set<String> prerequisites, String npcItemName, int purchaseCostSL, String treeId) {
        return new MCH_EconomyTechNode(id, NodeType.GE_UNLOCK, 0, costGE, 0, 0, 0, 0, copySet(prerequisites), npcItemName, purchaseCostSL, treeId);
    }

    public static MCH_EconomyTechNode slPurchase(String id, int costSL, Set<String> prerequisites, String npcItemName) {
        return slPurchase(id, costSL, prerequisites, npcItemName, "");
    }

    public static MCH_EconomyTechNode slPurchase(String id, int costSL, Set<String> prerequisites, String npcItemName, String treeId) {
        return new MCH_EconomyTechNode(id, NodeType.SL_PURCHASE, costSL, 0, 0, 0, 0, 0, copySet(prerequisites), npcItemName, 0, treeId);
    }

    public static MCH_EconomyTechNode geExchange(String id, int costGE, int grantSL, int grantRP, Set<String> prerequisites) {
        return geExchange(id, costGE, grantSL, grantRP, prerequisites, "");
    }

    public static MCH_EconomyTechNode geExchange(String id, int costGE, int grantSL, int grantRP, Set<String> prerequisites, String treeId) {
        return new MCH_EconomyTechNode(id, NodeType.GE_EXCHANGE, 0, costGE, 0, grantSL, 0, grantRP, copySet(prerequisites), "", 0, treeId);
    }

    private static Set<String> copySet(Set<String> src) {
        if (src == null || src.isEmpty()) {
            return new LinkedHashSet<String>();
        }
        return new LinkedHashSet<String>(src);
    }
}
