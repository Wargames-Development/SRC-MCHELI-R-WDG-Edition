package mcheli.flare;

import mcheli.MCH_MOD;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.network.packets.PacketChaffUse;
import mcheli.network.packets.PacketIronCurtainUse;
import mcheli.wrapper.W_McClient;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import java.util.Random;

public class MCH_Chaff {

    // Cooldown duration; 0 indicates cooldown finished
    public int tick;
    // Active duration; 0 indicates effect has ended
    public int useTick;
    // Chaff active duration
    public int chaffUseTime;
    // Chaff cooldown duration
    public int chaffWaitTime;
    public World worldObj;
    public MCH_EntityAircraft aircraft;
    // Interval between spawning chaff entities while active
    private int spawnChaffEntityIntervalTick;
    public final Random rand = new Random();

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
            if(this.useTick > 0) {
                this.onUsing();
            }
            if (!this.isUsing() && this.aircraft.getEntityData().getBoolean("ChaffUsing")) {
                this.aircraft.getEntityData().setBoolean("ChaffUsing", false);
            }
        }
    }

    private void onUsing() {
        if(spawnChaffEntityIntervalTick == 0) {
            spawnChaffEntityIntervalTick = chaffUseTime / 10;
            if(!worldObj.isRemote) {
                spawnChaffEntity();
            }
            if(worldObj.isRemote) {
                W_McClient.MOD_playSoundFX("chaff", 10.0F, 1.0F);
            }
        }
        if(spawnChaffEntityIntervalTick > 0) {
            spawnChaffEntityIntervalTick--;
        }
    }

    private void spawnChaffEntity() {
        // Get the aircraft’s yaw angle and convert it to radians
        float yaw = this.aircraft.rotationYaw;
        float rad = (float) (yaw / 180.0F * Math.PI);

        // Calculate the aircraft’s forward and side unit vectors
        double forwardX = -MathHelper.sin(rad);
        double forwardZ =  MathHelper.cos(rad);
        // Left and right directions are offset by ±90° from the yaw angle
        double leftX = -MathHelper.sin(rad + (float) Math.PI / 2F);
        double leftZ =  MathHelper.cos(rad + (float) Math.PI / 2F);
        double rightX = -MathHelper.sin(rad - (float) Math.PI / 2F);
        double rightZ =  MathHelper.cos(rad - (float) Math.PI / 2F);

        // Base position: slightly below and behind the aircraft’s tail
        double baseX = this.aircraft.lastTickPosX - forwardX * 20D;
        double baseY = this.aircraft.lastTickPosY - 10D;
        double baseZ = this.aircraft.lastTickPosZ - forwardZ * 20D;

        // Side offset distance, adjustable based on aircraft width
        double sideOffset = 1.5D;

        // Compute the spawn positions for the left and right chaff
        double leftPosX = baseX + leftX * sideOffset;
        double leftPosZ = baseZ + leftZ * sideOffset;
        double rightPosX = baseX + rightX * sideOffset;
        double rightPosZ = baseZ + rightZ * sideOffset;

        // Initial velocity: aircraft’s current velocity plus small lateral spread
        double sideSpeed = 0.2D;
        // Left-side initial velocity
        double leftVelX = this.aircraft.motionX + leftX * sideSpeed;
        double leftVelY = this.aircraft.motionY;
        double leftVelZ = this.aircraft.motionZ + leftZ * sideSpeed;
        // Right-side initial velocity
        double rightVelX = this.aircraft.motionX + rightX * sideSpeed;
        double rightVelY = this.aircraft.motionY;
        double rightVelZ = this.aircraft.motionZ + rightZ * sideSpeed;

        // Create and spawn two chaff entities
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
