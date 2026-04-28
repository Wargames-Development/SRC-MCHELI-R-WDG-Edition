package mcheli.weapon;

import mcheli.MCH_RadarDebug;
import mcheli.aircraft.MCH_EntityAircraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

public class MCH_EntityAAMissile extends MCH_EntityBaseBullet implements MCH_IEntityLockChecker, MCH_IMissile {

    private static final int DL_RELAY_LOST_GRACE_TICK = 15;
    private static final int ARM_STATE_HOMING = 1;
    private static final int ARM_STATE_MEMORY = 2;
    private static final int ARM_STATE_LOST = 3;
    public boolean passiveRadarBVRLocking = false;
    public int passiveRadarBVRLockingPosX = 0;
    public int passiveRadarBVRLockingPosY = 0;
    public int passiveRadarBVRLockingPosZ = 0;
    private int dlRelayLostTick = 0;
    private int armGuidanceState = ARM_STATE_HOMING;
    private int armLastRadiationSeenTick = -1;

    public MCH_EntityAAMissile(World par1World) {
        super(par1World);
        super.targetEntity = null;
    }

    public MCH_EntityAAMissile(World par1World, double posX, double posY, double posZ, double targetX, double targetY, double targetZ, float yaw, float pitch, double acceleration) {
        super(par1World, posX, posY, posZ, targetX, targetY, targetZ, yaw, pitch, acceleration);
    }

