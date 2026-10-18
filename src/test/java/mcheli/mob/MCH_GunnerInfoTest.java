package mcheli.mob;

import org.junit.Test;
import static org.junit.Assert.*;

public class MCH_GunnerInfoTest {
    @Test public void weaponMappingsAreExplicitValidatedAndCaseNormalized() {
        MCH_GunnerInfo info = new MCH_GunnerInfo("ciws_test");
        info.loadItemData("WeaponProfile", " AC_CRAM_M61A1 , GUNNER_AA_CRAM ");
        assertEquals("gunner_aa_cram", info.weaponProfiles.get("ac_cram_m61a1"));
        for (String value : new String[]{"", "gun", "gun,", ",profile", "gun,profile,extra", "../gun,profile", "gun,profile/path"}) {
            try {
                info.loadItemData("WeaponProfile", value);
                fail("Accepted invalid mapping: " + value);
            } catch (IllegalArgumentException expected) { }
        }
    }

    @Test public void performanceParametersLoadIndependentlyOfWeaponStats() {
        MCH_GunnerInfo info = new MCH_GunnerInfo("ciws_test");
        info.loadItemData("TargetType", "aa");
        info.loadItemData("DetectionRange", "450");
        info.loadItemData("ReactionTicks", "12");
        info.loadItemData("LookSpeed", "4");
        info.loadItemData("TrackingResponse", "0.25");
        info.loadItemData("AimError", "1");
        info.loadItemData("LeadFactor", "0.8");
        info.loadItemData("TargetAircraft", "false");
        assertEquals(MCH_EntityGunner.TARGET_AA_AMMO, info.targetType);
        assertEquals(450.0F, info.detectionRange, 0.0F);
        assertEquals(12, info.reactionTicks);
        assertEquals(4.0F, info.lookSpeed, 0.0F);
        assertEquals(0.25F, info.trackingResponse, 0.0F);
        assertEquals(1.0F, info.aimError, 0.0F);
        assertEquals(0.8F, info.leadFactor, 0.0F);
        assertFalse(info.targetAircraft);
        assertTrue(info.targetMissiles);
    }

    @Test public void rejectsNonFiniteOutOfRangeAndFractionalTiming() {
        for (String key : new String[]{"DetectionRange", "LookSpeed", "TrackingResponse", "AimError", "FireTolerance", "LeadFactor"}) {
            for (String value : new String[]{"NaN", "Infinity", "-1", "100000", "bad"}) {
                try {
                    new MCH_GunnerInfo("bad").loadItemData(key, value);
                    fail("Accepted " + key + "=" + value);
                } catch (IllegalArgumentException expected) { }
            }
        }
        for (String key : new String[]{"ReactionTicks", "ScanInterval"}) {
            try {
                new MCH_GunnerInfo("bad").loadItemData(key, "1.5");
                fail("Accepted fractional timing");
            } catch (IllegalArgumentException expected) { }
        }
    }
}
