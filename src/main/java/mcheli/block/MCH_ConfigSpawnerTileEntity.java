package mcheli.block;

import mcheli.MCH_ServerSettings;
import mcheli.MCH_WaypointNavDebug;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.aircraft.MCH_ItemAircraft;
import mcheli.aircraft.MCH_EntitySeat;
import mcheli.helicopter.MCH_HeliInfo;
import mcheli.helicopter.MCH_HeliInfoManager;
import mcheli.mob.MCH_EntityGunner;
import mcheli.mob.MCH_GunnerInfo;
import mcheli.mob.MCH_GunnerInfoManager;
import mcheli.plane.MCP_PlaneInfo;
import mcheli.plane.MCP_PlaneInfoManager;
import mcheli.tank.MCH_TankInfo;
import mcheli.tank.MCH_TankInfoManager;
import mcheli.vehicle.MCH_VehicleInfo;
import mcheli.vehicle.MCH_VehicleInfoManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MCH_ConfigSpawnerTileEntity extends TileEntity {

    public static final int STATE_ACTIVE = 0;
    public static final int STATE_SLEEP = 1;
    public static final int STATE_ERROR = 2;
    private String blockInfoName = "";
    private long nextCheckTick = 0L;
    private long cooldownEndTick = 0L;
    private boolean spawnedOnce = false;
    private int visualState = STATE_ACTIVE;
    private boolean waitingVehicleDestroyed = false;
    private int trackedVehicleEntityId = -1;
    private long trackedVehicleUuidMost = 0L;
    private long trackedVehicleUuidLeast = 0L;
    private int lastGlobalGunnerVehicleCount = -1;
    private boolean lastGlobalGunnerVehicleLock = false;
    private static final Map<Integer, Map<String, List<MCH_ConfigSpawnerTileEntity>>> WAYPOINT_REGISTRY = new HashMap<Integer, Map<String, List<MCH_ConfigSpawnerTileEntity>>>();

    public MCH_ConfigSpawnerTileEntity() {
    }

    public MCH_ConfigSpawnerTileEntity(String blockInfoName) {
        this.blockInfoName = blockInfoName;
    }

    /**
     * Force one immediate spawn attempt for structure placement flow.
     * This bypasses normal check interval/cooldown timing but still uses
     * configured vehicle pool and spawn collision checks.
     */
    public boolean forceSpawnOnceNow() {
        if (this.worldObj == null || this.worldObj.isRemote) {
            return false;
        }
        MCH_BlockInfo info = MCH_BlockInfoManager.get(this.blockInfoName);
        if (info == null) {
            this.setVisualState(STATE_ERROR, 2);
            return false;
        }
        this.updateWaypointRegistry(info);
        if (this.isWaypointMarker(info) && !info.enableSpawner) {
            this.setVisualState(STATE_ACTIVE, info.stateSyncTick);
            return false;
        }
        if (!info.enableSpawner || info.vehiclePool == null || info.vehiclePool.isEmpty()) {
            this.setVisualState(STATE_ERROR, 2);
            return false;
        }
        if (MCH_ServerSettings.freezeConfigSpawner) {
            this.setVisualState(STATE_ACTIVE, info.stateSyncTick);
            return false;
        }
        if (this.isGlobalGunnerVehicleLockActive(info)) {
            this.setVisualState(STATE_SLEEP, info.stateSyncTick);
            return false;
        }

        // Reset runtime waiting/cooldown state before force attempt.
        this.nextCheckTick = 0L;
        this.cooldownEndTick = 0L;
        this.waitingVehicleDestroyed = false;
        this.clearTrackedVehicle();

        boolean spawned = this.spawnOne(info);
        if (spawned) {
            if (this.isOnceMode(info)) {
                this.spawnedOnce = true;
                this.setVisualState(STATE_SLEEP, 2);
            } else {
                this.spawnedOnce = false;
                this.waitingVehicleDestroyed = true;
                this.setVisualState(STATE_SLEEP, 2);
            }
            this.markDirty();
            return true;
        }

        this.setVisualState(STATE_ACTIVE, info.stateSyncTick);
        this.markDirty();
        return false;
    }

    public void updateEntity() {
        if (this.worldObj == null || this.worldObj.isRemote) {
            return;
        }
        MCH_BlockInfo info = MCH_BlockInfoManager.get(this.blockInfoName);
        if (info == null) {
            this.setVisualState(STATE_ERROR, 2);
            return;
        }
        this.updateWaypointRegistry(info);
        if (this.isWaypointMarker(info)) {
            this.setVisualState(STATE_ACTIVE, info.stateSyncTick);
            if (!info.enableSpawner) {
                return;
            }
        } else if (!info.enableSpawner) {
            this.setVisualState(STATE_ERROR, 2);
            return;
        }
        if (info.vehiclePool == null || info.vehiclePool.isEmpty()) {
            this.setVisualState(STATE_ERROR, 2);
            return;
        }
        if (MCH_ServerSettings.freezeConfigSpawner) {
            // Freeze mode keeps block visually active and skips all spawn logic.
            this.setVisualState(STATE_ACTIVE, info.stateSyncTick);
            return;
        }
        if (this.isGlobalGunnerVehicleLockActive(info)) {
            this.setVisualState(STATE_SLEEP, info.stateSyncTick);
            return;
        }
        if (this.spawnedOnce && this.isOnceMode(info)) {
            this.setVisualState(STATE_SLEEP, 2);
            return;
        }
        long now = this.worldObj.getTotalWorldTime();
        if (this.waitingVehicleDestroyed) {
            if (this.isTrackedVehicleAlive()) {
                this.setVisualState(STATE_SLEEP, info.stateSyncTick);
                return;
            }
            this.waitingVehicleDestroyed = false;
            this.clearTrackedVehicle();
            if (!this.isOnceMode(info)) {
                this.cooldownEndTick = now + Math.max(1, info.cooldownTick);
            }
            this.setVisualState(STATE_SLEEP, 2);
            this.markDirty();
            return;
        }
        if (now < this.cooldownEndTick) {
            this.setVisualState(STATE_SLEEP, info.stateSyncTick);
            return;
        }
        if (now < this.nextCheckTick) {
            return;
        }
        this.nextCheckTick = now + Math.max(1, info.checkIntervalTick);
        if (!this.isAreaEmpty(info)) {
            this.setVisualState(STATE_ACTIVE, info.stateSyncTick);
            return;
        }
        boolean spawned = false;
        int spawnCount = Math.max(1, info.spawnVehicleCount);
        for (int i = 0; i < spawnCount; ++i) {
            if (this.spawnOne(info)) {
                spawned = true;
            }
        }
        if (spawned) {
            if (this.isOnceMode(info)) {
                this.spawnedOnce = true;
                this.setVisualState(STATE_SLEEP, 2);
            } else {
                this.cooldownEndTick = 0L;
                this.waitingVehicleDestroyed = true;
                this.setVisualState(STATE_SLEEP, 2);
            }
            this.markDirty();
        } else {
            this.cooldownEndTick = now + Math.max(20, Math.max(1, info.cooldownTick) / 4);
            this.setVisualState(STATE_ERROR, 2);
            this.markDirty();
        }
    }

    private boolean spawnOne(MCH_BlockInfo info) {
        String candidate = this.pickVehicleEntry(info);
        if (candidate == null || candidate.isEmpty()) {
            return false;
        }
        MCH_ItemAircraft aircraftItem = this.resolveAircraftItem(candidate);
        if (aircraftItem == null) {
            return false;
        }
        ItemStack itemStack = new ItemStack(aircraftItem, 1, 0);
        float yaw = this.resolveSpawnYaw(info);
        double px = this.xCoord + 0.5D;
        double py = this.yCoord + 1.0D + info.spawnYOffset;
        double pz = this.zCoord + 0.5D;
        MCH_EntityAircraft aircraft = aircraftItem.createAircraft(this.worldObj, px, py, pz, itemStack);
        if (aircraft == null) {
            return false;
        }
        aircraft.initRotationYaw(yaw);
        if (!this.worldObj.getCollidingBoundingBoxes(aircraft, aircraft.boundingBox.expand(-0.1D, -0.1D, -0.1D)).isEmpty()) {
            return false;
        }
        aircraft.getAcDataFromItem(itemStack);
        if (!this.worldObj.spawnEntityInWorld(aircraft)) {
            return false;
        }
        this.attachInitialWaypoint(aircraft, info);
        if (info.spawnGunner && !this.spawnAndMountGunner(info, aircraft)) {
            aircraft.setDead();
            return false;
        }
        this.trackVehicle(aircraft);
        return true;
    }

    private boolean spawnAndMountGunner(MCH_BlockInfo info, MCH_EntityAircraft aircraft) {
        if (info.gunnerMode != null && info.gunnerMode.equalsIgnoreCase("none")) {
            return true;
        }
        MCH_GunnerInfo profile = null;
        if (info.gunnerProfile != null && !info.gunnerProfile.trim().isEmpty()) {
            profile = MCH_GunnerInfoManager.get(info.gunnerProfile.trim());
            if (profile == null) {
                return false;
            }
        }
        MCH_EntityGunner gunner = new MCH_EntityGunner(this.worldObj, aircraft.posX, aircraft.posY, aircraft.posZ);
        gunner.rotationYaw = info.gunnerYaw;
        gunner.rotationPitch = info.gunnerPitch;
        gunner.isCreative = true;
        gunner.ownerUUID = "";
        int targetType = profile != null ? profile.targetType : info.gunnerTargetType;
        if (profile != null) {
            Team team = this.resolveTeamForSpawner(profile, info, targetType);
            if (targetType == MCH_EntityGunner.TARGET_PLAYER && this.needTeamByProfile(profile) && team == null) {
                return false;
            }
            if (team != null) {
                gunner.setTeamName(team.getRegisteredName());
            }
            String role = profile.factionRole == null ? "normal" : profile.factionRole;
            float chance = profile.getStupidChanceForRole(role);
            boolean stupidByChance = chance >= 0.0F && this.worldObj.rand.nextFloat() < chance;
            gunner.setStupidGunner(profile.stupidGunner || stupidByChance);
            gunner.setFactionRole(role);
            gunner.setProfileSearchRanges(
                profile.searchRangeGroundHorizontal,
                profile.searchRangeGroundVertical,
                profile.searchRangeAirHorizontal,
                profile.searchRangeAirVertical,
                profile.searchRangeFallbackToConfig
            );
            gunner.setProfileWeaponPriority(profile.airWeaponPriorityRaw, profile.groundWeaponPriorityRaw);
            gunner.setProfileCombatBehavior(
                profile.allowLeadForAirTarget,
                profile.stupidAttackSectorScaleGround,
                profile.enableShortBurst,
                profile.shortBurstFireTick,
                profile.shortBurstRestTick
            );
        } else if (info.gunnerMode != null && info.gunnerMode.equalsIgnoreCase("faction")) {
            targetType = MCH_EntityGunner.TARGET_PLAYER;
            String teamName = this.ensureFactionTeam(info);
            if (teamName == null || teamName.isEmpty()) {
                return false;
            }
            gunner.setTeamName(teamName);
        }
        gunner.setTargetType(targetType);
        if (!this.worldObj.spawnEntityInWorld(gunner)) {
            return false;
        }
        Entity mountTarget = this.resolveMountTarget(info, aircraft);
        if (mountTarget == null) {
            gunner.setDead();
            return false;
        }
        gunner.mountEntity(mountTarget);
        return true;
    }

    private Entity resolveMountTarget(MCH_BlockInfo info, MCH_EntityAircraft aircraft) {
        MCH_EntitySeat[] seats = aircraft.getSeats();
        if (info.gunnerSeatIndex > 0) {
            int seatIdx = info.gunnerSeatIndex - 1;
            if (seats != null && seatIdx >= 0 && seatIdx < seats.length) {
                MCH_EntitySeat seat = seats[seatIdx];
                if (seat != null && !seat.isDead && seat.riddenByEntity == null) {
                    return seat;
                }
            }
            return null;
        }
        if (aircraft.riddenByEntity == null) {
            return aircraft;
        }
        if (seats != null) {
            for (MCH_EntitySeat seat : seats) {
                if (seat != null && !seat.isDead && seat.riddenByEntity == null) {
                    return seat;
                }
            }
        }
        return null;
    }

    private String ensureFactionTeam(MCH_BlockInfo info) {
        Team team = this.resolveTeamByBlockFaction(info);
        return team != null ? team.getRegisteredName() : null;
    }

    private boolean needTeamByProfile(MCH_GunnerInfo profile) {
        if (profile == null) {
            return true;
        }
        if ("none".equalsIgnoreCase(profile.teamMode)) {
            return false;
        }
        return !"fixed".equalsIgnoreCase(profile.teamMode) ? profile.requirePlayerTeamWhenPvp : true;
    }

    private Team resolveTeamForSpawner(MCH_GunnerInfo profile, MCH_BlockInfo info, int targetType) {
        if (profile == null) {
            return this.resolveTeamByBlockFaction(info);
        }
        if ("none".equalsIgnoreCase(profile.teamMode)) {
            return null;
        }
        if ("fixed".equalsIgnoreCase(profile.teamMode)) {
            String teamId = profile.fixedTeamId == null ? "" : profile.fixedTeamId.trim();
            if (teamId.isEmpty()) {
                return null;
            }
            Scoreboard scoreboard = this.worldObj.getScoreboard();
            Team team = scoreboard.getTeam(teamId);
            if (team == null && profile.autoCreateTeam) {
                ScorePlayerTeam created = scoreboard.createTeam(teamId);
                if (created != null) {
                    if (profile.fixedTeamDisplayName != null && !profile.fixedTeamDisplayName.trim().isEmpty()) {
                        created.setTeamName(profile.fixedTeamDisplayName.trim());
                    }
                    team = created;
                }
            }
            return team;
        }
        // TeamMode=player has no player context in block spawner. Fallback to block faction settings.
        Team team = this.resolveTeamByBlockFaction(info);
        if (team != null) {
            return team;
        }
        // Non-PVP targets (AA/enemy/monster) can still benefit from team assignment for friendly filtering.
        if (targetType != MCH_EntityGunner.TARGET_PLAYER) {
            return this.resolveTeamByBlockFaction(info);
        }
        return null;
    }

    private Team resolveTeamByBlockFaction(MCH_BlockInfo info) {
        if (info == null || info.gunnerFactionId == null || info.gunnerFactionId.trim().isEmpty()) {
            return null;
        }
        String teamId = info.gunnerFactionId.trim();
        Scoreboard scoreboard = this.worldObj.getScoreboard();
        Team team = scoreboard.getTeam(teamId);
        if (team == null && info.autoCreateFaction) {
            ScorePlayerTeam created = scoreboard.createTeam(teamId);
            if (created != null) {
                if (info.gunnerFactionName != null && !info.gunnerFactionName.trim().isEmpty()) {
                    created.setTeamName(info.gunnerFactionName.trim());
                }
                team = created;
            }
        }
        return team;
    }

    private MCH_ItemAircraft resolveAircraftItem(String entry) {
        String[] parts = entry.split(":");
        if (parts.length != 2) {
            return null;
        }
        String type = parts[0].trim().toLowerCase();
        String name = parts[1].trim().toLowerCase();
        if (type.equals("heli")) {
            MCH_HeliInfo info = MCH_HeliInfoManager.get(name);
            return info != null ? (MCH_ItemAircraft) info.item : null;
        }
        if (type.equals("plane")) {
            MCP_PlaneInfo info = MCP_PlaneInfoManager.get(name);
            return info != null ? (MCH_ItemAircraft) info.item : null;
        }
        if (type.equals("tank")) {
            MCH_TankInfo info = MCH_TankInfoManager.get(name);
            return info != null ? (MCH_ItemAircraft) info.item : null;
        }
        if (type.equals("vehicle")) {
            MCH_VehicleInfo info = MCH_VehicleInfoManager.get(name);
            return info != null ? (MCH_ItemAircraft) info.item : null;
        }
        return null;
    }

    private String pickVehicleEntry(MCH_BlockInfo info) {
        if (info.vehiclePool == null || info.vehiclePool.isEmpty()) {
            return null;
        }
        if (info.vehicleWeight == null || info.vehicleWeight.isEmpty() || info.vehicleWeight.size() != info.vehiclePool.size()) {
            return info.vehiclePool.get(this.worldObj.rand.nextInt(info.vehiclePool.size()));
        }
        int total = 0;
        for (int i = 0; i < info.vehicleWeight.size(); ++i) {
            total += Math.max(1, info.vehicleWeight.get(i));
        }
        if (total <= 0) {
            return info.vehiclePool.get(this.worldObj.rand.nextInt(info.vehiclePool.size()));
        }
        int roll = this.worldObj.rand.nextInt(total);
        int sum = 0;
        for (int i = 0; i < info.vehiclePool.size(); ++i) {
            sum += Math.max(1, info.vehicleWeight.get(i));
            if (roll < sum) {
                return info.vehiclePool.get(i);
            }
        }
        return info.vehiclePool.get(info.vehiclePool.size() - 1);
    }

    private float resolveSpawnYaw(MCH_BlockInfo info) {
        if (info.spawnYawMode != null && info.spawnYawMode.equalsIgnoreCase("fixed")) {
            return info.spawnYaw;
        }
        if (info.spawnYawMode != null && info.spawnYawMode.equalsIgnoreCase("block_facing")) {
            int meta = this.worldObj.getBlockMetadata(this.xCoord, this.yCoord, this.zCoord);
            return (meta & 3) * 90.0F;
        }
        return this.worldObj.rand.nextFloat() * 360.0F;
    }

    private boolean isAreaEmpty(MCH_BlockInfo info) {
        double r = Math.max(1.0D, info.checkRadius);
        AxisAlignedBB aabb = AxisAlignedBB.getBoundingBox(this.xCoord + 0.5D - r, this.yCoord + 0.5D - r, this.zCoord + 0.5D - r, this.xCoord + 0.5D + r, this.yCoord + 0.5D + r, this.zCoord + 0.5D + r);
        if (info.detectPlayers) {
            List<EntityPlayer> players = this.worldObj.getEntitiesWithinAABB(EntityPlayer.class, aabb);
            for (EntityPlayer player : players) {
                if (player == null || player.isDead) {
                    continue;
                }
                if (info.ignoreCreativePlayer && player.capabilities.isCreativeMode) {
                    continue;
                }
                return false;
            }
        }
        if (info.detectMobs) {
            List<IMob> mobs = this.worldObj.getEntitiesWithinAABB(IMob.class, aabb);
            for (IMob mob : mobs) {
                if (((Entity) mob).isDead) {
                    continue;
                }
                return false;
            }
        }
        if (info.detectAnimals) {
            List<EntityLivingBase> living = this.worldObj.getEntitiesWithinAABB(EntityLivingBase.class, aabb);
            for (EntityLivingBase entity : living) {
                if (entity == null || entity.isDead) {
                    continue;
                }
                if (entity instanceof EntityPlayer || entity instanceof IMob || entity instanceof MCH_EntityGunner) {
                    continue;
                }
                return false;
            }
        }
        if (info.detectVehicles) {
            List<MCH_EntityAircraft> vehicles = this.worldObj.getEntitiesWithinAABB(MCH_EntityAircraft.class, aabb);
            for (MCH_EntityAircraft ac : vehicles) {
                if (ac != null && !ac.isDead) {
                    return false;
                }
            }
        }
        if (info.detectGunners) {
            List<MCH_EntityGunner> gunners = this.worldObj.getEntitiesWithinAABB(MCH_EntityGunner.class, aabb);
            for (MCH_EntityGunner gunner : gunners) {
                if (gunner != null && !gunner.isDead) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isOnceMode(MCH_BlockInfo info) {
        return info.spawnMode != null && info.spawnMode.equalsIgnoreCase("once");
    }

    private void attachInitialWaypoint(MCH_EntityAircraft aircraft, MCH_BlockInfo info) {
        if (aircraft == null || info == null || !info.enableWaypointPatrol) {
            return;
        }
        String initialId = info.initialWaypointId == null ? "" : info.initialWaypointId.trim().toLowerCase();
        if (initialId.isEmpty()) {
            return;
        }
        NBTTagCompound nav = aircraft.getEntityData().getCompoundTag("MCHWaypointNav");
        nav.setBoolean("HoldAsFreeState", info.holdAsFreeState);
        nav.setBoolean("NavigateSuppressLargeTurn", info.navigateSuppressLargeTurn);
        nav.setInteger("NavigateTimeoutTick", Math.max(20, info.navigateTimeoutTick));
        nav.setString("NavigateDrivePriority", info.navigateDrivePriority == null ? "avoid>navigate>combat" : info.navigateDrivePriority);
        nav.setString("PendingWaypointId", initialId);
        nav.setString("State", "PENDING");
        nav.setBoolean("Enabled", false);
        MCH_ConfigSpawnerTileEntity nearest = resolveNearestWaypoint(this.worldObj, initialId, aircraft.posX, aircraft.posY, aircraft.posZ);
        if (nearest == null) {
            aircraft.getEntityData().setTag("MCHWaypointNav", nav);
            MCH_WaypointNavDebug.trace(this.worldObj, null, "AttachInitialWaypoint pending: acId=%d initial=%s from=%d,%d,%d",
                aircraft.getEntityId(), initialId, this.xCoord, this.yCoord, this.zCoord);
            return;
        }
        MCH_BlockInfo wp = nearest.getBlockInfo();
        if (wp == null) {
            aircraft.getEntityData().setTag("MCHWaypointNav", nav);
            return;
        }
        nav.setBoolean("Enabled", true);
        nav.setString("State", "NAVIGATE");
        nav.setString("PendingWaypointId", "");
        nav.setString("CurrentWaypointId", wp.waypointId == null ? "" : wp.waypointId);
        nav.setInteger("CurrentWaypointX", nearest.xCoord);
        nav.setInteger("CurrentWaypointY", nearest.yCoord);
        nav.setInteger("CurrentWaypointZ", nearest.zCoord);
        nav.setString("NextWaypointId", wp.nextWaypointId == null ? "" : wp.nextWaypointId);
        nav.setInteger("HoldCountdownTick", Math.max(1, wp.patrolTimeTick));
        nav.setDouble("Radius", Math.max(1.0F, wp.waypointRadius));
        nav.setDouble("Height", Math.max(1.0F, wp.waypointHeight));
        nav.setBoolean("IsTerminator", wp.isTerminator);
        nav.setString("TerminateAction", wp.terminateAction == null ? "free" : wp.terminateAction);
        nav.setBoolean("TerminatorAfterHold", wp.terminatorAfterHold);
        aircraft.getEntityData().setTag("MCHWaypointNav", nav);
        MCH_WaypointNavDebug.trace(this.worldObj, null, "AttachInitialWaypoint success: acId=%d current=%s next=%s at=%d,%d,%d",
            aircraft.getEntityId(), wp.waypointId, wp.nextWaypointId, nearest.xCoord, nearest.yCoord, nearest.zCoord);
    }

    private void trackVehicle(MCH_EntityAircraft aircraft) {
        if (aircraft == null) {
            this.clearTrackedVehicle();
            return;
        }
        this.trackedVehicleEntityId = aircraft.getEntityId();
        if (aircraft.getUniqueID() != null) {
            this.trackedVehicleUuidMost = aircraft.getUniqueID().getMostSignificantBits();
            this.trackedVehicleUuidLeast = aircraft.getUniqueID().getLeastSignificantBits();
        }
    }

    private void clearTrackedVehicle() {
        this.trackedVehicleEntityId = -1;
        this.trackedVehicleUuidMost = 0L;
        this.trackedVehicleUuidLeast = 0L;
    }

    private boolean isTrackedVehicleAlive() {
        Entity byId = this.worldObj.getEntityByID(this.trackedVehicleEntityId);
        if (byId instanceof MCH_EntityAircraft) {
            return !byId.isDead;
        }
        if (this.trackedVehicleUuidMost == 0L && this.trackedVehicleUuidLeast == 0L) {
            return false;
        }
        if (this.worldObj.loadedEntityList == null || this.worldObj.loadedEntityList.isEmpty()) {
            return false;
        }
        for (Object obj : this.worldObj.loadedEntityList) {
            if (!(obj instanceof MCH_EntityAircraft)) {
                continue;
            }
            MCH_EntityAircraft ac = (MCH_EntityAircraft) obj;
            if (ac.isDead || ac.getUniqueID() == null) {
                continue;
            }
            if (ac.getUniqueID().getMostSignificantBits() == this.trackedVehicleUuidMost
                    && ac.getUniqueID().getLeastSignificantBits() == this.trackedVehicleUuidLeast) {
                this.trackedVehicleEntityId = ac.getEntityId();
                return true;
            }
        }
        return false;
    }

    private boolean isGlobalGunnerVehicleLockActive(MCH_BlockInfo info) {
        if (info == null || info.globalGunnerVehicleCountLimit <= 0 || info.globalGunnerVehicleCountRadius <= 0.0F) {
            this.lastGlobalGunnerVehicleCount = -1;
            this.lastGlobalGunnerVehicleLock = false;
            return false;
        }
        int count = this.countGunnerDrivenVehiclesInRadius(info.globalGunnerVehicleCountRadius);
        this.lastGlobalGunnerVehicleCount = count;
        this.lastGlobalGunnerVehicleLock = count >= info.globalGunnerVehicleCountLimit;
        return this.lastGlobalGunnerVehicleLock;
    }

    private int countGunnerDrivenVehiclesInRadius(float radius) {
        double r = Math.max(1.0D, radius);
        AxisAlignedBB aabb = AxisAlignedBB.getBoundingBox(
            this.xCoord + 0.5D - r, this.yCoord + 0.5D - r, this.zCoord + 0.5D - r,
            this.xCoord + 0.5D + r, this.yCoord + 0.5D + r, this.zCoord + 0.5D + r
        );
        List<MCH_EntityAircraft> vehicles = this.worldObj.getEntitiesWithinAABB(MCH_EntityAircraft.class, aabb);
        int count = 0;
        for (MCH_EntityAircraft ac : vehicles) {
            if (ac == null || ac.isDead) {
                continue;
            }
            if (this.hasAliveGunnerDriver(ac)) {
                ++count;
            }
        }
        return count;
    }

    private boolean hasAliveGunnerDriver(MCH_EntityAircraft aircraft) {
        if (aircraft.riddenByEntity instanceof MCH_EntityGunner && !aircraft.riddenByEntity.isDead) {
            return true;
        }
        MCH_EntitySeat[] seats = aircraft.getSeats();
        if (seats == null) {
            return false;
        }
        for (MCH_EntitySeat seat : seats) {
            if (seat == null || seat.isDead || seat.riddenByEntity == null || seat.riddenByEntity.isDead) {
                continue;
            }
            if (seat.riddenByEntity instanceof MCH_EntityGunner) {
                return true;
            }
        }
        return false;
    }

    private void setVisualState(int state, int syncTick) {
        if (this.visualState == state) {
            return;
        }
        this.visualState = state;
        if (this.worldObj != null && !this.worldObj.isRemote) {
            this.worldObj.setBlockMetadataWithNotify(this.xCoord, this.yCoord, this.zCoord, state, 2);
        }
        this.markDirty();
    }

    public void writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setString("BlockInfoName", this.blockInfoName);
        nbt.setLong("NextCheckTick", this.nextCheckTick);
        nbt.setLong("CooldownEndTick", this.cooldownEndTick);
        nbt.setBoolean("SpawnedOnce", this.spawnedOnce);
        nbt.setInteger("VisualState", this.visualState);
        nbt.setBoolean("WaitingVehicleDestroyed", this.waitingVehicleDestroyed);
        nbt.setInteger("TrackedVehicleEntityId", this.trackedVehicleEntityId);
        nbt.setLong("TrackedVehicleUuidMost", this.trackedVehicleUuidMost);
        nbt.setLong("TrackedVehicleUuidLeast", this.trackedVehicleUuidLeast);
    }

    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        this.blockInfoName = nbt.getString("BlockInfoName");
        this.nextCheckTick = nbt.getLong("NextCheckTick");
        this.cooldownEndTick = nbt.getLong("CooldownEndTick");
        this.spawnedOnce = nbt.getBoolean("SpawnedOnce");
        this.visualState = nbt.getInteger("VisualState");
        this.waitingVehicleDestroyed = nbt.getBoolean("WaitingVehicleDestroyed");
        this.trackedVehicleEntityId = nbt.getInteger("TrackedVehicleEntityId");
        this.trackedVehicleUuidMost = nbt.getLong("TrackedVehicleUuidMost");
        this.trackedVehicleUuidLeast = nbt.getLong("TrackedVehicleUuidLeast");
    }

    public void validate() {
        super.validate();
        MCH_BlockInfo info = this.getBlockInfo();
        this.updateWaypointRegistry(info);
    }

    public void invalidate() {
        this.removeWaypointRegistry();
        super.invalidate();
    }

    public void onChunkUnload() {
        this.removeWaypointRegistry();
        super.onChunkUnload();
    }

    public MCH_BlockInfo getBlockInfo() {
        return MCH_BlockInfoManager.get(this.blockInfoName);
    }

    public String getDebugStatusLine() {
        MCH_BlockInfo info = this.getBlockInfo();
        long now = this.worldObj != null ? this.worldObj.getTotalWorldTime() : 0L;
        long nextIn = Math.max(0L, this.nextCheckTick - now);
        long coolIn = Math.max(0L, this.cooldownEndTick - now);
        String infoName = info != null ? info.name : "<null>";
        int pool = info != null && info.vehiclePool != null ? info.vehiclePool.size() : 0;
        boolean enableSpawner = info != null && info.enableSpawner;
        String state = this.visualState == STATE_ACTIVE ? "ACTIVE" : (this.visualState == STATE_SLEEP ? "SLEEP" : "ERROR");
        return "state=" + state
            + " blockInfoName=" + this.blockInfoName
            + " cfg=" + infoName
            + " spawner=" + enableSpawner
            + " pool=" + pool
            + " spawnedOnce=" + this.spawnedOnce
            + " waitingVehicleDestroyed=" + this.waitingVehicleDestroyed
            + " freeze=" + MCH_ServerSettings.freezeConfigSpawner
            + " nextCheckIn=" + nextIn
            + " cooldownIn=" + coolIn
            + " trackedId=" + this.trackedVehicleEntityId
            + " globalGvLock=" + this.lastGlobalGunnerVehicleLock
            + " globalGvCount=" + this.lastGlobalGunnerVehicleCount
            + " globalGvLimit=" + (info != null ? info.globalGunnerVehicleCountLimit : 0)
            + " globalGvRadius=" + (info != null ? info.globalGunnerVehicleCountRadius : 0.0F);
    }

    public String getBlockInfoName() {
        return this.blockInfoName;
    }

    public void setBlockInfoName(String name) {
        String next = name == null ? "" : name.trim();
        if (next.equals(this.blockInfoName)) {
            return;
        }
        this.removeWaypointRegistry();
        this.blockInfoName = next;
        MCH_BlockInfo info = this.getBlockInfo();
        this.updateWaypointRegistry(info);
        this.markDirty();
    }

    public String getWaypointId() {
        MCH_BlockInfo info = this.getBlockInfo();
        return info == null || info.waypointId == null ? "" : info.waypointId.trim().toLowerCase();
    }

    public String getNextWaypointId() {
        MCH_BlockInfo info = this.getBlockInfo();
        return info == null || info.nextWaypointId == null ? "" : info.nextWaypointId.trim().toLowerCase();
    }

    public boolean isWaypointMarker() {
        return this.isWaypointMarker(this.getBlockInfo());
    }

    public String getWaypointLabel() {
        if (!this.isWaypointMarker()) {
            return "";
        }
        String cur = this.getWaypointId();
        String next = this.getNextWaypointId();
        if (cur.isEmpty()) {
            return "";
        }
        if (next.isEmpty()) {
            next = "<END>";
        }
        return cur + "->" + next;
    }

    private boolean isWaypointMarker(MCH_BlockInfo info) {
        return info != null && info.enableWaypoint && info.waypointId != null && !info.waypointId.trim().isEmpty();
    }

    private void updateWaypointRegistry(MCH_BlockInfo info) {
        if (this.worldObj == null || this.worldObj.isRemote) {
            return;
        }
        this.removeWaypointRegistry();
        if (!this.isWaypointMarker(info)) {
            return;
        }
        int dim = this.worldObj.provider.dimensionId;
        String id = info.waypointId.trim().toLowerCase();
        Map<String, List<MCH_ConfigSpawnerTileEntity>> byId = WAYPOINT_REGISTRY.get(dim);
        if (byId == null) {
            byId = new HashMap<String, List<MCH_ConfigSpawnerTileEntity>>();
            WAYPOINT_REGISTRY.put(dim, byId);
        }
        List<MCH_ConfigSpawnerTileEntity> list = byId.get(id);
        if (list == null) {
            list = new ArrayList<MCH_ConfigSpawnerTileEntity>();
            byId.put(id, list);
        }
        if (!list.contains(this)) {
            list.add(this);
        }
    }

    private void removeWaypointRegistry() {
        if (this.worldObj == null || this.worldObj.isRemote) {
            return;
        }
        int dim = this.worldObj.provider.dimensionId;
        Map<String, List<MCH_ConfigSpawnerTileEntity>> byId = WAYPOINT_REGISTRY.get(dim);
        if (byId == null || byId.isEmpty()) {
            return;
        }
        List<String> removeKeys = new ArrayList<String>();
        for (Map.Entry<String, List<MCH_ConfigSpawnerTileEntity>> e : byId.entrySet()) {
            List<MCH_ConfigSpawnerTileEntity> list = e.getValue();
            if (list == null) {
                removeKeys.add(e.getKey());
                continue;
            }
            list.remove(this);
            if (list.isEmpty()) {
                removeKeys.add(e.getKey());
            }
        }
        for (String k : removeKeys) {
            byId.remove(k);
        }
        if (byId.isEmpty()) {
            WAYPOINT_REGISTRY.remove(dim);
        }
    }

    public static MCH_ConfigSpawnerTileEntity resolveNearestWaypoint(net.minecraft.world.World world, String waypointId, double x, double y, double z) {
        if (world == null || waypointId == null || waypointId.trim().isEmpty()) {
            return null;
        }
        Map<String, List<MCH_ConfigSpawnerTileEntity>> byId = WAYPOINT_REGISTRY.get(world.provider.dimensionId);
        if (byId == null || byId.isEmpty()) {
            return null;
        }
        List<MCH_ConfigSpawnerTileEntity> list = byId.get(waypointId.trim().toLowerCase());
        if (list == null || list.isEmpty()) {
            return null;
        }
        MCH_ConfigSpawnerTileEntity nearest = null;
        double best = Double.MAX_VALUE;
        for (MCH_ConfigSpawnerTileEntity tile : list) {
            if (tile == null || tile.isInvalid() || tile.worldObj != world) {
                continue;
            }
            MCH_BlockInfo info = tile.getBlockInfo();
            if (!tile.isWaypointMarker(info)) {
                continue;
            }
            double dx = tile.xCoord + 0.5D - x;
            double dy = tile.yCoord + 0.5D - y;
            double dz = tile.zCoord + 0.5D - z;
            double distSq = dx * dx + dy * dy + dz * dz;
            if (distSq < best) {
                best = distSq;
                nearest = tile;
            }
        }
        if (nearest == null) {
            MCH_WaypointNavDebug.trace(world, null, "ResolveNearest miss: waypoint=%s candidates=%d pos=%.1f,%.1f,%.1f",
                waypointId, list.size(), x, y, z);
        }
        return nearest;
    }
}
