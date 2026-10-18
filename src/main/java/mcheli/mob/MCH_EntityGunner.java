package mcheli.mob;

import mcheli.MCH_MOD;
import mcheli.MCH_Lib;
import mcheli.aircraft.MCH_AircraftInfo;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.aircraft.MCH_EntitySeat;
import mcheli.helicopter.MCH_EntityHeli;
import mcheli.plane.MCP_EntityPlane;
import mcheli.vehicle.MCH_EntityVehicle;
import mcheli.weapon.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.DamageSource;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import java.util.List;

/** A mounted, server-owned AA operator. Registry and legacy NBT identifiers are retained. */
public class MCH_EntityGunner extends EntityLivingBase {
    public static final int TARGET_MONSTER = 0;
    public static final int TARGET_PLAYER = 1;
    public static final int TARGET_AA_AMMO = 2;
    public static final int TARGET_ENEMY = 3;
    public boolean isCreative;
    public String ownerUUID = "";
    public int targetType;
    public Entity targetEntity;
    private String profileName = "";
    private final MCH_GunnerInfo defaults = new MCH_GunnerInfo("aa_builtin");
    private int unmountedTicks;
    private int nextScan;
    private int reactionRemaining;
    private int mountedId = -1;
    private int weaponId = -1;
    private float yawError;
    private float pitchError;
    private int burstTicks;
    private int restTicks;
    private String status = "idle";
    private int scannedMissiles;
    private int interceptableMissiles;
    private int eligibleTargets;
    private int rejectedTeam;
    private int rejectedSight;
    private int rejectedTraverse;
    private int rejectedIntercept;
    private float fireYawError = Float.NaN;
    private float firePitchError = Float.NaN;
    private float requestedYaw = Float.NaN;
    private float requestedPitch = Float.NaN;
    private float aimYaw = Float.NaN;
    private float aimPitch = Float.NaN;
    private int rejectedTargetId = -1;
    private String rejectedTargetReason = "none";

    public MCH_EntityGunner(World world) { super(world); }

    public MCH_EntityGunner(World world, double x, double y, double z) {
        this(world);
        setPosition(x, y, z);
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        getDataWatcher().addObject(17, "");
        getDataWatcher().addObject(18, Integer.valueOf(TARGET_MONSTER));
    }

    public String getTeamName() { return getDataWatcher().getWatchableObjectString(17); }
    public void setTeamName(String name) { getDataWatcher().updateObject(17, name == null ? "" : name); }
    public int getTargetType() { return getDataWatcher().getWatchableObjectInt(18); }
    public void setTargetType(int type) {
        targetType = MathHelper.clamp_int(type, TARGET_MONSTER, TARGET_ENEMY);
        getDataWatcher().updateObject(18, Integer.valueOf(targetType));
    }

    @Override
    public Team getTeam() { return worldObj.getScoreboard().getTeam(getTeamName()); }

    public IChatComponent getFormattedCommandSenderName() {
        return new ChatComponentText(getTeamName() + " AA Gunner");
    }

    public void applyProfile(MCH_GunnerInfo info) { profileName = info == null ? "" : info.name; }

    private MCH_GunnerInfo profile() {
        MCH_GunnerInfo info = MCH_GunnerInfoManager.get(profileName);
        if (info == null) info = MCH_GunnerInfoManager.get("gunner_aa_default");
        if (info == null) info = defaults;
        MCH_EntityAircraft ac = getAc();
        MCH_WeaponSet ws = ac == null ? null : ac.getCurrentWeapon(this);
        if (ws != null && ws.getInfo() != null) {
            // Resolve one tuning profile for the current gun; placement/team identity stays on the base profile.
            MCH_GunnerInfo weaponInfo = MCH_GunnerInfoManager.get(info.weaponProfiles.get(ws.getInfo().name));
            if (weaponInfo != null && weaponInfo.targetType == TARGET_AA_AMMO) return weaponInfo;
        }
        return info;
    }

