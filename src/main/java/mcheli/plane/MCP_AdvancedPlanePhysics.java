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

        // Phase 1: keep quaternion as a shadow copy of the current MCHeli attitude.
        // This must not change flight behavior yet.
        if (!plane.advancedQuatInitialized) {
            syncQuatFromEuler(plane);
        }

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
            double[] fwdAxis = getAircraftForwardAxis(plane);
            double[] rightAxis = getAircraftRightAxis(plane);
            double[] upAxis = getAircraftUpAxis(plane);

            double[] targetDir = normalizeVec(new double[] {
                    plane.advancedTargetDirX,
                    plane.advancedTargetDirY,
                    plane.advancedTargetDirZ
            });

            double targetForward = dotVec(targetDir, fwdAxis);
            double targetRight = dotVec(targetDir, rightAxis);
            double targetUp = dotVec(targetDir, upAxis);

            double pitchErrorDeg = Math.toDegrees(Math.atan2(targetUp, targetForward));
            double yawErrorDeg = Math.toDegrees(Math.atan2(targetRight, targetForward));

            plane.advancedLocalPitchError = pitchErrorDeg;
            plane.advancedLocalYawError = yawErrorDeg;

            double pitchKp = 0.060D;
            double pitchKd = 0.010D;

            double targetPitchInput =
                    pitchErrorDeg * pitchKp
                            - plane.advancedPitchRate * pitchKd;

            if (targetPitchInput > 1.0D) targetPitchInput = 1.0D;
            if (targetPitchInput < -1.0D) targetPitchInput = -1.0D;

            plane.advancedPitchInput = targetPitchInput;

            // Keep yaw disabled for now until pitch works safely.
            plane.advancedYawInput = 0.0D;
        } else {
            plane.advancedPitchInput = 0.0D;
            plane.advancedYawInput = 0.0D;
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

        // =============================
// PHASE 2B: LIVE QUATERNION PITCH + ROLL
// =============================
// Pitch rotates around the aircraft local right / wing-to-wing axis.
// Roll rotates around the aircraft local forward axis.
// Yaw remains simple for now because yaw control is not active yet.

// Keep yaw on the stable Euler path for now.
        plane.setRotYaw((float)(plane.getRotYaw() + plane.advancedYawRate * DT));

// Start from current visible attitude.
        syncQuatFromEuler(plane);

        double pitchStepRad = Math.toRadians(plane.advancedPitchRate * DT);
        double rollStepRad = Math.toRadians(plane.advancedRollRate * DT);

        boolean quatChanged = false;

        if (Math.abs(pitchStepRad) > 1.0E-8D) {
            double[] pitchAxis = getAircraftRightAxis(plane);

            // Pitch around aircraft local wing-to-wing axis.
            rotateAircraftQuatWorldAxis(plane, pitchAxis, pitchStepRad);

            quatChanged = true;
        }

        if (Math.abs(rollStepRad) > 1.0E-8D) {
            double[] rollAxis = getAircraftForwardAxis(plane);

            // Roll around aircraft local nose/tail axis.
            rotateAircraftQuatWorldAxis(plane, rollAxis, rollStepRad);

            quatChanged = true;
        }

        if (quatChanged) {
            applyEulerFromQuat(plane);
        } else {
            syncQuatFromEuler(plane);
        }

        // Roll slowly returns toward level when player is not rolling
        // Roll slowly returns toward level when player is not rolling.
        // Keep quaternion shadow matched after this Euler-leveling helper.
        if (Math.abs(plane.advancedRollInput) < 0.05D) {
            plane.setRotRoll((float)(plane.getRotRoll() * 0.995D));
            syncQuatFromEuler(plane);
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

        // =============================
        // ADVANCED PHYSICS SAFETY GUARD
        // =============================
        if (!isFinite(plane.motionX) || !isFinite(plane.motionY) || !isFinite(plane.motionZ)
                || !isFinite(plane.getRotPitch()) || !isFinite(plane.getRotYaw()) || !isFinite(plane.getRotRoll())) {

            plane.motionX = 0.0D;
            plane.motionY = 0.0D;
            plane.motionZ = 0.0D;

            plane.advancedPitchRate = 0.0D;
            plane.advancedRollRate = 0.0D;
            plane.advancedYawRate = 0.0D;

            plane.advancedPitchInput = 0.0D;
            plane.advancedRollInput = 0.0D;
            plane.advancedYawInput = 0.0D;

            return;
        }

// Prevent corrupt save / void kick if physics ever sends entity out of sane range.
        if (Math.abs(plane.posX) > 2.9E7D || Math.abs(plane.posZ) > 2.9E7D || Math.abs(plane.posY) > 1000000.0D) {
            plane.setDead();
            return;
        }

// Clamp motion to avoid single-tick world jumps.
        double maxMotionBT = 10.0D;
        double motionLen = Math.sqrt(
                plane.motionX * plane.motionX
                        + plane.motionY * plane.motionY
                        + plane.motionZ * plane.motionZ
        );

        if (motionLen > maxMotionBT) {
            double scale = maxMotionBT / motionLen;
            plane.motionX *= scale;
            plane.motionY *= scale;
            plane.motionZ *= scale;
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

            MCH_Lib.Log("[AdvInput] pitchIn=%.2f localPitchErr=%.1f localYawErr=%.1f rollIn=%.2f yawIn=%.2f pitchRate=%.2f rollRate=%.2f yawRate=%.2f rotPitch=%.1f rotRoll=%.1f targetDir=(%.2f, %.2f, %.2f)",
                    plane.advancedPitchInput,
                    plane.advancedLocalPitchError,
                    plane.advancedLocalYawError,
                    plane.advancedRollInput,
                    plane.advancedYawInput,
                    plane.advancedPitchRate,
                    plane.advancedRollRate,
                    plane.advancedYawRate,
                    plane.getRotPitch(),
                    plane.getRotRoll(),
                    plane.advancedTargetDirX,
                    plane.advancedTargetDirY,
                    plane.advancedTargetDirZ);

            double[] qEuler = getEulerFromAircraftQuat(plane);

            double dYaw = wrapDeg(qEuler[0] - plane.getRotYaw());
            double dPitch = wrapDeg(qEuler[1] - plane.getRotPitch());
            double dRoll = wrapDeg(qEuler[2] - plane.getRotRoll());

            MCH_Lib.Log("[AdvQuatRT] srcYaw=%.1f outYaw=%.1f dYaw=%.2f srcPitch=%.1f outPitch=%.1f dPitch=%.2f srcRoll=%.1f outRoll=%.1f dRoll=%.2f",
                    plane.getRotYaw(),
                    qEuler[0],
                    dYaw,
                    plane.getRotPitch(),
                    qEuler[1],
                    dPitch,
                    plane.getRotRoll(),
                    qEuler[2],
                    dRoll);
        }
    }

    private static void normalizeAircraftQuat(MCP_EntityPlane plane) {
        double len = Math.sqrt(
                plane.advancedQw * plane.advancedQw
                        + plane.advancedQx * plane.advancedQx
                        + plane.advancedQy * plane.advancedQy
                        + plane.advancedQz * plane.advancedQz
        );

        if (len < 1.0E-8D || Double.isNaN(len) || Double.isInfinite(len)) {
            plane.advancedQw = 1.0D;
            plane.advancedQx = 0.0D;
            plane.advancedQy = 0.0D;
            plane.advancedQz = 0.0D;
            plane.advancedQuatInitialized = false;
            return;
        }

        plane.advancedQw /= len;
        plane.advancedQx /= len;
        plane.advancedQy /= len;
        plane.advancedQz /= len;
    }

    private static void syncQuatFromEuler(MCP_EntityPlane plane) {
        // Build quaternion from MCHeli's current yaw/pitch/roll.
        // This is only a shadow copy for Phase 1.
        Vec3 fwdVec = MCH_Lib.RotVec3(
                0.0D, 0.0D, 1.0D,
                -plane.getRotYaw(),
                -plane.getRotPitch(),
                -plane.getRotRoll()
        );

        Vec3 upVec = MCH_Lib.RotVec3(
                0.0D, 1.0D, 0.0D,
                -plane.getRotYaw(),
                -plane.getRotPitch(),
                -plane.getRotRoll()
        );

        Vec3 rightVec = MCH_Lib.RotVec3(
                1.0D, 0.0D, 0.0D,
                -plane.getRotYaw(),
                -plane.getRotPitch(),
                -plane.getRotRoll()
        );

        // Rotation matrix columns are local right, up, forward in world space.
        double m00 = rightVec.xCoord;
        double m01 = upVec.xCoord;
        double m02 = fwdVec.xCoord;

        double m10 = rightVec.yCoord;
        double m11 = upVec.yCoord;
        double m12 = fwdVec.yCoord;

        double m20 = rightVec.zCoord;
        double m21 = upVec.zCoord;
        double m22 = fwdVec.zCoord;

        double trace = m00 + m11 + m22;

        if (trace > 0.0D) {
            double s = Math.sqrt(trace + 1.0D) * 2.0D;
            plane.advancedQw = 0.25D * s;
            plane.advancedQx = (m21 - m12) / s;
            plane.advancedQy = (m02 - m20) / s;
            plane.advancedQz = (m10 - m01) / s;
        } else if (m00 > m11 && m00 > m22) {
            double s = Math.sqrt(1.0D + m00 - m11 - m22) * 2.0D;
            plane.advancedQw = (m21 - m12) / s;
            plane.advancedQx = 0.25D * s;
            plane.advancedQy = (m01 + m10) / s;
            plane.advancedQz = (m02 + m20) / s;
        } else if (m11 > m22) {
            double s = Math.sqrt(1.0D + m11 - m00 - m22) * 2.0D;
            plane.advancedQw = (m02 - m20) / s;
            plane.advancedQx = (m01 + m10) / s;
            plane.advancedQy = 0.25D * s;
            plane.advancedQz = (m12 + m21) / s;
        } else {
            double s = Math.sqrt(1.0D + m22 - m00 - m11) * 2.0D;
            plane.advancedQw = (m10 - m01) / s;
            plane.advancedQx = (m02 + m20) / s;
            plane.advancedQy = (m12 + m21) / s;
            plane.advancedQz = 0.25D * s;
        }

        normalizeAircraftQuat(plane);
        plane.advancedQuatInitialized = true;
    }

    private static double dotVec(double[] a, double[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    private static double[] crossVec(double[] a, double[] b) {
        return new double[] {
                a[1] * b[2] - a[2] * b[1],
                a[2] * b[0] - a[0] * b[2],
                a[0] * b[1] - a[1] * b[0]
        };
    }

    private static double[] normalizeVec(double[] v) {
        double len = Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);

        if (len < 1.0E-8D || Double.isNaN(len) || Double.isInfinite(len)) {
            return new double[] {0.0D, 0.0D, 0.0D};
        }

        return new double[] {
                v[0] / len,
                v[1] / len,
                v[2] / len
        };
    }

    private static double[] getAircraftForwardAxis(MCP_EntityPlane plane) {
        return normalizeVec(rotateLocalVectorByAircraftQuat(plane, 0.0D, 0.0D, 1.0D));
    }

    private static double[] getAircraftRightAxis(MCP_EntityPlane plane) {
        return normalizeVec(rotateLocalVectorByAircraftQuat(plane, 1.0D, 0.0D, 0.0D));
    }

    private static double[] getAircraftUpAxis(MCP_EntityPlane plane) {
        return normalizeVec(rotateLocalVectorByAircraftQuat(plane, 0.0D, 1.0D, 0.0D));
    }

    private static double[] rotateLocalVectorByAircraftQuat(MCP_EntityPlane plane, double x, double y, double z) {
        normalizeAircraftQuat(plane);

        double qw = plane.advancedQw;
        double qx = plane.advancedQx;
        double qy = plane.advancedQy;
        double qz = plane.advancedQz;

        // v' = v + 2*qw*(q.xyz x v) + 2*(q.xyz x (q.xyz x v))
        double tx = 2.0D * (qy * z - qz * y);
        double ty = 2.0D * (qz * x - qx * z);
        double tz = 2.0D * (qx * y - qy * x);

        return new double[] {
                x + qw * tx + (qy * tz - qz * ty),
                y + qw * ty + (qz * tx - qx * tz),
                z + qw * tz + (qx * ty - qy * tx)
        };
    }

    private static void rotateAircraftQuatWorldAxis(MCP_EntityPlane plane, double[] axisIn, double angleRad) {
        double[] axis = normalizeVec(axisIn);

        double half = angleRad * 0.5D;
        double s = Math.sin(half);

        double dw = Math.cos(half);
        double dx = axis[0] * s;
        double dy = axis[1] * s;
        double dz = axis[2] * s;

        // qNew = deltaWorld * qCurrent
        double qw = plane.advancedQw;
        double qx = plane.advancedQx;
        double qy = plane.advancedQy;
        double qz = plane.advancedQz;

        plane.advancedQw = dw * qw - dx * qx - dy * qy - dz * qz;
        plane.advancedQx = dw * qx + dx * qw + dy * qz - dz * qy;
        plane.advancedQy = dw * qy - dx * qz + dy * qw + dz * qx;
        plane.advancedQz = dw * qz + dx * qy - dy * qx + dz * qw;

        normalizeAircraftQuat(plane);
    }

    private static void applyEulerFromQuat(MCP_EntityPlane plane) {
        double[] fwd = getAircraftForwardAxis(plane);
        double[] up = getAircraftUpAxis(plane);

        double newYaw = Math.toDegrees(Math.atan2(-fwd[0], fwd[2]));
        double newPitch = Math.toDegrees(Math.atan2(
                -fwd[1],
                Math.sqrt(fwd[0] * fwd[0] + fwd[2] * fwd[2])
        ));

        Vec3 noRollUpVec = MCH_Lib.RotVec3(
                0.0D, 1.0D, 0.0D,
                (float)-newYaw,
                (float)-newPitch,
                0.0F
        );

        Vec3 noRollRightVec = MCH_Lib.RotVec3(
                1.0D, 0.0D, 0.0D,
                (float)-newYaw,
                (float)-newPitch,
                0.0F
        );

        double[] noRollUp = normalizeVec(new double[] {
                noRollUpVec.xCoord,
                noRollUpVec.yCoord,
                noRollUpVec.zCoord
        });

        double[] noRollRight = normalizeVec(new double[] {
                noRollRightVec.xCoord,
                noRollRightVec.yCoord,
                noRollRightVec.zCoord
        });

        double newRoll = -Math.toDegrees(Math.atan2(
                dotVec(up, noRollRight),
                dotVec(up, noRollUp)
        ));

        plane.setRotYaw(MathHelper.wrapAngleTo180_float((float)newYaw));
        plane.setRotPitch((float)newPitch);
        plane.setRotRoll(MathHelper.wrapAngleTo180_float((float)newRoll));

        plane.rotationYaw = plane.getRotYaw();
        plane.rotationPitch = plane.getRotPitch();
    }

    private static double wrapDeg(double a) {
        while (a > 180.0D) {
            a -= 360.0D;
        }

        while (a < -180.0D) {
            a += 360.0D;
        }

        return a;
    }

    private static double[] getEulerFromAircraftQuat(MCP_EntityPlane plane) {
        double[] fwd = getAircraftForwardAxis(plane);
        double[] up = getAircraftUpAxis(plane);

        double yaw = Math.toDegrees(Math.atan2(-fwd[0], fwd[2]));

        double pitch = Math.toDegrees(Math.atan2(
                -fwd[1],
                Math.sqrt(fwd[0] * fwd[0] + fwd[2] * fwd[2])
        ));

        Vec3 noRollUpVec = MCH_Lib.RotVec3(
                0.0D, 1.0D, 0.0D,
                (float)-yaw,
                (float)-pitch,
                0.0F
        );

        Vec3 noRollRightVec = MCH_Lib.RotVec3(
                1.0D, 0.0D, 0.0D,
                (float)-yaw,
                (float)-pitch,
                0.0F
        );

        double[] noRollUp = normalizeVec(new double[] {
                noRollUpVec.xCoord,
                noRollUpVec.yCoord,
                noRollUpVec.zCoord
        });

        double[] noRollRight = normalizeVec(new double[] {
                noRollRightVec.xCoord,
                noRollRightVec.yCoord,
                noRollRightVec.zCoord
        });

        double roll = -Math.toDegrees(Math.atan2(
                dotVec(up, noRollRight),
                dotVec(up, noRollUp)
        ));

        return new double[] {
                wrapDeg(yaw),
                wrapDeg(pitch),
                wrapDeg(roll)
        };
    }

    private static boolean isFinite(double v) {
        return !Double.isNaN(v) && !Double.isInfinite(v);
    }
}