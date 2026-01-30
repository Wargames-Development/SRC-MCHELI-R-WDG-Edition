package mcheli.hud;

import mcheli.*;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.eval.eval.ExpRuleFactory;
import mcheli.eval.eval.Expression;
import mcheli.eval.eval.var.MapVariable;
import mcheli.helicopter.MCH_EntityHeli;
import mcheli.plane.MCP_EntityPlane;
import mcheli.plane.MCP_PlaneInfo;
import mcheli.weapon.*;
import mcheli.wrapper.W_McClient;
import mcheli.wrapper.W_OpenGlHelper;
import mcheli.wrapper.W_WorldFunc;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import org.lwjgl.opengl.GL11;

import java.util.*;

public abstract class MCH_HudItem extends Gui {

    public static Minecraft mc;
    public static EntityPlayer player;
    public static MCH_EntityAircraft ac;
    public static double width;
    public static double height;
    public static int scaleFactor;
    public static int colorSetting = -16777216;
    protected static double centerX = 0.0D;
    protected static double centerY = 0.0D;
    protected static Random rand = new Random();
    protected static int altitudeUpdateCount = 0;
    protected static int Altitude = 0;
    protected static float prevRadarRot;
    protected static String WeaponName = "";
    protected static String WeaponAmmo = "";
    protected static String WeaponAllAmmo = "";
    protected static MCH_WeaponSet CurrentWeapon = null;
    protected static float ReloadPer = 0.0F;
    protected static float ReloadSec = 0.0F;
    protected static float MortarDist = 0.0F;
    protected static MCH_LowPassFilterFloat StickX_LPF = new MCH_LowPassFilterFloat(4);
    protected static MCH_LowPassFilterFloat StickY_LPF = new MCH_LowPassFilterFloat(4);
    protected static double StickX;
    protected static double StickY;
    protected static double TVM_PosX;
    protected static double TVM_PosY;
    protected static double TVM_PosZ;
    protected static double TVM_Diff;
    protected static double UAV_Dist;
    protected static int countFuelWarn;
    protected static ArrayList EntityList;
    protected static ArrayList EnemyList;
    protected static Map varMap = null;
    protected static float partialTicks;
    protected static String railgunPer;
    protected static String airburstDist;
    private static MCH_HudItemExit dummy = new MCH_HudItemExit(0);
    public final int fileLine;
    protected MCH_Hud parent;


    public MCH_HudItem(int fileLine) {
        this.fileLine = fileLine;
        super.zLevel = -110.0F;
    }

    public static void update() {
        MCH_WeaponSet ws = ac.getCurrentWeapon(player);
        updateRadar(ac);
        updateStick();
        updateAltitude(ac);
        updateTvMissile(ac);
        updateUAV(ac);
        updateWeapon(ac, ws);
        updateVarMap(ac, ws);
    }

    public static String toFormula(String s) {
        return s.toLowerCase().replaceAll("#", "0x").replace("\t", " ").replace(" ", "");
    }

    public static double calc(String s) {
        Expression exp = ExpRuleFactory.getDefaultRule().parse(s);
        exp.setVariable(new MapVariable(varMap));
        return exp.evalDouble();
    }

    public static long calcLong(String s) {
        Expression exp = ExpRuleFactory.getDefaultRule().parse(s);
        exp.setVariable(new MapVariable(varMap));
        return exp.evalLong();
    }

    public static void drawRect(double par0, double par1, double par2, double par3, int par4) {
        double j1;
        if (par0 < par2) {
            j1 = par0;
            par0 = par2;
            par2 = j1;
        }

        if (par1 < par3) {
            j1 = par1;
            par1 = par3;
            par3 = j1;
        }

        float f3 = (float) (par4 >> 24 & 255) / 255.0F;
        float f = (float) (par4 >> 16 & 255) / 255.0F;
        float f1 = (float) (par4 >> 8 & 255) / 255.0F;
        float f2 = (float) (par4 & 255) / 255.0F;
        Tessellator tessellator = Tessellator.instance;
        GL11.glEnable(3042);
        GL11.glDisable(3553);
        W_OpenGlHelper.glBlendFunc(770, 771, 1, 0);
        GL11.glColor4f(f, f1, f2, f3);
        tessellator.startDrawingQuads();
        tessellator.addVertex(par0, par3, 0.0D);
        tessellator.addVertex(par2, par3, 0.0D);
        tessellator.addVertex(par2, par1, 0.0D);
        tessellator.addVertex(par0, par1, 0.0D);
        tessellator.draw();
        GL11.glEnable(3553);
        GL11.glDisable(3042);
    }

