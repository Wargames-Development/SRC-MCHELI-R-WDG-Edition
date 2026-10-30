package mcheli.weapon;

import org.junit.Test;
import static org.junit.Assert.*;

public class MCH_FmurMountedGunTest {
    @Test public void highRateRoundsAreBoundedAndRequireNativeOneTickMachineGuns() {
        for (String[] setting : new String[][]{{"RoundsPerShot", "0"}, {"RoundsPerShot", "9"},
            {"delay", "0"}, {"type", "machinegun1"}, {"FMURBullet", "50calammo"}}) {
            MCH_WeaponInfo gun = new MCH_WeaponInfo("ciws_batch");
            gun.loadItemData("type", "machinegun2");
            gun.loadItemData("delay", "1");
            gun.loadItemData("RoundsPerShot", "8");
            gun.checkData();
            gun.loadItemData(setting[0], setting[1]);
            try { gun.checkData(); fail(setting[0] + "=" + setting[1]); }
            catch (IllegalArgumentException expected) { }
        }
    }

    @Test public void nativeTracerGlowIsOptInAndRejectsInvalidScaleOrForeignRounds() {
        MCH_WeaponInfo gun = new MCH_WeaponInfo("ciws");
        gun.loadItemData("type", "machinegun2");
        assertFalse(gun.fmurTracerGlow);
        gun.loadItemData("FMURTracerGlow", "true");
        gun.loadItemData("FMURTracerScale", "1.25");
        gun.checkData();
        assertTrue(gun.fmurTracerGlow);
        assertEquals(1.25F, gun.fmurTracerScale, 0F);
        for (String[] setting : new String[][]{{"FMURTracerScale", "NaN"}, {"FMURTracerScale", "Infinity"},
            {"FMURTracerScale", "0"}, {"FMURTracerScale", "4.1"}, {"type", "rocket"}, {"FMURBullet", "50calammo"}}) {
            MCH_WeaponInfo invalid = new MCH_WeaponInfo("bad_ciws");
            invalid.loadItemData("type", "machinegun2");
            invalid.loadItemData("FMURTracerGlow", "true");
            invalid.loadItemData(setting[0], setting[1]);
            try { invalid.checkData(); fail(setting[0] + "=" + setting[1]); }
            catch (IllegalArgumentException expected) { }
        }
    }

    @Test public void nativeGunsRemainDefaultAndFmurNamesKeepTheirCase() {
        MCH_WeaponInfo nativeGun = new MCH_WeaponInfo("native");
        assertEquals("", nativeGun.fmurBullet);
        MCH_WeaponInfo gun = configured();
        gun.loadItemData("FMURBullet", "  CaseSensitiveAmmo  ");
        gun.checkData();
        assertEquals("CaseSensitiveAmmo", gun.fmurBullet);
        assertEquals(1.0F, gun.fmurBulletDamage, 0.0F);
        gun.loadItemData("fmurbulletdamage", "5");
        assertEquals(5.0F, gun.fmurBulletDamage, 0.0F);
    }

    @Test public void malformedDamageSpeedAndUnsupportedWeaponsAreRejected() {
        for (String[] setting : new String[][]{{"FMURBulletDamage", "NaN"}, {"FMURBulletDamage", "Infinity"},
            {"FMURBulletDamage", "0"}, {"FMURBulletDamage", "-1"}, {"acceleration", "0"},
            {"acceleration", "NaN"}, {"type", "rocket"}, {"canister", "2"}}) {
            MCH_WeaponInfo gun = configured();
            gun.loadItemData(setting[0], setting[1]);
            try { gun.checkData(); fail(setting[0] + "=" + setting[1]); }
            catch (IllegalArgumentException expected) { }
        }
    }

    @Test public void tracerSettingsRequireAmmoAndAPositivePairedInterval() {
        MCH_WeaponInfo gun = configured();
        assertEquals("", gun.fmurTracerBullet);
        assertEquals(0, gun.fmurTracerInterval);
        gun.loadItemData("FMURTracerBullet", "  RedTracer  ");
        gun.loadItemData("FMURTracerInterval", "5");
        gun.checkData();
        assertEquals("RedTracer", gun.fmurTracerBullet);
        assertEquals(5, gun.fmurTracerInterval);
        for (String[] setting : new String[][]{{"FMURTracerBullet", ""}, {"FMURTracerInterval", "0"},
            {"FMURTracerInterval", "-1"}, {"FMURBullet", ""}}) {
            MCH_WeaponInfo invalid = configured();
            invalid.loadItemData("FMURTracerBullet", "RedTracer");
            invalid.loadItemData("FMURTracerInterval", "5");
            invalid.loadItemData(setting[0], setting[1]);
            try { invalid.checkData(); fail(setting[0] + "=" + setting[1]); }
            catch (IllegalArgumentException expected) { }
        }
    }

    private static MCH_WeaponInfo configured() {
        MCH_WeaponInfo gun = new MCH_WeaponInfo("mounted");
        gun.loadItemData("type", "machinegun1");
        gun.loadItemData("acceleration", "15");
        gun.loadItemData("FMURBullet", "50calammo");
        return gun;
    }
}
