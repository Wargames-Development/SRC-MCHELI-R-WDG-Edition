package mcheli.aircraft;

import mcheli.MCH_PacketNotifyLock;
import mcheli.tank.MCH_EntityTank;
import mcheli.vehicle.MCH_EntityVehicle;
import mcheli.weapon.MCH_EntityBaseBullet;
import mcheli.wrapper.W_Lib;
import mcheli.wrapper.W_McClient;
import mcheli.wrapper.W_WorldFunc;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.World;

import java.util.List;

public class MCH_MissileDetector {

    public static final int SEARCH_RANGE = 60;
    private MCH_EntityAircraft ac;
    private World world;
    private int alertCount;


    public MCH_MissileDetector(MCH_EntityAircraft aircraft, World w) {
        this.world = w;
        this.ac = aircraft;
        this.alertCount = 0;
    }

    public void update() {
        if (this.ac.haveFlare()) {
            if (this.alertCount > 0) {
                --this.alertCount;
            }

            boolean isLocked = this.ac.getEntityData().getBoolean("Tracking");
            if (isLocked) {
                this.ac.getEntityData().setBoolean("Tracking", false);
            }

            if (this.ac.getEntityData().getBoolean("LockOn")) {
                if (this.alertCount == 0) {
                    this.alertCount = 10;
                    if (this.ac != null && this.ac.haveFlare() && !this.ac.isDestroyed()) {
                        for (int rider = 0; rider < 2; ++rider) {
                            Entity entity = this.ac.getEntityBySeatId(rider);
                            if (entity instanceof EntityPlayerMP) {
                                MCH_PacketNotifyLock.sendToPlayer((EntityPlayerMP) entity);
                            }
                        }
                    }
                }

                this.ac.getEntityData().setBoolean("LockOn", false);
            }

            if (!this.ac.isDestroyed()) {
                Entity var4 = this.ac.getRiddenByEntity();
                if (var4 == null) {
                    var4 = this.ac.getEntityBySeatId(1);
                }

                if (var4 != null) {
                    if (this.ac.isFlareUsing()) {
                        this.destroyMissile();
                    } else if (!this.ac.isUAV() && !this.world.isRemote) {
                        if (this.alertCount == 0 && (isLocked || this.isLockedByMissile().result)) {
                            this.alertCount = 20;
                            if(isLockedByMissile().isRadarMissile) {
                                W_WorldFunc.MOD_playSoundAtEntity(var4, "alert_radar", 3.0F, 1.0F);
                            } else {
                                W_WorldFunc.MOD_playSoundAtEntity(var4, "alert", 3.0F, 1.0F);
                            }
                        }
                    } else if (this.ac.isUAV() && this.world.isRemote && this.alertCount == 0 && (isLocked || this.isLockedByMissile().result)) {
                        this.alertCount = 20;
                        if (W_Lib.isClientPlayer(var4)) {
                            W_McClient.MOD_playSoundFX("alert", 1.0F, 1.0F);
                        }
                    }
                }

            }
        }
    }

    public void destroyMissile() {
        if (world.isRemote) return;
        List list = this.world.getEntitiesWithinAABB(MCH_EntityBaseBullet.class, this.ac.boundingBox.expand(400.0D, 400.0D, 400.0D));
        if (list == null) {
            return;
        }
        for (Object o : list) {
            MCH_EntityBaseBullet msl = (MCH_EntityBaseBullet) o;
            if (msl.targetEntity != null && (this.ac.isMountedEntity(msl.targetEntity) || msl.targetEntity.equals(this.ac))) {
                //红外弹
                if (msl.getInfo().isHeatSeekerMissile) {
                    //抗干扰弹
                    if (msl.getInfo().antiFlareCount > 0 && !msl.antiFlareUse) {
                        msl.antiFlareUse = true;
                        msl.antiFlareTick = msl.getInfo().antiFlareCount;
                    }
                    //非抗干扰
                    else {
                        msl.setTargetEntity(null);
                    }
                }
                //雷达弹不做处理
                else if (msl.getInfo().isRadarMissile) {
                    if(ac instanceof MCH_EntityTank || ac instanceof MCH_EntityVehicle) {
                        msl.setTargetEntity(null);
                    }
                }
            }
        }
    }


    public LockResult isLockedByMissile() {
        List list = this.world.getEntitiesWithinAABB(MCH_EntityBaseBullet.class, this.ac.boundingBox.expand(600.0D, 600.0D, 600.0D));
        boolean result = false , hasRadar = false, hasHeetseeker = false;
        if (list != null) {
            for (Object o : list) {
                MCH_EntityBaseBullet msl = (MCH_EntityBaseBullet) o;
                if (msl.targetEntity != null && (this.ac.isMountedEntity(msl.targetEntity) || msl.targetEntity.equals(this.ac))) {
                    result = true;
                    if (msl.getInfo().isRadarMissile) {
                        hasRadar = true;
                    }
                    if (msl.getInfo().isHeatSeekerMissile) {
                        hasHeetseeker = true;
                    }
                }
            }
        }
        if(result) {
            return new LockResult(true, hasRadar, hasHeetseeker);
        } else {
            return new LockResult();
        }
    }

    static class LockResult {
        boolean result;
        boolean isRadarMissile;
        boolean isHeetseeker;
        public LockResult(boolean result, boolean isRadarMissile, boolean isHeetseeker) {
            this.result = result;
            this.isRadarMissile = isRadarMissile;
            this.isHeetseeker = isHeetseeker;
        }

        public LockResult() {
        }
    }
}
