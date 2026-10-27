package mcheli.weapon;

import net.minecraft.util.Vec3;
import org.junit.Test;
import static org.junit.Assert.*;

public class MCH_LaserTargetingTest {
    @Test public void movingSnapshotResolutionKeepsTheExistingAimConeAndRangeChecks() {
        Vec3 start = Vec3.createVectorHelper(0, 64.5, 0.5), look = Vec3.createVectorHelper(1, 0, 0);
        Vec3 requested = Vec3.createVectorHelper(500, 64.5, 0.5);
        assertTrue(MCH_LaserTargeting.snapshotAimCompatible(start, requested, point(520), look));
        assertFalse(MCH_LaserTargeting.snapshotAimCompatible(start, requested, point(565), look));
        assertFalse(MCH_LaserTargeting.snapshotAimCompatible(start, requested,
                Vec3.createVectorHelper(500, 120, 0.5), look));
        assertFalse(MCH_LaserTargeting.snapshotAimCompatible(start, point(1500), point(1540), look));
        assertFalse(MCH_LaserTargeting.snapshotAimCompatible(start, point(-500), point(-520), look));
    }

    private static Vec3 point(double x) { return Vec3.createVectorHelper(x, 64.5, 0.5); }

    @Test public void loadedTerrainBeyondVanillaRayLimitWins() {
        FakeTerrain terrain = new FakeTerrain(); terrain.wall = 1000;
        assertEquals(1000, MCH_LaserTargeting.trace(terrain, point(0.5), point(1500)).xCoord, 0);
        assertTrue(terrain.blocks > 200 && terrain.blocks < 1600);
        assertEquals(0, terrain.gaps);
    }

    @Test public void cachedSurfaceBridgesAnUnloadedChunkWithoutWorldReadsThere() {
        FakeTerrain terrain = new FakeTerrain(); terrain.missingChunk = 1; terrain.cachedWall = 24;
        assertEquals(24, MCH_LaserTargeting.trace(terrain, point(0.5), point(1500)).xCoord, 0);
        assertEquals(1, terrain.gaps);
        assertTrue(terrain.blocks < 20);
        assertTrue(terrain.gapStart > 16 && terrain.gapEnd < 32);
    }

    @Test public void nearerLiveWallWinsAndClearCachedGapCanReachALiveSurface() {
        FakeTerrain near = new FakeTerrain(); near.wall = 8; near.missingChunk = 1; near.cachedWall = 24;
        assertEquals(8, MCH_LaserTargeting.trace(near, point(0.5), point(1500)).xCoord, 0);
        assertEquals(0, near.gaps);
        FakeTerrain far = new FakeTerrain(); far.missingChunk = 1; far.wall = 1000;
        assertEquals(1000, MCH_LaserTargeting.trace(far, point(0.5), point(1500)).xCoord, 0);
        assertEquals(1, far.gaps);
    }

    @Test public void unknownBeforeKnownWallCannotValidateATarget() {
        FakeTerrain terrain = new FakeTerrain(); terrain.missingChunk = 1; terrain.unknown = true; terrain.wall = 1000;
        assertNull(MCH_LaserTargeting.trace(terrain, point(0.5), point(1500)));
        assertEquals(1, terrain.gaps);
    }

    @Test public void reverseNegativeAndVerticalTraversalTerminateAtCorrectSurface() {
        FakeTerrain negative = new FakeTerrain(); negative.wall = -501;
        assertEquals(-501, MCH_LaserTargeting.trace(negative, point(-0.5), point(-1500)).xCoord, 0);
        FakeTerrain vertical = new FakeTerrain(); vertical.floor = 32;
        Vec3 hit = MCH_LaserTargeting.trace(vertical, point(0.5), Vec3.createVectorHelper(0.5, 0, 0.5));
        assertEquals(32, hit.yCoord, 0);
        FakeTerrain diagonal = new FakeTerrain(); diagonal.wall = 501;
        assertEquals(501, MCH_LaserTargeting.trace(diagonal, point(0.5),
                Vec3.createVectorHelper(900, 128, 900)).xCoord, 0);
    }

    @Test public void clearRayReturnsItsExactEndAndOversizedRaysDoNoWork() {
        FakeTerrain terrain = new FakeTerrain();
        Vec3 end = point(1500);
        assertSame(end, MCH_LaserTargeting.trace(terrain, point(0.5), end));
        int count = terrain.blocks;
        assertNull(MCH_LaserTargeting.trace(terrain, point(0.5), point(1000000)));
        assertEquals(count, terrain.blocks);
    }

    @Test public void aimRejectsImpossibleCoordinatesRangeAndDirection() {
        Vec3 start = point(0.5), look = Vec3.createVectorHelper(1, 0, 0);
        assertTrue(MCH_LaserTargeting.validAim(start, point(1500), look));
        assertTrue(MCH_LaserTargeting.validAim(start, Vec3.createVectorHelper(10, 65, 0.5), look));
        assertFalse(MCH_LaserTargeting.validAim(start, point(1537), look));
        assertFalse(MCH_LaserTargeting.validAim(start, point(-100), look));
        assertFalse(MCH_LaserTargeting.validAim(start, Vec3.createVectorHelper(1000, 1000, 1000), look));
        assertFalse(MCH_LaserTargeting.validAim(start, start, look));
        assertFalse(MCH_LaserTargeting.position(Double.NaN, 64, 0));
        assertFalse(MCH_LaserTargeting.position(0, Double.POSITIVE_INFINITY, 0));
        assertFalse(MCH_LaserTargeting.position(30000000, 64, 0));
        assertFalse(MCH_LaserTargeting.position(0, -1, 0));
    }

    private static final class FakeTerrain implements MCH_LaserTargeting.Terrain {
        int missingChunk = Integer.MIN_VALUE, wall = Integer.MIN_VALUE, floor = Integer.MIN_VALUE;
        double cachedWall = Double.NaN, gapStart, gapEnd;
        boolean unknown;
        int blocks, gaps;
        public boolean loaded(int x, int z) { return (x >> 4) != missingChunk; }
        public Vec3 blockHit(int x, int y, int z, Vec3 start, Vec3 end) {
            assertTrue("An unloaded chunk must not be read", loaded(x, z)); blocks++;
            return x == wall ? point(x) : y == floor ? Vec3.createVectorHelper(x, y, z) : null;
        }
        public Vec3 missingHit(Vec3 start, Vec3 end) {
            gaps++; gapStart = start.xCoord; gapEnd = end.xCoord;
            if (unknown) return null;
            return Double.isNaN(cachedWall) ? end : point(cachedWall);
        }
    }
}
