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
        new PacketEntityInfoSync(Collections.singletonList(tank), 8L, 12, Collections.singletonList(37)).encodeInto(null, bytes);
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
        new PacketEntityInfoSync(Collections.<MCH_EntityInfo>emptyList(), 8L).encodeInto(null, bytes);
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
        new PacketEntityInfoSync(Collections.singletonList(tank), 8L).encodeInto(null, bytes);

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
        new PacketEntityInfoSync(Collections.singletonList(tank), 8L, 12, Collections.singletonList(37)).encodeInto(null, bytes);
        PacketEntityInfoSync decoded = new PacketEntityInfoSync();
        decoded.decodeInto(null, bytes.copy());
        assertTrue(getEntities(decoded).get(0).isArmEmitterForWeapon("atmissile"));
        assertEquals(Collections.singletonList(37), getField(decoded, "terrainBlockedIds"));

        tank.armEmitter = false;
        bytes.clear();
        new PacketEntityInfoSync(Collections.singletonList(tank), 9L).encodeInto(null, bytes);
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
        new PacketEntityInfoSync(Collections.singletonList(tank), 8L).encodeInto(null, bytes);
        bytes.setInt(bytes.writerIndex() - 5, Integer.MAX_VALUE);
        PacketEntityInfoSync decoded = new PacketEntityInfoSync();
        decoded.decodeInto(null, bytes);
        assertFalse(getEntities(decoded).get(0).armEmitter);
    }

    @SuppressWarnings("unchecked")
    private static List<MCH_EntityInfo> getEntities(PacketEntityInfoSync packet) throws Exception {
        Field field = PacketEntityInfoSync.class.getDeclaredField("entities");
        field.setAccessible(true);
        return (List<MCH_EntityInfo>)field.get(packet);
    }
}
