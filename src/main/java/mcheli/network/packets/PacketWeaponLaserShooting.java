package mcheli.network.packets;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import mcheli.network.PacketBase;
import mcheli.weapon.MCH_RenderLaser;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.Vec3;

public class PacketWeaponLaserShooting extends PacketBase {

    double srcX, srcY, srcZ, destX, destY, destZ;
    int argb;
    float width;
    int life;
    boolean pulsate;

    public PacketWeaponLaserShooting(Vec3 start, Vec3 hit, int argb, float width, int life, boolean pulsate) {
        this.srcX = start.xCoord;
        this.srcY = start.yCoord;
        this.srcZ = start.zCoord;
        this.destX = hit.xCoord;
        this.destY = hit.yCoord;
        this.destZ = hit.zCoord;
        this.argb = argb;
        this.width = width;
        this.life = life;
        this.pulsate = pulsate;
    }

    public PacketWeaponLaserShooting() {
    }

    @Override
    public void encodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        data.writeDouble(srcX);
        data.writeDouble(srcY);
        data.writeDouble(srcZ);
        data.writeDouble(destX);
        data.writeDouble(destY);
        data.writeDouble(destZ);
        data.writeInt(argb);
        data.writeFloat(width);
        data.writeInt(life);
        data.writeBoolean(pulsate);
    }

    @Override
    public void decodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        srcX = data.readDouble();
        srcY = data.readDouble();
        srcZ = data.readDouble();
        destX = data.readDouble();
        destY = data.readDouble();
        destZ = data.readDouble();
        argb = data.readInt();
        width = data.readFloat();
        life = data.readInt();
        pulsate = data.readBoolean();
    }

    @Override
    public void handleServerSide(EntityPlayerMP playerEntity) {

    }

    @Override
    public void handleClientSide(EntityPlayer clientPlayer) {
        MCH_RenderLaser.addBeam(Vec3.createVectorHelper(srcX, srcY, srcZ), Vec3.createVectorHelper(destX, destY, destZ), argb, width, life, pulsate);
    }
}
