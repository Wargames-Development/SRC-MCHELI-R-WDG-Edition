package mcheli.render;

import mcheli.MCH_EntityInfo;
import mcheli.MCH_Math;
import org.junit.Test;

import java.nio.FloatBuffer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MCH_AircraftSnapshotRotationTest {
    private MCH_EntityInfo snapshot(long millis, float yaw, float pitch, float roll) {
        MCH_EntityInfo sample = new MCH_EntityInfo(1, "world", "plane", "mcheli.plane.MCP_EntityPlane",
            0, 64, 0, 0, 64, 0);
        sample.lastUpdateTime = millis;
        sample.rotationYaw = yaw;
        sample.rotationPitch = pitch;
        sample.rotationRoll = roll;
        return sample;
    }

    @Test
    public void slowTurnsAdvanceBetweenFramesInsteadOfWaitingForVanillaLookThreshold() {
        for (int fps : new int[]{20, 60, 144, 240, 1000}) {
            MCH_RenderFarVehicle.SnapshotRotation pose = new MCH_RenderFarVehicle.SnapshotRotation();
            MCH_EntityInfo sample = snapshot(0, 0, 0, 0);
            pose.update(sample, 0);
            float previous = 0;
            int sampleIndex = 0;
            for (int frame = 1; frame <= fps; frame++) {
                long nanos = Math.round(frame * 1_000_000_000.0D / fps);
                int index = (int)(nanos / 100_000_000L);
                if (index != sampleIndex) {
                    sampleIndex = index;
                    sample = snapshot(index * 100L, index * 0.25F, index * -0.1F, index * 0.5F);
                }
                pose.update(sample, nanos);
                if (nanos > 150_000_000L) assertTrue("Stepped at " + fps + " FPS", pose.yaw > previous);
                assertTrue(pose.yaw <= sample.rotationYaw);
                previous = pose.yaw;
            }
            // Less than half the vanilla 5.625-degree threshold, yet visibly turning.
            assertEquals(2.25F, pose.yaw, 0.03F);
            assertEquals(-0.9F, pose.pitch, 0.02F);
            assertEquals(4.5F, pose.roll, 0.06F);
        }
    }

    @Test
    public void wrapBoundariesTakeTheShortPathOnEveryAxis() {
        MCH_RenderFarVehicle.SnapshotRotation pose = new MCH_RenderFarVehicle.SnapshotRotation();
        pose.update(snapshot(0, 179, -179, 179), 0);
        MCH_EntityInfo sample = snapshot(100, -179, 179, -179);
        pose.update(sample, 100_000_000L);
        pose.update(sample, 150_000_000L);
        assertEquals(180, pose.yaw, 0.0001F);
        assertEquals(-180, pose.pitch, 0.0001F);
        assertEquals(180, pose.roll, 0.0001F);
    }

    @Test
    public void verticalLoopDoesNotInterpolateTheEulerInversionAsAHalfTurn() {
        MCH_RenderFarVehicle.SnapshotRotation pose = new MCH_RenderFarVehicle.SnapshotRotation();
        pose.update(snapshot(0, 10, 89, 20), 0);
        MCH_EntityInfo sample = snapshot(100, -170, 89, -160);
        pose.update(sample, 100_000_000L);
        pose.update(sample, 150_000_000L);
        assertEquals(10, pose.yaw, 0.0001F);
        assertEquals(90, pose.pitch, 0.0001F);
        assertEquals(20, pose.roll, 0.0001F);
        pose.update(sample, 200_000_000L);
        // The unwrapped result must still be the received attitude in the mod's rotation order.
        FloatBuffer expected = MCH_Math.EulerToMatrix(-170, 89, -160).toFloatBuffer();
        FloatBuffer actual = MCH_Math.EulerToMatrix(pose.yaw, pose.pitch, pose.roll).toFloatBuffer();
        for (int i = 0; i < 16; i++) assertEquals(expected.get(i), actual.get(i), 0.00001F);
    }

    @Test
    public void irregularDeliveryIsContinuousAndMissingPacketsHoldAtTheLastTarget() {
        MCH_RenderFarVehicle.SnapshotRotation pose = new MCH_RenderFarVehicle.SnapshotRotation();
        pose.update(snapshot(0, 0, 0, 0), 0);
        MCH_EntityInfo sample = snapshot(100, 10, 0, 0);
        pose.update(sample, 100_000_000L);
        pose.update(sample, 140_000_000L);
        assertEquals(4, pose.yaw, 0.0001F);
        sample = snapshot(140, 20, 0, 0);
        pose.update(sample, 140_000_000L);
        assertEquals(4, pose.yaw, 0.0001F);
        pose.update(sample, 165_000_000L);
        assertEquals(12, pose.yaw, 0.0001F);
        pose.update(sample, 5_000_000_000L);
        assertEquals(20, pose.yaw, 0.0001F);
        pose.update(sample, 10_000_000_000L);
        assertEquals(20, pose.yaw, 0.0001F);
    }

    @Test
    public void clearingDiscardsOldVehicleStateAndInvalidAnglesCannotPoisonThePose() {
        MCH_RenderFarVehicle.SnapshotRotation pose = new MCH_RenderFarVehicle.SnapshotRotation();
        pose.update(snapshot(0, 20, 30, 40), 0);
        pose.update(snapshot(100, Float.NaN, 0, 0), 100_000_000L);
        pose.update(snapshot(200, 0, Float.POSITIVE_INFINITY, 0), 200_000_000L);
        assertEquals(20, pose.yaw, 0);
        assertEquals(30, pose.pitch, 0);
        pose.clear();
        pose.update(snapshot(300, -80, -10, 0), 300_000_000L);
        assertEquals(-80, pose.yaw, 0);
        assertEquals(-10, pose.pitch, 0);
        assertEquals(0, pose.roll, 0);
    }
}