    public static void updateVarMap(MCH_EntityAircraft ac, MCH_WeaponSet ws) {
        if (varMap == null) {
            varMap = new LinkedHashMap();
        }

        updateVarMapItem("color", getColor());
        updateVarMapItem("center_x", centerX);
        updateVarMapItem("center_y", centerY);
        updateVarMapItem("width", width);
        updateVarMapItem("height", height);
        updateVarMapItem("time", (double) (player.worldObj.getWorldTime() % 24000L));
        updateVarMapItem("test_mode", MCH_Config.TestMode.prmBool ? 1.0D : 0.0D);
        updateVarMapItem("plyr_yaw", MathHelper.wrapAngleTo180_float(player.rotationYaw));
        updateVarMapItem("plyr_pitch", player.rotationPitch);
        updateVarMapItem("yaw", MathHelper.wrapAngleTo180_float(ac.getRotYaw()));
        updateVarMapItem("pitch", ac.getRotPitch());
        updateVarMapItem("roll", MathHelper.wrapAngleTo180_float(ac.getRotRoll()));
        updateVarMapItem("altitude", Altitude);
        updateVarMapItem("sea_alt", getSeaAltitude(ac));
        updateVarMapItem("have_radar", ac.isEntityRadarMounted() ? 1.0D : 0.0D);
        updateVarMapItem("radar_rot", getRadarRot(ac));
        updateVarMapItem("hp", ac.getHP());
        updateVarMapItem("max_hp", ac.getMaxHP());
        updateVarMapItem("hp_rto", ac.getMaxHP() > 0 ? (double) ac.getHP() / (double) ac.getMaxHP() : 0.0D);
        updateVarMapItem("throttle", ac.getCurrentThrottle());
        updateVarMapItem("pos_x", ac.posX);
        updateVarMapItem("pos_y", ac.posY);
        updateVarMapItem("pos_z", ac.posZ);
        updateVarMapItem("motion_x", ac.motionX);
        updateVarMapItem("motion_y", ac.motionY);
        updateVarMapItem("motion_z", ac.motionZ);
        updateVarMapItem("speed", Math.sqrt(ac.motionX * ac.motionX + ac.motionY * ac.motionY + ac.motionZ * ac.motionZ));
        updateVarMapItem("fuel", ac.getFuelP());
        updateVarMapItem("low_fuel", isLowFuel(ac));
        updateVarMapItem("stick_x", StickX);
        updateVarMapItem("stick_y", StickY);
        updateVarMap_Weapon(ws);
        updateVarMapItem("vtol_stat", getVtolStat(ac));
        updateVarMapItem("free_look", getFreeLook(ac, player));
        updateVarMapItem("gunner_mode", ac.getIsGunnerMode(player) ? 1.0D : 0.0D);
        updateVarMapItem("cam_mode", ac.getCameraMode(player));
        updateVarMapItem("cam_zoom", ac.camera.getCameraZoom());
        updateVarMapItem("auto_pilot", getAutoPilot(ac, player));
        updateVarMapItem("have_flare", ac.haveFlare() ? 1.0D : 0.0D);
        updateVarMapItem("can_flare", ac.canUseFlare() ? 1.0D : 0.0D);
        updateVarMapItem("inventory", ac.getSizeInventory());
        updateVarMapItem("hovering", ac instanceof MCH_EntityHeli && ac.isHoveringMode() ? 1.0D : 0.0D);
        updateVarMapItem("is_uav", ac.isUAV() ? 1.0D : 0.0D);
        updateVarMapItem("uav_fs", getUAV_Fs(ac));
        updateVarMapItem("can_chaff", ac.canUseChaff() ? 1.0D : 0.0D);
        updateVarMapItem("can_maintenance", ac.canUseMaintenance() ? 1.0D : 0.0D);
        updateVarMapItem("have_chaff", ac.haveChaff() ? 1.0D : 0.0D);
        updateVarMapItem("have_maintenance", ac.haveMaintenance() ? 1.0D : 0.0D);
        updateVarMapItem("have_aps", ac.haveAPS() ? 1.0D : 0.0D);
        updateVarMapItem("can_aps", ac.canUseAPS() ? 1.0D : 0.0D);
        updateVarMapItem("have_ecm_jammer", ac.haveECMJammer() ? 1.0D : 0.0D);
        updateVarMapItem("can_ecm_jammer", ac.canUseECMJammer() ? 1.0D : 0.0D);
        updateVarMapItem("ecm_jammer_type", ac.getAcInfo().ecmJammerType);
        updateVarMapItem("is_engine_shutdown", ac.getHP() * 100 / ac.getMaxHP() < ac.getAcInfo().engineShutdownThreshold ? 1.0D : 0.0D);
        updateVarMapItem("hud_type", ac.getAcInfo().hudType);
        updateVarMapItem("weapon_group_type", ac.getAcInfo().weaponGroupType);
        updateVarMapItem("third_person", Minecraft.getMinecraft().gameSettings.thirdPersonView);
        updateVarMapItem("have_rwr", ac.getAcInfo().hasRWR ? 1.0D : 0.0D);
        updateVarMapItem("missile_lock_type", ac.missileDetector != null ? ac.missileDetector.missileLockType : 0.0D);
        updateVarMapItem("vehicle_lock_type", ac.missileDetector != null ? ac.missileDetector.vehicleLockType : 0.0D);
        updateVarMapItem("missile_lock_dist", ac.missileDetector != null ? ac.missileDetector.missileLockDist : 0.0D);
        updateVarMapItem("third_person", Minecraft.getMinecraft().gameSettings.thirdPersonView);
        updateVarMapItem("have_rwr", ac.getAcInfo().hasRWR ? 1.0D : 0.0D);
        updateVarMapItem("have_dircm", ac.getAcInfo().hasDIRCM ? 1.0D : 0.0D);
        updateVarMapItem("is_jammed", ac.jammingTick > 0 ? 1.0D : 0.0D);
        if (ac instanceof MCP_EntityPlane) {
            MCP_PlaneInfo info = ((MCP_EntityPlane) ac).getPlaneInfo();
            updateVarMapItem("have_sweepwing", info.isVariableSweepWing ? 1.0D : 0.0D);
            if (((MCP_EntityPlane) ac).partWing != null) {
                updateVarMapItem("is_sweepwing_fold", ((MCP_EntityPlane) ac).partWing.isOFF() ? 1.0D : 0.0D);
            } else {
                updateVarMapItem("is_sweepwing_fold", 0.0D);
            }
        }
    }

