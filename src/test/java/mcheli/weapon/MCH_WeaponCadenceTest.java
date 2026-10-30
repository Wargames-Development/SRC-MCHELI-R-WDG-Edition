package mcheli.weapon;

import java.util.ArrayDeque;
import java.util.Queue;
import net.minecraft.util.Vec3;
import org.junit.Test;
import static org.junit.Assert.*;

public class MCH_WeaponCadenceTest {
    @Test public void delayedShotAcknowledgementDoesNotRestartAnElapsedCooldown() throws Exception {
        MCH_WeaponSet gun = gun(3);
        gun.countWait = 3;
        for (int tick = 0; tick < 3; ++tick) gun.update(null, true, false);
        assertTrue(gun.canUse());
        gun.applyServerUseState(99, 100, 0, 0, 2, 0);
        assertTrue("The reply describes the earlier shot, not a new cooldown", gun.canUse());
        assertEquals(99, gun.getAmmoNum());
    }

    @Test public void ciwsPlayerCadenceSurvivesTwoTickRoundTripAcknowledgements() throws Exception {
        for (int delay : new int[]{1, 2}) {
            MCH_WeaponSet gun = gun(delay + 1); // Existing client-side interval adjustment.
            Queue<Integer> replies = new ArrayDeque<Integer>();
            int shots = 0;
            for (int tick = 0; tick < 120; ++tick) {
                gun.update(null, true, false);
                while (!replies.isEmpty() && replies.peek() <= tick) {
                    replies.remove();
                    gun.applyServerUseState(100, 100, 0, 0, delay, 0);
                }
                if (gun.canUse()) {
                    ++shots;
                    gun.countWait = delay + 1;
                    replies.add(tick + 2);
                }
            }
            assertEquals("Delayed acknowledgements must not lower CIWS request cadence", 120 / (delay + 1), shots);
        }
    }

    @Test public void reloadsSwitchDelaysAndServerCooldownRemainBlocking() throws Exception {
        MCH_WeaponSet gun = gun(2);
        gun.applyServerUseState(50, 100, 5, 0, -10, 0);
        assertEquals(-10, gun.countWait);
        assertFalse(gun.canUse());
        gun.applyServerUseState(50, 100, 5, 0, 2, 20);
        assertEquals(20, gun.countReloadWait);
        assertFalse(gun.canUse());
        gun.countReloadWait = 0;
        gun.countWait = 2;
        assertFalse(gun.canUse());
        gun.update(null, true, false);
        assertFalse(gun.canUse());
        gun.update(null, true, false);
        assertTrue(gun.canUse());
    }

    private static MCH_WeaponSet gun(int interval) throws Exception {
        new mcheli.MCH_Config(".", "unused.cfg");
        // These timing methods only read the world's side flag for debug logging.
        java.lang.reflect.Field singleton = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        singleton.setAccessible(true);
        net.minecraft.world.World world = (net.minecraft.world.World) ((sun.misc.Unsafe)singleton.get(null))
            .allocateInstance(net.minecraft.client.multiplayer.WorldClient.class);
        MCH_WeaponInfo info = new MCH_WeaponInfo("ciws_cadence");
        info.type = "machinegun2"; info.round = info.maxAmmo = 100;
        MCH_WeaponMachineGun2 gun = new MCH_WeaponMachineGun2(world, Vec3.createVectorHelper(0, 0, 0), 0, 0, info.name, info);
        gun.interval = interval;
        return new MCH_WeaponSet(gun);
    }
}
