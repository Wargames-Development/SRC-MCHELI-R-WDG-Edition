/*
 * Decompiled with CFR 0_123.
 *
 * Could not load the following classes:
 *  cpw.mods.fml.common.network.NetworkRegistry
 *  cpw.mods.fml.common.network.NetworkRegistry$TargetPoint
 *  cpw.mods.fml.common.network.simpleimpl.IMessage
 *  cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.entity.player.EntityPlayerMP
 */
package mcheli.wrapper;

import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import mcheli.MCH_NetworkProfiler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

public class W_Network {
    public static final SimpleNetworkWrapper INSTANCE = NetworkRegistry.INSTANCE.newSimpleChannel("MCHeli_CH");

    public static void sendToServer(W_PacketBase pkt) {
        INSTANCE.sendToServer((IMessage) pkt);
    }

    public static void sendToPlayer(W_PacketBase pkt, EntityPlayer player) {
        if (player instanceof EntityPlayerMP) {
            MCH_NetworkProfiler.recordServerSend("MCHeli_CH", pkt, 1);
            INSTANCE.sendTo((IMessage) pkt, (EntityPlayerMP) player);
        }
    }

    public static void sendToAllAround(W_PacketBase pkt, Entity sender, double renge) {
        MCH_NetworkProfiler.recordServerSend("MCHeli_CH", pkt,
            MCH_NetworkProfiler.countPlayersAround(sender.dimension, sender.posX, sender.posY, sender.posZ, renge));
        NetworkRegistry.TargetPoint t = new NetworkRegistry.TargetPoint(sender.dimension, sender.posX, sender.posY, sender.posZ, renge);
        INSTANCE.sendToAllAround((IMessage) pkt, t);
    }

    public static void sendToAllPlayers(W_PacketBase pkt) {
        MCH_NetworkProfiler.recordServerSend("MCHeli_CH", pkt, MCH_NetworkProfiler.getPlayerCount());
        INSTANCE.sendToAll((IMessage) pkt);
    }
}
