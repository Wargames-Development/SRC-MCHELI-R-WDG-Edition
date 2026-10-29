package mcheli.aircraft;

import mcheli.tank.MCH_TankInfo;
import net.minecraft.util.Vec3;
import org.junit.Test;

import static org.junit.Assert.*;

public class MCH_ThermalProfileTest {
    @Test
    public void vehicleDefaultsAndCameraOverridesAreIndependentOfFieldOrder() {
        MCH_TankInfo info = new MCH_TankInfo("sensor_test");
        MCH_AircraftInfo.CameraPosition first = info.new CameraPosition();
        MCH_AircraftInfo.CameraPosition second = info.new CameraPosition();
        info.cameraPosition.add(first);
        info.cameraPosition.add(second);
        assertEquals(30, info.getThermalMaxFps(first));
        info.loadItemData("CameraThermalMaxFPS", "1, 10");
        info.loadItemData("ThermalMaxFPS", "45");
        assertEquals(45, info.getThermalMaxFps(first));
        assertEquals(10, info.getThermalMaxFps(second));
        assertEquals(45, info.getThermalMaxFps(info.new CameraPosition(Vec3.createVectorHelper(0, 1, 0))));
        assertEquals(45, info.getThermalMaxFps(null));
    }

    @Test
    public void reloadRemovesOldDeviceLimits() {
        MCH_TankInfo info = new MCH_TankInfo("sensor_reload_test");
        info.loadItemData("ThermalMaxFPS", "15");
        info.loadItemData("CameraThermalMaxFPS", "0, 5");
        info.preReload();
        MCH_AircraftInfo.CameraPosition camera = info.new CameraPosition();
        info.cameraPosition.add(camera);
        assertEquals(30, info.getThermalMaxFps(camera));
    }

    @Test
    public void malformedRatesAndCameraEntriesAreRejected() {
        MCH_TankInfo info = new MCH_TankInfo("sensor_invalid_test");
        for (String value : new String[] {"0", "-1", "241", "NaN", "Infinity", "1.5", "bad"}) {
            reject(info, "ThermalMaxFPS", value);
            reject(info, "CameraThermalMaxFPS", "0," + value);
        }
        for (String value : new String[] {"-1,15", "bad,15", "0", "0,15,30"}) {
            reject(info, "CameraThermalMaxFPS", value);
        }
    }

    private static void reject(MCH_TankInfo info, String key, String value) {
        try {
            info.loadItemData(key, value);
            fail("Accepted " + key + "=" + value);
        } catch (IllegalArgumentException expected) { }
    }

    @Test
    public void fullProfilesInheritVehicleCameraAndSeatCharacteristics() {
        MCH_TankInfo info = new MCH_TankInfo("sensor_inheritance_test");
        MCH_AircraftInfo.CameraPosition camera = info.new CameraPosition();
        info.cameraPosition.add(camera);
        info.loadItemData("ThermalResolution", "640,512");
        info.loadItemData("ThermalGrain", "0.1");
        info.loadItemData("ThermalPalette", "WHITE_HOT");
        info.loadItemData("CameraThermalPalette", "0,IRONBOW");
        info.loadItemData("SeatThermalResolution", "1,320,256");
        info.loadItemData("SeatThermalMaxFPS", "1,5");
        assertEquals("IRONBOW", info.getThermalProfile(camera, 0).get("ThermalPalette"));
        assertEquals("640 512", info.getThermalProfile(camera, 0).get("ThermalResolution"));
        assertEquals("320 256", info.getThermalProfile(camera, 1).get("ThermalResolution"));
        assertEquals("0.1", info.getThermalProfile(camera, 1).get("ThermalGrain"));
        assertEquals("5", info.getThermalProfile(camera, 1).get("ThermalMaxFPS"));
        assertSame(info.getThermalProfile(camera, 1), info.getThermalProfile(camera, 1));
        try { info.getThermalProfile(camera, 1).put("ThermalGrain", "0"); fail("Mutable content profile"); }
        catch (UnsupportedOperationException expected) { }
        for (String[] bad : new String[][] {{"ThermalGrain", "NaN"}, {"ThermalContrast", "Infinity"},
                {"ThermalResolution", "4096,256"}, {"ThermalPalette", "INVALID"}, {"ThermalSensitivity", "0"}})
            reject(info, bad[0], bad[1]);
    }

