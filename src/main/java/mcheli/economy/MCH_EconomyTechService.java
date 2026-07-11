package mcheli.economy;

import mcheli.MCH_Config;
import mcheli.MCH_MOD;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.item.ItemStack;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

public final class MCH_EconomyTechService {

    private static final String ROOT_TAG = "MCH_Economy";
    private static final String TECH_TAG = "Tech";
    private static final String KEY_UNLOCKED = "UnlockedNodes";
    private static final String KEY_ACTIVE_TREE = "ActiveTreeId";
    private static final String KEY_ALLOWED_TREES = "AllowedTreeIds";

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
        if (MCH_MOD.config == null || !MCH_Config.EnableTechTreeGameplay.prmBool) {
            return Result.fail(action, nodeId, "科技树玩法未启用");
        }
        if (player == null) {
            return Result.fail(action, nodeId, "玩家无效");
        }
        String normalized = MCH_EconomyTechRegistry.normalizeId(nodeId);
        MCH_EconomyTechNode node = MCH_EconomyTechRegistry.getNode(normalized);
        if (node == null) {
            return Result.fail(action, normalized, "节点不存在");
        }
        Set<String> allowedTreeIds = getAllowedTechTreeIds(player);
        if (!allowedTreeIds.isEmpty() && !allowedTreeIds.contains(MCH_EconomyTechRegistry.normalizeId(node.treeId))) {
            return Result.fail(action, normalized, "节点不属于当前科技树");
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
        if (action == MCH_PacketIndEconomyTechAction.ACTION_PURCHASE_SL
            && (node.type == MCH_EconomyTechNode.NodeType.RP_UNLOCK || node.type == MCH_EconomyTechNode.NodeType.GE_UNLOCK)
            && !unlocked.contains(normalized)) {
            return Result.fail(action, normalized, "请先研发该节点");
        }
        int spendSL = 0;
        int spendGE = 0;
        int spendRP = 0;
        if (action == MCH_PacketIndEconomyTechAction.ACTION_UNLOCK_RP) {
            if (node.type == MCH_EconomyTechNode.NodeType.GE_UNLOCK) {
                spendGE = node.costGE;
            } else {
                spendRP = node.costRP;
            }
        } else if (action == MCH_PacketIndEconomyTechAction.ACTION_PURCHASE_SL) {
            spendSL = node.type == MCH_EconomyTechNode.NodeType.RP_UNLOCK ? node.purchaseCostSL : node.costSL;
            if (node.type == MCH_EconomyTechNode.NodeType.GE_UNLOCK) {
                spendSL = node.purchaseCostSL;
            }
        } else if (action == MCH_PacketIndEconomyTechAction.ACTION_EXCHANGE_GE) {
            spendGE = node.costGE;
        }

        if (!MCH_EconomyService.trySpend(player, spendSL, spendGE, spendRP, "Tech:" + normalized)) {
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
            grantPurchasedItem(player, node);
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

    public static String getActiveTechTreeId(EntityPlayerMP player) {
        if (player == null) {
            return "";
        }
        return MCH_EconomyTechRegistry.normalizeId(getOrCreateTechTag(player).getString(KEY_ACTIVE_TREE));
    }

    public static void setActiveTechTreeId(EntityPlayerMP player, String treeId) {
        if (player == null) {
            return;
        }
        String normalized = MCH_EconomyTechRegistry.normalizeId(treeId);
        getOrCreateTechTag(player).setString(KEY_ACTIVE_TREE, normalized);
    }

    public static String getAllowedTechTreeIdsRaw(EntityPlayerMP player) {
        if (player == null) {
            return "";
        }
        return getOrCreateTechTag(player).getString(KEY_ALLOWED_TREES);
    }

    public static Set<String> getAllowedTechTreeIds(EntityPlayerMP player) {
        LinkedHashSet<String> set = new LinkedHashSet<String>();
        String raw = getAllowedTechTreeIdsRaw(player);
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

    public static void setAllowedTechTreeIdsRaw(EntityPlayerMP player, String rawTreeIds) {
        if (player == null) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        if (rawTreeIds != null && !rawTreeIds.trim().isEmpty()) {
            String[] arr = rawTreeIds.split("[|,;]");
            LinkedHashSet<String> set = new LinkedHashSet<String>();
            for (String s : arr) {
                String id = MCH_EconomyTechRegistry.normalizeId(s);
                if (!id.isEmpty()) {
                    set.add(id);
                }
            }
            for (String id : set) {
                if (sb.length() > 0) {
                    sb.append(';');
                }
                sb.append(id);
            }
        }
        getOrCreateTechTag(player).setString(KEY_ALLOWED_TREES, sb.toString());
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
            if (action == MCH_PacketIndEconomyTechAction.ACTION_UNLOCK_RP) {
                return true;
            }
            return action == MCH_PacketIndEconomyTechAction.ACTION_PURCHASE_SL && node.purchaseCostSL > 0;
        }
        if (node.type == MCH_EconomyTechNode.NodeType.GE_UNLOCK) {
            if (action == MCH_PacketIndEconomyTechAction.ACTION_UNLOCK_RP) {
                return true;
            }
            return action == MCH_PacketIndEconomyTechAction.ACTION_PURCHASE_SL && node.purchaseCostSL > 0;
        }
        if (node.type == MCH_EconomyTechNode.NodeType.SL_PURCHASE) {
            return action == MCH_PacketIndEconomyTechAction.ACTION_PURCHASE_SL;
        }
        return action == MCH_PacketIndEconomyTechAction.ACTION_EXCHANGE_GE;
    }

    private static void grantPurchasedItem(EntityPlayerMP player, MCH_EconomyTechNode node) {
        if (player == null || node == null || node.npcItemName == null || node.npcItemName.isEmpty()) {
            return;
        }
        ItemStack stack = MCH_EconomyVehicleResolver.createVehicleStack(node.npcItemName);
        if (stack == null) {
            Object obj = Item.itemRegistry.getObject(node.npcItemName);
            if (!(obj instanceof Item)) {
                obj = Item.itemRegistry.getObject("mcheli:" + node.npcItemName);
            }
            if (obj instanceof Item) {
                stack = new ItemStack((Item) obj);
            }
        }
        if (stack == null) {
            return;
        }
        ItemStack remain = stack.copy();
        remain = insertIntoPlayerInventory(player, remain);
        if (remain != null && remain.stackSize > 0) {
            player.entityDropItem(remain, 0.0F);
        }
    }

    private static ItemStack insertIntoPlayerInventory(EntityPlayerMP player, ItemStack stack) {
        if (player == null || player.inventory == null || stack == null || stack.stackSize <= 0) {
            return stack;
        }

        ItemStack remain = stack.copy();
        ItemStack[] inv = player.inventory.mainInventory;
        if (inv == null || inv.length <= 0) {
            return remain;
        }

        // Pass 1: merge into existing stacks.
        for (int i = 0; i < inv.length && remain.stackSize > 0; i++) {
            ItemStack slot = inv[i];
            if (slot == null) {
                continue;
            }
            if (!slot.isStackable()) {
                continue;
            }
            if (!slot.isItemEqual(remain) || !ItemStack.areItemStackTagsEqual(slot, remain)) {
                continue;
            }
            int max = Math.min(slot.getMaxStackSize(), player.inventory.getInventoryStackLimit());
            int canMove = max - slot.stackSize;
            if (canMove <= 0) {
                continue;
            }
            int moved = Math.min(canMove, remain.stackSize);
            slot.stackSize += moved;
            remain.stackSize -= moved;
        }

        // Pass 2: place into first empty slots.
        for (int i = 0; i < inv.length && remain.stackSize > 0; i++) {
            if (inv[i] != null) {
                continue;
            }
            int max = Math.min(remain.getMaxStackSize(), player.inventory.getInventoryStackLimit());
            int moved = Math.min(max, remain.stackSize);
            ItemStack placed = remain.copy();
            placed.stackSize = moved;
            inv[i] = placed;
            remain.stackSize -= moved;
        }

        player.inventory.markDirty();
        player.inventoryContainer.detectAndSendChanges();
        return remain.stackSize > 0 ? remain : null;
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
