package mcheli.economy;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import mcheli.MCH_Config;
import mcheli.MCH_MOD;
import mcheli.aircraft.MCH_AircraftInfo;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.aircraft.MCH_EntitySeat;
import mcheli.event.AircraftDamageEvent;
import mcheli.event.AircraftDestoryEvent;
import mcheli.helicopter.MCH_HeliInfoManager;
import mcheli.plane.MCP_PlaneInfoManager;
import mcheli.tank.MCH_TankInfoManager;
import mcheli.vehicle.MCH_VehicleInfoManager;
import mcheli.weapon.MCH_EntityBaseBullet;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.boss.EntityWither;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;

import java.util.Map;
import java.util.Random;

public class MCH_EconomyEventHandler {

    private static final String ROOT_TAG = "MCH_Economy";

    public MCH_EconomyEventHandler() {
        MCH_EconomyRewardConfig.ensureLoaded();
    }

    private boolean isTechTreeGameplayEnabled() {
        return MCH_MOD.config != null && MCH_Config.EnableTechTreeGameplay.prmBool;
    }

    @SubscribeEvent
    public void onPlayerJoinSyncEconomy(EntityJoinWorldEvent event) {
        if (!isTechTreeGameplayEnabled()) {
            return;
        }
        if (event == null || event.world == null || event.world.isRemote) {
            return;
        }
        if (!(event.entity instanceof EntityPlayerMP)) {
            return;
        }
        MCH_EconomyService.syncToClient((EntityPlayerMP) event.entity);
    }

    @SubscribeEvent
    public void onPlayerCloneKeepEconomy(PlayerEvent.Clone event) {
        if (!isTechTreeGameplayEnabled()) {
            return;
        }
        if (MCH_Config.EconomyKeepOnDeath != null && !MCH_Config.EconomyKeepOnDeath.prmBool) {
            return;
        }
        if (event == null || event.entityPlayer == null || event.original == null) {
            return;
        }
        if (event.entityPlayer.worldObj == null || event.entityPlayer.worldObj.isRemote) {
            return;
        }
        NBTTagCompound oldRoot = event.original.getEntityData();
        if (oldRoot == null || !oldRoot.hasKey(ROOT_TAG)) {
            return;
        }
        NBTTagCompound oldEco = oldRoot.getCompoundTag(ROOT_TAG);
        event.entityPlayer.getEntityData().setTag(ROOT_TAG, oldEco.copy());
        if (event.entityPlayer instanceof EntityPlayerMP) {
            MCH_EconomyService.syncToClient((EntityPlayerMP) event.entityPlayer);
        }
    }

    @SubscribeEvent
    public void onAircraftDestroyReward(AircraftDestoryEvent event) {
        if (!isTechTreeGameplayEnabled()) {
            return;
        }
        if (event == null || event.getAttackerName() == null || event.getAttackerName().isEmpty()) {
            return;
        }

        EntityPlayerMP player = findOnlinePlayerByName(event.getAttackerName());
        if (player == null) {
            return;
        }

        Reward reward = getVehicleDestroyReward(event.getVehicleName(), player.worldObj.rand);
        if (reward.isZero()) {
            return;
        }
        String damageKey = getVehicleDamageTrackerKey(event.getVehicleName());
        if (!MCH_EconomyService.passDamageShare(player, damageKey, true)) {
            return;
        }
        long tick = player.worldObj != null ? player.worldObj.getTotalWorldTime() : 0L;
        MCH_EconomyService.GrantResult result = MCH_EconomyService.grantWithPolicy(player, reward.sl, reward.ge, reward.rp, "Vehicle Destroy", "vehType:" + normalizeTypeName(event.getVehicleName()), tick, false);
        if (!result.isZero()) {
            MCH_PacketNotifyEconomyGainToast.sendToPlayer(player, MCH_PacketNotifyEconomyGainToast.TYPE_VEHICLE_DESTROY, result.sl, result.ge, result.rp);
        }
    }

    @SubscribeEvent
    public void onAircraftDamageRecord(AircraftDamageEvent event) {
        if (!isTechTreeGameplayEnabled()) {
            return;
        }
        if (event == null || event.getAttackerName() == null || event.getAttackerName().isEmpty()) {
            return;
        }
        EntityPlayerMP player = findOnlinePlayerByName(event.getAttackerName());
        if (player == null || player.worldObj == null || player.worldObj.isRemote) {
            return;
        }
        long tick = player.worldObj.getTotalWorldTime();
        MCH_EconomyService.recordDamage(player, getVehicleDamageTrackerKey(event.getVehicleName()), event.getDamage(), event.getMaxDamage(), tick);
    }

