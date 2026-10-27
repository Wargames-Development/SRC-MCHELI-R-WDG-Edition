package mcheli.weapon;

import cpw.mods.fml.common.Loader;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.integration.wgmap.WGMapLaserServerBridge;
import mcheli.lweapon.MCH_ItemLightWeaponBase;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

/** Server-thread reconstruction of laser intent, using live collision and trusted WGMap gaps. */
public final class MCH_LaserTargeting {
    private static final double MAX_RANGE = 1536.0D; // 1500-block designation plus camera/seat parallax.
    private static final double AIM_COS = Math.cos(Math.toRadians(5.0D));
    private static final double STEP_EPSILON = 0.00000001D;
    private MCH_LaserTargeting() { }

    public static Vec3 validate(EntityPlayerMP player, int source, double x, double y, double z) {
        if (player == null || player.isDead || player.worldObj == null || player.worldObj.isRemote
                || !position(x, y, z)) return null;
        World world = player.worldObj;
        MCH_EntityAircraft ac = MCH_EntityAircraft.getAircraft_RiddenOrControl(player);
        MCH_WeaponInfo info;
        if (source == MCH_LaserStateStore.SOURCE_AIRCRAFT) {
            MCH_WeaponSet set = ac == null ? null : ac.getCurrentWeapon(player);
            MCH_WeaponBase weapon = set == null ? null : set.getCurrentWeapon();
            if (ac == null || ac.isDead || ac.worldObj != world || weapon == null) return null;
            info = weapon.getInfo();
            // Weapon mode switching is client-local here; the server missile's launch mode
            // already decides whether it consumes laser state or follows TV commands.
        } else if (source == MCH_LaserStateStore.SOURCE_HANDHELD) {
            ItemStack held = player.getHeldItem();
            if (ac != null || held == null || !(held.getItem() instanceof MCH_ItemLightWeaponBase)) return null;
            info = MCH_WeaponInfoManager.get(((MCH_ItemLightWeaponBase)held.getItem()).getWeaponInfoName(held));
        } else return null;
        if (info == null || !info.laserGuidance) return null;
        Vec3 start = Vec3.createVectorHelper(player.posX, player.posY + player.getEyeHeight(), player.posZ);
        // A UAV operator's camera follows the controlled aircraft rather than the station.
        if (ac != null && player.getDistanceSqToEntity(ac) > 64.0D)
            start = Vec3.createVectorHelper(ac.posX, ac.posY + ac.height * 0.5D, ac.posZ);
        Vec3 target = Vec3.createVectorHelper(x, y, z);
        Vec3 look = player.getLookVec();
        if (ac != null && !info.hasLaserGuidancePod) {
            double yaw = Math.toRadians(ac.rotationYaw), pitch = Math.toRadians(ac.rotationPitch);
            look = Vec3.createVectorHelper(-Math.sin(yaw) * Math.cos(pitch), -Math.sin(pitch),
                    Math.cos(yaw) * Math.cos(pitch));
        }
        if (!validAim(start, target, look)) return null;
        Vec3 vehicleTarget = info.lockEntity ? resolveVehicle(world, player, ac, start, target, look) : null;
        if (vehicleTarget != null) target = vehicleTarget;
        double distance = start.distanceTo(target);
        double extend = Math.min(MAX_RANGE, distance + 0.1D) / distance;
        Vec3 end = start.addVector((target.xCoord - start.xCoord) * extend,
                (target.yCoord - start.yCoord) * extend, (target.zCoord - start.zCoord) * extend);
        Vec3 hit = trace(new Terrain() {
            public boolean loaded(int bx, int bz) { return world.getChunkProvider().chunkExists(bx >> 4, bz >> 4); }
            public Vec3 blockHit(int bx, int by, int bz, Vec3 from, Vec3 to) {
                Block block = world.getBlock(bx, by, bz);
                if (!block.canCollideCheck(world.getBlockMetadata(bx, by, bz), false)) return null;
                MovingObjectPosition result = block.collisionRayTrace(world, bx, by, bz, from, to);
                return result == null ? null : result.hitVec;
            }
            public Vec3 missingHit(Vec3 from, Vec3 to) {
                return Loader.isModLoaded("wgmap") ? WGMapLaserServerBridge.trace(world, from, to) : null;
            }
        }, start, end);
        if (hit == null || hit != end) return hit;
        if (vehicleTarget != null) return vehicleTarget; // Authoritative current pose, after actual LOS validation.
        if (info.lockEntity) {
            AxisAlignedBB box = AxisAlignedBB.getBoundingBox(x - 2, y - 2, z - 2, x + 2, y + 2, z + 2);
            for (Object value : world.getEntitiesWithinAABBExcludingEntity(player, box)) {
                Entity entity = (Entity)value;
                if (!entity.isDead && MCH_LaserGuidanceSystem.canHitByLaser(entity, player, ac)
                        && entity.boundingBox.expand(1, 1, 1).isVecInside(target))
                    return Vec3.createVectorHelper(entity.posX, entity.posY + entity.height * 0.5D, entity.posZ);
            }
        }
        // Preserve the original full-range sky aim, but reject arbitrary mid-air coordinates.
        return distance >= 1490.0D ? start.addVector((x - start.xCoord) * 1500.0D / distance,
                (y - start.yCoord) * 1500.0D / distance, (z - start.zCoord) * 1500.0D / distance) : null;
    }

