package mcheli.weapon;

import mcheli.MCH_EntityInfo;
import mcheli.MCH_EntityInfoClientTracker;
import mcheli.MCH_Lib;
import mcheli.MCH_MOD;
import mcheli.MCH_PlayerViewHandler;
import mcheli.MCH_RadarDebug;
import mcheli.aircraft.MCH_AircraftInfo;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.network.packets.PacketLockTargetBVR;
import mcheli.render.MCH_RenderRWR;
import mcheli.tank.MCH_EntityTank;
import mcheli.wrapper.W_Entity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
public class MCH_WeaponAAMissile extends MCH_WeaponEntitySeeker {
    private static final int OPTION_FLAG_DATALINK = 1 << 8;
    private static final int OPTION_FLAG_DATALINK_TWS_SELECTED_ONLY = 1 << 9;
    private static final int OPTION_FLAG_ARM_NARROW_BAND = 1 << 10;
    private static final long SNAPSHOT_TARGET_STALE_MS = 1500L;

    public MCH_WeaponAAMissile(World w, Vec3 v, float yaw, float pitch, String nm, MCH_WeaponInfo wi) {
        super(w, v, yaw, pitch, nm, wi);
        super.power = 12;
        super.acceleration = 2.5F;
        super.explosionPower = 4;
        super.interval = 5;
        super.guidanceSystem.canLockInAir = true;
        super.guidanceSystem.ridableOnly = wi.ridableOnly;
    }

    public boolean isCooldownCountReloadTime() {
        return true;
    }

    public void update(int countWait) {
        super.update(countWait);
    }

