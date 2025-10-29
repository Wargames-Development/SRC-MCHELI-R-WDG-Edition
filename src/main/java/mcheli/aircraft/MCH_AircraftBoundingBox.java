package mcheli.aircraft;

import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

/**
 * Represents the aircraft’s overall bounding box, used for collision checks
 * against external AxisAlignedBBs and for combining the rotated bounding boxes
 * of individual components.
 * Inherits from AxisAlignedBB to maintain compatibility with Minecraft’s native logic.
 */
public class MCH_AircraftBoundingBox extends AxisAlignedBB {

   private final MCH_EntityAircraft ac;

   protected MCH_AircraftBoundingBox(MCH_EntityAircraft ac) {
      super(ac.boundingBox.minX, ac.boundingBox.minY, ac.boundingBox.minZ,
              ac.boundingBox.maxX, ac.boundingBox.maxY, ac.boundingBox.maxZ);
      this.ac = ac;
   }

   /**
    * Factory method for creating a new MCH_AircraftBoundingBox instance
    * and setting its bounds.
    */
   public AxisAlignedBB NewAABB(double minX, double minY, double minZ,
                                double maxX, double maxY, double maxZ) {
      return (new MCH_AircraftBoundingBox(this.ac)).setBounds(minX, minY, minZ, maxX, maxY, maxZ);
   }

   // Overall bounding box of the aircraft

   @Override
   public boolean intersectsWith(AxisAlignedBB aabb) {
      boolean ret = false;
      double dist = 1.0E7D;
      this.ac.lastBBDamageFactor = 1.0F;
      this.ac.lastBBName = null;

      // First perform a quick check using the overall external AABB
      if (super.intersectsWith(aabb)) {
         dist = this.getDistSq(aabb, this);
         ret = true;
      }

      // Iterate through bounding boxes of all components
      for (MCH_BoundingBox bb : this.ac.extraBoundingBox) {
         // First perform a quick filter using each part’s axis-aligned bounding box
         if (bb.boundingBox.intersectsWith(aabb)) {
            // Replace the original corner-based check with a full OBB–AABB intersection test
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
    * Gets the intersection point between the bounding box and a ray.
    * If a component’s bounding box is closer, that one is used instead.
    */
   public MovingObjectPosition calculateIntercept(Vec3 start, Vec3 end) {
      this.ac.lastBBDamageFactor = 1.0F;
      this.ac.lastBBName = null;
      MovingObjectPosition bestMop = super.calculateIntercept(start, end);
      double bestDist = (bestMop != null) ? start.distanceTo(bestMop.hitVec) : Double.MAX_VALUE;

      // Iterate through rotated component bounding boxes and perform OBB ray intersection checks
      for (MCH_BoundingBox bb : this.ac.extraBoundingBox) {
         // Transform the ray into the bounding box’s local coordinate system
         Vec3 dir = end.subtract(start);
         // Calculate the ray’s component along bb.axis
         double dirX = dir.dotProduct(bb.axisX);
         double dirY = dir.dotProduct(bb.axisY);
         double dirZ = dir.dotProduct(bb.axisZ);
         // Vector from the ray’s origin to the center of the bounding box
         Vec3 relStart = start.subtract(bb.center);
         double startX = relStart.dotProduct(bb.axisX);
         double startY = relStart.dotProduct(bb.axisY);
         double startZ = relStart.dotProduct(bb.axisZ);

         // Slab method: calculate intersection parameters t along each axis
         double tMin = 0.0D;
         double tMax = 1.0D;
         boolean skip = false;
         // X
         if (Math.abs(dirX) > 1e-7) {
            double invDx = 1.0D / dirX;
            double t1 = (-bb.halfWidth  - startX) * invDx;
            double t2 = ( bb.halfWidth  - startX) * invDx;
            if (t1 > t2) {
               double tmp = t1; t1 = t2; t2 = tmp;
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
               double t2 = ( bb.halfHeight - startY) * invDy;
               if (t1 > t2) {
                  double tmp = t1; t1 = t2; t2 = tmp;
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
               double t1 = (-bb.halfDepth  - startZ) * invDz;
               double t2 = ( bb.halfDepth  - startZ) * invDz;
               if (t1 > t2) {
                  double tmp = t1; t1 = t2; t2 = tmp;
               }
               tMin = Math.max(tMin, t1);
               tMax = Math.min(tMax, t2);
            } else if (Math.abs(startZ) > bb.halfDepth + 1e-6) {
               skip = true;
            }
         }
         // Check if an intersection exists
         if (!skip && tMax >= tMin && tMin <= 1.0D && tMax >= 0.0D) {
            double tHit = (tMin < 0.0D) ? tMax : tMin;
            // Calculate the intersection point
            Vec3 hit = start.addVector(
                    dir.xCoord * tHit,
                    dir.yCoord * tHit,
                    dir.zCoord * tHit);
            double dist = start.distanceTo(hit);
            if (dist < bestDist) {
               bestDist = dist;
               int bx = (int)Math.floor(hit.xCoord);
               int by = (int)Math.floor(hit.yCoord);
               int bz = (int)Math.floor(hit.zCoord);
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
