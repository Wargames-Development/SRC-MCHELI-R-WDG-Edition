package mcheli.weapon;

import mcheli.MCH_Lib;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.aircraft.MCH_EntitySeat;
import mcheli.flare.MCH_EntityChaff;
import mcheli.flare.MCH_EntityFlare;
import mcheli.plane.MCP_EntityPlane;
import mcheli.tank.MCH_EntityTank;
import mcheli.uav.MCH_EntityUavStation;
import mcheli.vector.Vector3f;
import mcheli.vehicle.MCH_EntityVehicle;
import mcheli.wrapper.W_Entity;
import mcheli.wrapper.W_Lib;
import mcheli.wrapper.W_MovingObjectPosition;
import mcheli.wrapper.W_WorldFunc;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import java.util.List;

public class MCH_WeaponGuidanceSystem extends MCH_EntityGuidanceSystem {

   public World worldObj;
   protected Entity user;
   public Entity lastLockEntity;
   private Entity targetEntity;


   public MCH_WeaponGuidanceSystem() {
      this((World)null);
   }

   public MCH_WeaponGuidanceSystem(World w) {
      this.worldObj = w;
      this.targetEntity = null;
      this.lastLockEntity = null;
      this.lockCount = 0;
      this.continueLockCount = 0;
      this.lockCountMax = 1;
      this.prevLockCount = 0;
      this.canLockInWater = false;
      this.canLockOnGround = false;
      this.canLockInAir = false;
      this.ridableOnly = false;
      this.lockRange = 300.0D;
      this.lockAngle = 10;
      this.checker = null;
   }

   public void setWorld(World w) {
      this.worldObj = w;
   }

   public void setLockCountMax(int i) {
      this.lockCountMax = i > 0 ? i : 1;
   }

   @Override
   public int getLockCountMax() {
      float stealth = getEntityStealth(this.targetEntity);
      return (int)((float)this.lockCountMax + (float)this.lockCountMax * stealth);
   }
   @Override
   public int getLockCount() {
      return this.lockCount;
   }
   @Override
   public boolean isLockingEntity(Entity entity) {
      return this.getLockCount() > 0 && this.targetEntity != null && !this.targetEntity.isDead && W_Entity.isEqual(entity, this.targetEntity);
   }

   public Entity getLockingEntity() {
      return this.getLockCount() > 0 && this.targetEntity != null && !this.targetEntity.isDead?this.targetEntity:null;
   }

   public Entity getTargetEntity() {
      return this.targetEntity;
   }

   public boolean isLockComplete() {
      return this.getLockCount() == this.getLockCountMax() && this.lastLockEntity != null;
   }

   @Override
   public void update() {
      if(this.worldObj != null && this.worldObj.isRemote) {
         if(this.lockCount != this.prevLockCount) {
            this.prevLockCount = this.lockCount;
         } else {
            this.lockCount = this.prevLockCount = 0;
         }
      }

   }

   public static boolean isEntityOnGround(Entity entity, int height) {
      if(entity != null && !entity.isDead) {
         if(entity.onGround) {
            return true;
         }

         for(int i = 0; i < height; ++i) {
            int x = (int)(entity.posX + 0.5D);
            int y = (int)(entity.posY + 0.5D) - i;
            int z = (int)(entity.posZ + 0.5D);
            int blockId = W_WorldFunc.getBlockId(entity.worldObj, x, y, z);
            if(blockId != 0) {
               return true;
            }
         }
      }

      return false;
   }

   @Override
   public boolean lock(Entity user) {
      this.user = user;
      return this.lock(user, true);
   }

