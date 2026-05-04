package mcheli.weapon;

import mcheli.MCH_RadarDebug;
import mcheli.aircraft.MCH_EntityAircraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

public class MCH_EntityATMissile extends MCH_EntityBaseBullet implements MCH_IEntityLockChecker, MCH_IMissile {

    private static final int ARM_STATE_HOMING = 1;
    private static final int ARM_STATE_MEMORY = 2;
    private static final int ARM_STATE_LOST = 3;
    public int guidanceType = 0;
    private int armGuidanceState = ARM_STATE_HOMING;
    private int armLastRadiationSeenTick = -1;


    public MCH_EntityATMissile(World par1World) {
        super(par1World);
        super.targetEntity = null;
    }

    public MCH_EntityATMissile(World par1World, double posX, double posY, double posZ, double targetX, double targetY, double targetZ, float yaw, float pitch, double acceleration) {
        super(par1World, posX, posY, posZ, targetX, targetY, targetZ, yaw, pitch, acceleration);
    }

//   public void onUpdate() {
//      super.onUpdate();
//      if(this.getInfo() != null && !this.getInfo().disableSmoke && super.ticksExisted >= this.getInfo().trajectoryParticleStartTick) {
//         this.spawnExplosionParticle(this.getInfo().trajectoryParticleName, 3, 5.0F * this.getInfo().smokeSize * 0.5F);
//      }
//
//      if(!super.worldObj.isRemote) {
//         if(super.shootingEntity != null && super.targetEntity != null && !super.targetEntity.isDead) {
//            this.onUpdateMotion();
//         } else {
//            //this.setDead();
//         }
//      }
//
//      double a = (double)((float)Math.atan2(super.motionZ, super.motionX));
//      super.rotationYaw = (float)(a * 180.0D / 3.141592653589793D) - 90.0F;
//      double r = Math.sqrt(super.motionX * super.motionX + super.motionZ * super.motionZ);
//      super.rotationPitch = -((float)(Math.atan2(super.motionY, r) * 180.0D / 3.141592653589793D));
//   }
//
//   public void onUpdateMotion() {
//      double x = super.targetEntity.posX - super.posX;
//      double y = super.targetEntity.posY - super.posY;
//      double z = super.targetEntity.posZ - super.posZ;
//      double d = x * x + y * y + z * z;
//      if(d <= 2250000.0D && !super.targetEntity.isDead) {
//         if(this.getInfo().proximityFuseDist >= 0.1F && d < (double)this.getInfo().proximityFuseDist) {
//            MovingObjectPosition var11 = new MovingObjectPosition(super.targetEntity);
//            var11.entityHit = null;
//            this.onImpact(var11, 1.0F);
//         } else {
//            int rigidityTime = this.getInfo().rigidityTime;
//            float af = this.getCountOnUpdate() < rigidityTime + this.getInfo().trajectoryParticleStartTick?0.5F:1.0F;
//            if(this.getCountOnUpdate() > rigidityTime) {
//               if(this.guidanceType == 1) {
//                  if(this.getCountOnUpdate() <= rigidityTime + 20) {
//                     this.guidanceToTarget(super.targetEntity.posX, super.shootingEntity.posY + 150.0D, super.targetEntity.posZ, af);
//                  } else if(this.getCountOnUpdate() <= rigidityTime + 30) {
//                     this.guidanceToTarget(super.targetEntity.posX, super.shootingEntity.posY, super.targetEntity.posZ, af);
//                  } else {
//                     if(this.getCountOnUpdate() == rigidityTime + 35) {
//                        this.setPower((int)((float)this.getPower() * 1.2F));
//                        if(super.explosionPower > 0) {
//                           ++super.explosionPower;
//                        }
//                     }
//
//                     this.guidanceToTarget(super.targetEntity.posX, super.targetEntity.posY, super.targetEntity.posZ, af);
//                  }
//               } else {
//                  d = (double)MathHelper.sqrt_double(d);
//                  super.motionX = x * super.acceleration / d * (double)af;
//                  super.motionY = y * super.acceleration / d * (double)af;
//                  super.motionZ = z * super.acceleration / d * (double)af;
//               }
//            }
//         }
//      } else {
//         //this.setDead();
//      }
//
//   }

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
                double x = super.posX - super.targetEntity.posX;
                double y = super.posY - super.targetEntity.posY;
                double z = super.posZ - super.targetEntity.posZ;
                double d = x * x + y * y + z * z;
                if (d > 3422500.0D) {
                    if (MCH_RadarDebug.isEnabled()) {
                        MCH_RadarDebug.trace(this.worldObj, this,
                            "msl_death type=AT reason=TARGET_DISTANCE_LIMIT msl=%d target=%d dist=%.1f distSq=%.1f limitSq=3422500.0 pos=(%.1f,%.1f,%.1f) tpos=(%.1f,%.1f,%.1f)",
                            this.getEntityId(),
                            super.targetEntity.getEntityId(),
                            Math.sqrt(d), d,
                            this.posX, this.posY, this.posZ,
                            super.targetEntity.posX, super.targetEntity.posY, super.targetEntity.posZ);
                    }
                    this.setDead();
                } else if (this.getCountOnUpdate() > this.getInfo().rigidityTime) {

                    //攻顶导弹逻辑
                    if (this.guidanceType == 1) {
                        float af = this.getCountOnUpdate() < getInfo().rigidityTime + getInfo().trajectoryParticleStartTick ? 0.5F : 1.0F;
                        //攻顶向上运动
                        if (this.getCountOnUpdate() <= getInfo().rigidityTime + 20) {
                            doingTopAttack = true;
                            this.guidanceToTarget(super.targetEntity.posX, super.shootingEntity.posY + 100.0D, super.targetEntity.posZ, af);
                        } else if (this.getCountOnUpdate() <= getInfo().rigidityTime + 30) {
                            this.guidanceToTarget(super.targetEntity.posX, super.shootingEntity.posY, super.targetEntity.posZ, af);
                        } else {
                            if (this.getCountOnUpdate() == getInfo().rigidityTime + 35) {
                                this.setPower((int) ((float) this.getPower() * 1.2F));
                                if (super.explosionPower > 0) {
                                    ++super.explosionPower;
                                }
                            }
                            doingTopAttack = false;
                            this.guidanceToTarget(super.targetEntity.posX, super.targetEntity.posY, super.targetEntity.posZ, af);
                        }
                    }

                    //非攻顶
                    else {
                        if (dlRelay && getInfo().armCruiseEnable) {
                            this.guidanceToPosWithCruise(super.targetEntity.posX, super.targetEntity.posY, super.targetEntity.posZ);
                        } else {
                            this.guidanceToTarget(super.targetEntity.posX, super.targetEntity.posY, super.targetEntity.posZ);
                        }
                    }
                }
            } else {
                if (dlRelay) {
                    if (getInfo().passiveRadar || getInfo().semiActiveRadar) {
                        this.setDataLinkRelayMode(false);
                        this.setActiveRadarCaptured(false);
                    } else if (getInfo().activeRadar) {
                        if (this.isDataLinkActiveRadarDelayPhase()) {
                            this.setActiveRadarCaptured(false);
                        } else if (this.isActiveRadarCaptured() && ticksExisted % getInfo().scanInterval == 0) {
                            scanForTargets();
                        }
                    }
                } else if ((getInfo().activeRadar || getInfo().passiveRadar || getInfo().semiActiveRadar)
                    && ticksExisted % getInfo().scanInterval == 0) {
                    if ((getInfo().passiveRadar || getInfo().semiActiveRadar) && this.wasDataLinkRelayEverEnabled()) {
                        return;
                    }
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

    private void guideAtToPosition(double tx, double ty, double tz) {
        if (armHojCepActive) {
            tx += (super.rand.nextDouble() - 0.5D) * 14.0D;
            tz += (super.rand.nextDouble() - 0.5D) * 14.0D;
        }
        if (shouldUseCruise(tx, ty, tz)) {
            this.guidanceToPos(tx, this.posY, tz);
            return;
        }
        if (this.guidanceType == 1 && super.shootingEntity != null) {
            float af = this.getCountOnUpdate() < getInfo().rigidityTime + getInfo().trajectoryParticleStartTick ? 0.5F : 1.0F;
            if (this.getCountOnUpdate() <= getInfo().rigidityTime + 20) {
                doingTopAttack = true;
                this.guidanceToTarget(tx, super.shootingEntity.posY + 100.0D, tz, af);
            } else if (this.getCountOnUpdate() <= getInfo().rigidityTime + 30) {
                this.guidanceToTarget(tx, super.shootingEntity.posY, tz, af);
            } else {
                if (this.getCountOnUpdate() == getInfo().rigidityTime + 35) {
                    this.setPower((int) ((float) this.getPower() * 1.2F));
                    if (super.explosionPower > 0) {
                        ++super.explosionPower;
                    }
                }
                doingTopAttack = false;
                this.guidanceToTarget(tx, ty, tz, af);
            }
        } else {
            this.guidanceToPos(tx, ty, tz);
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
                        "msl_death type=AT_ARM reason=TARGET_DISTANCE_LIMIT msl=%d target=%d dist=%.1f distSq=%.1f limitSq=3422500.0 pos=(%.1f,%.1f,%.1f) tpos=(%.1f,%.1f,%.1f)",
                        this.getEntityId(),
                        super.targetEntity.getEntityId(),
                        Math.sqrt(d), d,
                        this.posX, this.posY, this.posZ,
                        super.targetEntity.posX, super.targetEntity.posY, super.targetEntity.posZ);
                }
                this.setDead();
                return;
            }
            if (this.getCountOnUpdate() > this.getInfo().rigidityTime) {
                guideAtToPosition(super.targetEntity.posX, super.targetEntity.posY, super.targetEntity.posZ);
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
            if (this.getCountOnUpdate() > this.getInfo().rigidityTime) {
                guideAtToPosition(super.targetEntity.posX, super.targetEntity.posY, super.targetEntity.posZ);
            }
            return;
        }

        int lostTick = this.armLastRadiationSeenTick < 0 ? Integer.MAX_VALUE : this.ticksExisted - this.armLastRadiationSeenTick;
        int armGraceTick = Math.max(0, getInfo().armEmitterLostGraceTick);
        int armMemoryTick = Math.max(0, getInfo().armMemoryTimeTick);
        if (this.hasLastKnownTarget && lostTick <= armGraceTick + armMemoryTick) {
            this.armGuidanceState = ARM_STATE_MEMORY;
            if (this.getCountOnUpdate() > this.getInfo().rigidityTime) {
                // Memory phase keeps flying to the last radiating coordinate only.
                guideAtToPosition(this.lastTargetPosX, this.lastTargetPosY, this.lastTargetPosZ);
            }
            return;
        }

        this.armGuidanceState = ARM_STATE_LOST;
        if (super.targetEntity != null) {
            this.setTargetEntity(null);
        }
    }

    public MCH_BulletModel getDefaultBulletModel() {
        return MCH_DefaultBulletModels.ATMissile;
    }

    @Override
    public boolean canLockEntity(Entity var1) {
        return false;
    }
}
