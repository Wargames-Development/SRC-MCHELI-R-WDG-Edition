package mcheli.integration.dh;

import cpw.mods.fml.common.Loader;
import mcheli.MCH_EntityInfoManager;
import mcheli.MCH_Lib;
import mcheli.MCH_RadarTerrain;
import net.minecraft.world.World;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/** Optional binding to DH's versioned server radar API. */
public final class DhRadarTerrainBridge {
    private static final boolean DISABLED = Boolean.getBoolean("mcheli.disableDhRadarTerrain");
    private static final double MAX_DISTANCE = MCH_EntityInfoManager.ENTITY_INFO_SYNC_RANGE + 2.0D;

    private DhRadarTerrainBridge() { }

    public static MCH_RadarTerrain.Terrain open(World world, double originX, double originY, double originZ) {
        if (DISABLED || !Loader.isModLoaded("distanthorizons")) return null;
        MethodHandle sample = Binding.SAMPLE;
        if (sample == null) return null;
        return (x, y, z) -> {
            double dx = x + 0.5D - originX;
            double dy = y + 0.5D - originY;
            double dz = z + 0.5D - originZ;
            return dx * dx + dy * dy + dz * dz <= MAX_DISTANCE * MAX_DISTANCE
                    ? sample(sample, world, x, y, z) : 2;
        };
    }

    private static int sample(MethodHandle sample, World world, int x, int y, int z) {
        try {
            return (boolean) sample.invokeExact(world, x, y, z) ? 1 : 2;
        } catch (RuntimeException | LinkageError unavailable) {
            return 2;
        } catch (Error fatal) {
            throw fatal;
        } catch (Throwable unavailable) {
            return 2;
        }
    }

    private static final class Binding {
        private static final MethodHandle SAMPLE = bind();

        private static MethodHandle bind() {
            try {
                Class<?> api = Class.forName("com.seibel.distanthorizons.forge17.terrain.DhRadarTerrainApi",
                        false, DhRadarTerrainBridge.class.getClassLoader());
                if (api.getField("API_VERSION").getInt(null) == 1) {
                    MethodHandle sample = MethodHandles.publicLookup().findStatic(api, "isOpaque",
                            MethodType.methodType(boolean.class, World.class, int.class, int.class, int.class));
                    MCH_Lib.Log("[Radar] Using Distant Horizons stored terrain for server occlusion.");
                    return sample;
                }
            } catch (ReflectiveOperationException | LinkageError unavailable) {
                // Supported older installations may lack the radar extension.
            }
            MCH_Lib.Log("[Radar] Distant Horizons server radar API unavailable. Using the existing terrain provider.");
            return null;
        }
    }
}
