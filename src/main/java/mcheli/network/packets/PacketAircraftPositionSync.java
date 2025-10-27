package mcheli.network.packets;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.network.PacketBase;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

public class PacketAircraftPositionSync extends PacketBase {

    public double posX;
    public double posY;
    public double posZ;
    public float yaw;
    public float pitch;
    public float roll;
    public int entityId;

    public PacketAircraftPositionSync(double posX, double posY, double posZ, float yaw, float pitch, float roll, int entityId) {
        this.posX = posX;
        this.posY = posY;
        this.posZ = posZ;
        this.yaw = yaw;
        this.pitch = pitch;
        this.roll = roll;
        this.entityId = entityId;
    }

    public PacketAircraftPositionSync() {
    }

    @Override
    public void encodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        data.writeDouble(posX);
        data.writeDouble(posY);
        data.writeDouble(posZ);
        data.writeFloat(yaw);
        data.writeFloat(pitch);
        data.writeFloat(roll);
        data.writeInt(entityId);
    }

    @Override
    public void decodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        posX = data.readDouble();
        posY = data.readDouble();
        posZ = data.readDouble();
        yaw = data.readFloat();
        pitch = data.readFloat();
        roll = data.readFloat();
        entityId = data.readInt();
    }

    @Override
    public void handleServerSide(EntityPlayerMP playerEntity) {

    }

    @Override
    public void handleClientSide(EntityPlayer clientPlayer) {
        Entity e = clientPlayer.worldObj.getEntityByID(entityId);
        if (e instanceof MCH_EntityAircraft) {
            MCH_EntityAircraft ac = (MCH_EntityAircraft) e;
            ac.setPositionAndRotation(posX, posY, posZ, yaw, pitch);
            ac.setRotRoll(roll);
        }
    }
}