    @Override
    public boolean shot(MCH_WeaponParam prm) {
        if (getInfo().antiRadiationMissile) {
            super.optionParameter2 &= ~OPTION_FLAG_ARM_NARROW_BAND;
            if (isArmNarrowBandMode()) {
                super.optionParameter2 |= OPTION_FLAG_ARM_NARROW_BAND;
                if (super.worldObj.isRemote && prm.entity instanceof MCH_EntityAircraft && prm.user != null) {
                    MCH_EntityAircraft ac = (MCH_EntityAircraft) prm.entity;
                    int armLockTargetId = MCH_RenderRWR.getArmTrackingTargetId(ac);
                    prm.option1 = Math.max(0, armLockTargetId);
                    super.optionParameter1 = prm.option1;
                }
            }
        }
        if (shouldBlockShotByDataLink(prm)) {
            return false;
        }
        if (shouldBlockShotByHeatSeekerDatalink(prm)) {
            return false;
        }
        if (shouldBlockShotByArmBandConstraint(prm)) {
            return false;
        }
        boolean result = false;
        float yaw, pitch;
        if (prm.entity instanceof MCH_EntityTank) {
            MCH_EntityTank tank = (MCH_EntityTank) prm.entity;
            // MCH_WeaponSet has already combined the hull and turret rotations here.
            yaw = prm.rotYaw;
            pitch = prm.rotPitch;
            int wid = tank.getCurrentWeaponID(prm.user);
            MCH_AircraftInfo.Weapon w = tank.getAcInfo().getWeaponById(wid);
            float minPitch = w == null ? tank.getAcInfo().minRotationPitch : w.minPitch;
            float maxPitch = w == null ? tank.getAcInfo().maxRotationPitch : w.maxPitch;
            float playerYaw = MathHelper.wrapAngleTo180_float(tank.getRotYaw() - yaw);
            float playerPitch = tank.getRotPitch() * MathHelper.cos((float) (playerYaw * Math.PI / 180.0D))
                + -tank.getRotRoll() * MathHelper.sin((float) (playerYaw * Math.PI / 180.0D));
            float playerYawRel = MathHelper.wrapAngleTo180_float(yaw - tank.getRotYaw());
            float yawLimit = (w == null ? 360F : w.maxYaw);
            float relativeYaw = MCH_Lib.RNG(playerYawRel, -yawLimit, yawLimit);
            yaw = MathHelper.wrapAngleTo180_float(tank.getRotYaw() + relativeYaw);
            if(fixRotationPitch == 0) {
                pitch = MCH_Lib.RNG(pitch, playerPitch + minPitch, playerPitch + maxPitch);
            }
            pitch = MCH_Lib.RNG(pitch, -90.0F, 90.0F);
        } else if (getInfo().enableOffAxis) {
            yaw = prm.user.rotationYaw + super.fixRotationYaw;
            pitch = prm.user.rotationPitch + super.fixRotationPitch;
        } else {
            yaw = prm.entity.rotationYaw + super.fixRotationYaw;
            pitch = prm.entity.rotationPitch + super.fixRotationPitch;
        }
        if (!super.worldObj.isRemote) {
            if (getInfo().passiveRadar || getInfo().activeRadar || getInfo().semiActiveRadar) {
                Entity tgtEnt = prm.user.worldObj.getEntityByID(prm.option1);
                if (!getInfo().antiRadiationMissile) {
                    boolean validTarget = isValidServerTarget(prm, tgtEnt);
                    boolean requiresTrackedTarget = getInfo().passiveRadar || getInfo().semiActiveRadar;
                    if (requiresTrackedTarget && !validTarget) {
                        return false;
                    }
                    if (!validTarget) {
                        tgtEnt = null;
                    }
                }
                this.playSound(prm.entity);
                double tX = -MathHelper.sin(yaw / 180.0F * 3.1415927F) * MathHelper.cos(pitch / 180.0F * 3.1415927F);
                double tZ = MathHelper.cos(yaw / 180.0F * 3.1415927F) * MathHelper.cos(pitch / 180.0F * 3.1415927F);
                double tY = -MathHelper.sin(pitch / 180.0F * 3.1415927F);
                MCH_EntityAAMissile e = new MCH_EntityAAMissile(super.worldObj, prm.posX, prm.posY, prm.posZ, tX, tY, tZ, yaw, pitch, (double) super.acceleration);
                if (yaw > 180.0F) {
                    yaw -= 360.0F;
                } else if (yaw < -180.0F) {
                    yaw += 360.0F;
                }
                e.setInfoByName(super.name);
                e.setParameterFromWeapon(this, prm.entity, prm.user);
                e.setDataLinkRelayMode((prm.option2 & OPTION_FLAG_DATALINK) != 0);
                e.setDataLinkTwsSelectedOnly((prm.option2 & OPTION_FLAG_DATALINK_TWS_SELECTED_ONLY) != 0);
                if (tgtEnt != null) {
                    e.setTargetEntity(tgtEnt);
                }
                if (MCH_RadarDebug.isEnabled()) {
                    double dist = -1.0D;
                    if (tgtEnt != null) {
                        double dx = prm.posX - tgtEnt.posX;
                        double dy = prm.posY - tgtEnt.posY;
                        double dz = prm.posZ - tgtEnt.posZ;
                        dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
                    }
                    MCH_RadarDebug.trace(super.worldObj, prm.entity,
                        "msl_spawn type=AA msl=%d reqTargetId=%d resolved=%s resolvedId=%d dist=%.1f dlRelay=%s twsSelOnly=%s option2=0x%X",
                        e.getEntityId(),
                        prm.option1,
                        String.valueOf(tgtEnt != null && !tgtEnt.isDead),
                        tgtEnt != null ? tgtEnt.getEntityId() : -1,
                        dist,
                        String.valueOf(e.isDataLinkRelayMode()),
                        String.valueOf(e.isDataLinkTwsSelectedOnly()),
                        prm.option2);
                }
                super.worldObj.spawnEntityInWorld(e);
                result = true;
            } else {
                Entity tgtEnt = prm.user.worldObj.getEntityByID(prm.option1);
                if (tgtEnt != null && !tgtEnt.isDead) {
                    this.playSound(prm.entity);
                    double tX = -MathHelper.sin(yaw / 180.0F * 3.1415927F) * MathHelper.cos(pitch / 180.0F * 3.1415927F);
                    double tZ = MathHelper.cos(yaw / 180.0F * 3.1415927F) * MathHelper.cos(pitch / 180.0F * 3.1415927F);
                    double tY = -MathHelper.sin(pitch / 180.0F * 3.1415927F);
                    MCH_EntityAAMissile e = new MCH_EntityAAMissile(super.worldObj, prm.posX, prm.posY, prm.posZ, tX, tY, tZ, yaw, pitch, (double) super.acceleration);
                    if (yaw > 180.0F) {
                        yaw -= 360.0F;
                    } else if (yaw < -180.0F) {
                        yaw += 360.0F;
                    }
                    e.setInfoByName(super.name);
                    e.setParameterFromWeapon(this, prm.entity, prm.user);
                    e.setDataLinkTwsSelectedOnly(false);
                    e.setTargetEntity(tgtEnt);
                    super.worldObj.spawnEntityInWorld(e);
                    result = true;
                }
            }
        } else {
            if (getInfo().activeRadar) {
                result = true;
            } else if ((getInfo().passiveRadar || getInfo().semiActiveRadar)
                && (getInfo().antiRadiationMissile || super.optionParameter1 > 0)) {
                result = true;
            } else if (isPureHeatSeeker() && super.optionParameter1 > 0) {
                result = true;
            } else if (super.guidanceSystem.lock(prm.user) && super.guidanceSystem.lastLockEntity != null) {
                result = true;
                super.optionParameter1 = W_Entity.getEntityId(super.guidanceSystem.lastLockEntity);
            }
            if(result) {
                MCH_PlayerViewHandler.applyRecoil(getInfo().getRecoilPitch(), getInfo().getRecoilYaw(), getInfo().recoilRecoverFactor);
                spawnMuzzleFlash(worldObj, prm, getInfo(), yaw, pitch, prm.muzzleFlashPosX, prm.muzzleFlashPosY, prm.muzzleFlashPosZ);
            }
        }

        return result;
    }

