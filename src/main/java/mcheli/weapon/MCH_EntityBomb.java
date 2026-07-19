package mcheli.weapon;

import mcheli.wrapper.W_Lib;
import net.minecraft.entity.Entity;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import java.util.List;

public class MCH_EntityBomb extends MCH_EntityBaseBullet {

    public double targetPosX;
    public double targetPosY;
    public double targetPosZ;
    public double originTargetPosX;
    public double originTargetPosY;
    public double originTargetPosZ;
    public boolean targeting;

    public MCH_EntityBomb(World par1World) {
        super(par1World);
    }

    public MCH_EntityBomb(World par1World, double posX, double posY, double posZ, double targetX, double targetY, double targetZ, float yaw, float pitch, double acceleration) {
        super(par1World, posX, posY, posZ, targetX, targetY, targetZ, yaw, pitch, acceleration);
    }

    public void onUpdate() {
        super.onUpdate();
        if (!super.worldObj.isRemote && this.getInfo() != null) {
            super.motionX *= 0.999D;
            super.motionZ *= 0.999D;
            if (this.isInWater()) {
                super.motionX *= this.getInfo().velocityInWater;
                super.motionY *= this.getInfo().velocityInWater;
                super.motionZ *= this.getInfo().velocityInWater;
            }
            updateGpsGuidance();
        }
        this.onUpdateBomblet();
    }

    public void setGpsTarget(double x, double y, double z) {
        this.targetPosX = x;
        this.targetPosY = y;
        this.targetPosZ = z;
        this.originTargetPosX = x;
        this.originTargetPosY = y;
        this.originTargetPosZ = z;
        this.targeting = true;
    }

    private void updateGpsGuidance() {
        if (!this.targeting || this.getInfo() == null || !this.getInfo().isGPSMissile) {
            return;
        }
        if (this.getCountOnUpdate() <= this.getInfo().rigidityTime) {
            return;
        }
        double dx = this.targetPosX - super.posX;
        double dz = this.targetPosZ - super.posZ;
        double horizontalDistSq = dx * dx + dz * dz;
        if (horizontalDistSq <= 9.0D) {
            this.targeting = false;
            return;
        }
        double horizontalDist = Math.sqrt(horizontalDistSq);
        double horizontalSpeed = Math.sqrt(super.motionX * super.motionX + super.motionZ * super.motionZ);
        double minGuidanceSpeed = Math.max(0.15D, this.acceleration * 0.25D);
        if (horizontalSpeed < minGuidanceSpeed) {
            horizontalSpeed = minGuidanceSpeed;
        }
        double desiredX = dx / horizontalDist * horizontalSpeed;
        double desiredZ = dz / horizontalDist * horizontalSpeed;
        double turn = clamp(this.getInfo().turningFactor, 0.02D, 0.25D);
        super.motionX += (desiredX - super.motionX) * turn;
        super.motionZ += (desiredZ - super.motionZ) * turn;
        double yaw = Math.atan2(super.motionZ, super.motionX);
        super.rotationYaw = (float)(yaw * 180.0D / Math.PI) - 90.0F;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public void sprinkleBomblet() {
        if (!super.worldObj.isRemote) {
            MCH_EntityBomb e = new MCH_EntityBomb(super.worldObj, super.posX, super.posY, super.posZ, super.motionX, super.motionY, super.motionZ, (float) super.rand.nextInt(360), 0.0F, super.acceleration);
            e.setParameterFromWeapon(this, super.shootingAircraft, super.shootingEntity);
            e.setInfoByName(this.getName());
            float MOTION = 1.0F;
            float RANDOM = this.getInfo().bombletDiff;
            e.motionX = super.motionX * 1.0D + (double) ((super.rand.nextFloat() - 0.5F) * RANDOM);
            e.motionY = super.motionY * 1.0D / 2.0D + (double) ((super.rand.nextFloat() - 0.5F) * RANDOM / 2.0F);
            e.motionZ = super.motionZ * 1.0D + (double) ((super.rand.nextFloat() - 0.5F) * RANDOM);
            e.setBomblet();
            super.worldObj.spawnEntityInWorld(e);
        }

    }

    public MCH_BulletModel getDefaultBulletModel() {
        return MCH_DefaultBulletModels.Bomb;
    }
}
