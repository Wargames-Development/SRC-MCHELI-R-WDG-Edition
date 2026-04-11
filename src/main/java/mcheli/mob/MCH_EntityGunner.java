package mcheli.mob;

import java.util.List;
import mcheli.MCH_Config;
import mcheli.MCH_Lib;
import mcheli.MCH_MOD;
import mcheli.aircraft.MCH_AircraftInfo;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.aircraft.MCH_EntitySeat;
import mcheli.aircraft.MCH_SeatInfo;
import mcheli.helicopter.MCH_EntityHeli;
import mcheli.plane.MCP_EntityPlane;
import mcheli.tank.MCH_EntityTank;
import mcheli.weapon.*;
import mcheli.wrapper.W_WorldFunc;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.DamageSource;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;

public class MCH_EntityGunner extends EntityLivingBase {
    public static final int TARGET_MONSTER = 0;
    public static final int TARGET_PLAYER = 1;
    public static final int TARGET_AA_AMMO = 2;
    public static final int TARGET_ENEMY = 3;
    private static final int HELI_STATE_CRUISE = 0;
    private static final int HELI_STATE_FOCUS = 3;
    private static final int HELI_STATE_ATTACK = 1;
    private static final int HELI_STATE_DISENGAGE = 2;
    private static final int PLANE_STATE_SEARCH = 0;
    private static final int PLANE_STATE_FOCUS = 1;
    private static final int PLANE_STATE_ATTACK = 2;
    private static final int PLANE_STATE_DISENGAGE = 3;
    private static final int PLANE_STATE_RTB = 4;
    private static final int PLANE_MANEUVER_STRAIGHT = 0;
    private static final int PLANE_MANEUVER_ORBIT = 1;
    private static final int PLANE_MANEUVER_EXTEND = 2;
    private static final int PLANE_MANEUVER_BREAK_TURN = 3;
    private static final int PLANE_MANEUVER_YOYO_HIGH = 4;
    public boolean isCreative = false;
    public String ownerUUID = "";
    public int targetType = 0;
    public int despawnCount = 0;
    public int switchTargetCount = 0;
    public Entity targetEntity = null;
    public double targetPrevPosX = 0.0D;
    public double targetPrevPosY = 0.0D;
    public double targetPrevPosZ = 0.0D;
    public boolean waitCooldown = false;
    public int idleCount = 0;
    public int idleRotation = 0;
    private int lastTargetUpdateTick = -1;
    private int wanderTurnTicks = 0;
    private int wanderTurnDir = 0;
    private int combatMoveTicks = 0;
    private float combatYawBias = 0.0F;
    private int combatStrafeDir = 1;
    private int throttlePulseTicks = 0;
    private boolean throttlePulseOn = true;
    private int leftTurnAccum = 0;
    private int rightTurnAccum = 0;
    private int turnSampleTicks = 0;
    private int turnFeedbackTicks = 0;
    private int turnFeedbackDir = 0;
    private int prevRideEntityId = -1;
    private int noMoveTicks = 0;
    private int noMoveTurnThreshold = 340;
    private int noMoveTurnCooldownTicks = 0;
    private int obstacleTurnDir = 0;
    private float largeTurnRemain = 0.0F;
    private int largeTurnDir = 0;
    private int heliState = 0;
    private int heliStateTicks = 0;
    private int heliStateDuration = 0;
    private float heliCruiseAltitude = 50.0F;
    private boolean heliAllowFire = true;
    private int heliPatrolYawTicks = 0;
    private float heliPatrolYawStep = 0.0F;
    private int heliCruiseTurnCooldown = 0;
    private float heliCruiseTurnRemain = 0.0F;
    private int heliCruiseTurnDir = 0;
    private double heliLastTargetX = 0.0D;
    private double heliLastTargetZ = 0.0D;
    private int weaponRotateTicks = 0;
    private int weaponRotateThreshold = 240;
    private int weaponRotateWeaponId = -1;
    private int planeState = PLANE_STATE_SEARCH;
    private int planeStateTicks = 0;
    private int planeStateDuration = 0;
    private boolean planeAllowFire = false;
    private boolean planeOriginInitialized = false;
    private double planeOriginX = 0.0D;
    private double planeOriginZ = 0.0D;
    private double planeLastTargetX = 0.0D;
    private double planeLastTargetZ = 0.0D;
    private int planeAimStableTicks = 0;
    private int planeAimStableNeed = 10;
    private int planeManeuver = PLANE_MANEUVER_STRAIGHT;
    private int planeManeuverTicks = 0;
    private int planeManeuverDuration = 0;
    private int planeManeuverDir = 1;
    private int planeDiveCooldownTicks = 0;
    private int planeDiveActiveTicks = 0;
    private int autoCountermeasureCooldown = 0;

    public MCH_EntityGunner(World world) {
        super(world);
    }

    public MCH_EntityGunner(World world, double x, double y, double z) {
        this(world);
        setPosition(x, y, z);
    }

    protected void entityInit() {
        super.entityInit();
        getDataWatcher().addObject(17, "");
        getDataWatcher().addObject(18, Integer.valueOf(0));
    }

    public String getTeamName() {
        return getDataWatcher().getWatchableObjectString(17);
    }

    public void setTeamName(String name) {
        getDataWatcher().updateObject(17, name);
    }

    public int getTargetType() {
        return getDataWatcher().getWatchableObjectInt(18);
    }

    public void setTargetType(int type) {
        int v = MathHelper.clamp_int(type, TARGET_MONSTER, TARGET_ENEMY);
        this.targetType = v;
        getDataWatcher().updateObject(18, Integer.valueOf(v));
    }

    public Team getTeam() {
        return (Team)this.worldObj.getScoreboard().getTeam(getTeamName());
    }

    public boolean isOnSameTeam(EntityLivingBase p_142014_1_) {
        return super.isOnSameTeam(p_142014_1_);
    }

    public IChatComponent getFormattedCommandSenderName() {
        Team team = getTeam();
        if (team != null)
            return (IChatComponent)new ChatComponentText(ScorePlayerTeam.formatPlayerName(team, team.getRegisteredName() + " Gunner"));
        return (IChatComponent)new ChatComponentText("");
    }

    public boolean isEntityInvulnerable() {
        return this.isCreative;
    }

    public void onDeath(DamageSource source) {
        super.onDeath(source);
    }

    public boolean interactFirst(EntityPlayer player) {
        if (this.worldObj.isRemote)
            return false;
        if (this.ridingEntity == null)
            return false;
        if (player.capabilities.isCreativeMode) {
            removeFromAircraft(player);
            return true;
        }
        if (this.isCreative) {
            player.addChatMessage((IChatComponent)new ChatComponentText("Creative mode only."));
            return false;
        }
        if (getTeam() == null || isOnSameTeam((EntityLivingBase)player)) {
            removeFromAircraft(player);
            return true;
        }
        player.addChatMessage((IChatComponent)new ChatComponentText("You are other team."));
        return false;
    }

    public void removeFromAircraft(EntityPlayer player) {
        if (!this.worldObj.isRemote) {
            W_WorldFunc.MOD_playSoundAtEntity((Entity)player, "wrench", 1.0F, 1.0F);
            setDead();
            MCH_EntityAircraft ac = null;
            if (this.ridingEntity instanceof MCH_EntityAircraft) {
                ac = (MCH_EntityAircraft)this.ridingEntity;
            } else if (this.ridingEntity instanceof MCH_EntitySeat) {
                ac = ((MCH_EntitySeat)this.ridingEntity).getParent();
            }
            String name = "";
            if (ac != null && ac.getAcInfo() != null)
                name = " on " + (ac.getAcInfo()).displayName + " seat " + (ac.getSeatIdByEntity((Entity)this) + 1);
            player.addChatMessage((IChatComponent)new ChatComponentText("Remove gunner" + name + " by " + ScorePlayerTeam.formatPlayerName(player.getTeam(), player.getDisplayName()) + "."));
            mountEntity(null);
        }
    }

    public void onUpdate() {
        super.onUpdate();
        if (this.worldObj.isRemote) {
            this.targetType = getTargetType();
        }
        if (!this.worldObj.isRemote && !this.isDead) {
            if (this.ridingEntity != null && this.ridingEntity.isDead)
                this.ridingEntity = null;
            int rideId = this.ridingEntity != null ? this.ridingEntity.getEntityId() : -1;
            if (rideId != this.prevRideEntityId) {
                this.prevRideEntityId = rideId;
                this.switchTargetCount = 0;
                this.targetEntity = null;
                this.turnSampleTicks = 0;
                this.leftTurnAccum = 0;
                this.rightTurnAccum = 0;
                this.turnFeedbackTicks = 0;
                this.turnFeedbackDir = 0;
                this.noMoveTicks = 0;
                this.noMoveTurnThreshold = 300 + this.rand.nextInt(101);
                this.noMoveTurnCooldownTicks = 0;
                this.obstacleTurnDir = 0;
                this.largeTurnRemain = 0.0F;
                this.largeTurnDir = 0;
                this.heliState = HELI_STATE_CRUISE;
                this.heliStateTicks = 0;
                this.heliStateDuration = 0;
                this.heliCruiseAltitude = 40.0F + this.rand.nextFloat() * 20.0F;
                this.heliAllowFire = true;
                this.heliPatrolYawTicks = 0;
                this.heliPatrolYawStep = 0.0F;
                this.heliCruiseTurnCooldown = 40 + this.rand.nextInt(81);
                this.heliCruiseTurnRemain = 0.0F;
                this.heliCruiseTurnDir = 0;
                this.heliLastTargetX = 0.0D;
                this.heliLastTargetZ = 0.0D;
                this.weaponRotateTicks = 0;
                this.weaponRotateThreshold = 200 + this.rand.nextInt(101);
                this.weaponRotateWeaponId = -1;
                this.autoCountermeasureCooldown = 0;
                this.planeState = PLANE_STATE_SEARCH;
                this.planeStateTicks = 0;
                this.planeStateDuration = 0;
                this.planeAllowFire = false;
                this.planeOriginInitialized = false;
                this.planeOriginX = 0.0D;
                this.planeOriginZ = 0.0D;
                this.planeLastTargetX = 0.0D;
                this.planeLastTargetZ = 0.0D;
                this.planeAimStableTicks = 0;
                this.planeAimStableNeed = 10;
                this.planeManeuver = PLANE_MANEUVER_STRAIGHT;
                this.planeManeuverTicks = 0;
                this.planeManeuverDuration = 0;
                this.planeManeuverDir = this.rand.nextBoolean() ? 1 : -1;
                this.planeDiveCooldownTicks = 0;
                this.planeDiveActiveTicks = 0;
                if (this.ridingEntity instanceof MCH_EntityHeli || (this.ridingEntity instanceof MCH_EntitySeat && ((MCH_EntitySeat)this.ridingEntity).getParent() instanceof MCH_EntityHeli)) {
                    this.heliState = HELI_STATE_DISENGAGE;
                    this.heliStateDuration = 100 + this.rand.nextInt(51);
                }
                if (this.ridingEntity instanceof MCP_EntityPlane || (this.ridingEntity instanceof MCH_EntitySeat && ((MCH_EntitySeat)this.ridingEntity).getParent() instanceof MCP_EntityPlane)) {
                    enterPlaneState(PLANE_STATE_SEARCH, MCH_Config.GunnerPlaneStateSearchMin.prmInt, MCH_Config.GunnerPlaneStateSearchMax.prmInt);
                }
                this.lastTargetUpdateTick = -1;
            }
            MCH_EntityAircraft ac = null;
            if (this.ridingEntity instanceof MCH_EntityAircraft) {
                ac = (MCH_EntityAircraft)this.ridingEntity;
            } else if (this.ridingEntity instanceof MCH_EntitySeat && ((MCH_EntitySeat)this.ridingEntity).getParent() != null) {
                ac = ((MCH_EntitySeat)this.ridingEntity).getParent();
            }
            if (ac != null) {
                if (!ac.getGunnerStatus()) {
                    ac.setGunnerStatus(true);
                }
                if (this.targetEntity != null && (this.targetEntity.isDead || this.getDistanceSqToEntity(this.targetEntity) > 160000.0D)) {
                    this.targetEntity = null;
                    this.switchTargetCount = 0;
                    this.lastTargetUpdateTick = -1;
                }
                if (this.targetEntity == null && this.switchTargetCount > 0) {
                    this.switchTargetCount = 0;
                }
                if (ac instanceof MCH_EntityTank && ac.getGunnerStatus()) {
                    MCH_WeaponSet ws = ac.getCurrentWeapon((Entity)this);
                    if (ws != null && ws.getInfo() != null && ws.getCurrentWeapon() != null) {
                        Vec3 pos = getGunnerWeaponPos(ac, ws);
                        updateTargetForWeapon(ac, ws, pos);
                    }
                    updateTankDrive((MCH_EntityTank)ac);
                } else if (ac instanceof MCH_EntityHeli && ac.getGunnerStatus()) {
                    MCH_WeaponSet ws = ac.getCurrentWeapon((Entity)this);
                    if (ws != null && ws.getInfo() != null && ws.getCurrentWeapon() != null) {
                        Vec3 pos = getGunnerWeaponPos(ac, ws);
                        updateTargetForWeapon(ac, ws, pos);
                    }
                    updateHeliDrive((MCH_EntityHeli)ac);
                } else if (ac instanceof MCP_EntityPlane && ac.getGunnerStatus()) {
                    MCH_WeaponSet ws = ac.getCurrentWeapon((Entity)this);
                    if (ws != null && ws.getInfo() != null && ws.getCurrentWeapon() != null) {
                        Vec3 pos = getGunnerWeaponPos(ac, ws);
                        updateTargetForWeapon(ac, ws, pos);
                    }
                    updatePlaneDrive((MCP_EntityPlane)ac);
                }
                autoUseCountermeasures(ac);
                updateWeaponRotation(ac);
                shotTarget(ac);
            } else if (this.despawnCount < 20) {
                this.despawnCount++;
            } else if (this.ridingEntity == null || this.ticksExisted > 100) {
                setDead();
            }
            if (this.targetEntity == null) {
                if (this.idleCount == 0) {
                    this.idleCount = (3 + this.rand.nextInt(5)) * 20;
                    this.idleRotation = this.rand.nextInt(5) - 2;
                }
                this.rotationYaw += this.idleRotation / 2.0F;
            } else {
                this.idleCount = 60;
            }
        }
        if (this.switchTargetCount > 0)
            this.switchTargetCount--;
        if (this.idleCount > 0)
            this.idleCount--;
        if (this.autoCountermeasureCooldown > 0)
            this.autoCountermeasureCooldown--;
    }

