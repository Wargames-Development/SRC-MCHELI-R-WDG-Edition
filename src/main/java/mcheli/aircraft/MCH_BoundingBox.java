package mcheli.aircraft;

import mcheli.MCH_Lib;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;

/**
 * 旋转包围盒类，可记录包围盒的朝向和局部轴向量，以便进行更精细的碰撞检测。
 */
public class MCH_BoundingBox {

    /**
     * 实时包围盒（轴对齐，用于宽相交检测）
     */
    public AxisAlignedBB boundingBox;
    /**
     * 上一帧包围盒，保留历史
     */
    public AxisAlignedBB backupBoundingBox;

    /**
     * 初始的局部偏移
     */
    public double offsetX;
    public double offsetY;
    public double offsetZ;

    /**
     * 宽度（X 方向），高度（Y 方向），深度（Z 方向）
     */
    public float width;
    public float widthZ;
    public float height;

    /**
     * 半长半宽半高，用于内部计算
     */
    public float halfWidth;
    public float halfHeight;
    public float halfDepth;

    /**
     * 更新后旋转的偏移（局部原点经过旋转后的向量）
     */
    public Vec3 rotatedOffset;
    /**
     * 当前中心位置，上一帧中心位置
     */
    public Vec3 nowPos;
    public Vec3 prevPos;

    /**
     * 包围盒受击伤害系数
     */
    public float damageFactor;
    /**
     * 包围盒类型（暂未用，可扩展）
     */
    public EnumBoundingBoxType boundingBoxType = EnumBoundingBoxType.DEFAULT;
    public String name = "";

    // === 新增字段：记录包围盒朝向和局部轴向量 ===
    /**
     * 当前的旋转角（度）
     */
    public float rotationYaw = 0.0F;
    public float rotationPitch = 0.0F;
    public float rotationRoll = 0.0F;

    /**
     * 世界坐标下的局部轴向量
     */
    public Vec3 axisX = Vec3.createVectorHelper(1.0D, 0.0D, 0.0D);
    public Vec3 axisY = Vec3.createVectorHelper(0.0D, 1.0D, 0.0D);
    public Vec3 axisZ = Vec3.createVectorHelper(0.0D, 0.0D, 1.0D);

    /**
     * 包围盒中心的世界坐标
     */
    public Vec3 center;

    public float localRotYaw = 0.0F;
    public float localRotPitch = 0.0F;
    public float localRotRoll = 0.0F;

    // ===== 构造函数 =====
    public MCH_BoundingBox(double x, double y, double z, float w, float h, float df) {
        this(x, y, z, w, h, w, df);
    }

    public MCH_BoundingBox(double posX, double posY, double posZ,
                           float widthX, float height, float widthZ, float df) {
        this.offsetX = posX;
        this.offsetY = posY;
        this.offsetZ = posZ;
        this.width = widthX;
        this.widthZ = widthZ;
        this.height = height;
        this.halfWidth = widthX / 2.0F;
        this.halfHeight = height / 2.0F;
        this.halfDepth = widthZ / 2.0F;
        this.damageFactor = df;

        this.center = Vec3.createVectorHelper(posX, posY, posZ);
        this.nowPos = Vec3.createVectorHelper(posX, posY, posZ);
        this.prevPos = Vec3.createVectorHelper(posX, posY, posZ);

        this.boundingBox = AxisAlignedBB.getBoundingBox(
            posX - halfWidth, posY - halfHeight, posZ - halfDepth,
            posX + halfWidth, posY + halfHeight, posZ + halfDepth);
        this.backupBoundingBox = this.boundingBox.copy();
    }

    public void setBoundingBoxType(EnumBoundingBoxType type) {
        this.boundingBoxType = type;
    }

