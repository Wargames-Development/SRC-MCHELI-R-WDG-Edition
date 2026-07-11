package mcheli.structure;

import mcheli.MCH_ServerSettings;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

public final class MCH_StructureDebugLogger {
    private static final int LOG_INTERVAL_TICKS = 150;
    private static int tickCounter = 0;
    private static int cycleChecked = 0;
    private static int cyclePlaced = 0;
    private static int cycleFailed = 0;

    private MCH_StructureDebugLogger() {
    }

    public static synchronized void setEnabled(boolean enabled) {
        MCH_ServerSettings.enableStructureDebugTicker = enabled;
        tickCounter = 0;
        cycleChecked = 0;
        cyclePlaced = 0;
        cycleFailed = 0;
        appendLine("toggle=" + (enabled ? "ON" : "OFF"));
    }

    public static synchronized boolean isEnabled() {
        return MCH_ServerSettings.enableStructureDebugTicker;
    }

    public static synchronized void onRuleChecked() {
        if (!isEnabled()) {
            return;
        }
        cycleChecked++;
    }

    public static synchronized void onPlaced(boolean success) {
        if (!isEnabled()) {
            return;
        }
        if (success) {
            cyclePlaced++;
        } else {
            cycleFailed++;
        }
    }

    public static synchronized void onServerTick() {
        if (!isEnabled()) {
            return;
        }
        tickCounter++;
        if (tickCounter < LOG_INTERVAL_TICKS) {
            return;
        }
        tickCounter = 0;
        appendLine(String.format(
            "tickWindow=%d checked=%d placed=%d failed=%d generated=%s",
            LOG_INTERVAL_TICKS,
            cycleChecked,
            cyclePlaced,
            cycleFailed,
            cyclePlaced > 0 ? "YES" : "NO"
        ));
        cycleChecked = 0;
        cyclePlaced = 0;
        cycleFailed = 0;
    }

    private static void appendLine(String msg) {
        PrintWriter pw = null;
        try {
            File dir = new File("logs");
            if (!dir.exists()) {
                dir.mkdirs();
            }
            File file = new File(dir, "mcheli_structure_debug.log");
            pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file, true), "UTF-8"));
            String ts = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date());
            pw.println("[" + ts + "][mcheli][structdebug] " + msg);
        } catch (Exception ignored) {
        } finally {
            if (pw != null) {
                pw.close();
            }
        }
    }
}
