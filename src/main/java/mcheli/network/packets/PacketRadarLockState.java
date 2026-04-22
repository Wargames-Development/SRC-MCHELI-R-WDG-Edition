package mcheli.network.packets;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import mcheli.MCH_MOD;
import mcheli.network.PacketBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

public class PacketRadarLockState extends PacketBase {

    public int emitterAircraftId;
    public int trackingTargetId;

    public PacketRadarLockState() {
    }

    public PacketRadarLockState(int emitterAircraftId, int trackingTargetId) {
        this.emitterAircraftId = emitterAircraftId;
        this.trackingTargetId = trackingTargetId;
    }

    @Override
    public void encodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        data.writeInt(this.emitterAircraftId);
        data.writeInt(this.trackingTargetId);
    }

    @Override
    public void decodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        this.emitterAircraftId = data.readInt();
        this.trackingTargetId = data.readInt();
    }

    @Override
    public void handleServerSide(EntityPlayerMP playerEntity) {
        MCH_MOD.rwrThreatManager.reportRadarTracking(playerEntity, this.emitterAircraftId, this.trackingTargetId);
    }

    @Override
    public void handleClientSide(EntityPlayer clientPlayer) {
    }
}
