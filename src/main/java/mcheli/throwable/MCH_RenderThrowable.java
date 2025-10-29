package mcheli.throwable;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mcheli.MCH_Camera;
import mcheli.wrapper.W_Render;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

@SideOnly(Side.CLIENT)
public class MCH_RenderThrowable extends W_Render {

    /** Halo radius */
    private static final float _RADIUS = 6.0F;
    /** Halo brightness */
    private static final float BRIGHTNESS = 1.0F;

    public MCH_RenderThrowable() {
        super.shadowSize = 0.0F;
    }

    @Override
    public void doRender(Entity entity, double posX, double posY, double posZ,
                         float yaw, float partialTicks) {
        if (!(entity instanceof MCH_EntityThrowable)) {
            return;
        }

        MCH_EntityThrowable throwable = (MCH_EntityThrowable) entity;
        MCH_ThrowableInfo info = throwable.getInfo();
        if (info == null) {
            return;
        }

        GL11.glPushMatrix();
        // Move to entity position
        GL11.glTranslated(posX, posY, posZ);

        // Save and set maximum brightness
        final float prevBrightnessX = OpenGlHelper.lastBrightnessX;
        final float prevBrightnessY = OpenGlHelper.lastBrightnessY;
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240F, 240F);

        // ==== Render light halo (only when holding a flare) ====
        if (info.handFlare) {
            renderLight(entity, partialTicks);
        }

        // ==== Render model ====
        // Rotate model to match entity orientation
        GL11.glRotatef(entity.prevRotationYaw + (entity.rotationYaw - entity.prevRotationYaw) * partialTicks, 0.0F, -1.0F, 0.0F);
        GL11.glRotatef(entity.prevRotationPitch + (entity.rotationPitch - entity.prevRotationPitch) * partialTicks, 1.0F, 0.0F, 0.0F);

        this.setCommonRenderParam(true, entity.getBrightnessForRender(partialTicks));
        if (info.model != null) {
            this.bindTexture("textures/throwable/" + info.name + ".png");
            info.model.renderAll();
        }
        this.restoreCommonRenderParam();

        // Restore brightness
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, prevBrightnessX, prevBrightnessY);

        GL11.glPopMatrix();
    }

    /**
     * Renders a circular light halo — transparency is disabled in thermal vision mode.
     * @param entity The entity to render the light for
     * @param partialTicks Interpolation factor between ticks
     */
    private void renderLight(Entity entity, float partialTicks) {
        float RADIUS = _RADIUS;
        boolean isThermalVision = MCH_Camera.currentCameraMode == MCH_Camera.MODE_THERMALVISION;

        if (isThermalVision) {
            RADIUS += 4f;
        }

        // Rendering setup similar to spotlight rendering
        RenderHelper.disableStandardItemLighting();
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glShadeModel(GL11.GL_SMOOTH);
        GL11.glEnable(GL11.GL_BLEND);

        // Choose blend function based on mode
        if (isThermalVision) {
            // Thermal vision mode: disable transparency and use opaque blending
            GL11.glDisable(GL11.GL_ALPHA_TEST);
            GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ZERO); // Opaque blend
        } else {
            // Normal mode: use additive transparent blending
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
            GL11.glDisable(GL11.GL_ALPHA_TEST);
        }

        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDepthMask(false); // Disable depth writing so the halo isn't blocked by world geometry

        GL11.glPushMatrix();

        // Make the halo always face the player
        GL11.glRotatef(-RenderManager.instance.playerViewY, 0.0F, 1.0F, 0.0F);
        GL11.glRotatef(RenderManager.instance.playerViewX, 1.0F, 0.0F, 0.0F);

        // Slightly move forward to avoid z-fighting
        GL11.glTranslatef(0.0F, 0.0F, 0.1F);

        Tessellator tessellator = Tessellator.instance;

        // Use multiple concentric ring layers to create a radial gradient
        int layers = 16; // Number of layers; more layers = smoother gradient
        int segments = 32; // Number of segments per layer

        for (int layer = 0; layer < layers; layer++) {
            float innerRadius = (layer * RADIUS) / layers;
            float outerRadius = ((layer + 1) * RADIUS) / layers;

            // Calculate color and transparency for inner and outer rings
            float innerProgress = innerRadius / RADIUS;
            float outerProgress = outerRadius / RADIUS;

            float innerRed, innerGreen, innerBlue, innerAlpha;
            float outerRed, outerGreen, outerBlue, outerAlpha;

            if (isThermalVision) {
                // Thermal vision mode: magenta color, fully opaque
                innerRed = 1.0F;
                innerGreen = 0.0F;
                innerBlue = 1.0F;
                innerAlpha = 1.0F; // Fully opaque

                outerRed = 1.0F;
                outerGreen = 0.0F;
                outerBlue = 1.0F;
                outerAlpha = 1.0F; // Fully opaque
            } else {
                // Normal mode: gradient from red to white
                innerRed = 1.0F;
                innerGreen = innerProgress; // 0 → 1
                innerBlue = innerProgress;  // 0 → 1
                innerAlpha = 1.0F - innerProgress * innerProgress; // Squared falloff for opacity

                outerRed = 1.0F;
                outerGreen = outerProgress; // 0 → 1
                outerBlue = outerProgress;  // 0 → 1
                outerAlpha = 1.0F - outerProgress * outerProgress; // Squared falloff for opacity
            }

            // Apply brightness
            innerRed *= BRIGHTNESS; innerGreen *= BRIGHTNESS; innerBlue *= BRIGHTNESS;
            outerRed *= BRIGHTNESS; outerGreen *= BRIGHTNESS; outerBlue *= BRIGHTNESS;

            tessellator.startDrawing(GL11.GL_QUAD_STRIP);

            for (int i = 0; i <= segments; i++) {
                double angle = 2.0 * Math.PI * i / segments;
                double sin = Math.sin(angle);
                double cos = Math.cos(angle);

                // Inner ring vertex
                double xInner = sin * innerRadius;
                double yInner = cos * innerRadius;
                tessellator.setColorRGBA_F(innerRed, innerGreen, innerBlue, innerAlpha);
                tessellator.addVertex(xInner, yInner, 0.0D);

                // Outer ring vertex
                double xOuter = sin * outerRadius;
                double yOuter = cos * outerRadius;
                tessellator.setColorRGBA_F(outerRed, outerGreen, outerBlue, outerAlpha);
                tessellator.addVertex(xOuter, yOuter, 0.0D);
            }

            tessellator.draw();
        }

        GL11.glPopMatrix();

        // Restore previous render state
        GL11.glDepthMask(true);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_ALPHA_TEST);

        // Restore default blend function
        if (isThermalVision) {
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        } else {
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        }

        RenderHelper.enableStandardItemLighting();
    }

    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        return W_Render.TEX_DEFAULT;
    }
}
