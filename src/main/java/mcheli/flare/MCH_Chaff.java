package mcheli.flare;

import mcheli.MCH_MOD;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.network.packets.PacketChaffUse;
import mcheli.wrapper.W_McClient;
import net.minecraft.util.MathHelper;
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
    private int spawnChaffEntityIntervalTick;

    public MCH_Chaff(World w, MCH_EntityAircraft ac) {
        this.worldObj = w;
        this.aircraft = ac;
    }

    public boolean onUse() {
        boolean result = false;
        System.out.println("MCH_Chaff.onUse");
        if (worldObj.isRemote) {
            if (tick == 0) {
                tick = chaffWaitTime;
                useTick = chaffUseTime;
                spawnChaffEntityIntervalTick = 0;
                result = true;
                //W_McClient.DEF_playSoundFX("flare_deploy", 10.0F, 1.0F);
            }
        } else {
            result = true;
            tick = chaffWaitTime;
            useTick = chaffUseTime;
            spawnChaffEntityIntervalTick = 0;
            MCH_MOD.getPacketHandler().sendToAll(new PacketChaffUse(aircraft.getEntityId(), useTick));
            aircraft.getEntityData().setBoolean("ChaffUsing", true);
        }
        return result;
    }

    public void onUpdate() {
        if (this.aircraft != null && !this.aircraft.isDead) {
            if (this.tick > 0) {
                --this.tick;
            }
            if (this.useTick > 0) {
                --this.useTick;
            }
            if (this.useTick > 0) {
                this.onUsing();
            }
            if (!this.isUsing() && this.aircraft.getEntityData().getBoolean("ChaffUsing")) {
                this.aircraft.getEntityData().setBoolean("ChaffUsing", false);
            }
        }
    }

    private void onUsing() {
        if (spawnChaffEntityIntervalTick == 0) {
            spawnChaffEntityIntervalTick = chaffUseTime / 10;
            if (!worldObj.isRemote) {
                spawnChaffEntity();
            }
            if (worldObj.isRemote) {
                W_McClient.MOD_playSoundFX("chaff", 10.0F, 1.0F);
            }
        }
        if (spawnChaffEntityIntervalTick > 0) {
            spawnChaffEntityIntervalTick--;
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
        double baseX = this.aircraft.lastTickPosX - forwardX * 20D;
        double baseY = this.aircraft.lastTickPosY - 10D;
        double baseZ = this.aircraft.lastTickPosZ - forwardZ * 20D;

        // 左右偏移距离，可根据机体宽度调整
        double sideOffset = 1.5D;

        // 计算左侧与右侧箔条的生成位置
        double leftPosX = baseX + leftX * sideOffset;
        double leftPosZ = baseZ + leftZ * sideOffset;
        double rightPosX = baseX + rightX * sideOffset;
        double rightPosZ = baseZ + rightZ * sideOffset;

        // 初速度：用飞机当前速度加上一小段侧向速度，使箔条朝两侧散开
        double sideSpeed = 0.2D;
        // 左侧初速度
        double leftVelX = this.aircraft.motionX + leftX * sideSpeed;
        double leftVelY = this.aircraft.motionY;
        double leftVelZ = this.aircraft.motionZ + leftZ * sideSpeed;
        // 右侧初速度
        double rightVelX = this.aircraft.motionX + rightX * sideSpeed;
        double rightVelY = this.aircraft.motionY;
        double rightVelZ = this.aircraft.motionZ + rightZ * sideSpeed;

        // 创建并加入两枚箔条实体
        MCH_EntityChaff leftChaff = new MCH_EntityChaff(worldObj,
            leftPosX, baseY, leftPosZ,
            leftVelX, leftVelY, leftVelZ);
        MCH_EntityChaff rightChaff = new MCH_EntityChaff(worldObj,
            rightPosX, baseY, rightPosZ,
            rightVelX, rightVelY, rightVelZ);

        this.worldObj.spawnEntityInWorld(leftChaff);
        this.worldObj.spawnEntityInWorld(rightChaff);
    }


    public boolean isInPreparation() {
        return this.tick != 0;
    }

    public boolean isUsing() {
        return this.useTick > 0;
    }
}
