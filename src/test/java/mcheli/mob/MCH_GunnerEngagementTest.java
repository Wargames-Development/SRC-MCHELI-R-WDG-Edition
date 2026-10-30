package mcheli.mob;

import com.mojang.authlib.GameProfile;
import cpw.mods.fml.common.event.FMLMissingMappingsEvent;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import mcheli.vehicle.MCH_EntityVehicle;
import mcheli.weapon.MCH_WeaponParam;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.profiler.Profiler;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraft.world.WorldProviderSurface;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraft.world.chunk.IChunkProvider;
import org.junit.Test;
import org.objectweb.asm.*;

import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.*;

/** Runs the real constructors, NBT and AA update with only Forge bootstrap/transport replaced. */
public class MCH_GunnerEngagementTest {
    @Test public void highRateCiwsUsesRealRoundsAmmoHeatAndCollisionCheckedLaunchPhases() throws Exception {
        for (int rounds : new int[]{4, 8}) {
            String name = rounds == 4 ? "ac_cram_m61a1" : "ac_hpj1130_30mm";
            Fixture f = new Fixture(name, "0,3,0.5,0,0,true,1,0,-360,360,-80,25");
            Object oldSet = f.call(f.turret, "getCurrentWeapon", new Class[]{Entity.class}, f.gunner);
            Object info = f.call(oldSet, "getInfo", new Class[0]);
            for (String[] setting : new String[][]{{"delay", "1"}, {"RoundsPerShot", String.valueOf(rounds)},
                {"round", "1000"}, {"maxammo", "1000"}, {"heatcount", "1"}, {"maxheatcount", "4000"},
                {"gravity", "0"}, {"accuracy", "0"}, {"FMURTracerGlow", "true"}}) f.load(info, setting[0], setting[1]);
            f.call(info, "checkData", new Class[0]);
            Object gun = f.type("mcheli.weapon.MCH_WeaponCreator").getMethod("createWeapon", World.class, String.class,
                Vec3.class, float.class, float.class, f.type("mcheli.weapon.MCH_IEntityLockChecker"), boolean.class)
                .invoke(null, f.world, name, Vec3.createVectorHelper(0, 0, 0), 0F, 0F, null, true);
            f.field(f.world, "isRemote").setBoolean(f.world, true);
            Object clientGun = f.type("mcheli.weapon.MCH_WeaponCreator").getMethod("createWeapon", World.class, String.class,
                Vec3.class, float.class, float.class, f.type("mcheli.weapon.MCH_IEntityLockChecker"), boolean.class)
                .invoke(null, f.world, name, Vec3.createVectorHelper(0, 0, 0), 0F, 0F, null, true);
            assertEquals("Player prediction must request one trigger per tick", 1, f.integer(clientGun, "interval"));
            f.field(f.world, "isRemote").setBoolean(f.world, false);
            f.field(gun, "canPlaySound").setBoolean(gun, false);
            Object ws = f.type("mcheli.weapon.MCH_WeaponSet").getConstructor(f.type("mcheli.weapon.MCH_WeaponBase")).newInstance(gun);
            f.call(ws, "setAmmoNum", new Class[]{int.class}, 1000);
            Object sets = java.lang.reflect.Array.newInstance(ws.getClass(), 1);
            java.lang.reflect.Array.set(sets, 0, ws);
            f.field(f.turret, "weapons").set(f.turret, sets);
            for (int tick = 0; tick < 20; ++tick) {
                if (tick > 0) f.call(ws, "update", new Class[]{Entity.class, boolean.class, boolean.class}, f.turret, true, false);
                Object intent = f.type("mcheli.weapon.MCH_WeaponParam").newInstance();
                f.field(intent, "user").set(intent, f.gunner);
                f.field(intent, "entity").set(intent, f.turret);
                f.call(intent, "setPosition", new Class[]{double.class, double.class, double.class}, 0D, 64D, 0D);
                assertTrue((Boolean) f.call(ws, "canUse", new Class[0]));
                assertTrue((Boolean) f.call(ws, "use", new Class[]{intent.getClass()}, intent));
                assertEquals(rounds, f.integer(intent, "roundsFired"));
                assertFalse("Server cooldown still blocks another trigger", (Boolean) f.call(ws, "canUse", new Class[0]));
            }
            assertEquals("Real rounds per second", rounds * 20, f.world.spawned.size());
            assertEquals(1000 - rounds * 20, f.call(ws, "getAmmoNum", new Class[0]));
            assertEquals(rounds * 20 * 2 - 19, f.integer(ws, "currentHeat"));
            for (int i = 0; i < rounds; ++i) {
                Entity bullet = f.world.spawned.get(i);
                assertEquals("mcheli.weapon.MCH_EntityBullet", bullet.getClass().getName());
                double z = bullet.posZ;
                double factor = ((Number) f.value(bullet, "accelerationFactor")).doubleValue();
                double speed = bullet.motionZ * factor;
                double fraction = (Math.floorMod(bullet.getEntityId(), rounds) + 1) / (double) rounds;
                f.call(bullet, "onUpdate", new Class[0]);
                assertEquals("First movement is phased without skipping collision", speed * fraction, bullet.posZ - z, 0.00001D);
                assertEquals(factor, ((Number) f.value(bullet, "accelerationFactor")).doubleValue(), 0D);
                z = bullet.posZ;
                f.call(bullet, "onUpdate", new Class[0]);
                assertEquals("Later movement retains full projectile speed", speed, bullet.posZ - z, 0.00001D);
            }
            f.call(ws, "update", new Class[]{Entity.class, boolean.class, boolean.class}, f.turret, true, false);
            f.call(ws, "setAmmoNum", new Class[]{int.class}, 2);
            Object last = f.type("mcheli.weapon.MCH_WeaponParam").newInstance();
            f.field(last, "user").set(last, f.gunner); f.field(last, "entity").set(last, f.turret);
            int before = f.world.spawned.size();
            assertTrue((Boolean) f.call(ws, "use", new Class[]{last.getClass()}, last));
            assertEquals("A partial belt cannot spawn free rounds", before + 2, f.world.spawned.size());
            assertEquals(0, f.call(ws, "getAmmoNum", new Class[0]));
            f.field(ws, "countReloadWait").setInt(ws, 0); f.field(ws, "countWait").setInt(ws, 0);
            f.call(ws, "setAmmoNum", new Class[]{int.class}, 100);
            f.world.rejectProjectileSpawns = true;
            last = f.type("mcheli.weapon.MCH_WeaponParam").newInstance();
            f.field(last, "user").set(last, f.gunner); f.field(last, "entity").set(last, f.turret);
            assertFalse((Boolean) f.call(ws, "use", new Class[]{last.getClass()}, last));
            assertEquals(100, f.call(ws, "getAmmoNum", new Class[0]));
        }
    }

    @Test public void installedFmurMountedGunMappingsLoadThroughTheWeaponManager() throws Exception {
        String root = System.getProperty("mcheli.test.minecraftRoot");
        org.junit.Assume.assumeNotNull(root);
        Fixture f = new Fixture(0);
        Class<?> manager = f.type("mcheli.weapon.MCH_WeaponInfoManager");
        java.nio.file.Path weapons = java.nio.file.Paths.get(root,
            "mods/MCHELI-R-WDG-Edition-1.1.8_X194/assets/mcheli/weapons");
        assertTrue((Boolean) manager.getMethod("load", String.class).invoke(null, weapons.toString()));
        int count = 0;
        int ciwsGlow = 0;
        for (Object info : (java.util.Collection) manager.getMethod("getValues").invoke(null)) {
            if ((Boolean) f.value(info, "fmurTracerGlow")) {
                String name = (String) f.value(info, "name");
                assertTrue(name.equals("ac_cram_m61a1") || name.equals("ac_hpj1130_30mm"));
                assertEquals("", f.value(info, "fmurBullet"));
                assertEquals(name.equals("ac_cram_m61a1") ? 4 : 8, f.integer(info, "roundsPerShot"));
                assertEquals(1, f.integer(info, "delay"));
                assertEquals(name.equals("ac_cram_m61a1") ? 22 : 28, f.integer(info, "timeFuse"));
                assertEquals(name.equals("ac_cram_m61a1") ? 1.0F : 1.25F,
                    ((Number) f.value(info, "fmurTracerScale")).floatValue(), 0F);
                ++ciwsGlow;
            }
            String bullet = (String) f.value(info, "fmurBullet");
            if (bullet.isEmpty()) continue;
            assertTrue(bullet.equals("50calammo") || bullet.equals("762x51100rdbox"));
            assertEquals(bullet + "Tracer", f.value(info, "fmurTracerBullet"));
            assertEquals(1, f.integer(info, "fmurTracerInterval"));
            assertEquals(bullet.equals("50calammo") ? 1F : ((Number) f.value(info, "power")).floatValue(),
                ((Number) f.value(info, "fmurBulletDamage")).floatValue(), 0F);
            ++count;
        }
        assertEquals(16, count);
        assertEquals("Only the two requested CIWS weapons opt in", 2, ciwsGlow);
        for (String nativeGun : new String[]{"ac_cram_m61a1", "ac_hpj1130_30mm", "ac_a10_gau8"}) {
            assertEquals("", f.value(manager.getMethod("get", String.class).invoke(null, nativeGun), "fmurBullet"));
        }
    }

