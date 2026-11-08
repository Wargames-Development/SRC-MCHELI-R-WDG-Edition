package mcheli.weapon;

import mcheli.plane.MCP_EntityPlane;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.profiler.Profiler;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraft.world.WorldProviderSurface;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraft.world.chunk.IChunkProvider;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class MCH_RadarNotchTest {
    private TestWorld world;
    private TestMissile missile;
    private MCP_EntityPlane target;
    private MCH_WeaponInfo info;

    @Before public void setup() throws Exception {
        world = new TestWorld();
        missile = new TestMissile(world);
        info = new MCH_WeaponInfo("notch_test");
        info.activeRadar = true;
        info.isHeatSeekerMissile = false;
        info.notchDegrees = 85;
        info.rigidityTime = 0;
        info.lockMinHeight = 0;
        missile.setInfo("notch_test", info);
        missile.shootingEntity = new MCH_EntityBullet(world);
        missile.motionX = 1;
        // Aircraft construction needs the full mod lifecycle; only server kinematics are needed here.
        java.lang.reflect.Field singleton = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        singleton.setAccessible(true);
        target = (MCP_EntityPlane)((sun.misc.Unsafe)singleton.get(null)).allocateInstance(MCP_EntityPlane.class);
        target.worldObj = world;
        target.setEntityId(1000);
        target.posX = 100;
        target.motionZ = 1;
        world.candidates.add(target);
        missile.setTargetEntity(target);
    }

    @Test public void symmetricWindowUsesFullVelocityAndRejectsInvalidVectors() {
        for (double degrees : new double[]{85, 90, 95}) assertTrue(atAngle(degrees, 85));
        for (double degrees : new double[]{0, 84.99, 95.01, 180}) assertFalse(atAngle(degrees, 85));
        assertTrue(MCH_EntityBaseBullet.isVelocityInNotch(0, 1, 0, 0, 0, -2, 85));
        assertFalse(MCH_EntityBaseBullet.isVelocityInNotch(0, 0, 0, 1, 0, 0, 85));
        assertFalse(MCH_EntityBaseBullet.isVelocityInNotch(1, 0, 0, 0, 0, 0, 85));
        assertFalse(MCH_EntityBaseBullet.isVelocityInNotch(Double.NaN, 0, 0, 0, 1, 0, 85));
        assertFalse(MCH_EntityBaseBullet.isVelocityInNotch(1, 0, 0, 0, Double.POSITIVE_INFINITY, 0, 85));
        assertFalse(atAngle(90, 0));
        assertFalse(atAngle(90, 90));
        assertTrue(atAngle(86, 85));
        assertFalse("The AIM-260's narrower window should resist the same maneuver", atAngle(86, 88));
    }

    private static boolean atAngle(double angle, float threshold) {
        return MCH_EntityBaseBullet.isVelocityInNotch(1, 0, 0,
            Math.cos(Math.toRadians(angle)), 0, Math.sin(Math.toRadians(angle)), threshold);
    }

    @Test public void definitionsDefaultToDisabledAndRejectInvalidAnglesSafely() {
        MCH_WeaponInfo parsed = new MCH_WeaponInfo("notch_parse");
        assertEquals(0, parsed.notchDegrees, 0);
        assertEquals(2, parsed.notchBreakTicks);
        assertEquals(3, parsed.notchReacquireTicks);
        parsed.loadItemData("notchdegrees", "85.5");
        assertEquals(85.5, parsed.notchDegrees, 0);
        for (String value : new String[]{"-1", "0", "90", "180", "NaN", "Infinity"}) {
            parsed.loadItemData("NotchDegrees", value);
            assertEquals(0, parsed.notchDegrees, 0);
        }
        parsed.loadItemData("NotchBreakTicks", "0");
        parsed.loadItemData("NotchReacquireTicks", "1000");
        assertEquals(1, parsed.notchBreakTicks);
        assertEquals(200, parsed.notchReacquireTicks);
    }

    @Test public void breakRequiresConsecutiveTicksOnTheSameTargetAndRespectsLaunchDelay() {
        info.rigidityTime = 10;
        assertFalse(missile.updateRadarNotch());
        info.rigidityTime = 0;
        assertFalse(missile.updateRadarNotch());
        target.motionX = 1;
        assertFalse(missile.updateRadarNotch());
        target.motionX = 0;
        assertFalse(missile.updateRadarNotch());
        missile.setTargetEntity(null);
        missile.setTargetEntity(target);
        assertFalse(missile.updateRadarNotch());
        assertTrue(missile.updateRadarNotch());
        assertNull(missile.targetEntity);
        assertEquals(0, missile.getTargetEntityID());
        assertFalse(missile.hasLastKnownTarget);
    }

    @Test public void coastBlocksDatalinkSnapshotsAndSteeringThenRestoresCapture() {
        missile.setDataLinkRelayMode(true);
        missile.setActiveRadarCaptured(true);
        missile.setSnapshotTarget(target.getEntityId(), 100, 0, 0, 0, 0, 1);
        breakTrack();
        missile.setTargetEntity(target);
        missile.setTargetEntity(missile.shootingEntity);
        missile.setSnapshotTarget(target.getEntityId(), 100, 0, 100, 1, 0, 0);
        missile.guidanceToPos(100, 0, 30);
        assertNull(missile.targetEntity);
        assertEquals(0, missile.snapshotTargetId);
        assertEquals(0, missile.motionZ, 0);
        assertTrue(missile.isDataLinkRelayMode());
        assertFalse(missile.isActiveRadarCaptured());
        target.motionX = 1;
        assertTrue(missile.updateRadarNotch());
        target.motionX = 0;
        assertTrue(missile.updateRadarNotch());
        target.motionX = 1;
        assertTrue(missile.updateRadarNotch());
        assertTrue(missile.updateRadarNotch());
        assertFalse(missile.updateRadarNotch());
        assertSame(target, missile.targetEntity);
        assertTrue(missile.isActiveRadarCaptured());
    }

    @Test public void recoveryRequiresRangeConeHeightAndRadarSupport() {
        info.notchReacquireTicks = 1;
        breakTrack();
        target.motionX = 1;
        target.posX = 10000;
        assertTrue(missile.updateRadarNotch());
        target.posX = -100;
        assertTrue(missile.updateRadarNotch());
        target.posX = 100;
        target.onGround = true;
        assertTrue(missile.updateRadarNotch());
        target.onGround = false;
        missile.supported = false;
        assertTrue(missile.updateRadarNotch());
        missile.supported = true;
        assertFalse(missile.updateRadarNotch());
        assertSame(target, missile.targetEntity);
    }

    @Test public void missingOrDeadTargetCannotBeResurrectedAndActiveSearchCanResume() {
        missile.setActiveRadarCaptured(true);
        breakTrack();
        world.candidates.clear();
        target.motionX = 1;
        assertFalse(missile.updateRadarNotch());
        assertNull(missile.targetEntity);
        assertTrue(missile.isActiveRadarCaptured());
        world.candidates.add(target);
        missile.setTargetEntity(target);
        target.motionX = 0;
        breakTrack();
        target.isDead = true;
        assertFalse(missile.updateRadarNotch());
        assertNull(missile.targetEntity);
    }

    @Test public void jammingCancelsPendingRecovery() {
        breakTrack();
        missile.applyRadarJamming(20);
        target.motionX = 1;
        assertFalse(missile.updateRadarNotch());
        missile.setTargetEntity(target);
        assertNull(missile.targetEntity);
        assertFalse(missile.isActiveRadarCaptured());
    }

    @Test public void semiActiveRecoveryFailsClosedWithoutServerRadarSupport() {
        assertTrue(missile.serverSupports(target));
        info.passiveRadar = true;
        assertFalse(missile.serverSupports(target));
        info.passiveRadar = false;
        info.semiActiveRadar = true;
        assertFalse(missile.serverSupports(target));
    }

    @Test public void autonomousScanSkipsNotchingAircraftAndKeepsOtherTargetsEligible() {
        missile.setTargetEntity(null);
        missile.scanForTargets();
        assertNull(missile.targetEntity);
        target.motionX = 1;
        missile.scanForTargets();
        assertSame(target, missile.targetEntity);
    }

    @Test public void omittedSettingAndNonRadarOrArmWeaponsRetainExistingBehavior() {
        info.notchDegrees = 0;
        assertFalse(missile.updateRadarNotch());
        assertFalse(missile.updateRadarNotch());
        info.notchDegrees = 85;
        info.activeRadar = false;
        assertFalse(missile.updateRadarNotch());
        assertFalse(missile.updateRadarNotch());
        info.activeRadar = true;
        info.antiRadiationMissile = true;
        assertFalse(missile.updateRadarNotch());
        assertFalse(missile.updateRadarNotch());
        assertSame(target, missile.targetEntity);
    }

    private void breakTrack() {
        assertFalse(missile.updateRadarNotch());
        assertTrue(missile.updateRadarNotch());
    }

    private static final class TestMissile extends MCH_EntityAAMissile {
        boolean supported = true;
        TestMissile(World world) { super(world); }
        @Override public int getCountOnUpdate() { return 1; }
        @Override protected boolean isRadarNotchTargetSupported(Entity target) { return supported; }
        boolean serverSupports(Entity target) { return super.isRadarNotchTargetSupported(target); }
    }

    private static final class TestWorld extends World {
        final List<Entity> candidates = new ArrayList<Entity>();
        TestWorld() {
            super(null, "notch_test", new WorldProviderSurface() {
                @Override protected void registerWorldChunkManager() { }
                @Override public void calculateInitialWeather() { }
            }, new WorldSettings(0, WorldSettings.GameType.SURVIVAL, false, false, WorldType.DEFAULT), new Profiler());
        }
        @Override protected IChunkProvider createChunkProvider() { return null; }
        @Override protected int func_152379_p() { return 0; }
        @Override public void calculateInitialSkylight() { }
        @Override public Block getBlock(int x, int y, int z) { return Blocks.air; }
        @Override public Entity getEntityByID(int id) {
            for (Entity entity : candidates) if (entity.getEntityId() == id) return entity;
            return null;
        }
        @Override public List getEntitiesWithinAABB(Class type, AxisAlignedBB box) { return candidates; }
    }
}
