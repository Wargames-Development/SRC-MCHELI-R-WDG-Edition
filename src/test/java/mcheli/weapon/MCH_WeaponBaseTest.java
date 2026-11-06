package mcheli.weapon;

import net.minecraft.util.Vec3;
import org.junit.Test;

import static org.junit.Assert.*;

public class MCH_WeaponBaseTest {
    @Test
    public void gltdSupportWeaponAllowsMissingDefinition() {
        MCH_WeaponCAS weapon = new MCH_WeaponCAS(null,
                Vec3.createVectorHelper(0.0D, 0.0D, 0.0D), 0.0F, 0.0F, "a10gau8", null);

        assertNull(weapon.getInfo());
        assertEquals("", weapon.displayName);
        assertEquals("", weapon.getName());
        assertEquals("", weapon.explosionType);
        assertEquals(0, weapon.nukeYield);
        assertEquals(0, weapon.chemYield);
        assertEquals(32, weapon.power);
        assertEquals(2, weapon.explosionPower);
        assertEquals(-300, weapon.interval);
    }

    @Test
    public void existingDefinitionKeepsItsExplosionSettings() {
        MCH_WeaponInfo info = new MCH_WeaponInfo("a10gau8");
        info.displayName = "Support weapon";
        info.explosionType = "nuke";
        info.nukeYield = 120;
        info.chemYield = 40;

        MCH_WeaponCAS weapon = new MCH_WeaponCAS(null,
                Vec3.createVectorHelper(0.0D, 0.0D, 0.0D), 0.0F, 0.0F, "a10gau8", info);

        assertSame(info, weapon.getInfo());
        assertEquals(info.displayName, weapon.displayName);
        assertEquals(info.explosionType, weapon.explosionType);
        assertEquals(info.nukeYield, weapon.nukeYield);
        assertEquals(info.chemYield, weapon.chemYield);
    }

    @Test
    public void missilesAndGpsBombsShowConfiguredRangeWithoutRenamingDefinitions() {
        for (String type : new String[]{"aamissile", "atmissile", "asmissile", "tvmissile", "bomb"}) {
            MCH_WeaponInfo info = new MCH_WeaponInfo("test_weapon");
            info.type = type;
            info.displayName = "Guided weapon";
            info.maxLockOnRange = 1100;
            info.isGPSMissile = "bomb".equals(type);
            MCH_WeaponBase weapon = createWeapon(info);

            assertEquals(type, "Guided weapon [1100m]", weapon.getName());
            assertEquals(type, "Guided weapon [1100m]", weapon.getName());
            assertEquals("Guided weapon", info.displayName);
            assertEquals("Guided weapon", weapon.displayName);
            assertEquals("test_weapon", weapon.name);

            info.maxLockOnRange = 2000;
            assertEquals(type, "Guided weapon [2000m]", weapon.getName());
        }
    }

    @Test
    public void otherWeaponsKeepNamesAndModeLabels() {
        for (String type : new String[]{"machinegun1", "machinegun2", "rocket", "bomb", "torpedo", "cas"}) {
            MCH_WeaponInfo info = new MCH_WeaponInfo("test_weapon");
            info.type = type;
            info.displayName = "Weapon";
            info.maxLockOnRange = 1100;
            assertEquals(type, "Weapon", createWeapon(info).getName());
        }

        MCH_WeaponInfo info = new MCH_WeaponInfo("rocket");
        info.type = "rocket";
        MCH_WeaponRocket rocket = new MCH_WeaponRocket(null, null, 0.0F, 0.0F, info.name, info);
        rocket.setCurrentMode(1);
        assertEquals("rocket [HEIAP]", rocket.getName());
    }

    private MCH_WeaponBase createWeapon(MCH_WeaponInfo info) {
        return new MCH_WeaponBase(null, null, 0.0F, 0.0F, info.name, info) {
            @Override
            public boolean shot(MCH_WeaponParam prm) {
                return false;
            }
        };
    }
}
