package mcheli;


import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.*;
import cpw.mods.fml.common.registry.EntityRegistry;
import cpw.mods.fml.common.registry.GameRegistry;
import mcheli.aircraft.*;
import mcheli.block.MCH_BlockInfo;
import mcheli.block.MCH_BlockInfoManager;
import mcheli.block.MCH_ConfigBlock;
import mcheli.block.MCH_ConfigSpawnerBlock;
import mcheli.block.MCH_ConfigSpawnerTileEntity;
import mcheli.block.MCH_DraftingTableBlock;
import mcheli.block.MCH_DraftingTableTileEntity;
import mcheli.chain.MCH_EntityChain;
import mcheli.chain.MCH_ItemChain;
import mcheli.command.MCH_Command;
import mcheli.command.MCH_CommandAddGunner;
import mcheli.container.MCH_EntityContainer;
import mcheli.container.MCH_ItemContainer;
import mcheli.flare.MCH_EntityChaff;
import mcheli.flare.MCH_EntityFlare;
import mcheli.gltd.MCH_EntityGLTD;
import mcheli.gltd.MCH_ItemGLTD;
import mcheli.gui.MCH_GuiCommonHandler;
import mcheli.helicopter.MCH_EntityHeli;
import mcheli.helicopter.MCH_HeliInfo;
import mcheli.helicopter.MCH_HeliInfoManager;
import mcheli.helicopter.MCH_ItemHeli;
import mcheli.lweapon.MCH_LightWeaponAmmoInfo;
import mcheli.lweapon.MCH_LightWeaponAmmoInfoManager;
import mcheli.lweapon.MCH_LightWeaponInfo;
import mcheli.lweapon.MCH_LightWeaponInfoManager;
import mcheli.lweapon.MCH_ItemLightWeaponBase;
import mcheli.lweapon.MCH_ItemLightWeaponBullet;
import mcheli.mob.MCH_EntityGunner;
import mcheli.mob.MCH_GunnerInfo;
import mcheli.mob.MCH_GunnerInfoManager;
import mcheli.mob.MCH_ItemSpawnGunner;
import mcheli.network.PacketHandler;
import mcheli.parachute.MCH_EntityParachute;
import mcheli.parachute.MCH_ItemParachute;
import mcheli.plane.MCP_EntityPlane;
import mcheli.plane.MCP_ItemPlane;
import mcheli.plane.MCP_PlaneInfo;
import mcheli.plane.MCP_PlaneInfoManager;
import mcheli.tank.MCH_EntityTank;
import mcheli.tank.MCH_ItemTank;
import mcheli.tank.MCH_TankInfo;
import mcheli.tank.MCH_TankInfoManager;
import mcheli.throwable.MCH_EntityThrowable;
import mcheli.throwable.MCH_ItemThrowable;
import mcheli.throwable.MCH_ThrowableInfo;
import mcheli.throwable.MCH_ThrowableInfoManager;
import mcheli.tool.MCH_ItemWrench;
import mcheli.tool.rangefinder.MCH_ItemRangeFinder;
import mcheli.uav.MCH_EntityUavStation;
import mcheli.uav.MCH_ItemUavStation;
import mcheli.vehicle.MCH_EntityVehicle;
import mcheli.vehicle.MCH_ItemVehicle;
import mcheli.vehicle.MCH_VehicleInfo;
import mcheli.vehicle.MCH_VehicleInfoManager;
import mcheli.weapon.*;
import mcheli.wrapper.*;
import net.minecraft.command.CommandHandler;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item.ToolMaterial;
import net.minecraft.item.Item;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.common.MinecraftForge;

import java.io.File;
import java.util.Iterator;
import java.util.Locale;

@Mod(
    modid = "mcheli",
    name = "MCH-Reforged",
    dependencies = "required-after:Forge@[10.13.2.1230,)"
)
@NetworkMod(
    clientSideRequired = true,
    serverSideRequired = false
)
public class MCH_MOD {