    public boolean assignTeam(EntityPlayer player, MCH_GunnerInfo info) {
        String team = info != null && "fixed".equalsIgnoreCase(info.teamMode)
            ? MCH_GunnerTeams.fixedTeam(worldObj, info.fixedTeamId)
            : MCH_GunnerTeams.teamOf(worldObj, player);
        setTeamName(team);
        return MCH_GunnerTeams.validTeam(worldObj, team);
    }

    public static boolean canOperate(MCH_EntityAircraft ac) {
        // The first replacement operator controls CIWS; it must never drive a tank or aircraft.
        return ac instanceof MCH_EntityVehicle && ac.getAcInfo() != null && !ac.isDead && !ac.isDestroyed();
    }

    public static boolean canMount(Entity mount) {
        MCH_EntityAircraft ac = mount instanceof MCH_EntityAircraft ? (MCH_EntityAircraft) mount
            : mount instanceof MCH_EntitySeat ? ((MCH_EntitySeat) mount).getParent() : null;
        if (!canOperate(ac)) return false;
        int seatId = mount instanceof MCH_EntitySeat ? ((MCH_EntitySeat) mount).seatID + 1 : 0;
        for (int id = 0; id < ac.getWeaponNum(); ++id) {
            MCH_AircraftInfo.Weapon gun = ac.getAcInfo().getWeaponById(id);
            MCH_WeaponInfo weapon = ac.getWeaponInfoById(id);
            if (gun == null || weapon == null || !gun.turret
                || (gun.minYaw == 0.0F && gun.maxYaw == 0.0F)
                || (!"machinegun1".equalsIgnoreCase(weapon.type) && !"machinegun2".equalsIgnoreCase(weapon.type))) continue;
            Entity operator = ac.getEntityBySeatId(gun.seatID);
            if (gun.seatID == seatId || (seatId == 0 && gun.canUsePilot
                && !(operator instanceof EntityPlayer) && !(operator instanceof MCH_EntityGunner))) return true;
        }
        return false;
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        targetType = getTargetType();
        if (worldObj.isRemote || isDead) return;
        updateAAGunner();
        if (profile().debugGunner && ticksExisted % 40 == 0) {
            MCH_EntityAircraft ac = getAc();
            MCH_WeaponSet ws = ac == null ? null : ac.getCurrentWeapon(this);
            MCH_Lib.Log(worldObj, "[CIWS] gunner=%d team=%s state=%s enabled=%s seat=%d weapon=%d target=%d missiles=%d interceptable=%d eligible=%d rejected(team/sight/traverse/intercept)=%d/%d/%d/%d aimError(yaw/pitch)=%.2f/%.2f ammo=%d heat=%d",
                getEntityId(), getTeamName(), status,
                ac != null && ac.getGunnerStatus(), ac == null ? -1 : ac.getSeatIdByEntity(this),
                ac == null ? -1 : ac.getCurrentWeaponID(this),
                targetEntity == null ? -1 : targetEntity.getEntityId(),
                scannedMissiles, interceptableMissiles, eligibleTargets,
                rejectedTeam, rejectedSight, rejectedTraverse, rejectedIntercept, fireYawError, firePitchError,
                ws == null ? -1 : ws.getAmmoNum(), ws == null ? -1 : ws.currentHeat);
            if (ac != null && ac.getAcInfo() != null && ws != null && ws.getInfo() != null && ws.getCurrentWeapon() != null) {
                MCH_AircraftInfo.Weapon mount = ac.getAcInfo().getWeaponById(ac.getCurrentWeaponID(this));
                if (mount != null) MCH_Lib.Log(worldObj,
                    "[CIWS aim] gunner=%d profile=%s weapon=%s lookSpeed/response=%.2f/%.2f look=%.2f/%.2f requested=%.2f/%.2f barrel=%.2f/%.2f aim=%.2f/%.2f body=%.2f/%.2f mountYaw=%.2f..%.2f mountPitch=%.2f..%.2f rejectedTarget=%d reason=%s",
                    getEntityId(), profile().name, ws.getInfo().name, profile().lookSpeed, profile().trackingResponse,
                    rotationYaw, rotationPitch, requestedYaw, requestedPitch,
                    ac.getRotYaw() + ws.rotationYaw + ws.getCurrentWeapon().fixRotationYaw,
                    ac.getRotPitch() + ws.rotationPitch + ws.getCurrentWeapon().fixRotationPitch,
                    aimYaw, aimPitch, ac.getRotYaw(), ac.getRotPitch(), mount.minYaw, mount.maxYaw,
                    mount.minPitch, mount.maxPitch, rejectedTargetId, rejectedTargetReason);
            }
        }
    }

