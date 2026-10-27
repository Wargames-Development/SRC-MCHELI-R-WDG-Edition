package mcheli.render;

import mcheli.MCH_Config;
import mcheli.MCH_ConfigPrm;
import mcheli.MCH_EntityInfoManager;
import org.junit.Test;
import static org.junit.Assert.*;

public final class MCH_VehicleRenderDistanceTest {
    @Test public void existingConfigLoaderRegistersTheNewDefaultAndReadsOverrides() throws Exception {
        MCH_Config config = new MCH_Config("", "unused-test.cfg");
        assertEquals(750, MCH_Config.VehicleLodRenderDistance.prmInt);
        boolean registered = false;
        for (MCH_ConfigPrm parameter : MCH_Config.General)
            registered |= parameter == MCH_Config.VehicleLodRenderDistance;
        assertTrue(registered);
        java.lang.reflect.Method read = MCH_Config.class.getDeclaredMethod("readConfigData", String.class);
        read.setAccessible(true);
        read.invoke(config, "VehicleLodRenderDistance = 1250");
        assertEquals(1250D, MCH_RenderFarVehicle.getVehicleRenderDistance(), 0D);
    }

    @Test public void defaultAndConfiguredDistanceOnlyGatePresentation() {
        MCH_ConfigPrm saved = MCH_Config.VehicleLodRenderDistance;
        try {
            MCH_Config.VehicleLodRenderDistance = null;
            assertEquals(750D, MCH_RenderFarVehicle.getVehicleRenderDistance(), 0D);
            assertTrue(MCH_RenderFarVehicle.withinVehicleRenderDistance(750, 0, 0, 750));
            assertFalse(MCH_RenderFarVehicle.withinVehicleRenderDistance(750.01, 0, 0, 750));
            assertFalse(MCH_RenderFarVehicle.withinVehicleRenderDistance(600, 600, 0, 750));
            MCH_Config.VehicleLodRenderDistance = new MCH_ConfigPrm("VehicleLodRenderDistance", 1200);
            assertEquals(1200D, MCH_RenderFarVehicle.getVehicleRenderDistance(), 0D);
            MCH_Config.VehicleLodRenderDistance.prmInt = -100;
            assertEquals(1D, MCH_RenderFarVehicle.getVehicleRenderDistance(), 0D);
            MCH_Config.VehicleLodRenderDistance.prmInt = Integer.MAX_VALUE;
            assertEquals(4096D, MCH_RenderFarVehicle.getVehicleRenderDistance(), 0D);
            assertEquals("BVR contact tracking keeps its original range", 4096D,
                    MCH_EntityInfoManager.ENTITY_INFO_SYNC_RANGE, 0D);
        } finally { MCH_Config.VehicleLodRenderDistance = saved; }
    }
}
