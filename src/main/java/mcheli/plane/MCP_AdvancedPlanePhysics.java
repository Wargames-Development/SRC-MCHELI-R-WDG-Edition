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
// BODY-RELATIVE AOA + SIDESLIP
// =============================
// AoA = airflow angle in local forward/up plane.
// Beta = airflow angle in local forward/right plane.
// The 90-degree roll + hard pitch failure is often high beta/sideslip,
// not only high AoA.

        Vec3 aoaUpVec = MCH_Lib.RotVec3(
                0.0D, 1.0D, 0.0D,
                -plane.getRotYaw(),
                -plane.getRotPitch(),
                -plane.getRotRoll()
        );

        Vec3 betaRightVec = MCH_Lib.RotVec3(
                1.0D, 0.0D, 0.0D,
                -plane.getRotYaw(),
                -plane.getRotPitch(),
                -plane.getRotRoll()
        );

        double aoaUpX = aoaUpVec.xCoord;
        double aoaUpY = aoaUpVec.yCoord;
        double aoaUpZ = aoaUpVec.zCoord;

        double rightX = betaRightVec.xCoord;
        double rightY = betaRightVec.yCoord;
        double rightZ = betaRightVec.zCoord;

        double upLen = Math.sqrt(
                aoaUpX * aoaUpX
                        + aoaUpY * aoaUpY
                        + aoaUpZ * aoaUpZ
        );

        if (upLen > 1.0E-6D) {
            aoaUpX /= upLen;
            aoaUpY /= upLen;
            aoaUpZ /= upLen;
        }

        double rightLen = Math.sqrt(
                rightX * rightX
                        + rightY * rightY
                        + rightZ * rightZ
        );

        if (rightLen > 1.0E-6D) {
            rightX /= rightLen;
            rightY /= rightLen;
            rightZ /= rightLen;
        }

        double forwardSpeedBody = vx * forwardX + vy * forwardY + vz * forwardZ;
        double verticalSpeedBody = vx * aoaUpX + vy * aoaUpY + vz * aoaUpZ;
        double sideSpeedBody = vx * rightX + vy * rightY + vz * rightZ;

        double rawAoA = 0.0D;
        double rawBeta = 0.0D;

        if (speed > 1.0D) {
            rawAoA = Math.toDegrees(Math.atan2(
                    -verticalSpeedBody,
                    Math.max(1.0D, Math.abs(forwardSpeedBody))
            ));

            rawBeta = Math.toDegrees(Math.atan2(
                    sideSpeedBody,
                    Math.max(1.0D, Math.abs(forwardSpeedBody))
            ));
        }

