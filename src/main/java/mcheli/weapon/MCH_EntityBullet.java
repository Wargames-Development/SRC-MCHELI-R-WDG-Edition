package mcheli.weapon;

import java.util.List;

import com.flansmod.common.network.PacketPlaySound;
import com.flansmod.common.vector.Vector3f;
import mcheli.MCH_Config;
import mcheli.MCH_Lib;
import mcheli.MCH_MOD;
import mcheli.weapon.MCH_BulletModel;
import mcheli.weapon.MCH_DefaultBulletModels;
import mcheli.weapon.MCH_EntityBaseBullet;
import mcheli.wrapper.W_MovingObjectPosition;
import mcheli.wrapper.W_WorldFunc;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public class MCH_EntityBullet extends MCH_EntityBaseBullet {

   public MCH_EntityBullet(World par1World) {
      super(par1World);
   }

   public MCH_EntityBullet(World par1World, double pX, double pY, double pZ, double targetX, double targetY, double targetZ, float yaw, float pitch, double acceleration) {
      super(par1World, pX, pY, pZ, targetX, targetY, targetZ, yaw, pitch, acceleration);
   }

   public void onUpdate() {
      super.onUpdate();
      if(!super.isDead && !super.worldObj.isRemote && this.getCountOnUpdate() > 1 && this.getInfo() != null && super.explosionPower > 0) {
         float pDist = this.getInfo().proximityFuseDist;
         if((double)pDist > 0.1D) {
            ++pDist;
            float rng = pDist + MathHelper.abs(this.getInfo().acceleration);
            List list = super.worldObj.getEntitiesWithinAABBExcludingEntity(this, super.boundingBox.expand(rng, rng, rng));

            for (Object o : list) {
               Entity entity1 = (Entity) o;
               if (this.canBeCollidedEntity(entity1) && entity1.getDistanceSqToEntity(this) < (double) (pDist * pDist)) {
                  MCH_Lib.DbgLog(super.worldObj, "MCH_EntityBullet.onUpdate:proximityFuse:" + entity1);
                  super.posX = (entity1.posX + super.posX) / 2.0D;
                  super.posY = (entity1.posY + super.posY) / 2.0D;
                  super.posZ = (entity1.posZ + super.posZ) / 2.0D;
                  MovingObjectPosition mop = W_MovingObjectPosition.newMOP((int) super.posX, (int) super.posY, (int) super.posZ, 0, W_WorldFunc.getWorldVec3EntityPos(this), false);
                  this.onImpact(mop, 1.0F);
                  break;
               }
            }
         }
      }

   }

   @Override
   protected void onUpdateCollided() {
       double mx = super.motionX * this.accelerationFactor;
       double my = super.motionY * this.accelerationFactor;
       double mz = super.motionZ * this.accelerationFactor;
      float damageFactor = 1.0F;
      MovingObjectPosition m = null;

      Vec3 src;
      Vec3 dir;
      for(int entity = 0; entity < 5; ++entity) {
          src = W_WorldFunc.getWorldVec3(super.worldObj, super.posX, super.posY, super.posZ);
          dir = W_WorldFunc.getWorldVec3(super.worldObj, super.posX + mx, super.posY + my, super.posZ + mz);
         m = W_WorldFunc.clip(super.worldObj, src, dir);
         boolean list = false;
         if(super.shootingEntity != null && W_MovingObjectPosition.isHitTypeTile(m)) {
            Block d0 = W_WorldFunc.getBlock(super.worldObj, m.blockX, m.blockY, m.blockZ);
             if(MCH_Config.bulletBreakableBlocks.contains(d0)) {
               W_WorldFunc.destroyBlock(super.worldObj, m.blockX, m.blockY, m.blockZ, true);
               list = true;
            }
         }

         if(!list) {
            break;
         }
      }

      src = W_WorldFunc.getWorldVec3(super.worldObj, super.posX, super.posY, super.posZ);
      dir = W_WorldFunc.getWorldVec3(super.worldObj, super.posX + mx, super.posY + my, super.posZ + mz);
      if(this.getInfo().delayFuse > 0) {
         if(m != null) {
            this.boundBullet(m.sideHit);
            if(super.delayFuse == 0) {
               super.delayFuse = this.getInfo().delayFuse;
            }
         }

      } else {
          if (m != null) {
              dir = W_WorldFunc.getWorldVec3(super.worldObj, m.hitVec.xCoord, m.hitVec.yCoord, m.hitVec.zCoord);
          }

          Entity hitEntity = null;
          List entities = super.worldObj.getEntitiesWithinAABBExcludingEntity(this, super.boundingBox.addCoord(mx, my, mz).expand(21.0D, 21.0D, 21.0D));
          double d2 = 0.0D;
          MovingObjectPosition result = m;
          for (Object o : entities) {
              Entity entity = (Entity) o;
              if (this.canBeCollidedEntity(entity) && shootingAircraft != o) {
                  float f = 0.3F;
                  MovingObjectPosition movingObjectPosition = entity.boundingBox.expand(f, f, f).calculateIntercept(src, dir);
                  if (movingObjectPosition != null) {
                      double d1 = src.distanceTo(movingObjectPosition.hitVec);
                      if (d1 < d2 || d2 == 0.0D) {
                          hitEntity = entity;
                          d2 = d1;
                          result = movingObjectPosition;
                      }
                  }
              }
          }

          if(result != null) {
              dir = Vec3.createVectorHelper(result.hitVec.xCoord - this.posX, result.hitVec.yCoord - this.posY, result.hitVec.zCoord - this.posZ);
              double d = 1.0;
              if (mx != 0.0) {
                  d = dir.xCoord / mx;
              } else if (my != 0.0) {
                  d = dir.yCoord / my;
              } else if (mz != 0.0) {
                  d = dir.zCoord / mz;
              }
              if (d < 0.0) {
                  d = -d;
              }

              Vec3 newHitVec = Vec3.createVectorHelper(posX + mx * d, posY + my * d, posZ + mz * d);
              if (hitEntity != null) {
                  this.onImpact(new MovingObjectPosition(hitEntity, newHitVec), damageFactor);
              } else {
                  this.onImpact(result, damageFactor);
              }
          }
      }
   }


   public MCH_BulletModel getDefaultBulletModel() {
      return MCH_DefaultBulletModels.Bullet;
   }
}