    private void updateAAGunner() {
        status = "searching";
        fireYawError = firePitchError = Float.NaN;
        requestedYaw = requestedPitch = aimYaw = aimPitch = Float.NaN;
        MCH_EntityAircraft ac = getAc();
        if (ac == null || ridingEntity == null || ridingEntity.isDead) {
            stopGunner("unmounted");
            if (++unmountedTicks > 100) setDead();
            return;
        }
        unmountedTicks = 0;
        // Old non-AA entities remain removable, but their broken combat/navigation AI is retired.
        if (targetType != TARGET_AA_AMMO) { stopGunner("inactive: non-AA type"); return; }
        if (!canMount(ridingEntity)) { stopGunner("inactive: no compatible turret control"); return; }
        if (!ac.getGunnerStatus()) { stopGunner("inactive: GunnerStatus disabled"); return; }
        if (!MCH_GunnerTeams.validTeam(worldObj, getTeamName())) { stopGunner("inactive: missing or invalid team"); return; }
        MCH_WeaponSet ws = ac.getCurrentWeapon(this);
        if (ws == null || ws.getCurrentWeapon() == null || ws.getInfo() == null) {
            stopGunner("no selected weapon");
            return;
        }
        String type = ws.getInfo().type;
        if (!"machinegun1".equalsIgnoreCase(type) && !"machinegun2".equalsIgnoreCase(type)) {
            stopGunner("inactive: selected weapon is not a CIWS machine gun");
            return;
        }
        MCH_GunnerInfo info = profile();
        Vec3 muzzle = getGunnerWeaponPos(ac, ws);
        if (mountedId != ridingEntity.getEntityId() || weaponId != ac.getCurrentWeaponID(this)) {
            mountedId = ridingEntity.getEntityId();
            weaponId = ac.getCurrentWeaponID(this);
            setTarget(null, info);
            nextScan = 0;
        }
        if (targetEntity != null && rejectTarget(targetEntity, ac, ws, muzzle, info) != null) setTarget(null, info);
        if (--nextScan <= 0) {
            nextScan = info.scanInterval;
            Entity candidate = selectTarget(ac, ws, muzzle, info);
            if (candidate != targetEntity) setTarget(candidate, info);
        }
        if (targetEntity == null) return;
        if (reactionRemaining > 0) { status = "reaction delay"; --reactionRemaining; return; }
        aimAndFire(ac, ws, muzzle, info);
    }

    private void stopGunner(String reason) {
        status = reason;
        targetEntity = null;
        nextScan = 0;
        reactionRemaining = burstTicks = restTicks = 0;
        scannedMissiles = interceptableMissiles = eligibleTargets = 0;
        rejectedTeam = rejectedSight = rejectedTraverse = rejectedIntercept = 0;
        rejectedTargetId = -1;
        rejectedTargetReason = "none";
    }

    private void setTarget(Entity target, MCH_GunnerInfo info) {
        targetEntity = target;
        reactionRemaining = info.reactionTicks;
        yawError = (rand.nextFloat() * 2.0F - 1.0F) * info.aimError;
        pitchError = (rand.nextFloat() * 2.0F - 1.0F) * info.aimError;
        burstTicks = 0;
        restTicks = 0;
    }

    private boolean isMissile(Entity entity) {
        return entity instanceof MCH_EntityBaseBullet && ((MCH_EntityBaseBullet) entity).isMissileProjectile();
    }

