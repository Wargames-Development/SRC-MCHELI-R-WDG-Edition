package mcheli.integration.wgmap.client;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mcheli.render.MCH_WGMapOcclusion;
import mcheli.render.MCH_WGMapOcclusion.Result;
import mcheli.weapon.MCH_LaserTargeting;
import net.minecraft.block.Block;
import net.minecraft.client.multiplayer.ChunkProviderClient;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraft.world.chunk.EmptyChunk;
import net.minecraft.world.chunk.IChunkProvider;

/** Client prediction only: cached terrain never replaces a live block or nearer entity hit. */
@SideOnly(Side.CLIENT)
public final class WGMapLaserClientBridge {
    private WGMapLaserClientBridge() { }

    public static MovingObjectPosition trace(World world, Vec3 start, Vec3 end, MovingObjectPosition liveHit) {
        return traceDetailed(world, start, end, liveHit).hit;
    }

    public static final class Trace {
        public final MovingObjectPosition hit;
        public final boolean known;
        Trace(MovingObjectPosition hit, boolean known) { this.hit = hit; this.known = known; }
    }

    public static Trace traceDetailed(World world, Vec3 start, Vec3 end, MovingObjectPosition liveHit) {
        if (world == null || !world.isRemote || world.provider == null) return new Trace(liveHit, false);
        Vec3 limit = liveHit != null && liveHit.hitVec != null ? liveHit.hitVec : end;
        double distance = start.distanceTo(limit);
        if (!Double.isFinite(distance) || distance <= 0 || distance > 1500.01D) return new Trace(liveHit, false);
        // Warm the bounded complete corridor once, so a chain of initially missing
        // chunks can arrive together. Its coarse result is never a hit position.
        MCH_WGMapOcclusion.traceSegment(world.provider.dimensionId,
                start.xCoord, start.yCoord, start.zCoord,
                limit.xCoord, limit.yCoord, limit.zCoord);
        final double[] coordinates = new double[3];
        Vec3 hit = traceTerrain(new MCH_LaserTargeting.Terrain() {
            @Override public boolean loaded(int x, int z) {
                return nativeChunkPresent(world.getChunkProvider(), x >> 4, z >> 4);
            }
            @Override public Vec3 blockHit(int x, int y, int z, Vec3 from, Vec3 to) {
                Block block = world.getBlock(x, y, z);
                if (!block.canCollideCheck(world.getBlockMetadata(x, y, z), false)) return null;
                MovingObjectPosition result = block.collisionRayTrace(world, x, y, z, from, to);
                return result == null ? null : result.hitVec;
            }
            @Override public Vec3 missingHit(Vec3 from, Vec3 to) {
                Result result = MCH_WGMapOcclusion.traceFirstHit(world.provider.dimensionId,
                        from.xCoord, from.yCoord, from.zCoord, to.xCoord, to.yCoord, to.zCoord, coordinates);
                if (result == Result.UNAVAILABLE) {
                    // Older WGMap keeps its conservative prefix-query fallback.
                    double fraction = firstHit(from.distanceTo(to), t -> MCH_WGMapOcclusion.traceSegment(
                            world.provider.dimensionId, from.xCoord, from.yCoord, from.zCoord,
                            from.xCoord + (to.xCoord - from.xCoord) * t,
                            from.yCoord + (to.yCoord - from.yCoord) * t,
                            from.zCoord + (to.zCoord - from.zCoord) * t));
                    return Double.isFinite(fraction) ? from.addVector((to.xCoord - from.xCoord) * fraction,
                            (to.yCoord - from.yCoord) * fraction, (to.zCoord - from.zCoord) * fraction) : null;
                }
                return cachedHit(result, coordinates, to);
            }
        }, start, limit);
        if (hit == null) return new Trace(liveHit, false);
        if (hit == limit) return new Trace(liveHit, true);
        int x = MathHelper.floor_double(hit.xCoord), y = MathHelper.floor_double(hit.yCoord);
        int z = MathHelper.floor_double(hit.zCoord);
        return new Trace(new MovingObjectPosition(x, y, z, -1, hit), true);
    }

    static boolean nativeChunkPresent(IChunkProvider provider, int x, int z) {
        // In 1.7.10 chunkExists() always returns true on ChunkProviderClient.
        // Its provideChunk() is an in-memory map lookup returning EmptyChunk on
        // a miss; only this verified client provider is safe to call here.
        return provider instanceof ChunkProviderClient
                && !(provider.provideChunk(x, z) instanceof EmptyChunk);
    }

    /** Shared bounded live/gap traversal; no cached data is required for loaded chunks. */
    static Vec3 traceTerrain(MCH_LaserTargeting.Terrain terrain, Vec3 start, Vec3 end) {
        return MCH_LaserTargeting.trace(terrain, start, end);
    }

    static Vec3 cachedHit(Result result, double[] hit, Vec3 end) {
        if (result == Result.CLEAR) return end;
        if (result != Result.BLOCKED || hit == null || hit.length < 3
                || !Double.isFinite(hit[0]) || !Double.isFinite(hit[1]) || !Double.isFinite(hit[2])) return null;
        return Vec3.createVectorHelper(hit[0], hit[1], hit[2]);
    }

    /** Prefix queries locate the first obstruction to within 5 cm without sampling over thin walls. */
    static double firstHit(double distance, java.util.function.DoubleFunction<Result> query) {
        if (!Double.isFinite(distance) || distance <= 0 || distance > 1500.01D) return Double.NaN;
        if (query.apply(1.0D) != Result.BLOCKED || query.apply(0.0D) != Result.CLEAR) return Double.NaN;
        double clear = 0.0D, blocked = 1.0D;
        for (int i = 0; i < 16 && (blocked - clear) * distance > 0.05D; i++) {
            double middle = (clear + blocked) * 0.5D;
            Result result = query.apply(middle);
            if (result == Result.BLOCKED) blocked = middle;
            else if (result == Result.CLEAR) clear = middle;
            else return Double.NaN; // Missing/uncertain terrain cannot establish the nearest surface.
        }
        return blocked;
    }
}
