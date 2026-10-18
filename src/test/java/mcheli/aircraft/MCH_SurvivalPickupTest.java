package mcheli.aircraft;

import mcheli.MCH_Config;
import mcheli.MCH_ConfigPrm;
import mcheli.helicopter.MCH_HeliInfo;
import mcheli.plane.MCP_PlaneInfo;
import mcheli.tank.MCH_TankInfo;
import mcheli.vehicle.MCH_VehicleInfo;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class MCH_SurvivalPickupTest {
    private MCH_ConfigPrm previousPickupConfig;

    @Before
    public void configurePickup() {
        previousPickupConfig = MCH_Config.EnableSurvivalVehiclePickup;
        MCH_Config.EnableSurvivalVehiclePickup = new MCH_ConfigPrm("EnableSurvivalVehiclePickup", false);
    }

    @After
    public void restoreConfig() {
        MCH_Config.EnableSurvivalVehiclePickup = previousPickupConfig;
    }

    @Test
    public void everyVehicleTypeInheritsGlobalDefaultAndSupportsBothOverrides() {
        for (MCH_AircraftInfo info : new MCH_AircraftInfo[]{
                new MCH_VehicleInfo("tow"), new MCH_TankInfo("abrams"),
                new MCP_PlaneInfo("a10"), new MCH_HeliInfo("heli")}) {
            MCH_Config.EnableSurvivalVehiclePickup.prmBool = false;
            assertFalse(info.isSurvivalPickupAllowed());
            info.loadItemData("survivalpickup", " TrUe ");
            assertTrue(info.isSurvivalPickupAllowed());
            MCH_Config.EnableSurvivalVehiclePickup.prmBool = true;
            info.loadItemData("SurvivalPickup", "false");
            assertFalse(info.isSurvivalPickupAllowed());
            info.preReload();
            assertTrue(info.isSurvivalPickupAllowed());
            MCH_Config.EnableSurvivalVehiclePickup.prmBool = false;
            assertFalse(info.isSurvivalPickupAllowed());
        }
    }

    @Test
    public void malformedOverridesFailDefinitionLoading() {
        for (String value : new String[]{"", "yes", "1", "default"}) {
            try {
                new MCH_VehicleInfo("invalid").loadItemData("SurvivalPickup", value);
                fail("Accepted invalid SurvivalPickup: " + value);
            } catch (IllegalArgumentException expected) {
                // Reject a typo instead of silently changing pickup permissions.
            }
        }
    }
}