    public static final String MOD_ID = "mcheli";
    public static final String DOMAIN = "mcheli";
    public static final String MCVER = "1.7.10";
    public static final String MOD_CH = "MCHeli_CH";
    public static final PacketHandler newPacketHandler = new PacketHandler();
    public static final MCH_EntityInfoManager entityInfoManager = new MCH_EntityInfoManager();
    public static String VER = "";
    @Instance("mcheli")
    public static MCH_MOD instance;
    @SidedProxy(
        clientSide = "mcheli.MCH_ClientProxy",
        serverSide = "mcheli.MCH_CommonProxy"
    )
    public static MCH_CommonProxy proxy;
    public static MCH_PacketHandler packetHandler = new MCH_PacketHandler();
    public static MCH_Config config;
    public static String sourcePath;
    public static MCH_InvisibleItem invisibleItem;
    public static MCH_ItemGLTD itemGLTD;
    public static MCH_ItemLightWeaponBullet itemStingerBullet;
    public static MCH_ItemLightWeaponBase itemStinger;
    public static MCH_ItemLightWeaponBullet itemJavelinBullet;
    public static MCH_ItemLightWeaponBase itemJavelin;
    public static MCH_ItemLightWeaponBase itemRpg;
    public static MCH_ItemLightWeaponBullet itemRpgBullet;
    public static MCH_ItemUavStation[] itemUavStation;
    public static MCH_ItemParachute itemParachute;
    public static MCH_ItemContainer itemContainer;
    public static MCH_ItemChain itemChain;
    public static MCH_ItemFuel itemFuel;
    public static MCH_ItemWrench itemWrench;
    public static MCH_ItemRangeFinder itemRangeFinder;
    public static MCH_ItemSpawnGunner itemSpawnGunnerVsPlayer;
    public static MCH_ItemSpawnGunner itemSpawnGunnerVsMonster;
    public static MCH_ItemSpawnGunner itemSpawnGunnerAA;
    public static MCH_ItemSpawnGunner itemSpawnGunnerEnemy;
    public static MCH_ItemSpawnGunner itemSpawnGunnerVsMonsterStupid;
    public static MCH_ItemSpawnGunner itemSpawnGunnerEnemyStupid;
    public static MCH_CreativeTabs creativeTabs;
    public static MCH_CreativeTabs creativeTabsHeli;
    public static MCH_CreativeTabs creativeTabsPlane;
    public static MCH_CreativeTabs creativeTabsTank;
    public static MCH_CreativeTabs creativeTabsVehicle;
    public static MCH_CreativeTabs creativeTabsBlock;
    public static MCH_CreativeTabs creativeTabsGunner;
    public static MCH_DraftingTableBlock blockDraftingTable;
    public static MCH_DraftingTableBlock blockDraftingTableLit;

    public static PacketHandler getPacketHandler() {
        return newPacketHandler;
    }

    public static void registerItem(W_Item item, String name, MCH_CreativeTabs ct) {
        item.setUnlocalizedName("mcheli:" + name);
        item.setTexture(name);
        if (ct != null) {
            item.setCreativeTab(ct);
            ct.addIconItem(item);
        }
        GameRegistry.registerItem(item, name);
    }

    public static void registerItemThrowable() {
        for (Object o : MCH_ThrowableInfoManager.getKeySet()) {
            String name = (String) o;
            MCH_ThrowableInfo info = MCH_ThrowableInfoManager.get(name);
            info.item = new MCH_ItemThrowable(info.itemID);
            info.item.setMaxStackSize(info.stackSize);
            registerItem(info.item, name, creativeTabs);
            MCH_ItemThrowable.registerDispenseBehavior(info.item);
            info.itemID = W_Item.getIdFromItem(info.item) - 256;
            W_LanguageRegistry.addName(info.item, info.displayName);
            for (Object object : info.displayNameLang.keySet()) {
                String lang = (String) object;
                W_LanguageRegistry.addNameForObject(info.item, lang, (String) info.displayNameLang.get(lang));
            }
        }

    }

    public static void registerItemAircraft() {
        Iterator i$ = MCH_HeliInfoManager.map.keySet().iterator();

        String name;
        Iterator i$1;
        String lang;
        while (i$.hasNext()) {
            name = (String) i$.next();
            MCH_HeliInfo info = (MCH_HeliInfo) MCH_HeliInfoManager.map.get(name);
            info.item = new MCH_ItemHeli(info.itemID);
            info.item.setMaxDamage(info.maxHp);
            if (!info.canRide && (info.ammoSupplyRange > 0.0F || info.fuelSupplyRange > 0.0F)) {
                registerItem(info.item, name, creativeTabs);
            } else {
                registerItem(info.item, name, creativeTabsHeli);
            }

            MCH_ItemAircraft.registerDispenseBehavior(info.item);
            info.itemID = W_Item.getIdFromItem(info.item) - 256;
            W_LanguageRegistry.addName(info.item, info.displayName);
            i$1 = info.displayNameLang.keySet().iterator();

            while (i$1.hasNext()) {
                lang = (String) i$1.next();
                W_LanguageRegistry.addNameForObject(info.item, lang, (String) info.displayNameLang.get(lang));
            }
        }

        i$ = MCP_PlaneInfoManager.map.keySet().iterator();

        while (i$.hasNext()) {
            name = (String) i$.next();
            MCP_PlaneInfo info1 = (MCP_PlaneInfo) MCP_PlaneInfoManager.map.get(name);
            info1.item = new MCP_ItemPlane(info1.itemID);
            info1.item.setMaxDamage(info1.maxHp);
            if (!info1.canRide && (info1.ammoSupplyRange > 0.0F || info1.fuelSupplyRange > 0.0F)) {
                registerItem(info1.item, name, creativeTabs);
            } else {
                registerItem(info1.item, name, creativeTabsPlane);
            }

            MCH_ItemAircraft.registerDispenseBehavior(info1.item);
            info1.itemID = W_Item.getIdFromItem(info1.item) - 256;
            W_LanguageRegistry.addName(info1.item, info1.displayName);
            i$1 = info1.displayNameLang.keySet().iterator();

            while (i$1.hasNext()) {
                lang = (String) i$1.next();
                W_LanguageRegistry.addNameForObject(info1.item, lang, (String) info1.displayNameLang.get(lang));
            }
        }

        i$ = MCH_TankInfoManager.map.keySet().iterator();

        while (i$.hasNext()) {
            name = (String) i$.next();
            MCH_TankInfo info2 = (MCH_TankInfo) MCH_TankInfoManager.map.get(name);
            info2.item = new MCH_ItemTank(info2.itemID);
            info2.item.setMaxDamage(info2.maxHp);
            if (!info2.canRide && (info2.ammoSupplyRange > 0.0F || info2.fuelSupplyRange > 0.0F)) {
                registerItem(info2.item, name, creativeTabs);
            } else {
                registerItem(info2.item, name, creativeTabsTank);
            }

            MCH_ItemAircraft.registerDispenseBehavior(info2.item);
            info2.itemID = W_Item.getIdFromItem(info2.item) - 256;
            W_LanguageRegistry.addName(info2.item, info2.displayName);
            i$1 = info2.displayNameLang.keySet().iterator();

            while (i$1.hasNext()) {
                lang = (String) i$1.next();
                W_LanguageRegistry.addNameForObject(info2.item, lang, (String) info2.displayNameLang.get(lang));
            }
        }

        i$ = MCH_VehicleInfoManager.map.keySet().iterator();

        while (i$.hasNext()) {
            name = (String) i$.next();
            MCH_VehicleInfo info3 = (MCH_VehicleInfo) MCH_VehicleInfoManager.map.get(name);
            info3.item = new MCH_ItemVehicle(info3.itemID);
            info3.item.setMaxDamage(info3.maxHp);
            if (!info3.canRide && (info3.ammoSupplyRange > 0.0F || info3.fuelSupplyRange > 0.0F)) {
                registerItem(info3.item, name, creativeTabs);
            } else {
                registerItem(info3.item, name, creativeTabsVehicle);
            }

            MCH_ItemAircraft.registerDispenseBehavior(info3.item);
            info3.itemID = W_Item.getIdFromItem(info3.item) - 256;
            W_LanguageRegistry.addName(info3.item, info3.displayName);
            i$1 = info3.displayNameLang.keySet().iterator();

            while (i$1.hasNext()) {
                lang = (String) i$1.next();
                W_LanguageRegistry.addNameForObject(info3.item, lang, (String) info3.displayNameLang.get(lang));
            }
        }

    }

