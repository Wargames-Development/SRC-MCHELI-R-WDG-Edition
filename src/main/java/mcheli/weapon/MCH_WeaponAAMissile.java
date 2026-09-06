package mcheli.weapon;

import mcheli.MCH_EntityInfo;
import mcheli.MCH_EntityInfoClientTracker;
import mcheli.MCH_Lib;
import mcheli.MCH_MOD;
import mcheli.MCH_PlayerViewHandler;
import mcheli.MCH_RadarDebug;
import mcheli.aircraft.MCH_AircraftInfo;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.network.packets.PacketLockTargetBVR;
import mcheli.plane.MCP_EntityPlane;
import mcheli.render.MCH_RenderRWR;
import mcheli.tank.MCH_EntityTank;
import mcheli.wrapper.W_Entity;
import mcheli.wrapper.W_MovingObjectPosition;
import mcheli.wrapper.W_WorldFunc;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
public class MCH_WeaponAAMissile extends MCH_WeaponEntitySeeker {
    private static final int OPTION_FLAG_DATALINK = 1 << 8;
    private static final int OPTION_FLAG_DATALINK_TWS_SELECTED_ONLY = 1 << 9;
    private static final int OPTION_FLAG_ARM_NARROW_BAND = 1 << 10;
    private static final long SNAPSHOT_TARGET_STALE_MS = 1500L;
    private int snapshotHeatSeekerTargetId;

    public MCH_WeaponAAMissile(World w, Vec3 v, float yaw, float pitch, String nm, MCH_WeaponInfo wi) {
        super(w, v, yaw, pitch, nm, wi);
        super.power = 12;
        super.acceleration = 2.5F;
        super.explosionPower = 4;
        super.interval = 5;
        super.guidanceSystem.canLockInAir = true;
        super.guidanceSystem.ridableOnly = wi.ridableOnly;
        super.guidanceSystem.playDefaultLockSounds = !("aamissile".equals(wi.type)
            && wi.isHeatSeekerMissile && !wi.activeRadar && !wi.passiveRadar
            && !wi.semiActiveRadar && !wi.antiRadiationMissile);
        this.snapshotHeatSeekerTargetId = 0;
    }

    public boolean isCooldownCountReloadTime() {
        return true;
    }

    public void update(int countWait) {
        super.update(countWait);
        // MCH_WeaponGuidanceSystem clears a lock when it was not refreshed by the
        // selected weapon this tick. Mirror that lifecycle for snapshot-only IR locks.
        if (super.worldObj.isRemote && this.snapshotHeatSeekerTargetId > 0
            && super.guidanceSystem.lockCount == 0 && super.guidanceSystem.prevLockCount == 0) {
            this.snapshotHeatSeekerTargetId = 0;
            super.optionParameter1 = 0;
        }
    }

