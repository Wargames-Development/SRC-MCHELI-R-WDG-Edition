package mcheli.weapon;

import mcheli.MCH_Camera;
import mcheli.MCH_I18n;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.aircraft.MCH_EntitySeat;
import mcheli.gui.MCH_Gui;
import mcheli.uav.MCH_EntityUavStation;
import mcheli.wrapper.W_Render;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import org.lwjgl.opengl.GL11;

import java.util.List;

public class MCH_RenderLockBox extends W_Render {

    public static void renderGuidanceHUD() {
        EntityClientPlayerMP player = Minecraft.getMinecraft().thePlayer;
        if (player == null) return;
        MCH_EntityAircraft ac = null;
        if (player.ridingEntity instanceof MCH_EntityAircraft) {
            ac = (MCH_EntityAircraft) player.ridingEntity;
        } else if (player.ridingEntity instanceof MCH_EntitySeat) {
            ac = ((MCH_EntitySeat) player.ridingEntity).getParent();
        } else if (player.ridingEntity instanceof MCH_EntityUavStation) {
            ac = ((MCH_EntityUavStation) player.ridingEntity).getControlAircract();
        }
        if (ac == null) return;
        MCH_IGuidanceSystem guidanceSystem = ac.getCurrentWeapon(player).getCurrentWeapon().getGuidanceSystem();
        if (guidanceSystem == null) {
            return;
        }

        if (guidanceSystem instanceof MCH_LaserGuidanceSystem) {

            if (!((MCH_LaserGuidanceSystem) guidanceSystem).targeting) return;

            double lockPosX = guidanceSystem.getLockPosX();
            double lockPosY = guidanceSystem.getLockPosY();
            double lockPosZ = guidanceSystem.getLockPosZ();

            double posX = RenderManager.renderPosX;
            double posY = RenderManager.renderPosY;
            double posZ = RenderManager.renderPosZ;

            RenderManager rm = RenderManager.instance;
            double distance = Math.sqrt(Math.pow(lockPosX - posX, 2) + Math.pow(lockPosY - posY, 2) + Math.pow(lockPosZ - posZ, 2));

            double x = lockPosX - posX;
            double y = lockPosY - posY;
            double z = lockPosZ - posZ;

            if (distance > 1000) return;

            boolean jammed = false;
            {
                double r = 6.0D;
                AxisAlignedBB aabb = AxisAlignedBB.getBoundingBox(lockPosX - r, lockPosY - r, lockPosZ - r, lockPosX + r, lockPosY + r, lockPosZ + r);
                List list = player.worldObj.getEntitiesWithinAABB(mcheli.aircraft.MCH_EntityAircraft.class, aabb);
                for (Object o : list) {
                    MCH_EntityAircraft veh = (MCH_EntityAircraft) o;
                    if (veh != null && veh.getAcInfo() != null && (veh.getAcInfo().hasPhotoelectricJammer || veh.isECMJammerUsing())) {
                        jammed = true;
                        break;
                    }
                }
            }

            GL11.glPushMatrix();
            GL11.glTranslatef((float) x, (float) y, (float) z);
            GL11.glNormal3f(0.0F, 1.0F, 0.0F);
            GL11.glRotatef(-rm.playerViewY, 0.0F, 1.0F, 0.0F);
            GL11.glRotatef(rm.playerViewX, 1.0F, 0.0F, 0.0F);
            GL11.glScalef(-0.02666667F, -0.02666667F, 0.02666667F);
            GL11.glDisable(2896);
            GL11.glDepthMask(false);
            GL11.glEnable(3042);
            GL11.glBlendFunc(770, 771);
            GL11.glDisable(3553);
            GL11.glDisable(2929);
            if (MCH_Camera.currentCameraMode == MCH_Camera.MODE_THERMALVISION) {
                RenderHelper.disableStandardItemLighting();
                OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240, 240);
                GL11.glColor4f(1000F, 0F, 1000F, 1.0F);
            }
            int prevWidth = GL11.glGetInteger(2849);
            float minDistance = 50.0F;
            float size1 = 20.0F;
            float maxDistance = 300.0F;
            float maxSize = 100.0F;
            float size = size1 + (float) ((distance - minDistance) / (maxDistance - minDistance)) * (maxSize - size1);
            size = Math.max(size1, Math.min(maxSize, size));

            Tessellator tessellator = Tessellator.instance;
            tessellator.startDrawing(2);
            tessellator.setBrightness(240);

            GL11.glLineWidth((float) MCH_Gui.scaleFactor * 1.5F);
            if (jammed) {
                tessellator.setColorRGBA_F(1.0F, 0.0F, 0.0F, 1.0F);
            } else {
                tessellator.setColorRGBA_F(0.0F, 1.0F, 0.0F, 1.0F);
            }

            tessellator.addVertex(-size - 1.0F, -size, 0.0D);
            tessellator.addVertex(-size - 1.0F, size, 0.0D);
            tessellator.addVertex(size + 1.0F, size, 0.0D);
            tessellator.addVertex(size + 1.0F, -size, 0.0D);
            tessellator.draw();

            if (distance > 10) {
                FontRenderer fontRenderer = Minecraft.getMinecraft().fontRenderer;
                {
                    GL11.glPushMatrix();
                    GL11.glTranslatef(0.0F, size + 1.0F, 0.0F);
                    float fontSize = 5.0F + (float) ((distance - 10.0D) / (300.0D - 10.0D)) * (40.0F - 5.0F);
                    fontSize = Math.max(5.0F, Math.min(40.0F, fontSize));
                    String text;
                    int color;
                    if (jammed) {
                        text = MCH_I18n.format("message.mcheli.jamming");
                        color = 0xff0000;
                    } else {
                        text = String.format("%.1f", distance);
                        color = 0x00ff00;
                    }
                    GL11.glScalef(fontSize, fontSize, fontSize);
                    fontRenderer.drawString(text, -fontRenderer.getStringWidth(text) / 2, 0, color);
                    GL11.glPopMatrix();
                }
            }

            GL11.glPopMatrix();
            GL11.glLineWidth((float) prevWidth);
            GL11.glEnable(3553);
            GL11.glDepthMask(true);
            GL11.glEnable(2896);
            GL11.glDisable(3042);
            GL11.glEnable(2929);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    @Override
    public void doRender(Entity entity, double posX, double posY, double posZ, float par8, float tickTime) {
        renderGuidanceHUD();
    }
}
