package mcheli.network.packets;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import mcheli.network.PacketBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

public class PacketLaserGuidanceTargeting extends PacketBase {

    boolean targeting;

    double targetPosX;
    double targetPosY;
    double targetPosZ;

    public PacketLaserGuidanceTargeting(boolean targeting, double targetPosX, double targetPosY, double targetPosZ) {
        this.targeting = targeting;
        this.targetPosX = targetPosX;
        this.targetPosY = targetPosY;
        this.targetPosZ = targetPosZ;
    }

    public PacketLaserGuidanceTargeting() {
    }

    @Override
    public void encodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        data.writeBoolean(targeting);
        data.writeDouble(targetPosX);
        data.writeDouble(targetPosY);
        data.writeDouble(targetPosZ);
    }

    @Override
    public void decodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        targeting = data.readBoolean();
        targetPosX = data.readDouble();
        targetPosY = data.readDouble();
        targetPosZ = data.readDouble();
    }

    @Override
    public void handleServerSide(EntityPlayerMP playerEntity) {
        PacketLaserStateSync.enqueueGuidance(playerEntity, targeting, targetPosX, targetPosY, targetPosZ);
    }

    @Override
    public void handleClientSide(EntityPlayer clientPlayer) {

    }
}
