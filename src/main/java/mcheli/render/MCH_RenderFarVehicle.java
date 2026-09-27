package mcheli.render;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mcheli.MCH_Camera;
import mcheli.MCH_ClientEventHook;
import mcheli.MCH_Config;
import mcheli.MCH_EntityInfo;
import mcheli.MCH_EntityInfoClientTracker;
import mcheli.MCH_EntityInfoManager;
import mcheli.MCH_ModelManager;
import mcheli.aircraft.MCH_AircraftInfo;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.aircraft.MCH_RenderAircraft;
import mcheli.helicopter.MCH_EntityHeli;
import mcheli.helicopter.MCH_HeliInfo;
import mcheli.helicopter.MCH_HeliInfoManager;
import mcheli.plane.MCP_EntityPlane;
import mcheli.plane.MCP_PlaneInfo;
import mcheli.plane.MCP_PlaneInfoManager;
import mcheli.tank.MCH_EntityTank;
import mcheli.tank.MCH_TankInfoManager;
import mcheli.vehicle.MCH_EntityVehicle;
import mcheli.vehicle.MCH_VehicleInfo;
import mcheli.vehicle.MCH_VehicleInfoManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import org.lwjgl.opengl.GL11;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/** Draws loaded aircraft and snapshot-only contacts through the same client presentation pass. */
@SideOnly(Side.CLIENT)
public class MCH_RenderFarVehicle {

