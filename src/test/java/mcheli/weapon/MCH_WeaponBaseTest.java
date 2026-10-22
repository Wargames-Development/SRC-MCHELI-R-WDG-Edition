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
}
