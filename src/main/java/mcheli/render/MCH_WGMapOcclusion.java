package mcheli.render;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mcheli.aircraft.MCH_AircraftInfo;
import mcheli.aircraft.MCH_EntityAircraft;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/** Optional client-only bridge to WGMap's stable terrain occlusion API. */
@SideOnly(Side.CLIENT)
public final class MCH_WGMapOcclusion {

    private static final String MOD_ID = "wgmap";
    private static final String API_CLASS_NAME = "com.wdg.wgmap.client.occlusion.TerrainOcclusionApi";
    private static final int MAX_API_VERSION = 2;

    enum Result {
        CLEAR,
        BLOCKED,
        MISSING_DATA,
        UNCERTAIN_BLOCK,
        UNKNOWN,
        UNAVAILABLE
    }

    private static boolean bindingResolved;
    private static Binding binding;

    private MCH_WGMapOcclusion() {
    }

    /** Shared normal-render entry point using the aircraft's interpolated world pose. */
    public static boolean shouldSuppressAircraft(MCH_EntityAircraft aircraft, float partialTicks) {
        Minecraft mc = Minecraft.getMinecraft();
        if (aircraft == null || mc.theWorld == null || aircraft.worldObj != mc.theWorld) {
            return false;
        }
        if (mc.renderViewEntity != null && mc.gameSettings != null) {
            double dx = aircraft.posX - mc.renderViewEntity.posX;
            double dz = aircraft.posZ - mc.renderViewEntity.posZ;
            double nearRange = Math.max(16.0D, (mc.gameSettings.renderDistanceChunks - 1) * 16.0D);
            if (Math.max(Math.abs(dx), Math.abs(dz)) <= nearRange) return false;
        }
        return shouldSuppressConfirmedBounds(mc, aircraft.getAcInfo(),
            aircraft.lastTickPosX + (aircraft.posX - aircraft.lastTickPosX) * partialTicks,
            aircraft.lastTickPosY + (aircraft.posY - aircraft.lastTickPosY) * partialTicks,
            aircraft.lastTickPosZ + (aircraft.posZ - aircraft.lastTickPosZ) * partialTicks,
            partialTicks);
    }

    static boolean shouldSuppressBounds(Minecraft mc, MCH_AircraftInfo info,
            double targetX, double targetY, double targetZ, float partialTicks) {
        return shouldSuppress(traceTargetBounds(mc, info, targetX, targetY, targetZ, partialTicks));
    }

    static boolean shouldSuppressConfirmedBounds(Minecraft mc, MCH_AircraftInfo info,
            double targetX, double targetY, double targetZ, float partialTicks) {
        // Terrain outside WGMap's shadow coverage is unknown, not confirmed occlusion.
        return shouldSuppress(traceTargetBounds(mc, info, targetX, targetY, targetZ, partialTicks));
    }

    /** Four physical body samples. Two clear rays expose meaningful geometry; three blocked rays
     * hide the model. Ambiguous combinations retain the previous decision briefly in the renderer. */
    static Result traceProjectedSamples(Minecraft mc, MCH_AircraftInfo info,
            double targetX, double targetY, double targetZ, float partialTicks) {
        if (mc == null || mc.theWorld == null || mc.theWorld.provider == null
                || mc.renderViewEntity == null || info == null) {
            return Result.UNAVAILABLE;
        }
        Entity viewer = mc.renderViewEntity;
        double cameraX = viewer.lastTickPosX + (viewer.posX - viewer.lastTickPosX) * partialTicks;
        double cameraY = viewer.lastTickPosY + (viewer.posY - viewer.lastTickPosY) * partialTicks;
        double cameraZ = viewer.lastTickPosZ + (viewer.posZ - viewer.lastTickPosZ) * partialTicks;
        if (viewer instanceof EntityLivingBase) {
            cameraY += ((EntityLivingBase)viewer).getEyeHeight();
        }

        double bodyHeight = Math.max(0.5D, (double)info.bodyHeight);
        double width = Math.max(0.5D, info.bodyWidth * 0.35D);
        double horizontal = Math.hypot(targetX - cameraX, targetZ - cameraZ);
        double sideX = horizontal > 0.001D ? -(targetZ - cameraZ) / horizontal * width : width;
        double sideZ = horizontal > 0.001D ? (targetX - cameraX) / horizontal * width : 0.0D;
        int clear = 0, blocked = 0;
        boolean missing = false, uncertain = false, unknown = false;
        int dimension = mc.theWorld.provider.dimensionId;
        for (int sample = 0; sample < 4; sample++) {
            double sx = targetX + (sample == 2 ? sideX : sample == 3 ? -sideX : 0.0D);
            double sy = targetY + bodyHeight * (sample == 0 ? 0.45D : 0.85D);
            double sz = targetZ + (sample == 2 ? sideZ : sample == 3 ? -sideZ : 0.0D);
            Result result = traceSegment(dimension, cameraX, cameraY, cameraZ, sx, sy, sz);
            if (result == Result.UNAVAILABLE) return Result.UNAVAILABLE;
            if (result == Result.CLEAR) clear++;
            else if (result == Result.BLOCKED) blocked++;
            else if (result == Result.MISSING_DATA) missing = true;
            else if (result == Result.UNCERTAIN_BLOCK) uncertain = true;
            else unknown = true;
            if (clear >= 2) return Result.CLEAR;
            if (blocked >= 3) return Result.BLOCKED;
        }
        return missing ? Result.MISSING_DATA : uncertain ? Result.UNCERTAIN_BLOCK
                : unknown ? Result.UNKNOWN : blocked > clear ? Result.BLOCKED : Result.CLEAR;
    }