    @Test public void fmurMountedGunsSpawnOneRealBulletAtTheMuzzleAndFailWithoutAmmo() throws Exception {
        org.junit.Assume.assumeNotNull(System.getProperty("mcheli.test.fmurClasses"));
        Fixture f = new Fixture(0);
        Class<?> bulletType = f.type("com.flansmod.common.guns.type.BulletType");
        Field singleton = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        singleton.setAccessible(true);
        Object ammo = ((sun.misc.Unsafe) singleton.get(null)).allocateInstance(bulletType);
        f.field(ammo, "shortName").set(ammo, "mounted_test_ammo");
        f.field(ammo, "hitBoxSize").setFloat(ammo, 0.1F);
        ((List) bulletType.getField("bullets").get(null)).add(ammo);
        Object tracerAmmo = ((sun.misc.Unsafe) singleton.get(null)).allocateInstance(bulletType);
        f.field(tracerAmmo, "shortName").set(tracerAmmo, "mounted_red_tracer");
        f.field(tracerAmmo, "hitBoxSize").setFloat(tracerAmmo, 0.1F);
        f.field(tracerAmmo, "tracer").setBoolean(tracerAmmo, true);
        ((List) bulletType.getField("bullets").get(null)).add(tracerAmmo);
        for (String kind : new String[]{"machinegun1", "machinegun2"}) {
            Object info = f.type("mcheli.weapon.MCH_WeaponInfo").getConstructor(String.class).newInstance("mounted_test");
            f.load(info, "type", kind);
            f.load(info, "fmurbullet", "mounted_test_ammo");
            f.load(info, "fmurbulletdamage", "5");
            f.load(info, "acceleration", "15");
            Object gun = f.type("mcheli.weapon.MCH_Weapon" + (kind.endsWith("1") ? "MachineGun1" : "MachineGun2"))
                .getConstructor(World.class, Vec3.class, float.class, float.class, String.class, info.getClass())
                .newInstance(f.world, Vec3.createVectorHelper(0, 0, 0), 0F, 0F, "mounted_test", info);
            f.field(gun, "acceleration").setFloat(gun, 15);
            f.field(gun, "canPlaySound").setBoolean(gun, false);
            Object intent = f.type("mcheli.weapon.MCH_WeaponParam").newInstance();
            f.field(intent, "entity").set(intent, f.turret);
            f.field(intent, "user").set(intent, f.gunner);
            f.call(intent, "setPosAndRot", new Class[]{double.class, double.class, double.class, float.class, float.class},
                3D, 66D, 7D, 90F, 0F);
            int before = f.world.spawned.size();
            assertTrue((Boolean) f.call(gun, "shot", new Class[]{intent.getClass()}, intent));
            assertEquals(before + 1, f.world.spawned.size());
            Entity bullet = f.world.spawned.get(before);
            assertEquals("com.flansmod.common.guns.EntityBullet", bullet.getClass().getName());
            assertSame(ammo, f.value(bullet, "type"));
            assertSame(f.gunner, f.value(bullet, "owner"));
            assertEquals(5F, ((Number) f.value(bullet, "damage")).floatValue(), 0F);
            assertEquals(3D, bullet.posX, 0D);
            assertEquals(66D, bullet.posY, 0D);
            assertEquals(7D, bullet.posZ, 0D);
            assertEquals(-15D, bullet.motionX, 0.001D);
            Object handler = f.type("mcheli.MCH_FMURUtil$MountedBulletHandler").newInstance();
            Class<?> eventType = f.type("com.flansmod.common.eventhandlers.BulletHitEvent");
            Class<?> hitType = f.type("com.flansmod.common.guns.raytracing.BulletHit");
            for (Entity hit : new Entity[]{(Entity) f.turret, (Entity) f.gunner, f.aircraft("wge_t2", 30, 64, 0)}) {
                Object intersection = f.type("com.flansmod.common.guns.raytracing.EntityHit")
                    .getConstructor(Entity.class, float.class).newInstance(hit, 0.1F);
                Object event = eventType.getConstructor(bullet.getClass(), hitType).newInstance(bullet, intersection);
                f.call(handler, "onBulletHit", new Class[]{eventType}, event);
                assertEquals(hit == f.turret || hit == f.gunner,
                    f.call(event, "isCanceled", new Class[0]));
            }
            bullet.getEntityData().removeTag("MCHRMountedAircraft");
            Object ownHit = f.type("com.flansmod.common.guns.raytracing.EntityHit")
                .getConstructor(Entity.class, float.class).newInstance(f.turret, 0.1F);
            Object ordinaryEvent = eventType.getConstructor(bullet.getClass(), hitType).newInstance(bullet, ownHit);
            f.call(handler, "onBulletHit", new Class[]{eventType}, ordinaryEvent);
            assertFalse("Unmarked FMUR bullets keep server behavior", (Boolean) f.call(ordinaryEvent, "isCanceled", new Class[0]));
            Field remote = World.class.getField("isRemote");
            remote.setAccessible(true);
            remote.setBoolean(f.world, true);
            Object clientEvent = eventType.getConstructor(bullet.getClass(), hitType).newInstance(bullet, ownHit);
            f.call(handler, "onBulletHit", new Class[]{eventType}, clientEvent);
            assertTrue("Client prediction must not consume the shot against its own hull",
                (Boolean) f.call(clientEvent, "isCanceled", new Class[0]));
            remote.setBoolean(f.world, false);
            f.load(info, "fmurbullet", "missing_ammo");
            assertFalse((Boolean) f.call(gun, "shot", new Class[]{intent.getClass()}, intent));
            assertEquals(before + 1, f.world.spawned.size());
            f.load(info, "fmurbullet", "mounted_test_ammo");
            f.load(info, "fmurtracerbullet", "mounted_red_tracer");
            f.load(info, "fmurtracerinterval", "5");
            for (int shot = 1; shot <= 10; ++shot) {
                int spawned = f.world.spawned.size();
                if (shot == 5) {
                    f.load(info, "fmurtracerbullet", "missing_tracer");
                    assertFalse((Boolean) f.call(gun, "shot", new Class[]{intent.getClass()}, intent));
                    assertEquals(4, f.integer(gun, "fmurShotsSinceTracer"));
                    f.load(info, "fmurtracerbullet", "mounted_red_tracer");
                    f.world.rejectProjectileSpawns = true;
                    assertFalse((Boolean) f.call(gun, "shot", new Class[]{intent.getClass()}, intent));
                    assertEquals(4, f.integer(gun, "fmurShotsSinceTracer"));
                    f.world.rejectProjectileSpawns = false;
                }
                assertTrue((Boolean) f.call(gun, "shot", new Class[]{intent.getClass()}, intent));
                assertEquals(spawned + 1, f.world.spawned.size());
                Entity round = f.world.spawned.get(spawned);
                assertSame("Only shots 5 and 10 are tracers", shot % 5 == 0 ? tracerAmmo : ammo, f.value(round, "type"));
                assertEquals(5F, ((Number) f.value(round, "damage")).floatValue(), 0F);
            }
            assertFalse("Do not mutate the shared ordinary ammo definition", (Boolean) f.value(ammo, "tracer"));
            f.load(info, "fmurtracerinterval", "1");
            for (int shot = 0; shot < 6; ++shot) {
                int beforeTracer = f.world.spawned.size();
                assertTrue((Boolean) f.call(gun, "shot", new Class[]{intent.getClass()}, intent));
                assertSame("Interval 1 makes every round a tracer", tracerAmmo, f.value(f.world.spawned.get(beforeTracer), "type"));
            }
        }
    }

