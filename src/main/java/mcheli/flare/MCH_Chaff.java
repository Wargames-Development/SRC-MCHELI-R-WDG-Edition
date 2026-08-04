package mcheli.flare;

import mcheli.MCH_MOD;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.network.packets.PacketChaffUse;
import mcheli.wrapper.W_McClient;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import java.util.Random;

public class MCH_Chaff {

    public final Random rand = new Random();
    //冷却时长 0代表冷却结束
    public int tick;
    //生效时长 0代表使用结束
    public int useTick;
    //箔条使用时间
    public int chaffUseTime;
    //箔条等待时间
    public int chaffWaitTime;
    public World worldObj;
    public MCH_EntityAircraft aircraft;
    //箔条使用时分批间隔

    public MCH_Chaff(World w, MCH_EntityAircraft ac) {
        this.worldObj = w;
        this.aircraft = ac;
    }

    public boolean onUse() {
        if (tick != 0) {
            return false;
        }
        tick = chaffWaitTime;
        useTick = chaffUseTime;
        if (worldObj.isRemote) {
            W_McClient.MOD_playSoundFX("chaff", 10.0F, 1.0F);
        } else {
            this.spawnChaffEntity();
            MCH_MOD.getPacketHandler().sendToAllAround(new PacketChaffUse(aircraft.getEntityId(), useTick),
                aircraft.posX, aircraft.posY, aircraft.posZ, 256.0F, aircraft.dimension);
            aircraft.getEntityData().setBoolean("ChaffUsing", true);
        }
        return true;
    }

    public void onUpdate() {
        if (this.aircraft != null && !this.aircraft.isDead) {
            if (this.tick > 0) {
                --this.tick;
            }
            if (this.useTick > 0) {
                --this.useTick;
            }
            if (!this.isUsing() && this.aircraft.getEntityData().getBoolean("ChaffUsing")) {
                this.aircraft.getEntityData().setBoolean("ChaffUsing", false);
            }
        }
    }

    private void spawnChaffEntity() {
        // 获取飞机的偏航角，换算成弧度
        float yaw = this.aircraft.rotationYaw;
        float rad = (float) (yaw / 180.0F * Math.PI);

        // 计算机身前向与左右侧向单位向量
        double forwardX = -MathHelper.sin(rad);
        double forwardZ = MathHelper.cos(rad);
        // 左右方向相当于在偏航角上加/减 90°
        double leftX = -MathHelper.sin(rad + (float) Math.PI / 2F);
        double leftZ = MathHelper.cos(rad + (float) Math.PI / 2F);
        double rightX = -MathHelper.sin(rad - (float) Math.PI / 2F);
        double rightZ = MathHelper.cos(rad - (float) Math.PI / 2F);

        // 基准位置：在飞机尾部稍微偏下一点
        Vec3 dispenser = this.aircraft.getAcInfo().chaff.pos;
        Vec3 base = this.aircraft.getTransformedPosition(dispenser.xCoord, dispenser.yCoord, dispenser.zCoord,
            this.aircraft.posX, this.aircraft.posY, this.aircraft.posZ);
        double baseX = base.xCoord;
        double baseY = base.yCoord;
        double baseZ = base.zCoord;

        // 左右偏移距离，可根据机体宽度调整
        double sideOffset = 0.75D;

        // 计算左侧与右侧箔条的生成位置
        double leftPosX = baseX + leftX * sideOffset;
        double leftPosZ = baseZ + leftZ * sideOffset;
        double rightPosX = baseX + rightX * sideOffset;
        double rightPosZ = baseZ + rightZ * sideOffset;

        // 初速度：用飞机当前速度加上一小段侧向速度，使箔条朝两侧散开
        double sideSpeed = 0.2D;
        double rearSpeed = 0.15D;
        // 左侧初速度
        double leftVelX = this.aircraft.motionX + leftX * sideSpeed - forwardX * rearSpeed;
        double leftVelY = this.aircraft.motionY - 0.05D;
        double leftVelZ = this.aircraft.motionZ + leftZ * sideSpeed - forwardZ * rearSpeed;
        // 右侧初速度
        double rightVelX = this.aircraft.motionX + rightX * sideSpeed - forwardX * rearSpeed;
        double rightVelY = this.aircraft.motionY - 0.05D;
        double rightVelZ = this.aircraft.motionZ + rightZ * sideSpeed - forwardZ * rearSpeed;

        // 创建并加入两枚箔条实体
        MCH_EntityChaff leftChaff = new MCH_EntityChaff(worldObj,
            leftPosX, baseY, leftPosZ,
            leftVelX, leftVelY, leftVelZ);
        MCH_EntityChaff rightChaff = new MCH_EntityChaff(worldObj,
            rightPosX, baseY, rightPosZ,
            rightVelX, rightVelY, rightVelZ);
        long releaseId = (this.worldObj.getTotalWorldTime() << 20) ^ (long)this.aircraft.getEntityId();
        leftChaff.getEntityData().setLong("CountermeasureReleaseId", releaseId);
        rightChaff.getEntityData().setLong("CountermeasureReleaseId", releaseId);

        this.worldObj.spawnEntityInWorld(leftChaff);
        this.worldObj.spawnEntityInWorld(rightChaff);
    }


    public boolean isInPreparation() {
        return this.tick != 0;
    }

    public boolean isUsing() {
        return this.useTick > 0;
    }

    public void setUseTickClient(int time) {
        if (this.worldObj.isRemote) {
            boolean wasUsing = this.isUsing();
            this.useTick = Math.max(this.useTick, Math.max(0, time));
            if (!wasUsing && this.useTick > 0) {
                W_McClient.MOD_playSoundFX("chaff", 10.0F, 1.0F);
            }
        }
    }
}
