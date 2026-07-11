package mcheli.lweapon;

import mcheli.wrapper.W_Item;

public class MCH_ItemLightWeaponBullet extends W_Item {

    public MCH_ItemLightWeaponBullet(int par1) {
        this(par1, 2);
    }

    public MCH_ItemLightWeaponBullet(int par1, int maxStackSize) {
        super(par1);
        this.setMaxStackSize(maxStackSize);
        this.setMaxDamage(-1);
    }
}