    private static final int DEFAULT_RENDER_DISTANCE_CHUNKS = 8;
    private static final double CHUNK_SIZE = 16.0D;
    private static final double TRANSITION_WIDTH = CHUNK_SIZE;
    private static final double POSITION_SMOOTHING_MILLIS = 100.0D;
    private static final double SNAP_DISTANCE_SQ = 64.0D * 64.0D;
    private static final long OCCLUSION_UNAVAILABLE_HOLD_MILLIS = 1000L;
    private static final double MAX_CONTACT_DISTANCE_SQ = MCH_EntityInfoManager.ENTITY_INFO_SYNC_RANGE
        * MCH_EntityInfoManager.ENTITY_INFO_SYNC_RANGE;
    private static final ResourceLocation THERMAL_WHITE = new ResourceLocation("mcheli", "textures/test.png");
    private final Map<String, RenderDefinition> definitions = new HashMap<String, RenderDefinition>();
    private final Map<Integer, SmoothedPose> smoothedPoses = new HashMap<Integer, SmoothedPose>();
    private final Map<Integer, UUID> retiredEntityIds = new HashMap<Integer, UUID>();
    private final Map<UUID, MCH_EntityInfo> newestByUuid = new HashMap<UUID, MCH_EntityInfo>();
    private World lastWorld;
    private int renderFrame;

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null || mc.thePlayer == null) {
            this.smoothedPoses.clear();
            this.retiredEntityIds.clear();
            this.newestByUuid.clear();
            this.lastWorld = null;
            return;
        }
        if (this.lastWorld != mc.theWorld) {
            this.smoothedPoses.clear();
            this.retiredEntityIds.clear();
            this.newestByUuid.clear();
            this.lastWorld = mc.theWorld;
        }

        ++this.renderFrame;
        long now = System.currentTimeMillis();
        Collection<MCH_EntityInfo> contacts = MCH_EntityInfoClientTracker.getAllTrackedEntities();
        this.newestByUuid.clear();
        for (MCH_EntityInfo contact : contacts) {
            if (contact.destroyed || contact.aircraftUuid == null) {
                continue;
            }
            MCH_EntityInfo previous = this.newestByUuid.get(contact.aircraftUuid);
            if (previous == null || contact.lastUpdateTime > previous.lastUpdateTime
                || contact.lastUpdateTime == previous.lastUpdateTime
                    && MCH_EntityInfoClientTracker.isEntityInLatestSnapshot(contact.entityId)) {
                this.newestByUuid.put(contact.aircraftUuid, contact);
            }
        }
        for (MCH_EntityInfo contact : contacts) {
            if (contact.entityClassName == null || contact.entityName == null
                    || !isSupportedVehicleClass(contact.entityClassName)) {
                continue;
            }
            if (contact.destroyed) {
                this.smoothedPoses.remove(Integer.valueOf(contact.entityId));
                continue;
            }
            UUID uuid = contact.aircraftUuid;
            if (uuid != null && this.newestByUuid.get(uuid) != contact) {
                this.retiredEntityIds.put(Integer.valueOf(contact.entityId), uuid);
                continue;
            }
            SmoothedPose pose = this.smoothedPoses.get(Integer.valueOf(contact.entityId));
            if (uuid != null) {
                this.retiredEntityIds.remove(Integer.valueOf(contact.entityId));
                Iterator<Map.Entry<Integer, SmoothedPose>> oldPoses = this.smoothedPoses.entrySet().iterator();
                while (oldPoses.hasNext()) {
                    Map.Entry<Integer, SmoothedPose> old = oldPoses.next();
                    if (old.getKey().intValue() != contact.entityId
                        && uuid.equals(old.getValue().aircraftUuid)) {
                        if (pose == null) {
                            pose = old.getValue();
                        }
                        this.retiredEntityIds.put(old.getKey(), uuid);
                        oldPoses.remove();
                    }
                }
            }
            if (pose == null) {
                pose = new SmoothedPose();
                this.smoothedPoses.put(Integer.valueOf(contact.entityId), pose);
            } else if (!this.smoothedPoses.containsKey(Integer.valueOf(contact.entityId))) {
                this.smoothedPoses.put(Integer.valueOf(contact.entityId), pose);
            }
            pose.observe(contact);
        }

        Iterator<Map.Entry<Integer, UUID>> retired = this.retiredEntityIds.entrySet().iterator();
        while (retired.hasNext()) {
            if (!this.newestByUuid.containsKey(retired.next().getValue())) {
                retired.remove();
            }
        }

        // RenderGlobal can skip an entity during tracker/chunk handoff. WorldClient's loaded list
        // supplies the model directly even on a frame with no snapshot or normal render call.
        for (Object object : mc.theWorld.loadedEntityList) {
            if (!(object instanceof MCH_EntityAircraft)) {
                continue;
            }
            MCH_EntityAircraft aircraft = (MCH_EntityAircraft)object;
            Integer entityId = Integer.valueOf(aircraft.getEntityId());
            UUID retiredUuid = this.retiredEntityIds.get(entityId);
            if (retiredUuid != null) {
                // Entity IDs can be reused. Retire only the old instance while its replacement exists.
                if (retiredUuid.equals(aircraft.getUniqueID()) && this.newestByUuid.containsKey(retiredUuid)) {
                    continue;
                }
                this.retiredEntityIds.remove(entityId);
            }
            if (!usesUnifiedRender(aircraft) || MCH_RenderAircraft.shouldSkipRender(aircraft)) {
                continue;
            }
            RenderDefinition definition = this.resolveDefinition(aircraft);
            if (definition == null) {
                continue;
            }
            SmoothedPose pose = this.getSmoothedPose(aircraft.getEntityId());
            pose.updateFromAircraft(aircraft, definition, now, this.renderFrame, event.partialTicks);
            pose.visualAircraft = aircraft;
            pose.lightmapBrightness = pose.contact != null && pose.contact.packedLight >= 0
                ? pose.contact.packedLight : aircraft.getBrightnessForRender(event.partialTicks);
        }

        if (this.smoothedPoses.isEmpty()) {
            return;
        }
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_LIGHTING_BIT);
        mc.entityRenderer.enableLightmap(event.partialTicks);
        try {
            Iterator<Map.Entry<Integer, SmoothedPose>> iterator = this.smoothedPoses.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<Integer, SmoothedPose> entry = iterator.next();
                SmoothedPose pose = entry.getValue();
                if (pose.lastRenderFrame != this.renderFrame
                    && !MCH_EntityInfoClientTracker.isEntityInLatestSnapshot(entry.getKey().intValue())
                    && isOverlappedByLoadedAircraft(mc.theWorld, pose, entry.getKey().intValue())) {
                    continue;
                }
                if (pose.lastRenderFrame != this.renderFrame) {
                    if (pose.contact != null) {
                        pose.update(pose.contact, now, this.renderFrame);
                    } else if (pose.visualAircraft == null) {
                        iterator.remove();
                        continue;
                    }
                }
                double dx = pose.x - RenderManager.instance.viewerPosX;
                double dy = pose.y - RenderManager.instance.viewerPosY;
                double dz = pose.z - RenderManager.instance.viewerPosZ;
                if (dx * dx + dy * dy + dz * dz > MAX_CONTACT_DISTANCE_SQ) {
                    iterator.remove();
                    continue;
                }
                MCH_EntityAircraft current = pose.visualAircraft;
                if (current != null && current.worldObj != mc.theWorld) {
                    pose.visualAircraft = null;
                    current = null;
                }
                if (current != null && current.isDestroyed()) {
                    iterator.remove();
                    continue;
                }
                if (current != null && MCH_RenderAircraft.shouldSkipRender(current)) {
                    continue;
                }
                RenderDefinition definition = current != null
                    ? this.resolveDefinition(current) : this.resolveDefinition(pose.entityClassName, pose.entityName);
                if (definition == null || definition.info.model == null) {
                    continue;
                }
                // Test terrain at the contact's true world-space position. Once renderContact()
                // projects a distant contact toward the vanilla far plane, whole-model traceBounds()
                // becomes too permissive: one clear corner can reveal the entire projected model.
                // Use a single physical-core ray for projected BVR contacts, while retaining the
                // conservative bounds query for contacts still rendered at their true distance.
                double trueDistance = Math.sqrt(dx * dx + dy * dy + dz * dz);
                boolean projectedBvr = trueDistance > getTransitionStart(mc);
                // Loaded nearby aircraft render at their true position, so the world's depth buffer
                // handles terrain. WGMap coverage may be incomplete during chunk handoff.
                MCH_WGMapOcclusion.Result terrainResult = current != null
                    ? MCH_WGMapOcclusion.Result.CLEAR : MCH_WGMapOcclusion.Result.UNAVAILABLE;
                if (projectedBvr) {
                    terrainResult = MCH_WGMapOcclusion.traceTargetCore(
                        mc, definition.info, pose.x, pose.y, pose.z, event.partialTicks);
                } else if (current == null) {
                    terrainResult = MCH_WGMapOcclusion.traceTargetBounds(
                        mc, definition.info, pose.x, pose.y, pose.z, event.partialTicks);
                }
                if (!projectedBvr && terrainResult == MCH_WGMapOcclusion.Result.UNKNOWN) {
                    terrainResult = MCH_WGMapOcclusion.Result.UNAVAILABLE;
                }
                if (pose.shouldSuppressForTerrain(terrainResult, now)) {
                    continue;
                }
                int brightness = pose.lightmapBrightness >= 0 ? pose.lightmapBrightness
                    : this.resolveLightmapBrightness(mc, pose, null, event.partialTicks);
                this.renderContact(mc, pose, definition, current, event.partialTicks, brightness);
            }
        } finally {
            mc.entityRenderer.disableLightmap(event.partialTicks);
            GL11.glPopAttrib();
        }
    }

    private void renderContact(Minecraft mc, SmoothedPose pose, RenderDefinition definition,
            MCH_EntityAircraft localAircraft, float partialTicks, int lightmapBrightness) {
        RenderManager renderManager = RenderManager.instance;
        double x = pose.x - renderManager.viewerPosX;
        double y = pose.y - renderManager.viewerPosY;
        double z = pose.z - renderManager.viewerPosZ;
        double distance = Math.sqrt(x * x + y * y + z * z);
        // Equal position/model scaling preserves screen direction and angular size without replacing projection.
        // RenderWorldLast is outside the normal fog pass. Keep the projected contact inside the fog end
        // so enabling fog does not make every beyond-range contact completely invisible.
        double safeDistance = getTransitionStart(mc);
        float projectionScale = distance > safeDistance ? (float)(safeDistance / distance) : 1.0F;
        x *= projectionScale;
        y *= projectionScale;
        z *= projectionScale;

        float oldBrightnessX = OpenGlHelper.lastBrightnessX;
        float oldBrightnessY = OpenGlHelper.lastBrightnessY;
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT
            | GL11.GL_TEXTURE_BIT | GL11.GL_LIGHTING_BIT | GL11.GL_POLYGON_BIT | GL11.GL_FOG_BIT);
        RenderHelper.enableStandardItemLighting();
        GL11.glPushMatrix();
        try {
            GL11.glTranslated(x, y, z);
            GL11.glScalef(projectionScale, projectionScale, projectionScale);

            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glEnable(GL11.GL_FOG);
            GL11.glEnable(GL11.GL_LIGHTING);
            GL11.glEnable(GL11.GL_COLOR_MATERIAL);
            GL11.glEnable(GL11.GL_CULL_FACE);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glAlphaFunc(GL11.GL_GREATER, 0.001F);
            GL11.glColorMaterial(GL11.GL_FRONT, GL11.GL_AMBIENT);
            GL11.glEnable(GL11.GL_NORMALIZE);
            GL11.glShadeModel(definition.info.smoothShading && MCH_Config.SmoothShading.prmBool
                ? GL11.GL_SMOOTH : GL11.GL_FLAT);
            boolean thermalVision = MCH_Camera.currentCameraMode == MCH_Camera.MODE_THERMALVISION
                && (localAircraft == null || MCH_EntityAircraft.getAircraft_RiddenOrControl(mc.thePlayer) != localAircraft);
            if (thermalVision) {
                // Match the normal aircraft thermal marker so the post-process maps the whole LOD to white.
                GL11.glDisable(GL11.GL_FOG);
                RenderHelper.disableStandardItemLighting();
                OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240.0F, 240.0F);
                GL11.glColor4f(1.0F, 0.0F, 1.0F, 1.0F);
                mc.getTextureManager().bindTexture(THERMAL_WHITE);
            } else {
                GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
                int blockLight = lightmapBrightness % 65536;
                int skyLight = lightmapBrightness / 65536;
                OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, blockLight, skyLight);
                mc.getTextureManager().bindTexture(pose.texture != null ? pose.texture : definition.texture);
            }
            if (MCH_Camera.currentCameraMode == MCH_Camera.MODE_NIGHTVISION) {
                GL11.glColor4f(10.0F, 10.0F, 0.2F, 1.0F);
            }
            if (localAircraft != null && localAircraft.ironCurtainRunningTick > 0) {
                float factor = localAircraft.ironCurtainLastFactor
                    + (localAircraft.ironCurtainCurrentFactor - localAircraft.ironCurtainLastFactor)
                    * (float)Math.sin(MCH_ClientEventHook.smoothing * Math.PI / 2.0D);
                GL11.glColor4f(0.8F * factor, 0.4F * factor, 0.4F * factor, 1.0F);
            }
            Render renderer = localAircraft != null
                ? renderManager.getEntityRenderObject(localAircraft) : null;
            if (renderer instanceof MCH_RenderAircraft && localAircraft.getAcInfo() != null) {
                // The tracked entity has all animated part state and its selected skin.
                MCH_RenderAircraft aircraftRenderer = (MCH_RenderAircraft)renderer;
                aircraftRenderer.renderAircraft(localAircraft, 0.0D, 0.0D, 0.0D,
                    pose.yaw, pose.pitch, pose.roll, partialTicks);
                aircraftRenderer.renderCommonPart(localAircraft, localAircraft.getAcInfo(),
                    0.0D, 0.0D, 0.0D, partialTicks);
                MCH_RenderAircraft.renderLight(0.0D, 0.0D, 0.0D, partialTicks,
                    localAircraft, localAircraft.getAcInfo());
            } else {
                GL11.glRotatef(pose.yaw, 0.0F, -1.0F, 0.0F);
                GL11.glRotatef(pose.pitch, 1.0F, 0.0F, 0.0F);
                GL11.glRotatef(pose.roll, 0.0F, 0.0F, 1.0F);
                MCH_RenderAircraft.renderBody(definition.info.model);
                this.renderLightweightParts(definition.info, pose);
            }
        } finally {
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, oldBrightnessX, oldBrightnessY);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            GL11.glPopMatrix();
            GL11.glPopAttrib();
        }
    }

    private int resolveLightmapBrightness(Minecraft mc, SmoothedPose pose, MCH_EntityAircraft localAircraft, float partialTicks) {
        if (localAircraft != null && !localAircraft.isDead) {
            return localAircraft.getBrightnessForRender(partialTicks);
        }

        int blockX = MathHelper.floor_double(pose.x);
        int blockY = MathHelper.floor_double(pose.y);
        int blockZ = MathHelper.floor_double(pose.z);
        Chunk chunk = mc.theWorld.getChunkProvider().provideChunk(blockX >> 4, blockZ >> 4);
        if (chunk != null && !chunk.isEmpty()) {
            return mc.theWorld.getLightBrightnessForSkyBlocks(blockX, blockY, blockZ, 0);
        }

        // Unloaded chunks have no light data. Approximate exposed-sky light from the world's current
        // day/night and weather subtraction instead of making every far contact permanently bright.
        int skyLight = mc.theWorld.provider.hasNoSky ? 0 : Math.max(0, 15 - mc.theWorld.skylightSubtracted);
        return skyLight << 20;
    }

    private void renderLightweightParts(MCH_AircraftInfo info, SmoothedPose pose) {
        this.renderWeaponParts(info, pose);
        this.renderRestingParts(info, info.hatchList);
        this.renderRestingParts(info, info.cameraList);
        this.renderRestingParts(info, info.partWeaponBay);
        this.renderRestingParts(info, info.partTurretWeaponBay);
        this.renderRestingParts(info, info.canopyList);
        this.renderRestingParts(info, info.landingGear);
        this.renderRestingParts(info, info.partThrottle);
        this.renderRestingParts(info, info.partRotPart);
        this.renderRestingParts(info, info.partTurretRotPart);
        this.renderRestingParts(info, info.partTrackRoller);
        this.renderRestingParts(info, info.partWheel);
        this.renderRestingParts(info, info.partSteeringWheel);
        this.renderRestingParts(info, info.lightHatchList);
        if (info instanceof MCH_VehicleInfo) {
            MCH_VehicleInfo vehicleInfo = (MCH_VehicleInfo)info;
            for (Object part : vehicleInfo.partList) {
                this.renderVehiclePart(vehicleInfo, (MCH_VehicleInfo.VPart)part, pose);
            }
        }
        if (info instanceof MCH_HeliInfo) {
            this.renderHelicopterRotors((MCH_HeliInfo)info);
        }
        if (info instanceof MCP_PlaneInfo) {
            this.renderPlaneParts((MCP_PlaneInfo)info);
        }
        if (!info.partCrawlerTrack.isEmpty()) {
            GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT);
            MCH_RenderAircraft.renderCrawlerTrack(null, info, 0.0F);
            GL11.glPopAttrib();
        }
    }

    private static boolean isOverlappedByLoadedAircraft(World world, SmoothedPose pose, int oldId) {
        for (Object object : world.loadedEntityList) {
            if (!(object instanceof MCH_EntityAircraft)) {
                continue;
            }
            MCH_EntityAircraft aircraft = (MCH_EntityAircraft)object;
            if (aircraft.getEntityId() == oldId || aircraft.isDead || aircraft.getAcInfo() == null
                || !aircraft.getClass().getName().equals(pose.entityClassName)
                || !aircraft.getAcInfo().name.equals(pose.entityName)) {
                continue;
            }
            MCH_EntityInfo other = MCH_EntityInfoClientTracker.getEntityInfo(aircraft.getEntityId());
            if (other != null && pose.aircraftUuid != null && other.aircraftUuid != null
                && !pose.aircraftUuid.equals(other.aircraftUuid)) {
                continue;
            }
            double dx = aircraft.posX - pose.x;
            double dy = aircraft.posY - pose.y;
            double dz = aircraft.posZ - pose.z;
            if (dx * dx + dy * dy + dz * dz < 4.0D) {
                return true;
            }
        }
        return false;
    }

    private void renderRestingParts(MCH_AircraftInfo info, Collection parts) {
        for (Object object : parts) {
            MCH_AircraftInfo.DrawnPart part = (MCH_AircraftInfo.DrawnPart)object;
            MCH_RenderAircraft.renderPart(part.model, info.model, part.modelName);
        }
    }

    private void renderPlaneParts(MCP_PlaneInfo info) {
        this.renderRestingParts(info, info.nozzles);
        for (Object object : info.wingList) {
            MCP_PlaneInfo.Wing wing = (MCP_PlaneInfo.Wing)object;
            MCH_RenderAircraft.renderPart(wing.model, info.model, wing.modelName);
            if (wing.pylonList != null) {
                this.renderRestingParts(info, wing.pylonList);
            }
        }
        for (Object object : info.rotorList) {
            MCP_PlaneInfo.Rotor rotor = (MCP_PlaneInfo.Rotor)object;
            MCH_RenderAircraft.renderPart(rotor.model, info.model, rotor.modelName);
            for (Object bladeObject : rotor.blades) {
                MCP_PlaneInfo.Blade blade = (MCP_PlaneInfo.Blade)bladeObject;
                GL11.glPushMatrix();
                GL11.glTranslated(blade.pos.xCoord, blade.pos.yCoord, blade.pos.zCoord);
                for (int i = 0; i < blade.numBlade; ++i) {
                    GL11.glRotatef(blade.rotBlade, (float)blade.rot.xCoord,
                        (float)blade.rot.yCoord, (float)blade.rot.zCoord);
                    GL11.glPushMatrix();
                    GL11.glTranslated(-blade.pos.xCoord, -blade.pos.yCoord, -blade.pos.zCoord);
                    MCH_RenderAircraft.renderPart(blade.model, info.model, blade.modelName);
                    GL11.glPopMatrix();
                }
                GL11.glPopMatrix();
            }
        }
    }

    private void renderWeaponParts(MCH_AircraftInfo info, SmoothedPose pose) {
        float turretYaw = wrapAngle(pose.turretYaw - pose.yaw);
        float turretPitch = clamp(pose.turretPitch, info.minRotationPitch, info.maxRotationPitch);
        for (Object part : info.partWeapon) {
            MCH_AircraftInfo.PartWeapon weapon = (MCH_AircraftInfo.PartWeapon)part;
            GL11.glPushMatrix();
            if (weapon.turret) {
                GL11.glTranslated(info.turretPosition.xCoord, info.turretPosition.yCoord, info.turretPosition.zCoord);
                GL11.glRotatef(turretYaw, 0.0F, -1.0F, 0.0F);
                GL11.glTranslated(-info.turretPosition.xCoord, -info.turretPosition.yCoord, -info.turretPosition.zCoord);
            }
            GL11.glTranslated(weapon.pos.xCoord, weapon.pos.yCoord, weapon.pos.zCoord);
            if (weapon.yaw && !weapon.turret) {
                GL11.glRotatef(turretYaw, 0.0F, -1.0F, 0.0F);
            }
            if (weapon.pitch) {
                GL11.glRotatef(turretPitch, 1.0F, 0.0F, 0.0F);
            }
            GL11.glTranslated(-weapon.pos.xCoord, -weapon.pos.yCoord, -weapon.pos.zCoord);
            MCH_RenderAircraft.renderPart(weapon.model, info.model, weapon.modelName);
            for (Object childPart : weapon.child) {
                MCH_AircraftInfo.PartWeaponChild child = (MCH_AircraftInfo.PartWeaponChild)childPart;
                GL11.glPushMatrix();
                GL11.glTranslated(child.pos.xCoord, child.pos.yCoord, child.pos.zCoord);
                if (child.yaw) {
                    GL11.glRotatef(turretYaw, 0.0F, -1.0F, 0.0F);
                }
                if (child.pitch) {
                    GL11.glRotatef(turretPitch, 1.0F, 0.0F, 0.0F);
                }
                GL11.glTranslated(-child.pos.xCoord, -child.pos.yCoord, -child.pos.zCoord);
                MCH_RenderAircraft.renderPart(child.model, info.model, child.modelName);
                GL11.glPopMatrix();
            }
            GL11.glPopMatrix();
        }
    }

    private void renderVehiclePart(MCH_VehicleInfo info, MCH_VehicleInfo.VPart part, SmoothedPose pose) {
        GL11.glPushMatrix();
        GL11.glTranslated(part.pos.xCoord, part.pos.yCoord, part.pos.zCoord);
        if (part.rotYaw) {
            GL11.glRotatef(wrapAngle(pose.turretYaw - pose.yaw), 0.0F, -1.0F, 0.0F);
        }
        if (part.rotPitch) {
            float pitch = clamp(pose.turretPitch, info.minRotationPitch, info.maxRotationPitch);
            GL11.glRotatef(pitch - pose.pitch, 1.0F, 0.0F, 0.0F);
        }
        GL11.glTranslated(-part.pos.xCoord, -part.pos.yCoord, -part.pos.zCoord);
        MCH_RenderAircraft.renderPart(part.model, info.model, part.modelName);
        MCH_ModelManager.render("vehicles", part.modelName);
        if (part.child != null) {
            for (Object child : part.child) {
                this.renderVehiclePart(info, (MCH_VehicleInfo.VPart)child, pose);
            }
        }
        GL11.glPopMatrix();
    }

    private void renderHelicopterRotors(MCH_HeliInfo info) {
        // Far contacts do not need authoritative rotor RPM; a local phase keeps this cosmetic animation packet-free.
        float rotorPhase = (System.currentTimeMillis() % 500L) * 0.72F;
        for (Object part : info.rotorList) {
            MCH_HeliInfo.Rotor rotor = (MCH_HeliInfo.Rotor)part;
            GL11.glPushMatrix();
            if (rotor.oldRenderMethod) {
                GL11.glTranslated(rotor.pos.xCoord, rotor.pos.yCoord, rotor.pos.zCoord);
            }
            for (int blade = 0; blade < rotor.bladeNum; ++blade) {
                GL11.glPushMatrix();
                if (!rotor.oldRenderMethod) {
                    GL11.glTranslated(rotor.pos.xCoord, rotor.pos.yCoord, rotor.pos.zCoord);
                }
                GL11.glRotatef(rotorPhase + rotor.bladeRot * blade, (float)rotor.rot.xCoord, (float)rotor.rot.yCoord, (float)rotor.rot.zCoord);
                if (!rotor.oldRenderMethod) {
                    GL11.glTranslated(-rotor.pos.xCoord, -rotor.pos.yCoord, -rotor.pos.zCoord);
                }
                MCH_RenderAircraft.renderPart(rotor.model, info.model, rotor.modelName);
                GL11.glPopMatrix();
            }
            GL11.glPopMatrix();
        }
    }

    private RenderDefinition resolveDefinition(MCH_EntityInfo contact) {
        return this.resolveDefinition(contact.entityClassName, contact.entityName);
    }

    private RenderDefinition resolveDefinition(MCH_EntityAircraft aircraft) {
        String className = aircraft.getClass().getName();
        MCH_AircraftInfo info = aircraft.getAcInfo();
        RenderDefinition definition = this.resolveDefinition(className, info.name);
        if (definition != null) {
            return definition;
        }
        String directory = aircraft instanceof MCH_EntityHeli ? "helicopters"
            : aircraft instanceof MCP_EntityPlane ? "planes"
            : aircraft instanceof MCH_EntityTank ? "tanks" : "vehicles";
        definition = new RenderDefinition(info, directory,
            new ResourceLocation("mcheli", "textures/" + directory + "/" + info.name + ".png"));
        this.definitions.put(className + ':' + info.name, definition);
        return definition;
    }

    private RenderDefinition resolveDefinition(String className, String entityName) {
        if (className == null || entityName == null) {
            return null;
        }
        String key = className + ':' + entityName;
        if (this.definitions.containsKey(key)) {
            return this.definitions.get(key);
        }

        MCH_AircraftInfo info = null;
        String directory = null;
        if (isVehicleClass(className, MCH_EntityHeli.class, ".helicopter.")) {
            info = MCH_HeliInfoManager.get(entityName);
            directory = "helicopters";
        } else if (isVehicleClass(className, MCP_EntityPlane.class, ".plane.")) {
            info = MCP_PlaneInfoManager.get(entityName);
            directory = "planes";
        } else if (isVehicleClass(className, MCH_EntityTank.class, ".tank.")) {
            info = MCH_TankInfoManager.get(entityName);
            directory = "tanks";
        } else if (isVehicleClass(className, MCH_EntityVehicle.class, ".vehicle.")) {
            info = MCH_VehicleInfoManager.get(entityName);
            directory = "vehicles";
        }

        RenderDefinition definition = info != null
            ? new RenderDefinition(info, directory, new ResourceLocation("mcheli", "textures/" + directory + "/" + info.name + ".png"))
            : null;
        if (definition != null) {
            this.definitions.put(key, definition);
        }
        return definition;
    }

    private SmoothedPose getSmoothedPose(int entityId) {
        SmoothedPose pose = this.smoothedPoses.get(Integer.valueOf(entityId));
        if (pose == null) {
            pose = new SmoothedPose();
            this.smoothedPoses.put(Integer.valueOf(entityId), pose);
        }
        return pose;
    }

    public static boolean usesUnifiedRender(MCH_EntityAircraft aircraft) {
        if (aircraft == null || aircraft.isDestroyed() || aircraft.getAcInfo() == null || aircraft.getAcInfo().model == null
            || !isSupportedVehicleClass(aircraft.getClass().getName())) {
            return false;
        }
        return true;
    }

    /** Retained for callers that need the legacy distance-based transition weight. */
    public static float getLodTransitionAlpha(Minecraft mc, double cameraRelativeX, double cameraRelativeZ) {
        double distance = horizontalChunkDistance(cameraRelativeX, cameraRelativeZ);
        double end = getTransitionEnd(mc);
        double start = getTransitionStart(mc);
        if (end <= start) {
            return distance >= end ? 1.0F : 0.0F;
        }
        return (float)Math.max(0.0D, Math.min(1.0D, (distance - start) / (end - start)));
    }

    private static double getTransitionEnd(Minecraft mc) {
        int chunks = mc != null && mc.gameSettings != null ? mc.gameSettings.renderDistanceChunks : DEFAULT_RENDER_DISTANCE_CHUNKS;
        if (chunks <= 0) {
            chunks = DEFAULT_RENDER_DISTANCE_CHUNKS;
        }
        return chunks * CHUNK_SIZE;
    }

    private static double getTransitionStart(Minecraft mc) {
        return Math.max(CHUNK_SIZE, getTransitionEnd(mc) - TRANSITION_WIDTH);
    }

    private static double horizontalChunkDistance(double x, double z) {
        return Math.max(Math.abs(x), Math.abs(z));
    }

    private static float clamp(float value, float minimum, float maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static float wrapAngle(float angle) {
        angle %= 360.0F;
        if (angle > 180.0F) angle -= 360.0F;
        if (angle < -180.0F) angle += 360.0F;
        return angle;
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
        private final String directory;

        private RenderDefinition(MCH_AircraftInfo info, String directory, ResourceLocation texture) {
            this.info = info;
            this.directory = directory;
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
        private float turretYaw;
        private float turretPitch;
        private ResourceLocation texture;
        private String textureName;
        private String entityClassName;
        private String entityName;
        private MCH_EntityInfo contact;
        private UUID aircraftUuid;
        private MCH_EntityAircraft visualAircraft;
        private int lightmapBrightness = -1;
        private boolean terrainBlocked;
        private long terrainBlockedConfirmedMillis;
        private long lastUpdateMillis;
        private int lastRenderFrame;
        private boolean initialized;

        private void observe(MCH_EntityInfo contact) {
            this.ensureIdentity(contact.entityClassName, contact.entityName);
            if (this.aircraftUuid != null && contact.aircraftUuid != null
                && !this.aircraftUuid.equals(contact.aircraftUuid)) {
                this.texture = null;
                this.textureName = null;
                this.contact = null;
                this.visualAircraft = null;
                this.lightmapBrightness = -1;
                this.clearTerrainOcclusion();
                this.initialized = false;
            }
            this.aircraftUuid = contact.aircraftUuid;
            if (this.contact == null || contact.lastUpdateTime >= this.contact.lastUpdateTime) {
                this.contact = contact;
                if (contact.packedLight >= 0) {
                    this.lightmapBrightness = contact.packedLight;
                }
            }
        }

        private void updateFromAircraft(MCH_EntityAircraft aircraft, RenderDefinition definition,
                long now, int renderFrame, float partialTicks) {
            this.ensureIdentity(aircraft.getClass().getName(), aircraft.getAcInfo().name);
            this.x = aircraft.lastTickPosX + (aircraft.posX - aircraft.lastTickPosX) * partialTicks;
            this.y = aircraft.lastTickPosY + (aircraft.posY - aircraft.lastTickPosY) * partialTicks;
            this.z = aircraft.lastTickPosZ + (aircraft.posZ - aircraft.lastTickPosZ) * partialTicks;
            this.yaw = interpolateAngle(aircraft.prevRotationYaw, aircraft.getRotYaw(), partialTicks);
            this.pitch = aircraft.calcRotPitch(partialTicks);
            this.roll = interpolateAngle(aircraft.prevRotationRoll, aircraft.getRotRoll(), partialTicks);
            this.turretYaw = aircraft.getLastRiderYaw();
            this.turretPitch = aircraft.getLastRiderPitch();
            String currentTexture = aircraft.getTextureName();
            if (currentTexture == null || currentTexture.isEmpty()) {
                currentTexture = definition.info.name;
            }
            if (!currentTexture.equals(this.textureName)) {
                this.textureName = currentTexture;
                this.texture = new ResourceLocation("mcheli", "textures/" + definition.directory + "/"
                    + currentTexture + ".png");
            }
            this.lastUpdateMillis = now;
            this.lastRenderFrame = renderFrame;
            this.initialized = true;
        }

        private void update(MCH_EntityInfo contact, long now, int renderFrame) {
            this.ensureIdentity(contact.entityClassName, contact.entityName);
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
                this.turretYaw = contact.turretYaw;
                this.turretPitch = contact.turretPitch;
                this.initialized = true;
            } else {
                float factor = (float)(1.0D - Math.exp(-elapsedMillis / POSITION_SMOOTHING_MILLIS));
                this.x += dx * factor;
                this.y += dy * factor;
                this.z += dz * factor;
                this.yaw = interpolateAngle(this.yaw, contact.rotationYaw, factor);
                this.pitch = interpolateAngle(this.pitch, contact.rotationPitch, factor);
                this.roll = interpolateAngle(this.roll, contact.rotationRoll, factor);
                this.turretYaw = interpolateAngle(this.turretYaw, contact.turretYaw, factor);
                this.turretPitch = interpolateAngle(this.turretPitch, contact.turretPitch, factor);
            }
            this.lastUpdateMillis = now;
            this.lastRenderFrame = renderFrame;
        }

        private void ensureIdentity(String className, String name) {
            if (!className.equals(this.entityClassName) || !name.equals(this.entityName)) {
                this.entityClassName = className;
                this.entityName = name;
                this.texture = null;
                this.textureName = null;
                this.contact = null;
                this.aircraftUuid = null;
                this.visualAircraft = null;
                this.lightmapBrightness = -1;
                this.clearTerrainOcclusion();
                this.initialized = false;
            }
        }

        private boolean shouldSuppressForTerrain(MCH_WGMapOcclusion.Result result, long nowMillis) {
            if (result == MCH_WGMapOcclusion.Result.BLOCKED) {
                this.terrainBlocked = true;
                this.terrainBlockedConfirmedMillis = nowMillis;
                return true;
            }
            if (result == MCH_WGMapOcclusion.Result.UNKNOWN) {
                return true;
            }
            if (result == MCH_WGMapOcclusion.Result.CLEAR) {
                this.clearTerrainOcclusion();
                return false;
            }
            if (!this.terrainBlocked) {
                return false;
            }
            if (nowMillis - this.terrainBlockedConfirmedMillis <= OCCLUSION_UNAVAILABLE_HOLD_MILLIS) {
                return true;
            }
            this.clearTerrainOcclusion();
            return false;
        }

        private void clearTerrainOcclusion() {
            this.terrainBlocked = false;
            this.terrainBlockedConfirmedMillis = 0L;
        }

        private static float interpolateAngle(float current, float target, float factor) {
            float delta = (target - current) % 360.0F;
            if (delta > 180.0F) delta -= 360.0F;
            if (delta < -180.0F) delta += 360.0F;
            return current + delta * factor;
        }
    }
}
