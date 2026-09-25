package mcheli.weapon;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class MCH_ApexGpsSafetyTest {
    @Test
    public void rejectsLauncherAndNearbyTargets() {
        assertFalse(MCH_GPSPosition.isSafeApexTarget(120.0D, -80.0D, 120.0D, -80.0D));
        assertFalse(MCH_GPSPosition.isSafeApexTarget(120.0D, -80.0D, 184.0D, -80.0D));
        assertFalse(MCH_GPSPosition.isSafeApexTarget(120.0D, -80.0D, 120.0D, -143.0D));
        assertTrue(MCH_GPSPosition.isSafeApexTarget(120.0D, -80.0D, 185.0D, -80.0D));
    }

    @Test
    public void rejectsNonFiniteTargetCoordinates() {
        assertFalse(MCH_GPSPosition.isSafeApexTarget(0.0D, 0.0D, Double.NaN, 100.0D));
        assertFalse(MCH_GPSPosition.isSafeApexTarget(0.0D, 0.0D, 100.0D, Double.POSITIVE_INFINITY));
    }
}
