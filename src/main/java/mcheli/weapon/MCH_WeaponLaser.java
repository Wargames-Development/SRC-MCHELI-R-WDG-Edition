package mcheli.weapon;

import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mcheli.*;
import mcheli.aircraft.*;
import mcheli.chain.MCH_EntityChain;
import mcheli.network.packets.PacketWeaponLaserShooting;
import mcheli.tank.MCH_EntityTank;
import mcheli.wrapper.W_Entity;
import mcheli.wrapper.W_MovingObjectPosition;
import net.minecraft.client.particle.EntityCloudFX;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.*;
import net.minecraft.world.World;

import java.util.List;

public class MCH_WeaponLaser extends MCH_WeaponBase {


    public MCH_WeaponLaser(World w, Vec3 v, float yaw, float pitch, String nm, MCH_WeaponInfo wi) {
        super(w, v, yaw, pitch, nm, wi);
        super.power = 8;
        super.acceleration = 4.0F;
        super.explosionPower = 0;
        super.interval = 0;
    }

    @Override
    public boolean shot(MCH_WeaponParam prm) {
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
        updateLaserBB(prm, tX, tY, tZ);
        onLaserStrike(super.worldObj, prm, tX, tY, tZ);
        if (!super.worldObj.isRemote) {
            this.playSound(prm.entity);
        } else {

        }
        return true;
    }

    private void updateLaserBB(MCH_WeaponParam prm, double tX, double tY, double tZ) {
    }


    private void onLaserStrike(World worldObj,
                               MCH_WeaponParam prm,
                               double tX, double tY, double tZ) {

        double defaultRange = weaponInfo.length;
        Vec3 look = Vec3.createVectorHelper(tX, tY, tZ);
        Vec3 start = Vec3.createVectorHelper(prm.posX, prm.posY, prm.posZ);
        Vec3 end = start.addVector(look.xCoord * defaultRange, look.yCoord * defaultRange, look.zCoord * defaultRange);
        MovingObjectPosition result = laserRaytrace(prm, start, look, end);

        Vec3 hitVec = (result != null && result.hitVec != null) ? result.hitVec : end;
        // 颜色 & 宽度 & 生存时长（按需可从 config / weapon info 里读）
        int argb = weaponInfo.color.toARGB();
        float width = weaponInfo.radius;
        int life = weaponInfo.timeFuse;
        boolean pulsate = true;
        if (!worldObj.isRemote) {
            MCH_MOD.getPacketHandler().sendToAllExcept(new PacketWeaponLaserShooting(start, hitVec, argb, width, life, pulsate, weaponInfo.turningFactor),
                prm.posX, prm.posY, prm.posZ, 1000F, prm.user instanceof EntityPlayer ? (EntityPlayer) prm.user : null, prm.user.dimension);
        } else {
            renderLaser(start, end, argb, width, life, pulsate);
        }

        if (result != null) {
            if (result.entityHit != null) {
                if (!worldObj.isRemote) {
                    Entity hit = result.entityHit;
                    MCH_Lib.applyEntityHurtResistantTimeConfig(hit);
                    DamageSource ds = DamageSource.causeThrownDamage(prm.entity, prm.user);
                    float damage = MCH_Config.applyDamageVsEntity(hit, ds, power);
                    damage *= (this.getInfo() != null) ? this.getInfo().getDamageFactor(hit) : 1.0F;
                    if(weaponInfo.enableBulletDecay) {
                        float decayFactor = 1f;
                        float dist = (float) start.distanceTo(hitVec);
                        for (MCH_IBulletDecay decay : weaponInfo.bulletDecay) {
                            decayFactor = decay.calculateDecayFactor(dist);
                        }
                        damage *= decayFactor;
                    }
                    if(hit instanceof EntityLivingBase) {
                        hit.setFire(5);
                    }
                    hit.attackEntityFrom(ds, damage);
                    if (prm.user instanceof EntityPlayer && prm.entity instanceof MCH_EntityAircraft) {
                        MCH_PacketNotifyHitBullet.send((MCH_EntityAircraft) prm.entity, (EntityPlayer) prm.user);
                    }
                } else {
                    spawnBlockPar(result);
                }
            } else if (W_MovingObjectPosition.isHitTypeTile(result)) {
                if (worldObj.isRemote) {
                    spawnBlockPar(result);
                }
            }

        }

    }

    @SideOnly(Side.CLIENT)
    public void renderLaser(Vec3 start, Vec3 hit, int argb, float width, int life, boolean pulsate) {
        MCH_RenderLaser.addBeam(start, hit, argb, width, life, pulsate, weaponInfo.turningFactor);
    }


