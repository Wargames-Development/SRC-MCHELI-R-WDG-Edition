package mcheli.weapon;

import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.wrapper.W_Entity;
import net.minecraft.entity.Entity;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public class MCH_EntityASMissile extends MCH_EntityBaseBullet implements MCH_IEntityLockChecker, MCH_IMissile {

    public double targetPosX;
    public double targetPosY;
    public double targetPosZ;
    public double originTargetPosX;
    public double originTargetPosY;
    public double originTargetPosZ;
    public boolean targeting;
    public boolean cruiseMode;
    public boolean gpsGuidanceReleased;
    public double launchPosX;
    public double launchPosY;
    public double launchPosZ;
    public boolean launchPosInitialized;
    private double ballisticProgress;

    public MCH_EntityASMissile(World par1World) {
        super(par1World);
        this.targetPosX = 0.0D;
        this.targetPosY = 0.0D;
        this.targetPosZ = 0.0D;
        this.gpsGuidanceReleased = false;
        this.launchPosInitialized = false;
        this.ballisticProgress = 0.0D;
    }

    public MCH_EntityASMissile(World par1World, double posX, double posY, double posZ, double targetX, double targetY, double targetZ, float yaw, float pitch, double acceleration) {
        super(par1World, posX, posY, posZ, targetX, targetY, targetZ, yaw, pitch, acceleration);
        this.launchPosX = posX;
        this.launchPosY = posY;
        this.launchPosZ = posZ;
        this.launchPosInitialized = true;
    }

    public float getGravity() {
        return this.getBomblet() == 1 ? -0.03F : super.getGravity();
    }

    public float getGravityInWater() {
        return this.getBomblet() == 1 ? -0.03F : super.getGravityInWater();
    }

    public void onUpdate() {
        super.onUpdate();
        if (!this.launchPosInitialized) {
            this.launchPosX = this.posX;
            this.launchPosY = this.posY;
            this.launchPosZ = this.posZ;
            this.launchPosInitialized = true;
        }
        this.onUpdateBomblet();
        if (this.getInfo() != null && !this.getInfo().disableSmoke && this.isWithinTrajectoryParticleEndTick()) {
            this.spawnExplosionParticle(this.getInfo().trajectoryParticleName, 3, 5.0F * this.getInfo().smokeSize * 0.5F);
        }

        if (super.shootingEntity != null) {
            double x = super.posX - super.shootingEntity.posX;
            double y = super.posY - super.shootingEntity.posY;
            double z = super.posZ - super.shootingEntity.posZ;
            if (x * x + y * y + z * z > 2000 * 2000.0D) {
                this.setDead();
            }

            if (!super.worldObj.isRemote && !super.isDead && targeting && this.getCountOnUpdate() > this.getInfo().rigidityTime) {
                if (!gpsGuidanceReleased && getInfo().isGPSMissile && !getInfo().lockEntity) {
                    double dx = originTargetPosX - this.posX;
                    double dy = originTargetPosY - this.posY;
                    double dz = originTargetPosZ - this.posZ;
                    if (dx * dx + dy * dy + dz * dz <= 25.0D) {
                        gpsGuidanceReleased = true;
                        targeting = false;
                    }
                }
                if (gpsGuidanceReleased) {
                    return;
                }
                if (getInfo().lockEntity) {
                    int range = getInfo().maxLockOnRange;
                    for (Entity entity : super.worldObj.getEntitiesWithinAABBExcludingEntity(this, super.boundingBox.expand(100, 100, 100))) {
                        if(entity instanceof MCH_EntityAircraft && !W_Entity.isEqual(entity, shootingAircraft)){
                            double d0 = entity.posX - originTargetPosX;
                            double d1 = entity.posY - originTargetPosY;
                            double d2 = entity.posZ - originTargetPosZ;
                            if (d0 * d0 + d1 * d1 + d2 * d2 <= range * range) {
                                targetPosX = entity.posX;
                                targetPosY = entity.posY;
                                targetPosZ = entity.posZ;
                            }
                        }
                    }
                    Vec3 aim = computeBallisticAimPoint(targetPosX, targetPosY, targetPosZ);
                    if (this.cruiseMode) {
                        guidanceToPosWithCruise(aim.xCoord, aim.yCoord, aim.zCoord);
                    } else {
                        guidanceToPos(aim.xCoord, aim.yCoord, aim.zCoord);
                    }
                } else {
                    Vec3 aim = computeBallisticAimPoint(originTargetPosX, originTargetPosY, originTargetPosZ);
                    if (this.cruiseMode) {
                        guidanceToPosWithCruise(aim.xCoord, aim.yCoord, aim.zCoord);
                    } else {
                        guidanceToPos(aim.xCoord, aim.yCoord, aim.zCoord);
                    }
                }
            }
        } else if (!super.worldObj.isRemote) {
            this.setDead();
        }

    }

    public void sprinkleBomblet() {
        if (!super.worldObj.isRemote) {
            MCH_EntityASMissile e = new MCH_EntityASMissile(super.worldObj, super.posX, super.posY, super.posZ, super.motionX, super.motionY, super.motionZ, (float) super.rand.nextInt(360), 0.0F, super.acceleration);
            e.setParameterFromWeapon(this, super.shootingAircraft, super.shootingEntity);
            e.setInfoByName(this.getName());
            float MOTION = 0.5F;
            float RANDOM = this.getInfo().bombletDiff;
            e.motionX = super.motionX * 0.5D + (double) ((super.rand.nextFloat() - 0.5F) * RANDOM);
            e.motionY = super.motionY * 0.5D / 2.0D + (double) ((super.rand.nextFloat() - 0.5F) * RANDOM / 2.0F);
            e.motionZ = super.motionZ * 0.5D + (double) ((super.rand.nextFloat() - 0.5F) * RANDOM);
            e.setBomblet();
            super.worldObj.spawnEntityInWorld(e);
        }

    }

    public MCH_BulletModel getDefaultBulletModel() {
        return MCH_DefaultBulletModels.ASMissile;
    }

    @Override
    public boolean canLockEntity(Entity var1) {
        return false;
    }

    private Vec3 computeBallisticAimPoint(double targetX, double targetY, double targetZ) {
        MCH_WeaponInfo info = getInfo();
        if (info == null || !info.isGPSMissile || !info.ballisticMissile || !launchPosInitialized) {
            return Vec3.createVectorHelper(targetX, targetY, targetZ);
        }
        double totalX = targetX - launchPosX;
        double totalY = targetY - launchPosY;
        double totalZ = targetZ - launchPosZ;
        double totalDistSq = totalX * totalX + totalY * totalY + totalZ * totalZ;
        if (totalDistSq < 1.0E-6D) {
            return Vec3.createVectorHelper(targetX, targetY, targetZ);
        }
        double horizontalTotal = Math.sqrt(totalX * totalX + totalZ * totalZ);
        if (horizontalTotal < info.ballisticMinDistance) {
            return Vec3.createVectorHelper(targetX, targetY, targetZ);
        }
        double horizontalTotalSq = totalX * totalX + totalZ * totalZ;
        if (horizontalTotalSq < 1.0E-6D) {
            return Vec3.createVectorHelper(targetX, targetY, targetZ);
        }
        // Use ground-track progress and keep it monotonic, so launch attitude does not shift final aim point.
        double relX = posX - launchPosX;
        double relZ = posZ - launchPosZ;
        double uRaw = (relX * totalX + relZ * totalZ) / horizontalTotalSq;
        double u = clamp(uRaw, 0.0D, 1.0D);
        if (u < ballisticProgress) {
            u = ballisticProgress;
        } else {
            ballisticProgress = u;
        }
        double toTargetXNow = targetX - posX;
        double toTargetYNow = targetY - posY;
        double toTargetZNow = targetZ - posZ;
        double distanceToTarget = Math.sqrt(toTargetXNow * toTargetXNow + toTargetYNow * toTargetYNow + toTargetZNow * toTargetZNow);
        double arcHeight = Math.max(info.ballisticArcMinHeight, horizontalTotal * info.ballisticArcFactor);
        arcHeight = Math.min(arcHeight, info.ballisticArcMaxHeight);
        // Keep horizontal guidance anchored to target so missile never turns back to launch point.
        double aimX = targetX;
        double aimY = targetY + 4.0D * u * (1.0D - u) * arcHeight;
        double aimZ = targetZ;

        boolean weaveEnabled = info.ballisticLateralSine
            && info.ballisticLateralAmplitude > 0.0D
            && info.ballisticLateralWaves > 0.0D
            && info.ballisticLateralEndRatio > info.ballisticLateralStartRatio;
        if (weaveEnabled) {
            double horizontalToTarget = Math.sqrt(toTargetXNow * toTargetXNow + toTargetZNow * toTargetZNow);
            boolean terminalNoWeave = distanceToTarget <= info.ballisticTerminalNoWeaveDist
                || horizontalToTarget <= info.ballisticTerminalCylinderRadius;
            if (terminalNoWeave) {
                // Terminal phase must collapse to exact target altitude to remove upward miss bias.
                aimY = targetY;
            }
            if (!terminalNoWeave && u >= info.ballisticLateralStartRatio && u <= info.ballisticLateralEndRatio && horizontalTotal > 1.0E-6D) {
                double t = clamp((u - info.ballisticLateralStartRatio) / (info.ballisticLateralEndRatio - info.ballisticLateralStartRatio), 0.0D, 1.0D);
                double envelope = Math.sin(Math.PI * t);
                double nx = -totalZ / horizontalTotal;
                double nz = totalX / horizontalTotal;
                double phase = Math.toRadians(info.ballisticLateralPhaseDeg);
                double wave = Math.sin(2.0D * Math.PI * info.ballisticLateralWaves * u + phase);
                double offset = info.ballisticLateralAmplitude * wave * envelope;
                aimX += nx * offset;
                aimZ += nz * offset;
            }
        }
        return Vec3.createVectorHelper(aimX, aimY, aimZ);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
