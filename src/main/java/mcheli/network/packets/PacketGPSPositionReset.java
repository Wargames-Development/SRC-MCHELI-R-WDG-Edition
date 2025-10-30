package mcheli.network.packets;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import mcheli.network.PacketBase;
import mcheli.weapon.MCH_GPSPosition;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

public class PacketGPSPositionReset extends PacketBase {

    double targetPosX;
    double targetPosY;
    double targetPosZ;
    boolean isActive;
    int ownerId;

    public PacketGPSPositionReset() {
    }

    public PacketGPSPositionReset(double targetPosX, double targetPosY, double targetPosZ, boolean isActive, int ownerId) {
        this.targetPosX = targetPosX;
        this.targetPosY = targetPosY;
        this.targetPosZ = targetPosZ;
        this.isActive = isActive;
        this.ownerId = ownerId;
    }

    @Override
    public void encodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        data.writeDouble(targetPosX);
        data.writeDouble(targetPosY);
        data.writeDouble(targetPosZ);
        data.writeBoolean(isActive);
        data.writeInt(ownerId);
    }

    @Override
    public void decodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        targetPosX = data.readDouble();
        targetPosY = data.readDouble();
        targetPosZ = data.readDouble();
        isActive = data.readBoolean();
        ownerId = data.readInt();
    }

    @Override
    public void handleServerSide(EntityPlayerMP playerEntity) {
        MCH_GPSPosition.set(targetPosX, targetPosY, targetPosZ, isActive, playerEntity);
    }

    @Override
    public void handleClientSide(EntityPlayer clientPlayer) {

    }
}
