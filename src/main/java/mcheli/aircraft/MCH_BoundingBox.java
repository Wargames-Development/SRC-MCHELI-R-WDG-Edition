package mcheli.aircraft;

import mcheli.MCH_Lib;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;

/**
 * Rotated bounding box class.
 * Stores the box’s orientation and local axis vectors
 * for more precise collision detection.
 */
public class MCH_BoundingBox {

   /** Real-time bounding box (axis-aligned, used for broad-phase collision detection) */
   public AxisAlignedBB boundingBox;
   /** Bounding box from the previous frame (retained for history) */
   public AxisAlignedBB backupBoundingBox;

   /** Initial local offset */
   public double offsetX;
   public double offsetY;
   public double offsetZ;

   /** Width (X axis), height (Y axis), and depth (Z axis) */
   public float width;
   public float widthZ;
   public float height;

   /** Half-width, half-height, and half-depth — used for internal calculations */
   public float halfWidth;
   public float halfHeight;
   public float halfDepth;

   /** Rotated offset after update (local origin vector after rotation) */
   public Vec3 rotatedOffset;
   /** Current center position and previous frame center position */
   public Vec3 nowPos;
   public Vec3 prevPos;

   /** Damage multiplier applied to this bounding box when hit */
   public float damageFactor;
   /** Bounding box type (currently unused, reserved for future extension) */
   public EnumBoundingBoxType boundingBoxType = EnumBoundingBoxType.DEFAULT;
   public String name = "";

   // === New fields: orientation and local axis vectors of the bounding box ===
   /** Current rotation angles (in degrees) */
   public float rotationYaw   = 0.0F;
   public float rotationPitch = 0.0F;
   public float rotationRoll  = 0.0F;

   /** Local axis vectors in world coordinates */
   public Vec3 axisX = Vec3.createVectorHelper(1.0D, 0.0D, 0.0D);
   public Vec3 axisY = Vec3.createVectorHelper(0.0D, 1.0D, 0.0D);
   public Vec3 axisZ = Vec3.createVectorHelper(0.0D, 0.0D, 1.0D);

   /** World coordinates of the bounding box center */
   public Vec3 center;

   public float localRotYaw   = 0.0F;
   public float localRotPitch = 0.0F;
   public float localRotRoll  = 0.0F;

   // ===== Constructors =====
   public MCH_BoundingBox(double x, double y, double z, float w, float h, float df) {
      this(x, y, z, w, h, w, df);
   }

