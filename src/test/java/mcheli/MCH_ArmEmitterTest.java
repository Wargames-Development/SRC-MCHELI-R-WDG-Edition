package mcheli;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class MCH_ArmEmitterTest {
    @Test public void onlyActiveUndestroyedEmittersMatchTheWeaponTargetType() {
        MCH_EntityInfo target = new MCH_EntityInfo(37, "world", "AA", "mcheli.tank.MCH_EntityTank", 0, 64, 0, 0, 64, 0);
        assertFalse(target.isArmEmitterForWeapon("atmissile"));
        target.armEmitter = true;
        assertTrue(target.isArmEmitterForWeapon("atmissile"));
        assertFalse(target.isArmEmitterForWeapon("aamissile"));
        target.destroyed = true;
        assertFalse(target.isArmEmitterForWeapon("atmissile"));
        target.destroyed = false;
        target.entityClassName = "mcheli.plane.MCP_EntityPlane";
        assertFalse(target.isArmEmitterForWeapon("atmissile"));
        assertTrue(target.isArmEmitterForWeapon("aamissile"));
        target.armEmitter = false;
        assertFalse(target.isArmEmitterForWeapon("aamissile"));
        target.armEmitter = true;
        target.entityClassName = null;
        assertFalse(target.isArmEmitterForWeapon("atmissile"));
    }
}