    @Test public void harmRecognizesRadarAaWithEmptyPlayerOrGunnerCrew() throws Exception {
        Fixture f = new Fixture(0);
        Object guidance = f.type("mcheli.weapon.MCH_WeaponGuidanceSystem").newInstance();
        f.field(guidance, "antiRadiationMissile").setBoolean(guidance, true);
        Object missile = f.type("mcheli.weapon.MCH_EntityATMissile").getConstructor(World.class).newInstance(f.world);
        Method radiating = missile.getClass().getDeclaredMethod("isArmEmitterRadiating", Entity.class);
        radiating.setAccessible(true);
        for (String kind : new String[]{"vehicle.MCH_EntityVehicle", "tank.MCH_EntityTank"}) {
            Entity target = (Entity) f.type("mcheli." + kind).getConstructor(World.class).newInstance(f.world);
            Object info = f.type("mcheli." + (kind.startsWith("vehicle") ? "vehicle.MCH_VehicleInfo" : "tank.MCH_TankInfo"))
                .getConstructor(String.class).newInstance("radar_aa");
            f.load(info, "enableradar", "true");
            f.call(target, "setAcInfo", new Class[]{f.type("mcheli.aircraft.MCH_AircraftInfo")}, info);
            assertFalse("Loaded radar capability must not require the unused legacy flag", (Boolean) f.value(info, "hasAARadar"));
            for (Entity crew : new Entity[]{null, TestPlayer.create(f.world), (Entity) f.gunner}) {
                target.riddenByEntity = crew;
                assertTrue((Boolean) f.call(target, "isArmEmitterRadiating", new Class[0]));
                assertTrue((Boolean) f.call(guidance, "canLockEntity", new Class[]{Entity.class}, target));
                assertTrue("In-flight ARM homing must use the same emitter state", (Boolean) radiating.invoke(missile, target));
                f.call(target, "setRadarEnabledRuntime", new Class[]{boolean.class}, false);
                assertFalse((Boolean) f.call(guidance, "canLockEntity", new Class[]{Entity.class}, target));
                assertFalse((Boolean) radiating.invoke(missile, target));
                f.call(target, "setRadarEnabledRuntime", new Class[]{boolean.class}, true);
            }
            f.call(target, "setDespawnCount", new Class[]{int.class}, 1);
            assertFalse((Boolean) f.call(guidance, "canLockEntity", new Class[]{Entity.class}, target));
            f.call(target, "setDespawnCount", new Class[]{int.class}, 0);
            target.isDead = true;
            assertFalse((Boolean) f.call(guidance, "canLockEntity", new Class[]{Entity.class}, target));
        }
    }

    @Test public void harmRejectsReceiveOnlyGroundVehiclesAndAirborneTargets() throws Exception {
        Fixture f = new Fixture(0);
        Object guidance = f.type("mcheli.weapon.MCH_WeaponGuidanceSystem").newInstance();
        f.field(guidance, "antiRadiationMissile").setBoolean(guidance, true);
        Object info = f.call(f.turret, "getAcInfo", new Class[0]);
        f.load(info, "enablerwr", "true");
        assertFalse((Boolean) f.call(guidance, "canLockEntity", new Class[]{Entity.class}, f.turret));
        Entity plane = (Entity) f.type("mcheli.plane.MCP_EntityPlane").getConstructor(World.class).newInstance(f.world);
        Object planeInfo = f.type("mcheli.plane.MCP_PlaneInfo").getConstructor(String.class).newInstance("radar_plane");
        f.load(planeInfo, "enableradar", "true");
        f.call(plane, "setAcInfo", new Class[]{f.type("mcheli.aircraft.MCH_AircraftInfo")}, planeInfo);
        assertFalse("The ground-radar crew exception must not change aircraft emission rules",
            (Boolean) f.call(plane, "isArmEmitterRadiating", new Class[0]));
        plane.riddenByEntity = (Entity) f.gunner;
        assertTrue((Boolean) f.call(plane, "isArmEmitterRadiating", new Class[0]));
        assertFalse("Ground HARM must still reject aircraft", (Boolean) f.call(guidance, "canLockEntity", new Class[]{Entity.class}, plane));
        assertFalse((Boolean) f.call(guidance, "canLockEntity", new Class[]{Entity.class}, TestPlayer.create(f.world)));
    }

    @Test public void spawnedPlaneUsesPreciseSnapshotPitchWhileVanillaPitchHasNotChanged() throws Exception {
        // Reuse the existing headless aircraft bootstrap to exercise spawn and rendering together.
        Fixture f = new Fixture(0);
        Entity server = (Entity) f.type("mcheli.plane.MCP_EntityPlane").getConstructor(World.class).newInstance(f.world);
        Entity client = (Entity) server.getClass().getConstructor(World.class).newInstance(f.world);
        Object info = f.type("mcheli.plane.MCP_PlaneInfo").getConstructor(String.class).newInstance("test_plane");
        Class<?> aircraftType = f.type("mcheli.aircraft.MCH_EntityAircraft");
        Class<?> infoType = f.type("mcheli.aircraft.MCH_AircraftInfo");
        ((Map) f.type("mcheli.plane.MCP_PlaneInfoManager").getField("map").get(null)).put("test_plane", info);
        f.call(server, "setAcInfo", new Class[]{infoType}, info);
        f.call(server, "setTypeName", new Class[]{String.class}, "test_plane");
        assertNotEquals(server.getUniqueID(), client.getUniqueID());
        ByteBuf spawn = Unpooled.buffer();
        try {
            f.call(server, "writeSpawnData", new Class[]{ByteBuf.class}, spawn);
            f.call(client, "readSpawnData", new Class[]{ByteBuf.class}, spawn);
            assertEquals("The precise-angle guard requires the server's identity", server.getUniqueID(), client.getUniqueID());
        } finally {
            spawn.release();
        }
        assertSame(info, f.call(client, "getAcInfo", new Class[0]));
        client.setEntityId(server.getEntityId());
        Class<?> poseType = f.type("mcheli.render.MCH_RenderFarVehicle$SmoothedPose");
        java.lang.reflect.Constructor<?> poseConstructor = poseType.getDeclaredConstructor();
        poseConstructor.setAccessible(true);
        Object pose = poseConstructor.newInstance();
        Class<?> definitionType = f.type("mcheli.render.MCH_RenderFarVehicle$RenderDefinition");
        java.lang.reflect.Constructor<?> definitionConstructor = definitionType.getDeclaredConstructor(
            infoType, String.class, net.minecraft.util.ResourceLocation.class);
        definitionConstructor.setAccessible(true);
        Object definition = definitionConstructor.newInstance(info, "planes", null);
        Class<?> snapshotType = f.type("mcheli.MCH_EntityInfo");
        Method observe = poseType.getDeclaredMethod("observe", snapshotType, long.class);
        observe.setAccessible(true);
        Method update = poseType.getDeclaredMethod("updateFromAircraft", aircraftType, definitionType,
            long.class, long.class, int.class, float.class);
        update.setAccessible(true);
        Object first = f.rotationSnapshot(server, 0, 1000);
        observe.invoke(pose, first, 1000L);
        update.invoke(pose, client, definition, 1000L, 0L, 1, 0.5F);
        Object next = f.rotationSnapshot(server, 1, 1100);
        observe.invoke(pose, next, 1100L);
        update.invoke(pose, client, definition, 1100L, 100_000_000L, 2, 0.5F);
        update.invoke(pose, client, definition, 1150L, 150_000_000L, 3, 0.5F);
        assertEquals(0.0F, client.rotationPitch, 0.0F);
        assertEquals("Render pitch advances even below the vanilla look threshold", 0.5F,
            ((Number) f.value(pose, "pitch")).floatValue(), 0.0001F);
    }

    @Test public void legacyOrIncompleteAircraftSpawnIdentityKeepsTheLocalUuid() throws Exception {
        Fixture f = new Fixture(0);
        for (int trailerBytes : new int[]{0, 4, 12, 19}) {
            Entity client = (Entity) f.type(TestVehicle.class.getName()).getConstructor(World.class).newInstance(f.world);
            UUID localUuid = client.getUniqueID();
            ByteBuf legacy = Unpooled.buffer();
            try {
                legacy.writeFloat(2).writeFloat(3).writeFloat(4).writeShort(0);
                if (trailerBytes > 0) legacy.writeInt(0x41494431).writeZero(trailerBytes - 4);
                f.call(client, "readSpawnData", new Class[]{ByteBuf.class}, legacy);
                assertEquals(localUuid, client.getUniqueID());
                assertEquals(2, client.height, 0);
                assertEquals(3, client.width, 0);
            } finally {
                legacy.release();
            }
        }
    }

