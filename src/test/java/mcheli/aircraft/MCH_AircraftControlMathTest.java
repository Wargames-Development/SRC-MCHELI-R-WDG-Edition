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
}
