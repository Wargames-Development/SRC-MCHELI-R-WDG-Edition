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
    private int apsShots;
    private boolean apsEnabled;

    public PacketCountermeasureState() {
    }

    public PacketCountermeasureState(MCH_EntityAircraft aircraft) {
        this.aircraftId = aircraft.getEntityId();
        this.flarePairs = aircraft.getRemainingFlarePairs();
        this.chaffPairs = aircraft.getRemainingChaffPairs();
        this.apsShots = Math.max(0, aircraft.aps.remainingShots);
        this.apsEnabled = aircraft.aps.enabled;
    }

    @Override
    public void encodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        data.writeInt(this.aircraftId);
        data.writeInt(this.flarePairs);
        data.writeInt(this.chaffPairs);
        data.writeInt(this.apsShots);
        data.writeBoolean(this.apsEnabled);
    }

    @Override
    public void decodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        this.aircraftId = data.readInt();
        this.flarePairs = data.readInt();
        this.chaffPairs = data.readInt();
        this.apsShots = data.readInt();
        this.apsEnabled = data.readBoolean();
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
            MCH_EntityAircraft aircraft = (MCH_EntityAircraft)entity;
            aircraft.setCountermeasureStateClient(this.flarePairs, this.chaffPairs);
            aircraft.aps.remainingShots = Math.min(Math.max(0, this.apsShots), aircraft.getAPSShotCapacity());
            aircraft.aps.enabled = this.apsEnabled && aircraft.aps.remainingShots > 0;
        }
    }
}
