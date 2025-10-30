package mcheli.network.packets;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import mcheli.MCH_DamageIndicator;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.network.PacketBase;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.Vec3;

public class PacketDamageIndicator extends PacketBase {
    public double relativeHitPosX, relativeHitPosY, relativeHitPosZ;
    public double relativeDirX, relativeDirY, relativeDirZ;
    public String weaponName;
    public String hitBoxName;
    public double damage;
    public int acId;


    public PacketDamageIndicator() {
    }

    public PacketDamageIndicator(double relativeHitPosX, double relativeHitPosY, double relativeHitPosZ, double relativeDirX, double relativeDirY, double relativeDirZ, String weaponName, String hitBoxName, double damage, int acId) {
        this.relativeHitPosX = relativeHitPosX;
        this.relativeHitPosY = relativeHitPosY;
        this.relativeHitPosZ = relativeHitPosZ;
        this.relativeDirX = relativeDirX;
        this.relativeDirY = relativeDirY;
        this.relativeDirZ = relativeDirZ;
        this.weaponName = weaponName;
        this.hitBoxName = hitBoxName;
        this.damage = damage;
        this.acId = acId;
    }

    @Override
    public void encodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        data.writeDouble(relativeHitPosX);
        data.writeDouble(relativeHitPosY);
        data.writeDouble(relativeHitPosZ);
        data.writeDouble(relativeDirX);
        data.writeDouble(relativeDirY);
        data.writeDouble(relativeDirZ);
        writeUTF(data, weaponName);
        writeUTF(data, hitBoxName);
        data.writeDouble(damage);
        data.writeInt(acId);
    }

    @Override
    public void decodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        relativeHitPosX = data.readDouble();
        relativeHitPosY = data.readDouble();
        relativeHitPosZ = data.readDouble();
        relativeDirX = data.readDouble();
        relativeDirY = data.readDouble();
        relativeDirZ = data.readDouble();
        weaponName = readUTF(data);
        hitBoxName = readUTF(data);
        damage = data.readDouble();
        acId = data.readInt();
    }

    @Override
    public void handleServerSide(EntityPlayerMP playerEntity) {
    }

    @Override
    public void handleClientSide(EntityPlayer clientPlayer) {
        Vec3 relHitPos = Vec3.createVectorHelper(relativeHitPosX, relativeHitPosY, relativeHitPosZ);
        Vec3 relDir = Vec3.createVectorHelper(relativeDirX, relativeDirY, relativeDirZ);
        Entity e = clientPlayer.worldObj.getEntityByID(acId);
        if (e instanceof MCH_EntityAircraft) {
            MCH_DamageIndicator damageIndicator = new MCH_DamageIndicator(relHitPos, relDir, weaponName, hitBoxName, damage);
            ((MCH_EntityAircraft) e).damageIndicatorList.add(damageIndicator);
        }
    }
}