    public boolean isHostileMissile(MCH_EntityBaseBullet missile, MCH_EntityAircraft ac) {
        if (!missile.isMissileProjectile() || !missile.isInterceptable() || missile.isDead
            || (ac != null && (missile.shootingAircraft == ac || ac.isMountedEntity(missile.shootingEntity)))) return false;
        String shooterTeam = MCH_GunnerTeams.teamOf(worldObj, missile.shootingEntity);
        if (getTeamName().isEmpty() || getTeamName().equals(shooterTeam)) return false;
        if (missile.shootingAircraft instanceof MCH_EntityAircraft) {
            MCH_EntityAircraft launcher = (MCH_EntityAircraft) missile.shootingAircraft;
            for (int seat = 0; seat <= launcher.getSeatNum(); ++seat)
                if (getTeamName().equals(MCH_GunnerTeams.teamOf(worldObj, launcher.getEntityBySeatId(seat)))) return false;
            if (shooterTeam.isEmpty()) return hostileAircraft(launcher);
        }
        return MCH_AAGunnerMath.enemies(getTeamName(), shooterTeam);
    }

    private boolean hostileAircraft(MCH_EntityAircraft target) {
        boolean enemy = false;
        for (int seat = 0; seat <= target.getSeatNum(); ++seat) {
            Entity occupant = target.getEntityBySeatId(seat);
            String team = MCH_GunnerTeams.teamOf(worldObj, occupant);
            if (getTeamName().equals(team)) return false;
            if (occupant instanceof EntityPlayer && ((EntityPlayer) occupant).capabilities.isCreativeMode)
                return false;
            enemy |= MCH_AAGunnerMath.enemies(getTeamName(), team);
        }
        return enemy;
    }

    private String rejectTarget(Entity target, MCH_EntityAircraft ac, MCH_WeaponSet ws, Vec3 muzzle, MCH_GunnerInfo info) {
        if (target == null || target.isDead || target == ac || ac.isMountedEntity(target)
            || worldObj.getEntityByID(target.getEntityId()) != target
            || target.getDistanceSq(muzzle.xCoord, muzzle.yCoord, muzzle.zCoord)
                > info.detectionRange * info.detectionRange) return "range/state";
        boolean hostile = false;
        if (info.targetMissiles && isMissile(target)) {
            hostile = isHostileMissile((MCH_EntityBaseBullet) target, ac);
        } else if (info.targetAircraft && (target instanceof MCH_EntityHeli || target instanceof MCP_EntityPlane)) {
            MCH_EntityAircraft aircraft = (MCH_EntityAircraft) target;
            hostile = !aircraft.isDestroyed() && hostileAircraft(aircraft);
        }
        if (!hostile) return "team/type";
        Vec3 center = targetCenter(target);
        if (!hasLineOfSight(muzzle, center)) return "sight";
        Vec3 aim = getAimPoint(ac, ws, muzzle, target, info);
        if (aim == null) return "intercept";
        String traverse = traverseRejection(ac, ws, muzzle, aim);
        if (traverse != null) return traverse;
        return hasLineOfSight(muzzle, aim) ? null : "sight";
    }

    private boolean hasLineOfSight(Vec3 from, Vec3 to) {
        double dx = to.xCoord - from.xCoord;
        double dy = to.yCoord - from.yCoord;
        double dz = to.zCoord - from.zCoord;
        // 100-block segments need at most ~174 voxel steps, below vanilla's 200-step cap.
        int segments = Math.max(1, (int) Math.ceil(Math.sqrt(dx * dx + dy * dy + dz * dz) / 100.0D));
        for (int segment = 0; segment < segments; ++segment) {
            double first = (double) segment / segments;
            double last = (double) (segment + 1) / segments;
            Vec3 start = Vec3.createVectorHelper(from.xCoord + dx * first, from.yCoord + dy * first, from.zCoord + dz * first);
            Vec3 end = Vec3.createVectorHelper(from.xCoord + dx * last, from.yCoord + dy * last, from.zCoord + dz * last);
            // Do not force terrain chunks into memory to evaluate an uncertain shot.
            if (!worldObj.checkChunksExist(MathHelper.floor_double(Math.min(start.xCoord, end.xCoord)), 0,
                MathHelper.floor_double(Math.min(start.zCoord, end.zCoord)),
                MathHelper.floor_double(Math.max(start.xCoord, end.xCoord)), 255,
                MathHelper.floor_double(Math.max(start.zCoord, end.zCoord)))) return false;
            // 1.7.10 mutates the ray start in place. Only disposable copies may cross this boundary.
            if (worldObj.rayTraceBlocks(start, end) != null) return false;
        }
        return true;
    }

