package mcheli.weapon;

import mcheli.MCH_Lib;
import mcheli.MCH_PlayerViewHandler;
import mcheli.aircraft.MCH_AircraftInfo;
import mcheli.tank.MCH_EntityTank;
import mcheli.weapon.MCH_EntityRocket;
import mcheli.weapon.MCH_WeaponBase;
import mcheli.weapon.MCH_WeaponInfo;
import mcheli.weapon.MCH_WeaponParam;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public class MCH_WeaponRocket extends MCH_WeaponBase {

   public MCH_WeaponRocket(World w, Vec3 v, float yaw, float pitch, String nm, MCH_WeaponInfo wi) {
      super(w, v, yaw, pitch, nm, wi);
      super.acceleration = 4.0F;
      super.explosionPower = 3;
      super.power = 22;
      super.interval = 5;
      if(w.isRemote) {
         super.interval += 2;
      }

   }

   public String getName() {
      return super.getName() + (this.getCurrentMode() == 0?"":" [HEIAP]");
   }

   public boolean shot(MCH_WeaponParam prm) {
      if(!super.worldObj.isRemote) {
         this.playSound(prm.entity);
         float yaw,pitch;
         if(prm.entity instanceof MCH_EntityTank) {
            MCH_EntityTank tank = (MCH_EntityTank) prm.entity;
            yaw = prm.user.rotationYaw ;
            pitch = prm.user.rotationPitch ;
            yaw += prm.randYaw;
            pitch += prm.randPitch;
            int wid = tank.getCurrentWeaponID(prm.user);
            MCH_AircraftInfo.Weapon w = tank.getAcInfo().getWeaponById(wid);
            float minPitch = w == null ? tank.getAcInfo().minRotationPitch : w.minPitch;
            float maxPitch = w == null ? tank.getAcInfo().maxRotationPitch : w.maxPitch;
            float playerYaw = MathHelper.wrapAngleTo180_float(tank.getRotYaw() - yaw);
            float playerPitch = tank.getRotPitch() * MathHelper.cos((float) (playerYaw * Math.PI / 180.0D))
                    + -tank.getRotRoll() * MathHelper.sin((float) (playerYaw * Math.PI / 180.0D));
            float playerYawRel = MathHelper.wrapAngleTo180_float(yaw - tank.getRotYaw());
            float yawLimit = (w == null ? 360F : w.maxYaw);
            float relativeYaw = MCH_Lib.RNG(playerYawRel, -yawLimit, yawLimit);
            yaw = MathHelper.wrapAngleTo180_float(tank.getRotYaw() + relativeYaw);
            pitch = MCH_Lib.RNG(pitch, playerPitch + minPitch, playerPitch + maxPitch);
            pitch = MCH_Lib.RNG(pitch, -90.0F, 90.0F);
         } else {
            yaw = prm.rotYaw;
            pitch = prm.rotPitch;
         }
         double tX = -MathHelper.sin(yaw / 180.0F * 3.1415927F) * MathHelper.cos(pitch / 180.0F * 3.1415927F);
         double tZ = MathHelper.cos(yaw / 180.0F * 3.1415927F) * MathHelper.cos(pitch / 180.0F * 3.1415927F);
         double tY = -MathHelper.sin(pitch / 180.0F * 3.1415927F);
         MCH_EntityRocket e = new MCH_EntityRocket(super.worldObj, prm.posX, prm.posY, prm.posZ, tX, tY, tZ, yaw, pitch, super.acceleration);
         e.setAirburstDist(this.airburstDist);
         e.setName(super.name);
         e.setParameterFromWeapon(this, prm.entity, prm.user);
         if(prm.option1 == 0 && super.numMode > 1) {
            e.piercing = 0;
         }

         super.worldObj.spawnEntityInWorld(e);
      } else {
         super.optionParameter1 = this.getCurrentMode();
         MCH_PlayerViewHandler.applyRecoil(getInfo().getRecoilPitch(), getInfo().getRecoilYaw(), getInfo().recoilRecoverFactor);
      }

      return true;
   }
}
