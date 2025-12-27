package mcheli.aircraft;

import mcheli.MCH_MOD;
import mcheli.MCH_PacketNotifyLock;
import mcheli.helicopter.MCH_EntityHeli;
import mcheli.network.packets.PacketMissileLockType;
import mcheli.plane.MCP_EntityPlane;
import mcheli.tank.MCH_EntityTank;
import mcheli.vehicle.MCH_EntityVehicle;
import mcheli.weapon.MCH_EntityASMissile;
import mcheli.weapon.MCH_EntityBaseBullet;
import mcheli.weapon.MCH_EntityTvMissile;
import mcheli.wrapper.W_Entity;
import mcheli.wrapper.W_Lib;
import mcheli.wrapper.W_McClient;
import mcheli.wrapper.W_WorldFunc;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.World;

import java.util.List;

public class MCH_MissileDetector {

    public static final int SEARCH_RANGE = 60;
    public byte missileLockType; // 0-未锁定 1-半主动 2-红外 3-主动 4-未知
    public byte vehicleLockType; // 0-未锁定 1-扫描 2-锁定-地面载具 3-锁定-空中载具 4-锁定-未知
    public byte missileLockDist; // 0-未锁定 1-50m内 2-150m内 3-600m内
    private MCH_EntityAircraft ac;
    private World world;
    private int alertCount;

    public MCH_MissileDetector(MCH_EntityAircraft aircraft, World w) {
        this.world = w;
        this.ac = aircraft;
        this.alertCount = 0;
    }

    public void update() {
        byte missileLockType = 0;
        byte missileLockDist = 0;
        byte vehicleLockType = 0;
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
                vehicleLockType = 1;
                this.ac.getEntityData().setBoolean("LockOn", false);
            }

