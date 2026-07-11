package mcheli.lweapon;

import mcheli.MCH_BaseInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MCH_LightWeaponAmmoInfo extends MCH_BaseInfo {

    public final String name;
    public String displayName;
    public HashMap<String, String> displayNameLang;
    public int itemID;
    public int stackSize;
    public String textureName;
    public List<String> recipeString;
    public List recipe;
    public boolean isShapedRecipe;
    public MCH_ItemLightWeaponBullet item;

    public MCH_LightWeaponAmmoInfo(String name) {
        this.name = name;
        this.displayName = name;
        this.displayNameLang = new HashMap<String, String>();
        this.itemID = 0;
        this.stackSize = 2;
        this.textureName = name;
        this.recipeString = new ArrayList<String>();
        this.recipe = new ArrayList();
        this.isShapedRecipe = true;
        this.item = null;
    }

    public void loadItemData(String item, String data) {
        if (item.equalsIgnoreCase("DisplayName")) {
            this.displayName = data;
        } else if (item.equalsIgnoreCase("AddDisplayName")) {
            String[] s = this.splitParam(data);
            if (s.length == 2) {
                this.displayNameLang.put(s[0].trim(), s[1].trim());
            }
        } else if (item.equalsIgnoreCase("ItemID")) {
            this.itemID = this.toInt(data, 0, '\uffff');
        } else if (item.equalsIgnoreCase("StackSize")) {
            this.stackSize = this.toInt(data, 1, 64);
        } else if (item.equalsIgnoreCase("TextureName")) {
            this.textureName = data.trim().isEmpty() ? this.name : data.trim();
        } else if (item.equalsIgnoreCase("AddRecipe") || item.equalsIgnoreCase("AddShapelessRecipe")) {
            this.isShapedRecipe = item.equalsIgnoreCase("AddRecipe");
            this.recipeString.add(data.toUpperCase());
        }
    }
}

