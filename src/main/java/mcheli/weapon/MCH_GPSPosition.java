package mcheli.weapon;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mcheli.MCH_MOD;
import mcheli.network.packets.PacketGPSPositionReset;
import net.minecraft.entity.Entity;

import java.util.HashMap;
import java.util.Map;

public class MCH_GPSPosition {

    public static Map<Integer, MCH_GPSPosition> currentGPSPositions = new HashMap<>();

    public static MCH_GPSPosition currentClientGPSPosition = new MCH_GPSPosition(0, 0, 0);

    /** Half-second waypoint creation cooldown at 20 TPS. */
    public static final long CLIENT_WAYPOINT_COOLDOWN_TICKS = 10L;

    private static int lastClientWaypointOwnerId = Integer.MIN_VALUE;
    private static int lastClientWaypointDimension = Integer.MIN_VALUE;
    private static long lastClientWaypointTick = Long.MIN_VALUE;

    public double x, y, z;
    public Entity owner;
    public boolean isActive = false;

    public MCH_GPSPosition(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /**
     * Reserves the next client GPS waypoint update. Repeated right-click lock
     * calls are ignored until ten client ticks (0.5 seconds) have elapsed.
     *
     * Call this before ray tracing, reading JourneyMap files, playing the mark
     * sound, or sending the GPS packet so the expensive work is throttled too.
     */
    @SideOnly(Side.CLIENT)
    public static boolean tryBeginClientWaypointUpdate(Entity owner) {
        if (owner == null || owner.worldObj == null || !owner.worldObj.isRemote) {
            return false;
        }

        int ownerId = owner.getEntityId();
        int dimension = owner.worldObj.provider.dimensionId;
        long now = owner.worldObj.getTotalWorldTime();
        boolean sameContext = ownerId == lastClientWaypointOwnerId
            && dimension == lastClientWaypointDimension;

        if (sameContext
            && lastClientWaypointTick != Long.MIN_VALUE
            && now >= lastClientWaypointTick
            && now - lastClientWaypointTick < CLIENT_WAYPOINT_COOLDOWN_TICKS) {
            return false;
        }

        lastClientWaypointOwnerId = ownerId;
        lastClientWaypointDimension = dimension;
        lastClientWaypointTick = now;
        return true;
    }

    public static void set(double x, double y, double z, boolean isActive, Entity owner) {
        if (owner != null && owner.worldObj != null && owner.worldObj.isRemote) {
            clientSet(x, y, z, isActive, owner);
            MCH_MOD.getPacketHandler().sendToServer(new PacketGPSPositionReset(x, y, z, isActive, owner.getEntityId()));
        }
    }

    public static MCH_GPSPosition get(Entity owner) {
        return owner != null ? currentGPSPositions.get(owner.getEntityId()) : null;
    }

    public static boolean isUsableTarget(MCH_GPSPosition position) {
        return position != null
            && position.isActive
            && isFinite(position.x)
            && isFinite(position.y)
            && isFinite(position.z)
            && Math.abs(position.x) <= 30000000.0D
            && Math.abs(position.z) <= 30000000.0D
            && position.y >= -64.0D
            && position.y <= 4096.0D;
    }

    public static boolean isFinite(double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value);
    }

    @SideOnly(Side.CLIENT)
    public static void clientSet(double x, double y, double z, boolean isActive, Entity owner) {
        currentClientGPSPosition.x = x;
        currentClientGPSPosition.y = y;
        currentClientGPSPosition.z = z;
        currentClientGPSPosition.isActive = isActive;
        currentClientGPSPosition.owner = owner;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }
}