            if (!this.ac.isDestroyed()) {
                Entity var4 = this.ac.getRiddenByEntity();
                if (var4 == null) {
                    var4 = this.ac.getEntityBySeatId(1);
                }

                if (var4 != null) {
                    if (this.ac.isFlareUsing()) {
                        this.destroyMissileFlare();
                    } else if (this.ac.isChaffUsing()) {
                        this.destroyMissileChaff();
                    } else if (this.ac.isECMJammerUsing()) {
                        this.destroyMissileECM();
                    } else if (!this.ac.isUAV() && !this.world.isRemote) {
                        LockResult result = isLockedByMissile();
                        if (this.alertCount == 0 && (isLocked || result.isLock)) {
                            this.alertCount = 20;
                            if (result.isRadarMissile) {
                                W_WorldFunc.MOD_playSoundAtEntity(var4, "alert_radar", 3.0F, 1.0F);
                            } else {
                                W_WorldFunc.MOD_playSoundAtEntity(var4, "alert", 3.0F, 1.0F);
                            }
                        }
                        if (result.isLock) {

                            if (result.dist < 50) {
                                missileLockDist = 1;
                            } else if (result.dist < 150) {
                                missileLockDist = 2;
                            } else if (result.dist < 600) {
                                missileLockDist = 3;
                            }
                            MCH_EntityBaseBullet bullet = result.entity;
                            if (bullet != null && bullet.getInfo() != null) {
                                if (bullet.getInfo().passiveRadar) {
                                    missileLockType = 1;
                                } else if (bullet.getInfo().isHeatSeekerMissile) {
                                    missileLockType = 2;
                                } else if (bullet.getInfo().activeRadar) {
                                    missileLockType = 3;
                                } else {
                                    missileLockType = 4;
                                }
                                if (bullet.shootingAircraft instanceof MCH_EntityAircraft) {
                                    if (bullet.shootingAircraft instanceof MCH_EntityTank || bullet.shootingAircraft instanceof MCH_EntityVehicle) {
                                        vehicleLockType = 2;
                                    } else if (bullet.shootingAircraft instanceof MCP_EntityPlane || bullet.shootingAircraft instanceof MCH_EntityHeli) {
                                        vehicleLockType = 3;
                                    } else {
                                        vehicleLockType = 4;
                                    }
                                } else {
                                    vehicleLockType = 4;
                                }
                            }
                        }
                        if (ac.ticksExisted % 5 == 0) {
                            for (int rider = 0; rider < 2; ++rider) {
                                Entity entity = this.ac.getEntityBySeatId(rider);
                                if (entity instanceof EntityPlayerMP) {
                                    MCH_MOD.getPacketHandler().sendTo(new PacketMissileLockType(missileLockType, vehicleLockType, missileLockDist), (EntityPlayerMP) entity);
                                }
                            }
                        }
                    } else if (this.ac.isUAV() && this.world.isRemote && this.alertCount == 0 && (isLocked || isLockedByMissile().isLock)) {
                        this.alertCount = 20;
                        if (W_Lib.isClientPlayer(var4)) {
                            W_McClient.MOD_playSoundFX("alert", 1.0F, 1.0F);
                        }
                    }
                }

            }
        }
    }

    public void destroyMissileFlare() {
        if (world.isRemote) return;
        if (ac.getAcInfo().hasDIRCM) {
            List list = this.world.getEntitiesWithinAABB(MCH_EntityBaseBullet.class, this.ac.boundingBox.expand(30.0D, 30.0D, 30.0D));
            if (list == null) {
                return;
            }
            for (Object o : list) {
                MCH_EntityBaseBullet msl = (MCH_EntityBaseBullet) o;
                if(!W_Entity.isEqual(msl.shootingAircraft, ac)) {
                    if (msl instanceof MCH_EntityTvMissile) {
                        ((MCH_EntityTvMissile) msl).targeting = false;
                    } else if (msl instanceof MCH_EntityASMissile) {
                        ((MCH_EntityASMissile) msl).targeting = false;
                    }
                }
                if (msl.targetEntity != null && (this.ac.isMountedEntity(msl.targetEntity) || msl.targetEntity.equals(this.ac))) {
                    if (msl.getInfo().isHeatSeekerMissile) {
                        msl.setTargetEntity(null);
                    }
                }
            }
        } else {
            List list = this.world.getEntitiesWithinAABB(MCH_EntityBaseBullet.class, this.ac.boundingBox.expand(250.0D, 250.0D, 250.0D));
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
                        if (ac instanceof MCH_EntityTank || ac instanceof MCH_EntityVehicle) {
                            msl.setTargetEntity(null);
                        }
                    }
                }
            }
        }
    }

    public void destroyMissileChaff() {
        if (world.isRemote) return;
        List list = this.world.getEntitiesWithinAABB(MCH_EntityBaseBullet.class, this.ac.boundingBox.expand(100.0D, 100.0D, 100.0D));
        if (list == null) {
            return;
        }
        for (Object o : list) {
            MCH_EntityBaseBullet msl = (MCH_EntityBaseBullet) o;
            if (msl.targetEntity != null && (this.ac.isMountedEntity(msl.targetEntity) || msl.targetEntity.equals(this.ac))) {
                if (msl.getInfo().isRadarMissile) {
                    msl.setTargetEntity(null);
                }
            }
        }
    }

    public void destroyMissileECM() {
        if (world.isRemote) return;
        List list = this.world.getEntitiesWithinAABB(MCH_EntityBaseBullet.class, this.ac.boundingBox.expand(50.0D, 50.0D, 50.0D));
        if (list == null) {
            return;
        }
        for (Object o : list) {
            MCH_EntityBaseBullet msl = (MCH_EntityBaseBullet) o;
            if (msl.targetEntity != null && (this.ac.isMountedEntity(msl.targetEntity) || msl.targetEntity.equals(this.ac))) {
                if (msl.getInfo().antiRadiationMissile) {
                    msl.setTargetEntity(null);
                }
            }
        }

    }


    public LockResult isLockedByMissile() {
        List list = this.world.getEntitiesWithinAABB(MCH_EntityBaseBullet.class, this.ac.boundingBox.expand(600.0D, 600.0D, 600.0D));
        boolean result = false, hasRadar = false, hasHeetseeker = false;
        double minDist = Double.MAX_VALUE;
        MCH_EntityBaseBullet closestMissile = null;
        if (list != null) {
            for (Object o : list) {
                MCH_EntityBaseBullet msl = (MCH_EntityBaseBullet) o;
                if (msl.targetEntity != null && (this.ac.isMountedEntity(msl.targetEntity) || msl.targetEntity.equals(this.ac))) {
                    result = true;
                    double dx = msl.posX - this.ac.posX;
                    double dy = msl.posY - this.ac.posY;
                    double dz = msl.posZ - this.ac.posZ;
                    double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
                    if (dist < minDist) {
                        minDist = dist;
                        closestMissile = msl;
                    }
                    if (msl.getInfo().isRadarMissile) {
                        hasRadar = true;
                    }
                    if (msl.getInfo().isHeatSeekerMissile) {
                        hasHeetseeker = true;
                    }
                }
            }
        }
        if (result) {
            return new LockResult(true, hasRadar, hasHeetseeker, (int) minDist, closestMissile);
        } else {
            return new LockResult();
        }
    }


    public static class LockResult {
        boolean isLock;
        boolean isRadarMissile;
        boolean isHeetseeker;
        int dist;
        MCH_EntityBaseBullet entity;

        public LockResult(boolean isLock, boolean isRadarMissile, boolean isHeetseeker, int dist, MCH_EntityBaseBullet entity) {
            this.isLock = isLock;
            this.isRadarMissile = isRadarMissile;
            this.isHeetseeker = isHeetseeker;
            this.dist = dist;
            this.entity = entity;
        }

        public LockResult() {
        }
    }
}
