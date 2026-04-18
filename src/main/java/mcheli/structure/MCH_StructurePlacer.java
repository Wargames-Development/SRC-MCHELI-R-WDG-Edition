package mcheli.structure;

import mcheli.MCH_Lib;
import mcheli.block.MCH_ConfigSpawnerBlock;
import mcheli.block.MCH_ConfigSpawnerTileEntity;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public final class MCH_StructurePlacer {
    public static class PlaceResult {
        public boolean success;
        public String error = "";
        public int placed;
        public int skipped;
        public int teLoaded;
        public int repairedBlockInfo;
        public int forceSpawnTry;
        public int forceSpawnSuccess;
    }

    private MCH_StructurePlacer() {
    }

    public static PlaceResult placeStructure(World world, File rootDir, String name, int baseX, int baseY, int baseZ, int rot, boolean forceSpawnNow) {
        PlaceResult result = new PlaceResult();
        if (world == null) {
            result.error = "world is null";
            return result;
        }
        MCH_StructureMeta meta;
        MCH_StructureBlob blob;
        try {
            meta = MCH_StructureIO.loadMeta(rootDir, name);
            blob = MCH_StructureIO.loadBlob(rootDir, name);
        } catch (IOException e) {
            result.error = e.getMessage();
            return result;
        }
        int normalizedRot = normalizeRotation(rot);
        if (normalizedRot < 0) {
            result.error = "invalid rot: " + rot;
            return result;
        }

        int[] anchorR = rotateXZ(meta.anchorX, meta.anchorZ, blob.sizeX, blob.sizeZ, normalizedRot);
        HashMap<Long, String> expectedBlockInfoByPos = new HashMap<Long, String>();

        for (MCH_StructureBlob.BlockEntry e : blob.blocks) {
            int[] rz = rotateXZ(e.x, e.z, blob.sizeX, blob.sizeZ, normalizedRot);
            int wx = baseX + (rz[0] - anchorR[0]);
            int wy = baseY + (e.y - meta.anchorY);
            int wz = baseZ + (rz[1] - anchorR[1]);
            if (wy < 0 || wy >= 256 || !world.blockExists(wx, wy, wz)) {
                result.skipped++;
                continue;
            }
            Block block = Block.getBlockFromName(e.blockName);
            if (block == null) {
                result.skipped++;
                continue;
            }
            if (block == Blocks.air && world.isAirBlock(wx, wy, wz)) {
                result.skipped++;
                continue;
            }
            if (!world.setBlock(wx, wy, wz, block, e.meta & 15, 3)) {
                result.skipped++;
                continue;
            }
            String expectedBlockInfo = extractExpectedBlockInfoName(e, block);
            if (expectedBlockInfo != null && !expectedBlockInfo.isEmpty()) {
                expectedBlockInfoByPos.put(packBlockPos(wx, wy, wz), expectedBlockInfo);
            }
            if (e.tileEntity != null) {
                TileEntity te = world.getTileEntity(wx, wy, wz);
                if (te != null) {
                    NBTTagCompound teTag = (NBTTagCompound)e.tileEntity.copy();
                    teTag.setInteger("x", wx);
                    teTag.setInteger("y", wy);
                    teTag.setInteger("z", wz);
                    if (te instanceof MCH_ConfigSpawnerTileEntity) {
                        sanitizeSpawnerRuntimeNBT(teTag);
                    }
                    te.readFromNBT(teTag);
                    te.markDirty();
                    result.teLoaded++;
                }
            }
            result.placed++;
        }

        result.repairedBlockInfo = repairPlacedStructureBlockInfo(world, expectedBlockInfoByPos);
        if (forceSpawnNow) {
            for (Long key : expectedBlockInfoByPos.keySet()) {
                int x = unpackX(key.longValue());
                int y = unpackY(key.longValue());
                int z = unpackZ(key.longValue());
                TileEntity te = world.getTileEntity(x, y, z);
                if (!(te instanceof MCH_ConfigSpawnerTileEntity)) {
                    continue;
                }
                MCH_ConfigSpawnerTileEntity tile = (MCH_ConfigSpawnerTileEntity)te;
                if (tile.getBlockInfo() == null || !tile.getBlockInfo().enableSpawner) {
                    continue;
                }
                result.forceSpawnTry++;
                if (tile.forceSpawnOnceNow()) {
                    result.forceSpawnSuccess++;
                }
            }
        }
        result.success = true;
        return result;
    }

    private static int repairPlacedStructureBlockInfo(World world, Map<Long, String> expectedMap) {
        if (expectedMap == null || expectedMap.isEmpty()) {
            return 0;
        }
        int repaired = 0;
        for (Map.Entry<Long, String> e : expectedMap.entrySet()) {
            long key = e.getKey();
            String expected = e.getValue();
            int x = unpackX(key);
            int y = unpackY(key);
            int z = unpackZ(key);
            TileEntity te = world.getTileEntity(x, y, z);
            if (!(te instanceof MCH_ConfigSpawnerTileEntity)) {
                continue;
            }
            MCH_ConfigSpawnerTileEntity tile = (MCH_ConfigSpawnerTileEntity)te;
            String cur = tile.getBlockInfoName();
            boolean mismatch = cur == null || cur.trim().isEmpty() || !cur.equalsIgnoreCase(expected);
            if (!mismatch && tile.getBlockInfo() != null) {
                continue;
            }
            tile.setBlockInfoName(expected);
            tile.markDirty();
            repaired++;
        }
        return repaired;
    }

    private static void sanitizeSpawnerRuntimeNBT(NBTTagCompound nbt) {
        if (nbt == null) {
            return;
        }
        nbt.removeTag("NextCheckTick");
        nbt.removeTag("CooldownEndTick");
        nbt.removeTag("SpawnedOnce");
        nbt.removeTag("VisualState");
        nbt.removeTag("WaitingVehicleDestroyed");
        nbt.removeTag("TrackedVehicleEntityId");
        nbt.removeTag("TrackedVehicleUuidMost");
        nbt.removeTag("TrackedVehicleUuidLeast");

        nbt.setLong("NextCheckTick", 0L);
        nbt.setLong("CooldownEndTick", 0L);
        nbt.setBoolean("SpawnedOnce", false);
        nbt.setInteger("VisualState", MCH_ConfigSpawnerTileEntity.STATE_ACTIVE);
        nbt.setBoolean("WaitingVehicleDestroyed", false);
        nbt.setInteger("TrackedVehicleEntityId", -1);
        nbt.setLong("TrackedVehicleUuidMost", 0L);
        nbt.setLong("TrackedVehicleUuidLeast", 0L);
    }

    private static String extractExpectedBlockInfoName(MCH_StructureBlob.BlockEntry e, Block block) {
        if (e.tileEntity != null && e.tileEntity.hasKey("BlockInfoName")) {
            String fromTag = e.tileEntity.getString("BlockInfoName");
            if (fromTag != null && !fromTag.trim().isEmpty()) {
                return fromTag.trim();
            }
        }
        if (block instanceof MCH_ConfigSpawnerBlock) {
            String fallback = ((MCH_ConfigSpawnerBlock)block).getDefaultBlockInfoName();
            if (fallback != null && !fallback.trim().isEmpty()) {
                return fallback.trim();
            }
        }
        return "";
    }

    private static int normalizeRotation(int rot) {
        int r = rot % 360;
        if (r < 0) {
            r += 360;
        }
        if (r == 0 || r == 90 || r == 180 || r == 270) {
            return r;
        }
        return -1;
    }

    private static int[] rotateXZ(int x, int z, int sizeX, int sizeZ, int rot) {
        if (rot == 90) {
            return new int[]{sizeZ - 1 - z, x};
        }
        if (rot == 180) {
            return new int[]{sizeX - 1 - x, sizeZ - 1 - z};
        }
        if (rot == 270) {
            return new int[]{z, sizeX - 1 - x};
        }
        return new int[]{x, z};
    }

    private static long packBlockPos(int x, int y, int z) {
        return ((long)(x & 0x3FFFFFF) << 38) | ((long)(z & 0x3FFFFFF) << 12) | (long)(y & 0xFFF);
    }

    private static int unpackX(long packed) {
        int x = (int)(packed >> 38);
        if (x >= 0x2000000) {
            x -= 0x4000000;
        }
        return x;
    }

    private static int unpackY(long packed) {
        return (int)(packed & 0xFFFL);
    }

    private static int unpackZ(long packed) {
        int z = (int)((packed >> 12) & 0x3FFFFFFL);
        if (z >= 0x2000000) {
            z -= 0x4000000;
        }
        return z;
    }
}