    @Test public void shiftRightClickOnTurretReturnsOneEggAndFreesItForReloading() throws Exception {
        Fixture f = new Fixture(0);
        TestPlayer player = TestPlayer.create(f.world);
        f.field(f.gunner, "ownerUUID").set(f.gunner, player.getUniqueID().toString());
        f.type("mcheli.MCH_MOD").getField("itemSpawnGunnerAA").set(null,
            f.type("mcheli.mob.MCH_ItemSpawnGunner").newInstance());
        Object weapon = f.call(f.turret, "getCurrentWeapon", new Class[]{Entity.class}, f.gunner);
        int ammo = ((Number) f.call(weapon, "getAmmoNum", new Class[0])).intValue();
        player.setSneaking(true);
        assertTrue((Boolean) f.call(f.turret, "interactFirst", new Class[]{EntityPlayer.class}, player));
        assertTrue(((Entity) f.gunner).isDead);
        assertNull(((Entity) f.gunner).ridingEntity);
        assertNull(((Entity) f.turret).riddenByEntity);
        assertFalse(((Entity) f.turret).isDead);
        assertEquals(ammo, ((Number) f.call(weapon, "getAmmoNum", new Class[0])).intValue());
        assertEquals(1, f.world.droppedEggs);
        assertFalse((Boolean) f.call(f.gunner, "interactFirst", new Class[]{EntityPlayer.class}, player));
        assertEquals("Repeated interaction must not duplicate the egg", 1, f.world.droppedEggs);
        f.load(f.call(f.turret, "getAcInfo", new Class[0]), "inventorysize", "1");
        f.call(f.turret, "interactFirst", new Class[]{EntityPlayer.class}, player);
        assertEquals("An empty turret keeps its ordinary inventory interaction", 1, player.inventoriesOpened);
    }

    @Test public void removalRequiresSneakingAndOwnerTeamOrCreativePermission() throws Exception {
        for (String access : new String[]{"owner", "team", "enemy", "creative", "creativePlaced"}) {
            Fixture f = new Fixture(0);
            TestPlayer player = TestPlayer.create(f.world);
            if (access.equals("owner") || access.equals("creativePlaced"))
                f.field(f.gunner, "ownerUUID").set(f.gunner, player.getUniqueID().toString());
            if (access.equals("team")) f.world.getScoreboard().func_151392_a(player.getCommandSenderName(), "wge_t1");
            if (access.equals("enemy")) f.world.getScoreboard().func_151392_a(player.getCommandSenderName(), "wge_t2");
            player.capabilities.isCreativeMode = access.equals("creative");
            f.field(f.gunner, "isCreative").setBoolean(f.gunner, access.equals("creativePlaced"));
            assertFalse((Boolean) f.call(f.gunner, "interactFirst", new Class[]{EntityPlayer.class}, player));
            assertFalse(((Entity) f.gunner).isDead);
            player.setSneaking(true);
            assertTrue((Boolean) f.call(f.turret, "interactFirst", new Class[]{EntityPlayer.class}, player));
            boolean allowed = !access.equals("enemy") && !access.equals("creativePlaced");
            assertEquals(access, allowed, ((Entity) f.gunner).isDead);
            assertEquals("Occupied turret must not open its inventory on the removal click", 0, player.inventoriesOpened);
        }
    }

    @Test public void shiftRightClickAlsoRemovesAnOccupiedPassengerSeatGunner() throws Exception {
        Fixture f = new Fixture(0);
        Object seat = f.type("mcheli.aircraft.MCH_EntitySeat").getConstructor(World.class).newInstance(f.world);
        f.call(seat, "setParent", new Class[]{f.type("mcheli.aircraft.MCH_EntityAircraft")}, f.turret);
        ((Entity) f.turret).riddenByEntity = null;
        ((Entity) seat).riddenByEntity = (Entity) f.gunner;
        ((Entity) f.gunner).ridingEntity = (Entity) seat;
        TestPlayer player = TestPlayer.create(f.world);
        player.capabilities.isCreativeMode = true;
        player.setSneaking(true);
        assertTrue((Boolean) f.call(seat, "interactFirst", new Class[]{EntityPlayer.class}, player));
        assertNull(((Entity) seat).riddenByEntity);
        assertNull(((Entity) f.gunner).ridingEntity);
        assertTrue(((Entity) f.gunner).isDead);
    }

    @Test public void missileBeyondTheRoundsLifetimeDoesNotStarveAReachableAircraft() throws Exception {
        Fixture f = new Fixture(0);
        Object weapon = f.call(f.turret, "getCurrentWeapon", new Class[]{Entity.class}, f.gunner);
        Object info = f.call(weapon, "getInfo", new Class[0]);
        f.load(info, "timefuse", "22");
        Entity aircraft = f.aircraft("wge_t2", 0, 80, 100);
        Entity missile = f.projectile("mcheli.weapon.MCH_EntityRocket", "wge_t2", 0, 80, 180);
        missile.motionZ = 16;
        f.weaponTick();
        assertSame(aircraft, f.target());
        assertEquals(1, f.integer(f.gunner, "rejectedIntercept"));
        assertTrue(f.shots() > 0);
    }

    @Test public void oneEggUsesTheMountedWeaponsBalancedProfileAndItsRangeAndReaction() throws Exception {
        for (String weaponName : new String[]{"ac_cram_m61a1", "ac_hpj1130_30mm"}) {
            Fixture f = new Fixture(weaponName, "0,3,0.5,0,0,true,1,0,-360,360,-80,25");
            Object base = f.loadProfile("gunner_aa_default");
            f.loadProfile("gunner_aa_cram");
            f.loadProfile("gunner_aa_hpj1130");
            f.call(f.gunner, "applyProfile", new Class[]{f.type("mcheli.mob.MCH_GunnerInfo")}, base);
            String model = weaponName.equals("ac_cram_m61a1") ? "gunner_aa_cram" : "gunner_aa_hpj1130";
            float range = model.equals("gunner_aa_cram") ? 320 : 420;
            int reaction = model.equals("gunner_aa_cram") ? 4 : 3;
            assertEquals(model, f.value(f.resolvedProfile(), "name"));
            assertEquals(range, ((Number) f.value(f.resolvedProfile(), "detectionRange")).floatValue(), 0);
            assertEquals("wge_t1", f.call(f.gunner, "getTeamName", new Class[0]));
            Entity outside = f.aircraft("wge_t2", 0, 67.5, range + 30);
            for (int tick = 0; tick < 20; ++tick) f.weaponTick();
            assertNull(f.target());
            assertEquals(0, f.shots());
            outside.isDead = true;
            Entity inside = f.aircraft("wge_t2", 0, 67.5, range - 20);
            for (int tick = 0; tick < reaction; ++tick) f.weaponTick();
            assertEquals("Balanced profiles must not fire immediately", 0, f.shots());
            for (int tick = 0; tick < 40; ++tick) f.weaponTick();
            assertSame(inside, f.target());
            assertTrue("Balanced profile must still acquire and fire", f.shots() > 0);
            assertTrue("Burst pauses must leave gaps in firing", f.shots() < 40);
            // Legacy saves without a named profile also pick up the model binding.
            f.call(f.gunner, "applyProfile", new Class[]{f.type("mcheli.mob.MCH_GunnerInfo")}, new Object[]{null});
            assertEquals(model, f.value(f.resolvedProfile(), "name"));
        }
    }

