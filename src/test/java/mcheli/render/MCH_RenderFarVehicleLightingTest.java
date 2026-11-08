package mcheli.render;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MCH_RenderFarVehicleLightingTest {

    @Test
    public void nativeAndSnapshotSamplesPreserveShadeAndBlockLight() {
        assertEquals(0, MCH_RenderFarVehicle.vehicleLightmapBrightness(0, 0xF00000, 0xF00000, false));
        assertEquals(0x2000A0, MCH_RenderFarVehicle.vehicleLightmapBrightness(-1, 0x2000A0, 0xF00000, false));
        assertEquals(0xF000F0, MCH_RenderFarVehicle.vehicleLightmapBrightness(0xF000F0, 0, 0, false));
        assertEquals(0, MCH_RenderFarVehicle.vehicleLightmapBrightness(-1, 0, 0xF00000, false));
    }

    @Test
    public void unloadedHandoffUsesLastSampleAndUnknownSkyIsNotDimmedTwice() {
        assertEquals(0x400030, MCH_RenderFarVehicle.vehicleLightmapBrightness(-1, -1, 0x400030, false));
        assertEquals(0xF00000, MCH_RenderFarVehicle.vehicleLightmapBrightness(-1, -1, -1, false));
        assertEquals(0, MCH_RenderFarVehicle.vehicleLightmapBrightness(-1, -1, -1, true));
        assertEquals(0xA0, MCH_RenderFarVehicle.vehicleLightmapBrightness(-1, 0xA0, -1, true));
    }
}
