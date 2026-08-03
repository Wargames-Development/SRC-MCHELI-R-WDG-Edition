package mcheli.render;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mcheli.MCH_EntityInfo;
import mcheli.MCH_EntityInfoClientTracker;
import mcheli.MCH_EntityInfoManager;
import mcheli.aircraft.MCH_AircraftInfo;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.aircraft.MCH_RenderAircraft;
import mcheli.helicopter.MCH_EntityHeli;
import mcheli.helicopter.MCH_HeliInfoManager;
import mcheli.plane.MCP_EntityPlane;
import mcheli.plane.MCP_PlaneInfoManager;
import mcheli.tank.MCH_EntityTank;
import mcheli.tank.MCH_TankInfoManager;
import mcheli.vehicle.MCH_EntityVehicle;
import mcheli.vehicle.MCH_VehicleInfoManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import org.lwjgl.opengl.GL11;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/** Renders a presentation-only model for an aircraft contact absent from WorldClient. */
@SideOnly(Side.CLIENT)
public class MCH_RenderFarVehicle {

    private static final int DEFAULT_RENDER_DISTANCE_CHUNKS = 8;
    private static final double CHUNK_SIZE = 16.0D;
    private static final double TRANSITION_WIDTH = CHUNK_SIZE;
    private static final double POSITION_SMOOTHING_MILLIS = 100.0D;
    private static final double SNAP_DISTANCE_SQ = 64.0D * 64.0D;
    private static final double MAX_CONTACT_DISTANCE_SQ = MCH_EntityInfoManager.ENTITY_INFO_SYNC_RANGE
        * MCH_EntityInfoManager.ENTITY_INFO_SYNC_RANGE;

