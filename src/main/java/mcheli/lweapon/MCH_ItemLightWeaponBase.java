package mcheli.lweapon;

import mcheli.wrapper.W_Item;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.world.World;

public class MCH_ItemLightWeaponBase extends W_Item {

    public final MCH_ItemLightWeaponBullet bullet;
    private final String weaponInfoName;
    private final int reloadTick;
    private final double lockRangeOverride;
    private final String hudType;
    private final String modelName;
    private final String textureName;
    private final String scopeTexture;
    private final String scopeOverlayTexture2;
    private final String soundReload;
    private final boolean enableNightVision;
    private final float[] zoomLevels;


    public MCH_ItemLightWeaponBase(int par1, MCH_ItemLightWeaponBullet bullet) {
        this(par1, bullet, null);
    }

    public MCH_ItemLightWeaponBase(int par1, MCH_ItemLightWeaponBullet bullet, MCH_LightWeaponInfo info) {
        super(par1);
        this.setMaxDamage(info != null ? info.maxDurability : 10);
        this.setMaxStackSize(1);
        this.bullet = bullet;
        this.weaponInfoName = info != null ? info.weaponInfoName : "";
        this.reloadTick = info != null ? info.reloadTick : 60;
        this.lockRangeOverride = info != null ? info.lockRangeOverride : -1.0D;
        this.hudType = info != null && !info.hudType.isEmpty() ? info.hudType : "";
        this.modelName = info != null && !info.modelName.isEmpty() ? info.modelName : "";
        this.textureName = info != null && !info.textureName.isEmpty() ? info.textureName : "";
        this.scopeTexture = info != null && !info.scopeTexture.isEmpty() ? info.scopeTexture : "";
        this.scopeOverlayTexture2 = info != null && !info.scopeOverlayTexture2.isEmpty() ? info.scopeOverlayTexture2 : "";
        this.soundReload = info != null && !info.soundReload.isEmpty() ? info.soundReload : "fim92_reload";
        this.enableNightVision = info == null || info.enableNightVision;
        this.zoomLevels = info != null ? info.zoomLevels : new float[0];
    }

    public static String getName(ItemStack itemStack) {
        if (itemStack != null && itemStack.getItem() instanceof MCH_ItemLightWeaponBase) {
            String name = itemStack.getUnlocalizedName();
            int li = name.lastIndexOf(":");
            if (li >= 0) {
                name = name.substring(li + 1);
            }

            return name;
        } else {
            return "";
        }
    }

    public static boolean isHeld(EntityPlayer player) {
        ItemStack is = player != null ? player.getHeldItem() : null;
        return is != null && is.getItem() instanceof MCH_ItemLightWeaponBase ? player.getItemInUseDuration() > 10 : false;
    }

    public void onUsingTick(ItemStack stack, EntityPlayer player, int count) {
        if (!this.enableNightVision) {
            return;
        }
        PotionEffect pe = player.getActivePotionEffect(Potion.nightVision);
        if (pe != null && pe.getDuration() < 220) {
            player.addPotionEffect(new PotionEffect(Potion.nightVision.getId(), 250, 0, false));
        }

    }

    public boolean onEntitySwing(EntityLivingBase entityLiving, ItemStack stack) {
        return true;
    }

    public EnumAction getItemUseAction(ItemStack par1ItemStack) {
        return EnumAction.bow;
    }

    public int getMaxItemUseDuration(ItemStack par1ItemStack) {
        return 72000;
    }

    public ItemStack onItemRightClick(ItemStack par1ItemStack, World par2World, EntityPlayer par3EntityPlayer) {
        if (par1ItemStack != null) {
            par3EntityPlayer.setItemInUse(par1ItemStack, this.getMaxItemUseDuration(par1ItemStack));
        }

        return par1ItemStack;
    }

    public String getWeaponInfoName(ItemStack stack) {
        if (this.weaponInfoName != null && !this.weaponInfoName.isEmpty()) {
            return this.weaponInfoName;
        }
        return getName(stack);
    }

    public int getReloadTick() {
        return this.reloadTick;
    }

    public double getLockRangeOverride() {
        return this.lockRangeOverride;
    }

    public String getHudType(ItemStack stack) {
        if (this.hudType != null && !this.hudType.isEmpty()) {
            return this.hudType;
        }
        return getName(stack);
    }

    public String getReloadSound() {
        return this.soundReload;
    }

    public String getModelName(ItemStack stack) {
        if (this.modelName != null && !this.modelName.isEmpty()) {
            return this.modelName;
        }
        return getName(stack);
    }

    public String getTextureName(ItemStack stack) {
        if (this.textureName != null && !this.textureName.isEmpty()) {
            return this.textureName;
        }
        return getName(stack);
    }

    public String getScopeTexture() {
        return this.scopeTexture;
    }

    public String getScopeOverlayTexture2() {
        return this.scopeOverlayTexture2;
    }

    public boolean isNightVisionEnabled() {
        return this.enableNightVision;
    }

    public float[] getZoomLevels() {
        return this.zoomLevels != null ? this.zoomLevels : new float[0];
    }
}
