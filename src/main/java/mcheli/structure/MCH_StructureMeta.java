package mcheli.structure;

import java.util.LinkedHashMap;
import java.util.Map;

public class MCH_StructureMeta {
    public static final int CURRENT_VERSION = 1;

    public int version = CURRENT_VERSION;
    public String name = "";
    public int sizeX = 1;
    public int sizeY = 1;
    public int sizeZ = 1;
    public int anchorX = 0;
    public int anchorY = 0;
    public int anchorZ = 0;
    public String author = "";
    public String createdAt = "";
    public String description = "";

    public static MCH_StructureMeta fromMap(Map<String, String> map) {
        MCH_StructureMeta meta = new MCH_StructureMeta();
        meta.version = parseInt(map.get("Version"), CURRENT_VERSION);
        meta.name = getOrDefault(map.get("Name"), "");
        int[] size = parseVec3(map.get("Size"), 1, 1, 1);
        meta.sizeX = Math.max(1, size[0]);
        meta.sizeY = Math.max(1, size[1]);
        meta.sizeZ = Math.max(1, size[2]);
        int[] anchor = parseVec3(map.get("Anchor"), 0, 0, 0);
        meta.anchorX = clamp(anchor[0], 0, meta.sizeX - 1);
        meta.anchorY = clamp(anchor[1], 0, meta.sizeY - 1);
        meta.anchorZ = clamp(anchor[2], 0, meta.sizeZ - 1);
        meta.author = getOrDefault(map.get("Author"), "");
        meta.createdAt = getOrDefault(map.get("CreatedAt"), "");
        meta.description = getOrDefault(map.get("Description"), "");
        return meta;
    }

    public Map<String, String> toMap() {
        LinkedHashMap<String, String> map = new LinkedHashMap<String, String>();
        map.put("Version", String.valueOf(this.version));
        map.put("Name", this.name);
        map.put("Size", this.sizeX + "," + this.sizeY + "," + this.sizeZ);
        map.put("Anchor", this.anchorX + "," + this.anchorY + "," + this.anchorZ);
        map.put("Author", this.author);
        map.put("CreatedAt", this.createdAt);
        map.put("Description", this.description);
        return map;
    }

    private static String getOrDefault(String s, String def) {
        return s != null ? s.trim() : def;
    }

    private static int parseInt(String s, int def) {
        if (s == null) {
            return def;
        }
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return def;
        }
    }

    private static int[] parseVec3(String s, int dx, int dy, int dz) {
        int[] out = new int[]{dx, dy, dz};
        if (s == null) {
            return out;
        }
        String[] p = s.split(",");
        if (p.length >= 3) {
            out[0] = parseInt(p[0], dx);
            out[1] = parseInt(p[1], dy);
            out[2] = parseInt(p[2], dz);
        }
        return out;
    }

    private static int clamp(int v, int min, int max) {
        if (v < min) {
            return min;
        }
        return v > max ? max : v;
    }
}
