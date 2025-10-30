package mcheli.render;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import mcheli.MCH_EntityInfo;
import mcheli.MCH_EntityInfoClientTracker;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.aircraft.MCH_EntitySeat;
import mcheli.uav.MCH_EntityUavStation;
import mcheli.vector.Vector3f;
import mcheli.weapon.MCH_WeaponInfo;
import mcheli.wrapper.W_MOD;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
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

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return;

        //获取基本信息
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.thePlayer;
        World world = mc.theWorld;
        ScaledResolution sc = new ScaledResolution(Minecraft.getMinecraft(), Minecraft.getMinecraft().displayWidth, Minecraft.getMinecraft().displayHeight);
        if (player == null || world == null) return;
        if (mc.gameSettings.thirdPersonView != 0) return;

        //获取玩家机载武器
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

        //开始渲染
        GL11.glPushMatrix();
        {
            List<MCH_EntityInfo> entities = new ArrayList<>(getServerLoadedEntity());
            for (MCH_EntityInfo entity : entities) {
                if (!canRenderEntity(entity, player, wi)) continue;
                double x = interpolate(entity.posX, entity.lastTickPosX, event.partialTicks);
                double y = interpolate(entity.posY, entity.lastTickPosY, event.partialTicks) + 1;
                double z = interpolate(entity.posZ, entity.lastTickPosZ, event.partialTicks);
                Vec3 entityPos = Vec3.createVectorHelper(x, y, z);
                double[] screenPos = worldToScreen(new Vector3f(entityPos), event.partialTicks);
                double sx = screenPos[0];
                double sy = screenPos[1];
                boolean lock = false;
                if (sx > 0 && sy > 0) {
                    float alpha = 0.1f;
                    double ox = screenPos[2];
                    double oy = screenPos[3];
                    double distScreen = ox * ox + oy * oy;
                    if (distScreen < Math.pow(0.038 * sc.getScaledHeight(), 2)) { // 20
                        alpha = 1f;
                        currentLockedEntities.put(entity.entityId, entity);
                        lock = true;
                    } else if (distScreen < Math.pow(0.076 * sc.getScaledHeight(), 2)) { // 40
                        alpha = 1f;
                    } else if (distScreen < Math.pow(0.152 * sc.getScaledHeight(), 2)) { // 80
                        alpha = 0.8f;
                    } else if (distScreen < Math.pow(0.228 * sc.getScaledHeight(), 2)) { // 120
                        alpha = 0.6f;
                    } else if (distScreen < Math.pow(0.288 * sc.getScaledHeight(), 2)) { // 150
                        alpha = 0.4f;
                    } else if (distScreen > Math.pow(0.384 * sc.getScaledHeight(), 2)) { // 200
                        double distance = Math.sqrt(distScreen);
                        double ratio = 200 / distance;
                        sx = sc.getScaledWidth() / 2.0 + ox * ratio;
                        sy = sc.getScaledHeight() / 2.0 + oy * ratio;
                        alpha = 0.2f;
                    }
                    if (entity.entityClassName.contains("MCH_EntityAAMissile")) {
                        if (player.getDistanceSq(x, y, z) < 1000 * 1000 && alpha > 0.4) {
                            drawMSLMarker(sx, sy, true, alpha);
                            Minecraft.getMinecraft().fontRenderer.drawString(
                                String.format("[MSL %.1fm]", player.getDistance(x, y, z)),
                                (int) (sx - 20), (int) (sy + 12), 0xFF0000
                            );
                        }
                    } else {
                        drawEntityMarker(sx, sy, lock, alpha);
                        if (alpha >= 0.6f) {
                            Minecraft.getMinecraft().fontRenderer.drawString(
                                String.format("[%s %.1fm]", ac.getNameOnMyRadar(entity), player.getDistance(x, y, z)),
                                (int) (sx - 20), (int) (sy + 12),
                                lock ? 0xFF0000 : 0x00FF00
                            );
                        }
                    }
                    if (!lock) {
                        currentLockedEntities.clear();
                    }
                }
            }
        }
        GL11.glPopMatrix();
    }

    public List<MCH_EntityInfo> getServerLoadedEntity() {
        return new ArrayList<>(MCH_EntityInfoClientTracker.getAllTrackedEntities());
    }

    private boolean canRenderEntity(MCH_EntityInfo entity, EntityPlayer player, MCH_WeaponInfo wi) {
        boolean result = false;
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
        } else if (entity.entityClassName.contains("MCH_EntityAAMissile") && entity.getDistanceSqToEntity(player) > 100 * 100) {
            return true;
        }
        return result;
    }

    private void drawEntityMarker(double x, double y, boolean lock, float alpha) {
        prepareRenderState(lock, alpha);
        Minecraft.getMinecraft().renderEngine.bindTexture(FRAME);
        Tessellator tess = Tessellator.instance;
        tess.startDrawingQuads();
        double halfSize = BOX_SIZE / 2.0;
        tess.addVertexWithUV(x - halfSize, y + halfSize, 0, 0, 1);
        tess.addVertexWithUV(x + halfSize, y + halfSize, 0, 1, 1);
        tess.addVertexWithUV(x + halfSize, y - halfSize, 0, 1, 0);
        tess.addVertexWithUV(x - halfSize, y - halfSize, 0, 0, 0);
        tess.draw();
        restoreRenderState();
    }

    private void drawMSLMarker(double x, double y, boolean lock, float alpha) {
        prepareRenderState(lock, alpha);
        Minecraft.getMinecraft().renderEngine.bindTexture(MSL);
        Tessellator tess = Tessellator.instance;
        tess.startDrawingQuads();
        double halfSize = BOX_SIZE / 2.0;
        tess.addVertexWithUV(x - halfSize, y + halfSize, 0, 0, 1);
        tess.addVertexWithUV(x + halfSize, y + halfSize, 0, 1, 1);
        tess.addVertexWithUV(x + halfSize, y - halfSize, 0, 1, 0);
        tess.addVertexWithUV(x - halfSize, y - halfSize, 0, 0, 0);
        tess.draw();
        restoreRenderState();
    }

    private double interpolate(double now, double old, float partialTicks) {
        return old + (now - old) * partialTicks;
    }

    private void prepareRenderState(boolean lock, float alpha) {
        GL11.glEnable(3042);
        if (lock) {
            GL11.glColor4f(1.0F, 0F, 0F, 1.0F);
        } else {
            GL11.glColor4f(0F, 1.0F, 0F, alpha);
        }
        GL11.glBlendFunc(770, 771);
    }

    private void restoreRenderState() {
        int srcBlend = GL11.glGetInteger(3041);
        int dstBlend = GL11.glGetInteger(3040);
        GL11.glBlendFunc(srcBlend, dstBlend);
        GL11.glDisable(3042);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

}
