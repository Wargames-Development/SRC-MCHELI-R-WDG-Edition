package mcheli.network.packets;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.aircraft.MCH_EntitySeat;
import mcheli.network.PacketBase;
import mcheli.uav.MCH_EntityUavStation;
import mcheli.weapon.MCH_WeaponParam;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

public class PacketUseWeapon extends PacketBase {

    public int useWeaponOption1 = 0;
    public int useWeaponOption2 = 0;
    public double useWeaponPosX = 0.0D;
    public double useWeaponPosY = 0.0D;
    public double useWeaponPosZ = 0.0D;

    public PacketUseWeapon(int useWeaponOption1, int useWeaponOption2, double useWeaponPosX, double useWeaponPosY, double useWeaponPosZ) {
        this.useWeaponOption1 = useWeaponOption1;
        this.useWeaponOption2 = useWeaponOption2;
        this.useWeaponPosX = useWeaponPosX;
        this.useWeaponPosY = useWeaponPosY;
        this.useWeaponPosZ = useWeaponPosZ;
    }

    public PacketUseWeapon() {
    }

    @Override
    public void encodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        data.writeInt(useWeaponOption1);
        data.writeInt(useWeaponOption2);
        data.writeDouble(useWeaponPosX);
        data.writeDouble(useWeaponPosY);
        data.writeDouble(useWeaponPosZ);
    }

    @Override
    public void decodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        useWeaponOption1 = data.readInt();
        useWeaponOption2 = data.readInt();
        useWeaponPosX =  data.readDouble();
        useWeaponPosY = data.readDouble();
        useWeaponPosZ = data.readDouble();
    }

    @Override
    public void handleServerSide(EntityPlayerMP player) {
        MCH_EntityAircraft ac = null;
        if (player.ridingEntity instanceof MCH_EntityAircraft) {
            ac = (MCH_EntityAircraft) player.ridingEntity;
        } else if (player.ridingEntity instanceof MCH_EntitySeat) {
            if (((MCH_EntitySeat) player.ridingEntity).getParent() instanceof MCH_EntityAircraft) {
                ac = ((MCH_EntitySeat) player.ridingEntity).getParent();
            }
        } else if (player.ridingEntity instanceof MCH_EntityUavStation) {
            MCH_EntityUavStation uavStation = (MCH_EntityUavStation) player.ridingEntity;
            if (uavStation.getControlAircract() instanceof MCH_EntityAircraft) {
                ac = uavStation.getControlAircract();
            }
        }
        if (ac != null) {
            MCH_WeaponParam param = new MCH_WeaponParam();
            param.entity = ac;
            param.user = player;
            param.setPosAndRot(useWeaponPosX, useWeaponPosY, useWeaponPosZ, 0.0F, 0.0F);
            param.option1 = useWeaponOption1;
            param.option2 = useWeaponOption2;
            ac.useCurrentWeapon(param);
        }
    }

    @Override
    public void handleClientSide(EntityPlayer clientPlayer) {

    }
}
