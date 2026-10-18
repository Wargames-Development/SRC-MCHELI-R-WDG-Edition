package mcheli.weapon;

import mcheli.MCH_Config;
import mcheli.mob.MCH_EntityGunner;
import net.minecraft.entity.Entity;
import net.minecraft.profiler.Profiler;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraft.world.WorldProviderSurface;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraft.world.chunk.IChunkProvider;
import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.*;

public class MCH_ProjectileCollisionTest {
    @Test public void catchesMissileCrossingBetweenStaticEndpointBoxes() {
        Vec3 from = Vec3.createVectorHelper(0, 0, 0);
        Vec3 to = Vec3.createVectorHelper(10, 0, 0);
        AxisAlignedBB box = AxisAlignedBB.getBoundingBox(4.5, -0.5, -5.5, 5.5, 0.5, -4.5);
        assertNull(box.calculateIntercept(from, to));
        assertNull(box.getOffsetBoundingBox(0, 0, 10).calculateIntercept(from, to));
        MovingObjectPosition hit = MCH_ProjectileCollision.sweep(box, from, to, 0, 0, 10);
        assertNotNull(hit);
        assertTrue(hit.hitVec.xCoord >= 4.5D && hit.hitVec.xCoord <= 5.5D);
        assertEquals(0.0D, hit.hitVec.zCoord, 0.0D);
    }

    @Test public void catchesHeadOnAndInsideBoxButRejectsParallelMiss() {
        Vec3 from = Vec3.createVectorHelper(0, 0, 0);
        Vec3 to = Vec3.createVectorHelper(10, 0, 0);
        AxisAlignedBB box = AxisAlignedBB.getBoundingBox(9.5, -0.5, -0.5, 10.5, 0.5, 0.5);
        assertEquals(4.75D, MCH_ProjectileCollision.sweep(box, from, to, -10, 0, 0).hitVec.xCoord, 1.0E-8D);
        assertNull(MCH_ProjectileCollision.sweep(box.getOffsetBoundingBox(0, 3, 0), from, to, 10, 0, 0));
        AxisAlignedBB surrounding = AxisAlignedBB.getBoundingBox(-0.5, -0.5, -0.5, 0.5, 0.5, 0.5);
        assertEquals(0.0D, MCH_ProjectileCollision.sweep(surrounding, from, to, 10, 0, 0).hitVec.xCoord, 0.0D);
    }

    @Test public void missileBoxIsCenteredOnItsRealPositionAndRepairedAfterRawSpawnOffset() {
        TestWorld world = new TestWorld();
        MCH_EntityRocket missile = new MCH_EntityRocket(world);
        missile.setPosition(10, 20, 30);
        assertEquals(20.0D, (missile.boundingBox.minY + missile.boundingBox.maxY) * 0.5D, 0.0D);
        missile.posX += 5.0D;
        MCH_EntityBullet round = new MCH_EntityBullet(world);
        round.getProjectileCollision(missile, Vec3.createVectorHelper(14, 20, 30), Vec3.createVectorHelper(16, 20, 30));
        assertEquals(15.0D, (missile.boundingBox.minX + missile.boundingBox.maxX) * 0.5D, 0.0D);
        assertEquals(20.0D, (missile.boundingBox.minY + missile.boundingBox.maxY) * 0.5D, 0.0D);
    }

    @Test public void collisionResultDoesNotDependOnWhichProjectileUpdatedFirst() throws Exception {
        TestWorld world = new TestWorld();
        MCH_EntityRocket missile = new MCH_EntityRocket(world);
        MCH_EntityBullet round = new MCH_EntityBullet(world);
        Vec3 from = Vec3.createVectorHelper(0, 20, 0);
        Vec3 to = Vec3.createVectorHelper(10, 20, 0);
        missile.setPosition(5, 20, -5);
        missile.motionZ = 10;
        MovingObjectPosition before = round.getProjectileCollision(missile, from, to);
        missile.prevPosX = 5;
        missile.prevPosY = 20;
        missile.prevPosZ = -5;
        missile.setPosition(5, 20, 5);
        java.lang.reflect.Field tick = MCH_EntityBaseBullet.class.getDeclaredField("lastMovementWorldTick");
        tick.setAccessible(true);
        tick.setLong(missile, world.getTotalWorldTime());
        MovingObjectPosition after = round.getProjectileCollision(missile, from, to);
        assertNotNull(before);
        assertNotNull(after);
        assertEquals(before.hitVec.xCoord, after.hitVec.xCoord, 1.0E-8D);
        assertEquals(before.hitVec.zCoord, after.hitVec.zCoord, 1.0E-8D);
    }

