package mcheli.weapon;

import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

/** Continuous collision against a translating projectile box during one server tick. */
final class MCH_ProjectileCollision {
    private MCH_ProjectileCollision() { }

    static MovingObjectPosition sweep(AxisAlignedBB targetStartBox, Vec3 from, Vec3 to,
                                      double targetX, double targetY, double targetZ) {
        double dx = to.xCoord - from.xCoord;
        double dy = to.yCoord - from.yCoord;
        double dz = to.zCoord - from.zCoord;
        Vec3 relativeEnd = from.addVector(dx - targetX, dy - targetY, dz - targetZ);
        MovingObjectPosition hit = targetStartBox.calculateIntercept(from, relativeEnd);
        if (targetStartBox.isVecInside(from))
            return new MovingObjectPosition(0, 0, 0, 0, from);
        if (hit == null) return null;
        double rx = relativeEnd.xCoord - from.xCoord;
        double ry = relativeEnd.yCoord - from.yCoord;
        double rz = relativeEnd.zCoord - from.zCoord;
        double lengthSquared = rx * rx + ry * ry + rz * rz;
        if (lengthSquared < 1.0E-12D) return null;
        double fraction = ((hit.hitVec.xCoord - from.xCoord) * rx
            + (hit.hitVec.yCoord - from.yCoord) * ry + (hit.hitVec.zCoord - from.zCoord) * rz) / lengthSquared;
        return new MovingObjectPosition(0, 0, 0, 0, from.addVector(dx * fraction, dy * fraction, dz * fraction));
    }
}
