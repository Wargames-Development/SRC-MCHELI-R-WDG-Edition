package mcheli.mob;

import cpw.mods.fml.common.Loader;
import mcheli.wgc.Integrations;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.scoreboard.Team;
import net.minecraft.world.World;

import java.util.UUID;

/** Uses WGEvents' published scoreboard identities or WGCore's optional API boundary. */
public final class MCH_GunnerTeams {
    private MCH_GunnerTeams() { }

    public static String teamOf(World world, Entity entity) {
        if (entity == null) return "";
        if (entity instanceof MCH_EntityGunner) return ((MCH_EntityGunner) entity).getTeamName();
        if (entity instanceof EntityPlayer && Loader.isModLoaded("wgcore")) {
            UUID faction = Integrations.getPlayerFactionWGC(world, entity.getUniqueID());
            return faction == null ? "" : "wgc:" + faction;
        }
        Team team = entity instanceof EntityLivingBase ? ((EntityLivingBase) entity).getTeam() : null;
        String name = team == null ? "" : team.getRegisteredName();
        if (Loader.isModLoaded("wgcore")) return "";
        // WGEvents TeamService stores all event teams as wge_<internalTeamId>.
        if (Loader.isModLoaded("wgevents") && !name.startsWith("wge_")) return "";
        return name;
    }

    public static String fixedTeam(World world, String id) {
        if (id == null || id.trim().isEmpty()) return "";
        id = id.trim();
        if (Loader.isModLoaded("wgcore")) {
            try {
                return "wgc:" + UUID.fromString(id.startsWith("wgc:") ? id.substring(4) : id);
            } catch (IllegalArgumentException invalid) {
                return "";
            }
        }
        if (Loader.isModLoaded("wgevents") && !id.startsWith("wge_")) id = "wge_" + id;
        return world.getScoreboard().getTeam(id) == null ? "" : id;
    }

    public static boolean validTeam(World world, String id) {
        return id != null && !id.isEmpty() && id.equals(fixedTeam(world, id));
    }
}
