package mcheli.render;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/** Optional client-only bridge to WGMap's stable terrain occlusion API. */
@SideOnly(Side.CLIENT)
final class MCH_WGMapOcclusion {

    private static final String MOD_ID = "wgmap";
    private static final String API_CLASS_NAME = "com.wdg.wgmap.client.occlusion.TerrainOcclusionApi";
    private static final int SUPPORTED_API_VERSION = 1;

    enum Result {
        CLEAR,
        BLOCKED,
        UNKNOWN
    }

    private static boolean bindingResolved;
    private static Binding binding;

    private MCH_WGMapOcclusion() {
    }

    static Result traceBounds(int dimension,
            double cameraX, double cameraY, double cameraZ,
            double minX, double minY, double minZ,
            double maxX, double maxY, double maxZ) {
        Binding current = getBinding();
        if (current == null) {
            return Result.UNKNOWN;
        }
        try {
            Object available = current.isAvailable.invoke(null);
            if (!Boolean.TRUE.equals(available)) {
                return Result.UNKNOWN;
            }
            Object result = current.traceBounds.invoke(null,
                Integer.valueOf(dimension),
                Double.valueOf(cameraX), Double.valueOf(cameraY), Double.valueOf(cameraZ),
                Double.valueOf(minX), Double.valueOf(minY), Double.valueOf(minZ),
                Double.valueOf(maxX), Double.valueOf(maxY), Double.valueOf(maxZ));
            return mapApiResult(result);
        } catch (IllegalAccessException e) {
            disableBinding();
        } catch (InvocationTargetException e) {
            disableBinding();
        } catch (RuntimeException e) {
            disableBinding();
        } catch (LinkageError e) {
            disableBinding();
        }
        return Result.UNKNOWN;
    }

    /** UNKNOWN deliberately preserves the pre-WGMap renderer behavior. */
    static boolean shouldSuppress(Result result) {
        return result == Result.BLOCKED;
    }

    static Result mapApiResultName(String name) {
        if ("CLEAR".equals(name)) return Result.CLEAR;
        if ("BLOCKED".equals(name)) return Result.BLOCKED;
        return Result.UNKNOWN;
    }

    private static Result mapApiResult(Object result) {
        return result instanceof Enum<?>
            ? mapApiResultName(((Enum<?>)result).name())
            : Result.UNKNOWN;
    }

    private static Binding getBinding() {
        if (bindingResolved) {
            return binding;
        }
        bindingResolved = true;
        if (!Loader.isModLoaded(MOD_ID)) {
            return null;
        }
        try {
            Class<?> api = Class.forName(API_CLASS_NAME, false, MCH_WGMapOcclusion.class.getClassLoader());
            Field versionField = api.getField("API_VERSION");
            if (versionField.getInt(null) != SUPPORTED_API_VERSION) {
                return null;
            }
            Method isAvailable = api.getMethod("isAvailable");
            Method traceBounds = api.getMethod("traceBounds",
                Integer.TYPE,
                Double.TYPE, Double.TYPE, Double.TYPE,
                Double.TYPE, Double.TYPE, Double.TYPE,
                Double.TYPE, Double.TYPE, Double.TYPE);
            binding = new Binding(isAvailable, traceBounds);
        } catch (ClassNotFoundException e) {
            binding = null;
        } catch (NoSuchFieldException e) {
            binding = null;
        } catch (NoSuchMethodException e) {
            binding = null;
        } catch (IllegalAccessException e) {
            binding = null;
        } catch (RuntimeException e) {
            binding = null;
        } catch (LinkageError e) {
            binding = null;
        }
        return binding;
    }

    private static void disableBinding() {
        bindingResolved = true;
        binding = null;
    }

    private static final class Binding {
        final Method isAvailable;
        final Method traceBounds;

        Binding(Method isAvailable, Method traceBounds) {
            this.isAvailable = isAvailable;
            this.traceBounds = traceBounds;
        }
    }
}