    @EventHandler
    public void PreInit(FMLPreInitializationEvent evt) {

        VER = Loader.instance().activeModContainer().getVersion();
        MCH_Lib.init();
        MCH_Lib.Log("MC Ver:1.7.10 MOD Ver:" + VER);
        MCH_Lib.Log("Start load...");
        sourcePath = Loader.instance().activeModContainer().getSource().getPath();
        ///sourcePath = "D:\\软件\\GitHub\\MCHeli-Reforged\\src\\main\\resources";
        //new File(evt.getModConfigurationDirectory().getParentFile(), "/mods").getPath();
        MCH_Lib.Log("SourcePath: " + sourcePath);
        MCH_Lib.Log("CurrentDirectory:" + (new File(".")).getAbsolutePath());

        proxy.init();
        creativeTabs = new MCH_CreativeTabs("MCH-Reforged Item");
        creativeTabsHeli = new MCH_CreativeTabs("MCH-Reforged Helicopters");
        creativeTabsPlane = new MCH_CreativeTabs("MCH-Reforged Planes");
        creativeTabsTank = new MCH_CreativeTabs("MCH-Reforged Tanks");
        creativeTabsVehicle = new MCH_CreativeTabs("MCH-Reforged Vehicles");
        creativeTabsBlock = new MCH_CreativeTabs("MCH-Reforged Blocks");
        creativeTabsGunner = new MCH_CreativeTabs("MCH-Reforged Gunners");
        W_ItemList.init();
        config = proxy.loadConfig("config/mcheli.cfg");
        proxy.loadHUD(sourcePath + "/assets/" + "mcheli" + "/hud");
        MCH_WeaponInfoManager.load(sourcePath + "/assets/" + "mcheli" + "/weapons");
        MCH_HeliInfoManager.getInstance().load(sourcePath + "/assets/" + "mcheli" + "/", "helicopters");
        MCP_PlaneInfoManager.getInstance().load(sourcePath + "/assets/" + "mcheli" + "/", "planes");
        MCH_TankInfoManager.getInstance().load(sourcePath + "/assets/" + "mcheli" + "/", "tanks");
        MCH_VehicleInfoManager.getInstance().load(sourcePath + "/assets/" + "mcheli" + "/", "vehicles");
        MCH_ThrowableInfoManager.load(sourcePath + "/assets/" + "mcheli" + "/throwable");
        MCH_BlockInfoManager.load(sourcePath + "/assets/" + "mcheli" + "/blocks");
        MCH_GunnerInfoManager.load(sourcePath + "/assets/" + "mcheli" + "/");
        MCH_LightWeaponAmmoInfoManager.load(sourcePath + "/assets/" + "mcheli" + "/lweapon_ammo");
        MCH_LightWeaponInfoManager.load(sourcePath + "/assets/" + "mcheli" + "/lweapons");
        MCH_SoundsJson.update(sourcePath + "/assets/" + "mcheli" + "/");
        MCH_Lib.Log("Register item");
        this.registerItemRangeFinder();
        this.registerItemSpawnGunner();
        this.registerConfiguredGunnerItems();
        this.registerItemWrench();
        this.registerItemFuel();
        this.registerItemGLTD();
        this.registerItemChain();
        this.registerItemParachute();
        this.registerItemContainer();
        this.registerItemUavStation();
        this.registerItemInvisible();
        registerItemThrowable();
        this.registerItemLightWeaponBullet();
        this.registerItemLightWeapon();
        registerItemAircraft();
        MCH_DraftingTableBlock var10000 = new MCH_DraftingTableBlock(MCH_Config.BlockID_DraftingTableOFF.prmInt, false);
        blockDraftingTable = var10000;
        blockDraftingTable.setBlockName("drafting_table");
        blockDraftingTable.setCreativeTab(creativeTabsBlock);
        var10000 = new MCH_DraftingTableBlock(MCH_Config.BlockID_DraftingTableON.prmInt, true);
        blockDraftingTableLit = var10000;
        blockDraftingTableLit.setBlockName("lit_drafting_table");
        GameRegistry.registerBlock(blockDraftingTable, "drafting_table");
        GameRegistry.registerBlock(blockDraftingTableLit, "lit_drafting_table");
        this.registerConfiguredBlocks();
        W_LanguageRegistry.addName(blockDraftingTable, "Drafting Table");
        W_LanguageRegistry.addNameForObject(blockDraftingTable, "zh_CN", "蓝图制作台");
        MCH_Achievement.PreInit();
        MCH_Lib.Log("Register system");
        W_NetworkRegistry.registerChannel(packetHandler, "MCHeli_CH");
        MinecraftForge.EVENT_BUS.register(new MCH_EventHook());

        proxy.registerClientTick();

        W_NetworkRegistry.registerGuiHandler(this, new MCH_GuiCommonHandler());
        MCH_Lib.Log("Register entity");
        this.registerEntity();
        MCH_Lib.Log("Register renderer");
        proxy.registerRenderer();
        MCH_Lib.Log("Register models");
        proxy.registerModels();
        MCH_Lib.Log("Register Sounds");
        proxy.registerSounds();
        W_LanguageRegistry.updateLang(sourcePath + "/assets/" + "mcheli" + "/lang/");
        MCH_Lib.Log("End load");


        try {
            ForgeChunkManager.setForcedChunkLoadingCallback(this, (tickets, world) -> {
                for (ForgeChunkManager.Ticket ticket : tickets) {
                    if (ticket.getEntity() instanceof MCH_EntityBullet) {
                        ((MCH_IChunkLoader) ticket.getEntity()).init(ticket);
                    }
                }
            });
        } catch (Exception e) {
            System.out.println("error loading chunk");
        }

    }