    public static void updateVarMapItem(String key, double value) {
        varMap.put(key, value);
    }

    public static void drawVarMap() {
        MCH_Config var10000 = MCH_MOD.config;
        if (MCH_Config.TestMode.prmBool) {
            int i = 0;
            int x = (int) (-300.0D + centerX);
            int y = (int) (-100.0D + centerY);
            Iterator i$ = varMap.keySet().iterator();

            while (i$.hasNext()) {
                String key = (String) i$.next();
                dummy.drawString(key, x, y, -12544);
                Double d = (Double) varMap.get(key);
                String fmt = key.equalsIgnoreCase("color") ? String.format(": 0x%08X", d.intValue()) : String.format(": %.2f", d);
                dummy.drawString(fmt, x + 50, y, -12544);
                ++i;
                y += 8;
                if (i == varMap.size() / 2) {
                    x = (int) (200.0D + centerX);
                    y = (int) (-100.0D + centerY);
                }
            }
        }

    }

    private static double getUAV_Fs(MCH_EntityAircraft ac) {
        double uav_fs = 0.0D;
        if (ac.isUAV() && ac.getUavStation() != null) {
            double dx = ac.posX - ac.getUavStation().posX;
            double dz = ac.posZ - ac.getUavStation().posZ;
            float dist = (float) Math.sqrt(dx * dx + dz * dz);
            float distMax = 120.0F;
            if (dist > 120.0F) {
                dist = 120.0F;
            }

            uav_fs = (double) (1.0F - dist / 120.0F);
        }

        return uav_fs;
    }

