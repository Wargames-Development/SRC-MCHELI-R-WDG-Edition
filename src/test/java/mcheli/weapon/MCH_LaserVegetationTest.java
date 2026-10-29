package mcheli.weapon;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import mcheli.integration.wgmap.client.WGMapLaserClientBridge;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.multiplayer.ChunkProviderClient;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
import net.minecraft.launchwrapper.LaunchClassLoader;
import net.minecraft.profiler.Profiler;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraft.world.WorldProviderSurface;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraft.world.chunk.IChunkProvider;
import org.junit.Test;
import static org.junit.Assert.*;

/** Isolated real vanilla registries keep plant identity tests out of other tests' fixtures. */
public class MCH_LaserVegetationTest {
    @Test public void realVanillaPlantsAreSkippedByNativePredictionAndSharedValidation() throws Throwable {
        try (LaunchClassLoader loader = loader(false)) {
            ClassLoader previous = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(loader);
                loader.loadClass("cpw.mods.fml.common.Loader").getMethod("injectData", Object[].class).invoke(null,
                        new Object[] {new Object[] {"7", "10", "99", "99", "1.7.10", "9.05",
                                new File("build/laser-plant-test-runtime"), Collections.emptyList()}});
                loader.loadClass(getClass().getName()).getMethod("vanillaCases").invoke(null);
            } catch (InvocationTargetException failed) { throw failed.getCause(); }
            finally { Thread.currentThread().setContextClassLoader(previous); }
        }
    }

    @Test public void commonLaserPolicyLoadsWithClientAndOptionalModsUnavailable() throws Exception {
        try (LaunchClassLoader loader = loader(true)) {
            Class<?> type = loader.loadClass("mcheli.weapon.MCH_LaserTargeting");
            type.getDeclaredFields(); type.getDeclaredMethods();
        }
    }

    private static LaunchClassLoader loader(final boolean dedicated) {
        Set<URL> paths = new LinkedHashSet<URL>();
        for (ClassLoader current = MCH_LaserVegetationTest.class.getClassLoader(); current != null; current = current.getParent())
            if (current instanceof URLClassLoader) Collections.addAll(paths, ((URLClassLoader) current).getURLs());
        LaunchClassLoader loader = new LaunchClassLoader(paths.toArray(new URL[paths.size()])) {
            @Override public Class<?> findClass(String name) throws ClassNotFoundException {
                if (dedicated && (name.startsWith("net.minecraft.client.") || name.startsWith("net.minecraftforge.client.")
                        || name.startsWith("cpw.mods.fml.client.") || name.startsWith("org.lwjgl.")
                        || name.startsWith("com.wdg.wgmap."))) throw new ClassNotFoundException(name);
                return super.findClass(name);
            }
        };
        loader.addClassLoaderExclusion("org.junit.");
        return loader;
    }

    public static void vanillaCases() throws Exception {
        Bootstrap.func_151354_b();
        // This isolated registry bootstrap has no FML discovery phase. Model WGMap absent.
        java.lang.reflect.Field mods = cpw.mods.fml.common.Loader.class.getDeclaredField("namedMods");
        mods.setAccessible(true); mods.set(cpw.mods.fml.common.Loader.instance(), Collections.emptyMap());
        Block[] plants = {Blocks.tallgrass, Blocks.deadbush, Blocks.yellow_flower, Blocks.red_flower,
                Blocks.double_plant, Blocks.sapling, Blocks.brown_mushroom, Blocks.red_mushroom,
                Blocks.wheat, Blocks.carrots, Blocks.potatoes, Blocks.nether_wart, Blocks.reeds, Blocks.vine};
        Vec3 from = Vec3.createVectorHelper(0.5, 70.5, 0.5), to = Vec3.createVectorHelper(0.5, 63.5, 0.5);
        TestWorld world = new TestWorld();
        for (Block plant : plants) {
            assertTrue(MCH_LaserTargeting.ignoresPlant(plant));
            world.plant = plant;
            for (int metadata = 0; metadata < 16; metadata++) {
                world.metadata = metadata;
                assertNull(MCH_LaserTargeting.nativeBlockHit(world, 0, 66, 0, from, to));
                Vec3 solid = MCH_LaserTargeting.nativeBlockHit(world, 0, 64, 0, from, to);
                assertNotNull(solid); assertEquals(65.0, solid.yCoord, 0.000001);
                MovingObjectPosition initialPlant = new MovingObjectPosition(0, 66, 0, 1,
                        Vec3.createVectorHelper(0.5, 67, 0.5));
                assertTrue(WGMapLaserClientBridge.isIgnoredPlantHit(world, initialPlant));
                WGMapLaserClientBridge.Trace trace = WGMapLaserClientBridge.traceDetailed(world, from, to, initialPlant);
                assertTrue(trace.known); assertNotNull(trace.hit);
                assertEquals("plant aim must not truncate the trace", 65.0, trace.hit.hitVec.yCoord, 0.000001);
            }
        }
        for (Block solid : new Block[] {Blocks.grass, Blocks.dirt, Blocks.log, Blocks.log2,
                Blocks.leaves, Blocks.leaves2, Blocks.glass, Blocks.stone_slab, Blocks.fence,
                Blocks.brown_mushroom_block, Blocks.red_mushroom_block}) assertFalse(MCH_LaserTargeting.ignoresPlant(solid));
        assertFalse(MCH_LaserTargeting.ignoresPlant(null));
        assertFalse(MCH_LaserTargeting.ignoresPlant(new Block(Material.plants) { }));
        world.plant = Blocks.tallgrass; world.metadata = 1;
        Entity user = new mcheli.flare.MCH_EntityChaff(world, 0.5, 70, 0.5, 0, 0, 0);
        world.target = new mcheli.flare.MCH_EntityChaff(world, 0.5, 65.2, 0.5, 0, 0, 0) {
            @Override public float getCollisionBorderSize() { return 0.05F; }
        };
        MovingObjectPosition entity = MCH_LaserGuidanceSystem.laserRaytrace(user, null, from,
                Vec3.createVectorHelper(0, -1, 0), to);
        assertNotNull(entity); assertSame("plant aim must not exclude a nearer valid entity", world.target, entity.entityHit);
        WGMapLaserClientBridge.Trace entityTrace = WGMapLaserClientBridge.traceDetailed(world, from, to, entity);
        assertTrue(entityTrace.known); assertSame(entity, entityTrace.hit);
        world.plant = Blocks.leaves; world.metadata = 0;
        WGMapLaserClientBridge.Trace cover = WGMapLaserClientBridge.traceDetailed(world, from, to, entity);
        assertTrue(cover.known); assertNull(cover.hit.entityHit);
        assertEquals(67.0, cover.hit.hitVec.yCoord, 0.000001);
    }

    private static final class TestWorld extends World {
        Block plant = Blocks.tallgrass;
        int metadata;
        Entity target;
        final ChunkProviderClient chunks;
        TestWorld() {
            super(null, "laser_plants", new WorldProviderSurface() {
                @Override protected void registerWorldChunkManager() { }
                @Override public void calculateInitialWeather() { }
            }, new WorldSettings(0, WorldSettings.GameType.SURVIVAL, false, false, WorldType.DEFAULT), new Profiler());
            isRemote = true; chunks = new ChunkProviderClient(this); chunks.loadChunk(0, 0);
        }
        @Override public Block getBlock(int x, int y, int z) { return y == 65 || y == 66 ? plant : y == 64 ? Blocks.grass : Blocks.air; }
        @Override public int getBlockMetadata(int x, int y, int z) { return metadata; }
        @Override public IChunkProvider getChunkProvider() { return chunks; }
        @Override public java.util.List getEntitiesWithinAABBExcludingEntity(Entity user, net.minecraft.util.AxisAlignedBB bounds) {
            return target == null ? Collections.emptyList() : Collections.singletonList(target);
        }
        @Override protected IChunkProvider createChunkProvider() { return null; }
        @Override protected int func_152379_p() { return 0; }
        @Override public Entity getEntityByID(int id) { return null; }
        @Override public void calculateInitialSkylight() { }
    }
}
