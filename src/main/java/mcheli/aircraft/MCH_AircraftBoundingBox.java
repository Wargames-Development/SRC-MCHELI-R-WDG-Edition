package mcheli.aircraft;

import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

/**
 * 飞机的整体包围盒，用于与外界的 AxisAlignedBB 做检测，同时组合检测部件的旋转包围盒。
 * 继承自 AxisAlignedBB，使其仍可与 Minecraft 自带逻辑兼容。
 */
public class MCH_AircraftBoundingBox extends AxisAlignedBB {

    private final MCH_EntityAircraft ac;

    protected MCH_AircraftBoundingBox(MCH_EntityAircraft ac) {
        super(ac.boundingBox.minX, ac.boundingBox.minY, ac.boundingBox.minZ,
            ac.boundingBox.maxX, ac.boundingBox.maxY, ac.boundingBox.maxZ);
        this.ac = ac;
    }

    /**
     * 工厂方法，用于创建新的 MCH_AircraftBoundingBox 实例并设置边界。
     */
    public AxisAlignedBB NewAABB(double minX, double minY, double minZ,
                                 double maxX, double maxY, double maxZ) {
        return (new MCH_AircraftBoundingBox(this.ac)).setBounds(minX, minY, minZ, maxX, maxY, maxZ);
    }

    // 飞机整体包围盒

    @Override
    public boolean intersectsWith(AxisAlignedBB aabb) {
        boolean ret = false;
        double dist = 1.0E7D;
        this.ac.lastBBDamageFactor = 1.0F;
        this.ac.lastBBName = null;

        // 仍先用整体外接 AABB 快速判定
        if (super.intersectsWith(aabb)) {
            dist = this.getDistSq(aabb, this);
            ret = true;
        }

        // 遍历各部件包围盒
        for (MCH_BoundingBox bb : this.ac.extraBoundingBox) {
            // 先用部件的轴对齐外包围盒做快速过滤
            if (bb.boundingBox.intersectsWith(aabb)) {
                // 用完整的 OBB-AABB 判定代替原来的 corners 判定
                if (bb.intersectsAABB(aabb)) {
                    double dist2 = this.getDistSq(aabb, this);
                    if (dist2 < dist) {
                        dist = dist2;
                        this.ac.lastBBDamageFactor = bb.damageFactor;
                        this.ac.lastBBName = bb.name;
                    }
                    ret = true;
                }
            }
        }
        return ret;
    }

    public double getDistSq(AxisAlignedBB a1, AxisAlignedBB a2) {
        double x1 = (a1.maxX + a1.minX) / 2.0D;
        double y1 = (a1.maxY + a1.minY) / 2.0D;
        double z1 = (a1.maxZ + a1.minZ) / 2.0D;
        double x2 = (a2.maxX + a2.minX) / 2.0D;
        double y2 = (a2.maxY + a2.minY) / 2.0D;
        double z2 = (a2.maxZ + a2.minZ) / 2.0D;
        double dx = x1 - x2;
        double dy = y1 - y2;
        double dz = z1 - z2;
        return dx * dx + dy * dy + dz * dz;
    }

