package mcheli.integration.wgmap.client;

import com.wdg.wgmap.integration.mchr.WaypointGpsAccess;
import com.wdg.wgmap.waypoint.model.Waypoint;
import com.wdg.wgmap.waypoint.model.WaypointAuthority;
import mcheli.integration.wgmap.WGMapGpsTarget;
import mcheli.weapon.MCH_GPSPosition;
import net.minecraft.entity.player.EntityPlayer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Loaded by client input only when WGMap is installed. */
public final class WGMapGpsClientBridge {
    private WGMapGpsClientBridge() { }

    public static WGMapGpsTarget select(EntityPlayer player, double originX, double originZ) {
        WGMapGpsTarget target = peek(player, originX, originZ);
        if (target != null) MCH_GPSPosition.clientSet(target.x, target.y, target.z, true, player);
        return target;
    }

    /** Read-only half of the firing selection; select() also updates MCHR's client GPS state. */
    public static WGMapGpsTarget peek(EntityPlayer player, double originX, double originZ) {
        if (player == null) return null;
        try {
            if (WaypointGpsAccess.apiVersion() != 1) return null;
            Waypoint waypoint = WaypointGpsAccess.nearestClientArmed(originX, originZ, player.dimension);
            if (waypoint == null) return null;
            return new WGMapGpsTarget(waypoint.getX(), waypoint.getY(), waypoint.getZ(),
                    waypoint.getAuthority() != WaypointAuthority.LOCAL);
        } catch (LinkageError incompatible) {
            return null;
        }
    }

    /** Radar presentation is not limited to the single armed firing target. */
    public static List<WGMapGpsTarget> radarPoints(EntityPlayer player, double x, double z, double maxRange) {
        if (player == null) return Collections.emptyList();
        try {
            if (WaypointGpsAccess.apiVersion() != 1) return Collections.emptyList();
            List<Waypoint> points = WaypointGpsAccess.clientRadarPoints(x, z, player.dimension, maxRange);
            ArrayList<WGMapGpsTarget> targets = new ArrayList<WGMapGpsTarget>(points.size());
            for (Waypoint point : points) targets.add(new WGMapGpsTarget(point.getX(), point.getY(), point.getZ(),
                    point.getAuthority() != WaypointAuthority.LOCAL));
            return targets;
        } catch (LinkageError incompatible) {
            return Collections.emptyList();
        }
    }
}
