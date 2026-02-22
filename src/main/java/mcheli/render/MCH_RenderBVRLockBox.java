package mcheli.render;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import mcheli.MCH_EntityInfo;
import mcheli.MCH_EntityInfoClientTracker;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.aircraft.MCH_EntitySeat;
import mcheli.uav.MCH_EntityUavStation;
import mcheli.vector.Vector3f;
import mcheli.weapon.MCH_WeaponInfo;
import mcheli.weapon.MCH_WeaponInfoManager;
import mcheli.wrapper.W_MOD;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MCH_RenderBVRLockBox {
    private static final ResourceLocation FRAME = new ResourceLocation(W_MOD.DOMAIN, "textures/BVRLockBox.png");
    private static final ResourceLocation MSL = new ResourceLocation(W_MOD.DOMAIN, "textures/MSL.png");
    private static final int BOX_SIZE = 24;
    public static Map<Integer, MCH_EntityInfo> currentLockedEntities = new HashMap<>();

    // Best “hard lock” candidate this frame (red box target)
    public static volatile MCH_EntityInfo bestLockedEntity = null;
    public static volatile long bestLockedEntityTimeMs = 0L;

    public static double[] worldToScreen(Vector3f pos, float partialTicks) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityClientPlayerMP viewer = mc.thePlayer;
        if (viewer == null) return new double[]{-1, -1, -1, -1};
        Vector3f camPos = new Vector3f(
            (float) RenderManager.renderPosX,
            (float) RenderManager.renderPosY,
            (float) RenderManager.renderPosZ
        );
        Vector3f rPos = new Vector3f();
        Vector3f.sub(pos, camPos, rPos);
        Vec3 fwdV3 = viewer.getLook(partialTicks);
        Vector3f F = new Vector3f((float) fwdV3.xCoord, (float) fwdV3.yCoord, (float) fwdV3.zCoord);
        F.normalise();
        Vector3f worldUp = new Vector3f(0, 1, 0);
        Vector3f R = new Vector3f();
        Vector3f.cross(F, worldUp, R);
        if (R.lengthSquared() < 1e-5f) {
            float yawRad = (float) Math.toRadians(viewer.rotationYaw + 90.0f);
            R.set((float) Math.cos(yawRad), 0f, (float) -Math.sin(yawRad));
        }
        R.normalise();
        Vector3f U = new Vector3f();
        Vector3f.cross(R, F, U);
        U.normalise();
        float dx = Vector3f.dot(rPos, R);
        float dy = Vector3f.dot(rPos, U);
        float dz = Vector3f.dot(rPos, F);
        if (dz <= 0) return new double[]{-1, -1, -1, -1};
        double fovDeg = mc.gameSettings.fovSetting;
        double tanHalfFov = Math.tan(Math.toRadians(fovDeg) * 0.5);
        double aspect = (double) mc.displayWidth / (double) mc.displayHeight;
        double ndcX = (dx / dz) / (aspect * tanHalfFov);
        double ndcY = (dy / dz) / (tanHalfFov);
        ScaledResolution sc = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
        double cx = sc.getScaledWidth() * 0.5;
        double cy = sc.getScaledHeight() * 0.5;
        double screenX = cx + ndcX * cx;
        double screenY = cy - ndcY * cy;
        return new double[]{
            screenX, screenY,
            screenX - cx, screenY - cy
        };
    }

    private static double calculateAngle(Entity viewer, double x, double y, double z) {
        double dx = x - viewer.posX;
        double dy = y - viewer.posY;
        double dz = z - viewer.posZ;
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (dist < 1e-6) {
            return 0.0;
        }
        dx /= dist;
        dy /= dist;
        dz /= dist;
        double yawRad = Math.toRadians(viewer.rotationYaw);
        double pitchRad = Math.toRadians(viewer.rotationPitch);
        double fx = -Math.sin(yawRad) * Math.cos(pitchRad);
        double fy = -Math.sin(pitchRad);
        double fz = Math.cos(yawRad) * Math.cos(pitchRad);
        double fLen = Math.sqrt(fx * fx + fy * fy + fz * fz);
        if (fLen > 1e-6) {
            fx /= fLen;
            fy /= fLen;
            fz /= fLen;
        }
        double dot = dx * fx + dy * fy + dz * fz;
        dot = Math.max(-1.0, Math.min(1.0, dot));
        return Math.toDegrees(Math.acos(dot));
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.thePlayer;
        if (player == null || mc.theWorld == null) return;
        if (mc.gameSettings.thirdPersonView != 0) return;
        MCH_EntityAircraft ac = null;
        if (player.ridingEntity instanceof MCH_EntityAircraft) {
            ac = (MCH_EntityAircraft) player.ridingEntity;
        } else if (player.ridingEntity instanceof MCH_EntitySeat) {
            ac = ((MCH_EntitySeat) player.ridingEntity).getParent();
        } else if (player.ridingEntity instanceof MCH_EntityUavStation) {
            ac = ((MCH_EntityUavStation) player.ridingEntity).getControlAircract();
        }
        if (ac == null || ac.getCurrentWeapon(player) == null || ac.getCurrentWeapon(player).getCurrentWeapon() == null)
            return;
        MCH_WeaponInfo wi = ac.getCurrentWeapon(player).getCurrentWeapon().getInfo();
        if (wi == null || !wi.enableBVR) return;
        // Clear per-frame state ONCE (the original code clears inside the loop, which breaks BVR selection)
        currentLockedEntities.clear();
        bestLockedEntity = null;
        bestLockedEntityTimeMs = 0L;

        double bestAngle = 9999.0;
        double bestDistSq = 9.9e18;

        RenderManager rm = RenderManager.instance;
        final double camX = rm.viewerPosX;
        final double camY = rm.viewerPosY;
        final double camZ = rm.viewerPosZ;
        float partialTicks = event.partialTicks;
        ScaledResolution sc = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
        double fovDeg = mc.gameSettings.fovSetting;
        double fovRad = Math.toRadians(fovDeg);
        float rollDeg = getViewRollDeg(mc, ac, partialTicks);
        List<MCH_EntityInfo> entities = new ArrayList<>(getServerLoadedEntity());
        for (MCH_EntityInfo entity : entities) {
            if (!canRenderEntity(entity, player, wi)) continue;
            if(ac.jammingTick > 0) {
                continue;
            }
            double gx = interpolate(entity.posX, entity.lastTickPosX, partialTicks);
            double gy = interpolate(entity.posY, entity.lastTickPosY, partialTicks) + 1;
            double gz = interpolate(entity.posZ, entity.lastTickPosZ, partialTicks);
            double x = gx - camX;
            double y = gy - camY;
            double z = gz - camZ;
            double distSq = (gx - ac.posX) * (gx - ac.posX) + (gy - ac.posY) * (gy - ac.posY) + (gz - ac.posZ) * (gz - ac.posZ);
            double dist = Math.sqrt(distSq);
            double angle = calculateAngle(wi.enableHMS ? player : ac, gx, gy, gz);
            MCH_RWRResult rwrResult = getTargetTypeOnRadar(entity, ac);
            boolean isMSL = isMissile(entity.entityClassName);
            boolean lock = false;
            float alpha = 0.4f;
            if (angle <= 90) {
                alpha = 1.0f;

                if (!isMSL) currentLockedEntities.put(entity.entityId, entity);

                // Hard-lock condition (red box)
                if (distSq <= wi.maxLockOnRange * wi.maxLockOnRange && angle <= wi.maxLockOnAngle) {
                    lock = true;

                    // Track best hard-lock target ONLY (smallest angle, then nearest)
                    if (!isMSL) {
                        if (angle < bestAngle || (Math.abs(angle - bestAngle) < 1e-6 && distSq < bestDistSq)) {
                            bestAngle = angle;
                            bestDistSq = distSq;
                            bestLockedEntity = entity;
                            bestLockedEntityTimeMs = System.currentTimeMillis();
                        }
                    }
                }
            }
            else if (angle <= 100.0) alpha = 1.0f;
            else if (angle <= 110.0) alpha = 0.8f;
            else if (angle <= 120.0) alpha = 0.6f;
            if (isMSL && dist >= 1000) continue;
            double vdist = Math.sqrt(x * x + y * y + z * z);
            if (vdist < 1e-4) continue;
            float sPerPixel = (float) ((2.0 * vdist * Math.tan(fovRad * 0.5)) / sc.getScaledHeight_double());
            String text;
            int color;
            if("".equals(rwrResult.name)) continue;
            if (isMSL) {
                text = String.format("[%s %.1fm]", rwrResult.name, dist);
                color = 0xFF0000;
            } else {
                text = String.format("[%s %.1fm]", rwrResult.name, dist);
                color = lock ? 0xFF0000 : 0x00FF00;
            }
            boolean drawText = isMSL || (alpha >= 0.6f);
            GL11.glPushMatrix();
            {
                GL11.glTranslated(x, y + 0.2, z);
                GL11.glRotatef(-rm.playerViewY, 0.0F, 1.0F, 0.0F);
                GL11.glRotatef(rm.playerViewX, 1.0F, 0.0F, 0.0F);
                GL11.glRotatef(-rollDeg, 0.0F, 0.0F, 1.0F);
                GL11.glScalef(-sPerPixel, -sPerPixel, sPerPixel);
                GL11.glDisable(GL11.GL_DEPTH_TEST);
                GL11.glDepthMask(false);
                GL11.glEnable(GL11.GL_BLEND);
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                GL11.glDisable(GL11.GL_LIGHTING);
                if (isMSL || lock) GL11.glColor4f(1.0F, 0F, 0F, alpha);
                else GL11.glColor4f(0F, 1.0F, 0F, alpha);
                Minecraft.getMinecraft().getTextureManager().bindTexture(isMSL ? MSL : FRAME);
                Tessellator tess = Tessellator.instance;
                float half = BOX_SIZE * 0.5f;
                tess.startDrawingQuads();
                tess.addVertexWithUV(-half, half, 0, 0, 1);
                tess.addVertexWithUV(half, half, 0, 1, 1);
                tess.addVertexWithUV(half, -half, 0, 1, 0);
                tess.addVertexWithUV(-half, -half, 0, 0, 0);
                tess.draw();
                if (drawText) {
                    GL11.glTranslatef(0.0F, BOX_SIZE * 0.5f + 8.0f, 0.0F);
                    int fw = mc.fontRenderer.getStringWidth(text);
                    mc.fontRenderer.drawString(text, -fw / 2, 0, color, false);
                }
                GL11.glEnable(GL11.GL_LIGHTING);
                GL11.glDisable(GL11.GL_BLEND);
                GL11.glDepthMask(true);
                GL11.glEnable(GL11.GL_DEPTH_TEST);
                GL11.glColor4f(1F, 1F, 1F, 1F);
            }
            GL11.glPopMatrix();
//            if (!lock) {
//                currentLockedEntities.clear();
//            }
        }
    }

    public List<MCH_EntityInfo> getServerLoadedEntity() {
        return new ArrayList<>(MCH_EntityInfoClientTracker.getAllTrackedEntities());
    }

    private boolean canRenderEntity(MCH_EntityInfo entity, EntityPlayer player, MCH_WeaponInfo wi) {
        boolean result = false;
        double distSq = entity.getDistanceSqToEntity(player);
        if (entity.entityClassName.contains("MCP_EntityPlane")) {
            if (entity.getDistanceSqToEntity(player) > wi.minRangeBVR * wi.minRangeBVR) {
                return true;
            }
        } else if (entity.entityClassName.contains("MCH_EntityHeli")) {
            if (entity.getDistanceSqToEntity(player) > wi.minRangeBVR * wi.minRangeBVR) {
                return true;
            }
        } else if (entity.entityClassName.contains("MCH_EntityChaff") && wi.isRadarMissile) {
            if (entity.getDistanceSqToEntity(player) > wi.minRangeBVR * wi.minRangeBVR) {
                return true;
            }
        } else if (isMissile(entity.entityClassName) && distSq > 20 * 20 && distSq < 300 * 300) {
            return true;
        }
        return result;
    }

    private double interpolate(double now, double old, float partialTicks) {
        return old + (now - old) * partialTicks;
    }

    private float getViewRollDeg(Minecraft mc, MCH_EntityAircraft ac, float partialTicks) {
        return -ac.rotationRoll;
    }

    public MCH_RWRResult getTargetTypeOnRadar(MCH_EntityInfo entity, MCH_EntityAircraft ac) {
        int color = 0x00FF00;
        switch (ac.getAcInfo().rwrType) {
            case DIGITAL: {
                if (isVehicle(entity.entityClassName)) {
                    return new MCH_RWRResult(ac.getNameOnMyRadar(entity), color);
                } else if (isMissile(entity.entityClassName)) {
                    MCH_WeaponInfo wi = MCH_WeaponInfoManager.get(entity.entityName);
                    if (wi != null) {
                        return new MCH_RWRResult(wi.nameOnRWR, 0xFF0000);
                    }
                }
            }
        }
        return new MCH_RWRResult("?", 0x00FF00);
    }

    public boolean isVehicle(String className) {
        return className.contains("MCH_EntityHeli")
            || className.contains("MCP_EntityPlane")
            || className.contains("MCH_EntityTank")
            || className.contains("MCH_EntityVehicle");
    }

    public boolean isMissile(String className) {
        return className.contains("MCH_EntityAAMissile")
            || className.contains("MCH_EntityASMissile")
            || className.contains("MCH_EntityATMissile")
            || className.contains("MCH_EntityTvMissile");
    }
}