    private static void updateVarMap_Weapon(MCH_WeaponSet ws) {
        int reloading = 0;
        double wpn_heat = 0.0D;
        int is_heat_wpn = 0;
        byte sight_type = 0;
        double lock = 0.0D;
        float rel_time = 0.0F;
        int display_mortar_dist = 0;
        float railgun_time = 0.0F;
        int has_airburst = 0;
        int cross_type = 0;
        if (ws != null) {
            MCH_WeaponBase wb = ws.getCurrentWeapon();
            MCH_WeaponInfo wi = wb.getInfo();
            if (wi == null) {
                return;
            }

            is_heat_wpn = wi.maxHeatCount > 0 ? 1 : 0;
            reloading = ws.isInPreparation() ? 1 : 0;
            display_mortar_dist = wi.displayMortarDistance ? 1 : 0;
            if (wi.delay > wi.reloadTime) {
                rel_time = (float) ws.countWait / (float) (wi.delay > 0 ? wi.delay : 1);
                if (rel_time < 0.0F) {
                    rel_time = -rel_time;
                }

                if (rel_time > 1.0F) {
                    rel_time = 1.0F;
                }
            } else {
                rel_time = (float) ws.countReloadWait / (float) (wi.reloadTime > 0 ? wi.reloadTime : 1);
            }

            if (wi.maxHeatCount > 0) {
                double cntLockMax = (double) ws.currentHeat / (double) wi.maxHeatCount;
                wpn_heat = Math.min(cntLockMax, 1.0D);
            }

            int cntLockMax1 = wb.getLockCountMax();
            MCH_SightType sight = wb.getSightType();
            if (sight == MCH_SightType.LOCK && cntLockMax1 > 0) {
                lock = (double) wb.getLockCount() / (double) cntLockMax1;
                sight_type = 2;
            }

            if (sight == MCH_SightType.ROCKET) {
                sight_type = 1;
            }

            cross_type = wi.crossType;

            if (wb instanceof MCH_WeaponRailgun) {
                railgun_time = ((MCH_WeaponRailgun) wb).getRailgunTime();
            }
            if (wb instanceof MCH_WeaponASMissile) {
                lock = ((MCH_WeaponASMissile) wb).getLockTime();
            }
            has_airburst = wb.airburstDist == 0 ? 0 : 1;
        }

        updateVarMapItem("reloading", reloading);
        updateVarMapItem("reload_time", rel_time);
        updateVarMapItem("wpn_heat", wpn_heat);
        updateVarMapItem("is_heat_wpn", is_heat_wpn);
        updateVarMapItem("sight_type", sight_type);
        updateVarMapItem("lock", lock);
        updateVarMapItem("dsp_mt_dist", display_mortar_dist);
        updateVarMapItem("mt_dist", MortarDist);
        updateVarMapItem("railgun_time", railgun_time);
        updateVarMapItem("has_airburst", has_airburst);
        updateVarMapItem("cross_type", cross_type);

//        if (ws != null) {
//            for (int i = 1; i <= ws.weapons.length; i++) {
//                MCH_WeaponBase wb = ws.weapons[i - 1];
//                if (wb != null) {
//                    MCH_WeaponInfo wi = wb.getInfo();
//                    if (wi != null) {
//                        int sight_type_i = 0;
//                        double wpn_heat_i = 0.0D;
//                        int is_heat_wpn_i = wi.maxHeatCount > 0 ? 1 : 0;
//                        if (wi.maxHeatCount > 0) {
//                            double cntLockMax = (double) wi.heatCount / (double) wi.maxHeatCount;
//                            wpn_heat_i = Math.min(cntLockMax, 1.0D);
//                        }
//                        MCH_SightType sight = wb.getSightType();
//                        if (sight == MCH_SightType.LOCK && wb.getLockCountMax() > 0) {
//                            sight_type_i = 2;
//                        }
//                        if (sight == MCH_SightType.ROCKET) {
//                            sight_type_i = 1;
//                        }
//                        updateVarMapItem("wpn_heat" + i, wpn_heat_i);
//                        updateVarMapItem("is_heat_wpn" + i, is_heat_wpn_i);
//                        updateVarMapItem("sight_type" + i, sight_type_i);
//                        updateVarMapItem("cross_type" + i, wi.crossType);
//                    }
//                }
//            }
//        }

    }

