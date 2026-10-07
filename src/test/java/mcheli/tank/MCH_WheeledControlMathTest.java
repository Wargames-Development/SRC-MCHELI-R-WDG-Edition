package mcheli.tank;

import org.junit.Test;

import static org.junit.Assert.*;

public class MCH_WheeledControlMathTest {
    @Test
    public void distinctProfilesChangeAccelerationWithoutChangingEventualTopSpeed() {
        double itv = 0.0D;
        double lav = 0.0D;
        double itvLimit = 137.0D / 72.0D;
        double lavLimit = 100.0D / 72.0D;
        for (int tick = 0; tick < 80; ++tick) {
            itv = MCH_WheeledControlMath.forwardSpeed(itv, itvLimit, true, false, false, true,
                    MCH_WheeledControlMath.accelerationStep(2.5D / 400.0D, itv, itvLimit));
            lav = MCH_WheeledControlMath.forwardSpeed(lav, lavLimit, true, false, false, true,
                    MCH_WheeledControlMath.accelerationStep(1.3D / 400.0D, lav, lavLimit));
        }
        assertTrue(itv * 20.0D > 9.5D && itv * 20.0D < 10.0D);
        assertTrue(lav * 20.0D > 5.0D && lav * 20.0D < 5.2D);
        for (int tick = 80; tick < 1200; ++tick) {
            itv = MCH_WheeledControlMath.forwardSpeed(itv, itvLimit, true, false, false, true,
                    MCH_WheeledControlMath.accelerationStep(2.5D / 400.0D, itv, itvLimit));
            lav = MCH_WheeledControlMath.forwardSpeed(lav, lavLimit, true, false, false, true,
                    MCH_WheeledControlMath.accelerationStep(1.3D / 400.0D, lav, lavLimit));
        }
        assertEquals(itvLimit, itv, 0.000001D);
        assertEquals(lavLimit, lav, 0.000001D);
        assertEquals(0.0D, MCH_WheeledControlMath.forwardSpeed(0.0D, itvLimit, true, false, false, true, Double.NaN), 0.0D);
    }

    @Test
    public void profilePullTapersSymmetricallyWithoutStallingNearTheLimit() {
        assertTrue(MCH_WheeledControlMath.accelerationStep(0.01D, 0.5D, 1.0D)
                > MCH_WheeledControlMath.accelerationStep(0.01D, 0.9D, 1.0D));
        assertEquals(MCH_WheeledControlMath.accelerationStep(0.01D, 0.5D, 1.0D),
                MCH_WheeledControlMath.accelerationStep(0.01D, -0.5D, 1.0D), 0.0D);
        assertTrue(MCH_WheeledControlMath.accelerationStep(0.01D, 1.0D, 1.0D) > 0.0D);
        assertEquals(0.0D, MCH_WheeledControlMath.accelerationStep(0.01D, 0.0D, 0.0D), 0.0D);
    }

    @Test
    public void profileDoesNotWeakenBrakesOrBypassReversePermission() {
        double acceleration = 0.5D / 400.0D;
        assertEquals(MCH_WheeledControlMath.forwardSpeed(1.0D, 1.5D, true, false, true, true),
                MCH_WheeledControlMath.forwardSpeed(1.0D, 1.5D, true, false, true, true, acceleration), 0.0D);
        assertEquals(0.0D, MCH_WheeledControlMath.forwardSpeed(0.0D, 1.5D, false, true, false, false, acceleration), 0.0D);
        assertTrue(MCH_WheeledControlMath.forwardSpeed(1.0D, 1.5D, false, true, false, true, acceleration) > 0.0D);
    }

    @Test
    public void throttleTakesTwoSecondsToBuildAndOneToRelease() {
        double throttle = 0.0D;
        for (int tick = 0; tick < 20; ++tick) {
            throttle = MCH_WheeledControlMath.throttle(throttle, true);
        }
        assertEquals(0.5D, throttle, 0.000001D);
        for (int tick = 0; tick < 20; ++tick) {
            throttle = MCH_WheeledControlMath.throttle(throttle, true);
        }
        assertEquals(1.0D, throttle, 0.000001D);
        for (int tick = 0; tick < 20; ++tick) {
            throttle = MCH_WheeledControlMath.throttle(throttle, false);
        }
        assertEquals(0.0D, throttle, 0.000001D);
    }

    @Test
    public void lightVehicleAcceleratesProgressivelyWithoutReducingTopSpeed() {
        double speed = 0.0D;
        for (int tick = 0; tick < 20; ++tick) {
            speed = MCH_WheeledControlMath.forwardSpeed(speed, 1.65D, true, false, false, true);
        }
        assertTrue(speed > 0.3D && speed < 0.45D);
        for (int tick = 20; tick < 80; ++tick) {
            speed = MCH_WheeledControlMath.forwardSpeed(speed, 1.65D, true, false, false, true);
        }
        assertTrue(speed > 1.4D && speed < 1.65D);
        for (int tick = 80; tick < 100; ++tick) {
            speed = MCH_WheeledControlMath.forwardSpeed(speed, 1.65D, true, false, false, true);
        }
        assertEquals(1.65D, speed, 0.000001D);
    }

