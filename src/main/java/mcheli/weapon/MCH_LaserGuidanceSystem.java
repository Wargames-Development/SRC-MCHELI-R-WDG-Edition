package mcheli.weapon;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mcheli.MCH_MOD;
import mcheli.MCH_PlayerViewHandler;
import mcheli.MCH_RayTracer;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.aircraft.MCH_EntityHide;
import mcheli.aircraft.MCH_EntityHitBox;
import mcheli.aircraft.MCH_EntitySeat;
import mcheli.chain.MCH_EntityChain;
import mcheli.flare.MCH_EntityChaff;
import mcheli.network.packets.PacketLaserGuidanceTargeting;
import mcheli.tank.MCH_EntityTank;
import mcheli.uav.MCH_EntityUavStation;
import mcheli.wrapper.W_Entity;
import mcheli.wrapper.W_WorldFunc;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import java.util.List;

import static mcheli.MCH_RayTracer.rayTraceAllBlocks;

public class MCH_LaserGuidanceSystem implements MCH_IGuidanceSystem {

    public World worldObj;
    public double targetPosX;
    public double targetPosY;
    public double targetPosZ;
    public boolean targeting = false;
    @SideOnly(Side.CLIENT)
    public MCH_EntityLockBox lockBox;
    public MCH_WeaponParam param;
    public boolean hasLaserGuidancePod = true;
    public boolean lockEntity = false;
    public boolean cameraFollowLockEntity = false;
    public float cameraFollowStrength = 0.3f;
    protected Entity user;
    public boolean sendToServer = false;

    @Override
    public double getLockPosX() {
        return targetPosX;
    }

    @Override
    public double getLockPosY() {
        return targetPosY;
    }

    @Override
    public double getLockPosZ() {
        return targetPosZ;
    }

    @Override
    public void update() {

        if (worldObj.isRemote) {

            if (!targeting) return;

            float yaw;
            float pitch;
            MCH_EntityAircraft ac = null; //玩家乘坐的实体
            if (user.ridingEntity instanceof MCH_EntityAircraft) {
                ac = (MCH_EntityAircraft) user.ridingEntity;
            } else if (user.ridingEntity instanceof MCH_EntitySeat) {
                ac = ((MCH_EntitySeat) user.ridingEntity).getParent();
            } else if (user.ridingEntity instanceof MCH_EntityUavStation) {
                ac = ((MCH_EntityUavStation) user.ridingEntity).getControlAircract();
            }

            if (hasLaserGuidancePod) {
                yaw = user.rotationYaw;  // 获取玩家的偏航角度
                pitch = user.rotationPitch;  // 获取玩家的俯仰角度
            } else {
                if (ac == null) return;
                yaw = ac.rotationYaw;
                pitch = ac.rotationPitch;
            }

            // 计算目标方向的三维坐标变化量
            double targetX = -MathHelper.sin(yaw / 180.0F * (float) Math.PI) * MathHelper.cos(pitch / 180.0F * (float) Math.PI);
            double targetZ = MathHelper.cos(yaw / 180.0F * (float) Math.PI) * MathHelper.cos(pitch / 180.0F * (float) Math.PI);
            double targetY = -MathHelper.sin(pitch / 180.0F * (float) Math.PI);

            // 计算方向的距离
            double maxDist = 1500.0;

            double posX = RenderManager.renderPosX;
            double posY = RenderManager.renderPosY;
            double posZ = RenderManager.renderPosZ;

            if (ac != null) {
                posX = RenderManager.renderPosX;
                posY = RenderManager.renderPosY;
                posZ = RenderManager.renderPosZ;
            }

            // 计算发射源
            Vec3 src = W_WorldFunc.getWorldVec3(this.worldObj, posX, posY, posZ);
            Vec3 look = Vec3.createVectorHelper(targetX, targetY, targetZ);
            Vec3 end = src.addVector(look.xCoord * maxDist, look.yCoord * maxDist, look.zCoord * maxDist);

            // 射线检测
            MovingObjectPosition hitResult;

            if(lockEntity) {
                hitResult = laserRaytrace(user, ac, src, look, end);
            } else {
                hitResult = MCH_RayTracer.rayTrace(worldObj, src, end);
            }

            // 如果没有检测到碰撞，则返回默认的目标位置
            if (hitResult == null) {
                hitResult = new MovingObjectPosition(null, end);  // 使用目标点作为默认值
            }

            if(hitResult.entityHit != null) {
                boolean jamming = false;
                //无法锁定释放烟雾弹的地面载具
                if(hitResult.entityHit instanceof MCH_EntityTank && ((MCH_EntityTank) hitResult.entityHit).isFlareUsing()) {
                    targetPosX = hitResult.hitVec.xCoord;
                    targetPosY = hitResult.hitVec.yCoord;
                    targetPosZ = hitResult.hitVec.zCoord;
                    jamming = true;
                }

                if(!jamming) {
                    targetPosX = hitResult.entityHit.posX;
                    targetPosY = hitResult.entityHit.posY + hitResult.entityHit.height / 2;
                    targetPosZ = hitResult.entityHit.posZ;
                    //让玩家的视角跟随锁定的实体
                    if (cameraFollowLockEntity) {
                        MCH_PlayerViewHandler.updatePlayerViewDirection(user, targetPosX, targetPosY, targetPosZ, cameraFollowStrength);
                    }
                }

            } else {
                targetPosX = hitResult.hitVec.xCoord;
                targetPosY = hitResult.hitVec.yCoord;
                targetPosZ = hitResult.hitVec.zCoord;
            }

            if(sendToServer) {
                MCH_MOD.getPacketHandler().sendToServer(new PacketLaserGuidanceTargeting(true, targetPosX, targetPosY, targetPosZ));
            }

            if (lockBox != null) {
                lockBox.setPosition(targetPosX, targetPosY, targetPosZ);
            } else {
                lockBox = new MCH_EntityLockBox(worldObj);
                worldObj.spawnEntityInWorld(lockBox);
            }
        }
    }

    public static boolean canHitByLaser(Entity entity, Entity user, Entity ac) {
        if (entity == null) return false;
        if (W_Entity.isEqual(entity, ac)) return false;
        if (W_Entity.isEqual(entity, user.ridingEntity)) return false;
        if (entity instanceof MCH_EntityChaff) return true;
        return entity instanceof MCH_EntityAircraft;
    }

    public static MovingObjectPosition laserRaytrace(Entity user, Entity ac, Vec3 start, Vec3 lookVec, Vec3 end) {
        double distance = start.distanceTo(end);
        Entity pointedEntity = null;
        MovingObjectPosition result = MCH_RayTracer.rayTrace(user.worldObj, start, end);
        double dist = distance;
        if (result != null) {
            dist = result.hitVec.distanceTo(start);
        }
        Vec3 hitVec = null;
        float f1 = 1.0F;
        List list = user.worldObj.getEntitiesWithinAABBExcludingEntity(user,
            user.boundingBox.addCoord(lookVec.xCoord * distance, lookVec.yCoord * distance, lookVec.zCoord * distance).expand(f1, f1, f1));
        double d2 = dist;

        for (Object o : list) {
            Entity entity = (Entity) o;
            if (entity.canBeCollidedWith()
                && canHitByLaser(entity, user, ac)) {
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

        if (pointedEntity != null && (d2 < dist || result == null)) {
            result = new MovingObjectPosition(pointedEntity, hitVec);
        }

        return result;
    }

}
