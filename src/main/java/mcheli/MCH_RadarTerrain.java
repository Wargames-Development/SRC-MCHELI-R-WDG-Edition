package mcheli;

import mcheli.aircraft.MCH_BoundingBox;
import mcheli.aircraft.MCH_EntityAircraft;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.ChunkProviderServer;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

/** Server terrain evidence shared by radar presentation and tracking validation. */
public final class MCH_RadarTerrain {
    private static final int MAX_TERRAIN_CHECKS = 8192;
    private static final double BLOCK_EDGE_MARGIN = 1.0E-7D;
    private static final Map<MCH_EntityAircraft, Cache> caches = new WeakHashMap<>();
    private static final Map<Integer, Cache> published = new ConcurrentHashMap<>();

    private MCH_RadarTerrain() { }

    public static boolean isBlocked(MCH_EntityAircraft emitter, Entity target) {
        if (emitter == null || target == null || emitter == target || emitter.worldObj == null
            || emitter.worldObj.isRemote || target.worldObj != emitter.worldObj
            || !(target instanceof MCH_EntityAircraft)) return false;
        World world = emitter.worldObj;
        long slot = world.getTotalWorldTime() / 5L;
        Cache cache = caches.get(emitter);
        if (cache == null || cache.slot != slot) {
            cache = new Cache(slot, emitter);
            caches.put(emitter, cache);
            published.values().removeIf(old -> old.emitter.get() == null);
            published.put(emitter.getEntityId(), cache);
        }
        Boolean known = cache.targets.get(target.getEntityId());
        if (known != null) return known;
        AxisAlignedBB source = emitter.boundingBox;
        double ox = (source.minX + source.maxX) * 0.5D;
        double oy = source.maxY + 0.05D;
        double oz = (source.minZ + source.maxZ) * 0.5D;
        final int height = world.getHeight();
        // The loaded table avoids both chunk generation and World.blockExists' vanilla Y<256 gate.
        final ChunkProviderServer provider = world instanceof WorldServer
                && world.getChunkProvider() instanceof ChunkProviderServer
                ? (ChunkProviderServer)world.getChunkProvider() : null;
        // Stored LOD and map snapshots cannot verify that these cells are still solid.
        Terrain terrain = (x, y, z) -> {
            if (y < 0 || y >= height) return 0;
            Chunk chunk = provider == null ? null : (Chunk)provider.loadedChunkHashMap.getValueByKey(
                    ChunkCoordIntPair.chunkXZ2Int(x >> 4, z >> 4));
            if (chunk == null || !chunk.isChunkLoaded || chunk.worldObj != world) return 2;
            Block block = chunk.getBlock(x & 15, y, z & 15);
            return block.getMaterial() != Material.leaves && block.isOpaqueCube() ? 1 : 0;
        };
        MCH_EntityAircraft aircraft = (MCH_EntityAircraft)target;
        boolean blocked = isBoxBlocked(terrain, ox, oy, oz, target.boundingBox);
        if (blocked && aircraft.extraBoundingBox != null) {
            for (MCH_BoundingBox box : aircraft.extraBoundingBox) {
                if (box != null && box.boundingBox != null
                    && !isBoxBlocked(terrain, ox, oy, oz, box.boundingBox)) {
                    blocked = false;
                    break;
                }
            }
        }
        cache.targets.put(target.getEntityId(), blocked);
        return blocked;
    }

    /** Network-thread weapon checks consume server evidence without touching the world. */
    public static boolean isKnownBlocked(int emitterId, int targetId) {
        Cache cache = published.get(emitterId);
        return cache != null && cache.emitter.get() != null && Boolean.TRUE.equals(cache.targets.get(targetId));
    }

