package mcheli.weapon;

import mcheli.MCH_EntityInfo;
import mcheli.MCH_EntityInfoClientTracker;
import mcheli.MCH_Lib;
import mcheli.MCH_PlayerViewHandler;
import mcheli.MCH_RadarDebug;
import mcheli.aircraft.MCH_AircraftInfo;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.render.MCH_RenderRWR;
import mcheli.tank.MCH_EntityTank;
import mcheli.vehicle.MCH_EntityVehicle;
import mcheli.wrapper.W_Entity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import java.util.Locale;

public class MCH_WeaponATMissile extends MCH_WeaponEntitySeeker {
    private static final int OPTION_FLAG_DATALINK = 1 << 8;
    private static final int OPTION_FLAG_DATALINK_TWS_SELECTED_ONLY = 1 << 9;
    private static final int OPTION_FLAG_ARM_NARROW_BAND = 1 << 10;
    private static final long SNAPSHOT_TARGET_STALE_MS = 1500L;

    public MCH_WeaponATMissile(World w, Vec3 v, float yaw, float pitch, String nm, MCH_WeaponInfo wi) {
        super(w, v, yaw, pitch, nm, wi);
        super.power = 32;
        super.acceleration = 2.0F;
        super.explosionPower = 4;
        super.interval = 5;
        super.numMode = 2;
        super.guidanceSystem.canLockOnGround = true;
        super.guidanceSystem.ridableOnly = wi.ridableOnly;
    }

    public boolean isCooldownCountReloadTime() {
        return true;
    }

