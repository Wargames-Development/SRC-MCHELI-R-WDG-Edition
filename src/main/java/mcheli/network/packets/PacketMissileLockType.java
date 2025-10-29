package mcheli.network.packets;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.network.PacketBase;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

public class PacketMissileLockType extends PacketBase {

    public byte missileLockType;
    /** Missile Lock Type:
     * 0 = None,
     * 1 = Semi-active,
     * 2 = Infrared,
     * 3 = Active,
     * 4 = Unknown
     */

    public byte vehicleLockType;
    /** Vehicle lock type:
     * 0 = None,
     * 1 = Scanning,
     * 2 = Locked – Ground vehicle,
     * 3 = Locked – Airborne vehicle,
     * 4 = Locked – Unknown
     */

    public byte missileLockDist;
    /** Missile lock distance category:
     * 0 = None,
     * 1 = Within 50m,
     * 2 = Within 150m,
     * 3 = Within 600m
     */

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
