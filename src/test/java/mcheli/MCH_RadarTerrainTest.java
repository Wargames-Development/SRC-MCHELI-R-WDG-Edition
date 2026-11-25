package mcheli;

import net.minecraft.util.AxisAlignedBB;
import org.junit.Test;

import static org.junit.Assert.*;

public class MCH_RadarTerrainTest {
    private final AxisAlignedBB target = AxisAlignedBB.getBoundingBox(10, 0, -1, 12, 4, 1);

    @Test public void anUnloadedRidgeCanOnlyHideTheContactWhenItsCoverIsConfirmed() {
        AxisAlignedBB distant = AxisAlignedBB.getBoundingBox(2498, 60, -3, 2502, 65, 3);
        int[] ridgeState = {2};
        MCH_RadarTerrain.Terrain terrain = (x, y, z) -> x == 800 ? ridgeState[0] : 0;
        assertFalse(MCH_RadarTerrain.isBoxBlocked(terrain, 0.5, 64.5, 0.5, distant));
        ridgeState[0] = 1;
        assertTrue(MCH_RadarTerrain.isBoxBlocked(terrain, 0.5, 64.5, 0.5, distant));
        ridgeState[0] = 0;
        assertFalse(MCH_RadarTerrain.isBoxBlocked(terrain, 0.5, 64.5, 0.5, distant));
    }

    @Test public void broadTerrainHidesTheWholeVehicle() {
        assertTrue(MCH_RadarTerrain.isBoxBlocked((x, y, z) -> x == 5 ? 1 : 0, 0.5, 1.5, 0.5, target));
    }

    @Test public void exposedRoofRemainsDetectableWithOriginBehindCover() {
        MCH_RadarTerrain.Terrain wall = (x, y, z) -> x == 5 && y < 2 ? 1 : 0;
        assertFalse(MCH_RadarTerrain.isBoxBlocked(wall, 0.5, 1.5, 0.5, target));
    }

    @Test public void exposedSideRemainsDetectableWithCenterBehindCover() {
        MCH_RadarTerrain.Terrain wall = (x, y, z) -> x == 5 && z >= -1 && z <= 1 ? 1 : 0;
        AxisAlignedBB wide = AxisAlignedBB.getBoundingBox(10, 0, -6, 12, 4, 6);
        assertFalse(MCH_RadarTerrain.isBoxBlocked(wall, 0.5, 1.5, 0.5, wide));
    }

    @Test public void anOpeningBetweenTheOldSampleRaysKeepsTheContactVisible() {
        AxisAlignedBB wide = AxisAlignedBB.getBoundingBox(10, 0, -10, 12, 8, 10);
        int[] opening = {0};
        MCH_RadarTerrain.Terrain wall = (x, y, z) -> x == 8 ? (y == 2 && z == 3 ? opening[0] : 1) : 0;
        assertFalse(MCH_RadarTerrain.isBoxBlocked(wall, 0.5, 1.5, 0.5, wide));
        opening[0] = 2;
        assertFalse(MCH_RadarTerrain.isBoxBlocked(wall, 0.5, 1.5, 0.5, wide));
        opening[0] = 1;
        assertTrue(MCH_RadarTerrain.isBoxBlocked(wall, 0.5, 1.5, 0.5, wide));
    }

    @Test public void negativeCoordinatesWorkInBothDirections() {
        MCH_RadarTerrain.Terrain wall = (x, y, z) -> x == -6 ? 1 : 0;
        assertTrue(MCH_RadarTerrain.isBoxBlocked(wall, -0.5, 3, 0.5, AxisAlignedBB.getBoundingBox(-12, 2, 0, -10, 4, 1)));
        assertTrue(MCH_RadarTerrain.isBoxBlocked(wall, -12, 3, 0.5, AxisAlignedBB.getBoundingBox(-2, 2, 0, -0.5, 4, 1)));
    }

    @Test public void diagonalVerticalAndZPathsFindCover() {
        AxisAlignedBB diagonal = AxisAlignedBB.getBoundingBox(10.25, 3, 10.25, 10.75, 4, 10.75);
        assertTrue(MCH_RadarTerrain.isBoxBlocked((x, y, z) -> x == 5 && z == 5 ? 1 : 0, 0.5, 3.5, 0.5, diagonal));
        AxisAlignedBB above = AxisAlignedBB.getBoundingBox(0, 10, 0, 1, 12, 1);
        assertTrue(MCH_RadarTerrain.isBoxBlocked((x, y, z) -> y == 5 ? 1 : 0, 0.5, 0.5, 0.5, above));
        AxisAlignedBB behind = AxisAlignedBB.getBoundingBox(0, 3, -12, 1, 4, -10);
        assertTrue(MCH_RadarTerrain.isBoxBlocked((x, y, z) -> z == -5 ? 1 : 0, 0.5, 3.5, 0.5, behind));
    }

    @Test public void unknownTerrainAloneDoesNotHideButKnownTerrainAfterGapDoes() {
        assertFalse(MCH_RadarTerrain.isBoxBlocked((x, y, z) -> 2, 0.5, 1.5, 0.5, target));
        assertTrue(MCH_RadarTerrain.isBoxBlocked((x, y, z) -> x == 5 ? 1 : 2, 0.5, 1.5, 0.5, target));
    }

    @Test public void emitterCellAndTerrainAtOrBeyondTargetAreNotCover() {
        assertFalse(MCH_RadarTerrain.isBoxBlocked((x, y, z) -> x == 0 || x >= 10 ? 1 : 0, 0.5, 1.5, 0.5, target));
    }

    @Test public void clearPathAndInvalidOrExcessiveDistancesDoNotInventCover() {
        assertFalse(MCH_RadarTerrain.isBoxBlocked((x, y, z) -> 0, 0.5, 1.5, 0.5, target));
        assertFalse(MCH_RadarTerrain.isBoxBlocked((x, y, z) -> 1, Double.NaN, 0, 0, target));
        AxisAlignedBB distant = AxisAlignedBB.getBoundingBox(1_000_000, 0, 0, 1_000_001, 1, 1);
        int[] calls = {0};
        assertFalse(MCH_RadarTerrain.isBoxBlocked((x, y, z) -> { calls[0]++; return 0; }, 0.5, 0.5, 0.5, distant));
        assertEquals(8192, calls[0]);
    }
}
