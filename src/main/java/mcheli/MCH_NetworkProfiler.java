package mcheli;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.weapon.MCH_EntityBaseBullet;
import mcheli.wrapper.W_Reflection;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetworkManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/** Server-side, command-controlled network and tick profiler. */
public final class MCH_NetworkProfiler {
    private static final int REPORT_TICKS = 100;
    private static final int MAX_LEGACY_PACKET_IDS = 128;
    private static final Object SESSION_LOCK = new Object();
    private static final MCH_NetworkProfiler INSTANCE = new MCH_NetworkProfiler();
    private static final Map<String, PacketMetric> LEGACY_S2C = new ConcurrentHashMap<String, PacketMetric>();
    private static final Map<String, PacketMetric> LEGACY_C2S = new ConcurrentHashMap<String, PacketMetric>();
    private static final Map<String, PacketMetric> MODERN_S2C = new ConcurrentHashMap<String, PacketMetric>();
    private static final Map<String, PacketMetric> MODERN_C2S = new ConcurrentHashMap<String, PacketMetric>();
    private static final List<Map<String, PacketMetric>> PACKET_GROUPS = Arrays.asList(
        LEGACY_S2C, LEGACY_C2S, MODERN_S2C, MODERN_C2S);
    private static final Map<Integer, String> LEGACY_PACKET_NAMES = new ConcurrentHashMap<Integer, String>();
    private static final double[] WINDOW_TICKS_MS = new double[REPORT_TICKS];
    private static final long[] TOTAL_TICK_HISTOGRAM = new long[1002];

    private static volatile boolean active;
    private static boolean registered;
    private static String sessionName = "";
    private static String summaryPath = "";
    private static String packetPath = "";
    private static PrintWriter summaryWriter;
    private static PrintWriter packetWriter;
    private static long startMillis;
    private static long windowStartMillis;
    private static long tickStartNanos;
    private static int windowTickCount;
    private static long totalTickCount;
    private static double totalTickMillis;
    private static double totalTickMaxMillis;
    private static long totalTicksOver50;
    private static long totalTicksOver100;
    private static long windowRecvQueueSum;
    private static long windowSendQueueSum;
    private static int windowRecvQueueMax;
    private static int windowSendQueueMax;
    private static long totalRecvQueueSum;
    private static long totalSendQueueSum;
    private static int totalRecvQueueMax;
    private static int totalSendQueueMax;
    private static long startGcCollections;
    private static long startGcMillis;
    private static long windowGcCollections;
    private static long windowGcMillis;

    private MCH_NetworkProfiler() {
    }

    public static boolean isActive() {
        return active;
    }

    public static String getSessionName() {
        return sessionName;
    }

    public static String getSummaryPath() {
        return summaryPath;
    }

    public static String getPacketPath() {
        return packetPath;
    }

    public static long getElapsedSeconds() {
        return active ? Math.max(0L, (System.currentTimeMillis() - startMillis) / 1000L) : 0L;
    }

