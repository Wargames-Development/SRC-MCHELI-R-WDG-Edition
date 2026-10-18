package mcheli.command;

import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.aircraft.MCH_EntitySeat;
import mcheli.mob.MCH_EntityGunner;
import mcheli.mob.MCH_GunnerInfo;
import mcheli.mob.MCH_GunnerInfoManager;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.World;

import java.util.List;
import java.util.Locale;

public class MCH_CommandAddGunner extends CommandBase {
    @Override public String getCommandName() { return "addgunner"; }
    @Override public int getRequiredPermissionLevel() { return 2; }
    @Override public String getCommandUsage(ICommandSender sender) {
        return "/addgunner aa <x> <y> <z> <radius> | /addgunner profile <name> <x> <y> <z> <radius>";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        boolean useProfile = args.length == 6 && "profile".equalsIgnoreCase(args[0]);
        if (!useProfile && (args.length != 5 || !"aa".equalsIgnoreCase(args[0])))
            throw new WrongUsageException(getCommandUsage(sender));
        MCH_GunnerInfo profile = useProfile ? MCH_GunnerInfoManager.get(args[1].toLowerCase(Locale.ROOT)) : null;
        if (useProfile && (profile == null || profile.targetType != MCH_EntityGunner.TARGET_AA_AMMO))
            throw new CommandException("Select a valid AA gunner profile.");
        if (!(sender instanceof EntityPlayer) && (profile == null || !"fixed".equalsIgnoreCase(profile.teamMode)))
            throw new CommandException("Command blocks require an AA profile with TeamMode=fixed.");
        World world = sender.getEntityWorld();
        EntityPlayer player = sender instanceof EntityPlayer ? (EntityPlayer) sender : null;
        MCH_EntityGunner teamProbe = new MCH_EntityGunner(world);
        if (!teamProbe.assignTeam(player, profile))
            throw new CommandException("A valid WGE team or WGC faction is required.");
        int offset = useProfile ? 2 : 1;
        ChunkCoordinates origin = sender.getPlayerCoordinates();
        double x = func_110666_a(sender, origin.posX, args[offset]);
        double y = func_110666_a(sender, origin.posY, args[offset + 1]);
        double z = func_110666_a(sender, origin.posZ, args[offset + 2]);
        double radius;
        try { radius = Double.parseDouble(args[offset + 3]); }
        catch (NumberFormatException invalid) { throw new CommandException("Invalid radius."); }
        if (!Double.isFinite(radius) || radius <= 0.0D || radius > 128.0D)
            throw new CommandException("Radius must be between 0 and 128 blocks.");
        List<MCH_EntityAircraft> aircraft = world.getEntitiesWithinAABB(MCH_EntityAircraft.class,
            AxisAlignedBB.getBoundingBox(x - radius, y - radius, z - radius, x + radius, y + radius, z + radius));
        int count = 0;
        for (MCH_EntityAircraft ac : aircraft) {
            if (!MCH_EntityGunner.canOperate(ac) || ac.isDead || ac.getDistanceSq(x, y, z) > radius * radius) continue;
            for (MCH_EntitySeat seat : ac.getSeats()) {
                if (seat != null && !seat.isDead && seat.riddenByEntity == null
                    && spawn(world, player, profile, teamProbe.getTeamName(), seat)) ++count;
            }
            if (ac.getRiddenByEntity() == null && spawn(world, player, profile, teamProbe.getTeamName(), ac)) ++count;
        }
        sender.addChatMessage(new ChatComponentText("Added " + count + " AA gunners."));
    }

    private boolean spawn(World world, EntityPlayer player, MCH_GunnerInfo profile, String team, Entity mount) {
        if (!MCH_EntityGunner.canMount(mount)) return false;
        MCH_EntityGunner gunner = new MCH_EntityGunner(world, mount.posX, mount.posY, mount.posZ);
        gunner.setTargetType(MCH_EntityGunner.TARGET_AA_AMMO);
        gunner.setTeamName(team);
        gunner.applyProfile(profile);
        gunner.rotationYaw = mount.rotationYaw;
        gunner.isCreative = player == null || player.capabilities.isCreativeMode;
        gunner.ownerUUID = player == null ? "" : player.getUniqueID().toString();
        if (!world.spawnEntityInWorld(gunner)) return false;
        gunner.mountEntity(mount);
        return true;
    }
}
