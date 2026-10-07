package mcheli.tank;

/** Ground driving calculations; no Minecraft or client dependencies. */
public final class MCH_WheeledControlMath {
    // One block is one metre; 20 ticks/second converts 6 m/s^2 to blocks/tick^2.
    private static final double BRAKE_DECELERATION = 6.0D / 400.0D;

    private MCH_WheeledControlMath() {
    }

    public static float speedLimit(float declared, double multiplier, boolean wheeled) {
        if (!Float.isFinite(declared) || !Double.isFinite(multiplier) || multiplier < 0.0D) {
            throw new IllegalArgumentException("Invalid ground vehicle speed");
        }
        double speed = Math.max(0.0D, Math.min(declared, wheeled ? 8.0D : 1.8D)) * multiplier;
        if (wheeled) {
            speed = Math.min(speed, 8.0D);
        }
        if (speed > Float.MAX_VALUE) {
            throw new IllegalArgumentException("Ground vehicle speed overflow");
        }
        return (float) speed;
    }

    public static double approach(double value, double target, double step) {
        return value + Math.max(-step, Math.min(target - value, step));
    }

    public static double throttle(double value, boolean accelerating) {
        return approach(value, accelerating ? 1.0D : 0.0D, accelerating ? 0.025D : 0.05D);
    }

    public static double clientPosition(double position, double serverPosition, double velocity, int packetAge) {
        if (!Double.isFinite(position) || !Double.isFinite(serverPosition) || !Double.isFinite(velocity)
                || packetAge < 0 || packetAge > 2 && velocity != 0.0D) {
            return position;
        }
        // Bridge two missing updates using server velocity, then hold until fresh state.
        double target = serverPosition + velocity * Math.min(packetAge, 2);
        if (Math.abs(target - position) > 32.0D) {
            return target;
        }
        double predicted = position + velocity;
        double correction = (target - predicted) / 3.0D;
        if (velocity != 0.0D) {
            // A late position must not reverse forward/reverse motion. A fresh stopped
            // or reversed velocity still permits correction toward the authoritative pose.
            correction = Math.max(-Math.abs(velocity), Math.min(correction, Math.abs(velocity)));
        }
        // Advance first, then reconcile: constant-speed per-tick packets have no extra
        // interpolation lag between the barrel and server-spawned projectiles.
        return predicted + correction;
    }

    public static float relativeRenderYaw(float relativeYaw, float referenceHullYaw, float renderHullYaw) {
        // Preserve a world aim direction on the interpolated hull, including yaw wrap.
        float yaw = relativeYaw + referenceHullYaw - renderHullYaw;
        return yaw - (float) Math.floor((yaw + 180.0F) / 360.0F) * 360.0F;
    }

    public static float steering(float value, boolean left, boolean right) {
        double target = left == right ? 0.0D : (left ? -1.0D : 1.0D);
        return (float) approach(value, target, target == 0.0D ? 0.2D : 0.15D);
    }

    public static double accelerationStep(double peak, double speed, double limit) {
        if (!Double.isFinite(peak) || !Double.isFinite(speed) || !Double.isFinite(limit) || peak <= 0.0D || limit <= 0.0D) {
            return 0.0D;
        }
        // Approximate diminishing engine pull near road speed without adding a gear model.
        // Retain positive pull at the limit so the configured top speed stays reachable.
        double fraction = Math.min(1.0D, Math.abs(speed) / limit);
        return peak * (1.0D - 0.75D * fraction * fraction);
    }

    public static double forwardSpeed(double speed, double limit, boolean up, boolean down,
                                      boolean brake, boolean enableBack) {
        return forwardSpeed(speed, limit, up, down, brake, enableBack, 0.012D + limit / 240.0D);
    }

    public static double forwardSpeed(double speed, double limit, boolean up, boolean down,
                                      boolean brake, boolean enableBack, double acceleration) {
        if (!Double.isFinite(speed) || !Double.isFinite(limit) || limit <= 0.0D
                || !Double.isFinite(acceleration) || acceleration <= 0.0D) {
            return 0.0D;
        }
        if (brake || up && down || down && speed > 0.02D || up && speed < -0.02D) {
            return approach(speed, 0.0D, BRAKE_DECELERATION);
        }
        if (up || down && enableBack) {
            double target = up ? limit : -Math.min(0.6D, limit * 0.3D);
            return approach(speed, target, acceleration);
        }
        // Mild speed-dependent drag and rolling resistance let throttle release coast.
        return approach(speed * 0.999D, 0.0D, 0.001D);
    }

    public static float effectiveSteering(double speed, float steering, double wheelbase) {
        if (!Double.isFinite(speed) || !Float.isFinite(steering) || !Double.isFinite(wheelbase)) {
            return 0.0F;
        }
        // Bicycle geometry at low speed; approximately 0.5g of road grip widens fast turns.
        // 5 m/s^2 becomes 0.0125 blocks/tick^2 at 20 TPS, rather than the old 3.3g budget.
        double yawLimit = Math.min(Math.toRadians(8.0D), 0.0125D / Math.max(0.1D, Math.abs(speed)));
        double angleLimit = Math.atan(yawLimit * Math.max(1.0D, wheelbase) / Math.max(0.001D, Math.abs(speed)));
        double limit = Math.min(1.0D, angleLimit / Math.toRadians(35.0D));
        return (float) Math.max(-limit, Math.min(steering, limit));
    }

    public static float yawStep(double speed, float steering, double wheelbase) {
        if (!Double.isFinite(speed) || !Double.isFinite(wheelbase)) {
            return 0.0F;
        }
        double angle = Math.toRadians(35.0D) * effectiveSteering(speed, steering, wheelbase);
        return (float) Math.toDegrees(speed * Math.tan(angle) / Math.max(1.0D, wheelbase));
    }
}
