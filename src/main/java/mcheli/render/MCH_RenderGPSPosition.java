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
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import org.lwjgl.opengl.GL11;

public class MCH_RenderGPSPosition {

    private static final ResourceLocation GPS_POS = new ResourceLocation(W_MOD.DOMAIN, "textures/GPSPosition.png");
    private static final int ICON_SIZE_PX = 24; // 以像素为基准的目标尺寸

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.thePlayer;
        if (player == null || mc.theWorld == null) return;

        // —— 载具判定（与原逻辑一致）——
        MCH_EntityAircraft ac = null;
        if (player.ridingEntity instanceof MCH_EntityAircraft) {
            ac = (MCH_EntityAircraft) player.ridingEntity;
        } else if (player.ridingEntity instanceof MCH_EntitySeat) {
            ac = ((MCH_EntitySeat) player.ridingEntity).getParent();
        } else if (player.ridingEntity instanceof MCH_EntityUavStation) {
            ac = ((MCH_EntityUavStation) player.ridingEntity).getControlAircract();
        }
        if (ac == null) return;

        MCH_GPSPosition gps = MCH_GPSPosition.currentClientGPSPosition;
        if (gps == null || !gps.isActive()) return;

        // —— 世界/相机坐标 ——
        final double gx = gps.x, gy = gps.y, gz = gps.z;
        RenderManager rm = RenderManager.instance;
        final double camX = rm.viewerPosX, camY = rm.viewerPosY, camZ = rm.viewerPosZ;
        final double x = gx - camX, y = gy - camY, z = gz - camZ;

        // —— 视线夹角用于透明度/锁定 ——
        double px = player.prevPosX + (player.posX - player.prevPosX) * event.partialTicks;
        double py = player.prevPosY + (player.posY - player.prevPosY) * event.partialTicks + player.getEyeHeight();
        double pz = player.prevPosZ + (player.posZ - player.prevPosZ) * event.partialTicks;

        double vx = gx - px, vy = gy - py, vz = gz - pz;
        double vlen = Math.sqrt(vx*vx + vy*vy + vz*vz);
        if (vlen < 1e-4) return;
        vx /= vlen; vy /= vlen; vz /= vlen;

        Vector3f look = new Vector3f((float)player.getLookVec().xCoord, (float)player.getLookVec().yCoord, (float)player.getLookVec().zCoord);
        double dot = Math.max(-1.0, Math.min(1.0, vx*look.x + vy*look.y + vz*look.z));
        double angleDeg = Math.toDegrees(Math.acos(dot));

        float alpha;
        boolean inLock = false;
        if (angleDeg <= 1.5) { alpha = 1.0f; inLock = true; }
        else if (angleDeg <= 3.0)  alpha = 1.0f;
        else if (angleDeg <= 6.0)  alpha = 0.8f;
        else if (angleDeg <= 9.0)  alpha = 0.6f;
        else                       alpha = 0.4f; // 最小透明度 0.4

        // —— 基于 FOV 的恒定像素大小 ——
        ScaledResolution sc = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
        double dist = Math.sqrt(x*x + y*y + z*z);
        double fovDeg = mc.gameSettings.fovSetting;
        double fovRad = Math.toRadians(fovDeg);
        float sPerPixel = (float)((2.0 * dist * Math.tan(fovRad * 0.5)) / sc.getScaledHeight_double());

        // —— 取得视角 roll（度）并在 billboard 后抵消 ——
        float rollDeg = getViewRollDeg(mc, ac, event.partialTicks); // 正负方向以 MCH 的右手坐标为准
        // 如果无法获取则返回 0

        // —— 渲染 ——
        GL11.glPushMatrix();
        {
            GL11.glTranslated(x, y + 0.2, z);

            // billboard 朝向 yaw、pitch：
            GL11.glRotatef(-rm.playerViewY, 0.0F, 1.0F, 0.0F);
            GL11.glRotatef(rm.playerViewX, 1.0F, 0.0F, 0.0F);

            // 关键：抵消相机 roll，让图标始终“屏幕正立”
            GL11.glRotatef(-rollDeg, 0.0F, 0.0F, 1.0F);

            // 以像素为单位渲染
            GL11.glScalef(-sPerPixel, -sPerPixel, sPerPixel);

            // 始终可见
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDepthMask(false);

            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glDisable(GL11.GL_LIGHTING);

            // 图标
            if (inLock) GL11.glColor4f(1.0F, 0F, 0F, 1.0F);
            else        GL11.glColor4f(0F, 1.0F, 0F, alpha);

            Minecraft.getMinecraft().getTextureManager().bindTexture(GPS_POS);
            Tessellator tess = Tessellator.instance;
            float half = ICON_SIZE_PX * 0.5f;
            tess.startDrawingQuads();
            tess.addVertexWithUV(-half,  half, 0, 0, 1);
            tess.addVertexWithUV( half,  half, 0, 1, 1);
            tess.addVertexWithUV( half, -half, 0, 1, 0);
            tess.addVertexWithUV(-half, -half, 0, 0, 0);
            tess.draw();

            // 文字（固定像素大小）
            String text = String.format("[GPS %.1fm]", player.getDistance((float)gx, (float)gy, (float)gz));
            int color = inLock ? 0xFF0000 : 0x00FF00;
            GL11.glTranslatef(0.0F, ICON_SIZE_PX * 0.5f + 8.0f, 0.0F);
            int fw = mc.fontRenderer.getStringWidth(text);
            mc.fontRenderer.drawString(text, -fw / 2, 0, color, false);

            // 还原
            GL11.glEnable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glDepthMask(true);
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glColor4f(1F, 1F, 1F, 1F);
        }
        GL11.glPopMatrix();
    }

    private float getViewRollDeg(Minecraft mc, MCH_EntityAircraft ac, float partialTicks) {
        return -ac.rotationRoll;
    }


}