    @Test public void balancedGunsFireAtFastIncomingMissilesBeforeTheFormerRangeBoundary() throws Exception {
        for (String weaponName : new String[]{"ac_cram_m61a1", "ac_hpj1130_30mm"}) {
            Fixture f = new Fixture(weaponName, "0,3,0.5,0,0,true,1,0,-360,360,-80,25");
            Object base = f.loadProfile("gunner_aa_default");
            f.loadProfile("gunner_aa_cram");
            f.loadProfile("gunner_aa_hpj1130");
            f.call(f.gunner, "applyProfile", new Class[]{f.type("mcheli.mob.MCH_GunnerInfo")}, base);
            boolean cram = weaponName.equals("ac_cram_m61a1");
            double range = cram ? 320 : 420;
            Object weapon = f.call(f.turret, "getCurrentWeapon", new Class[]{Entity.class}, f.gunner);
            f.load(f.call(weapon, "getInfo", new Class[0]), "timefuse", cram ? "22" : "28");
            Entity missile = f.projectile("mcheli.weapon.MCH_EntityRocket", "wge_t2", 0, 67.5, range + 20);
            missile.motionZ = -12;
            int shots = f.shots();
            for (int tick = 0; tick < 30 && f.shots() == shots; ++tick) {
                f.weaponTick();
                if (f.shots() == shots) missile.setPosition(missile.posX, missile.posY, missile.posZ + missile.motionZ);
            }
            assertSame(weaponName, missile, f.target());
            assertTrue(weaponName + " must fire before impact: " + f.status(), f.shots() > shots);
            assertTrue(weaponName + " must fire while still beyond its previous range", missile.posZ > (cram ? 180 : 240));
        }
    }

    @Test public void explicitCustomProfileAndMissingModelProfilesKeepTheirOwnTuning() throws Exception {
        Fixture f = new Fixture("ac_cram_m61a1", "0,3,0.5,0,0,true,1,0,-360,360,-80,25");
        Object base = f.loadProfile("gunner_aa_default");
        f.call(f.gunner, "applyProfile", new Class[]{f.type("mcheli.mob.MCH_GunnerInfo")}, base);
        assertSame("Missing mapped profile safely falls back to base", base, f.resolvedProfile());
        f.loadProfile("gunner_aa_cram");
        Object custom = f.type("mcheli.mob.MCH_GunnerInfo").getConstructor(String.class).newInstance("test_custom");
        f.load(custom, "detectionrange", "90");
        f.registerProfile(custom);
        f.call(f.gunner, "applyProfile", new Class[]{f.type("mcheli.mob.MCH_GunnerInfo")}, custom);
        assertSame(custom, f.resolvedProfile());
    }

    @Test public void vanillaVisibilityTraceMutatesItsStartVector() {
        TestWorld world = new TestWorld();
        Vec3 start = Vec3.createVectorHelper(0.25, 65.5, 0.25);
        assertNull(world.rayTraceBlocks(start, Vec3.createVectorHelper(80.5, 100.5, 120.5)));
        assertTrue("The fixture must exercise vanilla's in-place start-vector mutation", start.xCoord > 1);
    }

    @Test public void actualVisibilityTraceAllowsSustainedFireAtANearbyHoveringHelicopter() throws Exception {
        Fixture f = new Fixture(0);
        Entity aircraft = f.aircraft("wge_t2", -50, 110, 80);
        for (int tick = 0; tick < 60; ++tick) f.weaponTick();
        assertSame(aircraft, f.target());
        assertTrue("Native visibility traces must not move the aiming origin toward the target: " + f.status(), f.shots() > 50);
        assertTrue(Math.abs(((Number) f.value(f.gunner, "fireYawError")).floatValue()) <= 1.5F);
        assertTrue(Math.abs(((Number) f.value(f.gunner, "firePitchError")).floatValue()) <= 1.5F);
    }

    @Test public void actualTerrainBeyondVanillaRayStepLimitStillBlocksAcquisition() throws Exception {
        Fixture f = new Fixture(0);
        f.world.wallZ = 250;
        f.aircraft("wge_t2", 0, 100, 350);
        for (int tick = 0; tick < 20; ++tick) f.weaponTick();
        assertNull("A wall beyond vanilla's single-ray step budget must still block the target", f.target());
        assertEquals(0, f.shots());
        assertTrue(f.integer(f.gunner, "rejectedSight") > 0);
    }

    @Test public void visibilityDoesNotReadTerrainWhenItsChunksAreMissing() throws Exception {
        Fixture f = new Fixture(0);
        f.aircraft("wge_t2", 0, 100, 350);
        f.world.chunksLoaded = false;
        int before = f.world.blockQueries;
        f.weaponTick();
        assertNull(f.target());
        assertEquals(0, f.shots());
        assertEquals("No ray or chunk load should be attempted across missing terrain", before, f.world.blockQueries);
    }

    @Test public void onlyRemovedSpawnEggMappingsAreDiscardedWhenLoadingAnOldWorld() throws Exception {
        Fixture f = new Fixture(0);
        final List<FMLMissingMappingsEvent.MissingMapping> mappings = new ArrayList<FMLMissingMappingsEvent.MissingMapping>();
        for (String name : new String[]{"spawn_gunner_vs_monster", "spawn_gunner_vs_player", "gunner_enemy",
            "gunner_friendly_stupid", "gunner_enemy_stupid", "spawn_npc", "gunner_aa", "unrelated_item"}) {
            mappings.add(new FMLMissingMappingsEvent.MissingMapping("\u0002mcheli:" + name, 1000 + mappings.size()));
        }
        mappings.add(new FMLMissingMappingsEvent.MissingMapping("\u0001mcheli:gunner_enemy", 1007));
        mappings.add(new FMLMissingMappingsEvent.MissingMapping("\u0002othermod:gunner_enemy", 1008));
        mappings.add(new FMLMissingMappingsEvent.MissingMapping("\u0001mcheli:spawn_npc", 1009));
        mappings.add(new FMLMissingMappingsEvent.MissingMapping("\u0002othermod:spawn_npc", 1010));
        FMLMissingMappingsEvent event = new FMLMissingMappingsEvent(null) {
            @Override public List<MissingMapping> get() { return mappings; }
        };
        Object mod = f.type("mcheli.MCH_MOD").newInstance();
        f.call(mod, "onMissingMappings", new Class[]{FMLMissingMappingsEvent.class}, event);
        for (int index = 0; index < mappings.size(); ++index) {
            assertEquals(index < 6 ? FMLMissingMappingsEvent.Action.IGNORE : FMLMissingMappingsEvent.Action.DEFAULT,
                mappings.get(index).getAction());
        }
    }

    @Test public void freshTurretEngagesApproachingEnemyBeforeItPasses() throws Exception {
        Fixture f = new Fixture(0);
        Entity aircraft = f.aircraft("wge_t2", 0, 80, 400);
        aircraft.motionZ = -3;
        for (int tick = 0; tick < 30; ++tick) {
            f.update();
            aircraft.setPosition(0, 80, 400 - tick * 3);
        }
        assertTrue(f.enabled());
        assertTrue("Fresh gunner must fire during approach without an autosave", f.shots() > 0);
        assertSame(aircraft, f.target());
    }

    @Test public void nativeMountTracksAcrossCompassDirectionsOnARotatedTurret() throws Exception {
        Fixture f = new Fixture(0);
        ((Entity) f.turret).rotationYaw = ((Entity) f.turret).prevRotationYaw = 73;
        Entity aircraft = f.aircraft("wge_t2", 0, 100, 350);
        for (int direction = 0; direction < 8; ++direction) {
            double bearing = direction * Math.PI / 4;
            aircraft.setPosition(Math.sin(bearing) * 350, 100, Math.cos(bearing) * 350);
            int before = f.shots();
            for (int tick = 0; tick < 30; ++tick) {
                f.call(f.turret, "updateWeaponsRotation", new Class[0]);
                f.update();
            }
            assertTrue("Mounted gunner must fire at bearing " + direction + ": " + f.status(), f.shots() > before);
        }
    }

    @Test public void bothCiwsMountsEngageHoveringAircraftAndSwitchToAnIncomingMissile() throws Exception {
        // Native mount geometry and limits from the supplied C-RAM and HPJ 1130 definitions.
        for (String mount : new String[]{"0,3.134,0.54,0,0,true,1,0,-360,360,-80,25",
            "0.008515,2.774,0.278,0,0,true,1,0,-360,360,-88,12"}) {
            Fixture f = new Fixture(mount);
            ((Entity) f.turret).rotationYaw = ((Entity) f.turret).prevRotationYaw = 73;
            Object info = f.call(f.turret, "getAcInfo", new Class[0]);
            f.field(info, "minRotationPitch").setFloat(info, -85);
            f.field(info, "maxRotationPitch").setFloat(info, 10);
            Entity aircraft = f.aircraft("wge_t2", 0, 100, 350);
            for (int direction = 0; direction < 8; ++direction) {
                double bearing = direction * Math.PI / 4;
                aircraft.setPosition(Math.sin(bearing) * 350, 100, Math.cos(bearing) * 350);
                int before = f.shots();
                for (int tick = 0; tick < 40; ++tick) f.weaponTick();
                assertSame(aircraft, f.target());
                assertTrue("Hover must produce sustained fire for mount " + mount + ", bearing " + direction,
                    f.shots() - before >= 25);
            }
            Entity missile = f.projectile("mcheli.weapon.MCH_EntityRocket", "wge_t2", 100, 100, 350);
            missile.motionZ = -3;
            int before = f.shots();
            for (int tick = 0; tick < 40; ++tick) {
                missile.setPosition(100, 100, 350 - tick * 3);
                f.weaponTick();
                assertSame(missile, f.target());
            }
            assertTrue("Missile must receive sustained firing intent for mount " + mount, f.shots() - before >= 25);
        }
    }

