package mcheli.economy;

import cpw.mods.fml.common.Loader;
import mcheli.mob.MCH_GunnerInfo;
import mcheli.mob.MCH_GunnerInfoManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class MCH_EconomyTechRegistry {

    public static final String NODE_GUNNER_LICENSE = "core.gunner_license";
    public static final String NODE_EXCHANGE_GE_TO_RP = "exchange.ge_to_rp_small";
    public static final String NODE_EXCHANGE_GE_TO_SL = "exchange.ge_to_sl_small";

    private static final Map<String, MCH_EconomyTechNode> NODE_MAP = new LinkedHashMap<String, MCH_EconomyTechNode>();
    private static final Map<String, String> TREE_DISPLAY_NAME_MAP = new LinkedHashMap<String, String>();
    private static final Map<String, Map<String, String>> TREE_LOCALIZED_DISPLAY_NAME_MAP = new LinkedHashMap<String, Map<String, String>>();
    private static boolean initialized;

    private MCH_EconomyTechRegistry() {
    }

    public static synchronized void ensureInitialized() {
        if (initialized) {
            return;
        }
        initialized = true;
        NODE_MAP.clear();
        TREE_DISPLAY_NAME_MAP.clear();
        TREE_LOCALIZED_DISPLAY_NAME_MAP.clear();

        registerNode(MCH_EconomyTechNode.rpUnlock(NODE_GUNNER_LICENSE, 500, emptySet()));
        registerNode(MCH_EconomyTechNode.geExchange(NODE_EXCHANGE_GE_TO_RP, 1, 0, 300, emptySet()));
        registerNode(MCH_EconomyTechNode.geExchange(NODE_EXCHANGE_GE_TO_SL, 1, 1500, 0, emptySet()));
        loadTechTreeConfigs();

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

    public static Collection<MCH_EconomyTechNode> getNodesByTree(String treeId) {
        ensureInitialized();
        String normalizedTreeId = normalizeId(treeId);
        if (normalizedTreeId.isEmpty()) {
            return getNodes();
        }
        List<MCH_EconomyTechNode> list = new ArrayList<MCH_EconomyTechNode>();
        for (MCH_EconomyTechNode node : NODE_MAP.values()) {
            if (normalizedTreeId.equals(normalizeId(node.treeId))) {
                list.add(node);
            }
        }
        return Collections.unmodifiableList(list);
    }

    public static Collection<MCH_EconomyTechNode> getNodesByTrees(Set<String> treeIds) {
        ensureInitialized();
        if (treeIds == null || treeIds.isEmpty()) {
            return getNodes();
        }
        LinkedHashSet<String> normalizedSet = new LinkedHashSet<String>();
        for (String id : treeIds) {
            String normalized = normalizeId(id);
            if (!normalized.isEmpty()) {
                normalizedSet.add(normalized);
            }
        }
        if (normalizedSet.isEmpty()) {
            return getNodes();
        }
        List<MCH_EconomyTechNode> list = new ArrayList<MCH_EconomyTechNode>();
        for (MCH_EconomyTechNode node : NODE_MAP.values()) {
            if (normalizedSet.contains(normalizeId(node.treeId))) {
                list.add(node);
            }
        }
        return Collections.unmodifiableList(list);
    }

    public static String normalizeId(String id) {
        if (id == null) {
            return "";
        }
        return id.trim().toLowerCase(Locale.ROOT);
    }

    public static String getTreeDisplayName(String treeId) {
        return getTreeDisplayName(treeId, "");
    }

    public static String getTreeDisplayName(String treeId, String languageCode) {
        ensureInitialized();
        String id = normalizeId(treeId);
        if (id.isEmpty()) {
            return "";
        }
        String lang = normalizeLang(languageCode);
        if (!lang.isEmpty()) {
            Map<String, String> localized = TREE_LOCALIZED_DISPLAY_NAME_MAP.get(id);
            if (localized != null && !localized.isEmpty()) {
                String exact = localized.get(lang);
                if (exact != null && !exact.trim().isEmpty()) {
                    return exact.trim();
                }
                int sep = lang.indexOf('_');
                if (sep > 0) {
                    String primary = lang.substring(0, sep);
                    String primaryValue = localized.get(primary);
                    if (primaryValue != null && !primaryValue.trim().isEmpty()) {
                        return primaryValue.trim();
                    }
                }
            }
        }
        String display = TREE_DISPLAY_NAME_MAP.get(id);
        if (display == null || display.trim().isEmpty()) {
            return id;
        }
        return display.trim();
    }

    private static Set<String> emptySet() {
        return new LinkedHashSet<String>();
    }

    private static Set<String> singletonSet(String value) {
        LinkedHashSet<String> set = new LinkedHashSet<String>();
        set.add(normalizeId(value));
        return set;
    }

    private static void loadTechTreeConfigs() {
        File dir = new File(Loader.instance().getConfigDir(), "mcheli/tech_tree");
        if (!dir.isDirectory()) {
            return;
        }
        File[] files = dir.listFiles();
        if (files == null || files.length == 0) {
            return;
        }
        for (File file : files) {
            if (file == null || !file.isFile()) {
                continue;
            }
            String name = file.getName().toLowerCase(Locale.ROOT);
            if (!name.endsWith(".txt")) {
                continue;
            }
            loadTechTreeFile(file);
        }
    }

    private static void loadTechTreeFile(File file) {
        String treeId = normalizeId(file.getName().replace(".txt", ""));
        String displayName = treeId;
        LinkedHashMap<String, String> localizedDisplayName = new LinkedHashMap<String, String>();
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
            String line;
            while ((line = br.readLine()) != null) {
                String raw = stripComment(line).trim();
                if (raw.isEmpty()) {
                    continue;
                }
                int idx = raw.indexOf('=');
                if (idx <= 0 || idx >= raw.length() - 1) {
                    continue;
                }
                String key = raw.substring(0, idx).trim();
                if (!key.isEmpty() && key.charAt(0) == '\uFEFF') {
                    key = key.substring(1);
                }
                String value = raw.substring(idx + 1).trim();
                if (key.equalsIgnoreCase("TechTreeId")) {
                    String oldTreeId = treeId;
                    String id = normalizeId(value);
                    if (!id.isEmpty()) {
                        treeId = id;
                        if (displayName == null || displayName.trim().isEmpty() || normalizeId(displayName).equals(oldTreeId)) {
                            displayName = id;
                        }
                    }
                    continue;
                }
                if (key.equalsIgnoreCase("DisplayName")) {
                    if (!value.isEmpty()) {
                        displayName = value;
                    }
                    continue;
                }
                if (key.equalsIgnoreCase("AddDisplayName")) {
                    int split = value.indexOf(',');
                    if (split > 0 && split < value.length() - 1) {
                        String lang = normalizeLang(value.substring(0, split));
                        String text = value.substring(split + 1).trim();
                        if (!lang.isEmpty() && !text.isEmpty()) {
                            localizedDisplayName.put(lang, text);
                        }
                    }
                    continue;
                }
                if (key.equalsIgnoreCase("Node")) {
                    registerNode(parseNode(treeId, value));
                }
            }
            if (!treeId.isEmpty()) {
                TREE_DISPLAY_NAME_MAP.put(treeId, (displayName == null || displayName.trim().isEmpty()) ? treeId : displayName.trim());
                if (!localizedDisplayName.isEmpty()) {
                    TREE_LOCALIZED_DISPLAY_NAME_MAP.put(treeId, new LinkedHashMap<String, String>(localizedDisplayName));
                }
            }
        } catch (IOException ignored) {
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    private static MCH_EconomyTechNode parseNode(String treeId, String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String[] arr = value.split(",", -1);
        if (arr.length < 1) {
            return null;
        }
        String nodeId = normalizeId(arr[0]);
        if (nodeId.isEmpty()) {
            return null;
        }
        String vehicleName = arr.length > 2 ? arr[2].trim().toLowerCase(Locale.ROOT) : "";
        int rpCost = arr.length > 4 ? parseInt(arr[4]) : 0;
        int slCost = arr.length > 5 ? parseInt(arr[5]) : 0;
        int gePrice = arr.length > 6 ? parseInt(arr[6]) : 0;
        Set<String> prerequisites = parsePrerequisites(arr.length > 8 ? arr[8] : "");

        if (rpCost > 0) {
            return MCH_EconomyTechNode.rpUnlock(nodeId, rpCost, prerequisites, vehicleName, slCost, treeId);
        }
        if (gePrice > 0 && slCost > 0) {
            return MCH_EconomyTechNode.geUnlock(nodeId, gePrice, prerequisites, vehicleName, slCost, treeId);
        }
        if (slCost > 0) {
            return MCH_EconomyTechNode.slPurchase(nodeId, slCost, prerequisites, vehicleName, treeId);
        }
        if (gePrice > 0) {
            return MCH_EconomyTechNode.geExchange(nodeId, gePrice, 0, 0, prerequisites, treeId);
        }
        return null;
    }

    private static Set<String> parsePrerequisites(String raw) {
        LinkedHashSet<String> set = new LinkedHashSet<String>();
        if (raw == null) {
            return set;
        }
        String normalized = raw.trim();
        if (normalized.isEmpty()) {
            return set;
        }
        String[] arr = normalized.split("[&|]");
        for (String s : arr) {
            String id = normalizeId(s);
            if (!id.isEmpty()) {
                set.add(id);
            }
        }
        return set;
    }

    private static int parseInt(String value) {
        if (value == null || value.trim().isEmpty()) {
            return 0;
        }
        try {
            return Math.max(0, Integer.parseInt(value.trim()));
        } catch (Exception ignored) {
            return 0;
        }
    }

    private static String normalizeLang(String languageCode) {
        if (languageCode == null) {
            return "";
        }
        return languageCode.trim().toLowerCase(Locale.ROOT).replace('-', '_');
    }

    private static String stripComment(String line) {
        if (line == null) {
            return "";
        }
        int hash = line.indexOf('#');
        int semicolon = line.indexOf(';');
        int cut = -1;
        if (hash >= 0) {
            cut = hash;
        }
        if (semicolon >= 0 && (cut < 0 || semicolon < cut)) {
            cut = semicolon;
        }
        if (cut < 0) {
            return line;
        }
        return line.substring(0, cut);
    }
}