    private Vec3 targetCenter(Entity target) {
        if (target instanceof MCH_EntityBaseBullet) target.setPosition(target.posX, target.posY, target.posZ);
        return Vec3.createVectorHelper((target.boundingBox.minX + target.boundingBox.maxX) * 0.5D,
            (target.boundingBox.minY + target.boundingBox.maxY) * 0.5D,
            (target.boundingBox.minZ + target.boundingBox.maxZ) * 0.5D);
    }

    private String traverseRejection(MCH_EntityAircraft ac, MCH_WeaponSet ws, Vec3 muzzle, Vec3 point) {
        MCH_AircraftInfo.Weapon mount = ac.getAcInfo().getWeaponById(ac.getCurrentWeaponID(this));
        if (mount == null) return "yaw limits";
        double x = point.xCoord - muzzle.xCoord;
        double y = point.yCoord - muzzle.yCoord;
        double z = point.zCoord - muzzle.zCoord;
        float pitch = (float) -Math.toDegrees(Math.atan2(y, Math.sqrt(x * x + z * z)))
            - ws.getCurrentWeapon().fixRotationPitch - ac.getRotPitch();
        // Vehicle body attitude limits do not constrain its independently rotating gun.
        if (pitch < mount.minPitch || pitch > mount.maxPitch) return "pitch limits";
        if (Math.abs(mount.minYaw) >= 360.0F || Math.abs(mount.maxYaw) >= 360.0F) return null;
        float yaw = (float) Math.toDegrees(Math.atan2(z, x)) - 90.0F - ws.getCurrentWeapon().fixRotationYaw;
        float base = mount.turret ? ac.getLastRiderYaw() : ac.getRotYaw();
        float relative = MCH_AAGunnerMath.wrap(yaw - base - mount.defaultYaw);
        return relative >= mount.minYaw && relative <= mount.maxYaw ? null : "yaw limits";
    }

    private Entity selectTarget(MCH_EntityAircraft ac, MCH_WeaponSet ws, Vec3 muzzle, MCH_GunnerInfo info) {
        Entity best = null;
        double bestScore = Double.MAX_VALUE;
        scannedMissiles = 0;
        interceptableMissiles = 0;
        eligibleTargets = 0;
        rejectedTeam = rejectedSight = rejectedTraverse = rejectedIntercept = 0;
        rejectedTargetId = -1;
        rejectedTargetReason = "none";
        List<Entity> nearby = worldObj.getEntitiesWithinAABBExcludingEntity(ac,
            ac.boundingBox.expand(info.detectionRange, info.detectionRange, info.detectionRange));
        for (Entity entity : nearby) {
            if (!isMissile(entity) && !(entity instanceof MCH_EntityHeli) && !(entity instanceof MCP_EntityPlane)) continue;
            if (isMissile(entity)) {
                ++scannedMissiles;
                if (((MCH_EntityBaseBullet) entity).isInterceptable()) ++interceptableMissiles;
            }
            String reason = rejectTarget(entity, ac, ws, muzzle, info);
            if (reason != null) {
                rejectedTargetId = entity.getEntityId();
                rejectedTargetReason = reason;
                if (reason.equals("team/type")) ++rejectedTeam;
                else if (reason.equals("sight")) ++rejectedSight;
                else if (reason.equals("pitch limits") || reason.equals("yaw limits")) ++rejectedTraverse;
                else if (reason.equals("intercept")) ++rejectedIntercept;
                continue;
            }
            ++eligibleTargets;
            double score = targetScore(entity, ac);
            if (score < bestScore) { best = entity; bestScore = score; }
        }
        // Hysteresis applies to an existing track, not the order candidates were returned in.
        if (targetEntity != null && best != null && isMissile(best) == isMissile(targetEntity)
            && bestScore >= targetScore(targetEntity, ac) * 0.8D) return targetEntity;
        return best;
    }

