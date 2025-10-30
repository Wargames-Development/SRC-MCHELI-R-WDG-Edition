package mcheli.weapon;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mcheli.MCH_Lib;
import mcheli.MCH_PlayerViewHandler;
import mcheli.MCH_RayTracer;
import mcheli.aircraft.MCH_AircraftInfo;
import mcheli.tank.MCH_EntityTank;
import mcheli.wrapper.W_WorldFunc;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import java.util.List;

public class MCH_WeaponASMissile extends MCH_WeaponBase {

    private int lockCount;
    private int prevLockCount;

    public MCH_WeaponASMissile(World world, Vec3 position, float yaw, float pitch, String name, MCH_WeaponInfo weaponInfo) {
        super(world, position, yaw, pitch, name, weaponInfo);
        this.acceleration = 3.0F;  // 加速度
        this.explosionPower = 9;   // 爆炸威力
        this.power = 40;           // 武器威力
        this.interval = -350;      // 射击间隔
        if (world.isRemote) {
            this.interval -= 10;     // 如果是客户端，减少射击间隔
        }
    }

    public boolean isCooldownCountReloadTime() {
        return true;
    }


    public void update(int countWait) {
        super.update(countWait);
        if (this.worldObj != null && this.worldObj.isRemote) {
            if (this.lockCount != this.prevLockCount) {
                this.prevLockCount = this.lockCount;
            } else {
                this.lockCount = this.prevLockCount = 0;
            }
        }
    }

    @Override
    public boolean shot(MCH_WeaponParam prm) {
        if (getInfo().isGPSMissile) {
            if (!super.worldObj.isRemote) {
                this.playSound(prm.entity);
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
                MCH_EntityASMissile missile = new MCH_EntityASMissile(this.worldObj, prm.posX, prm.posY, prm.posZ, tX, tY, tZ, yaw, pitch, this.acceleration);
                missile.setName(this.name);
                missile.setParameterFromWeapon(this, prm.entity, prm.user);

                MCH_GPSPosition gpsPosition;
                if ((gpsPosition = MCH_GPSPosition.get(prm.user)) != null) {
                    if (gpsPosition.isActive) {
                        missile.targetPosX = gpsPosition.x;
                        missile.targetPosY = gpsPosition.y;
                        missile.targetPosZ = gpsPosition.z;
                        missile.targeting = true;
                    }
                }
                this.worldObj.spawnEntityInWorld(missile);
                playSound(prm.entity);
            } else {
                super.optionParameter1 = this.getCurrentMode();
                MCH_PlayerViewHandler.applyRecoil(getInfo().getRecoilPitch(), getInfo().getRecoilYaw(), getInfo().recoilRecoverFactor);
            }
            return true;
        } else {
            float yaw = prm.user.rotationYaw;
            float pitch = prm.user.rotationPitch;
            double targetX = -MathHelper.sin(yaw / 180.0F * (float) Math.PI) * MathHelper.cos(pitch / 180.0F * (float) Math.PI);
            double targetZ = MathHelper.cos(yaw / 180.0F * (float) Math.PI) * MathHelper.cos(pitch / 180.0F * (float) Math.PI);
            double targetY = -MathHelper.sin(pitch / 180.0F * (float) Math.PI);
            double dist = MathHelper.sqrt_double(targetX * targetX + targetY * targetY + targetZ * targetZ);
            double maxDist = 1500.0;
            double segmentLength = 100.0;
            int numSegments = (int) (maxDist / segmentLength);
            targetX = targetX * maxDist / dist;
            targetY = targetY * maxDist / dist;
            targetZ = targetZ * maxDist / dist;
            Vec3 src = W_WorldFunc.getWorldVec3(this.worldObj, prm.entity.posX, prm.entity.posY + prm.entity.getEyeHeight(), prm.entity.posZ);
            MovingObjectPosition hitResult = null;
            for (int i = 1; i <= numSegments; i++) {
                Vec3 currentDst = W_WorldFunc.getWorldVec3(this.worldObj,
                    prm.entity.posX + targetX * i / numSegments,
                    prm.entity.posY + prm.entity.getEyeHeight() + targetY * i / numSegments,
                    prm.entity.posZ + targetZ * i / numSegments);
                List<MovingObjectPosition> hitResults = MCH_RayTracer.rayTraceAllBlocks(this.worldObj, src, currentDst, false, true, true);
                if (hitResults != null && !hitResults.isEmpty()) {
                    hitResult = hitResults.get(0);
                    break;
                }
                src = currentDst;
            }
            if (hitResult == null) {
                hitResult = new MovingObjectPosition(null, src.addVector(targetX, targetY, targetZ));  // 使用目标点作为默认值
            }
            if (!this.worldObj.isRemote) {
                MCH_EntityASMissile missile = new MCH_EntityASMissile(this.worldObj, prm.posX, prm.posY, prm.posZ, targetX, targetY, targetZ, yaw, pitch, this.acceleration);
                missile.setName(this.name);
                missile.setParameterFromWeapon(this, prm.entity, prm.user);
                missile.targetPosX = hitResult.hitVec.xCoord;
                missile.targetPosY = hitResult.hitVec.yCoord;
                missile.targetPosZ = hitResult.hitVec.zCoord;
                missile.targeting = true;
                this.worldObj.spawnEntityInWorld(missile);
                playSound(prm.entity);
            } else {
                if (prm.user instanceof EntityPlayer) {
                    MCH_GPSPosition.set(hitResult.hitVec.xCoord, hitResult.hitVec.yCoord, hitResult.hitVec.zCoord, true, prm.user);
                }
            }
            return true;
        }
    }

