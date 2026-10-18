package mcheli.mob;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.util.List;
import java.util.Locale;


import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.aircraft.MCH_EntitySeat;
import mcheli.wrapper.W_Item;
import mcheli.wrapper.W_WorldFunc;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.IIcon;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public class MCH_ItemSpawnGunner extends W_Item {
    public int primaryColor = 16777215;
    public int secondaryColor = 16777215;
    public boolean applyItemColorTint = false;
    public int targetType = 0;
    public boolean useLayeredIcon = true;
    public String gunnerProfileName = "";
    @SideOnly(Side.CLIENT)
    private IIcon theIcon;

    public MCH_ItemSpawnGunner() {
        this.maxStackSize = 1;
        setCreativeTab(CreativeTabs.tabTransport);
    }

    public ItemStack onItemRightClick(ItemStack itemStack, World world, EntityPlayer player) {
        MCH_GunnerInfo profile = this.resolveProfile();
        int spawnTargetType = profile != null ? profile.targetType : this.targetType;
        if (spawnTargetType != MCH_EntityGunner.TARGET_AA_AMMO) {
            if (!world.isRemote) player.addChatMessage(new ChatComponentText("The old gunner AI has been retired. Use the AA gunner."));
            return itemStack;
        }

        float searchRange = profile != null ? profile.mountSearchRange : 5.0F;
        boolean allowMountAircraft = profile == null || profile.allowMountAircraft;
        boolean allowMountSeat = profile == null || profile.allowMountSeat;
        boolean allowReplace = profile != null && profile.allowReplaceExistingGunner;

        Entity mountTarget = null;
        MCH_EntityGunner hitGunner = null;
        MCH_EntitySeat hitSeat = null;
        float f = 1.0F;
        float pitch = player.prevRotationPitch + (player.rotationPitch - player.prevRotationPitch) * f;
        float yaw = player.prevRotationYaw + (player.rotationYaw - player.prevRotationYaw) * f;
        double dx = player.prevPosX + (player.posX - player.prevPosX) * f;
        double dy = player.prevPosY + (player.posY - player.prevPosY) * f + 1.62D - player.yOffset;
        double dz = player.prevPosZ + (player.posZ - player.prevPosZ) * f;
        Vec3 vec3 = Vec3.createVectorHelper(dx, dy, dz);
        float f3 = MathHelper.cos(-yaw * 0.017453292F - 3.1415927F);
        float f4 = MathHelper.sin(-yaw * 0.017453292F - 3.1415927F);
        float f5 = -MathHelper.cos(-pitch * 0.017453292F);
        float f6 = MathHelper.sin(-pitch * 0.017453292F);
        float f7 = f4 * f5;
        float f8 = f3 * f5;
        Vec3 vec31 = vec3.addVector(f7 * searchRange, f6 * searchRange, f8 * searchRange);

        List<MCH_EntityGunner> gunnerList = world.getEntitiesWithinAABB(MCH_EntityGunner.class, player.boundingBox.expand(searchRange, searchRange, searchRange));
        Entity nearest = null;
        int i;
        for (i = 0; i < gunnerList.size(); i++) {
            MCH_EntityGunner gunner = gunnerList.get(i);
            if (gunner.boundingBox.calculateIntercept(vec3, vec31) != null && (nearest == null || player.getDistanceSqToEntity((Entity) gunner) < player.getDistanceSqToEntity(nearest))) {
                hitGunner = gunner;
                nearest = gunner;
                mountTarget = gunner;
            }
        }

        if (hitGunner == null && allowMountSeat) {
            List<MCH_EntitySeat> seatList = world.getEntitiesWithinAABB(MCH_EntitySeat.class, player.boundingBox.expand(searchRange, searchRange, searchRange));
            for (i = 0; i < seatList.size(); i++) {
                MCH_EntitySeat seat = seatList.get(i);
                if (MCH_EntityGunner.canMount(seat) && seat.boundingBox.calculateIntercept(vec3, vec31) != null) {
                    if (hitSeat != null && player.getDistanceSqToEntity((Entity) seat) >= player.getDistanceSqToEntity((Entity) hitSeat)) {
                        continue;
                    }
                    if (seat.riddenByEntity instanceof MCH_EntityGunner && !allowReplace) {
                        continue;
                    }
                    hitSeat = seat;
                    mountTarget = seat;
                }
            }
        }

        if (hitGunner == null && hitSeat == null && allowMountAircraft) {
            List<MCH_EntityAircraft> aircraftList = world.getEntitiesWithinAABB(MCH_EntityAircraft.class, player.boundingBox.expand(searchRange, searchRange, searchRange));
            for (i = 0; i < aircraftList.size(); i++) {
                MCH_EntityAircraft ac = aircraftList.get(i);
                if (MCH_EntityGunner.canMount(ac) && ac.boundingBox.calculateIntercept(vec3, vec31) != null) {
                    if (mountTarget != null && player.getDistanceSqToEntity(ac) >= player.getDistanceSqToEntity(mountTarget)) {
                        continue;
                    }
                    if (ac.getRiddenByEntity() instanceof MCH_EntityGunner && !allowReplace) {
                        continue;
                    }
                    mountTarget = ac;
                }
            }
        }

        if (mountTarget instanceof MCH_EntityGunner) {
            mountTarget.interactFirst(player);
            return itemStack;
        }
        if (mountTarget == null) {
            if (!world.isRemote) {
                player.addChatMessage((IChatComponent) new ChatComponentText("Right click to seat."));
            }
            return itemStack;
        }

        if (!world.isRemote) {
            MCH_EntityAircraft aircraft = mountTarget instanceof MCH_EntityAircraft
                ? (MCH_EntityAircraft) mountTarget : ((MCH_EntitySeat) mountTarget).getParent();
            if (!MCH_EntityGunner.canMount(mountTarget)) {
                player.addChatMessage(new ChatComponentText("The AA gunner requires a CIWS vehicle."));
                return itemStack;
            }
            Entity occupant = mountTarget.riddenByEntity;
            if (occupant != null && (!(occupant instanceof MCH_EntityGunner) || !allowReplace
                || (!player.capabilities.isCreativeMode && !((MCH_EntityGunner) occupant).ownerUUID.equals(player.getUniqueID().toString())))) {
                player.addChatMessage(new ChatComponentText("This seat is occupied."));
                return itemStack;
            }
            MCH_EntityGunner gunner = new MCH_EntityGunner(world, mountTarget.posX, mountTarget.posY, mountTarget.posZ);
            gunner.rotationYaw = mountTarget.rotationYaw;
            gunner.isCreative = player.capabilities.isCreativeMode;
            gunner.ownerUUID = player.getUniqueID().toString();
            gunner.setTargetType(spawnTargetType);
            gunner.applyProfile(profile);
            if (!gunner.assignTeam(player, profile)) {
                player.addChatMessage(new ChatComponentText("Join a WGE team or WGC faction before placing an AA gunner."));
                return itemStack;
            }
            for (int seatId = 0; seatId <= aircraft.getSeatNum(); ++seatId) {
                String crewTeam = MCH_GunnerTeams.teamOf(world, aircraft.getEntityBySeatId(seatId));
                if (!crewTeam.isEmpty() && !crewTeam.equals(gunner.getTeamName())) {
                    player.addChatMessage(new ChatComponentText("This vehicle belongs to another team."));
                    return itemStack;
                }
            }
            if (!world.spawnEntityInWorld(gunner)) return itemStack;
            if (occupant != null) { occupant.setDead(); occupant.mountEntity(null); }
            gunner.mountEntity(mountTarget);
            W_WorldFunc.MOD_playSoundAtEntity((Entity) gunner, "wrench", 1.0F, 3.0F);
            MCH_EntityAircraft ac = (mountTarget instanceof MCH_EntityAircraft) ? (MCH_EntityAircraft) mountTarget : ((MCH_EntitySeat) mountTarget).getParent();
            player.addChatMessage((IChatComponent) new ChatComponentText("The gunner was put on " + EnumChatFormatting.GOLD + (ac.getAcInfo()).displayName + EnumChatFormatting.RESET + " seat " + (ac.getSeatIdByEntity((Entity) gunner) + 1) + " by " + ScorePlayerTeam.formatPlayerName(player.getTeam(), player.getDisplayName())));
        }

        if (!world.isRemote && !player.capabilities.isCreativeMode) {
            itemStack.stackSize--;
        }
        return itemStack;
    }

    private MCH_GunnerInfo resolveProfile() {
        if (this.gunnerProfileName == null || this.gunnerProfileName.trim().isEmpty()) {
            return null;
        }
        return MCH_GunnerInfoManager.get(this.gunnerProfileName.trim().toLowerCase(Locale.ROOT));
    }

    @SideOnly(Side.CLIENT)
    public int getColorFromItemStack(ItemStack itemStack, int layer) {
        if (!this.applyItemColorTint) {
            return 16777215;
        }
        if (!requiresMultipleRenderPasses())
            return this.primaryColor;
        return (layer == 0) ? this.primaryColor : this.secondaryColor;
    }

    @SideOnly(Side.CLIENT)
    public boolean requiresMultipleRenderPasses() {
        return this.useLayeredIcon && this.targetType != 2 && this.targetType != 3;
    }

    @SideOnly(Side.CLIENT)
    public IIcon getIconFromDamageForRenderPass(int p_77618_1_, int p_77618_2_) {
        if (!requiresMultipleRenderPasses())
            return super.getIconFromDamageForRenderPass(p_77618_1_, p_77618_2_);
        return (p_77618_2_ > 0) ? this.theIcon : super.getIconFromDamageForRenderPass(p_77618_1_, p_77618_2_);
    }

    @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister icon) {
        super.registerIcons(icon);
        if (requiresMultipleRenderPasses())
            this.theIcon = icon.registerIcon(getIconString() + "_overlay");
    }
}
