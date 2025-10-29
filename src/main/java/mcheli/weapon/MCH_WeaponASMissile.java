package mcheli.weapon;

import mcheli.MCH_RayTracer;
import mcheli.wrapper.W_WorldFunc;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import java.util.List;

public class MCH_WeaponASMissile extends MCH_WeaponBase {

    // Constructor: initialize weapon properties
    public MCH_WeaponASMissile(World world, Vec3 position, float yaw, float pitch, String name, MCH_WeaponInfo weaponInfo) {
        super(world, position, yaw, pitch, name, weaponInfo);
        this.acceleration = 3.0F;  // Acceleration
        this.explosionPower = 9;   // Explosion strength
        this.power = 40;           // Weapon damage
        this.interval = -350;      // Firing interval
        if (world.isRemote) {
            this.interval -= 10;   // On client side, slightly reduce the firing interval
        }
    }

    public boolean isCooldownCountReloadTime() {
        return true;
    }


    public void update(int countWait) {
        super.update(countWait);
    }


    public boolean shot(MCH_WeaponParam params) {

        float yaw = params.user.rotationYaw;  // Get player's yaw angle
        float pitch = params.user.rotationPitch;  // Get player's pitch angle

        // Calculate directional vector components based on yaw/pitch
        double targetX = -MathHelper.sin(yaw / 180.0F * (float) Math.PI) * MathHelper.cos(pitch / 180.0F * (float) Math.PI);
        double targetZ = MathHelper.cos(yaw / 180.0F * (float) Math.PI) * MathHelper.cos(pitch / 180.0F * (float) Math.PI);
        double targetY = -MathHelper.sin(pitch / 180.0F * (float) Math.PI);

        // Compute total direction distance
        double dist = MathHelper.sqrt_double(targetX * targetX + targetY * targetY + targetZ * targetZ);
        double maxDist = 1500.0;
        double segmentLength = 100.0;  // Length per segment
        int numSegments = (int) (maxDist / segmentLength);  // Number of segments to trace

        // Normalize direction across both client and server
        targetX = targetX * maxDist / dist;
        targetY = targetY * maxDist / dist;
        targetZ = targetZ * maxDist / dist;

        // Compute ray origin (launch point)
        Vec3 src = W_WorldFunc.getWorldVec3(this.worldObj, params.entity.posX, params.entity.posY + params.entity.getEyeHeight(), params.entity.posZ);

        // Ray trace initialization
        MovingObjectPosition hitResult = null;

        for (int i = 1; i <= numSegments; i++) {
            // Compute endpoint for this segment — each segment starts where the last ended
            Vec3 currentDst = W_WorldFunc.getWorldVec3(this.worldObj,
                params.entity.posX + targetX * i / numSegments,
                params.entity.posY + params.entity.getEyeHeight() + targetY * i / numSegments,
                params.entity.posZ + targetZ * i / numSegments);

            // Perform ray tracing for blocks
            List<MovingObjectPosition> hitResults = MCH_RayTracer.rayTraceAllBlocks(this.worldObj, src, currentDst, false, true, true);

            if (hitResults != null && !hitResults.isEmpty()) {
                hitResult = hitResults.get(0);
                break;  // Exit loop upon first valid collisio
            }

            // Update starting point for the next segment
            src = currentDst;
        }

        // If no collision was found, fallback to default target position
        if (hitResult == null) {
            hitResult = new MovingObjectPosition(null, src.addVector(targetX, targetY, targetZ));  // Use far target point as default
        }

        // If the ray hits a valid block and it’s not underwater
        if (!this.worldObj.isRemote) {
            // Create missile entity and configure parameters
            MCH_EntityASMissile missile = new MCH_EntityASMissile(this.worldObj, params.posX, params.posY, params.posZ, targetX, targetY, targetZ, yaw, pitch, this.acceleration);
            missile.setName(this.name);
            missile.setParameterFromWeapon(this, params.entity, params.user);
            // Assign missile's target position
            missile.targetPosX = hitResult.hitVec.xCoord;
            missile.targetPosY = hitResult.hitVec.yCoord;
            missile.targetPosZ = hitResult.hitVec.zCoord;

            // Spawn the missile into the world
            this.worldObj.spawnEntityInWorld(missile);

            // Play launch sound effect
            playSound(params.entity);
        }
        return true;  // Hit confirmed and missile successfully launched
    }

}
