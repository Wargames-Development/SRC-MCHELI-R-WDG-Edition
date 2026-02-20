package mcheli.weapon;

import net.minecraft.entity.Entity;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.player.EntityPlayer;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.player.EntityPlayer;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class MCH_EntityAAMissile extends MCH_EntityBaseBullet implements MCH_IEntityLockChecker, MCH_IMissile {

/**
 * CLIENT-ONLY helper without hard-linking net.minecraft.client.* classes.
 * Uses reflection so this class can be loaded on a dedicated server.
 */
private static EntityPlayer tryGetClientPlayer() {
    try {
        Class<?> mcClz = Class.forName("net.minecraft.client.Minecraft");
        Object mc = mcClz.getMethod("getMinecraft").invoke(null);
        Object player = mcClz.getField("thePlayer").get(mc);
        return (EntityPlayer) player;
    } catch (Throwable t) {
        return null;
    }
}




    public boolean passiveRadarBVRLocking = false;
    public int passiveRadarBVRLockingPosX = 0;
    public int passiveRadarBVRLockingPosY = 0;
    public int passiveRadarBVRLockingPosZ = 0;

    // Client-side: track missiles fired by the local player so we can keep guiding them BVR
    // even after they leave render/tracking range.
    private static final Map<Integer, Long> CLIENT_BVR_MISSILES = new HashMap<Integer, Long>();
    private static final long CLIENT_BVR_MISSILE_TTL_MS = 30_000L; // keep ids for 30s


    @SideOnly(Side.CLIENT)
    public static int[] getClientTrackedBvrMissileIds() {
        long now = System.currentTimeMillis();

        // cull old entries
        Iterator<Map.Entry<Integer, Long>> it = CLIENT_BVR_MISSILES.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, Long> e = it.next();
            if (now - e.getValue() > CLIENT_BVR_MISSILE_TTL_MS) {
                it.remove();
            }
        }

        int[] out = new int[CLIENT_BVR_MISSILES.size()];
        int i = 0;
        for (Integer id : CLIENT_BVR_MISSILES.keySet()) {
            out[i++] = id;
        }
        return out;
    }

    private double calculateAngle(Entity viewer, double x, double y, double z) {
        double dx = x - viewer.posX;
        double dy = y - viewer.posY;
        double dz = z - viewer.posZ;

        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (dist < 1e-6) return 0.0;

        dx /= dist;
        dy /= dist;
        dz /= dist;

        double yawRad = Math.toRadians(viewer.rotationYaw);
        double pitchRad = Math.toRadians(viewer.rotationPitch);

        double fx = -Math.sin(yawRad) * Math.cos(pitchRad);
        double fy = -Math.sin(pitchRad);
        double fz =  Math.cos(yawRad) * Math.cos(pitchRad);

        double fLen = Math.sqrt(fx * fx + fy * fy + fz * fz);
        if (fLen > 1e-6) {
            fx /= fLen;
            fy /= fLen;
            fz /= fLen;
        }

        double dot = dx * fx + dy * fy + dz * fz;
        dot = Math.max(-1.0, Math.min(1.0, dot));
        return Math.toDegrees(Math.acos(dot));
    }

    public MCH_EntityAAMissile(World par1World) {
        super(par1World);
        super.targetEntity = null;
    }

    public MCH_EntityAAMissile(World par1World, double posX, double posY, double posZ, double targetX, double targetY, double targetZ, float yaw, float pitch, double acceleration) {
        super(par1World, posX, posY, posZ, targetX, targetY, targetZ, yaw, pitch, acceleration);
    }

    public void onUpdate() {
        super.onUpdate();

        // CLIENT: remember BVR SARH missiles fired by me / from my controlled aircraft
        if (worldObj.isRemote && getInfo() != null && getInfo().passiveRadar && getInfo().enableBVR) {
            try {
                EntityPlayer me = tryGetClientPlayer();
                if (me != null) {
                    boolean firedByMe =
                            (this.shootingEntity == me) ||
                                    (this.shootingAircraft != null && me.ridingEntity == this.shootingAircraft);

                    // UAV station controlling aircraft
                    if (!firedByMe && me.ridingEntity instanceof mcheli.uav.MCH_EntityUavStation) {
                        mcheli.aircraft.MCH_EntityAircraft ctrl =
                                ((mcheli.uav.MCH_EntityUavStation) me.ridingEntity).getControlAircract();
                        if (ctrl != null && ctrl == this.shootingAircraft) firedByMe = true;
                    }

                    if (firedByMe) {
                        CLIENT_BVR_MISSILES.put(this.getEntityId(), System.currentTimeMillis());
                    }
                }
            } catch (Throwable t) {
                // ignore
            }
        }

        if (this.getCountOnUpdate() > 4 && this.getInfo() != null && !this.getInfo().disableSmoke) {
            this.spawnExplosionParticle(this.getInfo().trajectoryParticleName, 3, 7.0F * this.getInfo().smokeSize * 0.5F);
        }

        // CLIENT: remember missiles fired by me (for BVR guidance packets later)
        if (worldObj.isRemote && getInfo() != null && getInfo().passiveRadar && getInfo().enableBVR) {
            try {
                EntityPlayer me = tryGetClientPlayer();
                if (me != null) {
                    boolean firedByMe =
                            (this.shootingEntity == me) ||
                                    (this.shootingAircraft != null && me.ridingEntity == this.shootingAircraft);

                    if (firedByMe) {
                        CLIENT_BVR_MISSILES.put(this.getEntityId(), System.currentTimeMillis());
                    }
                }
            } catch (Throwable t) {
                // keep client safe if anything weird happens in obf/env
            }
        }

        // SERVER: guidance
        if (!worldObj.isRemote && this.getInfo() != null) {
            if (getInfo().passiveRadar && getInfo().enableBVR && passiveRadarBVRLocking && targetEntity == null) {
                if (getCountOnUpdate() > getInfo().rigidityTime) {
                    double tx = passiveRadarBVRLockingPosX + 0.5D;
                    double ty = passiveRadarBVRLockingPosY + 0.5D;
                    double tz = passiveRadarBVRLockingPosZ + 0.5D;
                    guidanceToPos(tx, ty, tz);
                }
            }
            // BVR SARH point guidance (no targetEntity required)
            if (getInfo().passiveRadar && getInfo().enableBVR && passiveRadarBVRLocking && super.targetEntity == null) {
                if (getCountOnUpdate() > getInfo().rigidityTime) {
                    guidanceToPos(passiveRadarBVRLockingPosX + 0.5D,
                            passiveRadarBVRLockingPosY + 0.5D,
                            passiveRadarBVRLockingPosZ + 0.5D);
                }
                return;
            }
            // Normal entity-homing
            if (super.shootingEntity != null && super.targetEntity != null && !super.targetEntity.isDead) {

                double x = super.posX - super.targetEntity.posX;
                double y = super.posY - super.targetEntity.posY;
                double z = super.posZ - super.targetEntity.posZ;
                double d = x * x + y * y + z * z;

                if (d > 3422500.0D) {
                    setDead();
                } else if (getCountOnUpdate() > getInfo().rigidityTime) {
                    guidanceToTarget(super.targetEntity.posX, super.targetEntity.posY, super.targetEntity.posZ);
                }
            }

            // BVR SARH point-guidance (no targetEntity required)
            else if (getInfo().passiveRadar && getInfo().enableBVR && passiveRadarBVRLocking && passiveRadarBVRLockingPosY > 0) {

                if (getCountOnUpdate() > getInfo().rigidityTime) {

                    double tx = passiveRadarBVRLockingPosX + 0.5D;
                    double ty = passiveRadarBVRLockingPosY + 0.5D;
                    double tz = passiveRadarBVRLockingPosZ + 0.5D;

                    // Enforce SARH “painting cone” using the shooter/aircraft as viewer (same idea as classic SARH)
                    Entity viewer = null;
                    if (getInfo().enableHMS) {
                        if (this.shootingEntity != null) viewer = this.shootingEntity;
                    } else {
                        if (this.shootingAircraft != null) viewer = this.shootingAircraft;
                    }
                    if (viewer == null) viewer = this;

                    double dist = Math.sqrt((tx - this.posX) * (tx - this.posX) + (ty - this.posY) * (ty - this.posY) + (tz - this.posZ) * (tz - this.posZ));
                    double ang = calculateAngle(viewer, tx, ty, tz);

                    // If you stop painting (nose not pointed), drop guidance
                    if (ang > getInfo().maxLockOnAngle || dist > getInfo().maxLockOnRange) {
                        passiveRadarBVRLocking = false;
                    } else {
                        // guidanceToPos() already exists in BaseBullet and does NOT require targetEntity
                        guidanceToPos(tx, ty, tz);
                    }
                }
            }

            // Active radar scan fallback
            else {
                if (getInfo().activeRadar && ticksExisted % getInfo().scanInterval == 0) {
                    scanForTargets();
                }
            }
        }
    }


    public MCH_BulletModel getDefaultBulletModel() {
        return MCH_DefaultBulletModels.AAMissile;
    }

    @Override
    public boolean canLockEntity(Entity var1) {
        return false;
    }
}