   public boolean lock(Entity user, boolean isLockContinue) {

      // If running on the server side, skip lock-on processing
      if(!this.worldObj.isRemote) {
         return false;
      } else {

         boolean result = false;  // Final lock result
         double dz;  // Z-axis distance to potential target

         if(this.lockCount == 0) {  // If no current target is locked
            // Retrieve all entities within the weapon’s lock range
            List canLock = this.worldObj.getEntitiesWithinAABBExcludingEntity(user, user.boundingBox.expand(this.lockRange, this.lockRange, this.lockRange));
            Entity potentialTarget = null;  // Candidate entity for lock-on
            double dist = this.lockRange * this.lockRange * 2.0D;  // Max lock-on distance threshold

            // Iterate through all entities within range
            for(int i = 0; i < canLock.size(); ++i) {
               Entity currentEntity = (Entity)canLock.get(i);
               // Check if this entity is lockable
               if(this.canLockEntity(currentEntity)) {
                  dz = currentEntity.posX - user.posX;
                  double dy = currentEntity.posY - user.posY;
                  double dz1 = currentEntity.posZ - user.posZ;
                  double distance = dz * dz + dy * dy + dz1 * dz1;
                  Entity entityLocker1 = this.getLockEntity(user);
                  float stealth1 = 1.0F - getEntityStealth(currentEntity);
                  double range1 = this.lockRange;
                  // Compute adjusted lock angle based on target stealth
                  float angle = (float)this.lockAngle * (stealth1 / 2.0F + 0.5F);
                  // Check if entity is within lock-on distance and cone angle
                  if(distance < range1 * range1 && distance < dist && inLockAngle(entityLocker1, user.rotationYaw, user.rotationPitch, currentEntity, angle)) {
                     // Perform line-of-sight check (raycast)
                     Vec3 v1 = W_WorldFunc.getWorldVec3(this.worldObj, entityLocker1.posX, entityLocker1.posY, entityLocker1.posZ);
                     Vec3 v2 = W_WorldFunc.getWorldVec3(this.worldObj, currentEntity.posX, currentEntity.posY + (double)(currentEntity.height / 2.0F), currentEntity.posZ);
                     MovingObjectPosition m = W_WorldFunc.clip(this.worldObj, v1, v2, false, true, false);
                     // If no obstruction, confirm as potential lock target
                     if(m == null || W_MovingObjectPosition.isHitTypeEntity(m)) {
                        potentialTarget = currentEntity;
                     }
                  }
               }
            }

            // Assign final target
            this.targetEntity = potentialTarget;
            if(potentialTarget != null) {
               ++this.lockCount;  // Increase lock counter when a valid target is found
            }
         // If already locked onto a valid target
         } else if(this.targetEntity != null && !this.targetEntity.isDead) {
            boolean canLockTarget = true;  // Whether to maintain current lock

            // Check radar missile countermeasures
            if(targetEntity instanceof MCH_EntityAircraft) {
               if(isRadarMissile && ((MCH_EntityAircraft) targetEntity).chaffUseTime > 0) {
                  canLockTarget = false;
               }
            }

            // Check if target is on ground, and if ground targets are allowed
//            if(!this.canLockInWater && this.targetEntity.isInWater()) {
//               canLockTarget = false;
//            }

            boolean isTargetOnGround = isEntityOnGround(this.targetEntity, lockMinHeight);  // 判断目标是否在地面上
            // Deny lock if target is on ground and this missile can’t lock ground targets
            if(!this.canLockOnGround && isTargetOnGround) {
               canLockTarget = false;
            }

            // Deny lock if target is airborne and air locking is disabled
            if(!this.canLockInAir && !isTargetOnGround) {
               canLockTarget = false;
            }

            MCH_EntityAircraft ac = null; // The entity currently being ridden by the player
            if(user.ridingEntity instanceof MCH_EntityAircraft) {
               ac = (MCH_EntityAircraft)user.ridingEntity;
            } else if(user.ridingEntity instanceof MCH_EntitySeat) {
               ac = ((MCH_EntitySeat)user.ridingEntity).getParent();
            } else if(user.ridingEntity instanceof MCH_EntityUavStation) {
               ac = ((MCH_EntityUavStation)user.ridingEntity).getControlAircract();
            }
            if(ac instanceof MCP_EntityPlane && targetEntity instanceof MCP_EntityPlane) {
               // Player aircraft velocity vector
               Vector3f playerVelocity = new Vector3f(ac.motionX, ac.motionY, ac.motionZ);
               // Target aircraft velocity vector
               Vector3f targetVelocity = new Vector3f(targetEntity.motionX, targetEntity.motionY, targetEntity.motionZ);
               float angleInDegrees = 0;
               if (playerVelocity.length() > 0.001 && targetVelocity.length() > 0.001) {
                  // Compute dot product between the two velocity vectors
                  float dotProduct = Vector3f.dot(playerVelocity, targetVelocity);
                  // Compute magnitudes of both vectors
                  float playerSpeed = playerVelocity.length();
                  float targetSpeed = targetVelocity.length();
                  // Compute cosine of the angle between them
                  float cosAngle = dotProduct / (playerSpeed * targetSpeed);
                  // Clamp cosine value to avoid floating-point drift errors
                  cosAngle = Math.max(-1.0f, Math.min(1.0f, cosAngle));
                  // Convert to angle in radians
                  float angle = (float) Math.acos(cosAngle);
                  // If the angle is obtuse (> 90°), convert it to an acute angle (< 90°)
                  if (angle > Math.PI / 2) {
                     angle = (float) (Math.PI - angle);  // 转换为锐角
                  }
                  // Convert radians to degrees for easier comparison
                  angleInDegrees = (float) Math.toDegrees(angle);
               }
               // If the relative velocity angle exceeds the missile’s PD threshold, cancel lock
               if (angleInDegrees > ac.getCurrentWeapon(user).getCurrentWeapon().getInfo().pdHDNMaxDegree) {
                  canLockTarget = false;
               }
            }

            // If the target can continue to be locked
            if(canLockTarget) {
               double dx = this.targetEntity.posX - user.posX;
               double dy = this.targetEntity.posY - user.posY;
               dz = this.targetEntity.posZ - user.posZ;
               float stealth = 1.0F - getEntityStealth(this.targetEntity);
               double lockRange = this.lockRange * (double)stealth;
               // Check if target is still within lock range
               if(dx * dx + dy * dy + dz * dz < lockRange * lockRange) {
                  if(this.worldObj.isRemote && this.lockSoundCount == 1) {
                     // MCH_PacketNotifyLock.send(this.getTargetEntity());
                  }

                  this.lockSoundCount = (this.lockSoundCount + 1) % 15;
                  Entity entityLocker = this.getLockEntity(user);
                  // Check if target remains inside the lock cone
                  if(inLockAngle(entityLocker, user.rotationYaw, user.rotationPitch, this.targetEntity, (float)this.lockAngle)) {
                     if(this.lockCount < this.getLockCountMax()) {
                        ++this.lockCount;  // Gradually increase lock strength
                     }
                  // Gradual loss of lock if target drifts outside cone
                  } else if(this.continueLockCount > 0) {
                     --this.continueLockCount;
                     if(this.continueLockCount <= 0 && this.lockCount > 0) {
                        --this.lockCount;
                     }
                  // Fully lose lock if target remains outside too long
                  } else {
                     this.continueLockCount = 0;
                     --this.lockCount;
                  }

                  // If lock strength reaches maximum threshold → full lock achieved
                  if(this.lockCount >= this.getLockCountMax()) {
                     if(this.continueLockCount <= 0) {
                        this.continueLockCount = this.getLockCountMax() / 3;
                        if(this.continueLockCount > 20) {
                           this.continueLockCount = 20;
                        }
                     }

                     result = true;  // Successful lock-on
                     this.lastLockEntity = this.targetEntity;
                     if(isLockContinue) {
                        this.prevLockCount = this.lockCount - 1;
                     } else {
                        this.clearLock();
                     }
                  }
               } else {
                  this.clearLock();  // Target moved out of range, clear lock
               }
            } else {
               this.clearLock();  // Lock lost due to conditions (e.g., countermeasures)
            }
         } else {
            this.clearLock();  // Target is null or dead, clear lock
         }

         // Determine if lock was successful
         result = this.lockCount >= this.getLockCountMax();

         if(result) {
            this.lastLockEntity = targetEntity;
            // Play lock-acquired sound effect
            this.worldObj.playSoundAtEntity(user, "mcheli:ir_basic_tone", 1.0f, 1.0f);
         } else {
            // Play lock-attempt tone
            this.worldObj.playSoundAtEntity(user, "mcheli:ir_lock_tone", 1.0f, 1.0f);
         }

         return result;  // Return final lock result
      }
   }