    private boolean isPureHeatSeeker() {
        return "aamissile".equals(getInfo().type)
            && getInfo().isHeatSeekerMissile
            && !getInfo().activeRadar
            && !getInfo().passiveRadar
            && !getInfo().semiActiveRadar
            && !getInfo().antiRadiationMissile;
    }

    private boolean isValidServerTarget(MCH_WeaponParam prm, Entity target) {
        if (prm == null || prm.entity == null || target == null || target.isDead || target == prm.entity || target == prm.user) {
            return false;
        }
        if (!super.guidanceSystem.canLockEntity(target)) {
            return false;
        }
        double maxRange = Math.max(1.0D, getInfo().maxLockOnRange);
        return prm.entity.getDistanceSqToEntity(target) <= maxRange * maxRange;
    }

    private boolean hasIntegratedRadar(MCH_WeaponParam prm) {
        if (!(prm.entity instanceof MCH_EntityAircraft)) {
            return false;
        }
        MCH_EntityAircraft ac = (MCH_EntityAircraft)prm.entity;
        return ac.getAcInfo() != null && ac.getAcInfo().enableRadar && ac.isRadarEnabledRuntime();
    }

    private void setClientTarget(Entity user, int targetId, Entity target, MCH_EntityInfo snapshot) {
        if (targetId <= 0 || (target == null && snapshot == null)) {
            super.optionParameter1 = 0;
        } else {
            super.optionParameter1 = targetId;
        }

        for (MCH_EntityBaseBullet bullet : getShootBullets(worldObj, user, getInfo().maxLockOnRange)) {
            bullet.clientSetTargetEntity(target);
            if (target == null && snapshot != null) {
                double vx = snapshot.posX - snapshot.lastTickPosX;
                double vy = snapshot.posY - snapshot.lastTickPosY;
                double vz = snapshot.posZ - snapshot.lastTickPosZ;
                bullet.setSnapshotTarget(targetId, snapshot.posX, snapshot.posY, snapshot.posZ, vx, vy, vz);
            }
        }
    }

