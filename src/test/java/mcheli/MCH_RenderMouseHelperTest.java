package mcheli;

import net.minecraft.util.MouseHelper;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MCH_RenderMouseHelperTest {
    @Test
    public void zoomKeepsSingleCountScreenMovementConsistentAcrossSensitivitySettings() {
        for (float sensitivity : new float[]{0.0F, 0.1F, 0.5F, 1.0F}) {
            double normalGain = mouseGain(sensitivity);
            for (float zoom : new float[]{1.0F, 2.0F, 4.0F, 10.0F, 40.0F, 100.0F}) {
                float adjusted = MCH_RenderMouseHelper.getZoomSensitivity(sensitivity, zoom);
                double zoomedGain = mouseGain(adjusted);
                assertTrue(zoomedGain > 0.0D);
                // A single positive or negative mouse count must survive without integer rounding.
                assertEquals(normalGain, zoomedGain * zoom, normalGain * 0.00001D);
                assertEquals(-normalGain, -zoomedGain * zoom, normalGain * 0.00001D);
            }
        }
    }

    @Test
    public void highZoomCanGoBelowVanillasMinimumSensitivityGain() {
        float adjusted = MCH_RenderMouseHelper.getZoomSensitivity(0.0F, 40.0F);
        assertTrue(adjusted < 0.0F);
        assertEquals(mouseGain(0.0F) / 40.0D, mouseGain(adjusted), 0.00000001D);
    }

    @Test
    public void unzoomedOrInvalidZoomPreservesSensitivity() {
        for (float zoom : new float[]{1.0F, 0.0F, -2.0F, Float.NaN, Float.POSITIVE_INFINITY}) {
            assertEquals(0.37F, MCH_RenderMouseHelper.getZoomSensitivity(0.37F, zoom), 0.0F);
        }
    }

    private static double mouseGain(float sensitivity) {
        float gain = sensitivity * 0.6F + 0.2F;
        return gain * gain * gain * 8.0F;
    }

    @Test
    public void vanillaCannotApplyOrDiscardMovementArrivingAfterVehicleInput() {
        QueuedMouse mouse = new QueuedMouse();
        mouse.pendingX = 12;
        mouse.pendingY = -4;
        mouse.mouseXYChange();
        assertEquals(12, mouse.deltaX);
        assertEquals(-4, mouse.deltaY);

        MCH_RenderMouseHelper renderMouse = new MCH_RenderMouseHelper(mouse);
        mouse.pendingX = -7;
        mouse.pendingY = 3;
        renderMouse.mouseXYChange();
        renderMouse.mouseXYChange();
        assertEquals(0, renderMouse.deltaX);
        assertEquals(0, renderMouse.deltaY);
        assertEquals(1, mouse.polls);

        // Restoring the original helper lets the next vehicle frame consume the late sample.
        renderMouse.delegate.mouseXYChange();
        assertEquals(-7, mouse.deltaX);
        assertEquals(3, mouse.deltaY);
        assertEquals(2, mouse.polls);
    }

    @Test
    public void focusChangesStillReachTheOriginalMouseHelper() {
        QueuedMouse mouse = new QueuedMouse();
        MCH_RenderMouseHelper renderMouse = new MCH_RenderMouseHelper(mouse);
        renderMouse.grabMouseCursor();
        renderMouse.ungrabMouseCursor();
        assertEquals(1, mouse.grabs);
        assertEquals(1, mouse.ungrabs);
        assertEquals(0, mouse.polls);
    }

    private static final class QueuedMouse extends MouseHelper {
        int pendingX;
        int pendingY;
        int polls;
        int grabs;
        int ungrabs;

        @Override
        public void mouseXYChange() {
            this.polls++;
            this.deltaX = this.pendingX;
            this.deltaY = this.pendingY;
            this.pendingX = this.pendingY = 0;
        }

        @Override
        public void grabMouseCursor() {
            this.grabs++;
        }

        @Override
        public void ungrabMouseCursor() {
            this.ungrabs++;
        }
    }
}
