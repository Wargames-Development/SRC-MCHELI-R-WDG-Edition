package mcheli.render;

import mcheli.MCH_EntityInfo;
import mcheli.aircraft.MCH_AircraftInfo;
import mcheli.helicopter.MCH_EntityHeli;
import mcheli.helicopter.MCH_HeliInfo;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.*;

/** Real renderer pose-selection path, without constructing GL resources or a live world. */
public final class MCH_LaserLodTargetTest {
    @Test public void snapshotOnlyPoseCanBeAcquiredFollowedAcrossIdChangeAndReleasedWhenStale() throws Exception {
        Field unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        sun.misc.Unsafe unsafe = (sun.misc.Unsafe)unsafeField.get(null);
        MCH_RenderFarVehicle renderer = (MCH_RenderFarVehicle)unsafe.allocateInstance(MCH_RenderFarVehicle.class);
        World world = (World)unsafe.allocateInstance(WorldClient.class);
        Object oldRenderer = get(MCH_RenderFarVehicle.class, null, "activeRenderer");
        try {
            set(MCH_RenderFarVehicle.class, null, "activeRenderer", renderer);
            set(MCH_RenderFarVehicle.class, renderer, "lastWorld", world);
            set(MCH_RenderFarVehicle.class, renderer, "renderFrame", 10);
            set(MCH_RenderFarVehicle.class, renderer, "colorFrameMillis", System.currentTimeMillis());
            UUID identity = UUID.randomUUID();
            String type = MCH_EntityHeli.class.getName();
            MCH_EntityInfo contact = new MCH_EntityInfo(42, "test", "test", type, 273, 64, 0, 273, 64, 0);
            contact.aircraftUuid = identity;
            Class<?> poseType = Class.forName("mcheli.render.MCH_RenderFarVehicle$SmoothedPose");
            Constructor<?> poseConstructor = poseType.getDeclaredConstructor(); poseConstructor.setAccessible(true);
            Object pose = poseConstructor.newInstance();
            set(poseType, pose, "aircraftUuid", identity);
            set(poseType, pose, "entityClassName", type); set(poseType, pose, "entityName", "test");
            set(poseType, pose, "x", 273D); set(poseType, pose, "y", 64D); set(poseType, pose, "z", 0D);
            set(poseType, pose, "lastRenderFrame", 10); set(poseType, pose, "contact", contact);
            Map<Integer,Object> poses = new HashMap<Integer,Object>(); poses.put(42, pose);
            set(MCH_RenderFarVehicle.class, renderer, "smoothedPoses", poses);

            MCH_AircraftInfo info = (MCH_AircraftInfo)unsafe.allocateInstance(MCH_HeliInfo.class);
            info.bodyWidth = 6; info.bodyHeight = 4;
            Class<?> definitionType = Class.forName("mcheli.render.MCH_RenderFarVehicle$RenderDefinition");
            Constructor<?> constructor = definitionType.getDeclaredConstructor(MCH_AircraftInfo.class, String.class, ResourceLocation.class);
            constructor.setAccessible(true);
            Map<String,Object> definitions = new HashMap<String,Object>();
            definitions.put(type + ":test", constructor.newInstance(info, "helicopters", new ResourceLocation("mcheli", "test")));
            set(MCH_RenderFarVehicle.class, renderer, "definitions", definitions);
            Vec3 start = Vec3.createVectorHelper(0, 66, 0), end = Vec3.createVectorHelper(750, 66, 0);
            set(MCH_RenderFarVehicle.class, renderer, "colorFrameMillis", System.currentTimeMillis());
            contact.lastUpdateTime = System.currentTimeMillis();
            MCH_RenderFarVehicle.LaserTarget target = MCH_RenderFarVehicle.findLaserTarget(world, start, end, null, -1);
            assertNotNull("snapshot-only aircraft must be selectable without a client Entity", target);
            assertEquals(identity, target.uuid); assertEquals(273D, target.centre.xCoord, 0D);

            set(poseType, pose, "x", 300D); set(poseType, pose, "z", 50D);
            poses.clear(); poses.put(43, pose); contact.entityId = 43;
            assertNull("acquisition still requires a ray hit", MCH_RenderFarVehicle.findLaserTarget(world, start, end, null, -1));
            target = MCH_RenderFarVehicle.findLaserTarget(world, start, end, identity, -1);
            assertNotNull("follow keeps UUID through motion and numeric-ID changes", target);
            assertEquals(50D, target.centre.zCoord, 0D);
            assertNull(MCH_RenderFarVehicle.findLaserTarget(world, start, end, identity, 43));
            contact.destroyed = true;
            assertNull(MCH_RenderFarVehicle.findLaserTarget(world, start, end, identity, -1));
            contact.destroyed = false; contact.countermeasureFlags = MCH_EntityInfo.CM_FLAG_JAMMING;
            assertNull(MCH_RenderFarVehicle.findLaserTarget(world, start, end, identity, -1));
            contact.countermeasureFlags = 0;
            contact.lastUpdateTime = 0L;
            assertNull("stale snapshots must not be followed", MCH_RenderFarVehicle.findLaserTarget(world, start, end, identity, -1));
            contact.lastUpdateTime = System.currentTimeMillis();
            set(MCH_RenderFarVehicle.class, renderer, "colorFrameMillis", 0L);
            assertNull("stale rendered poses must not remain laser targets", MCH_RenderFarVehicle.findLaserTarget(world, start, end, identity, -1));
        } finally {
            set(MCH_RenderFarVehicle.class, null, "activeRenderer", oldRenderer);
        }
    }

    private static Object get(Class<?> type, Object owner, String name) throws Exception {
        Field field = type.getDeclaredField(name); field.setAccessible(true); return field.get(owner);
    }
    private static void set(Class<?> type, Object owner, String name, Object value) throws Exception {
        Field field = type.getDeclaredField(name); field.setAccessible(true); field.set(owner, value);
    }
}