    @SideOnly(Side.CLIENT)
    public void clientLock(MCH_WeaponParam prm) {
        Minecraft.getMinecraft().getSoundHandler().playSound(
            new PositionedSoundRecord(new ResourceLocation("mcheli:mark"), 10.0F, 1.0F,
                (float) prm.user.posX, (float) prm.user.posY, (float) prm.user.posZ));
        float yaw = prm.user.rotationYaw;
        float pitch = prm.user.rotationPitch;
        double targetX = -MathHelper.sin(yaw / 180.0F * (float) Math.PI) * MathHelper.cos(pitch / 180.0F * (float) Math.PI);
        double targetZ = MathHelper.cos(yaw / 180.0F * (float) Math.PI) * MathHelper.cos(pitch / 180.0F * (float) Math.PI);
        double targetY = -MathHelper.sin(pitch / 180.0F * (float) Math.PI);
        double dist = MathHelper.sqrt_double(targetX * targetX + targetY * targetY + targetZ * targetZ);
        double maxDist = 1500.0;
        double segmentLength = 100.0;
        int numSegments = (int) (maxDist / segmentLength);
        double posX = RenderManager.renderPosX;
        double posY = RenderManager.renderPosY;
        double posZ = RenderManager.renderPosZ;
        targetX = targetX * maxDist / dist;
        targetY = targetY * maxDist / dist;
        targetZ = targetZ * maxDist / dist;
        Vec3 src = W_WorldFunc.getWorldVec3(this.worldObj, posX, posY, posZ);
        MovingObjectPosition hitResult = null;
        for (int i = 1; i <= numSegments; i++) {
            Vec3 currentDst = W_WorldFunc.getWorldVec3(this.worldObj,
                posX + targetX * i / numSegments,
                posY + targetY * i / numSegments,
                posZ + targetZ * i / numSegments);
            List<MovingObjectPosition> hitResults = MCH_RayTracer.rayTraceAllBlocks(this.worldObj, src, currentDst, false, true, true);
            if (hitResults != null && !hitResults.isEmpty()) {
                hitResult = hitResults.get(0);
                break;
            }
            src = currentDst;
        }
        if (hitResult == null) {
            hitResult = new MovingObjectPosition(null, src.addVector(targetX, targetY, targetZ));
        }
        MCH_GPSPosition.set(hitResult.hitVec.xCoord, hitResult.hitVec.yCoord, hitResult.hitVec.zCoord, true, prm.user);
    }

    @Override
    public boolean lock(MCH_WeaponParam prm) {
        if (super.worldObj.isRemote) {
            if (lockCount <= weaponInfo.lockTime) {
                if (lockCount == 1) {
                }
                lockCount++;
                if (lockCount == weaponInfo.lockTime) {
                    lockCount = 0;
                    clientLock(prm);
                }
            }
        }
        return false;
    }

    public float getLockTime() {
        return (float) lockCount / weaponInfo.lockTime;
    }
}
