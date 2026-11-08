package mcheli.network.packets;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import mcheli.MCH_EntityInfo;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class PacketEntityInfoSyncTest {
    @Test public void radarTerrainTrailerRoundTripsWithoutChangingVehicleIdentity() throws Exception {
        MCH_EntityInfo tank = new MCH_EntityInfo(37, "world", "tank", "mcheli.tank.MCH_EntityTank", 10, 64, 20, 10, 64, 20);
        tank.aircraftUuid = UUID.randomUUID();
        ByteBuf bytes = Unpooled.buffer();
        encodeWithoutLandingGear(new PacketEntityInfoSync(Collections.singletonList(tank), 8L, 12, Collections.singletonList(37)), bytes, 1);
        PacketEntityInfoSync decoded = new PacketEntityInfoSync();
        decoded.decodeInto(null, bytes.copy());
        assertEquals(12, getField(decoded, "radarEmitterId"));
        assertEquals(Collections.singletonList(37), getField(decoded, "terrainBlockedIds"));
        assertEquals(tank.aircraftUuid, getEntities(decoded).get(0).aircraftUuid);

        // The old decoder can stop at VID1; the updated decoder accepts an absent RAD1.
        bytes.writerIndex(bytes.writerIndex() - 9 - 16);
        PacketEntityInfoSync legacy = new PacketEntityInfoSync();
        legacy.decodeInto(null, bytes);
        assertEquals(0, getField(legacy, "radarEmitterId"));
        assertEquals(Collections.emptyList(), getField(legacy, "terrainBlockedIds"));
        assertEquals(tank.aircraftUuid, getEntities(legacy).get(0).aircraftUuid);
    }

    @Test public void malformedRadarTerrainCountsAreIgnored() throws Exception {
        ByteBuf bytes = Unpooled.buffer();
        encodeWithoutLandingGear(new PacketEntityInfoSync(Collections.<MCH_EntityInfo>emptyList(), 8L), bytes, 0);
        bytes.writerIndex(bytes.writerIndex() - 8); // Strip ARM1 to simulate an older sender.
        bytes.writeInt(0x52414431).writeInt(12).writeInt(Integer.MAX_VALUE);
        PacketEntityInfoSync decoded = new PacketEntityInfoSync();
        decoded.decodeInto(null, bytes);
        assertEquals(0, getField(decoded, "radarEmitterId"));
        assertEquals(Collections.emptyList(), getField(decoded, "terrainBlockedIds"));
    }

    private static Object getField(PacketEntityInfoSync packet, String name) throws Exception {
        Field field = PacketEntityInfoSync.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(packet);
    }

    @Test public void vehicleIdentitySurvivesSnapshotAndOldPacketsRemainReadable() throws Exception {
        MCH_EntityInfo tank = new MCH_EntityInfo(37, "world", "tank", "mcheli.tank.MCH_EntityTank",
            10.0D, 64.0D, 20.0D, 10.0D, 64.0D, 20.0D);
        tank.aircraftUuid = UUID.fromString("8a7211b2-9609-443b-8dcb-b7833bfd1d53");
        ByteBuf bytes = Unpooled.buffer();
        encodeWithoutLandingGear(new PacketEntityInfoSync(Collections.singletonList(tank), 8L), bytes, 1);

        PacketEntityInfoSync decoded = new PacketEntityInfoSync();
        decoded.decodeInto(null, bytes.copy());
        assertEquals(tank.aircraftUuid, getEntities(decoded).get(0).aircraftUuid);

        // VID1 is optional, so a packet from an older server still decodes.
        bytes.writerIndex(bytes.writerIndex() - 9 - 24);
        PacketEntityInfoSync legacy = new PacketEntityInfoSync();
        legacy.decodeInto(null, bytes);
        assertEquals(37, getEntities(legacy).get(0).entityId);
        assertNull(getEntities(legacy).get(0).aircraftUuid);
    }

    @Test public void armEmissionRoundTripsAndRadarShutdownReplacesActiveState() throws Exception {
        MCH_EntityInfo tank = new MCH_EntityInfo(37, "world", "AA", "mcheli.tank.MCH_EntityTank", 0, 64, 0, 0, 64, 0);
        tank.armEmitter = true;
        ByteBuf bytes = Unpooled.buffer();
        encodeWithoutLandingGear(new PacketEntityInfoSync(Collections.singletonList(tank), 8L, 12, Collections.singletonList(37)), bytes, 1);
        PacketEntityInfoSync decoded = new PacketEntityInfoSync();
        decoded.decodeInto(null, bytes.copy());
        assertTrue(getEntities(decoded).get(0).isArmEmitterForWeapon("atmissile"));
        assertEquals(Collections.singletonList(37), getField(decoded, "terrainBlockedIds"));

        tank.armEmitter = false;
        bytes.clear();
        encodeWithoutLandingGear(new PacketEntityInfoSync(Collections.singletonList(tank), 9L), bytes, 1);
        decoded.decodeInto(null, bytes.copy());
        assertFalse(getEntities(decoded).get(0).armEmitter);

        // Missing or truncated trailers cannot promote an unknown target to an emitter.
        bytes.writerIndex(bytes.writerIndex() - 1);
        decoded.decodeInto(null, bytes.copy());
        assertFalse(getEntities(decoded).get(0).armEmitter);
        bytes.writerIndex(bytes.writerIndex() - 8);
        decoded.decodeInto(null, bytes);
        assertFalse(getEntities(decoded).get(0).armEmitter);
    }

    @Test public void malformedArmCountDoesNotEnableAnyTarget() throws Exception {
        MCH_EntityInfo tank = new MCH_EntityInfo(37, "world", "AA", "mcheli.tank.MCH_EntityTank", 0, 64, 0, 0, 64, 0);
        tank.armEmitter = true;
        ByteBuf bytes = Unpooled.buffer();
        encodeWithoutLandingGear(new PacketEntityInfoSync(Collections.singletonList(tank), 8L), bytes, 1);
        bytes.setInt(bytes.writerIndex() - 5, Integer.MAX_VALUE);
        PacketEntityInfoSync decoded = new PacketEntityInfoSync();
        decoded.decodeInto(null, bytes);
        assertFalse(getEntities(decoded).get(0).armEmitter);
    }

    private static void encodeWithoutLandingGear(PacketEntityInfoSync packet, ByteBuf bytes, int count) {
        packet.encodeInto(null, bytes);
        // Preserve the older trailer boundaries exercised by the existing compatibility tests.
        bytes.writerIndex(bytes.writerIndex() - 8 - count * 4);
    }

    @Test public void landingGearRoundTripsBothEndpointsAndIntermediateRotation() throws Exception {
        MCH_EntityInfo down = aircraft(1), moving = aircraft(2), up = aircraft(3);
        down.landingGearRotation = 0;
        moving.landingGearRotation = 42.5F;
        up.landingGearRotation = 90;
        java.util.List<MCH_EntityInfo> aircraft = java.util.Arrays.asList(down, moving, up);
        ByteBuf bytes = Unpooled.buffer();
        new PacketEntityInfoSync(aircraft, 8L, 12, Collections.singletonList(1)).encodeInto(null, bytes);
        PacketEntityInfoSync decoded = new PacketEntityInfoSync();
        decoded.decodeInto(null, bytes.copy());
        assertEquals(0, getEntities(decoded).get(0).landingGearRotation, 0);
        assertEquals(42.5F, getEntities(decoded).get(1).landingGearRotation, 0);
        assertEquals(90, getEntities(decoded).get(2).landingGearRotation, 0);
        assertEquals(Collections.singletonList(1), getField(decoded, "terrainBlockedIds"));

        bytes.writerIndex(bytes.writerIndex() - 8 - 3 * 4);
        decoded.decodeInto(null, bytes);
        for (MCH_EntityInfo info : getEntities(decoded)) assertTrue(Float.isNaN(info.landingGearRotation));
    }

    @Test public void malformedLandingGearTrailerCannotSupplyFalsePartState() throws Exception {
        MCH_EntityInfo plane = aircraft(1);
        plane.landingGearRotation = 90;
        ByteBuf bytes = Unpooled.buffer();
        new PacketEntityInfoSync(Collections.singletonList(plane), 8L).encodeInto(null, bytes);
        for (int count : new int[]{-1, 0, Integer.MAX_VALUE}) {
            ByteBuf malformed = bytes.copy();
            malformed.setInt(malformed.writerIndex() - 8, count);
            PacketEntityInfoSync decoded = new PacketEntityInfoSync();
            decoded.decodeInto(null, malformed);
            assertTrue(Float.isNaN(getEntities(decoded).get(0).landingGearRotation));
        }
        bytes.writerIndex(bytes.writerIndex() - 1);
        PacketEntityInfoSync decoded = new PacketEntityInfoSync();
        decoded.decodeInto(null, bytes);
        assertTrue(Float.isNaN(getEntities(decoded).get(0).landingGearRotation));
        for (float rotation : new float[]{-1, 91, Float.NaN, Float.POSITIVE_INFINITY}) {
            plane.landingGearRotation = rotation;
            bytes.clear();
            new PacketEntityInfoSync(Collections.singletonList(plane), 9L).encodeInto(null, bytes);
            decoded.decodeInto(null, bytes);
            assertTrue(Float.isNaN(getEntities(decoded).get(0).landingGearRotation));
        }
    }

    private static MCH_EntityInfo aircraft(int id) {
        return new MCH_EntityInfo(id, "world", "plane", "mcheli.plane.MCP_EntityPlane", 0, 64, 0, 0, 64, 0);
    }

    @SuppressWarnings("unchecked")
    private static List<MCH_EntityInfo> getEntities(PacketEntityInfoSync packet) throws Exception {
        Field field = PacketEntityInfoSync.class.getDeclaredField("entities");
        field.setAccessible(true);
        return (List<MCH_EntityInfo>)field.get(packet);
    }
}