    private boolean updateRadarTargetFromTrack(MCH_WeaponParam prm) {
        if (prm.user == null || !hasIntegratedRadar(prm) || getInfo().antiRadiationMissile
            || !(getInfo().activeRadar || getInfo().passiveRadar || getInfo().semiActiveRadar)) {
            return false;
        }
        MCH_EntityAircraft ac = (MCH_EntityAircraft)prm.entity;
        int targetId = Math.max(0, MCH_RenderRWR.getRadarTrackingTargetId(ac));
        Entity target = targetId > 0 ? prm.user.worldObj.getEntityByID(targetId) : null;
        if (target != null && (target.isDead || !super.guidanceSystem.canLockEntity(target))) {
            target = null;
            targetId = 0;
        }
        MCH_EntityInfo snapshot = targetId > 0 && target == null ? MCH_EntityInfoClientTracker.getEntityInfo(targetId) : null;
        if (snapshot != null && System.currentTimeMillis() - snapshot.lastUpdateTime > SNAPSHOT_TARGET_STALE_MS) {
            snapshot = null;
            targetId = 0;
        }
        setClientTarget(prm.user, targetId, target, snapshot);
        return true;
    }

    private boolean updateHeatSeekerTargetFromRadar(MCH_WeaponParam prm) {
        if (prm.user == null || !isPureHeatSeeker() || !hasIntegratedRadar(prm)) {
            return false;
        }
        MCH_EntityAircraft ac = (MCH_EntityAircraft)prm.entity;
        int targetId = Math.max(0, MCH_RenderRWR.getRadarTrackingTargetId(ac));
        if (targetId <= 0) {
            return false;
        }
        Entity target = prm.user.worldObj.getEntityByID(targetId);
        if (target == null || target.isDead || !super.guidanceSystem.canLockEntity(target)
            || prm.entity.getDistanceToEntity(target) > 350.0D || !isTargetInMissileFov(prm.user, target)) {
            setClientTarget(prm.user, 0, null, null);
            return true;
        }
        setClientTarget(prm.user, targetId, target, null);
        return true;
    }

    private void updateLegacySeekerTarget(MCH_WeaponParam prm) {
        Entity target = null;
        if (super.guidanceSystem.lock(prm.user) && super.guidanceSystem.lastLockEntity != null) {
            target = super.guidanceSystem.lastLockEntity;
        }
        setClientTarget(prm.user, target != null ? W_Entity.getEntityId(target) : 0, target, null);
    }

    private boolean isArmNarrowBandMode() {
        return getInfo().antiRadiationMissile && this.getCurrentMode() == 1;
    }

    private boolean shouldBlockShotByArmBandConstraint(MCH_WeaponParam prm) {
        if (!getInfo().antiRadiationMissile) {
            return false;
        }
        boolean narrowBand = (super.optionParameter2 & OPTION_FLAG_ARM_NARROW_BAND) != 0;
        if (!narrowBand) {
            return false;
        }
        boolean hasTarget = prm.user != null && prm.user.worldObj != null && prm.option1 > 0
            && (prm.user.worldObj.getEntityByID(prm.option1) != null
                || mcheli.MCH_EntityInfoClientTracker.getEntityInfo(prm.option1) != null);
        if (!hasTarget) {
            if (super.worldObj.isRemote) {
                sendDenyMessage(prm.user, "weapon.deny.narrow_band");
            }
            return true;
        }
        return false;
    }

