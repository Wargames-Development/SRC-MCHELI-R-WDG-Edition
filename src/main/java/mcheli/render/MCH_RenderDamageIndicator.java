package mcheli.render;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import mcheli.MCH_DamageIndicator;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.helicopter.MCH_EntityHeli;
import mcheli.plane.MCP_EntityPlane;
import mcheli.tank.MCH_EntityTank;
import mcheli.vehicle.MCH_EntityVehicle;
import mcheli.wrapper.W_MOD;
import mcheli.wrapper.W_ScaledResolution;
import mcheli.wrapper.modelloader.W_ModelCustom;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import org.lwjgl.opengl.GL11;

import java.util.List;

/**
 * Vehicle HUD Renderer – includes damage indicator rendering.
 *
 * <p>This class is responsible for drawing the vehicle's 3D model and damage indicators
 * on the top-left area of the screen when the player is riding a vehicle.
 * To prevent the rendered area from exceeding screen boundaries, the rendering
 * position is adjusted using {@link #horizontalOffset} and {@link #verticalOffset}.
 * Additionally, the damage indicator rendering uses relative coordinates and directions,
 * which have already been transformed on the server side according to the vehicle's
 * pitch, roll, and yaw rotations.</p>
 */
public class MCH_RenderDamageIndicator {

    /**
     * Model spin angle, used to make the model slowly rotate for display.
     */
    private static float spinYaw = 0.0F;
    /**
     * Pixel base value used to calculate model scaling during rendering.
     * Adjusting this value changes the model’s size within the frame.
     */
    private static final double BOX_PIXELS = 220.0;
    /**
     * Additional zoom factor for fine-tuning the model size.
     */
    private static final float EXTRA_ZOOM = 1.2F;