    /**
     * 复制一个新的 MCH_BoundingBox 实例（不共享实例字段）。
     */
    public MCH_BoundingBox copy() {
        MCH_BoundingBox bb = new MCH_BoundingBox(this.offsetX, this.offsetY, this.offsetZ,
            this.width, this.height, this.widthZ, this.damageFactor);
        bb.rotationYaw = this.rotationYaw;
        bb.rotationPitch = this.rotationPitch;
        bb.rotationRoll = this.rotationRoll;
        bb.axisX = Vec3.createVectorHelper(this.axisX.xCoord, this.axisX.yCoord, this.axisX.zCoord);
        bb.axisY = Vec3.createVectorHelper(this.axisY.xCoord, this.axisY.yCoord, this.axisY.zCoord);
        bb.axisZ = Vec3.createVectorHelper(this.axisZ.xCoord, this.axisZ.yCoord, this.axisZ.zCoord);
        bb.center = Vec3.createVectorHelper(this.center.xCoord, this.center.yCoord, this.center.zCoord);
        bb.halfWidth = this.halfWidth;
        bb.halfHeight = this.halfHeight;
        bb.halfDepth = this.halfDepth;
        if (this.rotatedOffset != null) {
            bb.rotatedOffset = Vec3.createVectorHelper(
                this.rotatedOffset.xCoord, this.rotatedOffset.yCoord, this.rotatedOffset.zCoord);
        }
        bb.nowPos = Vec3.createVectorHelper(this.nowPos.xCoord, this.nowPos.yCoord, this.nowPos.zCoord);
        bb.prevPos = Vec3.createVectorHelper(this.prevPos.xCoord, this.prevPos.yCoord, this.prevPos.zCoord);
        bb.boundingBox = this.boundingBox.copy();
        bb.backupBoundingBox = this.backupBoundingBox.copy();
        bb.boundingBoxType = this.boundingBoxType;
        bb.name = this.name;
        return bb;
    }

    /**
     * 根据实体的世界位置和朝向更新包围盒状态。
     * yaw、pitch、roll 为角度制，旋转顺序与原版 RotVec3 相同。
     */
    public void updatePosition(double posX, double posY, double posZ,
                               float yaw, float pitch, float roll) {
        // 保存新的旋转角
        rotationYaw = yaw;
        rotationPitch = pitch;
        rotationRoll = roll;

        float extraYaw = yaw;
        float extraPitch = pitch;
        float extraRoll = roll;
        if (this.boundingBoxType == EnumBoundingBoxType.TURRET) {
            extraYaw += localRotYaw;
            extraPitch += localRotPitch;
            extraRoll += localRotRoll;
        }

        // 计算旋转后的偏移量
        Vec3 localOffset = Vec3.createVectorHelper(offsetX, offsetY, offsetZ);
        rotatedOffset = MCH_Lib.RotVec3(localOffset, -extraYaw, -extraPitch, -extraRoll);

        // 更新中心坐标（世界）
        double cx = posX + rotatedOffset.xCoord;
        double cy = posY + rotatedOffset.yCoord;
        double cz = posZ + rotatedOffset.zCoord;

        prevPos.xCoord = nowPos.xCoord;
        prevPos.yCoord = nowPos.yCoord;
        prevPos.zCoord = nowPos.zCoord;

        nowPos.xCoord = cx;
        nowPos.yCoord = cy;
        nowPos.zCoord = cz;

        center = Vec3.createVectorHelper(cx, cy, cz);

        // 更新局部轴向量（单位向量）
        axisX = MCH_Lib.RotVec3(Vec3.createVectorHelper(1.0D, 0.0D, 0.0D), -extraYaw, -extraPitch, -extraRoll);
        axisY = MCH_Lib.RotVec3(Vec3.createVectorHelper(0.0D, 1.0D, 0.0D), -extraYaw, -extraPitch, -extraRoll);
        axisZ = MCH_Lib.RotVec3(Vec3.createVectorHelper(0.0D, 0.0D, 1.0D), -extraYaw, -extraPitch, -extraRoll);

        // 更新轴对齐外包围盒（用于快速检测）
        double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY, minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY, maxZ = Double.NEGATIVE_INFINITY;

        for (int xi = -1; xi <= 1; xi += 2) {
            for (int yi = -1; yi <= 1; yi += 2) {
                for (int zi = -1; zi <= 1; zi += 2) {
                    Vec3 cornerLocal = Vec3.createVectorHelper(
                        offsetX + xi * halfWidth,
                        offsetY + yi * halfHeight,
                        offsetZ + zi * halfDepth);
                    Vec3 cornerWorld = MCH_Lib.RotVec3(cornerLocal, -extraYaw, -extraPitch, -extraRoll);
                    double px = posX + cornerWorld.xCoord;
                    double py = posY + cornerWorld.yCoord;
                    double pz = posZ + cornerWorld.zCoord;
                    if (px < minX) minX = px;
                    if (py < minY) minY = py;
                    if (pz < minZ) minZ = pz;
                    if (px > maxX) maxX = px;
                    if (py > maxY) maxY = py;
                    if (pz > maxZ) maxZ = pz;
                }
            }
        }

        // 更新 AxisAlignedBB 封装
        backupBoundingBox.setBB(boundingBox);
        boundingBox.setBounds(minX, minY, minZ, maxX, maxY, maxZ);
    }