    private boolean shouldBlockShotByDataLink(MCH_WeaponParam prm) {
        super.optionParameter2 &= ~(OPTION_FLAG_DATALINK | OPTION_FLAG_DATALINK_TWS_SELECTED_ONLY);
        if (!(prm.entity instanceof MCH_EntityAircraft) || prm.user == null || !super.worldObj.isRemote) {
            return false;
        }
        if (getInfo().antiRadiationMissile || !(getInfo().activeRadar || getInfo().passiveRadar || getInfo().semiActiveRadar)) {
            return false;
        }
        MCH_EntityAircraft ac = (MCH_EntityAircraft)prm.entity;
        MCH_WeaponSet ws = ac.getCurrentWeapon(prm.user);
        if (ws == null || ws.getInfo() == null || !ws.getInfo().enableDataLink) {
            return false;
        }
        boolean dlMode = ws.getInfo().onlyDataLink || ws.isDataLinkMode();
        if (!dlMode) {
            return false;
        }
        String searchType = MCH_RenderRWR.getRadarSearchType(ac);
        int trackingId = MCH_RenderRWR.getRadarTrackingTargetId(ac);
        int selectedId = MCH_RenderRWR.getRadarSelectedTargetId(ac);
        int targetId = -1;
        boolean twsSelectedOnlyLaunch = false;
        if (getInfo().passiveRadar || getInfo().semiActiveRadar) {
            targetId = trackingId;
            if (targetId <= 0) {
                sendDenyMessage(prm.user, "weapon.deny.lock_first");
                return true;
            }
        } else if (getInfo().activeRadar) {
            boolean srcLike = "SRC".equals(searchType) || "GMTI_SRC".equals(searchType);
            if (srcLike) {
                targetId = trackingId;
            } else {
                targetId = trackingId > 0 ? trackingId : selectedId;
                twsSelectedOnlyLaunch = trackingId <= 0 && selectedId > 0;
            }
            if (targetId <= 0) {
                sendDenyMessage(prm.user, "weapon.deny.select_or_lock");
                return true;
            }
        }
        Entity target = prm.user.worldObj.getEntityByID(targetId);
        if (target != null && !target.isDead) {
            if (!isTargetInMissileFov(prm.user, target)) {
                sendDenyMessage(prm.user, "weapon.deny.lock_first");
                return true;
            }
        } else {
            MCH_EntityInfo snap = MCH_EntityInfoClientTracker.getEntityInfo(targetId);
            if (!isSnapshotTargetUsable(prm.user, snap)) {
                sendDenyMessage(prm.user, "weapon.deny.lock_first");
                return true;
            }
        }
        super.optionParameter1 = targetId;
        super.optionParameter2 |= OPTION_FLAG_DATALINK;
        if (twsSelectedOnlyLaunch) {
            super.optionParameter2 |= OPTION_FLAG_DATALINK_TWS_SELECTED_ONLY;
        } else {
            super.optionParameter2 &= ~OPTION_FLAG_DATALINK_TWS_SELECTED_ONLY;
        }
        return false;
    }

    private boolean isTargetInMissileFov(Entity user, Entity target) {
        if (user == null || target == null) {
            return false;
        }
        Vec3 look = user.getLookVec();
        Vec3 to = Vec3.createVectorHelper(target.posX - user.posX, target.posY + target.height * 0.5D - (user.posY + user.height * 0.5D), target.posZ - user.posZ);
        double len = to.lengthVector();
        if (len <= 1.0E-6D) {
            return false;
        }
        to = to.normalize();
        double dot = look.dotProduct(to);
        dot = Math.max(-1.0D, Math.min(1.0D, dot));
        double angle = Math.acos(dot) * 180.0D / Math.PI;
        return angle <= getInfo().getEffectiveMaxDegreeOfMissile(0);
    }

    private boolean isSnapshotTargetUsable(Entity user, MCH_EntityInfo snap) {
        if (user == null || snap == null) {
            return false;
        }
        if (System.currentTimeMillis() - snap.lastUpdateTime > SNAPSHOT_TARGET_STALE_MS) {
            return false;
        }
        return isTargetInMissileFov(user, snap.posX, snap.posY, snap.posZ);
    }

