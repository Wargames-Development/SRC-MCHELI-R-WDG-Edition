package mcheli.render;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import mcheli.MCH_EntityInfo;
import mcheli.MCH_EntityInfoClientTracker;
import mcheli.MCH_RadarDebug;
import mcheli.aircraft.MCH_AircraftInfo;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.aircraft.MCH_EntitySeat;
import mcheli.tank.MCH_EntityTank;
import mcheli.uav.MCH_EntityUavStation;
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
    private static final double HUD_BLEND_START_RANGE = 900.0D;
    private static final double HUD_BLEND_END_RANGE = 1100.0D;
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
        MCH_AircraftInfo acInfo = ac.getAcInfo();
        if (wi == null || acInfo == null || !acInfo.enableBVR) return;
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
        int renderedTargetCount = 0;
        int highlightedTargetCount = 0;
        int selectedHitCount = 0;
        int trackingHitCount = 0;
        int skippedByCanRender = 0;
        int skippedByJamming = 0;
        int skippedByRadarVisible = 0;
        int skippedByMissileRange = 0;
        int skippedByNameMask = 0;
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
            if (!isRadarSelectedOrTracking && !canRenderEntity(entity, player, wi, acInfo)) {
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
            if (!MCH_RenderRWR.isRadarContactVisible(ac, player, entity, partialTicks)) {
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
            boolean lock = false;
            float alpha = 0.4f;
            if (angle <= 90) {
                alpha = 1.0f;
                if (!isMSL) currentLockedEntities.put(entity.entityId, entity);
                if (distSq <= wi.maxLockOnRange * wi.maxLockOnRange && angle <= wi.maxLockOnAngle) {
                    lock = true;
                }
            } else if (angle <= 100.0) alpha = 1.0f;
            else if (angle <= 110.0) alpha = 0.8f;
            else if (angle <= 120.0) alpha = 0.6f;
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
            if (targetName.isEmpty() && !(isRadarSelectedOrTracking || isFireControlLocked)) {
                skippedByNameMask++;
                if (bvrDebugTick && bvrDebugVerbose) {
                    MCH_RadarDebug.traceBvr(ac.worldObj, ac,
                        "bvr-filter id=%d reason=targetNameEmpty class=%s dist=%.1f",
                        entity.entityId, entity.entityClassName, dist);
                }
                continue;
            }
            if (targetName.isEmpty()) {
                targetName = "UNKNOWN";
            }
            if (isMSL) {
                text = String.format("[%s %.1fm]", targetName, dist);
                color = 0xFF0000;
            } else {
                text = String.format("[%s %.1fm]", targetName, dist);
                color = (isRadarSelectedOrTracking || isFireControlLocked) ? 0xFF0000 : 0x00FF00;
            }
            boolean drawText = isMSL || isRadarSelectedOrTracking || (alpha >= 0.6f);
            MCH_WeaponSet currentWs = ac.getCurrentWeapon(player);
            MCH_WeaponInfo currentWi = currentWs != null ? currentWs.getInfo() : null;
            boolean dataLinkMode = currentWi != null && currentWi.enableDataLink && (currentWi.onlyDataLink || currentWs.isDataLinkMode()) && !currentWi.antiRadiationMissile;
            boolean inMissileFov = currentWi != null && angle <= currentWi.getHudPreferredMissileFovDeg();
            boolean highlight = isMSL || isRadarSelectedOrTracking || isFireControlLocked;
            boolean showDataLinkRings = dataLinkMode && inMissileFov && (isRadarSelectedOrTracking || isFireControlLocked);
            String stateText = null;
            if (isRadarTracking) {
                stateText = "LOCK";
            } else if (isRadarSelected) {
                stateText = "SELECT";
            } else if (isFireControlLocked) {
                stateText = "LOCK";
            }
            double hudBlend = getHudBlendFactor(dist);
            float worldAlpha = (float)(alpha * (1.0D - hudBlend));
            float hudAlpha = (float)(alpha * hudBlend);
            if (worldAlpha > 0.01F) {
                drawBillboardMarker(mc, rm, rollDeg, x, y + 0.2D, z, sPerPixel, isMSL, highlight, showDataLinkRings, stateText, text, color, drawText, worldAlpha);
            }
            if (hudAlpha > 0.01F) {
                HudProjection projection = projectRelativeToHud((float)gx, (float)(gy + 0.2D), (float)gz, sc, mc, partialTicks);
                if (projection != null) {
                    drawHudMarker(mc, projection, isMSL, highlight, showDataLinkRings, stateText, text, color, drawText, hudAlpha);
                }
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
                "bvr-scan acId=%d total=%d rendered=%d skipCan=%d skipJam=%d skipVisible=%d skipMslRange=%d skipName=%d skipNear=%d selectedId=%d trackingId=%d fireLockId=%d mslMax=%.1f",
                ac.getEntityId(), entities.size(), renderedTargetCount,
                skippedByCanRender, skippedByJamming, skippedByRadarVisible, skippedByMissileRange, skippedByNameMask, skippedByTooNear,
                radarSelectedId, radarTrackingId, fireControlLockedId, missileDisplayMaxRange);
        }
    }

    private void drawBillboardMarker(Minecraft mc, RenderManager rm, float rollDeg, double x, double y, double z, float sPerPixel,
                                     boolean isMSL, boolean highlight, boolean showDataLinkRings, String stateText,
                                     String text, int textColor, boolean drawText, float alpha) {
        GL11.glPushMatrix();
        GL11.glTranslated(x, y, z);
        GL11.glRotatef(-rm.playerViewY, 0.0F, 1.0F, 0.0F);
        GL11.glRotatef(rm.playerViewX, 1.0F, 0.0F, 0.0F);
        GL11.glRotatef(-rollDeg, 0.0F, 0.0F, 1.0F);
        GL11.glScalef(-sPerPixel, -sPerPixel, sPerPixel);
        drawMarkerCore(mc, isMSL, highlight, showDataLinkRings, stateText, text, textColor, drawText, alpha);
        GL11.glPopMatrix();
    }

    private void drawHudMarker(Minecraft mc, HudProjection projection, boolean isMSL, boolean highlight, boolean showDataLinkRings,
                               String stateText, String text, int textColor, boolean drawText, float alpha) {
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glOrtho(0.0D, projection.screenW, projection.screenH, 0.0D, -1.0D, 1.0D);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glTranslated(projection.x, projection.y, 0.0D);
        drawMarkerCore(mc, isMSL, highlight, showDataLinkRings, stateText, text, textColor, drawText, alpha);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
    }

    private void drawMarkerCore(Minecraft mc, boolean isMSL, boolean highlight, boolean showDataLinkRings, String stateText,
                                String text, int textColor, boolean drawText, float alpha) {
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(false);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_LIGHTING);
        if (highlight) {
            GL11.glColor4f(1.0F, 0F, 0F, alpha);
        } else {
            GL11.glColor4f(0F, 1.0F, 0F, alpha);
        }
        mc.getTextureManager().bindTexture(isMSL ? MSL : FRAME);
        Tessellator tess = Tessellator.instance;
        float half = BOX_SIZE * 0.5f;
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
        if (showDataLinkRings) {
            drawDualRedRings(half * 0.88F, half * 1.03F, alpha);
        }
        if (stateText != null) {
            int lw = mc.fontRenderer.getStringWidth(stateText);
            mc.fontRenderer.drawString(stateText, -lw / 2, (int)(-half - 9.0F), 0xFF4040, false);
        }
        if (drawText) {
            GL11.glTranslatef(0.0F, BOX_SIZE * 0.5f + 8.0f, 0.0F);
            int fw = mc.fontRenderer.getStringWidth(text);
            mc.fontRenderer.drawString(text, -fw / 2, 0, textColor, false);
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
        double sx = projected[0];
        double sy = projected[1];
        if (sx < 0.0D || sy < 0.0D) {
            return null;
        }
        if (sx < -BOX_SIZE || sx > sc.getScaledWidth_double() + BOX_SIZE || sy < -BOX_SIZE || sy > sc.getScaledHeight_double() + BOX_SIZE) {
            return null;
        }
        return new HudProjection(sx, sy, sc.getScaledWidth_double(), sc.getScaledHeight_double());
    }

    private static class HudProjection {
        final double x;
        final double y;
        final double screenW;
        final double screenH;

        HudProjection(double x, double y, double screenW, double screenH) {
            this.x = x;
            this.y = y;
            this.screenW = screenW;
            this.screenH = screenH;
        }
    }

    private void drawDualRedRings(float r1, float r2, float alpha) {
        Tessellator tess = Tessellator.instance;
        GL11.glLineWidth(1.0F);
        GL11.glColor4f(1.0F, 0.0F, 0.0F, alpha);
        drawRingLine(tess, r1);
        drawRingLine(tess, r2);
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
        String searchType = acInfo != null ? acInfo.radarSearchType : "SRC";
        boolean gmtiMode = "GMTI_SRC".equalsIgnoreCase(searchType) || "GMTI_TWS".equalsIgnoreCase(searchType);
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
