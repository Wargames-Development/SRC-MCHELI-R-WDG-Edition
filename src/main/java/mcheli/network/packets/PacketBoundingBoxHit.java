package mcheli.network.packets;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import mcheli.MCH_ClientCommonTickHandler;
import mcheli.network.PacketBase;
import mcheli.weapon.MCH_EntityBaseBullet;
import mcheli.wrapper.W_Entity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

public class PacketBoundingBoxHit extends PacketBase {
    public int targetID;
    public String name;
    public float damage;

    public PacketBoundingBoxHit() {
    }

    public PacketBoundingBoxHit(int targetID, String name, float damage) {
        this.targetID = targetID;
        this.name = name;
        this.damage = damage;
    }

    @Override
    public void encodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        data.writeInt(targetID);
        writeUTF(data, name);
        data.writeFloat(damage);
    }

    @Override
    public void decodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        targetID = data.readInt();
        name = readUTF(data);
        damage = data.readFloat();
    }

    @Override
    public void handleServerSide(EntityPlayerMP playerEntity) {
    }

    @Override
    public void handleClientSide(EntityPlayer clientPlayer) {
        MCH_ClientCommonTickHandler.hitDisplayCountdown = 20;
        MCH_ClientCommonTickHandler.hitTotalDamageClearCountdown = 60;
        MCH_ClientCommonTickHandler.hitTotalDamage += damage;
        MCH_ClientCommonTickHandler.hitDisplay = name;
    }
}
