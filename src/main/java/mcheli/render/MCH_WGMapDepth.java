package mcheli.render;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

import java.lang.reflect.Method;

/** Optional, versioned client-only bridge; WGMap is never a compile or classloading dependency. */
@SideOnly(Side.CLIENT)
final class MCH_WGMapDepth {
    private static final String API = "com.wdg.wgmap.client.occlusion.TerrainDepthApi";
    private static boolean resolved;
    private static Method available, prepare, draw;

    private MCH_WGMapDepth() { }

    static boolean isAvailable() {
        if (!bind()) return false;
        try {
            return Boolean.TRUE.equals(available.invoke(null));
        } catch (ReflectiveOperationException | RuntimeException | LinkageError failed) {
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
            disable(); return null;
        }
    }

    static boolean draw() {
        if (!bind()) return false;
        try {
            draw.invoke(null);
            return true;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError failed) {
            disable(); return false;
        }
    }

    private static boolean bind() {
        if (resolved) return available != null;
        resolved = true;
        if (!Loader.isModLoaded("wgmap")) return false;
        try {
            Class<?> api = Class.forName(API, false, MCH_WGMapDepth.class.getClassLoader());
            if (api.getField("API_VERSION").getInt(null) != 1
                    || api.getField("PROJECTION_VERSION").getInt(null) != MCH_CompressedDepthProjection.VERSION) return false;
            available = api.getMethod("isAvailable");
            prepare = api.getMethod("prepareDepth", Integer.TYPE, Double.TYPE, Double.TYPE,
                    Double.TYPE, Double.TYPE, double[].class, double[].class, Integer.TYPE);
            draw = api.getMethod("drawPreparedDepth");
            return true;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError failed) {
            disable(); return false;
        }
    }

    private static void disable() {
        resolved = true;
        available = null; prepare = null; draw = null;
    }
}
