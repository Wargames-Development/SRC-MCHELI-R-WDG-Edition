package mcheli.mob;

import org.junit.Test;
import static org.junit.Assert.*;

public class MCH_AAGunnerMathTest {
    @Test public void turnsAcrossYawSeamWithoutSnapping() {
        float step = MCH_AAGunnerMath.lookStep(MCH_AAGunnerMath.wrap(-179.0F - 179.0F), 6.0F, 0.5F);
        assertEquals(1.0F, step, 0.00001F);
        assertEquals(6.0F, MCH_AAGunnerMath.lookStep(160.0F, 6.0F, 1.0F), 0.0F);
        assertEquals(-6.0F, MCH_AAGunnerMath.lookStep(-160.0F, 6.0F, 1.0F), 0.0F);
        assertEquals(0.0F, MCH_AAGunnerMath.lookStep(0.0F, 6.0F, 1.0F), 0.0F);
    }

    @Test public void interceptsStationaryCrossingAndApproachingTargets() {
        assertEquals(10.0D, MCH_AAGunnerMath.interceptTime(100, 0, 0, 0, 0, 0, 10), 1.0E-8D);
        double crossing = MCH_AAGunnerMath.interceptTime(100, 0, 0, 0, 0, 6, 10);
        assertEquals(12.5D, crossing, 1.0E-8D);
        assertEquals(10.0D * crossing, Math.sqrt(10000.0D + 36.0D * crossing * crossing), 1.0E-8D);
        assertEquals(5.0D, MCH_AAGunnerMath.interceptTime(100, 0, 0, -10, 0, 0, 10), 1.0E-8D);
    }

    @Test public void includesNativeMuzzleAdvanceAndScaledDiscreteGravity() {
        double speed = 3.9D * 18.0D / 4.0D;
        double offset = 3.9D * 0.5D;
        double time = MCH_AAGunnerMath.interceptTime(100, 0, 0, 0, 0, 0, speed, offset);
        assertEquals(100.0D, speed * time + offset, 1.0E-8D);
        time = MCH_AAGunnerMath.interceptTime(100, 0, 0, 0, 0, 4, speed, offset);
        assertEquals(Math.sqrt(10000.0D + 16.0D * time * time), speed * time + offset, 1.0E-8D);
        assertEquals(-0.6D, MCH_AAGunnerMath.gravityDisplacement(3.0D, -0.1D), 1.0E-8D);
        assertEquals(-0.8D, MCH_AAGunnerMath.gravityDisplacement(3.5D, -0.1D), 1.0E-8D);
    }

    @Test public void fallingTargetLeadIsConsistentAcrossTickOrder() {
        net.minecraft.util.Vec3 before = MCH_AAGunnerMath.interceptPoint(300, 100, 0, 0, -1, 0,
            17.55, 1.95, -0.05, 0, false);
        net.minecraft.util.Vec3 after = MCH_AAGunnerMath.interceptPoint(300, 100, 0, 0, -1.05, 0,
            17.55, 1.95, -0.05, 0, true);
        assertNotNull(before);
        assertNotNull(after);
        assertEquals(before.yCoord, after.yCoord, 1.0E-6D);
        double time = (Math.sqrt(before.xCoord * before.xCoord + before.yCoord * before.yCoord) - 1.95) / 17.55;
        assertEquals(100.0D - time + MCH_AAGunnerMath.gravityDisplacement(time, -0.05), before.yCoord, 0.001D);
        assertNull(MCH_AAGunnerMath.interceptPoint(100, 0, 0, 0, 0, 30, 17.55, 1.95, 0, 0, false));
    }

    @Test public void rejectsImpossibleOrInvalidIntercepts() {
        assertEquals(-1.0D, MCH_AAGunnerMath.interceptTime(100, 0, 0, 20, 0, 0, 10), 0.0D);
        assertEquals(-1.0D, MCH_AAGunnerMath.interceptTime(100, 0, 0, 10, 0, 0, 10), 0.0D);
        assertEquals(-1.0D, MCH_AAGunnerMath.interceptTime(100, 0, 0, 0, 0, 20, 10), 0.0D);
        assertEquals(-1.0D, MCH_AAGunnerMath.interceptTime(100, 0, 0, 0, 0, 0, Double.NaN), 0.0D);
        assertEquals(-1.0D, MCH_AAGunnerMath.interceptTime(100, 0, 0, 0, 0, 0, 0), 0.0D);
    }

    @Test public void friendlyOrUnidentifiedTeamsNeverQualifyAsEnemies() {
        for (String team : new String[]{"wge_t1", "wgc:11111111-1111-1111-1111-111111111111"}) {
            assertFalse(MCH_AAGunnerMath.enemies(team, team));
            assertFalse(MCH_AAGunnerMath.enemies(team, ""));
            assertFalse(MCH_AAGunnerMath.enemies("", team));
            assertFalse(MCH_AAGunnerMath.enemies(team, null));
            assertTrue(MCH_AAGunnerMath.enemies(team, team + "enemy"));
        }
    }
}
