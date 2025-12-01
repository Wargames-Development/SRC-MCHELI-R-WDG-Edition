package mcheli.weapon;

import mcheli.MCH_Lib;
import mcheli.MCH_PlayerViewHandler;
import mcheli.aircraft.MCH_AircraftInfo;
import mcheli.tank.MCH_EntityTank;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public class MCH_WeaponMachineGun2 extends MCH_WeaponBase {

    public MCH_WeaponMachineGun2(World w, Vec3 v, float yaw, float pitch, String nm, MCH_WeaponInfo wi) {
        super(w, v, yaw, pitch, nm, wi);
        super.power = 16;
        super.acceleration = 4.0F;
        super.explosionPower = 1;
        super.numMode = 2;
    }

    public void modifyParameters() {
        if (super.explosionPower == 0) {
            super.numMode = 0;
        }

    }

    public String getName() {
        return super.getName() + (this.getCurrentMode() == 0 ? "" : " [HE]");
    }

    public boolean shot(MCH_WeaponParam prm) {
        if (!super.worldObj.isRemote) {
            float yaw, pitch;
            if (prm.entity instanceof MCH_EntityTank) {
                MCH_EntityTank tank = (MCH_EntityTank) prm.entity;
                yaw = prm.user.rotationYaw;
                pitch = prm.user.rotationPitch;
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
            MCH_EntityBullet e = new MCH_EntityBullet(super.worldObj, prm.posX, prm.posY, prm.posZ, tX, tY, tZ, yaw, pitch, super.acceleration);
            e.setAirburstDist(this.airburstDist);
            e.setName(super.name);
            e.setParameterFromWeapon(this, prm.entity, prm.user);
            if (this.getInfo().modeNum < 2) {
                e.explosionPower = super.explosionPower;
            } else {
                e.explosionPower = prm.option1 == 0 ? -super.explosionPower : super.explosionPower;
            }

            e.posX += e.motionX * 0.5D;
            e.posY += e.motionY * 0.5D;
            e.posZ += e.motionZ * 0.5D;
            super.worldObj.spawnEntityInWorld(e);
            this.playSound(prm.entity);
        } else {
            super.optionParameter1 = this.getCurrentMode();
            MCH_PlayerViewHandler.applyRecoil(getInfo().getRecoilPitch(), getInfo().getRecoilYaw(), getInfo().recoilRecoverFactor);
        }

        return true;
    }
}
