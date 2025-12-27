package mcheli.hud;

import mcheli.MCH_Config;
import mcheli.MCH_KeyName;
import mcheli.MCH_Lib;
import mcheli.MCH_MOD;
import net.minecraft.client.Minecraft;
import net.minecraft.util.MathHelper;

import java.util.Date;

public class MCH_HudItemString extends MCH_HudItem {

    private final String posX;
    private final String posY;
    private final String format;
    private final MCH_HudItemStringArgs[] args;
    private final boolean isCenteredString;


    public MCH_HudItemString(int fileLine, String posx, String posy, String fmt, String[] args, boolean centered) {
        super(fileLine);
        this.posX = posx.toLowerCase();
        this.posY = posy.toLowerCase();
        this.format = fmt;

        int argLength = Math.max(0, args.length - 3);
        this.args = new MCH_HudItemStringArgs[argLength];

        for (int i = 0; i < argLength; ++i) {
            try {
                this.args[i] = MCH_HudItemStringArgs.fromString(args[3 + i]);
            } catch (ArrayIndexOutOfBoundsException e) {
                this.args[i] = MCH_HudItemStringArgs.NONE;
            }
        }
        this.isCenteredString = centered;
    }


    public void execute() {
        int x = (int) (MCH_HudItem.centerX + calc(this.posX));
        int y = (int) (MCH_HudItem.centerY + calc(this.posY));
        long dateCount = Minecraft.getMinecraft().thePlayer.worldObj.getTotalWorldTime();
        int worldTime = (int) ((MCH_HudItem.ac.worldObj.getWorldTime() + 6000L) % 24000L);
        Date date = new Date();
        Object[] prm = new Object[this.args.length];

        double hp_per = MCH_HudItem.ac.getMaxHP() > 0 ? (double) MCH_HudItem.ac.getHP() / (double) MCH_HudItem.ac.getMaxHP() : 0.0D;

        for (int i = 0; i < prm.length; ++i) {
            MCH_HudItemStringArgs arg = this.args[i];
            prm[i] = getParameterValue(arg, date, worldTime, hp_per);
        }

        String formattedString = String.format(this.format, prm);
        if (this.isCenteredString) {
            this.drawCenteredString(formattedString, x, y, MCH_HudItem.colorSetting);
        } else {
            this.drawString(formattedString, x, y, MCH_HudItem.colorSetting);
        }
    }

