package mcheli.network.packets;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import mcheli.network.PacketBase;
import mcheli.weapon.MCH_EntityAAMissile;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;

import java.util.List;

public class PacketLockTargetBVR extends PacketBase {

    public int mslId;

    public int posX;

    public int posY;

    public int posZ;

    public PacketLockTargetBVR(int mslId, int posX, int posY, int posZ) {
        this.mslId = mslId;
        this.posX = posX;
        this.posY = posY;
        this.posZ = posZ;
    }

    public PacketLockTargetBVR() {
    }

    @Override
    public void encodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        data.writeInt(mslId);
        data.writeInt(posX);
        data.writeInt(posY);
        data.writeInt(posZ);
    }

    @Override
    public void decodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        mslId = data.readInt();
        posX = data.readInt();
        posY = data.readInt();
        posZ = data.readInt();
    }

    @Override
    public void handleServerSide(EntityPlayerMP playerEntity) {
        for (WorldServer world : MinecraftServer.getServer().worldServers) {
            for (Entity entity : (List<Entity>) world.loadedEntityList) {
                if (entity.getEntityId() == mslId && entity instanceof MCH_EntityAAMissile) {
                    MCH_EntityAAMissile aa = (MCH_EntityAAMissile) entity;
                    if (posY <= 0) {
                        aa.passiveRadarBVRLocking = false;
                    } else {
                        aa.passiveRadarBVRLocking = true;
                    }
                    aa.passiveRadarBVRLockingPosX = posX;
                    aa.passiveRadarBVRLockingPosY = posY;
                    aa.passiveRadarBVRLockingPosZ = posZ;
                }
            }
        }
    }

    @Override
    public void handleClientSide(EntityPlayer clientPlayer) {

    }
}
