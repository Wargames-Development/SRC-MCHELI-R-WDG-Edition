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

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        // Only proceed after the full HUD has rendered
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return;

        // === Basic context setup ===
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.thePlayer;
        World world = mc.theWorld;
        ScaledResolution sc = new ScaledResolution(Minecraft.getMinecraft(), Minecraft.getMinecraft().displayWidth, Minecraft.getMinecraft().displayHeight);
        if (player == null || world == null) return;
        if (mc.gameSettings.thirdPersonView != 0) return; // Only render in first-person view

        // === Retrieve the player’s current aircraft ===
        MCH_EntityAircraft ac = null;
        if(player.ridingEntity instanceof MCH_EntityAircraft) {
            ac = (MCH_EntityAircraft)player.ridingEntity;
        } else if(player.ridingEntity instanceof MCH_EntitySeat) {
            ac = ((MCH_EntitySeat)player.ridingEntity).getParent();
        } else if(player.ridingEntity instanceof MCH_EntityUavStation) {
            ac = ((MCH_EntityUavStation)player.ridingEntity).getControlAircract();
        }

        // Skip if the player isn’t controlling a valid aircraft with a weapon
        if(ac == null || ac.getCurrentWeapon(player) == null || ac.getCurrentWeapon(player).getCurrentWeapon() == null)
            return;

        MCH_WeaponInfo wi = ac.getCurrentWeapon(player).getCurrentWeapon().getInfo();
        if(wi == null || !wi.enableBVR) return; // Only render for BVR-capable weapons

        // === Begin rendering overlay ===
        GL11.glPushMatrix();
        {
            // Copy the latest synchronized entity list from server state
            List<MCH_EntityInfo> entities = new ArrayList<>(getServerLoadedEntity());
            for (MCH_EntityInfo entity : entities) {
                if (!canRenderEntity(entity, player, wi)) continue;

                // Interpolate position between ticks for smooth rendering
                double x = interpolate(entity.posX, entity.lastTickPosX, event.partialTicks);
                double y = interpolate(entity.posY, entity.lastTickPosY, event.partialTicks) + 1;
                double z = interpolate(entity.posZ, entity.lastTickPosZ, event.partialTicks);

                Vec3 entityPos = Vec3.createVectorHelper(x, y, z);
                double[] screenPos = worldToScreen(new Vector3f(entityPos));

                double sx = screenPos[0];
                double sy = screenPos[1];
                boolean lock = false;

                if (sx > 0 && sy > 0) {
                    float alpha = 0.1f; // base transparency
                    double ox = screenPos[2];
                    double oy = screenPos[3];
                    double distScreen = ox * ox + oy * oy;

                    // === Proximity-based scaling of alpha and lock acquisition ===
                    if(distScreen < Math.pow(0.038 * sc.getScaledHeight(), 2)) { // < 20px radius
                        alpha = 1f;
                        currentLockedEntities.put(entity.entityId, entity);
                        lock = true;
                    } else if (distScreen < Math.pow(0.076 * sc.getScaledHeight(), 2)) { // < 40px
                        alpha = 1f;
                    } else if (distScreen < Math.pow(0.152 * sc.getScaledHeight(), 2)) { // < 80px
                        alpha = 0.8f;
                    } else if (distScreen < Math.pow(0.228 * sc.getScaledHeight(), 2)) { // < 120px
                        alpha = 0.6f;
                    } else if (distScreen < Math.pow(0.288 * sc.getScaledHeight(), 2)) { // < 150px
                        alpha = 0.4f;
                    } else if (distScreen > Math.pow(0.384 * sc.getScaledHeight(), 2)) { // > 200px, clamp on HUD edge
                        double distance = Math.sqrt(distScreen);
                        double ratio = 200 / distance;
                        sx = sc.getScaledWidth() / 2.0 + ox * ratio;
                        sy = sc.getScaledHeight() / 2.0 + oy * ratio;
                        alpha = 0.2f;
                    }

                    // === Missile markers ===
                    if(entity.entityClassName.contains("MCH_EntityAAMissile")) {
                        if(player.getDistanceSq(x, y, z) < 1000 * 1000 && alpha > 0.4) {
                            drawMSLMarker(sx, sy, true, alpha);
                            Minecraft.getMinecraft().fontRenderer.drawString(
                                    String.format("[MSL %.1fm]", player.getDistance(x, y, z)),
                                    (int) (sx - 20), (int) (sy + 12), 0xFF0000
                            );
                        }
                    } else {
                        // === Regular aircraft/entity marker ===
                        drawEntityMarker(sx, sy, lock, alpha);
                        if(alpha >= 0.6f) {
                            Minecraft.getMinecraft().fontRenderer.drawString(
                                    String.format("[%s %.1fm]", ac.getNameOnMyRadar(entity) , player.getDistance(x, y, z)),
                                    (int) (sx - 20), (int) (sy + 12),
                                    lock ? 0xFF0000 : 0x00FF00 // red if locked, green otherwise
                            );
                        }
                    }
                    // Clear locks if the entity is no longer under lock range
                    if(!lock) {
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

    private double[] worldToScreen(Vector3f pos) {
        EntityClientPlayerMP player = Minecraft.getMinecraft().thePlayer;
        Vector3f playerPos = new Vector3f(RenderManager.renderPosX, RenderManager.renderPosY, RenderManager.renderPosZ);
        Vector3f rPos = new Vector3f();
        Vector3f.sub(pos, playerPos, rPos);
        Vector3f lookVec = new Vector3f(player.getLookVec());
        if(Math.toDegrees(Vector3f.angle(rPos, lookVec)) > 45) {
            return new double[] {-1, -1, -1, -1};
        }
        // Calculate camera coordinate system
        Vector3f worldUp = new Vector3f(0, 1, 0);
        Vector3f R = new Vector3f();
        Vector3f.cross(lookVec, worldUp, R);
        // Handle zero cross-product case (e.g., looking straight up or down)
        if (R.lengthSquared() < 1e-5) {
            float yawRad = (float) Math.toRadians(player.rotationYaw + 90);
            R.set((float) Math.cos(yawRad), 0, (float) -Math.sin(yawRad));
        }
        R.normalise();
        Vector3f U = new Vector3f();
        Vector3f.cross(R, lookVec, U);
        U.normalise();
        // Decompose relative coordinates into camera axes
        float dx = Vector3f.dot(rPos, R);
        float dy = Vector3f.dot(rPos, U);
        float dz = Vector3f.dot(rPos, lookVec);
        if (dz <= 0) return new double[]{-1, -1, -1, -1};
        // Get display parameters
        ScaledResolution sc = new ScaledResolution(Minecraft.getMinecraft(), Minecraft.getMinecraft().displayWidth, Minecraft.getMinecraft().displayHeight);
        float fov = Minecraft.getMinecraft().gameSettings.fovSetting;
        double tanHalfFov = Math.tan(Math.toRadians(fov) * 0.5);
        double aspect = (double) sc.getScaledWidth() / sc.getScaledHeight();
        // Perspective projection calculation
        double xProj = (dx / dz) / (aspect * tanHalfFov);
        double yProj = (dy / dz) / tanHalfFov;
        // Convert to screen coordinates
        double screenX = sc.getScaledWidth() / 2.0 + xProj * (sc.getScaledWidth() / 2.0);
        double screenY = sc.getScaledHeight() / 2.0 - yProj * (sc.getScaledHeight() / 2.0);
        return new double[]{screenX, screenY, screenX - sc.getScaledWidth() / 2.0, screenY - sc.getScaledHeight() / 2.0};
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

    // Modified rendering state settings
    private void prepareRenderState(boolean lock, float alpha) {
        GL11.glEnable(3042);
        if(lock) {
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


    //    public static Vector4f Multiply(Vector4f vec, float[] mat) {
//        return new Vector4f(
//                vec.x * mat[0] + vec.y * mat[4] + vec.z * mat[8] + vec.w * mat[12],
//                vec.x * mat[1] + vec.y * mat[5] + vec.z * mat[9] + vec.w * mat[13],
//                vec.x * mat[2] + vec.y * mat[6] + vec.z * mat[10] + vec.w * mat[14],
//                vec.x * mat[3] + vec.y * mat[7] + vec.z * mat[11] + vec.w * mat[15]
//        );
//    }
//
//    public static float[] bufferToArray(FloatBuffer buffer) {
//        if (buffer.capacity() < 16) {
//            throw new IllegalArgumentException("FloatBuffer must have at least 16 elements.");
//        }
//        int originalPosition = buffer.position(); // 保存原始位置
//        buffer.position(0); // 重置到起始位置
//        float[] array = new float[16];
//        buffer.get(array); // 读取16个元素
//        buffer.position(originalPosition); // 恢复原始位置
//        return array;
//    }
//
//    public static int[] bufferToArray(IntBuffer buffer) {
//        if (buffer.capacity() < 16) {
//            throw new IllegalArgumentException("IntBuffer must have at least 16 elements.");
//        }
//        int originalPosition = buffer.position();
//        buffer.position(0);
//        int[] array = new int[16];
//        buffer.get(array);
//        buffer.position(originalPosition);
//        return array;
//    }

//    private double[] worldToScreen(Vec3 pos) {
//        Vector4f clipSpacePos = Multiply(new Vector4f((float) pos.xCoord, (float) pos.yCoord, (float) pos.zCoord, 1.0f), bufferToArray(MCH_RenderUtil.modelview));
//        clipSpacePos = Multiply(clipSpacePos, bufferToArray(MCH_RenderUtil.projection));
//        Vector3f ndcSpacePos = new Vector3f(clipSpacePos.x / clipSpacePos.w, clipSpacePos.y / clipSpacePos.w, clipSpacePos.z / clipSpacePos.w);
//        int[] viewPort = bufferToArray(MCH_RenderUtil.viewport);
//        if (ndcSpacePos.z < -1.0f || ndcSpacePos.z > 1.0f) {
//            return new double[]{0, 0};
//        }
//        return new double[] {
//                ((ndcSpacePos.x + 1.0f) / 2.0f) * viewPort[2],
//                ((1.0f - ndcSpacePos.y) / 2.0f) * viewPort[3]
//        };
//    }

}
