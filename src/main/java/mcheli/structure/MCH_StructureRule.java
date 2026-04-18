package mcheli.structure;

import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;

import java.util.HashSet;
import java.util.Set;

public class MCH_StructureRule {
    public String id = "rule";
    public boolean enable = true;
    public String structure = "base_small";
    public final Set<Integer> dimensions = new HashSet<Integer>();
    public final Set<String> biomes = new HashSet<String>();
    public final Set<String> worldNameWhitelist = new HashSet<String>();
    public final Set<String> worldNameBlacklist = new HashSet<String>();
    public int gridSpacingChunk = 48;
    public float chance = 0.18F;
    public int heightMin = 62;
    public int heightMax = 90;
    public int slopeMax = 4;
    public boolean forceSpawnNow = true;

    public boolean matchesWorld(World world, int centerX, int centerZ) {
        if (!this.enable || world == null || world.isRemote) {
            return false;
        }
        int dim = world.provider != null ? world.provider.dimensionId : 0;
        if (!this.dimensions.isEmpty() && !this.dimensions.contains(dim)) {
            return false;
        }
        String worldName = world.getWorldInfo() != null ? safeLower(world.getWorldInfo().getWorldName()) : "";
        if (!this.worldNameWhitelist.isEmpty() && !this.worldNameWhitelist.contains(worldName)) {
            return false;
        }
        if (!this.worldNameBlacklist.isEmpty() && this.worldNameBlacklist.contains(worldName)) {
            return false;
        }
        BiomeGenBase biome = world.getBiomeGenForCoords(centerX, centerZ);
        String biomeName = biome != null ? safeLower(biome.biomeName) : "";
        if (!this.biomes.isEmpty() && !this.biomes.contains(biomeName)) {
            return false;
        }
        return true;
    }

    private static String safeLower(String s) {
        return s != null ? s.trim().toLowerCase() : "";
    }
}