    /** A solid cross-section proves cover for every point on the box, including between sample rays. */
    static boolean isBoxBlocked(Terrain terrain, double ox, double oy, double oz, AxisAlignedBB box) {
        if (!Double.isFinite(ox) || !Double.isFinite(oy) || !Double.isFinite(oz)
            || !Double.isFinite(box.minX) || !Double.isFinite(box.maxX)
            || !Double.isFinite(box.minY) || !Double.isFinite(box.maxY)
            || !Double.isFinite(box.minZ) || !Double.isFinite(box.maxZ)) return false;
        double dx = Math.abs((box.minX + box.maxX) * 0.5D - ox);
        double dy = Math.abs((box.minY + box.maxY) * 0.5D - oy);
        double dz = Math.abs((box.minZ + box.maxZ) * 0.5D - oz);
        if (dx >= dy && dx >= dz) {
            return hasSolidSlice(terrain, ox, oy, oz, box.minX, box.maxX, box.minY, box.maxY, box.minZ, box.maxZ);
        }
        if (dy >= dz) {
            return hasSolidSlice((y, x, z) -> terrain.blockAt(x, y, z), oy, ox, oz,
                box.minY, box.maxY, box.minX, box.maxX, box.minZ, box.maxZ);
        }
        return hasSolidSlice((z, x, y) -> terrain.blockAt(x, y, z), oz, ox, oy,
            box.minZ, box.maxZ, box.minX, box.maxX, box.minY, box.maxY);
    }

    private static boolean hasSolidSlice(Terrain terrain, double ox, double oy, double oz,
                                         double minX, double maxX, double minY, double maxY, double minZ, double maxZ) {
        if (minX <= ox && ox <= maxX) return false;
        int direction = ox < minX ? 1 : -1;
        double near = (direction > 0 ? minX : maxX) - ox;
        double far = (direction > 0 ? maxX : minX) - ox;
        double middle = (near + far) * 0.5D;
        double centerY = (minY + maxY) * 0.5D - oy;
        double centerZ = (minZ + maxZ) * 0.5D - oz;
        int remaining = MAX_TERRAIN_CHECKS;
        // Skip the emitter's own cell and stop before the bounds. Each plane bisects a block column.
        for (int x = (int)Math.floor(ox) + direction; ; x += direction) {
            double offset = x + 0.5D - ox;
            double nearFraction = offset / near;
            if (nearFraction >= 1.0D || --remaining < 0) return false;
            int centerBlockY = (int)Math.floor(oy + centerY * (offset / middle));
            int centerBlockZ = (int)Math.floor(oz + centerZ * (offset / middle));
            if (terrain.blockAt(x, centerBlockY, centerBlockZ) != 1) continue;

            double farFraction = offset / far;
            int y0 = (int)Math.floor(oy + projectOffset(minY - oy, nearFraction, farFraction) - BLOCK_EDGE_MARGIN);
            int y1 = (int)Math.floor(oy + projectOffset(maxY - oy, farFraction, nearFraction) + BLOCK_EDGE_MARGIN);
            int z0 = (int)Math.floor(oz + projectOffset(minZ - oz, nearFraction, farFraction) - BLOCK_EDGE_MARGIN);
            int z1 = (int)Math.floor(oz + projectOffset(maxZ - oz, farFraction, nearFraction) + BLOCK_EDGE_MARGIN);
            boolean solid = true;
            for (int y = y0; y <= y1 && solid; y++) {
                for (int z = z0; z <= z1; z++) {
                    if (y == centerBlockY && z == centerBlockZ) continue;
                    if (--remaining < 0) return false;
                    if (terrain.blockAt(x, y, z) != 1) {
                        solid = false;
                        break;
                    }
                }
            }
            if (solid) return true;
        }
    }

    private static double projectOffset(double offset, double negativeFraction, double positiveFraction) {
        return offset * (offset < 0.0D ? negativeFraction : positiveFraction);
    }

    public interface Terrain {
        // 0: clear, 1: opaque solid, 2: missing terrain.
        int blockAt(int x, int y, int z);
    }

    private static final class Cache {
        final long slot;
        final WeakReference<MCH_EntityAircraft> emitter;
        final Map<Integer, Boolean> targets = new ConcurrentHashMap<>();
        Cache(long slot, MCH_EntityAircraft emitter) {
            this.slot = slot;
            this.emitter = new WeakReference<>(emitter);
        }
    }
}