    private Object getParameterValue(MCH_HudItemStringArgs arg, Date date, int worldTime, double hp_per) {
        switch (arg) {
            case NAME:
                return MCH_HudItem.ac.getAcInfo().displayName;
            case ALTITUDE:
                return MCH_HudItem.Altitude;
            case DATE:
                return date;
            case MC_THOR:
                return worldTime / 1000;
            case MC_TMIN:
                return worldTime % 1000 * 36 / 10 / 60;
            case MC_TSEC:
                return worldTime % 1000 * 36 / 10 % 60;
            case MAX_HP:
                return MCH_HudItem.ac.getMaxHP();
            case HP:
                return MCH_HudItem.ac.getHP();
            case HP_PER:
                return hp_per * 100.0D;
            case POS_X:
                return MCH_HudItem.ac.posX;
            case POS_Y:
                return MCH_HudItem.ac.posY;
            case POS_Z:
                return MCH_HudItem.ac.posZ;
            case MOTION_X:
                return MCH_HudItem.ac.motionX;
            case MOTION_Y:
                return MCH_HudItem.ac.motionY;
            case MOTION_Z:
                return MCH_HudItem.ac.motionZ;
            case INVENTORY:
                return MCH_HudItem.ac.getSizeInventory();
            case WPN_NAME:
                return MCH_HudItem.WeaponName;
            case WPN_AMMO:
                return MCH_HudItem.WeaponAmmo;
            case WPN_RM_AMMO:
                return MCH_HudItem.WeaponAllAmmo;
            case RELOAD_PER:
                return MCH_HudItem.ReloadPer;
            case RELOAD_SEC:
                return MCH_HudItem.ReloadSec;
            case MORTAR_DIST:
                return MCH_HudItem.MortarDist;
            case MC_VER:
                return "1.7.10";
            case MOD_VER:
                return MCH_MOD.VER;
            case MOD_NAME:
                return "MCHeli Reforged";
            case YAW:
                return MCH_Lib.getRotate360(MCH_HudItem.ac.getRotYaw() + 180.0F);
            case PITCH:
                return -MCH_HudItem.ac.getRotPitch();
            case ROLL:
                return MathHelper.wrapAngleTo180_float(MCH_HudItem.ac.getRotRoll());
            case PLYR_YAW:
                return MCH_Lib.getRotate360(MCH_HudItem.player.rotationYaw + 180.0F);
            case PLYR_PITCH:
                return -MCH_HudItem.player.rotationPitch;
            case TVM_POS_X:
                return MCH_HudItem.TVM_PosX;
            case TVM_POS_Y:
                return MCH_HudItem.TVM_PosY;
            case TVM_POS_Z:
                return MCH_HudItem.TVM_PosZ;
            case TVM_DIFF:
                return MCH_HudItem.TVM_Diff;
            case CAM_ZOOM:
                return MCH_HudItem.ac.camera.getCameraZoom();
            case UAV_DIST:
                return MCH_HudItem.UAV_Dist;
            case KEY_GUI:
                return MCH_KeyName.getDescOrName(MCH_Config.KeyGUI.prmInt);
            case THROTTLE:
                return MCH_HudItem.ac.getCurrentThrottle() * 100.0D;
            case AIRBURST_DIST:
                return MCH_HudItem.airburstDist;
            case SPEED:
                return Math.sqrt(
                    MCH_HudItem.ac.motionX * MCH_HudItem.ac.motionX +
                        MCH_HudItem.ac.motionY * MCH_HudItem.ac.motionY +
                        MCH_HudItem.ac.motionZ * MCH_HudItem.ac.motionZ
                );
            case KEY_FLARE:
                return MCH_KeyName.getDescOrName(MCH_Config.KeyFlare.prmInt);
            case KEY_CHAFF:
                return MCH_KeyName.getDescOrName(MCH_Config.KeyChaff.prmInt);
            case KEY_APS:
                return MCH_KeyName.getDescOrName(MCH_Config.KeyAPS.prmInt);
            case KEY_MAINTENANCE:
                return MCH_KeyName.getDescOrName(MCH_Config.KeyMaintenance.prmInt);
            case KEY_ECM_JAMMER:
                return MCH_KeyName.getDescOrName(MCH_Config.KeyECMJammer.prmInt);
            case KEY_SWEEPWING:
                return MCH_KeyName.getDescOrName(MCH_Config.KeyZoom.prmInt);
            case KEY_CAMERAMODE:
                return MCH_KeyName.getDescOrName(MCH_Config.KeyCameraMode.prmInt);
            case KEY_VTOL:
                return MCH_KeyName.getDescOrName(MCH_Config.KeyExtra.prmInt);
            case COOLDOWN_FLARE:
                return MCH_HudItem.ac.getFlareTick() / 20f;
            case COOLDOWN_CHAFF:
                return MCH_HudItem.ac.chaff.tick / 20f;
            case COOLDOWN_APS:
                return MCH_HudItem.ac.aps.tick / 20f;
            case COOLDOWN_MAINTENANCE:
                return MCH_HudItem.ac.maintenance.tick / 20f;
            case COOLDOWN_ECM_JAMMER:
                return MCH_HudItem.ac.ecmJammer.tick / 20f;
            case G_FORCE:
                return String.format("%.1fG", MCH_HudItem.ac.gForce);
            default:
                return null;
        }
    }
}
