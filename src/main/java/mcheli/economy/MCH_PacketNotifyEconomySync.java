package mcheli.economy;

import com.google.common.io.ByteArrayDataInput;
import mcheli.MCH_Packet;
import mcheli.wrapper.W_Network;
import net.minecraft.entity.player.EntityPlayerMP;

import java.io.DataOutputStream;
import java.io.IOException;

public class MCH_PacketNotifyEconomySync extends MCH_Packet {

    public int sl;
    public int ge;
    public int rp;
    public String unlockedNodes = "";

    public MCH_PacketNotifyEconomySync() {
    }

    public MCH_PacketNotifyEconomySync(int sl, int ge, int rp) {
        this(sl, ge, rp, "");
    }

    public MCH_PacketNotifyEconomySync(int sl, int ge, int rp, String unlockedNodes) {
        this.sl = Math.max(0, sl);
        this.ge = Math.max(0, ge);
        this.rp = Math.max(0, rp);
        this.unlockedNodes = unlockedNodes == null ? "" : unlockedNodes;
    }

    public static void sendToPlayer(EntityPlayerMP player, int sl, int ge, int rp) {
        sendToPlayer(player, sl, ge, rp, "");
    }

    public static void sendToPlayer(EntityPlayerMP player, int sl, int ge, int rp, String unlockedNodes) {
        if (player == null) {
            return;
        }
        W_Network.sendToPlayer(new MCH_PacketNotifyEconomySync(sl, ge, rp, unlockedNodes), player);
    }

    @Override
    public int getMessageID() {
        return MSGID_NOTIFY_ECONOMY_SYNC;
    }

    @Override
    public void readData(ByteArrayDataInput data) {
        try {
            this.sl = data.readInt();
            this.ge = data.readInt();
            this.rp = data.readInt();
            this.unlockedNodes = data.readUTF();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void writeData(DataOutputStream dos) {
        try {
            dos.writeInt(Math.max(0, this.sl));
            dos.writeInt(Math.max(0, this.ge));
            dos.writeInt(Math.max(0, this.rp));
            dos.writeUTF(this.unlockedNodes == null ? "" : this.unlockedNodes);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
