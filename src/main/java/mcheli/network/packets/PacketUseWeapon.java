package mcheli.network.packets;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import mcheli.MCH_MOD;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.aircraft.MCH_EntitySeat;
import mcheli.integration.wgmap.WGMapGpsServerBridge;
import mcheli.integration.wgmap.WGMapGpsTarget;
import mcheli.network.PacketBase;
import mcheli.tank.MCH_EntityTank;
import mcheli.uav.MCH_EntityUavStation;
import mcheli.vehicle.MCH_EntityVehicle;
import mcheli.weapon.MCH_GPSPosition;
import mcheli.weapon.MCH_WeaponBomb;
import mcheli.weapon.MCH_WeaponInfo;
import mcheli.weapon.MCH_WeaponParam;
import mcheli.weapon.MCH_WeaponSet;
import mcheli.wrapper.W_EntityPlayer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.World;
import cpw.mods.fml.common.Loader;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ArrayBlockingQueue;

public class PacketUseWeapon extends PacketBase {

    /** Reserved negative option values; the five-field wire layout stays unchanged. */
    public static final int WGM_LOCAL_GPS = Integer.MIN_VALUE;
    public static final int WGM_SHARED_GPS = Integer.MIN_VALUE + 1;
    private static final int WGM_SELECT_LOCAL_GPS_BOMB = Integer.MIN_VALUE + 2;
    private static final int WGM_SELECT_SHARED_GPS_BOMB = Integer.MIN_VALUE + 3;
    private static final int WGM_CLEAR_GPS_BOMB = Integer.MIN_VALUE + 4;
    private static final ArrayBlockingQueue<WaypointRequest> WAYPOINT_REQUESTS = new ArrayBlockingQueue<>(128);
    private static final Map<EntityPlayerMP, SelectedGpsBombTarget> GPS_BOMB_TARGETS = new WeakHashMap<>();

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

    public static PacketUseWeapon selectGpsBombTarget(WGMapGpsTarget target) {
        if (target == null) {
            return new PacketUseWeapon(0, WGM_CLEAR_GPS_BOMB, 0.0D, 0.0D, 0.0D);
        }
        int option = target.shared ? WGM_SELECT_SHARED_GPS_BOMB : WGM_SELECT_LOCAL_GPS_BOMB;
        return new PacketUseWeapon(0, option, target.x, target.y, target.z);
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
        if (isWaypointShot() || isGpsBombSelection()) {
            if (player != null && (useWeaponOption2 != WGM_SHARED_GPS || Loader.isModLoaded("wgmap"))) {
                WAYPOINT_REQUESTS.offer(new WaypointRequest(this, player));
            }
            return;
        }
        handleServerOnWorldThread(player);
    }

    /** Process selection and fire intents in order on the server tick. */
    public static void drainWaypointShots() {
        for (int i = 0; i < 32; i++) {
            WaypointRequest next = WAYPOINT_REQUESTS.poll();
            if (next == null) break;
            EntityPlayerMP player = next.player.get();
            if (player == null || player.worldObj == null || player.worldObj.isRemote
                    || next.world.get() != player.worldObj
                    || !player.worldObj.playerEntities.contains(player)
                    || System.nanoTime() - next.receivedAt > 2_000_000_000L) continue;
            next.packet.handleServerOnWorldThread(player);
        }
    }

