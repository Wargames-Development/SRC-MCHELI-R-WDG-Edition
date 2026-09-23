package mcheli.integration.wgmap.client;

import com.wdg.wgmap.integration.mchr.WaypointGpsAccess;
import com.wdg.wgmap.waypoint.model.Waypoint;
import com.wdg.wgmap.waypoint.model.WaypointAuthority;
import mcheli.integration.wgmap.WGMapGpsTarget;
import mcheli.weapon.MCH_GPSPosition;
import net.minecraft.entity.player.EntityPlayer;

/** Loaded by client input only when WGMap is installed. */
public final class WGMapGpsClientBridge {
    private WGMapGpsClientBridge() { }

    public static WGMapGpsTarget select(EntityPlayer player, double originX, double originZ) {
        try {
            if (WaypointGpsAccess.apiVersion() != 1) return null;
            Waypoint waypoint = WaypointGpsAccess.nearestClientArmed(originX, originZ, player.dimension);
            if (waypoint == null) return null;
            MCH_GPSPosition.clientSet(waypoint.getX(), waypoint.getY(), waypoint.getZ(), true, player);
            return new WGMapGpsTarget(waypoint.getX(), waypoint.getY(), waypoint.getZ(),
                    waypoint.getAuthority() != WaypointAuthority.LOCAL);
        } catch (LinkageError incompatible) {
            return null;
        }
    }
}