    public static int isLowFuel(MCH_EntityAircraft ac) {
        byte is_low_fuel = 0;
        if (countFuelWarn <= 0) {
            countFuelWarn = 280;
        }

        --countFuelWarn;
        if (countFuelWarn < 160 && ac.getMaxFuel() > 0 && ac.getFuelP() < 0.1F && !ac.isInfinityFuel(player, false)) {
            is_low_fuel = 1;
        }

        return is_low_fuel;
    }

    public static double getSeaAltitude(MCH_EntityAircraft ac) {
        double a = ac.posY - ac.worldObj.getHorizon();
        return Math.max(a, 0.0D);
    }

    public static float getRadarRot(MCH_EntityAircraft ac) {
        float rot = (float) ac.getRadarRotate();
        float prevRot = prevRadarRot;
        if (rot < prevRot) {
            rot += 360.0F;
        }

        prevRadarRot = (float) ac.getRadarRotate();
        return MCH_Lib.smooth(rot, prevRot, partialTicks);
    }

    public static int getVtolStat(MCH_EntityAircraft ac) {
        return ac instanceof MCP_EntityPlane ? ((MCP_EntityPlane) ac).getVtolMode() : 0;
    }

    public static int getFreeLook(MCH_EntityAircraft ac, EntityPlayer player) {
        return ac.isPilot(player) && ac.canSwitchFreeLook() && ac.isFreeLookMode() ? 1 : 0;
    }

    public static int getAutoPilot(MCH_EntityAircraft ac, EntityPlayer player) {
        return ac instanceof MCP_EntityPlane && ac.isPilot(player) && ac.getIsGunnerMode(player) ? 1 : 0;
    }

    public static double getColor() {
        long l = (long) colorSetting;
        l &= 4294967295L;
        return (double) l;
    }

    private static void updateStick() {
        StickX_LPF.put((float) (MCH_ClientCommonTickHandler.getCurrentStickX() / MCH_ClientCommonTickHandler.getMaxStickLength()));
        StickY_LPF.put((float) (-MCH_ClientCommonTickHandler.getCurrentStickY() / MCH_ClientCommonTickHandler.getMaxStickLength()));
        StickX = StickX_LPF.getAvg();
        StickY = StickY_LPF.getAvg();
    }

    private static void updateRadar(MCH_EntityAircraft ac) {
        EntityList = ac.getRadarEntityList();
        EnemyList = ac.getRadarEnemyList();
    }

    private static void updateAltitude(MCH_EntityAircraft ac) {
        if (altitudeUpdateCount <= 0) {
            int heliY = (int) ac.posY;
            if (heliY > 256) {
                heliY = 256;
            }

            for (int i = 0; i < 256 && heliY - i > 0; ++i) {
                int id = W_WorldFunc.getBlockId(ac.worldObj, (int) ac.posX, heliY - i, (int) ac.posZ);
                if (id != 0) {
                    Altitude = i;
                    if (ac.posY > 256.0D) {
                        Altitude = (int) ((double) Altitude + (ac.posY - 256.0D));
                    }
                    break;
                }
            }

            altitudeUpdateCount = 30;
        } else {
            --altitudeUpdateCount;
        }

    }

