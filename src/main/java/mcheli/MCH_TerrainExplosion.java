package mcheli;

import mcheli.wgc.Integrations;
import mcheli.wrapper.W_Entity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockFence;
import net.minecraft.entity.Entity;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Server-thread terrain work. An explosion finishes tracing before it alters any blocks. */
final class MCH_TerrainExplosion {
    private static final long TICK_BUDGET_NS = 4_000_000L;
    private static final int TRACE_SLICE = 128;
    private static final int APPLY_SLICE = 16;
    private static final int MAX_ACTIVE_JOBS = 3;
    private static final double MIN_INCOMING = 0.05D;
    private static final double MIN_BREAK_DAMAGE = 2.85D;
    private static final double SURFACE_OFFSET = 0.75D;
    private static final double SURFACE_INSET = 0.0001D;
    private static final double GOLDEN_ANGLE = Math.PI * (3D - Math.sqrt(5D));
    private static final Deque<MCH_TerrainExplosion> WAITING = new ArrayDeque<>();
    private static final Deque<MCH_TerrainExplosion> ACTIVE = new ArrayDeque<>();

    private final MCH_Explosion explosion;
    private final World world;
    private final double radiusSquared;
    private final double radius;
    private final double sourceDamage;
    private final int rayCount;
    private final double rayX;
    private final double rayY;
    private final double rayZ;
    private final boolean ignite;
    private final boolean requiresWGCore;
    private final MovingObjectPosition hit;
    private final ForgeDirection face;
    private final int surfaceRadius;
    private final int surfaceStride;
    private final Map<ChunkPosition, Target> targets = new LinkedHashMap<>();
    private final long enqueuedAt = System.nanoTime();
    private Iterator<Map.Entry<ChunkPosition, Target>> pendingTargets;
    private int nextRay;
    private int nextSurface;
    private int surfaceRays;
    private Ray ray;
    private int visits;
    private int candidates;
    private int approved;
    private double farthestApproved;
    private int lowestApprovedY = Integer.MAX_VALUE;
    private long activeNanos;
    private long longestSliceNanos;

    static void enqueue(MCH_Explosion explosion) {
        if (explosion.param.size > 0F) WAITING.addLast(new MCH_TerrainExplosion(explosion));
    }

    static void clear() {
        WAITING.clear();
        ACTIVE.clear();
    }

    static void tick() {
        long start = System.nanoTime();
        while (System.nanoTime() - start < TICK_BUDGET_NS) {
            while (ACTIVE.size() < MAX_ACTIVE_JOBS && !WAITING.isEmpty()) {
                ACTIVE.addLast(WAITING.removeFirst());
            }
            if (ACTIVE.isEmpty()) return;
            MCH_TerrainExplosion job = ACTIVE.removeFirst();
            if (DimensionManager.getWorld(job.world.provider.dimensionId) != job.world
                    || (job.requiresWGCore && !Integrations.isWGCoreIntegrationEnabled())
                    || !job.explosion.terrainDamageEnabled()) continue;
            long sliceStart = System.nanoTime();
            boolean finished = job.runSlice();
            long duration = System.nanoTime() - sliceStart;
            job.activeNanos += duration;
            job.longestSliceNanos = Math.max(job.longestSliceNanos, duration);
            if (finished) {
                job.logResult();
            } else {
                ACTIVE.addLast(job);
            }
        }
    }

    MCH_TerrainExplosion(MCH_Explosion explosion) {
        this.explosion = explosion;
        this.world = explosion.world;
        MCH_ExplosionParam param = explosion.param;
        this.radius = 2D * param.size;
        this.radiusSquared = radius * radius;
        this.sourceDamage = 1.35D * param.sizeBlock * param.sizeBlock;
        this.rayCount = MCH_Explosion.getSphericalSampleCount(MCH_Explosion.getDynamicRayCount(param.sizeBlock));
        this.hit = param.terrainImpact != null
                && param.terrainImpact.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK
                ? param.terrainImpact : null;
        this.face = hit == null || param.isInWater || !world.blockExists(hit.blockX, hit.blockY, hit.blockZ)
                || world.isAirBlock(hit.blockX, hit.blockY, hit.blockZ) ? ForgeDirection.UNKNOWN
                : ForgeDirection.getOrientation(hit.sideHit);
        // Sample the exposed side of an impact face. Damage still uses the true detonation point.
        this.rayX = param.x + SURFACE_OFFSET * face.offsetX;
        this.rayY = param.y + SURFACE_OFFSET * face.offsetY;
        this.rayZ = param.z + SURFACE_OFFSET * face.offsetZ;
        // Fill in the exposed face where a one-shot break is possible. Spherical rays sample the rest.
        double surfaceReach = Math.min(radius, Math.sqrt(sourceDamage / MIN_BREAK_DAMAGE));
        this.surfaceStride = Math.max(1, (int) Math.ceil(surfaceReach / 64D));
        this.surfaceRadius = face == ForgeDirection.UNKNOWN ? 0 : (int) (surfaceReach / surfaceStride);
        this.ignite = param.isFlaming && MCH_Config.Explosion_FlamingBlock.prmBool;
        this.requiresWGCore = Integrations.isWGCoreIntegrationEnabled();
    }