    @Test
    public void stationaryWorldAimSurvivesHullInterpolationAndYawWrap() {
        for (float tickYaw : new float[]{25.0F, 179.0F, -179.0F}) {
            float previousYaw = tickYaw - 4.0F;
            for (float frame : new float[]{0.0F, 0.25F, 0.5F, 0.75F, 1.0F}) {
                float renderedHull = previousYaw + 4.0F * frame;
                float turret = MCH_WheeledControlMath.relativeRenderYaw(0.0F, 73.0F, renderedHull);
                assertEquals(0.0F, MCH_WheeledControlMath.relativeRenderYaw(turret, renderedHull, 73.0F), 0.00001F);
            }
        }
        assertEquals(-180.0F, MCH_WheeledControlMath.relativeRenderYaw(0.0F, 0.0F, 180.0F), 0.0F);
        assertEquals(-2.0F, MCH_WheeledControlMath.relativeRenderYaw(0.0F, 179.0F, -179.0F), 0.0F);
    }

    @Test
    public void speedDefinitionsRaiseOnlyTheWheeledLimit() {
        assertEquals(2.0F, MCH_WheeledControlMath.speedLimit(2.0F, 1.0D, true), 0.0F);
        assertEquals(4.0F, MCH_WheeledControlMath.speedLimit(4.0F, 1.0D, true), 0.0F);
        assertEquals(1.8F, MCH_WheeledControlMath.speedLimit(4.0F, 1.0D, false), 0.0F);
        assertEquals(3.6F, MCH_WheeledControlMath.speedLimit(4.0F, 2.0D, false), 0.000001F);
        assertEquals(8.0F, MCH_WheeledControlMath.speedLimit(4.0F, 1000.0D, true), 0.0F);
    }

    @Test(expected = IllegalArgumentException.class)
    public void malformedSpeedDefinitionIsRejected() {
        MCH_WheeledControlMath.speedLimit(Float.NaN, 1.0D, true);
    }

    @Test
    public void configuredTwoAndFourAreBothReachable() {
        for (double limit : new double[]{0.1D, 1.8D, 2.0D, 4.0D, 8.0D}) {
            double speed = 0.0D;
            for (int tick = 0; tick < 400; ++tick) {
                speed = MCH_WheeledControlMath.forwardSpeed(speed, limit, true, false, false, true);
                assertTrue(speed <= limit);
            }
            assertEquals(limit, speed, 0.000001D);
        }
    }

    @Test
    public void directionChangeBrakesBeforeReversingAndReverseIsLimited() {
        double speed = 4.0D;
        double first = MCH_WheeledControlMath.forwardSpeed(speed, 4.0D, false, true, false, true);
        assertTrue(first > 0.0D && first < speed);
        for (int tick = 0; tick < 400; ++tick) {
            speed = MCH_WheeledControlMath.forwardSpeed(speed, 4.0D, false, true, false, true);
        }
        assertEquals(-0.6D, speed, 0.000001D);
        assertTrue(MCH_WheeledControlMath.forwardSpeed(-0.6D, 4.0D, true, false, false, true) < 0.0D);
        assertEquals(0.0D, MCH_WheeledControlMath.forwardSpeed(0.0D, 4.0D, false, true, false, false), 0.0D);
    }

    @Test
    public void brakesStopWithoutOvershootAndCoastingIsGentler() {
        double speed = 4.0D;
        assertTrue(MCH_WheeledControlMath.forwardSpeed(speed, 4.0D, false, false, false, true)
                > MCH_WheeledControlMath.forwardSpeed(speed, 4.0D, false, false, true, true));
        for (int tick = 0; tick < 400; ++tick) {
            speed = MCH_WheeledControlMath.forwardSpeed(speed, 4.0D, false, false, true, true);
            assertTrue(speed >= 0.0D);
        }
        assertEquals(0.0D, speed, 0.0D);
        assertEquals(0.0D, MCH_WheeledControlMath.forwardSpeed(0.0D, 4.0D, true, true, false, true), 0.0D);
    }

