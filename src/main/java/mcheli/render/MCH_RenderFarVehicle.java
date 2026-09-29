package mcheli.render;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mcheli.MCH_Camera;
import mcheli.MCH_ClientEventHook;
import mcheli.MCH_Config;
import mcheli.MCH_EntityInfo;
import mcheli.MCH_EntityInfoClientTracker;
import mcheli.MCH_EntityInfoManager;
import mcheli.MCH_Lib;
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
import net.minecraft.client.renderer.culling.Frustrum;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
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
    private static final long OCCLUSION_RESULT_CACHE_MILLIS = 160L;
    private static final long OCCLUSION_RETRY_MILLIS = 80L;
    private static final long MISSING_HOLD_MILLIS = 500L;
    private static final long UNCERTAIN_HOLD_MILLIS = 220L;
    // One short mesh-build window; camera and target motion are bounded separately.
    private static final long GPU_PARTIAL_HOLD_MILLIS = 250L;
    private static final long TRACKER_EXIT_GRACE_MILLIS = 300L;
    private static final double MAX_CONTACT_DISTANCE_SQ = MCH_EntityInfoManager.ENTITY_INFO_SYNC_RANGE
        * MCH_EntityInfoManager.ENTITY_INFO_SYNC_RANGE;
    private static final ResourceLocation THERMAL_WHITE = new ResourceLocation("mcheli", "textures/test.png");
    private final Map<String, RenderDefinition> definitions = new HashMap<String, RenderDefinition>();
    private final Map<Integer, SmoothedPose> smoothedPoses = new HashMap<Integer, SmoothedPose>();
    private final Map<Integer, UUID> retiredEntityIds = new HashMap<Integer, UUID>();
    private final Map<UUID, MCH_EntityInfo> newestByUuid = new HashMap<UUID, MCH_EntityInfo>();
    private final Map<UUID, MCH_EntityAircraft> loadedByUuid = new HashMap<UUID, MCH_EntityAircraft>();
    private final List<MCH_EntityAircraft> loadedAircraft = new ArrayList<MCH_EntityAircraft>();
    private final List<RenderCandidate> candidates = new ArrayList<RenderCandidate>();
    private double[] depthTargets = new double[24];
    private double[] depthRadii = new double[8];
    private final FloatBuffer depthModelview = BufferUtils.createFloatBuffer(16);
    private final FloatBuffer depthProjection = BufferUtils.createFloatBuffer(16);
    private final MCH_FarVehicleDepthLayer precisionLayer = new MCH_FarVehicleDepthLayer();
    private int candidateCount;
    private int lastBvrCount = -1, lastGpuReadyCount = -1;
    private boolean lastDepthAvailable;
    private World lastWorld;
    private int renderFrame;

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null || mc.thePlayer == null) {
            this.smoothedPoses.clear();
            this.retiredEntityIds.clear();
            this.newestByUuid.clear();
            this.loadedByUuid.clear();
            this.loadedAircraft.clear();
            this.candidates.clear();
            this.lastWorld = null;
            return;
        }
        if (this.lastWorld != mc.theWorld) {
            this.smoothedPoses.clear();
            this.retiredEntityIds.clear();
            this.newestByUuid.clear();
            this.loadedByUuid.clear();
            this.loadedAircraft.clear();
            this.candidates.clear();
            this.lastWorld = mc.theWorld;
        }

        ++this.renderFrame;
        long now = System.currentTimeMillis();
        this.loadedByUuid.clear();
        this.loadedAircraft.clear();
        for (Object object : mc.theWorld.loadedEntityList) {
            if (object instanceof MCH_EntityAircraft && !((MCH_EntityAircraft)object).isDead) {
                MCH_EntityAircraft aircraft = (MCH_EntityAircraft)object;
                this.loadedAircraft.add(aircraft);
                this.loadedByUuid.put(aircraft.getUniqueID(), aircraft);
            }
        }
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
                SmoothedPose destroyed = this.smoothedPoses.remove(Integer.valueOf(contact.entityId));
                if (destroyed != null) destroyed.report("DESTROYED", contact.entityId);
                continue;
            }
            UUID uuid = contact.aircraftUuid;
            MCH_EntityAircraft loaded = uuid != null ? this.loadedByUuid.get(uuid) : null;
            if (loaded != null && loaded.getEntityId() != contact.entityId) {
                this.retiredEntityIds.put(Integer.valueOf(contact.entityId), uuid);
                SmoothedPose old = this.smoothedPoses.remove(Integer.valueOf(contact.entityId));
                if (old != null) {
                    old.report("RETIRED", contact.entityId);
                    Integer loadedId = Integer.valueOf(loaded.getEntityId());
                    SmoothedPose destination = this.smoothedPoses.get(loadedId);
                    if (destination == null || !uuid.equals(destination.aircraftUuid)) {
                        this.smoothedPoses.put(loadedId, old);
                    }
                }
                continue;
            }
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
            pose.observe(contact, now);
        }

        Iterator<Map.Entry<Integer, UUID>> retired = this.retiredEntityIds.entrySet().iterator();
        while (retired.hasNext()) {
            if (!this.newestByUuid.containsKey(retired.next().getValue())) {
                retired.remove();
            }
        }

        // RenderGlobal can skip an entity during tracker/chunk handoff. WorldClient's loaded list
        // supplies the model directly even on a frame with no snapshot or normal render call.
        for (MCH_EntityAircraft aircraft : this.loadedAircraft) {
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
            SmoothedPose pose = this.getSmoothedPose(aircraft);
            pose.updateFromAircraft(aircraft, definition, now, this.renderFrame, event.partialTicks);
            pose.visualAircraft = aircraft;
            pose.visualSeenFrame = this.renderFrame;
            pose.lightmapBrightness = aircraft.getBrightnessForRender(event.partialTicks);
        }

        if (this.smoothedPoses.isEmpty()) {
            return;
        }
        this.candidateCount = 0;
        double transitionStart = getTransitionStart(mc);
        boolean depthAvailable = MCH_WGMapDepth.isAvailable();
        Frustrum frustum = new Frustrum();
        frustum.setPosition(RenderManager.instance.viewerPosX,
                RenderManager.instance.viewerPosY, RenderManager.instance.viewerPosZ);
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_LIGHTING_BIT);
        mc.entityRenderer.enableLightmap(event.partialTicks);
        try {
            if (depthAvailable) {
                depthModelview.clear();
                depthProjection.clear();
                GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, depthModelview);
                GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, depthProjection);
            }
            Iterator<Map.Entry<Integer, SmoothedPose>> iterator = this.smoothedPoses.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<Integer, SmoothedPose> entry = iterator.next();
                SmoothedPose pose = entry.getValue();
                if (pose.lastRenderFrame != this.renderFrame
                    && !MCH_EntityInfoClientTracker.isEntityInLatestSnapshot(entry.getKey().intValue())
                    && this.isOverlappedByLoadedAircraft(pose, entry.getKey().intValue())) {
                    continue;
                }
                if (pose.contact != null && MCH_EntityInfoClientTracker.getEntityInfo(entry.getKey().intValue()) == null
                        && now - pose.lastContactSeenMillis > TRACKER_EXIT_GRACE_MILLIS) {
                    pose.contact = null;
                    pose.report("TRACKER_EXPIRED", entry.getKey().intValue());
                }
                if (pose.visualAircraft != null && pose.visualSeenFrame != this.renderFrame) {
                    pose.visualAircraft = null;
                    pose.report("STALE_VISUAL_ENTITY", entry.getKey().intValue());
                }
                if (pose.lastRenderFrame != this.renderFrame) {
                    if (pose.contact != null) {
                        pose.update(pose.contact, now, this.renderFrame);
                    } else if (pose.visualAircraft == null) {
                        pose.report("RETIRED", entry.getKey().intValue());
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
                if (current != null && (current.worldObj != mc.theWorld || current.isDead)) {
                    pose.visualAircraft = null;
                    current = null;
                }
                if (current != null && current.isDestroyed()) {
                    pose.report("DESTROYED", entry.getKey().intValue());
                    iterator.remove();
                    continue;
                }
                if (current != null && MCH_RenderAircraft.shouldSkipRender(current)) {
                    pose.report("SKIP_RENDER_STATE", entry.getKey().intValue());
                    continue;
                }
                RenderDefinition definition = current != null
                    ? this.resolveDefinition(current) : this.resolveDefinition(pose.entityClassName, pose.entityName);
                if (definition == null) {
                    pose.report("NO_DEFINITION", entry.getKey().intValue());
                    continue;
                }
                if (definition.info.model == null) {
                    pose.report("NO_MODEL", entry.getKey().intValue());
                    continue;
                }
                // Test terrain at the true world pose. The projected draw position is only for
                // frustum and depth placement.
                double trueDistance = Math.sqrt(dx * dx + dy * dy + dz * dz);
                boolean projectedBvr = trueDistance > transitionStart;
                pose.projectedBvr = projectedBvr;
                double projectedDistance = projectedBvr && depthAvailable
                        ? MCH_CompressedDepthProjection.projected(trueDistance, transitionStart)
                        : cpuProjectedDistance(trueDistance, transitionStart, getTransitionEnd(mc));
                if (!couldContributeToFrame(frustum, definition.info, dx, dy, dz,
                        projectedBvr ? projectedDistance / trueDistance : 1.0D)) {
                    pose.report("OUTSIDE_FRUSTUM", entry.getKey().intValue());
                    continue;
                }
                RenderCandidate candidate;
                if (candidateCount == candidates.size()) {
                    candidate = new RenderCandidate();
                    candidates.add(candidate);
                } else candidate = candidates.get(candidateCount);
                candidateCount++;
                candidate.pose = pose;
                candidate.definition = definition;
                candidate.aircraft = current;
                candidate.id = entry.getKey().intValue();
                candidate.brightness = trueDistance >= transitionStart - 32.0D
                        ? distantVehicleBrightness(pose.lightmapBrightness,
                                mc.theWorld.skylightSubtracted, mc.theWorld.provider.hasNoSky)
                        : pose.lightmapBrightness;
                candidate.projected = projectedBvr;
                candidate.depthIndex = -1;
                candidate.depthConfigured = !projectedBvr || !depthAvailable
                        || configureVehicleDepth(candidate, dx, dy, dz, trueDistance, transitionStart);
            }

            int projectedCount = 0, depthCandidateCount = 0;
            for (int i = 0; i < candidateCount; i++) {
                RenderCandidate candidate = candidates.get(i);
                if (!candidate.projected) continue;
                projectedCount++;
                if (depthAvailable && candidate.depthConfigured) depthCandidateCount++;
            }
            if (depthAvailable) {
                if (depthTargets.length < depthCandidateCount * 3) depthTargets = new double[depthCandidateCount * 3];
                if (depthRadii.length < depthCandidateCount) depthRadii = new double[depthCandidateCount];
            }
            int nextDepth = 0;
            if (depthAvailable) for (int i = 0; i < candidateCount; i++) {
                RenderCandidate candidate = candidates.get(i);
                if (!candidate.projected || !candidate.depthConfigured) continue;
                candidate.depthIndex = nextDepth;
                depthTargets[nextDepth * 3] = candidate.pose.x;
                depthTargets[nextDepth * 3 + 1] = candidate.pose.y;
                depthTargets[nextDepth * 3 + 2] = candidate.pose.z;
                MCH_AircraftInfo info = candidate.definition.info;
                depthRadii[nextDepth] = Math.max(0.5D, Math.max(info.bodyWidth * 0.5D,
                        Math.max(info.markerWidth, Math.max(Math.abs(info.bbZmin), Math.abs(info.bbZmax)))));
                nextDepth++;
            }
            boolean[] ready = depthAvailable && depthCandidateCount > 0 ? MCH_WGMapDepth.prepare(
                    mc.theWorld.provider.dimensionId, RenderManager.instance.viewerPosX,
                    RenderManager.instance.viewerPosY, RenderManager.instance.viewerPosZ,
                    transitionStart, depthTargets, depthRadii, depthCandidateCount) : null;
            if (depthCandidateCount > 0 && ready == null) depthAvailable = false;
            int gpuReadyCount = 0;
            if (ready != null) for (boolean covered : ready) if (covered) gpuReadyCount++;
            if (MCH_Config.DebugLog && (projectedCount != lastBvrCount
                    || gpuReadyCount != lastGpuReadyCount || depthAvailable != lastDepthAvailable)) {
                MCH_Lib.DbgLog(true, "[FarVehicle] gpuDepthAvailable=%s bvr=%d gpuReady=%d cpuFallback=%d",
                        Boolean.valueOf(depthAvailable), Integer.valueOf(projectedCount),
                        Integer.valueOf(gpuReadyCount), Integer.valueOf(projectedCount - gpuReadyCount));
                lastBvrCount = projectedCount;
                lastGpuReadyCount = gpuReadyCount;
                lastDepthAvailable = depthAvailable;
            }
            // Nearby vehicles use Minecraft's normal depth, before the compressed
            // terrain pass can alter the shared depth buffer.
            long terrainGeneration = MCH_WGMapOcclusion.terrainGeneration();
            for (int i = 0; i < candidateCount; i++) {
                RenderCandidate candidate = candidates.get(i);
                if (!candidate.projected)
                    renderCandidate(mc, candidate, event.partialTicks, now,
                            false, false, terrainGeneration);
            }
            // Terrain must write depth before projected vehicles draw.
            boolean drawn = ready != null && MCH_WGMapDepth.draw();
            for (int i = 0; i < candidateCount; i++) {
                RenderCandidate candidate = candidates.get(i);
                if (!candidate.projected) continue;
                boolean fullyReady = drawn && candidate.depthIndex >= 0 && ready[candidate.depthIndex];
                if (fullyReady) candidate.pose.rememberGpuDepth(now,
                        RenderManager.instance.viewerPosX, RenderManager.instance.viewerPosY,
                        RenderManager.instance.viewerPosZ);
                boolean partialGpu = !fullyReady && drawn && candidate.depthIndex >= 0
                        && candidate.depthConfigured && candidate.pose.recentGpuDepth(now,
                                RenderManager.instance.viewerPosX, RenderManager.instance.viewerPosY,
                                RenderManager.instance.viewerPosZ);
                renderCandidate(mc, candidate, event.partialTicks, now,
                        fullyReady || partialGpu, fullyReady, terrainGeneration);
            }
        } finally {
            mc.entityRenderer.disableLightmap(event.partialTicks);
            GL11.glPopAttrib();
        }
    }

    private boolean configureVehicleDepth(RenderCandidate candidate, double x, double y, double z,
            double distance, double transitionStart) {
        double scale = MCH_CompressedDepthProjection.projected(distance, transitionStart) / distance;
        double eyeDistance = -(depthModelview.get(2) * x + depthModelview.get(6) * y
                + depthModelview.get(10) * z);
        if (eyeDistance <= 0.0001D) return false;
        MCH_AircraftInfo info = candidate.definition.info;
        double extent = Math.max(16.0D, Math.max(info.bodyHeight,
                Math.max(info.bodyWidth, Math.max(Math.abs(info.bbZmin), Math.abs(info.bbZmax)))));
        double low = Math.max(0.001D, eyeDistance - extent);
        double high = Math.min(MCH_CompressedDepthProjection.MAX_DISTANCE, eyeDistance + extent);
        if (high <= low) return false;
        double actualLow = windowDepth(depthModelview.get(14) - low * scale);
        double actualHigh = windowDepth(depthModelview.get(14) - high * scale);
        double desiredLow = mappedDepth(low, transitionStart);
        double desiredHigh = mappedDepth(high, transitionStart);
        double slope = (desiredHigh - desiredLow) / (actualHigh - actualLow);
        double near = desiredLow - slope * actualLow;
        double far = near + slope;
        if (!Double.isFinite(near) || !Double.isFinite(far)
                || near < 0.0D || far > 1.0D || near >= far) return false;
        candidate.depthNear = near;
        candidate.depthFar = far;
        return true;
    }

    private double mappedDepth(double eyeDistance, double transitionStart) {
        if (eyeDistance <= transitionStart)
            return windowDepth(depthModelview.get(14) - eyeDistance);
        double startDepth = windowDepth(depthModelview.get(14) - transitionStart);
        return startDepth + (1.0D - startDepth)
                * MCH_CompressedDepthProjection.depthFraction(eyeDistance, transitionStart);
    }

    private double windowDepth(double eyeZ) {
        double clipZ = depthProjection.get(10) * eyeZ + depthProjection.get(14);
        double clipW = depthProjection.get(11) * eyeZ + depthProjection.get(15);
        return (clipZ / clipW + 1.0D) * 0.5D;
    }

    private void renderCandidate(Minecraft mc, RenderCandidate candidate,
            float partialTicks, long now, boolean gpuDepth, boolean fullyReady, long terrainGeneration) {
        SmoothedPose pose = candidate.pose;
        if (gpuDepth) {
            // The depth pass handles trees per pixel. Keep its projection through a
            // brief partial batch without inheriting a whole-model CPU hide.
            pose.clearCpuTerrainOcclusion();
            if (MCH_Config.DebugLog) pose.report(fullyReady ? "GPU_DEPTH_READY" : "GPU_DEPTH_PARTIAL", candidate.id);
            if (candidate.brightness < 0) candidate.brightness = resolveLightmapBrightness(
                    mc, pose, null, partialTicks);
            double cameraDx = pose.x - RenderManager.instance.viewerPosX;
            double cameraDy = pose.y - RenderManager.instance.viewerPosY;
            double cameraDz = pose.z - RenderManager.instance.viewerPosZ;
            boolean beyondViewDistance = cameraDx * cameraDx + cameraDy * cameraDy + cameraDz * cameraDz
                    > getTransitionEnd(mc) * getTransitionEnd(mc);
            boolean layered = beyondViewDistance && precisionLayer.begin(
                    pose.x, pose.y, pose.z, candidate.definition.info,
                    RenderManager.instance.viewerPosX, RenderManager.instance.viewerPosY,
                    RenderManager.instance.viewerPosZ, getTransitionStart(mc),
                    depthModelview, depthProjection);
            if (layered) {
                boolean completed = false;
                try {
                    renderContact(mc, pose, candidate.definition, candidate.aircraft,
                            partialTicks, candidate.brightness, false, true,
                            candidate.depthNear, candidate.depthFar);
                    completed = true;
                } finally {
                    precisionLayer.finish(completed, candidate.depthNear, candidate.depthFar,
                            depthProjection.get(10), depthProjection.get(14));
                }
            } else {
                renderContact(mc, pose, candidate.definition, candidate.aircraft,
                        partialTicks, candidate.brightness, true, false,
                        candidate.depthNear, candidate.depthFar);
            }
            return;
        }
        boolean normalLoaded = candidate.aircraft != null && !candidate.projected;
        long generation = normalLoaded ? 0L : terrainGeneration;
        MCH_WGMapOcclusion.Result result = normalLoaded ? MCH_WGMapOcclusion.Result.CLEAR
                : pose.queryTerrain(mc, candidate.definition.info, partialTicks, candidate.projected,
                        now, generation, RenderManager.instance.viewerPosX,
                        RenderManager.instance.viewerPosY, RenderManager.instance.viewerPosZ);
        boolean hidden = pose.shouldSuppressForTerrain(result, now, generation,
                RenderManager.instance.viewerPosX, RenderManager.instance.viewerPosY,
                RenderManager.instance.viewerPosZ);
        if (MCH_Config.DebugLog) {
            String reason = normalLoaded ? "NORMAL_DEPTH_RENDER"
                    : terrainReason(pose, result, hidden, now, generation);
            pose.report(candidate.projected
                    ? "GPU_DEPTH_UNAVAILABLE_CPU_FALLBACK " + reason
                    : reason, candidate.id);
        }
        if (!hidden) {
            if (candidate.brightness < 0) candidate.brightness = resolveLightmapBrightness(
                    mc, pose, null, partialTicks);
            renderContact(mc, pose, candidate.definition, candidate.aircraft,
                    partialTicks, candidate.brightness, false, false,
                    candidate.depthNear, candidate.depthFar);
        }
    }

    private void renderContact(Minecraft mc, SmoothedPose pose, RenderDefinition definition,
            MCH_EntityAircraft localAircraft, float partialTicks, int lightmapBrightness,
            boolean gpuDepth, boolean precisionPass, double depthNear, double depthFar) {
        RenderManager renderManager = RenderManager.instance;
        double x = pose.x - renderManager.viewerPosX;
        double y = pose.y - renderManager.viewerPosY;
        double z = pose.z - renderManager.viewerPosZ;
        double distance = Math.sqrt(x * x + y * y + z * z);
        // Equal position/model scaling preserves screen direction and angular size without replacing projection.
        // Compressed BVR positions sit in the view-distance fog band; fog there would darken
        // contacts solely as the camera crosses the transition instead of reflecting lighting.
        double safeDistance = getTransitionStart(mc);
        double projectedDistance = gpuDepth || precisionPass
                ? MCH_CompressedDepthProjection.projected(distance, safeDistance)
                : cpuProjectedDistance(distance, safeDistance, getTransitionEnd(mc));
        double projectionScale = distance > 0.0D ? projectedDistance / distance : 1.0D;
        x *= projectionScale;
        y *= projectionScale;
        z *= projectionScale;

        float oldBrightnessX = OpenGlHelper.lastBrightnessX;
        float oldBrightnessY = OpenGlHelper.lastBrightnessY;
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT
            | GL11.GL_TEXTURE_BIT | GL11.GL_LIGHTING_BIT | GL11.GL_POLYGON_BIT | GL11.GL_FOG_BIT
            | GL11.GL_VIEWPORT_BIT);
        RenderHelper.enableStandardItemLighting();
        GL11.glPushMatrix();
        try {
            if (gpuDepth) GL11.glDepthRange(depthNear, depthFar);
            GL11.glTranslated(x, y, z);
            GL11.glScaled(projectionScale, projectionScale, projectionScale);

            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            if (precisionPass) GL11.glDepthFunc(GL11.GL_LEQUAL);
            GL11.glDepthMask(true);
            if (distance >= safeDistance - 32.0D) GL11.glDisable(GL11.GL_FOG);
            else GL11.glEnable(GL11.GL_FOG);
            GL11.glEnable(GL11.GL_LIGHTING);
            GL11.glEnable(GL11.GL_COLOR_MATERIAL);
            GL11.glEnable(GL11.GL_CULL_FACE);
            GL11.glEnable(GL11.GL_BLEND);
            if (precisionPass) {
                // The layer stores premultiplied RGB and true coverage for its final blend.
                GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                        GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
            } else {
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            }
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

    private static final class RenderCandidate {
        SmoothedPose pose;
        RenderDefinition definition;
        MCH_EntityAircraft aircraft;
        int id;
        int brightness;
        int depthIndex;
        boolean projected;
        boolean depthConfigured;
        double depthNear, depthFar;
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

    static int distantVehicleBrightness(int packedLight, int skylightSubtracted, boolean noSky) {
        if (packedLight < 0 || noSky) return packedLight;
        // A loaded aircraft returns zero light when its chunk drops, while a snapshot carries
        // the raw block sample. Neither should black out an exposed distant model at handoff.
        int ambientSky = Math.max(0, 15 - skylightSubtracted) << 4;
        return (Math.max((packedLight >>> 16) & 0xffff, ambientSky) << 16)
                | (packedLight & 0xffff);
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

    private boolean isOverlappedByLoadedAircraft(SmoothedPose pose, int oldId) {
        if (pose.aircraftUuid != null) {
            MCH_EntityAircraft same = this.loadedByUuid.get(pose.aircraftUuid);
            return same != null && same.getEntityId() != oldId;
        }
        for (MCH_EntityAircraft aircraft : this.loadedAircraft) {
            if (aircraft.getEntityId() == oldId || aircraft.isDead || aircraft.getAcInfo() == null
                || !aircraft.getClass().getName().equals(pose.entityClassName)
                || !aircraft.getAcInfo().name.equals(pose.entityName)) {
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

    private SmoothedPose getSmoothedPose(MCH_EntityAircraft aircraft) {
        int entityId = aircraft.getEntityId();
        SmoothedPose pose = this.smoothedPoses.get(Integer.valueOf(entityId));
        UUID uuid = aircraft.getUniqueID();
        if (pose == null && uuid != null) {
            Iterator<Map.Entry<Integer, SmoothedPose>> iterator = this.smoothedPoses.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<Integer, SmoothedPose> old = iterator.next();
                if (uuid.equals(old.getValue().aircraftUuid)) {
                    pose = old.getValue();
                    iterator.remove();
                    break;
                }
            }
            if (pose != null) this.smoothedPoses.put(Integer.valueOf(entityId), pose);
        }
        return pose != null ? pose : this.getSmoothedPose(entityId);
    }

    private static boolean couldContributeToFrame(Frustrum frustum, MCH_AircraftInfo info,
            double dx, double dy, double dz, double scale) {
        if (!Double.isFinite(dx) || !Double.isFinite(dy) || !Double.isFinite(dz)
                || !Double.isFinite(scale) || scale <= 0.0D) return false;
        // Match renderContact's camera-relative projection. Generous model bounds keep edge vehicles.
        double x = RenderManager.instance.viewerPosX + dx * scale;
        double y = RenderManager.instance.viewerPosY + dy * scale;
        double z = RenderManager.instance.viewerPosZ + dz * scale;
        double radius = Math.max(3.0D, Math.max(info.bodyWidth, Math.max(Math.abs(info.bbZmin),
                Math.abs(info.bbZmax)))) * scale;
        double height = Math.max(3.0D, Math.max(info.bodyHeight, info.markerHeight)) * scale;
        return frustum.isBoxInFrustum(x - radius, y - radius, z - radius,
                x + radius, y + height + radius, z + radius);
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

    static double cpuProjectedDistance(double distance, double transitionStart, double transitionEnd) {
        // In the overlap with normal chunks, keep their physical depth when GPU
        // coverage is incomplete. Beyond it, retain the existing CPU fallback.
        return distance <= transitionEnd ? distance : transitionStart;
    }

    private static double horizontalChunkDistance(double x, double z) {
        return Math.max(Math.abs(x), Math.abs(z));
    }

    private static String terrainReason(SmoothedPose pose, MCH_WGMapOcclusion.Result result,
            boolean hidden, long now, long generation) {
        if (result == MCH_WGMapOcclusion.Result.BLOCKED) return "HIDDEN_TERRAIN_BLOCKED";
        if (result == MCH_WGMapOcclusion.Result.CLEAR) return "VISIBLE_CLEAR";
        if (hidden) return "HELD_PREVIOUS_BLOCKED";
        if (pose.terrainConfirmedMillis > 0L && !pose.terrainBlocked
                && pose.terrainConfirmedGeneration == generation
                && now - pose.terrainConfirmedMillis <= MISSING_HOLD_MILLIS) return "HELD_PREVIOUS_VISIBLE";
        if (result == MCH_WGMapOcclusion.Result.MISSING_DATA) return "OCCLUSION_UNKNOWN_MISSING_DATA";
        if (result == MCH_WGMapOcclusion.Result.UNCERTAIN_BLOCK) return "OCCLUSION_UNKNOWN_BLOCK_CLASS";
        return "OCCLUSION_UNAVAILABLE";
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
        private int visualSeenFrame;
        private long lastContactSeenMillis;
        private long handoffUntilMillis;
        private int lightmapBrightness = -1;
        private boolean terrainBlocked;
        private long terrainConfirmedMillis;
        private long terrainConfirmedGeneration;
        private double confirmedX, confirmedY, confirmedZ;
        private double confirmedCameraX, confirmedCameraY, confirmedCameraZ;
        private MCH_WGMapOcclusion.Result cachedTerrain;
        private long cachedTerrainMillis, cachedTerrainGeneration;
        private double cachedX, cachedY, cachedZ;
        private double cachedCameraX, cachedCameraY, cachedCameraZ;
        private boolean cachedProjected;
        private long lastGpuReadyMillis;
        private double lastGpuCameraX, lastGpuCameraY, lastGpuCameraZ;
        private double lastGpuTargetX, lastGpuTargetY, lastGpuTargetZ;
        private boolean projectedBvr;
        private String lastReason;
        private long lastUpdateMillis;
        private int lastRenderFrame;
        private boolean initialized;

        private void observe(MCH_EntityInfo contact, long now) {
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
            this.lastContactSeenMillis = now;
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
            if (this.aircraftUuid != null && !this.aircraftUuid.equals(aircraft.getUniqueID())) {
                this.contact = null;
                this.initialized = false;
                this.clearTerrainOcclusion();
            }
            this.aircraftUuid = aircraft.getUniqueID();
            double targetX = aircraft.lastTickPosX + (aircraft.posX - aircraft.lastTickPosX) * partialTicks;
            double targetY = aircraft.lastTickPosY + (aircraft.posY - aircraft.lastTickPosY) * partialTicks;
            double targetZ = aircraft.lastTickPosZ + (aircraft.posZ - aircraft.lastTickPosZ) * partialTicks;
            float targetYaw = interpolateAngle(aircraft.prevRotationYaw, aircraft.getRotYaw(), partialTicks);
            float targetPitch = aircraft.calcRotPitch(partialTicks);
            float targetRoll = interpolateAngle(aircraft.prevRotationRoll, aircraft.getRotRoll(), partialTicks);
            double dx = targetX - this.x, dy = targetY - this.y, dz = targetZ - this.z;
            if (this.visualAircraft != aircraft || this.visualSeenFrame != renderFrame - 1) {
                this.handoffUntilMillis = now + 180L;
            }
            if (!this.initialized || dx * dx + dy * dy + dz * dz > 16.0D * 16.0D
                    || now >= this.handoffUntilMillis) {
                this.x = targetX; this.y = targetY; this.z = targetZ;
                this.yaw = targetYaw; this.pitch = targetPitch; this.roll = targetRoll;
                this.turretYaw = aircraft.getLastRiderYaw();
                this.turretPitch = aircraft.getLastRiderPitch();
            } else {
                float factor = (float)(1.0D - Math.exp(-Math.max(1L, now - this.lastUpdateMillis) / 65.0D));
                this.x += dx * factor; this.y += dy * factor; this.z += dz * factor;
                this.yaw = interpolateAngle(this.yaw, targetYaw, factor);
                this.pitch = interpolateAngle(this.pitch, targetPitch, factor);
                this.roll = interpolateAngle(this.roll, targetRoll, factor);
                this.turretYaw = interpolateAngle(this.turretYaw, aircraft.getLastRiderYaw(), factor);
                this.turretPitch = interpolateAngle(this.turretPitch, aircraft.getLastRiderPitch(), factor);
            }
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

        private MCH_WGMapOcclusion.Result queryTerrain(Minecraft mc, MCH_AircraftInfo info,
                float partialTicks, boolean projected, long now, long generation,
                double cameraX, double cameraY, double cameraZ) {
            long age = now - this.cachedTerrainMillis;
            long lifetime = this.cachedTerrain == MCH_WGMapOcclusion.Result.CLEAR
                    || this.cachedTerrain == MCH_WGMapOcclusion.Result.BLOCKED
                    ? OCCLUSION_RESULT_CACHE_MILLIS : OCCLUSION_RETRY_MILLIS;
            if (this.cachedTerrain != null && age >= 0 && age < lifetime
                    && this.cachedTerrainGeneration == generation && this.cachedProjected == projected
                    && close(this.x, this.y, this.z, this.cachedX, this.cachedY, this.cachedZ, 0.25D)
                    && close(cameraX, cameraY, cameraZ,
                            this.cachedCameraX, this.cachedCameraY, this.cachedCameraZ, 0.25D)) {
                return this.cachedTerrain;
            }
            MCH_WGMapOcclusion.Result result = MCH_WGMapOcclusion.traceProjectedSamples(
                    mc, info, this.x, this.y, this.z, partialTicks);
            this.cachedTerrain = result;
            this.cachedTerrainMillis = now;
            this.cachedTerrainGeneration = generation;
            this.cachedProjected = projected;
            this.cachedX = this.x; this.cachedY = this.y; this.cachedZ = this.z;
            this.cachedCameraX = cameraX; this.cachedCameraY = cameraY; this.cachedCameraZ = cameraZ;
            return result;
        }

        private boolean shouldSuppressForTerrain(MCH_WGMapOcclusion.Result result, long nowMillis,
                long generation, double cameraX, double cameraY, double cameraZ) {
            if (result == MCH_WGMapOcclusion.Result.BLOCKED) {
                this.terrainBlocked = true;
                this.rememberTerrain(nowMillis, generation, cameraX, cameraY, cameraZ);
                return true;
            }
            if (result == MCH_WGMapOcclusion.Result.CLEAR) {
                this.terrainBlocked = false;
                this.rememberTerrain(nowMillis, generation, cameraX, cameraY, cameraZ);
                return false;
            }
            long hold = result == MCH_WGMapOcclusion.Result.UNCERTAIN_BLOCK
                    ? UNCERTAIN_HOLD_MILLIS : MISSING_HOLD_MILLIS;
            // UNKNOWN means no reliable answer, never a newly confirmed mountain.
            return this.terrainBlocked && nowMillis - this.terrainConfirmedMillis <= hold
                    && generation == this.terrainConfirmedGeneration
                    && close(this.x, this.y, this.z, this.confirmedX, this.confirmedY, this.confirmedZ, 1.0D)
                    && close(cameraX, cameraY, cameraZ,
                            this.confirmedCameraX, this.confirmedCameraY, this.confirmedCameraZ, 1.0D);
        }

        private void rememberTerrain(long now, long generation,
                double cameraX, double cameraY, double cameraZ) {
            this.terrainConfirmedMillis = now;
            this.terrainConfirmedGeneration = generation;
            this.confirmedX = this.x; this.confirmedY = this.y; this.confirmedZ = this.z;
            this.confirmedCameraX = cameraX; this.confirmedCameraY = cameraY; this.confirmedCameraZ = cameraZ;
        }

        private void clearCpuTerrainOcclusion() {
            this.terrainBlocked = false;
            this.terrainConfirmedMillis = 0L;
            this.cachedTerrain = null;
        }

        private void clearTerrainOcclusion() {
            this.clearCpuTerrainOcclusion();
            this.lastGpuReadyMillis = 0L;
        }

        private void rememberGpuDepth(long now,
                double cameraX, double cameraY, double cameraZ) {
            this.lastGpuReadyMillis = now;
            this.lastGpuCameraX = cameraX; this.lastGpuCameraY = cameraY; this.lastGpuCameraZ = cameraZ;
            this.lastGpuTargetX = this.x; this.lastGpuTargetY = this.y; this.lastGpuTargetZ = this.z;
        }

        private boolean recentGpuDepth(long now,
                double cameraX, double cameraY, double cameraZ) {
            return this.lastGpuReadyMillis > 0L && now >= this.lastGpuReadyMillis
                    && now - this.lastGpuReadyMillis <= GPU_PARTIAL_HOLD_MILLIS
                    && close(cameraX, cameraY, cameraZ,
                            this.lastGpuCameraX, this.lastGpuCameraY, this.lastGpuCameraZ, 8.0D)
                    && close(this.x, this.y, this.z,
                            this.lastGpuTargetX, this.lastGpuTargetY, this.lastGpuTargetZ, 4.0D);
        }

        private void report(String reason, int entityId) {
            if (MCH_Config.DebugLog && !reason.equals(this.lastReason)) {
                MCH_Lib.DbgLog(true, "[FarVehicle] id=%d uuid=%s mode=%s %s",
                        Integer.valueOf(entityId), this.aircraftUuid,
                        this.projectedBvr ? "PROJECTED_BVR" : "NORMAL_DEPTH_RENDER", reason);
                this.lastReason = reason;
            }
        }

        private static boolean close(double x, double y, double z,
                double otherX, double otherY, double otherZ, double limit) {
            double dx = x - otherX, dy = y - otherY, dz = z - otherZ;
            return dx * dx + dy * dy + dz * dz <= limit * limit;
        }

        private static float interpolateAngle(float current, float target, float factor) {
            float delta = (target - current) % 360.0F;
            if (delta > 180.0F) delta -= 360.0F;
            if (delta < -180.0F) delta += 360.0F;
            return current + delta * factor;
        }
    }
}
