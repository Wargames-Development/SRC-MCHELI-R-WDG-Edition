package mcheli;

import net.minecraft.entity.Entity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.World;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class MCH_RadarDebug {

    private static final Object LOCK = new Object();
    private static final SimpleDateFormat TS_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    private static final String LOG_PATH = "logs/mcheli_radar_debug.log";
    private static PrintWriter writer = null;
    private static volatile boolean enabled = false;
    private static volatile boolean verbose = false;
    private static volatile boolean dataLinkWatchEnabled = false;
    private static volatile int dataLinkWatchIntervalTick = 40;

    private MCH_RadarDebug() {
    }

    public static String getLogPath() {
        return LOG_PATH;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean value) {
        enabled = value;
    }

    public static boolean isVerbose() {
        return verbose;
    }

    public static void setDataLinkWatchEnabled(boolean value) {
        dataLinkWatchEnabled = value;
    }

    public static boolean isDataLinkWatchEnabled() {
        return dataLinkWatchEnabled;
    }

    public static void setDataLinkWatchIntervalTick(int tick) {
        dataLinkWatchIntervalTick = Math.max(5, tick);
    }

    public static int getDataLinkWatchIntervalTick() {
        return dataLinkWatchIntervalTick;
    }

    public static void setVerbose(boolean value) {
        verbose = value;
    }

    public static void trace(World world, Entity actor, String format, Object... data) {
        if (!enabled) {
            return;
        }
        String msg = String.format(Locale.ROOT, format, data);
        String side = world != null ? (world.isRemote ? "CLIENT" : "SERVER") : "UNKNOWN";
        String line = String.format(Locale.ROOT, "[%s][%s] %s", TS_FORMAT.format(new Date()), side, msg);
        MCH_Lib.Log(world, "[RadarDebug] %s", msg);
        appendLine(line);
    }

    public static void traceVerbose(World world, Entity actor, String format, Object... data) {
        if (!enabled || !verbose) {
            return;
        }
        trace(world, actor, format, data);
    }

    public static void appendManual(String format, Object... data) {
        String msg = String.format(Locale.ROOT, format, data);
        String line = String.format(Locale.ROOT, "[%s][MANUAL] %s", TS_FORMAT.format(new Date()), msg);
        appendLine(line);
    }

    private static void appendLine(String line) {
        synchronized (LOCK) {
            try {
                if (writer == null) {
                    File file = new File(LOG_PATH);
                    File parent = file.getParentFile();
                    if (parent != null && !parent.exists()) {
                        parent.mkdirs();
                    }
                    writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file, true), "UTF-8"));
                }
                writer.println(line);
                writer.flush();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
