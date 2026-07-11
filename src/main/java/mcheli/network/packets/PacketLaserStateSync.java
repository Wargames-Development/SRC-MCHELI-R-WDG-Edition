package mcheli.network.packets;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import mcheli.network.PacketBase;
import mcheli.weapon.MCH_LaserStateStore;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

/**
 * Phase-A packet skeleton.
 * Carries per-channel laser state with sequence for out-of-order protection.
 */
public class PacketLaserStateSync extends PacketBase {

    public int sourceType;
    public long sequence;
    public boolean active;
    public double x;
    public double y;
    public double z;
    public int ownerId;

    public PacketLaserStateSync() {
    }

    public PacketLaserStateSync(int sourceType, long sequence, boolean active, double x, double y, double z, int ownerId) {
        this.sourceType = sourceType;
        this.sequence = sequence;
        this.active = active;
        this.x = x;
        this.y = y;
        this.z = z;
        this.ownerId = ownerId;
    }

    @Override
    public void encodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        data.writeInt(sourceType);
        data.writeLong(sequence);
        data.writeBoolean(active);
        data.writeDouble(x);
        data.writeDouble(y);
        data.writeDouble(z);
        data.writeInt(ownerId);
    }

    @Override
    public void decodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        sourceType = data.readInt();
        sequence = data.readLong();
        active = data.readBoolean();
        x = data.readDouble();
        y = data.readDouble();
        z = data.readDouble();
        ownerId = data.readInt();
    }

    @Override
    public void handleServerSide(EntityPlayerMP playerEntity) {
        if (playerEntity == null || playerEntity.worldObj == null) {
            return;
        }
        if (!MCH_LaserStateStore.isValidSourceType(sourceType)) {
            return;
        }
        // Never trust owner from client packet.
        int trustedOwnerId = playerEntity.getEntityId();
        long now = playerEntity.worldObj.getTotalWorldTime();
        MCH_LaserStateStore.expireServerStates(now, MCH_LaserStateStore.DEFAULT_TTL_TICKS);
        MCH_LaserStateStore.upsertServerState(trustedOwnerId, sourceType, x, y, z, active, sequence, now);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void handleClientSide(EntityPlayer clientPlayer) {
        if (clientPlayer == null || clientPlayer.worldObj == null) {
            return;
        }
        if (!MCH_LaserStateStore.isValidSourceType(sourceType)) {
            return;
        }
        long now = clientPlayer.worldObj.getTotalWorldTime();
        MCH_LaserStateStore.expireClientStates(now, MCH_LaserStateStore.DEFAULT_TTL_TICKS);
        MCH_LaserStateStore.upsertClientState(ownerId, sourceType, x, y, z, active, sequence, now);
    }
}
