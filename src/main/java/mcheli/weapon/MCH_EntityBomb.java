package mcheli.weapon;

import mcheli.wrapper.W_Lib;
import net.minecraft.entity.Entity;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import java.util.List;

public class MCH_EntityBomb extends MCH_EntityBaseBullet {

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
        }
        this.onUpdateBomblet();
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
