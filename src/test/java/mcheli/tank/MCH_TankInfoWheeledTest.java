package mcheli.tank;

import org.junit.Test;

import static org.junit.Assert.*;

public class MCH_TankInfoWheeledTest {
    @Test
    public void profilesSeparateAccelerationAndSteeringFromSpeedAndTerrainProbes() {
        MCH_TankInfo info = new MCH_TankInfo("profile_test");
        info.loadItemData("WheeledAcceleration", "2.5");
        info.loadItemData("WheeledTurnRadius", "3.5");
        info.loadItemData("Speed", "1.902778");
        info.loadItemData("SetWheelPos", "0.68,-0.24,1.2,-1.1");
        assertEquals(2.5D / 400.0D, info.getWheeledAcceleration(), 0.0000001D);
        assertEquals(2.3D, info.getWheelbase(), 0.000001D);
        double yaw = Math.toRadians(MCH_WheeledControlMath.yawStep(0.05D, 1.0F, info.getSteeringWheelbase()));
        assertEquals(3.5D, 0.05D / yaw, 0.000001D);
        info.loadItemData("Speed", "0.5");
        assertEquals(2.5D / 400.0D, info.getWheeledAcceleration(), 0.0000001D);
    }

    @Test
    public void profileOverridesResetWhenRemovedDuringReload() {
        MCH_TankInfo info = new MCH_TankInfo("profile_reload_test");
        info.loadItemData("WheeledAcceleration", "1.0");
        info.loadItemData("WheeledTurnRadius", "10.0");
        info.preReload();
        info.loadItemData("Speed", "1.0");
        assertEquals(0.012D + 1.0D / 240.0D, info.getWheeledAcceleration(), 0.0000001D);
        assertEquals(info.getWheelbase(), info.getSteeringWheelbase(), 0.0D);
    }

    @Test
    public void invalidProfileValuesAreRejectedAtTheDefinitionBoundary() {
        for (String key : new String[]{"WheeledAcceleration", "WheeledTurnRadius"}) {
            for (String value : new String[]{"NaN", "Infinity", "-1", "0", "100", "bad"}) {
                MCH_TankInfo info = new MCH_TankInfo("invalid_profile_test");
                try {
                    info.loadItemData(key, value);
                    fail("Accepted " + key + "=" + value);
                } catch (IllegalArgumentException expected) {
                    // Malformed profiles must fail loading instead of poisoning motion.
                }
            }
        }
    }

    @Test
    public void handlingAndSpeedDoNotDependOnDefinitionFieldOrder() {
        for (boolean speedFirst : new boolean[]{false, true}) {
            MCH_TankInfo info = new MCH_TankInfo("wheeled_test");
            if (speedFirst) info.loadItemData("speed", "4.0");
            info.loadItemData("weighttype", "car");
            if (!speedFirst) info.loadItemData("speed", "4.0");
            assertTrue(info.isWheeledHandling());
            assertEquals(4.0F, info.speed, 0.0F);
            assertEquals(4.0F, MCH_WheeledControlMath.speedLimit(info.speed, 1.0D, info.isWheeledHandling()), 0.0F);
            info.loadItemData("wheeledhandling", "false");
            assertFalse(info.isWheeledHandling());
            assertEquals(1.8F, MCH_WheeledControlMath.speedLimit(info.speed, 1.0D, info.isWheeledHandling()), 0.0F);
        }
        MCH_TankInfo tracked = new MCH_TankInfo("tracked_test");
        tracked.loadItemData("weighttype", "tank");
        assertFalse(tracked.isWheeledHandling());
        tracked.loadItemData("wheeledhandling", "true");
        assertTrue(tracked.isWheeledHandling());
    }

    @Test
    public void wheelbaseUsesExistingAxlesWithSafeFallback() {
        MCH_TankInfo info = new MCH_TankInfo("wheelbase_test");
        assertEquals(4.0D, info.getWheelbase(), 0.0D);
        info.loadItemData("SetWheelPos", "1.5,-0.24,3,-3");
        assertEquals(6.0D, info.getWheelbase(), 0.0D);
        info.wheels.clear();
        assertEquals(4.0D, info.getWheelbase(), 0.0D);
    }
}
