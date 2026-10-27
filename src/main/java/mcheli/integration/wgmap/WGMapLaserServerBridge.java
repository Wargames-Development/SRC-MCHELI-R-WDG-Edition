package mcheli.integration.wgmap;

import com.wdg.wgmap.integration.mchr.LaserTerrainAccess;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

/** Loaded only after WGMap's mod-id check. No client classes or private WGMap access. */
public final class WGMapLaserServerBridge {
    private WGMapLaserServerBridge() { }

    /** The exact end object means known clear; null means unavailable or untrusted. */
    public static Vec3 trace(World world, Vec3 start, Vec3 end) {
        try {
            if (LaserTerrainAccess.apiVersion() != 1) return null;
            double[] hit = new double[3];
            LaserTerrainAccess.Result result = LaserTerrainAccess.traceServer(world,
                    start.xCoord, start.yCoord, start.zCoord, end.xCoord, end.yCoord, end.zCoord, hit);
            if (result == LaserTerrainAccess.Result.CLEAR) return end;
            if (result == LaserTerrainAccess.Result.BLOCKED && Double.isFinite(hit[0])
                    && Double.isFinite(hit[1]) && Double.isFinite(hit[2]))
                return Vec3.createVectorHelper(hit[0], hit[1], hit[2]);
        } catch (LinkageError incompatible) { }
        return null;
    }
}
