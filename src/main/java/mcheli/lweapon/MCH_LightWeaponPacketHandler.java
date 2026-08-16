package mcheli.lweapon;

import com.google.common.io.ByteArrayDataInput;
import mcheli.weapon.MCH_IEntityLockChecker;
import mcheli.weapon.MCH_WeaponAAMissile;
import mcheli.weapon.MCH_WeaponBase;
import mcheli.weapon.MCH_WeaponCreator;
import mcheli.weapon.MCH_WeaponParam;
import mcheli.wrapper.W_EntityPlayer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Vec3;

public class MCH_LightWeaponPacketHandler {

    public static void onPacket_PlayerControl(EntityPlayer player, ByteArrayDataInput data) {

        if (!player.worldObj.isRemote) {
            System.out.println("Server: Handling player control packet for player: " + player.getCommandSenderName());
            MCH_PacketLightWeaponPlayerControl pc = new MCH_PacketLightWeaponPlayerControl();
            pc.readData(data);
            System.out.println("Server: Packet camMode: " + pc.camMode);
            //if(pc.camMode == 1) {
            //   //player.removePotionEffect(Potion.nightVision.getId());
            //}

            ItemStack is = player.getHeldItem();
            if (is != null) {
                if (is.getItem() instanceof MCH_ItemLightWeaponBase) {
                    MCH_ItemLightWeaponBase lweapon = (MCH_ItemLightWeaponBase) is.getItem();
                    //System.out.println("Server: Handling light weapon: " + lweapon.getUnlocalizedName());
                    // if(pc.camMode == 2 && MCH_ItemLightWeaponBase.isHeld(player)) {
                    //    //System.out.println("Server: Adding night vision effect to player: " + player.getCommandSenderName());
                    //    //player.addPotionEffect(new PotionEffect(Potion.nightVision.getId(), 255, 0, false));
                    // }

                    // if(pc.camMode > 0) {
                    //    //MCH_Lib.DbgLog(false, "MCH_LightWeaponPacketHandler NV=%s", new Object[]{pc.camMode == 2?"ON":"OFF"});
                    // }

                    if (pc.useWeapon && is.getItemDamage() < is.getMaxDamage()) {
                        String name = lweapon.getWeaponInfoName(is);
                        MCH_WeaponBase w = MCH_WeaponCreator.createWeapon(player.worldObj, name, Vec3.createVectorHelper(0.0D, 0.0D, 0.0D), 0.0F, 0.0F, (MCH_IEntityLockChecker) null, false);
                        if (w == null) {
                            return;
                        }
                        MCH_WeaponParam prm = new MCH_WeaponParam();
                        prm.entity = player;
                        prm.user = player;

                        double launchX = pc.useWeaponPosX;
                        double launchY = pc.useWeaponPosY;
                        double launchZ = pc.useWeaponPosZ;
                        if (w instanceof MCH_WeaponAAMissile) {
                            // Handheld AAM packets historically used player.posY (feet) as the launch point.
                            // Rebuild the origin on the server at shoulder/eye height so terrain at the
                            // player's feet cannot reject or immediately eat an otherwise valid launch.
                            launchX = player.posX;
                            launchY = player.posY + player.getEyeHeight() - 0.15D;
                            launchZ = player.posZ;
                        }

                        prm.setPosAndRot(launchX, launchY, launchZ, player.rotationYaw, player.rotationPitch);
                        prm.option1 = pc.useWeaponOption1;
                        prm.option2 = pc.useWeaponOption2;

                        boolean fired = w.shot(prm);
                        if (fired) {
                            // Only consume ammunition after the server actually accepted/spawned the shot.
                            if (!player.capabilities.isCreativeMode && is.getMaxDamage() == 1) {
                                --is.stackSize;
                            }

                            if (is.getMaxDamage() > 1) {
                                is.setItemDamage(is.getMaxDamage());
                            }
                        }
                    } else if (pc.cmpReload > 0 && is.getItemDamage() > 1 && W_EntityPlayer.hasItem(player, lweapon.bullet)) {
                        if (!player.capabilities.isCreativeMode) {
                            W_EntityPlayer.consumeInventoryItem(player, lweapon.bullet);
                        }

                        is.setItemDamage(0);
                    }

                }
            }
        }
    }
}
