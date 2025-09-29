package mcheli;

import mcheli.container.MCH_ItemContainer;
import mcheli.helicopter.MCH_HeliInfo;
import mcheli.helicopter.MCH_HeliInfoManager;
import mcheli.plane.MCP_PlaneInfo;
import mcheli.plane.MCP_PlaneInfoManager;
import mcheli.vehicle.MCH_VehicleInfo;
import mcheli.vehicle.MCH_VehicleInfoManager;
import mcheli.wrapper.W_Achievement;
import mcheli.wrapper.W_Item;
import mcheli.wrapper.W_LanguageRegistry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.stats.Achievement;
import net.minecraftforge.common.AchievementPage;

public class MCH_Achievement {

    public static Achievement welcome = null;
    public static Achievement supplyFuel = null;
    public static Achievement supplyAmmo = null;
    public static Achievement aintWarHell = null;
    public static Achievement reliefSupplies = null;
    public static Achievement rideValkyries = null;


    public static void PreInit() {
        Item item = getAnyAircraftIcon("ah-64");
        boolean BC = true;
        boolean BR = true;
        String name = "McHeliWelcome";
        welcome = W_Achievement.registerAchievement("mcheli" + name, name, 1, 1, item, null);
        W_LanguageRegistry.addNameForObject(welcome, "en_US", "Welcome to MCH-Reforged MOD", name, "Put the helicopter");
        W_LanguageRegistry.addNameForObject(welcome, "ja_JP", "MCH-Reforged MOD へようこそ", name, "ヘリコプターを設置");
        W_LanguageRegistry.addNameForObject(welcome, "zh_CN", "欢迎下载MCH-Reforged模组", name, "放置直升机");
        name = "McHeliSupplyFuel";
        supplyFuel = W_Achievement.registerAchievement("mcheli" + name, name, -1, 1, MCH_MOD.itemFuel, null);
        W_LanguageRegistry.addNameForObject(supplyFuel, "en_US", "Refueling", name, "Refuel aircraft");
        W_LanguageRegistry.addNameForObject(supplyFuel, "ja_JP", "燃料補給", name, "燃料を補給");
        W_LanguageRegistry.addNameForObject(supplyFuel, "zh_CN", "燃料补给", name, "加注燃料");
        item = getAircraftIcon("ammo_box");
        name = "McHeliSupplyAmmo";
        supplyAmmo = W_Achievement.registerAchievement("mcheli" + name, name, 3, 1, item, null);
        W_LanguageRegistry.addNameForObject(supplyAmmo, "en_US", "Supply ammo", name, "Supply ammo to the aircraft");
        W_LanguageRegistry.addNameForObject(supplyAmmo, "ja_JP", "弾薬補給", name, "弾薬を補給");
        W_LanguageRegistry.addNameForObject(supplyFuel, "zh_CN", "弹药补给", name, "补充弹药");
        item = getAircraftIcon("uh-1c");
        name = "McHeliRideValkyries";
        rideValkyries = W_Achievement.registerAchievement("mcheli" + name, name, -1, 3, item, null);
        W_LanguageRegistry.addNameForObject(rideValkyries, "en_US", "Ride Of The Valkyries", name, "?");
        W_LanguageRegistry.addNameForObject(rideValkyries, "ja_JP", "ワルキューレの騎行", name, "?");
        W_LanguageRegistry.addNameForObject(rideValkyries, "zh_CN", "飞行的女武神", name, "?");
        item = getAircraftIcon("mh-60l_dap");
        name = "McHeliAintWarHell";
        aintWarHell = W_Achievement.registerAchievement("mcheli" + name, name, 3, 3, item, null);
        W_LanguageRegistry.addNameForObject(aintWarHell, "en_US", "Ain\'t war hell?", name, "?");
        W_LanguageRegistry.addNameForObject(aintWarHell, "ja_JP", "ホント戦争は地獄だぜ", name, "?");
        W_LanguageRegistry.addNameForObject(aintWarHell, "zh_CN", "战争即是地狱", name, "?");
        MCH_ItemContainer item1 = MCH_MOD.itemContainer;
        name = "McHeliReliefSupplies";
        reliefSupplies = W_Achievement.registerAchievement("mcheli" + name, name, -1, -1, item1, null);
        W_LanguageRegistry.addNameForObject(reliefSupplies, "en_US", "Relief supplies", name, "Drop a container");
        W_LanguageRegistry.addNameForObject(reliefSupplies, "ja_JP", "支援物資", name, "コンテナを投下");
        W_LanguageRegistry.addNameForObject(reliefSupplies, "zh_CN", "物资支援", name, "空投补给箱");
        Achievement[] achievements = new Achievement[]{welcome, supplyFuel, supplyAmmo, aintWarHell, rideValkyries, reliefSupplies};
        AchievementPage.registerAchievementPage(new AchievementPage("MCH-Reforged", achievements));
    }

    public static Item getAircraftIcon(String defaultIconAircraft) {
        Item item = W_Item.getItemByName("stone");
        MCH_HeliInfo info = MCH_HeliInfoManager.get(defaultIconAircraft);
        if (info != null && info.getItem() != null) {
            return info.getItem();
        } else {
            MCP_PlaneInfo info1 = MCP_PlaneInfoManager.get(defaultIconAircraft);
            if (info1 != null && info1.getItem() != null) {
                return info1.getItem();
            } else {
                MCH_VehicleInfo info2 = MCH_VehicleInfoManager.get(defaultIconAircraft);
                return info2 != null && info2.getItem() != null ? info2.getItem() : item;
            }
        }
    }

    public static Item getAnyAircraftIcon(String defaultIconAircraft) {
        Item item = W_Item.getItemByName("stone");
        if (!MCH_HeliInfoManager.map.isEmpty()) {
            MCH_HeliInfo info = MCH_HeliInfoManager.get(defaultIconAircraft);
            if (info != null && info.item != null) {
                item = info.item;
            } else {

                for (Object o : MCH_HeliInfoManager.map.values()) {
                    MCH_HeliInfo i = (MCH_HeliInfo) o;
                    if (i.item != null) {
                        item = i.item;
                        break;
                    }
                }
            }
        }

        return item;
    }

    public static void addStat(Entity player, Achievement a, int i) {
        if (a != null && player instanceof EntityPlayer && !player.worldObj.isRemote) {
            ((EntityPlayer) player).addStat(a, i);
        }

    }

}
