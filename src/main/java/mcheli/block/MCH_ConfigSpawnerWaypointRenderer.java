package mcheli.block;

import mcheli.MCH_ServerSettings;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import org.lwjgl.opengl.GL11;

public class MCH_ConfigSpawnerWaypointRenderer extends TileEntitySpecialRenderer {

    public void renderTileEntityAt(TileEntity tile, double x, double y, double z, float partialTick) {
        if (!(tile instanceof MCH_ConfigSpawnerTileEntity)) {
            return;
        }
        if (!MCH_ServerSettings.enableDebugWaypointLabel) {
            return;
        }
        MCH_ConfigSpawnerTileEntity wp = (MCH_ConfigSpawnerTileEntity)tile;
        String label = wp.getWaypointLabel();
        if (label == null || label.isEmpty()) {
            return;
        }

        FontRenderer fr = this.func_147498_b();
        if (fr == null) {
            return;
        }

        GL11.glPushMatrix();
        GL11.glTranslated(x + 0.5D, y + 1.25D, z + 0.5D);
        GL11.glNormal3f(0.0F, 1.0F, 0.0F);
        GL11.glRotatef(-RenderManager.instance.playerViewY, 0.0F, 1.0F, 0.0F);
        GL11.glRotatef(RenderManager.instance.playerViewX, 1.0F, 0.0F, 0.0F);
        float scale = 0.016666668F * 1.2F;
        GL11.glScalef(-scale, -scale, scale);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDepthMask(false);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        int half = fr.getStringWidth(label) / 2;
        Tessellator t = Tessellator.instance;
        t.startDrawingQuads();
        t.setColorRGBA_F(0.0F, 0.0F, 0.0F, 0.35F);
        t.addVertex((double)(-half - 2), -2.0D, 0.0D);
        t.addVertex((double)(-half - 2), 9.0D, 0.0D);
        t.addVertex((double)(half + 2), 9.0D, 0.0D);
        t.addVertex((double)(half + 2), -2.0D, 0.0D);
        t.draw();

        fr.drawString(label, -half, 0, 0x20FFFF);

        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(true);
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopMatrix();
    }
}
