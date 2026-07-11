package mcheli.structure;

import cpw.mods.fml.common.IWorldGenerator;
import mcheli.MCH_Lib;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.IChunkProvider;

import java.io.File;
import java.util.List;
import java.util.Random;

public class MCH_WorldStructureGenerator implements IWorldGenerator {
    public void generate(Random random, int chunkX, int chunkZ, World world, IChunkProvider chunkGenerator, IChunkProvider chunkProvider) {
        if (world == null || world.isRemote || random == null) {
            return;
        }
        String worldName = world.getWorldInfo() != null ? world.getWorldInfo().getWorldName() : "<unknown>";
        int dim = world.provider != null ? world.provider.dimensionId : 0;
        List<MCH_StructureRule> rules = MCH_StructureRuleManager.getRules();
        if (rules.isEmpty()) {
            return;
        }
        int centerX = chunkX * 16 + 8;
        int centerZ = chunkZ * 16 + 8;
        for (MCH_StructureRule rule : rules) {
            if (rule == null || !rule.enable) {
                continue;
            }
            MCH_StructureDebugLogger.onRuleChecked();
            if (rule.gridSpacingChunk <= 0) {
                continue;
            }
            if (floorMod(chunkX, rule.gridSpacingChunk) != 0 || floorMod(chunkZ, rule.gridSpacingChunk) != 0) {
                continue;
            }
            if (random.nextFloat() > rule.chance) {
                continue;
            }
            if (!rule.matchesWorld(world, centerX, centerZ)) {
                continue;
            }
            int y = world.getTopSolidOrLiquidBlock(centerX, centerZ);
            if (y < rule.heightMin || y > rule.heightMax) {
                MCH_Lib.Log("[mcheli][struct-gen] skip world=%s dim=%d chunk=%d,%d rule=%s y=%d reason=height_out_of_range",
                    worldName, dim, chunkX, chunkZ, rule.id, y);
                continue;
            }
            if (!checkSlope(world, centerX, centerZ, rule.slopeMax)) {
                MCH_Lib.Log("[mcheli][struct-gen] skip world=%s dim=%d chunk=%d,%d rule=%s reason=slope_too_high",
                    worldName, dim, chunkX, chunkZ, rule.id);
                continue;
            }
            int rot = (random.nextInt(4) * 90) % 360;
            File root = new File("config/mcheli/structures_runtime");
            MCH_StructurePlacer.PlaceResult res = MCH_StructurePlacer.placeStructure(world, root, rule.structure, centerX, y, centerZ, rot, rule.forceSpawnNow);
            if (!res.success) {
                MCH_StructureDebugLogger.onPlaced(false);
                MCH_Lib.Log("[mcheli][struct-gen] failed world=%s dim=%d chunk=%d,%d rule=%s structure=%s error=%s",
                    worldName, dim, chunkX, chunkZ, rule.id, rule.structure, res.error);
                continue;
            }
            MCH_StructureDebugLogger.onPlaced(true);
            MCH_Lib.Log(
                "[mcheli][struct-gen] placed world=%s dim=%d chunk=%d,%d rule=%s structure=%s rot=%d placed=%d skipped=%d te=%d repaired=%d forceTry=%d forceSpawn=%d",
                worldName, dim, chunkX, chunkZ, rule.id, rule.structure, rot,
                res.placed, res.skipped, res.teLoaded, res.repairedBlockInfo, res.forceSpawnTry, res.forceSpawnSuccess
            );
        }
    }

    private static int floorMod(int a, int b) {
        int r = a % b;
        return r < 0 ? r + b : r;
    }

    private static boolean checkSlope(World world, int centerX, int centerZ, int slopeMax) {
        if (slopeMax <= 0 || world == null) {
            return true;
        }
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (int dz = -1; dz <= 1; ++dz) {
            for (int dx = -1; dx <= 1; ++dx) {
                int y = world.getTopSolidOrLiquidBlock(centerX + dx * 4, centerZ + dz * 4);
                if (y < min) {
                    min = y;
                }
                if (y > max) {
                    max = y;
                }
            }
        }
        return max - min <= slopeMax;
    }
}
