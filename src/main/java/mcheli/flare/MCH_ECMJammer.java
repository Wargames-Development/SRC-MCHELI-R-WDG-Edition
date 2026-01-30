package mcheli.flare;

import mcheli.MCH_MOD;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.network.PacketBase;
import mcheli.network.packets.PacketECMJammerUse;
import mcheli.wrapper.W_McClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.World;

public class MCH_ECMJammer {

    //冷却时长 0代表冷却结束
    public int tick;
    //生效时长 0代表使用结束
    public int useTick;
    //维修系统生效时间
    public int useTime;
    //维修系统等待时间
    public int waitTime;

    public World worldObj;

    public MCH_EntityAircraft aircraft;

    public MCH_ECMJammer(World w, MCH_EntityAircraft ac) {
        this.worldObj = w;
        this.aircraft = ac;
    }

    public boolean onUse(Entity e) {
        boolean result = false;
        if (worldObj.isRemote) {
            if (tick == 0) {
                tick = waitTime;
                useTick = useTime;
                result = true;
                W_McClient.MOD_playSoundFX("ECMJammer", 10.0F, 1.0F);
            }
        } else {
            result = true;
            tick = waitTime;
            useTick = useTime;
            int jammingTime = 180;
            if(e instanceof EntityPlayer) {
                aircraft.getEntityData().setBoolean("ECMJammerUsing", true);
                if (aircraft.getAcInfo().ecmJammerType == 1) {
                    MCH_MOD.getPacketHandler().sendToAll(
                        new PacketECMJammerUse(aircraft.getEntityId(), useTick, aircraft.getAcInfo().ecmJammerType, jammingTime));
                } else {
                    MCH_MOD.getPacketHandler().sendToAll(
                        new PacketECMJammerUse(aircraft.getEntityId(), useTick, aircraft.getAcInfo().ecmJammerType, jammingTime));
                }
            }
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
            if (!this.isUsing() && this.aircraft.getEntityData().getBoolean("ECMJammerUsing")) {
                this.aircraft.getEntityData().setBoolean("ECMJammerUsing", false);
            }
        }
    }

    private void onUsing() {

    }


    public boolean isInPreparation() {
        return this.tick != 0;
    }

    public boolean isUsing() {
        return this.useTick > 0;
    }
}
