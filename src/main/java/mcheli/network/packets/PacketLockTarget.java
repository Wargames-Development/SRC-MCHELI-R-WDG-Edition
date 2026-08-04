package mcheli.network.packets;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import mcheli.network.PacketBase;
import mcheli.weapon.MCH_EntityBaseBullet;
import mcheli.wrapper.W_Entity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

public class PacketLockTarget extends PacketBase {
    public int targetID;
    public int entityID;

    public PacketLockTarget(int targetID, int entityID) {
        this.targetID = targetID;
        this.entityID = entityID;
    }

    public PacketLockTarget() {
    }

    @Override
    public void encodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        data.writeInt(targetID);
        data.writeInt(entityID);
    }

    @Override
    public void decodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        targetID = data.readInt();
        entityID = data.readInt();
    }

    @Override
    public void handleServerSide(EntityPlayerMP playerEntity) {
        Entity bulletEntity = playerEntity.worldObj.getEntityByID(this.entityID);
        if (!(bulletEntity instanceof MCH_EntityBaseBullet)) {
            return;
        }
        MCH_EntityBaseBullet bullet = (MCH_EntityBaseBullet)bulletEntity;
        if (!W_Entity.isEqual(bullet.shootingEntity, playerEntity) || bullet.isCountermeasureDiversionActive()) {
            return;
        }
        Entity target = this.targetID > 0 ? playerEntity.worldObj.getEntityByID(this.targetID) : null;
        if (target instanceof EntityPlayer || W_Entity.isEqual(target, bullet.shootingEntity)
            || W_Entity.isEqual(target, bullet.shootingAircraft)) {
            return;
        }
        if (target != null && bullet.getInfo() != null) {
            double maxRange = Math.max(256.0D, bullet.getInfo().maxLockOnRange + 256.0D);
            if (bullet.getDistanceSqToEntity(target) > maxRange * maxRange) {
                return;
            }
        }
        bullet.setTargetEntity(target);
    }

    @Override
    public void handleClientSide(EntityPlayer clientPlayer) {
    }
}
