package mcheli.weapon;

import mcheli.MCH_RadarDebug;
import mcheli.aircraft.MCH_EntityAircraft;
import net.minecraft.client.Minecraft;
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

    private static final int DL_RELAY_LOST_GRACE_TICK = 15;
    private static final int ARM_STATE_HOMING = 1;
    private static final int ARM_STATE_MEMORY = 2;
    private static final int ARM_STATE_LOST = 3;
    public boolean passiveRadarBVRLocking = false;
    public int passiveRadarBVRLockingPosX = 0;
    public int passiveRadarBVRLockingPosY = 0;
    public int passiveRadarBVRLockingPosZ = 0;
    private int dlRelayLostTick = 0;
    private int armGuidanceState = ARM_STATE_HOMING;
    private int armLastRadiationSeenTick = -1;

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

    @SideOnly(Side.CLIENT)
    private static EntityPlayer tryGetClientPlayer() {
        return Minecraft.getMinecraft().thePlayer;
    }

    public void onUpdate() {
        super.onUpdate();

        if (this.getCountOnUpdate() > 4 && this.getInfo() != null && !this.getInfo().disableSmoke && this.isWithinTrajectoryParticleEndTick()) {
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
            if (this.getInfo().antiRadiationMissile) {
                this.onUpdateArmGuidance();
                return;
            }
            boolean dlRelay = this.isDataLinkRelayMode();
            if (super.shootingEntity != null && super.targetEntity != null && !super.targetEntity.isDead) {
                if (dlRelay && (getInfo().passiveRadar || getInfo().semiActiveRadar) && !this.isDataLinkRelaySourceMaintained()) {
                    this.setTargetEntity(null);
                }
            }
            if (super.shootingEntity != null && super.targetEntity != null && !super.targetEntity.isDead) {
                if (dlRelay && this.dlRelayLostTick > 0) {
                    this.dlRelayLostTick = 0;
                }
                // Save last known target info in case we lose it.
                this.lastTargetPosX = super.targetEntity.posX;
                this.lastTargetPosY = super.targetEntity.posY;
                this.lastTargetPosZ = super.targetEntity.posZ;
                this.lastTargetVelX = super.targetEntity.motionX;
                this.lastTargetVelY = super.targetEntity.motionY;
                this.lastTargetVelZ = super.targetEntity.motionZ;
                this.hasLastKnownTarget = true;

                double x = super.posX - super.targetEntity.posX;
                double y = super.posY - super.targetEntity.posY;
                double z = super.posZ - super.targetEntity.posZ;
                double d = x * x + y * y + z * z;

                if (d > 3422500.0D) {
                    if (MCH_RadarDebug.isEnabled()) {
                        MCH_RadarDebug.trace(this.worldObj, this,
                            "msl_death type=AA reason=TARGET_DISTANCE_LIMIT msl=%d target=%d dist=%.1f distSq=%.1f limitSq=3422500.0 pos=(%.1f,%.1f,%.1f) tpos=(%.1f,%.1f,%.1f)",
                            this.getEntityId(),
                            super.targetEntity.getEntityId(),
                            Math.sqrt(d), d,
                            this.posX, this.posY, this.posZ,
                            super.targetEntity.posX, super.targetEntity.posY, super.targetEntity.posZ);
                    }
                    setDead();
                } else if (getCountOnUpdate() > getInfo().rigidityTime) {
                    guidanceToTarget(super.targetEntity.posX, super.targetEntity.posY, super.targetEntity.posZ);
                }
            } else {
                if (dlRelay) {
                    // Passive/semi-active datalink missiles lose relay => inertial flight (no autonomous reacquire).
                    if (getInfo().passiveRadar || getInfo().semiActiveRadar) {
                        this.dlRelayLostTick++;
                        if (this.dlRelayLostTick <= DL_RELAY_LOST_GRACE_TICK && this.hasLastKnownTarget) {
                            // Use last known target position + simple prediction during grace period.
                            double time = this.dlRelayLostTick;
                            double tx = this.lastTargetPosX + this.lastTargetVelX * time;
                            double ty = this.lastTargetPosY + this.lastTargetVelY * time;
                            double tz = this.lastTargetPosZ + this.lastTargetVelZ * time;
                            if (getCountOnUpdate() > getInfo().rigidityTime) {
                                guidanceToPos(tx, ty, tz);
                            }
                        } else if (this.dlRelayLostTick > DL_RELAY_LOST_GRACE_TICK) {
                            this.setDataLinkRelayMode(false);
                            this.setActiveRadarCaptured(false);
                            if (MCH_RadarDebug.isEnabled()) {
                                MCH_RadarDebug.trace(this.worldObj, this,
                                    "dl semi/passive relay timeout msl=%d grace=%d",
                                    this.getEntityId(), DL_RELAY_LOST_GRACE_TICK);
                            }
                        } else if (MCH_RadarDebug.isVerbose() && this.dlRelayLostTick == 1) {
                            MCH_RadarDebug.trace(this.worldObj, this,
                                "dl semi/passive relay grace start msl=%d grace=%d",
                                this.getEntityId(), DL_RELAY_LOST_GRACE_TICK);
                        }
                    } else if (getInfo().activeRadar) {
                        this.dlRelayLostTick = 0;
                        if (this.isDataLinkActiveRadarDelayPhase()) {
                            this.setActiveRadarCaptured(false);
                        } else if (this.isActiveRadarCaptured() && ticksExisted % getInfo().scanInterval == 0) {
                            // Active radar missile: autonomous scan starts only after onboard seeker capture phase.
                            scanForTargets();
                        }
                    }
                } else if (this.isSnapshotTargetUsable(3000L)) {
                    // Use snapshot fallback if entity is missing but snapshot is fresh (3 seconds).
                    double time = (System.currentTimeMillis() - this.snapshotLastUpdate) / 50.0;
                    double tx = this.snapshotPosX + this.snapshotVelX * time;
                    double ty = this.snapshotPosY + this.snapshotVelY * time;
                    double tz = this.snapshotPosZ + this.snapshotVelZ * time;
                    if (getCountOnUpdate() > getInfo().rigidityTime) {
                        guidanceToPos(tx, ty, tz);
                    }
                } else if ((getInfo().activeRadar || getInfo().passiveRadar || getInfo().semiActiveRadar)
                    && ticksExisted % getInfo().scanInterval == 0) {
                    if ((getInfo().passiveRadar || getInfo().semiActiveRadar) && this.wasDataLinkRelayEverEnabled()) {
                        return;
                    }
                    this.dlRelayLostTick = 0;
                    scanForTargets();
                }
            }
        }
    }

    private boolean isArmEmitterRadiating(Entity target) {
        if (!(target instanceof MCH_EntityAircraft)) {
            return false;
        }
        MCH_EntityAircraft ac = (MCH_EntityAircraft) target;
        return isArmEmitterRadiatingSource(ac);
    }

    private void saveArmLastKnownFromTarget(Entity target) {
        this.lastTargetPosX = target.posX;
        this.lastTargetPosY = target.posY;
        this.lastTargetPosZ = target.posZ;
        this.lastTargetVelX = target.motionX;
        this.lastTargetVelY = target.motionY;
        this.lastTargetVelZ = target.motionZ;
        this.hasLastKnownTarget = true;
    }

    private void guideArmToPosition(double tx, double ty, double tz) {
        if (armHojCepActive) {
            tx += (super.rand.nextDouble() - 0.5D) * 14.0D;
            tz += (super.rand.nextDouble() - 0.5D) * 14.0D;
        }
        guidanceToPosWithCruise(tx, ty, tz);
    }

    private void onUpdateArmGuidance() {
        boolean hasValidTarget = super.shootingEntity != null && super.targetEntity != null && !super.targetEntity.isDead;
        if (hasValidTarget && isArmEmitterRadiating(super.targetEntity)) {
            this.armGuidanceState = ARM_STATE_HOMING;
            this.armLastRadiationSeenTick = this.ticksExisted;
            saveArmLastKnownFromTarget(super.targetEntity);
            double x = super.posX - super.targetEntity.posX;
            double y = super.posY - super.targetEntity.posY;
            double z = super.posZ - super.targetEntity.posZ;
            double d = x * x + y * y + z * z;
            if (d > 3422500.0D) {
                if (MCH_RadarDebug.isEnabled()) {
                    MCH_RadarDebug.trace(this.worldObj, this,
                        "msl_death type=AA_ARM reason=TARGET_DISTANCE_LIMIT msl=%d target=%d dist=%.1f distSq=%.1f limitSq=3422500.0 pos=(%.1f,%.1f,%.1f) tpos=(%.1f,%.1f,%.1f)",
                        this.getEntityId(),
                        super.targetEntity.getEntityId(),
                        Math.sqrt(d), d,
                        this.posX, this.posY, this.posZ,
                        super.targetEntity.posX, super.targetEntity.posY, super.targetEntity.posZ);
                }
                setDead();
                return;
            }
            if (getCountOnUpdate() > getInfo().rigidityTime) {
                guideArmToPosition(super.targetEntity.posX, super.targetEntity.posY, super.targetEntity.posZ);
            }
            return;
        }

        if (super.targetEntity != null && !isArmEmitterRadiating(super.targetEntity)) {
            this.setTargetEntity(null);
        }
        if (super.targetEntity == null) {
            // ARM seeker reacquires radiation source continuously (not gated by scanInterval).
            scanForTargets();
        }
        hasValidTarget = super.shootingEntity != null && super.targetEntity != null && !super.targetEntity.isDead;
        if (hasValidTarget && isArmEmitterRadiating(super.targetEntity)) {
            this.armGuidanceState = ARM_STATE_HOMING;
            this.armLastRadiationSeenTick = this.ticksExisted;
            saveArmLastKnownFromTarget(super.targetEntity);
            if (getCountOnUpdate() > getInfo().rigidityTime) {
                guideArmToPosition(super.targetEntity.posX, super.targetEntity.posY, super.targetEntity.posZ);
            }
            return;
        }

        int lostTick = this.armLastRadiationSeenTick < 0 ? Integer.MAX_VALUE : this.ticksExisted - this.armLastRadiationSeenTick;
        int armGraceTick = Math.max(0, getInfo().armEmitterLostGraceTick);
        int armMemoryTick = Math.max(0, getInfo().armMemoryTimeTick);
        if (this.hasLastKnownTarget && lostTick <= armGraceTick + armMemoryTick) {
            this.armGuidanceState = ARM_STATE_MEMORY;
            if (getCountOnUpdate() > getInfo().rigidityTime) {
                // Memory phase keeps flying to the last radiating coordinate only.
                guideArmToPosition(this.lastTargetPosX, this.lastTargetPosY, this.lastTargetPosZ);
            }
            return;
        }

        this.armGuidanceState = ARM_STATE_LOST;
        if (super.targetEntity != null) {
            this.setTargetEntity(null);
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