    /** Resolve render/snapshot latency inside the existing aim cone; never trust a client entity ID. */
    private static Vec3 resolveVehicle(World world, EntityPlayerMP player, MCH_EntityAircraft own,
            Vec3 start, Vec3 requested, Vec3 look) {
        final double margin = 64D;
        AxisAlignedBB search = AxisAlignedBB.getBoundingBox(requested.xCoord - margin, requested.yCoord - margin,
                requested.zCoord - margin, requested.xCoord + margin, requested.yCoord + margin, requested.zCoord + margin);
        Vec3 best = null;
        double bestError = Double.POSITIVE_INFINITY;
        int examined = 0;
        for (Object value : world.getEntitiesWithinAABBExcludingEntity(player, search)) {
            if (++examined > 128) break;
            if (!(value instanceof MCH_EntityAircraft) || value == own) continue;
            MCH_EntityAircraft vehicle = (MCH_EntityAircraft)value;
            if (vehicle.isDead || vehicle.isDestroyed() || vehicle.worldObj != world
                    || vehicle instanceof mcheli.tank.MCH_EntityTank
                    && ((mcheli.tank.MCH_EntityTank)vehicle).isUsingFlareType(10)) continue;
            Vec3 centre = Vec3.createVectorHelper(vehicle.posX, vehicle.posY + vehicle.height / 2D, vehicle.posZ);
            double error = requested.squareDistanceTo(centre);
            if (error < bestError && snapshotAimCompatible(start, requested, centre, look)) {
                best = centre; bestError = error;
            }
        }
        return best;
    }

    static boolean snapshotAimCompatible(Vec3 start, Vec3 requested, Vec3 current, Vec3 look) {
        return requested.squareDistanceTo(current) <= 64D * 64D && validAim(start, current, look);
    }

    static boolean position(double x, double y, double z) {
        return Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z)
                && Math.abs(x) < 30000000 && Math.abs(z) < 30000000 && y >= 0 && y <= 1000000;
    }

    static boolean validAim(Vec3 start, Vec3 target, Vec3 look) {
        if (start == null || target == null || look == null || !position(target.xCoord, target.yCoord, target.zCoord))
            return false;
        double dx = target.xCoord - start.xCoord, dy = target.yCoord - start.yCoord, dz = target.zCoord - start.zCoord;
        double distanceSq = dx * dx + dy * dy + dz * dz;
        double dot = dx * look.xCoord + dy * look.yCoord + dz * look.zCoord;
        return Double.isFinite(distanceSq) && distanceSq > 0.0001D && distanceSq <= MAX_RANGE * MAX_RANGE
                && Double.isFinite(dot) && dot > 0
                && (dot >= Math.sqrt(distanceSq) * AIM_COS || distanceSq - dot * dot <= 64.0D);
    }

    /** Voxel traversal is bounded; unloaded spans are queried once per chunk without loading it. */
    public static Vec3 trace(Terrain terrain, Vec3 start, Vec3 end) {
        double dx = end.xCoord - start.xCoord, dy = end.yCoord - start.yCoord, dz = end.zCoord - start.zCoord;
        double distance = start.distanceTo(end);
        if (!Double.isFinite(distance) || distance > MAX_RANGE) return null;
        double t = 0;
        for (int steps = 0; steps < 8192; steps++) {
            int x = MathHelper.floor_double(start.xCoord + dx * t);
            int y = MathHelper.floor_double(start.yCoord + dy * t);
            int z = MathHelper.floor_double(start.zCoord + dz * t);
            double next;
            if (terrain.loaded(x, z)) {
                Vec3 hit = terrain.blockHit(x, y, z, start, end);
                if (hit != null) return hit;
                next = Math.min(boundary(start.xCoord, dx, x, 1),
                        Math.min(boundary(start.yCoord, dy, y, 1), boundary(start.zCoord, dz, z, 1)));
            } else {
                next = Math.min(1, Math.min(boundary(start.xCoord, dx, x >> 4, 16),
                        boundary(start.zCoord, dz, z >> 4, 16)));
                if (next < t) return null;
                // Stay inside this chunk so a touching loaded neighbour needs no persisted snapshot.
                double inset = Math.min(STEP_EPSILON, Math.max(0, (next - t) * 0.25D));
                Vec3 from = start.addVector(dx * (t + inset), dy * (t + inset), dz * (t + inset));
                Vec3 to = start.addVector(dx * (next - inset), dy * (next - inset), dz * (next - inset));
                Vec3 hit = terrain.missingHit(from, to);
                if (hit == null || hit != to) return hit;
            }
            if (next >= 1) return end;
            if (next < t) return null;
            t = Math.min(1, next + STEP_EPSILON);
        }
        return null;
    }

    private static double boundary(double origin, double delta, int cell, int size) {
        return delta == 0 ? Double.POSITIVE_INFINITY
                : ((cell + (delta > 0 ? 1.0D : 0.0D)) * size - origin) / delta;
    }

    public interface Terrain {
        boolean loaded(int x, int z);
        Vec3 blockHit(int x, int y, int z, Vec3 start, Vec3 end);
        // Return the exact end object for clear, a surface point for blocked, or null for unknown.
        Vec3 missingHit(Vec3 start, Vec3 end);
    }
}
