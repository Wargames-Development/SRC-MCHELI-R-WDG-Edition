package mcheli.integration.wgmap.client;

import mcheli.render.MCH_WGMapOcclusion.Result;
import org.junit.Test;
import mcheli.weapon.MCH_LaserTargeting;
import net.minecraft.client.multiplayer.ChunkProviderClient;
import net.minecraft.util.Vec3;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.*;

public class WGMapLaserClientBridgeTest {
    @Test public void actual1710ProviderDoesNotConfuseChunkExistsWithLoadedTerrain() {
        ChunkProviderClient provider = new ChunkProviderClient(null);
        assertTrue("Forge 1.7.10 reports true even for missing client chunks", provider.chunkExists(40, -20));
        assertFalse(WGMapLaserClientBridge.nativeChunkPresent(provider, 40, -20));
        provider.loadChunk(0, 0);
        assertTrue(WGMapLaserClientBridge.nativeChunkPresent(provider, 0, 0));
        assertFalse(WGMapLaserClientBridge.nativeChunkPresent(provider, 40, -20));
        assertFalse(WGMapLaserClientBridge.nativeChunkPresent(null, 0, 0));
    }

    @Test public void nativeNearTerrainNeedsNoSnapshotAndCachedDistantHitHasItsActualRange() {
        MixedTerrain terrain = new MixedTerrain();
        Vec3 hit = WGMapLaserClientBridge.traceTerrain(terrain, point(0.5), point(1500.5));
        assertEquals(731.125, hit.xCoord, 0.000001);
        assertEquals(730.625, point(0.5).distanceTo(hit), 0.000001);
        assertTrue(terrain.gaps > 0 && terrain.gaps < 100);
        assertTrue(terrain.liveReads > 0 && terrain.liveReads < 200);
    }

    @Test public void missingDataResolvesOnTheNextQueryWithoutInventingA1500BlockHit() {
        MixedTerrain terrain = new MixedTerrain(); terrain.missing = true;
        assertNull(WGMapLaserClientBridge.traceTerrain(terrain, point(0.5), point(1500.5)));
        terrain.missing = false;
        assertEquals(731.125, WGMapLaserClientBridge.traceTerrain(terrain, point(0.5), point(1500.5)).xCoord, 0.000001);
        Vec3 entityLimit = point(480);
        assertSame("nearer existing entity hit remains the exact traversal limit", entityLimit,
                WGMapLaserClientBridge.traceTerrain(terrain, point(0.5), entityLimit));
    }

    @Test public void directHitContractHandlesSolidGapEntryAndRejectsUntrustedCoordinates() {
        Vec3 end = point(32);
        assertSame(end, WGMapLaserClientBridge.cachedHit(Result.CLEAR, new double[3], end));
        assertEquals(16.00001, WGMapLaserClientBridge.cachedHit(Result.BLOCKED,
                new double[] {16.00001, 64.5, 0.5}, end).xCoord, 0);
        for (Result unknown : new Result[] {Result.UNKNOWN, Result.MISSING_DATA, Result.UNCERTAIN_BLOCK})
            assertNull(WGMapLaserClientBridge.cachedHit(unknown, new double[] {24, 64.5, 0.5}, end));
        assertNull(WGMapLaserClientBridge.cachedHit(Result.BLOCKED, new double[] {Double.NaN, 0, 0}, end));
    }

    private static Vec3 point(double x) { return Vec3.createVectorHelper(x, 64.5, 0.5); }

    private static final class MixedTerrain implements MCH_LaserTargeting.Terrain {
        int gaps, liveReads;
        boolean missing;
        @Override public boolean loaded(int x, int z) { return x < 192; }
        @Override public Vec3 blockHit(int x, int y, int z, Vec3 start, Vec3 end) {
            assertTrue(loaded(x, z)); liveReads++; return null;
        }
        @Override public Vec3 missingHit(Vec3 from, Vec3 to) {
            assertTrue("never require cached data for the native camera prefix", from.xCoord >= 192);
            gaps++;
            if (missing) return null;
            return WGMapLaserClientBridge.cachedHit(to.xCoord >= 731.125 ? Result.BLOCKED : Result.CLEAR,
                    new double[] {731.125, 64.5, 0.5}, to);
        }
    }

    @Test public void findsFirstDistantSurfaceWithinFiveCentimeters() {
        AtomicInteger calls = new AtomicInteger();
        double hit = WGMapLaserClientBridge.firstHit(1500, t -> {
            calls.incrementAndGet();
            return t * 1500 >= 731.125 ? Result.BLOCKED : Result.CLEAR;
        });
        assertEquals(731.125, hit * 1500, 0.05);
        assertTrue(hit * 1500 >= 731.125);
        assertTrue(calls.get() <= 18);
    }

    @Test public void prefixQueryKeepsThinNearWallAheadOfDistantGround() {
        // Once the prefix crosses the first thin wall, it remains blocked even beyond that wall.
        double hit = WGMapLaserClientBridge.firstHit(1500,
                t -> t * 1500 >= 517.5 ? Result.BLOCKED : Result.CLEAR);
        assertEquals(517.5, hit * 1500, 0.05);
    }

    @Test public void missingAndUncertainPrefixesCannotInventASurface() {
        for (Result unknown : new Result[]{Result.UNKNOWN, Result.MISSING_DATA,
                Result.UNCERTAIN_BLOCK, Result.UNAVAILABLE}) {
            assertTrue(Double.isNaN(WGMapLaserClientBridge.firstHit(1500, t -> unknown)));
            assertTrue(Double.isNaN(WGMapLaserClientBridge.firstHit(1500,
                    t -> t == 0 ? Result.CLEAR : t >= 0.75 ? Result.BLOCKED : unknown)));
        }
    }

    @Test public void clearSkyInsideTerrainAndInvalidRangeHaveNoSyntheticHit() {
        assertTrue(Double.isNaN(WGMapLaserClientBridge.firstHit(1500, t -> Result.CLEAR)));
        assertTrue(Double.isNaN(WGMapLaserClientBridge.firstHit(1500, t -> Result.BLOCKED)));
        for (double range : new double[]{0, -1, 2000, Double.NaN, Double.POSITIVE_INFINITY}) {
            assertTrue(Double.isNaN(WGMapLaserClientBridge.firstHit(range, t -> {
                fail("Invalid ranges must not query WGMap"); return Result.CLEAR;
            })));
        }
    }
}
