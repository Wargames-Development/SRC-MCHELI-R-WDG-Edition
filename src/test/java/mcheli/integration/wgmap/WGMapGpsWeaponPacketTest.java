package mcheli.integration.wgmap;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import mcheli.network.packets.PacketUseWeapon;
import mcheli.weapon.MCH_GPSPosition;
import mcheli.weapon.MCH_WeaponBomb;
import mcheli.weapon.MCH_WeaponInfo;
import mcheli.weapon.MCH_WeaponParam;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.player.EntityPlayerMP;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public final class WGMapGpsWeaponPacketTest {
    @Test public void waypointOptionRetainsExistingFiveFieldWireLayout() {
        assertRoundTrip(new PacketUseWeapon(7, PacketUseWeapon.WGM_SHARED_GPS, 12.5D, 80.0D, -41.5D));
        PacketUseWeapon selection = PacketUseWeapon.selectGpsBombTarget(new WGMapGpsTarget(12.5D, 80.0D, -41.5D, true));
        assertNotEquals(PacketUseWeapon.WGM_SHARED_GPS, selection.useWeaponOption2);
        assertRoundTrip(selection);
    }

    @Test public void bombDropMustMatchASelectionInTheSameWorld() throws Exception {
        EntityPlayerMP player = allocate(EntityPlayerMP.class);
        WorldClient firstWorld = allocate(WorldClient.class);
        player.worldObj = firstWorld;
        Method select = PacketUseWeapon.class.getDeclaredMethod("selectGpsBombTarget", EntityPlayerMP.class);
        Method matches = PacketUseWeapon.class.getDeclaredMethod("matchesGpsBombSelection",
            EntityPlayerMP.class, MCH_GPSPosition.class, boolean.class);
        select.setAccessible(true);
        matches.setAccessible(true);
        MCH_GPSPosition target = new MCH_GPSPosition(12.5D, 80.0D, -41.5D);
        MCH_GPSPosition other = new MCH_GPSPosition(12.5D, 80.0D, -40.5D);

        assertFalse((Boolean) matches.invoke(null, player, target, false));
        select.invoke(PacketUseWeapon.selectGpsBombTarget(new WGMapGpsTarget(target.x, target.y, target.z, false)), player);
        assertTrue((Boolean) matches.invoke(null, player, target, false));
        assertFalse((Boolean) matches.invoke(null, player, other, false));
        assertFalse((Boolean) matches.invoke(null, player, target, true));
        player.worldObj = allocate(WorldClient.class);
        assertFalse((Boolean) matches.invoke(null, player, target, false));
        player.worldObj = firstWorld;
        select.invoke(PacketUseWeapon.selectGpsBombTarget(null), player);
        assertFalse((Boolean) matches.invoke(null, player, target, false));
    }

    @Test public void gpsBombCannotUseAnImplicitTarget() throws Exception {
        WorldClient world = allocate(WorldClient.class);
        Field remote = net.minecraft.world.World.class.getField("isRemote");
        remote.setBoolean(world, true);
        MCH_WeaponInfo info = new MCH_WeaponInfo("test_gps_bomb");
        info.type = "bomb";
        info.isGPSMissile = true;
        MCH_WeaponBomb bomb = new MCH_WeaponBomb(world, null, 0, 0, info.name, info);
        MCH_WeaponParam intent = new MCH_WeaponParam();
        MCH_GPSPosition target = new MCH_GPSPosition(12.5D, 80.0D, -41.5D);
        target.isActive = true;
        MCH_GPSPosition previous = MCH_GPSPosition.currentClientGPSPosition;
        try {
            MCH_GPSPosition.currentClientGPSPosition = target;
            assertFalse(bomb.shot(intent));
            intent.gpsTarget = target;
            assertTrue(bomb.shot(intent));
        } finally {
            MCH_GPSPosition.currentClientGPSPosition = previous;
        }
    }

    private static <T> T allocate(Class<T> type) throws Exception {
        Field singleton = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        singleton.setAccessible(true);
        return type.cast(((sun.misc.Unsafe) singleton.get(null)).allocateInstance(type));
    }

    private static void assertRoundTrip(PacketUseWeapon source) {
        ByteBuf bytes = Unpooled.buffer();
        source.encodeInto(null, bytes);
        assertEquals(32, bytes.readableBytes());
        PacketUseWeapon decoded = new PacketUseWeapon();
        decoded.decodeInto(null, bytes);
        assertEquals(source.useWeaponOption1, decoded.useWeaponOption1);
        assertEquals(source.useWeaponOption2, decoded.useWeaponOption2);
        assertEquals(source.useWeaponPosX, decoded.useWeaponPosX, 0.0D);
        assertEquals(source.useWeaponPosY, decoded.useWeaponPosY, 0.0D);
        assertEquals(source.useWeaponPosZ, decoded.useWeaponPosZ, 0.0D);
    }
}