    private void autoUseCountermeasures(MCH_EntityAircraft ac) {
        if (ac == null || this.worldObj.isRemote || ac.isDestroyed())
            return;
        if (this.autoCountermeasureCooldown > 0 || this.ticksExisted % 5 != 0)
            return;
        if (!hasIncomingGuidedThreat(ac))
            return;
        boolean used = false;
        int flareType = getAutoCountermeasureFlareType(ac);
        if (flareType > 0 && ac.canUseFlare() && ac.useFlare(flareType))
            used = true;
        if (ac.canUseChaff() && ac.useChaff())
            used = true;
        if (ac.canUseECMJammer() && ac.useECMJammer((Entity)this))
            used = true;
        if (ac.canUseAPS() && ac.useAPS((Entity)this))
            used = true;
        if (used)
            this.autoCountermeasureCooldown = 30;
    }

    private int getAutoCountermeasureFlareType(MCH_EntityAircraft ac) {
        if (ac == null || !ac.haveFlare() || ac.getAcInfo() == null || ac.getAcInfo().flare == null || ac.getAcInfo().flare.types == null)
            return 0;
        int[] types = ac.getAcInfo().flare.types;
        for (int i = 0; i < types.length; i++) {
            if (types[i] == 10)
                return 10;
        }
        int current = ac.getCurrentFlareType();
        if (current > 0)
            return current;
        return types.length > 0 ? types[0] : 0;
    }

    private boolean hasIncomingGuidedThreat(MCH_EntityAircraft ac) {
        List<MCH_EntityBaseBullet> list = this.worldObj.getEntitiesWithinAABB(MCH_EntityBaseBullet.class, ac.boundingBox.expand(220.0D, 220.0D, 220.0D));
        if (list == null || list.isEmpty())
            return false;
        for (int i = 0; i < list.size(); i++) {
            MCH_EntityBaseBullet bullet = list.get(i);
            if (bullet == null || bullet.isDead || bullet.getInfo() == null || bullet.targetEntity == null)
                continue;
            if (!ac.isMountedEntity(bullet.targetEntity) && !bullet.targetEntity.equals(ac))
                continue;
            MCH_WeaponInfo info = bullet.getInfo();
            if (info.isHeatSeekerMissile || info.isRadarMissile || info.passiveRadar || info.activeRadar || info.laserGuidance || info.isGPSMissile || bullet instanceof MCH_EntityAAMissile || bullet instanceof MCH_EntityATMissile || bullet instanceof MCH_EntityASMissile || bullet instanceof MCH_EntityTvMissile)
                return true;
        }
        return false;
    }

    public boolean canAttackEntity(EntityLivingBase entity, MCH_EntityAircraft ac, MCH_WeaponSet ws) {
        boolean ret = false;
        if (this.targetType == TARGET_MONSTER) {
            ret = (entity != this && !(entity instanceof net.minecraft.entity.monster.EntityEnderman) && !entity.isDead && !isOnSameTeam(entity) && entity.getHealth() > 0.0F && !ac.isMountedEntity((Entity)entity));
        } else if (this.targetType == TARGET_PLAYER) {
            ret = (entity != this && !((EntityPlayer)entity).capabilities.isCreativeMode && !entity.isDead && !getTeamName().isEmpty() && !isOnSameTeam(entity) && entity.getHealth() > 0.0F && !ac.isMountedEntity((Entity)entity));
        } else if (this.targetType == TARGET_ENEMY) {
            if (entity == this || entity.isDead || entity.getHealth() <= 0.0F || ac.isMountedEntity((Entity)entity))
                ret = false;
            else if (entity instanceof EntityPlayer) {
                EntityPlayer player = (EntityPlayer)entity;
                boolean hardMode = this.worldObj.difficultySetting == EnumDifficulty.HARD;
                ret = (!player.capabilities.isCreativeMode || hardMode);
            } else if (isNeutralEntity(entity)) {
                ret = true;
            } else if (isFriendlyGunner(entity)) {
                ret = true;
            }
        }
        MCH_IGuidanceSystem guidanceSystem = ws.getCurrentWeapon().getGuidanceSystem();
        if (ret && guidanceSystem != null) {
            if (guidanceSystem instanceof MCH_EntityGuidanceSystem) {
                MCH_EntityGuidanceSystem gs = (MCH_EntityGuidanceSystem)guidanceSystem;
                ret = gs.canLockEntity((Entity)entity);
            }
        }
        return ret;
    }

    public void shotTarget(MCH_EntityAircraft ac) {
        if (ac.isDestroyed())
            return;
        if (!ac.getGunnerStatus())
            return;
        MCH_WeaponSet ws = ac.getCurrentWeapon((Entity)this);
        if (ws == null || ws.getInfo() == null || ws.getCurrentWeapon() == null)
            return;
        if (ac instanceof MCH_EntityHeli && ac.isPilot((Entity)this) && !this.heliAllowFire)
            return;
        if (ac instanceof MCP_EntityPlane && ac.isPilot((Entity)this) && !this.planeAllowFire)
            return;
        applyInfinityAmmoForGunner(ac, ws);
        MCH_WeaponBase cw = ws.getCurrentWeapon();
        if (this.targetEntity != null && (this.targetEntity.isDead || (this.targetEntity instanceof EntityLivingBase && ((EntityLivingBase)this.targetEntity).getHealth() <= 0.0F)))
            if (this.switchTargetCount > 20)
                this.switchTargetCount = 20;
        Vec3 pos = getGunnerWeaponPos(ac, ws);
        updateTargetForWeapon(ac, ws, pos);
        updateGunnerAheadPreSolve(ac, ws, cw, pos);
        if (this.targetEntity != null) {
            float rotSpeed = 10.0F;
            if (ac.isPilot((Entity)this))
                rotSpeed = (ac.getAcInfo()).cameraRotationSpeed / 10.0F;
            if (ac instanceof MCH_EntityHeli && ac.isPilot((Entity)this) && this.heliState == HELI_STATE_ATTACK)
                rotSpeed = Math.max(rotSpeed, 18.0F);
            boolean railgun = ws.getInfo() != null && ws.getInfo().type != null && ws.getInfo().type.equalsIgnoreCase("railgun");
            if (railgun)
                rotSpeed = Math.max(rotSpeed, 22.0F);
            this.rotationPitch = MathHelper.wrapAngleTo180_float(this.rotationPitch);
            this.rotationYaw = MathHelper.wrapAngleTo180_float(this.rotationYaw);
            double dist = getDistanceToEntity(this.targetEntity);
            double tick = 1.0D;
            if (dist >= 10.0D && (ws.getInfo()).acceleration > 1.0F)
                tick = dist / (ws.getInfo()).acceleration;
            if (this.targetEntity.ridingEntity instanceof MCH_EntitySeat || this.targetEntity.ridingEntity instanceof MCH_EntityAircraft)
                tick -= MCH_Config.HitBoxDelayTick.prmInt;
            double dx = (this.targetEntity.posX - this.targetPrevPosX) * tick;
            double dy = (this.targetEntity.posY - this.targetPrevPosY) * tick + this.targetEntity.height * this.rand.nextDouble();
            double dz = (this.targetEntity.posZ - this.targetPrevPosZ) * tick;
            double d0 = this.targetEntity.posX + dx - pos.xCoord;
            double d1 = this.targetEntity.posY + dy - pos.yCoord;
            double d2 = this.targetEntity.posZ + dz - pos.zCoord;
            double d3 = MathHelper.sqrt_double(d0 * d0 + d2 * d2);
            float yaw = MathHelper.wrapAngleTo180_float((float)(Math.atan2(d2, d0) * 180.0D / Math.PI) - 90.0F);
            float pitch = (float)-(Math.atan2(d1, d3) * 180.0D / Math.PI);
            float shotWindow = railgun ? (rotSpeed * 1.8F) : rotSpeed;
            boolean canFireInWindow = true;
            if (ac instanceof MCP_EntityPlane && ac.isPilot((Entity)this)) {
                shotWindow = Math.max(shotWindow, 16.0F);
                canFireInWindow = isPlaneFireWindowReady((MCP_EntityPlane)ac, this.targetEntity, yaw, pitch);
            }
            if (Math.abs(this.rotationPitch - pitch) < shotWindow && Math.abs(this.rotationYaw - yaw) < shotWindow && canFireInWindow) {
                float r = ac.isPilot((Entity)this) ? 0.1F : 0.5F;
                this.rotationPitch = pitch + (this.rand.nextFloat() - 0.5F) * r - cw.fixRotationPitch;
                this.rotationYaw = yaw + (this.rand.nextFloat() - 0.5F) * r;
                if (!this.waitCooldown || ws.currentHeat <= 0 || (ws.getInfo()).maxHeatCount <= 0) {
                    this.waitCooldown = false;
                    MCH_WeaponParam prm = new MCH_WeaponParam();
                    prm.setPosition(ac.posX, ac.posY, ac.posZ);
                    prm.user = (Entity)this;
                    prm.entity = (Entity)ac;
                    prm.option1 = (cw instanceof mcheli.weapon.MCH_WeaponEntitySeeker) ? this.targetEntity.getEntityId() : 0;
                    prm.option2 = (cw instanceof mcheli.weapon.MCH_WeaponATMissile) ? cw.getCurrentMode() : 0;
                    if (cw instanceof mcheli.weapon.MCH_WeaponASMissile && this.targetEntity != null) {
                        prm.option1 = this.targetEntity.getEntityId();
                    }
                    if (ac.useCurrentWeapon(prm))
                        if ((ws.getInfo()).maxHeatCount > 0 && ws.currentHeat > (ws.getInfo()).maxHeatCount * 4 / 5)
                            this.waitCooldown = true;
                }
            }
            if (Math.abs(pitch - this.rotationPitch) >= rotSpeed)
                this.rotationPitch += (pitch > this.rotationPitch) ? rotSpeed : -rotSpeed;
            if (Math.abs(yaw - this.rotationYaw) >= rotSpeed)
                if (Math.abs(yaw - this.rotationYaw) <= 180.0F) {
                    this.rotationYaw += (yaw > this.rotationYaw) ? rotSpeed : -rotSpeed;
                } else {
                    this.rotationYaw += (yaw > this.rotationYaw) ? -rotSpeed : rotSpeed;
                }
            this.rotationYawHead = this.rotationYaw;
            this.targetPrevPosX = this.targetEntity.posX;
            this.targetPrevPosY = this.targetEntity.posY;
            this.targetPrevPosZ = this.targetEntity.posZ;
        } else {
            this.rotationPitch *= 0.95F;
        }
    }

