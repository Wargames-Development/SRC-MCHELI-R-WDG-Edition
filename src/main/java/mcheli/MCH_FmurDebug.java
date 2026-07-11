package mcheli;

import net.minecraft.entity.Entity;
import net.minecraft.world.World;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class MCH_FmurDebug {

    private static final Object LOCK = new Object();
    private static final SimpleDateFormat TS_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    private static final String LOG_PATH = "logs/mcheli_fmurcheck.log";
    private static PrintWriter writer = null;
    private static volatile boolean enabled = false;

    private MCH_FmurDebug() {
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean value) {
        enabled = value;
    }

    public static String getLogPath() {
        return LOG_PATH;
    }

    public static void log(String format, Object... data) {
        if (!enabled) {
            return;
        }
        String msg = String.format(Locale.ROOT, format, data);
        String line = String.format(Locale.ROOT, "[%s] %s", TS_FORMAT.format(new Date()), msg);
        MCH_Lib.Log((World) null, "[FMUR] %s", msg);
        appendLine(line);
    }

    public static void log(World world, String format, Object... data) {
        if (!enabled) {
            return;
        }
        String side = world != null ? (world.isRemote ? "CLIENT" : "SERVER") : "UNKNOWN";
        String msg = String.format(Locale.ROOT, format, data);
        String line = String.format(Locale.ROOT, "[%s][%s] %s", TS_FORMAT.format(new Date()), side, msg);
        MCH_Lib.Log(world, "[FMUR] %s", msg);
        appendLine(line);
    }

    public static void log(Entity entity, String format, Object... data) {
        if (!enabled) {
            return;
        }
        String side = entity != null && entity.worldObj != null ? (entity.worldObj.isRemote ? "CLIENT" : "SERVER") : "UNKNOWN";
        String msg = String.format(Locale.ROOT, format, data);
        String line = String.format(Locale.ROOT, "[%s][%s] %s", TS_FORMAT.format(new Date()), side, msg);
        MCH_Lib.Log(entity, "[FMUR] %s", msg);
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
