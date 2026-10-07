package mcheli.integration.wgmap.client;

import com.wdg.wgmap.integration.mchr.WaypointGpsAccess;
import com.wdg.wgmap.waypoint.model.Waypoint;
import com.wdg.wgmap.waypoint.model.WaypointAuthority;
import mcheli.integration.wgmap.WGMapGpsTarget;
import mcheli.weapon.MCH_GPSPosition;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/** Loaded by client input only when WGMap is installed. */
public final class WGMapGpsClientBridge {
    private static WeakReference<World> selectionWorld = new WeakReference<World>(null);
    private static int selectionOwnerId;
    private static UUID selectedId;
    private static boolean cycled;
    private static WGMapGpsTarget selectedTarget;
    private static long lastSelectionCheck = Long.MIN_VALUE;
    private WGMapGpsClientBridge() { }

    public static WGMapGpsTarget cycle(EntityPlayer player) {
        if (player == null || player.worldObj == null) return null;
        checkContext(player);
        List<Waypoint> points = gpsPoints(player);
        // UUID order stays stable as the aircraft moves and distances change.
        Collections.sort(points, (a, b) -> a.getId().compareTo(b.getId()));
        List<UUID> ids = new ArrayList<UUID>(points.size());
        for (Waypoint point : points) ids.add(point.getId());
        int next = nextIndex(ids, selectedId);
        cycled = true;
        selectedId = null;
        selectedTarget = null;
        for (int checked = 0; checked < points.size(); checked++) {
            Waypoint point = points.get((next + checked) % points.size());
            if (isGpsEnabled(point)) {
                selectedId = point.getId();
                selectedTarget = target(point);
                break;
            }
        }
        lastSelectionCheck = player.worldObj.getTotalWorldTime();
        applySelection(player);
        return selectedTarget;
    }

    static int nextIndex(List<UUID> ids, UUID selected) {
        return ids.isEmpty() ? -1 : (ids.indexOf(selected) + 1) % ids.size();
    }

    /** Called from client ticks, never the marker render loop. */
    public static void refreshSelection(EntityPlayer player) {
        if (player == null || player.worldObj == null) return;
        checkContext(player);
        if (!cycled) return;
        long now = player.worldObj.getTotalWorldTime();
        if (now >= lastSelectionCheck && now - lastSelectionCheck < 10L) return;
        lastSelectionCheck = now;
        selectedTarget = null;
        for (Waypoint point : gpsPoints(player)) {
            if (point.getId().equals(selectedId)) {
                if (isGpsEnabled(point)) selectedTarget = target(point);
                break;
            }
        }
        applySelection(player);
    }

    private static void checkContext(EntityPlayer player) {
        if (selectionWorld.get() != player.worldObj || selectionOwnerId != player.getEntityId()) {
            if (cycled) MCH_GPSPosition.clientSet(0, 0, 0, false, player);
            selectionWorld = new WeakReference<World>(player.worldObj);
            selectionOwnerId = player.getEntityId();
            selectedId = null;
            selectedTarget = null;
            cycled = false;
            lastSelectionCheck = Long.MIN_VALUE;
        }
    }

    private static List<Waypoint> gpsPoints(EntityPlayer player) {
        try {
            if (WaypointGpsAccess.apiVersion() == 1) {
                // Finite world-wide range; WGMap bounds this API to 128 enabled GPS points.
                return new ArrayList<Waypoint>(WaypointGpsAccess.clientRadarPoints(
                        player.posX, player.posZ, player.dimension, 100_000_000.0D));
            }
        } catch (LinkageError incompatible) { }
        return new ArrayList<Waypoint>();
    }

    private static WGMapGpsTarget target(Waypoint point) {
        return new WGMapGpsTarget(point.getX(), point.getY(), point.getZ(),
                point.getAuthority() != WaypointAuthority.LOCAL, point.getName());
    }

    private static boolean isGpsEnabled(Waypoint point) {
        if (!point.isEnabled() || !point.isGps()) return false;
        // The radar API includes GPS-off points. The armed API is the public
        // boundary for their separate GPS toggle; checking identity avoids
        // accepting a disabled point just because another target is nearby.
        Waypoint armed = WaypointGpsAccess.nearestClientArmed(
                point.getX(), point.getZ(), point.getDimensionId());
        return armed != null && point.getId().equals(armed.getId());
    }

    private static void applySelection(EntityPlayer player) {
        WGMapGpsTarget point = selectedTarget;
        MCH_GPSPosition.clientSet(point != null ? point.x : 0, point != null ? point.y : 0,
                point != null ? point.z : 0, point != null, player);
    }

    public static WGMapGpsTarget select(EntityPlayer player, double originX, double originZ) {
        WGMapGpsTarget target = peek(player, originX, originZ);
        if (target != null) MCH_GPSPosition.clientSet(target.x, target.y, target.z, true, player);
        return target;
    }

    /** Read-only half of the firing selection; select() also updates MCHR's client GPS state. */
    public static WGMapGpsTarget peek(EntityPlayer player, double originX, double originZ) {
        if (player == null || player.worldObj == null) return null;
        refreshSelection(player);
        if (cycled) return selectedTarget;
        try {
            if (WaypointGpsAccess.apiVersion() != 1) return null;
            Waypoint waypoint = WaypointGpsAccess.nearestClientArmed(originX, originZ, player.dimension);
            if (waypoint == null) return null;
            return target(waypoint);
        } catch (LinkageError incompatible) {
            return null;
        }
    }

    /** Radar presentation includes all eligible GPS points, not just the firing target. */
    public static List<WGMapGpsTarget> radarPoints(EntityPlayer player, double x, double z, double maxRange) {
        if (player == null) return Collections.emptyList();
        try {
            if (WaypointGpsAccess.apiVersion() != 1) return Collections.emptyList();
            List<Waypoint> points = WaypointGpsAccess.clientRadarPoints(x, z, player.dimension, maxRange);
            ArrayList<WGMapGpsTarget> targets = new ArrayList<WGMapGpsTarget>(points.size());
            for (Waypoint point : points) {
                if (isGpsEnabled(point)) targets.add(target(point));
            }
            return targets;
        } catch (LinkageError incompatible) {
            return Collections.emptyList();
        }
    }
}
