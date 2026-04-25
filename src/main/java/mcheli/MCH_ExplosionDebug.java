package mcheli;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class MCH_ExplosionDebug {

    private static final Object LOCK = new Object();
    private static final SimpleDateFormat TS_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    private static final String LOG_PATH = "logs/mcheli_explosion_debug.log";
    private static PrintWriter writer = null;
    private static volatile boolean enabled = false;

    private MCH_ExplosionDebug() {
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

    public static void append(String format, Object... data) {
        if (!enabled) {
            return;
        }
        String msg = String.format(Locale.ROOT, format, data);
        String line = String.format(Locale.ROOT, "[%s] %s", TS_FORMAT.format(new Date()), msg);
        appendLine(line);
    }

    public static void appendRaw(String msg) {
        if (!enabled) {
            return;
        }
        String line = String.format(Locale.ROOT, "[%s] %s", TS_FORMAT.format(new Date()), msg);
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