    /**
     * 判断两个任意方向的包围盒是否相交（完整的 OBB-OBB 分离轴判定）。
     */
    public boolean intersectsOBB(MCH_BoundingBox other) {
        // 取当前包围盒的三个局部轴和半长度
        Vec3[] A = new Vec3[]{this.axisX, this.axisY, this.axisZ};
        Vec3[] B = new Vec3[]{other.axisX, other.axisY, other.axisZ};
        double[] a = new double[]{this.halfWidth, this.halfHeight, this.halfDepth};
        double[] b = new double[]{other.halfWidth, other.halfHeight, other.halfDepth};

        // 计算旋转矩阵 R[i][j] = dot(A_i, B_j)，并记录绝对值矩阵 absR 以便求投影半径
        double[][] R = new double[3][3];
        double[][] absR = new double[3][3];
        double EPS = 1.0E-6;
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                R[i][j] = A[i].dotProduct(B[j]);
                absR[i][j] = Math.abs(R[i][j]) + EPS; // 避免接近零的数值不稳定
            }
        }

        // 将另一包围盒中心移到当前包围盒局部坐标系下
        Vec3 d = other.center.subtract(this.center);
        double[] t = new double[]{
            d.dotProduct(A[0]),
            d.dotProduct(A[1]),
            d.dotProduct(A[2])
        };

        double ra, rb;

        // (1) 本包围盒的三个轴 A0/A1/A2
        for (int i = 0; i < 3; i++) {
            ra = a[i];
            rb = b[0] * absR[i][0] + b[1] * absR[i][1] + b[2] * absR[i][2];
            if (Math.abs(t[i]) > ra + rb) return false;
        }

        // (2) 另一包围盒的三个轴 B0/B1/B2
        for (int j = 0; j < 3; j++) {
            ra = a[0] * absR[0][j] + a[1] * absR[1][j] + a[2] * absR[2][j];
            rb = b[j];
            double tProj = Math.abs(t[0] * R[0][j] + t[1] * R[1][j] + t[2] * R[2][j]);
            if (tProj > ra + rb) return false;
        }

        // (3) 九个交叉轴 A_i × B_j
        // A0 x B0
        ra = a[1] * absR[2][0] + a[2] * absR[1][0];
        rb = b[1] * absR[0][2] + b[2] * absR[0][1];
        if (Math.abs(t[2] * R[1][0] - t[1] * R[2][0]) > ra + rb) return false;
        // A0 x B1
        ra = a[1] * absR[2][1] + a[2] * absR[1][1];
        rb = b[0] * absR[0][2] + b[2] * absR[0][0];
        if (Math.abs(t[2] * R[1][1] - t[1] * R[2][1]) > ra + rb) return false;
        // A0 x B2
        ra = a[1] * absR[2][2] + a[2] * absR[1][2];
        rb = b[0] * absR[0][1] + b[1] * absR[0][0];
        if (Math.abs(t[2] * R[1][2] - t[1] * R[2][2]) > ra + rb) return false;

        // A1 x B0
        ra = a[0] * absR[2][0] + a[2] * absR[0][0];
        rb = b[1] * absR[1][2] + b[2] * absR[1][1];
        if (Math.abs(t[0] * R[2][0] - t[2] * R[0][0]) > ra + rb) return false;
        // A1 x B1
        ra = a[0] * absR[2][1] + a[2] * absR[0][1];
        rb = b[0] * absR[1][2] + b[2] * absR[1][0];
        if (Math.abs(t[0] * R[2][1] - t[2] * R[0][1]) > ra + rb) return false;
        // A1 x B2
        ra = a[0] * absR[2][2] + a[2] * absR[0][2];
        rb = b[0] * absR[1][1] + b[1] * absR[1][0];
        if (Math.abs(t[0] * R[2][2] - t[2] * R[0][2]) > ra + rb) return false;

        // A2 x B0
        ra = a[0] * absR[1][0] + a[1] * absR[0][0];
        rb = b[1] * absR[2][2] + b[2] * absR[2][1];
        if (Math.abs(t[1] * R[0][0] - t[0] * R[1][0]) > ra + rb) return false;
        // A2 x B1
        ra = a[0] * absR[1][1] + a[1] * absR[0][1];
        rb = b[0] * absR[2][2] + b[2] * absR[2][0];
        if (Math.abs(t[1] * R[0][1] - t[0] * R[1][1]) > ra + rb) return false;
        // A2 x B2
        ra = a[0] * absR[1][2] + a[1] * absR[0][2];
        rb = b[0] * absR[2][1] + b[1] * absR[2][0];
        if (Math.abs(t[1] * R[0][2] - t[0] * R[1][2]) > ra + rb) return false;

        return true; // 无分离轴，存在交集
    }

    /**
     * 将一个轴对齐的 AABB 看作 OBB，进行完整的分离轴检测。
     */
    public boolean intersectsAABB(AxisAlignedBB aabb) {
        // 计算 AABB 的中心和半边长
        double cx = (aabb.minX + aabb.maxX) * 0.5;
        double cy = (aabb.minY + aabb.maxY) * 0.5;
        double cz = (aabb.minZ + aabb.maxZ) * 0.5;
        double hx = (aabb.maxX - aabb.minX) * 0.5;
        double hy = (aabb.maxY - aabb.minY) * 0.5;
        double hz = (aabb.maxZ - aabb.minZ) * 0.5;

        // 构造一个与 AABB 等价的临时 OBB，它的轴就是世界坐标轴
        MCH_BoundingBox tmp = new MCH_BoundingBox(cx, cy, cz, (float) (hx * 2.0), (float) (hy * 2.0), (float) (hz * 2.0), 1.0F);
        tmp.center = Vec3.createVectorHelper(cx, cy, cz);
        tmp.halfWidth = (float) hx;
        tmp.halfHeight = (float) hy;
        tmp.halfDepth = (float) hz;
        tmp.axisX = Vec3.createVectorHelper(1.0D, 0.0D, 0.0D);
        tmp.axisY = Vec3.createVectorHelper(0.0D, 1.0D, 0.0D);
        tmp.axisZ = Vec3.createVectorHelper(0.0D, 0.0D, 1.0D);

        // 直接使用 intersectsOBB 进行 OBB-OBB 判定
        return this.intersectsOBB(tmp);
    }

    public void setBoundingBoxName(String name) {
        this.name = name;
    }
}
