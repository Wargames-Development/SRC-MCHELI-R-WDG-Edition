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

        // Clamp raw AoA before filtering so bad transient values cannot spike lift.
        if (rawAoA > maxAoA) rawAoA = maxAoA;
        if (rawAoA < -maxAoA) rawAoA = -maxAoA;

        // Per-aircraft AoA smoothing.
        // Higher value = faster lift response, lower value = less oscillation.
        double aoaResponse = 0.08D;
        plane.advancedSmoothedAoA += (rawAoA - plane.advancedSmoothedAoA) * aoaResponse;

        // Starter F-16-ish CL curve.
        // Keep this conservative until pitch + AoA behavior feels stable.
        double baseCL = 0.18D;
        double clLinear = baseCL + liftSlopePerDeg * plane.advancedSmoothedAoA;

        // Soft stall starts before maxAoA.
        // This avoids the previous hard-ish lift behavior near 14–18 deg.
        double stallStartAoA = 13.0D;
        double stallFullAoA = 22.0D;

        double absAoA = Math.abs(plane.advancedSmoothedAoA);
        double stallFactor = 1.0D;

        if (absAoA > stallStartAoA) {
            stallFactor = 1.0D - ((absAoA - stallStartAoA) / (stallFullAoA - stallStartAoA));

            if (stallFactor < 0.30D) {
                stallFactor = 0.30D;
            }
        }

        double targetCL = clLinear * stallFactor;

        // Final CL clamp.
        // Negative CL is allowed, but limited so inverted/negative-AoA behavior is not explosive yet.
        if (targetCL > maxCL) {
            targetCL = maxCL;
        }

        if (targetCL < -0.35D) {
            targetCL = -0.35D;
        }

        // CL smoothing prevents lift spikes when pitch input changes quickly.
        double clResponse = 0.12D;
        plane.advancedSmoothedCL += (targetCL - plane.advancedSmoothedCL) * clResponse;

        double cl = plane.advancedSmoothedCL;

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

        // Pitch input comes from MCP_PlanePacketPlayerControl.
        // Do not overwrite it here.
        if (!isAirborne) {
            plane.advancedPitchInput = 0.0D;
            plane.advancedPitchRate *= 0.70D;
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

        // =============================
        // ROLL MOMENT MODEL
        // =============================
        // q = dynamic pressure
        // controlForce = q * controlArea * controlPower * input
        // rollMoment = controlForce * momentArm
        // angularAccel = moment / inertia

        double qControl = 0.5D * rho * speed * speed;

        // Pitch now uses the same dynamic-pressure moment model as roll.
        // Elevator/stabilator force = q * area * controlPower * input
        // Pitch moment = force * momentArm
        // Angular accel = moment / inertia
        double pitchControlForce =
                qControl
                        * info.advancedPitchControlArea
                        * info.advancedPitchControlPower
                        * plane.advancedPitchInput;

        double pitchMoment =
                pitchControlForce
                        * info.advancedPitchMomentArm;

        // =============================
        // LONGITUDINAL STABILITY
        // =============================
        // Adds natural nose-restoring tendency based on AoA.
        // Positive AoA creates nose-down restoring moment.
        // Negative AoA creates nose-up restoring moment.
        double pitchStabilityPower = 0.018D;

        // Pitch-rate damping resists rapid pitch rotation.
        // This prevents porpoising and over-control.
        double pitchRateDampingPower = 0.030D;

        double aoaStabilityMoment =
                -plane.advancedSmoothedAoA
                        * qControl
                        * info.advancedWingArea
                        * pitchStabilityPower;

        double pitchRateDampingMoment =
                -plane.advancedPitchRate
                        * qControl
                        * info.advancedWingArea
                        * pitchRateDampingPower;

        pitchMoment += aoaStabilityMoment + pitchRateDampingMoment;

        // Sign may need flipping depending on MCHR pitch convention.
        // If pressing pitch-up makes the nose go down, multiply this by -1.0D.
        double pitchAccelRad =
                pitchMoment / info.advancedPitchInertia;

        double pitchAccel =
                Math.toDegrees(pitchAccelRad);

        double rollControlForce =
                qControl
                        * info.advancedRollControlArea
                        * info.advancedRollControlPower
                        * plane.advancedRollInput;

        double rollMoment =
                rollControlForce
                        * info.advancedRollMomentArm;

        double rollAccelRad =
                rollMoment / info.advancedRollInertia;

        double rollAccel =
                Math.toDegrees(rollAccelRad);
        double yawAccel = plane.advancedYawInput * 35.0D * controlSpeedFactor;

        // Apply angular acceleration
        plane.advancedPitchRate += pitchAccel * DT;
        plane.advancedRollRate += rollAccel * DT;
        plane.advancedYawRate += yawAccel * DT;

        // Damping / stability
        plane.advancedPitchRate *= info.advancedPitchDamping;
        plane.advancedRollRate *= info.advancedRollDamping;
        plane.advancedYawRate *= 0.90D;

        // Clamp angular rates in deg/sec
        double maxPitchRate = info.advancedMaxPitchRate;

        if (plane.advancedPitchRate > maxPitchRate) {
            plane.advancedPitchRate = maxPitchRate;
        }

        if (plane.advancedPitchRate < -maxPitchRate) {
            plane.advancedPitchRate = -maxPitchRate;
        }

        double maxRollRate = info.advancedMaxRollRate;

        if (plane.advancedRollRate > maxRollRate) {
            plane.advancedRollRate = maxRollRate;
        }

        if (plane.advancedRollRate < -maxRollRate) {
            plane.advancedRollRate = -maxRollRate;
        }

        if (plane.advancedYawRate > 45.0D) plane.advancedYawRate = 45.0D;
        if (plane.advancedYawRate < -45.0D) plane.advancedYawRate = -45.0D;

        // Integrate rotation using the same pattern that already works for roll.
        plane.setRotPitch((float)(plane.getRotPitch() + plane.advancedPitchRate * DT));
        plane.setRotRoll((float)(plane.getRotRoll() + plane.advancedRollRate * DT));
        plane.setRotYaw((float)(plane.getRotYaw() + plane.advancedYawRate * DT));

        // Roll slowly returns toward level when player is not rolling
        if (Math.abs(plane.advancedRollInput) < 0.05D) {
            plane.setRotRoll((float)(plane.getRotRoll() * 0.995D));
        }

        // Sync vanilla entity rotation fields after final clamped attitude.
        plane.rotationPitch = plane.getRotPitch();
        plane.rotationYaw = plane.getRotYaw();

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

        if (!plane.worldObj.isRemote) {
            plane.getDataWatcher().updateObject(26, MathHelper.wrapAngleTo180_float(plane.getRotRoll()));
            plane.getDataWatcher().updateObject(28, MathHelper.wrapAngleTo180_float(plane.getRotPitch()));
        }

        plane.moveEntity(plane.motionX, plane.motionY, plane.motionZ);

        if (!plane.worldObj.isRemote && info.advancedFlightDebug && plane.ticksExisted % 20 == 0) {
            MCH_Lib.Log("[AdvPhys] %s speed=%.1f m/s hs=%.1f m/s AoA=%.1f CL=%.2f Lift/W=%.2f Thrust=%.0fN Drag=%.0fN",
                    info.name,
                    speed,
                    horizontalSpeed,
                    plane.advancedSmoothedAoA,
                    cl,
                    liftN / weightN,
                    thrustN,
                    dragN);

            MCH_Lib.Log("[AdvInput] pitchIn=%.2f rollIn=%.2f yawIn=%.2f pitchRate=%.2f rollRate=%.2f yawRate=%.2f rotPitch=%.1f rotRoll=%.1f",
                    plane.advancedPitchInput,
                    plane.advancedRollInput,
                    plane.advancedYawInput,
                    plane.advancedPitchRate,
                    plane.advancedRollRate,
                    plane.advancedYawRate,
                    plane.getRotPitch(),
                    plane.getRotRoll());
        }
    }
}