    @Override
    public void onUpdate() {
        super.onUpdate();

        if (this.getCountOnUpdate() > 4 && this.getInfo() != null && !this.getInfo().disableSmoke && this.isWithinTrajectoryParticleEndTick()) {
            this.spawnExplosionParticle(this.getInfo().trajectoryParticleName, 3, 7.0F * this.getInfo().smokeSize * 0.5F);
        }

        if (!worldObj.isRemote && this.getInfo() != null) {
            if (this.getInfo().antiRadiationMissile) {
                this.onUpdateArmGuidance();
                return;
            }
            boolean dlRelay = this.isDataLinkRelayMode();
            if (super.shootingEntity != null && super.targetEntity != null && !super.targetEntity.isDead) {
                if (dlRelay && (getInfo().passiveRadar || getInfo().semiActiveRadar) && !this.isDataLinkRelaySourceMaintained()) {
                    this.setTargetEntity(null);
                }
            }
            if (super.shootingEntity != null && super.targetEntity != null && !super.targetEntity.isDead) {
                if (dlRelay && this.dlRelayLostTick > 0) {
                    this.dlRelayLostTick = 0;
                }
                // Save last known target info in case we lose it.
                this.lastTargetPosX = super.targetEntity.posX;
                this.lastTargetPosY = super.targetEntity.posY;
                this.lastTargetPosZ = super.targetEntity.posZ;
                this.lastTargetVelX = super.targetEntity.motionX;
                this.lastTargetVelY = super.targetEntity.motionY;
                this.lastTargetVelZ = super.targetEntity.motionZ;
                this.hasLastKnownTarget = true;

                double x = super.posX - super.targetEntity.posX;
                double y = super.posY - super.targetEntity.posY;
                double z = super.posZ - super.targetEntity.posZ;
                double d = x * x + y * y + z * z;

                if (d > 3422500.0D) {
                    if (MCH_RadarDebug.isEnabled()) {
                        MCH_RadarDebug.trace(this.worldObj, this,
                            "msl_death type=AA reason=TARGET_DISTANCE_LIMIT msl=%d target=%d dist=%.1f distSq=%.1f limitSq=3422500.0 pos=(%.1f,%.1f,%.1f) tpos=(%.1f,%.1f,%.1f)",
                            this.getEntityId(),
                            super.targetEntity.getEntityId(),
                            Math.sqrt(d), d,
                            this.posX, this.posY, this.posZ,
                            super.targetEntity.posX, super.targetEntity.posY, super.targetEntity.posZ);
                    }
                    setDead();
                } else if (getCountOnUpdate() > getInfo().rigidityTime) {
                    guidanceToTarget(super.targetEntity.posX, super.targetEntity.posY, super.targetEntity.posZ);
                }
            } else {
                if (dlRelay) {
                    // Passive/semi-active datalink missiles lose relay => inertial flight (no autonomous reacquire).
                    if (getInfo().passiveRadar || getInfo().semiActiveRadar) {
                        this.dlRelayLostTick++;
                        if (this.dlRelayLostTick <= DL_RELAY_LOST_GRACE_TICK && this.hasLastKnownTarget) {
                            // Use last known target position + simple prediction during grace period.
                            double time = this.dlRelayLostTick;
                            double tx = this.lastTargetPosX + this.lastTargetVelX * time;
                            double ty = this.lastTargetPosY + this.lastTargetVelY * time;
                            double tz = this.lastTargetPosZ + this.lastTargetVelZ * time;
                            if (getCountOnUpdate() > getInfo().rigidityTime) {
                                guidanceToPos(tx, ty, tz);
                            }
                        } else if (this.dlRelayLostTick > DL_RELAY_LOST_GRACE_TICK) {
                            this.setDataLinkRelayMode(false);
                            this.setActiveRadarCaptured(false);
                            if (MCH_RadarDebug.isEnabled()) {
                                MCH_RadarDebug.trace(this.worldObj, this,
                                    "dl semi/passive relay timeout msl=%d grace=%d",
                                    this.getEntityId(), DL_RELAY_LOST_GRACE_TICK);
                            }
                        } else if (MCH_RadarDebug.isVerbose() && this.dlRelayLostTick == 1) {
                            MCH_RadarDebug.trace(this.worldObj, this,
                                "dl semi/passive relay grace start msl=%d grace=%d",
                                this.getEntityId(), DL_RELAY_LOST_GRACE_TICK);
                        }
                    } else if (getInfo().activeRadar) {
                        this.dlRelayLostTick = 0;
                        if (this.isDataLinkActiveRadarDelayPhase()) {
                            this.setActiveRadarCaptured(false);
                        } else if (this.isActiveRadarCaptured() && ticksExisted % getInfo().scanInterval == 0) {
                            // Active radar missile: autonomous scan starts only after onboard seeker capture phase.
                            scanForTargets();
                        }
                    }
                } else if (this.isSnapshotTargetUsable(3000L)) {
                    // Use snapshot fallback if entity is missing but snapshot is fresh (3 seconds).
                    double time = (System.currentTimeMillis() - this.snapshotLastUpdate) / 50.0;
                    double tx = this.snapshotPosX + this.snapshotVelX * time;
                    double ty = this.snapshotPosY + this.snapshotVelY * time;
                    double tz = this.snapshotPosZ + this.snapshotVelZ * time;
                    if (getCountOnUpdate() > getInfo().rigidityTime) {
                        guidanceToPos(tx, ty, tz);
                    }
                } else if ((getInfo().activeRadar || getInfo().passiveRadar || getInfo().semiActiveRadar)
                    && ticksExisted % getInfo().scanInterval == 0) {
                    if ((getInfo().passiveRadar || getInfo().semiActiveRadar) && this.wasDataLinkRelayEverEnabled()) {
                        return;
                    }
                    this.dlRelayLostTick = 0;
                    scanForTargets();
                }
            }
        }
    }

    private boolean isArmEmitterRadiating(Entity target) {
        if (!(target instanceof MCH_EntityAircraft)) {
            return false;
        }
        MCH_EntityAircraft ac = (MCH_EntityAircraft) target;
        return isArmEmitterRadiatingSource(ac);
    }

    private void saveArmLastKnownFromTarget(Entity target) {
        this.lastTargetPosX = target.posX;
        this.lastTargetPosY = target.posY;
        this.lastTargetPosZ = target.posZ;
        this.lastTargetVelX = target.motionX;
        this.lastTargetVelY = target.motionY;
        this.lastTargetVelZ = target.motionZ;
        this.hasLastKnownTarget = true;
    }

    private boolean shouldUseArmCruise(double tx, double ty, double tz) {
        if (!getInfo().armCruiseEnable) {
            return false;
        }
        double dx = tx - this.posX;
        double dy = ty - this.posY;
        double dz = tz - this.posZ;
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (dist <= getInfo().armCruiseStartDistance) {
            return false;
        }
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        return !(horizontal <= getInfo().armCruiseTerminalRadius && Math.abs(dy) <= getInfo().armCruiseTerminalHeight);
    }

