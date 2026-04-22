package mcheli.network.packets;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import mcheli.MCH_RWRThreatClientTracker;
import mcheli.MCH_RWRThreatEvent;
import mcheli.MCH_RWRThreatTable;
import mcheli.network.PacketBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

import java.util.ArrayList;
import java.util.List;

public class PacketRWRThreatSync extends PacketBase {

    public MCH_RWRThreatTable table;

    public PacketRWRThreatSync() {
        this.table = new MCH_RWRThreatTable();
    }

    public PacketRWRThreatSync(MCH_RWRThreatTable table) {
        this.table = table != null ? table : new MCH_RWRThreatTable();
    }

    @Override
    public void encodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        data.writeInt(this.table.receiverEntityId);
        data.writeLong(this.table.snapshotSeq);
        List<MCH_RWRThreatEvent> events = this.table.events != null ? this.table.events : new ArrayList<MCH_RWRThreatEvent>();
        data.writeInt(events.size());
        for (MCH_RWRThreatEvent e : events) {
            data.writeInt(e.emitterId);
            data.writeByte(e.emitterKind);
            data.writeByte(e.threatMode);
            data.writeFloat(e.bearingDeg);
            data.writeFloat(e.strength);
            data.writeInt(e.ttlTick);
            data.writeFloat(e.confidence);
            data.writeFloat(e.distanceMeters);
            writeUTF(data, e.sourceName != null ? e.sourceName : "?");
        }
    }

    @Override
    public void decodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        MCH_RWRThreatTable t = new MCH_RWRThreatTable();
        t.receiverEntityId = data.readInt();
        t.snapshotSeq = data.readLong();
        int count = data.readInt();
        t.events = new ArrayList<MCH_RWRThreatEvent>(Math.max(0, count));
        for (int i = 0; i < count; i++) {
            MCH_RWRThreatEvent e = new MCH_RWRThreatEvent();
            e.emitterId = data.readInt();
            e.emitterKind = data.readByte();
            e.threatMode = data.readByte();
            e.bearingDeg = data.readFloat();
            e.strength = data.readFloat();
            e.ttlTick = data.readInt();
            e.confidence = data.readFloat();
            e.distanceMeters = data.readFloat();
            e.sourceName = readUTF(data);
            t.events.add(e);
        }
        this.table = t;
    }

    @Override
    public void handleServerSide(EntityPlayerMP playerEntity) {
    }

    @Override
    public void handleClientSide(EntityPlayer clientPlayer) {
        MCH_RWRThreatClientTracker.updateTable(this.table);
    }
}
