package mcheli.weapon;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mcheli.MCH_MOD;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.aircraft.MCH_EntitySeat;
import mcheli.network.packets.PacketLaserGuidanceTargeting;
import mcheli.uav.MCH_EntityUavStation;
import mcheli.wrapper.W_WorldFunc;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import java.util.List;

import static mcheli.MCH_RayTracer.rayTraceAllBlocks;

public class MCH_LaserGuidanceSystem implements MCH_IGuidanceSystem {

    public World worldObj;
    protected Entity user;
    public double targetPosX;
    public double targetPosY;
    public double targetPosZ;
    public boolean targeting = false;
    @SideOnly(Side.CLIENT)
    public MCH_EntityLockBox lockBox;
    public boolean hasLaserGuidancePod = true;

    @Override
    public double getLockPosX() {
        return targetPosX;
    }

    @Override
    public double getLockPosY() {
        return targetPosY;
    }

    @Override
    public double getLockPosZ() {
        return targetPosZ;
    }

    @Override
    public void update() {

        if(worldObj.isRemote) {

            if(!targeting) return;

            float yaw;
            float pitch;
            MCH_EntityAircraft ac = null; // The entity the player is riding
            if(user.ridingEntity instanceof MCH_EntityAircraft) {
                ac = (MCH_EntityAircraft)user.ridingEntity;
            } else if(user.ridingEntity instanceof MCH_EntitySeat) {
                ac = ((MCH_EntitySeat)user.ridingEntity).getParent();
            } else if(user.ridingEntity instanceof MCH_EntityUavStation) {
                ac = ((MCH_EntityUavStation)user.ridingEntity).getControlAircract();
            }

            // Use player's orientation if using a laser designator
            if (hasLaserGuidancePod) {
                yaw = user.rotationYaw;  // Player's yaw (horizontal rotation)
                pitch = user.rotationPitch;  // Player's pitch (vertical rotation)
            } else {
                if(ac == null) return;
                yaw = ac.rotationYaw;
                pitch = ac.rotationPitch;
            }

            // Calculate 3D directional vector based on orientation
            double targetX = -MathHelper.sin(yaw / 180.0F * (float) Math.PI) * MathHelper.cos(pitch / 180.0F * (float) Math.PI);
            double targetZ = MathHelper.cos(yaw / 180.0F * (float) Math.PI) * MathHelper.cos(pitch / 180.0F * (float) Math.PI);
            double targetY = -MathHelper.sin(pitch / 180.0F * (float) Math.PI);

            // Compute direction magnitude
            double dist = MathHelper.sqrt_double(targetX * targetX + targetY * targetY + targetZ * targetZ);
            double maxDist = 1500.0;    // Maximum targeting distance (in meters/blocks)
            double segmentLength = 100.0;  // Length of each ray segment
            int numSegments = (int) (maxDist / segmentLength);  // Number of segments to trace

            // Normalize and scale direction vector
            targetX = targetX * maxDist / dist;
            targetY = targetY * maxDist / dist;
            targetZ = targetZ * maxDist / dist;

//            double posX = user.posX;
//            double posY = user.posY + user.getEyeHeight();
//            double posZ = user.posZ;

            double posX = RenderManager.renderPosX;
            double posY = RenderManager.renderPosY;
            double posZ = RenderManager.renderPosZ;

            if(ac != null) {
//                if (!ac.isUAV()) {
//                    double interpolatedPosX = ac.prevPosX + (ac.posX - ac.prevPosX) * 0.5;
//                    double interpolatedPosY = ac.prevPosY + (ac.posY - ac.prevPosY) * 0.5;
//                    double interpolatedPosZ = ac.prevPosZ + (ac.posZ - ac.prevPosZ) * 0.5;
//                    posX = interpolatedPosX;
//                    posY = interpolatedPosY;
//                    posZ = interpolatedPosZ;
//                } else {
//                    posX = RenderManager.renderPosX;
//                    posY = RenderManager.renderPosY;
//                    posZ = RenderManager.renderPosZ;
//                }
                posX = RenderManager.renderPosX;
                posY = RenderManager.renderPosY;
                posZ = RenderManager.renderPosZ;
            }

            // Compute the source vector (origin of laser beam)
            Vec3 src = W_WorldFunc.getWorldVec3(this.worldObj, posX, posY, posZ);

            // Perform ray tracing
            MovingObjectPosition hitResult = null;

            for (int i = 1; i <= numSegments; i++) {
                // Compute target point for this segment, each starting from the previous endpoint
                Vec3 currentDst = W_WorldFunc.getWorldVec3(this.worldObj,
                        posX + targetX * i / numSegments,
                        posY + targetY * i / numSegments,
                        posZ + targetZ * i / numSegments);

                // Perform collision detection (ray trace)
                List<MovingObjectPosition> hitResults = rayTraceAllBlocks(this.worldObj, src, currentDst, false, true, true);

                if (hitResults != null && !hitResults.isEmpty()) {
                    hitResult = hitResults.get(0);
                    break;  // Exit loop when a collision is found
                }

                // Update src to current segment endpoint for next iteration
                src = currentDst;
            }

            // If no collision was detected, default to the farthest target point
            if (hitResult == null) {
                hitResult = new MovingObjectPosition(null, src.addVector(targetX, targetY, targetZ));  // Use end-point as fallback
            }

            // Set missile target position
            targetPosX = hitResult.hitVec.xCoord;
            targetPosY = hitResult.hitVec.yCoord;
            targetPosZ = hitResult.hitVec.zCoord;

            // Send target update packet to server
            MCH_MOD.getPacketHandler().sendToServer(new PacketLaserGuidanceTargeting(true, targetPosX,targetPosY,targetPosZ));

            // Update or spawn target lock box entity
            if(lockBox != null) {
                lockBox.setPosition(targetPosX, targetPosY, targetPosZ);
            } else {
                lockBox = new MCH_EntityLockBox(worldObj);
                worldObj.spawnEntityInWorld(lockBox);
            }
        }
    }
}
