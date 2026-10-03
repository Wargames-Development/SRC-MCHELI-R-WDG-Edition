package mcheli;

import net.minecraft.util.AxisAlignedBB;
import org.junit.Test;

import static org.junit.Assert.*;

public class MCH_RadarTerrainTest {
    private final AxisAlignedBB target = AxisAlignedBB.getBoundingBox(10, 0, -1, 12, 4, 1);

    @Test public void broadTerrainHidesTheWholeVehicle() {
        assertTrue(MCH_RadarTerrain.isBoxBlocked((x, y, z) -> x == 5 ? 1 : 0, 0.5, 1.5, 0.5, target));
    }

    @Test public void exposedRoofRemainsDetectableWithOriginBehindCover() {
        MCH_RadarTerrain.Terrain wall = (x, y, z) -> x == 5 && y < 2 ? 1 : 0;
        assertTrue(MCH_RadarTerrain.isRayBlocked(wall, 0.5, 1.5, 0.5, 11, 0.1, 0));
        assertFalse(MCH_RadarTerrain.isBoxBlocked(wall, 0.5, 1.5, 0.5, target));
    }

    @Test public void exposedSideRemainsDetectableWithCenterBehindCover() {
        MCH_RadarTerrain.Terrain wall = (x, y, z) -> x == 5 && z >= -1 && z <= 1 ? 1 : 0;
        AxisAlignedBB wide = AxisAlignedBB.getBoundingBox(10, 0, -6, 12, 4, 6);
        assertTrue(MCH_RadarTerrain.isRayBlocked(wall, 0.5, 1.5, 0.5, 11, 2, 0));
        assertFalse(MCH_RadarTerrain.isBoxBlocked(wall, 0.5, 1.5, 0.5, wide));
    }

    @Test public void terrainFartherThanVanillaRayLimitStillBlocks() {
        assertTrue(MCH_RadarTerrain.isRayBlocked((x, y, z) -> x == 1000 ? 1 : 0, 0.5, 64.5, 0.5, 4096, 64.5, 0.5));
    }

    @Test public void negativeCoordinatesAndReverseDirectionTraverseCorrectCells() {
        assertTrue(MCH_RadarTerrain.isRayBlocked((x, y, z) -> x == -6 ? 1 : 0, -0.5, 3, 0.5, -12, 3, 0.5));
        assertTrue(MCH_RadarTerrain.isRayBlocked((x, y, z) -> x == -6 ? 1 : 0, -12, 3, 0.5, -0.5, 3, 0.5));
    }

    @Test public void diagonalAndVerticalRaysVisitObstructions() {
        assertTrue(MCH_RadarTerrain.isRayBlocked((x, y, z) -> x == 5 && z == 5 ? 1 : 0, 0.5, 3.5, 0.5, 10.5, 3.5, 10.5));
        assertTrue(MCH_RadarTerrain.isRayBlocked((x, y, z) -> y == 5 ? 1 : 0, 0.5, 0.5, 0.5, 0.5, 10, 0.5));
    }

    @Test public void unknownTerrainAloneDoesNotHideButKnownTerrainAfterGapDoes() {
        assertFalse(MCH_RadarTerrain.isBoxBlocked((x, y, z) -> 2, 0.5, 1.5, 0.5, target));
        assertTrue(MCH_RadarTerrain.isBoxBlocked((x, y, z) -> x == 5 ? 1 : 2, 0.5, 1.5, 0.5, target));
    }

    @Test public void clearPathAndInvalidOrExcessiveRaysDoNotInventCover() {
        assertFalse(MCH_RadarTerrain.isBoxBlocked((x, y, z) -> 0, 0.5, 1.5, 0.5, target));
        assertFalse(MCH_RadarTerrain.isRayBlocked((x, y, z) -> 1, Double.NaN, 0, 0, 10, 0, 0));
        int[] calls = {0};
        assertFalse(MCH_RadarTerrain.isRayBlocked((x, y, z) -> { calls[0]++; return 0; }, 0.5, 0.5, 0.5, 1000000, 0.5, 0.5));
        assertEquals(8192, calls[0]);
    }
}