    @EventHandler
    public void init(FMLInitializationEvent evt) {
        getPacketHandler().initialise();
        GameRegistry.registerTileEntity(MCH_DraftingTableTileEntity.class, "drafting_table");
        GameRegistry.registerTileEntity(MCH_ConfigSpawnerTileEntity.class, "mcheli_config_spawner");
        proxy.registerBlockRenderer();
    }

    @EventHandler
    public void postInit(FMLPostInitializationEvent evt) {
        getPacketHandler().postInitialise();
        creativeTabs.setFixedIconItem(MCH_Config.CreativeTabIcon.prmString);
        creativeTabsHeli.setFixedIconItem(MCH_Config.CreativeTabIconHeli.prmString);
        creativeTabsPlane.setFixedIconItem(MCH_Config.CreativeTabIconPlane.prmString);
        creativeTabsTank.setFixedIconItem(MCH_Config.CreativeTabIconTank.prmString);
        creativeTabsVehicle.setFixedIconItem(MCH_Config.CreativeTabIconVehicle.prmString);
        creativeTabsBlock.setFixedIconItem("drafting_table");
        creativeTabsGunner.setFixedIconItem("gunner_aa");
        MCH_ItemRecipe.registerItemRecipe();
        MCH_WeaponInfoManager.setRoundItems();
        proxy.readClientModList();
    }

    @EventHandler
    public void onStartServer(FMLServerStartingEvent event) {
        proxy.registerServerTick();
    }

    private void registerItemSpawnGunner() {
        itemSpawnGunnerVsMonster = registerLegacyOrProfileGunner(
            "spawn_gunner_vs_monster", "gunner_friendly_default",
            0, false, true, 12632224, 12582912,
            "Gunner [Friendly]", "対モンスター 射撃手", "炮手[友好]"
        );
        itemSpawnGunnerVsPlayer = registerLegacyOrProfileGunner(
            "spawn_gunner_vs_player", "gunner_player_default",
            1, false, true, 12632224, 49152,
            "Gunner [Faction]", "対他チームプレイヤー 射撃手", "炮手[阵营]"
        );
        itemSpawnGunnerAA = registerLegacyOrProfileGunner(
            "gunner_aa", "gunner_aa_default",
            2, false, true, 12632224, 32768,
            "Gunner [Anti-Missile]", "対弾薬迎撃 射撃手", "炮手[反导]"
        );
        itemSpawnGunnerEnemy = registerLegacyOrProfileGunner(
            "gunner_enemy", "gunner_enemy_default",
            3, false, true, 12632224, 2228224,
            "Gunner [Hostile]", "敵対 射撃手", "炮手[敌对]"
        );
        itemSpawnGunnerVsMonsterStupid = registerLegacyOrProfileGunner(
            "gunner_friendly_stupid", "gunner_friendly_stupid_default",
            0, true, false, 12632224, 12582912,
            "Gunner [Friendly][Stupid]", "対モンスター 射撃手[愚人]", "炮手[愚人][友好]"
        );
        itemSpawnGunnerEnemyStupid = registerLegacyOrProfileGunner(
            "gunner_enemy_stupid", "gunner_enemy_stupid_default",
            3, true, false, 12632224, 2228224,
            "Gunner [Hostile][Stupid]", "敵対 射撃手[愚人]", "炮手[愚人][敌对]"
        );
    }

