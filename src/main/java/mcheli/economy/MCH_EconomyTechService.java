package mcheli.economy;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

public final class MCH_EconomyTechService {

    private static final String ROOT_TAG = "MCH_Economy";
    private static final String TECH_TAG = "Tech";
    private static final String KEY_UNLOCKED = "UnlockedNodes";

    private MCH_EconomyTechService() {
    }

    public static Result unlockByRP(EntityPlayerMP player, String nodeId) {
        return execute(player, nodeId, MCH_PacketIndEconomyTechAction.ACTION_UNLOCK_RP);
    }

    public static Result purchaseBySL(EntityPlayerMP player, String nodeId) {
        return execute(player, nodeId, MCH_PacketIndEconomyTechAction.ACTION_PURCHASE_SL);
    }

    public static Result exchangeByGE(EntityPlayerMP player, String nodeId) {
        return execute(player, nodeId, MCH_PacketIndEconomyTechAction.ACTION_EXCHANGE_GE);
    }

    public static Result execute(EntityPlayerMP player, String nodeId, byte action) {
        if (player == null) {
            return Result.fail(action, nodeId, "玩家无效");
        }
        String normalized = MCH_EconomyTechRegistry.normalizeId(nodeId);
        MCH_EconomyTechNode node = MCH_EconomyTechRegistry.getNode(normalized);
        if (node == null) {
            return Result.fail(action, normalized, "节点不存在");
        }

        if (!matchAction(node, action)) {
            return Result.fail(action, normalized, "操作类型不匹配");
        }

        Set<String> unlocked = getUnlockedNodes(player);
        for (String pre : node.prerequisites) {
            if (!unlocked.contains(MCH_EconomyTechRegistry.normalizeId(pre))) {
                return Result.fail(action, normalized, "缺少前置: " + pre);
            }
        }

        if (action == MCH_PacketIndEconomyTechAction.ACTION_UNLOCK_RP && unlocked.contains(normalized)) {
            return Result.fail(action, normalized, "节点已解锁");
        }

        if (!MCH_EconomyService.trySpend(player, node.costSL, node.costGE, node.costRP, "Tech:" + normalized)) {
            return Result.fail(action, normalized, "余额不足");
        }

        if (action == MCH_PacketIndEconomyTechAction.ACTION_UNLOCK_RP) {
            unlocked.add(normalized);
            saveUnlockedNodes(player, unlocked);
            MCH_EconomyService.syncToClient(player);
            return Result.success(action, normalized, "解锁成功");
        }

        if (action == MCH_PacketIndEconomyTechAction.ACTION_PURCHASE_SL) {
            addPurchaseCount(player, normalized, 1);
            MCH_EconomyService.syncToClient(player);
            return Result.success(action, normalized, "购买成功");
        }

        if (action == MCH_PacketIndEconomyTechAction.ACTION_EXCHANGE_GE) {
            if (node.grantSL > 0 || node.grantGE > 0 || node.grantRP > 0) {
                MCH_EconomyService.grant(player, node.grantSL, node.grantGE, node.grantRP, "TechExchange:" + normalized);
            } else {
                MCH_EconomyService.syncToClient(player);
            }
            return Result.success(action, normalized, "兑换成功");
        }

        return Result.fail(action, normalized, "未知操作");
    }

    public static boolean isUnlocked(EntityPlayerMP player, String nodeId) {
        return getUnlockedNodes(player).contains(MCH_EconomyTechRegistry.normalizeId(nodeId));
    }

    public static String getUnlockedNodesRaw(EntityPlayerMP player) {
        Set<String> unlocked = getUnlockedNodes(player);
        StringBuilder sb = new StringBuilder();
        for (String s : unlocked) {
            if (s == null || s.isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(';');
            }
            sb.append(MCH_EconomyTechRegistry.normalizeId(s));
        }
        return sb.toString();
    }

    private static boolean matchAction(MCH_EconomyTechNode node, byte action) {
        if (node.type == MCH_EconomyTechNode.NodeType.RP_UNLOCK) {
            return action == MCH_PacketIndEconomyTechAction.ACTION_UNLOCK_RP;
        }
        if (node.type == MCH_EconomyTechNode.NodeType.SL_PURCHASE) {
            return action == MCH_PacketIndEconomyTechAction.ACTION_PURCHASE_SL;
        }
        return action == MCH_PacketIndEconomyTechAction.ACTION_EXCHANGE_GE;
    }

    private static NBTTagCompound getOrCreateTechTag(EntityPlayerMP player) {
        NBTTagCompound root = player.getEntityData();
        if (!root.hasKey(ROOT_TAG)) {
            root.setTag(ROOT_TAG, new NBTTagCompound());
        }
        NBTTagCompound eco = root.getCompoundTag(ROOT_TAG);
        if (!eco.hasKey(TECH_TAG)) {
            eco.setTag(TECH_TAG, new NBTTagCompound());
        }
        return eco.getCompoundTag(TECH_TAG);
    }

    private static Set<String> getUnlockedNodes(EntityPlayerMP player) {
        Set<String> set = new LinkedHashSet<String>();
        String raw = getOrCreateTechTag(player).getString(KEY_UNLOCKED);
        if (raw == null || raw.trim().isEmpty()) {
            return set;
        }
        String[] arr = raw.split(";");
        for (String s : arr) {
            String v = MCH_EconomyTechRegistry.normalizeId(s);
            if (!v.isEmpty()) {
                set.add(v);
            }
        }
        return set;
    }

    private static void saveUnlockedNodes(EntityPlayerMP player, Set<String> unlocked) {
        StringBuilder sb = new StringBuilder();
        for (String s : unlocked) {
            if (s == null || s.isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(';');
            }
            sb.append(MCH_EconomyTechRegistry.normalizeId(s));
        }
        getOrCreateTechTag(player).setString(KEY_UNLOCKED, sb.toString());
    }

    private static void addPurchaseCount(EntityPlayerMP player, String nodeId, int delta) {
        if (delta <= 0) {
            return;
        }
        NBTTagCompound tag = getOrCreateTechTag(player);
        String key = "Buy_" + sanitizeKey(nodeId);
        int current = Math.max(0, tag.getInteger(key));
        tag.setInteger(key, current + delta);
    }

    private static String sanitizeKey(String value) {
        String v = value == null ? "" : value.toLowerCase(Locale.ROOT);
        StringBuilder sb = new StringBuilder(v.length());
        for (int i = 0; i < v.length(); i++) {
            char c = v.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) {
                sb.append(c);
            } else {
                sb.append('_');
            }
        }
        return sb.toString();
    }

    public static final class Result {
        public final boolean success;
        public final byte action;
        public final String nodeId;
        public final String message;

        private Result(boolean success, byte action, String nodeId, String message) {
            this.success = success;
            this.action = action;
            this.nodeId = nodeId == null ? "" : nodeId;
            this.message = message == null ? "" : message;
        }

        public static Result success(byte action, String nodeId, String message) {
            return new Result(true, action, nodeId, message);
        }

        public static Result fail(byte action, String nodeId, String message) {
            return new Result(false, action, nodeId, message);
        }
    }
}
