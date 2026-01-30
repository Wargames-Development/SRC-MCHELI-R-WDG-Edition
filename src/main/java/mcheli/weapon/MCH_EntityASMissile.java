package mcheli.weapon;

import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.wrapper.W_Entity;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;

public class MCH_EntityASMissile extends MCH_EntityBaseBullet implements MCH_IEntityLockChecker, MCH_IMissile {

    public double targetPosX;
    public double targetPosY;
    public double targetPosZ;
    public double originTargetPosX;
    public double originTargetPosY;
    public double originTargetPosZ;
    public boolean targeting;

    public MCH_EntityASMissile(World par1World) {
        super(par1World);
        this.targetPosX = 0.0D;
        this.targetPosY = 0.0D;
        this.targetPosZ = 0.0D;
    }

    public MCH_EntityASMissile(World par1World, double posX, double posY, double posZ, double targetX, double targetY, double targetZ, float yaw, float pitch, double acceleration) {
        super(par1World, posX, posY, posZ, targetX, targetY, targetZ, yaw, pitch, acceleration);
    }

    public float getGravity() {
        return this.getBomblet() == 1 ? -0.03F : super.getGravity();
    }

    public float getGravityInWater() {
        return this.getBomblet() == 1 ? -0.03F : super.getGravityInWater();
    }

    public void onUpdate() {
        super.onUpdate();
        this.onUpdateBomblet();
        if (this.getInfo() != null && !this.getInfo().disableSmoke) {
            this.spawnExplosionParticle(this.getInfo().trajectoryParticleName, 3, 5.0F * this.getInfo().smokeSize * 0.5F);
        }

        if (super.shootingEntity != null) {
            double x = super.posX - super.shootingEntity.posX;
            double y = super.posY - super.shootingEntity.posY;
            double z = super.posZ - super.shootingEntity.posZ;
            if (x * x + y * y + z * z > 2000 * 2000.0D) {
                this.setDead();
            }

            if (!super.worldObj.isRemote && !super.isDead && targeting
                    && this.getInfo() != null
                    && this.getCountOnUpdate() > this.getInfo().rigidityTime) {

                if (this.getInfo().lockEntity) {
                    int range = this.getInfo().maxLockOnRange;

                    for (Object obj : super.worldObj.getEntitiesWithinAABBExcludingEntity(
                            this, super.boundingBox.expand(100, 100, 100))) {

                        if (!(obj instanceof MCH_EntityAircraft)) continue;

                        Entity entity = (Entity) obj;

                        if (W_Entity.isEqual(entity, shootingAircraft)) continue;

                        double d0 = entity.posX - originTargetPosX;
                        double d1 = entity.posY - originTargetPosY;
                        double d2 = entity.posZ - originTargetPosZ;

                        if (d0 * d0 + d1 * d1 + d2 * d2 <= (double) range * (double) range) {
                            targetPosX = entity.posX;
                            targetPosY = entity.posY;
                            targetPosZ = entity.posZ;
                        }
                    }

                    guidanceToPos(targetPosX, targetPosY, targetPosZ);
                } else {
                    guidanceToPos(originTargetPosX, originTargetPosY, originTargetPosZ);
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
}
