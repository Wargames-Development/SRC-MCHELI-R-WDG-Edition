package mcheli.mob;

import net.minecraft.util.Vec3;

/** Server-side look input and interception with discrete gravity, in blocks/tick. */
final class MCH_AAGunnerMath {
    private MCH_AAGunnerMath() { }

    static float wrap(float angle) {
        angle %= 360.0F;
        if (angle >= 180.0F) angle -= 360.0F;
        if (angle < -180.0F) angle += 360.0F;
        return angle;
    }

    static float lookStep(float error, float speed, float response) {
        return Math.max(-speed, Math.min(speed, error * response));
    }

    static boolean enemies(String ownTeam, String otherTeam) {
        return ownTeam != null && !ownTeam.isEmpty() && otherTeam != null
            && !otherTeam.isEmpty() && !ownTeam.equals(otherTeam);
    }

    static double interceptTime(double x, double y, double z,
                                double vx, double vy, double vz, double speed) {
        return interceptTime(x, y, z, vx, vy, vz, speed, 0.0D);
    }

    static double interceptTime(double x, double y, double z,
                                double vx, double vy, double vz, double speed, double launchOffset) {
        if (!Double.isFinite(speed) || speed <= 0.0D) return -1.0D;
        double a = vx * vx + vy * vy + vz * vz - speed * speed;
        double b = 2.0D * (x * vx + y * vy + z * vz - speed * launchOffset);
        double c = x * x + y * y + z * z - launchOffset * launchOffset;
        if (c < 1.0E-8D) return 0.0D;
        if (Math.abs(a) < 1.0E-8D) return b < 0.0D ? -c / b : -1.0D;
        double discriminant = b * b - 4.0D * a * c;
        if (discriminant < 0.0D) return -1.0D;
        double root = Math.sqrt(discriminant);
        double first = (-b - root) / (2.0D * a);
        double second = (-b + root) / (2.0D * a);
        if (first >= 0.0D && second >= 0.0D) return Math.min(first, second);
        return first >= 0.0D ? first : second >= 0.0D ? second : -1.0D;
    }

    static double gravityDisplacement(double ticks, double gravity) {
        double wholeTicks = Math.floor(ticks);
        return gravity * (wholeTicks * (wholeTicks + 1.0D) * 0.5D
            + (ticks - wholeTicks) * (wholeTicks + 1.0D));
    }

    static Vec3 interceptPoint(double x, double y, double z, double vx, double vy, double vz,
                               double speed, double offset, double targetGravity,
                               double roundGravity, boolean targetMoved) {
        double time = interceptTime(x, y, z, vx, vy, vz, speed, offset);
        if (time < 0.0D || time > 200.0D) return null;
        for (int iteration = 0; iteration < 4; ++iteration) {
            double ax = x + vx * time;
            double ay = y + vy * time + gravityDisplacement(time, targetGravity)
                - (targetMoved ? targetGravity * time : 0.0D) - gravityDisplacement(time, roundGravity);
            double az = z + vz * time;
            time = Math.max(0.0D, (Math.sqrt(ax * ax + ay * ay + az * az) - offset) / speed);
            if (!Double.isFinite(time) || time > 200.0D) return null;
        }
        return Vec3.createVectorHelper(x + vx * time,
            y + vy * time + gravityDisplacement(time, targetGravity)
                - (targetMoved ? targetGravity * time : 0.0D) - gravityDisplacement(time, roundGravity),
            z + vz * time);
    }
}
