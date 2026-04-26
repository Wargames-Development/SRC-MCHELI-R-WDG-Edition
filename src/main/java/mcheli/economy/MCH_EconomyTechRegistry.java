package mcheli.economy;

import mcheli.mob.MCH_GunnerInfo;
import mcheli.mob.MCH_GunnerInfoManager;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class MCH_EconomyTechRegistry {

    public static final String NODE_GUNNER_LICENSE = "core.gunner_license";
    public static final String NODE_EXCHANGE_GE_TO_RP = "exchange.ge_to_rp_small";
    public static final String NODE_EXCHANGE_GE_TO_SL = "exchange.ge_to_sl_small";

    private static final Map<String, MCH_EconomyTechNode> NODE_MAP = new LinkedHashMap<String, MCH_EconomyTechNode>();
    private static boolean initialized;

    private MCH_EconomyTechRegistry() {
    }

    public static synchronized void ensureInitialized() {
        if (initialized) {
            return;
        }
        initialized = true;
        NODE_MAP.clear();

        registerNode(MCH_EconomyTechNode.rpUnlock(NODE_GUNNER_LICENSE, 500, emptySet()));
        registerNode(MCH_EconomyTechNode.geExchange(NODE_EXCHANGE_GE_TO_RP, 1, 0, 300, emptySet()));
        registerNode(MCH_EconomyTechNode.geExchange(NODE_EXCHANGE_GE_TO_SL, 1, 1500, 0, emptySet()));

        Set<String> prereq = singletonSet(NODE_GUNNER_LICENSE);
        Collection<MCH_GunnerInfo> infos = MCH_GunnerInfoManager.getValues();
        if (infos != null) {
            for (MCH_GunnerInfo info : infos) {
                if (info == null || info.itemName == null || info.itemName.trim().isEmpty()) {
                    continue;
                }
                String normalized = normalizeId("npc." + info.itemName.trim());
                registerNode(MCH_EconomyTechNode.slPurchase(normalized, 2500, prereq, info.itemName.trim().toLowerCase(Locale.ROOT)));
            }
        }
    }

    public static synchronized void registerNode(MCH_EconomyTechNode node) {
        if (node == null || node.id == null || node.id.trim().isEmpty()) {
            return;
        }
        NODE_MAP.put(normalizeId(node.id), node);
    }

    public static MCH_EconomyTechNode getNode(String id) {
        ensureInitialized();
        return NODE_MAP.get(normalizeId(id));
    }

    public static Collection<MCH_EconomyTechNode> getNodes() {
        ensureInitialized();
        return Collections.unmodifiableCollection(NODE_MAP.values());
    }

    public static String normalizeId(String id) {
        if (id == null) {
            return "";
        }
        return id.trim().toLowerCase(Locale.ROOT);
    }

    private static Set<String> emptySet() {
        return new LinkedHashSet<String>();
    }

    private static Set<String> singletonSet(String value) {
        LinkedHashSet<String> set = new LinkedHashSet<String>();
        set.add(normalizeId(value));
        return set;
    }
}