// Let stall logic see deep AoA/beta.
// Only clamp impossible spikes.
        if (rawAoA > 50.0D) {
            rawAoA = 50.0D;
        }

        if (rawAoA < -50.0D) {
            rawAoA = -50.0D;
        }

        if (rawBeta > 50.0D) {
            rawBeta = 50.0D;
        }

        if (rawBeta < -50.0D) {
            rawBeta = -50.0D;
        }

        double absBeta = Math.abs(rawBeta);

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
        double stallStartAoA = 12.0D;
        double stallFullAoA = 28.0D;

        double absAoA = Math.abs(plane.advancedSmoothedAoA);
        double stallFactor = 1.0D;

        if (absAoA > stallStartAoA) {
            stallFactor = 1.0D - ((absAoA - stallStartAoA) / (stallFullAoA - stallStartAoA));

            // Deep stall keeps only a small amount of unstable lift.
            if (stallFactor < 0.10D) {
                stallFactor = 0.10D;
            }
        }
        double flowSeparationAngle = Math.max(absAoA, absBeta);

        // =============================
        // CORNER-SPEED / ALPHA LIMIT
        // =============================
        // Best pitch authority exists near corner speed.
        // Below corner speed: not enough airflow.
        // Above corner speed: G / turn-rate limit should reduce allowable AoA.
        // This prevents stable flight from becoming a 90-degree flat-plate drift.

        double cornerSpeed = 240.0D;       // m/s, tuning value
        double minControlSpeed = 85.0D;    // m/s
        double cornerAoALimit = 15.5D;     // deg, max stable commanded AoA near corner speed
        double minAoALimit = 8.0D;         // deg, minimum commanded limit at bad speeds

        double stableAoALimit;

        if (speed < cornerSpeed) {
            double lowSpeedFactor = (speed - minControlSpeed) / (cornerSpeed - minControlSpeed);

            if (lowSpeedFactor < 0.0D) {
                lowSpeedFactor = 0.0D;
            }

            if (lowSpeedFactor > 1.0D) {
                lowSpeedFactor = 1.0D;
            }

            stableAoALimit = minAoALimit + (cornerAoALimit - minAoALimit) * lowSpeedFactor;
        } else {
            double highSpeedFactor = Math.pow(cornerSpeed / speed, 0.45D);

            if (highSpeedFactor < 0.55D) {
                highSpeedFactor = 0.55D;
            }

            if (highSpeedFactor > 1.0D) {
                highSpeedFactor = 1.0D;
            }

            stableAoALimit = cornerAoALimit * highSpeedFactor;
        }

        if (stableAoALimit < minAoALimit) {
            stableAoALimit = minAoALimit;
        }

        plane.advancedStableAoALimit = stableAoALimit;
        plane.advancedAoAControlMargin = stableAoALimit - absAoA;

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

        // Use total airspeed for dynamic pressure.
        // horizontalSpeed was hiding vertical/deep-stall energy.
        double qLift = 0.5D * rho * speed * speed;
        double qDrag = 0.5D * rho * speed * speed;

        double liftN = qLift * wingArea * cl;
        double dragN = qDrag * wingArea * cd0;
        double thrustN = maxThrustN * throttle;
        double weightN = mass * 9.81D;

        // =============================
        // HIGH-AOA / STALL DRAG
        // =============================
        // After CLmax / stall AoA, drag rises hard. This bleeds speed
        // before the aircraft gets into clean backwards-flight behavior.
        double highAoADragN = 0.0D;
        double highAoAFactor = 0.0D;

        double highAoAStart = 14.0D;
        double highAoAFull = 45.0D;

        if (flowSeparationAngle > highAoAStart) {
            highAoAFactor = (flowSeparationAngle - highAoAStart) / (highAoAFull - highAoAStart);

            if (highAoAFactor > 1.0D) highAoAFactor = 1.0D;
            if (highAoAFactor < 0.0D) highAoAFactor = 0.0D;
        }

        double cdHighAoA =
                0.035D * highAoAFactor
                        + 0.22D * highAoAFactor * highAoAFactor;

        highAoADragN = qDrag * wingArea * cdHighAoA;
        dragN += highAoADragN;

        // Extra induced drag when producing lift.
        double inducedDragN = qLift * wingArea * 0.035D * cl * cl;
        dragN += inducedDragN;

        // =============================
        // REVERSE FLOW / BACKWARDS FLIGHT
        // =============================
        // If the aircraft nose points opposite velocity, lift becomes mostly useless
        // and drag becomes massive.
        double reverseFlowFactor = 0.0D;

        if (speed > 1.0D) {
            reverseFlowFactor = -forwardSpeedBody / speed;

            if (reverseFlowFactor < 0.0D) reverseFlowFactor = 0.0D;
            if (reverseFlowFactor > 1.0D) reverseFlowFactor = 1.0D;
        }

        if (reverseFlowFactor > 0.0D) {
            liftN *= 1.0D - 0.95D * reverseFlowFactor;

            double reverseCd =
                    0.35D * reverseFlowFactor
                            + 1.40D * reverseFlowFactor * reverseFlowFactor;

            dragN += qDrag * wingArea * reverseCd;
        }

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
        // Damp only velocity along aircraft local right axis.
        // Do not damp all non-forward velocity, because that also fights pitch/lift
        // and can flip AoA sign tick-to-tick.

        double lateralSpeed = 0.0D;
        double sideDampingAccel = 0.0D;

        if (speed > 1.0D) {
            // rightX/rightY/rightZ were already computed in the AoA + beta block.
            lateralSpeed = vx * rightX + vy * rightY + vz * rightZ;

            double sideDampingTime = 1.25D; // seconds. Lower = stronger damping.
            sideDampingAccel = -lateralSpeed / sideDampingTime;

            // Cap correction so it cannot snap the velocity vector every tick.
            double maxSideDampingAccel = 30.0D; // m/s^2

            if (sideDampingAccel > maxSideDampingAccel) {
                sideDampingAccel = maxSideDampingAccel;
            }

            if (sideDampingAccel < -maxSideDampingAccel) {
                sideDampingAccel = -maxSideDampingAccel;
            }

            forceX += rightX * sideDampingAccel * mass;
            forceY += rightY * sideDampingAccel * mass;
            forceZ += rightZ * sideDampingAccel * mass;
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

        // Roll input still uses the proven key-based server input path.
        if (isAirborne) {
            if (plane.moveLeft && !plane.moveRight) {
                rollInput = -1.0D;
            }

            if (plane.moveRight && !plane.moveLeft) {
                rollInput = 1.0D;
            }
        }

        plane.advancedRollInput = rollInput;

        // =============================
        // TARGET-DIRECTION CONTROL
        // =============================
        // Require real flight speed before allowing pitch control.
        // This prevents the aircraft from pitching hard while still below takeoff speed.
        boolean hasControlAuthority = isAirborne && speed > 85.0D;

        if (hasControlAuthority) {
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

            plane.advancedYawInput = 0.0D;

            // Clamp aim error so a huge camera/target jump does not command full elevator forever.
            double maxUsedPitchError = 25.0D;

            if (pitchErrorDeg > maxUsedPitchError) {
                pitchErrorDeg = maxUsedPitchError;
            }

            if (pitchErrorDeg < -maxUsedPitchError) {
                pitchErrorDeg = -maxUsedPitchError;
            }

// Rate-command controller:
// target direction -> desired pitch rate -> elevator input.
// This is smoother than direct angle-error -> elevator.
            double pitchRatePerDegError = 2.0D;
            double maxCommandedPitchRate = 80.0D;

            // Since yaw control is not implemented yet, reduce pitch authority when the
// target is mostly sideways relative to the aircraft nose.
            double yawAbs = Math.abs(yawErrorDeg);

            double yawBlend = 1.0D;
            if (yawAbs > 30.0D) {
                yawBlend = 1.0D - ((yawAbs - 30.0D) / 60.0D);

                if (yawBlend < 0.25D) {
                    yawBlend = 0.25D;
                }
            }

            pitchErrorDeg *= yawBlend;

            double desiredPitchRate = -pitchErrorDeg * pitchRatePerDegError;

            if (desiredPitchRate > maxCommandedPitchRate) {
                desiredPitchRate = maxCommandedPitchRate;
            }

            if (desiredPitchRate < -maxCommandedPitchRate) {
                desiredPitchRate = -maxCommandedPitchRate;
            }

            double pitchRateError = desiredPitchRate - plane.advancedPitchRate;

            // Convert pitch-rate error into elevator command.
            double pitchRateKp = 0.018D;

            double targetPitchInput = pitchRateError * pitchRateKp;

            if (targetPitchInput > 1.0D) {
                targetPitchInput = 1.0D;
            }

            if (targetPitchInput < -1.0D) {
                targetPitchInput = -1.0D;
            }

            // Smooth elevator command so target-direction control does not flip input every tick.
            plane.advancedPitchInput += (targetPitchInput - plane.advancedPitchInput) * 0.25D;

            // Yaw stays disabled for now.
            plane.advancedYawInput = 0.0D;
        } else {
            plane.advancedPitchInput *= 0.50D;

            if (Math.abs(plane.advancedPitchInput) < 0.01D) {
                plane.advancedPitchInput = 0.0D;
            }
            plane.advancedYawInput = 0.0D;

            plane.advancedPitchRate *= 0.70D;
            plane.advancedLocalPitchError = 0.0D;
            plane.advancedLocalYawError = 0.0D;
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

        // =============================
        // HIGH-AOA CONTROL AUTHORITY LIMIT
        // =============================
        // This prevents the plane from instantly rotating its nose 180 degrees
        // while the velocity vector is still going the old direction.
        double aeroAuthority = 1.0D;

        if (flowSeparationAngle > 8.0D) {
            aeroAuthority = 1.0D - ((flowSeparationAngle - 8.0D) / 27.0D);

            if (aeroAuthority < 0.15D) {
                aeroAuthority = 0.15D;
            }

            if (aeroAuthority > 1.0D) {
                aeroAuthority = 1.0D;
            }
        }

        // Reverse flow should make pitch/yaw authority collapse even more.
        if (reverseFlowFactor > 0.10D) {
            double reverseAuthority = 1.0D - reverseFlowFactor;

            if (reverseAuthority < 0.10D) {
                reverseAuthority = 0.10D;
            }

            if (reverseAuthority < aeroAuthority) {
                aeroAuthority = reverseAuthority;
            }
        }

        // Expose aerodynamic authority to MCHeli's normal rotation input path.
        // This is important because MCHeli can rotate the aircraft directly,
        // bypassing advancedPitchRate.
        plane.advancedAeroControlAuthority = aeroAuthority;
        plane.advancedFlowSeparationAngle = flowSeparationAngle;

        // Pitch now uses dynamic-pressure moment model, but control authority
        // collapses during stall/reverse-flow.
        double pitchControlForce =
                qControl
                        * info.advancedPitchControlArea
                        * info.advancedPitchControlPower
                        * aeroAuthority
                        * plane.advancedPitchInput;

        double pitchMoment =
                pitchControlForce
                        * info.advancedPitchMomentArm;

        // Pitch-rate damping is already applied below by:
        // plane.advancedPitchRate *= info.advancedPitchDamping;
        //
        // Do not add a qControl-scaled damping moment here.
        // At high speed it can overcorrect and flip pitchRate positive/negative every tick.

        // Sign may need flipping depending on MCHR pitch convention.
        // If pressing pitch-up makes the nose go down, multiply this by -1.0D.
        double pitchAccelRad =
                pitchMoment / info.advancedPitchInertia;

        double pitchAccel =
                Math.toDegrees(pitchAccelRad);

        // Limit pitch angular acceleration so pitchRate cannot flip violently
        // positive/negative every tick.
        double maxPitchAccel = 220.0D * aeroAuthority;

        if (maxPitchAccel < 45.0D) {
            maxPitchAccel = 45.0D;
        }

        if (pitchAccel > maxPitchAccel) {
            pitchAccel = maxPitchAccel;
        }

        if (pitchAccel < -maxPitchAccel) {
            pitchAccel = -maxPitchAccel;
        }

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
        double yawAccel = plane.advancedYawInput * 35.0D * controlSpeedFactor * aeroAuthority;

        // Apply angular acceleration
        plane.advancedPitchRate += pitchAccel * DT;
        plane.advancedRollRate += rollAccel * DT;
        plane.advancedYawRate += yawAccel * DT;

        // Damping / stability
        plane.advancedPitchRate *= info.advancedPitchDamping;
        plane.advancedRollRate *= info.advancedRollDamping;
        plane.advancedYawRate *= 0.90D;

        // Clamp angular rates in deg/sec
        // Clamp angular rates in deg/sec.
        // Clean flow can use the aircraft config, but separated/reverse flow
        // must not allow instant 180-degree nose flips.
        double cleanMaxPitchRate = info.advancedMaxPitchRate;

        if (cleanMaxPitchRate > 180.0D) {
            cleanMaxPitchRate = 180.0D;
        }

        double maxPitchRate = cleanMaxPitchRate * (0.20D + 0.80D * aeroAuthority);

        if (flowSeparationAngle > 25.0D) {
            double separatedMaxRate = 45.0D;

            if (maxPitchRate > separatedMaxRate) {
                maxPitchRate = separatedMaxRate;
            }
        }

        if (reverseFlowFactor > 0.25D) {
            double reverseMaxRate = 25.0D;

            if (maxPitchRate > reverseMaxRate) {
                maxPitchRate = reverseMaxRate;
            }
        }

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
// BRUTE-FORCE ALPHA / CORNER-SPEED GOVERNOR
// =============================
// Robust final limiter.
// Instead of assuming pitchRate sign, test possible pitch rates and choose
// the one that best keeps AoA inside the stable envelope.
// If already past the AoA limit, actively choose the pitch rate that reduces AoA.

        double debugPredictedAoA = plane.advancedSmoothedAoA;
        boolean debugAlphaLimited = false;

        double alphaLimit = plane.advancedStableAoALimit;

        if (Double.isNaN(alphaLimit) || Double.isInfinite(alphaLimit) || alphaLimit < 5.0D) {
            alphaLimit = 14.0D;
        }

// Work from current attitude before final pitch/roll integration.
        syncQuatFromEuler(plane);

        double[] alphaFwdAxis = getAircraftForwardAxis(plane);
        double[] alphaUpAxis = getAircraftUpAxis(plane);
        double[] alphaPitchAxis = getAircraftRightAxis(plane);

// Use raw predicted AoA for limiting, not smoothed AoA.
// Smoothed AoA lags too much at low speed / high pitch command.
        double currentRawAlpha = computeSignedAoAFromAxes(
                alphaFwdAxis,
                alphaUpAxis,
                vx,
                vy,
                vz
        );

        double absCurrentRawAlpha = Math.abs(currentRawAlpha);

        double commandedPitchRate = plane.advancedPitchRate;

// Only intervene near or beyond the stable AoA limit.
        double alphaSoftZone = 4.0D;
        boolean nearAlphaLimit = absCurrentRawAlpha > alphaLimit - alphaSoftZone;

        if (nearAlphaLimit && speed > 30.0D) {
            double maxCandidateRate = Math.abs(commandedPitchRate);

            // Always allow enough authority to unload, even if current command is small.
            if (maxCandidateRate < 35.0D) {
                maxCandidateRate = 35.0D;
            }

            // Do not let this search produce insane pitch rates.
            if (maxCandidateRate > 80.0D) {
                maxCandidateRate = 80.0D;
            }

            double bestRate = commandedPitchRate;
            double bestAlpha = currentRawAlpha;
            double bestScore = 1.0E30D;

            // Test candidate pitch rates from negative to positive.
            // 41 samples gives 4 deg/s resolution for +/-80, cheaper than any physics force calc.
            int samples = 41;

            for (int i = 0; i < samples; i++) {
                double t = (double)i / (double)(samples - 1);
                double testRate = -maxCandidateRate + 2.0D * maxCandidateRate * t;

                double testStepRad = Math.toRadians(testRate * DT);

                double[] testFwd = rotateWorldVectorAroundAxis(alphaFwdAxis, alphaPitchAxis, testStepRad);
                double[] testUp = rotateWorldVectorAroundAxis(alphaUpAxis, alphaPitchAxis, testStepRad);

                double testAlpha = computeSignedAoAFromAxes(
                        testFwd,
                        testUp,
                        vx,
                        vy,
                        vz
                );

                double absTestAlpha = Math.abs(testAlpha);

                double score;

                if (absCurrentRawAlpha > alphaLimit) {
                    // Already stalled: prioritize reducing AoA immediately.
                    score = absTestAlpha * 1000.0D;

                    // Small secondary cost to avoid violent rate jumps if multiple rates unload similarly.
                    score += Math.abs(testRate - commandedPitchRate) * 0.05D;
                } else {
                    // Near limit but not stalled yet:
                    // prefer staying under the limit, while preserving pilot command as much as possible.
                    double overLimit = absTestAlpha - alphaLimit;

                    if (overLimit < 0.0D) {
                        overLimit = 0.0D;
                    }

                    score = overLimit * overLimit * 10000.0D;
                    score += Math.abs(testRate - commandedPitchRate);
                }

                if (score < bestScore) {
                    bestScore = score;
                    bestRate = testRate;
                    bestAlpha = testAlpha;
                }
            }

            if (Math.abs(bestRate - commandedPitchRate) > 0.1D) {
                plane.advancedPitchRate = bestRate;
                debugAlphaLimited = true;
            }

            debugPredictedAoA = bestAlpha;
        }
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

// =============================
// FINAL AOA ATTITUDE CLAMP
// =============================
// This clamps the actual aircraft attitude after pitch/roll integration.
// It prevents the plane from remaining at 50 deg AoA / flat-plate drift,
// even if an upstream input/rate path failed to stop it.
        boolean alphaAttitudeClamped = false;

        if (speed > 45.0D) {
            alphaAttitudeClamped = clampAircraftQuatAoA(
                    plane,
                    vx,
                    vy,
                    vz,
                    plane.advancedStableAoALimit
            );

            if (alphaAttitudeClamped) {
                // Kill residual pitch rate so it does not fight the clamp next tick.
                plane.advancedPitchRate *= 0.15D;
            }
        }

        if (quatChanged || alphaAttitudeClamped) {
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
            MCH_Lib.Log("[AdvPhys] %s speed=%.1f m/s hs=%.1f m/s AoA=%.1f AoALim=%.1f AoAMargin=%.1f PredAoA=%.1f AlphaLim=%s AttClamp=%s Beta=%.1f Sep=%.1f CL=%.2f Lift/W=%.2f Drag=%.0f StallDrag=%.0f RevFlow=%.2f Auth=%.2f pitchRate=%.1f rotYaw=%.1f rotPitch=%.1f rotRoll=%.1f",
                    info.name,
                    speed,
                    horizontalSpeed,
                    plane.advancedSmoothedAoA,
                    plane.advancedStableAoALimit,
                    plane.advancedStableAoALimit - Math.abs(plane.advancedSmoothedAoA),
                    debugPredictedAoA,
                    debugAlphaLimited ? "Y" : "N",
                    alphaAttitudeClamped ? "Y" : "N",
                    rawBeta,
                    flowSeparationAngle,
                    cl,
                    liftN / weightN,
                    dragN,
                    highAoADragN,
                    reverseFlowFactor,
                    aeroAuthority,
                    plane.advancedPitchRate,
                    plane.getRotYaw(),
                    plane.getRotPitch(),
                    plane.getRotRoll());
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

    private static double computeSignedAoAFromAxes(double[] forwardAxis, double[] upAxis, double vx, double vy, double vz) {
        double speed = Math.sqrt(vx * vx + vy * vy + vz * vz);

        if (speed < 1.0D) {
            return 0.0D;
        }

        double forwardSpeedBody =
                vx * forwardAxis[0]
                        + vy * forwardAxis[1]
                        + vz * forwardAxis[2];

        double verticalSpeedBody =
                vx * upAxis[0]
                        + vy * upAxis[1]
                        + vz * upAxis[2];

        double aoa = Math.toDegrees(Math.atan2(
                -verticalSpeedBody,
                Math.max(1.0D, Math.abs(forwardSpeedBody))
        ));

        if (aoa > 50.0D) {
            aoa = 50.0D;
        }

        if (aoa < -50.0D) {
            aoa = -50.0D;
        }

        return aoa;
    }

    private static double[] rotateWorldVectorAroundAxis(double[] vIn, double[] axisIn, double angleRad) {
        double[] axis = normalizeVec(axisIn);

        double x = vIn[0];
        double y = vIn[1];
        double z = vIn[2];

        double ax = axis[0];
        double ay = axis[1];
        double az = axis[2];

        double cos = Math.cos(angleRad);
        double sin = Math.sin(angleRad);

        double dot = x * ax + y * ay + z * az;

        // Rodrigues rotation formula.
        return normalizeVec(new double[] {
                x * cos + (ay * z - az * y) * sin + ax * dot * (1.0D - cos),
                y * cos + (az * x - ax * z) * sin + ay * dot * (1.0D - cos),
                z * cos + (ax * y - ay * x) * sin + az * dot * (1.0D - cos)
        });
    }

    private static boolean clampAircraftQuatAoA(MCP_EntityPlane plane, double vx, double vy, double vz, double alphaLimit) {
        double speed = Math.sqrt(vx * vx + vy * vy + vz * vz);

        if (speed < 45.0D) {
            return false;
        }

        if (Double.isNaN(alphaLimit) || Double.isInfinite(alphaLimit) || alphaLimit < 5.0D) {
            alphaLimit = 14.0D;
        }

        double[] fwd = getAircraftForwardAxis(plane);
        double[] up = getAircraftUpAxis(plane);
        double[] right = getAircraftRightAxis(plane);

        double currentAlpha = computeSignedAoAFromAxes(fwd, up, vx, vy, vz);
        double absCurrentAlpha = Math.abs(currentAlpha);

        if (absCurrentAlpha <= alphaLimit + 0.25D) {
            return false;
        }

        double bestAngleDeg = 0.0D;
        double bestAlpha = currentAlpha;
        double bestScore = 1.0E30D;

        // Search a correction around the local pitch axis.
        // Large enough to pull the aircraft out of 50 deg AoA in one tick,
        // but still bounded so it cannot teleport attitude violently.
        double maxCorrectionDeg = 55.0D;
        int samples = 111;

        for (int i = 0; i < samples; i++) {
            double t = (double)i / (double)(samples - 1);
            double angleDeg = -maxCorrectionDeg + 2.0D * maxCorrectionDeg * t;
            double angleRad = Math.toRadians(angleDeg);

            double[] testFwd = rotateWorldVectorAroundAxis(fwd, right, angleRad);
            double[] testUp = rotateWorldVectorAroundAxis(up, right, angleRad);

            double testAlpha = computeSignedAoAFromAxes(testFwd, testUp, vx, vy, vz);
            double absTestAlpha = Math.abs(testAlpha);

            double score;

            if (absTestAlpha > alphaLimit) {
                // Strongly prefer getting under the limit.
                double over = absTestAlpha - alphaLimit;
                score = 100000.0D + over * over * 1000.0D;
            } else {
                // Once under the limit, prefer staying close to the limit
                // rather than snapping all the way to zero AoA.
                score = alphaLimit - absTestAlpha;
            }

            // Tiny correction cost so it chooses the least-violent valid correction.
            score += Math.abs(angleDeg) * 0.001D;

            if (score < bestScore) {
                bestScore = score;
                bestAngleDeg = angleDeg;
                bestAlpha = testAlpha;
            }
        }

        if (Math.abs(bestAlpha) >= absCurrentAlpha - 0.10D) {
            return false;
        }

        if (Math.abs(bestAngleDeg) < 0.01D) {
            return false;
        }

        rotateAircraftQuatWorldAxis(plane, right, Math.toRadians(bestAngleDeg));
        normalizeAircraftQuat(plane);

        return true;
    }
    private static boolean isFinite(double v) {
        return !Double.isNaN(v) && !Double.isInfinite(v);
    }
}