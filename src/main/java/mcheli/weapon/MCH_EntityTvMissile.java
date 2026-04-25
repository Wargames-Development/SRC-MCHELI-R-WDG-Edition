package mcheli.weapon;

import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.mob.MCH_EntityGunner;
import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import java.util.List;

public class MCH_EntityTvMissile extends MCH_EntityBaseBullet implements MCH_IEntityLockChecker, MCH_IMissile {

    public boolean isSpawnParticle = true;
    public boolean isTVMissile;
    public boolean targeting = true;

    public MCH_EntityTvMissile(World par1World) {
        super(par1World);
    }

    public MCH_EntityTvMissile(World par1World, double posX, double posY, double posZ, double targetX, double targetY, double targetZ, float yaw, float pitch, double acceleration) {
        super(par1World, posX, posY, posZ, targetX, targetY, targetZ, yaw, pitch, acceleration);
    }

    public void setMotion(double targetX, double targetY, double targetZ) {
        double d6 = MathHelper.sqrt_double(targetX * targetX + targetY * targetY + targetZ * targetZ);
        super.motionX = targetX * this.acceleration / d6;
        super.motionY = targetY * this.acceleration / d6;
        super.motionZ = targetZ * this.acceleration / d6;
    }

    public void setTVMissile(boolean isTVMissile) {
        this.isTVMissile = isTVMissile;
    }

    public void onUpdate() {
        super.onUpdate();
        this.onUpdateBomblet();
        if (this.isSpawnParticle && this.getInfo() != null && !this.getInfo().disableSmoke && this.isWithinTrajectoryParticleEndTick()) {
            this.spawnExplosionParticle(this.getInfo().trajectoryParticleName, 3, 5.0F * this.getInfo().smokeSize * 0.5F);
        }

        if (super.shootingEntity != null) {
            double x = super.posX - super.shootingEntity.posX;
            double y = super.posY - super.shootingEntity.posY;
            double z = super.posZ - super.shootingEntity.posZ;
            if (x * x + y * y + z * z > 2000 * 2000.0D) {
                this.setDead();
            }

            if (!super.worldObj.isRemote && !super.isDead && this.getCountOnUpdate() > this.getInfo().rigidityTime) {
                this.onUpdateMotion();
            }
        } else if (!super.worldObj.isRemote) {
            this.setDead();
        }

    }

    public void onUpdateMotion() {
        Entity e = super.shootingEntity;

        if(!targeting) return;

        // Mode split by runtime missile mode:
        // - isTVMissile=true  -> TV guidance (follow launcher view)
        // - isTVMissile=false -> laser guidance/terminal attack (GPS/laser point)
        // Do not fall back to TV by weapon info flag here, otherwise fixed laser mode can be misrouted.
        boolean treatAsTvGuidance = e instanceof MCH_EntityGunner || this.isTVMissile;
        if (treatAsTvGuidance) {
            if (e != null && !e.isDead) {
                MCH_EntityAircraft ac = MCH_EntityAircraft.getAircraft_RiddenOrControl(e);
                if (ac != null) {
                    if (!isTVMissile || ac.getTVMissile() == this) {
                        float yaw = e.rotationYaw;
                        float pitch = e.rotationPitch;
                        double tX = -MathHelper.sin(yaw / 180.0F * 3.1415927F) * MathHelper.cos(pitch / 180.0F * 3.1415927F);
                        double tZ = MathHelper.cos(yaw / 180.0F * 3.1415927F) * MathHelper.cos(pitch / 180.0F * 3.1415927F);
                        double tY = -MathHelper.sin(pitch / 180.0F * 3.1415927F);
                        this.setMotion(tX, tY, tZ);
                        this.setRotation(yaw, pitch);
                    }
                }

            }
        } else {
            // Command-line fallback:
            // mode=1 with laserGuidance=false keeps manual line command without TV camera follow.
            if (e != null && !e.isDead && this.getInfo() != null && !this.getInfo().laserGuidance) {
                float yaw = e.rotationYaw;
                float pitch = e.rotationPitch;
                double tX = -MathHelper.sin(yaw / 180.0F * 3.1415927F) * MathHelper.cos(pitch / 180.0F * 3.1415927F);
                double tZ = MathHelper.cos(yaw / 180.0F * 3.1415927F) * MathHelper.cos(pitch / 180.0F * 3.1415927F);
                double tY = -MathHelper.sin(pitch / 180.0F * 3.1415927F);
                this.setMotion(tX, tY, tZ);
                this.setRotation(yaw, pitch);
                return;
            }
            MCH_EntityAircraft ac = MCH_EntityAircraft.getAircraft_RiddenOrControl(e);
            if (e != null) {
                int sourceType = ac != null ? MCH_LaserStateStore.SOURCE_AIRCRAFT : MCH_LaserStateStore.SOURCE_HANDHELD;
                MCH_LaserStateStore.LaserState laser = MCH_LaserStateStore.getServerState(e.getEntityId(), sourceType);
                if (laser != null && laser.active) {
                    if (!this.isLaserPointJammed(laser.x, laser.y, laser.z)) {
                        onLaserGuide(laser.x, laser.y, laser.z);
                    }
                }
            }
        }

    }

    private boolean isLaserPointJammed(double x, double y, double z) {
        double r = 5.0D;
        AxisAlignedBB aabb = AxisAlignedBB.getBoundingBox(x - r, y - r, z - r, x + r, y + r, z + r);
        List list = super.worldObj.getEntitiesWithinAABB(MCH_EntityAircraft.class, aabb);
        for (Object o : list) {
            MCH_EntityAircraft veh = (MCH_EntityAircraft) o;
            if (veh != null && veh.getAcInfo() != null && (veh.getAcInfo().hasPhotoelectricJammer || veh.isUsingFlareType(10))) {
                return true;
            }
        }
        return false;
    }

    public void onLaserGuide(double x, double y, double z) {
        guidanceToPos(x, y, z);
    }


    public void sprinkleBomblet() {
        if (!super.worldObj.isRemote) {
            MCH_EntityRocket e = new MCH_EntityRocket(super.worldObj, super.posX, super.posY, super.posZ, super.motionX, super.motionY, super.motionZ, super.rotationYaw, super.rotationPitch, super.acceleration);
            e.setInfoByName(this.getName());
            e.setParameterFromWeapon(this, super.shootingAircraft, super.shootingEntity);
            float MOTION = this.getInfo().bombletDiff;
            float RANDOM = 1.2F;
            e.motionX += ((double) super.rand.nextFloat() - 0.5D) * (double) MOTION;
            e.motionY += ((double) super.rand.nextFloat() - 0.5D) * (double) MOTION;
            e.motionZ += ((double) super.rand.nextFloat() - 0.5D) * (double) MOTION;
            e.setBomblet();
            super.worldObj.spawnEntityInWorld(e);
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
