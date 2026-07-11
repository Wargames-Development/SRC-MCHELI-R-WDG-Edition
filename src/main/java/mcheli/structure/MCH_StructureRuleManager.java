package mcheli.structure;

import mcheli.MCH_InputFile;
import mcheli.MCH_Lib;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class MCH_StructureRuleManager {
    private static final List<MCH_StructureRule> RULES = new ArrayList<MCH_StructureRule>();
    private static File loadedDir = null;

    private MCH_StructureRuleManager() {
    }

    public static synchronized void load(File dir) {
        RULES.clear();
        loadedDir = dir;
        if (dir == null) {
            addBuiltinRule();
            return;
        }
        if (!dir.exists() && !dir.mkdirs()) {
            MCH_Lib.Log("[mcheli][struct-rule] failed to create dir=%s", dir.getAbsolutePath());
            addBuiltinRule();
            return;
        }
        File[] files = dir.listFiles(new FileFilter() {
            public boolean accept(File f) {
                return f != null && f.isFile() && f.getName().toLowerCase().endsWith(".txt");
            }
        });
        if (files == null || files.length == 0) {
            MCH_Lib.Log("[mcheli][struct-rule] no rule files found, fallback to builtin rule. dir=%s", dir.getAbsolutePath());
            addBuiltinRule();
            return;
        }
        List<File> list = new ArrayList<File>();
        Collections.addAll(list, files);
        Collections.sort(list, new Comparator<File>() {
            public int compare(File a, File b) {
                return a.getName().compareToIgnoreCase(b.getName());
            }
        });
        for (File f : list) {
            MCH_StructureRule r = readRuleFile(f);
            if (r != null) {
                RULES.add(r);
                MCH_Lib.Log("[mcheli][struct-rule] loaded id=%s structure=%s file=%s", r.id, r.structure, f.getName());
            }
        }
        if (RULES.isEmpty()) {
            addBuiltinRule();
        }
    }

    public static synchronized List<MCH_StructureRule> getRules() {
        return new ArrayList<MCH_StructureRule>(RULES);
    }

    public static synchronized File getLoadedDir() {
        return loadedDir;
    }

    private static void addBuiltinRule() {
        MCH_StructureRule rule = new MCH_StructureRule();
        rule.id = "builtin_mvp";
        rule.structure = "base_small";
        rule.dimensions.add(0);
        rule.biomes.add("plains");
        rule.biomes.add("savanna");
        rule.worldNameWhitelist.add("world");
        rule.gridSpacingChunk = 48;
        rule.chance = 0.18F;
        rule.heightMin = 62;
        rule.heightMax = 90;
        rule.slopeMax = 4;
        rule.forceSpawnNow = true;
        RULES.add(rule);
    }

    private static MCH_StructureRule readRuleFile(File file) {
        MCH_InputFile in = new MCH_InputFile();
        if (!in.openUTF8(file)) {
            return null;
        }
        MCH_StructureRule rule = new MCH_StructureRule();
        rule.id = stripExt(file.getName());
        try {
            while (true) {
                String line = in.readLine();
                if (line == null) {
                    break;
                }
                String s = line.trim();
                if (s.isEmpty() || s.startsWith(";") || s.startsWith("#")) {
                    continue;
                }
                int idx = s.indexOf('=');
                if (idx < 0) {
                    continue;
                }
                String key = s.substring(0, idx).trim().toLowerCase();
                String val = s.substring(idx + 1).trim();
                apply(rule, key, val);
            }
        } finally {
            in.close();
        }
        if (rule.structure == null || rule.structure.trim().isEmpty()) {
            MCH_Lib.Log("[mcheli][struct-rule] skip invalid rule file=%s reason=empty structure", file.getName());
            return null;
        }
        if (rule.gridSpacingChunk <= 0) {
            rule.gridSpacingChunk = 48;
        }
        if (rule.chance < 0.0F) {
            rule.chance = 0.0F;
        }
        if (rule.chance > 1.0F) {
            rule.chance = 1.0F;
        }
        if (rule.heightMin < 1) {
            rule.heightMin = 1;
        }
        if (rule.heightMax > 255) {
            rule.heightMax = 255;
        }
        if (rule.heightMax < rule.heightMin) {
            int t = rule.heightMin;
            rule.heightMin = rule.heightMax;
            rule.heightMax = t;
        }
        if (rule.slopeMax < 0) {
            rule.slopeMax = 0;
        }
        return rule;
    }

    private static void apply(MCH_StructureRule rule, String key, String val) {
        if ("enable".equals(key)) {
            rule.enable = parseBool(val, true);
        } else if ("id".equals(key) || "ruleid".equals(key)) {
            if (!val.isEmpty()) {
                rule.id = val;
            }
        } else if ("structure".equals(key) || "name".equals(key) || "structurename".equals(key)) {
            if (!val.isEmpty()) {
                rule.structure = val;
            }
        } else if ("dimension".equals(key) || "dimensions".equals(key)) {
            parseIntSet(rule.dimensions, val);
        } else if ("biome".equals(key) || "biomes".equals(key)) {
            parseLowerSet(rule.biomes, val);
        } else if ("worldnamewhitelist".equals(key)) {
            parseLowerSet(rule.worldNameWhitelist, val);
        } else if ("worldnameblacklist".equals(key)) {
            parseLowerSet(rule.worldNameBlacklist, val);
        } else if ("gridspacingchunk".equals(key)) {
            rule.gridSpacingChunk = parseInt(val, rule.gridSpacingChunk);
        } else if ("chance".equals(key)) {
            rule.chance = parseFloat(val, rule.chance);
        } else if ("heightmin".equals(key)) {
            rule.heightMin = parseInt(val, rule.heightMin);
        } else if ("heightmax".equals(key)) {
            rule.heightMax = parseInt(val, rule.heightMax);
        } else if ("slopemax".equals(key)) {
            rule.slopeMax = parseInt(val, rule.slopeMax);
        } else if ("forcespawnnow".equals(key)) {
            rule.forceSpawnNow = parseBool(val, rule.forceSpawnNow);
        }
    }

    private static void parseIntSet(java.util.Set<Integer> out, String val) {
        out.clear();
        if (val == null || val.trim().isEmpty()) {
            return;
        }
        String[] p = val.split(",");
        for (String s : p) {
            try {
                out.add(Integer.parseInt(s.trim()));
            } catch (Exception ignored) {
            }
        }
    }

    private static void parseLowerSet(java.util.Set<String> out, String val) {
        out.clear();
        if (val == null || val.trim().isEmpty()) {
            return;
        }
        String[] p = val.split(",");
        for (String s : p) {
            String t = s.trim().toLowerCase();
            if (!t.isEmpty()) {
                out.add(t);
            }
        }
    }

    private static int parseInt(String s, int def) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return def;
        }
    }

    private static float parseFloat(String s, float def) {
        try {
            return Float.parseFloat(s.trim());
        } catch (Exception e) {
            return def;
        }
    }

    private static boolean parseBool(String s, boolean def) {
        if (s == null) {
            return def;
        }
        String v = s.trim().toLowerCase();
        if ("true".equals(v) || "1".equals(v) || "yes".equals(v) || "on".equals(v)) {
            return true;
        }
        if ("false".equals(v) || "0".equals(v) || "no".equals(v) || "off".equals(v)) {
            return false;
        }
        return def;
    }

    private static String stripExt(String n) {
        int i = n.lastIndexOf('.');
        return i >= 0 ? n.substring(0, i) : n;
    }
}