    @SideOnly(Side.CLIENT)
    public void spawnBlockPar(MovingObjectPosition raytraceResult) {
        for (int i = 0; i < getInfo().numParticlesFlak; i++) {

            final double px = raytraceResult.hitVec.xCoord + (rand.nextFloat() - 0.5D);
            final double py = raytraceResult.hitVec.yCoord + 0.1D;
            final double pz = raytraceResult.hitVec.zCoord + (rand.nextFloat() - 0.5D);

            final double vx = (getInfo().flakParticlesDiff / 2.0) * rand.nextGaussian();
            final double vz = (getInfo().flakParticlesDiff / 2.0) * rand.nextGaussian();
            final double vy = (getInfo().flakParticlesDiff)       * Math.abs(rand.nextGaussian());

            EntityFX fxWhite = new EntityCloudFX(worldObj, px, py, pz, 0D, 0D, 0D);
            fxWhite.motionX += vx;
            fxWhite.motionZ += vz;
            fxWhite.motionY += vy;
            fxWhite.renderDistanceWeight = 500D;
            FMLClientHandler.instance().getClient().effectRenderer.addEffect(fxWhite);

            EntityFX fxBlack = new net.minecraft.client.particle.EntitySmokeFX(worldObj, px, py, pz, 0D, 0D, 0D);
            fxBlack.motionX += vx * 0.8;
            fxBlack.motionZ += vz * 0.8;
            fxBlack.motionY += vy * 0.7;
            fxBlack.renderDistanceWeight = 500D;
            fxBlack.setRBGColorF(0.08F, 0.08F, 0.08F);
            fxBlack.setAlphaF(0.9F);
            FMLClientHandler.instance().getClient().effectRenderer.addEffect(fxBlack);

            EntityFX fxFlame = new net.minecraft.client.particle.EntityFlameFX(worldObj, px, py, pz, 0D, 0D, 0D);
            fxFlame.motionX += vx * 0.4;
            fxFlame.motionZ += vz * 0.4;
            fxFlame.motionY += (vy * 0.5) + 0.02;
            fxFlame.renderDistanceWeight = 500D;
            FMLClientHandler.instance().getClient().effectRenderer.addEffect(fxFlame);
        }
    }


    public static boolean canHitByLaser(Entity entity, MCH_WeaponParam prm) {
        if (entity == null || entity.isDead) return false;
        if (entity instanceof MCH_EntityChain) return false;
        if (entity instanceof MCH_EntityBaseBullet) {
            MCH_EntityBaseBullet b = (MCH_EntityBaseBullet) entity;
            if (b.getInfo() != null) {
                if (!b.getInfo().canBeIntercepted) {
                    return false;
                }
            }
            if (W_Entity.isEqual(b.shootingAircraft, prm.entity)) return false;
            if (W_Entity.isEqual(b.shootingEntity, prm.user)) return false;
        }
        if (entity instanceof MCH_EntitySeat) return false;
        if (entity instanceof MCH_EntityHide) return false;
        if (entity instanceof MCH_EntityHitBox) {
            if(W_Entity.isEqual(((MCH_EntityHitBox) entity).parent, prm.entity)) return false;
        }
        if (W_Entity.isEqual(entity, prm.user)) return false;
        if (W_Entity.isEqual(entity, prm.user.ridingEntity)) return false;
        if (prm.entity instanceof MCH_EntityAircraft) {
            if (W_Entity.isEqual(entity, prm.entity)) return false;
            if (((MCH_EntityAircraft) prm.entity).isMountedEntity(entity)) return false;
        }
        return true;
    }


    public static MovingObjectPosition laserRaytrace(MCH_WeaponParam param, Vec3 start, Vec3 lookVec, Vec3 end) {
        double maxDoubledDist = start.distanceTo(end);
        Entity user = param.user;
        Entity pointedEntity = null;
        MovingObjectPosition result = MCH_RayTracer.rayTrace(user.worldObj, start, end);
        double dist = maxDoubledDist;
        if(result != null) {
            dist = result.hitVec.distanceTo(start);
        }
        Vec3 hitVec = null;
        float f1 = 1.0F;
        List list = user.worldObj.getEntitiesWithinAABBExcludingEntity(user,
            user.boundingBox.addCoord(lookVec.xCoord * maxDoubledDist, lookVec.yCoord * maxDoubledDist, lookVec.zCoord * maxDoubledDist).expand(f1, f1, f1));
        double d2 = dist;

        for (Object o : list) {
            Entity entity = (Entity) o;
            if (entity.canBeCollidedWith()
                && canHitByLaser(entity, param)) {
                float f2 = entity.getCollisionBorderSize();
                AxisAlignedBB axisalignedbb = entity.boundingBox.expand(f2, f2, f2);
                MovingObjectPosition movingobjectposition = axisalignedbb.calculateIntercept(start, end);
                if (axisalignedbb.isVecInside(start)) {
                    if (0.0D < d2 || d2 == 0.0D) {
                        pointedEntity = entity;
                        hitVec = movingobjectposition == null ? start : movingobjectposition.hitVec;
                        d2 = 0.0D;
                    }
                } else if (movingobjectposition != null) {
                    double d3 = start.distanceTo(movingobjectposition.hitVec);
                    if (d3 < d2 || d2 == 0.0D) {
                        if (entity == user.ridingEntity && !entity.canRiderInteract()) {
                            if (d2 == 0.0D) {
                                pointedEntity = entity;
                                hitVec = movingobjectposition.hitVec;
                            }
                        } else {
                            pointedEntity = entity;
                            hitVec = movingobjectposition.hitVec;
                            d2 = d3;
                        }
                    }
                }
            }
        }

        if(pointedEntity != null && (d2 < dist || result == null)) {
            result = new MovingObjectPosition(pointedEntity, hitVec);
        }

        return result;
    }


}
