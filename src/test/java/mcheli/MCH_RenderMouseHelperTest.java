package mcheli;

import net.minecraft.util.MouseHelper;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MCH_RenderMouseHelperTest {
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