    @Test public void nativeMountSustainsFireAcrossAMovingAircraftApproach() throws Exception {
        Fixture f = new Fixture(0);
        Entity aircraft = f.aircraft("wge_t2", -300, 100, 350);
        Field movementTick = f.type("mcheli.aircraft.MCH_EntityAircraft").getDeclaredField("lastPhysicsWorldTick");
        movementTick.setAccessible(true);
        for (int tick = 0; tick < 200; ++tick) {
            aircraft.prevPosX = aircraft.posX;
            aircraft.prevPosY = aircraft.posY;
            aircraft.prevPosZ = aircraft.posZ;
            aircraft.motionX = 3;
            aircraft.setPosition(aircraft.posX + 3, 100, 350);
            movementTick.setLong(aircraft, f.world.getTotalWorldTime());
            f.call(f.turret, "updateWeaponsRotation", new Class[0]);
            f.update();
            assertSame(aircraft, f.target());
        }
        assertTrue("Tracking should sustain aligned firing intent, not just produce one chance shot", f.shots() > 190);
    }

    @Test public void barrelFeedbackConvergesEvenWhenLookAndBarrelDisagree() throws Exception {
        Fixture f = new Fixture(0);
        f.aircraft("wge_t2", -350, 140, 100);
        Object weapon = f.call(f.turret, "getCurrentWeapon", new Class[]{Entity.class}, f.gunner);
        for (int tick = 0; tick < 25; ++tick) {
            // Reproduce a look/barrel disagreement between controller calls.
            ((Entity) f.gunner).rotationYaw = 0;
            ((Entity) f.gunner).rotationPitch = 0;
            float beforeYaw = ((Number) f.value(weapon, "rotationYaw")).floatValue();
            float beforePitch = ((Number) f.value(weapon, "rotationPitch")).floatValue();
            f.update();
            float afterYaw = ((Number) f.value(weapon, "rotationYaw")).floatValue();
            float afterPitch = ((Number) f.value(weapon, "rotationPitch")).floatValue();
            assertTrue(Math.abs(net.minecraft.util.MathHelper.wrapAngleTo180_float(afterYaw - beforeYaw)) <= 15.001F);
            assertTrue(Math.abs(afterPitch - beforePitch) <= 15.001F);
        }
        assertTrue("Actual barrel must converge instead of repeating the first look step: " + f.status(), f.shots() > 0);
    }

    @Test public void gunPitchLimitsApplyIndependentlyOfVehicleBodyPitchLimits() throws Exception {
        Fixture f = new Fixture(0);
        Object info = f.call(f.turret, "getAcInfo", new Class[0]);
        f.field(info, "minRotationPitch").setFloat(info, -5);
        f.field(info, "maxRotationPitch").setFloat(info, 5);
        Entity missile = f.projectile("mcheli.weapon.MCH_EntityRocket", "wge_t2", 0, 140, 300);
        for (int tick = 0; tick < 20; ++tick) f.update();
        assertSame("The gun's -80 degree elevation limit admits this target", missile, f.target());
        assertTrue(f.shots() > 0);
    }

    @Test public void cramCanDepressToItsGunLimitForAnIncomingMissile() throws Exception {
        Fixture f = new Fixture(0, -360, 360, 25);
        Object info = f.call(f.turret, "getAcInfo", new Class[0]);
        f.field(info, "minRotationPitch").setFloat(info, -85);
        f.field(info, "maxRotationPitch").setFloat(info, 10);
        Entity missile = f.projectile("mcheli.weapon.MCH_EntityRocket", "wge_t2", 0, 10, 200);
        missile.motionZ = -3;
        for (int tick = 0; tick < 10; ++tick) f.update();
        assertSame(missile, f.target());
        assertTrue("C-RAM's gun permits +25 degrees despite its +10 body limit", f.shots() > 0);
    }

    @Test public void explicitDisableSurvivesSavingAndLegacyMissingKeyDefaultsEnabled() throws Exception {
        Fixture f = new Fixture(0);
        f.call(f.turret, "setGunnerStatus", new Class[]{boolean.class}, false);
        NBTTagCompound saved = new NBTTagCompound();
        f.call(f.turret, "saveAircraft", new Class[]{NBTTagCompound.class}, saved);
        assertFalse(f.enabled());
        assertFalse(saved.getBoolean("AcGunnerStatus"));
        f.call(f.turret, "loadAircraft", new Class[]{NBTTagCompound.class}, saved);
        assertFalse(f.enabled());
        f.aircraft("wge_t2", 0, 80, 400);
        f.update();
        assertEquals("inactive: GunnerStatus disabled", f.status());
        assertEquals(0, f.shots());
        saved.removeTag("AcGunnerStatus");
        f.call(f.turret, "loadAircraft", new Class[]{NBTTagCompound.class}, saved);
        assertTrue(f.enabled());
        f.update();
        assertTrue(f.shots() > 0);
    }

    @Test public void unreachableMissileCannotStarveReachableAircraft() throws Exception {
        Fixture f = new Fixture(0);
        Entity aircraft = f.aircraft("wge_t2", 0, 80, 400);
        Entity missile = f.projectile("mcheli.weapon.MCH_EntityRocket", "wge_t2", 0, 80, 300);
        missile.motionZ = 30;
        f.update();
        assertSame(aircraft, f.target());
        assertTrue(f.shots() > 0);
        assertEquals(1, f.integer(f.gunner, "rejectedIntercept"));
    }

    @Test public void nearestCandidateIsIndependentOfIterationOrderAndTeamChangesStopFire() throws Exception {
        Fixture f = new Fixture(0);
        f.aircraft("wge_t2", 0, 80, 600);
        Entity near = f.aircraft("wge_t2", 0, 80, 560);
        f.update();
        assertSame(near, f.target());
        for (Entity entity : f.world.loadedEntityList())
            if (entity != f.turret && entity.riddenByEntity != null)
                f.call(entity.riddenByEntity, "setTeamName", new Class[]{String.class}, "wge_t1");
        int before = f.shots();
        f.update();
        assertNull(f.target());
        assertEquals(before, f.shots());
    }

    @Test public void optedInBombAcquiresAndFullCircleDefaultYawIsCompensated() throws Exception {
        Fixture f = new Fixture(90);
        Entity bomb = f.projectile("mcheli.weapon.MCH_EntityBomb", "wge_t2", 0, 90, 350);
        for (int tick = 0; tick < 10; ++tick) f.update();
        assertSame(bomb, f.target());
        assertTrue(f.shots() > 0);
        assertTrue(Math.abs(((Number) f.value(f.gunner, "fireYawError")).floatValue()) <= 1.5F);
    }

    @Test public void terrainAndPitchLimitsRejectTargetsAndLogTheActualReason() throws Exception {
        Fixture f = new Fixture(0);
        f.aircraft("wge_t2", 0, 200, 1);
        f.update();
        assertNull(f.target());
        assertEquals(1, f.integer(f.gunner, "rejectedTraverse"));
        Entity aircraft = f.aircraft("wge_t2", 0, 80, 400);
        f.world.blocked = true;
        f.update();
        assertNull(f.target());
        assertEquals(0, f.shots());
        assertTrue(f.integer(f.gunner, "rejectedSight") > 0);
        f.world.blocked = false;
        f.update();
        assertSame(aircraft, f.target());
        assertTrue(f.shots() > 0);
    }

