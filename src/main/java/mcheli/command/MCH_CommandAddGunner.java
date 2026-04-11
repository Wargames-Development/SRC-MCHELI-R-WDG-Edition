package mcheli.command;

import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.aircraft.MCH_EntitySeat;
import mcheli.mob.MCH_EntityGunner;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import java.util.List;
import java.util.Locale;

public class MCH_CommandAddGunner extends CommandBase {

    public String getCommandName() {
        return "addgunner";
    }

    public String getCommandUsage(ICommandSender sender) {
        return "/addgunner <gunner名称> <x> <y> <z> <r> 或 /addgunner pvp <teamname> <x> <y> <z> <r>";
    }

    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return true;
    }

    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length != 5 && args.length != 6) {
            throw new WrongUsageException(getCommandUsage(sender), new Object[0]);
        }

        int targetType = parseTargetType(args[0]);
        if (targetType < 0) {
            throw new CommandException("Unknown gunner name: " + args[0] + " (friendly|player|aa|enemy)", new Object[0]);
        }

        World world = sender.getEntityWorld();
        Team team = null;
        int argOffset = 1;
        if (targetType == MCH_EntityGunner.TARGET_PLAYER) {
            if (args.length == 6) {
                team = world.getScoreboard().getTeam(args[1]);
                if (team == null) {
                    throw new CommandException("Unknown team: " + args[1], new Object[0]);
                }
                argOffset = 2;
            } else {
                if (!(sender instanceof EntityPlayer)) {
                    throw new CommandException("For command blocks, use: /addgunner pvp <teamname> <x> <y> <z> <r>", new Object[0]);
                }
                EntityPlayer player = (EntityPlayer)sender;
                team = player.worldObj.getScoreboard().getPlayersTeam(player.getDisplayName());
                if (team == null) {
                    throw new CommandException("player gunner requires command sender on a scoreboard team or explicit <teamname>", new Object[0]);
                }
            }
        } else if (args.length != 5) {
            throw new WrongUsageException(getCommandUsage(sender), new Object[0]);
        }

        ChunkCoordinates cc = sender.getPlayerCoordinates();
        double x = func_110666_a(sender, cc.posX, args[argOffset]);
        double y = func_110666_a(sender, cc.posY, args[argOffset + 1]);
        double z = func_110666_a(sender, cc.posZ, args[argOffset + 2]);
        double r;
        try {
            r = Double.parseDouble(args[argOffset + 3]);
        } catch (Exception e) {
            throw new CommandException("Invalid radius: " + args[argOffset + 3], new Object[0]);
        }

        if (r <= 0.0D) {
            throw new CommandException("Radius must be > 0", new Object[0]);
        }

        AxisAlignedBB aabb = AxisAlignedBB.getBoundingBox(x - r, y - r, z - r, x + r, y + r, z + r);
        List list = world.getEntitiesWithinAABB(MCH_EntityAircraft.class, aabb);
        int aircraftCount = 0;
        int gunnerCount = 0;
        double r2 = r * r;

        for (Object o : list) {
            if (!(o instanceof MCH_EntityAircraft)) {
                continue;
            }

            MCH_EntityAircraft ac = (MCH_EntityAircraft)o;
            if (ac.isDead || ac.getAcInfo() == null || ac.isUAV()) {
                continue;
            }

            double dx = ac.posX - x;
            double dy = ac.posY - y;
            double dz = ac.posZ - z;
            if (dx * dx + dy * dy + dz * dz > r2) {
                continue;
            }

            boolean touched = false;
            if (ac.getRiddenByEntity() == null) {
                if (spawnGunner(world, sender, team, targetType, ac)) {
                    gunnerCount++;
                    touched = true;
                }
            }

            MCH_EntitySeat[] seats = ac.getSeats();
            for (MCH_EntitySeat seat : seats) {
                if (seat != null && !seat.isDead && seat.riddenByEntity == null) {
                    if (spawnGunner(world, sender, team, targetType, seat)) {
                        gunnerCount++;
                        touched = true;
                    }
                }
            }

            if (touched) {
                aircraftCount++;
            }
        }

        sender.addChatMessage(new ChatComponentText("addgunner: " + gunnerCount + " gunners added to " + aircraftCount + " aircraft(s)."));
    }

    private boolean spawnGunner(World world, ICommandSender sender, Team team, int targetType, Entity mountTarget) {
        MCH_EntityGunner gunner = new MCH_EntityGunner(world, mountTarget.posX, mountTarget.posY, mountTarget.posZ);
        gunner.rotationYaw = MathHelper.wrapAngleTo180_float(mountTarget.rotationYaw);
        gunner.setTargetType(targetType);
        if (sender instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer)sender;
            gunner.isCreative = player.capabilities.isCreativeMode;
            gunner.ownerUUID = player.getUniqueID().toString();
            if (team != null) {
                gunner.setTeamName(team.getRegisteredName());
            }
        } else {
            gunner.isCreative = true;
            gunner.ownerUUID = "";
        }
        world.spawnEntityInWorld(gunner);
        gunner.mountEntity(mountTarget);
        return true;
    }

    private int parseTargetType(String gunnerName) {
        String s = gunnerName.toLowerCase(Locale.ROOT);
        if (s.equals("friendly") || s.equals("friend") || s.equals("ally") || s.equals("友好")) {
            return MCH_EntityGunner.TARGET_MONSTER;
        }
        if (s.equals("player") || s.equals("pvp") || s.equals("阵营")) {
            return MCH_EntityGunner.TARGET_PLAYER;
        }
        if (s.equals("aa") || s.equals("antiair") || s.equals("anti_air") || s.equals("防空")) {
            return MCH_EntityGunner.TARGET_AA_AMMO;
        }
        if (s.equals("enemy") || s.equals("hostile") || s.equals("敌对")) {
            return MCH_EntityGunner.TARGET_ENEMY;
        }
        return -1;
    }
}
