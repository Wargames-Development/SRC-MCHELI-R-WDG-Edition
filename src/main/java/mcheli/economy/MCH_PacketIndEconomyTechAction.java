package mcheli.economy;

import com.google.common.io.ByteArrayDataInput;
import mcheli.MCH_Packet;
import mcheli.wrapper.W_Network;

import java.io.DataOutputStream;
import java.io.IOException;

public class MCH_PacketIndEconomyTechAction extends MCH_Packet {

    public static final byte ACTION_UNLOCK_RP = 0;
    public static final byte ACTION_PURCHASE_SL = 1;
    public static final byte ACTION_EXCHANGE_GE = 2;

    public byte action;
    public String nodeId = "";

    public static void send(byte action, String nodeId) {
        MCH_PacketIndEconomyTechAction pkt = new MCH_PacketIndEconomyTechAction();
        pkt.action = action;
        pkt.nodeId = nodeId == null ? "" : nodeId;
        W_Network.sendToServer(pkt);
    }

    @Override
    public int getMessageID() {
        return MSGID_IND_ECONOMY_TECH_ACTION;
    }

    @Override
    public void readData(ByteArrayDataInput data) {
        try {
            this.action = data.readByte();
            this.nodeId = data.readUTF();
        } catch (Exception e) {
            e.printStackTrace();
            this.action = ACTION_UNLOCK_RP;
            this.nodeId = "";
        }
    }

    @Override
    public void writeData(DataOutputStream dos) {
        try {
            dos.writeByte(this.action);
            dos.writeUTF(this.nodeId == null ? "" : this.nodeId);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
