package mcheli.wgc;

import java.lang.reflect.Method;
import net.minecraft.entity.Entity;

/** Optional server fire provenance; WGQOL is never a required dependency. */
public final class CombatBridge {
    private static Method ignite;
    private static boolean checked;
    private CombatBridge() { }
    public static void ignite(Entity victim, Entity projectile, int seconds) {
        if (victim.worldObj.isRemote) return;
        if (!checked) {
            checked = true;
            try { ignite = Class.forName("com.wdg.wgqol.stats.CombatService").getMethod("ignite", Entity.class, Entity.class, int.class); }
            catch (ReflectiveOperationException | LinkageError absent) { }
        }
        if (ignite != null) try { ignite.invoke(null, victim, projectile, seconds); }
        catch (ReflectiveOperationException error) { org.apache.logging.log4j.LogManager.getLogger("WGQOL").warn("Cannot record weapon fire provenance", error); }
    }
}