    @Test
    public void capabilityInheritsSeparatelyFromSensorDataAndReloads() {
        MCH_TankInfo info = new MCH_TankInfo("sensor_capability_test");
        MCH_AircraftInfo.CameraPosition camera = info.new CameraPosition();
        info.cameraPosition.add(camera);
        assertTrue(info.isThermalEnabled(camera, 0));
        info.loadItemData("ThermalEnabled", "false");
        assertFalse(info.isThermalEnabled(camera, 0));
        info.loadItemData("CameraThermalEnabled", "0,true");
        assertTrue(info.isThermalEnabled(camera, 0));
        info.loadItemData("SeatThermalEnabled", "1,false");
        assertFalse(info.isThermalEnabled(camera, 1));
        assertFalse(info.getThermalProfile(camera, 1).containsKey("ThermalEnabled"));
        reject(info, "ThermalEnabled", "maybe");
        info.preReload();
        assertTrue(info.isThermalEnabled(null, 1));
    }

    @Test
    public void vehicleCameraRejectsUnavailableThermalAndSkipsNightVision() throws Exception {
        java.lang.reflect.Field singleton = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        singleton.setAccessible(true);
        sun.misc.Unsafe allocator = (sun.misc.Unsafe)singleton.get(null);
        mcheli.tank.MCH_EntityTank tank = (mcheli.tank.MCH_EntityTank)allocator.allocateInstance(mcheli.tank.MCH_EntityTank.class);
        MCH_TankInfo info = new MCH_TankInfo("sensor_camera_test");
        info.cameraPosition.add(info.new CameraPosition());
        java.lang.reflect.Field definition = MCH_EntityAircraft.class.getDeclaredField("acInfo");
        definition.setAccessible(true); definition.set(tank, info);
        mcheli.MCH_Camera camera = new mcheli.MCH_Camera(null, tank);
        camera.setShaderSupport(0, true);
        camera.setMode(0, mcheli.MCH_Camera.MODE_THERMALVISION);
        assertEquals(mcheli.MCH_Camera.MODE_THERMALVISION, camera.getMode(0));
        info.loadItemData("ThermalEnabled", "false");
        assertEquals(mcheli.MCH_Camera.MODE_NORMAL, camera.getMode(0));
        camera.setMode(0, mcheli.MCH_Camera.MODE_THERMALVISION);
        assertEquals(mcheli.MCH_Camera.MODE_NORMAL, camera.getMode(0));
        camera.setMode(0, mcheli.MCH_Camera.MODE_NIGHTVISION);
        assertEquals(mcheli.MCH_Camera.MODE_NORMAL, camera.getMode(0));
        info.loadItemData("SeatThermalEnabled", "0,true");
        camera.setMode(0, mcheli.MCH_Camera.MODE_NIGHTVISION);
        assertEquals(mcheli.MCH_Camera.MODE_THERMALVISION, camera.getMode(0));
        camera.setMode(0, camera.getMode(0) + 1);
        assertEquals(mcheli.MCH_Camera.MODE_NORMAL, camera.getMode(0));
        camera.setMode(0, mcheli.MCH_Camera.MODE_THERMALVISION);
        assertEquals(mcheli.MCH_Camera.MODE_THERMALVISION, camera.getMode(0));
        camera.setShaderSupport(0, false);
        assertEquals(mcheli.MCH_Camera.MODE_NORMAL, camera.getMode(0));
    }

    @Test
    public void sensorCrewBeyondSecondSeatCanUseThermalWithoutGrantingItToPassengers() throws Exception {
        java.lang.reflect.Field singleton = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        singleton.setAccessible(true);
        sun.misc.Unsafe allocator = (sun.misc.Unsafe)singleton.get(null);
        mcheli.tank.MCH_EntityTank tank = (mcheli.tank.MCH_EntityTank)allocator.allocateInstance(mcheli.tank.MCH_EntityTank.class);
        MCH_TankInfo info = new MCH_TankInfo("sensor_extra_crew_test");
        info.cameraPosition.add(info.new CameraPosition());
        info.loadItemData("ThermalEnabled", "false");
        info.loadItemData("SeatThermalEnabled", "3,true");
        java.lang.reflect.Field definition = MCH_EntityAircraft.class.getDeclaredField("acInfo");
        definition.setAccessible(true); definition.set(tank, info);
        mcheli.MCH_Camera camera = new mcheli.MCH_Camera(null, tank);
        camera.setMode(0, mcheli.MCH_Camera.MODE_NIGHTVISION);
        for (int seat = 0; seat < 4; seat++) info.seatList.add(new MCH_SeatInfo(Vec3.createVectorHelper(0, 0, 0), false));
        assertEquals(4, camera.getUserMax());
        assertEquals(mcheli.MCH_Camera.MODE_NORMAL, camera.getMode(0));
        assertFalse(camera.isValidUid(2));
        assertTrue(camera.isValidUid(3));
        camera.setShaderSupport(3, true);
        camera.setMode(3, mcheli.MCH_Camera.MODE_THERMALVISION);
        assertEquals(mcheli.MCH_Camera.MODE_THERMALVISION, camera.getMode(3));
        camera.setShaderSupport(2, true);
        camera.setMode(2, mcheli.MCH_Camera.MODE_THERMALVISION);
        assertEquals(mcheli.MCH_Camera.MODE_NORMAL, camera.getMode(2));
        assertFalse(camera.isValidUid(4));
        info.loadItemData("SeatThermalEnabled", "3,false");
        assertEquals(mcheli.MCH_Camera.MODE_NORMAL, camera.getMode(3));
    }

