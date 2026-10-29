package mcheli.aircraft;

import mcheli.MCH_MOD;
import mcheli.tank.MCH_EntityTank;
import mcheli.tank.MCH_TankInfo;
import mcheli.wrapper.W_Item;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.entity.player.PlayerCapabilities;
import net.minecraft.item.ItemStack;
import net.minecraft.profiler.Profiler;
import net.minecraft.world.*;
import net.minecraft.world.chunk.IChunkProvider;
import org.junit.Test;
import org.objectweb.asm.*;

import java.io.InputStream;

import static org.junit.Assert.*;

public class MCH_CountermeasureReloadTest {
    @Test
    public void serverReloadScenariosPassWithoutStartingForge() throws Exception {
        // As in the gunner tests, omit the mod startup initializer in the headless fixture.
        ClassLoader loader = new ClassLoader(getClass().getClassLoader()) {
            @Override protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (!name.startsWith("mcheli.")) return super.loadClass(name, resolve);
                Class<?> loaded = findLoadedClass(name);
                if (loaded == null) {
                    try (InputStream input = getParent().getResourceAsStream(name.replace('.', '/') + ".class")) {
                        if (input == null) throw new ClassNotFoundException(name);
                        ClassWriter writer = new ClassWriter(0);
                        new ClassReader(input).accept(new ClassVisitor(Opcodes.ASM5, writer) {
                            @Override public MethodVisitor visitMethod(int access, String method, String descriptor, String signature, String[] exceptions) {
                                return name.equals("mcheli.MCH_MOD") && method.equals("<clinit>") ? null
                                    : super.visitMethod(access, method, descriptor, signature, exceptions);
                            }
                        }, 0);
                        byte[] bytes = writer.toByteArray();
                        loaded = defineClass(name, bytes, 0, bytes.length);
                    } catch (java.io.IOException failure) { throw new ClassNotFoundException(name, failure); }
                }
                if (resolve) resolveClass(loaded);
                return loaded;
            }
        };
        Class<?> scenarios = loader.loadClass(Scenarios.class.getName());
        for (String scenario : new String[]{
                "onePackFillsBothStoresAndFullStoresNeverConsumeAnother",
                "enabledTypesAndLargeCapacitiesStillCostOnlyOnePack",
                "missingPackAndVehiclesWithoutCountermeasuresCannotReload",
                "creativeReloadDoesNotRequireOrConsumeAPack",
                "clientAirborneDestroyedAndUnauthorizedRequestsLeaveInventoryUntouched",
                "containerSupportsWeaponlessVehiclesAndRejectsUnknownOrClosedWindowActions"}) {
            Object fixture = scenarios.newInstance();
            try {
                scenarios.getMethod("setup").invoke(fixture);
                scenarios.getMethod(scenario).invoke(fixture);
            } finally {
                scenarios.getMethod("restoreItem").invoke(fixture);
            }
        }
    }

    public static class Scenarios {
        private W_Item previousItem;
        private TestTank aircraft;
        private EntityPlayerMP player;
        private TestWorld world;

        public void setup() throws Exception {
            previousItem = MCH_MOD.itemCountermeasureReload;
            MCH_MOD.itemCountermeasureReload = new W_Item();
            java.lang.reflect.Field singleton = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            singleton.setAccessible(true);
            sun.misc.Unsafe allocator = (sun.misc.Unsafe) singleton.get(null);
            world = new TestWorld();
            aircraft = (TestTank) allocator.allocateInstance(TestTank.class);
            aircraft.worldObj = world;
            aircraft.setEntityId(10);
            aircraft.info = new MCH_TankInfo("countermeasure_reload_test");
            aircraft.info.flare.types = new int[]{1};
            aircraft.info.chaff = aircraft.info.new Chaff();
            aircraft.info.flareCapacity = 36;
            aircraft.info.chaffCapacity = 128;
            aircraft.supplyAllowed = true;
            aircraft.inventory = new MCH_AircraftInventory(aircraft);
            player = (EntityPlayerMP) allocator.allocateInstance(EntityPlayerMP.class);
            player.setEntityId(1);
            player.worldObj = world;
            player.inventory = new InventoryPlayer(player);
            player.capabilities = new PlayerCapabilities();
            player.inventory.mainInventory[5] = new ItemStack(MCH_MOD.itemCountermeasureReload, 2);
            player.ridingEntity = aircraft;
            aircraft.riddenByEntity = player;
        }

        public void restoreItem() {
            MCH_MOD.itemCountermeasureReload = previousItem;
        }

        public void onePackFillsBothStoresAndFullStoresNeverConsumeAnother() {
            aircraft.setCountermeasureStateClient(5, 19);
            assertTrue(aircraft.rearmCountermeasures(player));
            assertEquals(36, aircraft.getRemainingFlarePairs());
            assertEquals(128, aircraft.getRemainingChaffPairs());
            assertEquals(1, player.inventory.mainInventory[5].stackSize);
            assertEquals(1, aircraft.syncs);
            assertTrue(player.inventory.inventoryChanged);
            assertFalse(aircraft.rearmCountermeasures(player));
            assertEquals(1, player.inventory.mainInventory[5].stackSize);
        }

        public void enabledTypesAndLargeCapacitiesStillCostOnlyOnePack() {
            for (int types = 1; types <= 3; types++) {
                aircraft.info.flare.types = (types & 1) != 0 ? new int[]{1} : new int[0];
                aircraft.info.chaff = (types & 2) != 0 ? aircraft.info.new Chaff() : null;
                aircraft.info.flareCapacity = aircraft.info.chaffCapacity = 10000;
                aircraft.setCountermeasureStateClient(0, 0);
                player.inventory.mainInventory[5] = new ItemStack(MCH_MOD.itemCountermeasureReload);
                assertTrue(aircraft.rearmCountermeasures(player));
                assertEquals((types & 1) != 0 ? 10000 : 0, aircraft.getRemainingFlarePairs());
                assertEquals((types & 2) != 0 ? 10000 : 0, aircraft.getRemainingChaffPairs());
                assertNull(player.inventory.mainInventory[5]);
            }
        }

        public void missingPackAndVehiclesWithoutCountermeasuresCannotReload() {
            player.inventory.mainInventory[5] = null;
            assertFalse(aircraft.rearmCountermeasures(player));
            player.inventory.mainInventory[5] = new ItemStack(MCH_MOD.itemCountermeasureReload);
            aircraft.info.flare.types = new int[0];
            aircraft.info.chaff = null;
            assertFalse(aircraft.rearmCountermeasures(player));
            assertEquals(1, player.inventory.mainInventory[5].stackSize);
            assertEquals(0, aircraft.syncs);
        }

        public void creativeReloadDoesNotRequireOrConsumeAPack() {
            player.capabilities.isCreativeMode = true;
            player.inventory.mainInventory[5] = null;
            assertTrue(aircraft.rearmCountermeasures(player));
            aircraft.setCountermeasureStateClient(0, 0);
            player.inventory.mainInventory[5] = new ItemStack(MCH_MOD.itemCountermeasureReload);
            assertTrue(aircraft.rearmCountermeasures(player));
            assertEquals(1, player.inventory.mainInventory[5].stackSize);
        }

        public void clientAirborneDestroyedAndUnauthorizedRequestsLeaveInventoryUntouched() {
            assertFalse(aircraft.rearmCountermeasures(null));
            world.isRemote = true;
            assertFalse(aircraft.rearmCountermeasures(player));
            world.isRemote = false;
            world.ground = false;
            assertFalse(aircraft.rearmCountermeasures(player));
            world.ground = true;
            aircraft.supplyAllowed = false;
            assertFalse(aircraft.rearmCountermeasures(player));
            aircraft.supplyAllowed = true;
            aircraft.isDead = true;
            assertFalse(aircraft.rearmCountermeasures(player));
            aircraft.isDead = false;
            aircraft.setDespawnCount(1);
            assertFalse(aircraft.rearmCountermeasures(player));
            aircraft.setDespawnCount(0);
            player.posX = 13;
            assertFalse(aircraft.rearmCountermeasures(player));
            player.posX = 0;
            aircraft.riddenByEntity = null;
            assertFalse(aircraft.rearmCountermeasures(player));
            assertEquals(2, player.inventory.mainInventory[5].stackSize);
            assertEquals(0, aircraft.syncs);
        }

        public void containerSupportsWeaponlessVehiclesAndRejectsUnknownOrClosedWindowActions() {
            MCH_AircraftGuiContainer container = new MCH_AircraftGuiContainer(player, aircraft);
            assertFalse(container.enchantItem(player, MCH_AircraftGuiContainer.RELOAD_COUNTERMEASURES));
            player.openContainer = container;
            assertFalse(container.enchantItem(player, 1));
            assertTrue(container.enchantItem(player, MCH_AircraftGuiContainer.RELOAD_COUNTERMEASURES));
            assertEquals(1, player.inventory.mainInventory[5].stackSize);
            assertFalse(container.enchantItem(player, MCH_AircraftGuiContainer.RELOAD_COUNTERMEASURES));
        }
    }

    private static final class TestTank extends MCH_EntityTank {
        MCH_TankInfo info;
        MCH_AircraftInventory inventory;
        boolean supplyAllowed;
        int syncs;
        TestTank(World world) { super(world); }
        @Override public MCH_AircraftInfo getAcInfo() { return info; }
        @Override public MCH_AircraftInventory getGuiInventory() { return inventory; }
        @Override public boolean canSupply() { return supplyAllowed && ((TestWorld) worldObj).ground; }
        @Override public void syncCountermeasureState() { syncs++; }
    }

    private static final class TestWorld extends World {
        boolean ground = true;
        TestWorld() {
            super(null, "reload_test", new WorldProviderSurface() {
                @Override protected void registerWorldChunkManager() { }
                @Override public void calculateInitialWeather() { }
            }, new WorldSettings(0, WorldSettings.GameType.SURVIVAL, false, false, WorldType.DEFAULT), new Profiler());
        }
        @Override protected IChunkProvider createChunkProvider() { return null; }
        @Override protected int func_152379_p() { return 0; }
        @Override public Entity getEntityByID(int id) { return null; }
        @Override public void calculateInitialSkylight() { }
    }
}
