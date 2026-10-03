package mcheli;

import mcheli.aircraft.MCH_BoundingBox;
import mcheli.aircraft.MCH_EntityAircraft;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

/** Server terrain evidence shared by radar presentation and tracking validation. */
public final class MCH_RadarTerrain {
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
        // Missing chunks provide no obstruction evidence and must never be loaded by radar.
        Terrain terrain = (x, y, z) -> {
            if (y < 0 || y >= 256) return 0;
            if (!world.blockExists(x, y, z)) return 2;
            Block block = world.getBlock(x, y, z);
            return block.getMaterial() != Material.leaves && block.isOpaqueCube() ? 1 : 0;
        };
        AxisAlignedBB source = emitter.boundingBox;
        double ox = (source.minX + source.maxX) * 0.5D;
        double oy = source.maxY + 0.05D;
        double oz = (source.minZ + source.maxZ) * 0.5D;
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

    // An exposed top or side must count even when the vehicle's origin is hidden.
    static boolean isBoxBlocked(Terrain terrain, double ox, double oy, double oz, AxisAlignedBB box) {
        for (int level = 0; level < 3; level++) {
            double y = box.minY + (box.maxY - box.minY) * (level == 0 ? 0.01D : level == 1 ? 0.5D : 0.99D);
            for (int point = 0; point < 5; point++) {
                double x = box.minX + (box.maxX - box.minX) * (point == 0 ? 0.5D : (point & 1) == 0 ? 0.99D : 0.01D);
                double z = box.minZ + (box.maxZ - box.minZ) * (point == 0 ? 0.5D : point <= 2 ? 0.01D : 0.99D);
                if (!isRayBlocked(terrain, ox, oy, oz, x, y, z)) return false;
            }
        }
        return true;
    }

    /** Voxel traversal avoids vanilla's 200-block ray limit and chunk loading. */
    static boolean isRayBlocked(Terrain terrain, double ox, double oy, double oz, double tx, double ty, double tz) {
        if (!Double.isFinite(ox) || !Double.isFinite(oy) || !Double.isFinite(oz)
            || !Double.isFinite(tx) || !Double.isFinite(ty) || !Double.isFinite(tz)) return false;
        int x = (int)Math.floor(ox), y = (int)Math.floor(oy), z = (int)Math.floor(oz);
        double dx = tx - ox, dy = ty - oy, dz = tz - oz;
        int sx = (int)Math.signum(dx), sy = (int)Math.signum(dy), sz = (int)Math.signum(dz);
        double ax = nextBoundary(ox, x, dx, sx), ay = nextBoundary(oy, y, dy, sy), az = nextBoundary(oz, z, dz, sz);
        double ix = dx == 0.0D ? Double.POSITIVE_INFINITY : Math.abs(1.0D / dx);
        double iy = dy == 0.0D ? Double.POSITIVE_INFINITY : Math.abs(1.0D / dy);
        double iz = dz == 0.0D ? Double.POSITIVE_INFINITY : Math.abs(1.0D / dz);
        // More than the maximum Manhattan traversal for the existing 4096-block radar range.
        for (int steps = 0; steps < 8192; steps++) {
            int state = terrain.blockAt(x, y, z);
            // Known solid terrain is sufficient evidence even after an unloaded gap.
            if (state == 1) return true;
            double next = Math.min(ax, Math.min(ay, az));
            if (next > 1.0D) return false;
            if (ax == next) { x += sx; ax += ix; }
            if (ay == next) { y += sy; ay += iy; }
            if (az == next) { z += sz; az += iz; }
        }
        return false;
    }

    private static double nextBoundary(double origin, int block, double delta, int step) {
        return step == 0 ? Double.POSITIVE_INFINITY : (block + (step > 0 ? 1.0D : 0.0D) - origin) / delta;
    }

    interface Terrain {
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
