package mcheli.block;

import mcheli.MCH_BaseInfo;
import net.minecraft.block.Block;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MCH_BlockInfo extends MCH_BaseInfo {

    public final String name;
    public String displayName;
    public HashMap<String, String> displayNameLang;
    public int blockID;
    public String materialName;
    public float hardness;
    public float resistance;
    public float lightLevel;
    public String stepSound;
    public String creativeTab;
    public String textureName;
    public String textureActive;
    public String textureSleep;
    public String textureError;
    public List<String> recipeString;
    public List recipe;
    public boolean isShapedRecipe;
    public Block block;
    public boolean enableSpawner;
    public float checkRadius;
    public int checkIntervalTick;
    public boolean detectPlayers;
    public boolean detectMobs;
    public boolean detectAnimals;
    public boolean detectVehicles;
    public boolean detectGunners;
    public boolean ignoreCreativePlayer;
    public boolean ignoreSpectator;
    public String spawnMode;
    public int cooldownTick;
    public float spawnYOffset;
    public String spawnYawMode;
    public float spawnYaw;
    public boolean oneTimeKeepSleepTexture;
    public List<String> vehiclePool;
    public List<Integer> vehicleWeight;
    public int spawnVehicleCount;
    public String vehicleExtraNbt;
    public boolean spawnGunner;
    public String gunnerMode;
    public int gunnerSeatIndex;
    public int gunnerTargetType;
    public float gunnerYaw;
    public float gunnerPitch;
    public String gunnerFactionId;
    public String gunnerFactionName;
    public boolean autoCreateFaction;
    public int autoCreateFactionColor;
    public int stateSyncTick;

    public MCH_BlockInfo(String name) {
        this.name = name;
        this.displayName = name;
        this.displayNameLang = new HashMap<String, String>();
        this.blockID = 0;
        this.materialName = "iron";
        this.hardness = 3.0F;
        this.resistance = 10.0F;
        this.lightLevel = 0.0F;
        this.stepSound = "metal";
        this.creativeTab = "block";
        this.textureName = name;
        this.textureActive = this.textureName;
        this.textureSleep = this.textureName;
        this.textureError = this.textureName;
        this.recipeString = new ArrayList<String>();
        this.recipe = new ArrayList();
        this.isShapedRecipe = true;
        this.block = null;
        this.enableSpawner = false;
        this.checkRadius = 10.0F;
        this.checkIntervalTick = 20;
        this.detectPlayers = true;
        this.detectMobs = true;
        this.detectAnimals = true;
        this.detectVehicles = true;
        this.detectGunners = true;
        this.ignoreCreativePlayer = true;
        this.ignoreSpectator = false;
        this.spawnMode = "interval";
        this.cooldownTick = 1200;
        this.spawnYOffset = 1.0F;
        this.spawnYawMode = "random";
        this.spawnYaw = 0.0F;
        this.oneTimeKeepSleepTexture = true;
        this.vehiclePool = new ArrayList<String>();
        this.vehicleWeight = new ArrayList<Integer>();
        this.spawnVehicleCount = 1;
        this.vehicleExtraNbt = "";
        this.spawnGunner = false;
        this.gunnerMode = "none";
        this.gunnerSeatIndex = 0;
        this.gunnerTargetType = 0;
        this.gunnerYaw = 0.0F;
        this.gunnerPitch = 0.0F;
        this.gunnerFactionId = "";
        this.gunnerFactionName = "";
        this.autoCreateFaction = true;
        this.autoCreateFactionColor = 0x3A66FF;
        this.stateSyncTick = 5;
    }

    public void loadItemData(String item, String data) {
        if (item.equalsIgnoreCase("DisplayName")) {
            this.displayName = data;
        } else if (item.equalsIgnoreCase("AddDisplayName")) {
            String[] s = this.splitParam(data);
            if (s.length == 2) {
                this.displayNameLang.put(s[0].trim(), s[1].trim());
            }
        } else if (item.equalsIgnoreCase("BlockID")) {
            this.blockID = this.toInt(data, 0, '\uffff');
        } else if (item.equalsIgnoreCase("Material")) {
            this.materialName = data.trim().toLowerCase();
        } else if (item.equalsIgnoreCase("Hardness")) {
            this.hardness = this.toFloat(data, 0.0F, 10000.0F);
        } else if (item.equalsIgnoreCase("Resistance")) {
            this.resistance = this.toFloat(data, 0.0F, 10000.0F);
        } else if (item.equalsIgnoreCase("LightLevel")) {
            this.lightLevel = this.toFloat(data, 0.0F, 1.0F);
        } else if (item.equalsIgnoreCase("StepSound")) {
            this.stepSound = data.trim().toLowerCase();
        } else if (item.equalsIgnoreCase("CreativeTab")) {
            this.creativeTab = data.trim().toLowerCase();
        } else if (item.equalsIgnoreCase("TextureName")) {
            this.textureName = data.trim().isEmpty() ? this.name : data.trim();
            this.textureActive = this.textureName;
            this.textureSleep = this.textureName;
            this.textureError = this.textureName;
        } else if (item.equalsIgnoreCase("EnableSpawner")) {
            this.enableSpawner = this.toBool(data.trim(), this.enableSpawner);
        } else if (item.equalsIgnoreCase("CheckRadius")) {
            this.checkRadius = this.toFloat(data.trim(), 1.0F, 128.0F);
        } else if (item.equalsIgnoreCase("CheckIntervalTick")) {
            this.checkIntervalTick = this.toInt(data.trim(), 1, 1200);
        } else if (item.equalsIgnoreCase("DetectPlayers")) {
            this.detectPlayers = this.toBool(data.trim(), this.detectPlayers);
        } else if (item.equalsIgnoreCase("DetectMobs")) {
            this.detectMobs = this.toBool(data.trim(), this.detectMobs);
        } else if (item.equalsIgnoreCase("DetectAnimals")) {
            this.detectAnimals = this.toBool(data.trim(), this.detectAnimals);
        } else if (item.equalsIgnoreCase("DetectVehicles")) {
            this.detectVehicles = this.toBool(data.trim(), this.detectVehicles);
        } else if (item.equalsIgnoreCase("DetectGunners")) {
            this.detectGunners = this.toBool(data.trim(), this.detectGunners);
        } else if (item.equalsIgnoreCase("IgnoreCreativePlayer")) {
            this.ignoreCreativePlayer = this.toBool(data.trim(), this.ignoreCreativePlayer);
        } else if (item.equalsIgnoreCase("IgnoreSpectator")) {
            this.ignoreSpectator = this.toBool(data.trim(), this.ignoreSpectator);
        } else if (item.equalsIgnoreCase("SpawnMode")) {
            this.spawnMode = data.trim().toLowerCase();
        } else if (item.equalsIgnoreCase("CooldownTick")) {
            this.cooldownTick = this.toInt(data.trim(), 1, 120000);
        } else if (item.equalsIgnoreCase("SpawnYOffset")) {
            this.spawnYOffset = this.toFloat(data.trim(), -64.0F, 64.0F);
        } else if (item.equalsIgnoreCase("SpawnYawMode")) {
            this.spawnYawMode = data.trim().toLowerCase();
        } else if (item.equalsIgnoreCase("SpawnYaw")) {
            this.spawnYaw = this.toFloat(data.trim(), -360.0F, 360.0F);
        } else if (item.equalsIgnoreCase("OneTimeKeepSleepTexture")) {
            this.oneTimeKeepSleepTexture = this.toBool(data.trim(), this.oneTimeKeepSleepTexture);
        } else if (item.equalsIgnoreCase("VehiclePool")) {
            this.vehiclePool.clear();
            String[] entries = data.split("\\|");
            for (String entry : entries) {
                String v = entry.trim();
                if (!v.isEmpty()) {
                    this.vehiclePool.add(v.toLowerCase());
                }
            }
        } else if (item.equalsIgnoreCase("VehicleWeight")) {
            this.vehicleWeight.clear();
            String[] entries = data.split("\\|");
            for (String entry : entries) {
                String v = entry.trim();
                if (!v.isEmpty()) {
                    this.vehicleWeight.add(this.toInt(v, 1, 100000));
                }
            }
        } else if (item.equalsIgnoreCase("SpawnVehicleCount")) {
            this.spawnVehicleCount = this.toInt(data.trim(), 1, 8);
        } else if (item.equalsIgnoreCase("VehicleExtraNbt")) {
            this.vehicleExtraNbt = data.trim();
        } else if (item.equalsIgnoreCase("SpawnGunner")) {
            this.spawnGunner = this.toBool(data.trim(), this.spawnGunner);
        } else if (item.equalsIgnoreCase("GunnerMode")) {
            this.gunnerMode = data.trim().toLowerCase();
        } else if (item.equalsIgnoreCase("GunnerSeatIndex")) {
            this.gunnerSeatIndex = this.toInt(data.trim(), 0, 64);
        } else if (item.equalsIgnoreCase("GunnerTargetType")) {
            this.gunnerTargetType = this.toInt(data.trim(), 0, 3);
        } else if (item.equalsIgnoreCase("GunnerYaw")) {
            this.gunnerYaw = this.toFloat(data.trim(), -360.0F, 360.0F);
        } else if (item.equalsIgnoreCase("GunnerPitch")) {
            this.gunnerPitch = this.toFloat(data.trim(), -90.0F, 90.0F);
        } else if (item.equalsIgnoreCase("GunnerFactionId")) {
            this.gunnerFactionId = data.trim();
        } else if (item.equalsIgnoreCase("GunnerFactionName")) {
            this.gunnerFactionName = data.trim();
        } else if (item.equalsIgnoreCase("AutoCreateFaction")) {
            this.autoCreateFaction = this.toBool(data.trim(), this.autoCreateFaction);
        } else if (item.equalsIgnoreCase("AutoCreateFactionColor")) {
            this.autoCreateFactionColor = this.hex2dec(data.trim());
        } else if (item.equalsIgnoreCase("TextureActive")) {
            this.textureActive = data.trim().isEmpty() ? this.textureName : data.trim();
        } else if (item.equalsIgnoreCase("TextureSleep")) {
            this.textureSleep = data.trim().isEmpty() ? this.textureName : data.trim();
        } else if (item.equalsIgnoreCase("TextureError")) {
            this.textureError = data.trim().isEmpty() ? this.textureName : data.trim();
        } else if (item.equalsIgnoreCase("StateSyncTick")) {
            this.stateSyncTick = this.toInt(data.trim(), 1, 200);
        } else if (item.equalsIgnoreCase("AddRecipe") || item.equalsIgnoreCase("AddShapelessRecipe")) {
            this.isShapedRecipe = item.equalsIgnoreCase("AddRecipe");
            this.recipeString.add(data.toUpperCase());
        }
    }
}
