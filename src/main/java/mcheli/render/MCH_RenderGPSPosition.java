package mcheli.render;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.aircraft.MCH_EntitySeat;
import mcheli.uav.MCH_EntityUavStation;
import mcheli.vector.Vector3f;
import mcheli.weapon.MCH_GPSPosition;
import mcheli.wrapper.W_MOD;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import org.lwjgl.opengl.GL11;

public class MCH_RenderGPSPosition {

    private static final ResourceLocation GPS_POS = new ResourceLocation(W_MOD.DOMAIN, "textures/GPSPosition.png");
    private static final int ICON_SIZE = 24;

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return;

        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.thePlayer;
        if (player == null || mc.theWorld == null) return;

        MCH_EntityAircraft ac = null;
        if (player.ridingEntity instanceof MCH_EntityAircraft) {
            ac = (MCH_EntityAircraft) player.ridingEntity;
        } else if (player.ridingEntity instanceof MCH_EntitySeat) {
            ac = ((MCH_EntitySeat) player.ridingEntity).getParent();
        } else if (player.ridingEntity instanceof MCH_EntityUavStation) {
            ac = ((MCH_EntityUavStation) player.ridingEntity).getControlAircract();
        }
        if (ac == null) {
            return;
        }

        // 没有激活就不渲染
        MCH_GPSPosition gps = MCH_GPSPosition.currentClientGPSPosition;
        if (gps == null || !gps.isActive()) return;

        ScaledResolution sc = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);

        // 投影到屏幕
        double[] screenPos = MCH_RenderBVRLockBox.worldToScreen(new Vector3f((float) gps.x, (float) gps.y, (float) gps.z), event.partialTicks);
        double sx = screenPos[0];
        double sy = screenPos[1];
        double ox = screenPos[2];
        double oy = screenPos[3];

        if (sx < 0 || sy < 0) return; // 在镜头后方

        // 依据相对屏幕中心距离设置透明度与边缘夹取
        float alpha = 0.1f;
        boolean inLock = false;

        double distScreen = ox * ox + oy * oy;
        // 这些阈值与 BVR 一致（按屏幕高度比例缩放）
        double h = sc.getScaledHeight();
        if (distScreen < Math.pow(0.038 * h, 2)) {          // ~20 px
            alpha = 1.0f;
            inLock = true; // 中心圈
        } else if (distScreen < Math.pow(0.076 * h, 2)) {   // ~40 px
            alpha = 1.0f;
        } else if (distScreen < Math.pow(0.152 * h, 2)) {   // ~80 px
            alpha = 0.8f;
        } else if (distScreen < Math.pow(0.228 * h, 2)) {   // ~120 px
            alpha = 0.6f;
        } else if (distScreen < Math.pow(0.288 * h, 2)) {   // ~150 px
            alpha = 0.4f;
        } else if (distScreen > Math.pow(0.384 * h, 2)) {   // > ~200 px，夹到边缘
            double distance = Math.sqrt(distScreen);
            double ratio = 200.0 / distance; // 与 BVR 一样夹取到半径≈200px
            sx = sc.getScaledWidth() / 2.0 + ox * ratio;
            sy = sc.getScaledHeight() / 2.0 + oy * ratio;
            alpha = 0.2f;
        }

        // 渲染
        GL11.glPushMatrix();
        {
            drawGPSMarker(sx, sy, inLock, alpha);

            // 距离文字（接近中心时更明显）
            if (alpha >= 0.6f) {
                double dist = player.getDistance(gps.x, gps.y, gps.z);
                int color = inLock ? 0xFF0000 : 0x00FF00;
                Minecraft.getMinecraft().fontRenderer.drawString(
                    String.format("[GPS %.1fm]", dist),
                    (int) (sx - 20), (int) (sy + 12), color
                );
            }
        }
        GL11.glPopMatrix();
    }

    private void drawGPSMarker(double x, double y, boolean inLock, float alpha) {
        prepareRenderState(inLock, alpha);
        Minecraft.getMinecraft().renderEngine.bindTexture(GPS_POS);

        Tessellator tess = Tessellator.instance;
        tess.startDrawingQuads();
        double halfSize = ICON_SIZE / 2.0;

        tess.addVertexWithUV(x - halfSize, y + halfSize, 0, 0, 1);
        tess.addVertexWithUV(x + halfSize, y + halfSize, 0, 1, 1);
        tess.addVertexWithUV(x + halfSize, y - halfSize, 0, 1, 0);
        tess.addVertexWithUV(x - halfSize, y - halfSize, 0, 0, 0);

        tess.draw();
        restoreRenderState();
    }

    private void prepareRenderState(boolean lock, float alpha) {
        GL11.glEnable(GL11.GL_BLEND);
        if (lock) {
            GL11.glColor4f(1.0F, 0F, 0F, 1.0F); // 中心圈高亮红色
        } else {
            GL11.glColor4f(0F, 1.0F, 0F, alpha); // 其他为绿色且带透明度
        }
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    }

    private void restoreRenderState() {
        int srcBlend = GL11.glGetInteger(3041); // GL_BLEND_SRC
        int dstBlend = GL11.glGetInteger(3040); // GL_BLEND_DST
        GL11.glBlendFunc(srcBlend, dstBlend);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glColor4f(1F, 1F, 1F, 1F);
    }
}