    @Test
    public void roadSpeedBrakingTakesSeveralSecondsRegardlessOfConfiguredTopSpeed() {
        for (double limit : new double[]{1.0D, 4.0D, 8.0D}) {
            double speed = 1.0D; // 20 m/s: braking should take roughly 3.3 seconds.
            int ticks = 0;
            for (; ticks < 20; ++ticks) {
                speed = MCH_WheeledControlMath.forwardSpeed(speed, limit, false, false, true, true);
            }
            assertTrue(speed > 0.65D && speed < 0.75D);
            for (; ticks < 100 && speed > 0.0D; ++ticks) {
                double previous = speed;
                speed = MCH_WheeledControlMath.forwardSpeed(speed, limit, false, false, true, true);
                assertTrue(speed >= 0.0D && speed < previous);
            }
            assertEquals(0.0D, speed, 0.0D);
            assertTrue(ticks >= 60 && ticks <= 75);
        }
        double reverse = -0.6D;
        for (int tick = 0; tick < 20; ++tick) {
            reverse = MCH_WheeledControlMath.forwardSpeed(reverse, 1.5D, false, false, true, true);
        }
        assertTrue(reverse < -0.25D && reverse > -0.35D);
        for (int tick = 20; tick < 60; ++tick) {
            reverse = MCH_WheeledControlMath.forwardSpeed(reverse, 1.5D, false, false, true, true);
            assertTrue(reverse <= 0.0D);
        }
        assertEquals(0.0D, reverse, 0.0D);
    }

    @Test
    public void throttleReleaseKeepsRollingAtLowSpeedAndEventuallyStopsInBothDirections() {
        for (double start : new double[]{0.1D, 0.25D, 1.0D, 8.0D, -0.1D, -0.6D}) {
            double coast = start;
            double braking = start;
            for (int tick = 0; tick < 20; ++tick) {
                coast = MCH_WheeledControlMath.forwardSpeed(coast, 8.0D, false, false, false, true);
                braking = MCH_WheeledControlMath.forwardSpeed(braking, 8.0D, false, false, true, true);
            }
            assertTrue(coast * start > 0.0D);
            assertTrue(Math.abs(coast) > Math.abs(start) * 0.75D);
            assertTrue(Math.abs(coast) > Math.abs(braking));
            for (int tick = 20; tick < 5000 && coast != 0.0D; ++tick) {
                double previous = coast;
                coast = MCH_WheeledControlMath.forwardSpeed(coast, 8.0D, false, false, false, true);
                assertTrue(coast * start >= 0.0D && Math.abs(coast) < Math.abs(previous));
            }
            assertEquals(0.0D, coast, 0.0D);
        }
    }

    @Test
    public void steeringBuildsGraduallyAndReturnsToCenter() {
        float steering = MCH_WheeledControlMath.steering(0.0F, false, true);
        assertTrue(steering > 0.0F && steering < 1.0F);
        for (int tick = 0; tick < 20; ++tick) {
            steering = MCH_WheeledControlMath.steering(steering, false, true);
        }
        assertEquals(1.0F, steering, 0.0F);
        for (int tick = 0; tick < 20; ++tick) {
            steering = MCH_WheeledControlMath.steering(steering, true, true);
        }
        assertEquals(0.0F, steering, 0.0F);
    }

    @Test
    public void stationaryHullCannotPivotAndReversingFlipsYaw() {
        assertEquals(0.0F, MCH_WheeledControlMath.yawStep(0.0D, 1.0F, 4.0D), 0.0F);
        assertEquals(-MCH_WheeledControlMath.yawStep(0.2D, 1.0F, 4.0D),
                MCH_WheeledControlMath.yawStep(-0.2D, 1.0F, 4.0D), 0.000001F);
        assertTrue(MCH_WheeledControlMath.yawStep(0.2D, 1.0F, 8.0D)
                < MCH_WheeledControlMath.yawStep(0.2D, 1.0F, 4.0D));
    }

    @Test
    public void fasterFullLockTurnsWidenAndRespectGripBudget() {
        assertEquals(1.0F, MCH_WheeledControlMath.effectiveSteering(0.0D, 1.0F, 4.0D), 0.0F);
        assertTrue(MCH_WheeledControlMath.effectiveSteering(4.0D, 1.0F, 4.0D)
                < MCH_WheeledControlMath.effectiveSteering(2.0D, 1.0F, 4.0D));
        float fastSteering = MCH_WheeledControlMath.effectiveSteering(4.0D, 1.0F, 4.0D);
        assertEquals(0.0F, MCH_WheeledControlMath.steering(fastSteering, false, false), 0.0F);
        double previousRadius = 0.0D;
        for (double speed : new double[]{0.1D, 0.2D, 0.5D, 1.0D, 2.0D, 4.0D, 8.0D}) {
            double yaw = Math.toRadians(MCH_WheeledControlMath.yawStep(speed, 1.0F, 4.0D));
            double radius = speed / yaw;
            assertTrue(radius + 0.000001D >= previousRadius);
            assertTrue(speed * yaw * 400.0D <= 5.0001D);
            previousRadius = radius;
        }
        assertEquals(0.0F, MCH_WheeledControlMath.yawStep(Double.NaN, 1.0F, 4.0D), 0.0F);
        assertEquals(0.0D, MCH_WheeledControlMath.forwardSpeed(1.0D, Double.POSITIVE_INFINITY,
                true, false, false, true), 0.0D);
    }
}
