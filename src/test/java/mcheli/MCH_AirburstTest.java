package mcheli;

import mcheli.aircraft.MCH_AircraftBoundingBox;
import mcheli.aircraft.MCH_BoundingBox;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.network.packets.PacketAirburstDistReset;
import mcheli.plane.MCP_EntityPlane;
import mcheli.weapon.*;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
import net.minecraft.launchwrapper.LaunchClassLoader;
import net.minecraft.profiler.Profiler;
import net.minecraft.util.*;
import net.minecraft.world.*;
import net.minecraft.world.chunk.IChunkProvider;
import org.junit.Test;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

public class MCH_AirburstTest {
    private TestWorld world;
    private static int nextEntityId = 100;

    @Test public void airburstScenariosPassWithRealVanillaRegistries() throws Throwable {
        Set<URL> paths = new LinkedHashSet<URL>();
        for (ClassLoader current = getClass().getClassLoader(); current != null; current = current.getParent())
            if (current instanceof URLClassLoader) Collections.addAll(paths, ((URLClassLoader)current).getURLs());
        try (LaunchClassLoader loader = new LaunchClassLoader(paths.toArray(new URL[paths.size()]))) {
            loader.addClassLoaderExclusion("org.junit.");
            ClassLoader previous = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(loader);
                loader.loadClass("cpw.mods.fml.common.Loader").getMethod("injectData", Object[].class).invoke(null,
                    new Object[]{new Object[]{"7", "10", "99", "99", "1.7.10", "9.05",
                        new File("build/airburst-test-runtime"), Collections.emptyList()}});
                loader.loadClass(getClass().getName()).getMethod("runScenarios").invoke(null);
            } catch (InvocationTargetException failed) { throw failed.getCause(); }
            finally { Thread.currentThread().setContextClassLoader(previous); }
        }
    }

    public static void runScenarios() throws Exception {
        Bootstrap.func_151354_b();
        Field mods = cpw.mods.fml.common.Loader.class.getDeclaredField("namedMods");
        mods.setAccessible(true); mods.set(cpw.mods.fml.common.Loader.instance(), Collections.emptyMap());
        for (String name : new String[]{"rangeFindsNearestVehicleAndStopsAtTerrain",
            "rangeIntersectsOffsetPartAndPreservesDamageMetadata",
            "blastBesideDistantWingDamagesParentOnceEvenWhenNativeQueryMissesIt",
            "blastRespectsCoverTypeMultiplierRadiusAndInactiveParts",
            "rotatedPartUsesOrientedSurfaceRatherThanItsEnvelope",
            "rotatedPartRayHitsOnlyItsActualShape",
            "blastAtVehicleOriginDoesNotSkipDamageForZeroDirection",
            "airburstOnlyExplosiveUsesItsOwnRadiusAndServerAuthority",
            "programmingPacketRequiresControlledVehicleCapableWeaponAndValidRange"}) {
            MCH_AirburstTest fixture = new MCH_AirburstTest();
            fixture.setup();
            MCH_AirburstTest.class.getMethod(name).invoke(fixture);
        }
    }

    public void setup() {
        new MCH_Config(".", "unused.cfg");
        MCH_Config.EnableWGCoreIntegration.prmBool = false;
        world = new TestWorld();
    }

    public void rangeFindsNearestVehicleAndStopsAtTerrain() throws Exception {
        TestPlane far = plane(100);
        Vec3 start = vec(0, 0, 0), end = vec(200, 0, 0);
        assertEquals(99, MCH_RayTracer.rayTraceAirburst(world, null, start, end).hitVec.xCoord, 0.00001);
        TestPlane near = plane(60);
        assertSame(near, MCH_RayTracer.rayTraceAirburst(world, null, start, end).entityHit);
        assertSame(far, MCH_RayTracer.rayTraceAirburst(world, near, start, end).entityHit);
        world.wallX = 50;
        MovingObjectPosition wall = MCH_RayTracer.rayTraceAirburst(world, null, start, end);
        assertNull(wall.entityHit);
        assertEquals(50, wall.hitVec.xCoord, 0.00001);
        assertEquals(0, start.xCoord, 0);
    }

    public void rangeIntersectsOffsetPartAndPreservesDamageMetadata() throws Exception {
        TestPlane plane = plane(100);
        plane.extraBoundingBox = new MCH_BoundingBox[]{new MCH_BoundingBox(100, 0, 20, 20, 2, 2, 1)};
        plane.lastBBDamageFactor = 0.4F; plane.lastBBName = "previous"; plane.lastBBIndex = 7;
        MovingObjectPosition hit = MCH_RayTracer.rayTraceAirburst(world, null, vec(0, 0, 20), vec(200, 0, 20));
        assertSame(plane, hit.entityHit);
        assertEquals(90, hit.hitVec.xCoord, 0.00001);
        assertEquals(0.4F, plane.lastBBDamageFactor, 0);
        assertEquals("previous", plane.lastBBName); assertEquals(7, plane.lastBBIndex);
        plane.isDead = true;
        assertNull(MCH_RayTracer.rayTraceAirburst(world, null, vec(0, 0, 20), vec(200, 0, 20)));
    }

    public void blastBesideDistantWingDamagesParentOnceEvenWhenNativeQueryMissesIt() throws Exception {
        TestPlane plane = plane(0);
        plane.extraBoundingBox = new MCH_BoundingBox[]{new MCH_BoundingBox(60, 0, 0, 20, 2, 2, 1)};
        blast(72, 3, 1);
        assertEquals(1, plane.hits); assertTrue(plane.damage > 1);
        plane.hits = 0;
        world.query.add(plane);
        blast(72, 3, 1);
        assertEquals(1, plane.hits);
    }

    public void blastRespectsCoverTypeMultiplierRadiusAndInactiveParts() throws Exception {
        TestPlane plane = plane(0);
        MCH_BoundingBox wing = new MCH_BoundingBox(60, 0, 0, 20, 2, 2, 1);
        plane.extraBoundingBox = new MCH_BoundingBox[]{wing};
        blast(72, 3, 1); float openDamage = plane.damage;
        world.wallX = 71;
        blast(72, 3, 1); assertTrue(plane.damage < openDamage);
        world.wallX = Integer.MIN_VALUE;
        blast(72, 3, 0); assertEquals(0, plane.damage, 0);
        plane.hits = 0;
        blast(80, 3, 1); assertEquals(0, plane.hits);
        wing.isERA = true; wing.eraActive = false;
        blast(72, 3, 1); assertEquals(0, plane.hits);
    }

    public void rotatedPartUsesOrientedSurfaceRatherThanItsEnvelope() throws Exception {
        TestPlane plane = plane(0);
        MCH_BoundingBox wing = new MCH_BoundingBox(10, 0, 0, 8, 2, 2, 1);
        double s = Math.sqrt(0.5);
        wing.axisX = vec(s, 0, s); wing.axisZ = vec(-s, 0, s);
        plane.extraBoundingBox = new MCH_BoundingBox[]{wing};
        Vec3 surface = MCH_Explosion.closestAircraftPoint(plane, vec(10, 0, 6));
        assertEquals(10 + 3 * s, surface.xCoord, 0.00001);
        assertEquals(5 * s, surface.zCoord, 0.00001);
    }

    public void rotatedPartRayHitsOnlyItsActualShape() throws Exception {
        TestPlane plane = plane(100);
        MCH_BoundingBox wing = new MCH_BoundingBox(100, 0, 20, 8, 2, 2, 1);
        double s = Math.sqrt(0.5);
        wing.axisX = vec(s, 0, s); wing.axisZ = vec(-s, 0, s);
        plane.extraBoundingBox = new MCH_BoundingBox[]{wing};
        MovingObjectPosition hit = MCH_RayTracer.rayTraceAirburst(world, null, vec(90, 0, 20), vec(110, 0, 20));
        assertNotNull(hit); assertEquals(100 - Math.sqrt(2), hit.hitVec.xCoord, 0.00001);
        assertNull(MCH_RayTracer.rayTraceAirburst(world, null, vec(103.4, -2, 16.6), vec(103.4, 2, 16.6)));
    }

    public void blastAtVehicleOriginDoesNotSkipDamageForZeroDirection() throws Exception {
        TestPlane plane = plane(0);
        blast(0, 3, 1);
        assertEquals(1, plane.hits); assertTrue(plane.damage > 0);
    }

    public void airburstOnlyExplosiveUsesItsOwnRadiusAndServerAuthority() throws Exception {
        TestBullet bullet = new TestBullet(world);
        MCH_WeaponInfo info = new MCH_WeaponInfo("airburst_test");
        info.explosion = 0; info.explosionAirburst = 3;
        bullet.setInfo(info.name, info); bullet.airburstDist = 20; bullet.motionX = 40;
        Method update = MCH_EntityBaseBullet.class.getDeclaredMethod("onUpdateAirburst");
        update.setAccessible(true); update.invoke(bullet);
        assertEquals(3, bullet.radius, 0); assertEquals(23, bullet.burstX, 0);
        assertTrue(bullet.isDead);
        world.isRemote = true;
        TestBullet client = new TestBullet(world);
        client.setInfo(info.name, info); client.airburstDist = 20; client.motionX = 40;
        update.invoke(client);
        assertEquals(0, client.radius, 0); assertFalse(client.isDead);
    }

    public void programmingPacketRequiresControlledVehicleCapableWeaponAndValidRange() throws Exception {
        TestPlane plane = plane(0);
        EntityPlayerMP player = allocate(EntityPlayerMP.class);
        player.worldObj = world;
        MCH_WeaponInfo info = new MCH_WeaponInfo("airburst_gun"); info.canAirburst = true;
        plane.weapon = new MCH_WeaponSet(new MCH_WeaponRocket(world, vec(0, 0, 0), 0, 0, info.name, info));
        new PacketAirburstDistReset(plane.getEntityId(), 100).handleServerSide(player);
        assertEquals(0, plane.weapon.getCurrentWeapon().airburstDist);
        player.ridingEntity = plane;
        new PacketAirburstDistReset(plane.getEntityId(), 100).handleServerSide(player);
        assertEquals(100, plane.weapon.getCurrentWeapon().airburstDist);
        new PacketAirburstDistReset(plane.getEntityId(), 3000).handleServerSide(player);
        assertEquals(100, plane.weapon.getCurrentWeapon().airburstDist);
        new PacketAirburstDistReset(plane.getEntityId(), -1).handleServerSide(player);
        assertEquals(100, plane.weapon.getCurrentWeapon().airburstDist);
        info.canAirburst = false;
        new PacketAirburstDistReset(plane.getEntityId(), 200).handleServerSide(player);
        assertEquals(100, plane.weapon.getCurrentWeapon().airburstDist);
        info.ahead = true;
        new PacketAirburstDistReset(plane.getEntityId(), 200).handleServerSide(player);
        assertEquals(200, plane.weapon.getCurrentWeapon().airburstDist);
        new PacketAirburstDistReset(plane.getEntityId(), 0).handleServerSide(player);
        assertEquals(0, plane.weapon.getCurrentWeapon().airburstDist);
    }

    private void blast(double x, float size, float planeMultiplier) {
        new MCH_Explosion(world, MCH_ExplosionParam.builder().x(x).y(0).z(0)
            .size(size).sizeBlock(0).damageVsPlane(planeMultiplier).build()).doExplosionA();
    }

    private TestPlane plane(double x) throws Exception {
        TestPlane plane = allocate(TestPlane.class);
        plane.worldObj = world; plane.posX = x; plane.setEntityId(nextEntityId++);
        plane.extraBoundingBox = new MCH_BoundingBox[0];
        Field bounds = Entity.class.getDeclaredField("boundingBox"); bounds.setAccessible(true);
        bounds.set(plane, AxisAlignedBB.getBoundingBox(x - 1, -1, -1, x + 1, 1, 1));
        bounds.set(plane, new TestBounds(plane));
        world.loadedEntityList.add(plane);
        return plane;
    }

    private static Vec3 vec(double x, double y, double z) { return Vec3.createVectorHelper(x, y, z); }
    private static <T> T allocate(Class<T> type) throws Exception {
        Field field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe"); field.setAccessible(true);
        return type.cast(((sun.misc.Unsafe)field.get(null)).allocateInstance(type));
    }

    private static final class TestBounds extends MCH_AircraftBoundingBox {
        TestBounds(MCH_EntityAircraft aircraft) { super(aircraft); }
    }

    private static final class TestPlane extends MCP_EntityPlane {
        int hits; float damage; MCH_WeaponSet weapon;
        TestPlane(World world) { super(world); }
        @Override public boolean attackEntityFrom(DamageSource source, float damage) {
            hits++; this.damage = damage; return true;
        }
        @Override public MCH_WeaponSet getCurrentWeapon(Entity user) { return weapon; }
    }

    private static final class TestBullet extends MCH_EntityBullet {
        float radius; double burstX;
        TestBullet(World world) { super(world); }
        @Override public MCH_Explosion.ExplosionResult newExplosion(double x, double y, double z, float size, float blocks, boolean water) {
            radius = size; burstX = x; return null;
        }
    }

    private static final class TestWorld extends World {
        int wallX = Integer.MIN_VALUE;
        final List<Entity> query = new ArrayList<Entity>();
        TestWorld() {
            super(null, "airburst_test", new WorldProviderSurface() {
                @Override protected void registerWorldChunkManager() { }
                @Override public void calculateInitialWeather() { }
            }, new WorldSettings(0, WorldSettings.GameType.SURVIVAL, false, false, WorldType.DEFAULT), new Profiler());
        }
        @Override public Block getBlock(int x, int y, int z) { return x == wallX ? Blocks.stone : Blocks.air; }
        @Override public int getBlockMetadata(int x, int y, int z) { return 0; }
        @Override public List getEntitiesWithinAABBExcludingEntity(Entity entity, AxisAlignedBB box) { return query; }
        @Override public Entity getEntityByID(int id) {
            for (Object entity : loadedEntityList) if (((Entity)entity).getEntityId() == id) return (Entity)entity;
            return null;
        }
        @Override protected IChunkProvider createChunkProvider() { return null; }
        @Override protected int func_152379_p() { return 0; }
        @Override public void calculateInitialSkylight() { }
    }
}