    @Test public void finiteYawSectorSelectsTheReachableAircraft() throws Exception {
        Fixture f = new Fixture(15, -45, 45);
        f.aircraft("wge_t2", -300, 80, 50);
        Entity reachable = f.aircraft("wge_t2", 200, 80, 400);
        for (int tick = 0; tick < 5; ++tick) f.update();
        assertSame(reachable, f.target());
        assertTrue(f.shots() > 0);
        assertEquals(1, f.integer(f.gunner, "rejectedTraverse"));
    }

    @Test public void constructorAlsoPreservesConfiguredInfiniteAmmoAndFuelBits() throws Exception {
        Fixture f = new Fixture(0);
        Class<?> config = f.type("mcheli.MCH_Config");
        for (String key : new String[]{"InfinityAmmo", "InfinityFuel"}) {
            Object parameter = config.getField(key).get(null);
            parameter.getClass().getField("prmBool").setBoolean(parameter, true);
        }
        Object fresh = f.type(TestVehicle.class.getName()).getConstructor(World.class).newInstance(f.world);
        assertTrue((Boolean) f.call(fresh, "getCommonStatus", new Class[]{int.class}, 3));
        assertTrue((Boolean) f.call(fresh, "getCommonStatus", new Class[]{int.class}, 4));
        assertTrue((Boolean) f.call(fresh, "getGunnerStatus", new Class[0]));
    }

