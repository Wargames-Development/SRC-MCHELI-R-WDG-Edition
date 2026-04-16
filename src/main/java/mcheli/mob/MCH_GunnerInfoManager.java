package mcheli.mob;

import mcheli.MCH_InputFile;
import mcheli.MCH_Lib;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class MCH_GunnerInfoManager {

    private static final Map<String, MCH_GunnerInfo> map = new LinkedHashMap<String, MCH_GunnerInfo>();

    public static boolean load(String path) {
        path = path.replace('\\', '/');
        File dir = new File(path + "gunners");
        File[] files = dir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                String s = pathname.getName().toLowerCase();
                return pathname.isFile() && s.endsWith(".txt");
            }
        });
        if (files == null || files.length <= 0) {
            return false;
        }

        map.clear();
        for (File f : files) {
            MCH_InputFile inFile = new MCH_InputFile();
            int line = 0;
            try {
                String key = f.getName().toLowerCase();
                key = key.substring(0, key.length() - 4);
                if (!map.containsKey(key) && inFile.openUTF8(f)) {
                    MCH_GunnerInfo info = new MCH_GunnerInfo(key);
                    info.filePath = f.getAbsolutePath();
                    String str;
                    while ((str = inFile.br.readLine()) != null) {
                        line++;
                        str = str.trim();
                        int eqIdx = str.indexOf('=');
                        if (eqIdx >= 0 && str.length() > eqIdx + 1) {
                            info.loadItemData(str.substring(0, eqIdx).trim().toLowerCase(), str.substring(eqIdx + 1).trim());
                        }
                    }
                    if (info.isValidData()) {
                        map.put(key, info);
                    }
                }
            } catch (IOException e) {
                if (line > 0) {
                    MCH_Lib.Log("### Load failed %s : line=%d", f.getName(), line);
                } else {
                    MCH_Lib.Log("### Load failed %s", f.getName());
                }
                e.printStackTrace();
            } finally {
                inFile.close();
            }
        }

        MCH_Lib.Log("[mcheli] Read %d gunner profiles", map.size());
        return map.size() > 0;
    }

    public static MCH_GunnerInfo get(String name) {
        return map.get(name);
    }

    public static boolean contains(String name) {
        return map.containsKey(name);
    }

    public static Set<String> getKeySet() {
        return map.keySet();
    }

    public static Collection<MCH_GunnerInfo> getValues() {
        return map.values();
    }
}
