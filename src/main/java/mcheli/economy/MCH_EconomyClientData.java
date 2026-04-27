package mcheli.economy;

import java.util.LinkedHashSet;
import java.util.Set;

public final class MCH_EconomyClientData {

    private static volatile int sl;
    private static volatile int ge;
    private static volatile int rp;
    private static volatile String lastTechMessage = "";
    private static volatile boolean lastTechSuccess;
    private static volatile String lastTechNodeId = "";
    private static volatile byte lastTechAction;
    private static volatile String unlockedNodesRaw = "";
    private static volatile String activeTechTreeId = "";
    private static volatile String allowedTechTreeIdsRaw = "";
    private static volatile byte gainToastType;
    private static volatile int gainToastSL;
    private static volatile int gainToastGE;
    private static volatile int gainToastRP;
    private static volatile long gainToastExpireAtMs;

    private MCH_EconomyClientData() {
    }

    public static int getSL() {
        return sl;
    }

    public static int getGE() {
        return ge;
    }

    public static int getRP() {
        return rp;
    }

    public static void update(int nextSL, int nextGE, int nextRP) {
        sl = Math.max(0, nextSL);
        ge = Math.max(0, nextGE);
        rp = Math.max(0, nextRP);
    }

    public static void updateTechResult(boolean success, byte action, String nodeId, String message) {
        lastTechSuccess = success;
        lastTechAction = action;
        lastTechNodeId = nodeId == null ? "" : nodeId;
        lastTechMessage = message == null ? "" : message;
        if (success && action == MCH_PacketIndEconomyTechAction.ACTION_UNLOCK_RP && !lastTechNodeId.isEmpty()) {
            markUnlocked(lastTechNodeId);
        }
    }

    public static String getLastTechMessage() {
        return lastTechMessage;
    }

    public static boolean isLastTechSuccess() {
        return lastTechSuccess;
    }

    public static String getLastTechNodeId() {
        return lastTechNodeId;
    }

    public static byte getLastTechAction() {
        return lastTechAction;
    }

    public static void updateUnlockedNodes(String raw) {
        unlockedNodesRaw = raw == null ? "" : raw;
    }

    public static void updateActiveTechTreeId(String treeId) {
        activeTechTreeId = MCH_EconomyTechRegistry.normalizeId(treeId);
    }

    public static String getActiveTechTreeId() {
        return activeTechTreeId == null ? "" : activeTechTreeId;
    }

    public static void updateAllowedTechTreeIds(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            allowedTechTreeIdsRaw = "";
            return;
        }
        String[] arr = raw.split("[|,;]");
        LinkedHashSet<String> set = new LinkedHashSet<String>();
        for (String s : arr) {
            String id = MCH_EconomyTechRegistry.normalizeId(s);
            if (!id.isEmpty()) {
                set.add(id);
            }
        }
        StringBuilder sb = new StringBuilder();
        for (String id : set) {
            if (sb.length() > 0) {
                sb.append(';');
            }
            sb.append(id);
        }
        allowedTechTreeIdsRaw = sb.toString();
    }

    public static Set<String> getAllowedTechTreeIds() {
        LinkedHashSet<String> set = new LinkedHashSet<String>();
        String raw = allowedTechTreeIdsRaw;
        if (raw == null || raw.trim().isEmpty()) {
            return set;
        }
        String[] arr = raw.split(";");
        for (String s : arr) {
            String id = MCH_EconomyTechRegistry.normalizeId(s);
            if (!id.isEmpty()) {
                set.add(id);
            }
        }
        return set;
    }

    public static boolean isUnlocked(String nodeId) {
        if (nodeId == null || nodeId.trim().isEmpty()) {
            return false;
        }
        return getUnlockedNodes().contains(MCH_EconomyTechRegistry.normalizeId(nodeId));
    }

    public static Set<String> getUnlockedNodes() {
        LinkedHashSet<String> set = new LinkedHashSet<String>();
        String raw = unlockedNodesRaw;
        if (raw == null || raw.trim().isEmpty()) {
            return set;
        }
        String[] arr = raw.split(";");
        for (String s : arr) {
            String id = MCH_EconomyTechRegistry.normalizeId(s);
            if (!id.isEmpty()) {
                set.add(id);
            }
        }
        return set;
    }

    public static void markUnlocked(String nodeId) {
        String normalized = MCH_EconomyTechRegistry.normalizeId(nodeId);
        if (normalized.isEmpty()) {
            return;
        }
        Set<String> set = getUnlockedNodes();
        if (!set.add(normalized)) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (String id : set) {
            if (sb.length() > 0) {
                sb.append(';');
            }
            sb.append(id);
        }
        unlockedNodesRaw = sb.toString();
    }

    public static void showGainToast(byte type, int sl, int ge, int rp, long durationMs) {
        int addSL = Math.max(0, sl);
        int addGE = Math.max(0, ge);
        int addRP = Math.max(0, rp);
        if (addSL == 0 && addGE == 0 && addRP == 0) {
            return;
        }
        long now = System.currentTimeMillis();
        boolean canAccumulate = now < gainToastExpireAtMs;
        gainToastType = type;
        if (canAccumulate) {
            gainToastSL = safeAdd(gainToastSL, addSL);
            gainToastGE = safeAdd(gainToastGE, addGE);
            gainToastRP = safeAdd(gainToastRP, addRP);
        } else {
            gainToastSL = addSL;
            gainToastGE = addGE;
            gainToastRP = addRP;
        }
        long ms = Math.max(800L, durationMs);
        gainToastExpireAtMs = now + ms;
    }

    private static int safeAdd(int a, int b) {
        long r = (long) a + (long) b;
        if (r > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        if (r < 0L) {
            return 0;
        }
        return (int) r;
    }

    public static boolean hasGainToast() {
        return System.currentTimeMillis() < gainToastExpireAtMs;
    }

    public static byte getGainToastType() {
        return gainToastType;
    }

    public static int getGainToastSL() {
        return gainToastSL;
    }

    public static int getGainToastGE() {
        return gainToastGE;
    }

    public static int getGainToastRP() {
        return gainToastRP;
    }
}
