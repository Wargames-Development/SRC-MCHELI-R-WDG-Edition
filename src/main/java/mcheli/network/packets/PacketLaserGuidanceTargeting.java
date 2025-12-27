package mcheli.network.packets;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.network.PacketBase;
import mcheli.weapon.MCH_LaserGuidanceSystem;
import mcheli.weapon.MCH_WeaponBase;
import mcheli.weapon.MCH_WeaponBomb;
import mcheli.weapon.MCH_WeaponTvMissile;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

public class PacketLaserGuidanceTargeting extends PacketBase {

    boolean targeting;

    double targetPosX;
    double targetPosY;
    double targetPosZ;

    public PacketLaserGuidanceTargeting(boolean targeting, double targetPosX, double targetPosY, double targetPosZ) {
        this.targeting = targeting;
        this.targetPosX = targetPosX;
        this.targetPosY = targetPosY;
        this.targetPosZ = targetPosZ;
    }

    public PacketLaserGuidanceTargeting() {
    }

    @Override
    public void encodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        data.writeBoolean(targeting);
        data.writeDouble(targetPosX);
        data.writeDouble(targetPosY);
        data.writeDouble(targetPosZ);
    }

    @Override
    public void decodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        targeting = data.readBoolean();
        targetPosX = data.readDouble();
        targetPosY = data.readDouble();
        targetPosZ = data.readDouble();
    }

    @Override
    public void handleServerSide(EntityPlayerMP playerEntity) {
        MCH_EntityAircraft ac = MCH_EntityAircraft.getAircraft_RiddenOrControl(playerEntity);
        if (ac != null && ac.getCurrentWeapon(playerEntity).getCurrentWeapon() instanceof MCH_WeaponBase) {
            MCH_WeaponBase weapon = ac.getCurrentWeapon(playerEntity).getCurrentWeapon();
            if (weapon.getGuidanceSystem() instanceof MCH_LaserGuidanceSystem) {
                MCH_LaserGuidanceSystem system = (MCH_LaserGuidanceSystem) weapon.getGuidanceSystem();
                system.targeting = targeting;
                system.targetPosX = targetPosX;
                system.targetPosY = targetPosY;
                system.targetPosZ = targetPosZ;
            }
        }
    }

    @Override
    public void handleClientSide(EntityPlayer clientPlayer) {

    }
}