    private boolean shouldBlockShotByHeatSeekerDatalink(MCH_WeaponParam prm) {
        if (!(prm.entity instanceof MCH_EntityAircraft) || prm.user == null || !super.worldObj.isRemote) {
            return false;
        }
        if (!isPureHeatSeeker()) {
            return false;
        }
        super.optionParameter2 &= ~(OPTION_FLAG_DATALINK | OPTION_FLAG_DATALINK_TWS_SELECTED_ONLY);
        MCH_EntityAircraft ac = (MCH_EntityAircraft)prm.entity;
        MCH_WeaponSet ws = ac.getCurrentWeapon(prm.user);
        if (ws == null || ws.getInfo() == null || !ws.getInfo().enableDataLink) {
            return false;
        }
        boolean dlMode = ws.getInfo().onlyDataLink || ws.isDataLinkMode();
        if (!dlMode) {
            return false;
        }
        int trackingId = MCH_RenderRWR.getRadarTrackingTargetId(ac);
        if (trackingId <= 0) {
            sendDenyMessage(prm.user, "weapon.deny.radar_lock_first");
            return true;
        }
        Entity target = prm.user.worldObj.getEntityByID(trackingId);
        if (target == null || target.isDead) {
            sendDenyMessage(prm.user, "weapon.deny.radar_lock_first");
            return true;
        }
        double dist = prm.entity.getDistanceToEntity(target);
        if (dist > 350.0D) {
            sendDenyMessage(prm.user, "weapon.deny.ir_too_far");
            return true;
        }
        super.optionParameter1 = trackingId;
        super.optionParameter2 |= OPTION_FLAG_DATALINK;
        return false;
    }

    private boolean isTargetInMissileFov(Entity user, double targetX, double targetY, double targetZ) {
        if (user == null) {
            return false;
        }
        Vec3 look = user.getLookVec();
        if (look == null) {
            return false;
        }
        Vec3 to = Vec3.createVectorHelper(targetX - user.posX, targetY - (user.posY + user.height * 0.5D), targetZ - user.posZ);
        double len = to.lengthVector();
        if (len <= 1.0E-6D) {
            return false;
        }
        to = to.normalize();
        double dot = look.dotProduct(to);
        dot = Math.max(-1.0D, Math.min(1.0D, dot));
        double angle = Math.acos(dot) * 180.0D / Math.PI;
        return angle <= getInfo().getEffectiveMaxDegreeOfMissile(0);
    }

    private void sendDenyMessage(Entity user, String translationKey) {
        if (user instanceof EntityPlayer) {
            ((EntityPlayer)user).addChatMessage(new ChatComponentTranslation(translationKey));
        }
    }

    @Override
    public boolean lock(MCH_WeaponParam prm) {
        if (!super.worldObj.isRemote) {
            // do nothing
        } else {
            if (updateDataLinkTargetsFromRadar(prm, false)) {
                return false;
            }
            if (updateRadarTargetFromTrack(prm)) {
                return false;
            }
            if (updateHeatSeekerTargetFromRadar(prm)) {
                return false;
            }
            if (isPureHeatSeeker()) {
                updateLegacySeekerTarget(prm);
                return false;
            }
            if (getInfo().passiveRadar) {
                if (!super.guidanceSystem.lock(prm.user)) {
//               if(getInfo().enableBVR && tick % 5 == 0) {
//                  Map<Integer, MCH_EntityInfo> map = MCH_RenderBVRLockBox.currentLockedEntities;
//                  if(!map.isEmpty()) {
//                     MCH_EntityInfo info = (MCH_EntityInfo) map.values().toArray()[rand.nextInt(map.size())];
//                     if(info != null) {
//                        MCH_EntityAAMissile.clientTrackingMissile.forEach((K, V) -> {
//                           int mslId = K;
//                           if (V.getInfo() != null && V.getInfo().passiveRadar && V.getInfo().enableBVR) {
//                              MCH_MOD.getPacketHandler().sendToServer(new PacketLockTargetBVR(
//                                      mslId, (int) info.posX, (int) info.posY, (int) info.posZ
//                              ));
//                           }
//                        });
//                     }
//                  }
//               }
                }
                if (guidanceSystem.isLockComplete()) {
                    Entity target = guidanceSystem.lastLockEntity;
                    super.optionParameter1 = W_Entity.getEntityId(target);
                    //获取玩家射击的AA弹
                    for (MCH_EntityBaseBullet bullet : getShootBullets(worldObj, prm.user, getInfo().maxLockOnRange)) {
                        bullet.clientSetTargetEntity(target);
                    }
                } else {
                    super.optionParameter1 = 0;
                    for (MCH_EntityBaseBullet bullet : getShootBullets(worldObj, prm.user, getInfo().maxLockOnRange)) {
                        bullet.clientSetTargetEntity(null);
                    }
                }


            }
        }
        return false; // (keep as-is unless you want non-SARH lock here)
    }


