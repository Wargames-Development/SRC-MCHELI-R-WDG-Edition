package mcheli.economy;

import com.google.common.io.ByteArrayDataInput;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

public final class MCH_EconomyPacketHandler {

    private MCH_EconomyPacketHandler() {
    }

    public static void onPacketNotifyEconomySync(EntityPlayer player, ByteArrayDataInput data) {
        if (player == null || !player.worldObj.isRemote) {
            return;
        }
        MCH_PacketNotifyEconomySync pkt = new MCH_PacketNotifyEconomySync();
        pkt.readData(data);
        MCH_EconomyClientData.update(pkt.sl, pkt.ge, pkt.rp);
        MCH_EconomyClientData.updateUnlockedNodes(pkt.unlockedNodes);
    }

    public static void onPacketIndEconomyTechAction(EntityPlayer player, ByteArrayDataInput data) {
        if (player == null || player.worldObj.isRemote || !(player instanceof EntityPlayerMP)) {
            return;
        }
        MCH_PacketIndEconomyTechAction pkt = new MCH_PacketIndEconomyTechAction();
        pkt.readData(data);
        MCH_EconomyTechService.Result result = MCH_EconomyTechService.execute((EntityPlayerMP) player, pkt.nodeId, pkt.action);
        MCH_PacketNotifyEconomyTechResult.sendToPlayer((EntityPlayerMP) player, result);
    }

    public static void onPacketNotifyEconomyTechResult(EntityPlayer player, ByteArrayDataInput data) {
        if (player == null || !player.worldObj.isRemote) {
            return;
        }
        MCH_PacketNotifyEconomyTechResult pkt = new MCH_PacketNotifyEconomyTechResult();
        pkt.readData(data);
        MCH_EconomyClientData.update(pkt.sl, pkt.ge, pkt.rp);
        MCH_EconomyClientData.updateUnlockedNodes(pkt.unlockedNodes);
        MCH_EconomyClientData.updateTechResult(pkt.success, pkt.action, pkt.nodeId, pkt.message);
    }

    public static void onPacketNotifyEconomyGainToast(EntityPlayer player, ByteArrayDataInput data) {
        if (player == null || !player.worldObj.isRemote) {
            return;
        }
        MCH_PacketNotifyEconomyGainToast pkt = new MCH_PacketNotifyEconomyGainToast();
        pkt.readData(data);
        MCH_EconomyClientData.showGainToast(pkt.type, pkt.sl, pkt.ge, pkt.rp, 2000L);
    }
}