    public String getName() {
        if (getInfo().antiRadiationMissile) {
            String suffix = isArmNarrowBandMode()
                ? (isChineseLocale() ? " [窄频]" : " [NB]")
                : (isChineseLocale() ? " [宽频]" : " [WB]");
            return super.getName() + suffix;
        }
        String opt = "";
        if (this.getCurrentMode() == 1) {
            opt = " [TA]";
        }

        return super.getName() + opt;
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
        if (shouldBlockShotByArmBandConstraint(prm)) {
            return false;
        }
        if (adjustOrBlockArmAirTarget(prm)) {
            return false;
        }
        boolean result = false;
        float yaw, pitch;
        if (getInfo().enableOffAxis) {
            yaw = prm.user.rotationYaw + super.fixRotationYaw;
            pitch = prm.user.rotationPitch + super.fixRotationPitch;
        } else {
            yaw = prm.entity.rotationYaw + super.fixRotationYaw;
            pitch = prm.entity.rotationPitch + super.fixRotationPitch;
        }
        if (prm.entity instanceof MCH_EntityTank) {
            MCH_EntityTank tank = (MCH_EntityTank) prm.entity;
            yaw += prm.randYaw;
            pitch += prm.randPitch;
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
        }
        if (!super.worldObj.isRemote) {
            if (getInfo().passiveRadar || getInfo().activeRadar || getInfo().semiActiveRadar) {
                this.playSound(prm.entity);
                double tX = -MathHelper.sin(yaw / 180.0F * 3.1415927F) * MathHelper.cos(pitch / 180.0F * 3.1415927F);
                double tZ = MathHelper.cos(yaw / 180.0F * 3.1415927F) * MathHelper.cos(pitch / 180.0F * 3.1415927F);
                double tY = -MathHelper.sin(pitch / 180.0F * 3.1415927F);
                MCH_EntityATMissile e = new MCH_EntityATMissile(super.worldObj, prm.posX, prm.posY, prm.posZ, tX, tY, tZ, yaw, pitch, (double) super.acceleration);
                if (yaw > 180.0F) {
                    yaw -= 360.0F;
                } else if (yaw < -180.0F) {
                    yaw += 360.0F;
                }
                e.setInfoByName(super.name);
                e.setParameterFromWeapon(this, prm.entity, prm.user);
                e.setDataLinkRelayMode((prm.option2 & OPTION_FLAG_DATALINK) != 0);
                e.setDataLinkTwsSelectedOnly((prm.option2 & OPTION_FLAG_DATALINK_TWS_SELECTED_ONLY) != 0);
                Entity tgtEnt = prm.user.worldObj.getEntityByID(prm.option1);
                if (tgtEnt != null && !tgtEnt.isDead) {
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
                        "msl_spawn type=AT msl=%d reqTargetId=%d resolved=%s resolvedId=%d dist=%.1f dlRelay=%s twsSelOnly=%s option2=0x%X",
                        e.getEntityId(),
                        prm.option1,
                        String.valueOf(tgtEnt != null && !tgtEnt.isDead),
                        tgtEnt != null ? tgtEnt.getEntityId() : -1,
                        dist,
                        String.valueOf(e.isDataLinkRelayMode()),
                        String.valueOf(e.isDataLinkTwsSelectedOnly()),
                        prm.option2);
                }
                e.guidanceType = prm.option2 & 0xFF;
                super.worldObj.spawnEntityInWorld(e);
                result = true;
            } else {
                Entity tgtEnt = prm.user.worldObj.getEntityByID(prm.option1);
                if (tgtEnt != null && !tgtEnt.isDead) {
                    this.playSound(prm.entity);
                    if (prm.entity instanceof MCH_EntityTank) {
                        MCH_EntityTank tank = (MCH_EntityTank) prm.entity;
                        yaw += prm.randYaw;
                        pitch += prm.randPitch;
                        float minPitch = tank.getSeatInfo(prm.entity) == null ? tank.getAcInfo().minRotationPitch : tank.getSeatInfo(prm.entity).minPitch;
                        float maxPitch = tank.getSeatInfo(prm.entity) == null ? tank.getAcInfo().maxRotationPitch : tank.getSeatInfo(prm.entity).maxPitch;
                        float playerYaw = MathHelper.wrapAngleTo180_float(tank.getRotYaw() - yaw);
                        float playerPitch = tank.getRotPitch() * MathHelper.cos((float) (playerYaw * Math.PI / 180.0D))
                            + -tank.getRotRoll() * MathHelper.sin((float) (playerYaw * Math.PI / 180.0D));
                        if(fixRotationPitch == 0) {
                            pitch = MCH_Lib.RNG(pitch, playerPitch + minPitch, playerPitch + maxPitch);
                        }
                        pitch = MCH_Lib.RNG(pitch, -90.0F, 90.0F);
                    }
                    double tX = -MathHelper.sin(yaw / 180.0F * 3.1415927F) * MathHelper.cos(pitch / 180.0F * 3.1415927F);
                    double tZ = MathHelper.cos(yaw / 180.0F * 3.1415927F) * MathHelper.cos(pitch / 180.0F * 3.1415927F);
                    double tY = -MathHelper.sin(pitch / 180.0F * 3.1415927F);
                    MCH_EntityATMissile e = new MCH_EntityATMissile(super.worldObj, prm.posX, prm.posY, prm.posZ, tX, tY, tZ, yaw, pitch, (double) super.acceleration);
                    if (yaw > 180.0F) {
                        yaw -= 360.0F;
                    } else if (yaw < -180.0F) {
                        yaw += 360.0F;
                    }
                    e.setInfoByName(super.name);
                    e.setParameterFromWeapon(this, prm.entity, prm.user);
                    e.setDataLinkTwsSelectedOnly(false);
                    e.setTargetEntity(tgtEnt);
                    e.guidanceType = prm.option2 & 0xFF;
                    super.worldObj.spawnEntityInWorld(e);
                    result = true;
                }
            }
        } else {
            if (getInfo().passiveRadar || getInfo().activeRadar || getInfo().semiActiveRadar) {
                result = true;
            } else if (super.guidanceSystem.lock(prm.user) && super.guidanceSystem.lastLockEntity != null) {
                result = true;
                super.optionParameter1 = W_Entity.getEntityId(super.guidanceSystem.lastLockEntity);
                super.optionParameter2 = this.getCurrentMode();
            }
            if(result) {
                MCH_PlayerViewHandler.applyRecoil(getInfo().getRecoilPitch(), getInfo().getRecoilYaw(), getInfo().recoilRecoverFactor);
                spawnMuzzleFlash(worldObj, prm, getInfo(), yaw, pitch, prm.muzzleFlashPosX, prm.muzzleFlashPosY, prm.muzzleFlashPosZ);
            }
        }

