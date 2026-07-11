package mcheli.network.packets;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.network.PacketBase;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

public class PacketRadarSwitchState extends PacketBase {

    public int aircraftId;
    public boolean enabled;

    public PacketRadarSwitchState() {
    }

    public PacketRadarSwitchState(int aircraftId, boolean enabled) {
        this.aircraftId = aircraftId;
        this.enabled = enabled;
    }

    @Override
    public void encodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        data.writeInt(this.aircraftId);
        data.writeBoolean(this.enabled);
    }

    @Override
    public void decodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        this.aircraftId = data.readInt();
        this.enabled = data.readBoolean();
    }

    @Override
    public void handleServerSide(EntityPlayerMP playerEntity) {
        if (playerEntity == null || playerEntity.worldObj == null || this.aircraftId <= 0) {
            return;
        }
        Entity entity = playerEntity.worldObj.getEntityByID(this.aircraftId);
        if (!(entity instanceof MCH_EntityAircraft)) {
            return;
        }
        MCH_EntityAircraft ac = (MCH_EntityAircraft) entity;
        if (ac.getAcInfo() == null || !ac.getAcInfo().enableRadar || ac.isDestroyed()) {
            return;
        }
        int seatId = ac.getSeatIdByEntity(playerEntity);
        if (seatId < 0 || seatId > 1) {
            return;
        }
        ac.setRadarEnabledRuntime(this.enabled);
    }

    @Override
    public void handleClientSide(EntityPlayer clientPlayer) {
    }
}
