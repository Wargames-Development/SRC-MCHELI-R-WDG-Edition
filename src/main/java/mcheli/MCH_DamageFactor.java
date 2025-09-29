package mcheli;

import mcheli.helicopter.MCH_EntityHeli;
import mcheli.plane.MCP_EntityPlane;
import mcheli.tank.MCH_EntityTank;
import mcheli.vehicle.MCH_EntityVehicle;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;

import java.util.HashMap;

public class MCH_DamageFactor {

    public float damageVsLiving = 1f;
    public float damageVsPlayer = 1f;
    public float damageVsPlane = 1f;
    public float damageVsVehicle = 1f;
    public float damageVsTank = 1f;
    public float damageVsHeli = 1f;

    public float getDamageFactor(Entity entity) {
        if (entity instanceof EntityPlayer) return damageVsPlayer;
        else if (entity instanceof EntityLivingBase) return damageVsLiving;
        else if (entity instanceof MCP_EntityPlane) return damageVsPlane;
        else if (entity instanceof MCH_EntityHeli) return damageVsHeli;
        else if (entity instanceof MCH_EntityTank) return damageVsTank;
        else if (entity instanceof MCH_EntityVehicle) return damageVsVehicle;
        return 1.0f;
    }
}