    @Override
    public boolean shot(MCH_WeaponParam prm) {
        if (getInfo().antiRadiationMissile) {
            super.optionParameter2 &= ~OPTION_FLAG_ARM_NARROW_BAND;
            if (isArmNarrowBandMode()) {
                super.optionParameter2 |= OPTION_FLAG_ARM_NARROW_BAND;
                if (super.worldObj.isRemote && prm.entity instanceof MCH_EntityAircraft && prm.user != null) {
                    MCH_EntityAircraft ac = (MCH_EntityAircraft) prm.entity;
                    int armLockTargetId = MCH_RenderRWR.getArmTrackingTargetId(ac);
                    prm.option1 = Math.max(0, armLockTargetId);
                    super.optionParameter1 = prm.option1;
                }
            }
        }
        if (shouldBlockShotWithoutBvrRadarTrack(prm)) {
            return false;
        }
        if (shouldBlockShotByDataLink(prm)) {
            return false;
        }
        if (shouldBlockShotByArmBandConstraint(prm)) {
            return false;
        }
        boolean result = false;
        float yaw, pitch;
        if (prm.entity instanceof MCH_EntityTank) {
            MCH_EntityTank tank = (MCH_EntityTank) prm.entity;
            // MCH_WeaponSet has already combined the hull and turret rotations here.
            yaw = prm.rotYaw;
            pitch = prm.rotPitch;
            int wid = tank.getCurrentWeaponID(prm.user);
            MCH_AircraftInfo.Weapon w = tank.getAcInfo().getWeaponById(wid);
            float minPitch = w == null ? tank.getAcInfo().minRotationPitch : w.minPitch;
            float maxPitch = w == null ? tank.getAcInfo().maxRotationPitch : w.maxPitch;
            float playerYaw = MathHelper.wrapAngleTo180_float(tank.getRotYaw() - yaw);
            float playerPitch = tank.getRotPitch() * MathHelper.cos((float) (playerYaw * Math.PI / 180.0D))
                + -tank.getRotRoll() * MathHelper.sin((float) (playerYaw * Math.PI / 180.0D));
            float playerYawRel = MathHelper.wrapAngleTo180_float(yaw - tank.getRotYaw());
            float yawLimit = (w == null ? 360F : w.maxYaw);
            float relativeYaw = MCH_Lib.RNG(playerYawRel, -yawLimit, yawLimit);
            yaw = MathHelper.wrapAngleTo180_float(tank.getRotYaw() + relativeYaw);
            if(fixRotationPitch == 0) {
                pitch = MCH_Lib.RNG(pitch, playerPitch + minPitch, playerPitch + maxPitch);
            }
            pitch = MCH_Lib.RNG(pitch, -90.0F, 90.0F);
        } else if (getInfo().enableOffAxis) {
            yaw = prm.user.rotationYaw + super.fixRotationYaw;
            pitch = prm.user.rotationPitch + super.fixRotationPitch;
        } else {
            yaw = prm.entity.rotationYaw + super.fixRotationYaw;
            pitch = prm.entity.rotationPitch + super.fixRotationPitch;
        }
        if (!super.worldObj.isRemote) {
            if (getInfo().passiveRadar || getInfo().activeRadar || getInfo().semiActiveRadar) {
                Entity tgtEnt = prm.user.worldObj.getEntityByID(prm.option1);
                if (!getInfo().antiRadiationMissile) {
                    boolean validTarget = isValidServerTarget(prm, tgtEnt);
                    boolean requiresTrackedTarget = requiresBvrRadarTrack(prm)
                        || getInfo().passiveRadar || getInfo().semiActiveRadar;
                    if (requiresTrackedTarget && !validTarget) {
                        return false;
                    }
                    if (!validTarget) {
                        tgtEnt = null;
                    }
                }
                double tX = -MathHelper.sin(yaw / 180.0F * 3.1415927F) * MathHelper.cos(pitch / 180.0F * 3.1415927F);
                double tZ = MathHelper.cos(yaw / 180.0F * 3.1415927F) * MathHelper.cos(pitch / 180.0F * 3.1415927F);
                double tY = -MathHelper.sin(pitch / 180.0F * 3.1415927F);
                MCH_EntityAAMissile e = new MCH_EntityAAMissile(super.worldObj, prm.posX, prm.posY, prm.posZ, tX, tY, tZ, yaw, pitch, (double) super.acceleration);
                if (yaw > 180.0F) {
                    yaw -= 360.0F;
                } else if (yaw < -180.0F) {
                    yaw += 360.0F;
                }
                e.setInfoByName(super.name);
                e.setParameterFromWeapon(this, prm.entity, prm.user);
                e.setDataLinkRelayMode((prm.option2 & OPTION_FLAG_DATALINK) != 0);
                e.setDataLinkTwsSelectedOnly((prm.option2 & OPTION_FLAG_DATALINK_TWS_SELECTED_ONLY) != 0);
                if (tgtEnt != null) {
                    e.setTargetEntity(tgtEnt);
                }
                if (MCH_RadarDebug.isEnabled()) {
                    double dist = -1.0D;
                    if (tgtEnt != null) {
                        double dx = prm.posX - tgtEnt.posX;
                        double dy = prm.posY - tgtEnt.posY;
                        double dz = prm.posZ - tgtEnt.posZ;
                        dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
                    }
                    MCH_RadarDebug.trace(super.worldObj, prm.entity,
                        "msl_spawn type=AA msl=%d reqTargetId=%d resolved=%s resolvedId=%d dist=%.1f dlRelay=%s twsSelOnly=%s option2=0x%X",
                        e.getEntityId(),
                        prm.option1,
                        String.valueOf(tgtEnt != null && !tgtEnt.isDead),
                        tgtEnt != null ? tgtEnt.getEntityId() : -1,
                        dist,
                        String.valueOf(e.isDataLinkRelayMode()),
                        String.valueOf(e.isDataLinkTwsSelectedOnly()),
                        prm.option2);
                }
                result = super.worldObj.spawnEntityInWorld(e);
                if (result) {
                    this.playSound(prm.entity);
                }
            } else {
                Entity tgtEnt = prm.user.worldObj.getEntityByID(prm.option1);
                boolean validTarget = tgtEnt != null && !tgtEnt.isDead;
                if (isPureHeatSeeker()) {
                    validTarget = isValidServerHeatSeekerTarget(prm, tgtEnt);
                }
                if (validTarget) {
                    double tX = -MathHelper.sin(yaw / 180.0F * 3.1415927F) * MathHelper.cos(pitch / 180.0F * 3.1415927F);
                    double tZ = MathHelper.cos(yaw / 180.0F * 3.1415927F) * MathHelper.cos(pitch / 180.0F * 3.1415927F);
                    double tY = -MathHelper.sin(pitch / 180.0F * 3.1415927F);
                    MCH_EntityAAMissile e = new MCH_EntityAAMissile(super.worldObj, prm.posX, prm.posY, prm.posZ, tX, tY, tZ, yaw, pitch, (double) super.acceleration);
                    if (yaw > 180.0F) {
                        yaw -= 360.0F;
                    } else if (yaw < -180.0F) {
                        yaw += 360.0F;
                    }
                    e.setInfoByName(super.name);
                    e.setParameterFromWeapon(this, prm.entity, prm.user);
                    e.setDataLinkTwsSelectedOnly(false);
                    e.setTargetEntity(tgtEnt);
                    result = super.worldObj.spawnEntityInWorld(e);
                    if (result) {
                        this.playSound(prm.entity);
                    }
                }
            }
        } else {
            if (getInfo().activeRadar) {
                result = true;
            } else if ((getInfo().passiveRadar || getInfo().semiActiveRadar)
                && (getInfo().antiRadiationMissile || super.optionParameter1 > 0)) {
                result = true;
            } else if (isPureHeatSeeker() && super.optionParameter1 > 0) {
                result = true;
            } else if (super.guidanceSystem.lock(prm.user) && super.guidanceSystem.lastLockEntity != null) {
                result = true;
                super.optionParameter1 = W_Entity.getEntityId(super.guidanceSystem.lastLockEntity);
            }
            if(result) {
                MCH_PlayerViewHandler.applyRecoil(getInfo().getRecoilPitch(), getInfo().getRecoilYaw(), getInfo().recoilRecoverFactor);
                spawnMuzzleFlash(worldObj, prm, getInfo(), yaw, pitch, prm.muzzleFlashPosX, prm.muzzleFlashPosY, prm.muzzleFlashPosZ);
            }
        }

        return result;
    }