        return result;
    }

    private boolean isArmNarrowBandMode() {
        return getInfo().antiRadiationMissile && this.getCurrentMode() == 1;
    }

    private boolean isChineseLocale() {
        String lang = Locale.getDefault().toString().toLowerCase(Locale.ROOT);
        return lang.startsWith("zh");
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
            && prm.user.worldObj.getEntityByID(prm.option1) != null;
        if (!hasTarget) {
            if (super.worldObj.isRemote) {
                sendDenyMessage(prm.user, "窄频模式需要先锁定目标 / Narrow-band mode requires target lock");
            }
            return true;
        }
        return false;
    }

    private boolean adjustOrBlockArmAirTarget(MCH_WeaponParam prm) {
        if (!getInfo().antiRadiationMissile || prm == null || prm.user == null || prm.user.worldObj == null || prm.option1 <= 0) {
            return false;
        }
        Entity target = prm.user.worldObj.getEntityByID(prm.option1);
        if (target == null || target.isDead) {
            return false;
        }
        if (target instanceof MCH_EntityTank || target instanceof MCH_EntityVehicle) {
            return false;
        }
        boolean narrowBand = (super.optionParameter2 & OPTION_FLAG_ARM_NARROW_BAND) != 0;
        if (narrowBand) {
            if (super.worldObj.isRemote) {
                sendDenyMessage(prm.user, "ATM反辐射导弹不可锁定空中目标 / AT ARM cannot lock airborne target");
            }
            return true;
        }
        // Wide-band ARM should not inherit stale airborne lock id.
        prm.option1 = 0;
        super.optionParameter1 = 0;
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
                sendDenyMessage(prm.user, "请先锁定目标 / Please lock a target first");
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
                sendDenyMessage(prm.user, "请先选择或锁定目标 / Please select or lock a target first");
                return true;
            }
        }
        Entity target = prm.user.worldObj.getEntityByID(targetId);
        if (target != null && !target.isDead) {
            if (!isTargetInMissileFov(prm.user, target)) {
                sendDenyMessage(prm.user, "请先锁定目标 / Please lock a target first");
                return true;
            }
        } else {
            MCH_EntityInfo snap = MCH_EntityInfoClientTracker.getEntityInfo(targetId);
            if (!isSnapshotTargetUsable(prm.user, snap)) {
                sendDenyMessage(prm.user, "请先锁定目标 / Please lock a target first");
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

    private void sendDenyMessage(Entity user, String message) {
        if (user instanceof EntityPlayer) {
            ((EntityPlayer)user).addChatMessage(new ChatComponentText(message));
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
            if (getInfo().passiveRadar) {
                super.guidanceSystem.lock(prm.user);
                if (guidanceSystem.isLockComplete()) {
                    Entity target = guidanceSystem.lastLockEntity;
                    //获取玩家射击的AT弹
                    for (MCH_EntityBaseBullet bullet : getShootBullets(worldObj, prm.user, getInfo().maxLockOnRange)) {
                        bullet.clientSetTargetEntity(target);
                        super.optionParameter1 = W_Entity.getEntityId(target);
                    }
                } else {
                    for (MCH_EntityBaseBullet bullet : getShootBullets(worldObj, prm.user, getInfo().maxLockOnRange)) {
                        bullet.clientSetTargetEntity(null);
                        super.optionParameter1 = 0;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public void onUnlock(MCH_WeaponParam prm) {
        if (worldObj.isRemote) {
            if (updateDataLinkTargetsFromRadar(prm, false)) {
                return;
            }
            if (guidanceSystem != null && prm.user != null) {
                if (!guidanceSystem.isLockComplete()) {
                    for (MCH_EntityBaseBullet bullet : getShootBullets(worldObj, prm.user, getInfo().maxLockOnRange)) {
                        bullet.clientSetTargetEntity(null);
                        super.optionParameter1 = 0;
                    }
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
        if (ws == null || ws.getInfo() == null || !ws.getInfo().enableDataLink || ws.getInfo().antiRadiationMissile) {
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
        for (MCH_EntityBaseBullet bullet : getShootBullets(worldObj, prm.user, getInfo().maxLockOnRange)) {
            bullet.clientSetTargetEntity(target);
            super.optionParameter1 = target != null ? W_Entity.getEntityId(target) : 0;
        }
        return true;
    }
}