    private MCH_ItemSpawnGunner registerLegacyOrProfileGunner(String registerName, String profileKey,
                                                               int fallbackTargetType, boolean fallbackStupid, boolean fallbackLayeredIcon,
                                                               int fallbackPrimaryColor, int fallbackSecondaryColor,
                                                               String fallbackDisplayName, String fallbackJa, String fallbackZh) {
        MCH_GunnerInfo profile = MCH_GunnerInfoManager.get(profileKey);
        MCH_ItemSpawnGunner item = new MCH_ItemSpawnGunner();
        if (profile != null && profile.isValidData()) {
            item.targetType = profile.targetType;
            item.isStupid = profile.stupidGunner;
            item.useLayeredIcon = profile.useLayeredIcon;
            item.applyItemColorTint = profile.applyItemColorTint;
            item.primaryColor = profile.primaryColor;
            item.secondaryColor = profile.secondaryColor;
            item.gunnerProfileName = profile.name;
            registerItem(item, registerName, creativeTabsGunner);
            W_LanguageRegistry.addName(item, profile.displayName);
            for (String lang : profile.displayNameLang.keySet()) {
                W_LanguageRegistry.addNameForObject(item, lang, profile.displayNameLang.get(lang));
            }
            MCH_Lib.Log("[mcheli] Gunner legacy item '%s' mapped to profile '%s'.", registerName, profileKey);
            return item;
        }

        item.targetType = fallbackTargetType;
        item.isStupid = fallbackStupid;
        item.useLayeredIcon = fallbackLayeredIcon;
        item.primaryColor = fallbackPrimaryColor;
        item.secondaryColor = fallbackSecondaryColor;
        registerItem(item, registerName, creativeTabsGunner);
        W_LanguageRegistry.addName(item, fallbackDisplayName);
        W_LanguageRegistry.addNameForObject(item, "ja_JP", fallbackJa);
        W_LanguageRegistry.addNameForObject(item, "zh_CN", fallbackZh);
        MCH_Lib.Log("[mcheli] Gunner profile '%s' missing/invalid, fallback to legacy item '%s'.", profileKey, registerName);
        return item;
    }

    private void registerConfiguredGunnerItems() {
        for (MCH_GunnerInfo info : MCH_GunnerInfoManager.getValues()) {
            if (info == null || info.itemName == null || info.itemName.trim().isEmpty()) {
                continue;
            }
            String regName = info.itemName.trim().toLowerCase(Locale.ROOT);
            if (regName.equals("spawn_gunner_vs_monster")
                || regName.equals("spawn_gunner_vs_player")
                || regName.equals("gunner_aa")
                || regName.equals("gunner_enemy")
                || regName.equals("gunner_friendly_stupid")
                || regName.equals("gunner_enemy_stupid")) {
                continue;
            }
            MCH_ItemSpawnGunner item = new MCH_ItemSpawnGunner();
            item.targetType = info.targetType;
            item.isStupid = info.stupidGunner;
            item.useLayeredIcon = info.useLayeredIcon;
            item.applyItemColorTint = info.applyItemColorTint;
            item.primaryColor = info.primaryColor;
            item.secondaryColor = info.secondaryColor;
            item.gunnerProfileName = info.name;
            registerItem(item, regName, creativeTabsGunner);
            W_LanguageRegistry.addName(item, info.displayName);
            for (String lang : info.displayNameLang.keySet()) {
                W_LanguageRegistry.addNameForObject(item, lang, info.displayNameLang.get(lang));
            }
        }
    }

    private void registerConfiguredBlocks() {
        for (MCH_BlockInfo info : MCH_BlockInfoManager.getValues()) {
            Block block;
            if (info.enableSpawner || info.enableWaypoint) {
                block = new MCH_ConfigSpawnerBlock(info, this.resolveMaterial(info.materialName));
            } else {
                block = new MCH_ConfigBlock(this.resolveMaterial(info.materialName), info.textureName);
            }
            block.setBlockName(info.name);
            block.setHardness(info.hardness);
            block.setResistance(info.resistance);
            block.setLightLevel(info.lightLevel);
            block.setStepSound(this.resolveStepSound(info.stepSound));
            CreativeTabs creativeTab = this.resolveCreativeTab(info.creativeTab);
            block.setCreativeTab(creativeTab);
            GameRegistry.registerBlock(block, info.name);
            info.block = block;
            W_LanguageRegistry.addName(block, info.displayName);
            for (String lang : info.displayNameLang.keySet()) {
                W_LanguageRegistry.addNameForObject(block, lang, info.displayNameLang.get(lang));
            }
            Item item = W_Item.getItemFromBlock(block);
            if (item != null && creativeTab instanceof MCH_CreativeTabs) {
                ((MCH_CreativeTabs) creativeTab).addIconItem(item);
            }
        }
    }