    private double targetScore(Entity target, MCH_EntityAircraft ac) {
        // Missiles take priority, then distance; hysteresis prevents rapid target switching.
        return ac.getDistanceSqToEntity(target) + (isMissile(target) ? 0.0D : 2000000.0D);
    }

    private Vec3 getAimPoint(MCH_EntityAircraft ac, MCH_WeaponSet ws, Vec3 muzzle, Entity target, MCH_GunnerInfo info) {
        Vec3 center = targetCenter(target);
        double x = center.xCoord - muzzle.xCoord;
        double y = center.yCoord - muzzle.yCoord;
        double z = center.zCoord - muzzle.zCoord;
        double vx = target.motionX;
        double vy = target.motionY;
        double vz = target.motionZ;
        double targetGravity = 0.0D;
        boolean moved = false;
        if (target instanceof MCH_EntityBaseBullet) {
            MCH_EntityBaseBullet bullet = (MCH_EntityBaseBullet) target;
            vx *= bullet.accelerationFactor;
            vy *= bullet.accelerationFactor;
            vz *= bullet.accelerationFactor;
            targetGravity = bullet.getGravity() * bullet.accelerationFactor;
            moved = bullet.hasMovedThisWorldTick();
        } else if (target instanceof MCH_EntityAircraft) {
            moved = ((MCH_EntityAircraft) target).hasMovedThisWorldTick();
        }
        if (moved) {
            x -= target.posX - target.prevPosX;
            y -= target.posY - target.prevPosY;
            z -= target.posZ - target.prevPosZ;
        }
        double baseSpeed = Math.min(ws.getCurrentWeapon().acceleration, 3.9D);
        double speed = baseSpeed;
        if (ws.getInfo().speedDependsAircraft)
            speed += Math.sqrt(ac.motionX * ac.motionX + ac.motionY * ac.motionY + ac.motionZ * ac.motionZ);
        double factor = Math.max(1.0D, ws.getInfo().acceleration / 4.0D);
        // Match MG muzzle advance and discrete projectile movement, including opted-in falling bombs.
        Vec3 solution = MCH_AAGunnerMath.interceptPoint(x, y, z,
            vx * info.leadFactor, vy * info.leadFactor, vz * info.leadFactor,
            speed * factor, baseSpeed * 0.5D, targetGravity * info.leadFactor,
            ws.getInfo().gravity * factor, moved);
        if (solution != null && ws.getInfo().timeFuse > 0) {
            double distance = Math.sqrt(solution.xCoord * solution.xCoord + solution.yCoord * solution.yCoord
                + solution.zCoord * solution.zCoord);
            if (distance > speed * factor * ws.getInfo().timeFuse + baseSpeed * 0.5D) return null;
        }
        return solution == null ? null : solution.addVector(muzzle.xCoord, muzzle.yCoord, muzzle.zCoord);
    }