   public static float getEntityStealth(Entity entity) {
      return entity instanceof MCH_EntityAircraft?((MCH_EntityAircraft)entity).getStealth():(entity != null && entity.ridingEntity instanceof MCH_EntityAircraft?((MCH_EntityAircraft)entity.ridingEntity).getStealth():0.0F);
   }

   public void clearLock() {
      this.targetEntity = null;
      this.lockCount = 0;
      this.continueLockCount = 0;
      this.lockSoundCount = 0;
   }

   public Entity getLockEntity(Entity entity) {
      if(entity.ridingEntity instanceof MCH_EntityUavStation) {
         MCH_EntityUavStation us = (MCH_EntityUavStation)entity.ridingEntity;
         if(us.getControlAircract() != null) {
            return us.getControlAircract();
         }
      }

      return entity;
   }

   public boolean canLockEntity(Entity entity) {
      // If locking players is disallowed and the entity is a player not riding anything, return false
      if(this.ridableOnly && entity instanceof EntityPlayer && entity.ridingEntity == null) {
         return false;
      } else {
         // Get the entity's class name
         String className = entity.getClass().getName();

         // Do not lock EntityCamera types
         if(className.indexOf("EntityCamera") >= 0) {
            return false;
         }
         // Do not lock players who are riding a vehicle; lock the vehicle instead
         if(entity instanceof EntityPlayer && entity.ridingEntity != null) {
            return false;
         }
         // IR missiles can lock onto flares
         if(this.isHeatSeekerMissile && entity instanceof MCH_EntityFlare) {
            return true;
         }
         // Radar-guided missiles can lock onto chaff
         if(this.isRadarMissile && entity instanceof MCH_EntityChaff) {
            return true;
         }
         if(targetEntity instanceof MCH_EntityAircraft) {
            if(isRadarMissile && ((MCH_EntityAircraft) targetEntity).chaffUseTime > 0) {
               return false;
            }
         }
         if(targetEntity instanceof MCH_EntityTank || targetEntity instanceof MCH_EntityVehicle) {
            if(((MCH_EntityAircraft) targetEntity).isFlareUsing()) {
               return false;
            }
         }
         // Allow locking missiles themselves
         if(this.canLockMissile &&
                 (entity instanceof MCH_EntityAAMissile || entity instanceof MCH_EntityATMissile
                         || entity instanceof MCH_EntityASMissile || entity instanceof MCH_EntityTvMissile)) {
            if(!W_Entity.isEqual(user, ((MCH_EntityBaseBullet) entity).shootingEntity)) {
               return true;
            }
         }
         // If the entity is neither a living entity nor one of the specific vehicle types, return false
         if(!W_Lib.isEntityLivingBase(entity)
                 && !(entity instanceof MCH_EntityAircraft)
                 && !className.contains("EntityVehicle")
                 && !className.contains("EntityPlane")
                 && !className.contains("EntityMecha")
                 && !className.contains("EntityAAGun")) {
            return false;
         }
         // If the entity is in water and water-locking is disabled, return false
//         else if(!this.canLockInWater && entity.isInWater()) {
//            return false;
//         }
         // If a custom checker is present and it rejects this entity, return false
         else if(this.checker != null && !this.checker.canLockEntity(entity)) {
            return false;
         }

         else {
            // Determine if the entity is on the ground
            boolean ong = isEntityOnGround(entity, lockMinHeight);
            // Return true if (ground targets allowed OR entity not on ground) AND (air targets allowed OR entity on ground)
            return (this.canLockOnGround || !ong) && (this.canLockInAir || ong);
         }
      }
   }


