package mcheli;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.flare.MCH_EntityChaff;
import mcheli.helicopter.MCH_EntityHeli;
import mcheli.network.packets.PacketEntityInfoSync;
import mcheli.plane.MCP_EntityPlane;
import mcheli.weapon.MCH_IEntityLockChecker;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityTracker;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MCH_EntityInfoManager {

    // 服务器侧仅用于收集/去重，不再依赖“删除发包”
    public static final Map<Integer, MCH_EntityInfo> serverEntities = new ConcurrentHashMap<>();

    private int tickCounter;
    private long snapshotSeq = 0L; // 递增的全局快照序号

    public MCH_EntityInfoManager() {
        FMLCommonHandler.instance().bus().register(this);
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            tickCounter++;
            snapshotSeq++; // 每个服务端 Tick 递增一次
            serverTick();
        }
    }

    public void serverTick() {
        // 收集应跟踪的实体，写入（覆盖）快照
        for (WorldServer world : MinecraftServer.getServer().worldServers) {
            @SuppressWarnings("unchecked")
            List<Entity> loaded = world.loadedEntityList;
            for (Entity entity : loaded) {
                if (shouldTrack(world, entity)) {
                    serverEntities.put(entity.getEntityId(), MCH_EntityInfo.createInfo(entity));
                }
            }
        }

        // 仅做本地内存清理，避免服务端集合膨胀（不再对客户端发 REMOVE）
        if (tickCounter % 10 == 0) {
            Iterator<Map.Entry<Integer, MCH_EntityInfo>> it = serverEntities.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Integer, MCH_EntityInfo> entry = it.next();
                MCH_EntityInfo info = entry.getValue();
                Entity entity = serverGetEntity(info.entityId);
                // 若实体确实不存在/死亡，或者长时间未更新，直接从服务端集合剔除
                if (entity == null || entity.isDead || System.currentTimeMillis() - info.lastUpdateTime > 5_000L) {
                    it.remove();
                }
            }
        }

        // 发送本帧（本 tick）的全量权威快照（带序号）
        List<MCH_EntityInfo> list = new ArrayList<>(serverEntities.values());
        MCH_MOD.getPacketHandler().sendToAll(new PacketEntityInfoSync(list, snapshotSeq));
    }

    private Entity serverGetEntity(int entityId) {
        for (WorldServer world : MinecraftServer.getServer().worldServers) {
            @SuppressWarnings("unchecked")
            List<Entity> loaded = world.loadedEntityList;
            for (Entity entity : loaded) {
                if (entity.getEntityId() == entityId) {
                    return entity;
                }
            }
        }
        return null;
    }

    private boolean shouldTrack(WorldServer w, Entity entity) {
        if (entity.isDead) {
            return false;
        }
        if (MCH_FMURUtil.isSoldier(entity) || entity instanceof EntityPlayer || entity instanceof MCH_IEntityLockChecker) {
            if (entity instanceof MCP_EntityPlane || entity instanceof MCH_EntityHeli || entity instanceof MCH_EntityChaff) {
                if (!isShip(entity) && entity.posY - w.getHeightValue((int) entity.posX, (int) entity.posZ) < 30) {
                    return false;
                }
                if (!isShip(entity) && entity.motionX * entity.motionX + entity.motionY * entity.motionY + entity.motionZ * entity.motionZ < 0.5 * 0.5) {
                    return false;
                }
            }
            if (entity instanceof MCH_EntityAircraft) {
                MCH_EntityAircraft aircraft = (MCH_EntityAircraft) entity;
                if (aircraft.isECMJammerUsing()) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    private boolean isShip(Entity entity) {
        if (entity instanceof MCP_EntityPlane) {
            return ((MCP_EntityPlane) entity).getAcInfo() != null && ((MCP_EntityPlane) entity).getAcInfo().isFloat;
        }
        return false;
    }
}