    public boolean isPureHeatSeeker() {
        return "aamissile".equals(getInfo().type)
            && getInfo().isHeatSeekerMissile
            && !getInfo().activeRadar
            && !getInfo().passiveRadar
            && !getInfo().semiActiveRadar
            && !getInfo().antiRadiationMissile;
    }

    private boolean isValidServerTarget(MCH_WeaponParam prm, Entity target) {
        if (prm == null || prm.entity == null || target == null || target.isDead || target == prm.entity || target == prm.user) {
            return false;
        }
        if (!getInfo().antiRadiationMissile && target instanceof MCH_EntityAircraft
            && (getInfo().isRadarMissile || getInfo().activeRadar || getInfo().passiveRadar || getInfo().semiActiveRadar)
            && ((MCH_EntityAircraft)target).isECMJammerUsing()) {
            return false;
        }
        if (!super.guidanceSystem.canLockEntity(target)) {
            return false;
        }
        double maxRange = Math.max(1.0D, getInfo().maxLockOnRange);
        if (requiresBvrRadarTrack(prm)) {
            MCH_EntityAircraft ac = (MCH_EntityAircraft)prm.entity;
            if (!ac.isRadarEnabledRuntime()
                || !MCH_MOD.rwrThreatManager.isEmitterTrackingTarget(ac.getEntityId(), target.getEntityId(),
                    MCH_MOD.rwrThreatManager.getCurrentTick())) {
                return false;
            }
            if (ac.getAcInfo().radarMaxTargetRange > 0.0F) {
                maxRange = Math.min(maxRange, ac.getAcInfo().radarMaxTargetRange);
            }
        }
        return prm.entity.getDistanceSqToEntity(target) <= maxRange * maxRange;
    }

    private boolean requiresBvrRadarTrack(MCH_WeaponParam prm) {
        if (prm == null || !(prm.entity instanceof MCH_EntityAircraft) || getInfo().antiRadiationMissile
            || !(getInfo().activeRadar || getInfo().passiveRadar || getInfo().semiActiveRadar)) {
            return false;
        }
        MCH_EntityAircraft ac = (MCH_EntityAircraft)prm.entity;
        return ac.getAcInfo() != null && ac.getAcInfo().enableBVR && ac.getAcInfo().enableRadar;
    }

    private boolean shouldBlockShotWithoutBvrRadarTrack(MCH_WeaponParam prm) {
        if (!super.worldObj.isRemote || !requiresBvrRadarTrack(prm)) {
            return false;
        }
        if (prm.user == null || prm.user.worldObj == null) {
            return true;
        }
        MCH_EntityAircraft ac = (MCH_EntityAircraft)prm.entity;
        int targetId = ac.isRadarEnabledRuntime()
            ? Math.max(0, MCH_RenderRWR.getRadarTrackingTargetId(ac)) : 0;
        Entity target = targetId > 0 ? prm.user.worldObj.getEntityByID(targetId) : null;
        if (target != null && (target.isDead || !super.guidanceSystem.canLockEntity(target))) {
            target = null;
            targetId = 0;
        }
        MCH_EntityInfo snapshot = targetId > 0 && target == null
            ? MCH_EntityInfoClientTracker.getEntityInfo(targetId) : null;
        if (snapshot != null && System.currentTimeMillis() - snapshot.lastUpdateTime > SNAPSHOT_TARGET_STALE_MS) {
            snapshot = null;
            targetId = 0;
        }
        setClientTarget(prm.user, targetId, target, snapshot);
        if (targetId <= 0 || (target == null && snapshot == null)) {
            sendDenyMessage(prm.user, "weapon.deny.radar_lock_first");
            return true;
        }
        return false;
    }

    private boolean isValidServerHeatSeekerTarget(MCH_WeaponParam prm, Entity target) {
        if (!isValidServerTarget(prm, target)
            || MCH_WeaponGuidanceSystem.isEntityOnGround(target, getInfo().lockMinHeight)) {
            return false;
        }

        float launchYaw = getLaunchYaw(prm);
        float launchPitch = getLaunchPitch(prm);
        Entity cueOrigin = getInfo().enableHMS ? prm.user : prm.entity;
        float cueYaw = getInfo().enableHMS ? prm.user.rotationYaw : launchYaw;
        float cuePitch = getInfo().enableHMS ? prm.user.rotationPitch : launchPitch;
        if (!MCH_WeaponGuidanceSystem.inLockAngle(cueOrigin, cueYaw, cuePitch, target, getInfo().maxLockOnAngle)
            || !MCH_WeaponGuidanceSystem.inLockCone(prm.entity, launchYaw, launchPitch, target,
                (float)getInfo().getEffectiveMaxDegreeOfMissile(0))) {
            return false;
        }

        // Validate LOS from the actual launch/muzzle position. For handheld AAMs the packet
        // handler rebuilds this at shoulder/eye height instead of the player's feet.
        Vec3 from = W_WorldFunc.getWorldVec3(super.worldObj, prm.posX, prm.posY, prm.posZ);
        Vec3 to = W_WorldFunc.getWorldVec3(super.worldObj, target.posX,
            target.posY + (double)(target.height * 0.5F), target.posZ);
        MovingObjectPosition hit = W_WorldFunc.clip(super.worldObj, from, to, false, true, false);
        return hit == null || W_MovingObjectPosition.isHitTypeEntity(hit);
    }

