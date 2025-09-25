package mcheli.hud;

public enum MCH_HudItemStringArgs {
    NONE, NAME, ALTITUDE, DATE, MC_THOR, MC_TMIN, MC_TSEC, MAX_HP, HP, HP_PER,
    POS_X, POS_Y, POS_Z, MOTION_X, MOTION_Y, MOTION_Z, INVENTORY, WPN_NAME,
    WPN_AMMO, WPN_RM_AMMO, RELOAD_PER, RELOAD_SEC, MORTAR_DIST, MC_VER, MOD_VER,
    MOD_NAME, YAW, PITCH, ROLL, PLYR_YAW, PLYR_PITCH, TVM_POS_X, TVM_POS_Y,
    TVM_POS_Z, TVM_DIFF, CAM_ZOOM, UAV_DIST, KEY_GUI, THROTTLE, AIRBURST_DIST,
    SPEED,
    KEY_FLARE, KEY_CHAFF, KEY_APS, KEY_MAINTENANCE, KEY_SWEEPWING,
    COOLDOWN_FLARE, COOLDOWN_CHAFF, COOLDOWN_APS, COOLDOWN_MAINTENANCE;

    public static MCH_HudItemStringArgs fromString(String name) {
        try {
            return valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return NONE;
        }
    }
}