    @Override
    public void onUnlock(MCH_WeaponParam prm) {
        if (worldObj.isRemote) {
            if (updateDataLinkTargetsFromRadar(prm, false)) {
                return;
            }
            if (updateRadarTargetFromTrack(prm)) {
                return;
            }
            if (updateHeatSeekerTargetFromRadar(prm)) {
                return;
            }
            if (guidanceSystem != null && prm.user != null) {
                if (!guidanceSystem.isLockComplete()) {
                    super.optionParameter1 = 0;
                    for (MCH_EntityBaseBullet bullet : getShootBullets(worldObj, prm.user, getInfo().maxLockOnRange)) {
                        bullet.clientSetTargetEntity(null);
                    }
                }
            }

            // BVR: disable guidance on unlock
            if (getInfo() != null && getInfo().enableBVR && getInfo().passiveRadar) {
                int[] missileIds = MCH_EntityAAMissile.getClientTrackedBvrMissileIds();
                for (int mslId : missileIds) {
                    MCH_MOD.getPacketHandler().sendToServer(new PacketLockTargetBVR(mslId, 0, 0, -1, 0));
                }
            }
        }
    }

    private boolean updateDataLinkTargetsFromRadar(MCH_WeaponParam prm, boolean forceClear) {
        if (!(prm.entity instanceof MCH_EntityAircraft) || prm.user == null) {
            return false;
        }
        MCH_EntityAircraft ac = (MCH_EntityAircraft)prm.entity;
        MCH_WeaponSet ws = ac.getCurrentWeapon(prm.user);
        if (ws == null || ws.getInfo() == null || !ws.getInfo().enableDataLink || ws.getInfo().antiRadiationMissile
            || (ws.getInfo().isHeatSeekerMissile && !ws.getInfo().activeRadar && !ws.getInfo().passiveRadar && !ws.getInfo().semiActiveRadar)) {
            return false;
        }
        boolean dlMode = ws.getInfo().onlyDataLink || ws.isDataLinkMode();
        if (!dlMode) {
            return false;
        }
        int targetId = 0;
        if (!forceClear) {
            String searchType = MCH_RenderRWR.getRadarSearchType(ac);
            int trackingId = MCH_RenderRWR.getRadarTrackingTargetId(ac);
            int selectedId = MCH_RenderRWR.getRadarSelectedTargetId(ac);
            if (getInfo().passiveRadar || getInfo().semiActiveRadar) {
                targetId = Math.max(0, trackingId);
            } else if (getInfo().activeRadar) {
                boolean srcLike = "SRC".equals(searchType) || "GMTI_SRC".equals(searchType);
                targetId = srcLike ? Math.max(0, trackingId) : Math.max(0, trackingId > 0 ? trackingId : selectedId);
            }
        }
        Entity target = targetId > 0 ? prm.user.worldObj.getEntityByID(targetId) : null;
        MCH_EntityInfo snapshot = targetId > 0 && target == null ? MCH_EntityInfoClientTracker.getEntityInfo(targetId) : null;
        setClientTarget(prm.user, targetId, target, snapshot);
        return true;
    }
}