    public static void start(String requestedName) {
        synchronized (SESSION_LOCK) {
            if (active) {
                throw new IllegalStateException("Network logging is already active as " + sessionName + ".");
            }
            if (requestedName == null || !requestedName.matches("[A-Za-z0-9_-]{1,32}")) {
                throw new IllegalArgumentException("Session name must be 1-32 letters, numbers, underscores, or hyphens.");
            }
            ensureRegistered();
            resetCounters();
            sessionName = requestedName;
            String stamp = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.ROOT).format(new Date());
            File dir = new File("logs");
            if (!dir.exists() && !dir.mkdirs()) {
                throw new IllegalStateException("Could not create logs directory.");
            }
            String prefix = "mcheli_netlog_" + sessionName + "_" + stamp;
            summaryPath = new File(dir, prefix + "_summary.csv").getPath();
            packetPath = new File(dir, prefix + "_packets.csv").getPath();
            try {
                summaryWriter = openWriter(summaryPath);
                packetWriter = openWriter(packetPath);
                summaryWriter.println("record_type,session,elapsed_s,window_s,ticks,observed_tps,players,ping_avg_ms,ping_p95_ms,ping_max_ms,worlds,loaded_entities,aircraft,projectiles,tracked_contacts,tick_avg_ms,tick_p50_ms,tick_p95_ms,tick_p99_ms,tick_max_ms,ticks_over_50ms,ticks_over_100ms,connections,recv_queue_avg,recv_queue_max,send_queue_avg,send_queue_max,heap_used_mb,gc_collections_delta,gc_time_ms_delta");
                packetWriter.println("record_type,session,elapsed_s,interval_s,direction,channel,packet,send_calls,packet_count,packets_per_s,payload_bytes,estimated_deliveries,estimated_delivery_bytes,estimated_delivery_bytes_per_s,avg_payload_bytes,session_max_payload_bytes,avg_encode_us,session_max_encode_us,avg_handler_us,session_max_handler_us");
                summaryWriter.flush();
                packetWriter.flush();
            } catch (Exception e) {
                closeWriters();
                throw new IllegalStateException("Could not open network log files: " + e.getMessage());
            }
            startMillis = windowStartMillis = System.currentTimeMillis();
            long[] gc = readGcTotals();
            startGcCollections = windowGcCollections = gc[0];
            startGcMillis = windowGcMillis = gc[1];
            active = true;
        }
    }

    public static void stop() {
        synchronized (SESSION_LOCK) {
            if (!active) {
                throw new IllegalStateException("Network logging is not active.");
            }
            active = false;
            writeWindow("FINAL_WINDOW", true);
            writeTotal();
            closeWriters();
        }
    }

    public static void recordServerSend(String channel, Object packet, int recipients) {
        if (!active || packet == null) {
            return;
        }
        metric("S2C", channel, packet.getClass().getSimpleName()).recordSend(Math.max(0, recipients));
    }

    public static void recordServerEncoded(String channel, Object packet, int bytes, long encodeNanos) {
        if (!active || packet == null) {
            return;
        }
        metric("S2C", channel, packet.getClass().getSimpleName()).recordPacket(bytes, encodeNanos, 0L);
    }

    public static void recordServerReceived(String channel, String packet, int bytes, long handlerNanos) {
        if (!active) {
            return;
        }
        PacketMetric metric = metric("C2S", channel, packet);
        metric.recordPacket(bytes, 0L, handlerNanos);
        metric.recordDelivery();
    }

    public static String getLegacyPacketName(int messageId) {
        Integer key = Integer.valueOf(messageId);
        String existing = LEGACY_PACKET_NAMES.get(key);
        if (existing != null) {
            return existing;
        }
        if (LEGACY_PACKET_NAMES.size() >= MAX_LEGACY_PACKET_IDS) {
            return "MSG_OTHER";
        }
        String created = String.format(Locale.ROOT, "MSG_0x%08X", messageId);
        String raced = LEGACY_PACKET_NAMES.putIfAbsent(key, created);
        return raced != null ? raced : created;
    }

    public static int getPlayerCount() {
        MinecraftServer server = MinecraftServer.getServer();
        return active && server != null && server.getConfigurationManager() != null
            ? server.getConfigurationManager().playerEntityList.size() : 0;
    }

    public static int countPlayersInDimension(int dimension) {
        if (!active) {
            return 0;
        }
        MinecraftServer server = MinecraftServer.getServer();
        if (server == null || server.getConfigurationManager() == null) {
            return 0;
        }
        int count = 0;
        for (Object value : server.getConfigurationManager().playerEntityList) {
            if (value instanceof EntityPlayerMP && ((EntityPlayerMP)value).dimension == dimension) {
                count++;
            }
        }
        return count;
    }

    public static int countPlayersAround(int dimension, double x, double y, double z, double range) {
        if (!active) {
            return 0;
        }
        MinecraftServer server = MinecraftServer.getServer();
        if (server == null || server.getConfigurationManager() == null) {
            return 0;
        }
        int count = 0;
        double rangeSq = range * range;
        for (Object value : server.getConfigurationManager().playerEntityList) {
            if (value instanceof EntityPlayerMP) {
                EntityPlayerMP player = (EntityPlayerMP)value;
                double dx = player.posX - x;
                double dy = player.posY - y;
                double dz = player.posZ - z;
                if (player.dimension == dimension && dx * dx + dy * dy + dz * dz <= rangeSq) {
                    count++;
                }
            }
        }
        return count;
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (!active) {
            return;
        }
        if (event.phase == TickEvent.Phase.START) {
            tickStartNanos = System.nanoTime();
        } else if (tickStartNanos != 0L) {
            double tickMs = (System.nanoTime() - tickStartNanos) / 1000000.0D;
            tickStartNanos = 0L;
            recordTick(tickMs);
            sampleQueues();
            if (windowTickCount >= REPORT_TICKS) {
                synchronized (SESSION_LOCK) {
                    if (active) {
                        writeWindow("WINDOW", false);
                    }
                }
            }
        }
    }

    private static void recordTick(double tickMs) {
        if (windowTickCount < WINDOW_TICKS_MS.length) {
            WINDOW_TICKS_MS[windowTickCount] = tickMs;
        }
        windowTickCount++;
        totalTickCount++;
        totalTickMillis += tickMs;
        totalTickMaxMillis = Math.max(totalTickMaxMillis, tickMs);
        if (tickMs > 50.0D) totalTicksOver50++;
        if (tickMs > 100.0D) totalTicksOver100++;
        int bucket = tickMs >= 1001.0D ? 1001 : Math.max(0, (int)Math.floor(tickMs));
        TOTAL_TICK_HISTOGRAM[bucket]++;
    }

    @SuppressWarnings("unchecked")
    private static void sampleQueues() {
        int recv = 0;
        int send = 0;
        List managers = W_Reflection.getNetworkManagers();
        if (managers != null) {
            for (Object value : managers) {
                if (value instanceof NetworkManager) {
                    Queue received = W_Reflection.getReceivedPacketsQueue((NetworkManager)value);
                    Queue outbound = W_Reflection.getSendPacketsQueue((NetworkManager)value);
                    recv += received != null ? received.size() : 0;
                    send += outbound != null ? outbound.size() : 0;
                }
            }
        }
        windowRecvQueueSum += recv;
        windowSendQueueSum += send;
        windowRecvQueueMax = Math.max(windowRecvQueueMax, recv);
        windowSendQueueMax = Math.max(windowSendQueueMax, send);
        totalRecvQueueSum += recv;
        totalSendQueueSum += send;
        totalRecvQueueMax = Math.max(totalRecvQueueMax, recv);
        totalSendQueueMax = Math.max(totalSendQueueMax, send);
    }

    private static void writeWindow(String type, boolean allowEmpty) {
        if (summaryWriter == null || packetWriter == null || (!allowEmpty && windowTickCount == 0)) {
            return;
        }
        long now = System.currentTimeMillis();
        double elapsed = (now - startMillis) / 1000.0D;
        double windowSeconds = Math.max(0.001D, (now - windowStartMillis) / 1000.0D);
        ServerSnapshot server = captureServerSnapshot();
        double[] sorted = Arrays.copyOf(WINDOW_TICKS_MS, Math.min(windowTickCount, WINDOW_TICKS_MS.length));
        Arrays.sort(sorted);
        double tickSum = 0.0D;
        int over50 = 0;
        int over100 = 0;
        for (double value : sorted) {
            tickSum += value;
            if (value > 50.0D) over50++;
            if (value > 100.0D) over100++;
        }
        long[] gc = readGcTotals();
        summaryWriter.printf(Locale.ROOT,
            "%s,%s,%.3f,%.3f,%d,%.3f,%d,%.1f,%.1f,%d,%d,%d,%d,%d,%d,%.3f,%.3f,%.3f,%.3f,%.3f,%d,%d,%d,%.3f,%d,%.3f,%d,%.1f,%d,%d%n",
            type, sessionName, elapsed, windowSeconds, windowTickCount, windowTickCount / windowSeconds,
            server.players, server.pingAverage, server.pingP95, server.pingMax, server.worlds, server.loadedEntities,
            server.aircraft, server.projectiles, server.trackedContacts,
            sorted.length > 0 ? tickSum / sorted.length : 0.0D, percentile(sorted, 0.50D),
            percentile(sorted, 0.95D), percentile(sorted, 0.99D), sorted.length > 0 ? sorted[sorted.length - 1] : 0.0D,
            over50, over100, server.connections,
            windowTickCount > 0 ? (double)windowRecvQueueSum / windowTickCount : 0.0D, windowRecvQueueMax,
            windowTickCount > 0 ? (double)windowSendQueueSum / windowTickCount : 0.0D, windowSendQueueMax,
            readHeapUsedMb(), gc[0] - windowGcCollections, gc[1] - windowGcMillis);
        writePacketRows(type, elapsed, windowSeconds, false);
        summaryWriter.flush();
        packetWriter.flush();
        windowStartMillis = now;
        windowTickCount = 0;
        windowRecvQueueSum = windowSendQueueSum = 0L;
        windowRecvQueueMax = windowSendQueueMax = 0;
        windowGcCollections = gc[0];
        windowGcMillis = gc[1];
    }

    private static void writeTotal() {
        if (summaryWriter == null || packetWriter == null) {
            return;
        }
        long now = System.currentTimeMillis();
        double elapsed = Math.max(0.001D, (now - startMillis) / 1000.0D);
        ServerSnapshot server = captureServerSnapshot();
        long[] gc = readGcTotals();
        summaryWriter.printf(Locale.ROOT,
            "TOTAL,%s,%.3f,%.3f,%d,%.3f,%d,%.1f,%.1f,%d,%d,%d,%d,%d,%d,%.3f,%.3f,%.3f,%.3f,%.3f,%d,%d,%d,%.3f,%d,%.3f,%d,%.1f,%d,%d%n",
            sessionName, elapsed, elapsed, totalTickCount, totalTickCount / elapsed,
            server.players, server.pingAverage, server.pingP95, server.pingMax, server.worlds, server.loadedEntities,
            server.aircraft, server.projectiles, server.trackedContacts,
            totalTickCount > 0 ? totalTickMillis / totalTickCount : 0.0D,
            histogramPercentile(0.50D), histogramPercentile(0.95D), histogramPercentile(0.99D), totalTickMaxMillis,
            totalTicksOver50, totalTicksOver100, server.connections,
            totalTickCount > 0 ? (double)totalRecvQueueSum / totalTickCount : 0.0D, totalRecvQueueMax,
            totalTickCount > 0 ? (double)totalSendQueueSum / totalTickCount : 0.0D, totalSendQueueMax,
            readHeapUsedMb(), gc[0] - startGcCollections, gc[1] - startGcMillis);
        writePacketRows("TOTAL", elapsed, elapsed, true);
        summaryWriter.flush();
        packetWriter.flush();
    }

    private static void writePacketRows(String type, double elapsed, double interval, boolean total) {
        List<PacketSnapshot> rows = new ArrayList<PacketSnapshot>();
        for (Map<String, PacketMetric> group : PACKET_GROUPS) {
            for (PacketMetric metric : group.values()) {
                PacketSnapshot row = metric.snapshot(total);
                if (row.packetCount > 0L || row.sendCalls > 0L) {
                    rows.add(row);
                }
            }
        }
        Collections.sort(rows, new Comparator<PacketSnapshot>() {
            @Override
            public int compare(PacketSnapshot a, PacketSnapshot b) {
                return Long.compare(b.estimatedDeliveryBytes(), a.estimatedDeliveryBytes());
            }
        });
        for (PacketSnapshot row : rows) {
            packetWriter.printf(Locale.ROOT,
                "%s,%s,%.3f,%.3f,%s,%s,%s,%d,%d,%.3f,%d,%d,%d,%.3f,%.1f,%d,%.3f,%.3f,%.3f,%.3f%n",
                type, sessionName, elapsed, interval, row.direction, row.channel, row.packet, row.sendCalls,
                row.packetCount, row.packetCount / interval, row.payloadBytes, row.deliveries,
                row.estimatedDeliveryBytes(), row.estimatedDeliveryBytes() / interval, row.averagePayload(),
                row.maxPayloadBytes, row.averageEncodeMicros(), row.maxEncodeNanos / 1000.0D,
                row.averageHandlerMicros(), row.maxHandlerNanos / 1000.0D);
        }
    }

    private static ServerSnapshot captureServerSnapshot() {
        ServerSnapshot result = new ServerSnapshot();
        MinecraftServer server = MinecraftServer.getServer();
        if (server == null) {
            return result;
        }
        List<Integer> pings = new ArrayList<Integer>();
        if (server.getConfigurationManager() != null) {
            for (Object value : server.getConfigurationManager().playerEntityList) {
                if (value instanceof EntityPlayerMP) {
                    EntityPlayerMP player = (EntityPlayerMP)value;
                    result.players++;
                    result.pingMax = Math.max(result.pingMax, player.ping);
                    result.pingAverage += player.ping;
                    pings.add(Integer.valueOf(player.ping));
                }
            }
        }
        if (result.players > 0) result.pingAverage /= result.players;
        Collections.sort(pings);
        result.pingP95 = pings.isEmpty() ? 0.0D : pings.get(Math.min(pings.size() - 1, (int)Math.ceil(pings.size() * 0.95D) - 1));
        WorldServer[] worlds = server.worldServers;
        if (worlds != null) {
            for (WorldServer world : worlds) {
                if (world == null) continue;
                result.worlds++;
                result.loadedEntities += world.loadedEntityList.size();
                for (Object value : world.loadedEntityList) {
                    if (value instanceof MCH_EntityAircraft) result.aircraft++;
                    if (value instanceof MCH_EntityBaseBullet) result.projectiles++;
                }
            }
        }
        result.trackedContacts = MCH_EntityInfoManager.serverEntities.size();
        List managers = W_Reflection.getNetworkManagers();
        result.connections = managers != null ? managers.size() : 0;
        return result;
    }

    private static PacketMetric metric(String direction, String channel, String packet) {
        Map<String, PacketMetric> group;
        if ("MCHeli_CH".equals(channel)) {
            group = "S2C".equals(direction) ? LEGACY_S2C : LEGACY_C2S;
        } else {
            group = "S2C".equals(direction) ? MODERN_S2C : MODERN_C2S;
        }
        PacketMetric existing = group.get(packet);
        if (existing != null) return existing;
        PacketMetric created = new PacketMetric(direction, channel, packet);
        PacketMetric raced = group.putIfAbsent(packet, created);
        return raced != null ? raced : created;
    }

    private static double percentile(double[] sorted, double percentile) {
        if (sorted.length == 0) return 0.0D;
        int index = Math.min(sorted.length - 1, Math.max(0, (int)Math.ceil(sorted.length * percentile) - 1));
        return sorted[index];
    }

    private static double histogramPercentile(double percentile) {
        if (totalTickCount == 0L) return 0.0D;
        long wanted = Math.max(1L, (long)Math.ceil(totalTickCount * percentile));
        long seen = 0L;
        for (int i = 0; i < TOTAL_TICK_HISTOGRAM.length; i++) {
            seen += TOTAL_TICK_HISTOGRAM[i];
            if (seen >= wanted) return i == 1001 ? 1001.0D : i;
        }
        return 0.0D;
    }

    private static long[] readGcTotals() {
        long count = 0L;
        long millis = 0L;
        for (GarbageCollectorMXBean bean : ManagementFactory.getGarbageCollectorMXBeans()) {
            if (bean.getCollectionCount() > 0L) count += bean.getCollectionCount();
            if (bean.getCollectionTime() > 0L) millis += bean.getCollectionTime();
        }
        return new long[]{count, millis};
    }

    private static double readHeapUsedMb() {
        MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
        return memory.getHeapMemoryUsage().getUsed() / (1024.0D * 1024.0D);
    }

    private static PrintWriter openWriter(String path) throws Exception {
        return new PrintWriter(new OutputStreamWriter(new FileOutputStream(path, false), "UTF-8"));
    }

    private static void ensureRegistered() {
        if (!registered) {
            FMLCommonHandler.instance().bus().register(INSTANCE);
            registered = true;
        }
    }

    private static void resetCounters() {
        for (Map<String, PacketMetric> group : PACKET_GROUPS) group.clear();
        LEGACY_PACKET_NAMES.clear();
        Arrays.fill(WINDOW_TICKS_MS, 0.0D);
        Arrays.fill(TOTAL_TICK_HISTOGRAM, 0L);
        windowTickCount = 0;
        totalTickCount = 0L;
        totalTickMillis = totalTickMaxMillis = 0.0D;
        totalTicksOver50 = totalTicksOver100 = 0L;
        windowRecvQueueSum = windowSendQueueSum = totalRecvQueueSum = totalSendQueueSum = 0L;
        windowRecvQueueMax = windowSendQueueMax = totalRecvQueueMax = totalSendQueueMax = 0;
        tickStartNanos = 0L;
    }

    private static void closeWriters() {
        if (summaryWriter != null) summaryWriter.close();
        if (packetWriter != null) packetWriter.close();
        summaryWriter = null;
        packetWriter = null;
    }

    private static void updateMax(AtomicLong target, long value) {
        long current = target.get();
        while (value > current && !target.compareAndSet(current, value)) current = target.get();
    }

    private static final class PacketMetric {
        final String direction;
        final String channel;
        final String packet;
        final AtomicLong sendCalls = new AtomicLong();
        final AtomicLong deliveries = new AtomicLong();
        final AtomicLong packets = new AtomicLong();
        final AtomicLong payloadBytes = new AtomicLong();
        final AtomicLong maxPayloadBytes = new AtomicLong();
        final AtomicLong encodeNanos = new AtomicLong();
        final AtomicLong maxEncodeNanos = new AtomicLong();
        final AtomicLong handlerNanos = new AtomicLong();
        final AtomicLong maxHandlerNanos = new AtomicLong();
        final AtomicLong handlerCount = new AtomicLong();
        long lastSendCalls;
        long lastDeliveries;
        long lastPackets;
        long lastPayloadBytes;
        long lastEncodeNanos;
        long lastHandlerNanos;
        long lastHandlerCount;

        PacketMetric(String direction, String channel, String packet) {
            this.direction = direction;
            this.channel = channel;
            this.packet = packet;
        }

        void recordSend(int recipientCount) {
            sendCalls.incrementAndGet();
            deliveries.addAndGet(recipientCount);
        }

        void recordDelivery() {
            deliveries.incrementAndGet();
        }

        void recordPacket(int bytes, long encodeTime, long handlerTime) {
            packets.incrementAndGet();
            payloadBytes.addAndGet(Math.max(0, bytes));
            encodeNanos.addAndGet(Math.max(0L, encodeTime));
            updateMax(maxPayloadBytes, Math.max(0, bytes));
            updateMax(maxEncodeNanos, Math.max(0L, encodeTime));
            if (handlerTime > 0L) {
                handlerCount.incrementAndGet();
                handlerNanos.addAndGet(handlerTime);
                updateMax(maxHandlerNanos, handlerTime);
            }
        }

        synchronized PacketSnapshot snapshot(boolean total) {
            long currentSendCalls = sendCalls.get();
            long currentDeliveries = deliveries.get();
            long currentPackets = packets.get();
            long currentPayloadBytes = payloadBytes.get();
            long currentEncodeNanos = encodeNanos.get();
            long currentHandlerNanos = handlerNanos.get();
            long currentHandlerCount = handlerCount.get();
            PacketSnapshot result = new PacketSnapshot(direction, channel, packet,
                total ? currentSendCalls : currentSendCalls - lastSendCalls,
                total ? currentDeliveries : currentDeliveries - lastDeliveries,
                total ? currentPackets : currentPackets - lastPackets,
                total ? currentPayloadBytes : currentPayloadBytes - lastPayloadBytes,
                maxPayloadBytes.get(),
                total ? currentEncodeNanos : currentEncodeNanos - lastEncodeNanos,
                maxEncodeNanos.get(),
                total ? currentHandlerNanos : currentHandlerNanos - lastHandlerNanos,
                maxHandlerNanos.get(),
                total ? currentHandlerCount : currentHandlerCount - lastHandlerCount);
            if (!total) {
                lastSendCalls = currentSendCalls;
                lastDeliveries = currentDeliveries;
                lastPackets = currentPackets;
                lastPayloadBytes = currentPayloadBytes;
                lastEncodeNanos = currentEncodeNanos;
                lastHandlerNanos = currentHandlerNanos;
                lastHandlerCount = currentHandlerCount;
            }
            return result;
        }
    }

    private static final class PacketSnapshot {
        final String direction;
        final String channel;
        final String packet;
        final long sendCalls;
        final long deliveries;
        final long packetCount;
        final long payloadBytes;
        final long maxPayloadBytes;
        final long encodeNanos;
        final long maxEncodeNanos;
        final long handlerNanos;
        final long maxHandlerNanos;
        final long handlerCount;

        PacketSnapshot(String direction, String channel, String packet, long sendCalls, long deliveries,
                       long packetCount, long payloadBytes, long maxPayloadBytes, long encodeNanos,
                       long maxEncodeNanos, long handlerNanos, long maxHandlerNanos, long handlerCount) {
            this.direction = direction;
            this.channel = channel;
            this.packet = packet;
            this.sendCalls = sendCalls;
            this.deliveries = deliveries;
            this.packetCount = packetCount;
            this.payloadBytes = payloadBytes;
            this.maxPayloadBytes = maxPayloadBytes;
            this.encodeNanos = encodeNanos;
            this.maxEncodeNanos = maxEncodeNanos;
            this.handlerNanos = handlerNanos;
            this.maxHandlerNanos = maxHandlerNanos;
            this.handlerCount = handlerCount;
        }

        long estimatedDeliveryBytes() {
            return packetCount > 0L ? Math.round((double)payloadBytes / packetCount * deliveries) : 0L;
        }

        double averagePayload() {
            return packetCount > 0L ? (double)payloadBytes / packetCount : 0.0D;
        }

        double averageEncodeMicros() {
            return packetCount > 0L ? encodeNanos / 1000.0D / packetCount : 0.0D;
        }

        double averageHandlerMicros() {
            return handlerCount > 0L ? handlerNanos / 1000.0D / handlerCount : 0.0D;
        }
    }

    private static final class ServerSnapshot {
        int players;
        double pingAverage;
        double pingP95;
        int pingMax;
        int worlds;
        int loadedEntities;
        int aircraft;
        int projectiles;
        int trackedContacts;
        int connections;
    }
}
