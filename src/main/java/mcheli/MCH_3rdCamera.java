package mcheli;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
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

@SideOnly(Side.CLIENT)
public class MCH_3rdCamera extends EntityLivingBase {

    public static float lerpAmount = 0.3F;
    private static final double HARD_SNAP_DISTANCE_SQ = 64.0D * 64.0D;
    public MCH_EntityAircraft entity;

    public MCH_3rdCamera(World w) {
        super(w);
        setSize(0F, 0F);
    }

    public MCH_3rdCamera(World world, MCH_EntityAircraft ac) {
        this(world);
        entity = ac;
        hardSnap("create");
    }

    @Override
    public void onUpdate() {
        lastTickPosX = posX;
        lastTickPosY = posY;
        lastTickPosZ = posZ;
        prevPosX = posX;
        prevPosY = posY;
        prevPosZ = posZ;
        prevRotationYaw = rotationYaw;
        prevRotationPitch = rotationPitch;

        if (entity == null || entity.isDead || entity.worldObj != this.worldObj) {
            setDead();
            return;
        }

        Vec3 target = getTargetPosition();
        double dX = target.xCoord - posX;
        double dY = target.yCoord - posY;
        double dZ = target.zCoord - posZ;
        double distanceSq = dX * dX + dY * dY + dZ * dZ;
        if (distanceSq > HARD_SNAP_DISTANCE_SQ) {
            hardSnap("distance");
        } else {
            setPosition(posX + dX * lerpAmount, posY + dY * lerpAmount, posZ + dZ * lerpAmount);
        }

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

    public void hardSnap(String reason) {
        if (this.entity == null) {
            return;
        }
        Vec3 target = getTargetPosition();
        double dx = target.xCoord - this.posX;
        double dy = target.yCoord - this.posY;
        double dz = target.zCoord - this.posZ;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        setPosition(target.xCoord, target.yCoord, target.zCoord);
        this.prevPosX = this.lastTickPosX = this.posX;
        this.prevPosY = this.lastTickPosY = this.posY;
        this.prevPosZ = this.lastTickPosZ = this.posZ;
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        MCH_Lib.DbgTrace(this.worldObj,
            "event=third_camera_snap reason=%s distance=%.2f camera=%s player=%s riding=%s aircraft=%s",
            reason, Double.valueOf(distance), describe(this), describe(player),
            describe(player != null ? player.ridingEntity : null), describe(this.entity));
    }

    private Vec3 getTargetPosition() {
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

        return Vec3.createVectorHelper(entity.posX + uRoll.xCoord * upHeight,
            entity.posY + uRoll.yCoord * upHeight, entity.posZ + uRoll.zCoord * upHeight);
    }

    private static String describe(Entity value) {
        return value == null ? "null" : value.getClass().getSimpleName() + "#" + value.getEntityId()
            + "@" + System.identityHashCode(value);
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