    private void handleServerOnWorldThread(EntityPlayerMP player) {
        if (player == null) return;
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
            boolean gpsWeapon = weaponInfo != null && weaponInfo.isGPSMissile;
            if (isGpsBombSelection()) {
                if (gpsWeapon) selectGpsBombTarget(player);
                return;
            }
            boolean waypointShot = isWaypointShot();
            if (waypointShot && !gpsWeapon) {
                sendWeaponState(ac, player, weaponId, currentWeapon);
                return;
            }
            boolean shared = useWeaponOption2 == WGM_SHARED_GPS;
            MCH_GPSPosition waypointPosition = waypointShot ? resolveWaypointTarget(player, shared) : null;
            if (MCH_WeaponBomb.requiresSelectedGpsTarget(weaponInfo)
                    && (!waypointShot || !matchesGpsBombSelection(player, waypointPosition, shared))) {
                W_EntityPlayer.addChatMessage(player, MCH_WeaponBomb.SELECT_GPS_TARGET_MESSAGE);
                sendWeaponState(ac, player, weaponId, currentWeapon);
                return;
            }
            if (waypointShot && waypointPosition == null) {
                sendWeaponState(ac, player, weaponId, currentWeapon); return;
            }
            if (gpsWeapon && useWeaponOption2 < 0 && !waypointShot) {
                int radarTargetId = -useWeaponOption2;
                if (radarTargetId <= 0 || !applyGpsRadarTarget(ac, player, radarTargetId)) {
                    sendWeaponState(ac, player, weaponId, currentWeapon);
                    return;
                }
            }
            MCH_WeaponParam param = new MCH_WeaponParam();
            param.entity = ac;
            param.user = player;
            // Packet coordinates are GPS intent only for the reserved waypoint options;
            // never trust them as the weapon's spawn origin.
            param.setPosAndRot(ac.posX, ac.posY, ac.posZ, 0.0F, 0.0F);
            param.option1 = useWeaponOption1;
            param.option2 = useWeaponOption2;
            if (waypointPosition != null) {
                param.gpsTarget = waypointPosition;
            } else if (gpsWeapon) {
                MCH_GPSPosition current = MCH_GPSPosition.get(player);
                if (MCH_GPSPosition.isUsableTarget(current)) {
                    param.gpsTarget = new MCH_GPSPosition(current.x, current.y, current.z);
                    param.gpsTarget.owner = player;
                    param.gpsTarget.isActive = true;
                }
            }
            boolean used = ac.useCurrentWeapon(param);
            if (used || currentWeapon == null || !currentWeapon.hasPendingServerUseFor(player)) {
                sendWeaponState(ac, player, weaponId, currentWeapon);
            }
        }
    }

    private static final class WaypointRequest {
        private final PacketUseWeapon packet;
        private final WeakReference<EntityPlayerMP> player;
        private final WeakReference<World> world;
        private final long receivedAt = System.nanoTime();
        private WaypointRequest(PacketUseWeapon packet, EntityPlayerMP player) {
            this.packet = packet;
            this.player = new WeakReference<EntityPlayerMP>(player);
            this.world = new WeakReference<World>(player.worldObj);
        }
    }

    private boolean isWaypointShot() {
        return useWeaponOption2 == WGM_LOCAL_GPS || useWeaponOption2 == WGM_SHARED_GPS;
    }

    private boolean isGpsBombSelection() {
        return useWeaponOption2 == WGM_SELECT_LOCAL_GPS_BOMB
            || useWeaponOption2 == WGM_SELECT_SHARED_GPS_BOMB || useWeaponOption2 == WGM_CLEAR_GPS_BOMB;
    }

    private void selectGpsBombTarget(EntityPlayerMP player) {
        boolean shared = useWeaponOption2 == WGM_SELECT_SHARED_GPS_BOMB;
        MCH_GPSPosition target = useWeaponOption2 == WGM_CLEAR_GPS_BOMB ? null : resolveWaypointTarget(player, shared);
        if (target == null) {
            GPS_BOMB_TARGETS.remove(player);
        } else {
            GPS_BOMB_TARGETS.put(player, new SelectedGpsBombTarget(player.worldObj, target, shared));
        }
    }

    private static boolean matchesGpsBombSelection(EntityPlayerMP player, MCH_GPSPosition target, boolean shared) {
        SelectedGpsBombTarget selected = GPS_BOMB_TARGETS.get(player);
        return selected != null && target != null && selected.world.get() == player.worldObj
            && shared == selected.shared && target.x == selected.x && target.y == selected.y && target.z == selected.z;
    }

    private static final class SelectedGpsBombTarget {
        private final WeakReference<World> world;
        private final double x, y, z;
        private final boolean shared;

        private SelectedGpsBombTarget(World world, MCH_GPSPosition target, boolean shared) {
            this.world = new WeakReference<World>(world);
            this.x = target.x;
            this.y = target.y;
            this.z = target.z;
            this.shared = shared;
        }
    }

    private MCH_GPSPosition resolveWaypointTarget(EntityPlayerMP player, boolean shared) {
        if (shared && !Loader.isModLoaded("wgmap")) return null;
        MCH_GPSPosition position = new MCH_GPSPosition(useWeaponPosX, useWeaponPosY, useWeaponPosZ);
        position.owner = player;
        position.isActive = true;
        if (!MCH_GPSPosition.isUsableTarget(position)) return null;
        if (shared) {
            try {
                if (!WGMapGpsServerBridge.matchesShared(player, position.x, position.y, position.z)) return null;
            } catch (LinkageError incompatible) { return null; }
        }
        // Local waypoints are private client data, like MCHR's existing manual GPS input.
        return position;
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
