package mcheli.tank;

import org.junit.Test;

import static org.junit.Assert.*;

public class MCH_WheeledPositionTest {
    @Test
    public void regularUpdatesHaveNoAdditionalSpeedDependentBarrelLag() {
        for (double velocity : new double[]{0.0D, 0.2D, 1.388889D, 1.902778D, -1.9D, 4.0D}) {
            double position = 100.0D;
            for (int tick = 1; tick <= 120; ++tick) {
                double serverPosition = 100.0D + tick * velocity;
                position = MCH_WheeledControlMath.clientPosition(position, serverPosition, velocity, 0);
                assertEquals(serverPosition, position, 0.0000001D);
            }
        }
    }

    @Test
    public void missingPacketsAllowOnlyTwoTicksOfPrediction() {
        double position = MCH_WheeledControlMath.clientPosition(10.0D, 12.0D, 2.0D, 0);
        assertEquals(12.0D, position, 0.0D);
        position = MCH_WheeledControlMath.clientPosition(position, 12.0D, 2.0D, 1);
        assertEquals(14.0D, position, 0.0D);
        position = MCH_WheeledControlMath.clientPosition(position, 12.0D, 2.0D, 2);
        assertEquals(16.0D, position, 0.0D);
        for (int age = 3; age < 20; ++age) {
            position = MCH_WheeledControlMath.clientPosition(position, 12.0D, 2.0D, age);
            assertEquals(16.0D, position, 0.0D);
        }
    }

    @Test
    public void latePacketsCannotReverseMotionButFreshStoppedOrReverseStateCanCorrectIt() {
        assertEquals(10.0D, MCH_WheeledControlMath.clientPosition(10.0D, 4.0D, 2.0D, 0), 0.0D);
        assertEquals(10.0D, MCH_WheeledControlMath.clientPosition(10.0D, 16.0D, -2.0D, 0), 0.0D);
        assertEquals(12.0D, MCH_WheeledControlMath.clientPosition(10.0D, 12.0D, 2.0D, 0), 0.0D);
        assertEquals(9.0D, MCH_WheeledControlMath.clientPosition(10.0D, 9.0D, -1.0D, 0), 0.0D);
        double position = 10.0D;
        for (int tick = 0; tick < 60; ++tick) {
            double next = MCH_WheeledControlMath.clientPosition(position, 8.0D, 0.0D, tick);
            assertTrue(next >= 8.0D && next <= position);
            if (tick == 0) assertTrue(next > 8.0D);
            position = next;
        }
        assertEquals(8.0D, position, 0.00000001D);
    }

    @Test
    public void delayedThreeTickPacketCadenceDoesNotCreateBackwardCorrectionsOrExtraLag() {
        for (double velocity : new double[]{1.9D, -1.9D}) {
            double position = 0.0D;
            double receivedPosition = 0.0D;
            int age = 0;
            for (int tick = 1; tick <= 120; ++tick) {
                if (tick % 3 == 1) {
                    receivedPosition = (tick - 1) * velocity;
                    age = 0;
                }
                double next = MCH_WheeledControlMath.clientPosition(position, receivedPosition, velocity, age);
                assertTrue((next - position) * velocity >= -0.0000001D);
                if (tick > 60) assertEquals((tick - 1) * velocity, next, 0.0000001D);
                position = next;
                ++age;
            }
        }
    }

    @Test
    public void slowerServerCadenceStaysBoundedInsteadOfDriftingAheadForever() {
        double position = 0.0D;
        double receivedPosition = 0.0D;
        int age = -1;
        for (int tick = 1; tick <= 600; ++tick) {
            if (tick % 2 == 0) {
                receivedPosition = tick / 2 * 1.9D;
                age = 0;
            }
            double next = MCH_WheeledControlMath.clientPosition(position, receivedPosition, 1.9D, age);
            assertTrue(next >= position);
            assertTrue(Math.abs(next - receivedPosition) < 3.1D * 1.9D);
            position = next;
            if (age >= 0) ++age;
        }
    }

    @Test
    public void initialStateAndInvalidTargetsHoldWhileRealTeleportsAreAccepted() {
        assertEquals(4.0D, MCH_WheeledControlMath.clientPosition(4.0D, 5.0D, 1.0D, -1), 0.0D);
        assertEquals(4.0D, MCH_WheeledControlMath.clientPosition(4.0D, Double.NaN, 1.0D, 0), 0.0D);
        assertEquals(4.0D, MCH_WheeledControlMath.clientPosition(4.0D, 5.0D, Double.POSITIVE_INFINITY, 0), 0.0D);
        assertEquals(100.0D, MCH_WheeledControlMath.clientPosition(0.0D, 100.0D, 2.0D, 0), 0.0D);
    }
}
