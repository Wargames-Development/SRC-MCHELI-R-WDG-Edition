package mcheli;

import com.flansmod.api.FMUR_API;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import mcheli.flare.MCH_EntityChaff;
import mcheli.helicopter.MCH_EntityHeli;
import mcheli.network.packets.PacketEntityInfoSync;
import mcheli.plane.MCP_EntityPlane;
import mcheli.weapon.MCH_IEntityLockChecker;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MCH_EntityInfoManager {

    // Server side: used only for collecting and deduplicating entities.
    // No longer depends on “remove” packets being sent to clients.
    public static final Map<Integer, MCH_EntityInfo> serverEntities = new ConcurrentHashMap<>();

    private int tickCounter;
    private long snapshotSeq = 0L; // Incremental global snapshot sequence number

    public MCH_EntityInfoManager() {
        FMLCommonHandler.instance().bus().register(this);
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            tickCounter++;
            snapshotSeq++; // Increment sequence every server tick
            serverTick();
        }
    }

    public void serverTick() {
        // Collect and record all entities that should be tracked, updating the snapshot
        for (WorldServer world : MinecraftServer.getServer().worldServers) {
            @SuppressWarnings("unchecked")
            List<Entity> loaded = (List<Entity>) world.loadedEntityList;
            for (Entity entity : loaded) {
                if (shouldTrack(world, entity)) {
                    serverEntities.put(entity.getEntityId(), MCH_EntityInfo.createInfo(entity));
                }
            }
        }

        // Perform local memory cleanup only; no longer sends REMOVE packets to clients
        if (tickCounter % 10 == 0) {
            Iterator<Map.Entry<Integer, MCH_EntityInfo>> it = serverEntities.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Integer, MCH_EntityInfo> entry = it.next();
                MCH_EntityInfo info = entry.getValue();
                Entity entity = serverGetEntity(info.entityId);
                // Remove from the server map if the entity no longer exists, is dead,
                // or hasn't updated for more than 5 seconds.
                if (entity == null || entity.isDead || System.currentTimeMillis() - info.lastUpdateTime > 5_000L) {
                    it.remove();
                }
            }
        }

        // Send the authoritative full snapshot (with sequence number) for this tick to all clients
        List<MCH_EntityInfo> list = new ArrayList<>(serverEntities.values());
        MCH_MOD.getPacketHandler().sendToAll(new PacketEntityInfoSync(list, snapshotSeq));
    }

    private Entity serverGetEntity(int entityId) {
        for (WorldServer world : MinecraftServer.getServer().worldServers) {
            @SuppressWarnings("unchecked")
            List<Entity> loaded = (List<Entity>) world.loadedEntityList;
            for (Entity entity : loaded) {
                if (entity.getEntityId() == entityId) {
                    return entity;
                }
            }
        }
        return null;
    }

    private boolean shouldTrack(WorldServer w, Entity entity) {
        if(entity.isDead) {
            return false;
        }
        if (MCH_FMURUtil.isSoldier(entity) || entity instanceof EntityPlayer || entity instanceof MCH_IEntityLockChecker) {
            if (entity instanceof MCP_EntityPlane || entity instanceof MCH_EntityHeli || entity instanceof MCH_EntityChaff) {
                if (entity.posY - w.getHeightValue((int) entity.posX, (int) entity.posZ) < 30) {
                    return false;
                }
                if (entity.motionX * entity.motionX + entity.motionY * entity.motionY + entity.motionZ * entity.motionZ < 0.5 * 0.5) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }
}
