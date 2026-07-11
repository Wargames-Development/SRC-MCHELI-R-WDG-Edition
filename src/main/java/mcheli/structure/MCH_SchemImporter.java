package mcheli.structure;

import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class MCH_SchemImporter {
    public static class ImportResult {
        public MCH_StructureMeta meta;
        public MCH_StructureBlob blob;
        public int totalCells;
        public int nonAirBlocks;
        public int tileEntities;
        public int unknownPaletteRefs;
    }

    private MCH_SchemImporter() {
    }

    public static ImportResult importToAsset(File schemFile, File rootDir, String assetName, String author) throws IOException {
        if (schemFile == null || !schemFile.exists() || !schemFile.isFile()) {
            throw new IOException("schem file not found: " + (schemFile != null ? schemFile.getAbsolutePath() : "<null>"));
        }

        NBTTagCompound root;
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(schemFile);
            root = CompressedStreamTools.readCompressed(fis);
        } finally {
            if (fis != null) {
                fis.close();
            }
        }

        int width = readDim(root, "Width");
        int height = readDim(root, "Height");
        int length = readDim(root, "Length");
        if (width <= 0 || height <= 0 || length <= 0) {
            throw new IOException("invalid schem size: " + width + "x" + height + "x" + length);
        }

        NBTTagCompound paletteTag = root.getCompoundTag("Palette");
        byte[] blockData = root.getByteArray("BlockData");
        if (paletteTag == null || blockData == null || blockData.length == 0) {
            throw new IOException("invalid schem data: missing Palette or BlockData");
        }

        Map<Integer, String> palette = buildPaletteMap(paletteTag);
        int volume = width * height * length;
        int[] ids = decodeVarIntArray(blockData, volume);

        Map<Long, NBTTagCompound> tileMap = extractTileEntities(root);

        MCH_StructureMeta meta = new MCH_StructureMeta();
        meta.name = assetName;
        meta.sizeX = width;
        meta.sizeY = height;
        meta.sizeZ = length;
        meta.anchorX = 0;
        meta.anchorY = 0;
        meta.anchorZ = 0;
        meta.author = author != null ? author : "";
        meta.createdAt = String.valueOf(System.currentTimeMillis());
        meta.description = "Imported from .schem: " + schemFile.getName();

        MCH_StructureBlob blob = new MCH_StructureBlob();
        blob.sizeX = width;
        blob.sizeY = height;
        blob.sizeZ = length;

        int nonAir = 0;
        int unknownPaletteRefs = 0;
        int teCount = 0;
        for (int i = 0; i < volume; ++i) {
            int paletteId = i < ids.length ? ids[i] : 0;
            String blockName = palette.get(paletteId);
            if (blockName == null || blockName.isEmpty()) {
                blockName = "minecraft:air";
                unknownPaletteRefs++;
            }
            if ("minecraft:air".equals(blockName)) {
                continue;
            }
            int x = i % width;
            int z = (i / width) % length;
            int y = i / (width * length);

            MCH_StructureBlob.BlockEntry entry = new MCH_StructureBlob.BlockEntry();
            entry.x = x;
            entry.y = y;
            entry.z = z;
            entry.blockName = blockName;
            entry.meta = 0;

            NBTTagCompound te = tileMap.get(packLocalPos(x, y, z));
            if (te != null) {
                entry.tileEntity = (NBTTagCompound) te.copy();
                teCount++;
            }

            blob.blocks.add(entry);
            nonAir++;
        }

        MCH_StructureIO.saveAsset(rootDir, assetName, meta, blob);

        ImportResult result = new ImportResult();
        result.meta = meta;
        result.blob = blob;
        result.totalCells = volume;
        result.nonAirBlocks = nonAir;
        result.tileEntities = teCount;
        result.unknownPaletteRefs = unknownPaletteRefs;
        return result;
    }

    private static int readDim(NBTTagCompound nbt, String key) {
        int v = nbt.getInteger(key);
        if (v > 0) {
            return v;
        }
        return nbt.getShort(key) & 0xFFFF;
    }

    private static Map<Integer, String> buildPaletteMap(NBTTagCompound paletteTag) {
        HashMap<Integer, String> map = new HashMap<Integer, String>();
        Set<?> keys = paletteTag.func_150296_c();
        for (Object o : keys) {
            String stateName = String.valueOf(o);
            int id = paletteTag.getInteger(stateName);
            String blockName = stateName;
            int bracket = blockName.indexOf('[');
            if (bracket >= 0) {
                blockName = blockName.substring(0, bracket);
            }
            if (blockName.indexOf(':') < 0) {
                blockName = "minecraft:" + blockName;
            }
            map.put(id, blockName);
        }
        return map;
    }

    private static int[] decodeVarIntArray(byte[] data, int expectedCount) throws IOException {
        int[] out = new int[expectedCount];
        int outIdx = 0;
        int i = 0;
        while (i < data.length && outIdx < expectedCount) {
            int value = 0;
            int position = 0;
            while (true) {
                if (i >= data.length) {
                    throw new IOException("Invalid BlockData varint stream");
                }
                int current = data[i++] & 0xFF;
                value |= (current & 0x7F) << position;
                if ((current & 0x80) == 0) {
                    break;
                }
                position += 7;
                if (position > 35) {
                    throw new IOException("VarInt too big in BlockData");
                }
            }
            out[outIdx++] = value;
        }
        return out;
    }

    private static Map<Long, NBTTagCompound> extractTileEntities(NBTTagCompound root) {
        HashMap<Long, NBTTagCompound> map = new HashMap<Long, NBTTagCompound>();
        NBTTagList list = root.getTagList("BlockEntities", 10);
        if (list == null || list.tagCount() <= 0) {
            list = root.getTagList("TileEntities", 10);
        }
        if (list == null) {
            return map;
        }
        for (int i = 0; i < list.tagCount(); ++i) {
            NBTTagCompound te = list.getCompoundTagAt(i);
            int x = 0;
            int y = 0;
            int z = 0;
            if (te.hasKey("Pos")) {
                int[] pos = te.getIntArray("Pos");
                if (pos != null && pos.length >= 3) {
                    x = pos[0];
                    y = pos[1];
                    z = pos[2];
                }
            } else {
                x = te.getInteger("x");
                y = te.getInteger("y");
                z = te.getInteger("z");
            }
            map.put(packLocalPos(x, y, z), (NBTTagCompound) te.copy());
        }
        return map;
    }

    private static long packLocalPos(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | (long) (y & 0xFFF);
    }
}