    boolean runSlice() {
        if (pendingTargets == null) {
            traceSlice();
            return false;
        }
        applySlice();
        return !pendingTargets.hasNext();
    }

    private void traceSlice() {
        if (nextRay == 0 && hit != null && world.blockExists(hit.blockX, hit.blockY, hit.blockZ)) {
            int x = hit.blockX;
            int y = hit.blockY;
            int z = hit.blockZ;
            Block block = world.getBlock(x, y, z);
            if (!block.isAir(world, x, y, z)) {
                record(x, y, z, block, 1D);
            }
        }
        if (ray != null) ray.chunk = null;
        int surfaceWidth = 2 * surfaceRadius + 1;
        int surfaceCount = surfaceRadius > 0 ? surfaceWidth * surfaceWidth : 0;
        for (int steps = 0; steps < TRACE_SLICE; steps++) {
            if (ray == null) {
                if (nextRay < rayCount) {
                    ray = sphericalRay(nextRay++);
                } else if (nextSurface < surfaceCount) {
                    ray = surfaceRay(nextSurface++, surfaceWidth);
                    if (ray == null) continue;
                    surfaceRays++;
                } else {
                    candidates = targets.size();
                    pendingTargets = targets.entrySet().iterator();
                    return;
                }
            }
            if (!visit(ray)) ray = null;
        }
    }

    private Ray sphericalRay(int index) {
        double dy = 1D - 2D * (index + 0.5D) / rayCount;
        double horizontal = Math.sqrt(1D - dy * dy);
        double angle = index * GOLDEN_ANGLE;
        return new Ray(Math.cos(angle) * horizontal, dy, Math.sin(angle) * horizontal,
                radius + (face == ForgeDirection.UNKNOWN ? 0D : SURFACE_OFFSET));
    }

    private Ray surfaceRay(int index, int width) {
        int u = (index % width - surfaceRadius) * surfaceStride;
        int v = (index / width - surfaceRadius) * surfaceStride;
        int x = hit.blockX + (face.offsetX == 0 ? u : 0);
        int y = hit.blockY + (face.offsetY == 0 ? v : 0);
        int z = hit.blockZ + (face.offsetZ != 0 ? 0 : face.offsetX != 0 ? u : v);
        if (distanceSquared(x, y, z) > Math.min(radiusSquared, sourceDamage / MIN_BREAK_DAMAGE)) return null;
        // End just inside the outward face to avoid tracing diagonally through the surface layer.
        double plane = 0.5D - SURFACE_INSET;
        double dx = x + 0.5D + face.offsetX * plane - rayX;
        double dy = y + 0.5D + face.offsetY * plane - rayY;
        double dz = z + 0.5D + face.offsetZ * plane - rayZ;
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        return new Ray(dx / length, dy / length, dz / length, length);
    }

    private boolean visit(Ray current) {
        double nearDistance = Math.max(0D, current.distance - (face == ForgeDirection.UNKNOWN ? 0D : SURFACE_OFFSET));
        if (current.distance >= current.limit
                || incoming(nearDistance * nearDistance, current.transmission) < MIN_INCOMING
                || current.y < 0 || current.y >= world.getHeight()
                || !current.findLoadedChunk()) return false;

        visits++;
        int x = current.x;
        int y = current.y;
        int z = current.z;
        double length = Math.min(current.limit, current.exitDistance()) - current.distance;
        if (length > 0D) {
            Block block = current.chunk.getBlock(x & 15, y, z & 15);
            if (!block.isAir(world, x, y, z)) {
                double resistance = record(x, y, z, block, current.transmission);
                current.transmission *= blockTransmission(resistance, length, block instanceof BlockFence);
            } else if (ignite && y > 0 && world.getBlock(x, y - 1, z).isOpaqueCube()
                    && distanceSquared(x, y, z) <= radiusSquared) {
                targets.putIfAbsent(new ChunkPosition(x, y, z), new Target(block, 0, 0D));
            }
        }
        current.advance();
        return true;
    }

