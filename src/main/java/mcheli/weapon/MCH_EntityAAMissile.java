package mcheli.weapon;

import mcheli.MCH_RadarDebug;
import net.minecraft.entity.Entity;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

public class MCH_EntityAAMissile extends MCH_EntityBaseBullet implements MCH_IEntityLockChecker, MCH_IMissile {

    private static final int DL_RELAY_LOST_GRACE_TICK = 15;
    public boolean passiveRadarBVRLocking = false;
    public int passiveRadarBVRLockingPosX = 0;
    public int passiveRadarBVRLockingPosY = 0;
    public int passiveRadarBVRLockingPosZ = 0;
    private int dlRelayLostTick = 0;

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
            boolean dlRelay = this.isDataLinkRelayMode();
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
                } else if ((getInfo().activeRadar || getInfo().passiveRadar || getInfo().semiActiveRadar) && ticksExisted % getInfo().scanInterval == 0) {
                    this.dlRelayLostTick = 0;
                    scanForTargets();
                }
            }
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
