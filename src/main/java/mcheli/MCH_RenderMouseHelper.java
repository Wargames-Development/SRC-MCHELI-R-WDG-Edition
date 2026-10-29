package mcheli;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.util.MouseHelper;

/** Keeps vanilla from consuming a second mouse sample after vehicle input. */
@SideOnly(Side.CLIENT)
final class MCH_RenderMouseHelper extends MouseHelper {
    final MouseHelper delegate;

    MCH_RenderMouseHelper(MouseHelper delegate) {
        this.delegate = delegate;
    }

    static float getZoomSensitivity(float sensitivity, float zoom) {
        if (!Float.isFinite(sensitivity) || !Float.isFinite(zoom) || zoom <= 1.0F) {
            return sensitivity;
        }
        // Scale angular gain, not the setting: vanilla cubes (sensitivity * 0.6 + 0.2).
        // Negative settings are intentional at high zoom; clamping restores the coarse minimum step.
        double gain = sensitivity * 0.6D + 0.2D;
        return (float) ((gain / Math.cbrt(zoom) - 0.2D) / 0.6D);
    }

    @Override
    public void mouseXYChange() {
        // Do not drain LWJGL: movement arriving during rendering belongs to the next frame.
        this.deltaX = 0;
        this.deltaY = 0;
    }

    @Override
    public void grabMouseCursor() {
        this.delegate.grabMouseCursor();
    }

    @Override
    public void ungrabMouseCursor() {
        this.delegate.ungrabMouseCursor();
    }
}