    @Test
    public void thermalRequiresGunnerViewAndClearsWhenLeavingIt() throws Exception {
        java.lang.reflect.Field singleton = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        singleton.setAccessible(true);
        sun.misc.Unsafe allocator = (sun.misc.Unsafe)singleton.get(null);
        ViewTank tank = (ViewTank)allocator.allocateInstance(ViewTank.class);
        MCH_TankInfo info = new MCH_TankInfo("sensor_view_test");
        info.cameraPosition.add(info.new CameraPosition());
        info.isEnableNightVision = false;
        java.lang.reflect.Field definition = MCH_EntityAircraft.class.getDeclaredField("acInfo");
        definition.setAccessible(true); definition.set(tank, info);
        tank.camera = new mcheli.MCH_Camera(null, tank);
        tank.camera.setShaderSupport(0, true);
        assertTrue(tank.canSwitchCameraMode(0));
        tank.switchCameraMode(null);
        assertEquals(0, tank.getCameraMode(null));
        tank.gunnerView = true;
        tank.switchCameraMode(null);
        assertEquals(2, tank.getCameraMode(null));
        tank.gunnerView = false;
        assertEquals(0, tank.getCameraMode(null));
        tank.gunnerView = true;
        assertEquals(0, tank.getCameraMode(null));

        net.minecraft.entity.player.EntityPlayerMP player = (net.minecraft.entity.player.EntityPlayerMP)
                allocator.allocateInstance(net.minecraft.entity.player.EntityPlayerMP.class);
        player.ridingEntity = tank;
        java.lang.reflect.Field effects = net.minecraft.entity.EntityLivingBase.class.getDeclaredField("activePotionsMap");
        effects.setAccessible(true);
        java.util.Map<Integer, net.minecraft.potion.PotionEffect> potions = new java.util.HashMap<>();
        net.minecraft.potion.PotionEffect nvg = new net.minecraft.potion.PotionEffect(net.minecraft.potion.Potion.nightVision.id, 250);
        potions.put(net.minecraft.potion.Potion.nightVision.id, nvg);
        effects.set(player, potions);
        tank.switchCameraMode(player);
        tank.camera.updateViewer(0, player);
        assertSame(nvg, potions.get(net.minecraft.potion.Potion.nightVision.id));

        net.minecraft.client.Minecraft mc = (net.minecraft.client.Minecraft)allocator.allocateInstance(net.minecraft.client.Minecraft.class);
        mc.gameSettings = (net.minecraft.client.settings.GameSettings)allocator.allocateInstance(net.minecraft.client.settings.GameSettings.class);
        mcheli.MCH_ClientCommonTickHandler tick = (mcheli.MCH_ClientCommonTickHandler)
                allocator.allocateInstance(mcheli.MCH_ClientCommonTickHandler.class);
        java.lang.reflect.Field client = mcheli.wrapper.W_TickHandler.class.getDeclaredField("mc");
        client.setAccessible(true); client.set(tick, mc);
        java.lang.reflect.Method expected = mcheli.MCH_ClientCommonTickHandler.class.getDeclaredMethod("getExpectedCameraMode", net.minecraft.entity.player.EntityPlayer.class);
        expected.setAccessible(true);
        assertEquals(2, expected.invoke(tick, player));
        mc.gameSettings.thirdPersonView = 1;
        assertEquals(0, expected.invoke(tick, player));
        mc.gameSettings.thirdPersonView = 0;
        assertEquals(0, expected.invoke(tick, player));
        info.loadItemData("ThermalEnabled", "false");
        assertFalse(tank.canSwitchCameraMode(0));
    }

    private static final class ViewTank extends mcheli.tank.MCH_EntityTank {
        boolean gunnerView;
        private ViewTank() { super(null); }
        @Override public int getSeatIdByEntity(net.minecraft.entity.Entity entity) { return 0; }
        @Override public boolean isCameraView(net.minecraft.entity.Entity entity) { return gunnerView; }
        @Override public boolean isDestroyed() { return false; }
    }
}