    private void aimAndFire(MCH_EntityAircraft ac, MCH_WeaponSet ws, Vec3 muzzle, MCH_GunnerInfo info) {
        MCH_WeaponBase weapon = ws.getCurrentWeapon();
        Vec3 aim = getAimPoint(ac, ws, muzzle, targetEntity, info);
        if (aim == null) { status = "no reachable intercept"; return; }
        double x = aim.xCoord - muzzle.xCoord;
        double y = aim.yCoord - muzzle.yCoord;
        double z = aim.zCoord - muzzle.zCoord;
        float shotYaw = (float) Math.toDegrees(Math.atan2(z, x)) - 90.0F;
        float shotPitch = (float) -Math.toDegrees(Math.atan2(y, Math.sqrt(x * x + z * z)));
        float desiredYaw = shotYaw - weapon.fixRotationYaw + yawError;
        float desiredPitch = shotPitch - weapon.fixRotationPitch + pitchError;
        MCH_AircraftInfo.Weapon mount = ac.getAcInfo().getWeaponById(ac.getCurrentWeaponID(this));
        if (mount == null) return;
        // The native full-circle branch subtracts DefaultYaw rather than adding it back.
        if (Math.abs(mount.minYaw) >= 360.0F || Math.abs(mount.maxYaw) >= 360.0F)
            desiredYaw += mount.defaultYaw;
        desiredPitch = MathHelper.clamp_float(desiredPitch,
            mount.minPitch + ac.getRotPitch(), mount.maxPitch + ac.getRotPitch());
        // Native turret yaw constraints use the pilot look direction as the turret reference.
        float yawBase = mount.turret ? ac.getLastRiderYaw() : ac.getRotYaw();
        if (Math.abs(mount.minYaw) < 360.0F && Math.abs(mount.maxYaw) < 360.0F
            && (mount.minYaw != 0.0F || mount.maxYaw != 0.0F)) {
            float relative = MCH_AAGunnerMath.wrap(desiredYaw - yawBase - mount.defaultYaw);
            desiredYaw = yawBase + mount.defaultYaw + MathHelper.clamp_float(relative, mount.minYaw, mount.maxYaw);
        }
        // Close the loop on native barrel state. Entity look alone can disagree with the
        // gun after mount clamping or a rider rotation update, causing the same step forever.
        float barrelLookYaw = ac.getRotYaw() + ws.rotationYaw;
        if (Math.abs(mount.minYaw) >= 360.0F || Math.abs(mount.maxYaw) >= 360.0F)
            barrelLookYaw += mount.defaultYaw;
        float barrelLookPitch = ac.getRotPitch() + ws.rotationPitch;
        float yawStep = MCH_AAGunnerMath.lookStep(MCH_AAGunnerMath.wrap(desiredYaw - barrelLookYaw), info.lookSpeed, info.trackingResponse);
        float pitchStep = MCH_AAGunnerMath.lookStep(desiredPitch - barrelLookPitch,
            info.lookSpeed * ws.getInfo().cameraRotationSpeedPitch, info.trackingResponse);
        requestedYaw = barrelLookYaw + yawStep;
        requestedPitch = barrelLookPitch + pitchStep;
        applyLookInput(MCH_AAGunnerMath.wrap(requestedYaw - rotationYaw) / 0.15F,
            -(requestedPitch - rotationPitch) / 0.15F);
        rotationYawHead = rotationYaw;
        // Consume this tick's bounded look intent through the normal mount solver before testing
        // fire alignment. Previously the fire gate examined the preceding tick's barrel angles.
        ac.updateWeaponsRotation();
        Vec3 finalMuzzle = getGunnerWeaponPos(ac, ws);
        Vec3 finalAim = getAimPoint(ac, ws, finalMuzzle, targetEntity, info);
        if (finalAim == null || traverseRejection(ac, ws, finalMuzzle, finalAim) != null) {
            status = "intercept outside weapon limits";
            return;
        }
        if (!hasLineOfSight(finalMuzzle, finalAim)) { status = "blocked sight after aiming"; return; }
        x = finalAim.xCoord - finalMuzzle.xCoord;
        y = finalAim.yCoord - finalMuzzle.yCoord;
        z = finalAim.zCoord - finalMuzzle.zCoord;
        shotYaw = (float) Math.toDegrees(Math.atan2(z, x)) - 90.0F;
        shotPitch = (float) -Math.toDegrees(Math.atan2(y, Math.sqrt(x * x + z * z)));
        aimYaw = shotYaw;
        aimPitch = shotPitch;
        if (restTicks > 0) { --restTicks; return; }
        if (info.enableShortBurst && burstTicks > 0 && --burstTicks == 0) {
            restTicks = info.shortBurstRestTick;
            if (restTicks > 0) { --restTicks; return; }
        }
        // Read the actual barrel state; never rotate the weapon set directly to the target.
        float barrelYaw = ac.getRotYaw() + ws.rotationYaw + weapon.fixRotationYaw;
        float barrelPitch = ac.getRotPitch() + ws.rotationPitch + weapon.fixRotationPitch;
        fireYawError = MCH_AAGunnerMath.wrap(barrelYaw - shotYaw);
        firePitchError = barrelPitch - shotPitch;
        if (Math.abs(fireYawError) > info.fireTolerance || Math.abs(firePitchError) > info.fireTolerance) {
            status = "turning to intercept";
            return;
        }
        if (!ws.canUse()) { status = "weapon cooldown/reload"; return; }
        MCH_WeaponParam prm = new MCH_WeaponParam();
        prm.setPosition(ac.posX, ac.posY, ac.posZ);
        prm.entity = ac;
        prm.user = this;
        boolean fired = ac.useCurrentWeapon(prm);
        status = fired ? "firing" : "weapon refused shot: check ammo/heat";
        if (fired && info.enableShortBurst && burstTicks == 0) burstTicks = info.shortBurstFireTick;
    }