    private float getLaunchYaw(MCH_WeaponParam prm) {
        if (prm.entity instanceof MCH_EntityTank) {
            return prm.rotYaw;
        }
        return getInfo().enableOffAxis
            ? prm.user.rotationYaw + super.fixRotationYaw
            : prm.entity.rotationYaw + super.fixRotationYaw;
    }

    private float getLaunchPitch(MCH_WeaponParam prm) {
        if (prm.entity instanceof MCH_EntityTank) {
            return prm.rotPitch;
        }
        return getInfo().enableOffAxis
            ? prm.user.rotationPitch + super.fixRotationPitch
            : prm.entity.rotationPitch + super.fixRotationPitch;
    }

    private boolean hasIntegratedRadar(MCH_WeaponParam prm) {
        if (!(prm.entity instanceof MCH_EntityAircraft)) {
            return false;
        }
        MCH_EntityAircraft ac = (MCH_EntityAircraft)prm.entity;
        return ac.getAcInfo() != null && ac.getAcInfo().enableRadar && ac.isRadarEnabledRuntime();
    }

    private void setClientTarget(Entity user, int targetId, Entity target, MCH_EntityInfo snapshot) {
        if (targetId <= 0 || (target == null && snapshot == null)) {
            super.optionParameter1 = 0;
        } else {
            super.optionParameter1 = targetId;
        }

        for (MCH_EntityBaseBullet bullet : getShootBullets(worldObj, user, getInfo().maxLockOnRange)) {
            bullet.clientSetTargetEntity(target);
            if (target == null && snapshot != null) {
                double vx = snapshot.posX - snapshot.lastTickPosX;
                double vy = snapshot.posY - snapshot.lastTickPosY;
                double vz = snapshot.posZ - snapshot.lastTickPosZ;
                bullet.setSnapshotTarget(targetId, snapshot.posX, snapshot.posY, snapshot.posZ, vx, vy, vz);
            }
        }
    }

    private boolean updateRadarTargetFromTrack(MCH_WeaponParam prm) {
        if (prm.user == null || !hasIntegratedRadar(prm) || getInfo().antiRadiationMissile
            || !(getInfo().activeRadar || getInfo().passiveRadar || getInfo().semiActiveRadar)) {
            return false;
        }
        MCH_EntityAircraft ac = (MCH_EntityAircraft)prm.entity;
        int targetId = Math.max(0, MCH_RenderRWR.getRadarTrackingTargetId(ac));
        Entity target = targetId > 0 ? prm.user.worldObj.getEntityByID(targetId) : null;
        if (target != null && (target.isDead || !super.guidanceSystem.canLockEntity(target))) {
            target = null;
            targetId = 0;
        }
        MCH_EntityInfo snapshot = targetId > 0 && target == null ? MCH_EntityInfoClientTracker.getEntityInfo(targetId) : null;
        if (snapshot != null && System.currentTimeMillis() - snapshot.lastUpdateTime > SNAPSHOT_TARGET_STALE_MS) {
            snapshot = null;
            targetId = 0;
        }
        setClientTarget(prm.user, targetId, target, snapshot);
        return true;
    }

    private void updateIndependentHeatSeekerTarget(MCH_WeaponParam prm) {
        if (prm == null || prm.user == null || prm.entity == null) {
            clearSnapshotHeatSeekerLock();
            return;
        }

        // Once a far snapshot has started a lock, keep using its stable entity ID so
        // crossing the vanilla entity-render boundary does not reset lock progress.
        if (this.snapshotHeatSeekerTargetId > 0) {
            if (updateSnapshotHeatSeekerLock(prm)) {
                return;
            }
        }

        float launchYaw = getLaunchYaw(prm);
        float launchPitch = getLaunchPitch(prm);
        Entity cueOrigin = getInfo().enableHMS ? prm.user : prm.entity;
        float cueYaw = getInfo().enableHMS ? prm.user.rotationYaw : launchYaw;
        float cuePitch = getInfo().enableHMS ? prm.user.rotationPitch : launchPitch;
        boolean complete = super.guidanceSystem.lock(prm.user, cueOrigin, cueYaw, cuePitch,
            prm.entity, launchYaw, launchPitch, (float)getInfo().getEffectiveMaxDegreeOfMissile(0));
        Entity localTarget = super.guidanceSystem.getLockingEntity();
        if (localTarget != null) {
            this.snapshotHeatSeekerTargetId = 0;
            super.optionParameter1 = complete && super.guidanceSystem.lastLockEntity != null
                ? W_Entity.getEntityId(super.guidanceSystem.lastLockEntity) : 0;
            return;
        }

        // Vanilla's client entity list ends near normal render/tracking distance. Fall
        // back to the sensor-independent snapshots already used by far vehicle LODs.
        if (!acquireSnapshotHeatSeekerTarget(prm, cueOrigin, cueYaw, cuePitch, launchYaw, launchPitch)) {
            super.optionParameter1 = 0;
        }
    }

