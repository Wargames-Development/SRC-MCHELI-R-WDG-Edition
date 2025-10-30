package mcheli.network.packets;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import mcheli.MCH_ClientCommonTickHandler;
import mcheli.MCH_I18n;
import mcheli.network.PacketBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

public class PacketBoundingBoxHit extends PacketBase {
    public int targetID;
    public String name;
    public float damage;
    public byte damageType; // 0正常 1爆炸

    public PacketBoundingBoxHit() {
    }

    public PacketBoundingBoxHit(int targetID, String name, float damage, byte damageType) {
        this.targetID = targetID;
        this.name = name;
        this.damage = damage;
        this.damageType = damageType;
    }

    @Override
    public void encodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        data.writeInt(targetID);
        writeUTF(data, name);
        data.writeFloat(damage);
        data.writeByte(damageType);
    }

    @Override
    public void decodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        targetID = data.readInt();
        name = readUTF(data);
        damage = data.readFloat();
        damageType = data.readByte();
    }

    @Override
    public void handleServerSide(EntityPlayerMP playerEntity) {
    }

    @Override
    public void handleClientSide(EntityPlayer clientPlayer) {
        MCH_ClientCommonTickHandler.hitDisplayCountdown = 40;
        MCH_ClientCommonTickHandler.hitTotalDamageClearCountdown = 60;
        MCH_ClientCommonTickHandler.HitMessage hitMessage = new MCH_ClientCommonTickHandler.HitMessage();
        if (damageType == 0) {
            hitMessage.hitDisplay = name;
        } else if (damageType == 1) {
            hitMessage.hitDisplay = MCH_I18n.format("message.mcheli.overpressure");
        }
        hitMessage.hitDamage = damage;
        hitMessage.hitDamageType = damageType;
        MCH_ClientCommonTickHandler.addHitMessage(hitMessage);
        MCH_ClientCommonTickHandler.addTotalDamage(damage);
    }
}
