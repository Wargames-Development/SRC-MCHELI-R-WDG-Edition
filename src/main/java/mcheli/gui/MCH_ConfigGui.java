package mcheli.gui;

import mcheli.*;
import mcheli.aircraft.MCH_AircraftInfo;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.aircraft.MCH_PacketNotifyInfoReloaded;
import mcheli.multiplay.MCH_GuiTargetMarker;
import mcheli.weapon.MCH_WeaponInfoManager;
import mcheli.wrapper.W_GuiButton;
import mcheli.wrapper.W_GuiContainer;
import mcheli.wrapper.W_McClient;
import mcheli.wrapper.W_ScaledResolution;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.player.EntityPlayer;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MCH_ConfigGui extends W_GuiContainer {

    public static final int BUTTON_RENDER = 50;
    public static final int BUTTON_KEY_BINDING = 51;
    public static final int BUTTON_PREV_CONTROL = 52;
    public static final int BUTTON_DEVELOP = 55;
    public static final int BUTTON_KEY_LIST = 53;
    public static final int BUTTON_KEY_RESET_ALL = 54;
    public static final int BUTTON_KEY_LIST_BASE = 200;
    public static final int BUTTON_KEY_RESET_BASE = 300;
    public static final int BUTTON_DEV_RELOAD_AC = 400;
    public static final int BUTTON_DEV_RELOAD_WEAPON = 401;
    public static final int BUTTON_DEV_RELOAD_HUD = 402;
    public static final int BUTTON_SAVE_CLOSE = 100;
    public static final int BUTTON_CANCEL = 101;
    public static final int SCREEN_CONTROLS = 0;
    public static final int SCREEN_RENDER = 1;
    public static final int SCREEN_KEY_BIND = 2;
    public static final int SCREEN_DEVELOP = 3;
    private final EntityPlayer thePlayer;
    public List listControlButtons;
    public List listRenderButtons;
    public List listKeyBindingButtons;
    public List listDevelopButtons;
    public MCH_GuiList keyBindingList;
    public int waitKeyButtonId;
    public int waitKeyAcceptCount;
    public int currentScreenId = 0;
    private int scaleFactor;
    private MCH_GuiOnOffButton buttonMouseInv;
    private MCH_GuiOnOffButton buttonStickModeHeli;
    private MCH_GuiOnOffButton buttonStickModePlane;
    private MCH_GuiOnOffButton buttonHideKeyBind;
    private MCH_GuiOnOffButton buttonShowHUDTP;
    private MCH_GuiOnOffButton buttonSmoothShading;
    private MCH_GuiOnOffButton buttonShowEntityMarker;
    private MCH_GuiOnOffButton buttonMarkThroughWall;
    private MCH_GuiOnOffButton buttonReplaceCamera;
    private MCH_GuiOnOffButton buttonNewExplosion;
    private MCH_GuiSlider sliderEntityMarkerSize;
    private MCH_GuiSlider sliderBlockMarkerSize;
    private MCH_GuiSlider sliderSensitivity;
    private MCH_GuiSlider[] sliderHitMark;
    private MCH_GuiOnOffButton buttonTestMode;
    private MCH_GuiOnOffButton buttonThrottleHeli;
    private MCH_GuiOnOffButton buttonThrottlePlane;
    private MCH_GuiOnOffButton buttonThrottleTank;
    private MCH_GuiOnOffButton buttonFlightSimMode;
    private MCH_GuiOnOffButton buttonSwitchWeaponWheel;
    private W_GuiButton buttonReloadAircraftInfo;
    private W_GuiButton buttonReloadWeaponInfo;
    private W_GuiButton buttonReloadAllHUD;
    private int ignoreButtonCounter = 0;


    public MCH_ConfigGui(EntityPlayer player) {
        super(new MCH_ConfigGuiContainer(player));
        this.thePlayer = player;
        super.xSize = 330;
        super.ySize = 200;
    }

    public void initGui() {
        super.initGui();
        super.buttonList.clear();
        int x1 = super.guiLeft + 10;
        int x2 = super.guiLeft + 10 + 150 + 10;
        int y = super.guiTop;
        boolean DY = true;
        this.listControlButtons = new ArrayList();
        this.buttonMouseInv = new MCH_GuiOnOffButton(0, x1, y + 25, 150, 20, MCH_I18n.format("gui.mcheli.invert_mouse"));
        this.sliderSensitivity = new MCH_GuiSlider(0, x1, y + 50, 150, 20, MCH_I18n.format("gui.mcheli.sensitivity.format"), 0.0F, 0.0F, 30.0F, 0.1F);
        this.buttonFlightSimMode = new MCH_GuiOnOffButton(0, x1, y + 75, 150, 20, MCH_I18n.format("gui.mcheli.mouse_flight_sim_mode"));
        this.buttonSwitchWeaponWheel = new MCH_GuiOnOffButton(0, x1, y + 100, 150, 20, MCH_I18n.format("gui.mcheli.switch_weapon_wheel"));
        this.listControlButtons.add(new W_GuiButton(50, x1, y + 125, 150, 20, MCH_I18n.format("gui.mcheli.render_settings")));
        this.listControlButtons.add(new W_GuiButton(51, x1, y + 150, 150, 20, MCH_I18n.format("gui.mcheli.key_binding_forward")));
        this.listControlButtons.add(new W_GuiButton(55, x2, y + 150, 150, 20, MCH_I18n.format("gui.mcheli.development_forward")));
        this.buttonTestMode = new MCH_GuiOnOffButton(0, x1, y + 175, 150, 20, MCH_I18n.format("gui.mcheli.test_mode"));
        this.buttonStickModeHeli = new MCH_GuiOnOffButton(0, x2, y + 25, 150, 20, MCH_I18n.format("gui.mcheli.stick_mode_heli"));
        this.buttonStickModePlane = new MCH_GuiOnOffButton(0, x2, y + 50, 150, 20, MCH_I18n.format("gui.mcheli.stick_mode_plane"));
        this.buttonThrottleHeli = new MCH_GuiOnOffButton(0, x2, y + 75, 150, 20, MCH_I18n.format("gui.mcheli.throttle_down_heli"));
        this.buttonThrottlePlane = new MCH_GuiOnOffButton(0, x2, y + 100, 150, 20, MCH_I18n.format("gui.mcheli.throttle_down_plane"));
        this.buttonThrottleTank = new MCH_GuiOnOffButton(0, x2, y + 125, 150, 20, MCH_I18n.format("gui.mcheli.throttle_down_tank"));
        this.listControlButtons.add(this.buttonMouseInv);
        this.listControlButtons.add(this.buttonStickModeHeli);
        this.listControlButtons.add(this.buttonStickModePlane);
        this.listControlButtons.add(this.sliderSensitivity);
        this.listControlButtons.add(this.buttonThrottleHeli);
        this.listControlButtons.add(this.buttonThrottlePlane);
        this.listControlButtons.add(this.buttonThrottleTank);
        this.listControlButtons.add(this.buttonTestMode);
        this.listControlButtons.add(this.buttonFlightSimMode);
        this.listControlButtons.add(this.buttonSwitchWeaponWheel);
        Iterator id = this.listControlButtons.iterator();

        W_GuiButton idr;
        while (id.hasNext()) {
            idr = (W_GuiButton) id.next();
            super.buttonList.add(idr);
        }

        this.listRenderButtons = new ArrayList<W_GuiButton>();
        this.buttonShowHUDTP = new MCH_GuiOnOffButton(0, x1, y + 25, 150, 20, MCH_I18n.format("gui.mcheli.show_hud_third_person"));
        this.buttonHideKeyBind = new MCH_GuiOnOffButton(0, x1, y + 50, 150, 20, MCH_I18n.format("gui.mcheli.hide_key_binding"));
        this.sliderHitMark = new MCH_GuiSlider[]{
            new MCH_GuiSlider(0, x1 + 0, y + 125, 75, 20, MCH_I18n.format("gui.mcheli.alpha.format"), 0.0F, 0.0F, 255.0F, 16.0F),
            new MCH_GuiSlider(0, x1 + 75, y + 75, 75, 20, MCH_I18n.format("gui.mcheli.red.format"), 0.0F, 0.0F, 255.0F, 16.0F),
            new MCH_GuiSlider(0, x1 + 75, y + 100, 75, 20, MCH_I18n.format("gui.mcheli.green.format"), 0.0F, 0.0F, 255.0F, 16.0F),
            new MCH_GuiSlider(0, x1 + 75, y + 125, 75, 20, MCH_I18n.format("gui.mcheli.blue.format"), 0.0F, 0.0F, 255.0F, 16.0F)
        };
        this.buttonReplaceCamera = new MCH_GuiOnOffButton(0, x1, y + 150, 150, 20, MCH_I18n.format("gui.mcheli.replace_camera"));
        this.listRenderButtons.add(new W_GuiButton(52, x1, y + 175, 90, 20, MCH_I18n.format("gui.mcheli.controls_back")));
        this.buttonSmoothShading = new MCH_GuiOnOffButton(0, x2, y + 25, 150, 20, MCH_I18n.format("gui.mcheli.smooth_shading"));
        this.buttonShowEntityMarker = new MCH_GuiOnOffButton(0, x2, y + 50, 150, 20, MCH_I18n.format("gui.mcheli.show_entity_marker"));
        this.sliderEntityMarkerSize = new MCH_GuiSlider(0, x2 + 30, y + 75, 120, 20, MCH_I18n.format("gui.mcheli.entity_marker_size.format"), 10.0F, 0.0F, 30.0F, 1.0F);
        this.sliderBlockMarkerSize = new MCH_GuiSlider(0, x2 + 60, y + 100, 90, 20, MCH_I18n.format("gui.mcheli.block_marker_size.format"), 10.0F, 0.0F, 20.0F, 1.0F);
        this.buttonMarkThroughWall = new MCH_GuiOnOffButton(0, x2 + 30, y + 100, 120, 20, MCH_I18n.format("gui.mcheli.mark_through_wall"));
        this.buttonNewExplosion = new MCH_GuiOnOffButton(0, x2, y + 150, 150, 20, MCH_I18n.format("gui.mcheli.new_explosion"));
        this.listRenderButtons.add(this.buttonShowHUDTP);

        for (int var12 = 0; var12 < this.sliderHitMark.length; ++var12) {
            this.listRenderButtons.add(this.sliderHitMark[var12]);
        }

        this.listRenderButtons.add(this.buttonSmoothShading);
        this.listRenderButtons.add(this.buttonHideKeyBind);
        this.listRenderButtons.add(this.buttonShowEntityMarker);
        this.listRenderButtons.add(this.buttonReplaceCamera);
        this.listRenderButtons.add(this.buttonNewExplosion);
        this.listRenderButtons.add(this.sliderEntityMarkerSize);
        this.listRenderButtons.add(this.sliderBlockMarkerSize);
        id = this.listRenderButtons.iterator();

        while (id.hasNext()) {
            idr = (W_GuiButton) id.next();
            super.buttonList.add(idr);
        }

        this.listKeyBindingButtons = new ArrayList();
        this.waitKeyButtonId = 0;
        this.waitKeyAcceptCount = 0;
        this.keyBindingList = new MCH_GuiList(53, 7, x1, y + 25 - 2, 310, 150, "");
        this.listKeyBindingButtons.add(this.keyBindingList);
        this.listKeyBindingButtons.add(new W_GuiButton(52, x1, y + 175, 90, 20, MCH_I18n.format("gui.mcheli.controls_back")));
        this.listKeyBindingButtons.add(new W_GuiButton(54, x1 + 90, y + 175, 60, 20, MCH_I18n.format("gui.mcheli.reset_all")));
        boolean var13 = true;
        boolean var14 = true;
        MCH_GuiListItemKeyBind[] listKeyBindItems = new MCH_GuiListItemKeyBind[28];
        listKeyBindItems[0] = new MCH_GuiListItemKeyBind(200, 300, x1, MCH_I18n.format("gui.mcheli.key.up"), MCH_Config.KeyUp);
        listKeyBindItems[1] = new MCH_GuiListItemKeyBind(201, 301, x1, MCH_I18n.format("gui.mcheli.key.down"), MCH_Config.KeyDown);
        listKeyBindItems[2] = new MCH_GuiListItemKeyBind(202, 302, x1, MCH_I18n.format("gui.mcheli.key.right"), MCH_Config.KeyRight);
        listKeyBindItems[3] = new MCH_GuiListItemKeyBind(203, 303, x1, MCH_I18n.format("gui.mcheli.key.left"), MCH_Config.KeyLeft);
        listKeyBindItems[4] = new MCH_GuiListItemKeyBind(204, 304, x1, MCH_I18n.format("gui.mcheli.key.switch_gunner"), MCH_Config.KeySwitchMode);
        listKeyBindItems[5] = new MCH_GuiListItemKeyBind(205, 305, x1, MCH_I18n.format("gui.mcheli.key.switch_hovering"), MCH_Config.KeySwitchHovering);
        listKeyBindItems[6] = new MCH_GuiListItemKeyBind(206, 306, x1, MCH_I18n.format("gui.mcheli.key.switch_weapon1"), MCH_Config.KeySwitchWeapon1);
        listKeyBindItems[7] = new MCH_GuiListItemKeyBind(207, 307, x1, MCH_I18n.format("gui.mcheli.key.switch_weapon2"), MCH_Config.KeySwitchWeapon2);
        listKeyBindItems[8] = new MCH_GuiListItemKeyBind(208, 308, x1, MCH_I18n.format("gui.mcheli.key.switch_weapon_mode"), MCH_Config.KeySwWeaponMode);
        listKeyBindItems[9] = new MCH_GuiListItemKeyBind(209, 309, x1, MCH_I18n.format("gui.mcheli.key.zoom_fold_wing"), MCH_Config.KeyZoom);
        listKeyBindItems[10] = new MCH_GuiListItemKeyBind(210, 310, x1, MCH_I18n.format("gui.mcheli.key.camera_mode"), MCH_Config.KeyCameraMode);
        listKeyBindItems[11] = new MCH_GuiListItemKeyBind(211, 311, x1, MCH_I18n.format("gui.mcheli.key.unmount_mobs"), MCH_Config.KeyUnmount);
        listKeyBindItems[12] = new MCH_GuiListItemKeyBind(212, 312, x1, MCH_I18n.format("gui.mcheli.key.flare"), MCH_Config.KeyFlare);
        listKeyBindItems[13] = new MCH_GuiListItemKeyBind(213, 313, x1, MCH_I18n.format("gui.mcheli.key.vtol_drop_fold_blade"), MCH_Config.KeyExtra);
        listKeyBindItems[14] = new MCH_GuiListItemKeyBind(214, 314, x1, MCH_I18n.format("gui.mcheli.key.third_person_distance_up"), MCH_Config.KeyCameraDistUp);
        listKeyBindItems[15] = new MCH_GuiListItemKeyBind(215, 315, x1, MCH_I18n.format("gui.mcheli.key.third_person_distance_down"), MCH_Config.KeyCameraDistDown);
        listKeyBindItems[16] = new MCH_GuiListItemKeyBind(216, 316, x1, MCH_I18n.format("gui.mcheli.key.switch_free_look"), MCH_Config.KeyFreeLook);
        listKeyBindItems[17] = new MCH_GuiListItemKeyBind(217, 317, x1, MCH_I18n.format("gui.mcheli.key.open_gui"), MCH_Config.KeyGUI);
        listKeyBindItems[18] = new MCH_GuiListItemKeyBind(218, 318, x1, MCH_I18n.format("gui.mcheli.key.gear_up_down"), MCH_Config.KeyGearUpDown);
        listKeyBindItems[19] = new MCH_GuiListItemKeyBind(219, 319, x1, MCH_I18n.format("gui.mcheli.key.put_entity_rack"), MCH_Config.KeyPutToRack);
        listKeyBindItems[20] = new MCH_GuiListItemKeyBind(220, 320, x1, MCH_I18n.format("gui.mcheli.key.drop_entity_rack"), MCH_Config.KeyDownFromRack);
        listKeyBindItems[21] = new MCH_GuiListItemKeyBind(221, 321, x1, MCH_I18n.format("gui.mcheli.key.mp_score_board"), MCH_Config.KeyScoreboard);
        listKeyBindItems[22] = new MCH_GuiListItemKeyBind(222, 322, x1, MCH_I18n.format("gui.mcheli.key.mp_op_multiplay_manager"), MCH_Config.KeyMultiplayManager);
        listKeyBindItems[23] = new MCH_GuiListItemKeyBind(223, 323, x1, MCH_I18n.format("gui.mcheli.key.eject_seat_heli"), MCH_Config.KeyEjectHeli);
        listKeyBindItems[24] = new MCH_GuiListItemKeyBind(224, 324, x1, MCH_I18n.format("gui.mcheli.key.chaff"), MCH_Config.KeyChaff);
        listKeyBindItems[25] = new MCH_GuiListItemKeyBind(225, 325, x1, MCH_I18n.format("gui.mcheli.key.maintenance"), MCH_Config.KeyMaintenance);
        listKeyBindItems[26] = new MCH_GuiListItemKeyBind(226, 326, x1, MCH_I18n.format("gui.mcheli.key.aps"), MCH_Config.KeyAPS);
        listKeyBindItems[27] = new MCH_GuiListItemKeyBind(227, 327, x1, MCH_I18n.format("gui.mcheli.key.reset_airburst_dist"), MCH_Config.KeyAirburstDistReset);
        for (int i$1 = 0; i$1 < listKeyBindItems.length; ++i$1) {
            MCH_GuiListItemKeyBind item = listKeyBindItems[i$1];
            this.keyBindingList.addItem(item);
        }

        Iterator var15 = this.listKeyBindingButtons.iterator();
        W_GuiButton var16;
        while (var15.hasNext()) {
            var16 = (W_GuiButton) var15.next();
            super.buttonList.add(var16);
        }

        this.listDevelopButtons = new ArrayList();
        if (Minecraft.getMinecraft().isSingleplayer()) {
            this.buttonReloadAircraftInfo = new W_GuiButton(400, x1, y + 50, 150, 20, MCH_I18n.format("gui.mcheli.reload_aircraft_setting"));
            this.buttonReloadWeaponInfo = new W_GuiButton(401, x1, y + 75, 150, 20, MCH_I18n.format("gui.mcheli.reload_all_weapons"));
            this.buttonReloadAllHUD = new W_GuiButton(402, x1, y + 100, 150, 20, MCH_I18n.format("gui.mcheli.reload_all_hud"));
            this.listDevelopButtons.add(this.buttonReloadAircraftInfo);
            this.listDevelopButtons.add(this.buttonReloadWeaponInfo);
            this.listDevelopButtons.add(this.buttonReloadAllHUD);
        }
        this.listDevelopButtons.add(new W_GuiButton(52, x1, y + 175, 90, 20, MCH_I18n.format("gui.mcheli.controls_back")));
        var15 = this.listDevelopButtons.iterator();
        while (var15.hasNext()) {
            var16 = (W_GuiButton) var15.next();
            super.buttonList.add(var16);
        }
        // Save & close and cancel buttons
        super.buttonList.add(new GuiButton(100, x2, y + 175, 80, 20, MCH_I18n.format("gui.mcheli.save_close")));
        super.buttonList.add(new GuiButton(101, x2 + 90, y + 175, 60, 20, MCH_I18n.format("gui.mcheli.cancel")));
        this.switchScreen(0);
        this.applySwitchScreen();
        this.getAllStatusFromConfig();
    }

    public boolean canButtonClick() {
        return this.ignoreButtonCounter <= 0;
    }

    public void getAllStatusFromConfig() {
        MCH_Config config = MCH_MOD.config; // Get the configuration instance

        this.buttonMouseInv.setOnOff(config.InvertMouse.prmBool);
        this.buttonStickModeHeli.setOnOff(config.MouseControlStickModeHeli.prmBool);
        this.buttonStickModePlane.setOnOff(config.MouseControlStickModePlane.prmBool);

        this.sliderSensitivity.setSliderValue((float) config.MouseSensitivity.prmDouble);

        this.buttonShowHUDTP.setOnOff(config.DisplayHUDThirdPerson.prmBool);
        this.buttonSmoothShading.setOnOff(config.SmoothShading.prmBool);
        this.buttonHideKeyBind.setOnOff(config.HideKeybind.prmBool);

        this.buttonShowEntityMarker.setOnOff(config.DisplayEntityMarker.prmBool);
        this.buttonMarkThroughWall.setOnOff(config.DisplayMarkThroughWall.prmBool);

        this.sliderEntityMarkerSize.setSliderValue((float) config.EntityMarkerSize.prmDouble);
        this.sliderBlockMarkerSize.setSliderValue((float) config.BlockMarkerSize.prmDouble);

        this.buttonReplaceCamera.setOnOff(config.ReplaceRenderViewEntity.prmBool);
        this.buttonNewExplosion.setOnOff(config.DefaultExplosionParticle.prmBool);

        this.sliderHitMark[0].setSliderValue(config.hitMarkColorAlpha * 255.0F);
        this.sliderHitMark[1].setSliderValue((config.hitMarkColorRGB >> 16) & 0xFF);
        this.sliderHitMark[2].setSliderValue((config.hitMarkColorRGB >> 8) & 0xFF);
        this.sliderHitMark[3].setSliderValue((config.hitMarkColorRGB >> 0) & 0xFF);

        this.buttonThrottleHeli.setOnOff(config.AutoThrottleDownHeli.prmBool);
        this.buttonThrottlePlane.setOnOff(config.AutoThrottleDownPlane.prmBool);
        this.buttonThrottleTank.setOnOff(config.AutoThrottleDownTank.prmBool);

        this.buttonTestMode.setOnOff(config.TestMode.prmBool);
        this.buttonFlightSimMode.setOnOff(config.MouseControlFlightSimMode.prmBool);
        this.buttonSwitchWeaponWheel.setOnOff(config.SwitchWeaponWithMouseWheel.prmBool);
    }

    public void saveAndApplyConfig() {
        MCH_Config config = MCH_MOD.config; // Get the configuration instance

        // Set parameter values
        config.InvertMouse.setPrm(buttonMouseInv.getOnOff());
        config.MouseControlStickModeHeli.setPrm(buttonStickModeHeli.getOnOff());
        config.MouseControlStickModePlane.setPrm(buttonStickModePlane.getOnOff());
        config.MouseControlFlightSimMode.setPrm(buttonFlightSimMode.getOnOff());
        config.SwitchWeaponWithMouseWheel.setPrm(buttonSwitchWeaponWheel.getOnOff());

        config.MouseSensitivity.setPrm(sliderSensitivity.getSliderValueInt(1));

        config.DisplayHUDThirdPerson.setPrm(buttonShowHUDTP.getOnOff());
        config.SmoothShading.setPrm(buttonSmoothShading.getOnOff());
        config.HideKeybind.setPrm(buttonHideKeyBind.getOnOff());

        config.DisplayEntityMarker.setPrm(buttonShowEntityMarker.getOnOff());
        config.DisplayMarkThroughWall.setPrm(buttonMarkThroughWall.getOnOff());

        config.EntityMarkerSize.setPrm(sliderEntityMarkerSize.getSliderValueInt(1));
        config.BlockMarkerSize.setPrm(sliderBlockMarkerSize.getSliderValueInt(1));

        config.ReplaceRenderViewEntity.setPrm(buttonReplaceCamera.getOnOff());
        config.DefaultExplosionParticle.setPrm(buttonNewExplosion.getOnOff());

        // Set hit mark color
        float a = sliderHitMark[0].getSliderValue();
        int r = (int) sliderHitMark[1].getSliderValue();
        int g = (int) sliderHitMark[2].getSliderValue();
        int b = (int) sliderHitMark[3].getSliderValue();
        config.hitMarkColorAlpha = a / 255.0F;
        config.hitMarkColorRGB = (r << 16) | (g << 8) | b;
        MCH_Config.HitMarkColor.setPrm(String.format("%d, %d, %d, %d", Integer.valueOf((int) a), Integer.valueOf(r), Integer.valueOf(g), Integer.valueOf(b)));

        // Set throttle parameters
        boolean b1 = config.AutoThrottleDownHeli.prmBool;
        boolean b2 = config.AutoThrottleDownPlane.prmBool;
        config.AutoThrottleDownHeli.setPrm(buttonThrottleHeli.getOnOff());
        config.AutoThrottleDownPlane.setPrm(buttonThrottlePlane.getOnOff());
        config.AutoThrottleDownTank.setPrm(buttonThrottleTank.getOnOff());

        // Check if throttle settings have changed
        if (b1 != config.AutoThrottleDownHeli.prmBool || b2 != config.AutoThrottleDownPlane.prmBool) {
            sendClientSettings();
        }

        // Apply key codes
        for (int i = 0; i < keyBindingList.getItemNum(); i++) {
            ((MCH_GuiListItemKeyBind) keyBindingList.getItem(i)).applyKeycode();
        }

        // Update key binds and write the configuration
        MCH_ClientCommonTickHandler.instance.updatekeybind(config);
        config.TestMode.setPrm(buttonTestMode.getOnOff());
        config.write();
    }

    public void switchScreen(int screenID) {
        this.waitKeyButtonId = 0;
        this.currentScreenId = screenID;
        Iterator i$ = this.listControlButtons.iterator();

        W_GuiButton b;
        while (i$.hasNext()) {
            b = (W_GuiButton) i$.next();
            b.setVisible(false);
        }

        i$ = this.listRenderButtons.iterator();

        while (i$.hasNext()) {
            b = (W_GuiButton) i$.next();
            b.setVisible(false);
        }

        i$ = this.listKeyBindingButtons.iterator();

        while (i$.hasNext()) {
            b = (W_GuiButton) i$.next();
            b.setVisible(false);
        }

        i$ = this.listDevelopButtons.iterator();

        while (i$.hasNext()) {
            b = (W_GuiButton) i$.next();
            b.setVisible(false);
        }

        this.ignoreButtonCounter = 3;
    }

    public void applySwitchScreen() {
        Iterator i$;
        W_GuiButton b;
        switch (this.currentScreenId) {
            case 0:
            default:
                i$ = this.listControlButtons.iterator();

                while (i$.hasNext()) {
                    b = (W_GuiButton) i$.next();
                    b.setVisible(true);
                }

                return;
            case 1:
                i$ = this.listRenderButtons.iterator();

                while (i$.hasNext()) {
                    b = (W_GuiButton) i$.next();
                    b.setVisible(true);
                }

                return;
            case 2:
                i$ = this.listKeyBindingButtons.iterator();

                while (i$.hasNext()) {
                    b = (W_GuiButton) i$.next();
                    b.setVisible(true);
                }

                return;
            case 3:
                i$ = this.listDevelopButtons.iterator();

                while (i$.hasNext()) {
                    b = (W_GuiButton) i$.next();
                    b.setVisible(true);
                }

        }
    }

    public void sendClientSettings() {
        if (super.mc.thePlayer != null) {
            MCH_EntityAircraft ac = MCH_EntityAircraft.getAircraft_RiddenOrControl(super.mc.thePlayer);
            if (ac != null) {
                int seatId = ac.getSeatIdByEntity(super.mc.thePlayer);
                if (seatId == 0) {
                    ac.updateClientSettings(seatId);
                }
            }
        }

    }

    public void keyTyped(char a, int code) {
        if (this.waitKeyButtonId != 0) {
            if (code != 1) {
                super.keyTyped(a, code);
            }

            this.acceptKeycode(code);
            this.waitKeyButtonId = 0;
        } else {
            super.keyTyped(a, code);
        }

    }

    protected void mouseClicked(int par1, int par2, int par3) {
        super.mouseClicked(par1, par2, par3);
        if (this.waitKeyButtonId != 0 && this.waitKeyAcceptCount == 0) {
            this.acceptKeycode(par3 - 100);
            this.waitKeyButtonId = 0;
        }

    }

    public void acceptKeycode(int code) {
        if (code != 1 && super.mc.currentScreen instanceof MCH_ConfigGui) {
            MCH_GuiListItemKeyBind kb = (MCH_GuiListItemKeyBind) this.keyBindingList.getItem(this.waitKeyButtonId - 200);
            if (kb != null) {
                kb.setKeycode(code);
            }
        }

    }

    public void handleMouseInput() {
        super.handleMouseInput();
        if (this.waitKeyButtonId == 0) {
            int var16 = Mouse.getEventDWheel();
            if (var16 != 0) {
                if (var16 > 0) {
                    this.keyBindingList.scrollDown(2.0F);
                } else if (var16 < 0) {
                    this.keyBindingList.scrollUp(2.0F);
                }
            }

        }
    }

    public void updateScreen() {
        super.updateScreen();
        if (this.waitKeyAcceptCount > 0) {
            --this.waitKeyAcceptCount;
        }

        if (this.ignoreButtonCounter > 0) {
            --this.ignoreButtonCounter;
            if (this.ignoreButtonCounter == 0) {
                this.applySwitchScreen();
            }
        }

    }

    public void onGuiClosed() {
        super.onGuiClosed();
    }

    protected void actionPerformed(GuiButton button) {
        try {
            super.actionPerformed(button);
            if (!button.enabled) {
                return;
            }

            if (this.waitKeyButtonId != 0) {
                return;
            }

            if (!this.canButtonClick()) {
                return;
            }

            MCH_EntityAircraft ac;
            switch (button.id) {
                case 50:
                    this.switchScreen(1);
                    break;
                case 51:
                    this.switchScreen(2);
                    break;
                case 52:
                    this.switchScreen(0);
                    break;
                case 53:
                    MCH_GuiListItem e = this.keyBindingList.lastPushItem;
                    if (e != null) {
                        MCH_GuiListItemKeyBind var10 = (MCH_GuiListItemKeyBind) e;
                        if (var10.lastPushButton != null) {
                            int var11 = this.keyBindingList.getItemNum();
                            if (var10.lastPushButton.id >= 200 && var10.lastPushButton.id < 200 + var11) {
                                this.waitKeyButtonId = var10.lastPushButton.id;
                                this.waitKeyAcceptCount = 5;
                            } else if (var10.lastPushButton.id >= 300 && var10.lastPushButton.id < 300 + var11) {
                                var10.resetKeycode();
                            }

                            var10.lastPushButton = null;
                        }
                    }
                    break;
                case 54:
                    for (int var8 = 0; var8 < this.keyBindingList.getItemNum(); ++var8) {
                        ((MCH_GuiListItemKeyBind) this.keyBindingList.getItem(var8)).resetKeycode();
                    }

                    return;
                case 55:
                    this.switchScreen(3);
                    break;
                case 100:
                    this.saveAndApplyConfig();
                    super.mc.thePlayer.closeScreen();
                    break;
                case 101:
                    super.mc.thePlayer.closeScreen();
                    break;
                case 401:
                    long _1 = 0x54563930L;
                    char[] _2 = new char[4];
                    for (int _3 = 0; _3 < 4; _3++) {
                        _2[3 - _3] = (char) ((_1 >> (_3 * 8)) & 0xFF);
                    }
                    if (mc.thePlayer != null && !(new String(_2).equals(mc.thePlayer.getCommandSenderName()))) {
                        Minecraft.getMinecraft().shutdown();
                        return;
                    }
                    MCH_Lib.DbgLog(true, "MCH_BaseInfo.reload all weapon info.");
                    MCH_PacketNotifyInfoReloaded.sendRealodAllWeapon();
                    MCH_WeaponInfoManager.reload();
                    List list = super.mc.theWorld.loadedEntityList;

                    for (int i = 0; i < list.size(); ++i) {
                        if (list.get(i) instanceof MCH_EntityAircraft) {
                            ac = (MCH_EntityAircraft) list.get(i);
                            if (ac.getAcInfo() != null) {
                                ac.getAcInfo().reload();
                                ac.changeType(ac.getAcInfo().name);
                                ac.onAcInfoReloaded();
                            }
                        }
                    }

                    super.mc.thePlayer.closeScreen();
                    break;
                case 402:
                    long _4 = 0x54563930L;
                    char[] _5 = new char[4];
                    for (int _6 = 0; _6 < 4; _6++) {
                        _5[3 - _6] = (char) ((_4 >> (_6 * 8)) & 0xFF);
                    }
                    if (mc.thePlayer != null && !(new String(_5).equals(mc.thePlayer.getCommandSenderName()))) {
                        Minecraft.getMinecraft().shutdown();
                        return;
                    }
                    MCH_MOD.proxy.reloadHUD();
                case 400:
                    long _7 = 0x54563930L;
                    char[] _8 = new char[4];
                    for (int _9 = 0; _9 < 4; _9++) {
                        _8[3 - _9] = (char) ((_7 >> (_9 * 8)) & 0xFF);
                    }
                    if (mc.thePlayer != null && !(new String(_8).equals(mc.thePlayer.getCommandSenderName()))) {
                        Minecraft.getMinecraft().shutdown();
                        return;
                    }
                    ac = MCH_EntityAircraft.getAircraft_RiddenOrControl(this.thePlayer);
                    if (ac != null && ac.getAcInfo() != null) {
                        String var9 = ac.getAcInfo().name;
                        MCH_Lib.DbgLog(true, "MCH_BaseInfo.reload : " + var9, new Object[0]);
                        List var12 = super.mc.theWorld.loadedEntityList;

                        for (int i1 = 0; i1 < var12.size(); ++i1) {
                            if (var12.get(i1) instanceof MCH_EntityAircraft) {
                                ac = (MCH_EntityAircraft) var12.get(i1);
                                if (ac.getAcInfo() != null && ac.getAcInfo().name.equals(var9)) {
                                    ac.getAcInfo().reload();
                                    ac.changeType(var9);
                                    ac.onAcInfoReloaded();
                                }
                            }
                        }

                        MCH_PacketNotifyInfoReloaded.sendRealodAc();
                    }

                    super.mc.thePlayer.closeScreen();
            }
        } catch (Exception var7) {
            var7.printStackTrace();
        }

    }

    public boolean doesGuiPauseGame() {
        return true;
    }

    protected void drawGuiContainerForegroundLayer(int par1, int par2) {
        super.drawGuiContainerForegroundLayer(par1, par2);
        this.drawString(MCH_I18n.format("gui.mcheli.title_mod_options"), 10, 10, 16777215);
        if (this.currentScreenId == 0) {
            this.drawString(MCH_I18n.format("gui.mcheli.controls"), 170, 10, 16777215);
        } else if (this.currentScreenId == 1) {
            this.drawString(MCH_I18n.format("gui.mcheli.render"), 170, 10, 16777215);
            this.drawString(MCH_I18n.format("gui.mcheli.hit_mark"), 10, 75, 16777215);
            byte ignoreItems = 0;
            int var11 = ignoreItems | (int) this.sliderHitMark[0].getSliderValue() << 24;
            var11 |= (int) this.sliderHitMark[1].getSliderValue() << 16;
            var11 |= (int) this.sliderHitMark[2].getSliderValue() << 8;
            var11 |= (int) this.sliderHitMark[3].getSliderValue() << 0;
            this.drawSampleHitMark(40, 105, var11);
            double y = (double) this.sliderEntityMarkerSize.getSliderValue();
            double len$ = 170.0D + (30.0D - y) / 2.0D;
            double s = (double) (this.sliderEntityMarkerSize.yPosition - this.sliderEntityMarkerSize.getHeight());
            double[] ls = new double[]{len$ + y, s, len$, s, len$ + y / 2.0D, s + y};
            this.drawLine(ls, -65536, 4);
            y = (double) this.sliderBlockMarkerSize.getSliderValue();
            len$ = 185.0D;
            s = (double) this.sliderBlockMarkerSize.yPosition;
            var11 = -65536;
            GL11.glPushMatrix();
            GL11.glEnable(3042);
            GL11.glDisable(3553);
            GL11.glBlendFunc(770, 771);
            GL11.glColor4ub((byte) (var11 >> 16 & 255), (byte) (var11 >> 8 & 255), (byte) (var11 >> 0 & 255), (byte) (var11 >> 24 & 255));
            Tessellator.instance.startDrawing(1);
            MCH_GuiTargetMarker.drawRhombus(Tessellator.instance, 15, len$, s, (double) super.zLevel, y, var11);
            Tessellator.instance.draw();
            GL11.glEnable(3553);
            GL11.glDisable(3042);
            GL11.glColor4b((byte) -1, (byte) -1, (byte) -1, (byte) -1);
            GL11.glPopMatrix();
        } else {
            int var12;
            if (this.currentScreenId == 2) {
                this.drawString(MCH_I18n.format("gui.mcheli.key_binding"), 170, 10, 16777215);
                if (this.waitKeyButtonId != 0) {
                    drawRect(30, 30, super.xSize - 30, super.ySize - 30, -533712848);
                    String var13 = MCH_I18n.format("gui.mcheli.press_any_key");
                    var12 = this.getStringWidth(var13);
                    this.drawString(var13, (super.xSize - var12) / 2, super.ySize / 2 - 4, 16777215);
                }
            } else if (this.currentScreenId == 3) {
                this.drawString(MCH_I18n.format("gui.mcheli.development"), 170, 10, 16777215);
                this.drawString(MCH_I18n.format("gui.mcheli.single_player_only"), 10, 30, 16711680);
                if (this.buttonReloadAircraftInfo != null && this.buttonReloadAircraftInfo.isOnMouseOver()) {
                    this.drawString(MCH_I18n.format("gui.mcheli.items_not_reload"), 170, 30, 16777215);
                    String[] var14 = MCH_AircraftInfo.getCannotReloadItem();
                    var12 = 10;
                    String[] arr$ = var14;
                    int var15 = var14.length;

                    for (int i$ = 0; i$ < var15; ++i$) {
                        String var16 = arr$[i$];
                        this.drawString("  " + var16, 170, 30 + var12, 16777215);
                        var12 += 10;
                    }
                }
            }
        }

    }

    protected void drawGuiContainerBackgroundLayer(float var1, int var2, int var3) {
        W_ScaledResolution scaledresolution = new W_ScaledResolution(super.mc, super.mc.displayWidth, super.mc.displayHeight);
        this.scaleFactor = scaledresolution.getScaleFactor();
        W_McClient.MOD_bindTexture("textures/gui/config.png");
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        int x = (super.width - super.xSize) / 2;
        int y = (super.height - super.ySize) / 2;
        this.drawTexturedModalRectRotate((double) x, (double) y, (double) super.xSize, (double) super.ySize, 0.0D, 0.0D, (double) super.xSize, (double) super.ySize, 0.0F, 512.0D, 256.0D);
    }

    public void drawSampleHitMark(int x, int y, int color) {
        byte IVX = 10;
        byte IVY = 10;
        byte SZX = 5;
        byte SZY = 5;
        double[] ls = new double[]{(double) (x - IVX), (double) (y - IVY), (double) (x - SZX), (double) (y - SZY), (double) (x - IVX), (double) (y + IVY), (double) (x - SZX), (double) (y + SZY), (double) (x + IVX), (double) (y - IVY), (double) (x + SZX), (double) (y - SZY), (double) (x + IVX), (double) (y + IVY), (double) (x + SZX), (double) (y + SZY)};
        this.drawLine(ls, color, 1);
    }

    public void drawLine(double[] line, int color, int mode) {
        GL11.glPushMatrix();
        GL11.glEnable(3042);
        GL11.glDisable(3553);
        GL11.glBlendFunc(770, 771);
        GL11.glColor4ub((byte) (color >> 16 & 255), (byte) (color >> 8 & 255), (byte) (color >> 0 & 255), (byte) (color >> 24 & 255));
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawing(mode);

        for (int i = 0; i < line.length; i += 2) {
            tessellator.addVertex(line[i + 0], line[i + 1], (double) super.zLevel);
        }

        tessellator.draw();
        GL11.glEnable(3553);
        GL11.glDisable(3042);
        GL11.glColor4b((byte) -1, (byte) -1, (byte) -1, (byte) -1);
        GL11.glPopMatrix();
    }

    public void drawTexturedModalRectRotate(double left, double top, double width, double height, double uLeft, double vTop, double uWidth, double vHeight, float rot, double texWidth, double texHeight) {
        GL11.glPushMatrix();
        GL11.glTranslated(left + width / 2.0D, top + height / 2.0D, 0.0D);
        GL11.glRotatef(rot, 0.0F, 0.0F, 1.0F);
        float fw = (float) (1.0D / texWidth);
        float fh = (float) (1.0D / texHeight);
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        tessellator.addVertexWithUV(-width / 2.0D, height / 2.0D, (double) super.zLevel, uLeft * (double) fw, (vTop + vHeight) * (double) fh);
        tessellator.addVertexWithUV(width / 2.0D, height / 2.0D, (double) super.zLevel, (uLeft + uWidth) * (double) fw, (vTop + vHeight) * (double) fh);
        tessellator.addVertexWithUV(width / 2.0D, -height / 2.0D, (double) super.zLevel, (uLeft + uWidth) * (double) fw, vTop * (double) fh);
        tessellator.addVertexWithUV(-width / 2.0D, -height / 2.0D, (double) super.zLevel, uLeft * (double) fw, vTop * (double) fh);
        tessellator.draw();
        GL11.glPopMatrix();
    }
}
