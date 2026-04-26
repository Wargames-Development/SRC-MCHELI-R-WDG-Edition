package mcheli.economy;

import com.google.common.io.ByteArrayDataInput;
import mcheli.MCH_Packet;
import mcheli.wrapper.W_Network;
import net.minecraft.entity.player.EntityPlayerMP;

import java.io.DataOutputStream;
import java.io.IOException;

public class MCH_PacketNotifyEconomyGainToast extends MCH_Packet {

    public static final byte TYPE_MOB_KILL = 1;
    public static final byte TYPE_VEHICLE_DESTROY = 2;

    public byte type;
    public int sl;
    public int ge;
    public int rp;

    public static void sendToPlayer(EntityPlayerMP player, byte type, int sl, int ge, int rp) {
        if (player == null) {
            return;
        }
        MCH_PacketNotifyEconomyGainToast pkt = new MCH_PacketNotifyEconomyGainToast();
        pkt.type = type;
        pkt.sl = Math.max(0, sl);
        pkt.ge = Math.max(0, ge);
        pkt.rp = Math.max(0, rp);
        if (pkt.sl <= 0 && pkt.ge <= 0 && pkt.rp <= 0) {
            return;
        }
        W_Network.sendToPlayer(pkt, player);
    }

    @Override
    public int getMessageID() {
        return MSGID_NOTIFY_ECONOMY_GAIN_TOAST;
    }

    @Override
    public void readData(ByteArrayDataInput data) {
        try {
            this.type = data.readByte();
            this.sl = data.readInt();
            this.ge = data.readInt();
            this.rp = data.readInt();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void writeData(DataOutputStream dos) {
        try {
            dos.writeByte(this.type);
            dos.writeInt(Math.max(0, this.sl));
            dos.writeInt(Math.max(0, this.ge));
            dos.writeInt(Math.max(0, this.rp));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