    private boolean checkPitch(Entity entity, MCH_EntityAircraft ac, Vec3 pos) {
        try {
            double d0 = entity.posX - pos.xCoord;
            double d1 = entity.posY - pos.yCoord;
            double d2 = entity.posZ - pos.zCoord;
            double d3 = MathHelper.sqrt_double(d0 * d0 + d2 * d2);
            float pitch = (float)-(Math.atan2(d1, d3) * 180.0D / Math.PI);
            MCH_AircraftInfo ai = ac.getAcInfo();
            if ((this.targetType == TARGET_AA_AMMO || ac instanceof mcheli.vehicle.MCH_EntityVehicle) && ac.isPilot((Entity)this))
                if (Math.abs(ai.minRotationPitch) + Math.abs(ai.maxRotationPitch) > 0.0F) {
                    if (pitch < ai.minRotationPitch)
                        return false;
                    if (pitch > ai.maxRotationPitch)
                        return false;
                }
            MCH_WeaponBase cw = ac.getCurrentWeapon((Entity)this).getCurrentWeapon();
            if (!(cw instanceof mcheli.weapon.MCH_WeaponEntitySeeker)) {
                MCH_AircraftInfo.Weapon wi = ai.getWeaponById(ac.getCurrentWeaponID((Entity)this));
                if (Math.abs(wi.minPitch) + Math.abs(wi.maxPitch) > 0.0F) {
                    float pitchMargin = isGuidedMissileWeapon(cw, cw.getInfo()) ? 24.0F : 8.0F;
                    if (pitch < wi.minPitch - pitchMargin)
                        return false;
                    if (pitch > wi.maxPitch + pitchMargin)
                        return false;
                }
            }
        } catch (Exception e) {}
        return true;
    }

    public Vec3 getGunnerWeaponPos(MCH_EntityAircraft ac, MCH_WeaponSet ws) {
        MCH_SeatInfo seatInfo = ac.getSeatInfo((Entity)this);
        if ((seatInfo != null && seatInfo.rotSeat) || ac instanceof mcheli.vehicle.MCH_EntityVehicle)
            return ac.calcOnTurretPos((ws.getCurrentWeapon()).position).addVector(ac.posX, ac.posY, ac.posZ);
        return ac.getTransformedPosition((ws.getCurrentWeapon()).position);
    }

    private void updateGunnerAheadPreSolve(MCH_EntityAircraft ac, MCH_WeaponSet ws, MCH_WeaponBase cw, Vec3 shotPos) {
        if (ws == null || cw == null || ws.getInfo() == null) {
            return;
        }
        MCH_WeaponInfo info = ws.getInfo();
        if (!info.ahead || info.spawnBulletInAir) {
            if (cw.airburstDist != 0)
                cw.setAirburstDist(0);
            return;
        }
        if (this.targetEntity == null || this.targetEntity.isDead) {
            if (cw.airburstDist != 0)
                cw.setAirburstDist(0);
            return;
        }
        int interval = Math.max(1, info.aheadSolveIntervalTick);
        if (this.ticksExisted % interval != 0) {
            return;
        }
        double sx = shotPos.xCoord;
        double sy = shotPos.yCoord;
        double sz = shotPos.zCoord;
        double tx = this.targetEntity.posX;
        double ty = this.targetEntity.posY + this.targetEntity.height * 0.5D;
        double tz = this.targetEntity.posZ;
        double tvx = this.targetEntity.posX - this.targetEntity.prevPosX;
        double tvy = this.targetEntity.posY - this.targetEntity.prevPosY;
        double tvz = this.targetEntity.posZ - this.targetEntity.prevPosZ;
        double speed = cw.acceleration;
        if (info.speedDependsAircraft) {
            speed += Math.sqrt(ac.motionX * ac.motionX + ac.motionY * ac.motionY + ac.motionZ * ac.motionZ);
        }
        if (speed <= 1.0E-6D) {
            if (cw.airburstDist != 0)
                cw.setAirburstDist(0);
            return;
        }
        double rx = tx - sx;
        double ry = ty - sy;
        double rz = tz - sz;
        double a = tvx * tvx + tvy * tvy + tvz * tvz - speed * speed;
        double b = 2.0D * (rx * tvx + ry * tvy + rz * tvz);
        double c = rx * rx + ry * ry + rz * rz;
        double t = -1.0D;
        if (Math.abs(a) < 1.0E-6D) {
            if (Math.abs(b) > 1.0E-6D) {
                t = -c / b;
            }
        } else {
            double d = b * b - 4.0D * a * c;
            if (d < 0.0D) {
                if (cw.airburstDist != 0)
                    cw.setAirburstDist(0);
                return;
            }
            double sqrtD = Math.sqrt(d);
            double t1 = (-b - sqrtD) / (2.0D * a);
            double t2 = (-b + sqrtD) / (2.0D * a);
            if (t1 > 0.0D && t2 > 0.0D) {
                t = Math.min(t1, t2);
            } else if (t1 > 0.0D) {
                t = t1;
            } else if (t2 > 0.0D) {
                t = t2;
            }
        }
        if (t <= 0.0D || t > 600.0D) {
            if (cw.airburstDist != 0)
                cw.setAirburstDist(0);
            return;
        }
        double px = tx + tvx * t;
        double py = ty + tvy * t;
        double pz = tz + tvz * t;
        double impactDist = Math.sqrt((px - sx) * (px - sx) + (py - sy) * (py - sy) + (pz - sz) * (pz - sz));
        int triggerDist = (int)Math.floor(impactDist - info.proximityFuseDist);
        if (triggerDist <= 5 || triggerDist >= 3000) {
            triggerDist = 0;
        }
        cw.setAirburstDist(triggerDist);
    }

