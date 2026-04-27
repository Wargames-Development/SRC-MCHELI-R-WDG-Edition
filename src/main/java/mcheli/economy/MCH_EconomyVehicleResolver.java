package mcheli.economy;

import cpw.mods.fml.common.Loader;
import mcheli.aircraft.MCH_AircraftInfo;
import mcheli.helicopter.MCH_HeliInfoManager;
import mcheli.plane.MCP_PlaneInfoManager;
import mcheli.tank.MCH_TankInfoManager;
import mcheli.vehicle.MCH_VehicleInfoManager;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class MCH_EconomyVehicleResolver {

    private static final Map<String, String> DISPLAY_NAME_CACHE = new LinkedHashMap<String, String>();

    private MCH_EconomyVehicleResolver() {
    }

    public static ItemStack createVehicleStack(String vehicleName) {
        MCH_AircraftInfo info = findAircraftInfo(vehicleName);
        if (info == null) {
            return null;
        }
        Item item = info.getItem();
        if (item == null) {
            return null;
        }
        return new ItemStack(item);
    }

    public static MCH_AircraftInfo findAircraftInfo(String vehicleName) {
        String key = normalize(vehicleName);
        if (key.isEmpty()) {
            return null;
        }
        MCH_AircraftInfo byMapKey = findByMapKey(MCP_PlaneInfoManager.map, key);
        if (byMapKey != null) {
            return byMapKey;
        }
        MCH_AircraftInfo info = findInMap(MCP_PlaneInfoManager.map, key);
        if (info != null) {
            return info;
        }
        byMapKey = findByMapKey(MCH_HeliInfoManager.map, key);
        if (byMapKey != null) {
            return byMapKey;
        }
        info = findInMap(MCH_HeliInfoManager.map, key);
        if (info != null) {
            return info;
        }
        byMapKey = findByMapKey(MCH_TankInfoManager.map, key);
        if (byMapKey != null) {
            return byMapKey;
        }
        info = findInMap(MCH_TankInfoManager.map, key);
        if (info != null) {
            return info;
        }
        byMapKey = findByMapKey(MCH_VehicleInfoManager.map, key);
        if (byMapKey != null) {
            return byMapKey;
        }
        return findInMap(MCH_VehicleInfoManager.map, key);
    }

    public static String getVehicleDisplayName(String vehicleName, String languageCode) {
        MCH_AircraftInfo info = findAircraftInfo(vehicleName);
        String lang = normalizeLang(languageCode);
        if (info != null) {
            String localized = resolveLocalizedDisplayName(info.displayNameLang, lang);
            if (!localized.isEmpty()) {
                return localized;
            }
            if (info.displayName != null && !info.displayName.trim().isEmpty()) {
                return info.displayName.trim();
            }
            if (info.name != null && !info.name.trim().isEmpty()) {
                return info.name.trim();
            }
        }
        String fromFile = loadDisplayNameFromConfig(vehicleName, lang);
        if (!fromFile.isEmpty()) {
            return fromFile;
        }
        return vehicleName == null ? "" : vehicleName;
    }

    private static MCH_AircraftInfo findByMapKey(Map map, String key) {
        if (map == null || map.isEmpty()) {
            return null;
        }
        for (Object entryObj : map.entrySet()) {
            if (!(entryObj instanceof Map.Entry)) {
                continue;
            }
            Map.Entry entry = (Map.Entry) entryObj;
            String entryKey = normalize(entry.getKey() == null ? "" : entry.getKey().toString());
            if (!key.equals(entryKey)) {
                continue;
            }
            Object value = entry.getValue();
            if (value instanceof MCH_AircraftInfo) {
                return (MCH_AircraftInfo) value;
            }
        }
        return null;
    }

    private static String resolveLocalizedDisplayName(Map displayNameLang, String lang) {
        if (displayNameLang == null || displayNameLang.isEmpty() || lang.isEmpty()) {
            return "";
        }
        String exact = mapValueIgnoreCase(displayNameLang, lang);
        if (!exact.isEmpty()) {
            return exact;
        }
        int sep = lang.indexOf('_');
        if (sep > 0) {
            return mapValueIgnoreCase(displayNameLang, lang.substring(0, sep));
        }
        return "";
    }

    private static String mapValueIgnoreCase(Map map, String key) {
        if (map == null || map.isEmpty() || key == null || key.isEmpty()) {
            return "";
        }
        Object direct = map.get(key);
        if (direct != null) {
            String text = direct.toString().trim();
            if (!text.isEmpty()) {
                return text;
            }
        }
        for (Object entryObj : map.entrySet()) {
            if (!(entryObj instanceof Map.Entry)) {
                continue;
            }
            Map.Entry entry = (Map.Entry) entryObj;
            if (entry.getKey() == null || !key.equalsIgnoreCase(entry.getKey().toString())) {
                continue;
            }
            Object value = entry.getValue();
            String text = value == null ? "" : value.toString().trim();
            if (!text.isEmpty()) {
                return text;
            }
        }
        return "";
    }

    private static String loadDisplayNameFromConfig(String vehicleName, String lang) {
        String key = normalize(vehicleName);
        if (key.isEmpty()) {
            return "";
        }
        String cacheKey = key + "|" + lang;
        String cached = DISPLAY_NAME_CACHE.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        String value = parseDisplayNameFromConfigDir("planes", key, lang);
        if (value.isEmpty()) {
            value = parseDisplayNameFromConfigDir("helicopters", key, lang);
        }
        if (value.isEmpty()) {
            value = parseDisplayNameFromConfigDir("tanks", key, lang);
        }
        if (value.isEmpty()) {
            value = parseDisplayNameFromConfigDir("vehicles", key, lang);
        }
        DISPLAY_NAME_CACHE.put(cacheKey, value);
        return value;
    }

    private static String parseDisplayNameFromConfigDir(String childDir, String vehicleKey, String lang) {
        File root = new File(Loader.instance().getConfigDir(), "mcheli/" + childDir);
        if (!root.isDirectory()) {
            return "";
        }
        File[] files = root.listFiles();
        if (files == null || files.length == 0) {
            return "";
        }
        for (File file : files) {
            if (file == null || !file.isFile()) {
                continue;
            }
            String fileKey = normalize(file.getName().replace(".txt", ""));
            if (!vehicleKey.equals(fileKey)) {
                continue;
            }
            return parseDisplayNameFromFile(file, lang);
        }
        return "";
    }

    private static String parseDisplayNameFromFile(File file, String lang) {
        BufferedReader br = null;
        String displayName = "";
        LinkedHashMap<String, String> localized = new LinkedHashMap<String, String>();
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
                if (key.equalsIgnoreCase("DisplayName")) {
                    if (!value.isEmpty()) {
                        displayName = value;
                    }
                } else if (key.equalsIgnoreCase("AddDisplayName")) {
                    int split = value.indexOf(',');
                    if (split > 0 && split < value.length() - 1) {
                        String k = normalizeLang(value.substring(0, split));
                        String v = value.substring(split + 1).trim();
                        if (!k.isEmpty() && !v.isEmpty()) {
                            localized.put(k, v);
                        }
                    }
                }
            }
        } catch (IOException ignored) {
            return "";
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException ignored) {
                }
            }
        }
        String localizedName = mapValueIgnoreCase(localized, lang);
        if (!localizedName.isEmpty()) {
            return localizedName;
        }
        int sep = lang.indexOf('_');
        if (sep > 0) {
            String primary = mapValueIgnoreCase(localized, lang.substring(0, sep));
            if (!primary.isEmpty()) {
                return primary;
            }
        }
        return displayName;
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

    private static MCH_AircraftInfo findInMap(Map map, String key) {
        if (map == null || map.isEmpty()) {
            return null;
        }
        for (Object obj : map.values()) {
            if (!(obj instanceof MCH_AircraftInfo)) {
                continue;
            }
            MCH_AircraftInfo info = (MCH_AircraftInfo) obj;
            if (key.equals(normalize(info.name))) {
                return info;
            }
            if (key.equals(normalize(info.displayName))) {
                return info;
            }
            if (info.displayNameLang != null) {
                for (Object value : info.displayNameLang.values()) {
                    if (key.equals(normalize(value == null ? "" : value.toString()))) {
                        return info;
                    }
                }
            }
        }
        return null;
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeLang(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT).replace('-', '_');
    }
}
