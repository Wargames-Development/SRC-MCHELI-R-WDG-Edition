package mcheli.weapon;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mcheli.MCH_Lib;
import mcheli.MCH_PlayerViewHandler;
import mcheli.aircraft.MCH_AircraftInfo;
import mcheli.tank.MCH_EntityTank;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public class MCH_WeaponRailgun extends MCH_WeaponBase {

    private int lockCount;
    private int prevLockCount;
    @SideOnly(Side.CLIENT)
    private ISound lockSound;

    public MCH_WeaponRailgun(World w, Vec3 v, float yaw, float pitch, String nm, MCH_WeaponInfo wi) {
        super(w, v, yaw, pitch, nm, wi);
        super.power = 32;
        super.acceleration = 10.0F;
        super.explosionPower = 2;
        super.interval = 0;
    }

    @Override
    public void update(int countWait) {
        super.update(countWait);
        if (this.worldObj != null && this.worldObj.isRemote) {
            if (this.lockCount != this.prevLockCount) {
                this.prevLockCount = this.lockCount;
            } else {
                this.lockCount = this.prevLockCount = 0;
                stopLockSound();
            }
        }
    }

    @SideOnly(Side.CLIENT)
    public void stopLockSound() {
        if (lockSound != null) {
            Minecraft.getMinecraft().getSoundHandler().stopSound(lockSound);
            lockSound = null;
        }
    }

    // 播放蓄力音效，只在蓄力开始时播放一次
    @SideOnly(Side.CLIENT)
    public void playLockSound(float x, float y, float z) {
        if (lockSound == null) {
            //lockSound = PositionedSoundRecord.func_147674_a(new ResourceLocation("mcheli:" + getInfo().railgunSound), 1.0F);
            lockSound = new PositionedSoundRecord(new ResourceLocation("mcheli:" + getInfo().railgunSound), 10.0F, 1.0F, x, y, z);
            Minecraft.getMinecraft().getSoundHandler().playSound(lockSound);
        }
    }

    public boolean railGunShot(MCH_WeaponParam prm) {
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
            e.posX += e.motionX * 0.5D;
            e.posY += e.motionY * 0.5D;
            e.posZ += e.motionZ * 0.5D;
            super.worldObj.spawnEntityInWorld(e);
            this.playSound(prm.entity);
        } else {
            MCH_PlayerViewHandler.applyRecoil(getInfo().getRecoilPitch(), getInfo().getRecoilYaw(), getInfo().recoilRecoverFactor);
        }
        return true;
    }

    // 处理射击事件
    @Override
    public boolean shot(MCH_WeaponParam prm) {
        if (!super.worldObj.isRemote) {
            return railGunShot(prm);
        } else {
            if (lockCount <= weaponInfo.lockTime) {
                if (lockCount == 1) {
                    playLockSound((float) prm.user.posX, (float) prm.user.posY, (float) prm.user.posZ);
                }
                lockCount++;
                if (lockCount == weaponInfo.lockTime) {
                    lockCount = 0;
                    return railGunShot(prm);
                }
            }

        }
        return false;  // 继续蓄力，未发射
    }

    public float getRailgunTime() {
        return (float) lockCount / weaponInfo.lockTime;
    }
}
