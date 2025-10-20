package mcheli.render;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
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
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import org.lwjgl.opengl.GL11;

/**
 * 载具HUD渲染器
 */
public class MCH_RenderDamageIndicator {

    private static float spinYaw = 0.0F;
    private static final double BOX_PIXELS = 120.0;
    private static final float EXTRA_ZOOM = 1.0F;

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return;

        render(Minecraft.getMinecraft(), event.partialTicks);
    }

    public static void render(Minecraft mc, float partialTicks) {
        if (mc == null || mc.thePlayer == null || mc.theWorld == null) return;

        EntityPlayer player = mc.thePlayer;
        if (!(player.ridingEntity instanceof MCH_EntityAircraft)) {
            return;
        }

        MCH_EntityAircraft ac = (MCH_EntityAircraft) player.ridingEntity;
        if (ac.getAcInfo() == null) return;

        if(!(ac.getAcInfo().model instanceof W_ModelCustom)) return;

        W_ModelCustom model = (W_ModelCustom) ac.getAcInfo().model;
        if (model == null) return;

        ResourceLocation texture = getTextureForAircraft(ac);
        if (texture != null) {
            mc.getTextureManager().bindTexture(texture);
        }

        W_ScaledResolution sr = new W_ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
        int sw = sr.getScaledWidth();
        int sh = sr.getScaledHeight();

        final int boxW = 140;
        final int boxH = 140;
        final int margin = 6;

        double drawX = sw - margin - boxW * 0.5;
        double drawY = margin + boxH * 0.5;

        double modelSize = Math.abs(model.size) < 0.01 ? 0.01 : Math.abs(model.size);
        double scale = (BOX_PIXELS / modelSize) * EXTRA_ZOOM;

        double cx = (model.maxX - model.minX) * 0.5 + model.minX;
        double cy = (model.maxY - model.minY) * 0.5 + model.minY;
        double cz = (model.maxZ - model.minZ) * 0.5 + model.minZ;
        double[] center = new double[]{cx, cy, cz};

        spinYaw += (1.8F * partialTicks);
        if (spinYaw > 360.0F) spinYaw -= 360.0F;

        renderModel(mc, model, drawX, drawY, scale, center);
    }

    private static ResourceLocation getTextureForAircraft(MCH_EntityAircraft ac) {
        if (ac.getTextureName() == null) return null;

        String texturePath = "";
        if (ac instanceof MCH_EntityHeli) {
            texturePath = "textures/helicopters/" + ac.getTextureName() + ".png";
        } else if (ac instanceof MCP_EntityPlane) {
            texturePath = "textures/planes/" + ac.getTextureName() + ".png";
        } else if (ac instanceof MCH_EntityTank) {
            texturePath = "textures/tanks/" + ac.getTextureName() + ".png";
        } else if (ac instanceof MCH_EntityVehicle) {
            texturePath = "textures/vehicles/" + ac.getTextureName() + ".png";
        }

        return new ResourceLocation(W_MOD.DOMAIN, texturePath);
    }

    private static void renderModel(Minecraft mc, W_ModelCustom model,
                                    double drawX, double drawY,
                                    double scale, double[] center) {
        GL11.glPushMatrix();

        try {
            GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_COLOR_BUFFER_BIT);

            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glDepthFunc(GL11.GL_LEQUAL);
            GL11.glDepthMask(true);

            GL11.glEnable(GL11.GL_SCISSOR_TEST);

            W_ScaledResolution sr = new W_ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
            int scaleFactor = sr.getScaleFactor();
            int x = (sr.getScaledWidth() - 140 - 6) * scaleFactor;
            int y = 6 * scaleFactor;
            int width = 140 * scaleFactor;
            int height = 140 * scaleFactor;

            GL11.glScissor(x, y, width, height);
            GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_CULL_FACE);
            GL11.glCullFace(GL11.GL_BACK);
            GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
            GL11.glTranslated(drawX, drawY, 300.0);
            GL11.glRotated(180.0, 1.0, 0.0, 0.0);
            GL11.glRotated(spinYaw, 0.0, 1.0, 0.0);
            GL11.glScaled(scale, scale, -scale);
            GL11.glTranslated(-center[0], -center[1], -center[2]);

            model.renderAll(0, model.getFaceNum());
        } catch (Throwable t) {
            System.err.println("Error in depth render: " + t.getMessage());
        } finally {
            GL11.glPopAttrib();
            GL11.glPopMatrix();
        }
    }
}
