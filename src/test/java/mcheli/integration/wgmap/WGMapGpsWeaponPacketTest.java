package mcheli.integration.wgmap;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import mcheli.network.packets.PacketUseWeapon;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class WGMapGpsWeaponPacketTest {
    @Test public void waypointOptionRetainsExistingFiveFieldWireLayout() {
        PacketUseWeapon source = new PacketUseWeapon(7, PacketUseWeapon.WGM_SHARED_GPS,
                12.5D, 80.0D, -41.5D);
        ByteBuf bytes = Unpooled.buffer();
        source.encodeInto(null, bytes);
        assertEquals(32, bytes.readableBytes());
        PacketUseWeapon decoded = new PacketUseWeapon();
        decoded.decodeInto(null, bytes);
        assertEquals(7, decoded.useWeaponOption1);
        assertEquals(PacketUseWeapon.WGM_SHARED_GPS, decoded.useWeaponOption2);
        assertEquals(12.5D, decoded.useWeaponPosX, 0.0D);
        assertEquals(80.0D, decoded.useWeaponPosY, 0.0D);
        assertEquals(-41.5D, decoded.useWeaponPosZ, 0.0D);
    }
}