    static long terrainGeneration() {
        Binding current = getBinding();
        if (current == null || current.generation == null) return 0L;
        try {
            return ((Long)current.generation.invoke(null)).longValue();
        } catch (ReflectiveOperationException | RuntimeException | LinkageError failed) {
            disableBinding();
            return 0L;
        }
    }

    static Result traceTargetBounds(Minecraft mc, MCH_AircraftInfo info,
            double targetX, double targetY, double targetZ, float partialTicks) {
        if (mc == null || mc.theWorld == null || mc.theWorld.provider == null
                || mc.renderViewEntity == null || info == null) {
            return Result.UNAVAILABLE;
        }
        Entity viewer = mc.renderViewEntity;
        double cameraX = viewer.lastTickPosX + (viewer.posX - viewer.lastTickPosX) * partialTicks;
        double cameraY = viewer.lastTickPosY + (viewer.posY - viewer.lastTickPosY) * partialTicks;
        double cameraZ = viewer.lastTickPosZ + (viewer.posZ - viewer.lastTickPosZ) * partialTicks;
        if (viewer instanceof EntityLivingBase) {
            cameraY += ((EntityLivingBase)viewer).getEyeHeight();
        }

        double halfHorizontal = Math.max(0.5D, Math.max(info.bodyWidth * 0.5D, info.markerWidth));
        halfHorizontal = Math.max(halfHorizontal, Math.abs((double)info.bbZmin));
        halfHorizontal = Math.max(halfHorizontal, Math.abs((double)info.bbZmax));
        double height = Math.max(0.5D, Math.max(info.bodyHeight, info.markerHeight));
        return traceBounds(mc.theWorld.provider.dimensionId,
            cameraX, cameraY, cameraZ,
            targetX - halfHorizontal, targetY, targetZ - halfHorizontal,
            targetX + halfHorizontal, targetY + height, targetZ + halfHorizontal);
    }

    static Result traceSegment(int dimension,
            double cameraX, double cameraY, double cameraZ,
            double targetX, double targetY, double targetZ) {
        Binding current = getBinding();
        if (current == null) {
            return Result.UNAVAILABLE;
        }
        try {
            Object available = current.isAvailable.invoke(null);
            if (!Boolean.TRUE.equals(available)) {
                return Result.UNAVAILABLE;
            }
            Object result = (current.detailedSegment != null ? current.detailedSegment : current.traceSegment).invoke(null,
                Integer.valueOf(dimension),
                Double.valueOf(cameraX), Double.valueOf(cameraY), Double.valueOf(cameraZ),
                Double.valueOf(targetX), Double.valueOf(targetY), Double.valueOf(targetZ));
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
        return Result.UNAVAILABLE;
    }

    static Result traceBounds(int dimension,
            double cameraX, double cameraY, double cameraZ,
            double minX, double minY, double minZ,
            double maxX, double maxY, double maxZ) {
        Binding current = getBinding();
        if (current == null) {
            return Result.UNAVAILABLE;
        }
        try {
            Object available = current.isAvailable.invoke(null);
            if (!Boolean.TRUE.equals(available)) {
                return Result.UNAVAILABLE;
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
        return Result.UNAVAILABLE;
    }

    /** Only confirmed obstruction can establish a hidden state without prior evidence. */
    static boolean shouldSuppress(Result result) {
        return result == Result.BLOCKED;
    }

    static Result mapApiResultName(String name) {
        if ("CLEAR".equals(name)) return Result.CLEAR;
        if ("BLOCKED".equals(name)) return Result.BLOCKED;
        if ("MISSING_DATA".equals(name)) return Result.MISSING_DATA;
        if ("UNCERTAIN_BLOCK".equals(name)) return Result.UNCERTAIN_BLOCK;
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
            int apiVersion = versionField.getInt(null);
            if (apiVersion < 1 || apiVersion > MAX_API_VERSION) {
                return null;
            }
            Method isAvailable = api.getMethod("isAvailable");
            Method traceSegment = api.getMethod("traceSegment",
                Integer.TYPE,
                Double.TYPE, Double.TYPE, Double.TYPE,
                Double.TYPE, Double.TYPE, Double.TYPE);
            Method traceBounds = api.getMethod("traceBounds",
                Integer.TYPE,
                Double.TYPE, Double.TYPE, Double.TYPE,
                Double.TYPE, Double.TYPE, Double.TYPE,
                Double.TYPE, Double.TYPE, Double.TYPE);
            Method detailedSegment = null;
            Method generation = null;
            try {
                detailedSegment = api.getMethod("traceSegmentDetailed",
                    Integer.TYPE, Double.TYPE, Double.TYPE, Double.TYPE,
                    Double.TYPE, Double.TYPE, Double.TYPE);
                generation = api.getMethod("getTerrainGeneration");
            } catch (NoSuchMethodException legacyApi) {
                // WGMap v1 remains usable with its coarse UNKNOWN result and short retry.
            }
            binding = new Binding(isAvailable, traceSegment, traceBounds, detailedSegment, generation);
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
        final Method traceSegment;
        final Method traceBounds;
        final Method detailedSegment;
        final Method generation;

        Binding(Method isAvailable, Method traceSegment, Method traceBounds,
                Method detailedSegment, Method generation) {
            this.isAvailable = isAvailable;
            this.traceSegment = traceSegment;
            this.traceBounds = traceBounds;
            this.detailedSegment = detailedSegment;
            this.generation = generation;
        }
    }
}
