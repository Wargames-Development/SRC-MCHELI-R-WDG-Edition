package mcheli.weapon;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.aircraft.MCH_EntitySeat;
import mcheli.uav.MCH_EntityUavStation;
import mcheli.wrapper.W_Render;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

@SideOnly(Side.CLIENT)
public class MCH_RenderTvMissile extends MCH_RenderBulletBase {

    private int tickCounter = 0;

    public MCH_RenderTvMissile() {
        super.shadowSize = 0.5F;
    }

    @Override
    public void doRender(Entity entity, double posX, double posY, double posZ,
                         float yaw, float partialTicks) {
        super.doRender(entity, posX, posY, posZ, yaw, partialTicks);
        if (entity instanceof MCH_EntityBaseBullet) {
            MCH_EntityBaseBullet aam = (MCH_EntityBaseBullet) entity;
            if (aam.getInfo() != null && aam.getInfo().enableExhaustFlare) {
                // Interpolate velocity vector
                double mx = aam.prevMotionX + (aam.motionX - aam.prevMotionX) * partialTicks;
                double my = aam.prevMotionY + (aam.motionY - aam.prevMotionY) * partialTicks;
                double mz = aam.prevMotionZ + (aam.motionZ - aam.prevMotionZ) * partialTicks;
                double motionLen = Math.sqrt(mx * mx + my * my + mz * mz);

                GL11.glPushMatrix();
                // Move to missile position
                GL11.glTranslated(posX, posY, posZ);
                // Offset backward along the inverse of motion so the exhaust appears at the tail of the model
                if (motionLen > 0.0001D) {
                    double offset = 0.8D;
                    GL11.glTranslated(-mx / motionLen * offset,
                        -my / motionLen * offset,
                        -mz / motionLen * offset);
                }
                // Billboard: rotate by (180° − playerViewY) and −playerViewX so the quad faces the player
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
        MCH_EntityAircraft ac = null;
        Entity ridingEntity = Minecraft.getMinecraft().thePlayer.ridingEntity;
        if (ridingEntity instanceof MCH_EntityAircraft) {
            ac = (MCH_EntityAircraft) ridingEntity;
        } else if (ridingEntity instanceof MCH_EntitySeat) {
            ac = ((MCH_EntitySeat) ridingEntity).getParent();
        } else if (ridingEntity instanceof MCH_EntityUavStation) {
            ac = ((MCH_EntityUavStation) ridingEntity).getControlAircract();
        }

        if (ac == null || ac.isRenderBullet(entity, Minecraft.getMinecraft().thePlayer)) {
            if (entity instanceof MCH_EntityBaseBullet) {
                MCH_EntityBaseBullet bullet = (MCH_EntityBaseBullet) entity;
                GL11.glPushMatrix();
                GL11.glTranslated(posX, posY, posZ);
                GL11.glRotatef(-entity.rotationYaw, 0.0F, 1.0F, 0.0F);
                GL11.glRotatef(-entity.rotationPitch, -1.0F, 0.0F, 0.0F);
                this.renderModel(bullet);
                GL11.glPopMatrix();
            }

        }
    }

    protected ResourceLocation getEntityTexture(Entity entity) {
        return W_Render.TEX_DEFAULT;
    }
}
