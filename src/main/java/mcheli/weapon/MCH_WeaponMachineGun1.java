package mcheli.weapon;

import mcheli.MCH_Lib;
import mcheli.MCH_PlayerViewHandler;
import mcheli.aircraft.MCH_AircraftInfo;
import mcheli.tank.MCH_EntityTank;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public class MCH_WeaponMachineGun1 extends MCH_WeaponBase {

    public MCH_WeaponMachineGun1(World w, Vec3 v, float yaw, float pitch, String nm, MCH_WeaponInfo wi) {
        super(w, v, yaw, pitch, nm, wi);
        super.power = 8;
        super.acceleration = 4.0F;
        super.explosionPower = 0;
        super.interval = 0;
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

            double baseTXX = -MathHelper.sin(yaw / 180.0F * (float) Math.PI) * MathHelper.cos(pitch / 180.0F * (float) Math.PI);
            double baseTZZ = MathHelper.cos(yaw / 180.0F * (float) Math.PI) * MathHelper.cos(pitch / 180.0F * (float) Math.PI);
            double baseTYY = -MathHelper.sin(pitch / 180.0F * (float) Math.PI);

            if (getInfo().canister <= 0) {
                double x = prm.posX;
                double y = prm.posY;
                double z = prm.posZ;

                MCH_EntityBullet e = new MCH_EntityBullet(super.worldObj, x, y, z, baseTXX, baseTYY, baseTZZ, yaw, pitch, super.acceleration);
                e.setAirburstDist(this.airburstDist);
                e.setName(super.name);
                e.setParameterFromWeapon(this, prm.entity, prm.user);
                e.posX += e.motionX * 0.5D;
                e.posY += e.motionY * 0.5D;
                e.posZ += e.motionZ * 0.5D;
                super.worldObj.spawnEntityInWorld(e);
                this.playSound(prm.entity);
                return true;
            }

            int pellets = getInfo().canister;
            float diff = getInfo().bombletDiff;
            int type = getInfo().canisterType;
            float seqSpacing = getInfo().bombletSTime;

            for (int i = 0; i < pellets; i++) {
                double x = prm.posX;
                double y = prm.posY;
                double z = prm.posZ;
                double tX = baseTXX;
                double tY = baseTYY;
                double tZ = baseTZZ;
                float useYaw = yaw;
                float usePitch = pitch;

                if (type == 0) {
                    double ox = (Math.random() * 2.0D - 1.0D) * diff;
                    double oy = (Math.random() * 2.0D - 1.0D) * diff;
                    double oz = (Math.random() * 2.0D - 1.0D) * diff;
                    x += ox;
                    y += oy;
                    z += oz;
                } else if (type == 1) {
                    double dyaw = (Math.random() * 2.0D - 1.0D) * diff;
                    double dpitch = (Math.random() * 2.0D - 1.0D) * diff;
                    useYaw = yaw + (float) dyaw;
                    usePitch = pitch + (float) dpitch;
                    tX = -MathHelper.sin(useYaw / 180.0F * (float) Math.PI) * MathHelper.cos(usePitch / 180.0F * (float) Math.PI);
                    tZ = MathHelper.cos(useYaw / 180.0F * (float) Math.PI) * MathHelper.cos(usePitch / 180.0F * (float) Math.PI);
                    tY = -MathHelper.sin(usePitch / 180.0F * (float) Math.PI);
                } else if (type == 2) {
                    double dyaw = (Math.random() * 2.0D - 1.0D) * diff;
                    double dpitch = (Math.random() * 2.0D - 1.0D) * diff;
                    useYaw = yaw + (float) dyaw;
                    usePitch = pitch + (float) dpitch;
                    tX = -MathHelper.sin(useYaw / 180.0F * (float) Math.PI) * MathHelper.cos(usePitch / 180.0F * (float) Math.PI);
                    tZ = MathHelper.cos(useYaw / 180.0F * (float) Math.PI) * MathHelper.cos(usePitch / 180.0F * (float) Math.PI);
                    tY = -MathHelper.sin(usePitch / 180.0F * (float) Math.PI);
                    x += tX * (double) i * seqSpacing;
                    y += tY * (double) i * seqSpacing;
                    z += tZ * (double) i * seqSpacing;
                }

                MCH_EntityBullet e = new MCH_EntityBullet(super.worldObj, x, y, z, tX, tY, tZ, useYaw, usePitch, super.acceleration);
                e.setAirburstDist(this.airburstDist);
                e.setName(super.name);
                e.setParameterFromWeapon(this, prm.entity, prm.user);
                e.posX += e.motionX * 0.5D;
                e.posY += e.motionY * 0.5D;
                e.posZ += e.motionZ * 0.5D;
                super.worldObj.spawnEntityInWorld(e);
            }

            this.playSound(prm.entity);
        } else {
            MCH_PlayerViewHandler.applyRecoil(getInfo().getRecoilPitch(), getInfo().getRecoilYaw(), getInfo().recoilRecoverFactor);
        }
        return true;
    }

}