    @Override
    public AxisAlignedBB expand(double dX, double dY, double dZ) {
        double minX = super.minX - dX;
        double minY = super.minY - dY;
        double minZ = super.minZ - dZ;
        double maxX = super.maxX + dX;
        double maxY = super.maxY + dY;
        double maxZ = super.maxZ + dZ;
        return this.NewAABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    @Override
    public AxisAlignedBB func_111270_a(AxisAlignedBB aabb) {
        double minX = Math.min(super.minX, aabb.minX);
        double minY = Math.min(super.minY, aabb.minY);
        double minZ = Math.min(super.minZ, aabb.minZ);
        double maxX = Math.max(super.maxX, aabb.maxX);
        double maxY = Math.max(super.maxY, aabb.maxY);
        double maxZ = Math.max(super.maxZ, aabb.maxZ);
        return this.NewAABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    @Override
    public AxisAlignedBB addCoord(double dX, double dY, double dZ) {
        double minX = super.minX;
        double minY = super.minY;
        double minZ = super.minZ;
        double maxX = super.maxX;
        double maxY = super.maxY;
        double maxZ = super.maxZ;
        if (dX < 0.0D) {
            minX += dX;
        } else if (dX > 0.0D) {
            maxX += dX;
        }
        if (dY < 0.0D) {
            minY += dY;
        } else if (dY > 0.0D) {
            maxY += dY;
        }
        if (dZ < 0.0D) {
            minZ += dZ;
        } else if (dZ > 0.0D) {
            maxZ += dZ;
        }
        return this.NewAABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    @Override
    public AxisAlignedBB contract(double dX, double dY, double dZ) {
        double minX = super.minX + dX;
        double minY = super.minY + dY;
        double minZ = super.minZ + dZ;
        double maxX = super.maxX - dX;
        double maxY = super.maxY - dY;
        double maxZ = super.maxZ - dZ;
        return this.NewAABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    @Override
    public AxisAlignedBB copy() {
        return this.NewAABB(super.minX, super.minY, super.minZ,
            super.maxX, super.maxY, super.maxZ);
    }

    @Override
    public AxisAlignedBB getOffsetBoundingBox(double offsetX, double offsetY, double offsetZ) {
        return this.NewAABB(super.minX + offsetX, super.minY + offsetY, super.minZ + offsetZ,
            super.maxX + offsetX, super.maxY + offsetY, super.maxZ + offsetZ);
    }

    /**
     * 获取包围盒与射线的交点，若与部件包围盒更近，则使用部件包围盒。
     */
    public MovingObjectPosition calculateIntercept(Vec3 start, Vec3 end) {
        this.ac.lastBBDamageFactor = 1.0F;
        this.ac.lastBBName = null;
        MovingObjectPosition bestMop = super.calculateIntercept(start, end);
        double bestDist = (bestMop != null) ? start.distanceTo(bestMop.hitVec) : Double.MAX_VALUE;

        // 遍历旋转部件包围盒，使用 OBB 射线检测
        for (MCH_BoundingBox bb : this.ac.extraBoundingBox) {
            // 将射线转换到 bb 的局部坐标系
            Vec3 dir = end.subtract(start);
            // 计算射线在 bb.axis 上的分量
            double dirX = dir.dotProduct(bb.axisX);
            double dirY = dir.dotProduct(bb.axisY);
            double dirZ = dir.dotProduct(bb.axisZ);
            // 射线起点相对 bb 中心的向量
            Vec3 relStart = start.subtract(bb.center);
            double startX = relStart.dotProduct(bb.axisX);
            double startY = relStart.dotProduct(bb.axisY);
            double startZ = relStart.dotProduct(bb.axisZ);

            // slab 法：在各轴求交点参数 t
            double tMin = 0.0D;
            double tMax = 1.0D;
            boolean skip = false;
            // X
            if (Math.abs(dirX) > 1e-7) {
                double invDx = 1.0D / dirX;
                double t1 = (-bb.halfWidth - startX) * invDx;
                double t2 = (bb.halfWidth - startX) * invDx;
                if (t1 > t2) {
                    double tmp = t1;
                    t1 = t2;
                    t2 = tmp;
                }
                tMin = Math.max(tMin, t1);
                tMax = Math.min(tMax, t2);
            } else if (Math.abs(startX) > bb.halfWidth + 1e-6) {
                skip = true;
            }
            // Y
            if (!skip) {
                if (Math.abs(dirY) > 1e-7) {
                    double invDy = 1.0D / dirY;
                    double t1 = (-bb.halfHeight - startY) * invDy;
                    double t2 = (bb.halfHeight - startY) * invDy;
                    if (t1 > t2) {
                        double tmp = t1;
                        t1 = t2;
                        t2 = tmp;
                    }
                    tMin = Math.max(tMin, t1);
                    tMax = Math.min(tMax, t2);
                } else if (Math.abs(startY) > bb.halfHeight + 1e-6) {
                    skip = true;
                }
            }
            // Z
            if (!skip) {
                if (Math.abs(dirZ) > 1e-7) {
                    double invDz = 1.0D / dirZ;
                    double t1 = (-bb.halfDepth - startZ) * invDz;
                    double t2 = (bb.halfDepth - startZ) * invDz;
                    if (t1 > t2) {
                        double tmp = t1;
                        t1 = t2;
                        t2 = tmp;
                    }
                    tMin = Math.max(tMin, t1);
                    tMax = Math.min(tMax, t2);
                } else if (Math.abs(startZ) > bb.halfDepth + 1e-6) {
                    skip = true;
                }
            }
            // 是否有相交
            if (!skip && tMax >= tMin && tMin <= 1.0D && tMax >= 0.0D) {
                double tHit = (tMin < 0.0D) ? tMax : tMin;
                // 计算相交点
                Vec3 hit = start.addVector(
                    dir.xCoord * tHit,
                    dir.yCoord * tHit,
                    dir.zCoord * tHit);
                double dist = start.distanceTo(hit);
                if (dist < bestDist) {
                    bestDist = dist;
                    int bx = (int) Math.floor(hit.xCoord);
                    int by = (int) Math.floor(hit.yCoord);
                    int bz = (int) Math.floor(hit.zCoord);
                    Vec3 hitVec = Vec3.createVectorHelper(hit.xCoord, hit.yCoord, hit.zCoord);
                    bestMop = new MovingObjectPosition(bx, by, bz, 0, hitVec);
                    this.ac.lastBBDamageFactor = bb.damageFactor;
                    this.ac.lastBBName = bb.name;
                }
            }
        }
        return bestMop;
    }

}
