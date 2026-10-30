package mcheli.render;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.lang.reflect.Method;
import mcheli.MCH_Lib;
import mcheli.weapon.MCH_EntityBaseBullet;
import net.minecraft.entity.Entity;

/** Optional client API, following the existing shared FMUR thermal-render bridge. */
@SideOnly(Side.CLIENT)
public final class MCH_FMURTracer {
    private static boolean attempted;
    private static Method render;

    private MCH_FMURTracer() { }

    public static boolean render(MCH_EntityBaseBullet bullet, float partialTicks) {
        if (!attempted) {
            attempted = true;
            try {
                Class<?> api = Class.forName("com.flansmod.client.TracerRenderApi", false, MCH_FMURTracer.class.getClassLoader());
                if (api.getField("API_VERSION").getInt(null) != 1) return false;
                render = api.getMethod("render", Entity.class, float.class, double.class, double.class,
                    double.class, float.class, boolean.class, boolean.class);
            } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
                disable(failure);
            }
        }
        if (render == null) return false;
        try {
            // MCHR stores capped motion separately from its effective per-tick displacement.
            return Boolean.TRUE.equals(render.invoke(null, bullet, partialTicks,
                bullet.motionX * bullet.accelerationFactor, bullet.motionY * bullet.accelerationFactor,
                bullet.motionZ * bullet.accelerationFactor, bullet.getInfo().fmurTracerScale, false, false));
        } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
            disable(failure);
            return false;
        }
    }

    private static void disable(Throwable failure) {
        render = null;
        MCH_Lib.Log("[Tracer] shared FMUR renderer unavailable; using native bullet models: %s", failure.toString());
    }
}