    private Material resolveMaterial(String materialName) {
        if (materialName == null) {
            return Material.iron;
        }
        String key = materialName.trim().toLowerCase(Locale.ROOT);
        if (key.equals("rock") || key.equals("stone")) {
            return Material.rock;
        }
        if (key.equals("wood")) {
            return Material.wood;
        }
        if (key.equals("ground") || key.equals("dirt")) {
            return Material.ground;
        }
        if (key.equals("grass")) {
            return Material.grass;
        }
        if (key.equals("sand")) {
            return Material.sand;
        }
        if (key.equals("glass")) {
            return Material.glass;
        }
        if (key.equals("cloth")) {
            return Material.cloth;
        }
        if (key.equals("clay")) {
            return Material.clay;
        }
        if (key.equals("anvil")) {
            return Material.anvil;
        }
        if (key.equals("water")) {
            return Material.water;
        }
        if (key.equals("lava")) {
            return Material.lava;
        }
        if (key.equals("ice")) {
            return Material.ice;
        }
        return Material.iron;
    }

    private Block.SoundType resolveStepSound(String stepSoundName) {
        if (stepSoundName == null) {
            return Block.soundTypeMetal;
        }
        String key = stepSoundName.trim().toLowerCase(Locale.ROOT);
        if (key.equals("stone")) {
            return Block.soundTypeStone;
        }
        if (key.equals("wood")) {
            return Block.soundTypeWood;
        }
        if (key.equals("gravel")) {
            return Block.soundTypeGravel;
        }
        if (key.equals("grass")) {
            return Block.soundTypeGrass;
        }
        if (key.equals("cloth")) {
            return Block.soundTypeCloth;
        }
        if (key.equals("sand")) {
            return Block.soundTypeSand;
        }
        if (key.equals("glass")) {
            return Block.soundTypeGlass;
        }
        if (key.equals("ladder")) {
            return Block.soundTypeLadder;
        }
        if (key.equals("anvil")) {
            return Block.soundTypeAnvil;
        }
        return Block.soundTypeMetal;
    }

    private MCH_CreativeTabs resolveCreativeTab(String tabName) {
        if (tabName == null) {
            return creativeTabsBlock != null ? creativeTabsBlock : creativeTabs;
        }
        String key = tabName.trim().toLowerCase(Locale.ROOT);
        if (key.equals("misc") || key.equals("item")) {
            return creativeTabs;
        }
        if (key.equals("heli") || key.equals("helicopter")) {
            return creativeTabsHeli;
        }
        if (key.equals("plane")) {
            return creativeTabsPlane;
        }
        if (key.equals("tank")) {
            return creativeTabsTank;
        }
        if (key.equals("vehicle")) {
            return creativeTabsVehicle;
        }
        if (key.equals("block") || key.equals("blocks")) {
            return creativeTabsBlock != null ? creativeTabsBlock : creativeTabs;
        }
        return creativeTabsBlock != null ? creativeTabsBlock : creativeTabs;
    }

    public void registerEntity() {
        EntityRegistry.registerModEntity(MCH_EntitySeat.class, "MCH.E.Seat", 100, this, 200, 10, true);
        EntityRegistry.registerModEntity(MCH_EntityHeli.class, "MCH.E.Heli", 101, this, 500, 1, true);
        EntityRegistry.registerModEntity(MCH_EntityGLTD.class, "MCH.E.GLTD", 102, this, 200, 10, true);
        EntityRegistry.registerModEntity(MCP_EntityPlane.class, "MCH.E.Plane", 103, this, 500, 1, true);
        EntityRegistry.registerModEntity(MCH_EntityChain.class, "MCH.E.Chain", 104, this, 200, 10, true);
        EntityRegistry.registerModEntity(MCH_EntityHitBox.class, "MCH.E.PSeat", 105, this, 200, 10, true);
        EntityRegistry.registerModEntity(MCH_EntityParachute.class, "MCH.E.Parachute", 106, this, 200, 10, true);
        EntityRegistry.registerModEntity(MCH_EntityContainer.class, "MCH.E.Container", 107, this, 200, 10, true);
        EntityRegistry.registerModEntity(MCH_EntityVehicle.class, "MCH.E.Vehicle", 108, this, 500, 10, true);
        EntityRegistry.registerModEntity(MCH_EntityUavStation.class, "MCH.E.UavStation", 109, this, 200, 10, true);
        EntityRegistry.registerModEntity(MCH_EntityHitBox.class, "MCH.E.HitBox", 110, this, 200, 10, true);
        EntityRegistry.registerModEntity(MCH_EntityHide.class, "MCH.E.Hide", 111, this, 200, 10, true);
        EntityRegistry.registerModEntity(MCH_EntityTank.class, "MCH.E.Tank", 112, this, 500, 1, true);
        EntityRegistry.registerModEntity(MCH_EntityRocket.class, "MCH.E.Rocket", 200, this, 530, 3, true);
        EntityRegistry.registerModEntity(MCH_EntityTvMissile.class, "MCH.E.TvMissle", 201, this, 530, 2, true);
        EntityRegistry.registerModEntity(MCH_EntityBullet.class, "MCH.E.Bullet", 202, this, 530, 5, true);
        EntityRegistry.registerModEntity(MCH_EntityA10.class, "MCH.E.A10", 203, this, 530, 5, true);
        EntityRegistry.registerModEntity(MCH_EntityAAMissile.class, "MCH.E.AAM", 204, this, 530, 2, true);
        EntityRegistry.registerModEntity(MCH_EntityASMissile.class, "MCH.E.ASM", 205, this, 530, 2, true);
        EntityRegistry.registerModEntity(MCH_EntityTorpedo.class, "MCH.E.Torpedo", 206, this, 530, 5, true);
        EntityRegistry.registerModEntity(MCH_EntityATMissile.class, "MCH.E.ATMissle", 207, this, 530, 2, true);
        EntityRegistry.registerModEntity(MCH_EntityBomb.class, "MCH.E.Bomb", 208, this, 530, 5, true);
        EntityRegistry.registerModEntity(MCH_EntityMarkerRocket.class, "MCH.E.MkRocket", 209, this, 530, 5, true);
        EntityRegistry.registerModEntity(MCH_EntityDispensedItem.class, "MCH.E.DispItem", 210, this, 530, 5, true);
        EntityRegistry.registerModEntity(MCH_EntityFlare.class, "MCH.E.Flare", 300, this, 330, 10, true);
        EntityRegistry.registerModEntity(MCH_EntityThrowable.class, "MCH.E.Throwable", 400, this, 330, 10, true);
        EntityRegistry.registerModEntity(MCH_EntityGunner.class, "MCH.E.Gunner", 500, this, 530, 5, true);
        EntityRegistry.registerModEntity(MCH_EntityLockBox.class, "MCH.E.LockBox", 401, this, 32, 20, false);
        EntityRegistry.registerModEntity(MCH_EntityChaff.class, "MCH.E.Chaff", 402, this, 330, 10, true);
        EntityRegistry.registerModEntity(EntityNukeTorex.class, "MCH.E.Nuke", 403, this, 1000, 20, false);
    }

