package mcheli.weapon;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mcheli.wrapper.W_Render;
import mcheli.render.MCH_FMURTracer;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

@SideOnly(Side.CLIENT)
public class MCH_RenderBullet extends MCH_RenderBulletBase {

    public void renderBullet(Entity entity, double posX, double posY, double posZ, float yaw, float tickTime) {
        MCH_EntityBaseBullet blt = (MCH_EntityBaseBullet) entity;
        GL11.glPushMatrix();
        try {
            GL11.glTranslated(posX, posY, posZ);
            if (blt.getInfo() != null && blt.getInfo().fmurTracerGlow && MCH_FMURTracer.render(blt, tickTime)) return;
            GL11.glRotatef(-entity.rotationYaw, 0.0F, 1.0F, 0.0F);
            GL11.glRotatef(entity.rotationPitch, 1.0F, 0.0F, 0.0F);
            this.renderModel(blt);
        } finally {
            GL11.glPopMatrix();
        }
    }

    protected ResourceLocation getEntityTexture(Entity entity) {
        return W_Render.TEX_DEFAULT;
    }
}