    public static void updateWeapon(MCH_EntityAircraft ac, MCH_WeaponSet ws) {
        if (ac.getWeaponNum() > 0) {
            if (ws != null) {
                CurrentWeapon = ws;
                WeaponName = ac.isPilotReloading() ? "-- Reloading --" : ws.getName();
                if (ws.getAmmoNumMax() > 0) {
                    WeaponAmmo = ac.isPilotReloading() ? "----" : String.format("%4d", ws.getAmmoNum());
                    WeaponAllAmmo = ac.isPilotReloading() ? "----" : String.format("%4d", ws.getRestAllAmmoNum());
                } else {
                    WeaponAmmo = "";
                    WeaponAllAmmo = "";
                }

                MCH_WeaponInfo wi = ws.getInfo();
                if (wi.displayMortarDistance) {
                    MortarDist = (float) ac.getLandInDistance(player);
                } else {
                    MortarDist = -1.0F;
                }

                if (wi.delay > wi.reloadTime) {
                    ReloadSec = ws.countWait >= 0 ? (float) ws.countWait : (float) (-ws.countWait);
                    ReloadPer = (float) ws.countWait / (float) (wi.delay > 0 ? wi.delay : 1);
                    if (ReloadPer < 0.0F) {
                        ReloadPer = -ReloadPer;
                    }

                    if (ReloadPer > 1.0F) {
                        ReloadPer = 1.0F;
                    }
                } else {
                    ReloadSec = (float) ws.countReloadWait;
                    ReloadPer = (float) ws.countReloadWait / (float) (wi.reloadTime > 0 ? wi.reloadTime : 1);
                }

                if (ws.getCurrentWeapon() instanceof MCH_WeaponRailgun) {
                    railgunPer = ((MCH_WeaponRailgun) ws.getCurrentWeapon()).getRailgunTime() * 100 + "%";
                }

                if (ws.getCurrentWeapon().airburstDist <= 5 || ws.getCurrentWeapon().airburstDist >= 300) {
                    airburstDist = "---";
                } else {
                    airburstDist = ws.getCurrentWeapon().airburstDist + "m + 3";
                }

                ReloadSec /= 20.0F;
                ReloadPer = (1.0F - ReloadPer) * 100.0F;
            }
        }
    }

    public static void updateUAV(MCH_EntityAircraft ac) {
        if (ac.isUAV() && ac.getUavStation() != null) {
            double dx = ac.posX - ac.getUavStation().posX;
            double dz = ac.posZ - ac.getUavStation().posZ;
            UAV_Dist = (float) Math.sqrt(dx * dx + dz * dz);
        } else {
            UAV_Dist = 0.0D;
        }

    }

    private static void updateTvMissile(MCH_EntityAircraft ac) {
        MCH_EntityTvMissile tvmissile = ac.getTVMissile();
        if (tvmissile != null) {
            TVM_PosX = tvmissile.posX;
            TVM_PosY = tvmissile.posY;
            TVM_PosZ = tvmissile.posZ;
            double dx = tvmissile.posX - ac.posX;
            double dy = tvmissile.posY - ac.posY;
            double dz = tvmissile.posZ - ac.posZ;
            TVM_Diff = Math.sqrt(dx * dx + dy * dy + dz * dz);
        } else {
            TVM_PosX = 0.0D;
            TVM_PosY = 0.0D;
            TVM_PosZ = 0.0D;
            TVM_Diff = 0.0D;
        }

    }

    public abstract void execute();

    public boolean canExecute() {
        return !this.parent.isIfFalse;
    }

    public void drawCenteredString(String s, int x, int y, int color) {
        this.drawCenteredString(mc.fontRenderer, s, x, y, color);
    }