   public static boolean inLockAngle(Entity entity, float rotationYaw, float rotationPitch, Entity target, float lockAng) {
      double dx = target.posX - entity.posX;
      double dy = target.posY + (double)(target.height / 2.0F) - entity.posY;
      double dz = target.posZ - entity.posZ;
      float entityYaw = (float)MCH_Lib.getRotate360((double)rotationYaw);
      float targetYaw = (float)MCH_Lib.getRotate360(Math.atan2(dz, dx) * 180.0D / 3.141592653589793D);
      float diffYaw = (float)MCH_Lib.getRotate360((double)(targetYaw - entityYaw - 90.0F));
      double dxz = Math.sqrt(dx * dx + dz * dz);
      float targetPitch = -((float)(Math.atan2(dy, dxz) * 180.0D / 3.141592653589793D));
      float diffPitch = targetPitch - rotationPitch;
      return (diffYaw < lockAng || diffYaw > 360.0F - lockAng) && Math.abs(diffPitch) < lockAng;
   }

   @Override
   protected Entity getLastLockEntity() {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public double getLockPosX() {
      return targetEntity.posX;
   }

   @Override
   public double getLockPosY() {
      return targetEntity.posY;
   }

   @Override
   public double getLockPosZ() {
      return targetEntity.posZ;
   }
}
