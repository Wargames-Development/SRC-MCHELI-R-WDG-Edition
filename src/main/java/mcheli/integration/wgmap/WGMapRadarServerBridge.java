package mcheli.integration.wgmap;

import com.wdg.wgmap.integration.mchr.RadarTerrainAccess;
import mcheli.MCH_RadarTerrain;
import net.minecraft.world.World;

/** Loaded only after the WGMap mod-id guard. Older/missing APIs retain live-terrain radar. */
public final class WGMapRadarServerBridge {
    private WGMapRadarServerBridge() { }

    public static MCH_RadarTerrain.Terrain open(World world, double x, double y, double z) {
        try {
            if (RadarTerrainAccess.apiVersion() != 1) return null;
            RadarTerrainAccess.Query query = RadarTerrainAccess.openServer(world, x, y, z);
            return query == null ? null : query::stateAt;
        } catch (LinkageError incompatible) {
            return null;
        }
    }
}
