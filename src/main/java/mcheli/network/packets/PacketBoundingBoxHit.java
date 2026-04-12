package mcheli.network.packets;

import cpw.mods.fml.client.FMLClientHandler;
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
    public float impactAngle;

    public PacketBoundingBoxHit() {
    }

    public PacketBoundingBoxHit(int targetID, String name, float damage, byte damageType) {
        this(targetID, name, damage, damageType, -1.0F);
    }

    public PacketBoundingBoxHit(int targetID, String name, float damage, byte damageType, float impactAngle) {
        this.targetID = targetID;
        this.name = name;
        this.damage = damage;
        this.damageType = damageType;
        this.impactAngle = impactAngle;
    }

    @Override
    public void encodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        data.writeInt(targetID);
        writeUTF(data, name);
        data.writeFloat(damage);
        data.writeByte(damageType);
        data.writeFloat(impactAngle);
    }

    @Override
    public void decodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        targetID = data.readInt();
        name = readUTF(data);
        damage = data.readFloat();
        damageType = data.readByte();
        impactAngle = data.readFloat();
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
            hitMessage.hitDisplay = formatNormalHitMessage(name, impactAngle);
        } else if (damageType == 1) {
            hitMessage.hitDisplay = MCH_I18n.format("message.mcheli.overpressure");
        } else if (damageType == 2) {
            hitMessage.hitDisplay = formatRicochetMessage(impactAngle);
        }
        hitMessage.hitDamage = damage;
        hitMessage.hitDamageType = damageType;
        MCH_ClientCommonTickHandler.addHitMessage(hitMessage);
        MCH_ClientCommonTickHandler.addTotalDamage(damage);
    }

    private String formatNormalHitMessage(String hitName, float angle) {
        if (angle < 0.0F) {
            return hitName;
        }
        return isEnglishLocale() ? String.format("%s Angle: %.1f°", hitName, angle) : String.format("%s 入射角: %.1f°", hitName, angle);
    }

    private String formatRicochetMessage(float angle) {
        if (angle < 0.0F) {
            return isEnglishLocale() ? "Ricochet" : "跳弹";
        }
        return isEnglishLocale() ? String.format("Ricochet Angle: %.1f°", angle) : String.format("跳弹 入射角: %.1f°", angle);
    }

    private boolean isEnglishLocale() {
        String lang = FMLClientHandler.instance().getClient().gameSettings.language;
        return lang != null && lang.toLowerCase().startsWith("en");
    }
}
