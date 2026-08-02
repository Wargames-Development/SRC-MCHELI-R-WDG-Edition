package mcheli.render;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import mcheli.MCH_EntityInfo;
import mcheli.MCH_EntityInfoClientTracker;
import mcheli.MCH_RadarDebug;
import mcheli.MCH_RWRThreatEvent;
import mcheli.aircraft.MCH_AircraftInfo;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.aircraft.MCH_EntitySeat;
import mcheli.helicopter.MCH_EntityHeli;
import mcheli.plane.MCP_EntityPlane;
import mcheli.tank.MCH_EntityTank;
import mcheli.uav.MCH_EntityUavStation;
import mcheli.vehicle.MCH_EntityVehicle;
import mcheli.vector.Vector3f;
import mcheli.weapon.MCH_WeaponInfo;
import mcheli.weapon.MCH_WeaponInfoManager;
import mcheli.weapon.MCH_WeaponSet;
import mcheli.wrapper.W_MOD;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
public class MCH_RenderBVRLockBox {
    private static final ResourceLocation FRAME = new ResourceLocation(W_MOD.DOMAIN, "textures/bvrlockbox.png");
    private static final ResourceLocation MSL = new ResourceLocation(W_MOD.DOMAIN, "textures/msl.png");
    private static final int DEFAULT_BOX_SIZE = 12;
    private static final int SELECTED_BOX_SIZE = 16;
    private static final int LOCKED_BOX_SIZE = 20;
    // Complete the world-space to screen-space handoff before vanilla's far-plane
    // clipping can remove the 3D marker. This keeps the contact box continuous
    // while the actual vehicle model enters or leaves normal render distance.
    private static final double HUD_BLEND_START_RANGE = 160.0D;
    private static final double HUD_BLEND_END_RANGE = 220.0D;
    private static final double HUD_EDGE_MARGIN = 26.0D;
    private static final int HUD_OVERLAP_STEP = 12;
    public static Map<Integer, MCH_EntityInfo> currentLockedEntities = new HashMap<>();

    // Best “hard lock” candidate this frame (red box target)
    public static volatile MCH_EntityInfo bestLockedEntity = null;
    public static volatile long bestLockedEntityTimeMs = 0L;

    private static void drawTexturedQuad2D(double x, double y, float size) {
        Tessellator tess = Tessellator.instance;
        float half = size * 0.5f;

        // x,y is center in screen coords
        tess.startDrawingQuads();
        tess.addVertexWithUV(x - half, y + half, 0, 0, 1);
        tess.addVertexWithUV(x + half, y + half, 0, 1, 1);
        tess.addVertexWithUV(x + half, y - half, 0, 1, 0);
        tess.addVertexWithUV(x - half, y - half, 0, 0, 0);
        tess.draw();
    }
    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return;
    }

    private double[] worldToScreen(Vector3f pos, float partialTicks) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityLivingBase viewer = mc.renderViewEntity instanceof EntityLivingBase
            ? (EntityLivingBase) mc.renderViewEntity
            : mc.thePlayer;
        if (viewer == null) return new double[]{-1, -1, -1, -1, -1};

        Vector3f camPos = new Vector3f(
                (float) RenderManager.renderPosX,
                (float) RenderManager.renderPosY,
                (float) RenderManager.renderPosZ
        );
        Vector3f rPos = new Vector3f();
        Vector3f.sub(pos, camPos, rPos);
        // Compute forward vector from yaw/pitch (works for any Entity)
        float yaw = viewer.rotationYaw;
        float pitch = viewer.rotationPitch;

        double yawRad = Math.toRadians(yaw);
        double pitchRad = Math.toRadians(pitch);

        Vec3 fwdV3 = Vec3.createVectorHelper(
                -Math.sin(yawRad) * Math.cos(pitchRad),
                -Math.sin(pitchRad),
                Math.cos(yawRad) * Math.cos(pitchRad)
        );
        Vector3f F = new Vector3f((float) fwdV3.xCoord, (float) fwdV3.yCoord, (float) fwdV3.zCoord);
        if (mc.gameSettings.thirdPersonView == 2) {
            // Front third-person camera looks opposite to the player's facing direction.
            F.negate();
        }
        F.normalise();
        Vector3f worldUp = new Vector3f(0, 1, 0);
        Vector3f R = new Vector3f();
        Vector3f.cross(F, worldUp, R);
        if (R.lengthSquared() < 1e-5f) {
            float yawRad2 = (float) Math.toRadians(viewer.rotationYaw + 90.0f);
            R.set((float) Math.cos(yawRad2), 0f, (float) -Math.sin(yawRad2));
        }
        R.normalise();
        Vector3f U = new Vector3f();
        Vector3f.cross(R, F, U);
        U.normalise();

        // Apply camera roll so screen axes match what you see
        float rollDeg = 0.0F;
        MCH_EntityAircraft ac = MCH_EntityAircraft.getAircraft_RiddenOrControl(viewer);
        if (ac != null) {
            rollDeg = getViewRollDeg(mc, ac, partialTicks);
        }
        float rollRad = (float)Math.toRadians(rollDeg);
        float c = (float)Math.cos(rollRad);
        float s = (float)Math.sin(rollRad);

