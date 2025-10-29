package mcheli.weapon;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mcheli.MCH_Lib;
import mcheli.wrapper.W_Render;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;
import org.lwjgl.opengl.GL11;

@SideOnly(Side.CLIENT)
public class MCH_RenderAAMissile extends MCH_RenderBulletBase {

    private int tickCounter = 0;

    public MCH_RenderAAMissile() {
        super.shadowSize = 0.5F;
    }

    @Override
    public void doRender(Entity entity, double posX, double posY, double posZ,
                         float yaw, float partialTicks) {
        super.doRender(entity, posX, posY, posZ, yaw, partialTicks);
        if (entity instanceof MCH_EntityBaseBullet) {
            MCH_EntityBaseBullet aam = (MCH_EntityBaseBullet) entity;
            if (aam.getInfo() != null && aam.getInfo().enableExhaustFlare) {
                // Interpolate motion vector for smoother rendering
                double mx = aam.prevMotionX + (aam.motionX - aam.prevMotionX) * partialTicks;
                double my = aam.prevMotionY + (aam.motionY - aam.prevMotionY) * partialTicks;
                double mz = aam.prevMotionZ + (aam.motionZ - aam.prevMotionZ) * partialTicks;
                double motionLen = Math.sqrt(mx * mx + my * my + mz * mz);

                GL11.glPushMatrix();
                // Move to missile position
                GL11.glTranslated(posX, posY, posZ);
                // Offset slightly backward so the exhaust appears at the tail of the model
                if (motionLen > 0.0001D) {
                    double offset = 0.8D;
                    GL11.glTranslated(-mx / motionLen * offset,
                        -my / motionLen * offset,
                        -mz / motionLen * offset);
                }
                // Billboard: Rotate by (180° − playerViewY) and −playerViewX so the quad faces the player
                float viewYaw = this.renderManager.playerViewY;
                float viewPitch = this.renderManager.playerViewX;
                GL11.glRotatef(180.0F - viewYaw, 0.0F, 1.0F, 0.0F);
                GL11.glRotatef(-viewPitch, 1.0F, 0.0F, 0.0F);

                ResourceLocation res = (tickCounter / 5) % 2 == 0 ? MCH_RenderBulletBase.TEX_FLAME : MCH_RenderBulletBase.TEX_FLAME_1;
                this.bindTexture(res);

                GL11.glAlphaFunc(GL11.GL_GREATER, 0.001F);
                GL11.glEnable(GL11.GL_BLEND);
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                GL11.glDepthMask(false);

                Tessellator tess = Tessellator.instance;
                float size = 4F;
                tess.startDrawingQuads();
                tess.setBrightness(15728880);
                tess.setNormal(0.0F, 1.0F, 0.0F);
                tess.addVertexWithUV(-size, -size, 0.0D, 0.0F, 1.0F);
                tess.addVertexWithUV(size, -size, 0.0D, 1.0F, 1.0F);
                tess.addVertexWithUV(size, size, 0.0D, 1.0F, 0.0F);
                tess.addVertexWithUV(-size, size, 0.0D, 0.0F, 0.0F);
                tess.draw();

                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                GL11.glDisable(GL11.GL_BLEND);
                GL11.glDepthMask(true);
                GL11.glPopMatrix();

                tickCounter++;
            }
        }
    }

    public void renderBullet(Entity entity, double posX, double posY, double posZ, float par8, float par9) {
        if (entity instanceof MCH_EntityAAMissile) {
            MCH_EntityAAMissile aam = (MCH_EntityAAMissile) entity;
            double mx = aam.prevMotionX + (aam.motionX - aam.prevMotionX) * (double) par9;
            double my = aam.prevMotionY + (aam.motionY - aam.prevMotionY) * (double) par9;
            double mz = aam.prevMotionZ + (aam.motionZ - aam.prevMotionZ) * (double) par9;
            GL11.glPushMatrix();
            GL11.glTranslated(posX, posY, posZ);
            Vec3 v = MCH_Lib.getYawPitchFromVec(mx, my, mz);
            GL11.glRotatef((float) v.yCoord - 90.0F, 0.0F, -1.0F, 0.0F);
            GL11.glRotatef((float) v.zCoord, -1.0F, 0.0F, 0.0F);
            this.renderModel(aam);
            GL11.glPopMatrix();
        }
    }

    protected ResourceLocation getEntityTexture(Entity entity) {
        return W_Render.TEX_DEFAULT;
    }
}
