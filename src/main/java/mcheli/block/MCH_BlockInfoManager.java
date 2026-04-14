package mcheli.block;

import mcheli.MCH_InputFile;
import mcheli.MCH_Lib;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class MCH_BlockInfoManager {

    private static final Map<String, MCH_BlockInfo> map = new LinkedHashMap<String, MCH_BlockInfo>();

    public static boolean load(String path) {
        path = path.replace('\\', '/');
        File dir = new File(path);
        File[] files = dir.listFiles(new FileFilter() {
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
                    MCH_BlockInfo info = new MCH_BlockInfo(key);
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
                    map.put(key, info);
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
        MCH_Lib.Log("[mcheli] Read %d blocks", map.size());
        return map.size() > 0;
    }

    public static MCH_BlockInfo get(String name) {
        return map.get(name);
    }

    public static boolean contains(String name) {
        return map.containsKey(name);
    }

    public static Set<String> getKeySet() {
        return map.keySet();
    }

    public static Collection<MCH_BlockInfo> getValues() {
        return map.values();
    }
}
