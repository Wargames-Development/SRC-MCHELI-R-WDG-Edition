package mcheli.render;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import mcheli.MCH_EntityInfo;
import mcheli.MCH_EntityInfoClientTracker;
import mcheli.MCH_MOD;
import mcheli.MCH_RadarDebug;
import mcheli.MCH_RWRThreatClientTracker;
import mcheli.MCH_RWRThreatEvent;
import mcheli.MCH_RWRThreatTable;
import mcheli.aircraft.MCH_AircraftInfo;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.aircraft.MCH_EntitySeat;
import mcheli.helicopter.MCH_HeliInfoManager;
import mcheli.helicopter.MCH_EntityHeli;
import mcheli.network.packets.PacketRadarLockState;
import mcheli.plane.MCP_PlaneInfoManager;
import mcheli.plane.MCP_EntityPlane;
import mcheli.tank.MCH_TankInfoManager;
import mcheli.tank.MCH_EntityTank;
import mcheli.uav.MCH_EntityUavStation;
import mcheli.vehicle.MCH_VehicleInfoManager;
import mcheli.vehicle.MCH_EntityVehicle;
import mcheli.weapon.MCH_EntityBaseBullet;
import mcheli.weapon.MCH_WeaponInfo;
import mcheli.weapon.MCH_WeaponInfoManager;
import mcheli.weapon.MCH_WeaponSet;
import mcheli.wrapper.W_McClient;
import mcheli.wrapper.W_MOD;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Collections;
import java.util.Comparator;

public class MCH_RenderRWR {

    private static final ResourceLocation RWR = new ResourceLocation(W_MOD.DOMAIN, "textures/rwr.png");
    private static final ResourceLocation RWR_HELI = new ResourceLocation(W_MOD.DOMAIN, "textures/rwr_heli.png");
    private static final ResourceLocation RWR_TANK = new ResourceLocation(W_MOD.DOMAIN, "textures/rwr_tank.png");
    private static final ResourceLocation RWR_FAC = new ResourceLocation(W_MOD.DOMAIN, "textures/rwr_fac.png");
    private static final ResourceLocation RWR_jammed = new ResourceLocation(W_MOD.DOMAIN, "textures/rwr_jammed.png");
    private static final int RADAR_UI_COLOR_AMBER = 0xFFE400;
    private static final int RADAR_UI_COLOR_GREEN = 0x00FF00;
    private static final double PLANE_UI_BASE_WIDTH = 960.0D;
    private static final double PLANE_UI_BASE_HEIGHT = 500.0D;
    private static final double PLANE_COMPACT_AZIMUTH_DOWN_OFFSET = 24.0D;
    private static final int _RWR_SIZE = 180;
    private static final int _RWR_CENTER_X = 100;
    private static final int _RWR_CENTER_Y = 280;
    private static final double SCREEN_HEIGHT_ADAPT_CONSTANT = 520;
    private static final int RWR_SCAN_EVENT_TTL = 40;
    private static final int RWR_LOCK_SOUND_INTERVAL = 10;
    private static final int RWR_LOCK_HEARTBEAT_INTERVAL = 10;
    private static final int RWR_EVENT_MAX_ROWS = 4;
    private static final String RWR_SCAN_SOUND = "rwr_scan";
    private static final String RWR_LOCK_SOUND = "rwr_lock";
    private static final String RWR_SCAN_SOUND_FALLBACK = "alert";
    private static final String RWR_LOCK_SOUND_FALLBACK = "locked";
    private static final int RWR_LOCK_BLINK_TICK = 5;
    private static final int RWR_MSL_BLINK_TICK = 3;

    private static final double _MIN_DISTANCE = 50.0;  // 最小显示距离（米）
    private static final double _MAX_DISTANCE = 4096.0; // 雷达/火控最大处理距离（米）
    private static final double _RWR_RING_MAX_DISTANCE = 4096.0; // 仅RWR威胁环的显示最大距离（米）
    private static final double AIR_SPEED_GATE_BLOCK_PER_TICK = 0.5D / 3.0D;
    private static final double AIR_SPEED_GATE_SQ = AIR_SPEED_GATE_BLOCK_PER_TICK * AIR_SPEED_GATE_BLOCK_PER_TICK;
    private static final int _MIN_RADIUS = 30;          // 最小显示半径（像素
    private static final Map<Integer, Map<Integer, RadarContact>> radarContactCache = new HashMap<Integer, Map<Integer, RadarContact>>();
    private static final Map<Integer, Long> radarLastDecayTick = new HashMap<Integer, Long>();
    private static final Map<Integer, Long> radarLastScanSlot = new HashMap<Integer, Long>();
    private static final Map<Integer, Long> radarLastAcmScanSlot = new HashMap<Integer, Long>();
    private static final Map<Integer, RadarTrackState> radarTrackStateMap = new HashMap<Integer, RadarTrackState>();
    private static final Map<Integer, Long> radarLockHeartbeatLastSendTick = new HashMap<Integer, Long>();
    private static final Map<Integer, Long> dataLinkWatchLastLogTick = new HashMap<Integer, Long>();
    private static final Map<Integer, Long> rwrLastScanSoundTickMap = new HashMap<Integer, Long>();
    private static final Map<Integer, Long> rwrLastLockSoundTickMap = new HashMap<Integer, Long>();
    private static final Map<Integer, RwrHudState> rwrHudStateMap = new HashMap<Integer, RwrHudState>();
    private static final Map<Integer, Map<Integer, ArmBvrContact>> armBvrContactCache = new HashMap<Integer, Map<Integer, ArmBvrContact>>();
    private static final Map<Integer, ArmTrackState> armTrackStateMap = new HashMap<Integer, ArmTrackState>();
    private static boolean radarSelectKeyPrevDown = false;
    private static boolean radarAcmKeyPrevDown = false;
    private static boolean armSelectKeyPrevDown = false;
    private static boolean armTrackKeyPrevDown = false;
    private static final int ARM_BVR_BASE_COLOR = 0xFEE400;
    private static final int ARM_BVR_STT_COLOR = 0xFFF27A;
    private static final int ARM_BVR_REFRESH_MIN_TICK = 3;
    private static final int ARM_TRACK_LOCK_GUARD_TICK = 8;
    private static final int ARM_TRACK_LOST_GRACE_TICK = 10;
    private static final int ARM_TRACK_UNLOCK_INPUT_GUARD_TICK = 10;

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return;
        //获取基本信息
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.thePlayer;
        World world = mc.theWorld;
        ScaledResolution sc = new ScaledResolution(Minecraft.getMinecraft(), Minecraft.getMinecraft().displayWidth, Minecraft.getMinecraft().displayHeight);
        if (player == null || world == null) return;

        //获取玩家机载武器
        MCH_EntityAircraft ac = null;
        if (player.ridingEntity instanceof MCH_EntityAircraft) {
            ac = (MCH_EntityAircraft) player.ridingEntity;
        } else if (player.ridingEntity instanceof MCH_EntitySeat) {
            ac = ((MCH_EntitySeat) player.ridingEntity).getParent();
        } else if (player.ridingEntity instanceof MCH_EntityUavStation) {
            ac = ((MCH_EntityUavStation) player.ridingEntity).getControlAircract();
        }

        if (ac == null) {
            return;
        }
        boolean hasLegacyRwr = ac.getAcInfo().hasRWR;
        boolean enableRadarPanel = ac.getAcInfo().enableRadar;
        if (ac instanceof MCH_EntityTank && enableRadarPanel && hasLegacyRwr) {
            hasLegacyRwr = false;
        }
        if (enableRadarPanel && ac.isRadarEnabledRuntime()) {
            renderRadarScanPanel(mc, sc, player, ac, event.partialTicks);
            if (ac instanceof MCH_EntityTank) {
                return;
            }
        } else if (enableRadarPanel) {
            handleRadarPowerStateChanged(ac, false);
        }
        if (!hasLegacyRwr) {
            return;
        }

        int RWR_SIZE = _RWR_SIZE;
        int RWR_CENTER_X = _RWR_CENTER_X;
        int RWR_CENTER_Y = _RWR_CENTER_Y;
        double MIN_DISTANCE = _MIN_DISTANCE;
        double MAX_DISTANCE = _MAX_DISTANCE;
        int MIN_RADIUS = _MIN_RADIUS;

