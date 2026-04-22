package mcheli;

import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.aircraft.MCH_EntitySeat;
import mcheli.mob.MCH_EntityGunner;
import mcheli.network.packets.PacketRWRThreatSync;
import mcheli.weapon.MCH_EntityBaseBullet;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MCH_RWRThreatManager {

    private static final long LOCK_REPORT_EXPIRE_TICK = 40L;
    private static final int MIN_SCAN_DISTANCE = 15;
    private static final int LOCK_EVENT_TTL_TICK = 8;
    private static final int SEARCH_EVENT_TTL_TICK = 40;
    private static final int MISSILE_EVENT_TTL_TICK = 8;
    private static final int MAX_RADAR_SCAN_TICK = 400;
    private static final long SCAN_COUNTER_STALE_TICK = 400L;
    private static final long THREAT_STALE_TICK = 200L;
    private final Map<Integer, RadarTrackingReport> trackingReports = new HashMap<Integer, RadarTrackingReport>();
    private final Map<Integer, RadarTrackingReport> gunnerTrackingReports = new HashMap<Integer, RadarTrackingReport>();
    private final Map<Long, Integer> scanHitCounters = new HashMap<Long, Integer>();
    private final Map<Long, Long> scanPairLastSlot = new HashMap<Long, Long>();
    private final Map<Long, Long> scanPairLastTouchedTick = new HashMap<Long, Long>();
    private final Map<ThreatKey, ActiveThreatState> activeThreats = new HashMap<ThreatKey, ActiveThreatState>();
    private long snapshotSeq = 0L;

    public void reportRadarTracking(EntityPlayerMP reporter, int emitterAircraftId, int trackingTargetId) {
        if (reporter == null || reporter.worldObj == null || emitterAircraftId <= 0) {
            return;
        }
        Entity emitter = reporter.worldObj.getEntityByID(emitterAircraftId);
        if (!(emitter instanceof MCH_EntityAircraft)) {
            return;
        }
        MCH_EntityAircraft ac = (MCH_EntityAircraft) emitter;
        if (!isAircraftMannedForEmission(ac)) {
            trackingReports.remove(emitterAircraftId);
            return;
        }
        if (!isReporterControllingAircraft(ac, reporter)) {
            return;
        }
        long now = ac.worldObj.getTotalWorldTime();
        if (trackingTargetId <= 0) {
            trackingReports.remove(emitterAircraftId);
        } else {
            trackingReports.put(emitterAircraftId, new RadarTrackingReport(trackingTargetId, now + LOCK_REPORT_EXPIRE_TICK));
        }
    }

    public void reportGunnerTracking(MCH_EntityAircraft emitter, Entity tracker, Entity targetEntity) {
        if (emitter == null || emitter.worldObj == null) {
            return;
        }
        int emitterAircraftId = emitter.getEntityId();
        if (emitterAircraftId <= 0) {
            return;
        }
        if (!isAircraftMannedForEmission(emitter) || emitter.isDestroyed() || emitter.getAcInfo() == null || !emitter.getAcInfo().enableRadar) {
            gunnerTrackingReports.remove(emitterAircraftId);
            return;
        }
        if (tracker != null && emitter.getSeatIdByEntity(tracker) < 0) {
            return;
        }
        long now = emitter.worldObj.getTotalWorldTime();
        MCH_EntityAircraft target = resolveThreatReceiverAircraft(targetEntity);
        if (target == null || target == emitter || target.isDead || isSameTeam(emitter, target)) {
            gunnerTrackingReports.remove(emitterAircraftId);
            return;
        }
        gunnerTrackingReports.put(emitterAircraftId, new RadarTrackingReport(target.getEntityId(), now + LOCK_REPORT_EXPIRE_TICK));
    }

    public String getEmitterTrackingDebugLine(int emitterAircraftId) {
        if (emitterAircraftId <= 0) {
            return "tracking emitterId=invalid";
        }
        long now = 0L;
        if (MinecraftServer.getServer() != null && MinecraftServer.getServer().getEntityWorld() != null) {
            now = MinecraftServer.getServer().getEntityWorld().getTotalWorldTime();
        }
        RadarTrackingReport packet = trackingReports.get(emitterAircraftId);
        RadarTrackingReport gunner = gunnerTrackingReports.get(emitterAircraftId);
        String packetTarget = (packet != null && packet.expireTick >= now && packet.targetEntityId > 0) ? String.valueOf(packet.targetEntityId) : "-";
        long packetTtl = (packet != null && packet.expireTick >= now) ? (packet.expireTick - now) : -1L;
        String gunnerTarget = (gunner != null && gunner.expireTick >= now && gunner.targetEntityId > 0) ? String.valueOf(gunner.targetEntityId) : "-";
        long gunnerTtl = (gunner != null && gunner.expireTick >= now) ? (gunner.expireTick - now) : -1L;
        return String.format(Locale.ROOT, "tracking emitter=%d packetTarget=%s packetTtl=%d gunnerTarget=%s gunnerTtl=%d",
            emitterAircraftId, packetTarget, packetTtl, gunnerTarget, gunnerTtl);
    }

    public void serverTick() {
        WorldServer[] worlds = MinecraftServer.getServer().worldServers;
        if (worlds == null) {
            return;
        }
        long now = MinecraftServer.getServer().getEntityWorld().getTotalWorldTime();
        boolean shouldWatchLog = MCH_RadarDebug.isRwrWatchEnabled()
            && now % Math.max(1, MCH_RadarDebug.getRwrWatchIntervalTick()) == 0L;
        int activeTrackingReports = 0;
        int activeGunnerTrackingReports = 0;
        int gunnerAircraftTargets = 0;
        snapshotSeq++;
        for (WorldServer world : worlds) {
            if (world == null) {
                continue;
            }
            @SuppressWarnings("unchecked")
            List<Entity> loaded = world.loadedEntityList;
            for (Entity entity : loaded) {
                if (!(entity instanceof MCH_EntityAircraft)) {
                    continue;
                }
                MCH_EntityAircraft emitter = (MCH_EntityAircraft) entity;
                if (!canEmitRadarThreat(emitter)) {
                    continue;
                }
                if (shouldWatchLog) {
                    RadarTrackingReport report = trackingReports.get(emitter.getEntityId());
                    if (report != null && report.expireTick >= now && report.targetEntityId > 0) {
                        activeTrackingReports++;
                    }
                    RadarTrackingReport gunnerReport = gunnerTrackingReports.get(emitter.getEntityId());
                    if (gunnerReport != null && gunnerReport.expireTick >= now && gunnerReport.targetEntityId > 0) {
                        activeGunnerTrackingReports++;
                    }
                    gunnerAircraftTargets += countGunnerAircraftTargets(emitter);
                }
                emitScanThreatForEmitter(emitter, loaded, now);
                emitLockThreatForEmitter(emitter, now);
            }
            emitMissileThreats(world, loaded, now);
        }
        if (shouldWatchLog) {
            MCH_RadarDebug.appendManual(
                "RWRWATCH-SV tick=%d trackingReports=%d gunnerAircraftTargets=%d activeThreats=%d",
                now, activeTrackingReports + activeGunnerTrackingReports, gunnerAircraftTargets, activeThreats.size()
            );
        }
        cleanup(now);
        dispatchThreatTables(now);
    }

    private void emitScanThreatForEmitter(MCH_EntityAircraft emitter, List<Entity> loaded, long now) {
        if (emitter.getAcInfo() == null) {
            return;
        }
        double maxRange = emitter.getAcInfo().radarMaxTargetRange > 0.0F ? emitter.getAcInfo().radarMaxTargetRange : 4096.0D;
        float scanAz = Math.max(0.0F, Math.min(360.0F, emitter.getAcInfo().radarScanAzimuthDeg));
        float scanEl = Math.max(0.0F, Math.min(180.0F, emitter.getAcInfo().radarScanElevationDeg));
        int radarScanTick = normalizeRadarScanTick(emitter.getAcInfo().radarScanTick);
        int hitNeed = getScanHitNeed(radarScanTick);
        long scanSlot = now / radarScanTick;
        String sourceName = getEmitterRwrName(emitter);
        for (Entity e : loaded) {
            if (!(e instanceof MCH_EntityAircraft) || e == emitter) {
                continue;
            }
            MCH_EntityAircraft target = (MCH_EntityAircraft) e;
            long key = toPairKey(emitter.getEntityId(), target.getEntityId());
            Long lastSlot = scanPairLastSlot.get(key);
            if (lastSlot != null && lastSlot.longValue() == scanSlot) {
                continue;
            }
            scanPairLastSlot.put(key, scanSlot);
            scanPairLastTouchedTick.put(key, now);
            if (!isTargetRwrReceivable(target)) {
                continue;
            }
            if (isSameTeam(emitter, target)) {
                continue;
            }
            if (!isTargetInsideScanCone(emitter, target, maxRange, scanAz, scanEl)) {
                decreaseScanHitCounter(emitter.getEntityId(), target.getEntityId());
                continue;
            }
            int hit = scanHitCounters.containsKey(key) ? scanHitCounters.get(key) + 1 : 1;
            if (hit >= hitNeed) {
                scanHitCounters.put(key, 0);
                touchThreat(target, emitter.getEntityId(), getEmitterKind(emitter), MCH_RWRThreatEvent.MODE_SEARCH,
                    sourceName, now, SEARCH_EVENT_TTL_TICK, 0.85F, maxRange);
            } else {
                scanHitCounters.put(key, hit);
            }
        }
    }

    private void emitLockThreatForEmitter(MCH_EntityAircraft emitter, long now) {
        TrackingSource tracking = selectTrackingSource(emitter.getEntityId(), now);
        if (tracking == null || tracking.report == null) {
            return;
        }
        RadarTrackingReport report = tracking.report;
        if (report.targetEntityId <= 0 || emitter.worldObj == null) {
            return;
        }
        Entity targetEntity = emitter.worldObj.getEntityByID(report.targetEntityId);
        if (!(targetEntity instanceof MCH_EntityAircraft)) {
            return;
        }
        MCH_EntityAircraft target = (MCH_EntityAircraft) targetEntity;
        if (!isTargetRwrReceivable(target) || isSameTeam(emitter, target)) {
            return;
        }
        String sourceName = getEmitterRwrName(emitter);
        touchThreat(target, emitter.getEntityId(), getEmitterKind(emitter), MCH_RWRThreatEvent.MODE_STT,
            sourceName, now, LOCK_EVENT_TTL_TICK, 1.0F,
            emitter.getAcInfo().radarMaxTargetRange > 0.0F ? emitter.getAcInfo().radarMaxTargetRange : 4096.0D);
        if (MCH_RadarDebug.isRwrWatchEnabled() && now % Math.max(1, MCH_RadarDebug.getRwrWatchIntervalTick()) == 0L) {
            MCH_RadarDebug.appendManual(
                "RWRLOCK emitter=%d target=%d source=%s ttl=%d",
                emitter.getEntityId(), target.getEntityId(), tracking.source, LOCK_EVENT_TTL_TICK
            );
        }
    }

    private TrackingSource selectTrackingSource(int emitterAircraftId, long now) {
        RadarTrackingReport packet = trackingReports.get(emitterAircraftId);
        if (packet != null && packet.expireTick >= now && packet.targetEntityId > 0) {
            return new TrackingSource(packet, "packet_report");
        }
        RadarTrackingReport gunner = gunnerTrackingReports.get(emitterAircraftId);
        if (gunner != null && gunner.expireTick >= now && gunner.targetEntityId > 0) {
            return new TrackingSource(gunner, "gunner_report");
        }
        return null;
    }

    private int countGunnerAircraftTargets(MCH_EntityAircraft emitter) {
        if (emitter == null) {
            return 0;
        }
        int count = 0;
        count += countGunnerAircraftTargetOnEntity(emitter, emitter.getRiddenByEntity());
        for (int sid = 1; sid <= emitter.getSeatNum(); sid++) {
            count += countGunnerAircraftTargetOnEntity(emitter, emitter.getEntityBySeatId(sid));
        }
        return count;
    }

    private int countGunnerAircraftTargetOnEntity(MCH_EntityAircraft emitter, Entity crew) {
        if (!(crew instanceof MCH_EntityGunner)) {
            return 0;
        }
        MCH_EntityGunner gunner = (MCH_EntityGunner) crew;
        MCH_EntityAircraft target = resolveThreatReceiverAircraft(gunner.targetEntity);
        if (target == null || target == emitter || isSameTeam(emitter, target)) {
            return 0;
        }
        return 1;
    }

    private void emitMissileThreats(WorldServer world, List<Entity> loaded, long now) {
        if (world == null || loaded == null) {
            return;
        }
        for (Entity e : loaded) {
            if (!(e instanceof MCH_EntityBaseBullet)) {
                continue;
            }
            MCH_EntityBaseBullet bullet = (MCH_EntityBaseBullet) e;
            if (bullet.isDead || bullet.targetEntity == null || bullet.targetEntity.isDead) {
                continue;
            }
            MCH_EntityAircraft target = resolveThreatReceiverAircraft(bullet.targetEntity);
            if (!isTargetRwrReceivable(target) || target.worldObj != world) {
                continue;
            }
            byte mode;
            if (bullet.isActiveRadarCaptured()) {
                mode = MCH_RWRThreatEvent.MODE_MSL_ACTIVE;
            } else if (bullet.isDataLinkRelayMode()) {
                mode = MCH_RWRThreatEvent.MODE_MSL_DATALINK;
            } else {
                mode = MCH_RWRThreatEvent.MODE_MSL_ACTIVE;
            }
            int emitterId = bullet.getEntityId();
            if (bullet.shootingAircraft != null) {
                emitterId = bullet.shootingAircraft.getEntityId();
            } else if (bullet.shootingEntity != null) {
                emitterId = bullet.shootingEntity.getEntityId();
            }
            String sourceName = bullet.nameOnRWR != null && !bullet.nameOnRWR.trim().isEmpty() ? bullet.nameOnRWR.trim() : "MSL";
            touchThreatFromPosition(target, emitterId, MCH_RWRThreatEvent.EMITTER_MISSILE, mode, sourceName,
                bullet.posX, bullet.posY, bullet.posZ, now, MISSILE_EVENT_TTL_TICK, 1.0F);
        }
    }

    private MCH_EntityAircraft resolveThreatReceiverAircraft(Entity targetEntity) {
        if (targetEntity == null || targetEntity.isDead) {
            return null;
        }
        if (targetEntity instanceof MCH_EntityAircraft) {
            return (MCH_EntityAircraft) targetEntity;
        }
        if (targetEntity instanceof EntityPlayer) {
            return MCH_EntityAircraft.getAircraft_RiddenOrControl((EntityPlayer) targetEntity);
        }
        if (targetEntity.ridingEntity instanceof MCH_EntityAircraft) {
            return (MCH_EntityAircraft) targetEntity.ridingEntity;
        }
        if (targetEntity.ridingEntity instanceof MCH_EntitySeat) {
            return ((MCH_EntitySeat) targetEntity.ridingEntity).getParent();
        }
        return null;
    }

    private void touchThreat(MCH_EntityAircraft receiver, int emitterId, byte emitterKind, byte mode,
                             String sourceName, long now, int ttlTick, float confidence, double maxRange) {
        if (receiver == null || receiver.worldObj == null) {
            return;
        }
        Entity emitter = receiver.worldObj.getEntityByID(emitterId);
        if (emitter == null) {
            return;
        }
        touchThreatFromPosition(receiver, emitterId, emitterKind, mode, sourceName,
            emitter.posX, emitter.posY, emitter.posZ, now, ttlTick, confidence);
    }

    private void touchThreatFromPosition(MCH_EntityAircraft receiver, int emitterId, byte emitterKind, byte mode,
                                         String sourceName, double ex, double ey, double ez,
                                         long now, int ttlTick, float confidence) {
        if (receiver == null) {
            return;
        }
        ThreatKey key = new ThreatKey(receiver.getEntityId(), emitterId, mode);
        double dx = ex - receiver.posX;
        double dy = ey - receiver.posY;
        double dz = ez - receiver.posZ;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        float bearing = getRelativeBearing(receiver, dx, dz);
        float strength = (float) Math.max(0.0D, Math.min(1.0D, 1.0D - distance / 4096.0D));
        ActiveThreatState state = activeThreats.get(key);
        if (state == null) {
            state = new ActiveThreatState();
            activeThreats.put(key, state);
        }
        state.key = key;
        state.emitterKind = emitterKind;
        state.sourceName = sourceName != null && !sourceName.trim().isEmpty() ? sourceName.trim() : "?";
        state.bearingDeg = bearing;
        state.distanceMeters = (float) distance;
        state.strength = MCH_RWRThreatEvent.clamp01(strength);
        state.confidence = MCH_RWRThreatEvent.clamp01(confidence);
        state.expireTick = Math.max(state.expireTick, now + Math.max(1, ttlTick));
        state.lastTouchTick = now;
    }

    private void dispatchThreatTables(long now) {
        @SuppressWarnings("unchecked")
        List<EntityPlayerMP> players = MinecraftServer.getServer().getConfigurationManager().playerEntityList;
        if (players == null) {
            return;
        }
        for (EntityPlayerMP player : players) {
            if (player == null || player.isDead) {
                continue;
            }
            MCH_EntityAircraft ac = MCH_EntityAircraft.getAircraft_RiddenOrControl(player);
            if (ac == null || !isTargetRwrReceivable(ac)) {
                continue;
            }
            List<MCH_RWRThreatEvent> events = collectThreatEventsForReceiver(ac.getEntityId(), now);
            MCH_RWRThreatTable table = new MCH_RWRThreatTable(ac.getEntityId(), snapshotSeq, events);
            MCH_MOD.getPacketHandler().sendTo(new PacketRWRThreatSync(table), player);
        }
    }

    private List<MCH_RWRThreatEvent> collectThreatEventsForReceiver(int receiverId, long now) {
        List<MCH_RWRThreatEvent> list = new ArrayList<MCH_RWRThreatEvent>();
        for (ActiveThreatState s : activeThreats.values()) {
            if (s == null || s.key == null || s.key.receiverId != receiverId || s.expireTick < now) {
                continue;
            }
            int ttl = (int) Math.max(1L, s.expireTick - now);
            list.add(new MCH_RWRThreatEvent(
                s.key.emitterId,
                s.emitterKind,
                s.key.mode,
                s.bearingDeg,
                s.strength,
                ttl,
                s.confidence,
                s.distanceMeters,
                s.sourceName
            ));
        }
        Collections.sort(list, new Comparator<MCH_RWRThreatEvent>() {
            @Override
            public int compare(MCH_RWRThreatEvent a, MCH_RWRThreatEvent b) {
                int pa = threatPriority(a.threatMode);
                int pb = threatPriority(b.threatMode);
                if (pa != pb) {
                    return Integer.compare(pb, pa);
                }
                return Float.compare(a.distanceMeters, b.distanceMeters);
            }
        });
        if (list.size() > 12) {
            return new ArrayList<MCH_RWRThreatEvent>(list.subList(0, 12));
        }
        return list;
    }

    private int threatPriority(byte mode) {
        if (mode == MCH_RWRThreatEvent.MODE_MSL_ACTIVE || mode == MCH_RWRThreatEvent.MODE_MSL_DATALINK) {
            return 4;
        }
        if (mode == MCH_RWRThreatEvent.MODE_STT) {
            return 3;
        }
        if (mode == MCH_RWRThreatEvent.MODE_TRACK) {
            return 2;
        }
        return 1;
    }

    private float getRelativeBearing(MCH_EntityAircraft receiver, double dx, double dz) {
        // Keep handedness consistent with radar projection: right positive, left negative.
        double absYaw = Math.toDegrees(Math.atan2(-dx, dz));
        double yaw = interpolateRotation(receiver.prevRotationYaw, receiver.rotationYaw, 1.0F);
        double rel = absYaw - yaw;
        while (rel > 180.0D) {
            rel -= 360.0D;
        }
        while (rel < -180.0D) {
            rel += 360.0D;
        }
        return (float) rel;
    }

    private byte getEmitterKind(MCH_EntityAircraft emitter) {
        if (emitter instanceof MCH_EntityAircraft) {
            return MCH_RWRThreatEvent.EMITTER_AIRCRAFT;
        }
        return MCH_RWRThreatEvent.EMITTER_GROUND_VEHICLE;
    }

    private boolean canEmitRadarThreat(MCH_EntityAircraft ac) {
        return ac != null
            && ac.getAcInfo() != null
            && ac.getAcInfo().enableRadar
            && isAircraftMannedForEmission(ac)
            && !ac.isDestroyed();
    }

    private boolean isAircraftMannedForEmission(MCH_EntityAircraft ac) {
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

    private boolean isReporterControllingAircraft(MCH_EntityAircraft ac, EntityPlayerMP reporter) {
        if (ac == null || reporter == null) {
            return false;
        }
        return ac.getSeatIdByEntity(reporter) >= 0;
    }

    private boolean isTargetRwrReceivable(MCH_EntityAircraft target) {
        return target != null && target.getAcInfo() != null && target.getAcInfo().hasRWR;
    }

    private boolean isTargetInsideScanCone(MCH_EntityAircraft emitter, MCH_EntityAircraft target, double maxRange, float scanAzDeg, float scanElDeg) {
        double dx = target.posX - emitter.posX;
        double dy = target.posY - emitter.posY;
        double dz = target.posZ - emitter.posZ;
        double distSq = dx * dx + dy * dy + dz * dz;
        if (distSq < MIN_SCAN_DISTANCE * MIN_SCAN_DISTANCE || distSq > maxRange * maxRange) {
            return false;
        }
        double yaw = Math.toRadians(interpolateRotation(emitter.prevRotationYaw, emitter.rotationYaw, 1.0F));
        double pitch = Math.toRadians(interpolateRotation(emitter.prevRotationPitch, emitter.rotationPitch, 1.0F));
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
        if (Math.abs(relAz) > scanAzDeg * 0.5D) {
            return false;
        }
        String coverage = emitter.getAcInfo().radarElevationCoverage;
        if ("FULL".equalsIgnoreCase(coverage)) {
            return Math.abs(relElev) <= scanElDeg * 0.5D;
        }
        if ("DOWN_ONLY".equalsIgnoreCase(coverage)) {
            return relElev <= 0.0D && relElev >= -scanElDeg;
        }
        return relElev >= 0.0D && relElev <= scanElDeg;
    }

    private int getScanHitNeed(int radarScanTick) {
        if (radarScanTick >= 80) {
            return 1;
        }
        if (radarScanTick > 20) {
            return 2;
        }
        return 4;
    }

    private int normalizeRadarScanTick(int radarScanTick) {
        if (radarScanTick <= 0) {
            return 1;
        }
        return Math.min(radarScanTick, MAX_RADAR_SCAN_TICK);
    }

    private void decreaseScanHitCounter(int emitterId, int targetId) {
        long key = toPairKey(emitterId, targetId);
        Integer hit = scanHitCounters.get(key);
        if (hit == null || hit.intValue() <= 0) {
            return;
        }
        int next = hit.intValue() - 1;
        if (next <= 0) {
            scanHitCounters.remove(key);
        } else {
            scanHitCounters.put(key, next);
        }
    }

    private boolean isSameTeam(MCH_EntityAircraft emitter, MCH_EntityAircraft target) {
        if (emitter == null || target == null) {
            return false;
        }
        Entity eOp = getPrimaryOperator(emitter);
        Entity tOp = getPrimaryOperator(target);
        if (!(eOp instanceof EntityLivingBase) || !(tOp instanceof EntityLivingBase)) {
            return false;
        }
        EntityLivingBase a = (EntityLivingBase) eOp;
        EntityLivingBase b = (EntityLivingBase) tOp;
        return a.getTeam() != null && b.getTeam() != null && a.isOnSameTeam(b);
    }

    private Entity getPrimaryOperator(MCH_EntityAircraft ac) {
        if (ac == null) {
            return null;
        }
        Entity pilot = ac.getRiddenByEntity();
        if (pilot instanceof EntityLivingBase) {
            return pilot;
        }
        for (int sid = 1; sid <= ac.getSeatNum(); sid++) {
            Entity crew = ac.getEntityBySeatId(sid);
            if (crew instanceof EntityLivingBase) {
                return crew;
            }
        }
        return null;
    }

    private String getEmitterRwrName(MCH_EntityAircraft emitter) {
        if (emitter == null || emitter.getAcInfo() == null) {
            return "?";
        }
        String n = emitter.getAcInfo().nameOnRWR;
        if (n == null || n.trim().isEmpty()) {
            return "?";
        }
        return n.trim();
    }

    private void cleanup(long now) {
        trackingReports.entrySet().removeIf(e -> e.getValue() == null || e.getValue().expireTick < now);
        gunnerTrackingReports.entrySet().removeIf(e -> e.getValue() == null || e.getValue().expireTick < now);
        scanPairLastTouchedTick.entrySet().removeIf(e -> e.getValue() == null || now - e.getValue() > SCAN_COUNTER_STALE_TICK);
        scanPairLastSlot.entrySet().removeIf(e -> !scanPairLastTouchedTick.containsKey(e.getKey()));
        scanHitCounters.entrySet().removeIf(e -> !scanPairLastTouchedTick.containsKey(e.getKey()));
        Iterator<Map.Entry<ThreatKey, ActiveThreatState>> it = activeThreats.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<ThreatKey, ActiveThreatState> en = it.next();
            ActiveThreatState s = en.getValue();
            if (s == null || s.expireTick < now || now - s.lastTouchTick > THREAT_STALE_TICK) {
                it.remove();
            }
        }
    }

    private long toPairKey(int a, int b) {
        return ((long) a << 32) ^ (b & 0xFFFFFFFFL);
    }

    private float interpolateRotation(float prev, float cur, float factor) {
        return prev + (cur - prev) * factor;
    }

    private static class RadarTrackingReport {
        final int targetEntityId;
        final long expireTick;

        RadarTrackingReport(int targetEntityId, long expireTick) {
            this.targetEntityId = targetEntityId;
            this.expireTick = expireTick;
        }
    }

    private static class TrackingSource {
        final RadarTrackingReport report;
        final String source;

        TrackingSource(RadarTrackingReport report, String source) {
            this.report = report;
            this.source = source;
        }
    }

    private static class ThreatKey {
        final int receiverId;
        final int emitterId;
        final byte mode;

        ThreatKey(int receiverId, int emitterId, byte mode) {
            this.receiverId = receiverId;
            this.emitterId = emitterId;
            this.mode = mode;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof ThreatKey)) {
                return false;
            }
            ThreatKey o = (ThreatKey) obj;
            return this.receiverId == o.receiverId && this.emitterId == o.emitterId && this.mode == o.mode;
        }

        @Override
        public int hashCode() {
            int h = receiverId;
            h = 31 * h + emitterId;
            h = 31 * h + mode;
            return h;
        }
    }

    private static class ActiveThreatState {
        ThreatKey key;
        byte emitterKind;
        String sourceName;
        float bearingDeg;
        float strength;
        float confidence;
        float distanceMeters;
        long expireTick;
        long lastTouchTick;
    }
}