    private boolean acquireSnapshotHeatSeekerTarget(MCH_WeaponParam prm, Entity cueOrigin,
                                                     float cueYaw, float cuePitch,
                                                     float launchYaw, float launchPitch) {
        MCH_EntityInfo best = null;
        double bestDistanceSq = Double.MAX_VALUE;
        long now = System.currentTimeMillis();

        for (MCH_EntityInfo snapshot : MCH_EntityInfoClientTracker.getAllTrackedEntities()) {
            if (!isSnapshotHeatSeekerTargetUsable(prm, snapshot, now)) {
                continue;
            }
            // If a real entity is present, the normal seeker above owns it (including
            // client LOS and decoy behavior). Snapshots are only for genuinely far targets.
            Entity localEntity = prm.user.worldObj.getEntityByID(snapshot.entityId);
            if (localEntity != null && !localEntity.isDead) {
                continue;
            }

            float acquireAngle = (float)getInfo().maxLockOnAngle;
            if (!inSnapshotLockAngle(cueOrigin, cueYaw, cuePitch, snapshot, acquireAngle)
                || !inSnapshotLockCone(prm.entity, launchYaw, launchPitch, snapshot,
                    (float)getInfo().getEffectiveMaxDegreeOfMissile(0))) {
                continue;
            }

            double distanceSq = snapshot.getDistanceSqToEntity(cueOrigin);
            if (distanceSq < bestDistanceSq) {
                best = snapshot;
                bestDistanceSq = distanceSq;
            }
        }

        if (best == null) {
            return false;
        }

        super.guidanceSystem.clearLock();
        super.guidanceSystem.lastLockEntity = null;
        this.snapshotHeatSeekerTargetId = best.entityId;
        super.guidanceSystem.lockCount = 1;
        super.guidanceSystem.prevLockCount = 0;
        super.optionParameter1 = getSnapshotLockCountMax() <= 1 ? best.entityId : 0;
        return true;
    }

    private boolean updateSnapshotHeatSeekerLock(MCH_WeaponParam prm) {
        MCH_EntityInfo snapshot = MCH_EntityInfoClientTracker.getEntityInfo(this.snapshotHeatSeekerTargetId);
        long now = System.currentTimeMillis();
        if (!isSnapshotHeatSeekerTargetUsable(prm, snapshot, now)) {
            clearSnapshotHeatSeekerLock();
            return false;
        }

        float launchYaw = getLaunchYaw(prm);
        float launchPitch = getLaunchPitch(prm);
        Entity cueOrigin = getInfo().enableHMS ? prm.user : prm.entity;
        float cueYaw = getInfo().enableHMS ? prm.user.rotationYaw : launchYaw;
        float cuePitch = getInfo().enableHMS ? prm.user.rotationPitch : launchPitch;
        boolean inAngles = inSnapshotLockAngle(cueOrigin, cueYaw, cuePitch, snapshot, getInfo().maxLockOnAngle)
            && inSnapshotLockCone(prm.entity, launchYaw, launchPitch, snapshot,
                (float)getInfo().getEffectiveMaxDegreeOfMissile(0));

        int max = getSnapshotLockCountMax();
        if (inAngles) {
            if (super.guidanceSystem.lockCount < max) {
                ++super.guidanceSystem.lockCount;
            }
        } else if (super.guidanceSystem.continueLockCount > 0) {
            --super.guidanceSystem.continueLockCount;
            if (super.guidanceSystem.continueLockCount <= 0 && super.guidanceSystem.lockCount > 0) {
                --super.guidanceSystem.lockCount;
            }
        } else {
            super.guidanceSystem.continueLockCount = 0;
            if (super.guidanceSystem.lockCount > 0) {
                --super.guidanceSystem.lockCount;
            }
        }

        if (super.guidanceSystem.lockCount <= 0) {
            clearSnapshotHeatSeekerLock();
            return false;
        }

        if (super.guidanceSystem.lockCount >= max) {
            super.guidanceSystem.lockCount = max;
            if (super.guidanceSystem.continueLockCount <= 0) {
                super.guidanceSystem.continueLockCount = Math.min(20, Math.max(1, max / 3));
            }
            // Keep GuidanceSystem.update() from treating an unchanged complete lock as stale.
            super.guidanceSystem.prevLockCount = super.guidanceSystem.lockCount - 1;
            super.optionParameter1 = snapshot.entityId;
        } else {
            super.optionParameter1 = 0;
        }
        return true;
    }

    private boolean isSnapshotHeatSeekerTargetUsable(MCH_WeaponParam prm, MCH_EntityInfo snapshot, long now) {
        if (snapshot == null || snapshot.entityId <= 0 || snapshot.entityId == prm.entity.getEntityId()
            || snapshot.destroyed || !MCH_EntityInfoClientTracker.isEntityInLatestSnapshot(snapshot.entityId)
            || now - snapshot.lastUpdateTime > SNAPSHOT_TARGET_STALE_MS) {
            return false;
        }
        if (!isSnapshotAirTarget(snapshot) || Double.isNaN(snapshot.altitudeAboveGround)
            || Double.isInfinite(snapshot.altitudeAboveGround)
            || snapshot.altitudeAboveGround <= (double)getInfo().lockMinHeight) {
            return false;
        }
        // Do not compare WorldInfo names here: multiplayer clients commonly call the
        // remote world "MpServer" while the authoritative server uses its save name.
        // Entity snapshots are already sent per dimension and cleared on world unload.
        if (!isSnapshotVelocityGateValid(prm, snapshot)) {
            return false;
        }

        double range = Math.max(1.0D, getInfo().maxLockOnRange);
        return snapshot.getDistanceSqToEntity(prm.entity) <= range * range;
    }