    private double record(int x, int y, int z, Block block, double transmission) {
        ChunkPosition position = new ChunkPosition(x, y, z);
        Target target = targets.get(position);
        double resistance;
        if (target != null && target.block == block) {
            resistance = target.resistance;
        } else {
            float nativeResistance = explosion.param.exploder == null
                    ? block.getExplosionResistance(null, world, x, y, z, explosion.explosionX,
                        explosion.explosionY, explosion.explosionZ)
                    : W_Entity.getBlockExplosionResistance(explosion.param.exploder, explosion, world, x, y, z, block);
            double effective = Integrations.terrainResistance(world, x, y, z, nativeResistance);
            resistance = Double.isNaN(effective) ? Double.POSITIVE_INFINITY : Math.max(0D, effective);
        }
        double distance = distanceSquared(x, y, z);
        double incoming = incoming(distance, transmission);
        if (distance <= radiusSquared && incoming >= MIN_INCOMING && !block.getMaterial().isLiquid()
                && (explosion.param.exploder == null || W_Entity.shouldExplodeBlock(explosion.param.exploder,
                    explosion, world, x, y, z, Block.getIdFromBlock(block), (float) incoming))) {
            int metadata = world.getBlockMetadata(x, y, z);
            if (target == null || target.block != block || target.metadata != metadata) {
                target = new Target(block, metadata, resistance);
                targets.put(position, target);
            }
            target.incoming = Math.max(target.incoming, incoming);
        }
        return resistance;
    }

    private double incoming(double distanceSquared, double transmission) {
        return sourceDamage * transmission / Math.max(1D, distanceSquared);
    }

    static double blockTransmission(double resistance, double length, boolean fence) {
        // Fence rails and posts are a quarter of a block thick. Approximate a crossing through the panel.
        double materialLength = fence ? length * 0.25D : length;
        return Math.exp(-0.35D * resistance * materialLength);
    }

    private double distanceSquared(int x, int y, int z) {
        double dx = x + 0.5D - explosion.explosionX;
        double dy = y + 0.5D - explosion.explosionY;
        double dz = z + 0.5D - explosion.explosionZ;
        return dx * dx + dy * dy + dz * dz;
    }

    private void applySlice() {
        boolean integrated = Integrations.isWGCoreIntegrationEnabled();
        Map<ChunkPosition, Double> batch = new LinkedHashMap<>();
        Map<ChunkPosition, Target> expected = new LinkedHashMap<>();
        for (int i = 0; i < APPLY_SLICE && pendingTargets.hasNext(); i++) {
            Map.Entry<ChunkPosition, Target> entry = pendingTargets.next();
            ChunkPosition pos = entry.getKey();
            Target target = entry.getValue();
            pendingTargets.remove();
            int x = pos.chunkPosX;
            int y = pos.chunkPosY;
            int z = pos.chunkPosZ;
            if (!matches(pos, target)) continue;
            if (integrated || target.incoming == 0D || breaksWithoutWGCore(target, x, y, z)) {
                batch.put(pos, target.incoming);
                expected.put(pos, target);
            }
        }
        if (batch.isEmpty()) return;
        Integrations.ExplosionResult decision;
        Entity previousExploder = explosion.exploder;
        try {
            // Match the source used for native resistance while tracing.
            explosion.exploder = explosion.param.exploder;
            decision = Integrations.evaluateTerrainImpact(world, explosion.getWGCoreActingEntity(), explosion, batch);
        } finally {
            explosion.exploder = previousExploder;
        }
        if (decision == null || !decision.isExplosionAllowed() || !decision.isBlockDamageAllowed()) return;
        List<ChunkPosition> allowed = decision.isFiltered() ? decision.getFilteredAffectedBlocks()
                : new ArrayList<>(batch.keySet());
        for (ChunkPosition pos : allowed) {
            if (batch.get(pos) > 0D) {
                approved++;
                lowestApprovedY = Math.min(lowestApprovedY, pos.chunkPosY);
                double dx = pos.chunkPosX + 0.5D - explosion.explosionX;
                double dz = pos.chunkPosZ + 0.5D - explosion.explosionZ;
                farthestApproved = Math.max(farthestApproved, dx * dx + dz * dz);
            }
        }
        explosion.applyAffectedBlocks(allowed, pos -> matches(pos, expected.get(pos)));
    }

