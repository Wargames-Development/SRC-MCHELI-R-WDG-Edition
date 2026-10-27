package mcheli.network.packets;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;
import static org.junit.Assert.*;

public class PacketLaserIntentTest {
    @Test public void stateWireLayoutAndOwnerFieldAreUnchanged() {
        ByteBuf bytes = Unpooled.buffer();
        try {
            new PacketLaserStateSync(2, 123, true, -1000.5, 72.5, 500, 999).encodeInto(null, bytes);
            assertEquals(41, bytes.readableBytes());
            PacketLaserStateSync decoded = new PacketLaserStateSync(); decoded.decodeInto(null, bytes);
            assertEquals(2, decoded.sourceType); assertEquals(123, decoded.sequence); assertTrue(decoded.active);
            assertEquals(-1000.5, decoded.x, 0); assertEquals(72.5, decoded.y, 0); assertEquals(500, decoded.z, 0);
            assertEquals(999, decoded.ownerId);
            decoded.handleServerSide(null);
        } finally { bytes.release(); }
    }

    @Test public void legacyGuidanceWireLayoutStillRoundTripsAndNullSendersAreSafe() {
        ByteBuf bytes = Unpooled.buffer();
        try {
            new PacketLaserGuidanceTargeting(true, 1000, 64, -500).encodeInto(null, bytes);
            assertEquals(25, bytes.readableBytes());
            PacketLaserGuidanceTargeting decoded = new PacketLaserGuidanceTargeting(); decoded.decodeInto(null, bytes);
            assertTrue(decoded.targeting); assertEquals(1000, decoded.targetPosX, 0);
            assertEquals(64, decoded.targetPosY, 0); assertEquals(-500, decoded.targetPosZ, 0);
            decoded.handleServerSide(null);
        } finally { bytes.release(); }
        PacketLaserStateSync.drainServerIntents();
    }
}
