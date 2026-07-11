package mcheli.mob;

import cpw.mods.fml.common.Loader;
import mcheli.economy.MCH_EconomyTechRegistry;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public final class MCH_TechNpcConfig {

    private static volatile boolean loaded;
    private static volatile String displayName = "TechQuartermaster";
    private static volatile String techTreeId = "";
    private static volatile String techTreeIdsRaw = "";
    private static volatile String modelType = "steve";
    private static volatile String skin = "minecraft:textures/entity/steve.png";
    private static volatile String skinFallback = "minecraft:textures/entity/steve.png";
    private static volatile boolean enableVillageSpawn = false;
    private static volatile int villageWeight = 20;
    private static volatile int minVillagePopulation = 3;
    private static volatile int spawnCooldownTick = 24000;
    private static volatile int maxPerVillage = 1;
    private static volatile int minPlayerDistance = 16;
    private static volatile int maxPlayerDistance = 96;

    private MCH_TechNpcConfig() {
    }

    public static synchronized void ensureLoaded() {
        if (loaded) {
            return;
        }
        loaded = true;
        File file = new File(Loader.instance().getConfigDir(), "mcheli/npc/tech_npc_trader.txt");
        if (!file.isFile()) {
            return;
        }
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(file));
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
                String value = raw.substring(idx + 1).trim();
                if (key.equalsIgnoreCase("DisplayName") && !value.isEmpty()) {
                    displayName = value;
                } else if (key.equalsIgnoreCase("TechTreeId")) {
                    techTreeIdsRaw = normalizeTreeIds(value);
                    techTreeId = firstTreeId(techTreeIdsRaw);
                } else if (key.equalsIgnoreCase("ModelType") && !value.isEmpty()) {
                    modelType = value.trim().toLowerCase();
                } else if (key.equalsIgnoreCase("Skin") && !value.isEmpty()) {
                    skin = value.trim();
                } else if (key.equalsIgnoreCase("SkinFallback") && !value.isEmpty()) {
                    skinFallback = value.trim();
                } else if (key.equalsIgnoreCase("EnableVillageSpawn")) {
                    enableVillageSpawn = parseBool(value, enableVillageSpawn);
                } else if (key.equalsIgnoreCase("VillageWeight")) {
                    villageWeight = clamp(parseInt(value, villageWeight), 0, 100);
                } else if (key.equalsIgnoreCase("MinVillagePopulation")) {
                    minVillagePopulation = Math.max(0, parseInt(value, minVillagePopulation));
                } else if (key.equalsIgnoreCase("SpawnCooldownTick")) {
                    spawnCooldownTick = Math.max(200, parseInt(value, spawnCooldownTick));
                } else if (key.equalsIgnoreCase("MaxPerVillage")) {
                    maxPerVillage = Math.max(1, parseInt(value, maxPerVillage));
                } else if (key.equalsIgnoreCase("MinPlayerDistance")) {
                    minPlayerDistance = Math.max(0, parseInt(value, minPlayerDistance));
                } else if (key.equalsIgnoreCase("MaxPlayerDistance")) {
                    maxPlayerDistance = Math.max(minPlayerDistance + 1, parseInt(value, maxPlayerDistance));
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

    public static String getDisplayName() {
        ensureLoaded();
        return displayName;
    }

    public static String getTechTreeId() {
        ensureLoaded();
        return techTreeId;
    }

    public static String getTechTreeIdsRaw() {
        ensureLoaded();
        return techTreeIdsRaw;
    }

    public static String getModelType() {
        ensureLoaded();
        return modelType;
    }

    public static String getSkin() {
        ensureLoaded();
        return skin;
    }

    public static String getSkinFallback() {
        ensureLoaded();
        return skinFallback;
    }

    public static boolean isVillageSpawnEnabled() {
        ensureLoaded();
        return enableVillageSpawn;
    }

    public static int getVillageWeight() {
        ensureLoaded();
        return villageWeight;
    }

    public static int getMinVillagePopulation() {
        ensureLoaded();
        return minVillagePopulation;
    }

    public static int getSpawnCooldownTick() {
        ensureLoaded();
        return spawnCooldownTick;
    }

    public static int getMaxPerVillage() {
        ensureLoaded();
        return maxPerVillage;
    }

    public static int getMinPlayerDistance() {
        ensureLoaded();
        return minPlayerDistance;
    }

    public static int getMaxPlayerDistance() {
        ensureLoaded();
        return maxPlayerDistance;
    }

    private static String normalizeTreeIds(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return "";
        }
        String[] arr = raw.split("[|,;]");
        StringBuilder sb = new StringBuilder();
        for (String value : arr) {
            String id = MCH_EconomyTechRegistry.normalizeId(value);
            if (id.isEmpty()) {
                continue;
            }
            if (sb.indexOf(id) >= 0) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(';');
            }
            sb.append(id);
        }
        return sb.toString();
    }

    private static String firstTreeId(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }
        int idx = raw.indexOf(';');
        if (idx < 0) {
            return raw;
        }
        return raw.substring(0, idx);
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

    private static int parseInt(String value, int fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static boolean parseBool(String value, boolean fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        String s = value.trim();
        if ("true".equalsIgnoreCase(s) || "1".equals(s) || "yes".equalsIgnoreCase(s) || "on".equalsIgnoreCase(s)) {
            return true;
        }
        if ("false".equalsIgnoreCase(s) || "0".equals(s) || "no".equalsIgnoreCase(s) || "off".equalsIgnoreCase(s)) {
            return false;
        }
        return fallback;
    }

    private static int clamp(int value, int min, int max) {
        if (value < min) {
            return min;
        }
        return Math.min(value, max);
    }
}