    @EventHandler
    public void registerCommand(FMLServerStartedEvent e) {
        CommandHandler handler = (CommandHandler) FMLCommonHandler.instance().getSidedDelegate().getServer().getCommandManager();
        handler.registerCommand(new MCH_Command());
        handler.registerCommand(new MCH_CommandAddGunner());
    }

    private void registerItemRangeFinder() {
        MCH_ItemRangeFinder item = new MCH_ItemRangeFinder(MCH_Config.ItemID_RangeFinder.prmInt);
        itemRangeFinder = item;
        registerItem(item, "rangefinder", creativeTabs);
        W_LanguageRegistry.addName(item, "Portable Laser Designator");
        W_LanguageRegistry.addNameForObject(item, "zh_CN", "PLD单兵光电指示器");
    }

    private void registerItemWrench() {
        MCH_ItemWrench item = new MCH_ItemWrench(MCH_Config.ItemID_Wrench.prmInt, ToolMaterial.IRON);
        itemWrench = item;
        registerItem(item, "wrench", creativeTabs);
        W_LanguageRegistry.addName(item, "WRENCH");
        W_LanguageRegistry.addNameForObject(item, "zh_CN", "扳手");
    }

    public void registerItemInvisible() {
        MCH_InvisibleItem item = new MCH_InvisibleItem(MCH_Config.ItemID_InvisibleItem.prmInt);
        invisibleItem = item;
        registerItem(item, "internal", null);
    }

    public void registerItemUavStation() {
        String[] dispName = new String[]{"UAV Station", "Portable UAV Station"};
        itemUavStation = new MCH_ItemUavStation[MCH_ItemUavStation.UAV_STATION_KIND_NUM];
        for (int i = 0; i < itemUavStation.length; ++i) {
            String nn = i > 0 ? "" + (i + 1) : "";
            MCH_ItemUavStation item = new MCH_ItemUavStation(MCH_Config.ItemID_UavStation[i].prmInt, 1 + i);
            itemUavStation[i] = item;
            registerItem(item, "uav_station" + nn, creativeTabs);
            W_LanguageRegistry.addName(item, dispName[i]);
        }

    }

    public void registerItemParachute() {
        MCH_ItemParachute item = new MCH_ItemParachute(MCH_Config.ItemID_Parachute.prmInt);
        itemParachute = item;
        registerItem(item, "parachute", creativeTabs);
        W_LanguageRegistry.addName(item, "Parachute");
        W_LanguageRegistry.addNameForObject(item, "zh_CN", "降落伞");
    }

    public void registerItemContainer() {
        MCH_ItemContainer item = new MCH_ItemContainer(MCH_Config.ItemID_Container.prmInt);
        itemContainer = item;
        registerItem(item, "container", creativeTabs);
        W_LanguageRegistry.addName(item, "Container");
        W_LanguageRegistry.addNameForObject(item, "zh_CN", "空投箱");
    }

