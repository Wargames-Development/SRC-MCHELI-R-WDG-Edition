package mcheli.aircraft;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MCH_AircraftControlMathTest {
    @Test
    public void saturatedControlUsesTheSameMobilityBudgetAtDifferentFrameRates() {
        for (int fps : new int[]{20, 60, 144, 240, 1000}) {
            for (float mobility : new float[]{0.0F, 0.1F, 0.5F, 1.0F, 2.0F}) {
                for (float factor : new float[]{1.0F, 0.8F, 0.24F}) {
                    float rotation = 0.0F;
                    for (int frame = 0; frame < fps; frame++) {
                        rotation += MCH_AircraftControlMath.rotationStep(10000.0F, 40.0D * mobility, factor, 20.0F / fps);
                    }
                    assertEquals(48.0F * mobility * factor, rotation, 0.002F);
                }
            }
        }
    }

    @Test
    public void changingFrameTimesDoNotBorrowTimeFromEarlierFrames() {
        float[] deltas = {1.0F, 0.01F, 0.02F, 0.4F, 0.005F, 0.9F};
        float rotation = 0.0F;
        float ticks = 0.0F;
        for (float delta : deltas) {
            float step = MCH_AircraftControlMath.rotationStep(10000.0F, 10.0D, 1.0F, delta);
            assertTrue(step <= 0.6F * delta + 0.000001F);
            rotation += step;
            ticks += delta;
        }
        assertEquals(0.6F * ticks, rotation, 0.000001F);
    }

    @Test
    public void keyboardAndMouseShareOneRollBudget() {
        float mobility = 0.25F;
        assertEquals(0.15F, MCH_AircraftControlMath.rotationStep(100.0F + 20.0F * mobility,
                40.0D * mobility, 1.0F, 0.25F), 0.000001F);
        assertEquals(-0.15F, MCH_AircraftControlMath.rotationStep(-100.0F - 20.0F * mobility,
                40.0D * mobility, 1.0F, 0.25F), 0.000001F);
        assertEquals(0.0F, MCH_AircraftControlMath.rotationStep(100.0F, 0.0D, 1.0F, 1.0F), 0.0F);
    }

    @Test
    public void smallInputsAndAxisFactorsRemainProportional() {
        assertEquals(0.024F, MCH_AircraftControlMath.rotationStep(2.0F, 40.0D, 0.8F, 0.25F), 0.000001F);
        assertEquals(-0.024F, MCH_AircraftControlMath.rotationStep(-2.0F, 40.0D, 0.8F, 0.25F), 0.000001F);
        assertEquals(0.0F, MCH_AircraftControlMath.rotationStep(2.0F, 40.0D, 0.0F, 0.25F), 0.0F);
    }

    @Test
    public void highFpsTimingIsNeverInflated() {
        assertEquals(0.001F, MCH_AircraftControlMath.tickDelta(0.001F), 0.0F);
        assertEquals(0.0F, MCH_AircraftControlMath.tickDelta(0.0F), 0.0F);
        assertEquals(1.0F, MCH_AircraftControlMath.tickDelta(1.0F), 0.0F);
        assertEquals(0.0F, MCH_AircraftControlMath.tickDelta(-1.0F), 0.0F);
        assertEquals(1.0F, MCH_AircraftControlMath.tickDelta(10.0F), 0.0F);
        assertEquals(0.0F, MCH_AircraftControlMath.tickDelta(Float.NaN), 0.0F);
        assertEquals(0.0F, MCH_AircraftControlMath.tickDelta(Float.POSITIVE_INFINITY), 0.0F);
    }

    @Test
    public void nonFiniteMouseInputCannotCorruptRotation() {
        assertEquals(0.0F, MCH_AircraftControlMath.rotationStep(Float.NaN, 40.0D, 1.0F, 1.0F), 0.0F);
        assertEquals(0.0F, MCH_AircraftControlMath.rotationStep(Float.NEGATIVE_INFINITY, 40.0D, 1.0F, 1.0F), 0.0F);
    }

    @Test
    public void stabilizationDependsOnTimeInsteadOfFrameCount() {
        for (int fps : new int[]{20, 60, 144, 240, 1000}) {
            for (float retention : new float[]{0.85F, 0.857375F, 0.9F, 0.93F, 0.95F, 0.96F, 0.97F}) {
                float bank = 30.0F;
                for (int frame = 0; frame < fps; frame++) {
                    bank *= MCH_AircraftControlMath.retention(retention, 20.0F / fps);
                }
                assertEquals(30.0F * (float) Math.pow(retention, 20.0D), bank, 0.001F);
            }
        }
        assertEquals(1.0F, MCH_AircraftControlMath.retention(0.9F, 0.0F), 0.0F);
    }

    @Test
    public void reachablePlaneMouseInputScalesWithMobilityAtEveryFrameRate() {
        for (int fps : new int[]{20, 60, 144, 240, 1000}) {
            for (float mobility : new float[]{0.0F, 0.1F, 0.5F, 1.0F, 2.0F, 4.0F}) {
                float delta = 20.0F / fps;
                for (float mouse : new float[]{-40.0F, -10.0F, 10.0F, 40.0F}) {
                    for (boolean flightSim : new boolean[]{false, true}) {
                        float rotation = 0.0F;
                        for (int frame = 0; frame < fps; frame++) {
                            rotation += planeRollStep(mouse, 0.0F, mobility, flightSim, false, 0.8F, delta);
                        }
                        float expected = flightSim ? (Math.abs(mouse) == 40.0F ? 38.4F : 19.2F)
                                : Math.abs(mouse) * 0.48F;
                        assertEquals(Math.signum(mouse) * expected * mobility, rotation, 0.004F);
                    }
                }
            }
        }
    }

    @Test
    public void planeKeyboardRateIsRestoredWithoutRemovingTheCombinedInputCap() {
        assertEquals(3.0F, planeRollStep(0, 2, 1, false, false, 0.8F, 1), 0.000001F);
        assertEquals(6.0F, planeRollStep(0, 2, 2, false, false, 0.8F, 1), 0.000001F);
        assertEquals(-3.0F, planeRollStep(0, -2, 1, false, false, 0.8F, 1), 0.000001F);
        assertEquals(3.84F, planeRollStep(40, 2, 1, false, false, 0.8F, 1), 0.000001F);
        assertEquals(-3.84F, planeRollStep(-40, -2, 1, false, false, 0.8F, 1), 0.000001F);
        assertEquals(0.0F, planeRollStep(40, 2, 0, false, false, 0.8F, 1), 0.0F);
    }

    @Test
    public void heldAndReleasedPlaneKeysRemainTimeBased() {
        for (int fps : new int[]{20, 60, 144, 240, 1000}) {
            for (float mobility : new float[]{0.5F, 1.0F, 2.0F}) {
                float delta = 20.0F / fps;
                float key = 0.0F;
                float lastSecond = 0.0F;
                for (int frame = 0; frame < fps * 10; frame++) {
                    key += 0.2F * delta;
                    float step = planeRollStep(0, key, mobility, false, false, 0.8F, delta);
                    if (frame >= fps * 9) lastSecond += step;
                    key *= MCH_AircraftControlMath.retention(0.9F, delta);
                }
                // Existing exponential key damping has a small integration difference
                // across FPS, but must retain approximately the old 60 degree/s rate.
                assertEquals(60.0F * mobility, lastSecond, 3.1F * mobility);
                for (int frame = 0; frame < fps * 3; frame++) {
                    key *= MCH_AircraftControlMath.retention(0.9F, delta);
                }
                assertTrue(Math.abs(planeRollStep(0, key, mobility, false, false, 0.8F, 1))
                        < 0.006F * mobility);
            }
        }
    }

    @Test
    public void planeRollUsesActualElapsedTimeThroughUnevenFrames() {
        float[] deltas = {1.0F, 0.01F, 0.02F, 0.4F, 0.005F, 0.9F, 0.0F};
        float ticks = 0.0F;
        float rotation = 0.0F;
        for (float delta : deltas) {
            rotation += planeRollStep(40, 0, 2, false, false, 0.8F, delta);
            ticks += delta;
        }
        assertEquals(1.92F * ticks, rotation, 0.000001F);
    }

    @Test
    public void flightSimKeysRemainYawControlsAndVtolRetainsItsRollFactor() {
        assertEquals(planeRollStep(10, 0, 2, true, false, 0.8F, 1),
                planeRollStep(10, 2, 2, true, false, 0.8F, 1), 0.0F);
        assertEquals(0.288F, planeRollStep(40, 0, 1, false, true, 0.24F, 0.5F), 0.000001F);
        assertEquals(0.576F, planeRollStep(40, 0, 2, false, true, 0.24F, 0.5F), 0.000001F);
    }

    private static float planeRollStep(float mouse, float key, float mobility,
            boolean flightSim, boolean vtol, float factor, float delta) {
        return MCH_AircraftControlMath.rotationStep(
                MCH_AircraftControlMath.planeRollInput(mouse, key, mobility, flightSim, vtol),
                MCH_AircraftControlMath.planeRollLimit(mobility, flightSim), factor, delta);
    }
}