    @SubscribeEvent
    public void onMobHurtRecord(LivingHurtEvent event) {
        if (!isTechTreeGameplayEnabled()) {
            return;
        }
        if (event == null || event.entityLiving == null || event.entityLiving.worldObj == null || event.entityLiving.worldObj.isRemote) {
            return;
        }
        if (!(event.entityLiving instanceof IMob)) {
            return;
        }
        EntityPlayerMP attacker = resolvePlayerFromAttacker(event.source != null ? event.source.getEntity() : null, 0);
        if (attacker == null) {
            return;
        }
        float maxHealth = event.entityLiving.getMaxHealth();
        long tick = event.entityLiving.worldObj.getTotalWorldTime();
        MCH_EconomyService.recordDamage(attacker, getMobDamageTrackerKey(event.entityLiving), event.ammount, maxHealth, tick);
    }

    @SubscribeEvent
    public void onMobKillReward(LivingDeathEvent event) {
        if (!isTechTreeGameplayEnabled()) {
            return;
        }
        if (event == null || event.entityLiving == null || event.entityLiving.worldObj == null || event.entityLiving.worldObj.isRemote) {
            return;
        }
        if (!(event.entityLiving instanceof IMob)) {
            return;
        }

        EntityPlayerMP killer = resolvePlayerFromAttacker(event.source != null ? event.source.getEntity() : null, 0);
        if (killer == null) {
            return;
        }
        if (event.entityLiving == killer) {
            return;
        }
        String mobDamageKey = getMobDamageTrackerKey(event.entityLiving);
        if (!MCH_EconomyService.passDamageShare(killer, mobDamageKey, true)) {
            return;
        }

        Reward reward = getMobReward(event.entityLiving);
        if (reward.isZero()) {
            return;
        }
        long tick = event.entityLiving.worldObj.getTotalWorldTime();
        String repeatKey = "mobType:" + event.entityLiving.getClass().getSimpleName();
        MCH_EconomyService.GrantResult result = MCH_EconomyService.grantWithPolicy(killer, reward.sl, reward.ge, reward.rp, "Mob Kill", repeatKey, tick, false);
        if (!result.isZero()) {
            MCH_PacketNotifyEconomyGainToast.sendToPlayer(killer, MCH_PacketNotifyEconomyGainToast.TYPE_MOB_KILL, result.sl, result.ge, result.rp);
        }
    }

    private Reward getMobReward(Entity target) {
        String id = EntityList.getEntityString(target);
        Reward fromConfig = toReward(MCH_EconomyRewardConfig.getMobReward(id));
        if (fromConfig != null) {
            return fromConfig;
        }
        fromConfig = toReward(MCH_EconomyRewardConfig.getMobReward(target.getClass().getSimpleName()));
        if (fromConfig != null) {
            return fromConfig;
        }
        if (target instanceof EntityDragon || target instanceof EntityWither) {
            return toReward(MCH_EconomyRewardConfig.getDefaultBossReward());
        }
        return toReward(MCH_EconomyRewardConfig.getDefaultMobReward());
    }

    private Reward getVehicleDestroyReward(String vehicleName, Random rand) {
        Reward fromConfig = toReward(MCH_EconomyRewardConfig.getVehicleReward(vehicleName));
        if (fromConfig != null && !fromConfig.isZero()) {
            return fromConfig;
        }
        MCH_AircraftInfo info = findAircraftInfoByDisplayName(vehicleName);
        if (info == null) {
            return Reward.ZERO;
        }
        fromConfig = toReward(MCH_EconomyRewardConfig.getVehicleReward(info.name));
        if (fromConfig != null && !fromConfig.isZero()) {
            return fromConfig;
        }
        fromConfig = toReward(MCH_EconomyRewardConfig.getVehicleReward(info.displayName));
        if (fromConfig != null && !fromConfig.isZero()) {
            return fromConfig;
        }

        int sl = rollRewardRange(info.destroyRewardSLMin, info.destroyRewardSLMax, rand);
        int ge = rollRewardRange(info.destroyRewardGEMin, info.destroyRewardGEMax, rand);
        int rp = rollRewardRange(info.destroyRewardRPMin, info.destroyRewardRPMax, rand);
        return new Reward(sl, ge, rp);
    }

    private int rollRewardRange(int min, int max, Random rand) {
        if (min < 0 || max < 0) {
            return 0;
        }
        if (max < min) {
            int t = min;
            min = max;
            max = t;
        }
        if (min == max) {
            return min;
        }
        return min + rand.nextInt(max - min + 1);
    }