    public void registerItemLightWeapon() {
        if (!MCH_LightWeaponInfoManager.getValues().isEmpty()) {
            for (MCH_LightWeaponInfo info : MCH_LightWeaponInfoManager.getValues()) {
                MCH_LightWeaponAmmoInfo ammoInfo = MCH_LightWeaponAmmoInfoManager.get(info.ammoItemName);
                MCH_ItemLightWeaponBullet ammoItem = ammoInfo != null ? ammoInfo.item : null;
                if (ammoItem == null) {
                    MCH_Lib.Log("Skip light weapon %s : ammo %s not found", info.name, info.ammoItemName);
                    continue;
                }
                MCH_ItemLightWeaponBase item = new MCH_ItemLightWeaponBase(info.itemID, ammoItem, info);
                info.item = item;
                registerItem(item, info.name, creativeTabs);
                if (!info.textureName.isEmpty()) {
                    item.setTexture(info.textureName);
                }
                W_LanguageRegistry.addName(item, info.displayName);
                for (String lang : info.displayNameLang.keySet()) {
                    W_LanguageRegistry.addNameForObject(item, lang, info.displayNameLang.get(lang));
                }
                String lower = info.name.toLowerCase(Locale.ROOT);
                if (lower.equals("fim92") || lower.equals("fim192")) {
                    itemStinger = item;
                } else if (lower.equals("fgm148")) {
                    itemJavelin = item;
                } else if (lower.equals("rpg7")) {
                    itemRpg = item;
                }
            }
        }

        if (itemStinger == null && itemStingerBullet != null) {
            String name = "fim92";
            MCH_ItemLightWeaponBase item = new MCH_ItemLightWeaponBase(MCH_Config.ItemID_Stinger.prmInt, itemStingerBullet);
            itemStinger = item;
            registerItem(item, name, creativeTabs);
            W_LanguageRegistry.addName(item, "FIM-92 Stringer");
            W_LanguageRegistry.addNameForObject(item, "zh_CN", "FIM-92 刺针飞弹");
        }
        if (itemJavelin == null && itemJavelinBullet != null) {
            String name = "fgm148";
            MCH_ItemLightWeaponBase item = new MCH_ItemLightWeaponBase(MCH_Config.ItemID_Stinger.prmInt, itemJavelinBullet);
            itemJavelin = item;
            registerItem(item, name, creativeTabs);
            W_LanguageRegistry.addName(item, "FGM-148 Javelin");
            W_LanguageRegistry.addNameForObject(item, "zh_CN", "FGM-148 标枪飞弹");
        }
    }

    public void registerItemLightWeaponBullet() {
        if (!MCH_LightWeaponAmmoInfoManager.getValues().isEmpty()) {
            for (MCH_LightWeaponAmmoInfo info : MCH_LightWeaponAmmoInfoManager.getValues()) {
                MCH_ItemLightWeaponBullet item = new MCH_ItemLightWeaponBullet(info.itemID, info.stackSize);
                info.item = item;
                registerItem(item, info.name, creativeTabs);
                if (!info.textureName.isEmpty()) {
                    item.setTexture(info.textureName);
                }
                W_LanguageRegistry.addName(item, info.displayName);
                for (String lang : info.displayNameLang.keySet()) {
                    W_LanguageRegistry.addNameForObject(item, lang, info.displayNameLang.get(lang));
                }
                String lower = info.name.toLowerCase(Locale.ROOT);
                if (lower.equals("fim92_bullet") || lower.equals("fim192_bullet")) {
                    itemStingerBullet = item;
                } else if (lower.equals("fgm148_bullet")) {
                    itemJavelinBullet = item;
                } else if (lower.equals("rpg7_bullet") || lower.equals("rpg_bullet")) {
                    itemRpgBullet = item;
                }
            }
        }

        if (itemStingerBullet == null) {
            String name = "fim92_bullet";
            MCH_ItemLightWeaponBullet item = new MCH_ItemLightWeaponBullet(MCH_Config.ItemID_StingerMissile.prmInt);
            itemStingerBullet = item;
            registerItem(item, name, creativeTabs);
            W_LanguageRegistry.addName(item, "FIM-92 Stringer Ammo");
            W_LanguageRegistry.addNameForObject(item, "zh_CN", "FIM-92 弹药");
        }
        if (itemJavelinBullet == null) {
            String name = "fgm148_bullet";
            MCH_ItemLightWeaponBullet item = new MCH_ItemLightWeaponBullet(MCH_Config.ItemID_StingerMissile.prmInt);
            itemJavelinBullet = item;
            registerItem(item, name, creativeTabs);
            W_LanguageRegistry.addName(item, "FGM-148 Javelin Ammo");
            W_LanguageRegistry.addNameForObject(item, "zh_CN", "FGM-148 弹药");
        }
    }

    public void registerItemChain() {
        MCH_ItemChain item = new MCH_ItemChain(MCH_Config.ItemID_Chain.prmInt);
        itemChain = item;
        registerItem(item, "chain", creativeTabs);
        W_LanguageRegistry.addName(item, "Chain");
        W_LanguageRegistry.addNameForObject(item, "zh_CN", "锁链");
    }

    public void registerItemFuel() {
        MCH_ItemFuel item = new MCH_ItemFuel(MCH_Config.ItemID_Fuel.prmInt);
        itemFuel = item;
        registerItem(item, "fuel", creativeTabs);
        W_LanguageRegistry.addName(item, "Fuel");
        W_LanguageRegistry.addNameForObject(item, "zh_CN", "燃料");
    }

    public void registerItemGLTD() {
        MCH_ItemGLTD item = new MCH_ItemGLTD(MCH_Config.ItemID_GLTD.prmInt);
        itemGLTD = item;
        registerItem(item, "gltd", creativeTabs);
        W_LanguageRegistry.addName(item, "SOFLAM");
        W_LanguageRegistry.addNameForObject(item, "zh_CN", "SOFLAM 空袭指示器");
    }

}