    private boolean isSnapshotAirTarget(MCH_EntityInfo snapshot) {
        if (snapshot == null || snapshot.entityClassName == null) {
            return false;
        }
        return snapshot.entityClassName.contains("MCP_EntityPlane")
            || snapshot.entityClassName.contains("MCH_EntityHeli");
    }

    private boolean isSnapshotVelocityGateValid(MCH_WeaponParam prm, MCH_EntityInfo snapshot) {
        if (!(prm.entity instanceof MCP_EntityPlane)
            || !MCP_EntityPlane.class.getName().equals(snapshot.entityClassName)) {
            return true;
        }
        double sx = prm.entity.motionX;
        double sy = prm.entity.motionY;
        double sz = prm.entity.motionZ;
        double tx = snapshot.posX - snapshot.lastTickPosX;
        double ty = snapshot.posY - snapshot.lastTickPosY;
        double tz = snapshot.posZ - snapshot.lastTickPosZ;
        double sl = Math.sqrt(sx * sx + sy * sy + sz * sz);
        double tl = Math.sqrt(tx * tx + ty * ty + tz * tz);
        if (sl <= 0.001D || tl <= 0.001D) {
            return true;
        }
        double dot = (sx * tx + sy * ty + sz * tz) / (sl * tl);
        dot = Math.max(-1.0D, Math.min(1.0D, dot));
        double angle = Math.acos(dot);
        if (angle > Math.PI / 2.0D) {
            angle = Math.PI - angle;
        }
        return Math.toDegrees(angle) <= (double)getInfo().pdHDNMaxDegree;
    }

    private int getSnapshotLockCountMax() {
        return Math.max(1, super.guidanceSystem.lockCountMax);
    }

    private static boolean inSnapshotLockAngle(Entity origin, float yaw, float pitch,
                                                MCH_EntityInfo target, float angle) {
        if (origin == null || target == null) {
            return false;
        }
        double dx = target.posX - origin.posX;
        double dy = target.posY + 0.5D - origin.posY;
        double dz = target.posZ - origin.posZ;
        float originYaw = (float)MCH_Lib.getRotate360((double)yaw);
        float targetYaw = (float)MCH_Lib.getRotate360(Math.atan2(dz, dx) * 180.0D / Math.PI);
        float diffYaw = (float)MCH_Lib.getRotate360((double)(targetYaw - originYaw - 90.0F));
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        float targetPitch = -((float)(Math.atan2(dy, horizontal) * 180.0D / Math.PI));
        return (diffYaw < angle || diffYaw > 360.0F - angle) && Math.abs(targetPitch - pitch) < angle;
    }

