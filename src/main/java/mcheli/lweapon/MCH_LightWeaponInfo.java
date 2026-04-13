package mcheli.lweapon;

import mcheli.MCH_BaseInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MCH_LightWeaponInfo extends MCH_BaseInfo {

    public final String name;
    public String displayName;
    public HashMap<String, String> displayNameLang;
    public int itemID;
    public String weaponInfoName;
    public String ammoItemName;
    public int maxDurability;
    public int reloadTick;
    public double lockRangeOverride;
    public String hudType;
    public String modelName;
    public String textureName;
    public String scopeTexture;
    public String scopeOverlayTexture2;
    public String soundReload;
    public String soundFire;
    public boolean enableNightVision;
    public float[] zoomLevels;
    public List<String> recipeString;
    public List recipe;
    public boolean isShapedRecipe;
    public MCH_ItemLightWeaponBase item;

    public MCH_LightWeaponInfo(String name) {
        this.name = name;
        this.displayName = name;
        this.displayNameLang = new HashMap<String, String>();
        this.itemID = 0;
        this.weaponInfoName = name;
        this.ammoItemName = "";
        this.maxDurability = 10;
        this.reloadTick = 60;
        this.lockRangeOverride = -1.0D;
        this.hudType = "generic";
        this.modelName = name;
        this.textureName = name;
        this.scopeTexture = "";
        this.scopeOverlayTexture2 = "";
        this.soundReload = "fim92_reload";
        this.soundFire = "fim92_snd";
        this.enableNightVision = true;
        this.zoomLevels = new float[0];
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
        } else if (item.equalsIgnoreCase("WeaponInfoName")) {
            this.weaponInfoName = data.trim().isEmpty() ? this.name : data.trim();
        } else if (item.equalsIgnoreCase("AmmoItemName")) {
            this.ammoItemName = data.trim();
        } else if (item.equalsIgnoreCase("MaxDurability")) {
            this.maxDurability = this.toInt(data, 1, 10000);
        } else if (item.equalsIgnoreCase("ReloadTick")) {
            this.reloadTick = this.toInt(data, 1, 10000);
        } else if (item.equalsIgnoreCase("LockRangeOverride")) {
            this.lockRangeOverride = this.toDouble(data);
        } else if (item.equalsIgnoreCase("HudType")) {
            this.hudType = data.trim();
        } else if (item.equalsIgnoreCase("ModelName")) {
            this.modelName = data.trim().isEmpty() ? this.name : data.trim();
        } else if (item.equalsIgnoreCase("TextureName")) {
            this.textureName = data.trim().isEmpty() ? this.name : data.trim();
        } else if (item.equalsIgnoreCase("ScopeTexture")) {
            this.scopeTexture = data.trim();
        } else if (item.equalsIgnoreCase("ScopeOverlayTexture2")) {
            this.scopeOverlayTexture2 = data.trim();
        } else if (item.equalsIgnoreCase("SoundReload")) {
            this.soundReload = data.trim();
        } else if (item.equalsIgnoreCase("SoundFire")) {
            this.soundFire = data.trim();
        } else if (item.equalsIgnoreCase("EnableNightVision")) {
            this.enableNightVision = this.toBool(data, true);
        } else if (item.equalsIgnoreCase("ZoomLevels")) {
            String[] s = this.splitParam(data);
            List<Float> zooms = new ArrayList<Float>();
            for (String z : s) {
                if (!z.trim().isEmpty()) {
                    zooms.add(this.toFloat(z.trim(), 0.1F, 50.0F));
                }
            }
            this.zoomLevels = new float[zooms.size()];
            for (int i = 0; i < zooms.size(); i++) {
                this.zoomLevels[i] = zooms.get(i);
            }
        } else if (item.equalsIgnoreCase("AddRecipe") || item.equalsIgnoreCase("AddShapelessRecipe")) {
            this.isShapedRecipe = item.equalsIgnoreCase("AddRecipe");
            this.recipeString.add(data.toUpperCase());
        }
    }
}

