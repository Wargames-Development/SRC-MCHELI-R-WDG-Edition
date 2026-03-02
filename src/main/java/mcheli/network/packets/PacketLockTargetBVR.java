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

    // NEW: entity being “painted” (for RWR). 0 = none
    public int targetEntityId;

    public int posX;
    public int posY;
    public int posZ;

    public PacketLockTargetBVR(int mslId, int targetEntityId, int posX, int posY, int posZ) {
        this.mslId = mslId;
        this.targetEntityId = targetEntityId;
        this.posX = posX;
        this.posY = posY;
        this.posZ = posZ;
    }

    public PacketLockTargetBVR() {
    }

    @Override
    public void encodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        data.writeInt(mslId);
        data.writeInt(targetEntityId); // NEW
        data.writeInt(posX);
        data.writeInt(posY);
        data.writeInt(posZ);
    }

    @Override
    public void decodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        mslId = data.readInt();
        targetEntityId = data.readInt(); // NEW
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

                    // Lock state
                    boolean enable = (posY > 0);
                    aa.passiveRadarBVRLocking = enable;
                    aa.passiveRadarBVRLockingPosX = posX;
                    aa.passiveRadarBVRLockingPosY = posY;
                    aa.passiveRadarBVRLockingPosZ = posZ;

                    // NEW: set targetEntity only as “RWR illumination owner”
                    if (!enable || targetEntityId <= 0) {
                        aa.setTargetEntity(null);
                    } else {
                        Entity tgt = null;

                        // Find target in the same world first (fast)
                        tgt = world.getEntityByID(targetEntityId);

                        // If not found, try all worlds (dimension mismatch edge cases)
                        if (tgt == null) {
                            for (WorldServer w2 : MinecraftServer.getServer().worldServers) {
                                tgt = w2.getEntityByID(targetEntityId);
                                if (tgt != null) break;
                            }
                        }

                        aa.setTargetEntity(tgt); // may be null if not found; that’s fine
                    }
                    return;
                }
            }
        }
    }

    @Override
    public void handleClientSide(EntityPlayer clientPlayer) {
    }
}