    private void guideArmToPosition(double tx, double ty, double tz) {
        if (shouldUseArmCruise(tx, ty, tz)) {
            // Cruise segment: hold altitude and only perform horizontal steering.
            guidanceToPos(tx, this.posY, tz);
        } else {
            guidanceToPos(tx, ty, tz);
        }
    }

    private void onUpdateArmGuidance() {
        boolean hasValidTarget = super.shootingEntity != null && super.targetEntity != null && !super.targetEntity.isDead;
        if (hasValidTarget && isArmEmitterRadiating(super.targetEntity)) {
            this.armGuidanceState = ARM_STATE_HOMING;
            this.armLastRadiationSeenTick = this.ticksExisted;
            saveArmLastKnownFromTarget(super.targetEntity);
            double x = super.posX - super.targetEntity.posX;
            double y = super.posY - super.targetEntity.posY;
            double z = super.posZ - super.targetEntity.posZ;
            double d = x * x + y * y + z * z;
            if (d > 3422500.0D) {
                if (MCH_RadarDebug.isEnabled()) {
                    MCH_RadarDebug.trace(this.worldObj, this,
                        "msl_death type=AA_ARM reason=TARGET_DISTANCE_LIMIT msl=%d target=%d dist=%.1f distSq=%.1f limitSq=3422500.0 pos=(%.1f,%.1f,%.1f) tpos=(%.1f,%.1f,%.1f)",
                        this.getEntityId(),
                        super.targetEntity.getEntityId(),
                        Math.sqrt(d), d,
                        this.posX, this.posY, this.posZ,
                        super.targetEntity.posX, super.targetEntity.posY, super.targetEntity.posZ);
                }
                setDead();
                return;
            }
            if (getCountOnUpdate() > getInfo().rigidityTime) {
                guideArmToPosition(super.targetEntity.posX, super.targetEntity.posY, super.targetEntity.posZ);
            }
            return;
        }

        if (super.targetEntity != null && !isArmEmitterRadiating(super.targetEntity)) {
            this.setTargetEntity(null);
        }
        if (super.targetEntity == null) {
            // ARM seeker reacquires radiation source continuously (not gated by scanInterval).
            scanForTargets();
        }
        hasValidTarget = super.shootingEntity != null && super.targetEntity != null && !super.targetEntity.isDead;
        if (hasValidTarget && isArmEmitterRadiating(super.targetEntity)) {
            this.armGuidanceState = ARM_STATE_HOMING;
            this.armLastRadiationSeenTick = this.ticksExisted;
            saveArmLastKnownFromTarget(super.targetEntity);
            if (getCountOnUpdate() > getInfo().rigidityTime) {
                guideArmToPosition(super.targetEntity.posX, super.targetEntity.posY, super.targetEntity.posZ);
            }
            return;
        }

        int lostTick = this.armLastRadiationSeenTick < 0 ? Integer.MAX_VALUE : this.ticksExisted - this.armLastRadiationSeenTick;
        int armGraceTick = Math.max(0, getInfo().armEmitterLostGraceTick);
        int armMemoryTick = Math.max(0, getInfo().armMemoryTimeTick);
        if (this.hasLastKnownTarget && lostTick <= armGraceTick + armMemoryTick) {
            this.armGuidanceState = ARM_STATE_MEMORY;
            if (getCountOnUpdate() > getInfo().rigidityTime) {
                // Memory phase keeps flying to the last radiating coordinate only.
                guideArmToPosition(this.lastTargetPosX, this.lastTargetPosY, this.lastTargetPosZ);
            }
            return;
        }

        this.armGuidanceState = ARM_STATE_LOST;
        if (super.targetEntity != null) {
            this.setTargetEntity(null);
        }
    }


    public MCH_BulletModel getDefaultBulletModel() {
        return MCH_DefaultBulletModels.AAMissile;
    }

    @Override
    public boolean canLockEntity(Entity var1) {
        return false;
    }
}
