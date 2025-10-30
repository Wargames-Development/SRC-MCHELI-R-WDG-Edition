package mcheli.weapon;

import mcheli.MCH_Camera;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.aircraft.MCH_EntitySeat;
import mcheli.gui.MCH_Gui;
import mcheli.plane.MCP_EntityPlane;
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
import org.lwjgl.opengl.GL11;

public class MCH_RenderLockBox extends W_Render {
    @Override
    public void doRender(Entity entity, double posX, double posY, double posZ, float par8, float tickTime) {
        renderGuidanceHUD();
    }


    public static void renderGuidanceHUD() {
        EntityClientPlayerMP player = Minecraft.getMinecraft().thePlayer;
        if(player == null) return;
        MCH_EntityAircraft ac = null; // The entity the player is riding
        if(player.ridingEntity instanceof MCH_EntityAircraft) {
            ac = (MCH_EntityAircraft)player.ridingEntity;
        } else if(player.ridingEntity instanceof MCH_EntitySeat) {
            ac = ((MCH_EntitySeat)player.ridingEntity).getParent();
        } else if(player.ridingEntity instanceof MCH_EntityUavStation) {
            ac = ((MCH_EntityUavStation)player.ridingEntity).getControlAircract();
        }
        if(ac == null) return;
        MCH_IGuidanceSystem guidanceSystem = ac.getCurrentWeapon(player).getCurrentWeapon().getGuidanceSystem();
        if(guidanceSystem == null) {
            return;
        }

        if (guidanceSystem instanceof MCH_LaserGuidanceSystem) {

            if(!((MCH_LaserGuidanceSystem) guidanceSystem).targeting) return;

            double lockPosX = guidanceSystem.getLockPosX();
            double lockPosY = guidanceSystem.getLockPosY();
            double lockPosZ = guidanceSystem.getLockPosZ();

//            double posX = player.posX;
//            double posY = player.posY + player.getEyeHeight();
//            double posZ = player.posZ;

            double posX = RenderManager.renderPosX;
            double posY = RenderManager.renderPosY;
            double posZ = RenderManager.renderPosZ;

            RenderManager rm = RenderManager.instance;
            double distance = Math.sqrt(Math.pow(lockPosX - posX, 2) + Math.pow(lockPosY - posY, 2) + Math.pow(lockPosZ - posZ, 2));

            double x = lockPosX - posX;
            double y = lockPosY - posY;
            double z = lockPosZ - posZ;

            if(distance > 1000) return;

            GL11.glPushMatrix();
            // Apply positional transforms to render the target entity relative to the player's view
            GL11.glTranslatef((float)x, (float)y , (float)z);
            GL11.glNormal3f(0.0F, 1.0F, 0.0F);
            GL11.glRotatef(-rm.playerViewY, 0.0F, 1.0F, 0.0F);
            GL11.glRotatef(rm.playerViewX, 1.0F, 0.0F, 0.0F);
            GL11.glScalef(-0.02666667F, -0.02666667F, 0.02666667F);
            GL11.glDisable(2896); // Disable lighting
            GL11.glTranslatef(0.0F, 9.374999F, 0.0F); // Apply upward offset
            GL11.glDepthMask(false); // Disable depth writing
            GL11.glEnable(3042); // Enable blending
            GL11.glBlendFunc(770, 771); // Set blend mode (SRC_ALPHA, ONE_MINUS_SRC_ALPHA)
            GL11.glDisable(3553); // Disable texturing
            GL11.glDisable(2929 /* GL_DEPTH_TEST */); // Disable depth testing
            if (MCH_Camera.currentCameraMode == MCH_Camera.MODE_THERMALVISION) {
                RenderHelper.disableStandardItemLighting();
                OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240, 240);
                GL11.glColor4f(1000F, 0F, 1000F, 1.0F); // Magenta hue for thermal vision mode
            }
            // Get current viewport width before drawing
            int prevWidth = GL11.glGetInteger(2849);
            // Adjust entity size based on target distance (50–20 units near, 1000–1000 units far)
            float minDistance = 50.0F;
            float size1 = 20.0F;
            float maxDistance = 300.0F;
            float maxSize = 100.0F;
            float size = size1 + (float)((distance - minDistance) / (maxDistance - minDistance)) * (maxSize - size1);
            size = Math.max(size1, Math.min(maxSize, size));


            // Create a Tessellator instance for drawing geometric shapes
            Tessellator tessellator = Tessellator.instance;
            tessellator.startDrawing(2); // Begin drawing line primitives
            tessellator.setBrightness(240); // Set brightness level

            GL11.glLineWidth((float) MCH_Gui.scaleFactor * 1.5F); // Set line thickness
            tessellator.setColorRGBA_F(0.0F, 1.0F, 0.0F, 1.0F); // Set color to green

            // Draw a rectangular outline representing the lock-on area
            tessellator.addVertex(-size - 1.0F, 0.0D, 0.0D);
            tessellator.addVertex(-size - 1.0F, size * 2.0F, 0.0D);
            tessellator.addVertex(size + 1.0F, size * 2.0F, 0.0D);
            tessellator.addVertex(size + 1.0F, 0.0D, 0.0D);
            tessellator.draw(); // Render the lines


            if(distance > 10) {
                // Get FontRenderer and set text color to green
                FontRenderer fontRenderer = Minecraft.getMinecraft().fontRenderer;
                {
                    GL11.glPushMatrix();
                    GL11.glTranslatef(0.0F, size * 2.0F + 1.0F, 0.0F); // Position text below the rectangle
                    float fontSize = 5.0F + (float) ((distance - 10.0D) / (300.0D - 10.0D)) * (40.0F - 5.0F);
                    // Clamp font size between 5 and 40
                    fontSize = Math.max(5.0F, Math.min(40.0F, fontSize));
                    GL11.glScalef(fontSize, fontSize, fontSize);
                    String text = String.format("%.1f", distance); // Format distance with one decimal place
                    fontRenderer.drawString(text, -fontRenderer.getStringWidth(text) / 2, 0, 0x00ff00);
                    GL11.glPopMatrix();
                }
            }

            GL11.glPopMatrix();
            // Restore previous OpenGL states
            GL11.glLineWidth((float) prevWidth); // Reset line width
            GL11.glEnable(3553); // Enable texturing
            GL11.glDepthMask(true); // Re-enable depth writing
            GL11.glEnable(2896); // Re-enable lighting
            GL11.glDisable(3042); // Disable blending
            GL11.glEnable(2929 /* GL_DEPTH_TEST */); // Re-enable depth testing
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F); // Reset to default color
        }
    }
}
