package mcheli.render;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mcheli.MCH_Lib;

import java.lang.reflect.Method;

/** Optional, versioned client-only bridge; WGMap is never a compile or classloading dependency. */
@SideOnly(Side.CLIENT)
final class MCH_WGMapDepth {
    private static final String API = "com.wdg.wgmap.client.occlusion.TerrainDepthApi";
    private static boolean resolved;
    private static Method available, prepare, draw, viewDistance;

    private MCH_WGMapDepth() { }

    static void setViewDistance(double blocks) {
        if (!bind() || viewDistance == null) return;
        try { viewDistance.invoke(null, Double.valueOf(blocks)); }
        catch (ReflectiveOperationException | RuntimeException | LinkageError failed) {
            viewDistance = null; // An optional hint must not disable existing depth/CPU fallback.
        }
    }

    static boolean isAvailable() {
        if (!bind()) return false;
        try {
            return Boolean.TRUE.equals(available.invoke(null));
        } catch (ReflectiveOperationException | RuntimeException | LinkageError failed) {
            MCH_Lib.Log("[FarVehicle] WGMap GPU depth query failed: %s", failed.toString());
            disable(); return false;
        }
    }

    static boolean[] prepare(int dimension, double cameraX, double cameraY, double cameraZ,
            double transitionStart, double[] targets, double[] radii, int count) {
        if (!isAvailable()) return null;
        try {
            Object result = prepare.invoke(null, Integer.valueOf(dimension),
                    Double.valueOf(cameraX), Double.valueOf(cameraY), Double.valueOf(cameraZ),
                    Double.valueOf(transitionStart), targets, radii, Integer.valueOf(count));
            return result instanceof boolean[] && ((boolean[])result).length == count
                    ? (boolean[])result : null;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError failed) {
            MCH_Lib.Log("[FarVehicle] WGMap GPU depth prepare failed: %s", failed.toString());
            disable(); return null;
        }
    }

    static boolean draw() {
        if (!bind()) return false;
        try {
            return Boolean.TRUE.equals(draw.invoke(null));
        } catch (ReflectiveOperationException | RuntimeException | LinkageError failed) {
            MCH_Lib.Log("[FarVehicle] WGMap GPU depth draw failed: %s", failed.toString());
            disable(); return false;
        }
    }

    private static boolean bind() {
        if (resolved) return available != null;
        resolved = true;
        if (!Loader.isModLoaded("wgmap")) return false;
        try {
            Class<?> api = Class.forName(API, false, MCH_WGMapDepth.class.getClassLoader());
            int apiVersion = api.getField("API_VERSION").getInt(null);
            int projectionVersion = api.getField("PROJECTION_VERSION").getInt(null);
            if (apiVersion != 1 || projectionVersion != MCH_CompressedDepthProjection.VERSION) {
                MCH_Lib.Log("[FarVehicle] WGMap GPU depth disabled: API=%d projection=%d; expected API=1 projection=%d. Update both client mods together.",
                        Integer.valueOf(apiVersion), Integer.valueOf(projectionVersion),
                        Integer.valueOf(MCH_CompressedDepthProjection.VERSION));
                return false;
            }
            available = api.getMethod("isAvailable");
            if (api.getField("GAP_COVERAGE_VERSION").getInt(null) != 1) { disable(); return false; }
            prepare = api.getMethod("prepareDepthIncludingUnloadedChunks", Integer.TYPE, Double.TYPE, Double.TYPE,
                    Double.TYPE, Double.TYPE, double[].class, double[].class, Integer.TYPE);
            if (api.getField("DRAW_STATUS_VERSION").getInt(null) != 1) { disable(); return false; }
            draw = api.getMethod("drawPreparedDepthChecked");
            try {
                if (api.getField("VIEW_DISTANCE_VERSION").getInt(null) == 1)
                    viewDistance = api.getMethod("setViewDistance", Double.TYPE);
            } catch (NoSuchFieldException | NoSuchMethodException legacy) {
                viewDistance = null;
            }
            return true;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError failed) {
            MCH_Lib.Log("[FarVehicle] WGMap GPU depth API unavailable: %s. Update both client mods together.",
                    failed.toString());
            disable(); return false;
        }
    }

    private static void disable() {
        resolved = true;
        available = null; prepare = null; draw = null; viewDistance = null;
    }
}
