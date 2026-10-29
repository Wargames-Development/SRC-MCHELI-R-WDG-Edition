package mcheli.render;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.lang.reflect.Method;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import java.util.Map;
import java.util.Collections;
import mcheli.aircraft.MCH_AircraftInfo;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.MCH_Camera;
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
                            if (mc.theWorld == null || mc.thePlayer == null || mc.gameSettings.thirdPersonView != 0
                                    || MCH_Camera.currentCameraMode != MCH_Camera.MODE_THERMALVISION) return false;
                            MCH_EntityAircraft aircraft = currentAircraft();
                            return aircraft != null ? aircraft.getCameraMode(mc.thePlayer) == MCH_Camera.MODE_THERMALVISION
                                    : mc.thePlayer.ridingEntity instanceof mcheli.gltd.MCH_EntityGLTD;
                        }
                    }, renderer);
            try {
                api.getMethod("registerVehicleSensor", Supplier.class, IntSupplier.class).invoke(null,
                        new Supplier<Object>() {
                            @Override public Object get() {
                                MCH_EntityAircraft aircraft = currentAircraft();
                                if (aircraft == null || aircraft.getAcInfo() == null) return MCH_FMURThermal.class;
                                return aircraft.getAcInfo().getThermalProfile(aircraft.getCameraPosInfo(),
                                        aircraft.getSeatIdByEntity(Minecraft.getMinecraft().thePlayer));
                            }
                        }, new IntSupplier() {
                            @Override public int getAsInt() {
                                MCH_EntityAircraft aircraft = currentAircraft();
                                MCH_AircraftInfo info = aircraft == null ? null : aircraft.getAcInfo();
                                return info == null ? 30 : info.getThermalMaxFps(aircraft.getCameraPosInfo());
                            }
                        });
            } catch (NoSuchMethodException olderApi) {
                // Existing FMUR v1 builds still supply the heat renderer.
            }
            try {
                api.getMethod("registerVehicleProfile", Supplier.class).invoke(null, new Supplier<Map<String, String>>() {
                    @Override public Map<String, String> get() {
                        MCH_EntityAircraft aircraft = currentAircraft();
                        if (aircraft == null || aircraft.getAcInfo() == null) return Collections.emptyMap();
                        return aircraft.getAcInfo().getThermalProfile(aircraft.getCameraPosInfo(),
                                aircraft.getSeatIdByEntity(Minecraft.getMinecraft().thePlayer));
                    }
                });
            } catch (NoSuchMethodException olderApi) { }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
            disable(failure);
        }
    }

    private static MCH_EntityAircraft currentAircraft() {
        return MCH_EntityAircraft.getAircraft_RiddenOrControl(Minecraft.getMinecraft().thePlayer);
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
