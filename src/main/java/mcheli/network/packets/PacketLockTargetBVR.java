package mcheli.network.packets;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.network.PacketBase;
import mcheli.weapon.MCH_EntityAAMissile;
import mcheli.wrapper.W_Entity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

public class PacketLockTargetBVR extends PacketBase {

    public int mslId;
    public int targetEntityId;
    public int posX;
    public int posY;
    public int posZ;

    public PacketLockTargetBVR(int mslId, int targetEntityId, int posX, int posY, int posZ) {
        this.mslId = mslId;
        this.targetEntityId = targetEntityId;
        this.posX = posX;
        this.posY = posY;
        this.posZ = posZ;
    }

    public PacketLockTargetBVR() {
    }

    @Override
    public void encodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        data.writeInt(this.mslId);
        data.writeInt(this.targetEntityId);
        data.writeInt(this.posX);
        data.writeInt(this.posY);
        data.writeInt(this.posZ);
    }

    @Override
    public void decodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        this.mslId = data.readInt();
        this.targetEntityId = data.readInt();
        this.posX = data.readInt();
        this.posY = data.readInt();
        this.posZ = data.readInt();
    }

    @Override
    public void handleServerSide(EntityPlayerMP playerEntity) {
        Entity entity = playerEntity.worldObj.getEntityByID(this.mslId);
        if (!(entity instanceof MCH_EntityAAMissile)) {
            return;
        }
        MCH_EntityAAMissile missile = (MCH_EntityAAMissile)entity;
        if (!W_Entity.isEqual(missile.shootingEntity, playerEntity) || missile.getInfo() == null
            || !missile.getInfo().enableBVR || missile.isCountermeasureDiversionActive()) {
            return;
        }

        boolean enable = this.posY > 0;
        if (enable) {
            double dx = this.posX - missile.posX;
            double dy = this.posY - missile.posY;
            double dz = this.posZ - missile.posZ;
            double maxRange = Math.max(512.0D, missile.getInfo().maxLockOnRange * 1.25D);
            if (dx * dx + dy * dy + dz * dz > maxRange * maxRange) {
                return;
            }
        }

        missile.passiveRadarBVRLocking = enable;
        missile.passiveRadarBVRLockingPosX = this.posX;
        missile.passiveRadarBVRLockingPosY = this.posY;
        missile.passiveRadarBVRLockingPosZ = this.posZ;
        if (!enable || this.targetEntityId <= 0) {
            missile.setTargetEntity(null);
            return;
        }
        Entity target = playerEntity.worldObj.getEntityByID(this.targetEntityId);
        missile.setTargetEntity(target instanceof MCH_EntityAircraft ? target : null);
    }

    @Override
    public void handleClientSide(EntityPlayer clientPlayer) {
    }
}
