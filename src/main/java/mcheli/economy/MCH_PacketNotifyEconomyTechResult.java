package mcheli.economy;

import com.google.common.io.ByteArrayDataInput;
import mcheli.MCH_Packet;
import mcheli.wrapper.W_Network;
import net.minecraft.entity.player.EntityPlayerMP;

import java.io.DataOutputStream;
import java.io.IOException;

public class MCH_PacketNotifyEconomyTechResult extends MCH_Packet {

    public boolean success;
    public byte action;
    public String nodeId = "";
    public String message = "";
    public int sl;
    public int ge;
    public int rp;
    public String unlockedNodes = "";
    public String activeTreeId = "";
    public String allowedTreeIds = "";

    public static void sendToPlayer(EntityPlayerMP player, MCH_EconomyTechService.Result result) {
        if (player == null || result == null) {
            return;
        }
        MCH_PacketNotifyEconomyTechResult pkt = new MCH_PacketNotifyEconomyTechResult();
        pkt.success = result.success;
        pkt.action = result.action;
        pkt.nodeId = result.nodeId;
        pkt.message = result.message;
        pkt.sl = MCH_EconomyService.getSL(player);
        pkt.ge = MCH_EconomyService.getGE(player);
        pkt.rp = MCH_EconomyService.getRP(player);
        pkt.unlockedNodes = MCH_EconomyTechService.getUnlockedNodesRaw(player);
        pkt.activeTreeId = MCH_EconomyTechService.getActiveTechTreeId(player);
        pkt.allowedTreeIds = MCH_EconomyTechService.getAllowedTechTreeIdsRaw(player);
        W_Network.sendToPlayer(pkt, player);
    }

    @Override
    public int getMessageID() {
        return MSGID_NOTIFY_ECONOMY_TECH_RESULT;
    }

    @Override
    public void readData(ByteArrayDataInput data) {
        try {
            this.success = data.readBoolean();
            this.action = data.readByte();
            this.nodeId = data.readUTF();
            this.message = data.readUTF();
            this.sl = data.readInt();
            this.ge = data.readInt();
            this.rp = data.readInt();
            this.unlockedNodes = data.readUTF();
            this.activeTreeId = data.readUTF();
            this.allowedTreeIds = data.readUTF();
        } catch (Exception e) {
            e.printStackTrace();
            this.success = false;
            this.action = MCH_PacketIndEconomyTechAction.ACTION_UNLOCK_RP;
            this.nodeId = "";
            this.message = "数据解析失败";
            this.sl = 0;
            this.ge = 0;
            this.rp = 0;
            this.unlockedNodes = "";
            this.activeTreeId = "";
            this.allowedTreeIds = "";
        }
    }

    @Override
    public void writeData(DataOutputStream dos) {
        try {
            dos.writeBoolean(this.success);
            dos.writeByte(this.action);
            dos.writeUTF(this.nodeId == null ? "" : this.nodeId);
            dos.writeUTF(this.message == null ? "" : this.message);
            dos.writeInt(Math.max(0, this.sl));
            dos.writeInt(Math.max(0, this.ge));
            dos.writeInt(Math.max(0, this.rp));
            dos.writeUTF(this.unlockedNodes == null ? "" : this.unlockedNodes);
            dos.writeUTF(this.activeTreeId == null ? "" : this.activeTreeId);
            dos.writeUTF(this.allowedTreeIds == null ? "" : this.allowedTreeIds);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