    public void drawString(String s, int x, int y, int color) {
        this.drawString(mc.fontRenderer, s, x, y, color);
    }

    public void drawTexture(String name, double left, double top, double width, double height, double uLeft, double vTop, double uWidth, double vHeight, float rot, int textureWidth, int textureHeight) {
        GL11.glPushMatrix();
        GL11.glTranslated(left + width / 2.0D, top + height / 2.0D, 0.0D);
        GL11.glRotatef(rot, 0.0F, 0.0F, 1.0F);
        float fx = (float) (1.0D / (double) textureWidth);
        float fy = (float) (1.0D / (double) textureHeight);
        GL11.glEnable(3042 /* GL_BLEND */);
        GL11.glDisable(2929 /* GL_DEPTH_TEST */);
        GL11.glDepthMask(false);
        GL11.glBlendFunc(770, 771);
        GL11.glDisable(3008 /* GL_ALPHA_TEST */);
        W_McClient.MOD_bindTexture("textures/gui/" + name + ".png");
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        tessellator.addVertexWithUV(-width / 2.0D, height / 2.0D, -90, uLeft * (double) fx, (vTop + vHeight) * (double) fy);
        tessellator.addVertexWithUV(width / 2.0D, height / 2.0D, -90, (uLeft + uWidth) * (double) fx, (vTop + vHeight) * (double) fy);
        tessellator.addVertexWithUV(width / 2.0D, -height / 2.0D, -90, (uLeft + uWidth) * (double) fx, vTop * (double) fy);
        tessellator.addVertexWithUV(-width / 2.0D, -height / 2.0D, -90, uLeft * (double) fx, vTop * (double) fy);
        tessellator.draw();
        GL11.glDepthMask(true);
        GL11.glEnable(2929 /* GL_DEPTH_TEST */);
        GL11.glEnable(3008 /* GL_ALPHA_TEST */);
        GL11.glPopMatrix();
    }

    public void drawLine(double[] line, int color) {
        this.drawLine(line, color, 1);
    }

    public void drawLine(double[] line, int color, int mode) {
        GL11.glPushMatrix();
        GL11.glEnable(3042);
        GL11.glDisable(3553);
        GL11.glBlendFunc(770, 771);
        GL11.glColor4ub((byte) (color >> 16 & 255), (byte) (color >> 8 & 255), (byte) (color & 255), (byte) (color >> 24 & 255));
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawing(mode);

        for (int i = 0; i < line.length; i += 2) {
            tessellator.addVertex(line[i], line[i + 1], super.zLevel);
        }

        tessellator.draw();
        GL11.glEnable(3553);
        GL11.glDisable(3042);
        GL11.glColor4b((byte) -1, (byte) -1, (byte) -1, (byte) -1);
        GL11.glPopMatrix();
    }

    public void drawLineStipple(double[] line, int color, int factor, int pattern) {
        GL11.glEnable(2852);
        GL11.glLineStipple(factor * scaleFactor, (short) pattern);
        this.drawLine(line, color);
        GL11.glDisable(2852);
    }

    public void drawPoints(ArrayList points, int color, int pointWidth) {
        int prevWidth = GL11.glGetInteger(2833);
        GL11.glPushMatrix();
        GL11.glEnable(3042);
        GL11.glDisable(3553);
        GL11.glBlendFunc(770, 771);
        GL11.glColor4ub((byte) (color >> 16 & 255), (byte) (color >> 8 & 255), (byte) (color & 255), (byte) (color >> 24 & 255));
        GL11.glPointSize((float) pointWidth);
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawing(0);

        for (int i = 0; i < points.size(); i += 2) {
            tessellator.addVertex((Double) points.get(i), (Double) points.get(i + 1), 0.0D);
        }

        tessellator.draw();
        GL11.glEnable(3553);
        GL11.glDisable(3042);
        GL11.glPopMatrix();
        GL11.glColor4b((byte) -1, (byte) -1, (byte) -1, (byte) -1);
        GL11.glPointSize((float) prevWidth);
    }

}
