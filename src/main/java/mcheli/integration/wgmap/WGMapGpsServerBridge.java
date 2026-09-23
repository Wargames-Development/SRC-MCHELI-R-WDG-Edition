package mcheli.integration.wgmap;

import com.wdg.wgmap.integration.mchr.WaypointGpsAccess;
import net.minecraft.entity.player.EntityPlayerMP;

/** Loaded by the weapon packet only when WGMap is installed. */
public final class WGMapGpsServerBridge {
    private WGMapGpsServerBridge() { }

    public static boolean matchesShared(EntityPlayerMP player, double x, double y, double z) {
        try {
            return WaypointGpsAccess.apiVersion() == 1
                    && WaypointGpsAccess.matchesVisibleShared(player, x, y, z);
        } catch (LinkageError incompatible) {
            return false;
        }
    }
}