    private boolean isInAttackable(Entity entity, MCH_EntityAircraft ac, MCH_WeaponSet ws, Vec3 pos) {
        if (ac instanceof mcheli.vehicle.MCH_EntityVehicle)
            return true;
        try {
            MCH_WeaponBase cw = ac.getCurrentWeapon((Entity)this).getCurrentWeapon();
            MCH_WeaponInfo wiInfo = cw != null ? cw.getInfo() : null;
            if (cw instanceof mcheli.weapon.MCH_WeaponEntitySeeker)
                return true;
            MCH_AircraftInfo.Weapon wi = ac.getAcInfo().getWeaponById(ac.getCurrentWeaponID((Entity)this));
            Vec3 v1 = Vec3.createVectorHelper(0.0D, 0.0D, 1.0D);
            float yaw = -ac.getRotYaw() + (wi.maxYaw + wi.minYaw) / 2.0F - wi.defaultYaw;
            v1.rotateAroundY(yaw * 3.1415927F / 180.0F);
            Vec3 v2 = Vec3.createVectorHelper(entity.posX - pos.xCoord, 0.0D, entity.posZ - pos.zCoord).normalize();
            double dot = v1.dotProduct(v2);
            double rad = Math.acos(dot);
            double deg = rad * 180.0D / Math.PI;
            float limit = Math.abs(wi.maxYaw - wi.minYaw) / 2.0F;
            if (isGuidedMissileWeapon(cw, wiInfo)) {
                limit = Math.min(180.0F, limit + 45.0F);
            } else {
                limit = Math.min(180.0F, limit + 18.0F);
            }
            return (deg < limit);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isAATarget(Entity entity) {
        return entity instanceof MCH_EntityRocket || entity instanceof MCH_EntityASMissile || entity instanceof MCH_EntityTvMissile || entity instanceof MCH_EntityATMissile || entity instanceof MCH_EntityBomb || entity instanceof MCH_EntityMarkerRocket;
    }

    private boolean canTrackTargetAltitude(Entity entity) {
        return getHeightAboveGround(entity) > 30.0D;
    }

    private boolean isFriendlyGunner(Entity entity) {
        return entity instanceof MCH_EntityGunner && ((MCH_EntityGunner)entity).targetType == TARGET_MONSTER;
    }

    private boolean isEnemyGunner(Entity entity) {
        return entity instanceof MCH_EntityGunner && ((MCH_EntityGunner)entity).targetType == TARGET_ENEMY;
    }

    private boolean isNeutralEntity(Entity entity) {
        return entity instanceof EntityLivingBase && !(entity instanceof IMob) && !(entity instanceof EntityPlayer) && !(entity instanceof MCH_EntityGunner);
    }

    private boolean isPriorityTarget(EntityLivingBase entity) {
        if (this.targetType == TARGET_MONSTER)
            return isEnemyGunner((Entity)entity);
        if (this.targetType == TARGET_ENEMY)
            return isFriendlyGunner((Entity)entity);
        return false;
    }

    private void updateTargetForWeapon(MCH_EntityAircraft ac, MCH_WeaponSet ws, Vec3 pos) {
        if (this.lastTargetUpdateTick == this.ticksExisted)
            return;
        this.lastTargetUpdateTick = this.ticksExisted;
        if (!((this.targetEntity == null && this.switchTargetCount <= 0) || this.switchTargetCount <= 0))
            return;
        List<? extends Entity> list;
        this.switchTargetCount = 5;
        Entity nextTarget = null;
        boolean planePilot = ac instanceof MCP_EntityPlane && ac.isPilot((Entity)this);
        int planeAirRadius = Math.max(1, MCH_Config.GunnerPlaneSearchRadiusAir.prmInt);
        int planeGroundRadius = Math.max(1, MCH_Config.GunnerPlaneSearchRadiusGround.prmInt);
        if (planePilot) {
            planeGroundRadius = Math.max(planeGroundRadius, (int)(planeGroundRadius * 1.85D));
        }
        int planeAltitudeWindow = Math.max(0, MCH_Config.GunnerPlaneSearchAltitudeWindow.prmInt);
        int universalAirRadius = planeAirRadius;
        int universalAirAltitude = Math.max(planeAltitudeWindow, Math.max(MCH_Config.RangeOfGunner_VsMonster_Vertical.prmInt, MCH_Config.RangeOfGunner_VsPlayer_Vertical.prmInt));
        int planeSearchRadius = Math.max(planeAirRadius, planeGroundRadius);
        if (this.targetType == TARGET_MONSTER) {
            int rh = planePilot ? planeSearchRadius : MCH_Config.RangeOfGunner_VsMonster_Horizontal.prmInt;
            int rv = planePilot ? Math.max(planeAltitudeWindow, MCH_Config.RangeOfGunner_VsMonster_Vertical.prmInt) : MCH_Config.RangeOfGunner_VsMonster_Vertical.prmInt;
            rh = Math.max(rh, universalAirRadius);
            rv = Math.max(rv, universalAirAltitude);
            list = this.worldObj.getEntitiesWithinAABB(EntityLivingBase.class, this.boundingBox.expand(rh, rv, rh));
        } else if (this.targetType == TARGET_PLAYER) {
            int rh = planePilot ? planeSearchRadius : MCH_Config.RangeOfGunner_VsPlayer_Horizontal.prmInt;
            int rv = planePilot ? Math.max(planeAltitudeWindow, MCH_Config.RangeOfGunner_VsPlayer_Vertical.prmInt) : MCH_Config.RangeOfGunner_VsPlayer_Vertical.prmInt;
            rh = Math.max(rh, universalAirRadius);
            rv = Math.max(rv, universalAirAltitude);
            list = this.worldObj.getEntitiesWithinAABB(EntityLivingBase.class, this.boundingBox.expand(rh, rv, rh));
        } else if (this.targetType == TARGET_ENEMY) {
            int rh = planePilot ? planeSearchRadius : Math.max(MCH_Config.RangeOfGunner_VsMonster_Horizontal.prmInt, MCH_Config.RangeOfGunner_VsPlayer_Horizontal.prmInt);
            int rv = planePilot ? Math.max(planeAltitudeWindow, Math.max(MCH_Config.RangeOfGunner_VsMonster_Vertical.prmInt, MCH_Config.RangeOfGunner_VsPlayer_Vertical.prmInt)) : Math.max(MCH_Config.RangeOfGunner_VsMonster_Vertical.prmInt, MCH_Config.RangeOfGunner_VsPlayer_Vertical.prmInt);
            rh = Math.max(rh, universalAirRadius);
            rv = Math.max(rv, universalAirAltitude);
            list = this.worldObj.getEntitiesWithinAABB(EntityLivingBase.class, this.boundingBox.expand(rh, rv, rh));
        } else {
            int rh = Math.max(150, universalAirRadius);
            int rv = Math.max(150, universalAirAltitude);
            list = this.worldObj.getEntitiesWithinAABBExcludingEntity((Entity)this, this.boundingBox.expand(rh, rv, rh));
        }
        boolean priorityChosen = false;
        int planeBestBucket = -1;
        for (int i = 0; i < list.size(); i++) {
            Entity candidate = list.get(i);
            if (this.targetType == TARGET_AA_AMMO) {
                if (!isAATarget(candidate))
                    continue;
                if (this.getDistanceSqToEntity(candidate) > 22500.0D)
                    continue;
                if (!canTrackTargetAltitude(candidate))
                    continue;
                if (!checkPitch(candidate, ac, pos))
                    continue;
                if ((nextTarget == null || this.getDistanceSqToEntity(candidate) < this.getDistanceSqToEntity(nextTarget)) && this.canEntityBeSeen(candidate))
                    if (isInAttackable(candidate, ac, ws, pos)) {
                        nextTarget = candidate;
                        this.switchTargetCount = 60;
                    }
                continue;
            }
            if (!(candidate instanceof EntityLivingBase))
                continue;
            EntityLivingBase entity = (EntityLivingBase)candidate;
            if (this.targetType == TARGET_MONSTER) {
                if (!(entity instanceof IMob) && !isEnemyGunner((Entity)entity))
                    continue;
            } else if (this.targetType == TARGET_PLAYER) {
                if (!(entity instanceof EntityPlayer))
                    continue;
            } else if (this.targetType == TARGET_ENEMY) {
                if (!isFriendlyGunner((Entity)entity) && !(entity instanceof EntityPlayer) && !isNeutralEntity((Entity)entity))
                    continue;
            }
            boolean heliPilot = (ac instanceof MCH_EntityHeli && ac.isPilot((Entity)this));
            boolean priority = isPriorityTarget(entity);
            double distSq = this.getDistanceSqToEntity((Entity)entity);
            boolean airTarget = isPlaneAirTarget(entity);
            double dy = Math.abs(entity.posY - ac.posY);
            if (airTarget) {
                if (distSq > (double)(universalAirRadius * universalAirRadius))
                    continue;
                if ((double)universalAirAltitude > 0.0D && dy > (double)universalAirAltitude)
                    continue;
            }
            if (canAttackEntity(entity, ac, ws))
                if ((heliPilot || planePilot || checkPitch(entity, ac, pos)))
                    if (canEntityBeSeen((Entity)entity))
                        if (planePilot) {
                            if (airTarget) {
                                if (distSq > (double)(planeAirRadius * planeAirRadius))
                                    continue;
                                if ((double)planeAltitudeWindow > 0.0D && dy > (double)planeAltitudeWindow)
                                    continue;
                            } else if (distSq > (double)(planeGroundRadius * planeGroundRadius)) {
                                continue;
                            }
                            int bucket;
                            if (airTarget) {
                                if (distSq <= 14400.0D) {
                                    bucket = 2;
                                    this.switchTargetCount = 24;
                                } else if (isInAttackable(entity, ac, ws, pos)) {
                                    bucket = 1;
                                    this.switchTargetCount = 32;
                                } else {
                                    bucket = 0;
                                    this.switchTargetCount = 44;
                                }
                            } else {
                                bucket = isPlaneGroundHighValueTarget(entity) ? 12 : 10;
                                this.switchTargetCount = 16;
                            }
                            if (priority)
                                bucket += 10;
                            if (bucket > planeBestBucket || (bucket == planeBestBucket && (nextTarget == null || distSq < this.getDistanceSqToEntity(nextTarget)))) {
                                nextTarget = entity;
                                planeBestBucket = bucket;
                                priorityChosen = priority;
                            }
                        } else if (heliPilot) {
                            if ((priority && !priorityChosen) || (priority == priorityChosen && (nextTarget == null || getDistanceToEntity((Entity)entity) < getDistanceToEntity((Entity)nextTarget)))) {
                                nextTarget = entity;
                                priorityChosen = priority;
                                this.switchTargetCount = 30;
                            }
                        } else if (ws.getInfo() != null && ws.getInfo().type != null && ws.getInfo().type.equalsIgnoreCase("railgun")) {
                            if ((priority && !priorityChosen) || (priority == priorityChosen && (nextTarget == null || getDistanceToEntity((Entity)entity) < getDistanceToEntity((Entity)nextTarget)))) {
                                nextTarget = entity;
                                priorityChosen = priority;
                                this.switchTargetCount = 40;
                            }
                        } else if (isInAttackable(entity, ac, ws, pos)) {
                            if ((priority && !priorityChosen) || (priority == priorityChosen && (nextTarget == null || getDistanceToEntity((Entity)entity) < getDistanceToEntity((Entity)nextTarget)))) {
                                nextTarget = entity;
                                priorityChosen = priority;
                                this.switchTargetCount = 60;
                            }
                        }
        }
        if (nextTarget != null && this.targetEntity != nextTarget) {
            this.targetPrevPosX = nextTarget.posX;
            this.targetPrevPosY = nextTarget.posY;
            this.targetPrevPosZ = nextTarget.posZ;
        }
        this.targetEntity = nextTarget;
    }

    private boolean isPlaneAirTarget(EntityLivingBase entity) {
        if (entity.ridingEntity instanceof MCH_EntityAircraft)
            return true;
        if (entity.ridingEntity instanceof MCH_EntitySeat) {
            MCH_EntityAircraft parent = ((MCH_EntitySeat)entity.ridingEntity).getParent();
            if (parent != null)
                return true;
        }
        return getHeightAboveGround((Entity)entity) > 30.0D;
    }

    private boolean isPlaneGroundHighValueTarget(EntityLivingBase entity) {
        if (entity instanceof EntityPlayer)
            return true;
        if (entity instanceof MCH_EntityGunner)
            return true;
        return entity instanceof IMob;
    }

    private void updateTankDrive(MCH_EntityTank tank) {
        if (!tank.isPilot((Entity)this))
            return;
        boolean throttleUp = false;
        boolean throttleDown = false;
        boolean moveLeft = false;
        boolean moveRight = false;
        boolean brake = false;
        double mdx = tank.posX - tank.prevPosX;
        double mdz = tank.posZ - tank.prevPosZ;
        double moveDistSq = mdx * mdx + mdz * mdz;
        if (this.noMoveTurnCooldownTicks > 0)
            this.noMoveTurnCooldownTicks--;
        if (moveDistSq > 0.0009D) {
            this.noMoveTicks++;
        }
        if (tank.isDestroyed() || (!tank.canUseFuel() && !tank.isInfinityFuel((Entity)this, true))) {
            brake = true;
        } else {
            Entity target = this.targetEntity;
            if (target != null && (target.isDead || this.getDistanceSqToEntity(target) > 160000.0D))
                target = null;
            if (target != null) {
                double dx = target.posX - tank.posX;
                double dz = target.posZ - tank.posZ;
                double distSq = dx * dx + dz * dz;
                float targetYaw = MathHelper.wrapAngleTo180_float((float)(Math.atan2(dz, dx) * 180.0D / Math.PI) - 90.0F);
                if (this.combatMoveTicks <= 0) {
                    this.combatMoveTicks = 20 + this.rand.nextInt(60);
                    this.combatYawBias = (this.rand.nextFloat() - 0.5F) * 44.0F;
                    this.combatStrafeDir = this.rand.nextBoolean() ? 1 : -1;
                } else {
                    this.combatMoveTicks--;
                }
                if (this.throttlePulseTicks <= 0) {
                    this.throttlePulseTicks = 8 + this.rand.nextInt(18);
                    this.throttlePulseOn = !this.throttlePulseOn;
                } else {
                    this.throttlePulseTicks--;
                }
                if (distSq < 900.0D) {
                    float orbit = 26.0F * this.combatStrafeDir;
                    targetYaw = MathHelper.wrapAngleTo180_float(targetYaw + orbit);
                } else {
                    targetYaw = MathHelper.wrapAngleTo180_float(targetYaw + this.combatYawBias);
                }
                float yawDiff = MathHelper.wrapAngleTo180_float(targetYaw - tank.getRotYaw());
                float absYaw = Math.abs(yawDiff);
                if (yawDiff > 1.0F) {
                    moveRight = true;
                } else if (yawDiff < -1.0F) {
                    moveLeft = true;
                }
                applyTurnFeedback();
                if (this.turnFeedbackTicks > 0) {
                    if (this.turnFeedbackDir > 0 && this.rand.nextInt(100) < 78) {
                        moveRight = true;
                        if (Math.abs(yawDiff) < 5.0F || this.rand.nextInt(100) < 40)
                            moveLeft = false;
                    } else if (this.turnFeedbackDir < 0 && this.rand.nextInt(100) < 78) {
                        moveLeft = true;
                        if (Math.abs(yawDiff) < 5.0F || this.rand.nextInt(100) < 40)
                            moveRight = false;
                    }
                }
                if (distSq < 100.0D && absYaw < 28.0F) {
                    throttleDown = true;
                } else {
                    throttleUp = this.throttlePulseOn && tank.getCurrentThrottle() < 0.22D && (distSq > 169.0D || absYaw > 12.0F);
                }
                if (moveDistSq < 0.0025D && absYaw > 6.0F) {
                    float step = Math.max(-3.2F, Math.min(3.2F, yawDiff * 0.28F));
                    tank.setRotYaw(tank.getRotYaw() + step);
                }
                if (distSq < 64.0D && absYaw < 24.0F && tank.getCurrentThrottle() > 0.28D) {
                    brake = true;
                }
            } else {
                applyTurnFeedback();
                if (this.wanderTurnTicks <= 0) {
                    this.wanderTurnTicks = 8 + this.rand.nextInt(28);
                    int turn = this.rand.nextInt(7) - 3;
                    this.wanderTurnDir = turn > 0 ? 1 : (turn < 0 ? -1 : 0);
                } else {
                    this.wanderTurnTicks--;
                }
                throttleUp = tank.getCurrentThrottle() < 0.16D && (this.rand.nextInt(100) < 80);
                if (this.wanderTurnDir > 0) {
                    moveRight = true;
                } else if (this.wanderTurnDir < 0) {
                    moveLeft = true;
                }
                if (this.turnFeedbackTicks > 0) {
                    if (this.turnFeedbackDir > 0 && this.rand.nextInt(100) < 65) {
                        moveRight = true;
                        moveLeft = false;
                    } else if (this.turnFeedbackDir < 0 && this.rand.nextInt(100) < 65) {
                        moveLeft = true;
                        moveRight = false;
                    }
                }
                if (moveDistSq < 0.0016D) {
                    if (moveRight && !moveLeft) {
                        tank.setRotYaw(tank.getRotYaw() + 1.8F);
                    } else if (moveLeft && !moveRight) {
                        tank.setRotYaw(tank.getRotYaw() - 1.8F);
                    }
                }
                if (!throttleUp && tank.getCurrentThrottle() > 0.3D)
                    brake = true;
            }
        }
        if (this.noMoveTurnCooldownTicks <= 0 && this.noMoveTicks >= this.noMoveTurnThreshold) {
            int dir = this.rand.nextBoolean() ? 1 : -1;
            startLargeTurn(120.0F + this.rand.nextFloat() * 60.0F, dir);
            this.noMoveTicks = 0;
            this.noMoveTurnThreshold = 300 + this.rand.nextInt(101);
            this.noMoveTurnCooldownTicks = 120;
            this.wanderTurnTicks = 0;
            this.combatMoveTicks = 0;
        }
        boolean blockTooHigh = hasTooHighObstacleAhead(tank);
        if (blockTooHigh) {
            if (this.obstacleTurnDir == 0) {
                this.obstacleTurnDir = this.rand.nextBoolean() ? 1 : -1;
            }
            if (this.largeTurnRemain <= 0.0F) {
                startLargeTurn(120.0F + this.rand.nextFloat() * 60.0F, this.obstacleTurnDir);
            }
        } else if (this.largeTurnRemain <= 0.0F) {
            this.obstacleTurnDir = 0;
        }
        if (this.largeTurnRemain > 0.0F && this.largeTurnDir != 0) {
            float turnStep = getLargeTurnStep(tank);
            float apply = this.largeTurnRemain < turnStep ? this.largeTurnRemain : turnStep;
            tank.setRotYaw(MathHelper.wrapAngleTo180_float(tank.getRotYaw() + apply * this.largeTurnDir));
            this.largeTurnRemain -= apply;
            if (this.largeTurnRemain <= 0.0F) {
                this.largeTurnRemain = 0.0F;
                this.largeTurnDir = 0;
            }
            moveLeft = this.largeTurnDir < 0;
            moveRight = this.largeTurnDir > 0;
            throttleUp = true;
            throttleDown = false;
            brake = false;
        }
        Entity avoidEntity = findEmergencyAvoidEntity(tank, 10.0D);
        if (avoidEntity != null) {
            float avoidYaw = getAvoidYaw((Entity)tank, avoidEntity);
            float avoidDiff = MathHelper.wrapAngleTo180_float(avoidYaw - tank.getRotYaw());
            moveLeft = avoidDiff < -2.0F;
            moveRight = avoidDiff > 2.0F;
            throttleDown = true;
            throttleUp = tank.getCurrentThrottle() < 0.10D;
            brake = this.getDistanceSqToEntity(avoidEntity) < 16.0D;
            if (Math.abs(avoidDiff) > 8.0F && moveDistSq < 0.0064D) {
                float avoidStep = Math.max(-4.2F, Math.min(4.2F, avoidDiff * 0.32F));
                tank.setRotYaw(MathHelper.wrapAngleTo180_float(tank.getRotYaw() + avoidStep));
            }
            this.largeTurnRemain = 0.0F;
            this.largeTurnDir = 0;
        }
        if (moveLeft && !moveRight) {
            this.leftTurnAccum++;
        } else if (moveRight && !moveLeft) {
            this.rightTurnAccum++;
        }
        this.turnSampleTicks++;
        if (this.turnSampleTicks >= 90) {
            int diff = this.leftTurnAccum - this.rightTurnAccum;
            if (diff > 10) {
                this.turnFeedbackDir = 1;
                this.turnFeedbackTicks = 120;
            } else if (diff < -10) {
                this.turnFeedbackDir = -1;
                this.turnFeedbackTicks = 120;
            } else if (this.turnFeedbackTicks <= 0) {
                this.turnFeedbackDir = 0;
            }
            this.turnSampleTicks = 0;
            this.leftTurnAccum = 0;
            this.rightTurnAccum = 0;
        }
        if (brake) {
            throttleUp = false;
        }
        tank.throttleUp = throttleUp;
        tank.throttleDown = throttleDown;
        tank.moveLeft = moveLeft;
        tank.moveRight = moveRight;
        tank.setBrake(brake);
    }

    private void updateHeliDrive(MCH_EntityHeli heli) {
        if (!heli.isPilot((Entity)this))
            return;
        if (heli.isDestroyed() || (!heli.canUseFuel() && !heli.isInfinityFuel((Entity)this, true))) {
            this.heliAllowFire = false;
            heli.throttleUp = false;
            heli.throttleDown = false;
            heli.moveLeft = false;
            heli.moveRight = false;
            return;
        }
        if (this.heliCruiseAltitude < 40.0F || this.heliCruiseAltitude > 60.0F) {
            this.heliCruiseAltitude = 40.0F + this.rand.nextFloat() * 20.0F;
        }
        if (this.heliState == HELI_STATE_CRUISE && this.targetEntity != null) {
            enterHeliState(HELI_STATE_FOCUS, 20 + this.rand.nextInt(11));
        } else if (this.heliState == HELI_STATE_FOCUS) {
            if (this.targetEntity == null) {
                enterHeliState(HELI_STATE_CRUISE, 0);
            } else if (isHeliFocusWindowReady(heli, this.targetEntity) || this.heliStateTicks >= this.heliStateDuration) {
                enterHeliState(HELI_STATE_ATTACK, 100 + this.rand.nextInt(51));
            }
        } else if (this.heliState == HELI_STATE_ATTACK && (this.targetEntity == null || this.heliStateTicks >= this.heliStateDuration)) {
            enterHeliState(HELI_STATE_DISENGAGE, 100 + this.rand.nextInt(51));
        } else if (this.heliState == HELI_STATE_DISENGAGE && this.heliStateTicks >= this.heliStateDuration) {
            this.heliCruiseAltitude = 40.0F + this.rand.nextFloat() * 20.0F;
            enterHeliState(HELI_STATE_CRUISE, 0);
        }
        this.heliStateTicks++;
        double altitude = getHeightAboveGround((Entity)heli);
        boolean throttleUp = false;
        boolean throttleDown = false;
        boolean moveLeft = false;
        boolean moveRight = false;
        float desiredYaw = heli.getRotYaw();
        float desiredPitch = heli.getRotPitch();
        if (this.heliState == HELI_STATE_ATTACK) {
            this.heliAllowFire = true;
            if (this.targetEntity != null) {
                double dx = this.targetEntity.posX - heli.posX;
                double dz = this.targetEntity.posZ - heli.posZ;
                this.heliLastTargetX = this.targetEntity.posX;
                this.heliLastTargetZ = this.targetEntity.posZ;
                desiredYaw = MathHelper.wrapAngleTo180_float((float)(Math.atan2(dz, dx) * 180.0D / Math.PI) - 90.0F);
            }
            desiredPitch = MathHelper.clamp_float(7.0F + (float)((altitude - this.heliCruiseAltitude) * 0.05D), 4.0F, 13.0F);
            if (altitude < this.heliCruiseAltitude - 8.0F) {
                throttleUp = true;
            } else if (altitude > this.heliCruiseAltitude + 6.0F) {
                throttleDown = true;
            }
        } else if (this.heliState == HELI_STATE_FOCUS) {
            this.heliAllowFire = false;
            if (this.targetEntity != null) {
                double dx = this.targetEntity.posX - heli.posX;
                double dz = this.targetEntity.posZ - heli.posZ;
                this.heliLastTargetX = this.targetEntity.posX;
                this.heliLastTargetZ = this.targetEntity.posZ;
                desiredYaw = MathHelper.wrapAngleTo180_float((float)(Math.atan2(dz, dx) * 180.0D / Math.PI) - 90.0F);
            }
            desiredPitch = 6.5F;
            if (altitude < this.heliCruiseAltitude - 10.0F) {
                throttleUp = true;
            } else if (altitude > this.heliCruiseAltitude + 4.0F) {
                throttleDown = true;
            }
            throttleUp = true;
        } else if (this.heliState == HELI_STATE_DISENGAGE) {
            this.heliAllowFire = false;
            if (this.targetEntity != null) {
                this.heliLastTargetX = this.targetEntity.posX;
                this.heliLastTargetZ = this.targetEntity.posZ;
            }
            double awayX = heli.posX - this.heliLastTargetX;
            double awayZ = heli.posZ - this.heliLastTargetZ;
            if (awayX * awayX + awayZ * awayZ > 4.0D) {
                desiredYaw = MathHelper.wrapAngleTo180_float((float)(Math.atan2(awayZ, awayX) * 180.0D / Math.PI) - 90.0F);
            } else {
                desiredYaw = MathHelper.wrapAngleTo180_float(heli.getRotYaw() + (this.combatStrafeDir >= 0 ? 120.0F : -120.0F));
            }
            if (this.heliStateTicks < this.heliStateDuration / 2) {
                desiredPitch = -10.0F;
            } else {
                desiredPitch = 2.0F;
            }
            if (altitude < this.heliCruiseAltitude + 4.0F) {
                throttleUp = true;
            } else if (altitude > this.heliCruiseAltitude + 12.0F) {
                throttleDown = true;
            }
        } else {
            this.heliAllowFire = false;
            if (this.heliPatrolYawTicks <= 0) {
                this.heliPatrolYawTicks = 15 + this.rand.nextInt(31);
                this.heliPatrolYawStep = (this.rand.nextFloat() - 0.5F) * 2.0F;
            } else {
                this.heliPatrolYawTicks--;
            }
            desiredYaw = MathHelper.wrapAngleTo180_float(heli.getRotYaw() + this.heliPatrolYawStep);
            if (this.heliCruiseTurnCooldown > 0) {
                this.heliCruiseTurnCooldown--;
            }
            if (this.targetEntity == null && this.heliCruiseTurnRemain <= 0.0F && this.heliCruiseTurnCooldown <= 0) {
                this.heliCruiseTurnRemain = 60.0F + this.rand.nextFloat() * 30.0F;
                this.heliCruiseTurnDir = this.rand.nextBoolean() ? 1 : -1;
                this.heliCruiseTurnCooldown = 80 + this.rand.nextInt(101);
            }
            if (this.heliCruiseTurnRemain > 0.0F && this.heliCruiseTurnDir != 0) {
                float add = this.heliCruiseTurnRemain > 3.0F ? 3.0F : this.heliCruiseTurnRemain;
                desiredYaw = MathHelper.wrapAngleTo180_float(desiredYaw + add * this.heliCruiseTurnDir);
                this.heliCruiseTurnRemain -= add;
                if (this.heliCruiseTurnRemain <= 0.0F) {
                    this.heliCruiseTurnRemain = 0.0F;
                    this.heliCruiseTurnDir = 0;
                }
            }
            desiredPitch = 4.5F;
            if (altitude < this.heliCruiseAltitude - 2.0F) {
                throttleUp = true;
            } else if (altitude > this.heliCruiseAltitude + 2.0F) {
                throttleDown = true;
            }
        }
        Entity avoidEntity = findEmergencyAvoidEntity(heli, 10.0D);
        if (avoidEntity != null) {
            desiredYaw = getAvoidYaw((Entity)heli, avoidEntity);
            desiredPitch = -4.0F;
            throttleUp = true;
            throttleDown = false;
            this.heliAllowFire = false;
        }
        float yawDiff = MathHelper.wrapAngleTo180_float(desiredYaw - heli.getRotYaw());
        float yawStep = Math.max(-3.6F, Math.min(3.6F, yawDiff * 0.28F));
        heli.setRotYaw(MathHelper.wrapAngleTo180_float(heli.getRotYaw() + yawStep));
        if (yawDiff > 2.0F) {
            moveRight = true;
        } else if (yawDiff < -2.0F) {
            moveLeft = true;
        }
        float pitchDiff = desiredPitch - heli.getRotPitch();
        float pitchStep = Math.max(-1.4F, Math.min(1.4F, pitchDiff * 0.25F));
        heli.setRotPitch(MathHelper.clamp_float(heli.getRotPitch() + pitchStep, -25.0F, 25.0F));
        if (throttleUp && throttleDown) {
            throttleDown = false;
        }
        heli.throttleUp = throttleUp;
        heli.throttleDown = throttleDown;
        heli.moveLeft = moveLeft;
        heli.moveRight = moveRight;
    }

    private void enterHeliState(int state, int duration) {
        this.heliState = state;
        this.heliStateTicks = 0;
        this.heliStateDuration = duration;
        if (state == HELI_STATE_ATTACK) {
            this.heliAllowFire = true;
            this.combatStrafeDir = this.rand.nextBoolean() ? 1 : -1;
        } else {
            this.heliAllowFire = false;
        }
    }

    private void updatePlaneDrive(MCP_EntityPlane plane) {
        if (!plane.isPilot((Entity)this))
            return;
        if (plane.isDestroyed() || (!plane.canUseFuel() && !plane.isInfinityFuel((Entity)this, true))) {
            this.planeAllowFire = false;
            plane.throttleUp = false;
            plane.throttleDown = false;
            plane.moveLeft = false;
            plane.moveRight = false;
            return;
        }
        if (!this.planeOriginInitialized) {
            this.planeOriginInitialized = true;
            this.planeOriginX = plane.posX;
            this.planeOriginZ = plane.posZ;
        }
        if (this.planeState == PLANE_STATE_SEARCH && this.targetEntity != null) {
            enterPlaneStateDynamic(plane, PLANE_STATE_FOCUS, false);
        } else if (this.planeState == PLANE_STATE_FOCUS) {
            if (this.targetEntity == null) {
                enterPlaneStateDynamic(plane, PLANE_STATE_SEARCH, false);
            } else {
                boolean focusGroundTarget = this.targetEntity instanceof EntityLivingBase && !isPlaneAirTarget((EntityLivingBase)this.targetEntity);
                if (focusGroundTarget) {
                    this.planeAimStableTicks = 0;
                    this.planeAimStableNeed = 3 + this.rand.nextInt(3);
                    enterPlaneStateDynamic(plane, PLANE_STATE_ATTACK, true);
                } else {
                    if (isPlaneFocusWindowReady(plane, this.targetEntity)) {
                        this.planeAimStableTicks++;
                    } else {
                        this.planeAimStableTicks = 0;
                    }
                    if (this.planeAimStableTicks >= this.planeAimStableNeed || this.planeStateTicks >= this.planeStateDuration * 2 / 3) {
                        this.planeAimStableTicks = 0;
                        this.planeAimStableNeed = 5 + this.rand.nextInt(4);
                        enterPlaneStateDynamic(plane, PLANE_STATE_ATTACK, true);
                    }
                }
            }
        } else if (this.planeState == PLANE_STATE_ATTACK && (this.targetEntity == null || this.planeStateTicks >= this.planeStateDuration)) {
            enterPlaneStateDynamic(plane, PLANE_STATE_DISENGAGE, false);
        } else if (this.planeState == PLANE_STATE_DISENGAGE && this.planeStateTicks >= this.planeStateDuration) {
            enterPlaneStateDynamic(plane, PLANE_STATE_SEARCH, false);
        } else if (this.planeState == PLANE_STATE_RTB && this.planeStateTicks >= this.planeStateDuration) {
            enterPlaneStateDynamic(plane, PLANE_STATE_SEARCH, false);
        }
        double distXZ = getPlaneHorizontalDistance(plane);
        boolean targetIsAir = this.targetEntity instanceof EntityLivingBase && isPlaneAirTarget((EntityLivingBase)this.targetEntity);
        boolean dogfightEngaged = this.planeState == PLANE_STATE_ATTACK && this.targetEntity != null && targetIsAir;
        if (distXZ > 420.0D) {
            if (!dogfightEngaged) {
                if (this.planeState != PLANE_STATE_RTB) {
                    enterPlaneStateDynamic(plane, PLANE_STATE_RTB, false);
                }
            } else if (this.planeState != PLANE_STATE_RTB && this.planeStateTicks > 50) {
                enterPlaneStateDynamic(plane, PLANE_STATE_RTB, true);
            }
        } else if (this.planeState == PLANE_STATE_RTB && distXZ < 340.0D && this.planeStateTicks > 30) {
            enterPlaneStateDynamic(plane, PLANE_STATE_SEARCH, false);
        }
        this.planeStateTicks++;
        if (this.targetEntity != null) {
            this.planeLastTargetX = this.targetEntity.posX;
            this.planeLastTargetZ = this.targetEntity.posZ;
        }
        boolean throttleUp = false;
        boolean throttleDown = false;
        boolean moveLeft = false;
        boolean moveRight = false;
        float desiredYaw = plane.getRotYaw();
        float desiredPitch = plane.getRotPitch();
        double altitude = getHeightAboveGround((Entity)plane);
        if (this.planeDiveCooldownTicks > 0) {
            this.planeDiveCooldownTicks--;
        }
        if (this.planeDiveActiveTicks > 0) {
            this.planeDiveActiveTicks--;
        }
        boolean groundTarget = this.targetEntity instanceof EntityLivingBase && !isPlaneAirTarget((EntityLivingBase)this.targetEntity);
        if (!dogfightEngaged && altitude > 120.0D && this.planeDiveActiveTicks <= 0 && this.planeDiveCooldownTicks <= 0) {
            this.planeDiveActiveTicks = 80 + this.rand.nextInt(41);
            this.planeDiveCooldownTicks = 120 + this.rand.nextInt(81);
        }
        boolean periodicDive = !dogfightEngaged && altitude > 100.0D && this.planeDiveActiveTicks > 0;
        boolean takeoffPhase = altitude < 80.0D;
        if (this.planeState == PLANE_STATE_ATTACK) {
            this.planeAllowFire = true;
            if (this.targetEntity != null) {
                double dx = this.targetEntity.posX - plane.posX;
                double dz = this.targetEntity.posZ - plane.posZ;
                desiredYaw = MathHelper.wrapAngleTo180_float((float)(Math.atan2(dz, dx) * 180.0D / Math.PI) - 90.0F);
                double d3 = MathHelper.sqrt_double(dx * dx + dz * dz);
                desiredPitch = MathHelper.clamp_float((float)-(Math.atan2(this.targetEntity.posY - plane.posY, d3) * 180.0D / Math.PI), -20.0F, 20.0F);
                boolean airTarget = this.targetEntity instanceof EntityLivingBase && isPlaneAirTarget((EntityLivingBase)this.targetEntity);
                if (!airTarget) {
                    float divePitch = altitude > 180.0D ? 30.0F : (altitude > 120.0D ? 26.0F : 22.0F);
                    desiredPitch = Math.max(desiredPitch, divePitch);
                }
            }
            throttleUp = plane.getCurrentThrottle() < 0.78D;
        } else if (this.planeState == PLANE_STATE_FOCUS) {
            this.planeAllowFire = false;
            desiredPitch = 4.0F;
            if (this.targetEntity != null) {
                double dx = this.targetEntity.posX - plane.posX;
                double dz = this.targetEntity.posZ - plane.posZ;
                desiredYaw = MathHelper.wrapAngleTo180_float((float)(Math.atan2(dz, dx) * 180.0D / Math.PI) - 90.0F);
                if (groundTarget) {
                    desiredPitch = Math.max(desiredPitch, altitude > 140.0D ? 26.0F : 20.0F);
                }
            }
            throttleUp = plane.getCurrentThrottle() < 0.65D;
        } else if (this.planeState == PLANE_STATE_DISENGAGE) {
            this.planeAllowFire = false;
            double awayX = plane.posX - this.planeLastTargetX;
            double awayZ = plane.posZ - this.planeLastTargetZ;
            if (awayX * awayX + awayZ * awayZ > 4.0D) {
                desiredYaw = MathHelper.wrapAngleTo180_float((float)(Math.atan2(awayZ, awayX) * 180.0D / Math.PI) - 90.0F);
            }
            desiredPitch = -2.0F;
            throttleUp = plane.getCurrentThrottle() < 0.85D;
        } else if (this.planeState == PLANE_STATE_RTB) {
            this.planeAllowFire = false;
            desiredYaw = getPlaneYawToOrigin(plane);
            desiredPitch = 2.0F;
            throttleUp = plane.getCurrentThrottle() < 0.82D;
        } else {
            this.planeAllowFire = false;
            if (this.planeStateTicks % 40 == 0) {
                this.combatYawBias = (this.rand.nextFloat() - 0.5F) * 60.0F;
            }
            desiredYaw = MathHelper.wrapAngleTo180_float(desiredYaw + this.combatYawBias * 0.25F);
            desiredPitch = 4.5F;
            throttleUp = plane.getCurrentThrottle() < 0.55D;
        }
        if (takeoffPhase) {
            desiredPitch = Math.min(desiredPitch, -12.0F);
            throttleUp = plane.getCurrentThrottle() < 0.92D;
            throttleDown = false;
        }
        updatePlaneManeuverState(dogfightEngaged);
        if (this.planeManeuver == PLANE_MANEUVER_ORBIT) {
            desiredYaw = MathHelper.wrapAngleTo180_float(desiredYaw + this.planeManeuverDir * 18.0F);
            desiredPitch = MathHelper.clamp_float(desiredPitch + 1.5F, -24.0F, 24.0F);
            throttleUp = throttleUp || plane.getCurrentThrottle() < 0.68D;
        } else if (this.planeManeuver == PLANE_MANEUVER_EXTEND) {
            desiredYaw = MathHelper.wrapAngleTo180_float(desiredYaw + this.planeManeuverDir * 34.0F);
            desiredPitch = MathHelper.clamp_float(Math.min(desiredPitch, -8.0F), -24.0F, 24.0F);
            throttleUp = true;
        } else if (this.planeManeuver == PLANE_MANEUVER_BREAK_TURN) {
            desiredYaw = MathHelper.wrapAngleTo180_float(desiredYaw + this.planeManeuverDir * 95.0F);
            desiredPitch = MathHelper.clamp_float(desiredPitch + 4.0F, -24.0F, 24.0F);
            throttleUp = true;
            this.planeAllowFire = false;
        } else if (this.planeManeuver == PLANE_MANEUVER_YOYO_HIGH) {
            desiredYaw = MathHelper.wrapAngleTo180_float(desiredYaw + this.planeManeuverDir * 32.0F);
            desiredPitch = MathHelper.clamp_float(Math.min(desiredPitch, -12.0F), -24.0F, 24.0F);
            throttleUp = throttleUp || plane.getCurrentThrottle() < 0.75D;
            this.planeAllowFire = false;
        }
        float radialIntent = getPlaneRadialIntent(distXZ, dogfightEngaged);
        if (this.planeState == PLANE_STATE_RTB) {
            radialIntent = -1.0F;
        }
        if (Math.abs(radialIntent) > 0.001F) {
            float yawToOrigin = getPlaneYawToOrigin(plane);
            float radialYaw = radialIntent >= 0.0F ? MathHelper.wrapAngleTo180_float(yawToOrigin + 180.0F) : yawToOrigin;
            float toRadial = MathHelper.wrapAngleTo180_float(radialYaw - desiredYaw);
            float blend = Math.min(0.96F, 0.30F + Math.abs(radialIntent) * 0.65F);
            desiredYaw = MathHelper.wrapAngleTo180_float(desiredYaw + toRadial * blend);
        }
        Entity avoidEntity = findEmergencyAvoidEntity(plane, 10.0D);
        if (avoidEntity != null) {
            desiredYaw = getAvoidYaw((Entity)plane, avoidEntity);
            desiredPitch = -8.0F;
            throttleUp = true;
            throttleDown = false;
            this.planeAllowFire = false;
        }
        if (altitude > 200.0D) {
            desiredPitch = Math.max(desiredPitch, altitude > 280.0D ? 24.0F : 16.0F);
            if (altitude > 280.0D) {
                throttleDown = plane.getCurrentThrottle() > 0.58D;
            }
            throttleUp = false;
        }
        if (groundTarget && altitude > 50.0D) {
            float forcedDivePitch = altitude > 120.0D ? 30.0F : 24.0F;
            desiredPitch = Math.max(desiredPitch, forcedDivePitch);
            throttleUp = true;
            throttleDown = false;
            this.planeAllowFire = true;
        } else if (groundTarget && altitude <= 50.0D) {
            desiredPitch = Math.min(desiredPitch, -10.0F);
            this.planeDiveActiveTicks = 0;
        }
        if (periodicDive) {
            float periodicDivePitch = altitude > 160.0D ? 24.0F : 18.0F;
            desiredPitch = Math.max(desiredPitch, periodicDivePitch);
            throttleUp = true;
            throttleDown = false;
            if (altitude <= 100.0D) {
                this.planeDiveActiveTicks = 0;
            }
        }
        float yawDiff = MathHelper.wrapAngleTo180_float(desiredYaw - plane.getRotYaw());
        float yawStep = Math.max(-3.2F, Math.min(3.2F, yawDiff * 0.24F));
        plane.setRotYaw(MathHelper.wrapAngleTo180_float(plane.getRotYaw() + yawStep));
        if (yawDiff > 2.6F) {
            moveRight = true;
        } else if (yawDiff < -2.6F) {
            moveLeft = true;
        }
        float pitchDiff = desiredPitch - plane.getRotPitch();
        float pitchStep = Math.max(-1.2F, Math.min(1.2F, pitchDiff * 0.22F));
        plane.setRotPitch(MathHelper.clamp_float(plane.getRotPitch() + pitchStep, -30.0F, 30.0F));
        if (throttleUp && throttleDown) {
            throttleDown = false;
        }
        plane.throttleUp = throttleUp;
        plane.throttleDown = throttleDown;
        plane.moveLeft = moveLeft;
        plane.moveRight = moveRight;
    }

    private boolean isPlaneFocusWindowReady(MCP_EntityPlane plane, Entity target) {
        boolean airTarget = target instanceof EntityLivingBase && isPlaneAirTarget((EntityLivingBase)target);
        if (airTarget) {
            return isPlaneAimWindowReady(plane, target, 68.0F, 56.0F);
        }
        return isPlaneAimWindowReady(plane, target, 92.0F, 82.0F);
    }

    private boolean isPlaneFireWindowReady(MCP_EntityPlane plane, Entity target, float targetYaw, float targetPitch) {
        boolean airTarget = target instanceof EntityLivingBase && isPlaneAirTarget((EntityLivingBase)target);
        float yawLimit = airTarget ? 34.0F : 62.0F;
        float pitchLimit = airTarget ? 24.0F : 52.0F;
        float yawErr = Math.abs(MathHelper.wrapAngleTo180_float(targetYaw - plane.getRotYaw()));
        float pitchErr = Math.abs(targetPitch - plane.getRotPitch());
        if (yawErr > yawLimit || pitchErr > pitchLimit)
            return false;
        if (!canEntityBeSeen(target))
            return false;
        int range = airTarget ? Math.max(1, MCH_Config.GunnerPlaneSearchRadiusAir.prmInt) : Math.max(1, MCH_Config.GunnerPlaneSearchRadiusGround.prmInt);
        if (this.getDistanceSqToEntity(target) > (double)(range * range))
            return false;
        int altitudeWindow = Math.max(0, MCH_Config.GunnerPlaneSearchAltitudeWindow.prmInt);
        if (airTarget && altitudeWindow > 0 && Math.abs(target.posY - plane.posY) > (double)altitudeWindow)
            return false;
        return true;
    }

    private boolean isPlaneAimWindowReady(MCP_EntityPlane plane, Entity target, float yawLimit, float pitchLimit) {
        if (target == null)
            return false;
        if (!canEntityBeSeen(target))
            return false;
        double dx = target.posX - plane.posX;
        double dz = target.posZ - plane.posZ;
        double d3 = MathHelper.sqrt_double(dx * dx + dz * dz);
        float desiredYaw = MathHelper.wrapAngleTo180_float((float)(Math.atan2(dz, dx) * 180.0D / Math.PI) - 90.0F);
        float desiredPitch = MathHelper.clamp_float((float)-(Math.atan2(target.posY - plane.posY, d3) * 180.0D / Math.PI), -30.0F, 30.0F);
        float yawErr = Math.abs(MathHelper.wrapAngleTo180_float(desiredYaw - plane.getRotYaw()));
        float pitchErr = Math.abs(desiredPitch - plane.getRotPitch());
        float effectivePitchLimit = pitchLimit;
        if (target.posY < plane.posY) {
            effectivePitchLimit += 18.0F;
        }
        return yawErr <= yawLimit && pitchErr <= effectivePitchLimit;
    }

    private boolean isHeliFocusWindowReady(MCH_EntityHeli heli, Entity target) {
        if (target == null)
            return false;
        if (!canEntityBeSeen(target))
            return false;
        double dx = target.posX - heli.posX;
        double dz = target.posZ - heli.posZ;
        double d3 = MathHelper.sqrt_double(dx * dx + dz * dz);
        float desiredYaw = MathHelper.wrapAngleTo180_float((float)(Math.atan2(dz, dx) * 180.0D / Math.PI) - 90.0F);
        float desiredPitch = MathHelper.clamp_float((float)-(Math.atan2(target.posY - heli.posY, d3) * 180.0D / Math.PI), -35.0F, 35.0F);
        float yawErr = Math.abs(MathHelper.wrapAngleTo180_float(desiredYaw - heli.getRotYaw()));
        float pitchErr = Math.abs(desiredPitch - heli.getRotPitch());
        float pitchLimit = target.posY < heli.posY ? 60.0F : 48.0F;
        return yawErr <= 56.0F && pitchErr <= pitchLimit;
    }

    private void updatePlaneManeuverState(boolean dogfightEngaged) {
        if (this.planeState == PLANE_STATE_RTB) {
            this.planeManeuverTicks++;
            if (this.planeManeuver != PLANE_MANEUVER_STRAIGHT || this.planeManeuverTicks >= this.planeManeuverDuration) {
                enterPlaneManeuver(PLANE_MANEUVER_STRAIGHT, 80, 140, 0);
            }
            return;
        }
        this.planeManeuverTicks++;
        if (this.planeManeuverTicks < this.planeManeuverDuration)
            return;
        int dir = this.rand.nextBoolean() ? 1 : -1;
        if (this.planeState == PLANE_STATE_DISENGAGE) {
            enterPlaneManeuver(PLANE_MANEUVER_EXTEND, 70, 130, dir);
            return;
        }
        if (dogfightEngaged && this.planeState == PLANE_STATE_ATTACK) {
            int r = this.rand.nextInt(100);
            if (r < 36) {
                enterPlaneManeuver(PLANE_MANEUVER_BREAK_TURN, 35, 65, dir);
            } else if (r < 64) {
                enterPlaneManeuver(PLANE_MANEUVER_YOYO_HIGH, 35, 65, dir);
            } else if (r < 82) {
                enterPlaneManeuver(PLANE_MANEUVER_ORBIT, 55, 95, dir);
            } else {
                enterPlaneManeuver(PLANE_MANEUVER_EXTEND, 55, 95, dir);
            }
            return;
        }
        if (this.planeState == PLANE_STATE_FOCUS) {
            enterPlaneManeuver(PLANE_MANEUVER_STRAIGHT, 45, 90, 0);
            return;
        }
        int r = this.rand.nextInt(100);
        if (r < 62) {
            enterPlaneManeuver(PLANE_MANEUVER_STRAIGHT, 60, 120, 0);
        } else if (r < 90) {
            enterPlaneManeuver(PLANE_MANEUVER_ORBIT, 55, 100, dir);
        } else {
            enterPlaneManeuver(PLANE_MANEUVER_EXTEND, 50, 90, dir);
        }
    }

    private void enterPlaneManeuver(int maneuver, int minTick, int maxTick, int dir) {
        this.planeManeuver = maneuver;
        this.planeManeuverTicks = 0;
        int min = Math.max(1, minTick);
        int max = Math.max(min, maxTick);
        this.planeManeuverDuration = min + this.rand.nextInt(max - min + 1);
        if (dir == 0) {
            if (this.planeManeuverDir == 0) {
                this.planeManeuverDir = this.rand.nextBoolean() ? 1 : -1;
            }
        } else {
            this.planeManeuverDir = dir > 0 ? 1 : -1;
        }
    }

    private void enterPlaneStateDynamic(MCP_EntityPlane plane, int state, boolean dogfightEngaged) {
        int min;
        int max;
        if (state == PLANE_STATE_SEARCH) {
            min = MCH_Config.GunnerPlaneStateSearchMin.prmInt;
            max = MCH_Config.GunnerPlaneStateSearchMax.prmInt;
            if (isPlaneRecentlyThreatened(dogfightEngaged)) {
                min = Math.min(min, 20);
                max = Math.min(max, 60);
            } else if (this.targetEntity == null) {
                min = Math.max(min, 42);
                max = Math.max(max, 92);
            }
        } else if (state == PLANE_STATE_FOCUS) {
            min = MCH_Config.GunnerPlaneStateFocusMin.prmInt;
            max = MCH_Config.GunnerPlaneStateFocusMax.prmInt;
            min = Math.min(min, 18);
            max = Math.min(max, 44);
            if (isPlaneRecentlyThreatened(dogfightEngaged)) {
                max = Math.min(max, 28);
            }
        } else if (state == PLANE_STATE_ATTACK) {
            min = MCH_Config.GunnerPlaneStateAttackMin.prmInt;
            max = MCH_Config.GunnerPlaneStateAttackMax.prmInt;
            boolean airTarget = this.targetEntity instanceof EntityLivingBase && isPlaneAirTarget((EntityLivingBase)this.targetEntity);
            double distXZ = getPlaneHorizontalDistance(plane);
            if (airTarget) {
                min = Math.max(min, 110);
                max = Math.max(max, 220);
            } else {
                min = Math.max(min, 190);
                max = Math.max(max, 340);
            }
            if (distXZ > 520.0D) {
                min = Math.min(min, 60);
                max = Math.min(max, 120);
            }
        } else if (state == PLANE_STATE_DISENGAGE) {
            min = MCH_Config.GunnerPlaneStateDisengageMin.prmInt;
            max = MCH_Config.GunnerPlaneStateDisengageMax.prmInt;
            if (isPlaneLowSustain()) {
                min = Math.max(min, 160);
                max = Math.max(max, 300);
            } else if (dogfightEngaged || isPlaneRecentlyThreatened(true)) {
                min = Math.max(min, 120);
            }
        } else if (state == PLANE_STATE_RTB) {
            min = dogfightEngaged ? 120 : 120;
            max = dogfightEngaged ? 220 : 280;
        } else {
            min = 40;
            max = 80;
        }
        enterPlaneState(state, min, max);
    }

    private boolean isPlaneRecentlyThreatened(boolean dogfightEngaged) {
        return dogfightEngaged || this.hurtTime > 0 || this.hurtResistantTime > 0;
    }

    private boolean isPlaneLowSustain() {
        float maxHealth = getMaxHealth();
        if (maxHealth > 0.0F && getHealth() / maxHealth <= 0.35F)
            return true;
        return this.waitCooldown;
    }

    private void enterPlaneState(int state, int minTick, int maxTick) {
        this.planeState = state;
        this.planeStateTicks = 0;
        int min = Math.max(1, minTick);
        int max = Math.max(min, maxTick);
        this.planeStateDuration = min + this.rand.nextInt(max - min + 1);
        this.planeAllowFire = state == PLANE_STATE_ATTACK;
        if (state == PLANE_STATE_FOCUS) {
            this.planeAimStableTicks = 0;
            this.planeAimStableNeed = 5 + this.rand.nextInt(4);
        } else {
            this.planeAimStableTicks = 0;
        }
        this.planeManeuverTicks = this.planeManeuverDuration;
    }

    private double getPlaneHorizontalDistance(MCP_EntityPlane plane) {
        double dx = plane.posX - this.planeOriginX;
        double dz = plane.posZ - this.planeOriginZ;
        return MathHelper.sqrt_double(dx * dx + dz * dz);
    }

    private float getPlaneYawToOrigin(MCP_EntityPlane plane) {
        double dx = this.planeOriginX - plane.posX;
        double dz = this.planeOriginZ - plane.posZ;
        return MathHelper.wrapAngleTo180_float((float)(Math.atan2(dz, dx) * 180.0D / Math.PI) - 90.0F);
    }

    private float getPlaneRadialIntent(double distXZ, boolean dogfightEngaged) {
        if (distXZ <= 50.0D)
            return 1.0F;
        if (distXZ <= 120.0D)
            return 0.55F;
        if (distXZ <= 220.0D)
            return -0.40F;
        if (distXZ <= 320.0D)
            return -0.75F;
        if (distXZ <= 420.0D)
            return -1.00F;
        return dogfightEngaged ? -0.88F : -1.0F;
    }

    private void updateWeaponRotation(MCH_EntityAircraft ac) {
        boolean hasContextTarget = this.targetEntity instanceof EntityLivingBase && !this.targetEntity.isDead;
        boolean airContext = hasContextTarget && isPlaneAirTarget((EntityLivingBase)this.targetEntity);
        int weaponId = ac.getCurrentWeaponID((Entity)this);
        if (weaponId < 0) {
            this.weaponRotateWeaponId = -1;
            this.weaponRotateTicks = 0;
            return;
        }
        if (this.weaponRotateWeaponId != weaponId) {
            this.weaponRotateWeaponId = weaponId;
            this.weaponRotateTicks = 0;
            this.weaponRotateThreshold = hasContextTarget ? (airContext ? (30 + this.rand.nextInt(21)) : (42 + this.rand.nextInt(25))) : (140 + this.rand.nextInt(81));
            return;
        }
        this.weaponRotateTicks++;
        if (this.weaponRotateTicks < this.weaponRotateThreshold) {
            return;
        }
        int nextId = hasContextTarget ? selectWeightedWeaponId(ac, airContext) : ac.getNextWeaponID((Entity)this, 1);
        if (nextId >= 0 && nextId != weaponId) {
            ac.switchWeapon((Entity)this, nextId);
        }
        this.weaponRotateTicks = 0;
        this.weaponRotateThreshold = hasContextTarget ? (airContext ? (30 + this.rand.nextInt(21)) : (42 + this.rand.nextInt(25))) : (140 + this.rand.nextInt(81));
        this.weaponRotateWeaponId = ac.getCurrentWeaponID((Entity)this);
    }

    private int selectWeightedWeaponId(MCH_EntityAircraft ac, boolean airContext) {
        int sid = ac.getSeatIdByEntity((Entity)this);
        if (sid < 0)
            return ac.getCurrentWeaponID((Entity)this);
        int weaponNum = ac.getWeaponNum();
        int[] candidateIds = new int[weaponNum];
        int[] candidateWeights = new int[weaponNum];
        int candidateCount = 0;
        int totalWeight = 0;
        int currentId = ac.getCurrentWeaponID((Entity)this);
        for (int id = 0; id < weaponNum; id++) {
            if (!isWeaponSelectableForSeat(ac, sid, id))
                continue;
            MCH_WeaponInfo wi = ac.getWeaponInfoById(id);
            String type = getWeaponType(wi);
            if (!isWeaponTypeAllowedForContext(type, airContext))
                continue;
            int weight = getWeaponContextWeight(type, airContext);
            if (id == currentId)
                weight += 6;
            if (weight <= 0)
                continue;
            candidateIds[candidateCount] = id;
            candidateWeights[candidateCount] = weight;
            candidateCount++;
            totalWeight += weight;
        }
        if (candidateCount <= 0 || totalWeight <= 0)
            return ac.getNextWeaponID((Entity)this, 1);
        int roll = this.rand.nextInt(totalWeight);
        int sum = 0;
        for (int i = 0; i < candidateCount; i++) {
            sum += candidateWeights[i];
            if (roll < sum)
                return candidateIds[i];
        }
        return candidateIds[candidateCount - 1];
    }

    private boolean isWeaponSelectableForSeat(MCH_EntityAircraft ac, int sid, int weaponId) {
        MCH_AircraftInfo acInfo = ac.getAcInfo();
        if (acInfo == null)
            return false;
        MCH_AircraftInfo.Weapon w = acInfo.getWeaponById(weaponId);
        MCH_WeaponInfo wi = ac.getWeaponInfoById(weaponId);
        if (w == null || wi == null)
            return false;
        int wpsid = ac.getWeaponSeatID(wi, w);
        if (wpsid >= ac.getSeatNum() + 2)
            return false;
        if (wpsid == sid)
            return true;
        return sid == 0 && w.canUsePilot && !(ac.getEntityBySeatId(wpsid) instanceof EntityPlayer) && !(ac.getEntityBySeatId(wpsid) instanceof MCH_EntityGunner);
    }

    private String getWeaponType(MCH_WeaponInfo wi) {
        if (wi == null || wi.type == null)
            return "";
        return wi.type.toLowerCase();
    }

    private boolean isWeaponTypeAllowedForContext(String type, boolean airContext) {
        if (airContext)
            return type.equals("machinegun1") || type.equals("machinegun2") || type.equals("railgun") || type.equals("rocket") || type.equals("aamissile");
        return type.equals("machinegun1") || type.equals("machinegun2") || type.equals("railgun") || type.equals("rocket") || type.equals("bomb") || type.equals("atmissile") || type.equals("asmissile") || type.equals("tvmissile");
    }

    private int getWeaponContextWeight(String type, boolean airContext) {
        if (airContext) {
            if (type.equals("aamissile"))
                return 100;
            if (type.equals("railgun"))
                return 45;
            if (type.equals("machinegun1") || type.equals("machinegun2"))
                return 35;
            if (type.equals("rocket"))
                return 15;
            return 0;
        }
        if (type.equals("atmissile") || type.equals("asmissile") || type.equals("tvmissile"))
            return 90;
        if (type.equals("rocket"))
            return 60;
        if (type.equals("bomb"))
            return 40;
        if (type.equals("machinegun1") || type.equals("machinegun2") || type.equals("railgun"))
            return 25;
        return 0;
    }

    private boolean isGuidedMissileWeapon(MCH_WeaponBase cw, MCH_WeaponInfo wi) {
        if (cw instanceof MCH_WeaponEntitySeeker || cw instanceof MCH_WeaponTvMissile)
            return true;
        String type = getWeaponType(wi);
        return type.equals("aamissile") || type.equals("atmissile") || type.equals("asmissile") || type.equals("tvmissile");
    }

    private Entity findEmergencyAvoidEntity(MCH_EntityAircraft ac, double range) {
        double rangeSq = range * range;
        Entity nearest = null;
        double nearestSq = rangeSq;
        List<Entity> list = this.worldObj.getEntitiesWithinAABBExcludingEntity((Entity)ac, ac.boundingBox.expand(range, 4.0D, range));
        for (int i = 0; i < list.size(); i++) {
            Entity e = list.get(i);
            if (e == null || e.isDead || e == this || e == ac || ac.isMountedEntity(e))
                continue;
            boolean needAvoid = false;
            if (e instanceof MCH_EntityAircraft) {
                needAvoid = true;
            } else if (this.targetType == TARGET_MONSTER) {
                if (e instanceof EntityPlayer) {
                    needAvoid = true;
                } else if (e instanceof EntityLivingBase && !(e instanceof IMob) && !(e instanceof MCH_EntityGunner)) {
                    needAvoid = true;
                }
            } else if (this.targetType == TARGET_ENEMY) {
                if (e instanceof IMob) {
                    needAvoid = true;
                }
            }
            if (!needAvoid)
                continue;
            double dx = e.posX - ac.posX;
            double dz = e.posZ - ac.posZ;
            double distSq = dx * dx + dz * dz;
            if (distSq < nearestSq) {
                nearestSq = distSq;
                nearest = e;
            }
        }
        return nearest;
    }

    private float getAvoidYaw(Entity self, Entity threat) {
        double awayX = self.posX - threat.posX;
        double awayZ = self.posZ - threat.posZ;
        if (awayX * awayX + awayZ * awayZ < 0.01D) {
            return MathHelper.wrapAngleTo180_float(self.rotationYaw + (this.rand.nextBoolean() ? 90.0F : -90.0F));
        }
        return MathHelper.wrapAngleTo180_float((float)(Math.atan2(awayZ, awayX) * 180.0D / Math.PI) - 90.0F);
    }

    private boolean hasTooHighObstacleAhead(MCH_EntityTank tank) {
        double yawRad = Math.toRadians(tank.getRotYaw());
        double dirX = -Math.sin(yawRad);
        double dirZ = Math.cos(yawRad);
        double sideX = Math.cos(yawRad);
        double sideZ = Math.sin(yawRad);
        int baseY = MathHelper.floor_double(tank.boundingBox.minY + 0.01D);
        int step = Math.max(1, MathHelper.ceiling_float_int(tank.stepHeight + 0.01F));
        for (double dist = 2.0D; dist <= 5.0D; dist += 1.0D) {
            for (double side = -0.9D; side <= 0.9D; side += 0.9D) {
                int x = MathHelper.floor_double(tank.posX + dirX * dist + sideX * side);
                int z = MathHelper.floor_double(tank.posZ + dirZ * dist + sideZ * side);
                for (int h = 0; h <= step + 2; h++) {
                    Block b = W_WorldFunc.getBlock(this.worldObj, x, baseY + h, z);
                    if (b != null && b.canCollideCheck(0, true) && !this.worldObj.isAirBlock(x, baseY + h, z)) {
                        return h > step;
                    }
                }
            }
        }
        return false;
    }

    private void startLargeTurn(float angle, int dir) {
        this.largeTurnRemain = Math.max(0.0F, angle);
        this.largeTurnDir = dir >= 0 ? 1 : -1;
    }

    private float getLargeTurnStep(MCH_EntityTank tank) {
        MCH_AircraftInfo info = tank.getAcInfo();
        if (info == null) {
            return 2.0F;
        }
        float base = Math.max(0.6F, info.mobilityYawOnGround);
        float pitchFactor = info.onGroundPitchFactor > 0.0F ? info.onGroundPitchFactor : 1.0F;
        float rollFactor = info.onGroundRollFactor > 0.0F ? info.onGroundRollFactor : 1.0F;
        float groundFactor = MathHelper.clamp_float((pitchFactor + rollFactor) * 0.5F, 0.3F, 1.5F);
        float pivotFactor = info.pivotTurnThrottle <= 0.0F ? 1.0F : MathHelper.clamp_float(1.0F - info.pivotTurnThrottle * 0.5F, 0.5F, 1.0F);
        float throttle = (float)MathHelper.clamp_double(tank.getCurrentThrottle(), 0.0D, 1.0D);
        float throttleFactor = MathHelper.clamp_float(1.0F - throttle * 0.5F, 0.45F, 1.0F);
        return MathHelper.clamp_float(base * groundFactor * pivotFactor * throttleFactor, 0.6F, 6.0F);
    }

    private void applyTurnFeedback() {
        if (this.turnFeedbackTicks > 0) {
            this.turnFeedbackTicks--;
            if (this.turnFeedbackTicks <= 0) {
                this.turnFeedbackDir = 0;
            }
        }
    }

    private void applyInfinityAmmoForGunner(MCH_EntityAircraft ac, MCH_WeaponSet ws) {
        if (!ac.isInfinityAmmo((Entity)this))
            return;
        int ammoMax = ws.getAmmoNumMax();
        if (ammoMax <= 0)
            return;
        if (ws.getRestAllAmmoNum() < ammoMax)
            ws.setRestAllAmmoNum(ammoMax);
        if (ws.getAmmoNum() <= 0 && ws.getRestAllAmmoNum() > 0)
            ws.reloadMag();
    }

    private double getHeightAboveGround(Entity entity) {
        int x = MathHelper.floor_double(entity.posX);
        int z = MathHelper.floor_double(entity.posZ);
        int startY = MathHelper.floor_double(entity.posY);
        for (int y = startY; y >= 0; y--) {
            if (!this.worldObj.isAirBlock(x, y, z)) {
                if (W_WorldFunc.getBlock(this.worldObj, x, y, z).canCollideCheck(0, true))
                    return entity.posY - y;
            }
        }
        return entity.posY;
    }

    public MCH_EntityAircraft getAc() {
        if (this.ridingEntity == null)
            return null;
        return (this.ridingEntity instanceof MCH_EntitySeat) ? ((MCH_EntitySeat)this.ridingEntity).getParent() : ((this.ridingEntity instanceof MCH_EntityAircraft) ? (MCH_EntityAircraft)this.ridingEntity : null);
    }

    public void writeEntityToNBT(NBTTagCompound nbt) {
        super.writeEntityToNBT(nbt);
        nbt.setBoolean("Creative", this.isCreative);
        nbt.setString("OwnerUUID", this.ownerUUID);
        nbt.setString("TeamName", getTeamName());
        nbt.setInteger("TargetType", getTargetType());
    }

    public void readEntityFromNBT(NBTTagCompound nbt) {
        super.readEntityFromNBT(nbt);
        this.isCreative = nbt.getBoolean("Creative");
        this.ownerUUID = nbt.getString("OwnerUUID");
        setTeamName(nbt.getString("TeamName"));
        setTargetType(nbt.getInteger("TargetType"));
    }

    public void travelToDimension(int dim) {}

    public void setDead() {
        if (!this.worldObj.isRemote && !this.isDead && !this.isCreative)
            if (this.targetType == TARGET_MONSTER) {
                dropItem((Item)MCH_MOD.itemSpawnGunnerVsMonster, 1);
            } else if (this.targetType == TARGET_AA_AMMO) {
                dropItem((Item)MCH_MOD.itemSpawnGunnerAA, 1);
            } else if (this.targetType == TARGET_ENEMY) {
                dropItem((Item)MCH_MOD.itemSpawnGunnerEnemy, 1);
            } else {
                dropItem((Item)MCH_MOD.itemSpawnGunnerVsPlayer, 1);
            }
        super.setDead();
        MCH_Lib.DbgLog(this.worldObj, "MCH_EntityGunner.setDead type=%d :" + toString(), new Object[] { Integer.valueOf(this.targetType) });
    }

    public boolean attackEntityFrom(DamageSource ds, float p_70097_2_) {
        if (ds == DamageSource.outOfWorld)
            setDead();
        return super.attackEntityFrom(ds, p_70097_2_);
    }

    public ItemStack getHeldItem() {
        return null;
    }

    public ItemStack getEquipmentInSlot(int p_71124_1_) {
        return null;
    }

    public void setCurrentItemOrArmor(int p_70062_1_, ItemStack p_70062_2_) {}

    public ItemStack[] getLastActiveItems() {
        return new ItemStack[0];
    }

    public ItemStack[] getInventory() {
        return new ItemStack[0];
    }
}
