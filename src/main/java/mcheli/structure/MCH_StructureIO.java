package mcheli.structure;

import mcheli.MCH_InputFile;
import mcheli.MCH_Lib;
import mcheli.MCH_OutputFile;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class MCH_StructureIO {
    public static final String DIR_META = "meta";
    public static final String DIR_BLOB = "blob";
    public static final String EXT_META = ".txt";
    public static final String EXT_BLOB = ".nbt";

    public static File getMetaFile(File rootDir, String name) {
        return new File(new File(rootDir, DIR_META), name + EXT_META);
    }

    public static File getBlobFile(File rootDir, String name) {
        return new File(new File(rootDir, DIR_BLOB), name + EXT_BLOB);
    }

    public static void saveMeta(File rootDir, String name, MCH_StructureMeta meta) throws IOException {
        ensureBaseDirs(rootDir);
        File file = getMetaFile(rootDir, name);
        MCH_OutputFile out = new MCH_OutputFile();
        if (!out.openUTF8(file.getPath())) {
            throw new IOException("Failed to open meta file for writing: " + file.getAbsolutePath());
        }
        try {
            out.writeLine("; MCH structure meta");
            for (Map.Entry<String, String> e : meta.toMap().entrySet()) {
                out.writeLine(e.getKey() + " = " + (e.getValue() != null ? e.getValue() : ""));
            }
        } finally {
            out.close();
        }
    }

    public static MCH_StructureMeta loadMeta(File rootDir, String name) throws IOException {
        File file = getMetaFile(rootDir, name);
        if (!file.exists()) {
            throw new IOException("Meta file not found: " + file.getAbsolutePath());
        }
        MCH_InputFile in = new MCH_InputFile();
        if (!in.openUTF8(file.getPath())) {
            throw new IOException("Failed to open meta file: " + file.getAbsolutePath());
        }
        LinkedHashMap<String, String> map = new LinkedHashMap<String, String>();
        int lineNo = 0;
        try {
            while (true) {
                String line = in.readLine();
                if (line == null) {
                    break;
                }
                ++lineNo;
                String s = line.trim();
                if (s.isEmpty() || s.startsWith(";") || s.startsWith("#")) {
                    continue;
                }
                int idx = s.indexOf('=');
                if (idx < 0) {
                    MCH_Lib.Log("[mcheli][struct-meta] ignore invalid line file=%s line=%d text=%s", file.getName(), lineNo, s);
                    continue;
                }
                String key = s.substring(0, idx).trim();
                String val = s.substring(idx + 1).trim();
                map.put(key, val);
            }
        } finally {
            in.close();
        }
        return MCH_StructureMeta.fromMap(map);
    }

    public static void saveBlob(File rootDir, String name, MCH_StructureBlob blob) throws IOException {
        ensureBaseDirs(rootDir);
        File file = getBlobFile(rootDir, name);
        NBTTagCompound root = blob.toNBT();
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            CompressedStreamTools.writeCompressed(root, fos);
        } finally {
            if (fos != null) {
                fos.close();
            }
        }
    }

    public static MCH_StructureBlob loadBlob(File rootDir, String name) throws IOException {
        File file = getBlobFile(rootDir, name);
        if (!file.exists()) {
            throw new IOException("Blob file not found: " + file.getAbsolutePath());
        }
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            NBTTagCompound root = CompressedStreamTools.readCompressed(fis);
            return MCH_StructureBlob.fromNBT(root);
        } finally {
            if (fis != null) {
                fis.close();
            }
        }
    }

    public static void saveAsset(File rootDir, String name, MCH_StructureMeta meta, MCH_StructureBlob blob) throws IOException {
        saveMeta(rootDir, name, meta);
        saveBlob(rootDir, name, blob);
    }

    private static void ensureBaseDirs(File rootDir) throws IOException {
        if (rootDir == null) {
            throw new IOException("Root dir is null");
        }
        File metaDir = new File(rootDir, DIR_META);
        File blobDir = new File(rootDir, DIR_BLOB);
        if (!metaDir.exists() && !metaDir.mkdirs()) {
            throw new IOException("Failed to create meta dir: " + metaDir.getAbsolutePath());
        }
        if (!blobDir.exists() && !blobDir.mkdirs()) {
            throw new IOException("Failed to create blob dir: " + blobDir.getAbsolutePath());
        }
    }
}
