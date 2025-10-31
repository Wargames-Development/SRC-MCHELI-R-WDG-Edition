package mcheli;

import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.helicopter.MCH_EntityHeli;
import mcheli.plane.MCP_EntityPlane;
import mcheli.tank.MCH_EntityTank;
import mcheli.vehicle.MCH_EntityVehicle;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public class MCH_3rdCamera extends EntityLivingBase {

    public static float lerpAmount = 0.3F;
    public MCH_EntityAircraft entity;

    public MCH_3rdCamera(World w) {
        super(w);
        setSize(0F, 0F);
    }

    public MCH_3rdCamera(World world, MCH_EntityAircraft ac) {
        this(world);
        entity = ac;
        setPosition(ac.posX, ac.posY, ac.posZ);
    }

    @Override
    public void onUpdate() {
        prevPosX = posX;
        prevPosY = posY;
        prevPosZ = posZ;

        if (entity == null || entity.isDead) {
            setDead();
            return;
        }

        Vec3 camCfg = getCameraPositions(entity);
        final double upHeight = camCfg.yCoord;
        final double yawRad   = Math.toRadians(entity.rotationYaw);
        final double pitchRad = Math.toRadians(entity.rotationPitch);
        final double rollRad  = Math.toRadians(entity.getRotRoll());
        final double cy = Math.cos(yawRad),  sy = Math.sin(yawRad);
        final double cp = Math.cos(pitchRad), sp = Math.sin(pitchRad);

        Vec3 f = Vec3.createVectorHelper(-sy * cp, -sp, cy * cp).normalize();
        Vec3 worldUp = Vec3.createVectorHelper(0, 1, 0);
        Vec3 r = f.crossProduct(worldUp);
        if (r.lengthVector() < 1.0e-6) r = Vec3.createVectorHelper(1, 0, 0);
        else r = r.normalize();
        Vec3 u = r.crossProduct(f).normalize();
        Vec3 uRoll = rotateAroundAxis(u, f, rollRad).normalize();

        double targetX = entity.posX + uRoll.xCoord * upHeight;
        double targetY = entity.posY + uRoll.yCoord * upHeight;
        double targetZ = entity.posZ + uRoll.zCoord * upHeight;

        double dX = targetX - posX;
        double dY = targetY - posY;
        double dZ = targetZ - posZ;
        setPosition(posX + dX * lerpAmount, posY + dY * lerpAmount, posZ + dZ * lerpAmount);

        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        if (player != null) {
            rotationYaw   = player.rotationYaw;
            rotationPitch = clampPitch(player.rotationPitch + 2.0F);
        } else {
            rotationYaw   = entity.rotationYaw;
            rotationPitch = clampPitch(entity.rotationPitch + 2.0F);
        }

        for (; rotationYaw - prevRotationYaw >= 180F; rotationYaw -= 360F) ;
        for (; rotationYaw - prevRotationYaw <  -180F; rotationYaw += 360F) ;
    }

    private static Vec3 rotateAroundAxis(Vec3 v, Vec3 kUnit, double angle) {
        Vec3 k = kUnit.normalize();
        double c = Math.cos(angle), s = Math.sin(angle);
        Vec3 kv = k.crossProduct(v);
        double kdotv = k.xCoord * v.xCoord + k.yCoord * v.yCoord + k.zCoord * v.zCoord;
        double rx = v.xCoord * c + kv.xCoord * s + k.xCoord * kdotv * (1.0 - c);
        double ry = v.yCoord * c + kv.yCoord * s + k.yCoord * kdotv * (1.0 - c);
        double rz = v.zCoord * c + kv.zCoord * s + k.zCoord * kdotv * (1.0 - c);
        return Vec3.createVectorHelper(rx, ry, rz);
    }

    private static float clampPitch(float p) {
        if (p > 89.9F)  return 89.9F;
        return Math.max(p, -89.9F);
    }

    private Vec3 getCameraPositions(Entity entity) {
        if (entity instanceof MCP_EntityPlane) {
            return Vec3.createVectorHelper(0, 10, 0);
        } else if (entity instanceof MCH_EntityHeli) {
            return Vec3.createVectorHelper(0, 5, 0);
        } else if (entity instanceof MCH_EntityTank) {
            return Vec3.createVectorHelper(0, 4, 0);
        } else if (entity instanceof MCH_EntityVehicle) {
            return Vec3.createVectorHelper(0, 2, 0);
        }
        return Vec3.createVectorHelper(0, 0, 0);
    }

    @Override
    public ItemStack getHeldItem() {
        return null;
    }

    @Override
    public ItemStack getEquipmentInSlot(int p_71124_1_) {
        return null;
    }

    @Override
    public void setCurrentItemOrArmor(int slotIn, ItemStack itemStackIn) {
    }

    @Override
    public ItemStack[] getLastActiveItems() {
        return null;
    }
}
