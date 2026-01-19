package mcheli.aircraft;

import mcheli.MCH_Achievement;
import mcheli.MCH_Config;
import mcheli.MCH_I18n;
import mcheli.MCH_MOD;
import mcheli.weapon.MCH_WeaponInfo;
import mcheli.weapon.MCH_WeaponInfoManager;
import mcheli.wrapper.W_EntityPlayer;
import mcheli.wrapper.W_Item;
import mcheli.wrapper.W_MovingObjectPosition;
import mcheli.wrapper.W_WorldFunc;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDispenser;
import net.minecraft.block.BlockSponge;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityMinecartEmpty;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import java.util.List;

public abstract class MCH_ItemAircraft extends W_Item {

    private static boolean isRegistedDispenseBehavior = false;

    public MCH_ItemAircraft(int i) {
        super(i);
    }

    public static void registerDispenseBehavior(Item item) {
        if (!isRegistedDispenseBehavior) {
            BlockDispenser.dispenseBehaviorRegistry.putObject(item, new MCH_ItemAircraftDispenseBehavior());
        }
    }

    public static float roundFloat(float value, int points) {
        int pow = 10;
        for (int i = 1; i < points; i++)
            pow *= 10;
        float result = value * pow;

        return (float) (int) ((result - (int) result) >= 0.5f ? result + 1 : result) / pow;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void addInformation(ItemStack stack, EntityPlayer player, List lines, boolean advancedTooltips) {
        MCH_AircraftInfo info = getAircraftInfo();
        if (info == null) return;

        KeyBinding shift = Minecraft.getMinecraft().gameSettings.keyBindSneak;

        lines.add("\u00a7b\u00a7o" + info.displayName);

        if (!GameSettings.isKeyDown(shift)) {
            lines.add(MCH_I18n.format("aircraft.info.hold_shift", GameSettings.getKeyDisplayString(shift.getKeyCode())));
        } else {
            lines.add("");

            lines.add("\u00a79" + MCH_I18n.format("aircraft.info.max_hp") + "\u00a77: " + info.maxHp);
            lines.add("\u00a79" + MCH_I18n.format("aircraft.info.engine_shutdown_threshold") + "\u00a77: " + info.engineShutdownThreshold + "%");
            lines.add("\u00a79" + MCH_I18n.format("aircraft.info.armor_min_damage") + "\u00a77: " + roundFloat(info.armorMinDamage, 2));
            lines.add("\u00a79" + MCH_I18n.format("aircraft.info.armor_max_damage") + "\u00a77: " + roundFloat(info.armorMaxDamage, 2));
            lines.add("\u00a79" + MCH_I18n.format("aircraft.info.armor_damage_factor") + "\u00a77: " + roundFloat(info.armorDamageFactor, 2));
            lines.add("\u00a79" + MCH_I18n.format("aircraft.info.throttle_up_down") + "\u00a77: " + roundFloat(info.throttleUpDown, 2));
            lines.add("\u00a79" + MCH_I18n.format("aircraft.info.camera_zoom") + "\u00a77: " + info.cameraZoom);
            if (info.stealth != 0) {
                lines.add("\u00a79" + MCH_I18n.format("aircraft.info.stealth") + "\u00a77: " + roundFloat(info.stealth, 2));
            }
            lines.add("\u00a79" + MCH_I18n.format("aircraft.info.speed") + "\u00a77: " + roundFloat(info.speed, 2));
            if (info.hasRWR) {
                lines.add("\u00a79" + MCH_I18n.format("aircraft.info.radar_type") + "\u00a77: " + info.radarType);
                lines.add("\u00a79" + MCH_I18n.format("aircraft.info.rwr_type") + "\u00a77: " + info.rwrType);
            }
            lines.add("\u00a79" + MCH_I18n.format("aircraft.info.armor_explosion_damage_multiplier") + "\u00a77: " + roundFloat(info.armorExplosionDamageMultiplier, 2));

            lines.add("");
            if (info.haveFlare()) {
                lines.add("\u00a7e" + MCH_I18n.format("aircraft.info.flare"));
            }
            if (info.haveChaff()) {
                lines.add("\u00a7e" + MCH_I18n.format("aircraft.info.chaff"));
            }
            if (info.haveAPS()) {
                lines.add("\u00a7e" + MCH_I18n.format("aircraft.info.aps"));
            }
            if (info.haveMaintenance()) {
                lines.add("\u00a7e" + MCH_I18n.format("aircraft.info.maintenance"));
            }
            if (info.hasPhotoelectricJammer) {
                lines.add("\u00a7e" + MCH_I18n.format("aircraft.info.photoelectric_jammer"));
            }
            if (info.hasDIRCM) {
                lines.add("\u00a7e" + MCH_I18n.format("aircraft.info.dircm"));
            }
            if (info.hasRWR) {
                lines.add("\u00a7e" + MCH_I18n.format("aircraft.info.rwr"));
            }

            int num = info.getWeaponNum();
            if (num > 0) {
                lines.add("");
                lines.add("\u00a79" + MCH_I18n.format("aircraft.info.weapon_list") + ": ");
                for (int i = 0; i < num; i++) {
                    String s = info.getWeaponSetNameById(i);
                    if (s != null) {
                        MCH_WeaponInfo wi = MCH_WeaponInfoManager.get(s);
                        if (wi != null) {
                            lines.add("\u00a77" + wi.displayName);
                        }
                    }
                }
            } else {
                lines.add("\u00a77" + MCH_I18n.format("aircraft.info.no_weapon"));
            }

        }
    }

    public abstract MCH_AircraftInfo getAircraftInfo();

    public abstract MCH_EntityAircraft createAircraft(World var1, double var2, double var4, double var6, ItemStack var8);

    public MCH_EntityAircraft onTileClick(ItemStack itemStack, World world, float rotationYaw, int x, int y, int z) {
        MCH_EntityAircraft ac = this.createAircraft(world, (double) ((float) x + 0.5F), (double) ((float) y + 1.0F), (double) ((float) z + 0.5F), itemStack);
        if (ac == null) {
            return null;
        } else {
            ac.initRotationYaw((float) (((MathHelper.floor_double((double) (rotationYaw * 4.0F / 360.0F) + 0.5D) & 3) - 1) * 90));
            return !world.getCollidingBoundingBoxes(ac, ac.boundingBox.expand(-0.1D, -0.1D, -0.1D)).isEmpty() ? null : ac;
        }
    }

    public String toString() {
        MCH_AircraftInfo info = this.getAircraftInfo();
        return info != null ? super.toString() + "(" + info.getDirectoryName() + ":" + info.name + ")" : super.toString() + "(null)";
    }

    public ItemStack onItemRightClick(ItemStack par1ItemStack, World world, EntityPlayer player) {
        float f = 1.0F;
        float f1 = player.prevRotationPitch + (player.rotationPitch - player.prevRotationPitch) * f;
        float f2 = player.prevRotationYaw + (player.rotationYaw - player.prevRotationYaw) * f;
        double d0 = player.prevPosX + (player.posX - player.prevPosX) * (double) f;
        double d1 = player.prevPosY + (player.posY - player.prevPosY) * (double) f + 1.62D - (double) player.yOffset;
        double d2 = player.prevPosZ + (player.posZ - player.prevPosZ) * (double) f;
        Vec3 vec3 = W_WorldFunc.getWorldVec3(world, d0, d1, d2);
        float f3 = MathHelper.cos(-f2 * 0.017453292F - 3.1415927F);
        float f4 = MathHelper.sin(-f2 * 0.017453292F - 3.1415927F);
        float f5 = -MathHelper.cos(-f1 * 0.017453292F);
        float f6 = MathHelper.sin(-f1 * 0.017453292F);
        float f7 = f4 * f5;
        float f8 = f3 * f5;
        double d3 = 5.0D;
        Vec3 vec31 = vec3.addVector((double) f7 * d3, (double) f6 * d3, (double) f8 * d3);
        MovingObjectPosition mop = W_WorldFunc.clip(world, vec3, vec31, true);
        if (mop == null) {
            return par1ItemStack;
        } else {
            Vec3 vec32 = player.getLook(f);
            boolean flag = false;
            float f9 = 1.0F;
            List list = world.getEntitiesWithinAABBExcludingEntity(player, player.boundingBox.addCoord(vec32.xCoord * d3, vec32.yCoord * d3, vec32.zCoord * d3).expand((double) f9, (double) f9, (double) f9));

            for (int i = 0; i < list.size(); ++i) {
                Entity block = (Entity) list.get(i);
                if (block.canBeCollidedWith()) {
                    float f10 = block.getCollisionBorderSize();
                    AxisAlignedBB axisalignedbb = block.boundingBox.expand((double) f10, (double) f10, (double) f10);
                    if (axisalignedbb.isVecInside(vec3)) {
                        flag = true;
                    }
                }
            }

            if (flag) {
                return par1ItemStack;
            } else {
                if (W_MovingObjectPosition.isHitTypeTile(mop)) {
                    MCH_Config var10000 = MCH_MOD.config;
                    if (MCH_Config.PlaceableOnSpongeOnly.prmBool) {
                        Block var32 = world.getBlock(mop.blockX, mop.blockY, mop.blockZ);
                        if (!(var32 instanceof BlockSponge)) {
                            return par1ItemStack;
                        }
                    }


                    this.spawnAircraft(par1ItemStack, world, player, mop.blockX, mop.blockY, mop.blockZ);
                }

                return par1ItemStack;
            }
        }
    }

    public MCH_EntityAircraft spawnAircraft(ItemStack itemStack, World world, EntityPlayer player, int x, int y, int z) {
        MCH_EntityAircraft ac = this.onTileClick(itemStack, world, player.rotationYaw, x, y, z);
        if (ac != null) {
            if (ac.isUAV()) {
                if (world.isRemote) {
                    if (ac.isSmallUAV()) {
                        W_EntityPlayer.addChatMessage(player, "Please use the UAV station OR Portable Controller");
                    } else {
                        W_EntityPlayer.addChatMessage(player, "Please use the UAV station");
                    }
                }
                ac = null;
            } else {
                if (!world.isRemote) {
                    ac.getAcDataFromItem(itemStack);
                    world.spawnEntityInWorld(ac);
                    MCH_Achievement.addStat(player, MCH_Achievement.welcome, 1);
                }
                if (!player.capabilities.isCreativeMode) {
                    --itemStack.stackSize;
                }
            }
        }
        return ac;
    }

    public void rideEntity(ItemStack item, Entity target, EntityPlayer player) {
        if (!MCH_Config.PlaceableOnSpongeOnly.prmBool && target instanceof EntityMinecartEmpty && target.riddenByEntity == null) {
            MCH_EntityAircraft ac = this.spawnAircraft(item, player.worldObj, player, (int) target.posX, (int) target.posY + 2, (int) target.posZ);
            if (!player.worldObj.isRemote && ac != null) {
                ac.mountEntity(target);
            }
        }

    }

}
