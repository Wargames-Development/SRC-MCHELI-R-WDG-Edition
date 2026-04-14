package mcheli.block;

import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.aircraft.MCH_ItemAircraft;
import mcheli.aircraft.MCH_EntitySeat;
import mcheli.helicopter.MCH_HeliInfo;
import mcheli.helicopter.MCH_HeliInfoManager;
import mcheli.mob.MCH_EntityGunner;
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
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;

import java.util.List;

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

    public MCH_ConfigSpawnerTileEntity() {
    }

    public MCH_ConfigSpawnerTileEntity(String blockInfoName) {
        this.blockInfoName = blockInfoName;
    }

    public void updateEntity() {
        if (this.worldObj == null || this.worldObj.isRemote) {
            return;
        }
        MCH_BlockInfo info = MCH_BlockInfoManager.get(this.blockInfoName);
        if (info == null || !info.enableSpawner) {
            this.setVisualState(STATE_ERROR, 2);
            return;
        }
        if (info.vehiclePool == null || info.vehiclePool.isEmpty()) {
            this.setVisualState(STATE_ERROR, 2);
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
        MCH_EntityGunner gunner = new MCH_EntityGunner(this.worldObj, aircraft.posX, aircraft.posY, aircraft.posZ);
        gunner.rotationYaw = info.gunnerYaw;
        gunner.rotationPitch = info.gunnerPitch;
        gunner.isCreative = true;
        gunner.ownerUUID = "";
        int targetType = info.gunnerTargetType;
        if (info.gunnerMode != null && info.gunnerMode.equalsIgnoreCase("faction")) {
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
        if (info.gunnerFactionId == null || info.gunnerFactionId.trim().isEmpty()) {
            return null;
        }
        String teamId = info.gunnerFactionId.trim();
        Scoreboard scoreboard = this.worldObj.getScoreboard();
        ScorePlayerTeam team = (ScorePlayerTeam) scoreboard.getTeam(teamId);
        if (team == null && info.autoCreateFaction) {
            team = scoreboard.createTeam(teamId);
            if (team != null && info.gunnerFactionName != null && !info.gunnerFactionName.trim().isEmpty()) {
                team.setTeamName(info.gunnerFactionName.trim());
            }
        }
        return team != null ? team.getRegisteredName() : null;
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
}
