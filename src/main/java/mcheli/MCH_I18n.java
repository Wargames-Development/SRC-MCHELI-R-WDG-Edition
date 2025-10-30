package mcheli;

import mcheli.wrapper.W_LanguageRegistry;
import net.minecraft.client.resources.I18n;

public class MCH_I18n {

    public static void register() {
        addTranslation("message.mcheli.overpressure", "en_US", "Overpressure");
        addTranslation("message.mcheli.overpressure", "zh_CN", "超压");

        addTranslation("gui.mcheli.title_mod_options", "en_US", "MCH-Reforged MOD Options");
        addTranslation("gui.mcheli.title_mod_options", "zh_CN", "MCH-Reforged 模组选项");

        addTranslation("gui.mcheli.controls", "en_US", "< Controls >");
        addTranslation("gui.mcheli.controls", "zh_CN", "< 控制 >");

        addTranslation("gui.mcheli.render", "en_US", "< Render >");
        addTranslation("gui.mcheli.render", "zh_CN", "< 渲染 >");

        addTranslation("gui.mcheli.key_binding", "en_US", "< Key Binding >");
        addTranslation("gui.mcheli.key_binding", "zh_CN", "< 键位 >");

        addTranslation("gui.mcheli.development", "en_US", "< Development >");
        addTranslation("gui.mcheli.development", "zh_CN", "< 开发 >");

        addTranslation("gui.mcheli.hit_mark", "en_US", "Hit Mark");
        addTranslation("gui.mcheli.hit_mark", "zh_CN", "命中标记");

        addTranslation("gui.mcheli.single_player_only", "en_US", "Single player only!");
        addTranslation("gui.mcheli.single_player_only", "zh_CN", "仅限单人游戏!");

        addTranslation("gui.mcheli.items_not_reload", "en_US", "The following items are not reload.");
        addTranslation("gui.mcheli.items_not_reload", "zh_CN", "以下字段将不会重新加载 : ");

        addTranslation("gui.mcheli.press_any_key", "en_US", "Press any key or mouse button.");
        addTranslation("gui.mcheli.press_any_key", "zh_CN", "按任意键或鼠标按钮。");

        addTranslation("gui.mcheli.render_settings", "en_US", "Render Settings >>");
        addTranslation("gui.mcheli.render_settings", "zh_CN", "渲染设置 >>");

        addTranslation("gui.mcheli.key_binding_forward", "en_US", "Key Binding >>");
        addTranslation("gui.mcheli.key_binding_forward", "zh_CN", "键位绑定 >>");

        addTranslation("gui.mcheli.development_forward", "en_US", "Development >>");
        addTranslation("gui.mcheli.development_forward", "zh_CN", "开发选项 >>");

        addTranslation("gui.mcheli.controls_back", "en_US", "Controls <<");
        addTranslation("gui.mcheli.controls_back", "zh_CN", "返回控制 <<");

        addTranslation("gui.mcheli.save_close", "en_US", "Save & Close");
        addTranslation("gui.mcheli.save_close", "zh_CN", "保存并关闭");

        addTranslation("gui.mcheli.cancel", "en_US", "Cancel");
        addTranslation("gui.mcheli.cancel", "zh_CN", "取消");

        addTranslation("gui.mcheli.reset_all", "en_US", "Reset All");
        addTranslation("gui.mcheli.reset_all", "zh_CN", "全部重置");

        addTranslation("gui.mcheli.invert_mouse", "en_US", "Invert Mouse : ");
        addTranslation("gui.mcheli.invert_mouse", "zh_CN", "反转鼠标 : ");

        addTranslation("gui.mcheli.sensitivity.format", "en_US", "Sensitivity : %.1f");
        addTranslation("gui.mcheli.sensitivity.format", "zh_CN", "灵敏度 : %.1f");

        addTranslation("gui.mcheli.mouse_flight_sim_mode", "en_US", "Mouse Flight Sim Mode : ");
        addTranslation("gui.mcheli.mouse_flight_sim_mode", "zh_CN", "鼠标飞行模拟模式 : ");

        addTranslation("gui.mcheli.switch_weapon_wheel", "en_US", "Switch Weapon Wheel : ");
        addTranslation("gui.mcheli.switch_weapon_wheel", "zh_CN", "鼠标滚轮切换武器 : ");

        addTranslation("gui.mcheli.test_mode", "en_US", "Test Mode : ");
        addTranslation("gui.mcheli.test_mode", "zh_CN", "测试模式 : ");

        addTranslation("gui.mcheli.stick_mode_heli", "en_US", "Stick Mode Heli : ");
        addTranslation("gui.mcheli.stick_mode_heli", "zh_CN", "直升机操纵杆模式 : ");

        addTranslation("gui.mcheli.stick_mode_plane", "en_US", "Stick Mode Plane : ");
        addTranslation("gui.mcheli.stick_mode_plane", "zh_CN", "飞机操纵杆模式 : ");

        addTranslation("gui.mcheli.throttle_down_heli", "en_US", "Throttle Down Heli : ");
        addTranslation("gui.mcheli.throttle_down_heli", "zh_CN", "直升机自动降油门 : ");

        addTranslation("gui.mcheli.throttle_down_plane", "en_US", "Throttle Down Plane : ");
        addTranslation("gui.mcheli.throttle_down_plane", "zh_CN", "飞机自动降油门 : ");

        addTranslation("gui.mcheli.throttle_down_tank", "en_US", "Throttle Down Tank : ");
        addTranslation("gui.mcheli.throttle_down_tank", "zh_CN", "坦克自动降油门 : ");

        addTranslation("gui.mcheli.flight_sim_mode", "en_US", "Mouse Flight Sim Mode : ");
        addTranslation("gui.mcheli.flight_sim_mode", "zh_CN", "鼠标飞行模拟模式 : ");

        addTranslation("gui.mcheli.switch_weapon_wheel", "en_US", "Switch Weapon Wheel : ");
        addTranslation("gui.mcheli.switch_weapon_wheel", "zh_CN", "鼠标滚轮切换武器 : ");

        addTranslation("gui.mcheli.show_hud_third_person", "en_US", "Show HUD Third Person : ");
        addTranslation("gui.mcheli.show_hud_third_person", "zh_CN", "第三人称显示HUD : ");

        addTranslation("gui.mcheli.hide_key_binding", "en_US", "Hide Key Binding : ");
        addTranslation("gui.mcheli.hide_key_binding", "zh_CN", "隐藏按键绑定 : ");

        addTranslation("gui.mcheli.smooth_shading", "en_US", "Smooth Shading : ");
        addTranslation("gui.mcheli.smooth_shading", "zh_CN", "平滑着色 : ");

        addTranslation("gui.mcheli.show_entity_marker", "en_US", "Show Entity Marker : ");
        addTranslation("gui.mcheli.show_entity_marker", "zh_CN", "显示实体标记 : ");

        addTranslation("gui.mcheli.replace_camera", "en_US", "Change Camera Pos : ");
        addTranslation("gui.mcheli.replace_camera", "zh_CN", "更改摄像机位置 : ");

        addTranslation("gui.mcheli.new_explosion", "en_US", "Default Explosion : ");
        addTranslation("gui.mcheli.new_explosion", "zh_CN", "默认爆炸效果 : ");

        addTranslation("gui.mcheli.entity_marker_size.format", "en_US", "Entity Marker Size:%.0f");
        addTranslation("gui.mcheli.entity_marker_size.format", "zh_CN", "实体标记大小:%.0f");

        addTranslation("gui.mcheli.block_marker_size.format", "en_US", "Block Marker Size:%.0f");
        addTranslation("gui.mcheli.block_marker_size.format", "zh_CN", "方块标记大小:%.0f");

        addTranslation("gui.mcheli.mark_through_wall", "en_US", "Mark Through Wall : ");
        addTranslation("gui.mcheli.mark_through_wall", "zh_CN", "穿墙标记 : ");

        addTranslation("gui.mcheli.alpha.format", "en_US", "Alpha:%.0f");
        addTranslation("gui.mcheli.alpha.format", "zh_CN", "透明度:%.0f");

        addTranslation("gui.mcheli.red.format", "en_US", "Red:%.0f");
        addTranslation("gui.mcheli.red.format", "zh_CN", "红色:%.0f");

        addTranslation("gui.mcheli.green.format", "en_US", "Green:%.0f");
        addTranslation("gui.mcheli.green.format", "zh_CN", "绿色:%.0f");

        addTranslation("gui.mcheli.blue.format", "en_US", "Blue:%.0f");
        addTranslation("gui.mcheli.blue.format", "zh_CN", "蓝色:%.0f");

        addTranslation("gui.mcheli.reload_aircraft_setting", "en_US", "Reload aircraft setting");
        addTranslation("gui.mcheli.reload_aircraft_setting", "zh_CN", "重新加载所有载具");

        addTranslation("gui.mcheli.reload_all_weapons", "en_US", "Reload All Weapons");
        addTranslation("gui.mcheli.reload_all_weapons", "zh_CN", "重新加载所有武器");

        addTranslation("gui.mcheli.reload_all_hud", "en_US", "Reload All HUD");
        addTranslation("gui.mcheli.reload_all_hud", "zh_CN", "重新加载所有HUD");

        addTranslation("gui.mcheli.reload", "en_US", "Reload");
        addTranslation("gui.mcheli.reload", "zh_CN", "装填弹药");

        addTranslation("gui.mcheli.inventory", "en_US", "Inventory");
        addTranslation("gui.mcheli.inventory", "zh_CN", "储物箱");

        addTranslation("gui.mcheli.mod_options", "en_US", "MOD Options");
        addTranslation("gui.mcheli.mod_options", "zh_CN", "模组设置");

        addTranslation("gui.mcheli.close", "en_US", "Close");
        addTranslation("gui.mcheli.close", "zh_CN", "关闭");

        addTranslation("gui.mcheli.parachute", "en_US", "Parachute");
        addTranslation("gui.mcheli.parachute", "zh_CN", "降落伞");

        addTranslation("gui.mcheli.uav_connect", "en_US", "Connect UAV");
        addTranslation("gui.mcheli.uav_connect", "zh_CN", "连线");

        addTranslation("gui.mcheli.uav_not", "en_US", "Not UAV");
        addTranslation("gui.mcheli.uav_not", "zh_CN", "非无人机");

        addTranslation("gui.mcheli.uav_station", "en_US", "UAV Station");
        addTranslation("gui.mcheli.uav_station", "zh_CN", "无人机控制站");

        addTranslation("gui.mcheli.uav_small_only", "en_US", "Small UAV only");
        addTranslation("gui.mcheli.uav_small_only", "zh_CN", "仅限小型无人机");

        addTranslation("gui.mcheli.uav_controller", "en_US", "UAV Controller");
        addTranslation("gui.mcheli.uav_controller", "zh_CN", "无人机控制台");


        addTranslation("gui.mcheli.key.up", "en_US", "Up");
        addTranslation("gui.mcheli.key.up", "zh_CN", "向上");

        addTranslation("gui.mcheli.key.down", "en_US", "Down");
        addTranslation("gui.mcheli.key.down", "zh_CN", "向下");

        addTranslation("gui.mcheli.key.right", "en_US", "Right");
        addTranslation("gui.mcheli.key.right", "zh_CN", "向右");

        addTranslation("gui.mcheli.key.left", "en_US", "Left");
        addTranslation("gui.mcheli.key.left", "zh_CN", "向左");

        addTranslation("gui.mcheli.key.switch_gunner", "en_US", "Switch Gunner");
        addTranslation("gui.mcheli.key.switch_gunner", "zh_CN", "切换炮手模式");

        addTranslation("gui.mcheli.key.switch_hovering", "en_US", "Switch Hovering");
        addTranslation("gui.mcheli.key.switch_hovering", "zh_CN", "切换悬停模式");

        addTranslation("gui.mcheli.key.switch_weapon1", "en_US", "Switch Weapon1");
        addTranslation("gui.mcheli.key.switch_weapon1", "zh_CN", "切换武器1");

        addTranslation("gui.mcheli.key.switch_weapon2", "en_US", "Switch Weapon2");
        addTranslation("gui.mcheli.key.switch_weapon2", "zh_CN", "切换武器2");

        addTranslation("gui.mcheli.key.switch_weapon_mode", "en_US", "Switch Weapon Mode");
        addTranslation("gui.mcheli.key.switch_weapon_mode", "zh_CN", "切换武器模式");

        addTranslation("gui.mcheli.key.zoom_fold_wing", "en_US", "Zoom / Fold Wing");
        addTranslation("gui.mcheli.key.zoom_fold_wing", "zh_CN", "缩放炮镜/折叠机翼");

        addTranslation("gui.mcheli.key.camera_mode", "en_US", "Camera Mode");
        addTranslation("gui.mcheli.key.camera_mode", "zh_CN", "切换炮镜成像通道");

        addTranslation("gui.mcheli.key.unmount_mobs", "en_US", "Unmount Mobs");
        addTranslation("gui.mcheli.key.unmount_mobs", "zh_CN", "卸载生物");

        addTranslation("gui.mcheli.key.flare", "en_US", "Flare");
        addTranslation("gui.mcheli.key.flare", "zh_CN", "热诱弹");

        addTranslation("gui.mcheli.key.vtol_drop_fold_blade", "en_US", "Vtol / Drop / Fold Blade");
        addTranslation("gui.mcheli.key.vtol_drop_fold_blade", "zh_CN", "垂直起降/丢弃/折叠桨叶");

        addTranslation("gui.mcheli.key.third_person_distance_up", "en_US", "Third Person Distance Up");
        addTranslation("gui.mcheli.key.third_person_distance_up", "zh_CN", "增大第三人称视距");

        addTranslation("gui.mcheli.key.third_person_distance_down", "en_US", "Third Person Distance Down");
        addTranslation("gui.mcheli.key.third_person_distance_down", "zh_CN", "减小第三人称视距");

        addTranslation("gui.mcheli.key.switch_free_look", "en_US", "Switch Free Look");
        addTranslation("gui.mcheli.key.switch_free_look", "zh_CN", "切换自由视角");

        addTranslation("gui.mcheli.key.open_gui", "en_US", "Open GUI");
        addTranslation("gui.mcheli.key.open_gui", "zh_CN", "打开GUI");

        addTranslation("gui.mcheli.key.gear_up_down", "en_US", "Gear Up Down");
        addTranslation("gui.mcheli.key.gear_up_down", "zh_CN", "收放起落架");

        addTranslation("gui.mcheli.key.put_entity_rack", "en_US", "Put entity in the rack");
        addTranslation("gui.mcheli.key.put_entity_rack", "zh_CN", "放实体到架子");

        addTranslation("gui.mcheli.key.drop_entity_rack", "en_US", "Drop entity from the rack");
        addTranslation("gui.mcheli.key.drop_entity_rack", "zh_CN", "从架子丢实体");

        addTranslation("gui.mcheli.key.mp_score_board", "en_US", "[MP]Score board");
        addTranslation("gui.mcheli.key.mp_score_board", "zh_CN", "[多人]记分板");

        addTranslation("gui.mcheli.key.mp_op_multiplay_manager", "en_US", "[MP][OP]Multiplay manager");
        addTranslation("gui.mcheli.key.mp_op_multiplay_manager", "zh_CN", "[多人][管理员]多人管理");

        addTranslation("gui.mcheli.key.eject_seat_heli", "en_US", "Eject Seat");
        addTranslation("gui.mcheli.key.eject_seat_heli", "zh_CN", "弹射座椅");

        addTranslation("gui.mcheli.key.chaff", "en_US", "Chaff");
        addTranslation("gui.mcheli.key.chaff", "zh_CN", "干扰箔");

        addTranslation("gui.mcheli.key.maintenance", "en_US", "Maintenance");
        addTranslation("gui.mcheli.key.maintenance", "zh_CN", "快速维修");

        addTranslation("gui.mcheli.key.aps", "en_US", "APS");
        addTranslation("gui.mcheli.key.aps", "zh_CN", "主动防御系统");

        addTranslation("gui.mcheli.key.reset_airburst_dist", "en_US", "Reset Airburst Dist");
        addTranslation("gui.mcheli.key.reset_airburst_dist", "zh_CN", "重置空爆距离");

        addTranslation("gui.mcheli.key.key_reset", "en_US", "Reset");
        addTranslation("gui.mcheli.key.key_reset", "zh_CN", "重置按键");

        addTranslation("gui.mcheli.key.key_on", "en_US", "ON");
        addTranslation("gui.mcheli.key.key_on", "zh_CN", "开启");

        addTranslation("gui.mcheli.key.key_off", "en_US", "OFF");
        addTranslation("gui.mcheli.key.key_off", "zh_CN", "关闭");
    }

    private static void addTranslation(Object key, String lang, String value) {
        W_LanguageRegistry.addNameForObject(key, lang, value);
    }

    public static String format(String p_135052_0_, Object... p_135052_1_) {
        String s = I18n.format(p_135052_0_, p_135052_1_);
        if (s.startsWith("Format error: ")) {
            return s.substring(14);
        }
        return s;
    }
}
