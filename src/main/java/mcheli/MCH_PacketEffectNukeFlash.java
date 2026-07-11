package mcheli;

import com.google.common.io.ByteArrayDataInput;
import mcheli.wrapper.W_Network;
import net.minecraft.entity.Entity;

import java.io.DataOutputStream;
import java.io.IOException;

public class MCH_PacketEffectNukeFlash extends MCH_Packet {

    public double posX;
    public double posY;
    public double posZ;
    public float explosionSize;
    public float radiusFactor;
    public int minDurationTick;
    public int maxDurationTick;

    public static void send(Entity sender, double x, double y, double z, float explosionSize, float radiusFactor, int minDurationTick, int maxDurationTick) {
        MCH_PacketEffectNukeFlash pkt = new MCH_PacketEffectNukeFlash();
        pkt.posX = x;
        pkt.posY = y;
        pkt.posZ = z;
        pkt.explosionSize = explosionSize;
        pkt.radiusFactor = radiusFactor;
        pkt.minDurationTick = minDurationTick;
        pkt.maxDurationTick = maxDurationTick;
        double syncRange = Math.max(128.0D, explosionSize * radiusFactor + 64.0D);
        if (sender != null) {
            W_Network.sendToAllAround(pkt, sender, syncRange);
        } else {
            W_Network.sendToAllPlayers(pkt);
        }
    }

    @Override
    public int getMessageID() {
        return MSGID_EFFECT_NUKE_FLASH;
    }

    @Override
    public void readData(ByteArrayDataInput data) {
        try {
            this.posX = data.readDouble();
            this.posY = data.readDouble();
            this.posZ = data.readDouble();
            this.explosionSize = data.readFloat();
            this.radiusFactor = data.readFloat();
            this.minDurationTick = data.readInt();
            this.maxDurationTick = data.readInt();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void writeData(DataOutputStream dos) {
        try {
            dos.writeDouble(this.posX);
            dos.writeDouble(this.posY);
            dos.writeDouble(this.posZ);
            dos.writeFloat(this.explosionSize);
            dos.writeFloat(this.radiusFactor);
            dos.writeInt(this.minDurationTick);
            dos.writeInt(this.maxDurationTick);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
