package mcheli.mob;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.village.Village;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public final class MCH_TechNpcVillageSpawner {

    private static final MCH_TechNpcVillageSpawner INSTANCE = new MCH_TechNpcVillageSpawner();
    private final Map<String, Long> villageNextSpawnTick = new HashMap<String, Long>();
    private int serverTickCounter = 0;

    private MCH_TechNpcVillageSpawner() {
    }

    public static MCH_TechNpcVillageSpawner getInstance() {
        return INSTANCE;
    }

    public void serverTick() {
        if (!MCH_TechNpcConfig.isVillageSpawnEnabled()) {
            return;
        }
        MinecraftServer server = MinecraftServer.getServer();
        if (server == null || server.worldServers == null) {
            return;
        }
        serverTickCounter++;
        // Low-frequency scan: every 2 seconds (40 ticks)
        if (serverTickCounter % 40 != 0) {
            return;
        }
        for (WorldServer world : server.worldServers) {
            trySpawnInWorld(world);
        }
    }

    private void trySpawnInWorld(World world) {
        if (world == null || world.isRemote || world.playerEntities == null || world.playerEntities.isEmpty()) {
            return;
        }
        if (world.villageCollectionObj == null) {
            return;
        }
        List villages = world.villageCollectionObj.getVillageList();
        if (villages == null || villages.isEmpty()) {
            return;
        }

        int minPopulation = MCH_TechNpcConfig.getMinVillagePopulation();
        int spawnWeight = MCH_TechNpcConfig.getVillageWeight();
        int cooldownTick = MCH_TechNpcConfig.getSpawnCooldownTick();
        int maxPerVillage = MCH_TechNpcConfig.getMaxPerVillage();
        int minPlayerDistance = MCH_TechNpcConfig.getMinPlayerDistance();
        int maxPlayerDistance = MCH_TechNpcConfig.getMaxPlayerDistance();
        long nowTick = world.getTotalWorldTime();

        for (Object obj : villages) {
            if (!(obj instanceof Village)) {
                continue;
            }
            Village village = (Village) obj;
            if (village.getNumVillagers() < minPopulation) {
                continue;
            }
            int cx = village.getCenter().posX;
            int cy = village.getCenter().posY;
            int cz = village.getCenter().posZ;
            int radius = Math.max(8, village.getVillageRadius() + 4);
            String villageKey = world.provider.dimensionId + ":" + cx + ":" + cy + ":" + cz + ":" + radius;

            Long nextTick = villageNextSpawnTick.get(villageKey);
            if (nextTick != null && nowTick < nextTick.longValue()) {
                continue;
            }
            if (!isPlayerDistanceOk(world, cx, cy, cz, minPlayerDistance, maxPlayerDistance)) {
                continue;
            }
            if (countNpcInVillage(world, cx, cy, cz, radius) >= maxPerVillage) {
                villageNextSpawnTick.put(villageKey, nowTick + cooldownTick);
                continue;
            }
            if (spawnWeight < 100 && world.rand.nextInt(100) >= spawnWeight) {
                villageNextSpawnTick.put(villageKey, nowTick + cooldownTick);
                continue;
            }

            if (spawnOneNpc(world, cx, cy, cz, radius)) {
                villageNextSpawnTick.put(villageKey, nowTick + cooldownTick);
            }
        }

        // Avoid unbounded growth if villages disappear or world changes.
        if (villageNextSpawnTick.size() > 2048) {
            long expireBefore = nowTick - 120000L;
            Iterator<Map.Entry<String, Long>> it = villageNextSpawnTick.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, Long> e = it.next();
                if (e.getValue() == null || e.getValue().longValue() < expireBefore) {
                    it.remove();
                }
            }
        }
    }

    private boolean isPlayerDistanceOk(World world, int x, int y, int z, int minDist, int maxDist) {
        EntityPlayer nearest = world.getClosestPlayer(x + 0.5D, y + 0.5D, z + 0.5D, (double) maxDist);
        if (nearest == null) {
            return false;
        }
        if (minDist <= 0) {
            return true;
        }
        double distSq = nearest.getDistanceSq((double) x + 0.5D, (double) y + 0.5D, (double) z + 0.5D);
        return distSq >= (double) (minDist * minDist);
    }

    private int countNpcInVillage(World world, int x, int y, int z, int radius) {
        AxisAlignedBB box = AxisAlignedBB.getBoundingBox(
            (double) x - radius, (double) y - 24, (double) z - radius,
            (double) x + radius, (double) y + 24, (double) z + radius
        );
        List list = world.getEntitiesWithinAABB(MCH_EntityNPC.class, box);
        return list == null ? 0 : list.size();
    }

    private boolean spawnOneNpc(World world, int cx, int cy, int cz, int radius) {
        for (int i = 0; i < 8; i++) {
            double angle = world.rand.nextDouble() * Math.PI * 2.0D;
            double dist = (double) radius * (0.45D + world.rand.nextDouble() * 0.45D);
            int x = cx + (int) Math.round(Math.cos(angle) * dist);
            int z = cz + (int) Math.round(Math.sin(angle) * dist);
            int y = world.getTopSolidOrLiquidBlock(x, z);
            if (!canSpawnAt(world, x, y, z)) {
                continue;
            }
            MCH_EntityNPC npc = new MCH_EntityNPC(world);
            npc.func_110163_bv(); // Prevent natural despawn.
            npc.setLocationAndAngles((double) x + 0.5D, (double) y, (double) z + 0.5D, world.rand.nextFloat() * 360.0F, 0.0F);
            return world.spawnEntityInWorld(npc);
        }
        return false;
    }

    private boolean canSpawnAt(World world, int x, int y, int z) {
        if (y <= 1 || y >= 255) {
            return false;
        }
        if (!world.isAirBlock(x, y, z) || !world.isAirBlock(x, y + 1, z)) {
            return false;
        }
        Block ground = world.getBlock(x, y - 1, z);
        if (ground == null || ground.getMaterial() == null) {
            return false;
        }
        return ground.getMaterial().isSolid() && !ground.getMaterial().isLiquid();
    }
}
