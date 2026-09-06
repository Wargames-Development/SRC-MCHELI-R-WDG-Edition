package mcheli.network.packets;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import mcheli.MCH_MOD;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.aircraft.MCH_EntitySeat;
import mcheli.network.PacketBase;
import mcheli.tank.MCH_EntityTank;
import mcheli.uav.MCH_EntityUavStation;
import mcheli.vehicle.MCH_EntityVehicle;
import mcheli.weapon.MCH_GPSPosition;
import mcheli.weapon.MCH_WeaponInfo;
import mcheli.weapon.MCH_WeaponParam;
import mcheli.weapon.MCH_WeaponSet;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

public class PacketUseWeapon extends PacketBase {

    private static final long AMMO_MASK = 0x7FFFL;
    private static final long HEAT_MASK = 0x1FFFFL;

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
            MCH_WeaponSet currentWeapon = ac.getCurrentWeapon(player);
            int weaponId = ac.getCurrentWeaponID(player);
            MCH_WeaponInfo weaponInfo = currentWeapon != null ? currentWeapon.getInfo() : null;
            if (weaponInfo != null && weaponInfo.isGPSMissile && useWeaponOption2 < 0) {
                int radarTargetId = -useWeaponOption2;
                if (radarTargetId <= 0 || !applyGpsRadarTarget(ac, player, radarTargetId)) {
                    sendWeaponState(ac, player, weaponId, currentWeapon);
                    return;
                }
            }
            MCH_WeaponParam param = new MCH_WeaponParam();
            param.entity = ac;
            param.user = player;
            // Keep packet coordinates for protocol compatibility, but do not trust them as the spawn origin.
            param.setPosAndRot(ac.posX, ac.posY, ac.posZ, 0.0F, 0.0F);
            param.option1 = useWeaponOption1;
            param.option2 = useWeaponOption2;
            boolean used = ac.useCurrentWeapon(param);
            if (used || currentWeapon == null || !currentWeapon.hasPendingServerUseFor(player)) {
                sendWeaponState(ac, player, weaponId, currentWeapon);
            }
        }
    }

    public static void sendWeaponState(MCH_EntityAircraft ac, EntityPlayerMP player, int weaponId, MCH_WeaponSet weapon) {
        if (ac == null || player == null || weapon == null || weaponId < 0) {
            return;
        }
        // This packet already has a fixed request layout. Server-to-client instances reuse
        // the same fields as an authoritative response without changing the wire format.
        int packedWeaponState = (weaponId & 0xFFFF) | ((weapon.getCurrentWeaponIndex() & 0xFFFF) << 16);
        long packedAmmoState = ((long)weapon.getAmmoNum() & AMMO_MASK)
            | (((long)weapon.getRestAllAmmoNum() & AMMO_MASK) << 15)
            | (((long)weapon.currentHeat & HEAT_MASK) << 30);
        MCH_MOD.getPacketHandler().sendTo(new PacketUseWeapon(
            ac.getEntityId(), packedWeaponState,
            (double)packedAmmoState, weapon.countWait, weapon.countReloadWait
        ), player);
    }

    private boolean applyGpsRadarTarget(MCH_EntityAircraft ac, EntityPlayerMP player, int targetId) {
        if (ac.getAcInfo() == null || !ac.getAcInfo().enableRadar || !ac.isRadarEnabledRuntime()
            || !MCH_MOD.rwrThreatManager.isEmitterTrackingTarget(
                ac.getEntityId(), targetId, MCH_MOD.rwrThreatManager.getCurrentTick())) {
            return false;
        }

        Entity target = player.worldObj.getEntityByID(targetId);
        if (!(target instanceof MCH_EntityTank) && !(target instanceof MCH_EntityVehicle)) {
            return false;
        }
        MCH_EntityAircraft targetVehicle = (MCH_EntityAircraft)target;
        if (target == ac || target.isDead || targetVehicle.isDestroyed()
            || targetVehicle.isMountedSameTeamEntity(player)) {
            return false;
        }

        double maxRange = ac.getAcInfo().radarMaxTargetRange > 0.0F
            ? ac.getAcInfo().radarMaxTargetRange : 4096.0D;
        if (ac.getDistanceSqToEntity(target) > maxRange * maxRange) {
            return false;
        }

        MCH_GPSPosition position = new MCH_GPSPosition(target.posX, target.posY, target.posZ);
        position.isActive = true;
        position.owner = player;
        if (!MCH_GPSPosition.isUsableTarget(position)) {
            return false;
        }
        MCH_GPSPosition.currentGPSPositions.put(player.getEntityId(), position);
        return true;
    }

    @Override
    public void handleClientSide(EntityPlayer clientPlayer) {
        if (clientPlayer == null || clientPlayer.worldObj == null) {
            return;
        }
        Entity entity = clientPlayer.worldObj.getEntityByID(useWeaponOption1);
        if (entity instanceof MCH_EntityAircraft) {
            int weaponId = useWeaponOption2 & 0xFFFF;
            int weaponIndex = useWeaponOption2 >>> 16;
            long packedAmmoState = (long)useWeaponPosX;
            ((MCH_EntityAircraft)entity).applyServerWeaponUseState(
                weaponId,
                (int)(packedAmmoState & AMMO_MASK),
                (int)((packedAmmoState >>> 15) & AMMO_MASK),
                (int)((packedAmmoState >>> 30) & HEAT_MASK),
                weaponIndex,
                (int)useWeaponPosY,
                (int)useWeaponPosZ
            );
        }
    }
}
