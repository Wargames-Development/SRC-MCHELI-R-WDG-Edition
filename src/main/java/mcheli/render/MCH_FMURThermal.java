package mcheli.render;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.lang.reflect.Method;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import mcheli.MCH_Camera;
import mcheli.MCH_Config;
import mcheli.MCH_Lib;
import net.minecraft.client.Minecraft;

/** Optional public FMUR thermal API; no common/server or required mod dependency. */
@SideOnly(Side.CLIENT)
public final class MCH_FMURThermal {
    private static Method supported, heatPass, particlePass;
    private MCH_FMURThermal() { }

    public static void register(Consumer<Float> renderer) {
        if (!Loader.isModLoaded("flansmod")) return;
        try {
            Class<?> api = Class.forName("com.flansmod.client.ThermalRenderApi", false, MCH_FMURThermal.class.getClassLoader());
            if (api.getField("API_VERSION").getInt(null) != 1) return;
            supported = api.getMethod("isSupported");
            heatPass = api.getMethod("isHeatPass");
            try { particlePass = api.getMethod("isParticlePass"); }
            catch (NoSuchMethodException olderApi) { particlePass = null; }
            api.getMethod("registerVehicleRenderer", BooleanSupplier.class, Consumer.class).invoke(null,
                    new BooleanSupplier() {
                        @Override public boolean getAsBoolean() {
                            Minecraft mc = Minecraft.getMinecraft();
                            return mc.theWorld != null && mc.thePlayer != null && !MCH_Config.DisableShader.prmBool
                                    && MCH_Camera.currentCameraMode == MCH_Camera.MODE_THERMALVISION;
                        }
                    }, renderer);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
            disable(failure);
        }
    }

    public static boolean isAvailable() { return invoke(supported); }
    public static boolean isHeatPass() { return invoke(heatPass); }
    public static boolean isParticlePass() { return invoke(particlePass); }
    private static boolean invoke(Method method) {
        if (method == null) return false;
        try { return Boolean.TRUE.equals(method.invoke(null)); }
        catch (ReflectiveOperationException | RuntimeException | LinkageError failure) { disable(failure); return false; }
    }
    private static void disable(Throwable failure) {
        supported = null; heatPass = null; particlePass = null;
        MCH_Lib.Log("[Thermal] shared FMUR renderer unavailable: %s", failure.toString());
    }
}