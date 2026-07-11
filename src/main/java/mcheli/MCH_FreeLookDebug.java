package mcheli;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.World;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class MCH_FreeLookDebug {

    private static final Object LOCK = new Object();
    private static final SimpleDateFormat TS_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    private static final String LOG_PATH = "logs/mcheli_freelook_debug.log";
    private static PrintWriter writer = null;

    private MCH_FreeLookDebug() {
    }

    public static String getLogPath() {
        return LOG_PATH;
    }

    public static void trace(World world, Entity actor, String format, Object... data) {
        String msg = String.format(Locale.ROOT, format, data);
        String side = world != null ? (world.isRemote ? "CLIENT" : "SERVER") : "UNKNOWN";
        String line = String.format(Locale.ROOT, "[%s][%s] %s", TS_FORMAT.format(new Date()), side, msg);

        // Keep existing console visibility.
        MCH_Lib.Log(world, "[FreeLookDebug] %s", msg);
        appendLine(line);

        if (actor instanceof EntityPlayer) {
            ((EntityPlayer) actor).addChatMessage(new ChatComponentText("[FreeLookDebug] " + msg));
        }
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