    private static final class Fixture {
        final HeadlessLoader loader = new HeadlessLoader();
        final TestWorld world = new TestWorld();
        final Object turret;
        final Object gunner;
        Fixture(float defaultYaw) throws Exception {
            this(defaultYaw, -360, 360);
        }
        Fixture(float defaultYaw, float minYaw, float maxYaw) throws Exception {
            this(defaultYaw, minYaw, maxYaw, 10);
        }
        Fixture(float defaultYaw, float minYaw, float maxYaw, float maxPitch) throws Exception {
            this("0,3,0.5,0,0,true,1," + defaultYaw + "," + minYaw + "," + maxYaw + ",-80," + maxPitch);
        }
        Fixture(String mount) throws Exception {
            this("test_cannon", mount);
        }
        Fixture(String weaponName, String mount) throws Exception {
            Class<?> mod = type("mcheli.MCH_MOD");
            mod.getField("proxy").set(null, type("mcheli.MCH_CommonProxy").newInstance());
            type("mcheli.MCH_Config").getConstructor(String.class, String.class).newInstance(".", "unused.cfg");
            world.getScoreboard().createTeam("wge_t1");
            world.getScoreboard().createTeam("wge_t2");
            Object weapon = type("mcheli.weapon.MCH_WeaponInfo").getConstructor(String.class).newInstance(weaponName);
            load(weapon, "type", "machinegun2");
            load(weapon, "acceleration", "18");
            load(weapon, "round", "100");
            Field map = type("mcheli.weapon.MCH_WeaponInfoManager").getDeclaredField("map");
            map.setAccessible(true);
            ((Map) map.get(null)).put(weaponName, weapon);
            Object info = type("mcheli.vehicle.MCH_VehicleInfo").getConstructor(String.class).newInstance("test_vehicle");
            load(info, "addturretweapon", weaponName + "," + mount);
            ((Map) type("mcheli.vehicle.MCH_VehicleInfoManager").getField("map").get(null)).put("test_vehicle", info);
            turret = type(TestVehicle.class.getName()).getConstructor(World.class).newInstance(world);
            call(turret, "setAcInfo", new Class[]{type("mcheli.aircraft.MCH_AircraftInfo")}, info);
            call(turret, "setTypeName", new Class[]{String.class}, "test_vehicle");
            Object weapons = call(turret, "createWeapon", new Class[]{int.class}, 1);
            field(turret, "weapons").set(turret, weapons);
            field(turret, "currentWeaponID").set(turret, new int[]{0});
            ((Entity) turret).setPosition(0, 64, 0);
            gunner = newGunner("wge_t1");
            Object fast = type("mcheli.mob.MCH_GunnerInfo").getConstructor(String.class).newInstance("test_max_response");
            for (String setting : new String[]{"detectionrange=1000", "scaninterval=1", "reactionticks=0", "lookspeed=15",
                "trackingresponse=1", "aimerror=0", "firetolerance=1.5", "enableshortburst=false"}) {
                int equals = setting.indexOf('=');
                load(fast, setting.substring(0, equals), setting.substring(equals + 1));
            }
            registerProfile(fast);
            call(gunner, "applyProfile", new Class[]{type("mcheli.mob.MCH_GunnerInfo")}, fast);
            ((Entity) gunner).ridingEntity = (Entity) turret;
            ((Entity) turret).riddenByEntity = (Entity) gunner;
            world.loadedEntityList.add(turret);
        }
        Class<?> type(String name) throws Exception { return Class.forName(name, true, loader); }
        Object rotationSnapshot(Entity aircraft, float pitch, long millis) throws Exception {
            Object snapshot = type("mcheli.MCH_EntityInfo").getConstructor(int.class, String.class, String.class,
                String.class, double.class, double.class, double.class, double.class, double.class, double.class)
                .newInstance(aircraft.getEntityId(), "gunner_test", "test_plane", aircraft.getClass().getName(),
                    0D, 0D, 0D, 0D, 0D, 0D);
            field(snapshot, "aircraftUuid").set(snapshot, aircraft.getUniqueID());
            field(snapshot, "rotationPitch").setFloat(snapshot, pitch);
            field(snapshot, "lastUpdateTime").setLong(snapshot, millis);
            return snapshot;
        }
        void registerProfile(Object profile) throws Exception {
            Field map = type("mcheli.mob.MCH_GunnerInfoManager").getDeclaredField("map");
            map.setAccessible(true);
            ((Map) map.get(null)).put(value(profile, "name"), profile);
        }
        Object loadProfile(String name) throws Exception {
            Object profile = type("mcheli.mob.MCH_GunnerInfo").getConstructor(String.class).newInstance(name);
            try (InputStream input = getClass().getResourceAsStream("/assets/mcheli/gunners/" + name + ".txt")) {
                assertNotNull(input);
                BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.trim().startsWith(";")) continue;
                    int equals = line.indexOf('=');
                    if (equals > 0) load(profile, line.substring(0, equals).trim(), line.substring(equals + 1).trim());
                }
            }
            registerProfile(profile);
            return profile;
        }
        Object resolvedProfile() throws Exception {
            Method profile = gunner.getClass().getDeclaredMethod("profile");
            profile.setAccessible(true);
            return profile.invoke(gunner);
        }
        Object newGunner(String team) throws Exception {
            Object entity = type("mcheli.mob.MCH_EntityGunner").getConstructor(World.class).newInstance(world);
            call(entity, "setTeamName", new Class[]{String.class}, team);
            call(entity, "setTargetType", new Class[]{int.class}, 2);
            return entity;
        }
        Entity aircraft(String team, double x, double y, double z) throws Exception {
            Entity entity = (Entity) type("mcheli.helicopter.MCH_EntityHeli").getConstructor(World.class).newInstance(world);
            entity.setPosition(x, y, z);
            entity.riddenByEntity = (Entity) newGunner(team);
            world.loadedEntityList.add(entity);
            return entity;
        }
        Entity projectile(String type, String team, double x, double y, double z) throws Exception {
            Entity entity = (Entity) type(type).getConstructor(World.class).newInstance(world);
            Object info = type("mcheli.weapon.MCH_WeaponInfo").getConstructor(String.class).newInstance("projectile_test");
            load(info, "canbeintercepted", "true");
            call(entity, "setInfo", new Class[]{String.class, type("mcheli.weapon.MCH_WeaponInfo")}, "projectile_test", info);
            entity.setPosition(x, y, z);
            field(entity, "shootingEntity").set(entity, newGunner(team));
            world.loadedEntityList.add(entity);
            return entity;
        }
        void load(Object info, String key, String data) throws Exception { call(info, "loadItemData", new Class[]{String.class, String.class}, key, data); }
        Object call(Object target, String name, Class[] parameters, Object... args) throws Exception { return target.getClass().getMethod(name, parameters).invoke(target, args); }
        Field field(Object target, String name) throws Exception {
            for (Class<?> type = target.getClass(); type != null; type = type.getSuperclass()) {
                try { Field f = type.getDeclaredField(name); f.setAccessible(true); return f; }
                catch (NoSuchFieldException ignored) { }
            }
            throw new NoSuchFieldException(name);
        }
        Object value(Object target, String name) throws Exception { return field(target, name).get(target); }
        int integer(Object target, String name) throws Exception { return ((Number) value(target, name)).intValue(); }
        void update() throws Exception {
            Method update = gunner.getClass().getDeclaredMethod("updateAAGunner");
            update.setAccessible(true);
            update.invoke(gunner);
        }
        void weaponTick() throws Exception {
            // Same rider capture then weapon-update ordering as the aircraft's server tick.
            Class<?> aircraft = type("mcheli.aircraft.MCH_EntityAircraft");
            aircraft.getField("lastRiderYaw").setFloat(turret, ((Entity) gunner).rotationYaw);
            aircraft.getField("lastRiderPitch").setFloat(turret, ((Entity) gunner).rotationPitch);
            call(turret, "updateWeapons", new Class[0]);
            update();
        }
        boolean enabled() throws Exception { return (Boolean) call(turret, "getGunnerStatus", new Class[0]); }
        int shots() throws Exception { return integer(turret, "shots"); }
        Object target() throws Exception { return value(gunner, "targetEntity"); }
        String status() throws Exception { return (String) value(gunner, "status"); }
    }

    public static class TestVehicle extends MCH_EntityVehicle {
        public int shots;
        public TestVehicle(World world) { super(world); }
        @Override public boolean useCurrentWeapon(MCH_WeaponParam intent) {
            ++shots; return true;
        }
        public void saveAircraft(NBTTagCompound nbt) { super.writeEntityToNBT(nbt); }
        public void loadAircraft(NBTTagCompound nbt) { super.readEntityFromNBT(nbt); }
    }

    private static final class TestPlayer extends EntityPlayer {
        int inventoriesOpened;
        private boolean sneaking;
        private UUID identity;
        TestPlayer(World world) { super(world, new GameProfile(UUID.randomUUID(), "test_player")); }
        static TestPlayer create(World world) throws Exception {
            // Player construction initializes Forge's full recipe registry, unavailable in this
            // headless fixture. Only interaction-facing state is needed by these tests.
            Field singleton = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            singleton.setAccessible(true);
            TestPlayer player = (TestPlayer) ((sun.misc.Unsafe) singleton.get(null)).allocateInstance(TestPlayer.class);
            player.worldObj = world;
            player.identity = UUID.randomUUID();
            player.capabilities = new net.minecraft.entity.player.PlayerCapabilities();
            return player;
        }
        @Override public UUID getUniqueID() { return identity; }
        @Override public String getCommandSenderName() { return "test_player"; }
        @Override public boolean isSneaking() { return sneaking; }
        @Override public void setSneaking(boolean value) { sneaking = value; }
        @Override public net.minecraft.item.ItemStack getCurrentEquippedItem() { return null; }
        @Override public void addChatMessage(IChatComponent message) { }
        @Override public boolean canCommandSenderUseCommand(int level, String command) { return false; }
        @Override public ChunkCoordinates getPlayerCoordinates() { return new ChunkCoordinates(0, 64, 0); }
        @Override public void displayGUIChest(net.minecraft.inventory.IInventory inventory) { ++inventoriesOpened; }
    }

    private static final class TestWorld extends World {
        private static final Block AIR = new Block(Material.air) {
            @Override public boolean canCollideCheck(int metadata, boolean stopOnLiquid) { return false; }
        };
        private static final Block WALL = new Block(Material.rock) { };
        int wallZ = Integer.MIN_VALUE;
        int blockQueries;
        boolean chunksLoaded = true;
        boolean blocked;
        int droppedEggs;
        final List<Entity> spawned = new ArrayList<Entity>();
        boolean rejectProjectileSpawns;
        TestWorld() {
            super(null, "gunner_test", new WorldProviderSurface() {
                @Override protected void registerWorldChunkManager() { }
                @Override public void calculateInitialWeather() { }
            }, new WorldSettings(0, WorldSettings.GameType.SURVIVAL, false, false, WorldType.DEFAULT), new Profiler());
        }
        @Override protected IChunkProvider createChunkProvider() { return null; }
        @Override protected int func_152379_p() { return 0; }
        @Override public void calculateInitialSkylight() { }
        @Override public Block getBlock(int x, int y, int z) { ++blockQueries; return z == wallZ ? WALL : AIR; }
        @Override public int getBlockMetadata(int x, int y, int z) { return 0; }
        @Override public boolean spawnEntityInWorld(Entity entity) {
            if (rejectProjectileSpawns) return false;
            spawned.add(entity);
            if (entity instanceof EntityItem) ++droppedEggs;
            return true;
        }
        @Override public boolean checkChunksExist(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) { return chunksLoaded; }
        @Override public boolean blockExists(int x, int y, int z) { return chunksLoaded; }
        List<Entity> loadedEntityList() { return loadedEntityList; }
        @Override public Entity getEntityByID(int id) {
            for (Entity entity : loadedEntityList()) if (entity.getEntityId() == id) return entity;
            return null;
        }
        @Override public List getEntitiesWithinAABBExcludingEntity(Entity except, AxisAlignedBB bounds) {
            List<Entity> result = new ArrayList<Entity>();
            for (Entity entity : loadedEntityList()) if (entity != except && entity.boundingBox.intersectsWith(bounds)) result.add(entity);
            return result;
        }
        @Override public MovingObjectPosition rayTraceBlocks(Vec3 from, Vec3 to) {
            return blocked ? new MovingObjectPosition(0, 0, 0, 0, from) : super.rayTraceBlocks(from, to);
        }
    }

    /** Isolates Forge's event-bus startup and substitutes mod discovery; production methods stay intact. */
    private static final class HeadlessLoader extends ClassLoader {
        HeadlessLoader() { super(MCH_GunnerEngagementTest.class.getClassLoader()); }
        @Override protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            boolean fmur = name.startsWith("com.flansmod.") && System.getProperty("mcheli.test.fmurClasses") != null;
            if (!fmur && !name.startsWith("mcheli.") && !name.equals("cpw.mods.fml.common.Loader")) return super.loadClass(name, resolve);
            Class<?> existing = findLoadedClass(name);
            if (existing == null) {
                try {
                    byte[] bytes;
                    if (name.equals("cpw.mods.fml.common.Loader")) {
                        ClassWriter writer = new ClassWriter(0);
                        writer.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, name.replace('.', '/'), null, "java/lang/Object", null);
                        MethodVisitor method = writer.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "isModLoaded", "(Ljava/lang/String;)Z", null, null);
                        method.visitCode(); method.visitLdcInsn("wgevents"); method.visitVarInsn(Opcodes.ALOAD, 0);
                        method.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z", false);
                        method.visitInsn(Opcodes.IRETURN); method.visitMaxs(2, 1); method.visitEnd(); writer.visitEnd();
                        bytes = writer.toByteArray();
                    } else {
                        try (InputStream input = fmur ? java.nio.file.Files.newInputStream(java.nio.file.Paths.get(
                                System.getProperty("mcheli.test.fmurClasses"), name.replace('.', '/') + ".class"))
                                : getParent().getResourceAsStream(name.replace('.', '/') + ".class")) {
                            if (input == null) throw new ClassNotFoundException(name);
                            ClassReader reader = new ClassReader(input);
                            ClassWriter writer = new ClassWriter(0);
                            reader.accept(new ClassVisitor(Opcodes.ASM5, writer) {
                                @Override public void visitEnd() {
                                    // Forge's event transformer supplies this override in-game.
                                    if (name.equals("com.flansmod.common.eventhandlers.BulletHitEvent")) {
                                        MethodVisitor method = super.visitMethod(Opcodes.ACC_PUBLIC, "isCancelable", "()Z", null, null);
                                        method.visitCode(); method.visitInsn(Opcodes.ICONST_1); method.visitInsn(Opcodes.IRETURN);
                                        method.visitMaxs(1, 1); method.visitEnd();
                                    }
                                    super.visitEnd();
                                }
                                @Override public MethodVisitor visitMethod(int access, String method, String descriptor, String signature, String[] exceptions) {
                                    return (name.equals("mcheli.MCH_MOD") || name.equals("com.flansmod.common.FlansMod")) && method.equals("<clinit>") ? null
                                        : super.visitMethod(access, method, descriptor, signature, exceptions);
                                }
                            }, 0);
                            bytes = writer.toByteArray();
                        }
                    }
                    existing = defineClass(name, bytes, 0, bytes.length);
                } catch (Exception failure) { throw new ClassNotFoundException(name, failure); }
            }
            if (resolve) resolveClass(existing);
            return existing;
        }
    }
}
