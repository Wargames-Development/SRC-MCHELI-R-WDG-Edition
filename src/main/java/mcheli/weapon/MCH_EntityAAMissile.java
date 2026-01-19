package mcheli.weapon;

import net.minecraft.entity.Entity;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

public class MCH_EntityAAMissile extends MCH_EntityBaseBullet implements MCH_IEntityLockChecker, MCH_IMissile {

    public boolean passiveRadarBVRLocking = false;
    public int passiveRadarBVRLockingPosX = 0;
    public int passiveRadarBVRLockingPosY = 0;
    public int passiveRadarBVRLockingPosZ = 0;

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

        if (this.getCountOnUpdate() > 4 && this.getInfo() != null && !this.getInfo().disableSmoke) {
            this.spawnExplosionParticle(this.getInfo().trajectoryParticleName, 3, 7.0F * this.getInfo().smokeSize * 0.5F);
        }

        if (!worldObj.isRemote && this.getInfo() != null) {
            if (super.shootingEntity != null && super.targetEntity != null && !super.targetEntity.isDead) {
                double x = super.posX - super.targetEntity.posX;
                double y = super.posY - super.targetEntity.posY;
                double z = super.posZ - super.targetEntity.posZ;
                double d = x * x + y * y + z * z;

                if (d > 3422500.0D) {
                    setDead();
                } else if (getCountOnUpdate() > getInfo().rigidityTime) {
                    guidanceToTarget(super.targetEntity.posX, super.targetEntity.posY, super.targetEntity.posZ);
                }
            } else {
                if (getInfo().activeRadar && ticksExisted % getInfo().scanInterval == 0) {
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
