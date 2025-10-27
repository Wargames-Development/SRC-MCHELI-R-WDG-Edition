package mcheli;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import cpw.mods.fml.common.network.internal.FMLProxyPacket;
import mcheli.wrapper.W_Reflection;
import net.minecraft.network.NetworkManager;

import java.util.*;

public class MCH_ServerTickHandler {

    Map<String, Integer> rcvMap = new HashMap<>();
    Map<String, Integer> sndMap = new HashMap<>();
    int sndPacketNum = 0;
    int rcvPacketNum = 0;
    int tick;

    @SubscribeEvent
    public void onServerTickEvent(TickEvent.ServerTickEvent event) {
        if (event.phase == Phase.START) {
        }
        if (event.phase == Phase.END) {
        }
    }

    private void onServerTickPre() {
        ++this.tick;
        List list = W_Reflection.getNetworkManagers();
        if (list != null) {
            for (Object o : list) {
                Queue queue = W_Reflection.getReceivedPacketsQueue((NetworkManager) o);
                if (queue != null) {
                    this.putMap(this.rcvMap, queue.iterator());
                    this.rcvPacketNum += queue.size();
                }
                queue = W_Reflection.getSendPacketsQueue((NetworkManager) o);
                if (queue != null) {
                    this.putMap(this.sndMap, queue.iterator());
                    this.sndPacketNum += queue.size();
                }
            }
        }

        if (this.tick >= 20) {
            this.tick = 0;
            this.rcvPacketNum = this.sndPacketNum = 0;
            this.rcvMap.clear();
            this.sndMap.clear();
        }

    }

    public void putMap(Map<String, Integer> map, Iterator iterator) {
        while (iterator.hasNext()) {
            Object o = iterator.next();
            String key = o.getClass().getName();
            if (key.startsWith("net.minecraft.")) {
                key = "Minecraft";
            } else if (o instanceof FMLProxyPacket) {
                FMLProxyPacket p = (FMLProxyPacket) o;
                key = p.channel();
            } else {
                key = "Unknown!";
            }
            if (map.containsKey(key)) {
                map.put(key, 1 + map.get(key));
            } else {
                map.put(key, 1);
            }
        }

    }

    private void onServerTickPost() {
    }
}