    @Test public void thrownRoundOnlyImpactsEnabledMissileAtItsActualPosition() {
        TestWorld world = new TestWorld();
        TestRocket missile = new TestRocket(world);
        missile.setPosition(10.25, 20.5, 30.75);
        MCH_WeaponInfo info = new MCH_WeaponInfo("damage_test");
        missile.setInfo("damage_test", info);
        MCH_EntityBullet round = new MCH_EntityBullet(world);
        DamageSource damage = DamageSource.causeThrownDamage(round, new MCH_EntityGunner(world));
        assertFalse(missile.attackEntityFrom(damage, 1));
        assertNull(missile.impact);
        info.canBeIntercepted = true;
        assertTrue(missile.attackEntityFrom(damage, 1));
        assertEquals(10.25D, missile.impact.hitVec.xCoord, 0.0D);
        assertEquals(20.5D, missile.impact.hitVec.yCoord, 0.0D);
        assertEquals(30.75D, missile.impact.hitVec.zCoord, 0.0D);
    }

    @Test public void terrainClippedRayDoesNotAdvanceTargetThroughTheRestOfTheTick() {
        TestWorld world = new TestWorld();
        MCH_EntityRocket missile = new MCH_EntityRocket(world);
        MCH_EntityBullet round = new MCH_EntityBullet(world);
        round.motionX = 10;
        missile.setPosition(4, 20, -8);
        missile.motionZ = 10;
        Vec3 from = Vec3.createVectorHelper(0, 20, 0);
        Vec3 clippedEnd = Vec3.createVectorHelper(5, 20, 0);
        assertNotNull(MCH_ProjectileCollision.sweep(missile.boundingBox.expand(0.3, 0.3, 0.3), from, clippedEnd, 0, 0, 10));
        assertNull(round.getProjectileCollision(missile, from, clippedEnd));
    }

    @Test public void interceptionRequiresTargetFlagAndRejectsFriendlyOrUnknownMissiles() {
        TestWorld world = new TestWorld();
        MCH_EntityGunner gunner = new MCH_EntityGunner(world);
        gunner.setTeamName("wge_t1");
        gunner.setTargetType(MCH_EntityGunner.TARGET_AA_AMMO);
        MCH_EntityGunner enemy = new MCH_EntityGunner(world);
        enemy.setTeamName("wge_t2");
        MCH_EntityRocket missile = new MCH_EntityRocket(world);
        MCH_WeaponInfo missileInfo = new MCH_WeaponInfo("missile_test");
        missile.setInfo("missile_test", missileInfo);
        missile.shootingEntity = enemy;
        missile.shootingAircraft = enemy;
        MCH_EntityBullet round = new MCH_EntityBullet(world);
        MCH_WeaponInfo roundInfo = new MCH_WeaponInfo("round_test");
        round.setInfo("round_test", roundInfo);
        round.shootingEntity = gunner;
        round.shootingAircraft = gunner;
        MCH_Config.IgnoreBulletHitList = new ArrayList();
        assertFalse(gunner.isHostileMissile(missile, null));
        assertFalse(round.canBeCollidedEntity(missile));
        missileInfo.canBeIntercepted = true;
        assertTrue(gunner.isHostileMissile(missile, null));
        assertTrue(round.canBeCollidedEntity(missile));
        assertFalse("Interception must not require the attacking round flag", roundInfo.canBeIntercepted);
        enemy.setTeamName("wge_t1");
        assertFalse(round.canBeCollidedEntity(missile));
        enemy.setTeamName("");
        assertFalse(round.canBeCollidedEntity(missile));
    }

    private static final class TestWorld extends World {
        TestWorld() {
            super(null, "projectile_test", new WorldProviderSurface() {
                @Override protected void registerWorldChunkManager() { }
                @Override public void calculateInitialWeather() { }
            }, new WorldSettings(0, WorldSettings.GameType.SURVIVAL, false, false, WorldType.DEFAULT), new Profiler());
        }
        @Override protected IChunkProvider createChunkProvider() { return null; }
        @Override protected int func_152379_p() { return 0; }
        @Override public Entity getEntityByID(int id) { return null; }
        @Override public void calculateInitialSkylight() { }
        @Override public boolean handleMaterialAcceleration(AxisAlignedBB box, net.minecraft.block.material.Material material, Entity entity) { return false; }
    }

    private static final class TestRocket extends MCH_EntityRocket {
        MovingObjectPosition impact;
        TestRocket(World world) { super(world); }
        @Override protected void onImpact(MovingObjectPosition hit, float damage) { impact = hit; }
    }
}
