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
        bytes.writerIndex(bytes.writerIndex() - 16);
        PacketEntityInfoSync legacy = new PacketEntityInfoSync();
        legacy.decodeInto(null, bytes);
        assertEquals(0, getField(legacy, "radarEmitterId"));
        assertEquals(Collections.emptyList(), getField(legacy, "terrainBlockedIds"));
        assertEquals(tank.aircraftUuid, getEntities(legacy).get(0).aircraftUuid);
    }

    @Test public void malformedRadarTerrainCountsAreIgnored() throws Exception {
        ByteBuf bytes = Unpooled.buffer();
        new PacketEntityInfoSync(Collections.<MCH_EntityInfo>emptyList(), 8L).encodeInto(null, bytes);
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
        bytes.writerIndex(bytes.writerIndex() - 24);
        PacketEntityInfoSync legacy = new PacketEntityInfoSync();
        legacy.decodeInto(null, bytes);
        assertEquals(37, getEntities(legacy).get(0).entityId);
        assertNull(getEntities(legacy).get(0).aircraftUuid);
    }

    @SuppressWarnings("unchecked")
    private static List<MCH_EntityInfo> getEntities(PacketEntityInfoSync packet) throws Exception {
        Field field = PacketEntityInfoSync.class.getDeclaredField("entities");
        field.setAccessible(true);
        return (List<MCH_EntityInfo>)field.get(packet);
    }
}