    private EntityPlayerMP resolvePlayerFromAttacker(Entity attacker, int depth) {
        if (attacker == null || depth > 4) {
            return null;
        }
        if (attacker instanceof EntityPlayerMP) {
            return (EntityPlayerMP) attacker;
        }
        if (attacker instanceof EntityPlayer) {
            return findOnlinePlayerByName(attacker.getCommandSenderName());
        }
        if (attacker instanceof MCH_EntityBaseBullet) {
            MCH_EntityBaseBullet bullet = (MCH_EntityBaseBullet) attacker;
            EntityPlayerMP fromShooter = resolvePlayerFromAttacker(bullet.shootingEntity, depth + 1);
            if (fromShooter != null) {
                return fromShooter;
            }
            return resolvePlayerFromAttacker(bullet.shootingAircraft, depth + 1);
        }
        if (attacker instanceof MCH_EntitySeat) {
            MCH_EntityAircraft parent = ((MCH_EntitySeat) attacker).getParent();
            return resolvePlayerFromAttacker(parent, depth + 1);
        }
        if (attacker instanceof MCH_EntityAircraft) {
            EntityPlayer rider = ((MCH_EntityAircraft) attacker).getFirstMountPlayer();
            if (rider instanceof EntityPlayerMP) {
                return (EntityPlayerMP) rider;
            }
            if (rider != null) {
                return findOnlinePlayerByName(rider.getCommandSenderName());
            }
        }
        if (attacker.ridingEntity instanceof MCH_EntitySeat || attacker.ridingEntity instanceof MCH_EntityAircraft) {
            return resolvePlayerFromAttacker(attacker.ridingEntity, depth + 1);
        }
        return null;
    }

    private EntityPlayerMP findOnlinePlayerByName(String name) {
        if (name == null || name.isEmpty() || MinecraftServer.getServer() == null) {
            return null;
        }
        for (Object obj : MinecraftServer.getServer().getConfigurationManager().playerEntityList) {
            if (obj instanceof EntityPlayerMP) {
                EntityPlayerMP p = (EntityPlayerMP) obj;
                if (name.equalsIgnoreCase(p.getCommandSenderName())) {
                    return p;
                }
            }
        }
        return null;
    }

    private MCH_AircraftInfo findAircraftInfoByDisplayName(String vehicleName) {
        if (vehicleName == null || vehicleName.isEmpty()) {
            return null;
        }

        MCH_AircraftInfo info = findAircraftInfoInMap(MCP_PlaneInfoManager.map, vehicleName);
        if (info != null) return info;
        info = findAircraftInfoInMap(MCH_HeliInfoManager.map, vehicleName);
        if (info != null) return info;
        info = findAircraftInfoInMap(MCH_TankInfoManager.map, vehicleName);
        if (info != null) return info;
        return findAircraftInfoInMap(MCH_VehicleInfoManager.map, vehicleName);
    }

    private MCH_AircraftInfo findAircraftInfoInMap(Map map, String vehicleName) {
        for (Object obj : map.values()) {
            if (!(obj instanceof MCH_AircraftInfo)) {
                continue;
            }
            MCH_AircraftInfo info = (MCH_AircraftInfo) obj;
            if (vehicleName.equalsIgnoreCase(info.name)) {
                return info;
            }
            if (info.displayName != null && vehicleName.equalsIgnoreCase(info.displayName)) {
                return info;
            }
            if (info.displayNameLang != null) {
                Object enUS = info.displayNameLang.get("en_US");
                if (enUS != null && vehicleName.equalsIgnoreCase(enUS.toString())) {
                    return info;
                }
            }
        }
        return null;
    }

    private String getMobDamageTrackerKey(EntityLivingBase entity) {
        return "mob@" + entity.worldObj.provider.dimensionId + ":" + entity.getEntityId();
    }

    private String getVehicleDamageTrackerKey(String vehicleName) {
        return "veh@" + normalizeTypeName(vehicleName);
    }

    private String normalizeTypeName(String name) {
        return name == null ? "" : name.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private Reward toReward(MCH_EconomyRewardConfig.Reward reward) {
        if (reward == null) {
            return null;
        }
        return new Reward(reward.sl, reward.ge, reward.rp);
    }

    private static class Reward {
        private static final Reward ZERO = new Reward(0, 0, 0);
        private final int sl;
        private final int ge;
        private final int rp;

        private Reward(int sl, int ge, int rp) {
            this.sl = Math.max(0, sl);
            this.ge = Math.max(0, ge);
            this.rp = Math.max(0, rp);
        }

        private boolean isZero() {
            return this.sl <= 0 && this.ge <= 0 && this.rp <= 0;
        }
    }
}
