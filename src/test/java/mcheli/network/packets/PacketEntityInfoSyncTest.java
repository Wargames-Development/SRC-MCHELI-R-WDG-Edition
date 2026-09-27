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
