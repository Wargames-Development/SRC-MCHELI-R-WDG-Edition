package mcheli.network.packets;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.network.PacketBase;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

public class PacketMissileLockType extends PacketBase {

    public byte missileLockType; // 0-未锁定 1-半主动 2-红外 3-主动 4-未知
    public byte vehicleLockType; // 0-未锁定 1-扫描 2-锁定
    public byte missileLockDist; // 0-未锁定 1-50m内 2-150m内 3-600m内

    public PacketMissileLockType(byte missileLockType, byte vehicleLockType, byte missileLockDist) {
        this.missileLockType = missileLockType;
        this.vehicleLockType = vehicleLockType;
        this.missileLockDist = missileLockDist;
    }

    public PacketMissileLockType() {
    }

    @Override
    public void encodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        data.writeByte(this.missileLockType);
        data.writeByte(this.vehicleLockType);
        data.writeByte(this.missileLockDist);
    }

    @Override
    public void decodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        this.missileLockType = data.readByte();
        this.vehicleLockType = data.readByte();
        this.missileLockDist = data.readByte();
    }

    @Override
    public void handleServerSide(EntityPlayerMP playerEntity) {
    }

    @Override
    public void handleClientSide(EntityPlayer clientPlayer) {
        Entity e = clientPlayer.ridingEntity;
        if(e instanceof MCH_EntityAircraft) {
            MCH_EntityAircraft ac = (MCH_EntityAircraft)e;
            if(ac.missileDetector != null) {
                ac.missileDetector.missileLockType = this.missileLockType;
                ac.missileDetector.vehicleLockType = this.vehicleLockType;
                ac.missileDetector.missileLockDist = this.missileLockDist;
            }
        }
    }
}
