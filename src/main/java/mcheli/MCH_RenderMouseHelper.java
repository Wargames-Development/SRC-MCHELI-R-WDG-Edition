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
