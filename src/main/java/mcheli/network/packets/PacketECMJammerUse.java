package mcheli.network.packets;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.network.PacketBase;
import mcheli.wrapper.W_Entity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

public class PacketECMJammerUse extends PacketBase {

    public int acId;
    public int time;
    public int type;
    public int jammingTime;


    public PacketECMJammerUse() {
    }

    public PacketECMJammerUse(int acId, int time, int type,  int jammingTime) {
        this.acId = acId;
        this.time = time;
        this.type = type;
        this.jammingTime = jammingTime;
    }

    @Override
    public void encodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        data.writeInt(acId);
        data.writeInt(time);
        data.writeInt(type);
        data.writeInt(jammingTime);
    }

    @Override
    public void decodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        acId = data.readInt();
        time = data.readInt();
        type = data.readInt();
        jammingTime = data.readInt();
    }

    @Override
    public void handleServerSide(EntityPlayerMP playerEntity) {

    }

    @Override
    public void handleClientSide(EntityPlayer clientPlayer) {
        Entity e = clientPlayer.worldObj.getEntityByID(acId);
        Entity e1 = clientPlayer.ridingEntity;
        if (e1 instanceof MCH_EntityAircraft) {
            MCH_EntityAircraft ac = (MCH_EntityAircraft) e1;
            if (type == 1 && !W_Entity.isEqual(e, e1)){
                ac.jammingTick = jammingTime;
            }
        }
        if (e instanceof MCH_EntityAircraft) {
            ((MCH_EntityAircraft) e).ecmJammerUseTime = time;
        }
    }
}
