package mcheli.network.packets;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.network.PacketBase;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

public class PacketCountermeasureState extends PacketBase {

    private int aircraftId;
    private int flarePairs;
    private int chaffPairs;

    public PacketCountermeasureState() {
    }

    public PacketCountermeasureState(MCH_EntityAircraft aircraft) {
        this.aircraftId = aircraft.getEntityId();
        this.flarePairs = aircraft.getRemainingFlarePairs();
        this.chaffPairs = aircraft.getRemainingChaffPairs();
    }

    @Override
    public void encodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        data.writeInt(this.aircraftId);
        data.writeInt(this.flarePairs);
        data.writeInt(this.chaffPairs);
    }

    @Override
    public void decodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        this.aircraftId = data.readInt();
        this.flarePairs = data.readInt();
        this.chaffPairs = data.readInt();
    }

    @Override
    public void handleServerSide(EntityPlayerMP playerEntity) {
        // Server-owned state is never accepted from a client.
    }

    @Override
    public void handleClientSide(EntityPlayer playerEntity) {
        if (playerEntity == null || playerEntity.worldObj == null) {
            return;
        }
        Entity entity = playerEntity.worldObj.getEntityByID(this.aircraftId);
        if (entity instanceof MCH_EntityAircraft) {
            ((MCH_EntityAircraft)entity).setCountermeasureStateClient(this.flarePairs, this.chaffPairs);
        }
    }
}