    private void applyLookInput(float yawInput, float pitchInput) {
        // Entity.setAngles is stripped on dedicated servers in 1.7.10. Mirror its relative
        // input semantics here; the ordinary aircraft update still owns barrel rotation.
        float oldYaw = rotationYaw;
        float oldPitch = rotationPitch;
        rotationYaw += yawInput * 0.15F;
        rotationPitch = MathHelper.clamp_float(rotationPitch - pitchInput * 0.15F, -90.0F, 90.0F);
        prevRotationYaw += rotationYaw - oldYaw;
        prevRotationPitch += rotationPitch - oldPitch;
    }

    public Vec3 getGunnerWeaponPos(MCH_EntityAircraft ac, MCH_WeaponSet ws) {
        return ws.getCurrentWeapon().getShotPos(ac).addVector(ac.posX, ac.posY, ac.posZ);
    }

    public MCH_EntityAircraft getAc() {
        return ridingEntity instanceof MCH_EntitySeat ? ((MCH_EntitySeat) ridingEntity).getParent()
            : ridingEntity instanceof MCH_EntityAircraft ? (MCH_EntityAircraft) ridingEntity : null;
    }

    @Override
    public boolean interactFirst(EntityPlayer player) {
        if (isDead || ridingEntity == null || !player.isSneaking()) return false;
        if (worldObj.isRemote) return true;
        if (player.capabilities.isCreativeMode || (!isCreative && (ownerUUID.equals(player.getUniqueID().toString())
            || (!getTeamName().isEmpty() && getTeamName().equals(MCH_GunnerTeams.teamOf(worldObj, player)))))) {
            removeFromAircraft(player);
            return true;
        }
        player.addChatMessage(new ChatComponentText("Only the owner, their team, or a creative player can remove this gunner."));
        return true;
    }

    public void removeFromAircraft(EntityPlayer player) {
        if (worldObj.isRemote) return;
        setDead();
        mountEntity(null);
        player.addChatMessage(new ChatComponentText("AA gunner removed."));
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound nbt) {
        super.writeEntityToNBT(nbt);
        nbt.setBoolean("Creative", isCreative);
        nbt.setString("OwnerUUID", ownerUUID);
        nbt.setString("TeamName", getTeamName());
        nbt.setInteger("TargetType", getTargetType());
        nbt.setString("GunnerProfile", profileName);
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound nbt) {
        super.readEntityFromNBT(nbt);
        isCreative = nbt.getBoolean("Creative");
        ownerUUID = nbt.getString("OwnerUUID");
        setTeamName(nbt.getString("TeamName"));
        setTargetType(nbt.getInteger("TargetType"));
        profileName = nbt.getString("GunnerProfile");
    }

    @Override
    public boolean isEntityInvulnerable() { return isCreative; }

    @Override
    public void setDead() {
        if (!worldObj.isRemote && !isDead && !isCreative && getTargetType() == TARGET_AA_AMMO) {
            if (MCH_MOD.itemSpawnGunnerAA != null) dropItem(MCH_MOD.itemSpawnGunnerAA, 1);
        }
        super.setDead();
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float damage) {
        if (source == DamageSource.outOfWorld) setDead();
        return super.attackEntityFrom(source, damage);
    }

    @Override public void travelToDimension(int dimension) { }
    @Override public ItemStack getHeldItem() { return null; }
    @Override public ItemStack getEquipmentInSlot(int slot) { return null; }
    @Override public void setCurrentItemOrArmor(int slot, ItemStack stack) { }
    @Override public ItemStack[] getLastActiveItems() { return new ItemStack[0]; }
    public ItemStack[] getInventory() { return new ItemStack[0]; }
}
