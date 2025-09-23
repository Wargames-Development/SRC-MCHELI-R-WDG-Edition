package mcheli.weapon;

import mcheli.MCH_Config;
import mcheli.MCH_Lib;
import mcheli.MCH_PlayerViewHandler;
import mcheli.aircraft.MCH_AircraftInfo;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.aircraft.MCH_PacketNotifyHitBullet;
import mcheli.tank.MCH_EntityTank;
import mcheli.wrapper.W_Entity;
import mcheli.wrapper.W_EntityPlayer;
import mcheli.wrapper.W_MovingObjectPosition;
import mcheli.wrapper.W_WorldFunc;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.*;
import net.minecraft.world.World;

import java.util.List;

public class MCH_WeaponLaser extends MCH_WeaponBase {

    public MCH_WeaponLaser(World w, Vec3 v, float yaw, float pitch, String nm, MCH_WeaponInfo wi) {
        super(w, v, yaw, pitch, nm, wi);
        super.power = 10; // 激光的伤害
        super.acceleration = 0.0F; // 激光不需要加速度
        super.explosionPower = 0;
        super.interval = 0;
    }
    @Override
    public boolean shot(MCH_WeaponParam prm) {
//        if (!super.worldObj.isRemote) {
//            float yaw, pitch;
//
//            if (prm.entity instanceof MCH_EntityTank) {
//                MCH_EntityTank tank = (MCH_EntityTank) prm.entity;
//                yaw = prm.user.rotationYaw;
//                pitch = prm.user.rotationPitch;
//                yaw += prm.randYaw;
//                pitch += prm.randPitch;
//
//                int wid = tank.getCurrentWeaponID(prm.user);
//                MCH_AircraftInfo.Weapon w = tank.getAcInfo().getWeaponById(wid);
//                float minPitch = w == null ? tank.getAcInfo().minRotationPitch : w.minPitch;
//                float maxPitch = w == null ? tank.getAcInfo().maxRotationPitch : w.maxPitch;
//
//                float playerYaw = MathHelper.wrapAngleTo180_float(tank.getRotYaw() - yaw);
//                float playerPitch = tank.getRotPitch() * MathHelper.cos((float) (playerYaw * Math.PI / 180.0D))
//                        + -tank.getRotRoll() * MathHelper.sin((float) (playerYaw * Math.PI / 180.0D));
//                float playerYawRel = MathHelper.wrapAngleTo180_float(yaw - tank.getRotYaw());
//                float yawLimit = (w == null ? 360F : w.maxYaw);
//                float relativeYaw = MCH_Lib.RNG(playerYawRel, -yawLimit, yawLimit);
//                yaw = MathHelper.wrapAngleTo180_float(tank.getRotYaw() + relativeYaw);
//                pitch = MCH_Lib.RNG(pitch, playerPitch + minPitch, playerPitch + maxPitch);
//                pitch = MCH_Lib.RNG(pitch, -90.0F, 90.0F);
//            } else {
//                yaw = prm.rotYaw;
//                pitch = prm.rotPitch;
//            }
//
//            // === 射线方向与范围 ===
//            double tX = -MathHelper.sin(yaw / 180.0F * (float)Math.PI) * MathHelper.cos(pitch / 180.0F * (float)Math.PI);
//            double tY = -MathHelper.sin(pitch / 180.0F * (float)Math.PI);
//            double tZ =  MathHelper.cos(yaw / 180.0F * (float)Math.PI) * MathHelper.cos(pitch / 180.0F * (float)Math.PI);
//            final double RANGE = 300.0D;
//
//            // 用 W_WorldFunc.getWorldVec3 与子弹逻辑保持一致
//            Vec3 start = W_WorldFunc.getWorldVec3(super.worldObj, prm.posX, prm.posY, prm.posZ);
//            Vec3 endFull = W_WorldFunc.getWorldVec3(super.worldObj, prm.posX + tX * RANGE, prm.posY + tY * RANGE, prm.posZ + tZ * RANGE);
//
//            // ======================================================================
//            // 1) 多次 “钻透” 可破坏方块（最多 5 次），与子弹一致
//            // ======================================================================
//            MovingObjectPosition m = null;
//            Vec3 probeStart = start;
//            Vec3 probeEnd   = endFull;
//
//            for (int k = 0; k < 5; ++k) {
//                m = W_WorldFunc.clip(super.worldObj, probeStart, probeEnd);
//                boolean drilled = false;
//                if (W_MovingObjectPosition.isHitTypeTile(m)) {
//                    Block b = W_WorldFunc.getBlock(super.worldObj, m.blockX, m.blockY, m.blockZ);
//                    if (MCH_Config.bulletBreakableBlocks.contains(b)) {
//                        // 破坏后继续向前探测
//                        W_WorldFunc.destroyBlock(super.worldObj, m.blockX, m.blockY, m.blockZ, true);
//                        probeStart = W_WorldFunc.getWorldVec3(super.worldObj, m.hitVec.xCoord, m.hitVec.yCoord, m.hitVec.zCoord);
//                        drilled = true;
//                    }
//                }
//                if (!drilled) break;
//            }
//
//            // 最终一次方块 clip（若命中则把 end 截到方块命中点）
//            m = W_WorldFunc.clip(super.worldObj, probeStart, probeEnd);
//            Vec3 endForEntity = probeEnd;
//            double blockDist = probeStart.distanceTo(probeEnd);
//            if (W_MovingObjectPosition.isHitTypeTile(m)) {
//                endForEntity = W_WorldFunc.getWorldVec3(super.worldObj, m.hitVec.xCoord, m.hitVec.yCoord, m.hitVec.zCoord);
//                blockDist = probeStart.distanceTo(endForEntity);
//            } else {
//                m = null;
//            }
//
//            // ======================================================================
//            // 2) 实体候选收集：用大走廊 AABB（expand 21），与子弹一致
//            // ======================================================================
//            AxisAlignedBB sweep = AxisAlignedBB.getBoundingBox(
//                    Math.min(probeStart.xCoord, endForEntity.xCoord),
//                    Math.min(probeStart.yCoord, endForEntity.yCoord),
//                    Math.min(probeStart.zCoord, endForEntity.zCoord),
//                    Math.max(probeStart.xCoord, endForEntity.xCoord),
//                    Math.max(probeStart.yCoord, endForEntity.yCoord),
//                    Math.max(probeStart.zCoord, endForEntity.zCoord)
//            ).expand(21.0D, 21.0D, 21.0D);
//
//            @SuppressWarnings("unchecked")
//            List<Entity> list = super.worldObj.getEntitiesWithinAABBExcludingEntity(prm.user, sweep);
//
//            // 选取最近实体（严格早于方块）
//            Entity nearest = null;
//            double nearestDist = 0.0D;
//
//            if (list != null && !list.isEmpty()) {
//                for (Entity e : list) {
//                    if (!canRayHitEntityLikeBullet(e, prm)) continue;
//
//                    // 与子弹一致：把盒子略微放大
//                    AxisAlignedBB aabb = e.boundingBox.expand(0.3D, 0.3D, 0.3D);
//
//                    // 处理“起点在盒内”的情况：直接视为距离 0
//                    MovingObjectPosition emop = null;
//                    if (aabb.isVecInside(probeStart)) {
//                        emop = new MovingObjectPosition(e);
//                        // 构造一个命中点（用于 distance 计算稳定）
//                        emop.hitVec = probeStart;
//                    } else {
//                        // 飞机优先用 OBB 射线检测；失败则退回 AABB
//                        if (e instanceof MCH_EntityAircraft) {
//                            try {
//                                emop = ((MCH_EntityAircraft) e).getBoundingBox().calculateIntercept(probeStart, endForEntity);
//                                if (emop == null) {
//                                    emop = aabb.calculateIntercept(probeStart, endForEntity);
//                                }
//                            } catch (Throwable t) {
//                                emop = aabb.calculateIntercept(probeStart, endForEntity);
//                            }
//                        } else {
//                            emop = aabb.calculateIntercept(probeStart, endForEntity);
//                        }
//                    }
//
//                    if (emop != null && emop.hitVec != null) {
//                        double d1 = probeStart.distanceTo(emop.hitVec);
//                        if (nearest == null || d1 < nearestDist) {
//                            nearest = e;
//                            nearestDist = d1;
//                        }
//                    }
//                }
//            }
//
//            // 只有当实体严格早于方块时才命中实体（与子弹一致：我们把 vec31 截到了方块处）
//            if (nearest != null && nearestDist <= blockDist + 1.0e-6) {
//                if (!nearest.isDead) {
//                    MCH_Lib.applyEntityHurtResistantTimeConfig(nearest);
//                    DamageSource ds = DamageSource.causeThrownDamage(prm.user, prm.user);
//                    float damageFactor = 1.0F;
//                    float damage = MCH_Config.applyDamageVsEntity(nearest, ds, (float) power * damageFactor);
//                    damage *= (this.getInfo() != null) ? this.getInfo().getDamageFactor(nearest) : 1.0F;
//                    nearest.attackEntityFrom(ds, damage);
//                }
//                if (prm.entity instanceof MCH_EntityAircraft && prm.user instanceof EntityPlayer) {
//                    MCH_PacketNotifyHitBullet.send((MCH_EntityAircraft) prm.entity, (EntityPlayer) prm.user);
//                }
//                if (W_EntityPlayer.isPlayer(prm.user)) {
//                    MCH_PacketNotifyHitBullet.send(null, (EntityPlayer) prm.user);
//                }
//            } else if (m != null) {
//                // 命中方块：只破坏白名单内的
//                Block hitBlock = W_WorldFunc.getBlock(super.worldObj, m.blockX, m.blockY, m.blockZ);
//                if (MCH_Config.bulletBreakableBlocks.contains(hitBlock)) {
//                    W_WorldFunc.destroyBlock(super.worldObj, m.blockX, m.blockY, m.blockZ, true);
//                }
//            }
//
//            this.playSound(prm.entity);
//        } else {
//            // 客户端：视角后坐
//            MCH_PlayerViewHandler.applyRecoil(getInfo().getRecoilPitch(), getInfo().getRecoilYaw(), getInfo().recoilRecoverFactor);
//        }
        return true;
    }

//    private boolean canRayHitEntityLikeBullet(Entity entity, MCH_WeaponParam prm) {
//        if (entity == null || entity.isDead) return false;
//        if (!entity.canBeCollidedWith()) return false;
//
//        // 链/绳
//        if (entity instanceof mcheli.chain.MCH_EntityChain) return false;
//
//        // 子弹互撞（激光不需要）：保持与子弹一致的排除
//        if (entity instanceof MCH_EntityBaseBullet) {
//            if (super.worldObj.isRemote) return false;
//            MCH_EntityBaseBullet b = (MCH_EntityBaseBullet) entity;
//            if (W_Entity.isEqual(b.shootingAircraft, prm.entity)) return false;
//            if (W_Entity.isEqual(b.shootingEntity, prm.user)) return false;
//        }
//
//        // 座椅 / HitBox
//        if (entity instanceof mcheli.aircraft.MCH_EntitySeat) return false;
//        if (entity instanceof mcheli.aircraft.MCH_EntityHitBox) return false;
//
//        // 自身玩家
//        if (W_Entity.isEqual(entity, prm.user)) return false;
//
//        // 自己的载具及其乘员
//        if (prm.entity instanceof MCH_EntityAircraft) {
//            MCH_EntityAircraft ac = (MCH_EntityAircraft) prm.entity;
//            if (W_Entity.isEqual(entity, ac)) return false;
//            if (ac.isMountedEntity(entity)) return false;
//        }
//
//        return true;
//    }

}
