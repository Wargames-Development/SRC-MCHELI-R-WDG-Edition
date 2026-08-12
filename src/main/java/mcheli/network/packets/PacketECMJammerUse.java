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
        MCH_EntityAircraft ac = MCH_EntityAircraft.getAircraft_RiddenOrControl(clientPlayer);
        if (type == 1 && e instanceof MCH_EntityAircraft && ac != null && !W_Entity.isEqual(e, ac)) {
            MCH_EntityAircraft jammer = (MCH_EntityAircraft)e;
            float range = jammer.getAcInfo() != null ? jammer.getAcInfo().ecmJammerRange : 0.0F;
            double dx = jammer.posX - ac.posX;
            double dz = jammer.posZ - ac.posZ;
            if (range > 0.0F && dx * dx + dz * dz <= (double)range * (double)range) {
                ac.jammingTick = Math.max(ac.jammingTick, jammingTime);
            }
        }
        if (e instanceof MCH_EntityAircraft) {
            ((MCH_EntityAircraft) e).ecmJammerUseTime = time;
        }
    }
}
