package mcheli.render;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MCH_RenderFarVehicleLightingTest {

    @Test
    public void daylightContactOrUnloadedChunkKeepsSkyLevel() {
        assertEquals(0xF00000, MCH_RenderFarVehicle.distantVehicleBrightness(0, 0, false));
        assertEquals(0xF000A0, MCH_RenderFarVehicle.distantVehicleBrightness(0xF000A0, 0, false));
    }

    @Test
    public void nightSkylessAndUnavailableLightStayBounded() {
        assertEquals(0x400030, MCH_RenderFarVehicle.distantVehicleBrightness(0x30, 11, false));
        assertEquals(0x30, MCH_RenderFarVehicle.distantVehicleBrightness(0x30, 0, true));
        assertEquals(-1, MCH_RenderFarVehicle.distantVehicleBrightness(-1, 0, false));
    }
}
