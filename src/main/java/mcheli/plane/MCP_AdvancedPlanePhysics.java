package mcheli.plane;

import mcheli.aircraft.MCH_AircraftInfo;
import mcheli.MCH_Lib;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
public class MCP_AdvancedPlanePhysics {

    private static final double DT = 0.05D; // 20 ticks/sec
    private static final double METERS_PER_BLOCK = 3.0D;
    private static final double BLOCKS_PER_TICK_TO_MPS = 20.0D * METERS_PER_BLOCK;
    private static final double MPS_TO_BLOCKS_PER_TICK = 1.0D / (20.0D * METERS_PER_BLOCK);
    private static double smoothedAoA = 0.0D;

    public static void update(MCP_EntityPlane plane) {

        MCH_AircraftInfo info = plane.getAcInfo();
        if (info == null) return;

        double mass = plane.getAdvancedFlightCurrentMass();
        if (mass <= 0.0D) {
            mass = info.massEmpty + info.fuelMass;
        }
        if (mass <= 0.0D) {
            mass = 12000.0D;
        }

        double throttle = plane.getCurrentThrottle();

        double yaw = Math.toRadians(plane.getRotYaw());
        double pitch = Math.toRadians(plane.getRotPitch());

        // Aircraft forward vector
        double forwardX = -Math.sin(yaw) * Math.cos(pitch);
        double forwardY = -Math.sin(pitch);
        double forwardZ = Math.cos(yaw) * Math.cos(pitch);

        // Velocity in blocks/tick
        double vxBT = plane.motionX;
        double vyBT = plane.motionY;
        double vzBT = plane.motionZ;

        // Velocity in m/s
        double vx = vxBT * BLOCKS_PER_TICK_TO_MPS;
        double vy = vyBT * BLOCKS_PER_TICK_TO_MPS;
        double vz = vzBT * BLOCKS_PER_TICK_TO_MPS;

        double speed = Math.sqrt(vx * vx + vy * vy + vz * vz);
        double horizontalSpeed = Math.sqrt(vx * vx + vz * vz);

        // =============================
        // SI-LIKE AIRCRAFT VALUES
        // =============================

        double rho = info.advancedAirDensity;       // kg/m^3, use 1.225 later
        double wingArea = info.advancedWingArea;    // m^2
        double maxThrustN = info.advancedMaxThrust * 1000.0D; // config in kN
        double cd0 = info.advancedDragCoeff;

        // F-16-ish starter values
        double maxCL = 1.45D;
        double liftSlopePerDeg = 0.045D;
        double maxAoA = 18.0D;

        // =============================
        // SIGNED ANGLE OF ATTACK + CL
        // =============================

        double rawAoA = 0.0D;

        if (speed > 1.0D && horizontalSpeed > 1.0D) {
            double forwardHorizontal = Math.sqrt(forwardX * forwardX + forwardZ * forwardZ);
            double noseAngleRad = Math.atan2(forwardY, forwardHorizontal);
            double flightPathRad = Math.atan2(vy, horizontalSpeed);

            rawAoA = Math.toDegrees(noseAngleRad - flightPathRad);
        }

        if (rawAoA > maxAoA) rawAoA = maxAoA;
        if (rawAoA < -maxAoA) rawAoA = -maxAoA;

        smoothedAoA += (rawAoA - smoothedAoA) * 0.10D;

        double baseCL = 0.25D;
        double cl = baseCL + liftSlopePerDeg * smoothedAoA;

        if (Math.abs(smoothedAoA) > 14.0D) {
            double stallFactor = 1.0D - ((Math.abs(smoothedAoA) - 14.0D) / 8.0D);

            if (stallFactor < 0.25D) {
                stallFactor = 0.25D;
            }

            cl *= stallFactor;
        }

        if (cl > maxCL) {
            cl = maxCL;
        }

        if (cl < -0.10D) {
            cl = -0.10D;
        }

        // =============================
        // FORCES
        // =============================

        double qLift = 0.5D * rho * horizontalSpeed * horizontalSpeed;
        double qDrag = 0.5D * rho * speed * speed;

        double liftN = qLift * wingArea * cl;
        double dragN = qDrag * wingArea * cd0;
        double thrustN = maxThrustN * throttle;
        double weightN = mass * 9.81D;

        // Extra induced drag when producing lift
        double inducedDragN = qLift * wingArea * 0.035D * cl * cl;
        dragN += inducedDragN;

        // =============================
        // FORCE DIRECTIONS
        // =============================

        // Thrust follows aircraft nose
        double forceX = forwardX * thrustN;
        double forceY = forwardY * thrustN;
        double forceZ = forwardZ * thrustN;

        // Gravity vertical down
        forceY -= weightN;

        // =============================
        // TRUE LIFT VECTOR
        // =============================
        // Lift is perpendicular to airflow and biased toward the aircraft's
        // local "up" direction. This makes roll affect lift direction.
        // local "up" direction. This makes roll affect lift direction.

        if (speed > 1.0D) {
            double velDirX = vx / speed;
            double velDirY = vy / speed;
            double velDirZ = vz / speed;

            // Aircraft local up vector transformed into world space.
            Vec3 upVec = MCH_Lib.RotVec3(
                    0.0D, 1.0D, 0.0D,
                    -plane.getRotYaw(),
                    -plane.getRotPitch(),
                    -plane.getRotRoll()
            );

            double upX = upVec.xCoord;
            double upY = upVec.yCoord;
            double upZ = upVec.zCoord;

            // Project aircraft up vector onto the plane perpendicular to velocity.
            // This guarantees lift is perpendicular to airflow.
            double dot = upX * velDirX + upY * velDirY + upZ * velDirZ;

            double liftDirX = upX - velDirX * dot;
            double liftDirY = upY - velDirY * dot;
            double liftDirZ = upZ - velDirZ * dot;

            double liftLen = Math.sqrt(liftDirX * liftDirX + liftDirY * liftDirY + liftDirZ * liftDirZ);

            if (liftLen > 1.0E-6D) {
                liftDirX /= liftLen;
                liftDirY /= liftLen;
                liftDirZ /= liftLen;

                forceX += liftDirX * liftN;
                forceY += liftDirY * liftN;
                forceZ += liftDirZ * liftN;
            }
        }

        // Drag opposite velocity vector
        if (speed > 0.5D) {
            forceX -= (vx / speed) * dragN;
            forceY -= (vy / speed) * dragN;
            forceZ -= (vz / speed) * dragN;
        }

        // =============================
        // SIDESLIP / LATERAL DAMPING
        // =============================
        // Prevents aircraft from "strafing" sideways like a UFO.
        // Removes velocity perpendicular to the aircraft nose direction.

        double forwardSpeed = vx * forwardX + vy * forwardY + vz * forwardZ;

        double sideVelX = vx - forwardX * forwardSpeed;
        double sideVelY = vy - forwardY * forwardSpeed;
        double sideVelZ = vz - forwardZ * forwardSpeed;

        // Do not damp vertical flight-path component too aggressively yet.
        // Main goal is to remove sideways horizontal sliding.
        sideVelY *= 0.25D;

        double sideSpeed = Math.sqrt(sideVelX * sideVelX + sideVelY * sideVelY + sideVelZ * sideVelZ);

        if (sideSpeed > 0.5D) {
            double sideDampingCoeff = 2.5D;

            double sideForce = sideDampingCoeff * sideSpeed * sideSpeed * mass;

            forceX -= (sideVelX / sideSpeed) * sideForce;
            forceY -= (sideVelY / sideSpeed) * sideForce;
            forceZ -= (sideVelZ / sideSpeed) * sideForce;
        }

        // =============================
        // ACCELERATION + INTEGRATION
        // =============================

        double ax = forceX / mass;
        double ay = forceY / mass;
        double az = forceZ / mass;

        vx += ax * DT;
        vy += ay * DT;
        vz += az * DT;

        plane.motionX = vx * MPS_TO_BLOCKS_PER_TICK;
        plane.motionY = vy * MPS_TO_BLOCKS_PER_TICK;
        plane.motionZ = vz * MPS_TO_BLOCKS_PER_TICK;

        if (plane.onGround) {
            if (plane.motionY < 0.0D) {
                plane.motionY = 0.0D;
            }

            // wheel friction
            plane.motionX *= 0.999D;
            plane.motionZ *= 0.999D;

            // only leave ground if lift beats weight
            if (liftN <= weightN) {
                plane.motionY = 0.0D;
            }
        }

        double rollInput = 0.0D;

        boolean isAirborne = !plane.onGround && plane.worldObj.getCollidingBoundingBoxes(
                plane,
                plane.boundingBox.copy().offset(0.0D, -0.35D, 0.0D)
        ).isEmpty();

        if (isAirborne) {
            if (plane.moveLeft && !plane.moveRight) {
                rollInput = -1.0D;
            }

            if (plane.moveRight && !plane.moveLeft) {
                rollInput = 1.0D;
            }
        }

        plane.advancedRollInput = rollInput;

        if (!isAirborne) {
            plane.advancedRollRate *= 0.70D;
            plane.setRotRoll((float)(plane.getRotRoll() * 0.85F));
        }
        // =============================
        // ANGULAR INERTIA
        // =============================

        double controlSpeedFactor = speed / 120.0D;

        if (controlSpeedFactor < 0.15D) {
            controlSpeedFactor = 0.15D;
        }

        if (controlSpeedFactor > 1.0D) {
            controlSpeedFactor = 1.0D;
        }

        // Control accelerations in deg/sec^2
        double pitchAccel = plane.advancedPitchInput * 65.0D * controlSpeedFactor;
        double rollAccel = plane.advancedRollInput * 220.0D * controlSpeedFactor;
        double yawAccel = plane.advancedYawInput * 35.0D * controlSpeedFactor;

        // Apply angular acceleration
        plane.advancedPitchRate += pitchAccel * DT;
        plane.advancedRollRate += rollAccel * DT;
        plane.advancedYawRate += yawAccel * DT;

        // Damping / stability
        plane.advancedPitchRate *= 0.94D;
        plane.advancedRollRate *= 0.95D;
        plane.advancedYawRate *= 0.90D;

        // Clamp angular rates in deg/sec
        if (plane.advancedPitchRate > 75.0D) plane.advancedPitchRate = 75.0D;
        if (plane.advancedPitchRate < -75.0D) plane.advancedPitchRate = -75.0D;

        if (plane.advancedRollRate > 160.0D) plane.advancedRollRate = 160.0D;
        if (plane.advancedRollRate < -160.0D) plane.advancedRollRate = -160.0D;

        if (plane.advancedYawRate > 45.0D) plane.advancedYawRate = 45.0D;
        if (plane.advancedYawRate < -45.0D) plane.advancedYawRate = -45.0D;

        // Integrate rotation
        plane.setRotPitch((float)(plane.getRotPitch() + plane.advancedPitchRate * DT));
        plane.setRotRoll((float)(plane.getRotRoll() + plane.advancedRollRate * DT));
        plane.setRotYaw((float)(plane.getRotYaw() + plane.advancedYawRate * DT));

        // Roll slowly returns toward level when player is not rolling
        if (Math.abs(plane.advancedRollInput) < 0.05D) {
            plane.setRotRoll((float)(plane.getRotRoll() * 0.995D));
        }

        // Prevent pitch from going insane during testing
        if (plane.getRotPitch() > 85.0F) {
            plane.setRotPitch(85.0F);
        }

        if (plane.getRotPitch() < -85.0F) {
            plane.setRotPitch(-85.0F);
        }

        if (plane.getRotRoll() > 180.0F) {
            plane.setRotRoll(plane.getRotRoll() - 360.0F);
        }

        if (plane.getRotRoll() < -180.0F) {
            plane.setRotRoll(plane.getRotRoll() + 360.0F);
        }

        if (plane.getRotRoll() > 180.0F) {
            plane.setRotRoll(plane.getRotRoll() - 360.0F);
        }

        if (plane.getRotRoll() < -180.0F) {
            plane.setRotRoll(plane.getRotRoll() + 360.0F);
        }

        // Sync final roll value after all roll changes
        if (!plane.worldObj.isRemote) {
            plane.getDataWatcher().updateObject(26, MathHelper.wrapAngleTo180_float(plane.getRotRoll()));
        }

        plane.moveEntity(plane.motionX, plane.motionY, plane.motionZ);

        if (!plane.worldObj.isRemote && info.advancedFlightDebug && plane.ticksExisted % 20 == 0) {
            MCH_Lib.Log("[AdvPhys] %s speed=%.1f m/s hs=%.1f m/s AoA=%.1f CL=%.2f Lift/W=%.2f Thrust=%.0fN Drag=%.0fN",
                    info.name,
                    speed,
                    horizontalSpeed,
                    smoothedAoA,
                    cl,
                    liftN / weightN,
                    thrustN,
                    dragN);

            MCH_Lib.Log("[AdvInput] pitchIn=%.2f rollIn=%.2f yawIn=%.2f pitchRate=%.2f rollRate=%.2f yawRate=%.2f",
                    plane.advancedPitchInput,
                    plane.advancedRollInput,
                    plane.advancedYawInput,
                    plane.advancedPitchRate,
                    plane.advancedRollRate,
                    plane.advancedYawRate);
        }
    }
}