    private final Map<String, RenderDefinition> definitions = new HashMap<String, RenderDefinition>();
    private final Map<Integer, SmoothedPose> smoothedPoses = new HashMap<Integer, SmoothedPose>();
    private int renderFrame;

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null || mc.thePlayer == null) {
            this.smoothedPoses.clear();
            return;
        }

        Collection<MCH_EntityInfo> contacts = MCH_EntityInfoClientTracker.getAllTrackedEntities();
        if (contacts.isEmpty()) {
            this.smoothedPoses.clear();
            return;
        }

        ++this.renderFrame;
        long now = System.currentTimeMillis();
        for (MCH_EntityInfo contact : contacts) {
            if (!MCH_EntityInfoClientTracker.isEntityInLatestSnapshot(contact.entityId)
                || contact.destroyed
                || contact.getDistanceSqToEntity(mc.thePlayer) > MAX_CONTACT_DISTANCE_SQ) {
                continue;
            }
            RenderDefinition definition = this.resolveDefinition(contact);
            if (definition == null || definition.info.model == null) {
                continue;
            }

            Entity localEntity = mc.theWorld.getEntityByID(contact.entityId);
            boolean hasLiveEntity = localEntity instanceof MCH_EntityAircraft && !localEntity.isDead
                && !((MCH_EntityAircraft)localEntity).isDestroyed();
            if (localEntity instanceof MCH_EntityAircraft && !hasLiveEntity) {
                continue;
            }

            SmoothedPose pose = this.getSmoothedPose(contact, now);
            double x = pose.x - RenderManager.instance.viewerPosX;
            double z = pose.z - RenderManager.instance.viewerPosZ;
            float alpha = hasLiveEntity ? getTransitionAlpha(mc, x, z) : 1.0F;
            if (alpha <= 0.0F) {
                continue;
            }
            this.renderContact(mc, pose, definition, alpha);
        }
        this.removeUnusedPoses();
    }

    private void renderContact(Minecraft mc, SmoothedPose pose, RenderDefinition definition, float alpha) {
        RenderManager renderManager = RenderManager.instance;
        double x = pose.x - renderManager.viewerPosX;
        double y = pose.y - renderManager.viewerPosY;
        double z = pose.z - renderManager.viewerPosZ;
        double distance = Math.sqrt(x * x + y * y + z * z);
        if (distance < 0.001D) {
            return;
        }

        // Equal position/model scaling preserves screen direction and angular size without replacing projection.
        double safeDistance = getTransitionEnd(mc);
        float projectionScale = distance > safeDistance ? (float)(safeDistance / distance) : 1.0F;
        x *= projectionScale;
        y *= projectionScale;
        z *= projectionScale;

        float oldBrightnessX = OpenGlHelper.lastBrightnessX;
        float oldBrightnessY = OpenGlHelper.lastBrightnessY;
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_TEXTURE_BIT | GL11.GL_LIGHTING_BIT);
        GL11.glPushMatrix();
        try {
            GL11.glTranslated(x, y, z);
            GL11.glRotatef(pose.yaw, 0.0F, -1.0F, 0.0F);
            GL11.glRotatef(pose.pitch, 1.0F, 0.0F, 0.0F);
            GL11.glRotatef(pose.roll, 0.0F, 0.0F, 1.0F);
            GL11.glScalef(projectionScale, projectionScale, projectionScale);

            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glDisable(GL11.GL_LIGHTING);
            if (alpha < 1.0F) {
                GL11.glEnable(GL11.GL_BLEND);
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                GL11.glDepthMask(false);
            }
            GL11.glColor4f(1.0F, 1.0F, 1.0F, alpha);
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240.0F, 240.0F);
            mc.getTextureManager().bindTexture(definition.texture);
            MCH_RenderAircraft.renderStaticModel(definition.info.model);
        } finally {
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, oldBrightnessX, oldBrightnessY);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            GL11.glPopMatrix();
            GL11.glPopAttrib();
        }
    }

    private RenderDefinition resolveDefinition(MCH_EntityInfo contact) {
        if (contact.entityClassName == null || contact.entityName == null) {
            return null;
        }
        String key = contact.entityClassName + ':' + contact.entityName;
        if (this.definitions.containsKey(key)) {
            return this.definitions.get(key);
        }

        MCH_AircraftInfo info = null;
        String directory = null;
        if (isVehicleClass(contact.entityClassName, MCH_EntityHeli.class, ".helicopter.")) {
            info = MCH_HeliInfoManager.get(contact.entityName);
            directory = "helicopters";
        } else if (isVehicleClass(contact.entityClassName, MCP_EntityPlane.class, ".plane.")) {
            info = MCP_PlaneInfoManager.get(contact.entityName);
            directory = "planes";
        } else if (isVehicleClass(contact.entityClassName, MCH_EntityTank.class, ".tank.")) {
            info = MCH_TankInfoManager.get(contact.entityName);
            directory = "tanks";
        } else if (isVehicleClass(contact.entityClassName, MCH_EntityVehicle.class, ".vehicle.")) {
            info = MCH_VehicleInfoManager.get(contact.entityName);
            directory = "vehicles";
        }

        RenderDefinition definition = info != null
            ? new RenderDefinition(info, new ResourceLocation("mcheli", "textures/" + directory + "/" + info.name + ".png"))
            : null;
        this.definitions.put(key, definition);
        return definition;
    }

    private SmoothedPose getSmoothedPose(MCH_EntityInfo contact, long now) {
        SmoothedPose pose = this.smoothedPoses.get(Integer.valueOf(contact.entityId));
        if (pose == null) {
            pose = new SmoothedPose();
            this.smoothedPoses.put(Integer.valueOf(contact.entityId), pose);
        }
        pose.update(contact, now, this.renderFrame);
        return pose;
    }

    private void removeUnusedPoses() {
        Iterator<Map.Entry<Integer, SmoothedPose>> iterator = this.smoothedPoses.entrySet().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getValue().lastRenderFrame != this.renderFrame) {
                iterator.remove();
            }
        }
    }

    public static boolean shouldSuppressNormalRender(MCH_EntityAircraft aircraft, double cameraRelativeX, double cameraRelativeZ) {
        if (aircraft == null || aircraft.isDestroyed() || aircraft.getAcInfo() == null || aircraft.getAcInfo().model == null
            || !isSupportedVehicleClass(aircraft.getClass().getName())
            || !MCH_EntityInfoClientTracker.isEntityInLatestSnapshot(aircraft.getEntityId())) {
            return false;
        }
        return horizontalChunkDistance(cameraRelativeX, cameraRelativeZ) >= getTransitionEnd(Minecraft.getMinecraft());
    }

    private static float getTransitionAlpha(Minecraft mc, double cameraRelativeX, double cameraRelativeZ) {
        double distance = horizontalChunkDistance(cameraRelativeX, cameraRelativeZ);
        double end = getTransitionEnd(mc);
        double start = Math.max(CHUNK_SIZE, end - TRANSITION_WIDTH);
        return (float)Math.max(0.0D, Math.min(1.0D, (distance - start) / (end - start)));
    }

    private static double getTransitionEnd(Minecraft mc) {
        int chunks = mc != null && mc.gameSettings != null ? mc.gameSettings.renderDistanceChunks : DEFAULT_RENDER_DISTANCE_CHUNKS;
        if (chunks <= 0) {
            chunks = DEFAULT_RENDER_DISTANCE_CHUNKS;
        }
        return chunks * CHUNK_SIZE;
    }

    private static double horizontalChunkDistance(double x, double z) {
        return Math.max(Math.abs(x), Math.abs(z));
    }

    private static boolean isVehicleClass(String className, Class<?> baseClass, String packageSegment) {
        return className.equals(baseClass.getName()) || className.contains(packageSegment);
    }

    private static boolean isSupportedVehicleClass(String className) {
        return isVehicleClass(className, MCH_EntityHeli.class, ".helicopter.")
            || isVehicleClass(className, MCP_EntityPlane.class, ".plane.")
            || isVehicleClass(className, MCH_EntityTank.class, ".tank.")
            || isVehicleClass(className, MCH_EntityVehicle.class, ".vehicle.");
    }

    private static final class RenderDefinition {
        private final MCH_AircraftInfo info;
        private final ResourceLocation texture;

        private RenderDefinition(MCH_AircraftInfo info, ResourceLocation texture) {
            this.info = info;
            this.texture = texture;
        }
    }

    private static final class SmoothedPose {
        private double x;
        private double y;
        private double z;
        private float yaw;
        private float pitch;
        private float roll;
        private long lastUpdateMillis;
        private int lastRenderFrame;
        private boolean initialized;

        private void update(MCH_EntityInfo contact, long now, int renderFrame) {
            double ageTicks = Math.max(0.0D, Math.min(2.0D, (now - contact.lastUpdateTime) / 50.0D));
            double targetX = contact.posX + (contact.posX - contact.lastTickPosX) * ageTicks;
            double targetY = contact.posY + (contact.posY - contact.lastTickPosY) * ageTicks;
            double targetZ = contact.posZ + (contact.posZ - contact.lastTickPosZ) * ageTicks;
            double dx = targetX - this.x;
            double dy = targetY - this.y;
            double dz = targetZ - this.z;
            long elapsedMillis = this.lastUpdateMillis > 0L ? Math.min(100L, Math.max(0L, now - this.lastUpdateMillis)) : 0L;
            if (!this.initialized || dx * dx + dy * dy + dz * dz > SNAP_DISTANCE_SQ) {
                this.x = targetX;
                this.y = targetY;
                this.z = targetZ;
                this.yaw = contact.rotationYaw;
                this.pitch = contact.rotationPitch;
                this.roll = contact.rotationRoll;
                this.initialized = true;
            } else {
                float factor = (float)(1.0D - Math.exp(-elapsedMillis / POSITION_SMOOTHING_MILLIS));
                this.x += dx * factor;
                this.y += dy * factor;
                this.z += dz * factor;
                this.yaw = interpolateAngle(this.yaw, contact.rotationYaw, factor);
                this.pitch = interpolateAngle(this.pitch, contact.rotationPitch, factor);
                this.roll = interpolateAngle(this.roll, contact.rotationRoll, factor);
            }
            this.lastUpdateMillis = now;
            this.lastRenderFrame = renderFrame;
        }

        private static float interpolateAngle(float current, float target, float factor) {
            float delta = (target - current) % 360.0F;
            if (delta > 180.0F) delta -= 360.0F;
            if (delta < -180.0F) delta += 360.0F;
            return current + delta * factor;
        }
    }
}
