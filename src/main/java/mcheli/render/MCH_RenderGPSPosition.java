package mcheli.render;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.eventhandler.EventPriority;
import mcheli.lweapon.MCH_ClientLightWeaponTickHandler;
import mcheli.lweapon.MCH_ItemLightWeaponBase;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.aircraft.MCH_EntitySeat;
import mcheli.uav.MCH_EntityUavStation;
import mcheli.weapon.MCH_GPSPosition;
import mcheli.weapon.MCH_LaserGuidanceSystem;
import mcheli.weapon.MCH_LaserStateStore;
import mcheli.weapon.MCH_WeaponBase;
import mcheli.weapon.MCH_WeaponInfo;
import mcheli.weapon.MCH_WeaponSet;
import mcheli.wrapper.W_MOD;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import java.nio.FloatBuffer;

public class MCH_RenderGPSPosition {

    private static final ResourceLocation GPS_POS = new ResourceLocation(W_MOD.DOMAIN, "textures/gpsposition.png");
    private static final int ICON_SIZE_PX = 24; // 以像素为基准的目标尺寸
    private static final double LOCK_ALIGNMENT_COS = Math.cos(Math.toRadians(1.5D));
    private double cachedDistance = Double.NaN;
    private boolean cachedGps;
    private String cachedDistanceText;
    private final FloatBuffer worldModelView = BufferUtils.createFloatBuffer(16);
    private final FloatBuffer worldProjection = BufferUtils.createFloatBuffer(16);
    private World projectionWorld;
    private boolean projectionReady;
    private double originX,originY,originZ;
    private final double[] eyeOffset=new double[3];

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        if (MCH_FMURThermal.isHeatPass() || MCH_FMURThermal.isParticlePass()) return;
        Minecraft mc = Minecraft.getMinecraft();
        this.projectionReady = false;
        this.projectionWorld = mc.theWorld;
        if (mc.thePlayer == null || mc.theWorld == null) return;
        this.worldModelView.clear();
        this.worldProjection.clear();
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, this.worldModelView);
        GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, this.worldProjection);
        this.originX=RenderManager.renderPosX;this.originY=RenderManager.renderPosY;this.originZ=RenderManager.renderPosZ;
        if(!MCH_MarkerProjection.eye(this.worldModelView,this.eyeOffset)) return;
        this.projectionReady = true;
        this.renderPositionMarker(event.partialTicks, false);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return;
        Minecraft mc = Minecraft.getMinecraft();
        boolean ready = this.projectionReady;
        World capturedWorld = this.projectionWorld;
        this.projectionReady = false;
        this.projectionWorld = null;
        if (!ready || mc.thePlayer == null || mc.theWorld == null || capturedWorld != mc.theWorld
                || mc.gameSettings.hideGUI) return;

        // Thermal replaces the world at HELMET. Draw designation graphics afterwards,
        // using the real world camera matrices so zoom, roll and long-range aim still match.
        int matrixMode = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT
                | GL11.GL_CURRENT_BIT | GL11.GL_TEXTURE_BIT | GL11.GL_LINE_BIT);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadMatrix(this.worldProjection);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadMatrix(this.worldModelView);
        try {
            mcheli.weapon.MCH_RenderLockBox.renderGuidanceHUD();
            this.renderPositionMarker(event.partialTicks, true);
        } finally {
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glPopMatrix();
            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glPopMatrix();
            GL11.glMatrixMode(matrixMode);
            GL11.glPopAttrib();
        }
    }

    private void renderPositionMarker(float partialTicks, boolean laserOverlay) {
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
        boolean renderGps = !laserOverlay && isAircraftGpsWeaponContext(player, ac);
        boolean renderLzr = laserOverlay && isHandheldLaserContext(player, ac);
        if (!renderGps && !renderLzr) {
            return;
        }

        double gx;
        double gy;
        double gz;
        if (renderGps) {
            MCH_GPSPosition gps = MCH_GPSPosition.currentClientGPSPosition;
            if (gps == null || !gps.isActive()) {
                return;
            }
            gx = gps.x;
            gy = gps.y;
            gz = gps.z;
        } else {
            MCH_LaserStateStore.LaserState handheld = MCH_LaserStateStore.getClientState(player.getEntityId(), MCH_LaserStateStore.SOURCE_HANDHELD);
            if (handheld == null || !handheld.active) {
                return;
            }
            gx = handheld.x;
            gy = handheld.y;
            gz = handheld.z;
        }

        // —— 世界/相机坐标 ——
        RenderManager rm = RenderManager.instance;
        final double camX = this.originX, camY = this.originY, camZ = this.originZ;
        final double x = gx - camX, y = gy - camY, z = gz - camZ;

        // —— 视线夹角用于透明度/锁定 ——
        double px = player.prevPosX + (player.posX - player.prevPosX) * partialTicks;
        double py = player.prevPosY + (player.posY - player.prevPosY) * partialTicks + player.getEyeHeight();
        double pz = player.prevPosZ + (player.posZ - player.prevPosZ) * partialTicks;

        double vx = gx - px, vy = gy - py, vz = gz - pz;
        double vlen = Math.sqrt(vx*vx + vy*vy + vz*vz);
        if (vlen < 1e-4) return;
        vx /= vlen; vy /= vlen; vz /= vlen;

        Vec3 look = player.getLookVec();
        double dot = vx * look.xCoord + vy * look.yCoord + vz * look.zCoord;
        boolean inLock = dot >= LOCK_ALIGNMENT_COS;
        float alpha = 1.0F;

        // —— 基于 FOV 的恒定像素大小 ——
        ScaledResolution sc = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
        double ex=x-eyeOffset[0],ey=y+0.2-eyeOffset[1],ez=z-eyeOffset[2];
        double dist = Math.sqrt(ex*ex + ey*ey + ez*ez);
        double projectionScale = markerProjectionScale(dist, mc.gameSettings.renderDistanceChunks);
        if (!Double.isFinite(projectionScale)) return;
        double drawX=MCH_MarkerProjection.scaled(x,eyeOffset[0],projectionScale);
        double drawY=MCH_MarkerProjection.scaled(y+0.2,eyeOffset[1],projectionScale);
        double drawZ=MCH_MarkerProjection.scaled(z,eyeOffset[2],projectionScale);
        double depth=MCH_MarkerProjection.depth(worldModelView,drawX,drawY,drawZ);
        double yScale=Math.abs(worldProjection.get(5));
        if(depth<=0 || yScale<=1e-6) return;
        float sPerPixel = (float)(2.0 * depth / (yScale * sc.getScaledHeight_double()));

        // —— 取得视角 roll（度）并在 billboard 后抵消 ——
        float rollDeg = getViewRollDeg(mc, ac, partialTicks); // 正负方向以 MCH 的右手坐标为准
        // 如果无法获取则返回 0

        // —— 渲染 ——
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT
                | GL11.GL_CURRENT_BIT | GL11.GL_TEXTURE_BIT | GL11.GL_LINE_BIT);
        GL11.glPushMatrix();
        try {
            // Equal position/icon scaling preserves direction and pixel size, like vehicle LODs.
            GL11.glTranslated(drawX,drawY,drawZ);

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
            GL11.glDisable(GL11.GL_FOG);

            Tessellator tess = Tessellator.instance;
            float half = ICON_SIZE_PX * 0.5f;
            if (renderGps) {
                GL11.glEnable(GL11.GL_TEXTURE_2D);
                GL11.glColor4f(inLock ? 1.0F : 0.0F, inLock ? 0.0F : 1.0F, 0.0F, inLock ? 1.0F : alpha);
                mc.getTextureManager().bindTexture(GPS_POS);
                tess.startDrawingQuads();
                tess.addVertexWithUV(-half, half, 0.0D, 0.0D, 1.0D);
                tess.addVertexWithUV(half, half, 0.0D, 1.0D, 1.0D);
                tess.addVertexWithUV(half, -half, 0.0D, 1.0D, 0.0D);
                tess.addVertexWithUV(-half, -half, 0.0D, 0.0D, 0.0D);
                tess.draw();
            } else {
                GL11.glDisable(GL11.GL_TEXTURE_2D);
                GL11.glLineWidth(2.0F);
                tess.startDrawing(2);
                tess.setColorRGBA_F(inLock ? 1.0F : 0.0F, inLock ? 0.0F : 1.0F, 0.0F, inLock ? 1.0F : alpha);
                tess.addVertex(-half - 1.0F, -half - 1.0F, 0.0D);
                tess.addVertex(-half - 1.0F, half + 1.0F, 0.0D);
                tess.addVertex(half + 1.0F, half + 1.0F, 0.0D);
                tess.addVertex(half + 1.0F, -half - 1.0F, 0.0D);
                tess.draw();
                tess.startDrawing(1);
                tess.setColorRGBA_F(inLock ? 1.0F : 0.0F, inLock ? 0.0F : 1.0F, 0.0F, inLock ? 1.0F : alpha);
                tess.addVertex(-half, 0.0D, 0.0D);
                tess.addVertex(half, 0.0D, 0.0D);
                tess.addVertex(0.0D, -half, 0.0D);
                tess.addVertex(0.0D, half, 0.0D);
                tess.draw();
            }

            // 文字（固定像素大小）
            double distanceLabel = Math.round(player.getDistance((float)gx, (float)gy, (float)gz) * 10.0D) / 10.0D;
            if (distanceLabel != this.cachedDistance || renderGps != this.cachedGps) {
                this.cachedDistance = distanceLabel;
                this.cachedGps = renderGps;
                this.cachedDistanceText = String.format(renderGps ? "[GPS %.1fm]" : "[LZR %.1fm]", distanceLabel);
            }
            String text = this.cachedDistanceText;
            int color = inLock ? 0xFF0000 : 0x00FF00;
            GL11.glTranslatef(0.0F, ICON_SIZE_PX * 0.5f + 8.0f, 0.0F);
            int fw = mc.fontRenderer.getStringWidth(text);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            mc.fontRenderer.drawString(text, -fw / 2, 0, color, false);

        } finally {
            GL11.glPopMatrix();
            GL11.glPopAttrib();
        }
    }

    static double markerProjectionScale(double distance, int renderChunks) {
        if (!Double.isFinite(distance) || distance <= 0.0D) return Double.NaN;
        double transitionStart = Math.max(16.0D, (renderChunks > 0 ? renderChunks : 8) * 16.0D - 16.0D);
        double projected = MCH_CompressedDepthProjection.projected(distance, transitionStart);
        if (!Double.isFinite(projected)) projected = Math.min(distance, transitionStart);
        return projected / distance;
    }

    private float getViewRollDeg(Minecraft mc, MCH_EntityAircraft ac, float partialTicks) {
        return ac != null ? -ac.rotationRoll : 0.0F;
    }

    private boolean isAircraftGpsWeaponContext(EntityPlayer player, MCH_EntityAircraft ac) {
        if (player == null || ac == null) {
            return false;
        }
        MCH_WeaponSet ws = ac.getCurrentWeapon(player);
        if (ws == null) {
            return false;
        }
        MCH_WeaponInfo info = ws.getInfo();
        return info != null && info.isGPSMissile;
    }

    private boolean isHandheldLaserContext(EntityPlayer player, MCH_EntityAircraft ac) {
        if (player == null || ac != null) {
            return false;
        }
        ItemStack held = player.getHeldItem();
        if (held == null || !(held.getItem() instanceof MCH_ItemLightWeaponBase)) {
            return false;
        }
        MCH_WeaponBase weapon = MCH_ClientLightWeaponTickHandler.getCurrentWeapon();
        if (weapon == null || weapon.getInfo() == null) {
            return false;
        }
        if ("tvmissile".equalsIgnoreCase(weapon.getInfo().type)) {
            return weapon.getCurrentMode() == 1;
        }
        return weapon.getGuidanceSystem() instanceof MCH_LaserGuidanceSystem || weapon.getInfo().laserGuidance;
    }


}
