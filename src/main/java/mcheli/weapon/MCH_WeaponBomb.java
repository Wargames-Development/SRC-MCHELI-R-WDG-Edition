package mcheli.weapon;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mcheli.MCH_Explosion;
import mcheli.MCH_ExplosionParam;
import mcheli.MCH_RayTracer;
import mcheli.aircraft.MCH_EntityAircraft;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import mcheli.wrapper.W_WorldFunc;

import java.util.List;

public class MCH_WeaponBomb extends MCH_WeaponBase {

    public MCH_WeaponBomb(World w, Vec3 v, float yaw, float pitch, String nm, MCH_WeaponInfo wi) {
        super(w, v, yaw, pitch, nm, wi);
        super.acceleration = 0.5F;
        super.explosionPower = 9;
        super.power = 35;
        super.interval = -90;
    }

    public boolean shot(MCH_WeaponParam prm) {
        //自毁
        if (this.getInfo() != null && this.getInfo().destruct) {
            if (prm.entity instanceof MCH_EntityAircraft) {
                MCH_EntityAircraft e1 = (MCH_EntityAircraft) prm.entity;
                // 无人机且无座位：爆炸 + 自毁
                if (e1.isUAV() && e1.getSeatNum() == 0) {
                    if (!super.worldObj.isRemote) {
                        MCH_ExplosionParam p = MCH_ExplosionParam.builder()
                            .exploder(null) // 原代码就是 null
                            .player(prm.user instanceof EntityPlayer ? (EntityPlayer) prm.user : null)
                            .x(e1.posX).y(e1.posY).z(e1.posZ)
                            .size((float) this.getInfo().explosion)
                            .sizeBlock((float) this.getInfo().explosionBlock)
                            .isPlaySound(true)
                            .isSmoking(true)
                            .isFlaming(this.getInfo().flaming)
                            .isDestroyBlock(true)
                            .countSetFireEntity(0)
                            .isInWater(false)
                            .damageVsPlayer(getInfo().explosionDamageVsPlayer)
                            .damageVsLiving(getInfo().explosionDamageVsLiving)
                            .damageVsPlane(getInfo().explosionDamageVsPlane)
                            .damageVsHeli(getInfo().explosionDamageVsHeli)
                            .damageVsTank(getInfo().explosionDamageVsTank)
                            .damageVsVehicle(getInfo().explosionDamageVsVehicle)
                            .damageVsShip(getInfo().explosionDamageVsShip)
                            .build();
                        MCH_Explosion.newExplosion(super.worldObj, p);
                        this.playSound(prm.entity);
                    }
                    e1.destruct();
                }
                // 非无人机：爆炸 + 额外伤害
                if (!e1.isUAV()) {
                    if (!super.worldObj.isRemote) {
                        MCH_ExplosionParam p = MCH_ExplosionParam.builder()
                            .exploder(null)
                            .player(prm.user instanceof EntityPlayer ? (EntityPlayer) prm.user : null)
                            .x(e1.posX).y(e1.posY).z(e1.posZ)
                            .size((float) this.getInfo().explosion)
                            .sizeBlock((float) this.getInfo().explosionBlock)
                            .isPlaySound(true)
                            .isSmoking(true)
                            .isFlaming(this.getInfo().flaming)
                            .isDestroyBlock(true)
                            .countSetFireEntity(0)
                            .isInWater(false)
                            .damageVsPlayer(getInfo().explosionDamageVsPlayer)
                            .damageVsLiving(getInfo().explosionDamageVsLiving)
                            .damageVsPlane(getInfo().explosionDamageVsPlane)
                            .damageVsHeli(getInfo().explosionDamageVsHeli)
                            .damageVsTank(getInfo().explosionDamageVsTank)
                            .damageVsVehicle(getInfo().explosionDamageVsVehicle)
                            .damageVsShip(getInfo().explosionDamageVsShip)
                            .build();
                        MCH_Explosion.newExplosion(super.worldObj, p);
                        this.playSound(prm.entity);
                    }
                    if (prm.user instanceof EntityPlayer) {
                        e1.attackEntityFrom(DamageSource.inWall, 1000000);
                    }
                }
            }
        } else {
            MCH_GPSPosition gpsPosition = null;
            if (this.getInfo() != null && this.getInfo().isGPSMissile) {
                gpsPosition = super.worldObj.isRemote ? MCH_GPSPosition.currentClientGPSPosition : MCH_GPSPosition.get(prm.user);
                if (!MCH_GPSPosition.isUsableTarget(gpsPosition)) {
                    return false;
                }
            }
            if (super.worldObj.isRemote) {
                return true;
            }
            this.playSound(prm.entity);
            MCH_EntityBomb e = new MCH_EntityBomb(super.worldObj, prm.posX, prm.posY, prm.posZ, prm.entity.motionX, prm.entity.motionY, prm.entity.motionZ, prm.entity.rotationYaw, 0.0F, (double) super.acceleration);
            e.setInfoByName(super.name);
            e.setParameterFromWeapon(this, prm.entity, prm.user);
            if (gpsPosition != null) {
                e.setGpsTarget(gpsPosition.x, gpsPosition.y, gpsPosition.z);
            }
            e.motionX = prm.entity.motionX;
            e.motionY = prm.entity.motionY;
            e.motionZ = prm.entity.motionZ;
            super.worldObj.spawnEntityInWorld(e);
        }

        return true;
    }

    @SideOnly(Side.CLIENT)
    private void clientLockGpsTarget(MCH_WeaponParam prm) {
        Minecraft.getMinecraft().getSoundHandler().playSound(
            new PositionedSoundRecord(new ResourceLocation("mcheli:mark"), 10.0F, 1.0F,
                (float) prm.user.posX, (float) prm.user.posY, (float) prm.user.posZ));
        float yaw = prm.user.rotationYaw;
        float pitch = prm.user.rotationPitch;
        if (Minecraft.getMinecraft().gameSettings.thirdPersonView != 0) {
            EntityLivingBase e = Minecraft.getMinecraft().renderViewEntity;
            yaw = e.rotationYaw;
            pitch = e.rotationPitch;
        }
        double targetX = -MathHelper.sin(yaw / 180.0F * (float)Math.PI) * MathHelper.cos(pitch / 180.0F * (float)Math.PI);
        double targetZ = MathHelper.cos(yaw / 180.0F * (float)Math.PI) * MathHelper.cos(pitch / 180.0F * (float)Math.PI);
        double targetY = -MathHelper.sin(pitch / 180.0F * (float)Math.PI);
        double dist = MathHelper.sqrt_double(targetX * targetX + targetY * targetY + targetZ * targetZ);
        double maxDist = 1500.0D;
        double segmentLength = 100.0D;
        int numSegments = (int)(maxDist / segmentLength);
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
        if (prm.user instanceof EntityPlayer && hitResult.hitVec != null) {
            MCH_GPSPosition.set(hitResult.hitVec.xCoord, hitResult.hitVec.yCoord, hitResult.hitVec.zCoord, true, prm.user);
        }
    }

    @Override
    public boolean lock(MCH_WeaponParam prm) {
        if (this.getInfo() != null && this.getInfo().isGPSMissile && super.worldObj.isRemote) {
            clientLockGpsTarget(prm);
        }
        return false;
    }
}