    private static boolean inSnapshotLockCone(Entity origin, float yaw, float pitch,
                                               MCH_EntityInfo target, float angle) {
        if (origin == null || target == null || angle < 0.0F) {
            return false;
        }
        double dx = target.posX - origin.posX;
        double dy = target.posY + 0.5D - origin.posY;
        double dz = target.posZ - origin.posZ;
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length <= 1.0E-6D) {
            return true;
        }
        double yawRad = Math.toRadians(yaw);
        double pitchRad = Math.toRadians(pitch);
        double lookX = -Math.sin(yawRad) * Math.cos(pitchRad);
        double lookY = -Math.sin(pitchRad);
        double lookZ = Math.cos(yawRad) * Math.cos(pitchRad);
        double dot = (dx * lookX + dy * lookY + dz * lookZ) / length;
        dot = Math.max(-1.0D, Math.min(1.0D, dot));
        return Math.toDegrees(Math.acos(dot)) <= (double)angle;
    }

    private void clearSnapshotHeatSeekerLock() {
        this.snapshotHeatSeekerTargetId = 0;
        super.optionParameter1 = 0;
        super.guidanceSystem.clearLock();
        super.guidanceSystem.lastLockEntity = null;
    }

    private boolean isArmNarrowBandMode() {
        return getInfo().antiRadiationMissile && this.getCurrentMode() == 1;
    }

    private boolean shouldBlockShotByArmBandConstraint(MCH_WeaponParam prm) {
        if (!getInfo().antiRadiationMissile) {
            return false;
        }
        boolean narrowBand = (super.optionParameter2 & OPTION_FLAG_ARM_NARROW_BAND) != 0;
        if (!narrowBand) {
            return false;
        }
        boolean hasTarget = prm.user != null && prm.user.worldObj != null && prm.option1 > 0
            && (prm.user.worldObj.getEntityByID(prm.option1) != null
                || mcheli.MCH_EntityInfoClientTracker.getEntityInfo(prm.option1) != null);
        if (!hasTarget) {
            if (super.worldObj.isRemote) {
                sendDenyMessage(prm.user, "weapon.deny.narrow_band");
            }
            return true;
        }
        return false;
    }

    private boolean shouldBlockShotByDataLink(MCH_WeaponParam prm) {
        super.optionParameter2 &= ~(OPTION_FLAG_DATALINK | OPTION_FLAG_DATALINK_TWS_SELECTED_ONLY);
        if (!(prm.entity instanceof MCH_EntityAircraft) || prm.user == null || !super.worldObj.isRemote) {
            return false;
        }
        if (getInfo().antiRadiationMissile || !(getInfo().activeRadar || getInfo().passiveRadar || getInfo().semiActiveRadar)) {
            return false;
        }
        MCH_EntityAircraft ac = (MCH_EntityAircraft)prm.entity;
        MCH_WeaponSet ws = ac.getCurrentWeapon(prm.user);
        if (ws == null || ws.getInfo() == null || !ws.getInfo().enableDataLink) {
            return false;
        }
        boolean dlMode = ws.getInfo().onlyDataLink || ws.isDataLinkMode();
        if (!dlMode) {
            return false;
        }
        String searchType = MCH_RenderRWR.getRadarSearchType(ac);
        int trackingId = MCH_RenderRWR.getRadarTrackingTargetId(ac);
        int selectedId = MCH_RenderRWR.getRadarSelectedTargetId(ac);
        int targetId = -1;
        boolean twsSelectedOnlyLaunch = false;
        if (getInfo().passiveRadar || getInfo().semiActiveRadar) {
            targetId = trackingId;
            if (targetId <= 0) {
                sendDenyMessage(prm.user, "weapon.deny.lock_first");
                return true;
            }
        } else if (getInfo().activeRadar) {
            boolean srcLike = "SRC".equals(searchType) || "GMTI_SRC".equals(searchType);
            if (srcLike) {
                targetId = trackingId;
            } else {
                targetId = trackingId > 0 ? trackingId : selectedId;
                twsSelectedOnlyLaunch = trackingId <= 0 && selectedId > 0;
            }
            if (targetId <= 0) {
                sendDenyMessage(prm.user, "weapon.deny.select_or_lock");
                return true;
            }
        }
        Entity target = prm.user.worldObj.getEntityByID(targetId);
        if (target != null && !target.isDead) {
            if (!isTargetInMissileFov(prm.user, target)) {
                sendDenyMessage(prm.user, "weapon.deny.lock_first");
                return true;
            }
        } else {
            MCH_EntityInfo snap = MCH_EntityInfoClientTracker.getEntityInfo(targetId);
            if (!isSnapshotTargetUsable(prm.user, snap)) {
                sendDenyMessage(prm.user, "weapon.deny.lock_first");
                return true;
            }
        }
        super.optionParameter1 = targetId;
        super.optionParameter2 |= OPTION_FLAG_DATALINK;
        if (twsSelectedOnlyLaunch) {
            super.optionParameter2 |= OPTION_FLAG_DATALINK_TWS_SELECTED_ONLY;
        } else {
            super.optionParameter2 &= ~OPTION_FLAG_DATALINK_TWS_SELECTED_ONLY;
        }
        return false;
    }

    private boolean isTargetInMissileFov(Entity user, Entity target) {
        if (user == null || target == null) {
            return false;
        }
        Vec3 look = user.getLookVec();
        Vec3 to = Vec3.createVectorHelper(target.posX - user.posX, target.posY + target.height * 0.5D - (user.posY + user.height * 0.5D), target.posZ - user.posZ);
        double len = to.lengthVector();
        if (len <= 1.0E-6D) {
            return false;
        }
        to = to.normalize();
        double dot = look.dotProduct(to);
        dot = Math.max(-1.0D, Math.min(1.0D, dot));
        double angle = Math.acos(dot) * 180.0D / Math.PI;
        return angle <= getInfo().getEffectiveMaxDegreeOfMissile(0);
    }

    private boolean isSnapshotTargetUsable(Entity user, MCH_EntityInfo snap) {
        if (user == null || snap == null) {
            return false;
        }
        if (System.currentTimeMillis() - snap.lastUpdateTime > SNAPSHOT_TARGET_STALE_MS) {
            return false;
        }
        return isTargetInMissileFov(user, snap.posX, snap.posY, snap.posZ);
    }

    private boolean isTargetInMissileFov(Entity user, double targetX, double targetY, double targetZ) {
        if (user == null) {
            return false;
        }
        Vec3 look = user.getLookVec();
        if (look == null) {
            return false;
        }
        Vec3 to = Vec3.createVectorHelper(targetX - user.posX, targetY - (user.posY + user.height * 0.5D), targetZ - user.posZ);
        double len = to.lengthVector();
        if (len <= 1.0E-6D) {
            return false;
        }
        to = to.normalize();
        double dot = look.dotProduct(to);
        dot = Math.max(-1.0D, Math.min(1.0D, dot));
        double angle = Math.acos(dot) * 180.0D / Math.PI;
        return angle <= getInfo().getEffectiveMaxDegreeOfMissile(0);
    }

    private void sendDenyMessage(Entity user, String translationKey) {
        if (user instanceof EntityPlayer) {
            ((EntityPlayer)user).addChatMessage(new ChatComponentTranslation(translationKey));
        }
    }

    @Override
    public int getLockCountMax() {
        if (this.snapshotHeatSeekerTargetId > 0) {
            MCH_EntityInfo snapshot = MCH_EntityInfoClientTracker.getEntityInfo(this.snapshotHeatSeekerTargetId);
            if (snapshot != null) {
                return getSnapshotLockCountMax();
            }
        }
        return super.getLockCountMax();
    }

    @Override
    public boolean lock(MCH_WeaponParam prm) {
        if (!super.worldObj.isRemote) {
            // do nothing
        } else {
            if (requiresBvrRadarTrack(prm)) {
                super.guidanceSystem.clearLock();
                if (!updateRadarTargetFromTrack(prm)) {
                    setClientTarget(prm.user, 0, null, null);
                }
                return false;
            }
            if (updateDataLinkTargetsFromRadar(prm, false)) {
                return false;
            }
            if (updateRadarTargetFromTrack(prm)) {
                return false;
            }
            if (isPureHeatSeeker()) {
                updateIndependentHeatSeekerTarget(prm);
                return false;
            }
            if (getInfo().passiveRadar) {
                if (!super.guidanceSystem.lock(prm.user)) {
//               if(getInfo().enableBVR && tick % 5 == 0) {
//                  Map<Integer, MCH_EntityInfo> map = MCH_RenderBVRLockBox.currentLockedEntities;
//                  if(!map.isEmpty()) {
//                     MCH_EntityInfo info = (MCH_EntityInfo) map.values().toArray()[rand.nextInt(map.size())];
//                     if(info != null) {
//                        MCH_EntityAAMissile.clientTrackingMissile.forEach((K, V) -> {
//                           int mslId = K;
//                           if (V.getInfo() != null && V.getInfo().passiveRadar && V.getInfo().enableBVR) {
//                              MCH_MOD.getPacketHandler().sendToServer(new PacketLockTargetBVR(
//                                      mslId, (int) info.posX, (int) info.posY, (int) info.posZ
//                              ));
//                           }
//                        });
//                     }
//                  }
//               }
                }
                if (guidanceSystem.isLockComplete()) {
                    Entity target = guidanceSystem.lastLockEntity;
                    super.optionParameter1 = W_Entity.getEntityId(target);
                    //获取玩家射击的AA弹
                    for (MCH_EntityBaseBullet bullet : getShootBullets(worldObj, prm.user, getInfo().maxLockOnRange)) {
                        bullet.clientSetTargetEntity(target);
                    }
                } else {
                    super.optionParameter1 = 0;
                    for (MCH_EntityBaseBullet bullet : getShootBullets(worldObj, prm.user, getInfo().maxLockOnRange)) {
                        bullet.clientSetTargetEntity(null);
                    }
                }


            }
        }
        return false; // (keep as-is unless you want non-SARH lock here)
    }


    @Override
    public void onUnlock(MCH_WeaponParam prm) {
        if (worldObj.isRemote) {
            if (requiresBvrRadarTrack(prm)) {
                super.guidanceSystem.clearLock();
                if (!updateRadarTargetFromTrack(prm)) {
                    setClientTarget(prm.user, 0, null, null);
                }
                return;
            }
            if (updateDataLinkTargetsFromRadar(prm, false)) {
                return;
            }
            if (updateRadarTargetFromTrack(prm)) {
                return;
            }
            if (isPureHeatSeeker()) {
                clearSnapshotHeatSeekerLock();
                return;
            }
            if (guidanceSystem != null && prm.user != null) {
                if (!guidanceSystem.isLockComplete()) {
                    super.optionParameter1 = 0;
                    for (MCH_EntityBaseBullet bullet : getShootBullets(worldObj, prm.user, getInfo().maxLockOnRange)) {
                        bullet.clientSetTargetEntity(null);
                    }
                }
            }

            // BVR: disable guidance on unlock
            if (getInfo() != null && getInfo().enableBVR && getInfo().passiveRadar) {
                int[] missileIds = MCH_EntityAAMissile.getClientTrackedBvrMissileIds();
                for (int mslId : missileIds) {
                    MCH_MOD.getPacketHandler().sendToServer(new PacketLockTargetBVR(mslId, 0, 0, -1, 0));
                }
            }
        }
    }

    private boolean updateDataLinkTargetsFromRadar(MCH_WeaponParam prm, boolean forceClear) {
        if (!(prm.entity instanceof MCH_EntityAircraft) || prm.user == null) {
            return false;
        }
        MCH_EntityAircraft ac = (MCH_EntityAircraft)prm.entity;
        MCH_WeaponSet ws = ac.getCurrentWeapon(prm.user);
        if (ws == null || ws.getInfo() == null || !ws.getInfo().enableDataLink || ws.getInfo().antiRadiationMissile
            || (ws.getInfo().isHeatSeekerMissile && !ws.getInfo().activeRadar && !ws.getInfo().passiveRadar && !ws.getInfo().semiActiveRadar)) {
            return false;
        }
        boolean dlMode = ws.getInfo().onlyDataLink || ws.isDataLinkMode();
        if (!dlMode) {
            return false;
        }
        int targetId = 0;
        if (!forceClear) {
            String searchType = MCH_RenderRWR.getRadarSearchType(ac);
            int trackingId = MCH_RenderRWR.getRadarTrackingTargetId(ac);
            int selectedId = MCH_RenderRWR.getRadarSelectedTargetId(ac);
            if (getInfo().passiveRadar || getInfo().semiActiveRadar) {
                targetId = Math.max(0, trackingId);
            } else if (getInfo().activeRadar) {
                boolean srcLike = "SRC".equals(searchType) || "GMTI_SRC".equals(searchType);
                targetId = srcLike ? Math.max(0, trackingId) : Math.max(0, trackingId > 0 ? trackingId : selectedId);
            }
        }
        Entity target = targetId > 0 ? prm.user.worldObj.getEntityByID(targetId) : null;
        MCH_EntityInfo snapshot = targetId > 0 && target == null ? MCH_EntityInfoClientTracker.getEntityInfo(targetId) : null;
        setClientTarget(prm.user, targetId, target, snapshot);
        return true;
    }
}