        //开始渲染
        GL11.glPushMatrix();
        {

            ResourceLocation rwr;
            if (ac instanceof MCP_EntityPlane) {
                rwr = RWR;
                if (ac.getAcInfo().isFloat) {
                    rwr = RWR_FAC;
                    RWR_SIZE = 160;
                    RWR_CENTER_X = 220;
                    RWR_CENTER_Y = 370;
                    MIN_DISTANCE = 15;
                    MAX_DISTANCE = 800;
                    MIN_RADIUS = 30;
                }
            } else if (ac instanceof MCH_EntityHeli) {
                rwr = RWR_HELI;
            } else if (ac instanceof MCH_EntityTank) {
                rwr = RWR_TANK;
                RWR_SIZE = 160;
                RWR_CENTER_X = 220;
                RWR_CENTER_Y = 370;
                MIN_DISTANCE = 15;
                MAX_DISTANCE = 800;
                MIN_RADIUS = 30;
            } else {
                rwr = RWR;
            }

            double sx = sc.getScaledHeight() * (RWR_CENTER_X / SCREEN_HEIGHT_ADAPT_CONSTANT);
            double sy = sc.getScaledHeight() * (RWR_CENTER_Y / SCREEN_HEIGHT_ADAPT_CONSTANT);
            drawRWRCircle(sx, sy, sc, rwr, RWR_SIZE);
            if(ac.jammingTick > 0) {
                drawRWRCircle(sx, sy, sc, RWR_jammed, RWR_SIZE);
            }
            long nowTick = ac.worldObj != null ? ac.worldObj.getTotalWorldTime() : 0L;
            RwrHudState hudState = updateRwrHudState(ac, nowTick);
            renderRwrThreatRing(mc, sc, ac, player, sx, sy, RWR_SIZE, event.partialTicks, hudState, nowTick);
        }
        GL11.glPopMatrix();
    }

    private void renderRwrThreatRing(Minecraft mc, ScaledResolution sc, MCH_EntityAircraft ac, EntityPlayer player,
                                     double centerX, double centerY, int rwrSize, float partialTicks, RwrHudState state, long nowTick) {
        if (mc == null || sc == null || ac == null || player == null || ac.worldObj == null) {
            return;
        }
        double halfSize = sc.getScaledHeight() * (rwrSize / SCREEN_HEIGHT_ADAPT_CONSTANT) * 0.5D;
        double outerRadius = halfSize * 0.88D;
        double innerRadius = Math.max(10.0D, outerRadius * 0.22D);
        double minDistance = _MIN_DISTANCE;
        double maxDistance = _RWR_RING_MAX_DISTANCE;
        if (ac.getAcInfo() != null && ac.getAcInfo().radarMaxTargetRange > 0.0F) {
            maxDistance = Math.min(maxDistance, ac.getAcInfo().radarMaxTargetRange);
        }
        if (maxDistance <= minDistance) {
            minDistance = Math.max(0.0D, maxDistance - 50.0D);
        }
        List<RwrThreatPlot> plots = new ArrayList<RwrThreatPlot>();
        List<MCH_RWRThreatEvent> events = MCH_RWRThreatClientTracker.getEvents(ac.getEntityId());
        for (MCH_RWRThreatEvent evt : events) {
            if (evt == null || evt.emitterId == ac.getEntityId()) {
                continue;
            }
            String name = normalizeRwrSourceName(evt.sourceName);
            if (name.isEmpty() || "?".equals(name)) {
                continue;
            }
            double distance = evt.distanceMeters > 0.0F ? evt.distanceMeters
                : (maxDistance - MCH_RWRThreatEvent.clamp01(evt.strength) * (maxDistance - minDistance));
            if (distance <= 1.0E-4D) {
                continue;
            }
            distance = Math.max(minDistance, Math.min(maxDistance, distance));
            double rangeNorm = (distance - minDistance) / Math.max(1.0D, maxDistance - minDistance);
            rangeNorm = Math.max(0.0D, Math.min(1.0D, rangeNorm));
            double ringRadius = innerRadius + (outerRadius - innerRadius) * rangeNorm;
            double ang = Math.toRadians(evt.bearingDeg - 90.0D);
            double px = centerX + Math.cos(ang) * ringRadius;
            double py = centerY + Math.sin(ang) * ringRadius;
            boolean isMsl = evt.threatMode == MCH_RWRThreatEvent.MODE_MSL_ACTIVE || evt.threatMode == MCH_RWRThreatEvent.MODE_MSL_DATALINK;
            int color = resolveThreatColor(evt, nowTick);
            if (isMsl && state != null && state.missileUntilTick >= nowTick) {
                boolean strong = ((nowTick / RWR_MSL_BLINK_TICK) & 1L) == 0L;
                color = strong ? 0xFF2D2D : 0xCC3030;
            }
            plots.add(new RwrThreatPlot(px, py, color, name, isMsl, distance));
        }
        if (plots.isEmpty()) {
            return;
        }
        Collections.sort(plots, new Comparator<RwrThreatPlot>() {
            @Override
            public int compare(RwrThreatPlot a, RwrThreatPlot b) {
                return Double.compare(b.distance, a.distance);
            }
        });
        int maxPlots = Math.min(12, plots.size());
        for (int i = 0; i < maxPlots; i++) {
            RwrThreatPlot p = plots.get(i);
            drawRadarText(mc, sc, p.label, p.x, p.y, p.color, true, 0.90F);
        }
    }

    private static int resolveThreatColor(MCH_RWRThreatEvent evt, long nowTick) {
        byte threatMode = evt.threatMode;
        if (threatMode == MCH_RWRThreatEvent.MODE_MSL_ACTIVE || threatMode == MCH_RWRThreatEvent.MODE_MSL_DATALINK) {
            boolean strong = ((nowTick / RWR_MSL_BLINK_TICK) & 1L) == 0L;
            return strong ? 0xFF2D2D : 0xCC3030;
        }
        if (threatMode == MCH_RWRThreatEvent.MODE_STT) {
            return 0xFF4A4A;
        }
        if (threatMode == MCH_RWRThreatEvent.MODE_TRACK) {
            return 0xFF8A3A;
        }
        if (evt.emitterKind == MCH_RWRThreatEvent.EMITTER_AIRCRAFT) {
            return RADAR_UI_COLOR_GREEN;
        }
        return RADAR_UI_COLOR_AMBER;
    }

    private boolean isRwrEmitterThreat(MCH_EntityAircraft emitter, MCH_EntityAircraft target) {
        if (emitter == null || target == null || emitter == target || emitter.getAcInfo() == null || target.getAcInfo() == null) {
            return false;
        }
        if (!emitter.getAcInfo().enableRadar || !emitter.isRadarEnabledRuntime() || emitter.isDestroyed() || !isAircraftMannedForEmissionAnySeat(emitter)) {
            return false;
        }
        double maxRange = emitter.getAcInfo().radarMaxTargetRange > 0.0F ? emitter.getAcInfo().radarMaxTargetRange : _MAX_DISTANCE;
        float scanAz = Math.max(0.0F, Math.min(360.0F, emitter.getAcInfo().radarScanAzimuthDeg));
        float scanEl = Math.max(0.0F, Math.min(180.0F, emitter.getAcInfo().radarScanElevationDeg));
        if (scanAz <= 0.0F || scanEl <= 0.0F) {
            return false;
        }
        double dx = target.posX - emitter.posX;
        double dy = target.posY - emitter.posY;
        double dz = target.posZ - emitter.posZ;
        double distSq = dx * dx + dy * dy + dz * dz;
        if (distSq < _MIN_DISTANCE * _MIN_DISTANCE || distSq > maxRange * maxRange) {
            return false;
        }
        double yaw = Math.toRadians(interpolate(emitter.rotationYaw, emitter.prevRotationYaw, 1.0F));
        double pitch = Math.toRadians(interpolate(emitter.rotationPitch, emitter.prevRotationPitch, 1.0F));
        double fx = -Math.sin(yaw) * Math.cos(pitch);
        double fy = -Math.sin(pitch);
        double fz = Math.cos(yaw) * Math.cos(pitch);
        double forwardPitch = Math.toDegrees(Math.atan2(fy, Math.sqrt(fx * fx + fz * fz)));
        double horiz = Math.sqrt(dx * dx + dz * dz);
        if (horiz <= 1.0E-6D) {
            horiz = 1.0E-6D;
        }
        double targetPitch = Math.toDegrees(Math.atan2(dy, horiz));
        double relElev = targetPitch - forwardPitch;
        double fhl = Math.sqrt(fx * fx + fz * fz);
        if (fhl <= 1.0E-6D) {
            return false;
        }
        double fhx = fx / fhl;
        double fhz = fz / fhl;
        double thx = dx / horiz;
        double thz = dz / horiz;
        double dot = fhx * thx + fhz * thz;
        dot = Math.max(-1.0D, Math.min(1.0D, dot));
        double relAz = Math.toDegrees(Math.acos(dot));
        double crossY = fhx * thz - fhz * thx;
        if (crossY < 0.0D) {
            relAz = -relAz;
        }
        return Math.abs(relAz) <= scanAz * 0.5D && Math.abs(relElev) <= scanEl * 0.5D;
    }

    private boolean isAircraftMannedForEmissionAnySeat(MCH_EntityAircraft ac) {
        if (ac == null) {
            return false;
        }
        if (ac.getRiddenByEntity() != null) {
            return true;
        }
        for (int sid = 1; sid <= ac.getSeatNum(); sid++) {
            if (ac.getEntityBySeatId(sid) != null) {
                return true;
            }
        }
        return false;
    }

    private static class RwrThreatPlot {
        final double x;
        final double y;
        final int color;
        final String label;
        final boolean isMissile;
        final double distance;

        RwrThreatPlot(double x, double y, int color, String label, boolean isMissile, double distance) {
            this.x = x;
            this.y = y;
            this.color = color;
            this.label = label;
            this.isMissile = isMissile;
            this.distance = distance;
        }
    }

    private static RwrHudState updateRwrHudState(MCH_EntityAircraft ac, long nowTick) {
        int aircraftId = ac.getEntityId();
        boolean shouldWatchLog = MCH_RadarDebug.isRwrWatchEnabled()
            && nowTick % Math.max(1, MCH_RadarDebug.getRwrWatchIntervalTick()) == 0L;
        RwrHudState state = rwrHudStateMap.get(aircraftId);
        if (state == null) {
            state = new RwrHudState();
            rwrHudStateMap.put(aircraftId, state);
        }
        MCH_RWRThreatTable table = MCH_RWRThreatClientTracker.getTable(aircraftId);
        if (table == null || table.events == null) {
            state.scanEvents.clear();
            state.lockSourceName = "";
            state.lockUntilTick = -1L;
            state.missileSourceName = "";
            state.missileUntilTick = -1L;
            if (shouldWatchLog) {
                MCH_RadarDebug.appendManual(
                    "RWRWATCH acId=%d tick=%d table=empty scanPulse=false scanSound=false lockSound=false lock=false msl=false",
                    aircraftId, nowTick
                );
            }
            return state;
        }
        state.scanEvents.clear();
        state.lockSourceName = "";
        state.lockUntilTick = -1L;
        state.missileSourceName = "";
        state.missileUntilTick = -1L;
        Map<Integer, Integer> newSearchTtlByEmitter = new HashMap<Integer, Integer>();
        boolean scanPulseTriggered = false;

        for (MCH_RWRThreatEvent evt : table.events) {
            if (evt == null || evt.ttlTick <= 0) {
                continue;
            }
            long until = nowTick + evt.ttlTick;
            String source = normalizeRwrSourceName(evt.sourceName);
            if (evt.threatMode == MCH_RWRThreatEvent.MODE_SEARCH) {
                Long old = state.scanEvents.get(source);
                if (old == null || until > old.longValue()) {
                    state.scanEvents.put(source, until);
                }
                Integer prevTtl = state.searchTtlByEmitter.get(evt.emitterId);
                if (prevTtl == null || evt.ttlTick > prevTtl.intValue() + 1) {
                    scanPulseTriggered = true;
                }
                newSearchTtlByEmitter.put(evt.emitterId, evt.ttlTick);
            } else if (evt.threatMode == MCH_RWRThreatEvent.MODE_TRACK || evt.threatMode == MCH_RWRThreatEvent.MODE_STT) {
                if (until > state.lockUntilTick) {
                    state.lockUntilTick = until;
                    state.lockSourceName = source;
                }
            } else if (evt.threatMode == MCH_RWRThreatEvent.MODE_MSL_ACTIVE || evt.threatMode == MCH_RWRThreatEvent.MODE_MSL_DATALINK) {
                if (until > state.missileUntilTick) {
                    state.missileUntilTick = until;
                    state.missileSourceName = source;
                }
            }
        }

        while (state.scanEvents.size() > RWR_EVENT_MAX_ROWS) {
            String oldest = state.scanEvents.keySet().iterator().next();
            state.scanEvents.remove(oldest);
        }
        state.searchTtlByEmitter.clear();
        state.searchTtlByEmitter.putAll(newSearchTtlByEmitter);
        boolean playedScanSound = false;
        boolean playedLockSound = false;
        if (scanPulseTriggered) {
            long lastScanSound = rwrLastScanSoundTickMap.containsKey(aircraftId) ? rwrLastScanSoundTickMap.get(aircraftId) : -RWR_SCAN_EVENT_TTL;
            if (nowTick - lastScanSound >= 2L) {
                playedScanSound = playRwrSoundWithFallback(ac, RWR_SCAN_SOUND, RWR_SCAN_SOUND_FALLBACK);
                if (playedScanSound) {
                    rwrLastScanSoundTickMap.put(aircraftId, nowTick);
                }
            }
        }
        if (state.lockUntilTick >= nowTick && !state.lockSourceName.isEmpty()) {
            long lastLockSound = rwrLastLockSoundTickMap.containsKey(aircraftId) ? rwrLastLockSoundTickMap.get(aircraftId) : -RWR_LOCK_SOUND_INTERVAL;
            if (nowTick - lastLockSound >= RWR_LOCK_SOUND_INTERVAL) {
                playedLockSound = playRwrSoundWithFallback(ac, RWR_LOCK_SOUND, RWR_LOCK_SOUND_FALLBACK);
                if (playedLockSound) {
                    rwrLastLockSoundTickMap.put(aircraftId, nowTick);
                }
            }
        } else {
            rwrLastLockSoundTickMap.remove(aircraftId);
        }
        if (shouldWatchLog) {
            MCH_RadarDebug.appendManual(
                "RWRWATCH acId=%d tick=%d events=%d scanRows=%d scanPulse=%s scanSound=%s lockSound=%s lock=%s lockSrc=%s lockTtl=%d msl=%s mslSrc=%s mslTtl=%d scanSrc=%s",
                aircraftId,
                nowTick,
                table.events.size(),
                state.scanEvents.size(),
                String.valueOf(scanPulseTriggered),
                String.valueOf(playedScanSound),
                String.valueOf(playedLockSound),
                String.valueOf(state.lockUntilTick >= nowTick && !state.lockSourceName.isEmpty()),
                state.lockSourceName.isEmpty() ? "-" : state.lockSourceName,
                state.lockUntilTick >= nowTick ? (state.lockUntilTick - nowTick) : -1L,
                String.valueOf(state.missileUntilTick >= nowTick && !state.missileSourceName.isEmpty()),
                state.missileSourceName.isEmpty() ? "-" : state.missileSourceName,
                state.missileUntilTick >= nowTick ? (state.missileUntilTick - nowTick) : -1L,
                state.scanEvents.keySet().toString()
            );
        }
        return state;
    }

    private static boolean playRwrSoundWithFallback(MCH_EntityAircraft ac, String primary, String fallback) {
        try {
            W_McClient.MOD_playSoundFX(primary, 1.0F, 1.0F);
            return true;
        } catch (Throwable primaryEx) {
            MCH_RadarDebug.traceVerbose(ac != null ? ac.worldObj : null, ac, "RWR sound missing primary=%s", primary);
        }
        if (fallback != null && !fallback.isEmpty()) {
            try {
                W_McClient.MOD_playSoundFX(fallback, 1.0F, 1.0F);
                MCH_RadarDebug.traceVerbose(ac != null ? ac.worldObj : null, ac, "RWR sound fallback=%s", fallback);
                return true;
            } catch (Throwable fallbackEx) {
                MCH_RadarDebug.traceVerbose(ac != null ? ac.worldObj : null, ac, "RWR sound missing fallback=%s", fallback);
            }
        }
        return false;
    }

    private static String normalizeRwrSourceName(String sourceName) {
        if (sourceName == null) {
            return "?";
        }
        String n = sourceName.trim();
        return n.isEmpty() ? "?" : n;
    }

    private void drawRwrHudAlerts(Minecraft mc, ScaledResolution sc, MCH_EntityAircraft ac, double centerX, double centerY, int rwrSize, RwrHudState state, long nowTick) {
        if (mc == null || sc == null || state == null) {
            return;
        }
        if (!hasVisibleRwrAlerts(state, nowTick)) {
            return;
        }
        RwrHudLayout layout = resolveRwrHudLayout(ac, rwrSize, sc);
        double clipX = centerX + layout.offsetX;
        double clipY = centerY + layout.offsetY;
        double clipW = layout.width;
        double clipH = layout.height;
        drawRwrAlertMask(clipX, clipY, clipW, clipH, layout.frameColor, nowTick);
        ScissorState ss = beginScissor(mc, sc, clipX, clipY, clipW, clipH);
        try {
            double lineY = clipY + 2.0D;
            if (state.missileUntilTick >= nowTick && !state.missileSourceName.isEmpty()) {
                boolean blinkStrong = ((nowTick / RWR_MSL_BLINK_TICK) & 1L) == 0L;
                int mslColor = blinkStrong ? withAlpha(0xFF2D2D, 0xFF) : withAlpha(0xFF2D2D, 0x8C);
                drawRadarText(mc, sc, state.missileSourceName, clipX + 3.0D, lineY, mslColor, false, layout.fontScale);
                lineY += layout.lineStep + 1.0D;
            }
            if (state.lockUntilTick >= nowTick && !state.lockSourceName.isEmpty()) {
                boolean blinkStrong = ((nowTick / RWR_LOCK_BLINK_TICK) & 1L) == 0L;
                int lockColor = blinkStrong ? withAlpha(0xFF4A4A, 0xFF) : withAlpha(0xFF4A4A, 0x90);
                drawRadarText(mc, sc, state.lockSourceName, clipX + 3.0D, lineY, lockColor, false, layout.fontScale);
                lineY += layout.lineStep + 1.0D;
            }
            for (Map.Entry<String, Long> e : state.scanEvents.entrySet()) {
                if (e.getValue() < nowTick || lineY + layout.lineStep > clipY + clipH) {
                    continue;
                }
                long remain = e.getValue() - nowTick;
                float t = Math.max(0.0F, Math.min(1.0F, remain / (float) RWR_SCAN_EVENT_TTL));
                int alpha = (int) (100.0F + 155.0F * t);
                int color = withAlpha(layout.scanColor, alpha);
                drawRadarText(mc, sc, e.getKey(), clipX + 3.0D, lineY, color, false, layout.fontScale);
                lineY += layout.lineStep;
            }
        } finally {
            endScissor(ss);
        }
    }

    private boolean hasVisibleRwrAlerts(RwrHudState state, long nowTick) {
        if (state == null) {
            return false;
        }
        if (state.missileUntilTick >= nowTick && !state.missileSourceName.isEmpty()) {
            return true;
        }
        if (state.lockUntilTick >= nowTick && !state.lockSourceName.isEmpty()) {
            return true;
        }
        for (Map.Entry<String, Long> e : state.scanEvents.entrySet()) {
            if (e.getValue() >= nowTick) {
                return true;
            }
        }
        return false;
    }

    private static RwrHudLayout resolveRwrHudLayout(MCH_EntityAircraft ac, int rwrSize, ScaledResolution sc) {
        double halfSize = sc.getScaledHeight() * (rwrSize / SCREEN_HEIGHT_ADAPT_CONSTANT) * 0.5D;
        RwrHudLayout layout = new RwrHudLayout();
        if (ac instanceof MCP_EntityPlane && ac.getAcInfo().isFloat) {
            layout.offsetX = -halfSize * 0.75D;
            layout.offsetY = halfSize * 0.25D;
            layout.width = halfSize * 1.20D;
            layout.height = halfSize * 0.55D;
            layout.fontScale = 0.86F;
            layout.lineStep = 9.0D;
            layout.scanColor = 0xFFCF45;
            layout.frameColor = 0x66FFCC00;
            return layout;
        }
        if (ac instanceof MCH_EntityTank || ac instanceof MCH_EntityVehicle) {
            layout.offsetX = -halfSize * 0.72D;
            layout.offsetY = halfSize * 0.22D;
            layout.width = halfSize * 1.16D;
            layout.height = halfSize * 0.56D;
            layout.fontScale = 0.85F;
            layout.lineStep = 9.0D;
            layout.scanColor = 0xFFDD55;
            layout.frameColor = 0x66FFE400;
            return layout;
        }
        if (ac instanceof MCH_EntityHeli) {
            layout.offsetX = -halfSize * 0.70D;
            layout.offsetY = halfSize * 0.26D;
            layout.width = halfSize * 1.12D;
            layout.height = halfSize * 0.52D;
            layout.fontScale = 0.87F;
            layout.lineStep = 9.0D;
            layout.scanColor = 0x66FF66;
            layout.frameColor = 0x6600FF66;
            return layout;
        }
        layout.offsetX = -halfSize * 0.70D;
        layout.offsetY = halfSize * 0.24D;
        layout.width = halfSize * 1.12D;
        layout.height = halfSize * 0.52D;
        layout.fontScale = 0.87F;
        layout.lineStep = 9.0D;
        layout.scanColor = 0x66FF66;
        layout.frameColor = 0x6600FF66;
        return layout;
    }

    private void drawRwrAlertMask(double x, double y, double w, double h, int frameArgb, long nowTick) {
        float baseAlpha = ((frameArgb >>> 24) & 0xFF) / 255.0F;
        prepareShapeRenderState(frameArgb & 0x00FFFFFF, baseAlpha);
        Tessellator tess = Tessellator.instance;
        // Subtle background plate to make text integrated with custom RWR artwork.
        tess.startDrawingQuads();
        tess.addVertex(x, y + h, 0.0D);
        tess.addVertex(x + w, y + h, 0.0D);
        tess.addVertex(x + w, y, 0.0D);
        tess.addVertex(x, y, 0.0D);
        tess.draw();
        restoreShapeRenderState();
        // Top sweep line with tick-based opacity alternation.
        int sweepAlpha = ((nowTick / 5L) & 1L) == 0L ? 0xAA : 0x55;
        prepareShapeRenderState(frameArgb & 0x00FFFFFF, sweepAlpha / 255.0F);
        GL11.glLineWidth(1.0F);
        tess.startDrawing(GL11.GL_LINES);
        tess.addVertex(x + 1.0D, y + 1.0D, 0.0D);
        tess.addVertex(x + w - 1.0D, y + 1.0D, 0.0D);
        tess.draw();
        restoreShapeRenderState();
        // Border frame.
        prepareShapeRenderState(frameArgb & 0x00FFFFFF, 0xCC / 255.0F);
        GL11.glLineWidth(1.0F);
        tess.startDrawing(GL11.GL_LINE_LOOP);
        tess.addVertex(x, y + h, 0.0D);
        tess.addVertex(x + w, y + h, 0.0D);
        tess.addVertex(x + w, y, 0.0D);
        tess.addVertex(x, y, 0.0D);
        tess.draw();
        restoreShapeRenderState();
    }

    private static int withAlpha(int rgb, int alpha) {
        return ((alpha & 0xFF) << 24) | (rgb & 0x00FFFFFF);
    }

    private ScissorState beginScissor(Minecraft mc, ScaledResolution sc, double x, double y, double w, double h) {
        ScissorState state = new ScissorState();
        state.wasEnabled = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        int scale = sc.getScaleFactor();
        int sx = (int) Math.floor(x * scale);
        int sy = (int) Math.floor((sc.getScaledHeight_double() - (y + h)) * scale);
        int sw = Math.max(1, (int) Math.ceil(w * scale));
        int sh = Math.max(1, (int) Math.ceil(h * scale));
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(sx, sy, sw, sh);
        return state;
    }

    private void endScissor(ScissorState state) {
        if (state == null || !state.wasEnabled) {
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
        }
    }

    public static Vec3 getDirection(Entity e, float factor) {
        float f1;
        float f2;
        float f3;
        float f4;

        if (factor == 1.0F) {
            f1 = MathHelper.cos(-e.rotationYaw * 0.017453292F - (float) Math.PI);
            f2 = MathHelper.sin(-e.rotationYaw * 0.017453292F - (float) Math.PI);
            f3 = -MathHelper.cos(-e.rotationPitch * 0.017453292F);
            f4 = MathHelper.sin(-e.rotationPitch * 0.017453292F);
            return Vec3.createVectorHelper(f2 * f3, f4, f1 * f3);
        } else {
            f1 = e.prevRotationPitch + (e.rotationPitch - e.prevRotationPitch) * factor;
            f2 = e.prevRotationYaw + (e.rotationYaw - e.prevRotationYaw) * factor;
            f3 = MathHelper.cos(-f2 * 0.017453292F - (float) Math.PI);
            f4 = MathHelper.sin(-f2 * 0.017453292F - (float) Math.PI);
            float f5 = -MathHelper.cos(-f1 * 0.017453292F);
            float f6 = MathHelper.sin(-f1 * 0.017453292F);
            return Vec3.createVectorHelper(f4 * f5, f6, f3 * f5);
        }
    }


    // 新增实体校验方法
    private static boolean isValidEntity(MCH_EntityInfo entity, EntityPlayer player, double minDist, String searchType) {
        if (entity.entityClassName.contains("MCH_EntityChaff") || entity.entityClassName.contains("MCH_EntityFlare")) {
            return false;
        }
        if (isFacGroundTargetEntity(entity) && !isMultiMode(searchType) && !isGmtiMode(searchType)) {
            return false;
        }
        if (entity.getDistanceSqToEntity(player) < minDist * minDist) {
            return false;
        }
        return true;
    }

    public MCH_RWRResult getTargetTypeOnRadar(MCH_EntityInfo entity, MCH_EntityAircraft ac) {
        int color = 0x00FF00;
        if (ac instanceof MCH_EntityTank
            || (ac instanceof MCP_EntityPlane && ac.getAcInfo().isFloat)) {
            color = 0xFFCC00;
        }
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


    private void drawRWRCircle(double x, double y, ScaledResolution sc, ResourceLocation rwr, int size) {
        prepareRenderState();
        Minecraft.getMinecraft().renderEngine.bindTexture(rwr);
        Tessellator tess = Tessellator.instance;
        tess.startDrawingQuads();
        double halfSize = sc.getScaledHeight() * (size / SCREEN_HEIGHT_ADAPT_CONSTANT) / 2.0;
        tess.addVertexWithUV(x - halfSize, y + halfSize, 0, 0, 1);
        tess.addVertexWithUV(x + halfSize, y + halfSize, 0, 1, 1);
        tess.addVertexWithUV(x + halfSize, y - halfSize, 0, 1, 0);
        tess.addVertexWithUV(x - halfSize, y - halfSize, 0, 0, 0);
        tess.draw();
        restoreRenderState();
    }

    private void prepareRenderState() {
        GL11.glEnable(3042);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glBlendFunc(770, 771);
    }

    private void restoreRenderState() {
        int srcBlend = GL11.glGetInteger(3041);
        int dstBlend = GL11.glGetInteger(3040);
        GL11.glBlendFunc(srcBlend, dstBlend);
        GL11.glDisable(3042);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static double interpolate(double now, double old, float partialTicks) {
        return old + (now - old) * partialTicks;
    }

    public static List<MCH_EntityInfo> getServerLoadedEntityStatic() {
        return new ArrayList<>(MCH_EntityInfoClientTracker.getAllTrackedEntities());
    }

    public static int getEnableRadarUiColor(MCH_EntityAircraft ac) {
        if (ac == null || ac.getAcInfo() == null) {
            return RADAR_UI_COLOR_AMBER;
        }
        if (ac instanceof MCP_EntityPlane && !ac.getAcInfo().isFloat) {
            return RADAR_UI_COLOR_GREEN;
        }
        if (ac instanceof MCH_EntityHeli) {
            return RADAR_UI_COLOR_GREEN;
        }
        return RADAR_UI_COLOR_AMBER;
    }

    public List<MCH_EntityInfo> getServerLoadedEntity() {
        return getServerLoadedEntityStatic();
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

    private void renderRadarScanPanel(Minecraft mc, ScaledResolution sc, EntityPlayer player, MCH_EntityAircraft ac, float partialTicks) {
        int panelSize = _RWR_SIZE;
        int panelCenterX = _RWR_CENTER_X;
        int panelCenterY = _RWR_CENTER_Y;
        int radarUiColor = RADAR_UI_COLOR_AMBER;
        boolean planeAdaptiveLayout = false;
        double planeAdaptiveScale = 1.0D;

        if (ac instanceof MCP_EntityPlane) {
            if (ac.getAcInfo().isFloat) {
                panelSize = 160;
                panelCenterX = 220;
                panelCenterY = 370;
            } else {
                // Fixed-wing radar panel anchor (reference resolution 960x500), then adapt to current width/height.
                panelSize = 168;
                panelCenterX = 760;
                panelCenterY = 140;
                planeAdaptiveLayout = true;
                radarUiColor = RADAR_UI_COLOR_GREEN;
            }
        } else if (ac instanceof MCH_EntityHeli) {
            // Helicopter uses the same radar panel style/layout as fixed-wing.
            panelSize = 168;
            panelCenterX = 760;
            panelCenterY = 140;
            planeAdaptiveLayout = true;
            radarUiColor = RADAR_UI_COLOR_GREEN;
        } else if (ac instanceof MCH_EntityVehicle) {
            // FAC/Vehicle follows tank radar style.
            panelSize = 160;
            panelCenterX = 220;
            panelCenterY = 370;
            radarUiColor = RADAR_UI_COLOR_AMBER;
        } else if (ac instanceof MCH_EntityTank) {
            panelSize = 160;
            panelCenterX = 220;
            panelCenterY = 370;
        }

        double sx;
        double sy;
        double radius;
        if (planeAdaptiveLayout) {
            sx = sc.getScaledWidth_double() * (panelCenterX / PLANE_UI_BASE_WIDTH);
            sy = sc.getScaledHeight_double() * (panelCenterY / PLANE_UI_BASE_HEIGHT);
            double sw = sc.getScaledWidth_double() / PLANE_UI_BASE_WIDTH;
            double sh = sc.getScaledHeight_double() / PLANE_UI_BASE_HEIGHT;
            double scale = Math.min(sw, sh);
            planeAdaptiveScale = scale;
            radius = panelSize * scale * 0.5D;
        } else {
            sx = sc.getScaledHeight() * (panelCenterX / SCREEN_HEIGHT_ADAPT_CONSTANT);
            sy = sc.getScaledHeight() * (panelCenterY / SCREEN_HEIGHT_ADAPT_CONSTANT);
            radius = sc.getScaledHeight() * (panelSize / SCREEN_HEIGHT_ADAPT_CONSTANT) / 2.0D;
        }
        double minDistance = _MIN_DISTANCE;
        double maxDistance = _MAX_DISTANCE;
        int minRadius = _MIN_RADIUS;
        if (ac instanceof MCH_EntityTank || (ac instanceof MCP_EntityPlane && ac.getAcInfo().isFloat)) {
            minDistance = 15.0D;
            maxDistance = 4096.0D;
            minRadius = 30;
        }
        if (ac.getAcInfo().radarMaxTargetRange > 0.0F) {
            maxDistance = Math.min(maxDistance, ac.getAcInfo().radarMaxTargetRange);
        }
        if (maxDistance <= minDistance) {
            minDistance = Math.max(0.0D, maxDistance - 50.0D);
        }
        float scanAngleDeg = ac.getAcInfo().radarScanAzimuthDeg;
        float trackAzDeg = ac.getAcInfo().radarTrackAzimuthDeg;
        float panelFillAlpha = ac.getAcInfo().radarPanelFillAlpha;
        float elevationDeg = ac.getAcInfo().radarScanElevationDeg;
        String elevationRef = ac.getAcInfo().radarElevationReference;
        String elevationCoverage = ac.getAcInfo().radarElevationCoverage;
        String searchType = normalizeRadarSearchType(ac.getAcInfo().radarSearchType);
        boolean followTurretYaw = (ac instanceof MCH_EntityTank) && ac.getAcInfo().radarFollowTurretYaw;
        double adjustedCy = computeRadarPanelCenterY(sy, radius, scanAngleDeg, elevationDeg, elevationRef, elevationCoverage);
        if (planeAdaptiveLayout && scanAngleDeg > 0.0F && scanAngleDeg < 270.0F) {
            adjustedCy += PLANE_COMPACT_AZIMUTH_DOWN_OFFSET * planeAdaptiveScale;
        }
        // Keep radar UI orientation fixed on screen; do not rotate with aircraft attitude.
        double axisDeg = -90.0D;
        int scanTick = Math.max(1, ac.getAcInfo().radarScanTick);
        long t = ac.worldObj.getTotalWorldTime();
        int aircraftId = ac.getEntityId();
        RadarTrackState trackState = getOrCreateRadarTrackState(aircraftId);
        tickRadarTrackState(trackState, t);
        boolean acmMode = trackState.acmMode;
        float acmFovDeg = 5.0F;
        double acmMaxDistance;
        if (ac.getAcInfo().radarMaxTargetRange > 0.0F) {
            acmMaxDistance = Math.max(minDistance + 1.0D, ac.getAcInfo().radarMaxTargetRange * 0.5D);
        } else {
            acmMaxDistance = Math.max(minDistance + 1.0D, maxDistance * 0.5D);
        }
        double renderMaxDistance = acmMode ? acmMaxDistance : maxDistance;
        int acmScanTick = Math.max(1, scanTick / 4);
        int usedScanTick = acmMode ? acmScanTick : scanTick;
        double phase = ((double)(t % usedScanTick) + partialTicks) / (double)usedScanTick;
        if (phase > 1.0D) {
            phase = phase - Math.floor(phase);
        }
        float scanAzClamp = Math.max(0.0F, Math.min(360.0F, scanAngleDeg));
        float scanElClamp = Math.max(0.0F, Math.min(180.0F, elevationDeg));
        decayRadarContactsStatic(aircraftId, t);
        validateTrackingStateImmediate(ac, player, aircraftId, partialTicks, elevationRef, followTurretYaw,
            elevationCoverage, scanAzClamp, scanElClamp, ac.getAcInfo().radarMinScanAltitude, ac.getAcInfo().radarMaxScanAltitude, maxDistance, searchType);
        pruneRadarContactsImmediate(ac, player, aircraftId, partialTicks, elevationRef, followTurretYaw,
            elevationCoverage, scanAzClamp, scanElClamp, ac.getAcInfo().radarMinScanAltitude, ac.getAcInfo().radarMaxScanAltitude, maxDistance, searchType);
        if (ac.jammingTick <= 0) {
            if (acmMode) {
                refreshRadarContactsAcm(aircraftId, ac, player, t, partialTicks, usedScanTick, acmFovDeg, elevationDeg, elevationRef, elevationCoverage, followTurretYaw, minDistance, renderMaxDistance);
                if (!trackState.acmMode) {
                    acmMode = false;
                } else if (trackState.trackingTargetId > 0) {
                    trackState.acmMode = false;
                    acmMode = false;
                }
            } else {
                refreshRadarContacts(aircraftId, ac, player, t, partialTicks, usedScanTick, scanAngleDeg, elevationDeg, elevationRef, elevationCoverage, followTurretYaw, minDistance, maxDistance);
            }
        }
        boolean srcLikeMode = isSrcLikeMode(searchType);
        boolean hideSweepOnLock = !acmMode && srcLikeMode && trackState.trackingTargetId > 0;
        boolean showMainSweep = !hideSweepOnLock && !acmMode;
        // Keep normal radar panel elements visible in ACM; only replace the sweep animation.
        drawRadarScanSkeleton(sx, adjustedCy, radius, scanAngleDeg, phase, axisDeg, panelFillAlpha, radarUiColor, showMainSweep);
        drawTrackingSectorOverlay(sx, adjustedCy, radius, axisDeg, scanAngleDeg, trackAzDeg, panelFillAlpha, radarUiColor);
        if (acmMode) {
            drawAcmRadarSectorOverlay(ac, player, sx, adjustedCy, radius, axisDeg, phase, acmFovDeg, elevationRef, followTurretYaw, partialTicks, radarUiColor);
            drawAcmScanOverlay(sc, acmFovDeg, radarUiColor, ac.ticksExisted, followTurretYaw);
        }
        drawDataLinkFovRing(ac, player, trackState, sc);
        String modeLabel = searchType;
        if (acmMode) {
            modeLabel = "ACM";
        } else if (trackState.trackingTargetId > 0) {
            if ("SRC".equals(searchType)) {
                modeLabel = "STT";
            } else if ("TWS".equals(searchType)) {
                modeLabel = "TWS*";
            } else if ("GMTI_SRC".equals(searchType)) {
                modeLabel = "GMTI STT";
            } else if ("GMTI_TWS".equals(searchType)) {
                modeLabel = "GMTI TWS*";
            } else if ("MULTI_SRC".equals(searchType)) {
                modeLabel = "MULTI STT";
            } else if ("MULTI_TWS".equals(searchType)) {
                modeLabel = "MULTI TWS*";
            }
        } else if ("GMTI_SRC".equals(searchType)) {
            modeLabel = "GMTI SRC";
        } else if ("GMTI_TWS".equals(searchType)) {
            modeLabel = "GMTI TWS";
        } else if ("MULTI_SRC".equals(searchType)) {
            modeLabel = "MULTI SRC";
        } else if ("MULTI_TWS".equals(searchType)) {
            modeLabel = "MULTI TWS";
        }
        renderRadarPanelScaleText(mc, sc, sx, adjustedCy, radius, scanAngleDeg, elevationDeg, elevationCoverage, renderMaxDistance, modeLabel, radarUiColor);
        if (ac.jammingTick <= 0) {
            renderRadarContacts(mc, sc, ac, player, aircraftId, sx, adjustedCy, radius, minRadius, axisDeg, partialTicks, elevationRef, followTurretYaw, minDistance, renderMaxDistance, radarUiColor);
        }
        if (ac.jammingTick > 0) {
            drawRWRCircle(sx, adjustedCy, sc, RWR_jammed, panelSize);
        }
        sendRadarLockHeartbeat(ac, aircraftId, trackState, t);
        maybeLogDataLinkWatch(ac, player, trackState, searchType, t);
    }

    private void maybeLogDataLinkWatch(MCH_EntityAircraft ac, EntityPlayer player, RadarTrackState state, String searchType, long worldTick) {
        if (ac == null || player == null || state == null || !MCH_RadarDebug.isDataLinkWatchEnabled()) {
            return;
        }
        int acId = ac.getEntityId();
        long last = dataLinkWatchLastLogTick.containsKey(acId) ? dataLinkWatchLastLogTick.get(acId) : -1L;
        int interval = Math.max(5, MCH_RadarDebug.getDataLinkWatchIntervalTick());
        if (last >= 0L && worldTick - last < interval) {
            return;
        }
        dataLinkWatchLastLogTick.put(acId, worldTick);
        MCH_WeaponSet ws = ac.getCurrentWeapon(player);
        MCH_WeaponInfo wi = ws != null ? ws.getInfo() : null;
        String weaponName = ws != null ? ws.getName() : "(none)";
        boolean dlEnabled = wi != null && wi.enableDataLink;
        boolean dlOnly = wi != null && wi.onlyDataLink;
        boolean dlMode = ws != null && (dlOnly || ws.isDataLinkMode());
        int selected = state.selectedTargetId;
        int tracking = state.trackingTargetId;
        int missileCount = 0;
        int relayCount = 0;
        int capturedCount = 0;
        if (ac.worldObj != null) {
            for (Object o : ac.worldObj.loadedEntityList) {
                if (!(o instanceof MCH_EntityBaseBullet)) {
                    continue;
                }
                MCH_EntityBaseBullet m = (MCH_EntityBaseBullet)o;
                if (m.shootingAircraft == ac || m.shootingEntity == ac || m.shootingEntity == player) {
                    missileCount++;
                    if (m.isDataLinkRelayMode()) {
                        relayCount++;
                    }
                    if (m.isActiveRadarCaptured()) {
                        capturedCount++;
                    }
                }
            }
        }
        MCH_RadarDebug.appendManual("DLWATCH tick=%d acId=%d weapon=%s dlEnabled=%s dlOnly=%s dlMode=%s search=%s sel=%d trk=%d self=%d relay=%d captured=%d",
            worldTick, acId, weaponName, dlEnabled, dlOnly, dlMode, searchType, selected, tracking, missileCount, relayCount, capturedCount);
    }

    private void renderRadarPanelScaleText(Minecraft mc, ScaledResolution sc, double cx, double cy, double radius, float scanAngleDeg, float elevationDeg, String elevationCoverage, double maxDistance, String modeLabel, int radarUiColor) {
        if (mc == null || mc.fontRenderer == null) {
            return;
        }
        int color = radarUiColor;
        String degUnit = getDegreeUnit(mc);
        float clampedAz = Math.max(0.0F, Math.min(360.0F, scanAngleDeg));
        int halfAz = clampedAz >= 359.9F ? 180 : Math.round(clampedAz * 0.5F);
        int maxDistInt = (int)Math.round(maxDistance);
        String modeText = "RADAR " + (modeLabel == null ? "SRC" : modeLabel);
        int modeY = (int)Math.round(cy - radius - 24.0D);
        drawRadarText(mc, sc, modeText, cx, modeY, color, true, 1.00F);

        String top = String.format(Locale.ROOT, "-%d%s   %dm   %d%s", halfAz, degUnit, maxDistInt, halfAz, degUnit);
        int topY = (int)Math.round(cy - radius - 14.0D);
        drawRadarText(mc, sc, top, cx, topY, color, true, 1.00F);

        // Intentionally hide elevation labels on radar panel for cleaner HUD.
    }

    private String getDegreeUnit(Minecraft mc) {
        if (mc != null && mc.gameSettings != null && mc.gameSettings.language != null) {
            String lang = mc.gameSettings.language.toLowerCase(Locale.ROOT);
            if (lang.startsWith("zh")) {
                return "度";
            }
        }
        return "deg";
    }

    private void drawRadarScanSkeleton(double cx, double cy, double radius, float scanAngleDeg, double phase, double axisDeg, float panelFillAlpha, int radarUiColor, boolean showSweepLine) {
        float clamped = Math.max(0.0F, Math.min(360.0F, scanAngleDeg));
        float fillAlpha = Math.max(0.0F, Math.min(1.0F, panelFillAlpha));
        if (clamped <= 0.0F || radius <= 1.0D) {
            return;
        }
        if (clamped >= 359.9F) {
            drawCircleFilled(cx, cy, radius, radarUiColor, fillAlpha);
            drawCircleOutline(cx, cy, radius, radarUiColor, 1.0F);
            if (showSweepLine) {
                drawCircleSweepLine(cx, cy, radius, phase, axisDeg, radarUiColor, 1.0F);
            }
        } else {
            drawSectorFilled(cx, cy, radius, clamped, axisDeg, radarUiColor, fillAlpha);
            drawSectorOutline(cx, cy, radius, clamped, axisDeg, radarUiColor, 1.0F);
            if (showSweepLine) {
                drawSectorSweepLine(cx, cy, radius, clamped, phase, axisDeg, radarUiColor, 1.0F);
            }
        }
    }

    private void drawTrackingSectorOverlay(double cx, double cy, double radius, double axisDeg, float scanAngleDeg, float trackAzDeg, float panelFillAlpha, int radarUiColor) {
        float scanClamp = Math.max(0.0F, Math.min(360.0F, scanAngleDeg));
        float trackClamp = Math.max(0.0F, Math.min(scanClamp, trackAzDeg));
        if (trackClamp <= 0.0F || radius <= 2.0D) {
            return;
        }
        // Track sector must reflect RadarTrackAzimuthDeg angle accurately on the same radar scale.
        double trackRadius = radius;
        int color = radarUiColor;
        float fillAlpha = Math.max(0.0F, Math.min(1.0F, panelFillAlpha * 1.5F));
        if (trackClamp >= 359.9F) {
            // Full-circle track overlay should not use sector outline, otherwise
            // the sector boundary seam adds an extra radial line at 0/360 degrees.
            drawCircleFilled(cx, cy, trackRadius, color, fillAlpha);
            drawCircleOutline(cx, cy, trackRadius, color, 1.0F);
        } else {
            drawSectorFilled(cx, cy, trackRadius, trackClamp, axisDeg, color, fillAlpha);
            drawSectorOutline(cx, cy, trackRadius, trackClamp, axisDeg, color, 1.0F);
        }
    }

    private void drawDataLinkFovRing(MCH_EntityAircraft ac, EntityPlayer player, RadarTrackState state, ScaledResolution sc) {
        if (ac == null || player == null || state == null || sc == null) {
            return;
        }
        if (state.selectedTargetId <= 0 && state.trackingTargetId <= 0) {
            return;
        }
        MCH_WeaponSet ws = ac.getCurrentWeapon(player);
        if (ws == null || ws.getInfo() == null) {
            return;
        }
        MCH_WeaponInfo wi = ws.getInfo();
        if (wi.antiRadiationMissile || !wi.enableDataLink) {
            return;
        }
        if (!(wi.activeRadar || wi.passiveRadar || wi.semiActiveRadar) && !(("aamissile".equals(wi.type) || "atmissile".equals(wi.type)) && wi.isHeatSeekerMissile && !wi.activeRadar && !wi.passiveRadar && !wi.semiActiveRadar)) {
            return;
        }
        boolean dlMode = wi.onlyDataLink || ws.isDataLinkMode();
        if (!dlMode) {
            return;
        }
        float halfFovDeg = Math.max(0.0F, Math.min(89.0F, (float) wi.getHudPreferredMissileFovDeg()));
        if (halfFovDeg <= 0.0F) {
            return;
        }
        long tick = ac.worldObj != null ? ac.worldObj.getTotalWorldTime() : 0L;
        float blinkAlpha = ((tick / 5L) % 2L == 0L) ? 1.0F : 0.45F;
        // Draw around the mouse lock position (crosshair/screen center).
        double cx = sc.getScaledWidth_double() * 0.5D;
        double cy = sc.getScaledHeight_double() * 0.5D;
        Minecraft mc = Minecraft.getMinecraft();
        float cameraFovDeg = mc != null && mc.gameSettings != null ? mc.gameSettings.fovSetting : 70.0F;
        cameraFovDeg = Math.max(20.0F, Math.min(170.0F, cameraFovDeg));
        double baseRadius = Math.min(sc.getScaledWidth_double(), sc.getScaledHeight_double()) * 0.5D;
        double ringRadius = baseRadius * Math.tan(Math.toRadians(halfFovDeg)) / Math.tan(Math.toRadians(cameraFovDeg * 0.5D));
        double maxScreenRadius = Math.hypot(sc.getScaledWidth_double() * 0.5D, sc.getScaledHeight_double() * 0.5D);
        ringRadius = Math.max(12.0D, Math.min(maxScreenRadius, ringRadius));
        prepareShapeRenderState(0xFFFFFF, blinkAlpha);
        GL11.glLineWidth(2.0F);
        Tessellator tess = Tessellator.instance;
        int seg = 32;
        double step = 360.0D / seg;
        tess.startDrawing(GL11.GL_LINE_LOOP);
        for (int i = 0; i < seg; ++i) {
            double ang = Math.toRadians(step * i);
            tess.addVertex(cx + Math.cos(ang) * ringRadius, cy + Math.sin(ang) * ringRadius, 0.0D);
        }
        tess.draw();
        restoreShapeRenderState();
    }

    private void drawAcmScanOverlay(ScaledResolution sc, float acmFovDeg, int radarUiColor, int ticks, boolean followTurretYaw) {
        if (sc == null) {
            return;
        }
        // Keep ACM box centered on current view direction (crosshair).
        double cx = sc.getScaledWidth_double() * 0.5D;
        double cy = sc.getScaledHeight_double() * 0.5D;
        Minecraft mc = Minecraft.getMinecraft();
        float cameraFovDeg = mc != null && mc.gameSettings != null ? mc.gameSettings.fovSetting : 70.0F;
        cameraFovDeg = Math.max(20.0F, Math.min(170.0F, cameraFovDeg));
        double baseRadius = Math.min(sc.getScaledWidth_double(), sc.getScaledHeight_double()) * 0.5D;
        double acmRadius = baseRadius
            * Math.tan(Math.toRadians(acmFovDeg * 0.5D))
            / Math.tan(Math.toRadians(cameraFovDeg * 0.5D));
        acmRadius = Math.max(8.0D, Math.min(baseRadius * 0.35D, acmRadius));
        float alpha = (ticks / 3) % 2 == 0 ? 1.0F : 0.45F;
        prepareShapeRenderState(radarUiColor, alpha);
        GL11.glLineWidth(1.0F);
        Tessellator tess = Tessellator.instance;
        tess.startDrawing(GL11.GL_LINE_LOOP);
        tess.addVertex(cx - acmRadius, cy - acmRadius, 0.0D);
        tess.addVertex(cx + acmRadius, cy - acmRadius, 0.0D);
        tess.addVertex(cx + acmRadius, cy + acmRadius, 0.0D);
        tess.addVertex(cx - acmRadius, cy + acmRadius, 0.0D);
        tess.draw();
        double sweepPhase = ((ticks % 6) + 0.5D) / 6.0D;
        double sx = cx - acmRadius + acmRadius * 2.0D * sweepPhase;
        tess.startDrawing(GL11.GL_LINES);
        tess.addVertex(sx, cy - acmRadius, 0.0D);
        tess.addVertex(sx, cy + acmRadius, 0.0D);
        tess.draw();
        restoreShapeRenderState();
    }

    private static double[] getMouseScreenPosStatic(ScaledResolution sc) {
        double sw = sc.getScaledWidth_double();
        double sh = sc.getScaledHeight_double();
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.displayWidth <= 0 || mc.displayHeight <= 0) {
            return new double[]{sw * 0.5D, sh * 0.5D};
        }
        double mx = (double)Mouse.getX() * sw / (double)mc.displayWidth;
        // LWJGL mouse Y origin is bottom-left; GUI uses top-left.
        double my = sh - (double)Mouse.getY() * sh / (double)mc.displayHeight - 1.0D;
        mx = Math.max(0.0D, Math.min(sw, mx));
        my = Math.max(0.0D, Math.min(sh, my));
        return new double[]{mx, my};
    }

    private void drawAcmRadarSectorOverlay(MCH_EntityAircraft ac, EntityPlayer player, double cx, double cy, double radius,
                                           double axisDeg, double phase, float acmFovDeg, String elevationRef,
                                           boolean followTurretYaw, float partialTicks, int radarUiColor) {
        if (ac == null || player == null || radius <= 1.0D) {
            return;
        }
        double centerBearing = 0.0D;
        double[] center = computeAcmCenterAnglesFromMouse(ac, player, partialTicks, elevationRef, followTurretYaw);
        if (center != null) {
            centerBearing = center[0];
        }
        double localAxisDeg = axisDeg + centerBearing;
        float fillAlpha = 0.10F;
        drawSectorFilled(cx, cy, radius, acmFovDeg, localAxisDeg, radarUiColor, fillAlpha);
        drawSectorOutline(cx, cy, radius, acmFovDeg, localAxisDeg, radarUiColor, 1.0F);
        drawSectorSweepLine(cx, cy, radius, acmFovDeg, phase, localAxisDeg, radarUiColor, 1.0F);
    }

    private void drawSectorFilled(double cx, double cy, double radius, float scanAngleDeg, double axisDeg, int rgb, float alpha) {
        int seg = Math.max(24, (int)(scanAngleDeg / 3.0F));
        double start = axisDeg - scanAngleDeg * 0.5D;
        double step = scanAngleDeg / seg;
        prepareShapeRenderState(rgb, alpha);
        Tessellator tess = Tessellator.instance;
        tess.startDrawing(GL11.GL_TRIANGLE_FAN);
        tess.addVertex(cx, cy, 0.0D);
        for (int i = 0; i <= seg; i++) {
            double ang = Math.toRadians(start + step * i);
            tess.addVertex(cx + Math.cos(ang) * radius, cy + Math.sin(ang) * radius, 0.0D);
        }
        tess.draw();
        restoreShapeRenderState();
    }

    private void drawSectorOutline(double cx, double cy, double radius, float scanAngleDeg, double axisDeg, int rgb, float alpha) {
        int seg = Math.max(24, (int)(scanAngleDeg / 3.0F));
        double start = axisDeg - scanAngleDeg * 0.5D;
        double step = scanAngleDeg / seg;
        prepareShapeRenderState(rgb, alpha);
        GL11.glLineWidth(1.0F);
        Tessellator tess = Tessellator.instance;
        tess.startDrawing(GL11.GL_LINE_STRIP);
        tess.addVertex(cx, cy, 0.0D);
        for (int i = 0; i <= seg; i++) {
            double ang = Math.toRadians(start + step * i);
            tess.addVertex(cx + Math.cos(ang) * radius, cy + Math.sin(ang) * radius, 0.0D);
        }
        tess.addVertex(cx, cy, 0.0D);
        tess.draw();
        restoreShapeRenderState();
    }

    private void drawCircleFilled(double cx, double cy, double radius, int rgb, float alpha) {
        int seg = 96;
        prepareShapeRenderState(rgb, alpha);
        Tessellator tess = Tessellator.instance;
        tess.startDrawing(GL11.GL_TRIANGLE_FAN);
        tess.addVertex(cx, cy, 0.0D);
        for (int i = 0; i <= seg; i++) {
            double ang = Math.toRadians((360.0D / seg) * i - 90.0D);
            tess.addVertex(cx + Math.cos(ang) * radius, cy + Math.sin(ang) * radius, 0.0D);
        }
        tess.draw();
        restoreShapeRenderState();
    }

    private void drawCircleOutline(double cx, double cy, double radius, int rgb, float alpha) {
        int seg = 96;
        prepareShapeRenderState(rgb, alpha);
        GL11.glLineWidth(1.0F);
        Tessellator tess = Tessellator.instance;
        tess.startDrawing(GL11.GL_LINE_STRIP);
        for (int i = 0; i <= seg; i++) {
            double ang = Math.toRadians((360.0D / seg) * i - 90.0D);
            tess.addVertex(cx + Math.cos(ang) * radius, cy + Math.sin(ang) * radius, 0.0D);
        }
        tess.draw();
        restoreShapeRenderState();
    }

    private void drawSectorSweepLine(double cx, double cy, double radius, float scanAngleDeg, double phase, double axisDeg, int rgb, float alpha) {
        double half = scanAngleDeg * 0.5D;
        double start = axisDeg - half;
        double p = Math.max(0.0D, Math.min(1.0D, phase));
        // Ping-pong: 0->1->0 in one period
        double pingPong = p <= 0.5D ? (p * 2.0D) : (2.0D - p * 2.0D);
        double angleDeg = start + scanAngleDeg * pingPong;
        drawSweepLine(cx, cy, radius, angleDeg, rgb, alpha);
    }

    private void drawCircleSweepLine(double cx, double cy, double radius, double phase, double axisDeg, int rgb, float alpha) {
        double p = Math.max(0.0D, Math.min(1.0D, phase));
        // Single direction: 0->360 in one period
        double angleDeg = axisDeg + 360.0D * p;
        drawSweepLine(cx, cy, radius, angleDeg, rgb, alpha);
    }

    private void drawSweepLine(double cx, double cy, double radius, double angleDeg, int rgb, float alpha) {
        double ang = Math.toRadians(angleDeg);
        double x2 = cx + Math.cos(ang) * radius;
        double y2 = cy + Math.sin(ang) * radius;
        prepareShapeRenderState(rgb, alpha);
        GL11.glLineWidth(1.0F);
        Tessellator tess = Tessellator.instance;
        tess.startDrawing(GL11.GL_LINES);
        tess.addVertex(cx, cy, 0.0D);
        tess.addVertex(x2, y2, 0.0D);
        tess.draw();
        restoreShapeRenderState();
    }

    private void prepareShapeRenderState(int rgb, float alpha) {
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_LINE_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(false);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        float r = ((rgb >> 16) & 0xFF) / 255.0F;
        float g = ((rgb >> 8) & 0xFF) / 255.0F;
        float b = (rgb & 0xFF) / 255.0F;
        GL11.glColor4f(r, g, b, alpha);
    }

    private void restoreShapeRenderState() {
        GL11.glDepthMask(true);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glPopAttrib();
    }

    private void renderRadarContacts(Minecraft mc, ScaledResolution sc, MCH_EntityAircraft ac, EntityPlayer player, int aircraftId, double cx, double cy, double radius, int minRadius, double axisDeg, float partialTicks, String elevationRef, boolean followTurretYaw, double minDistance, double maxDistance, int radarUiColor) {
        float scanAzClamp = Math.max(0.0F, Math.min(360.0F, ac.getAcInfo().radarScanAzimuthDeg));
        float scanElClamp = Math.max(0.0F, Math.min(180.0F, ac.getAcInfo().radarScanElevationDeg));
        float minAltitude = ac.getAcInfo() != null ? ac.getAcInfo().radarMinScanAltitude : 10.0F;
        float maxAltitude = ac.getAcInfo() != null ? ac.getAcInfo().radarMaxScanAltitude : 25.0F;
        String elevationCoverage = ac.getAcInfo() != null ? ac.getAcInfo().radarElevationCoverage : "UP_ONLY";
        String searchType = normalizeRadarSearchType(ac.getAcInfo() != null ? ac.getAcInfo().radarSearchType : "SRC");
        boolean srcLikeMode = isSrcLikeMode(searchType);
        boolean twsLikeMode = isTwsLikeMode(searchType);
        RadarTrackState state = getOrCreateRadarTrackState(aircraftId);
        if (srcLikeMode && state.trackingTargetId > 0) {
            RadarRenderPoint p = renderSingleContact(mc, sc, ac, player, state.trackingTargetId, cx, cy, radius, minRadius, axisDeg, partialTicks, elevationRef, followTurretYaw, minDistance, maxDistance, scanAzClamp, scanElClamp, elevationCoverage, minAltitude, maxAltitude, searchType, true, radarUiColor);
            if (p != null) {
                drawTrackingLink(cx, cy, p.x, p.y, 0xFF4040);
            }
            renderRadarMissileOverlays(ac, player, cx, cy, radius, minRadius, axisDeg, partialTicks, elevationRef, followTurretYaw, minDistance, maxDistance, scanAzClamp, scanElClamp, elevationCoverage, minAltitude, maxAltitude, searchType, radarUiColor, state);
            return;
        }
        Map<Integer, RadarContact> cache = radarContactCache.get(aircraftId);
        if (cache == null || cache.isEmpty()) {
            renderRadarMissileOverlays(ac, player, cx, cy, radius, minRadius, axisDeg, partialTicks, elevationRef, followTurretYaw, minDistance, maxDistance, scanAzClamp, scanElClamp, elevationCoverage, minAltitude, maxAltitude, searchType, radarUiColor, state);
            return;
        }
        Iterator<Map.Entry<Integer, RadarContact>> it = cache.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, RadarContact> e = it.next();
            MCH_EntityInfo entity = MCH_EntityInfoClientTracker.getEntityInfo(e.getKey());
            if (entity == null) {
                it.remove();
                continue;
            }
            RadarRenderPoint p = renderSingleContact(mc, sc, ac, player, e.getKey(), cx, cy, radius, minRadius, axisDeg, partialTicks, elevationRef, followTurretYaw, minDistance, maxDistance, scanAzClamp, scanElClamp, elevationCoverage, minAltitude, maxAltitude, searchType, false, radarUiColor);
            if (p == null) {
                continue;
            }
        }
        if (twsLikeMode && state.selectedTargetId > 0 && state.selectedTargetId != state.trackingTargetId) {
            RadarRenderPoint sp = renderSingleContact(mc, sc, ac, player, state.selectedTargetId, cx, cy, radius, minRadius, axisDeg, partialTicks, elevationRef, followTurretYaw, minDistance, maxDistance, scanAzClamp, scanElClamp, elevationCoverage, minAltitude, maxAltitude, searchType, false, radarUiColor);
            if (sp != null) {
                drawTrackingLinkDashed(cx, cy, sp.x, sp.y, 0xFF4040);
            }
        }
        if (twsLikeMode && state.trackingTargetId > 0) {
            RadarRenderPoint tp = renderSingleContact(mc, sc, ac, player, state.trackingTargetId, cx, cy, radius, minRadius, axisDeg, partialTicks, elevationRef, followTurretYaw, minDistance, maxDistance, scanAzClamp, scanElClamp, elevationCoverage, minAltitude, maxAltitude, searchType, true, radarUiColor);
            if (tp != null) {
                drawTrackingLink(cx, cy, tp.x, tp.y, 0xFF4040);
            }
        }
        renderRadarMissileOverlays(ac, player, cx, cy, radius, minRadius, axisDeg, partialTicks, elevationRef, followTurretYaw, minDistance, maxDistance, scanAzClamp, scanElClamp, elevationCoverage, minAltitude, maxAltitude, searchType, radarUiColor, state);
    }

    private void renderRadarMissileOverlays(MCH_EntityAircraft ac, EntityPlayer player,
                                            double cx, double cy, double radius, int minRadius, double axisDeg, float partialTicks,
                                            String elevationRef, boolean followTurretYaw, double minDistance, double maxDistance,
                                            float scanAzClamp, float scanElClamp, String elevationCoverage, float minAltitude, float maxAltitude,
                                            String searchType, int radarUiColor, RadarTrackState state) {
        if (ac == null || ac.worldObj == null) {
            return;
        }
        for (Object obj : ac.worldObj.loadedEntityList) {
            if (!(obj instanceof MCH_EntityBaseBullet)) {
                continue;
            }
            MCH_EntityBaseBullet missile = (MCH_EntityBaseBullet)obj;
            if (!isMissile(missile.getClass().getName())) {
                continue;
            }
            if (!isOwnLaunchedMissile(ac, player, missile)) {
                continue;
            }
            MCH_WeaponInfo wi = missile.getInfo();
            if (wi == null || wi.antiRadiationMissile || !(wi.activeRadar || wi.passiveRadar || wi.semiActiveRadar)) {
                continue;
            }
            RadarProjection mProj = projectPointStatic(ac, player, missile.posX, missile.posY, missile.posZ, partialTicks, elevationRef, followTurretYaw);
            if (mProj == null) {
                continue;
            }
            double mAgl = computeAgl(ac.worldObj, missile.posX, missile.posY, missile.posZ);
            if (!isProjectionInsideScanRange(mProj, mAgl, scanAzClamp, scanElClamp, elevationCoverage, minAltitude, maxAltitude, maxDistance, searchType, null)) {
                continue;
            }
            RadarRenderPoint mp = projectToPanelPoint(mProj, cx, cy, radius, minRadius, axisDeg, minDistance, maxDistance);
            drawRadarContactPoint(null, null, mp.x, mp.y, 0xFF4040, 1, false, false, null);

            Entity target = missile.targetEntity;
            if (target == null || target.isDead) {
                continue;
            }
            RadarProjection tProj = projectPointStatic(ac, player, target.posX, target.posY + target.height * 0.5D, target.posZ, partialTicks, elevationRef, followTurretYaw);
            if (tProj == null) {
                continue;
            }
            double tAgl = computeAgl(ac.worldObj, target.posX, target.posY, target.posZ);
            MCH_EntityInfo tInfo = MCH_EntityInfoClientTracker.getEntityInfo(target.getEntityId());
            if (!isProjectionInsideScanRange(tProj, tAgl, scanAzClamp, scanElClamp, elevationCoverage, minAltitude, maxAltitude, maxDistance, searchType, tInfo)) {
                continue;
            }
            RadarRenderPoint tp = projectToPanelPoint(tProj, cx, cy, radius, minRadius, axisDeg, minDistance, maxDistance);

            if (wi.passiveRadar || wi.semiActiveRadar) {
                // Passive/Semi-active relay line is valid only while aircraft radar keeps tracking the same target.
                if (state != null && state.trackingTargetId > 0 && target.getEntityId() == state.trackingTargetId) {
                    drawTrackingLinkDashed(mp.x, mp.y, tp.x, tp.y, 0xFF4040);
                }
            } else if (wi.activeRadar) {
                boolean relayPhase = missile.isDataLinkRelayMode() && !missile.isActiveRadarCaptured();
                if (relayPhase) {
                    boolean radarHasLink = state != null && (target.getEntityId() == state.trackingTargetId || target.getEntityId() == state.selectedTargetId);
                    if (radarHasLink) {
                        drawTrackingLinkDashed(mp.x, mp.y, tp.x, tp.y, 0xFF4040);
                    }
                } else {
                    boolean seekerJammed = false;
                    if (target instanceof MCH_EntityAircraft) {
                        MCH_EntityAircraft tac = (MCH_EntityAircraft)target;
                        seekerJammed = tac.isChaffUsing() || tac.isECMJammerUsing() || tac.jammingTick > 0;
                    }
                    if (!seekerJammed) {
                        drawTrackingLink(mp.x, mp.y, tp.x, tp.y, 0xFF4040);
                    }
                }
            }
        }
    }

    private RadarRenderPoint renderSingleContact(Minecraft mc, ScaledResolution sc, MCH_EntityAircraft ac, EntityPlayer player, int entityId,
                                                 double cx, double cy, double radius, int minRadius, double axisDeg, float partialTicks,
                                                 String elevationRef, boolean followTurretYaw, double minDistance, double maxDistance,
                                                 float scanAzClamp, float scanElClamp, String elevationCoverage, float minAltitude, float maxAltitude, String searchType,
                                                 boolean forceTrackingColor, int radarUiColor) {
        MCH_EntityInfo entity = MCH_EntityInfoClientTracker.getEntityInfo(entityId);
        if (entity == null) {
            return null;
        }
        RadarTrackState state = radarTrackStateMap.get(ac.getEntityId());
        // Keep lock symbol and radar point in sync: while lock exists, do not suppress this target by normal scan filtering.
        boolean lockSyncedRender = forceTrackingColor && state != null && state.trackingTargetId == entityId;
        if (!lockSyncedRender && !isTrackableForSearchType(entity, searchType)) {
            return null;
        }
        RadarProjection projection = projectContact(ac, player, entity, partialTicks, elevationRef, followTurretYaw);
        if (projection == null) {
            return null;
        }
        double targetX = interpolate(entity.posX, entity.lastTickPosX, partialTicks);
        double targetY = interpolate(entity.posY, entity.lastTickPosY, partialTicks);
        double targetZ = interpolate(entity.posZ, entity.lastTickPosZ, partialTicks);
        double targetAgl = computeAgl(ac.worldObj, targetX, targetY, targetZ);
        if (!lockSyncedRender && !isProjectionInsideScanRange(projection, targetAgl, scanAzClamp, scanElClamp, elevationCoverage, minAltitude, maxAltitude, maxDistance, searchType, entity)) {
            return null;
        }
        MCH_RWRResult rwrResult = getTargetTypeOnRadar(entity, ac);
        RadarRenderPoint panelPoint = projectToPanelPoint(projection, cx, cy, radius, minRadius, axisDeg, minDistance, maxDistance);
        double px = panelPoint.x;
        double py = panelPoint.y;
        boolean isTracking = state != null && state.trackingTargetId == entityId;
        boolean isSelected = state != null && state.selectedTargetId == entityId;
        boolean highlight = forceTrackingColor || isSelected || isTracking;
        int color = highlight ? 0xFF4040 : radarUiColor;
        int pointSize = isMissile(entity.entityClassName) ? 1 : (isVehicle(entity.entityClassName) ? 3 : 2);
        String radarName = rwrResult != null && rwrResult.name != null ? rwrResult.name : "";
        drawRadarContactPoint(mc, sc, px, py, color, pointSize, isSelected, isTracking, radarName);
        if (isVehicle(entity.entityClassName)) {
            drawVehicleVelocityVector(ac, player, entity, px, py, cx, cy, radius, minRadius, axisDeg, minDistance, maxDistance, elevationRef, followTurretYaw, color, partialTicks);
        }
        RadarRenderPoint p = new RadarRenderPoint();
        p.x = px;
        p.y = py;
        return p;
    }

    private RadarRenderPoint projectToPanelPoint(RadarProjection projection, double cx, double cy, double radius, int minRadius, double axisDeg, double minDistance, double maxDistance) {
        double panelAngle = axisDeg + projection.bearingDeg;
        double rr = (projection.distance - minDistance) / Math.max(1.0D, (maxDistance - minDistance));
        rr = Math.max(0.0D, Math.min(1.0D, rr));
        double displayRadius = minRadius + (radius - minRadius) * rr;
        double rad = Math.toRadians(panelAngle);
        RadarRenderPoint p = new RadarRenderPoint();
        p.x = cx + Math.cos(rad) * displayRadius;
        p.y = cy + Math.sin(rad) * displayRadius;
        return p;
    }

    private void drawVehicleVelocityVector(MCH_EntityAircraft ac, EntityPlayer player, MCH_EntityInfo entity, double px, double py,
                                           double cx, double cy, double radius, int minRadius, double axisDeg, double minDistance, double maxDistance,
                                           String elevationRef, boolean followTurretYaw, int rgb, float partialTicks) {
        double vx = entity.posX - entity.lastTickPosX;
        double vy = entity.posY - entity.lastTickPosY;
        double vz = entity.posZ - entity.lastTickPosZ;
        double speed = Math.sqrt(vx * vx + vy * vy + vz * vz);
        if (speed <= 1.0E-4D) {
            return;
        }
        RadarProjection nextProj = projectPointStatic(ac, player, entity.posX + vx, entity.posY + vy, entity.posZ + vz, partialTicks, elevationRef, followTurretYaw);
        if (nextProj == null) {
            return;
        }
        RadarRenderPoint nextPoint = projectToPanelPoint(nextProj, cx, cy, radius, minRadius, axisDeg, minDistance, maxDistance);
        double dx = nextPoint.x - px;
        double dy = nextPoint.y - py;
        double len = Math.sqrt(dx * dx + dy * dy);
        if (len <= 1.0E-4D) {
            return;
        }
        double ux = dx / len;
        double uy = dy / len;
        double drawLen = Math.max(4.0D, Math.min(8.0D, 4.0D + speed * 4.0D));
        drawTrackingLink(px, py, px + ux * drawLen, py + uy * drawLen, rgb);
    }

    private static boolean isProjectionInsideScanRange(RadarProjection proj, double targetAgl, float scanAzClamp, float scanElClamp,
                                                String elevationCoverage, float minAltitude, float maxAltitude, double maxDistance, String searchType,
                                                MCH_EntityInfo targetInfo) {
        if (proj == null) {
            return false;
        }
        boolean missileTarget = targetInfo != null && isMissileClassName(targetInfo.entityClassName);
        if (!missileTarget) {
            if (isMultiMode(searchType)) {
                if (isGroundRadarTargetEntity(targetInfo)) {
                    if (targetAgl > maxAltitude) {
                        return false;
                    }
                } else if (targetAgl < minAltitude) {
                    return false;
                }
            } else if (isGmtiMode(searchType)) {
                if (targetAgl > maxAltitude) {
                    return false;
                }
            } else if (targetAgl < minAltitude) {
                return false;
            }
        }
        if (proj.distance <= 1.0E-4D || proj.distance > maxDistance) {
            return false;
        }
        if (scanAzClamp < 359.9F && Math.abs(proj.bearingDeg) > scanAzClamp * 0.5D) {
            return false;
        }
        return isElevationInCoverageStatic(proj.elevationDeg, scanElClamp, elevationCoverage);
    }

    private void drawTrackingLink(double cx, double cy, double tx, double ty, int color) {
        prepareShapeRenderState(color, 1.0F);
        GL11.glLineWidth(1.0F);
        Tessellator tess = Tessellator.instance;
        tess.startDrawing(GL11.GL_LINES);
        tess.addVertex(cx, cy, 0.0D);
        tess.addVertex(tx, ty, 0.0D);
        tess.draw();
        restoreShapeRenderState();
    }

    private void drawTrackingLinkDashed(double cx, double cy, double tx, double ty, int color) {
        double dx = tx - cx;
        double dy = ty - cy;
        double len = Math.sqrt(dx * dx + dy * dy);
        if (len <= 1.0E-4D) {
            return;
        }
        double ux = dx / len;
        double uy = dy / len;
        double dash = 6.0D;
        double gap = 4.0D;
        double pos = 0.0D;
        while (pos < len) {
            double s = pos;
            double e = Math.min(len, pos + dash);
            drawTrackingLink(cx + ux * s, cy + uy * s, cx + ux * e, cy + uy * e, color);
            pos += dash + gap;
        }
    }

    private void drawRadarContactPoint(Minecraft mc, ScaledResolution sc, double x, double y, int rgb, int pointSize, boolean isSelected, boolean isTracking, String radarName) {
        prepareShapeRenderState(rgb, 1.0F);
        double half = Math.max(0.5D, pointSize * 0.5D);
        Tessellator tess = Tessellator.instance;
        tess.startDrawingQuads();
        tess.addVertex(x - half, y + half, 0.0D);
        tess.addVertex(x + half, y + half, 0.0D);
        tess.addVertex(x + half, y - half, 0.0D);
        tess.addVertex(x - half, y - half, 0.0D);
        tess.draw();
        if (isTracking) {
            GL11.glLineWidth(1.0F);
            tess.startDrawing(GL11.GL_LINE_LOOP);
            tess.addVertex(x - 2.5D, y + 2.5D, 0.0D);
            tess.addVertex(x + 2.5D, y + 2.5D, 0.0D);
            tess.addVertex(x + 2.5D, y - 2.5D, 0.0D);
            tess.addVertex(x - 2.5D, y - 2.5D, 0.0D);
            tess.draw();
        } else if (isSelected) {
            GL11.glLineWidth(1.0F);
            tess.startDrawing(GL11.GL_LINES);
            tess.addVertex(x - 2.5D, y + 2.5D, 0.0D);
            tess.addVertex(x - 2.5D, y - 2.5D, 0.0D);
            tess.addVertex(x + 2.5D, y + 2.5D, 0.0D);
            tess.addVertex(x + 2.5D, y - 2.5D, 0.0D);
            tess.draw();
        }
        restoreShapeRenderState();
        if (mc != null && sc != null && radarName != null && !radarName.isEmpty()) {
            drawRadarText(mc, sc, radarName, x, y + 6.0D, rgb, true, 0.90F);
        }
    }

    private void drawRadarText(Minecraft mc, ScaledResolution sc, String text, double x, double y, int rgb, boolean center, float baseScale) {
        if (mc == null || mc.fontRenderer == null || text == null || text.isEmpty()) {
            return;
        }
        float scale = Math.max(0.85F, Math.min(1.20F, baseScale));
        GL11.glPushMatrix();
        GL11.glTranslatef((float)x, (float)y, 0.0F);
        GL11.glScalef(scale, scale, 1.0F);
        int tx = 0;
        if (center) {
            tx = -mc.fontRenderer.getStringWidth(text) / 2;
        }
        mc.fontRenderer.drawString(text, tx, 0, rgb, false);
        GL11.glPopMatrix();
    }

    private static void decayRadarContactsStatic(int aircraftId, long worldTick) {
        Long last = radarLastDecayTick.get(aircraftId);
        if (last == null) {
            radarLastDecayTick.put(aircraftId, worldTick);
            return;
        }
        long elapsed = worldTick - last.longValue();
        if (elapsed <= 0L) {
            return;
        }
        Map<Integer, RadarContact> cache = radarContactCache.get(aircraftId);
        if (cache != null && !cache.isEmpty()) {
            Iterator<Map.Entry<Integer, RadarContact>> it = cache.entrySet().iterator();
            while (it.hasNext()) {
                RadarContact c = it.next().getValue();
                c.ttl -= (int)elapsed;
                if (c.ttl <= 0) {
                    it.remove();
                }
            }
        }
        radarLastDecayTick.put(aircraftId, worldTick);
    }

    private static void validateTrackingStateImmediate(MCH_EntityAircraft ac, EntityPlayer player, int aircraftId, float partialTicks,
                                                       String elevationRef, boolean followTurretYaw, String elevationCoverage,
                                                       float scanAzClamp, float scanElClamp, float minAltitude, float maxAltitude,
                                                       double maxDistance, String searchType) {
        if (ac == null || player == null) {
            return;
        }
        RadarTrackState state = radarTrackStateMap.get(aircraftId);
        if (state == null || state.trackingTargetId <= 0) {
            return;
        }
        float trackAzClamp = Math.max(0.0F, Math.min(scanAzClamp, ac.getAcInfo() != null ? ac.getAcInfo().radarTrackAzimuthDeg : 90.0F));
        float trackElClamp = Math.max(0.0F, Math.min(scanElClamp, ac.getAcInfo() != null ? ac.getAcInfo().radarTrackElevationDeg : 45.0F));
        MCH_EntityInfo trackingInfo = MCH_EntityInfoClientTracker.getEntityInfo(state.trackingTargetId);
        RadarProjection trackProj = projectContactStatic(ac, player, trackingInfo, partialTicks, elevationRef, followTurretYaw);
        String invalidReason = getTrackingInvalidReason(ac, player, trackProj, trackingInfo, trackAzClamp, trackElClamp, elevationCoverage,
            minAltitude, maxAltitude, maxDistance, searchType);
        if (invalidReason != null) {
            if (MCH_RadarDebug.isEnabled()) {
                MCH_RadarDebug.trace(ac.worldObj, ac, "track drop acId=%d target=%d reason=%s(immediate)", aircraftId, state.trackingTargetId, invalidReason);
            }
            int oldTrack = state.trackingTargetId;
            setTrackingTarget(ac, aircraftId, state, -1);
            if (state.selectedTargetId == oldTrack) {
                state.selectedTargetId = -1;
            }
        }
    }

    private static void pruneRadarContactsImmediate(MCH_EntityAircraft ac, EntityPlayer player, int aircraftId, float partialTicks,
                                                    String elevationRef, boolean followTurretYaw, String elevationCoverage,
                                                    float scanAzClamp, float scanElClamp, float minAltitude, float maxAltitude,
                                                    double maxDistance, String searchType) {
        if (ac == null || player == null) {
            return;
        }
        Map<Integer, RadarContact> cache = radarContactCache.get(aircraftId);
        if (cache == null || cache.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<Integer, RadarContact>> it = cache.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, RadarContact> entry = it.next();
            MCH_EntityInfo info = MCH_EntityInfoClientTracker.getEntityInfo(entry.getKey());
            if (info == null) {
                it.remove();
                continue;
            }
            if (System.currentTimeMillis() - info.lastUpdateTime > 4000L) {
                it.remove();
                continue;
            }
            if (!isTrackableForSearchType(info, searchType)
                || isSelfTarget(ac, player, info)
                || isOwnLaunchedMissile(ac, player, info)
                || isSameTeamTarget(player, ac, info)
                || isTargetCountermeasureActive(ac, info)
                || shouldFilterByAirSpeedGate(searchType, info)) {
                it.remove();
                continue;
            }
            RadarProjection proj = projectContactStatic(ac, player, info, partialTicks, elevationRef, followTurretYaw);
            if (proj == null) {
                it.remove();
                continue;
            }
            double targetAgl = computeAgl(ac.worldObj, info.posX, info.posY, info.posZ);
            if (!isProjectionInsideScanRange(proj, targetAgl, scanAzClamp, scanElClamp, elevationCoverage, minAltitude, maxAltitude, maxDistance, searchType, info)) {
                it.remove();
            }
        }
    }

    private void refreshRadarContacts(int aircraftId, MCH_EntityAircraft ac, EntityPlayer player, long worldTick, float partialTicks, int scanTick,
                                      float scanAngleDeg, float elevationDeg, String elevationRef, String elevationCoverage, boolean followTurretYaw,
                                      double minDistance, double maxDistance) {
        refreshRadarContactsStatic(aircraftId, ac, player, worldTick, partialTicks, scanTick, scanAngleDeg, elevationDeg, elevationRef, elevationCoverage, followTurretYaw, minDistance, maxDistance);
    }

    public static boolean isRadarContactVisible(MCH_EntityAircraft ac, EntityPlayer player, MCH_EntityInfo entity, float partialTicks) {
        if (ac == null || player == null || entity == null) {
            return false;
        }
        if (ac.getAcInfo() == null || !ac.getAcInfo().enableRadar || !ac.isRadarEnabledRuntime()) {
            return false;
        }
        int aircraftId = ac.getEntityId();
        long t = ac.worldObj.getTotalWorldTime();
        double minDistance = 15.0D;
        double maxDistance = 4096.0D;
        if (ac.getAcInfo().radarMaxTargetRange > 0.0F) {
            maxDistance = Math.min(maxDistance, ac.getAcInfo().radarMaxTargetRange);
        }
        if (maxDistance <= minDistance) {
            minDistance = Math.max(0.0D, maxDistance - 50.0D);
        }
        decayRadarContactsStatic(aircraftId, t);
        if (ac.jammingTick <= 0) {
            boolean followTurretYaw = (ac instanceof MCH_EntityTank) && ac.getAcInfo().radarFollowTurretYaw;
            refreshRadarContactsStatic(aircraftId, ac, player, t, partialTicks, Math.max(1, ac.getAcInfo().radarScanTick),
                ac.getAcInfo().radarScanAzimuthDeg, ac.getAcInfo().radarScanElevationDeg, ac.getAcInfo().radarElevationReference,
                ac.getAcInfo().radarElevationCoverage, followTurretYaw, minDistance, maxDistance);
        }
        Map<Integer, RadarContact> cache = radarContactCache.get(aircraftId);
        if (cache != null && cache.containsKey(entity.entityId)) {
            return true;
        }
        RadarTrackState state = radarTrackStateMap.get(aircraftId);
        return state != null && state.trackingTargetId == entity.entityId;
    }

    public static boolean isTankRadarContactVisible(MCH_EntityAircraft ac, EntityPlayer player, MCH_EntityInfo entity, float partialTicks) {
        return isRadarContactVisible(ac, player, entity, partialTicks);
    }

    private static void refreshRadarContactsStatic(int aircraftId, MCH_EntityAircraft ac, EntityPlayer player, long worldTick, float partialTicks, int scanTick,
                                                   float scanAngleDeg, float elevationDeg, String elevationRef, String elevationCoverage, boolean followTurretYaw,
                                                   double minDistance, double maxDistance) {
        long slot = worldTick / Math.max(1, scanTick);
        Long lastSlot = radarLastScanSlot.get(aircraftId);
        if (lastSlot != null && lastSlot.longValue() == slot) {
            return;
        }
        radarLastScanSlot.put(aircraftId, slot);
        if (!isAircraftMannedForRadarEmission(ac)) {
            Map<Integer, RadarContact> cache = radarContactCache.get(aircraftId);
            if (cache != null) {
                cache.clear();
            }
            RadarTrackState state = radarTrackStateMap.get(aircraftId);
            if (state != null && state.trackingTargetId > 0) {
                setTrackingTarget(ac, aircraftId, state, -1);
            }
            return;
        }
        float pBase = Math.max(0.0F, Math.min(1.0F, ac.getAcInfo().radarDetectChanceBase));
        float gainNear = ac.getAcInfo() != null ? ac.getAcInfo().radarGainNearFactor : 1.5F;
        float gainFar = ac.getAcInfo() != null ? ac.getAcInfo().radarGainFarFactor : 0.5F;
        gainNear = Math.max(0.01F, Math.min(10.0F, gainNear));
        gainFar = Math.max(0.01F, Math.min(10.0F, gainFar));
        int holdTick = Math.max(1, ac.getAcInfo().radarContactHoldTick);
        String searchType = normalizeRadarSearchType(ac.getAcInfo() != null ? ac.getAcInfo().radarSearchType : "SRC");
        float minAltitude = ac.getAcInfo() != null ? ac.getAcInfo().radarMinScanAltitude : 10.0F;
        float maxAltitude = ac.getAcInfo() != null ? ac.getAcInfo().radarMaxScanAltitude : 25.0F;
        float azClamp = Math.max(0.0F, Math.min(360.0F, scanAngleDeg));
        float elClamp = Math.max(0.0F, Math.min(180.0F, elevationDeg));
        float trackAzClamp = Math.max(0.0F, Math.min(azClamp, ac.getAcInfo() != null ? ac.getAcInfo().radarTrackAzimuthDeg : 90.0F));
        float trackElClamp = Math.max(0.0F, Math.min(elClamp, ac.getAcInfo() != null ? ac.getAcInfo().radarTrackElevationDeg : 45.0F));
        int retargetCooldownCfg = Math.max(0, ac.getAcInfo() != null ? ac.getAcInfo().radarRetargetCooldownTick : 40);
        RadarTrackState trackState = getOrCreateRadarTrackState(aircraftId);
        Map<Integer, RadarContact> cache = radarContactCache.get(aircraftId);
        if (cache == null) {
            cache = new HashMap<Integer, RadarContact>();
            radarContactCache.put(aircraftId, cache);
        }
        int candidate = 0;
        int passAlt = 0;
        int passRange = 0;
        int passAz = 0;
        int passElev = 0;
        int passProb = 0;
        int hit = 0;
        for (MCH_EntityInfo info : getServerLoadedEntityStatic()) {
            boolean trackable = isTrackableForSearchType(info, searchType);
            if (!isValidEntity(info, player, minDistance, searchType) || !trackable) {
                continue;
            }
            if (isSelfTarget(ac, player, info) || isOwnLaunchedMissile(ac, player, info)) {
                continue;
            }
            candidate++;
            boolean missileTarget = isMissileClassName(info.entityClassName);
            double targetX = interpolate(info.posX, info.lastTickPosX, partialTicks);
            double targetY = interpolate(info.posY, info.lastTickPosY, partialTicks);
            double targetZ = interpolate(info.posZ, info.lastTickPosZ, partialTicks);
            double targetAgl = computeAgl(ac.worldObj, targetX, targetY, targetZ);
            if (!missileTarget) {
                if (isMultiMode(searchType)) {
                    if (isGroundRadarTargetEntity(info)) {
                        if (targetAgl > maxAltitude) {
                            continue;
                        }
                    } else if (targetAgl < minAltitude) {
                        continue;
                    }
                } else if (isGmtiMode(searchType)) {
                    if (targetAgl > maxAltitude) {
                        continue;
                    }
                } else if (targetAgl < minAltitude) {
                    continue;
                }
            }
            passAlt++;
            if (shouldFilterByAirSpeedGate(searchType, info)) {
                continue;
            }
            RadarProjection proj = projectContactStatic(ac, player, info, partialTicks, elevationRef, followTurretYaw);
            if (proj == null || proj.distance <= 1.0E-4D || proj.distance > maxDistance) {
                continue;
            }
            passRange++;
            if (azClamp < 359.9F && Math.abs(proj.bearingDeg) > azClamp * 0.5D) {
                continue;
            }
            passAz++;
            if (!isElevationInCoverageStatic(proj.elevationDeg, elClamp, elevationCoverage)) {
                continue;
            }
            passElev++;
            RcsProfile rcsProfile = computeTargetRcsProfile(ac, info, partialTicks);
            float p = computeDetectProbabilityStatic(pBase, proj.bearingDeg, proj.elevationDeg, azClamp, elClamp, proj.distance, maxDistance,
                elevationCoverage, gainNear, gainFar, rcsProfile.detectFactor);
            boolean halfGround = isMultiMode(searchType) && isGroundRadarTargetEntity(info);
            if (halfGround) {
                p *= 0.5F;
            }
            if (ac.worldObj.rand.nextFloat() > p) {
                continue;
            }
            passProb++;
            RadarContact c = cache.get(info.entityId);
            if (c == null) {
                c = new RadarContact();
                cache.put(info.entityId, c);
            }
            float holdFactor = halfGround ? (rcsProfile.holdTimeFactor * 0.5F) : rcsProfile.holdTimeFactor;
            c.ttl = computeContactHoldTick(holdTick, holdFactor);
            hit++;
        }
        if (trackState.trackingTargetId > 0) {
            MCH_EntityInfo trackingInfo = MCH_EntityInfoClientTracker.getEntityInfo(trackState.trackingTargetId);
            RadarProjection trackProj = projectContactStatic(ac, player, trackingInfo, partialTicks, elevationRef, followTurretYaw);
            String invalidReason = getTrackingInvalidReason(ac, player, trackProj, trackingInfo, trackAzClamp, trackElClamp, elevationCoverage, minAltitude, maxAltitude, maxDistance, searchType);
            if (invalidReason != null) {
                if (MCH_RadarDebug.isEnabled()) {
                    MCH_RadarDebug.trace(ac.worldObj, ac, "track drop acId=%d target=%d reason=%s", aircraftId, trackState.trackingTargetId, invalidReason);
                }
                setTrackingTarget(ac, aircraftId, trackState, -1);
            }
        }
        if (trackState.selectedTargetId > 0) {
            MCH_EntityInfo selectedInfo = MCH_EntityInfoClientTracker.getEntityInfo(trackState.selectedTargetId);
            boolean invalidTargetType = !isTrackableForSearchType(selectedInfo, searchType);
            if (invalidTargetType || isSelfTarget(ac, player, selectedInfo) || isTargetCountermeasureActive(ac, selectedInfo) || isSameTeamTarget(player, ac, selectedInfo)) {
                if (MCH_RadarDebug.isEnabled()) {
                    String reason = invalidTargetType
                        ? "INVALID_TARGET_TYPE"
                        : (isSelfTarget(ac, player, selectedInfo)
                        ? "SELF_TARGET"
                        : (isSameTeamTarget(player, ac, selectedInfo) ? "FRIENDLY_TARGET" : "COUNTERMEASURE_ACTIVE"));
                    MCH_RadarDebug.trace(ac.worldObj, ac, "select drop acId=%d target=%d reason=%s", aircraftId, trackState.selectedTargetId, reason);
                }
                if (trackState.trackingTargetId == trackState.selectedTargetId) {
                    setTrackingTarget(ac, aircraftId, trackState, -1);
                }
                trackState.selectedTargetId = -1;
            }
        }
        if (MCH_RadarDebug.isEnabled()) {
            MCH_RadarDebug.trace(ac.worldObj, ac,
                "scan slot=%d acId=%d name=%s cand=%d alt=%d range=%d azPass=%d elPass=%d prob=%d hit=%d cache=%d az=%.1f el=%.1f mode=%s/%s tick=%d hold=%d p=%.2f min=%.1f max=%.1f minAlt=%.1f search=%s trkAz=%.1f trkEl=%.1f sel=%d trk=%d drop=%d cd=%d cfgCd=%d",
                slot, aircraftId, ac.getEntityName(), candidate, passAlt, passRange, passAz, passElev, passProb, hit, cache.size(),
                azClamp, elClamp, elevationRef, elevationCoverage, scanTick, holdTick, pBase, minDistance, maxDistance, (double)minAltitude,
                searchType, trackAzClamp, trackElClamp, trackState.selectedTargetId, trackState.trackingTargetId, trackState.lastManualDroppedTargetId,
                trackState.manualDropCooldownTick, retargetCooldownCfg);
        }
    }

    private static void refreshRadarContactsAcm(int aircraftId, MCH_EntityAircraft ac, EntityPlayer player, long worldTick, float partialTicks, int scanTick,
                                                float acmFovDeg, float elevationDeg, String elevationRef, String elevationCoverage, boolean followTurretYaw,
                                                double minDistance, double maxDistance) {
        long slot = worldTick / Math.max(1, scanTick);
        Long lastSlot = radarLastAcmScanSlot.get(aircraftId);
        if (lastSlot != null && lastSlot.longValue() == slot) {
            return;
        }
        radarLastAcmScanSlot.put(aircraftId, slot);
        RadarTrackState trackState = getOrCreateRadarTrackState(aircraftId);
        Map<Integer, RadarContact> cache = radarContactCache.get(aircraftId);
        if (cache == null) {
            cache = new HashMap<Integer, RadarContact>();
            radarContactCache.put(aircraftId, cache);
        }
        String searchType = normalizeRadarSearchType(ac.getAcInfo() != null ? ac.getAcInfo().radarSearchType : "SRC");
        float minAltitude = ac.getAcInfo() != null ? ac.getAcInfo().radarMinScanAltitude : 10.0F;
        float maxAltitude = ac.getAcInfo() != null ? ac.getAcInfo().radarMaxScanAltitude : 25.0F;
        float scanAzLimit = Math.max(0.0F, Math.min(360.0F, ac.getAcInfo().radarScanAzimuthDeg));
        float scanElLimit = Math.max(0.0F, Math.min(180.0F, ac.getAcInfo().radarScanElevationDeg));
        float acmHalf = acmFovDeg * 0.5F;
        double acmCenterBearing = 0.0D;
        double acmCenterElevation = 0.0D;
        double[] center = computeAcmCenterAnglesFromMouse(ac, player, partialTicks, elevationRef, followTurretYaw);
        if (center != null) {
            acmCenterBearing = center[0];
            acmCenterElevation = center[1];
        }
        if (!followTurretYaw) {
            float trackAzLimit = Math.max(0.0F, Math.min(360.0F, ac.getAcInfo().radarTrackAzimuthDeg));
            float trackElLimit = Math.max(0.0F, Math.min(180.0F, ac.getAcInfo().radarTrackElevationDeg));
            boolean outOfAz = trackAzLimit < 359.9F && Math.abs(acmCenterBearing) > trackAzLimit * 0.5D;
            boolean outOfEl = trackElLimit < 179.9F && Math.abs(acmCenterElevation) > trackElLimit * 0.5D;
            if (outOfAz || outOfEl) {
                trackState.acmMode = false;
                if (MCH_RadarDebug.isEnabled()) {
                    MCH_RadarDebug.trace(ac.worldObj, ac,
                        "acm toggle acId=%d mode=OFF reason=AIM_OUT_OF_TRACK_LIMIT az=%.1f/%.1f el=%.1f/%.1f",
                        aircraftId, acmCenterBearing, (double)trackAzLimit, acmCenterElevation, (double)trackElLimit);
                }
                return;
            }
        }
        int holdTick = Math.max(2, ac.getAcInfo().radarContactHoldTick / 2);
        int bestId = -1;
        double bestScore = Double.MAX_VALUE;
        for (MCH_EntityInfo info : getServerLoadedEntityStatic()) {
            boolean trackable = isTrackableForSearchType(info, searchType);
            if (!trackable || !isValidEntity(info, player, minDistance, searchType)) {
                continue;
            }
            if (isSelfTarget(ac, player, info) || isOwnLaunchedMissile(ac, player, info) || isSameTeamTarget(player, ac, info) || isTargetCountermeasureActive(ac, info)) {
                continue;
            }
            RadarProjection proj = projectContactStatic(ac, player, info, partialTicks, elevationRef, followTurretYaw);
            if (proj == null || proj.distance <= 1.0E-4D || proj.distance > maxDistance) {
                continue;
            }
            if (scanAzLimit < 359.9F && Math.abs(proj.bearingDeg) > scanAzLimit * 0.5D) {
                continue;
            }
            if (!isElevationInCoverageStatic(proj.elevationDeg, scanElLimit, elevationCoverage)) {
                continue;
            }
            double targetAgl = computeAgl(ac.worldObj, info.posX, info.posY, info.posZ);
            boolean missileTarget = isMissileClassName(info.entityClassName);
            if (!missileTarget) {
                if (isMultiMode(searchType)) {
                    if (isGroundRadarTargetEntity(info)) {
                        if (targetAgl > maxAltitude) {
                            continue;
                        }
                    } else if (targetAgl < minAltitude) {
                        continue;
                    }
                } else if (isGmtiMode(searchType)) {
                    if (targetAgl > maxAltitude) {
                        continue;
                    }
                } else if (targetAgl < minAltitude) {
                    continue;
                }
            }
            if (shouldFilterByAirSpeedGate(searchType, info)) {
                continue;
            }
            double relBearing = wrapDeg180(proj.bearingDeg - acmCenterBearing);
            double relElevation = proj.elevationDeg - acmCenterElevation;
            if (Math.abs(relBearing) > acmHalf || Math.abs(relElevation) > acmHalf) {
                continue;
            }
            RcsProfile rcsProfile = computeTargetRcsProfile(ac, info, partialTicks);
            RadarContact c = cache.get(info.entityId);
            if (c == null) {
                c = new RadarContact();
                cache.put(info.entityId, c);
            }
            boolean halfGround = isMultiMode(searchType) && isGroundRadarTargetEntity(info);
            float holdFactor = halfGround ? (rcsProfile.holdTimeFactor * 0.5F) : rcsProfile.holdTimeFactor;
            c.ttl = computeContactHoldTick(holdTick, holdFactor);
            double score = Math.abs(relBearing) + Math.abs(relElevation) + proj.distance * 0.001D;
            if (score < bestScore) {
                bestScore = score;
                bestId = info.entityId;
            }
        }
        if (bestId > 0) {
            trackState.selectedTargetId = bestId;
            trackState.trackingTargetId = bestId;
            trackState.acmMode = false;
            trackState.lastTrackToggleTick = worldTick;
            if (MCH_RadarDebug.isEnabled()) {
                MCH_RadarDebug.trace(ac.worldObj, ac,
                    "acm auto-lock acId=%d target=%d fov=%.1f range=%.1f modeExit=AUTO_CAPTURE",
                    aircraftId, bestId, acmFovDeg, maxDistance);
            }
        }
    }

    private static double[] computeAcmCenterAnglesFromMouse(MCH_EntityAircraft ac, EntityPlayer player, float partialTicks, String elevationRef, boolean followTurretYaw) {
        if (ac == null || player == null) {
            return null;
        }
        Vec3 lookVec = player.getLook(partialTicks);
        if (lookVec == null) {
            lookVec = getDirection(player, partialTicks);
        }
        if (lookVec == null) {
            return null;
        }
        double fx = lookVec.xCoord;
        double fy = lookVec.yCoord;
        double fz = lookVec.zCoord;
        double fl = Math.sqrt(fx * fx + fy * fy + fz * fz);
        if (fl <= 1.0E-6D) {
            return null;
        }
        fx /= fl;
        fy /= fl;
        fz /= fl;
        double rayDist = 4096.0D;
        double pX = interpolate(player.posX, player.lastTickPosX, partialTicks);
        double pY = interpolate(player.posY, player.lastTickPosY, partialTicks);
        double pZ = interpolate(player.posZ, player.lastTickPosZ, partialTicks);
        double xPos = pX + fx * rayDist;
        double yPos = pY + fy * rayDist;
        double zPos = pZ + fz * rayDist;
        RadarProjection mouseProj = projectPointStatic(ac, player, xPos, yPos, zPos, partialTicks, elevationRef, followTurretYaw);
        if (mouseProj == null) {
            return null;
        }
        return new double[]{mouseProj.bearingDeg, mouseProj.elevationDeg};
    }

    private static double wrapDeg180(double deg) {
        double d = deg;
        while (d > 180.0D) {
            d -= 360.0D;
        }
        while (d < -180.0D) {
            d += 360.0D;
        }
        return d;
    }

    private static RadarProjection projectContact(MCH_EntityAircraft ac, EntityPlayer player, MCH_EntityInfo info, float partialTicks, String elevationRef, boolean followTurretYaw) {
        return projectContactStatic(ac, player, info, partialTicks, elevationRef, followTurretYaw);
    }

    private static RadarProjection projectContactStatic(MCH_EntityAircraft ac, EntityPlayer player, MCH_EntityInfo info, float partialTicks, String elevationRef, boolean followTurretYaw) {
        if (ac == null || player == null || info == null) {
            return null;
        }
        double xPos = interpolate(info.posX, info.lastTickPosX, partialTicks);
        double yPos = interpolate(info.posY, info.lastTickPosY, partialTicks);
        double zPos = interpolate(info.posZ, info.lastTickPosZ, partialTicks);
        double pX = interpolate(player.posX, player.lastTickPosX, partialTicks);
        double pY = interpolate(player.posY, player.lastTickPosY, partialTicks);
        double pZ = interpolate(player.posZ, player.lastTickPosZ, partialTicks);
        double dx = xPos - pX;
        double dy = yPos - pY;
        double dz = zPos - pZ;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (distance <= 1.0E-6D) {
            return null;
        }
        double[] angles = computeRadarAnglesStatic(ac, partialTicks, elevationRef, followTurretYaw, dx, dy, dz, distance);
        RadarProjection p = new RadarProjection();
        p.distance = distance;
        p.bearingDeg = angles[0];
        p.elevationDeg = angles[1];
        return p;
    }

    private static RadarProjection projectPointStatic(MCH_EntityAircraft ac, EntityPlayer player, double xPos, double yPos, double zPos, float partialTicks, String elevationRef, boolean followTurretYaw) {
        if (ac == null || player == null) {
            return null;
        }
        double pX = interpolate(player.posX, player.lastTickPosX, partialTicks);
        double pY = interpolate(player.posY, player.lastTickPosY, partialTicks);
        double pZ = interpolate(player.posZ, player.lastTickPosZ, partialTicks);
        double dx = xPos - pX;
        double dy = yPos - pY;
        double dz = zPos - pZ;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (distance <= 1.0E-6D) {
            return null;
        }
        double[] angles = computeRadarAnglesStatic(ac, partialTicks, elevationRef, followTurretYaw, dx, dy, dz, distance);
        RadarProjection p = new RadarProjection();
        p.distance = distance;
        p.bearingDeg = angles[0];
        p.elevationDeg = angles[1];
        return p;
    }

    private static double[] computeRadarAnglesStatic(MCH_EntityAircraft ac, float partialTicks, String elevationRef, boolean followTurretYaw, double dx, double dy, double dz, double distance) {
        double tx = dx / distance;
        double ty = dy / distance;
        double tz = dz / distance;
        double fx;
        double fy;
        double fz;
        if ("AIRCRAFT".equalsIgnoreCase(elevationRef)) {
            Vec3 fwd = getDirection(ac, partialTicks);
            double fl = Math.sqrt(fwd.xCoord * fwd.xCoord + fwd.yCoord * fwd.yCoord + fwd.zCoord * fwd.zCoord);
            if (fl < 1.0E-6D) {
                fx = 0.0D;
                fy = 0.0D;
                fz = 1.0D;
            } else {
                fx = fwd.xCoord / fl;
                fy = fwd.yCoord / fl;
                fz = fwd.zCoord / fl;
            }
        } else {
            double yawDeg = getRadarReferenceYawDeg(ac, partialTicks, followTurretYaw);
            double yaw = Math.toRadians(yawDeg);
            fx = -Math.sin(yaw);
            fy = 0.0D;
            fz = Math.cos(yaw);
        }
        // Right vector in MC coordinates should be forward x up = (-fz, 0, fx).
        // Previous sign mirrored left/right on radar projection.
        double rx = -fz;
        double ry = 0.0D;
        double rz = fx;
        double rLen = Math.sqrt(rx * rx + ry * ry + rz * rz);
        if (rLen < 1.0E-6D) {
            rx = 1.0D;
            ry = 0.0D;
            rz = 0.0D;
            rLen = 1.0D;
        }
        rx /= rLen;
        ry /= rLen;
        rz /= rLen;
        // With corrected right vector, use up = right x forward to keep elevation sign consistent.
        double ux = ry * fz - rz * fy;
        double uy = rz * fx - rx * fz;
        double uz = rx * fy - ry * fx;
        double fComp = tx * fx + ty * fy + tz * fz;
        double rComp = tx * rx + ty * ry + tz * rz;
        double uComp = tx * ux + ty * uy + tz * uz;
        double bearing = Math.toDegrees(Math.atan2(rComp, fComp));
        double elev = Math.toDegrees(Math.atan2(uComp, Math.sqrt(fComp * fComp + rComp * rComp)));
        return new double[]{bearing, elev};
    }

    private static boolean isElevationInCoverageStatic(double elevDeg, float elevationDeg, String coverage) {
        float e = Math.max(0.0F, Math.min(180.0F, elevationDeg));
        if (e <= 0.0F) {
            return false;
        }
        if ("FULL".equalsIgnoreCase(coverage)) {
            double half = e * 0.5D;
            return elevDeg >= -half && elevDeg <= half;
        }
        if ("DOWN_ONLY".equalsIgnoreCase(coverage)) {
            return elevDeg <= 0.0D && elevDeg >= -e;
        }
        return elevDeg >= 0.0D && elevDeg <= e;
    }

    private static float computeDetectProbabilityStatic(float base, double bearingDeg, double elevDeg, float azDeg, float elDeg, double distance, double maxDistance,
                                                        String coverage, float gainNear, float gainFar, float rcsFactor) {
        double angleFactor;
        if (azDeg >= 359.9F) {
            angleFactor = 1.0D;
        } else {
            double half = Math.max(1.0D, azDeg * 0.5D);
            angleFactor = 1.0D - Math.min(1.0D, Math.abs(bearingDeg) / half);
        }
        double elevFactor;
        if ("FULL".equalsIgnoreCase(coverage)) {
            double half = Math.max(1.0D, elDeg * 0.5D);
            elevFactor = 1.0D - Math.min(1.0D, Math.abs(elevDeg) / half);
        } else if ("DOWN_ONLY".equalsIgnoreCase(coverage)) {
            double max = Math.max(1.0D, elDeg);
            elevFactor = 1.0D - Math.min(1.0D, Math.max(0.0D, -elevDeg) / max);
        } else {
            double max = Math.max(1.0D, elDeg);
            elevFactor = 1.0D - Math.min(1.0D, Math.max(0.0D, elevDeg) / max);
        }
        double rangeRatio = Math.min(1.0D, Math.max(0.0D, distance / Math.max(1.0D, maxDistance)));
        double gain = gainNear + (gainFar - gainNear) * rangeRatio;
        double rcs = Math.max(0.01D, Math.min(10.0D, rcsFactor));
        double p = base * angleFactor * elevFactor * gain * rcs;
        return (float)Math.max(0.0D, Math.min(1.0D, p));
    }

    private static RcsProfile computeTargetRcsProfile(MCH_EntityAircraft ac, MCH_EntityInfo info, float partialTicks) {
        RcsProfile profile = new RcsProfile();
        if (ac == null || info == null) {
            return profile;
        }
        float front = 1.0F;
        float side = 1.0F;
        float rear = 1.0F;
        float holdTimeFactor = 1.0F;
        Entity target = ac.worldObj != null ? ac.worldObj.getEntityByID(info.entityId) : null;
        if (target instanceof MCH_EntityAircraft) {
            MCH_AircraftInfo ai = ((MCH_EntityAircraft)target).getAcInfo();
            if (ai != null) {
                front = ai.radarRcsFrontFactor;
                side = ai.radarRcsSideFactor;
                rear = ai.radarRcsRearFactor;
                holdTimeFactor = ai.radarRcsTimeFactor;
            }
        } else if (target instanceof MCH_EntityBaseBullet) {
            MCH_WeaponInfo weaponInfo = ((MCH_EntityBaseBullet)target).getInfo();
            if (weaponInfo != null) {
                front = weaponInfo.rcsFrontFactor;
                side = weaponInfo.rcsSideFactor;
                rear = weaponInfo.rcsRearFactor;
                holdTimeFactor = weaponInfo.rcsTimeFactor;
            }
        } else {
            String className = info.entityClassName != null ? info.entityClassName : "";
            if (className.contains("MCH_EntityHeli") || className.contains("MCP_EntityPlane")
                || className.contains("MCH_EntityTank") || className.contains("MCH_EntityVehicle")) {
                MCH_AircraftInfo ai = resolveAircraftInfoByName(info.entityName);
                if (ai != null) {
                    front = ai.radarRcsFrontFactor;
                    side = ai.radarRcsSideFactor;
                    rear = ai.radarRcsRearFactor;
                    holdTimeFactor = ai.radarRcsTimeFactor;
                }
            } else if (className.contains("MCH_EntityAAMissile") || className.contains("MCH_EntityASMissile")
                || className.contains("MCH_EntityATMissile") || className.contains("MCH_EntityTvMissile")) {
                MCH_WeaponInfo wi = MCH_WeaponInfoManager.get(info.entityName);
                if (wi != null) {
                    front = wi.rcsFrontFactor;
                    side = wi.rcsSideFactor;
                    rear = wi.rcsRearFactor;
                    holdTimeFactor = wi.rcsTimeFactor;
                }
            }
        }
        double targetX = interpolate(info.posX, info.lastTickPosX, partialTicks);
        double targetZ = interpolate(info.posZ, info.lastTickPosZ, partialTicks);
        double radarX = interpolate(ac.posX, ac.lastTickPosX, partialTicks);
        double radarZ = interpolate(ac.posZ, ac.lastTickPosZ, partialTicks);
        profile.detectFactor = computeAspectRcsFactor(targetX, targetZ, radarX, radarZ, info.rotationYaw, front, side, rear);
        profile.holdTimeFactor = Math.max(0.01F, Math.min(10.0F, holdTimeFactor));
        return profile;
    }

    private static int computeContactHoldTick(int baseHoldTick, float holdTimeFactor) {
        float factor = Math.max(0.01F, Math.min(10.0F, holdTimeFactor));
        return Math.max(1, Math.round(baseHoldTick * factor));
    }

    private static MCH_AircraftInfo resolveAircraftInfoByName(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        MCH_AircraftInfo info = MCH_HeliInfoManager.get(name);
        if (info != null) return info;
        info = MCP_PlaneInfoManager.get(name);
        if (info != null) return info;
        info = MCH_TankInfoManager.get(name);
        if (info != null) return info;
        return MCH_VehicleInfoManager.get(name);
    }

    private static float computeAspectRcsFactor(double targetX, double targetZ, double radarX, double radarZ, float targetYawDeg,
                                                float front, float side, float rear) {
        front = Math.max(0.01F, Math.min(10.0F, front));
        side = Math.max(0.01F, Math.min(10.0F, side));
        rear = Math.max(0.01F, Math.min(10.0F, rear));
        double toRadarX = radarX - targetX;
        double toRadarZ = radarZ - targetZ;
        double toRadarLen = Math.sqrt(toRadarX * toRadarX + toRadarZ * toRadarZ);
        if (toRadarLen <= 1.0E-6D) {
            return side;
        }
        double nx = toRadarX / toRadarLen;
        double nz = toRadarZ / toRadarLen;
        double yawRad = Math.toRadians(targetYawDeg);
        double fx = -Math.sin(yawRad);
        double fz = Math.cos(yawRad);
        double dot = Math.max(-1.0D, Math.min(1.0D, fx * nx + fz * nz));
        double aspect = Math.toDegrees(Math.acos(dot));
        float rcs;
        if (aspect <= 90.0D) {
            float t = (float)(aspect / 90.0D);
            rcs = front + (side - front) * t;
        } else {
            float t = (float)((aspect - 90.0D) / 90.0D);
            rcs = side + (rear - side) * t;
        }
        return Math.max(0.01F, Math.min(10.0F, rcs));
    }

    private double computeRadarPanelCenterY(double baseCy, double radius, float scanAngleDeg, float elevationDeg, String reference, String coverage) {
        float az = Math.max(0.0F, Math.min(360.0F, scanAngleDeg));
        if (az <= 0.0F || az > 180.0F) {
            return baseCy;
        }
        boolean upOnly = "UP_ONLY".equalsIgnoreCase(coverage);
        boolean downOnly = "DOWN_ONLY".equalsIgnoreCase(coverage);
        if (!upOnly && !downOnly) {
            return baseCy;
        }
        double halfRad = Math.toRadians(az * 0.5D);
        double offset = radius * Math.cos(halfRad);
        float el = Math.max(0.0F, Math.min(180.0F, elevationDeg));
        double elevationScale = 0.5D + (180.0D - el) / 180.0D * 0.5D;
        double refScale = "AIRCRAFT".equalsIgnoreCase(reference) ? 0.85D : 1.0D;
        return downOnly ? (baseCy - offset * elevationScale * refScale) : (baseCy + offset * elevationScale * refScale);
    }

    private double computeRadarAxisDeg(MCH_EntityAircraft ac, float partialTicks, String reference) {
        double base = -90.0D;
        if (!"AIRCRAFT".equalsIgnoreCase(reference) || ac == null) {
            return base;
        }
        float pitch = ac.prevRotationPitch + (ac.rotationPitch - ac.prevRotationPitch) * partialTicks;
        double clampedPitch = Math.max(-45.0D, Math.min(45.0D, pitch));
        return base + clampedPitch * 0.35D;
    }

    private static boolean isRadarTrackableEntity(MCH_EntityInfo entity) {
        if (entity == null || entity.entityClassName == null) {
            return false;
        }
        return isAirRadarTargetEntity(entity)
            || isGroundRadarTargetEntity(entity);
    }

    private static boolean isGmtiTrackableEntity(MCH_EntityInfo entity) {
        if (entity == null || entity.entityClassName == null) {
            return false;
        }
        return isGroundRadarTargetEntity(entity) || isAirRadarTargetEntity(entity);
    }

    private static boolean isTrackableForSearchType(MCH_EntityInfo entity, String searchType) {
        if (entity == null || entity.entityClassName == null) {
            return false;
        }
        if (isMultiMode(searchType)) {
            return isAirRadarTargetEntity(entity) || isGroundRadarTargetEntity(entity);
        }
        if (isGmtiMode(searchType)) {
            return isGmtiTrackableEntity(entity);
        }
        return isRadarTrackableEntity(entity);
    }

    private static boolean isAirRadarTargetEntity(MCH_EntityInfo entity) {
        if (entity == null || entity.entityClassName == null) {
            return false;
        }
        if (isFacGroundTargetEntity(entity)) {
            return false;
        }
        String className = entity.entityClassName;
        return className.contains("MCH_EntityHeli")
            || className.contains("MCP_EntityPlane")
            || isMissileClassName(className);
    }

    private static boolean isMissileClassName(String className) {
        if (className == null) {
            return false;
        }
        return className.contains("MCH_EntityAAMissile")
            || className.contains("MCH_EntityASMissile")
            || className.contains("MCH_EntityATMissile")
            || className.contains("MCH_EntityTvMissile");
    }

    private static boolean isGroundRadarTargetEntity(MCH_EntityInfo entity) {
        if (entity == null || entity.entityClassName == null) {
            return false;
        }
        String className = entity.entityClassName;
        return className.contains("MCH_EntityTank")
            || className.contains("MCH_EntityVehicle")
            || isFacGroundTargetEntity(entity);
    }

    private static boolean isFacGroundTargetEntity(MCH_EntityInfo entity) {
        if (entity == null || entity.entityClassName == null) {
            return false;
        }
        String className = entity.entityClassName;
        if (className.toUpperCase(Locale.ROOT).contains("FAC")) {
            return true;
        }
        if (isFacAircraftProfile(entity)) {
            return true;
        }
        return className.contains("EntitySoldier")
            || className.contains("MCH_EntityGunner");
    }

    private static boolean isFacAircraftProfile(MCH_EntityInfo entity) {
        if (entity == null || entity.entityName == null) {
            return false;
        }
        String name = entity.entityName.toLowerCase(Locale.ROOT);
        if (name.contains("fac")) {
            return true;
        }
        mcheli.plane.MCP_PlaneInfo pi = MCP_PlaneInfoManager.get(entity.entityName);
        if (pi != null) {
            String category = pi.category != null ? pi.category.toLowerCase(Locale.ROOT) : "";
            if (category.contains("fac")) {
                return true;
            }
            if (pi.name != null && pi.name.toLowerCase(Locale.ROOT).contains("fac")) {
                return true;
            }
        }
        mcheli.vehicle.MCH_VehicleInfo vi = MCH_VehicleInfoManager.get(entity.entityName);
        if (vi != null) {
            String category = vi.category != null ? vi.category.toLowerCase(Locale.ROOT) : "";
            if (category.contains("fac")) {
                return true;
            }
            if (vi.name != null && vi.name.toLowerCase(Locale.ROOT).contains("fac")) {
                return true;
            }
        }
        return false;
    }

    private static boolean shouldFilterByAirSpeedGate(String searchType, MCH_EntityInfo info) {
        if (isGmtiMode(searchType)) {
            return false;
        }
        if (!isAirRadarTargetEntity(info)) {
            return false;
        }
        if (info != null && isMissileClassName(info.entityClassName)) {
            return false;
        }
        double vx = info.posX - info.lastTickPosX;
        double vy = info.posY - info.lastTickPosY;
        double vz = info.posZ - info.lastTickPosZ;
        double speedSq = vx * vx + vy * vy + vz * vz;
        return speedSq < AIR_SPEED_GATE_SQ;
    }

    private static double computeAgl(World world, double x, double y, double z) {
        if (world == null) {
            return y;
        }
        int bx = MathHelper.floor_double(x);
        int bz = MathHelper.floor_double(z);
        int groundY = world.getHeightValue(bx, bz);
        return y - (double)groundY;
    }

    private static String getTrackingInvalidReason(MCH_EntityAircraft ac, EntityPlayer player, RadarProjection proj, MCH_EntityInfo info, float trackAzClamp, float trackElClamp,
                                                   String coverage, float minAltitude, float maxAltitude, double maxDistance, String searchType) {
        if (info == null) {
            return "TARGET_LOST";
        }
        if (!isTrackableForSearchType(info, searchType)) {
            return "INVALID_TARGET_TYPE";
        }
        if (isSelfTarget(ac, player, info)) {
            return "SELF_TARGET";
        }
        if (System.currentTimeMillis() - info.lastUpdateTime > 4000L) {
            return "TARGET_STALE";
        }
        if (isSameTeamTarget(player, ac, info)) {
            return "FRIENDLY_TARGET";
        }
        if (isTargetCountermeasureActive(ac, info)) {
            return "COUNTERMEASURE_ACTIVE";
        }
        if (proj == null || info == null) {
            return "NO_PROJECTION";
        }
        boolean missileTarget = isMissileClassName(info.entityClassName);
        double targetAgl = computeAgl(ac != null ? ac.worldObj : null, info.posX, info.posY, info.posZ);
        if (!missileTarget) {
            if (isMultiMode(searchType)) {
                if (isGroundRadarTargetEntity(info)) {
                    if (targetAgl > maxAltitude) {
                        return "ABOVE_MAX_ALTITUDE";
                    }
                } else if (targetAgl < minAltitude) {
                    return "BELOW_MIN_ALTITUDE";
                }
            } else if (isGmtiMode(searchType)) {
                if (targetAgl > maxAltitude) {
                    return "ABOVE_MAX_ALTITUDE";
                }
            } else if (targetAgl < minAltitude) {
                return "BELOW_MIN_ALTITUDE";
            }
        }
        if (shouldFilterByAirSpeedGate(searchType, info)) {
            return "BELOW_AIR_SPEED_GATE";
        }
        if (proj.distance <= 1.0E-4D || proj.distance > maxDistance) {
            return "OUT_OF_RANGE";
        }
        if (trackAzClamp < 359.9F && Math.abs(proj.bearingDeg) > trackAzClamp * 0.5D) {
            return "AZIMUTH_EXCEEDED";
        }
        if (!isElevationInCoverageStatic(proj.elevationDeg, trackElClamp, coverage)) {
            return "ELEVATION_EXCEEDED";
        }
        return null;
    }

    private static boolean isTargetCountermeasureActive(MCH_EntityAircraft ac, MCH_EntityInfo info) {
        if (ac == null || ac.worldObj == null || info == null) {
            return false;
        }
        Entity e = ac.worldObj.getEntityByID(info.entityId);
        if (e instanceof MCH_EntityAircraft) {
            MCH_EntityAircraft tgt = (MCH_EntityAircraft)e;
            return tgt.isChaffUsing()
                || tgt.isECMJammerUsing()
                || tgt.jammingTick > 0
                ;
        }
        long nowTick = ac.worldObj.getTotalWorldTime();
        return info.isCountermeasureActive(nowTick);
    }

    private static boolean isOwnLaunchedMissile(MCH_EntityAircraft ac, EntityPlayer player, MCH_EntityInfo info) {
        if (ac == null || ac.worldObj == null || info == null) {
            return false;
        }
        Entity e = ac.worldObj.getEntityByID(info.entityId);
        if (!(e instanceof MCH_EntityBaseBullet)) {
            return false;
        }
        return isOwnLaunchedMissile(ac, player, (MCH_EntityBaseBullet)e);
    }

    private static boolean isOwnLaunchedMissile(MCH_EntityAircraft ac, EntityPlayer player, MCH_EntityBaseBullet missile) {
        if (ac == null || missile == null) {
            return false;
        }
        if (missile.shootingAircraft != null && missile.shootingAircraft.getEntityId() == ac.getEntityId()) {
            return true;
        }
        if (missile.shootingEntity != null) {
            if (missile.shootingEntity.getEntityId() == ac.getEntityId()) {
                return true;
            }
            if (player != null && missile.shootingEntity.getEntityId() == player.getEntityId()) {
                return true;
            }
        }
        return false;
    }

    private static boolean isSelfTarget(MCH_EntityAircraft ac, EntityPlayer player, MCH_EntityInfo info) {
        if (ac == null || info == null) {
            return false;
        }
        if (info.entityId == ac.getEntityId()) {
            return true;
        }
        if (player != null && info.entityId == player.getEntityId()) {
            return true;
        }
        if (ac.worldObj == null) {
            return false;
        }
        Entity e = ac.worldObj.getEntityByID(info.entityId);
        if (e == null) {
            return false;
        }
        if (e == ac || (player != null && e == player)) {
            return true;
        }
        if (e instanceof MCH_EntitySeat) {
            MCH_EntityAircraft parent = ((MCH_EntitySeat)e).getParent();
            if (parent != null && parent.getEntityId() == ac.getEntityId()) {
                return true;
            }
        }
        Entity riding = e.ridingEntity;
        if (riding != null) {
            if (riding.getEntityId() == ac.getEntityId()) {
                return true;
            }
            if (riding instanceof MCH_EntitySeat) {
                MCH_EntityAircraft parent = ((MCH_EntitySeat)riding).getParent();
                if (parent != null && parent.getEntityId() == ac.getEntityId()) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isSameTeamTarget(EntityPlayer player, MCH_EntityAircraft ac, MCH_EntityInfo info) {
        if (player == null || ac == null || ac.worldObj == null || info == null) {
            return false;
        }
        Entity e = ac.worldObj.getEntityByID(info.entityId);
        if (e == null) {
            return false;
        }
        if (e instanceof MCH_EntityAircraft) {
            return ((MCH_EntityAircraft)e).isMountedSameTeamEntity(player);
        }
        if (e instanceof EntityLivingBase && player.getTeam() != null && ((EntityLivingBase)e).getTeam() != null) {
            return player.isOnSameTeam((EntityLivingBase)e);
        }
        return false;
    }

    private static double getRadarReferenceYawDeg(MCH_EntityAircraft ac, float partialTicks, boolean followTurretYaw) {
        if (ac == null) {
            return 0.0D;
        }
        if (followTurretYaw && ac instanceof MCH_EntityTank) {
            float turretPrev = ac.prevLastRiderYaw;
            float turretNow = ac.getLastRiderYaw();
            return turretPrev + (turretNow - turretPrev) * partialTicks;
        }
        return ac.prevRotationYaw + (ac.rotationYaw - ac.prevRotationYaw) * partialTicks;
    }

    private static String normalizeRadarSearchType(String value) {
        if (value == null) {
            return "SRC";
        }
        String mode = value.trim().toUpperCase(Locale.ROOT);
        if (mode.equals("TWS") || mode.equals("GMTI_SRC") || mode.equals("GMTI_TWS")
            || mode.equals("MULTI_SRC") || mode.equals("MULTI_TWS")) {
            return mode;
        }
        return "SRC";
    }

    public static String getRadarSearchType(MCH_EntityAircraft ac) {
        if (ac == null || ac.getAcInfo() == null) {
            return "SRC";
        }
        return normalizeRadarSearchType(ac.getAcInfo().radarSearchType);
    }

    private static boolean isGmtiMode(String searchType) {
        return "GMTI_SRC".equals(searchType) || "GMTI_TWS".equals(searchType);
    }

    private static boolean isMultiMode(String searchType) {
        return "MULTI_SRC".equals(searchType) || "MULTI_TWS".equals(searchType);
    }

    private static boolean isSrcLikeMode(String searchType) {
        return "SRC".equals(searchType) || "GMTI_SRC".equals(searchType) || "MULTI_SRC".equals(searchType);
    }

    private static boolean isTwsLikeMode(String searchType) {
        return "TWS".equals(searchType) || "GMTI_TWS".equals(searchType) || "MULTI_TWS".equals(searchType);
    }

    private static boolean isAircraftMannedForRadarEmission(MCH_EntityAircraft ac) {
        return ac != null && ac.isRadarEnabledRuntime() && (ac.getRiddenByEntity() != null || ac.getEntityBySeatId(1) != null);
    }

    private static void setTrackingTarget(MCH_EntityAircraft ac, int aircraftId, RadarTrackState state, int newTargetId) {
        if (state == null) {
            return;
        }
        if (ac != null && newTargetId > 0 && !ac.isRadarEnabledRuntime()) {
            return;
        }
        int oldTargetId = state.trackingTargetId;
        state.trackingTargetId = newTargetId;
        if (oldTargetId != newTargetId && ac != null && ac.worldObj != null && ac.worldObj.isRemote) {
            MCH_MOD.getPacketHandler().sendToServer(new PacketRadarLockState(aircraftId, newTargetId));
        }
    }

    private static void sendRadarLockHeartbeat(MCH_EntityAircraft ac, int aircraftId, RadarTrackState state, long nowTick) {
        if (ac == null || state == null || ac.worldObj == null || !ac.worldObj.isRemote) {
            return;
        }
        if (!ac.isRadarEnabledRuntime()) {
            setTrackingTarget(ac, aircraftId, state, -1);
            radarLockHeartbeatLastSendTick.remove(aircraftId);
            return;
        }
        if (state.trackingTargetId <= 0) {
            radarLockHeartbeatLastSendTick.remove(aircraftId);
            return;
        }
        Long last = radarLockHeartbeatLastSendTick.get(aircraftId);
        if (last != null && nowTick - last.longValue() < RWR_LOCK_HEARTBEAT_INTERVAL) {
            return;
        }
        MCH_MOD.getPacketHandler().sendToServer(new PacketRadarLockState(aircraftId, state.trackingTargetId));
        radarLockHeartbeatLastSendTick.put(aircraftId, nowTick);
    }

    public static void handleRadarPowerStateChanged(MCH_EntityAircraft ac, boolean enabled) {
        if (ac == null) {
            return;
        }
        int aircraftId = ac.getEntityId();
        RadarTrackState state = getOrCreateRadarTrackState(aircraftId);
        if (!enabled) {
            state.acmMode = false;
            state.selectedTargetId = -1;
            setTrackingTarget(ac, aircraftId, state, -1);
            radarLockHeartbeatLastSendTick.remove(aircraftId);
            armTrackStateMap.remove(aircraftId);
            armBvrContactCache.remove(aircraftId);
        }
    }

    private static RadarTrackState getOrCreateRadarTrackState(int aircraftId) {
        RadarTrackState state = radarTrackStateMap.get(aircraftId);
        if (state == null) {
            state = new RadarTrackState();
            state.lastUpdateTick = -1L;
            radarTrackStateMap.put(aircraftId, state);
        }
        return state;
    }

    public static int handleRadarSelectKey(boolean keyDown, EntityPlayer player, MCH_EntityAircraft ac) {
        if (!keyDown) {
            radarSelectKeyPrevDown = false;
            return 0;
        }
        if (radarSelectKeyPrevDown) {
            return 0;
        }
        radarSelectKeyPrevDown = true;
        if (player == null || ac == null || ac.getAcInfo() == null || !ac.getAcInfo().enableRadar || !ac.isRadarEnabledRuntime()) {
            return 2;
        }
        int aircraftId = ac.getEntityId();
        long t = ac.worldObj.getTotalWorldTime();
        RadarTrackState state = getOrCreateRadarTrackState(aircraftId);
        tickRadarTrackState(state, t);
        if (state.trackingTargetId > 0) {
            if (MCH_RadarDebug.isEnabled()) {
                MCH_RadarDebug.trace(ac.worldObj, ac, "select acId=%d target=%d reason=MANUAL_UNLOCK_BY_CAP", aircraftId, state.trackingTargetId);
            }
            setTrackingTarget(ac, aircraftId, state, -1);
            state.lastTrackToggleTick = t;
            return -1;
        }
        Map<Integer, RadarContact> cache = radarContactCache.get(aircraftId);
        if (cache == null || cache.isEmpty()) {
            state.selectedTargetId = -1;
            return 2;
        }
        List<ContactCandidate> candidates = getSelectableCandidates(ac, player, cache, state);
        if (candidates.isEmpty()) {
            state.selectedTargetId = -1;
            return 2;
        }
        int nextId = candidates.get(0).entityId;
        if (state.selectedTargetId > 0) {
            int currentIdx = -1;
            for (int i = 0; i < candidates.size(); i++) {
                if (candidates.get(i).entityId == state.selectedTargetId) {
                    currentIdx = i;
                    break;
                }
            }
            if (currentIdx >= 0) {
                nextId = candidates.get((currentIdx + 1) % candidates.size()).entityId;
            }
        }
        if (state.selectedTargetId > 0 && state.selectedTargetId != nextId) {
            state.lastManualDroppedTargetId = state.selectedTargetId;
            state.manualDropCooldownTick = Math.max(0, ac.getAcInfo().radarRetargetCooldownTick);
        }
        state.selectedTargetId = nextId;
        if (MCH_RadarDebug.isEnabled()) {
            MCH_RadarDebug.trace(ac.worldObj, ac,
                "select acId=%d selected=%d list=%d drop=%d cd=%d",
                aircraftId, state.selectedTargetId, candidates.size(), state.lastManualDroppedTargetId, state.manualDropCooldownTick);
        }
        return 1;
    }

    public static int handleRadarTrackToggleKey(boolean keyPress, EntityPlayer player, MCH_EntityAircraft ac) {
        if (!keyPress) {
            return 0;
        }
        if (player == null || ac == null || ac.getAcInfo() == null || !ac.getAcInfo().enableRadar || !ac.isRadarEnabledRuntime()) {
            return 2;
        }
        int aircraftId = ac.getEntityId();
        RadarTrackState state = getOrCreateRadarTrackState(aircraftId);
        long nowTick = ac.worldObj != null ? ac.worldObj.getTotalWorldTime() : 0L;
        if (state.lastTrackToggleTick >= 0L && nowTick - state.lastTrackToggleTick < 10L) {
            if (MCH_RadarDebug.isEnabled()) {
                MCH_RadarDebug.trace(ac.worldObj, ac, "track toggle acId=%d reason=TOGGLE_COOLDOWN remain=%d", aircraftId, 10L - (nowTick - state.lastTrackToggleTick));
            }
            return 0;
        }
        if (state.trackingTargetId > 0) {
            if (MCH_RadarDebug.isEnabled()) {
                MCH_RadarDebug.trace(ac.worldObj, ac, "track toggle acId=%d target=%d reason=MANUAL_CANCEL", aircraftId, state.trackingTargetId);
            }
            setTrackingTarget(ac, aircraftId, state, -1);
            state.lastTrackToggleTick = nowTick;
            return -1;
        }
        if (state.selectedTargetId <= 0) {
            if (MCH_RadarDebug.isEnabled()) {
                MCH_RadarDebug.trace(ac.worldObj, ac, "track toggle acId=%d reason=NO_SELECTED_TARGET", aircraftId);
            }
            // No selected target: silently ignore right-click to avoid noisy NG sound.
            return 0;
        }
        MCH_EntityInfo target = MCH_EntityInfoClientTracker.getEntityInfo(state.selectedTargetId);
        if (target == null) {
            state.selectedTargetId = -1;
            if (MCH_RadarDebug.isEnabled()) {
                MCH_RadarDebug.trace(ac.worldObj, ac, "track toggle acId=%d reason=SELECTED_TARGET_LOST", aircraftId);
            }
            return 2;
        }
        if (isOwnLaunchedMissile(ac, player, target)) {
            if (MCH_RadarDebug.isEnabled()) {
                MCH_RadarDebug.trace(ac.worldObj, ac, "track toggle acId=%d target=%d reason=OWN_MISSILE", aircraftId, state.selectedTargetId);
            }
            state.selectedTargetId = -1;
            return 2;
        }
        if (isSameTeamTarget(player, ac, target)) {
            if (MCH_RadarDebug.isEnabled()) {
                MCH_RadarDebug.trace(ac.worldObj, ac, "track toggle acId=%d target=%d reason=FRIENDLY_TARGET", aircraftId, state.selectedTargetId);
            }
            state.selectedTargetId = -1;
            return 2;
        }
        double minDistance = 15.0D;
        double maxDistance = 4096.0D;
        if (ac.getAcInfo().radarMaxTargetRange > 0.0F) {
            maxDistance = Math.min(maxDistance, ac.getAcInfo().radarMaxTargetRange);
        }
        if (maxDistance <= minDistance) {
            minDistance = Math.max(0.0D, maxDistance - 50.0D);
        }
        String coverage = ac.getAcInfo().radarElevationCoverage;
        String searchType = normalizeRadarSearchType(ac.getAcInfo().radarSearchType);
        float scanAz = Math.max(0.0F, Math.min(360.0F, ac.getAcInfo().radarScanAzimuthDeg));
        float scanEl = Math.max(0.0F, Math.min(180.0F, ac.getAcInfo().radarScanElevationDeg));
        float trackAz = Math.max(0.0F, Math.min(scanAz, ac.getAcInfo().radarTrackAzimuthDeg));
        float trackEl = Math.max(0.0F, Math.min(scanEl, ac.getAcInfo().radarTrackElevationDeg));
        float maxAltitude = ac.getAcInfo().radarMaxScanAltitude;
        boolean followTurretYaw = (ac instanceof MCH_EntityTank) && ac.getAcInfo().radarFollowTurretYaw;
        RadarProjection proj = projectContactStatic(ac, player, target, 0.0F, ac.getAcInfo().radarElevationReference, followTurretYaw);
        String invalidReason = getTrackingInvalidReason(ac, player, proj, target, trackAz, trackEl, coverage, ac.getAcInfo().radarMinScanAltitude, maxAltitude, maxDistance, searchType);
        if (invalidReason != null) {
            if (MCH_RadarDebug.isEnabled()) {
                MCH_RadarDebug.trace(ac.worldObj, ac, "track toggle acId=%d target=%d reason=%s", aircraftId, state.selectedTargetId, invalidReason);
            }
            return 2;
        }
        setTrackingTarget(ac, aircraftId, state, state.selectedTargetId);
        state.lastTrackToggleTick = nowTick;
        if (MCH_RadarDebug.isEnabled()) {
            MCH_RadarDebug.trace(ac.worldObj, ac, "track toggle acId=%d target=%d reason=LOCK_ON", aircraftId, state.trackingTargetId);
        }
        return 1;
    }

    public static int handleRadarAcmToggleKey(boolean keyDown, EntityPlayer player, MCH_EntityAircraft ac) {
        if (!keyDown) {
            radarAcmKeyPrevDown = false;
            return 0;
        }
        if (radarAcmKeyPrevDown) {
            return 0;
        }
        radarAcmKeyPrevDown = true;
        if (player == null || ac == null || ac.getAcInfo() == null || !ac.getAcInfo().enableRadar || !ac.isRadarEnabledRuntime()) {
            return 2;
        }
        RadarTrackState state = getOrCreateRadarTrackState(ac.getEntityId());
        if (state.acmMode) {
            if (MCH_RadarDebug.isEnabled()) {
                String reason = state.trackingTargetId > 0 ? "MANUAL_EXIT_WITH_TRACK" : "MANUAL_EXIT_NO_CAPTURE";
                MCH_RadarDebug.trace(ac.worldObj, ac, "acm toggle acId=%d mode=OFF reason=%s sel=%d trk=%d",
                    ac.getEntityId(), reason, state.selectedTargetId, state.trackingTargetId);
            }
            state.acmMode = false;
            return -1;
        }
        state.acmMode = true;
        state.selectedTargetId = -1;
        setTrackingTarget(ac, ac.getEntityId(), state, -1);
        if (MCH_RadarDebug.isEnabled()) {
            float cfgRange = ac.getAcInfo().radarMaxTargetRange;
            float acmRange = cfgRange > 0.0F ? cfgRange * 0.5F : 0.0F;
            MCH_RadarDebug.trace(ac.worldObj, ac, "acm toggle acId=%d mode=ON fov=%.1f range=%.1f",
                ac.getEntityId(), 5.0D, (double)acmRange);
        }
        return 1;
    }

    public static boolean isArmNarrowBandCurrentWeapon(MCH_EntityAircraft ac, EntityPlayer player) {
        if (ac == null || player == null) {
            return false;
        }
        if (ac.getAcInfo() == null || !ac.getAcInfo().enableRadar || !ac.isRadarEnabledRuntime()) {
            return false;
        }
        MCH_WeaponSet ws = ac.getCurrentWeapon(player);
        if (ws == null || ws.getCurrentWeapon() == null || ws.getCurrentWeapon().getInfo() == null) {
            return false;
        }
        MCH_WeaponInfo info = ws.getCurrentWeapon().getInfo();
        return info.antiRadiationMissile && ws.getCurrentWeapon().getCurrentMode() == 1;
    }

    public static boolean isArmCurrentWeapon(MCH_EntityAircraft ac, EntityPlayer player) {
        if (ac == null || player == null) {
            return false;
        }
        MCH_WeaponSet ws = ac.getCurrentWeapon(player);
        if (ws == null || ws.getCurrentWeapon() == null || ws.getCurrentWeapon().getInfo() == null) {
            return false;
        }
        return ws.getCurrentWeapon().getInfo().antiRadiationMissile;
    }

    public static void clearRadarTrackForArmMode(MCH_EntityAircraft ac) {
        if (ac == null) {
            return;
        }
        int aircraftId = ac.getEntityId();
        RadarTrackState state = getOrCreateRadarTrackState(aircraftId);
        if (state.selectedTargetId <= 0 && state.trackingTargetId <= 0) {
            return;
        }
        state.selectedTargetId = -1;
        setTrackingTarget(ac, aircraftId, state, -1);
        radarLockHeartbeatLastSendTick.remove(aircraftId);
    }

    private static ArmTrackState getOrCreateArmTrackState(int aircraftId) {
        ArmTrackState state = armTrackStateMap.get(aircraftId);
        if (state == null) {
            state = new ArmTrackState();
            armTrackStateMap.put(aircraftId, state);
        }
        return state;
    }

    private static void refreshArmBvrContacts(MCH_EntityAircraft ac, EntityPlayer player, long nowTick) {
        if (ac == null || ac.getAcInfo() == null) {
            return;
        }
        int aircraftId = ac.getEntityId();
        int baseTtl = Math.max(15, ac.getAcInfo().radarContactHoldTick / 2);
        Map<Integer, ArmBvrContact> cache = armBvrContactCache.get(aircraftId);
        if (cache == null) {
            cache = new HashMap<Integer, ArmBvrContact>();
            armBvrContactCache.put(aircraftId, cache);
        }
        Iterator<Map.Entry<Integer, ArmBvrContact>> it = cache.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, ArmBvrContact> e = it.next();
            ArmBvrContact c = e.getValue();
            if (c == null || c.untilTick < nowTick) {
                it.remove();
            }
        }
        List<MCH_RWRThreatEvent> events = MCH_RWRThreatClientTracker.getEvents(aircraftId);
        for (MCH_RWRThreatEvent evt : events) {
            if (evt == null || evt.emitterId <= 0 || evt.emitterId == aircraftId) {
                continue;
            }
            if (evt.emitterKind == MCH_RWRThreatEvent.EMITTER_MISSILE) {
                continue;
            }
            MCH_EntityInfo emitterInfo = MCH_EntityInfoClientTracker.getEntityInfo(evt.emitterId);
            if (isSelfTarget(ac, player, emitterInfo) || isOwnLaunchedMissile(ac, player, emitterInfo) || isSameTeamTarget(player, ac, emitterInfo)) {
                continue;
            }
            String sourceName = normalizeRwrSourceName(evt.sourceName);
            if (sourceName.isEmpty() || "?".equals(sourceName)) {
                if (emitterInfo != null && emitterInfo.entityName != null && !emitterInfo.entityName.isEmpty()) {
                    sourceName = emitterInfo.entityName;
                } else {
                    sourceName = "UNKNOWN";
                }
            }
            ArmBvrContact contact = cache.get(evt.emitterId);
            boolean stt = evt.threatMode == MCH_RWRThreatEvent.MODE_STT;
            if (contact != null && !stt && nowTick - contact.lastRefreshTick < ARM_BVR_REFRESH_MIN_TICK) {
                continue;
            }
            if (contact == null) {
                contact = new ArmBvrContact();
                contact.emitterId = evt.emitterId;
                cache.put(evt.emitterId, contact);
            }
            contact.sourceName = sourceName;
            contact.threatMode = evt.threatMode;
            contact.untilTick = nowTick + baseTtl;
            contact.lastRefreshTick = nowTick;
            contact.color = stt ? ARM_BVR_STT_COLOR : ARM_BVR_BASE_COLOR;
        }
        ArmTrackState state = getOrCreateArmTrackState(aircraftId);
        if (state.trackingTargetId > 0) {
            if (cache.containsKey(state.trackingTargetId)) {
                state.trackingLostSinceTick = -1L;
            } else if (nowTick - state.lastTrackAcquireTick < ARM_TRACK_LOCK_GUARD_TICK) {
                state.trackingLostSinceTick = -1L;
            } else {
                if (state.trackingLostSinceTick < 0L) {
                    state.trackingLostSinceTick = nowTick;
                }
                if (nowTick - state.trackingLostSinceTick >= ARM_TRACK_LOST_GRACE_TICK) {
                    state.trackingTargetId = -1;
                    state.trackingLostSinceTick = -1L;
                }
            }
        }
        if (state.selectedTargetId > 0 && !cache.containsKey(state.selectedTargetId)) {
            state.selectedTargetId = -1;
        }
    }

    public static List<ArmBvrDisplayContact> getArmBvrDisplayContacts(MCH_EntityAircraft ac, EntityPlayer player) {
        List<ArmBvrDisplayContact> result = new ArrayList<ArmBvrDisplayContact>();
        if (ac == null || ac.worldObj == null) {
            return result;
        }
        long nowTick = ac.worldObj.getTotalWorldTime();
        refreshArmBvrContacts(ac, player, nowTick);
        int aircraftId = ac.getEntityId();
        Map<Integer, ArmBvrContact> cache = armBvrContactCache.get(aircraftId);
        ArmTrackState state = getOrCreateArmTrackState(aircraftId);
        if (cache == null || cache.isEmpty()) {
            return result;
        }
        for (Map.Entry<Integer, ArmBvrContact> e : cache.entrySet()) {
            ArmBvrContact c = e.getValue();
            if (c == null) {
                continue;
            }
            MCH_EntityInfo emitterInfo = MCH_EntityInfoClientTracker.getEntityInfo(c.emitterId);
            if (isSelfTarget(ac, player, emitterInfo) || isOwnLaunchedMissile(ac, player, emitterInfo) || isSameTeamTarget(player, ac, emitterInfo)) {
                continue;
            }
            if (!isArmContactAllowedForCurrentWeapon(ac, player, c.emitterId)) {
                continue;
            }
            double distance = emitterInfo != null ? Math.sqrt(emitterInfo.getDistanceSqToEntity(ac)) : 99999.0D;
            ArmBvrDisplayContact dc = new ArmBvrDisplayContact();
            dc.emitterId = c.emitterId;
            dc.name = c.sourceName;
            dc.threatMode = c.threatMode;
            dc.color = c.color;
            dc.distanceMeters = distance;
            dc.selected = state.selectedTargetId == c.emitterId;
            dc.tracking = state.trackingTargetId == c.emitterId;
            result.add(dc);
        }
        Collections.sort(result, new Comparator<ArmBvrDisplayContact>() {
            @Override
            public int compare(ArmBvrDisplayContact a, ArmBvrDisplayContact b) {
                int pa = a.tracking ? 0 : (a.selected ? 1 : 2);
                int pb = b.tracking ? 0 : (b.selected ? 1 : 2);
                if (pa != pb) {
                    return pa - pb;
                }
                int ta = getArmThreatPriority(a.threatMode);
                int tb = getArmThreatPriority(b.threatMode);
                if (ta != tb) {
                    return tb - ta;
                }
                return Double.compare(a.distanceMeters, b.distanceMeters);
            }
        });
        return result;
    }

    private static boolean isArmContactAllowedForCurrentWeapon(MCH_EntityAircraft ac, EntityPlayer player, int emitterId) {
        if (ac == null || player == null || ac.worldObj == null) {
            return false;
        }
        MCH_WeaponSet ws = ac.getCurrentWeapon(player);
        MCH_WeaponInfo wi = ws != null ? ws.getInfo() : null;
        if (wi == null || !wi.antiRadiationMissile) {
            return false;
        }
        MCH_EntityInfo info = MCH_EntityInfoClientTracker.getEntityInfo(emitterId);
        if (info == null || info.entityClassName == null) {
            return false;
        }
        String type = wi.type != null ? wi.type.toLowerCase() : "";
        if ("aamissile".equals(type)) {
            return info.entityClassName.contains("MCP_EntityPlane") || info.entityClassName.contains("MCH_EntityHeli");
        }
        if ("atmissile".equals(type)) {
            return info.entityClassName.contains("MCH_EntityTank") || info.entityClassName.contains("MCH_EntityVehicle");
        }
        return true;
    }

    private static int getArmThreatPriority(byte threatMode) {
        if (threatMode == MCH_RWRThreatEvent.MODE_STT) {
            return 4;
        }
        if (threatMode == MCH_RWRThreatEvent.MODE_TRACK) {
            return 3;
        }
        if (threatMode == MCH_RWRThreatEvent.MODE_SEARCH) {
            return 2;
        }
        if (threatMode == MCH_RWRThreatEvent.MODE_MSL_ACTIVE || threatMode == MCH_RWRThreatEvent.MODE_MSL_DATALINK) {
            return 1;
        }
        return 0;
    }

    public static int handleArmSelectKey(boolean keyDown, EntityPlayer player, MCH_EntityAircraft ac) {
        if (!keyDown) {
            armSelectKeyPrevDown = false;
            return 0;
        }
        if (armSelectKeyPrevDown) {
            return 0;
        }
        armSelectKeyPrevDown = true;
        if (player == null || ac == null || !isArmNarrowBandCurrentWeapon(ac, player) || ac.worldObj == null) {
            return 2;
        }
        List<ArmBvrDisplayContact> candidates = getArmBvrDisplayContacts(ac, player);
        int aircraftId = ac.getEntityId();
        ArmTrackState state = getOrCreateArmTrackState(aircraftId);
        if (candidates.isEmpty()) {
            state.selectedTargetId = -1;
            return 2;
        }
        int nextId = candidates.get(0).emitterId;
        if (state.selectedTargetId > 0) {
            int cur = -1;
            for (int i = 0; i < candidates.size(); i++) {
                if (candidates.get(i).emitterId == state.selectedTargetId) {
                    cur = i;
                    break;
                }
            }
            if (cur >= 0) {
                nextId = candidates.get((cur + 1) % candidates.size()).emitterId;
            }
        }
        state.selectedTargetId = nextId;
        return 1;
    }

    public static int handleArmTrackToggleKey(boolean keyDown, EntityPlayer player, MCH_EntityAircraft ac) {
        if (!keyDown) {
            armTrackKeyPrevDown = false;
            return 0;
        }
        if (armTrackKeyPrevDown) {
            return 0;
        }
        armTrackKeyPrevDown = true;
        if (player == null || ac == null || !isArmNarrowBandCurrentWeapon(ac, player) || ac.worldObj == null) {
            return 2;
        }
        long nowTick = ac.worldObj.getTotalWorldTime();
        int aircraftId = ac.getEntityId();
        ArmTrackState state = getOrCreateArmTrackState(aircraftId);
        refreshArmBvrContacts(ac, player, nowTick);
        Map<Integer, ArmBvrContact> cache = armBvrContactCache.get(aircraftId);
        if (state.trackingTargetId > 0) {
            if (nowTick - state.lastTrackAcquireTick < ARM_TRACK_UNLOCK_INPUT_GUARD_TICK) {
                return 0;
            }
            state.trackingTargetId = -1;
            state.trackingLostSinceTick = -1L;
            return -1;
        }
        if (state.selectedTargetId <= 0 || cache == null || !cache.containsKey(state.selectedTargetId)) {
            return 2;
        }
        state.trackingTargetId = state.selectedTargetId;
        state.lastTrackAcquireTick = nowTick;
        state.trackingLostSinceTick = -1L;
        return 1;
    }

    public static int getArmTrackingTargetId(MCH_EntityAircraft ac) {
        if (ac == null) {
            return -1;
        }
        ArmTrackState state = armTrackStateMap.get(ac.getEntityId());
        return state != null ? state.trackingTargetId : -1;
    }

    public static int getArmSelectedTargetId(MCH_EntityAircraft ac) {
        if (ac == null) {
            return -1;
        }
        ArmTrackState state = armTrackStateMap.get(ac.getEntityId());
        return state != null ? state.selectedTargetId : -1;
    }

    public static int getRadarTrackingTargetId(MCH_EntityAircraft ac) {
        if (ac == null) {
            return -1;
        }
        RadarTrackState state = radarTrackStateMap.get(ac.getEntityId());
        return state != null ? state.trackingTargetId : -1;
    }

    public static int getRadarSelectedTargetId(MCH_EntityAircraft ac) {
        if (ac == null) {
            return -1;
        }
        RadarTrackState state = radarTrackStateMap.get(ac.getEntityId());
        return state != null ? state.selectedTargetId : -1;
    }

    public static boolean isRadarTargetSelectedOrTracking(MCH_EntityAircraft ac, int entityId) {
        if (ac == null || entityId <= 0) {
            return false;
        }
        RadarTrackState state = radarTrackStateMap.get(ac.getEntityId());
        return state != null && (state.selectedTargetId == entityId || state.trackingTargetId == entityId);
    }

    public static boolean isRadarTargetTracking(MCH_EntityAircraft ac, int entityId) {
        if (ac == null || entityId <= 0) {
            return false;
        }
        RadarTrackState state = radarTrackStateMap.get(ac.getEntityId());
        return state != null && state.trackingTargetId == entityId;
    }

    public static boolean isRadarTargetSelected(MCH_EntityAircraft ac, int entityId) {
        if (ac == null || entityId <= 0) {
            return false;
        }
        RadarTrackState state = radarTrackStateMap.get(ac.getEntityId());
        return state != null && state.selectedTargetId == entityId;
    }

    public static RadarDisplayFrame buildRadarDisplayFrame(MCH_EntityAircraft ac, EntityPlayer player, float partialTicks) {
        RadarDisplayFrame frame = new RadarDisplayFrame();
        frame.aircraft = ac;
        if (ac == null || ac.getAcInfo() == null || !ac.getAcInfo().enableRadar) {
            return frame;
        }
        int aircraftId = ac.getEntityId();
        RadarTrackState state = radarTrackStateMap.get(aircraftId);
        if (state != null) {
            frame.acmMode = state.acmMode;
        }
        int scanTick = Math.max(1, ac.getAcInfo().radarScanTick);
        int usedScanTick = frame.acmMode ? Math.max(1, scanTick / 4) : scanTick;
        long worldTick = ac.worldObj != null ? ac.worldObj.getTotalWorldTime() : 0L;
        double phase = ((double)(worldTick % usedScanTick) + partialTicks) / (double)usedScanTick;
        if (phase > 1.0D) {
            phase = phase - Math.floor(phase);
        }
        frame.scanPhase = phase;

        double minDistance = _MIN_DISTANCE;
        double maxDistance = _MAX_DISTANCE;
        if (ac instanceof MCH_EntityTank || (ac instanceof MCP_EntityPlane && ac.getAcInfo().isFloat)) {
            minDistance = 15.0D;
            maxDistance = 4096.0D;
        }
        if (ac.getAcInfo().radarMaxTargetRange > 0.0F) {
            maxDistance = Math.min(maxDistance, ac.getAcInfo().radarMaxTargetRange);
        }
        if (maxDistance <= minDistance) {
            minDistance = Math.max(0.0D, maxDistance - 50.0D);
        }
        String elevationRef = ac.getAcInfo().radarElevationReference;
        String elevationCoverage = ac.getAcInfo().radarElevationCoverage;
        String searchType = normalizeRadarSearchType(ac.getAcInfo().radarSearchType);
        boolean srcLikeMode = isSrcLikeMode(searchType);
        boolean twsLikeMode = isTwsLikeMode(searchType);
        boolean followTurretYaw = (ac instanceof MCH_EntityTank) && ac.getAcInfo().radarFollowTurretYaw;
        float scanAzClamp = Math.max(0.0F, Math.min(360.0F, ac.getAcInfo().radarScanAzimuthDeg));
        float scanElClamp = Math.max(0.0F, Math.min(180.0F, ac.getAcInfo().radarScanElevationDeg));
        float minAltitude = ac.getAcInfo().radarMinScanAltitude;
        float maxAltitude = ac.getAcInfo().radarMaxScanAltitude;
        validateTrackingStateImmediate(ac, player, aircraftId, partialTicks, elevationRef, followTurretYaw, elevationCoverage,
            scanAzClamp, scanElClamp, minAltitude, maxAltitude, maxDistance, searchType);
        pruneRadarContactsImmediate(ac, player, aircraftId, partialTicks, elevationRef, followTurretYaw, elevationCoverage,
            scanAzClamp, scanElClamp, minAltitude, maxAltitude, maxDistance, searchType);
        state = radarTrackStateMap.get(aircraftId);
        if (state != null) {
            frame.acmMode = state.acmMode;
            frame.selectedTargetId = state.selectedTargetId;
            frame.trackingTargetId = state.trackingTargetId;
        } else {
            frame.selectedTargetId = -1;
            frame.trackingTargetId = -1;
        }
        frame.scanAzimuthDeg = scanAzClamp;
        frame.trackAzimuthDeg = Math.max(0.0D, Math.min(frame.scanAzimuthDeg, ac.getAcInfo().radarTrackAzimuthDeg));
        frame.panelFillAlpha = Math.max(0.0D, Math.min(1.0D, ac.getAcInfo().radarPanelFillAlpha));
        frame.srcLikeMode = srcLikeMode;
        frame.twsLikeMode = twsLikeMode;
        frame.showMainSweep = !(srcLikeMode && frame.trackingTargetId > 0) && !frame.acmMode;
        frame.scanAxisDeg = -90.0D;
        frame.acmAxisDeg = -90.0D;
        frame.acmAzimuthDeg = frame.acmMode ? 5.0D : 0.0D;
        if (frame.acmMode) {
            double[] center = computeAcmCenterAnglesFromMouse(ac, player, partialTicks, elevationRef, followTurretYaw);
            if (center != null) {
                frame.acmAxisDeg += center[0];
            }
        }
        double renderMaxDistance = maxDistance;
        if (frame.acmMode) {
            double acmMaxDistance;
            if (ac.getAcInfo().radarMaxTargetRange > 0.0F) {
                acmMaxDistance = Math.max(minDistance + 1.0D, ac.getAcInfo().radarMaxTargetRange * 0.5D);
            } else {
                acmMaxDistance = Math.max(minDistance + 1.0D, maxDistance * 0.5D);
            }
            renderMaxDistance = acmMaxDistance;
        }
        frame.maxDistanceMeters = (int)Math.round(renderMaxDistance);
        frame.halfAzimuthDeg = frame.scanAzimuthDeg >= 359.9D ? 180 : (int)Math.round(frame.scanAzimuthDeg * 0.5D);
        frame.modeLabel = searchType;
        if (frame.acmMode) {
            frame.modeLabel = "ACM";
        } else if (frame.trackingTargetId > 0) {
            if ("SRC".equals(searchType)) {
                frame.modeLabel = "STT";
            } else if ("TWS".equals(searchType)) {
                frame.modeLabel = "TWS*";
            } else if ("GMTI_SRC".equals(searchType)) {
                frame.modeLabel = "GMTI STT";
            } else if ("GMTI_TWS".equals(searchType)) {
                frame.modeLabel = "GMTI TWS*";
            } else if ("MULTI_SRC".equals(searchType)) {
                frame.modeLabel = "MULTI STT";
            } else if ("MULTI_TWS".equals(searchType)) {
                frame.modeLabel = "MULTI TWS*";
            }
        } else if ("GMTI_SRC".equals(searchType)) {
            frame.modeLabel = "GMTI SRC";
        } else if ("GMTI_TWS".equals(searchType)) {
            frame.modeLabel = "GMTI TWS";
        } else if ("MULTI_SRC".equals(searchType)) {
            frame.modeLabel = "MULTI SRC";
        } else if ("MULTI_TWS".equals(searchType)) {
            frame.modeLabel = "MULTI TWS";
        }

        boolean hasTrackingPoint = false;
        Map<Integer, RadarContact> cache = radarContactCache.get(aircraftId);
        if (cache != null && !cache.isEmpty()) {
            for (Integer id : cache.keySet()) {
                MCH_EntityInfo info = MCH_EntityInfoClientTracker.getEntityInfo(id);
                if (info == null) {
                    continue;
                }
                if (isSelfTarget(ac, player, info) || isOwnLaunchedMissile(ac, player, info)) {
                    continue;
                }
                if (isSameTeamTarget(player, ac, info) || isTargetCountermeasureActive(ac, info)) {
                    continue;
                }
                RadarProjection proj = projectContactStatic(ac, player, info, partialTicks, elevationRef, followTurretYaw);
                if (proj == null) {
                    continue;
                }
                double targetAgl = computeAgl(ac.worldObj, info.posX, info.posY, info.posZ);
                if (!isProjectionInsideScanRange(proj, targetAgl, scanAzClamp, scanElClamp, elevationCoverage, minAltitude, maxAltitude, maxDistance, searchType, info)) {
                    continue;
                }
                double rr = (proj.distance - minDistance) / Math.max(1.0D, maxDistance - minDistance);
                rr = Math.max(0.0D, Math.min(1.0D, rr));
                double angle = Math.toRadians(-90.0D + proj.bearingDeg);
                RadarDisplayPoint p = new RadarDisplayPoint();
                p.x = Math.cos(angle) * rr;
                p.y = Math.sin(angle) * rr;
                p.selected = frame.selectedTargetId == id;
                p.tracking = frame.trackingTargetId == id;
                if (p.tracking) {
                    hasTrackingPoint = true;
                }
                p.pointSize = info.entityClassName.contains("MCH_EntityAAMissile")
                    || info.entityClassName.contains("MCH_EntityASMissile")
                    || info.entityClassName.contains("MCH_EntityATMissile")
                    || info.entityClassName.contains("MCH_EntityTvMissile") ? 1
                    : (info.entityClassName.contains("MCH_EntityHeli")
                    || info.entityClassName.contains("MCP_EntityPlane")
                    || info.entityClassName.contains("MCH_EntityTank")
                    || info.entityClassName.contains("MCH_EntityVehicle")) ? 3 : 2;
                if (p.pointSize == 3) {
                    double vx = info.posX - info.lastTickPosX;
                    double vy = info.posY - info.lastTickPosY;
                    double vz = info.posZ - info.lastTickPosZ;
                    double speed = Math.sqrt(vx * vx + vy * vy + vz * vz);
                    if (speed > 1.0E-4D) {
                        RadarProjection nextProj = projectPointStatic(ac, player, info.posX + vx, info.posY + vy, info.posZ + vz, partialTicks, elevationRef, followTurretYaw);
                        if (nextProj != null) {
                            double nrr = (nextProj.distance - minDistance) / Math.max(1.0D, maxDistance - minDistance);
                            nrr = Math.max(0.0D, Math.min(1.0D, nrr));
                            double na = Math.toRadians(-90.0D + nextProj.bearingDeg);
                            double nx = Math.cos(na) * nrr;
                            double ny = Math.sin(na) * nrr;
                            double dx = nx - p.x;
                            double dy = ny - p.y;
                            double len = Math.sqrt(dx * dx + dy * dy);
                            if (len > 1.0E-4D) {
                                p.velX = dx / len;
                                p.velY = dy / len;
                                p.velLen = Math.max(0.10D, Math.min(0.22D, 0.10D + speed * 0.06D));
                                p.hasVelocity = true;
                            }
                        }
                    }
                }
                frame.points.add(p);
            }
        }
        if (frame.trackingTargetId > 0 && !hasTrackingPoint) {
            MCH_EntityInfo tInfo = MCH_EntityInfoClientTracker.getEntityInfo(frame.trackingTargetId);
            if (tInfo != null && !isSelfTarget(ac, player, tInfo) && !isOwnLaunchedMissile(ac, player, tInfo)) {
                RadarProjection tProj = projectContactStatic(ac, player, tInfo, partialTicks, elevationRef, followTurretYaw);
                if (tProj != null) {
                    double rr = (tProj.distance - minDistance) / Math.max(1.0D, maxDistance - minDistance);
                    rr = Math.max(0.0D, Math.min(1.0D, rr));
                    double angle = Math.toRadians(-90.0D + tProj.bearingDeg);
                    RadarDisplayPoint tp = new RadarDisplayPoint();
                    tp.x = Math.cos(angle) * rr;
                    tp.y = Math.sin(angle) * rr;
                    tp.selected = frame.selectedTargetId == frame.trackingTargetId;
                    tp.tracking = true;
                    tp.pointSize = 2;
                    frame.points.add(tp);
                }
            }
        }
        frame.valid = true;
        return frame;
    }

    public static RWRDisplayFrame buildRWRDisplayFrame(MCH_EntityAircraft ac, EntityPlayer player, float partialTicks) {
        RWRDisplayFrame frame = new RWRDisplayFrame();
        frame.aircraft = ac;

        if (ac == null || ac.getAcInfo() == null || !ac.getAcInfo().hasRWR) {
            return frame;
        }

        long nowTick = ac.worldObj != null ? ac.worldObj.getTotalWorldTime() : 0L;

        MCH_RWRThreatTable table = MCH_RWRThreatClientTracker.getTable(ac.getEntityId());
        RwrHudState hudState = updateRwrHudState(ac, nowTick);

        if (hudState != null) {
            if (hudState.missileUntilTick >= nowTick && !hudState.missileSourceName.isEmpty()) {
                frame.missileSourceName = hudState.missileSourceName;
                frame.missileUntilTick = hudState.missileUntilTick;
            }
            if (hudState.lockUntilTick >= nowTick && !hudState.lockSourceName.isEmpty()) {
                frame.lockSourceName = hudState.lockSourceName;
                frame.lockUntilTick = hudState.lockUntilTick;
            }
            for (Map.Entry<String, Long> e : hudState.scanEvents.entrySet()) {
                if (e.getValue() >= nowTick) {
                    frame.scanSources.add(e.getKey());
                }
            }
        }

        double minDistance = _MIN_DISTANCE;
        double maxDistance = _RWR_RING_MAX_DISTANCE;
        if (ac.getAcInfo().radarMaxTargetRange > 0.0F) {
            maxDistance = Math.min(maxDistance, ac.getAcInfo().radarMaxTargetRange);
        }
        if (maxDistance <= minDistance) {
            minDistance = Math.max(0.0D, maxDistance - 50.0D);
        }
        frame.maxDistanceMeters = (int) Math.round(maxDistance);

        if (table != null && table.events != null) {

            for (MCH_RWRThreatEvent evt : table.events) {
                if (evt == null || evt.emitterId == ac.getEntityId()) {
                    continue;
                }
                String name = normalizeRwrSourceName(evt.sourceName);
                if (name.isEmpty() || "?".equals(name)) {
                    if (evt.emitterKind == MCH_RWRThreatEvent.EMITTER_MISSILE) {
                        name = "MSL";
                    } else {
                        MCH_EntityInfo emitterInfo = MCH_EntityInfoClientTracker.getEntityInfo(evt.emitterId);
                        if (emitterInfo != null && emitterInfo.entityName != null && !emitterInfo.entityName.trim().isEmpty()) {
                            name = emitterInfo.entityName.trim();
                        } else {
                            name = "UNKNOWN";
                        }
                    }
                }

                double distance = evt.distanceMeters > 0.0F ? evt.distanceMeters
                    : (maxDistance - MCH_RWRThreatEvent.clamp01(evt.strength) * (maxDistance - minDistance));
                if (distance <= 1.0E-4D) {
                    continue;
                }
                distance = Math.max(minDistance, Math.min(maxDistance, distance));
                double rangeNorm = (distance - minDistance) / Math.max(1.0D, maxDistance - minDistance);
                rangeNorm = Math.max(0.0D, Math.min(1.0D, rangeNorm));
                double angleRad = Math.toRadians(evt.bearingDeg - 90.0D);

                boolean isMsl = evt.threatMode == MCH_RWRThreatEvent.MODE_MSL_ACTIVE
                             || evt.threatMode == MCH_RWRThreatEvent.MODE_MSL_DATALINK;
                int color = resolveThreatColor(evt, nowTick);
                if (isMsl && hudState != null && hudState.missileUntilTick >= nowTick) {
                    boolean strong = ((nowTick / 3) & 1L) == 0L;
                    color = strong ? 0xFF2D2D : 0xCC3030;
                }

                RWRDisplayPoint point = new RWRDisplayPoint();
                point.rangeNorm = rangeNorm;
                point.angleRad = angleRad;
                point.color = color;
                point.label = name;
                point.threatMode = evt.threatMode;
                point.isMissile = isMsl;
                point.distanceMeters = distance;
                frame.points.add(point);
            }

            java.util.Collections.sort(frame.points, new java.util.Comparator<RWRDisplayPoint>() {
                @Override
                public int compare(RWRDisplayPoint a, RWRDisplayPoint b) {
                    return Double.compare(b.distanceMeters, a.distanceMeters);
                }
            });

            while (frame.points.size() > 12) {
                frame.points.remove(frame.points.size() - 1);
            }
        }

        frame.valid = true;
        return frame;
    }

    private static List<ContactCandidate> getSelectableCandidates(MCH_EntityAircraft ac, EntityPlayer player, Map<Integer, RadarContact> cache, RadarTrackState state) {
        List<ContactCandidate> result = new ArrayList<ContactCandidate>();
        if (ac == null || player == null || ac.getAcInfo() == null || cache == null || cache.isEmpty()) {
            return result;
        }
        String elevationRef = ac.getAcInfo().radarElevationReference;
        boolean followTurretYaw = (ac instanceof MCH_EntityTank) && ac.getAcInfo().radarFollowTurretYaw;
        double maxDistance = 4096.0D;
        if (ac.getAcInfo().radarMaxTargetRange > 0.0F) {
            maxDistance = Math.min(maxDistance, ac.getAcInfo().radarMaxTargetRange);
        }
        for (Integer id : cache.keySet()) {
            if (state != null && state.manualDropCooldownTick > 0 && state.lastManualDroppedTargetId == id) {
                continue;
            }
            MCH_EntityInfo info = MCH_EntityInfoClientTracker.getEntityInfo(id);
            if (info == null) {
                continue;
            }
            if (isSelfTarget(ac, player, info) || isOwnLaunchedMissile(ac, player, info)) {
                continue;
            }
            if (isSameTeamTarget(player, ac, info) || isTargetCountermeasureActive(ac, info)) {
                continue;
            }
            RadarProjection proj = projectContactStatic(ac, player, info, 0.0F, elevationRef, followTurretYaw);
            if (proj == null || proj.distance <= 1.0E-4D) {
                continue;
            }
            double angleNorm = Math.min(1.0D, Math.abs(proj.bearingDeg) / 180.0D);
            double distNorm = Math.min(1.0D, proj.distance / Math.max(1.0D, maxDistance));
            ContactCandidate c = new ContactCandidate();
            c.entityId = id;
            c.score = angleNorm * 0.7D + distNorm * 0.3D;
            result.add(c);
        }
        Collections.sort(result, new Comparator<ContactCandidate>() {
            @Override
            public int compare(ContactCandidate a, ContactCandidate b) {
                return Double.compare(a.score, b.score);
            }
        });
        return result;
    }

    private static void tickRadarTrackState(RadarTrackState state, long worldTick) {
        if (state == null) {
            return;
        }
        if (state.lastUpdateTick < 0L) {
            state.lastUpdateTick = worldTick;
            return;
        }
        long elapsed = worldTick - state.lastUpdateTick;
        if (elapsed > 0L && state.manualDropCooldownTick > 0) {
            state.manualDropCooldownTick -= (int)elapsed;
            if (state.manualDropCooldownTick < 0) {
                state.manualDropCooldownTick = 0;
            }
        }
        state.lastUpdateTick = worldTick;
    }

    private static class RadarProjection {
        public double bearingDeg;
        public double elevationDeg;
        public double distance;
    }

    private static class RcsProfile {
        public float detectFactor = 1.0F;
        public float holdTimeFactor = 1.0F;
    }

    public static class RadarDisplayPoint {
        public double x;
        public double y;
        public boolean selected;
        public boolean tracking;
        public int pointSize = 2;
        public boolean hasVelocity = false;
        public double velX = 0.0D;
        public double velY = 0.0D;
        public double velLen = 0.0D;
    }

    public static class RadarDisplayFrame {
        public MCH_EntityAircraft aircraft = null;
        public boolean valid = false;
        public boolean acmMode = false;
        public int selectedTargetId = -1;
        public int trackingTargetId = -1;
        public double scanPhase = 0.0D;
        public double scanAzimuthDeg = 360.0D;
        public double trackAzimuthDeg = 90.0D;
        public double panelFillAlpha = 0.075D;
        public boolean srcLikeMode = false;
        public boolean twsLikeMode = false;
        public boolean showMainSweep = true;
        public double scanAxisDeg = -90.0D;
        public double acmAxisDeg = -90.0D;
        public double acmAzimuthDeg = 0.0D;
        public int maxDistanceMeters = 0;
        public int halfAzimuthDeg = 180;
        public String modeLabel = "SRC";
        public final List<RadarDisplayPoint> points = new ArrayList<RadarDisplayPoint>();
    }

    public static class RWRDisplayPoint {
        public double rangeNorm;
        public double angleRad;
        public int color;
        public String label;
        public byte threatMode;
        public boolean isMissile;
        public double distanceMeters;
    }

    public static class RWRDisplayFrame {
        public MCH_EntityAircraft aircraft;
        public boolean valid = false;
        public String missileSourceName = "";
        public long missileUntilTick = -1L;
        public String lockSourceName = "";
        public long lockUntilTick = -1L;
        public final List<String> scanSources = new ArrayList<String>();
        public final List<RWRDisplayPoint> points = new ArrayList<RWRDisplayPoint>();
        public int maxDistanceMeters = 4096;
    }

    private static class RadarContact {
        public int ttl;
    }

    private static class RadarTrackState {
        public int selectedTargetId = -1;
        public int trackingTargetId = -1;
        public int lastManualDroppedTargetId = -1;
        public int manualDropCooldownTick = 0;
        public long lastUpdateTick = -1L;
        public long lastTrackToggleTick = -1L;
        public boolean acmMode = false;
    }

    private static class ArmTrackState {
        public int selectedTargetId = -1;
        public int trackingTargetId = -1;
        public long lastTrackAcquireTick = -1L;
        public long trackingLostSinceTick = -1L;
    }

    private static class ArmBvrContact {
        public int emitterId = -1;
        public String sourceName = "UNKNOWN";
        public byte threatMode = MCH_RWRThreatEvent.MODE_SEARCH;
        public int color = ARM_BVR_BASE_COLOR;
        public long untilTick = -1L;
        public long lastRefreshTick = -1L;
    }

    public static class ArmBvrDisplayContact {
        public int emitterId = -1;
        public String name = "UNKNOWN";
        public byte threatMode = MCH_RWRThreatEvent.MODE_SEARCH;
        public int color = ARM_BVR_BASE_COLOR;
        public boolean selected = false;
        public boolean tracking = false;
        public double distanceMeters = 0.0D;
    }

    private static class ContactCandidate {
        public int entityId;
        public double score;
    }

    private static class RadarRenderPoint {
        public double x;
        public double y;
    }

    private static class RwrHudState {
        public final Map<String, Long> scanEvents = new LinkedHashMap<String, Long>();
        public final Map<Integer, Integer> searchTtlByEmitter = new HashMap<Integer, Integer>();
        public String missileSourceName = "";
        public long missileUntilTick = -1L;
        public String lockSourceName = "";
        public long lockUntilTick = -1L;
    }

    private static class RwrHudLayout {
        public double offsetX;
        public double offsetY;
        public double width;
        public double height;
        public float fontScale;
        public double lineStep;
        public int scanColor;
        public int frameColor;
    }

    private static class ScissorState {
        public boolean wasEnabled;
    }
}
