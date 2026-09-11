package mcheli.weapon;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;

/**
 * Small client-side cache of enabled JourneyMap waypoints whose filename contains "gps".
 *
 * Filesystem I/O is deliberately throttled so HUD renderers can query this class every
 * frame without recursively scanning JourneyMap's data directory every frame.
 */
@SideOnly(Side.CLIENT)
public final class MCH_JourneyMapGPSCache {

    private static final long REFRESH_INTERVAL_MS = 1000L;
    private static final Pattern DISABLED_PATTERN = Pattern.compile("\\\"enable\\\"\\s*:\\s*false", Pattern.CASE_INSENSITIVE);
    private static final Pattern DIMENSIONS_PATTERN = Pattern.compile("\\\"dimensions\\\"\\s*:\\s*\\[([^\\]]*)\\]", Pattern.CASE_INSENSITIVE);
    private static final Pattern INTEGER_PATTERN = Pattern.compile("-?\\d+");
    private static final Pattern X_PATTERN = Pattern.compile("\\\"x\\\"\\s*:\\s*(-?\\d+)");
    private static final Pattern Y_PATTERN = Pattern.compile("\\\"y\\\"\\s*:\\s*(-?\\d+)");
    private static final Pattern Z_PATTERN = Pattern.compile("\\\"z\\\"\\s*:\\s*(-?\\d+)");

    private static List<Waypoint> cachedWaypoints = Collections.emptyList();
    private static long nextRefreshMs = 0L;
    private static int lastWorldIdentity = 0;
    private static int lastDimension = Integer.MIN_VALUE;
    private static boolean lastSingleplayer = false;

    private MCH_JourneyMapGPSCache() {
    }

    public static List<Waypoint> getWaypoints() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.theWorld == null) {
            clear();
            return Collections.emptyList();
        }

        int worldIdentity = System.identityHashCode(mc.theWorld);
        int dimension = mc.theWorld.provider.dimensionId;
        boolean singleplayer = mc.isSingleplayer();
        long now = System.currentTimeMillis();

        boolean contextChanged = worldIdentity != lastWorldIdentity
            || dimension != lastDimension
            || singleplayer != lastSingleplayer;
        if (contextChanged || now >= nextRefreshMs) {
            refresh(mc, dimension, singleplayer);
            lastWorldIdentity = worldIdentity;
            lastDimension = dimension;
            lastSingleplayer = singleplayer;
            nextRefreshMs = now + REFRESH_INTERVAL_MS;
        }

        return cachedWaypoints;
    }

    private static void refresh(Minecraft mc, int dimension, boolean singleplayer) {
        try {
            File jmRoot = new File(mc.mcDataDir, "journeymap/data");
            File modeRoot = new File(jmRoot, singleplayer ? "sp" : "mp");
            if (!modeRoot.exists() || !modeRoot.isDirectory()) {
                cachedWaypoints = Collections.emptyList();
                return;
            }

            ArrayList<Waypoint> result = new ArrayList<Waypoint>();
            collectEnabledGpsWaypoints(modeRoot, dimension, result);
            cachedWaypoints = result.isEmpty()
                ? Collections.<Waypoint>emptyList()
                : Collections.unmodifiableList(result);
        } catch (Exception e) {
            cachedWaypoints = Collections.emptyList();
        }
    }

    private static void collectEnabledGpsWaypoints(File node, int dimension, ArrayList<Waypoint> out) {
        if (node == null || !node.exists()) {
            return;
        }

        if (node.isDirectory()) {
            File[] children = node.listFiles();
            if (children == null) {
                return;
            }
            for (File child : children) {
                collectEnabledGpsWaypoints(child, dimension, out);
            }
            return;
        }

        if (!node.isFile()) {
            return;
        }

        String fileName = node.getName().toLowerCase(Locale.ROOT);
        if (!fileName.endsWith(".json") || !fileName.contains("gps")) {
            return;
        }

        try {
            String json = new String(java.nio.file.Files.readAllBytes(node.toPath()), "UTF-8");
            if (DISABLED_PATTERN.matcher(json).find() || !isForCurrentDimension(json, dimension)) {
                return;
            }

            Integer x = extractInt(json, X_PATTERN);
            Integer y = extractInt(json, Y_PATTERN);
            Integer z = extractInt(json, Z_PATTERN);
            if (x != null && y != null && z != null) {
                out.add(new Waypoint(x.doubleValue() + 0.5D, y.doubleValue() + 0.5D, z.doubleValue() + 0.5D));
            }
        } catch (Exception ignored) {
            // JourneyMap can rewrite waypoint files while the game is running.
            // Ignore a transient/incomplete file and retry it on the next cache refresh.
        }
    }

    private static Integer extractInt(String json, Pattern pattern) {
        Matcher matcher = pattern.matcher(json);
        if (!matcher.find()) {
            return null;
        }
        try {
            return Integer.valueOf(matcher.group(1));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static boolean isForCurrentDimension(String json, int dimension) {
        Matcher dimensions = DIMENSIONS_PATTERN.matcher(json);
        if (!dimensions.find()) {
            return true;
        }

        Matcher values = INTEGER_PATTERN.matcher(dimensions.group(1));
        boolean foundNumericDimension = false;
        while (values.find()) {
            foundNumericDimension = true;
            try {
                if (Integer.parseInt(values.group()) == dimension) {
                    return true;
                }
            } catch (NumberFormatException ignored) {
            }
        }

        // If this JourneyMap version uses non-numeric dimension identifiers,
        // do not hide the waypoint just because this lightweight parser cannot decode them.
        return !foundNumericDimension;
    }

    private static void clear() {
        cachedWaypoints = Collections.emptyList();
        nextRefreshMs = 0L;
        lastWorldIdentity = 0;
        lastDimension = Integer.MIN_VALUE;
    }

    public static final class Waypoint {
        public final double x;
        public final double y;
        public final double z;

        private Waypoint(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }
}
