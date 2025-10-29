package mcheli.network.packets;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import mcheli.MCH_EntityInfo;
import mcheli.MCH_EntityInfoClientTracker;
import mcheli.network.PacketBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

import java.util.ArrayList;
import java.util.List;

/**
 * Contains only: snapshot sequence number + full entity information for this frame.
 * OPERATION_REMOVE is no longer used; the server never sends delete operations.
 */
public class PacketEntityInfoSync extends PacketBase {

    private List<MCH_EntityInfo> entities;
    private long snapshotSeq; // Added: snapshot sequence number for this packet

    public PacketEntityInfoSync() {}

    public PacketEntityInfoSync(List<MCH_EntityInfo> entities, long snapshotSeq) {
        this.entities = entities;
        this.snapshotSeq = snapshotSeq;
    }

    @Override
    public void encodeInto(ChannelHandlerContext ctx, ByteBuf buf) {
        buf.writeLong(snapshotSeq);
        buf.writeInt(entities.size());
        for (MCH_EntityInfo info : entities) {
            buf.writeInt(info.entityId);
            writeUTF(buf, info.worldName);
            writeUTF(buf, info.entityName);
            writeUTF(buf, info.entityClassName);
            buf.writeDouble(info.posX);
            buf.writeDouble(info.posY);
            buf.writeDouble(info.posZ);
            buf.writeDouble(info.lastTickPosX);
            buf.writeDouble(info.lastTickPosY);
            buf.writeDouble(info.lastTickPosZ);
        }
    }

    @Override
    public void decodeInto(ChannelHandlerContext ctx, ByteBuf buf) {
        snapshotSeq = buf.readLong();
        int count = buf.readInt();
        entities = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            entities.add(new MCH_EntityInfo(
                    buf.readInt(),
                    readUTF(buf),
                    readUTF(buf),
                    readUTF(buf),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readDouble()
            ));
        }
    }

    @Override
    public void handleServerSide(EntityPlayerMP playerEntity) {
        // Server only sends this packet; no server-side handling logic
    }

    @Override
    public void handleClientSide(EntityPlayer player) {
        // Apply only if the packet’s snapshot sequence number is >= the client’s latest known sequence (out-of-order protection)
        MCH_EntityInfoClientTracker.updateEntities(entities, snapshotSeq);
    }
}