   public MCH_BoundingBox(double posX, double posY, double posZ,
                          float widthX, float height, float widthZ, float df) {
      this.offsetX = posX;
      this.offsetY = posY;
      this.offsetZ = posZ;
      this.width  = widthX;
      this.widthZ = widthZ;
      this.height = height;
      this.halfWidth  = widthX / 2.0F;
      this.halfHeight = height  / 2.0F;
      this.halfDepth  = widthZ / 2.0F;
      this.damageFactor = df;

      this.center = Vec3.createVectorHelper(posX, posY, posZ);
      this.nowPos  = Vec3.createVectorHelper(posX, posY, posZ);
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
    * Creates a copy of this MCH_BoundingBox instance
    * (fields are not shared between instances).
    */
   public MCH_BoundingBox copy() {
      MCH_BoundingBox bb = new MCH_BoundingBox(this.offsetX, this.offsetY, this.offsetZ,
              this.width, this.height, this.widthZ, this.damageFactor);
      bb.rotationYaw   = this.rotationYaw;
      bb.rotationPitch = this.rotationPitch;
      bb.rotationRoll  = this.rotationRoll;
      bb.axisX = Vec3.createVectorHelper(this.axisX.xCoord, this.axisX.yCoord, this.axisX.zCoord);
      bb.axisY = Vec3.createVectorHelper(this.axisY.xCoord, this.axisY.yCoord, this.axisY.zCoord);
      bb.axisZ = Vec3.createVectorHelper(this.axisZ.xCoord, this.axisZ.yCoord, this.axisZ.zCoord);
      bb.center = Vec3.createVectorHelper(this.center.xCoord, this.center.yCoord, this.center.zCoord);
      bb.halfWidth  = this.halfWidth;
      bb.halfHeight = this.halfHeight;
      bb.halfDepth  = this.halfDepth;
      if (this.rotatedOffset != null) {
         bb.rotatedOffset = Vec3.createVectorHelper(
                 this.rotatedOffset.xCoord, this.rotatedOffset.yCoord, this.rotatedOffset.zCoord);
      }
      bb.nowPos  = Vec3.createVectorHelper(this.nowPos.xCoord,  this.nowPos.yCoord,  this.nowPos.zCoord);
      bb.prevPos = Vec3.createVectorHelper(this.prevPos.xCoord, this.prevPos.yCoord, this.prevPos.zCoord);
      bb.boundingBox = this.boundingBox.copy();
      bb.backupBoundingBox = this.backupBoundingBox.copy();
      bb.boundingBoxType = this.boundingBoxType;
      bb.name = this.name;
      return bb;
   }

   /**
    * Updates the bounding box state based on the entity’s world position and orientation.
    * yaw, pitch, and roll are in degrees, and the rotation order matches the original RotVec3.
    */
   public void updatePosition(double posX, double posY, double posZ,
                              float yaw, float pitch, float roll) {
      // Store the new rotation angles
      rotationYaw   = yaw;
      rotationPitch = pitch;
      rotationRoll  = roll;

      float extraYaw   = yaw;
      float extraPitch = pitch;
      float extraRoll  = roll;
      if (this.boundingBoxType == EnumBoundingBoxType.TURRET) {
         extraYaw   += localRotYaw;
         extraPitch += localRotPitch;
         extraRoll  += localRotRoll;
      }

      // Calculate the rotated offset vector
      Vec3 localOffset = Vec3.createVectorHelper(offsetX, offsetY, offsetZ);
      rotatedOffset = MCH_Lib.RotVec3(localOffset, -extraYaw, -extraPitch, -extraRoll);

      // Update the world-space center coordinates
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

      // Update the local axis unit vectors (in world coordinates)
      axisX = MCH_Lib.RotVec3(Vec3.createVectorHelper(1.0D, 0.0D, 0.0D), -extraYaw, -extraPitch, -extraRoll);
      axisY = MCH_Lib.RotVec3(Vec3.createVectorHelper(0.0D, 1.0D, 0.0D), -extraYaw, -extraPitch, -extraRoll);
      axisZ = MCH_Lib.RotVec3(Vec3.createVectorHelper(0.0D, 0.0D, 1.0D), -extraYaw, -extraPitch, -extraRoll);

      // Update the axis-aligned outer bounding box (used for quick collision checks)
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

      // Update the AxisAlignedBB wrapper
      backupBoundingBox.setBB(boundingBox);
      boundingBox.setBounds(minX, minY, minZ, maxX, maxY, maxZ);
   }

   /**
    * Determines whether two arbitrarily oriented bounding boxes intersect
    * using the full OBB–OBB Separating Axis Theorem (SAT) test.
    */
   public boolean intersectsOBB(MCH_BoundingBox other) {
      // Get the three local axes and half-sizes of the current bounding box
      Vec3[] A = new Vec3[]{this.axisX, this.axisY, this.axisZ};
      Vec3[] B = new Vec3[]{other.axisX, other.axisY, other.axisZ};
      double[] a = new double[]{this.halfWidth, this.halfHeight, this.halfDepth};
      double[] b = new double[]{other.halfWidth, other.halfHeight, other.halfDepth};

      // Compute the rotation matrix R[i][j] = dot(A_i, B_j)
      // and the absolute value matrix absR for projection radius calculations
      double[][] R = new double[3][3];
      double[][] absR = new double[3][3];
      double EPS = 1.0E-6;
      for (int i = 0; i < 3; i++) {
         for (int j = 0; j < 3; j++) {
            R[i][j] = A[i].dotProduct(B[j]);
            absR[i][j] = Math.abs(R[i][j]) + EPS; /// Avoid instability near zero
         }
      }

      // Translate the other box’s center into this box’s local coordinate system
      Vec3 d = other.center.subtract(this.center);
      double[] t = new double[]{
              d.dotProduct(A[0]),
              d.dotProduct(A[1]),
              d.dotProduct(A[2])
      };

      double ra, rb;

      // (1) Test the three axes of this bounding box: A0, A1, A2
      for (int i = 0; i < 3; i++) {
         ra = a[i];
         rb = b[0] * absR[i][0] + b[1] * absR[i][1] + b[2] * absR[i][2];
         if (Math.abs(t[i]) > ra + rb) return false;
      }

      // (2) Test the three axes of the other bounding box: B0, B1, B2
      for (int j = 0; j < 3; j++) {
         ra = a[0] * absR[0][j] + a[1] * absR[1][j] + a[2] * absR[2][j];
         rb = b[j];
         double tProj = Math.abs(t[0] * R[0][j] + t[1] * R[1][j] + t[2] * R[2][j]);
         if (tProj > ra + rb) return false;
      }

      // (3) Test the nine cross-product axes A_i × B_j
      // A0 × B0
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

      return true; // No separating axis found — intersection exists
   }

   /**
    * Treats an axis-aligned AABB as an OBB and performs a full separating-axis test.
    */
   public boolean intersectsAABB(AxisAlignedBB aabb) {
      // Calculate the AABB’s center and half-extents
      double cx = (aabb.minX + aabb.maxX) * 0.5;
      double cy = (aabb.minY + aabb.maxY) * 0.5;
      double cz = (aabb.minZ + aabb.maxZ) * 0.5;
      double hx = (aabb.maxX - aabb.minX) * 0.5;
      double hy = (aabb.maxY - aabb.minY) * 0.5;
      double hz = (aabb.maxZ - aabb.minZ) * 0.5;

      // Construct a temporary OBB equivalent to the AABB, using world axes as its orientation
      MCH_BoundingBox tmp = new MCH_BoundingBox(cx, cy, cz, (float)(hx * 2.0), (float)(hy * 2.0), (float)(hz * 2.0), 1.0F);
      tmp.center = Vec3.createVectorHelper(cx, cy, cz);
      tmp.halfWidth  = (float)hx;
      tmp.halfHeight = (float)hy;
      tmp.halfDepth  = (float)hz;
      tmp.axisX = Vec3.createVectorHelper(1.0D, 0.0D, 0.0D);
      tmp.axisY = Vec3.createVectorHelper(0.0D, 1.0D, 0.0D);
      tmp.axisZ = Vec3.createVectorHelper(0.0D, 0.0D, 1.0D);

      // Perform an OBB–OBB intersection test using intersectsOBB
      return this.intersectsOBB(tmp);
   }

   public void setBoundingBoxName(String name) {
      this.name = name;
   }
}
