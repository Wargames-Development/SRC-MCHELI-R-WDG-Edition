package mcheli.network.packets;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import mcheli.network.PacketBase;
import mcheli.weapon.MCH_LaserStateStore;
import mcheli.weapon.MCH_LaserTargeting;
import mcheli.weapon.MCH_LaserGuidanceSystem;
import mcheli.weapon.MCH_WeaponSet;
import mcheli.aircraft.MCH_EntityAircraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import java.util.LinkedHashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Carries per-channel laser intent; only the server tick publishes validated gameplay state.
 */
public class PacketLaserStateSync extends PacketBase {
    private static final Map<EntityPlayerMP, Intent[]> PENDING = new LinkedHashMap<>();

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
        if (MCH_LaserStateStore.isValidSourceType(sourceType) && sequence >= 0)
            enqueue(playerEntity, sourceType - 1, new Intent(playerEntity, sourceType, sequence, active, x, y, z));
    }

    public static void enqueueGuidance(EntityPlayerMP player, boolean active, double x, double y, double z) {
        enqueue(player, 2, new Intent(player, MCH_LaserStateStore.SOURCE_AIRCRAFT, -1, active, x, y, z));
    }

    private static synchronized void enqueue(EntityPlayerMP player, int slot, Intent intent) {
        if (player == null) return;
        Intent[] pending = PENDING.get(player);
        if (pending == null) {
            if (PENDING.size() >= 128) return;
            pending = new Intent[3]; PENDING.put(player, pending);
        }
        if (pending[slot] == null || intent.sequence >= pending[slot].sequence) pending[slot] = intent;
    }

    /** At most 16 players and one latest intent per channel/player per tick. Never trace on Netty. */
    public static void drainServerIntents() {
        Map<EntityPlayerMP, Intent[]> batch = new LinkedHashMap<>();
        synchronized (PacketLaserStateSync.class) {
            for (int i = 0; i < 16; i++) {
                Iterator<Map.Entry<EntityPlayerMP, Intent[]>> it = PENDING.entrySet().iterator();
                if (!it.hasNext()) break;
                Map.Entry<EntityPlayerMP, Intent[]> entry = it.next();
                batch.put(entry.getKey(), entry.getValue()); it.remove();
            }
        }
        for (Map.Entry<EntityPlayerMP, Intent[]> entry : batch.entrySet()) {
            EntityPlayerMP player = entry.getKey();
            Intent[] pending = entry.getValue();
            for (Intent intent : pending) {
                if (intent == null || player.worldObj == null || player.worldObj != intent.world
                        || player.worldObj.isRemote || player.isDead || !player.worldObj.playerEntities.contains(player)
                        || System.nanoTime() - intent.receivedAt > 2_000_000_000L) continue;
                long now = player.worldObj.getTotalWorldTime();
                MCH_LaserStateStore.LaserState old = MCH_LaserStateStore.getServerState(
                        player.getEntityId(), intent.source, now, player.worldObj);
                if (intent.sequence >= 0 && old != null && intent.sequence <= old.sequence) continue;
                Vec3 point = intent.active ? MCH_LaserTargeting.validate(player, intent.source, intent.x, intent.y, intent.z) : null;
                boolean active = intent.active && point != null;
                if (intent.sequence >= 0) {
                    MCH_LaserStateStore.expireServerStates(now, MCH_LaserStateStore.DEFAULT_TTL_TICKS);
                    MCH_LaserStateStore.upsertServerState(player.getEntityId(), intent.source,
                            active ? point.xCoord : 0, active ? point.yCoord : 0, active ? point.zCoord : 0,
                            active, intent.sequence, now, player.worldObj);
                }
                if (intent.source == MCH_LaserStateStore.SOURCE_AIRCRAFT) {
                    MCH_EntityAircraft ac = MCH_EntityAircraft.getAircraft_RiddenOrControl(player);
                    MCH_WeaponSet set = ac == null ? null : ac.getCurrentWeapon(player);
                    if (set != null && set.getCurrentWeapon() != null
                            && set.getCurrentWeapon().getGuidanceSystem() instanceof MCH_LaserGuidanceSystem) {
                        MCH_LaserGuidanceSystem system = (MCH_LaserGuidanceSystem)set.getCurrentWeapon().getGuidanceSystem();
                        system.targeting = active;
                        if (active) { system.targetPosX = point.xCoord; system.targetPosY = point.yCoord; system.targetPosZ = point.zCoord; }
                    }
                }
            }
        }
    }

    private static final class Intent {
        final World world;
        final int source;
        final long sequence, receivedAt = System.nanoTime();
        final boolean active;
        final double x, y, z;
        Intent(EntityPlayerMP player, int source, long sequence, boolean active, double x, double y, double z) {
            this.world = player == null ? null : player.worldObj;
            this.source = source; this.sequence = sequence; this.active = active;
            this.x = x; this.y = y; this.z = z;
        }
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