    private boolean matches(ChunkPosition pos, Target target) {
        int x = pos.chunkPosX;
        int y = pos.chunkPosY;
        int z = pos.chunkPosZ;
        return world.blockExists(x, y, z) && world.getBlock(x, y, z) == target.block
                && world.getBlockMetadata(x, y, z) == target.metadata;
    }

    private boolean breaksWithoutWGCore(Target target, int x, int y, int z) {
        double hardness = target.block.getBlockHardness(world, x, y, z);
        if (hardness < 0D) return false;
        double damage = target.incoming / (1D + hardness + 0.2D * target.resistance);
        return damage >= 2.5D * (hardness + 1D) + 0.35D * (target.resistance + 1D);
    }

    private void logResult() {
        if (!MCH_ExplosionDebug.isEnabled()) return;
        MCH_ExplosionDebug.append(
                "[EXPDBG] terrain size=%.2f radius=%.2f rays=%d surfaceSamples=%d visits=%d candidates=%d approved=%d"
                + " outerHorizontalDistance=%.2f lowestY=%s activeMs=%.2f maxSliceMs=%.2f elapsedMs=%.2f",
                explosion.param.sizeBlock, radius, rayCount, surfaceRays, visits, candidates, approved,
                Math.sqrt(farthestApproved), approved == 0 ? "-" : Integer.toString(lowestApprovedY),
                activeNanos / 1e6D, longestSliceNanos / 1e6D, (System.nanoTime() - enqueuedAt) / 1e6D);
    }

    private static final class Target {
        final Block block;
        final int metadata;
        final double resistance;
        double incoming;

        Target(Block block, int metadata, double resistance) {
            this.block = block;
            this.metadata = metadata;
            this.resistance = resistance;
        }
    }

    /** Incremental voxel traversal, so a tick can yield in the middle of a long ray. */
    private final class Ray {
        int x = MathHelper.floor_double(rayX);
        int y = MathHelper.floor_double(rayY);
        int z = MathHelper.floor_double(rayZ);
        final int stepX, stepY, stepZ;
        final double deltaX, deltaY, deltaZ;
        final double limit;
        Chunk chunk;
        int chunkX, chunkZ;
        double nextX, nextY, nextZ;
        double distance;
        double transmission = 1D;

        Ray(double dx, double dy, double dz, double limit) {
            this.limit = limit;
            stepX = dx >= 0D ? 1 : -1;
            stepY = dy >= 0D ? 1 : -1;
            stepZ = dz >= 0D ? 1 : -1;
            deltaX = dx == 0D ? Double.POSITIVE_INFINITY : 1D / Math.abs(dx);
            deltaY = dy == 0D ? Double.POSITIVE_INFINITY : 1D / Math.abs(dy);
            deltaZ = dz == 0D ? Double.POSITIVE_INFINITY : 1D / Math.abs(dz);
            nextX = firstCrossing(rayX, x, stepX, deltaX);
            nextY = firstCrossing(rayY, y, stepY, deltaY);
            nextZ = firstCrossing(rayZ, z, stepZ, deltaZ);
        }

        boolean findLoadedChunk() {
            int nextChunkX = x >> 4;
            int nextChunkZ = z >> 4;
            if (chunk != null && chunkX == nextChunkX && chunkZ == nextChunkZ) return true;
            if (!world.getChunkProvider().chunkExists(nextChunkX, nextChunkZ)) return false;
            chunkX = nextChunkX;
            chunkZ = nextChunkZ;
            chunk = world.getChunkFromChunkCoords(chunkX, chunkZ);
            return true;
        }

        double exitDistance() {
            return Math.min(nextX, Math.min(nextY, nextZ));
        }

        void advance() {
            distance = exitDistance();
            if (nextX == distance) {
                x += stepX;
                nextX += deltaX;
            }
            if (nextY == distance) {
                y += stepY;
                nextY += deltaY;
            }
            if (nextZ == distance) {
                z += stepZ;
                nextZ += deltaZ;
            }
        }
    }

    private static double firstCrossing(double start, int block, int step, double delta) {
        if (Double.isInfinite(delta)) return delta;
        return (step > 0 ? block + 1D - start : start - block) * delta;
    }
}
