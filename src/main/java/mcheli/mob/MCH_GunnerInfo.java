package mcheli.mob;

import mcheli.MCH_BaseInfo;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class MCH_GunnerInfo extends MCH_BaseInfo {

    public final String name;
    public String displayName;
    public Map<String, String> displayNameLang;
    public String itemName;

    public int targetType;
    public boolean stupidGunner;
    public float stupidGunnerChance;
    public Map<String, Float> stupidGunnerChanceByRole;
    public String factionRole;

    public int primaryColor;
    public int secondaryColor;
    public boolean applyItemColorTint;
    public boolean useLayeredIcon;

    public String teamMode;
    public String fixedTeamId;
    public boolean autoCreateTeam;
    public String fixedTeamDisplayName;
    public int fixedTeamColor;
    public boolean requirePlayerTeamWhenPvp;

    public float mountSearchRange;
    public boolean allowMountAircraft;
    public boolean allowMountSeat;
    public boolean allowReplaceExistingGunner;

    public int searchRangeGroundHorizontal;
    public int searchRangeGroundVertical;
    public int searchRangeAirHorizontal;
    public int searchRangeAirVertical;
    public boolean searchRangeFallbackToConfig;

    public String airWeaponPriorityRaw;
    public String groundWeaponPriorityRaw;
    public boolean allowLeadForAirTarget;
    public float stupidAttackSectorScaleGround;
    public boolean enableShortBurst;
    public int shortBurstFireTick;
    public int shortBurstRestTick;

    public MCH_GunnerInfo(String name) {
        this.name = name;
        this.displayName = name;
        this.displayNameLang = new HashMap<String, String>();
        this.itemName = name;

        this.targetType = MCH_EntityGunner.TARGET_MONSTER;
        this.stupidGunner = false;
        this.stupidGunnerChance = -1.0F;
        this.stupidGunnerChanceByRole = new HashMap<String, Float>();
        this.factionRole = "normal";

        this.primaryColor = 0xC0C000;
        this.secondaryColor = 0x800000;
        this.applyItemColorTint = false;
        this.useLayeredIcon = true;

        this.teamMode = "player";
        this.fixedTeamId = "";
        this.autoCreateTeam = true;
        this.fixedTeamDisplayName = "";
        this.fixedTeamColor = 0x3A66FF;
        this.requirePlayerTeamWhenPvp = true;

        this.mountSearchRange = 5.0F;
        this.allowMountAircraft = true;
        this.allowMountSeat = true;
        this.allowReplaceExistingGunner = false;

        this.searchRangeGroundHorizontal = -1;
        this.searchRangeGroundVertical = -1;
        this.searchRangeAirHorizontal = -1;
        this.searchRangeAirVertical = -1;
        this.searchRangeFallbackToConfig = true;

        this.airWeaponPriorityRaw = "";
        this.groundWeaponPriorityRaw = "";
        this.allowLeadForAirTarget = true;
        this.stupidAttackSectorScaleGround = 1.0F;
        this.enableShortBurst = false;
        this.shortBurstFireTick = 14;
        this.shortBurstRestTick = 10;
    }

    @Override
    public void loadItemData(String item, String data) {
        if (item.equalsIgnoreCase("DisplayName")) {
            this.displayName = data;
        } else if (item.equalsIgnoreCase("AddDisplayName")) {
            String[] s = this.splitParam(data);
            if (s.length == 2) {
                this.displayNameLang.put(s[0].trim(), s[1].trim());
            }
        } else if (item.equalsIgnoreCase("ItemName")) {
            this.itemName = data.trim().toLowerCase(Locale.ROOT);
        } else if (item.equalsIgnoreCase("TargetType")) {
            this.targetType = this.parseTargetType(data.trim().toLowerCase(Locale.ROOT));
        } else if (item.equalsIgnoreCase("StupidGunner")) {
            this.stupidGunner = this.toBool(data.trim(), this.stupidGunner);
        } else if (item.equalsIgnoreCase("StupidGunnerChance")) {
            this.stupidGunnerChance = this.toFloat(data.trim(), 0.0F, 1.0F);
        } else if (item.equalsIgnoreCase("StupidGunnerChanceByRole")) {
            this.stupidGunnerChanceByRole.clear();
            String[] entries = data.split("\\|");
            for (String e : entries) {
                String v = e.trim();
                if (v.isEmpty()) {
                    continue;
                }
                int idx = v.indexOf(':');
                if (idx <= 0 || idx >= v.length() - 1) {
                    continue;
                }
                String role = v.substring(0, idx).trim().toLowerCase(Locale.ROOT);
                float chance = this.toFloat(v.substring(idx + 1).trim(), 0.0F, 1.0F);
                this.stupidGunnerChanceByRole.put(role, chance);
            }
        } else if (item.equalsIgnoreCase("FactionRole")) {
            this.factionRole = data.trim().toLowerCase(Locale.ROOT);
        } else if (item.equalsIgnoreCase("PrimaryColor")) {
            this.primaryColor = this.hex2dec(data.trim());
        } else if (item.equalsIgnoreCase("SecondaryColor")) {
            this.secondaryColor = this.hex2dec(data.trim());
        } else if (item.equalsIgnoreCase("ApplyItemColorTint")) {
            this.applyItemColorTint = this.toBool(data.trim(), this.applyItemColorTint);
        } else if (item.equalsIgnoreCase("UseLayeredIcon")) {
            this.useLayeredIcon = this.toBool(data.trim(), this.useLayeredIcon);
        } else if (item.equalsIgnoreCase("TeamMode")) {
            this.teamMode = data.trim().toLowerCase(Locale.ROOT);
        } else if (item.equalsIgnoreCase("FixedTeamId")) {
            this.fixedTeamId = data.trim();
        } else if (item.equalsIgnoreCase("AutoCreateTeam")) {
            this.autoCreateTeam = this.toBool(data.trim(), this.autoCreateTeam);
        } else if (item.equalsIgnoreCase("FixedTeamDisplayName")) {
            this.fixedTeamDisplayName = data.trim();
        } else if (item.equalsIgnoreCase("FixedTeamColor")) {
            this.fixedTeamColor = this.hex2dec(data.trim());
        } else if (item.equalsIgnoreCase("RequirePlayerTeamWhenPvp")) {
            this.requirePlayerTeamWhenPvp = this.toBool(data.trim(), this.requirePlayerTeamWhenPvp);
        } else if (item.equalsIgnoreCase("MountSearchRange")) {
            this.mountSearchRange = this.toFloat(data.trim(), 1.0F, 32.0F);
        } else if (item.equalsIgnoreCase("AllowMountAircraft")) {
            this.allowMountAircraft = this.toBool(data.trim(), this.allowMountAircraft);
        } else if (item.equalsIgnoreCase("AllowMountSeat")) {
            this.allowMountSeat = this.toBool(data.trim(), this.allowMountSeat);
        } else if (item.equalsIgnoreCase("AllowReplaceExistingGunner")) {
            this.allowReplaceExistingGunner = this.toBool(data.trim(), this.allowReplaceExistingGunner);
        } else if (item.equalsIgnoreCase("SearchRangeGroundHorizontal")) {
            this.searchRangeGroundHorizontal = this.toInt(data.trim(), 1, 4000);
        } else if (item.equalsIgnoreCase("SearchRangeGroundVertical")) {
            this.searchRangeGroundVertical = this.toInt(data.trim(), 1, 4000);
        } else if (item.equalsIgnoreCase("SearchRangeAirHorizontal")) {
            this.searchRangeAirHorizontal = this.toInt(data.trim(), 1, 4000);
        } else if (item.equalsIgnoreCase("SearchRangeAirVertical")) {
            this.searchRangeAirVertical = this.toInt(data.trim(), 1, 4000);
        } else if (item.equalsIgnoreCase("SearchRangeFallbackToConfig")) {
            this.searchRangeFallbackToConfig = this.toBool(data.trim(), this.searchRangeFallbackToConfig);
        } else if (item.equalsIgnoreCase("AirWeaponPriority")) {
            this.airWeaponPriorityRaw = data.trim().toLowerCase(Locale.ROOT);
        } else if (item.equalsIgnoreCase("GroundWeaponPriority")) {
            this.groundWeaponPriorityRaw = data.trim().toLowerCase(Locale.ROOT);
        } else if (item.equalsIgnoreCase("AllowLeadForAirTarget")) {
            this.allowLeadForAirTarget = this.toBool(data.trim(), this.allowLeadForAirTarget);
        } else if (item.equalsIgnoreCase("StupidAttackSectorScaleGround")) {
            this.stupidAttackSectorScaleGround = this.toFloat(data.trim(), 1.0F, 2.0F);
        } else if (item.equalsIgnoreCase("EnableShortBurst")) {
            this.enableShortBurst = this.toBool(data.trim(), this.enableShortBurst);
        } else if (item.equalsIgnoreCase("ShortBurstFireTick")) {
            this.shortBurstFireTick = this.toInt(data.trim(), 1, 200);
        } else if (item.equalsIgnoreCase("ShortBurstRestTick")) {
            this.shortBurstRestTick = this.toInt(data.trim(), 0, 200);
        }
    }

    public float getStupidChanceForRole(String role) {
        String r = role == null ? "" : role.toLowerCase(Locale.ROOT);
        if (this.stupidGunnerChanceByRole.containsKey(r)) {
            return this.stupidGunnerChanceByRole.get(r);
        }
        return this.stupidGunnerChance;
    }

    public Map<String, Integer> parseWeaponPriorityMap(boolean airContext) {
        String raw = airContext ? this.airWeaponPriorityRaw : this.groundWeaponPriorityRaw;
        Map<String, Integer> map = new LinkedHashMap<String, Integer>();
        if (raw == null || raw.isEmpty()) {
            return map;
        }
        String[] entries = raw.split("\\|");
        for (String e : entries) {
            String v = e.trim();
            if (v.isEmpty()) {
                continue;
            }
            int idx = v.indexOf(':');
            if (idx <= 0 || idx >= v.length() - 1) {
                continue;
            }
            String type = v.substring(0, idx).trim().toLowerCase(Locale.ROOT);
            int weight = this.toInt(v.substring(idx + 1).trim(), 0, 100000);
            map.put(type, weight);
        }
        return map;
    }

    @Override
    public boolean isValidData() {
        return this.itemName != null && !this.itemName.trim().isEmpty();
    }

    private int parseTargetType(String v) {
        if (v.equals("monster")) {
            return MCH_EntityGunner.TARGET_MONSTER;
        }
        if (v.equals("player") || v.equals("pvp")) {
            return MCH_EntityGunner.TARGET_PLAYER;
        }
        if (v.equals("aa") || v.equals("antiair")) {
            return MCH_EntityGunner.TARGET_AA_AMMO;
        }
        if (v.equals("enemy") || v.equals("hostile")) {
            return MCH_EntityGunner.TARGET_ENEMY;
        }
        return this.toInt(v, MCH_EntityGunner.TARGET_MONSTER, MCH_EntityGunner.TARGET_ENEMY);
    }
}