    /**
     * Horizontal pixel offset of the rendering area relative to the top-right corner of the screen.
     * Adjust this value to move the HUD away from the screen edge and prevent clipping.
     */
    private static final int horizontalOffset = 70;
    /**
     * Vertical pixel offset of the rendering area relative to the top of the screen.
     * Adjust this value to move the HUD away from the screen edge and prevent clipping.
     */
    private static final int verticalOffset = 30;

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return;
        if(true) {
            return;
        }
        render(Minecraft.getMinecraft(), event.partialTicks);
    }

    /**
     * Main rendering entry point. Draws the vehicle model and damage indicators on the HUD
     * when the player is riding a vehicle.
     *
     * @param mc           Minecraft client instance
     * @param partialTicks Render interpolation factor
     */
    public static void render(Minecraft mc, float partialTicks) {
        if (mc == null || mc.thePlayer == null || mc.theWorld == null) return;

        EntityPlayer player = mc.thePlayer;
        if (!(player.ridingEntity instanceof MCH_EntityAircraft)) {
            return;
        }

        MCH_EntityAircraft ac = (MCH_EntityAircraft) player.ridingEntity;
        if (ac.getAcInfo() == null) return;

        if (!(ac.getAcInfo().model instanceof W_ModelCustom)) return;

        W_ModelCustom model = (W_ModelCustom) ac.getAcInfo().model;
        if (model == null) return;

        // Get the list of damage indicators (received by client packets, already in relative coordinates and direction)
        List<MCH_DamageIndicator> damageIndicators = ac.damageIndicatorList;

        // Bind the vehicle texture
        ResourceLocation texture = getTextureForAircraft(ac);
        if (texture != null) {
            mc.getTextureManager().bindTexture(texture);
        }

        // Use ScaledResolution to get screen width and height (in HUD-scaled pixels)
        W_ScaledResolution sr = new W_ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
        int sw = sr.getScaledWidth();
        int sh = sr.getScaledHeight();

        // Base box size and margin, scaled relative to an 800px height reference
        final int baseBoxSize = 140;
        final int baseMargin = 6;

        // Adjust the box size based on screen height to prevent oversized HUDs on low resolutions
        float scaleFactor = Math.min(1.0f, (float) sh / 800.0f);
        int boxSize = (int) (baseBoxSize * scaleFactor);
        int margin = (int) (baseMargin * scaleFactor);

        // Calculate render frame center position (offset from top-left corner),
        // subtracting horizontal offset and adding vertical offset
        double drawX = sw - margin - boxSize * 0.5 - horizontalOffset;
        double drawY = margin + boxSize * 0.5 + verticalOffset;

        // Compute the model scaling factor so that the full model fits inside the frame
        double modelSize = Math.abs(model.size) < 0.01 ? 0.01 : Math.abs(model.size);
        double scale = (BOX_PIXELS / modelSize) * EXTRA_ZOOM;

        // Calculate model center; the coordinate bounds are provided by the loaded OBJ model
        double cx = (model.maxX - model.minX) * 0.5 + model.minX;
        double cy = (model.maxY - model.minY) * 0.5 + model.minY;
        double cz = (model.maxZ - model.minZ) * 0.5 + model.minZ;
        double[] center = new double[]{cx, cy, cz};

        // Increment spin angle to make the model rotate slowly
        spinYaw += (1.8F * partialTicks);
        if (spinYaw > 360.0F) spinYaw -= 360.0F;

        // Render the model and damage indicators
        renderModel(mc, model, drawX, drawY, scale, center, damageIndicators, boxSize, margin);
    }

    /**
     * Get the corresponding texture path based on the aircraft type.
     */
    private static ResourceLocation getTextureForAircraft(MCH_EntityAircraft ac) {
        if (ac.getTextureName() == null) return null;

        String texturePath;
        if (ac instanceof MCH_EntityHeli) {
            texturePath = "textures/helicopters/" + ac.getTextureName() + ".png";
        } else if (ac instanceof MCP_EntityPlane) {
            texturePath = "textures/planes/" + ac.getTextureName() + ".png";
        } else if (ac instanceof MCH_EntityTank) {
            texturePath = "textures/tanks/" + ac.getTextureName() + ".png";
        } else if (ac instanceof MCH_EntityVehicle) {
            texturePath = "textures/vehicles/" + ac.getTextureName() + ".png";
        } else {
            texturePath = null;
        }
        return texturePath != null ? new ResourceLocation(W_MOD.DOMAIN, texturePath) : null;
    }

    /**
     * Renders the 3D model and its damage indicators.
     * Uses OpenGL scissoring to restrict the drawing region and prevent rendering outside the HUD frame.
     */
    private static void renderModel(Minecraft mc, W_ModelCustom model,
                                    double drawX, double drawY,
                                    double scale, double[] center,
                                    List<MCH_DamageIndicator> damageIndicators,
                                    int boxSize, int margin) {
        GL11.glPushMatrix();
        try {
            GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_COLOR_BUFFER_BIT);

            // Enable depth testing to ensure correct occlusion between model parts
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glDepthFunc(GL11.GL_LEQUAL);
            GL11.glDepthMask(true);

            // Use a scissor region to limit the drawing area, preventing the model from rendering outside the HUD box
            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            W_ScaledResolution sr = new W_ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
            int scaleFactor = sr.getScaleFactor();
            int x = (sr.getScaledWidth() - boxSize - margin - horizontalOffset) * scaleFactor;
            int y = (margin + verticalOffset) * scaleFactor;
            int width = boxSize * scaleFactor;
            int height = boxSize * scaleFactor;
            GL11.glScissor(x, y, width, height);
            GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
            GL11.glDisable(GL11.GL_SCISSOR_TEST);

            // Configure OpenGL states
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_CULL_FACE);
            GL11.glCullFace(GL11.GL_BACK);
            GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);

            // Set model position and rotation: move to screen location, flip Y-axis (due to MC coordinate system),
            // and rotate around Y-axis for spinning animation
            GL11.glTranslated(drawX, drawY, 300.0);
            GL11.glRotated(180.0, 1.0, 0.0, 0.0);
            GL11.glRotated(spinYaw, 0.0, 1.0, 0.0);
            GL11.glScaled(scale, scale, -scale);
            GL11.glTranslated(-center[0], -center[1], -center[2]);

            // Render the model
            model.renderAll(0, model.getFaceNum());

            // Render damage indicators — these positions and orientations have already been transformed
            // on the server side into relative coordinates
            if (damageIndicators != null && !damageIndicators.isEmpty()) {
                renderDamageIndicators(damageIndicators);
            }

        } catch (Throwable t) {
            System.err.println("Error in depth render: " + t.getMessage());
            t.printStackTrace();
        } finally {
            GL11.glPopAttrib();
            GL11.glPopMatrix();
        }
    }

    /**
     * Renders a batch of damage indicators, each representing a bullet hit position
     * and direction on the vehicle model.
     * Depth testing remains enabled so that lines and markers are correctly occluded by the model.
     */
    private static void renderDamageIndicators(List<MCH_DamageIndicator> indicators) {
        GL11.glPushMatrix();
        try {
            GL11.glPushAttrib(GL11.GL_ENABLE_BIT);
            // Disable textures and face culling, enable blending
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            // Keep depth testing enabled so that lines and markers properly interact with the model
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glDepthFunc(GL11.GL_LEQUAL);
            // Line width
            GL11.glLineWidth(2.5F);
            for (MCH_DamageIndicator indicator : indicators) {
                if (indicator.relativeHitPos != null && indicator.relativeDir != null) {
                    renderDamageIndicator(indicator);
                }
            }
        } finally {
            GL11.glPopAttrib();
            GL11.glPopMatrix();
        }
    }

    /**
     * Renders a single damage indicator.
     *
     * <p>Uses the relative hit position and bullet trajectory direction to calculate the start point of the line.
     * To ensure that the line length is proportional to the amount of damage, the direction vector is normalized
     * and then multiplied by the computed length.</p>
     */
    private static void renderDamageIndicator(MCH_DamageIndicator indicator) {
        Vec3 hitPos = indicator.relativeHitPos;
        Vec3 bulletDir = indicator.relativeDir;
        if (hitPos == null || bulletDir == null) return;

        // Compute the line length based on damage — higher damage produces a longer line
        double baseLength = 0.5;
        double lineLength = baseLength * (0.7 + indicator.damage / 150.0);

        // Normalize bullet direction so it represents only direction, not speed
        Vec3 dirNorm = bulletDir.normalize();

        // Starting point is the hit position offset backwards along the bullet direction

        Vec3 lineStart = Vec3.createVectorHelper(
            hitPos.xCoord - dirNorm.xCoord * lineLength,
            hitPos.yCoord - dirNorm.yCoord * lineLength,
            hitPos.zCoord - dirNorm.zCoord * lineLength
        );
        Vec3 lineEnd = hitPos;

        // Set red color intensity based on damage — higher damage yields a brighter red
        float colorIntensity = (float) Math.min(1.0, 0.4 + indicator.damage / 120.0);
        GL11.glColor4f(1.0f, 0.0f, 0.0f, colorIntensity);

        // Draw the line segment
        GL11.glBegin(GL11.GL_LINES);
        try {
            GL11.glVertex3d(lineStart.xCoord, lineStart.yCoord, lineStart.zCoord);
            GL11.glVertex3d(lineEnd.xCoord, lineEnd.yCoord, lineEnd.zCoord);
        } finally {
            GL11.glEnd();
        }

        // Draw a small sphere marker at the hit position, with size based on damage
        GL11.glPushMatrix();
        GL11.glTranslated(hitPos.xCoord, hitPos.yCoord, hitPos.zCoord);
        GL11.glColor4f(1.0f, 0.0f, 0.0f, colorIntensity * 0.8f);
        double sphereRadius = 0.02 + 0.03 * (indicator.damage / 100.0);
        drawSimpleSphere(sphereRadius, 6, 4);
        GL11.glPopMatrix();

        // Restore color to white
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
    }

    /**
     * Draws a simplified sphere using triangle fans to construct the upper and lower hemispheres.
     */
    private static void drawSimpleSphere(double radius, int slices, int stacks) {
        // Upper hemisphere
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        GL11.glVertex3d(0, radius, 0);
        for (int i = 0; i <= slices; i++) {
            double theta = 2.0 * Math.PI * i / slices;
            double x = Math.sin(theta) * radius;
            double z = Math.cos(theta) * radius;
            GL11.glVertex3d(x, 0, z);
        }
        GL11.glEnd();
        // Lower hemisphere
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        GL11.glVertex3d(0, -radius, 0);
        for (int i = 0; i <= slices; i++) {
            double theta = 2.0 * Math.PI * i / slices;
            double x = Math.sin(theta) * radius;
            double z = Math.cos(theta) * radius;
            GL11.glVertex3d(x, 0, z);
        }
        GL11.glEnd();
    }
}
