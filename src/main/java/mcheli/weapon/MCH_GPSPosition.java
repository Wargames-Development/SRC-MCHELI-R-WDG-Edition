package mcheli.weapon;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mcheli.MCH_MOD;
import mcheli.network.packets.PacketGPSPositionReset;
import net.minecraft.entity.Entity;

import java.util.HashMap;
import java.util.Map;

public class MCH_GPSPosition {

    public static Map<Integer, MCH_GPSPosition> currentGPSPositions = new HashMap<>();
    @SideOnly(Side.CLIENT)
    public static MCH_GPSPosition currentClientGPSPosition = new MCH_GPSPosition(0, 0, 0);
    public double x, y, z;
    public Entity owner;
    public boolean isActive = false;

    public MCH_GPSPosition(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public static void set(double x, double y, double z, boolean isActive, Entity owner) {
        if (owner.worldObj.isRemote) {
            clientSet(x, y, z, isActive, owner);
            MCH_MOD.getPacketHandler().sendToServer(new PacketGPSPositionReset(x, y, z, isActive, owner.getEntityId()));
        } else {
            MCH_GPSPosition gpsPosition = new MCH_GPSPosition(x, y, z);
            gpsPosition.isActive = isActive;
            gpsPosition.owner = owner;
            currentGPSPositions.put(owner.getEntityId(), gpsPosition);
        }
    }

    public static MCH_GPSPosition get(Entity owner) {
        return currentGPSPositions.get(owner.getEntityId());
    }

    @SideOnly(Side.CLIENT)
    public static void clientSet(double x, double y, double z, boolean isActive, Entity owner) {
        currentClientGPSPosition.x = x;
        currentClientGPSPosition.y = y;
        currentClientGPSPosition.z = z;
        currentClientGPSPosition.isActive = isActive;
        currentClientGPSPosition.owner = owner;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }
}
