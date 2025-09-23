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
 * 仅包含：快照序号 + 本帧全量实体信息。
 * 不再使用 OPERATION_REMOVE；服务器从不下发删除。
 */
public class PacketEntityInfoSync extends PacketBase {

    private List<MCH_EntityInfo> entities;
    private long snapshotSeq; // 新增：包级快照序号

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
        // 仅服务器下发；无服务端处理逻辑
    }

    @Override
    public void handleClientSide(EntityPlayer player) {
        // 仅当该包的快照序号不小于客户端已知最新序号时才应用（乱序保护）
        MCH_EntityInfoClientTracker.updateEntities(entities, snapshotSeq);
    }
}