// R' = R*c + U*s
// U' = U*c - R*s
        Vector3f Rr = new Vector3f(
                R.x * c + U.x * s,
                R.y * c + U.y * s,
                R.z * c + U.z * s
        );
        Vector3f Ur = new Vector3f(
                U.x * c - R.x * s,
                U.y * c - R.y * s,
                U.z * c - R.z * s
        );
        R.set(Rr.x, Rr.y, Rr.z);
        U.set(Ur.x, Ur.y, Ur.z);
        float dx = Vector3f.dot(rPos, R);
        float dy = Vector3f.dot(rPos, U);
        float dz = Vector3f.dot(rPos, F);
        double projectionDepth = Math.max(1.0E-4D, Math.abs((double)dz));
        double fovDeg = mc.gameSettings.fovSetting;
        double tanHalfFov = Math.tan(Math.toRadians(fovDeg) * 0.5);
        double aspect = (double) mc.displayWidth / (double) mc.displayHeight;
        double ndcX = (dx / projectionDepth) / (aspect * tanHalfFov);
        double ndcY = (dy / projectionDepth) / (tanHalfFov);
        ScaledResolution sc = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
        double cx = sc.getScaledWidth() * 0.5;
        double cy = sc.getScaledHeight() * 0.5;
        double screenX = cx + ndcX * cx;
        double screenY = cy - ndcY * cy;
        return new double[]{
            screenX, screenY,
            screenX - cx, screenY - cy, dz
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
        MCH_AircraftInfo acInfo = ac.getAcInfo();
        if (wi == null || acInfo == null || (!acInfo.enableBVR && !isGmtiMode(acInfo))) return;
        if (!acInfo.enableRadar || !ac.isRadarEnabledRuntime()) return;
        if (wi.antiRadiationMissile) {
            renderArmNarrowBandBoxes(mc, player, ac, event.partialTicks);
        }
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
        currentLockedEntities.clear();
        int fireControlLockedId = MCH_RenderLeadCircle.getLeadLockedTargetId(ac);
        int radarTrackingId = MCH_RenderRWR.getRadarTrackingTargetId(ac);
        int radarSelectedId = MCH_RenderRWR.getRadarSelectedTargetId(ac);
        sortTargetsByDisplayPriority(entities, radarTrackingId, radarSelectedId, fireControlLockedId);
        List<HudRect> occupiedHudRects = new ArrayList<HudRect>();
        int renderedTargetCount = 0;
        int highlightedTargetCount = 0;
        int selectedHitCount = 0;
        int trackingHitCount = 0;
        int skippedByCanRender = 0;
        int skippedByJamming = 0;
        int skippedByRadarVisible = 0;
        int skippedByMissileRange = 0;
        int skippedByTooNear = 0;
        double missileDisplayMaxRange = 4096.0D;
        if (acInfo != null && acInfo.radarMaxTargetRange > 0.0F) {
            missileDisplayMaxRange = Math.min(4096.0D, acInfo.radarMaxTargetRange);
        }
        boolean bvrDebugEnabled = MCH_RadarDebug.isBvrDebugEnabled();
        boolean bvrDebugVerbose = MCH_RadarDebug.isBvrDebugVerbose();
        boolean bvrDebugTick = bvrDebugEnabled && ac.worldObj != null && ac.worldObj.getTotalWorldTime() % 10L == 0L;
        for (MCH_EntityInfo entity : entities) {
            boolean isRadarTracking = entity.entityId == radarTrackingId;
            boolean isRadarSelected = entity.entityId == radarSelectedId;
            boolean isRadarSelectedOrTracking = isRadarTracking || isRadarSelected;
            boolean isFireControlLocked = entity.entityId == fireControlLockedId;
            boolean isPriorityTarget = isRadarSelectedOrTracking || isFireControlLocked;
            if (!isPriorityTarget && !canRenderEntity(entity, player, wi, acInfo)) {
                skippedByCanRender++;
                if (bvrDebugTick && (bvrDebugVerbose || isFireControlLocked)) {
                    double distDbg = Math.sqrt(entity.getDistanceSqToEntity(ac));
                    MCH_RadarDebug.traceBvr(ac.worldObj, ac,
                        "bvr-filter id=%d reason=canRenderEntity_false dist=%.1f class=%s",
                        entity.entityId, distDbg, entity.entityClassName);
                }
                continue;
            }
            if(ac.jammingTick > 0) {
                skippedByJamming++;
                if (bvrDebugTick && (bvrDebugVerbose || isRadarSelectedOrTracking || isFireControlLocked)) {
                    MCH_RadarDebug.traceBvr(ac.worldObj, ac,
                        "bvr-filter id=%d reason=jamming jammingTick=%d sel=%s trk=%s fire=%s",
                        entity.entityId, ac.jammingTick,
                        String.valueOf(isRadarSelected), String.valueOf(isRadarTracking), String.valueOf(isFireControlLocked));
                }
                continue;
            }
            if (!isPriorityTarget && !MCH_RenderRWR.isRadarContactVisible(ac, player, entity, partialTicks)) {
                skippedByRadarVisible++;
                if (bvrDebugTick && (bvrDebugVerbose || isRadarSelectedOrTracking || isFireControlLocked)) {
                    double distDbg = Math.sqrt(entity.getDistanceSqToEntity(ac));
                    MCH_RadarDebug.traceBvr(ac.worldObj, ac,
                        "bvr-filter id=%d reason=radarContactInvisible dist=%.1f sel=%s trk=%s fire=%s",
                        entity.entityId, distDbg,
                        String.valueOf(isRadarSelected), String.valueOf(isRadarTracking), String.valueOf(isFireControlLocked));
                }
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
            boolean hardLock = isRadarTracking || isFireControlLocked;
            boolean selected = isRadarSelected && !hardLock;
            boolean highlight = hardLock || selected;
            boolean lock = false;
            float alpha = highlight ? 1.0F : 0.4F;
            if (angle <= 90) {
                alpha = 1.0f;
                if (!isMSL) currentLockedEntities.put(entity.entityId, entity);
                if (distSq <= wi.maxLockOnRange * wi.maxLockOnRange && angle <= wi.maxLockOnAngle) {
                    lock = true;
                }
            } else if (!highlight && angle <= 100.0) alpha = 1.0f;
            else if (!highlight && angle <= 110.0) alpha = 0.8f;
            else if (!highlight && angle <= 120.0) alpha = 0.6f;
            if (isMSL && dist >= missileDisplayMaxRange) {
                skippedByMissileRange++;
                if (bvrDebugTick && (bvrDebugVerbose || isRadarSelectedOrTracking || isFireControlLocked)) {
                    MCH_RadarDebug.traceBvr(ac.worldObj, ac,
                        "bvr-filter id=%d reason=missileRange dist=%.1f max=%.1f sel=%s trk=%s fire=%s",
                        entity.entityId, dist, missileDisplayMaxRange,
                        String.valueOf(isRadarSelected), String.valueOf(isRadarTracking), String.valueOf(isFireControlLocked));
                }
                continue;
            }
            double vdist = Math.sqrt(x * x + y * y + z * z);
            if (vdist < 1e-4) {
                skippedByTooNear++;
                continue;
            }
            float sPerPixel = (float) ((2.0 * vdist * Math.tan(fovRad * 0.5)) / sc.getScaledHeight_double());
            String text;
            int color;
            String targetName = rwrResult.name == null ? "" : rwrResult.name;
            renderedTargetCount++;
            if (isRadarSelected) selectedHitCount++;
            if (isRadarTracking) trackingHitCount++;
            if (isRadarSelectedOrTracking || isFireControlLocked) highlightedTargetCount++;
            if (targetName.isEmpty()) {
                targetName = "UNKNOWN";
            }
            text = String.format("[%s %s]", targetName, formatRange(dist));
            String detailText = null;
            if (isPriorityTarget) {
                double altitudeDifference = gy - ac.posY;
                double closureRate = calculateClosureRate(entity, ac);
                detailText = String.format("ALT %+.0fm  VC %+.0fm/s", altitudeDifference, closureRate);
            }
            color = isMSL ? 0xFF4040 : 0x00FF00;
            boolean drawText = true;
            MCH_WeaponSet currentWs = ac.getCurrentWeapon(player);
            MCH_WeaponInfo currentWi = currentWs != null ? currentWs.getInfo() : null;
            boolean dataLinkMode = currentWi != null && !currentWi.antiRadiationMissile && (
                (currentWi.enableDataLink && (currentWi.onlyDataLink || currentWs.isDataLinkMode()) && (currentWi.activeRadar || currentWi.passiveRadar || currentWi.semiActiveRadar))
                ||
                (currentWi.enableDataLink && (currentWi.onlyDataLink || currentWs.isDataLinkMode()) && ("aamissile".equals(currentWi.type) || "atmissile".equals(currentWi.type))
                    && currentWi.isHeatSeekerMissile && !currentWi.activeRadar && !currentWi.passiveRadar && !currentWi.semiActiveRadar)
            );
            boolean isHeatSeekerDatalink = currentWi != null && currentWi.enableDataLink && ("aamissile".equals(currentWi.type) || "atmissile".equals(currentWi.type))
                && currentWi.isHeatSeekerMissile && !currentWi.activeRadar && !currentWi.passiveRadar && !currentWi.semiActiveRadar && currentWs.isDataLinkMode();
            boolean inMissileFov = currentWi != null && angle <= currentWi.getHudPreferredMissileFovDeg();
            boolean showDataLinkRings = dataLinkMode && inMissileFov && isPriorityTarget;
            String stateText = hardLock ? "LOCK" : (selected ? "SELECT" : null);
            double hudBlend = getHudBlendFactor(dist);
            float worldAlpha = (float)(alpha * (1.0D - hudBlend));
            float hudAlpha = (float)(alpha * hudBlend);
            int markerColor = hardLock ? 0xFF2020 : (selected ? 0xFFC000 : (isMSL ? 0xFF4040 : 0x00FF00));
            int markerSize = hardLock ? LOCKED_BOX_SIZE : (selected ? SELECTED_BOX_SIZE : DEFAULT_BOX_SIZE);
            color = highlight ? markerColor : color;
            if (worldAlpha > 0.01F) {
                int heatSeekerRingColor = isHeatSeekerDatalink ? (dist <= 350.0D ? 0xFF0000 : 0xFFFFFF) : 0;
                drawBillboardMarker(mc, rm, rollDeg, x, y + 0.2D, z, sPerPixel, isMSL, markerColor, highlight, hardLock, markerSize, showDataLinkRings, isHeatSeekerDatalink, heatSeekerRingColor, stateText, text, detailText, color, drawText, worldAlpha);
            }
            HudProjection projection = projectRelativeToHud((float)gx, (float)(gy + 0.2D), (float)gz, sc, mc, partialTicks);
            float effectiveHudAlpha = projection != null && projection.edgeClamped ? alpha : hudAlpha;
            if (projection != null && effectiveHudAlpha > 0.01F) {
                int heatSeekerRingColor = isHeatSeekerDatalink ? (dist <= 350.0D ? 0xFF0000 : 0xFFFFFF) : 0;
                projection = avoidHudOverlap(projection, occupiedHudRects, mc, markerSize, stateText, text, detailText, drawText);
                drawHudMarker(mc, projection, isMSL, markerColor, highlight, hardLock, markerSize, showDataLinkRings, isHeatSeekerDatalink, heatSeekerRingColor, stateText, text, detailText, color, drawText, effectiveHudAlpha);
            }
        }
        if (MCH_RadarDebug.isEnabled() && MCH_RadarDebug.isVerbose() && ac.worldObj != null && ac.worldObj.getTotalWorldTime() % 10L == 0L) {
            MCH_RadarDebug.traceVerbose(ac.worldObj, ac,
                "bvr acId=%d radarOn=%s selectedId=%d trackingId=%d fireLockId=%d rendered=%d hl=%d selHit=%d trkHit=%d",
                ac.getEntityId(), String.valueOf(acInfo.enableRadar), radarSelectedId, radarTrackingId, fireControlLockedId,
                renderedTargetCount, highlightedTargetCount, selectedHitCount, trackingHitCount);
        }
        if (bvrDebugTick) {
            MCH_RadarDebug.traceBvr(ac.worldObj, ac,
                "bvr-scan acId=%d total=%d rendered=%d skipCan=%d skipJam=%d skipVisible=%d skipMslRange=%d skipNear=%d selectedId=%d trackingId=%d fireLockId=%d mslMax=%.1f",
                ac.getEntityId(), entities.size(), renderedTargetCount,
                skippedByCanRender, skippedByJamming, skippedByRadarVisible, skippedByMissileRange, skippedByTooNear,
                radarSelectedId, radarTrackingId, fireControlLockedId, missileDisplayMaxRange);
        }
    }

    private void renderArmNarrowBandBoxes(Minecraft mc, EntityPlayer player, MCH_EntityAircraft ac, float partialTicks) {
        ScaledResolution sc = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
        RenderManager rm = RenderManager.instance;
        float rollDeg = getViewRollDeg(mc, ac, partialTicks);
        double fovRad = Math.toRadians(mc.gameSettings.fovSetting);
        MCH_WeaponSet ws = ac.getCurrentWeapon(player);
        MCH_WeaponInfo wi = ws != null ? ws.getInfo() : null;
        if (wi == null) {
            return;
        }
        boolean armNarrowBandMode = MCH_RenderRWR.isArmNarrowBandCurrentWeapon(ac, player);
        List<MCH_RenderRWR.ArmBvrDisplayContact> contacts = MCH_RenderRWR.getArmBvrDisplayContacts(ac, player);
        List<HudRect> occupiedHudRects = new ArrayList<HudRect>();
        for (MCH_RenderRWR.ArmBvrDisplayContact contact : contacts) {
            MCH_EntityInfo targetInfo = MCH_EntityInfoClientTracker.getEntityInfo(contact.emitterId);
            if (targetInfo == null || targetInfo.entityClassName == null) {
                continue;
            }
            if (!isArmContactClassNameAllowedByWeaponType(targetInfo.entityClassName, wi)) {
                continue;
            }
            double gx = interpolate(targetInfo.posX, targetInfo.lastTickPosX, partialTicks);
            double gy = interpolate(targetInfo.posY, targetInfo.lastTickPosY, partialTicks) + 1.0D;
            double gz = interpolate(targetInfo.posZ, targetInfo.lastTickPosZ, partialTicks);
            double x = gx - rm.viewerPosX;
            double y = gy - rm.viewerPosY;
            double z = gz - rm.viewerPosZ;
            double dist = Math.sqrt(targetInfo.getDistanceSqToEntity(ac));
            double angle = calculateAngle(wi.enableHMS ? player : ac, gx, gy, gz);
            boolean hardLock = (armNarrowBandMode && contact.tracking) || contact.threatMode == MCH_RWRThreatEvent.MODE_STT;
            boolean selected = armNarrowBandMode && contact.selected && !hardLock;
            boolean highlight = hardLock || selected;
            float alpha = highlight ? 1.0F : 0.4F;
            if (angle <= 90.0D) {
                alpha = 1.0F;
            } else if (!highlight && angle <= 100.0D) {
                alpha = 1.0F;
            } else if (!highlight && angle <= 110.0D) {
                alpha = 0.8F;
            } else if (!highlight && angle <= 120.0D) {
                alpha = 0.6F;
            }
            if (alpha <= 0.01F) {
                continue;
            }
            double vdist = Math.sqrt(x * x + y * y + z * z);
            if (vdist < 1e-4) {
                continue;
            }
            float sPerPixel = (float) ((2.0 * vdist * Math.tan(fovRad * 0.5)) / sc.getScaledHeight_double());
            String stateText = hardLock ? "LOCK" : (selected ? "SELECT" : null);
            String text = String.format("[%s %s]", contact.name == null ? "UNKNOWN" : contact.name, formatRange(dist));
            String detailText = null;
            if (highlight) {
                double altitudeDifference = gy - ac.posY;
                double closureRate = calculateClosureRate(targetInfo, ac);
                detailText = String.format("ALT %+.0fm  VC %+.0fm/s", altitudeDifference, closureRate);
            }
            int markerColor = hardLock ? 0xFF2020 : (selected ? 0xFFC000 : contact.color);
            int textColor = highlight ? markerColor : contact.color;
            int markerSize = hardLock ? LOCKED_BOX_SIZE : (selected ? SELECTED_BOX_SIZE : DEFAULT_BOX_SIZE);
            boolean showArmLockRings = armNarrowBandMode && contact.tracking;
            double hudBlend = getHudBlendFactor(dist);
            float worldAlpha = (float)(alpha * (1.0D - hudBlend));
            float hudAlpha = (float)(alpha * hudBlend);
            if (worldAlpha > 0.01F) {
                drawBillboardMarker(mc, rm, rollDeg, x, y + 0.2D, z, sPerPixel, false, markerColor, highlight, hardLock, markerSize, showArmLockRings, false, 0, stateText, text, detailText, textColor, true, worldAlpha);
            }
            HudProjection projection = projectRelativeToHud((float)gx, (float)(gy + 0.2D), (float)gz, sc, mc, partialTicks);
            float effectiveHudAlpha = projection != null && projection.edgeClamped ? alpha : hudAlpha;
            if (projection != null && effectiveHudAlpha > 0.01F) {
                projection = avoidHudOverlap(projection, occupiedHudRects, mc, markerSize, stateText, text, detailText, true);
                drawHudMarker(mc, projection, false, markerColor, highlight, hardLock, markerSize, showArmLockRings, false, 0, stateText, text, detailText, textColor, true, effectiveHudAlpha);
            }
        }
    }

    private boolean isArmContactClassNameAllowedByWeaponType(String entityClassName, MCH_WeaponInfo wi) {
        if (entityClassName == null || wi == null || !wi.antiRadiationMissile) {
            return false;
        }
        String type = wi.type != null ? wi.type.toLowerCase() : "";
        if ("aamissile".equals(type)) {
            return entityClassName.contains("MCP_EntityPlane") || entityClassName.contains("MCH_EntityHeli");
        }
        if ("atmissile".equals(type)) {
            return entityClassName.contains("MCH_EntityTank") || entityClassName.contains("MCH_EntityVehicle");
        }
        return true;
    }

    private void drawBillboardMarker(Minecraft mc, RenderManager rm, float rollDeg, double x, double y, double z, float sPerPixel,
                                     boolean isMSL, int markerColor, boolean highlight, boolean hardLock, int markerSize, boolean showDataLinkRings, boolean isHeatSeekerDatalink, int heatSeekerRingColor, String stateText,
                                     String text, String detailText, int textColor, boolean drawText, float alpha) {
        GL11.glPushMatrix();
        GL11.glTranslated(x, y, z);
        GL11.glRotatef(-rm.playerViewY, 0.0F, 1.0F, 0.0F);
        GL11.glRotatef(rm.playerViewX, 1.0F, 0.0F, 0.0F);
        GL11.glRotatef(-rollDeg, 0.0F, 0.0F, 1.0F);
        GL11.glScalef(-sPerPixel, -sPerPixel, sPerPixel);
        drawMarkerCore(mc, isMSL, markerColor, highlight, hardLock, markerSize, showDataLinkRings, isHeatSeekerDatalink, heatSeekerRingColor, stateText, text, detailText, textColor, drawText, alpha);
        GL11.glPopMatrix();
    }

    private void drawHudMarker(Minecraft mc, HudProjection projection, boolean isMSL, int markerColor, boolean highlight, boolean hardLock, int markerSize, boolean showDataLinkRings, boolean isHeatSeekerDatalink, int heatSeekerRingColor,
                               String stateText, String text, String detailText, int textColor, boolean drawText, float alpha) {
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glOrtho(0.0D, projection.screenW, projection.screenH, 0.0D, -1.0D, 1.0D);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glTranslated(projection.x, projection.y, 0.0D);
        if (projection.edgeClamped) {
            drawHudEdgeArrow(projection, markerColor, markerSize, alpha);
        }
        drawMarkerCore(mc, isMSL, markerColor, highlight, hardLock, markerSize, showDataLinkRings, isHeatSeekerDatalink, heatSeekerRingColor, stateText, text, detailText, textColor, drawText, alpha);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
    }

    private void drawMarkerCore(Minecraft mc, boolean isMSL, int markerColor, boolean highlight, boolean hardLock, int markerSize, boolean showDataLinkRings, boolean isHeatSeekerDatalink, int heatSeekerRingColor, String stateText,
                                String text, String detailText, int textColor, boolean drawText, float alpha) {
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(false);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_LIGHTING);
        float mr = ((markerColor >> 16) & 0xFF) / 255.0F;
        float mg = ((markerColor >> 8) & 0xFF) / 255.0F;
        float mb = (markerColor & 0xFF) / 255.0F;
        GL11.glColor4f(mr, mg, mb, alpha);
        mc.getTextureManager().bindTexture(isMSL ? MSL : FRAME);
        Tessellator tess = Tessellator.instance;
        float half = markerSize * 0.5f;
        tess.startDrawingQuads();
        tess.addVertexWithUV(-half, half, 0, 0, 1);
        tess.addVertexWithUV(half, half, 0, 1, 1);
        tess.addVertexWithUV(half, -half, 0, 1, 0);
        tess.addVertexWithUV(-half, -half, 0, 0, 0);
        tess.draw();
        if (isMSL && highlight) {
            mc.getTextureManager().bindTexture(FRAME);
            tess.startDrawingQuads();
            tess.addVertexWithUV(-half, half, 0, 0, 1);
            tess.addVertexWithUV(half, half, 0, 1, 1);
            tess.addVertexWithUV(half, -half, 0, 1, 0);
            tess.addVertexWithUV(-half, -half, 0, 0, 0);
            tess.draw();
        }
        if (hardLock) {
            drawLockBrackets(half * 1.18F, alpha);
        }
        if (showDataLinkRings) {
            if (isHeatSeekerDatalink) {
                long tick = Minecraft.getMinecraft().theWorld != null ? Minecraft.getMinecraft().theWorld.getTotalWorldTime() : 0L;
                float blinkAlpha = ((tick / 5L) % 2L == 0L) ? 0.5F : 1.0F;
                 float ringAlpha = alpha * blinkAlpha;
                 drawSingleRedRing(half * 1.60F, ringAlpha, heatSeekerRingColor);
            } else {
                drawDualRedRings(half * 0.88F, half * 1.03F, alpha);
            }
        }
        int textAlpha = Math.max(4, Math.min(255, (int)(alpha * 255.0F)));
        int alphaTextColor = (textAlpha << 24) | (textColor & 0xFFFFFF);
        if (stateText != null) {
            int lw = mc.fontRenderer.getStringWidth(stateText);
            mc.fontRenderer.drawString(stateText, -lw / 2, (int)(-half - 9.0F), alphaTextColor, false);
        }
        if (drawText) {
            GL11.glTranslatef(0.0F, markerSize * 0.5f + 8.0f, 0.0F);
            int fw = mc.fontRenderer.getStringWidth(text);
            mc.fontRenderer.drawString(text, -fw / 2, 0, alphaTextColor, false);
            if (detailText != null) {
                int detailWidth = mc.fontRenderer.getStringWidth(detailText);
                mc.fontRenderer.drawString(detailText, -detailWidth / 2, 10, alphaTextColor, false);
            }
        }
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDepthMask(true);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glColor4f(1F, 1F, 1F, 1F);
    }

    private double getHudBlendFactor(double dist) {
        if (dist <= HUD_BLEND_START_RANGE) {
            return 0.0D;
        }
        if (dist >= HUD_BLEND_END_RANGE) {
            return 1.0D;
        }
        return (dist - HUD_BLEND_START_RANGE) / (HUD_BLEND_END_RANGE - HUD_BLEND_START_RANGE);
    }

    private HudProjection projectRelativeToHud(float worldX, float worldY, float worldZ, ScaledResolution sc, Minecraft mc, float partialTicks) {
        double[] projected = worldToScreen(new Vector3f(worldX, worldY, worldZ), partialTicks);
        double screenW = sc.getScaledWidth_double();
        double screenH = sc.getScaledHeight_double();
        double cx = screenW * 0.5D;
        double cy = screenH * 0.5D;
        double sx = projected[0];
        double sy = projected[1];
        double dirX = projected[2];
        double dirY = projected[3];
        boolean behind = projected[4] <= 0.0D;
        if (behind && Math.abs(dirX) < 1.0E-4D && Math.abs(dirY) < 1.0E-4D) {
            dirY = cy;
        }
        boolean outside = behind
            || sx < HUD_EDGE_MARGIN || sx > screenW - HUD_EDGE_MARGIN
            || sy < HUD_EDGE_MARGIN || sy > screenH - HUD_EDGE_MARGIN;
        if (!outside) {
            return new HudProjection(sx, sy, screenW, screenH, false, false, 0.0F);
        }
        double maxX = Math.max(1.0D, cx - HUD_EDGE_MARGIN);
        double maxY = Math.max(1.0D, cy - HUD_EDGE_MARGIN);
        double scaleX = Math.abs(dirX) > 1.0E-6D ? maxX / Math.abs(dirX) : Double.POSITIVE_INFINITY;
        double scaleY = Math.abs(dirY) > 1.0E-6D ? maxY / Math.abs(dirY) : Double.POSITIVE_INFINITY;
        double scale = Math.min(scaleX, scaleY);
        if (Double.isInfinite(scale) || Double.isNaN(scale)) {
            scale = 1.0D;
        }
        double edgeX = cx + dirX * scale;
        double edgeY = cy + dirY * scale;
        edgeX = Math.max(HUD_EDGE_MARGIN, Math.min(screenW - HUD_EDGE_MARGIN, edgeX));
        edgeY = Math.max(HUD_EDGE_MARGIN, Math.min(screenH - HUD_EDGE_MARGIN, edgeY));
        float edgeAngleDeg = (float)Math.toDegrees(Math.atan2(edgeY - cy, edgeX - cx));
        return new HudProjection(edgeX, edgeY, screenW, screenH, true, behind, edgeAngleDeg);
    }

    private static class HudProjection {
        final double x;
        final double y;
        final double screenW;
        final double screenH;
        final boolean edgeClamped;
        final boolean behind;
        final float edgeAngleDeg;

        HudProjection(double x, double y, double screenW, double screenH, boolean edgeClamped, boolean behind, float edgeAngleDeg) {
            this.x = x;
            this.y = y;
            this.screenW = screenW;
            this.screenH = screenH;
            this.edgeClamped = edgeClamped;
            this.behind = behind;
            this.edgeAngleDeg = edgeAngleDeg;
        }

        HudProjection withPosition(double newX, double newY) {
            return new HudProjection(newX, newY, this.screenW, this.screenH, this.edgeClamped, this.behind, this.edgeAngleDeg);
        }
    }

    private static class HudRect {
        final double left;
        final double top;
        final double right;
        final double bottom;

        HudRect(double left, double top, double right, double bottom) {
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
        }

        boolean intersects(HudRect other) {
            return this.left < other.right && this.right > other.left
                && this.top < other.bottom && this.bottom > other.top;
        }
    }

    private void sortTargetsByDisplayPriority(List<MCH_EntityInfo> entities, final int trackingId, final int selectedId, final int fireControlId) {
        Collections.sort(entities, new Comparator<MCH_EntityInfo>() {
            @Override
            public int compare(MCH_EntityInfo a, MCH_EntityInfo b) {
                int ap = getDisplayPriority(a.entityId, trackingId, selectedId, fireControlId);
                int bp = getDisplayPriority(b.entityId, trackingId, selectedId, fireControlId);
                if (ap != bp) {
                    return ap - bp;
                }
                return a.entityId - b.entityId;
            }
        });
    }

    private int getDisplayPriority(int entityId, int trackingId, int selectedId, int fireControlId) {
        if (entityId == trackingId || entityId == fireControlId) {
            return 0;
        }
        if (entityId == selectedId) {
            return 1;
        }
        return 2;
    }

    private HudProjection avoidHudOverlap(HudProjection projection, List<HudRect> occupied, Minecraft mc, int markerSize, String stateText, String text, String detailText, boolean drawText) {
        if (projection.edgeClamped) {
            HudRect edgeRect = buildHudRect(projection, mc, markerSize, stateText, text, detailText, drawText);
            double shiftX = 0.0D;
            double shiftY = 0.0D;
            if (edgeRect.left < 3.0D) shiftX = 3.0D - edgeRect.left;
            if (edgeRect.right > projection.screenW - 3.0D) shiftX = projection.screenW - 3.0D - edgeRect.right;
            if (edgeRect.top < 3.0D) shiftY = 3.0D - edgeRect.top;
            if (edgeRect.bottom > projection.screenH - 3.0D) shiftY = projection.screenH - 3.0D - edgeRect.bottom;
            HudProjection fitted = projection.withPosition(projection.x + shiftX, projection.y + shiftY);
            occupied.add(buildHudRect(fitted, mc, markerSize, stateText, text, detailText, drawText));
            return fitted;
        }
        for (int attempt = 0; attempt <= 10; attempt++) {
            int offsetSteps = attempt == 0 ? 0 : ((attempt + 1) / 2) * (attempt % 2 == 1 ? 1 : -1);
            double candidateY = projection.y + offsetSteps * HUD_OVERLAP_STEP;
            candidateY = Math.max(HUD_EDGE_MARGIN, Math.min(projection.screenH - HUD_EDGE_MARGIN, candidateY));
            HudProjection candidate = projection.withPosition(projection.x, candidateY);
            HudRect rect = buildHudRect(candidate, mc, markerSize, stateText, text, detailText, drawText);
            boolean clear = true;
            for (HudRect used : occupied) {
                if (rect.intersects(used)) {
                    clear = false;
                    break;
                }
            }
            if (clear || attempt == 10) {
                occupied.add(rect);
                return candidate;
            }
        }
        return projection;
    }

    private HudRect buildHudRect(HudProjection projection, Minecraft mc, int markerSize, String stateText, String text, String detailText, boolean drawText) {
        int textWidth = drawText && text != null ? mc.fontRenderer.getStringWidth(text) : 0;
        int detailWidth = drawText && detailText != null ? mc.fontRenderer.getStringWidth(detailText) : 0;
        int stateWidth = stateText != null ? mc.fontRenderer.getStringWidth(stateText) : 0;
        double halfWidth = Math.max(markerSize * 0.5D + 4.0D, Math.max(textWidth, Math.max(detailWidth, stateWidth)) * 0.5D + 3.0D);
        double top = projection.y - markerSize * 0.5D - (stateText != null ? 12.0D : 3.0D);
        double bottom = projection.y + markerSize * 0.5D + (drawText ? (detailText != null ? 24.0D : 14.0D) : 3.0D);
        return new HudRect(projection.x - halfWidth, top, projection.x + halfWidth, bottom);
    }

    private String formatRange(double distance) {
        if (distance >= 1000.0D) {
            return String.format("%.1f km", distance / 1000.0D);
        }
        return String.format("%.0f m", distance);
    }

    private double calculateClosureRate(MCH_EntityInfo target, MCH_EntityAircraft ownship) {
        double dx = target.posX - ownship.posX;
        double dy = target.posY - ownship.posY;
        double dz = target.posZ - ownship.posZ;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (distance < 1.0E-4D) {
            return 0.0D;
        }
        double targetVx = target.posX - target.lastTickPosX;
        double targetVy = target.posY - target.lastTickPosY;
        double targetVz = target.posZ - target.lastTickPosZ;
        double ownVx = ownship.posX - ownship.lastTickPosX;
        double ownVy = ownship.posY - ownship.lastTickPosY;
        double ownVz = ownship.posZ - ownship.lastTickPosZ;
        double relativeRadialVelocity = ((targetVx - ownVx) * dx + (targetVy - ownVy) * dy + (targetVz - ownVz) * dz) / distance;
        double closure = -relativeRadialVelocity * 20.0D;
        return Math.max(-5000.0D, Math.min(5000.0D, closure));
    }

    private void drawHudEdgeArrow(HudProjection projection, int color, int markerSize, float alpha) {
        GL11.glPushMatrix();
        GL11.glRotatef(projection.edgeAngleDeg, 0.0F, 0.0F, 1.0F);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(false);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        float r = ((color >> 16) & 0xFF) / 255.0F;
        float g = ((color >> 8) & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;
        GL11.glColor4f(r, g, b, alpha);
        float base = markerSize * 0.5F + 4.0F;
        drawArrowTriangle(base);
        if (projection.behind) {
            drawArrowTriangle(base - 6.0F);
        }
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDepthMask(true);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glPopMatrix();
    }

    private void drawArrowTriangle(float base) {
        Tessellator tess = Tessellator.instance;
        tess.startDrawing(GL11.GL_TRIANGLES);
        tess.addVertex(base + 7.0F, 0.0F, 0.0D);
        tess.addVertex(base, -4.0F, 0.0D);
        tess.addVertex(base, 4.0F, 0.0D);
        tess.draw();
    }

    private void drawLockBrackets(float extent, float alpha) {
        Tessellator tess = Tessellator.instance;
        float corner = Math.max(5.0F, extent * 0.38F);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glLineWidth(2.5F);
        GL11.glColor4f(1.0F, 0.1F, 0.1F, alpha);
        tess.startDrawing(GL11.GL_LINES);
        addCornerLines(tess, -extent, -extent, corner, 1.0F, 1.0F);
        addCornerLines(tess, extent, -extent, corner, -1.0F, 1.0F);
        addCornerLines(tess, extent, extent, corner, -1.0F, -1.0F);
        addCornerLines(tess, -extent, extent, corner, 1.0F, -1.0F);
        tess.draw();
        GL11.glLineWidth(1.0F);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
    }

    private void addCornerLines(Tessellator tess, float x, float y, float length, float xDirection, float yDirection) {
        tess.addVertex(x, y, 0.0D);
        tess.addVertex(x + length * xDirection, y, 0.0D);
        tess.addVertex(x, y, 0.0D);
        tess.addVertex(x, y + length * yDirection, 0.0D);
    }

    private void drawDualRedRings(float r1, float r2, float alpha) {
        Tessellator tess = Tessellator.instance;
        GL11.glLineWidth(1.0F);
        GL11.glColor4f(1.0F, 0.0F, 0.0F, alpha);
        drawRingLine(tess, r1);
        drawRingLine(tess, r2);
    }

    private void drawSingleRedRing(float radius, float alpha, int color) {
        Tessellator tess = Tessellator.instance;
        GL11.glLineWidth(1.5F);
        float r = ((color >> 16) & 0xFF) / 255.0F;
        float g = ((color >> 8) & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;
        GL11.glColor4f(r, g, b, alpha);
        drawRingLine(tess, radius);
    }

    private void drawRingLine(Tessellator tess, float radius) {
        int seg = 24;
        tess.startDrawing(GL11.GL_LINE_LOOP);
        for (int i = 0; i < seg; i++) {
            double ang = i * (Math.PI * 2.0D / seg);
            tess.addVertex(Math.cos(ang) * radius, Math.sin(ang) * radius, 0.0D);
        }
        tess.draw();
    }

    public List<MCH_EntityInfo> getServerLoadedEntity() {
        return new ArrayList<>(MCH_EntityInfoClientTracker.getAllTrackedEntities());
    }

    private boolean canRenderEntity(MCH_EntityInfo entity, EntityPlayer player, MCH_WeaponInfo wi, MCH_AircraftInfo acInfo) {
        boolean result = false;
        boolean gmtiMode = isGmtiMode(acInfo);
        double distSq = entity.getDistanceSqToEntity(player);
        if (acInfo != null && acInfo.radarMaxTargetRange > 0.0F) {
            double maxRangeSq = acInfo.radarMaxTargetRange * acInfo.radarMaxTargetRange;
            if (distSq > maxRangeSq) {
                return false;
            }
        }
        if (gmtiMode) {
            if (!isVehicle(entity.entityClassName)) {
                return false;
            }
            float maxAgl = acInfo != null ? acInfo.radarMaxScanAltitude : 25.0F;
            double agl = computeAgl(player.worldObj, entity.posX, entity.posY, entity.posZ);
            return agl <= maxAgl;
        }
        // minRangeBVR gates weapon employment, not whether a maintained radar contact is drawn.
        if (entity.entityClassName.contains("MCP_EntityPlane")) {
            return true;
        } else if (entity.entityClassName.contains("MCH_EntityHeli")) {
            return true;
        } else if (entity.entityClassName.contains("MCH_EntityChaff") && wi.isRadarMissile) {
            if (entity.getDistanceSqToEntity(player) > wi.minRangeBVR * wi.minRangeBVR) {
                return true;
            }
        } else if (isMissile(entity.entityClassName)) {
            double missileRange = 4096.0D;
            if (acInfo != null && acInfo.radarMaxTargetRange > 0.0F) {
                missileRange = Math.min(4096.0D, acInfo.radarMaxTargetRange);
            }
            if (distSq > 20 * 20 && distSq < missileRange * missileRange) {
                return true;
            }
        }
        return result;
    }

    private boolean isGmtiMode(MCH_AircraftInfo acInfo) {
        if (acInfo == null || acInfo.radarSearchType == null) {
            return false;
        }
        return "GMTI_SRC".equalsIgnoreCase(acInfo.radarSearchType)
            || "GMTI_TWS".equalsIgnoreCase(acInfo.radarSearchType);
    }

    private double computeAgl(net.minecraft.world.World world, double x, double y, double z) {
        if (world == null) {
            return y;
        }
        int bx = net.minecraft.util.MathHelper.floor_double(x);
        int bz = net.minecraft.util.MathHelper.floor_double(z);
        int groundY = world.getHeightValue(bx, bz);
        return y - (double)groundY;
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
