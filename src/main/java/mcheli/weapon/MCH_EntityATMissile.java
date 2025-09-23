package mcheli.weapon;

import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.vector.Vector3f;
import mcheli.wrapper.W_Entity;
import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

import java.util.List;

public class MCH_EntityATMissile extends MCH_EntityBaseBullet {

    public int guidanceType = 0;


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
        if (this.getCountOnUpdate() > 4 && this.getInfo() != null && !this.getInfo().disableSmoke) {
            this.spawnExplosionParticle(this.getInfo().trajectoryParticleName, 3, 7.0F * this.getInfo().smokeSize * 0.5F);
        }

        if (!super.worldObj.isRemote && this.getInfo() != null) {
            if (super.shootingEntity != null && super.targetEntity != null && !super.targetEntity.isDead) {
                double x = super.posX - super.targetEntity.posX;
                double y = super.posY - super.targetEntity.posY;
                double z = super.posZ - super.targetEntity.posZ;
                double d = x * x + y * y + z * z;
                if (d > 3422500.0D) {
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
                        if (this.getInfo().proximityFuseDist >= 0.1F && d < (double) this.getInfo().proximityFuseDist) {
                            MovingObjectPosition mop = new MovingObjectPosition(super.targetEntity);
                            super.posX = (super.targetEntity.posX + super.posX) / 2.0D;
                            super.posY = (super.targetEntity.posY + super.posY) / 2.0D;
                            super.posZ = (super.targetEntity.posZ + super.posZ) / 2.0D;
                            this.onImpact(mop, 1.0F);
                        } else {
                            this.guidanceToTarget(super.targetEntity.posX, super.targetEntity.posY, super.targetEntity.posZ);
                        }
                    }
                }
            } else {
                if(getInfo().activeRadar && ticksExisted % getInfo().scanInterval == 0) {
                    scanForTargets();
                }
            }
        }

    }

    public MCH_BulletModel getDefaultBulletModel() {
        return MCH_DefaultBulletModels.ATMissile;
    }
}
