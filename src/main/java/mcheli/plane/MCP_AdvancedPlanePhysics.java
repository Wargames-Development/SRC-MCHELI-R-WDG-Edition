package mcheli.plane;

import mcheli.aircraft.MCH_AircraftInfo;
import mcheli.MCH_Lib;
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
        // BODY-RELATIVE AOA
        // =============================
        // AoA is based on velocity relative to the aircraft body axes,
        // not world horizon angle. This behaves correctly while rolled.

        Vec3 aoaUpVec = MCH_Lib.RotVec3(
                0.0D, 1.0D, 0.0D,
                -plane.getRotYaw(),
                -plane.getRotPitch(),
                -plane.getRotRoll()
        );

        double aoaUpX = aoaUpVec.xCoord;
        double aoaUpY = aoaUpVec.yCoord;
        double aoaUpZ = aoaUpVec.zCoord;

        double aoaUpLen = Math.sqrt(aoaUpX * aoaUpX + aoaUpY * aoaUpY + aoaUpZ * aoaUpZ);

        if (aoaUpLen > 1.0E-6D) {
            aoaUpX /= aoaUpLen;
            aoaUpY /= aoaUpLen;
            aoaUpZ /= aoaUpLen;
        }

        double forwardSpeedBody = vx * forwardX + vy * forwardY + vz * forwardZ;
        double verticalSpeedBody = vx * aoaUpX + vy * aoaUpY + vz * aoaUpZ;

        double rawAoA = 0.0D;

        if (speed > 1.0D) {
            rawAoA = Math.toDegrees(Math.atan2(
                    -verticalSpeedBody,
                    Math.max(1.0D, Math.abs(forwardSpeedBody))
            ));
        }

// Do not clamp to CLmax AoA here. Let the stall model see high AoA.
// Only clamp impossible spikes.
        if (rawAoA > 45.0D) rawAoA = 45.0D;
        if (rawAoA < -45.0D) rawAoA = -45.0D;

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

            // Past full stall, only a small amount of unstable lift remains.
            if (stallFactor < 0.12D) {
                stallFactor = 0.12D;
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

        double qLift = 0.5D * rho * speed * speed;
        double qDrag = 0.5D * rho * speed * speed;

        double liftN = qLift * wingArea * cl;
        double dragN = qDrag * wingArea * cd0;
        double thrustN = maxThrustN * throttle;
        double weightN = mass * 9.81D;

        // =============================
        // HIGH-AOA / STALL DRAG
        // =============================
        // Once AoA exceeds the stall region, drag rises hard.
        // This makes hard 180 turns bleed speed instead of producing a clean drift.

        double highAoADragN = 0.0D;
        double highAoAStart = 10.0D;
        double highAoAFull = 35.0D;

        double highAoAFactor = 0.0D;

        if (absAoA > highAoAStart) {
            highAoAFactor = (absAoA - highAoAStart) / (highAoAFull - highAoAStart);

            if (highAoAFactor > 1.0D) {
                highAoAFactor = 1.0D;
            }

            if (highAoAFactor < 0.0D) {
                highAoAFactor = 0.0D;
            }
        }

        // Extra drag coefficient from separated/stalled flow.
        double cdHighAoA =
                0.12D * highAoAFactor
                        + 0.75D * highAoAFactor * highAoAFactor;

        highAoADragN = qDrag * wingArea * cdHighAoA;
        dragN += highAoADragN;

        // =============================
        // REVERSE FLOW / BACKWARDS FLIGHT
        // =============================
        // If the aircraft nose points opposite the velocity vector,
        // lift becomes mostly useless and drag becomes massive.

        double reverseFlowFactor = 0.0D;

        if (speed > 1.0D) {
            reverseFlowFactor = -forwardSpeedBody / speed;

            if (reverseFlowFactor < 0.0D) {
                reverseFlowFactor = 0.0D;
            }

            if (reverseFlowFactor > 1.0D) {
                reverseFlowFactor = 1.0D;
            }
        }

        if (reverseFlowFactor > 0.0D) {
            liftN *= 1.0D - 0.95D * reverseFlowFactor;

            double reverseCd =
                    0.30D * reverseFlowFactor
                            + 1.20D * reverseFlowFactor * reverseFlowFactor;

            dragN += qDrag * wingArea * reverseCd;
        }

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
// Motion-only advanced model:
// damp only local sideways velocity, not all non-forward velocity.
// This avoids huge correction forces during hard turns.

        double lateralSpeed = 0.0D;
        double sideDampingAccel = 0.0D;

        if (speed > 1.0D) {
            Vec3 rightVec = MCH_Lib.RotVec3(
                    1.0D, 0.0D, 0.0D,
                    -plane.getRotYaw(),
                    -plane.getRotPitch(),
                    -plane.getRotRoll()
            );

            double rightX = rightVec.xCoord;
            double rightY = rightVec.yCoord;
            double rightZ = rightVec.zCoord;

            double rightLen = Math.sqrt(rightX * rightX + rightY * rightY + rightZ * rightZ);

            if (rightLen > 1.0E-6D) {
                rightX /= rightLen;
                rightY /= rightLen;
                rightZ /= rightLen;

                // Signed velocity along aircraft local right axis.
                lateralSpeed = vx * rightX + vy * rightY + vz * rightZ;

                // Damps sideways slip over time instead of applying V^2 force spikes.
                double sideDampingTime = 1.25D; // seconds. Lower = stronger damping.
                sideDampingAccel = -lateralSpeed / sideDampingTime;

                // Cap lateral correction so hard turns do not snap/stutter.
                double maxSideDampingAccel = 35.0D; // m/s^2, about 3.6 g sideways correction

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
        }

        // =============================
        // ACCELERATION + INTEGRATION
        // =============================

        double ax = forceX / mass;
        double ay = forceY / mass;
        double az = forceZ / mass;

// Prevent extreme single-tick acceleration spikes from causing snap/stutter.
// 120 m/s^2 is about 12 g total acceleration.
        double accelMag = Math.sqrt(ax * ax + ay * ay + az * az);
        double maxAccel = 120.0D;

        if (accelMag > maxAccel) {
            double accelScale = maxAccel / accelMag;
            ax *= accelScale;
            ay *= accelScale;
            az *= accelScale;
        }

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
        // =============================
        // SAFETY GUARD
        // =============================
        if (!isFinite(plane.motionX) || !isFinite(plane.motionY) || !isFinite(plane.motionZ)) {
            plane.motionX = 0.0D;
            plane.motionY = 0.0D;
            plane.motionZ = 0.0D;
            return;
        }

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
            MCH_Lib.Log("[AdvPhys] %s speed=%.1f m/s hs=%.1f m/s AoA=%.1f CL=%.2f Lift/W=%.2f Thrust=%.0fN Drag=%.0fN StallDrag=%.0fN RevFlow=%.2f",
                    info.name,
                    speed,
                    horizontalSpeed,
                    plane.advancedSmoothedAoA,
                    cl,
                    liftN / weightN,
                    thrustN,
                    dragN,
                    highAoADragN,
                    reverseFlowFactor);
        }
    }

    private static boolean isFinite(double v) {
        return !Double.isNaN(v) && !Double.isInfinite(v);
    }
}