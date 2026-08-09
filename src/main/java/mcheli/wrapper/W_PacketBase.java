/*
 * Decompiled with CFR 0_123.
 *
 * Could not load the following classes:
 *  com.google.common.io.ByteArrayDataInput
 *  com.google.common.io.ByteStreams
 *  cpw.mods.fml.common.network.simpleimpl.IMessage
 *  io.netty.buffer.ByteBuf
 */
package mcheli.wrapper;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;
import mcheli.MCH_NetworkProfiler;

public class W_PacketBase
    implements IMessage {
    ByteArrayDataInput data;

    public byte[] createData() {
        return null;
    }

    public void fromBytes(ByteBuf buf) {
        if (MCH_NetworkProfiler.isActive() && FMLCommonHandler.instance().getEffectiveSide().isServer()) {
            int readableBytes = buf.readableBytes();
            String packetName = readableBytes >= 4
                ? MCH_NetworkProfiler.getLegacyPacketName(buf.getInt(buf.readerIndex())) : "MSG_TRUNCATED";
            MCH_NetworkProfiler.recordServerReceived("MCHeli_CH", packetName, readableBytes, 0L);
        }
        byte[] dst = new byte[buf.array().length - 1];
        buf.getBytes(0, dst);
        this.data = ByteStreams.newDataInput(dst);
    }

    public void toBytes(ByteBuf buf) {
        boolean profiling = MCH_NetworkProfiler.isActive()
            && FMLCommonHandler.instance().getEffectiveSide().isServer();
        long started = profiling ? System.nanoTime() : 0L;
        byte[] encoded = this.createData();
        buf.writeBytes(encoded);
        if (profiling) {
            MCH_NetworkProfiler.recordServerEncoded("MCHeli_CH", this, encoded.length,
                System.nanoTime() - started);
        }
    }
}
