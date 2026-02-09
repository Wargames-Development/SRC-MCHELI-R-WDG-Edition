文档:
2016/03/28

;***********************************************************************************
;■HUD设定文件 hud/***.txt
;***********************************************************************************

; ★ 重要 ★
; HUD可以在不关闭Minecraft的情况下重新加载。
; [ 乘坐载具 → R键打开补给界面 → MOD Option → Development → Reload All HUD ]
; 纹理的重新加载不使用直升机MOD的功能，而是使用Minecraft的默认功能。
; [ Esc键打开游戏菜单 → 设置 → 资源包 → 完成 ]


;************************************************************************************************************
; 仅HUD的设定文件顺序有影响。
; 按照设定文件中记载的顺序执行处理，该顺序即为绘制顺序。

;************************************************************************************************************
; 关于计算公式
; ■ 除了DrawString, DrawCenteredString的数据部分，其他数值都可以设置计算公式。
;   例：Color = 123 + 456
; ■ 公式中可以设置16进制数。在数值前加上 # 或 0x。
;   例：Color = #123 + 0x456 + 789
; ■ 公式中可以设置“变量”。
;   例：Color = hp_rto * 100
;       hp_rto 表示载具的耐久值，范围0.0～1.0。因此 * 100 后变为 0.0～100.0。
; ■ 公式中可以设置条件。
;   格式： 条件 ? 条件成立时的值 : 条件不成立时的值
;   例：Color = hp_rto>0.2? 0xFFFFFFFF: 0x00000000
;       当 hp_rto 大于 0.2 时，Color 设置为 0xFFFFFFFF，小于等于 0.2 时设置为 0x00000000
;
; “变量一览”
; center_x    : 画面中央的坐标X
; center_y    : 画面中央的坐标Y
; width       : 画面的宽度
; height      : 画面的高度
; yaw         : 载具的偏航角[横向旋转](-180.0～180.0)
; pitch       : 载具的俯仰角[纵向旋转](-90.0～90.0)
; roll        : 载具的滚转角(-180.0～180.0)
; plyr_yaw    : 玩家的偏航角[横向旋转](-180.0～180.0)
; plyr_pitch  : 玩家的俯仰角[纵向旋转](-90.0～90.0)
; altitude    : 载具的高度(0～) 载具到下方方块的距离(不会为负数)
; sea_alt     : 载具的高度(0～) 海拔。距离基准海平面的高度(不会为负数)
; hp          : 载具的剩余耐久值(0～)
; max_hp      : 载具的最大耐久值(0～)
; hp_rto      : 载具的剩余HP比例(0.0～1.0)
; throttle    : 载具的当前油门(0.0～1.0)
; pos_x       : 载具的坐标X
; pos_y       : 载具的坐标Y
; pos_z       : 载具的坐标Z
; motion_x    : 载具的加速度X
; motion_y    : 载具的加速度Y
; motion_z    : 载具的加速度Z
; fuel        : 载具的剩余燃料比例(0.0～1.0)
; low_fuel    : 燃料不足警告显示。不足时在0和1之间闪烁( 0=燃料充足、1=燃料不足 )
; stick_x     : 操纵杆的X方向量(-1.0～1.0)
; stick_y     : 操纵杆的Y方向量(-1.0～1.0)
; reloading   : 武器的重新装填状态(0=未在装填。1=装填中)
; reload_time : 武器的剩余装填时间比例(0.0～1.0) 0.0表示装填完成
; wpn_heat    : 武器的热量(0.0～1.0)
; is_heat_wpn : 是否是热量式武器(0=非热量式。1=热量式)
; dsp_mt_dist : 是否显示落弹距离(0=不显示。1=显示)
; mt_dist     : 落弹距离(小于0.0表示无法计算)
; have_radar  : 是否装备雷达(0=无雷达。1=有雷达)
; radar_rot   : 雷达的旋转角度。旋转一周后实体位置更新
; vtol_stat   : 仅固定翼飞机。0=通常、1=VTOL切换中、2=VTOL模式中
; free_look   : 0=通常、1=自由视角中
; cam_mode    : 0=通常、1=夜视模式中、2=热成像模式
; cam_zoom    : 相机倍率。1.0 ~ 10.0
; auto_pilot  : 0=通常、1=自动驾驶中
; have_flare  : 是否装备闪光弹发射装置(0=无。1=有)
; can_flare   : 是否可以使用闪光弹(0=可使用。1=准备中，不可使用)
; sight_type  : 瞄准镜类型( 0=无、1=移动瞄准镜、2=导弹瞄准镜 )
; lock        : 导弹的锁定状态(0.0～1.0)
; color       : 当前的颜色设置 (0x00000000~0xFFFFFFFF)
; inventory   : 物品栏的槽位数(0～54)
; hovering    : 悬停状态( 0=未悬停、1=悬停中 )
; is_uav      : 是否是UAV( 0=不是UAV、1=是UAV )
; uav_fs      : UAV的信号强度。值越大离UAV基站越近( 0.0 ~ 1.0 )
; gunner_mode : 是否是炮手模式( 0=通常模式、1=炮手模式 )
; time        : 游戏内时间 1天为24000计数(0～24000)、0=上午6点、6000=正午
; test_mode   : 是否是测试模式( 0=通常模式、1=测试模式 )


;************************************************************************************************************
; 调用其他HUD绘制文件
; 指定HUD的文本文件名，可以执行该设定文件的绘制处理。
;
; Call = common_pilot 的情况下，会执行 common_pilot.txt
; 被调用的设定文件内可以再调用其他文件，但不能双重调用同一个文件。
; 例如，在 heli.txt 中调用 Call = heli 的情况下，由于 heli.txt 已经在执行中，因此会被忽略。
; 另外，在 heli.txt 中调用 Call = plane，并在 plane.txt 中调用 Call = heli 的情况下，
; 由于 heli.txt 已在执行中，同样不会被执行。
Call = common_pilot


;************************************************************************************************************
; 结束当前HUD文件的绘制
;
; 如果是由 Call 调用的，则返回到调用源
; 例如从 heli.txt 调用 uav_fs.txt，并在 uav_fs.txt 中使用Exit，则 uav_fs 的绘制结束，
; 处理会从 heli.txt 中调用 uav_fs.txt 的下一行继续。
;
Exit


;************************************************************************************************************
; 条件分支
; 仅在满足指定条件时执行处理
;
; 条件不为 0 时，会执行 处理1 处理2 处理3
; 条件为 0 时，不会执行 处理1 处理2 处理3
; If = 条件
; 	处理1
; 	处理2
; 	处理3
; EndIf
; ※不支持嵌套 (不能在If～EndIf 之间再放入If)
;
; 下面的例子中，如果 is_heat_wpn 等于1，则执行 Color 和 DrawRect。
If = is_heat_wpn==1
	Color = 0xFF28d448
	DrawRect = -145, 57, 43, 10
EndIf


;************************************************************************************************************
; 颜色设置
; 进行颜色设置后，之后的文字颜色和线条颜色将以此设置的颜色进行绘制
;
; 有两种设置方法。
; 一种是用1个参数指定，另一种是用4个参数指定。
; 1个参数的情况: 从高位到低位 不透明度、红、绿、蓝
;                  :例) 0xAABBCCDD 的情况下，不透明度=AA、红=BB、绿=CC、蓝=DD
; 4个参数的情况: 按顺序 不透明度、红、绿、蓝
;                  :例) 12, 34, 56, 78  的情况下，不透明度=12、红=34、绿=56、蓝=78
; 可以用16进制指定，或者用10进制指定4个值
; 例 : 以下均为有效设置
Color = 0xFFFFFFFF
Color = #FFFFFFFF
Color = 0xFF,  40, 212,  72


;************************************************************************************************************
; 绘制字符串
;  DrawString         : 从指定坐标开始向右绘制
;  DrawCenteredString : 以指定坐标为中心进行绘制
;
; 参数1 = 距离画面中央的X坐标
; 参数2 = 距离画面中央的Y坐标
; 参数3 = 显示格式(printf or String.format的形式。不能使用 , )
;               %的数量必须与参数4以后的数据数量一致
; 参数4以后 = ★数据(可省略。无上限)
;
; 例(高度)：DrawString = -20,  20, "Hello"
;           在画面上显示 Hello 这个字符串
;
; 例(高度)：DrawString =  0,  40, "[ %3d ]", ALTITUDE
;           以3位整数显示 ALTITUDE(高度) → [ 123 ]
;
; 例(日期)：DrawString =  0, -30, "%tY/%tm/%td", DATE, DATE, DATE
;           用 / 分隔显示 DATE(日期) → 2014/10/11
;
; ★数据(不区分大小写)
; 数据部分不能使用计算公式
;
; 如下所示，%s、%d、%f 对每个数据是固定的。但是，可以在%和字母之间加入表示位数的数值。
; 例如 %f 不指定小数位数，所以不知道会显示多少位。 → 123.45678 等
; 使用 %.2f 则小数点后固定为 2 位。 %.0f → 123    %.1f → 123.4    %.2f → 123.45
; 这在 %d 中也可以。详细请搜索 printf。
;
;  NAME        : %s : 载具名(显示与物品名相同)
;  ALTITUDE    : %d : 以整数表示高度。
;  DATE        : %tY %tm %td %td %tH %tM %tS ...
;              : 表示当前日期时间。%tY=年、%tm=月、%td=日、%tH=时、%tM=分、%tS=秒
;  MC_THOR     : %d : 表示游戏内时间的小时。0～23。注意游戏内时间是现实的72倍
;  MC_TMIN     : %d : 表示游戏内时间的分钟。0～59。注意游戏内时间是现实的72倍
;  MC_TSEC     : %d : 表示游戏内时间的秒。0～59。注意游戏内时间是现实的72倍
;  MAX_HP      : %d : 以整数表示最大耐久值。
;  HP          : %d : 以整数表示剩余耐久值。
;  HP_PER      : %f : 以小数(100.0～0.0)表示剩余比例。%.1f 表示显示小数点后1位。
;  THROTTLE    : %f : 以小数(100.0～0.0)表示油门。%.1f 表示显示小数点后1位。
;  POS_X       : %f : 以小数显示载具的X坐标。
;  POS_Y       : %f : 以小数显示载具的Y坐标。
;  POS_Z       : %f : 以小数显示载具的Z坐标。
;  MOTION_X    : %f : 以小数显示载具的X方向加速度。
;  MOTION_Y    : %f : 以小数显示载具的Y方向加速度。
;  MOTION_Z    : %f : 以小数显示载具的Z方向加速度。
;  YAW         : %f : 以小数显示载具的横向角度。
;  PITCH       : %f : 以小数显示载具的纵向角度。★注意与实际俯仰角正负相反。(上为正，下为负)
;  ROLL        : %f : 以小数显示载具的滚转角度。
;  PLYR_YAW    : %f : 以小数显示玩家的横向角度。
;  PLYR_PITCH  : %f : 以小数显示玩家的纵向角度。★注意与实际俯仰角正负相反。(上为正，下为负)
;  INVENTORY   : %d : 显示载具的物品栏数量。
;  WPN_NAME    : %s : 显示当前选择的武器名。
;  WPN_AMMO    : %s : 显示当前选择武器的弹药数。
;  WPN_RM_AMMO : %s : 显示当前选择武器的剩余弹药。
;  RELOAD_PER  : %f : 以小数(100.0～0.0)表示重新装填状况。100.0表示装填完成。%.1f 表示显示小数点后1位。
;  RELOAD_SEC  : %f : 以小数(0.0～)表示剩余装填秒数。0.0表示装填完成。%.1f 表示显示小数点后1位。
;  MORTAR_DIST : %f : 落弹距离(小于0.0表示无法计算)
;  MC_VER      : %s : Minecraft的版本。
;  MOD_VER     : %s : MOD的版本。
;  MOD_NAME    : %s : MOD的名称。固定为「MC Helicopter MOD」。
;  TVM_POS_X   : %f : TV导弹的X坐标
;  TVM_POS_Y   : %f : TV导弹的Y坐标
;  TVM_POS_Z   : %f : TV导弹的Z坐标
;  TVM_DIFF    : %f : TV导弹与载具的距离
;  CAM_ZOOM    : %f : 相机的倍率
;  UAV_DIST    : %f : 与UAV基站的距离 (0.0～)
;  KEY_GUI     : %s : GUI 键的名称 (默认键位设置下是 R)
;
DrawCenteredString = 0,  40, "[ %3d ]", ALTITUDE
→ 如果ALTITUDE是12，画面上会显示 [  12 ]。虽然指定了3位，但12是2位，所以百位是空格。
DrawString         = 0,  30, "%tY %tm %td  [ %02d:%02d:%02d ]", DATE, DATE, DATE, MC_THOR, MC_TMIN, MC_TSEC
→ 如果2014/10/24的游戏内时间是12点34分56秒，画面上会显示 2014 10 24  [ 12:34:56 ]。
DrawString         = 0,  20, "%3d/%3d  = %.1f%%", HP, MAX_HP, HP_PER
→ 如果当前HP=50，最大HP=100，画面上会显示  50/100  = 50%。%% 表示%字符本身。
DrawString         = 0,  10, "[ %s ]", name
→ 如果载具是 AH-64，画面上会显示 AH-64D Apache Longbow。
DrawCenteredString = 0, -10, "HUD Test"
→ 没有数据部分，所以画面上会一直显示 HUD Test。


;************************************************************************************************************
; 绘制纹理
; 纹理推荐使用 256x256。参数2以后可以全部指定整数或小数
; 参数1  :纹理文件名 (无扩展名。位于 assets\mcheli\textures\gui 内的文件)
; 参数2  : 距离画面中央的X坐标
; 参数3  : 距离画面中央的Y坐标
; 参数4  : 在画面上的宽度
; 参数5  : 在画面上的高度
; 参数6  : 要读取的纹理的X坐标
; 参数7  : 要读取的纹理的Y坐标
; 参数8  : 要读取的纹理的宽度
; 参数9  : 要读取的纹理的高度
; 参数10 : 在画面上的旋转角度(可省略)
DrawTexture = heli_hud, -100.0, 20,  50,20,  0,0, 64,64,  90.0


;************************************************************************************************************
; 绘制矩形
; 参数1  : 距离画面中央的X坐标
; 参数2  : 距离画面中央的Y坐标
; 参数3  : 在画面上的宽度
; 参数4  : 在画面上的高度
DrawRect = -20, -30, 40*throttle, -20


;************************************************************************************************************
; 绘制线条
; 指定参数5以后，可以连续画线。
; 1,2 → 3,4 → 5,6 → 7,8 ...
; 参数4是必须的，5以后根据需要可以使用
;
; 参数1  : 距离画面中央的X坐标
; 参数2  : 距离画面中央的Y坐标
; 参数3  : 距离画面中央的X坐标
; 参数4  : 距离画面中央的Y坐标
; 参数5  : 参数5以后是，X坐标、Y坐标、X坐标、Y坐标...
DrawLine = -40,  30,   40, 30
DrawLine = -20, -30,  -20+40*throttle, -30,  -20+40*throttle, -20,  -20, -20,  -20, -30


;************************************************************************************************************
; 绘制虚线
; 指定参数7以后，可以连续画线。
; 3,4 → 5,6 → 7,8 ...
; 参数6是必须的，7以后根据需要可以使用
; 另外，参数1 是 glLineStipple 的 factor，参数2 是 glLineStipple 的 pattern。
;
; 参数1  : 用16进制的 0 or 1 表示虚线模式 ( 如果不太明白，设置为 0xCCCC 或 0xAAAA 即可 )
; 参数2  : 虚线的倍率 (整数)
; 参数3  : 距离画面中央的X坐标
; 参数4  : 距离画面中央的Y坐标
; 参数5  : 距离画面中央的X坐标
; 参数6  : 距离画面中央的Y坐标
; 参数7  : 参数7以后是，X坐标、Y坐标、X坐标、Y坐标...
DrawLineStipple = 0xF0F0, 1,   -40,  30,   40, 30
DrawLineStipple = 0xFF00, 1,   -20, -30,  -20+40*throttle, -30,  -20


;************************************************************************************************************
; 绘制实体位置(雷达)
; DrawEntityRadar 绘制怪物以外的生物，DrawEnemyRadar 绘制怪物
;
; 参数1  : 旋转角度
; 参数2  : 距离画面中央的X坐标
; 参数3  : 距离画面中央的Y坐标
; 参数4  : 宽度
; 参数5  : 高度
DrawEntityRadar = plyr_yaw, 40,  30,   32, 32
DrawEnemyRadar  = plyr_yaw, 40,  30,   32, 32


;************************************************************************************************************
; 绘制表示角度的刻度
;
; DrawGraduationYaw    用刻度显示横向旋转角度。例子的设置在画面上部显示
; DrawGraduationPitch1 用刻度显示纵向旋转角度。例子的设置在画面左右显示
; DrawGraduationPitch2 显示纵向和滚转角度。例子的设置在画面中央显示
; 参数1  : 旋转角度
; 参数2  : 滚转角度
; 参数3  : 距离画面中央的X坐标
; 参数4  : 距离画面中央的Y坐标
; 除了 DrawGraduationYaw 的参数3，推荐将其他值设置为以下值。
DrawGraduationYaw    = plyr_yaw,    0,     0, -100
DrawGraduationPitch1 = plyr_pitch,  0,     0, 0
DrawGraduationPitch2 = plyr_pitch, -roll,  0, 0


;************************************************************************************************************
; 绘制玩家与载具的角度差
;
; 用一个小方块显示玩家相对于载具朝向哪个方向。
; 参数1  : 距离画面中央的X坐标
; 参数2  : 距离画面中央的Y坐标
DrawCameraRot = 0, 60

======================================2025.10.9更新，以下为MCHeli-Reforged参数=====================================

1，显示参数
AIRBURST_DIST
;显示空爆弹药的空爆距离，float类型。使用%f
;用例如下
DrawString  =(-270*(width/960)), (25*(height/500)), "AB Range:%s.0m" , AIRBURST_DIST

SPEED
;显示当前载具的速度，float类型。使用%f
;用例如下
DrawCenteredString = (265*(width/960)), (-5*(height/500)), "M:%.2f", SPEED


KEY_FLARE，KEY_CHAFF, KEY_APS, KEY_MAINTENANCE, KEY_SWEEPWING, KEY_CAMERAMODE
;显示热焰弹/烟雾弹，箔条，主动防御系统，维修系统，减速板/后掠翼，以及瞄准镜白光/夜视/热成像通道切换的按键显示，string类型。使用%s
;用例如下
DrawCenteredString = (350*(width/960)), (30*(height/500)), "Activate:%s", KEY_FLARE

DrawCenteredString = (350*(width/960)), (70*(height/500)), "USE:%s", KEY_CHAFF

DrawCenteredString = (350*(width/960)), (70*(height/500)), "Activate:%s", KEY_APS

DrawCenteredString = (350*(width/960)), (110*(height/500)), "Activate:%s", KEY_MAINTENANCE

DrawCenteredString = (350*(width/960)), (150*(height/500)), "Deploy:%s", KEY_SWEEPWING

DrawCenteredString= (350*(width/960)), (155*(height/500)), "Toggle:%s", KEY_CAMERAMODE


COOLDOWN_FLARE, COOLDOWN_CHAFF, COOLDOWN_APS, COOLDOWN_MAINTENANCE, 
;显示热焰弹/烟雾弹，箔条，主动防御系统，维修系统的冷却时间，float类型。使用%f
;用例如下

DrawCenteredString = (330*(width/960)), (30*(height/500)), "CD:%.1fsec", COOLDOWN_FLARE

DrawCenteredString = (330*(width/960)), (70*(height/500)), "CD:%.1fsec", COOLDOWN_CHAFF

DrawCenteredString = (337*(width/960)), (70*(height/500)), "CD:%.1fs", COOLDOWN_APS

DrawCenteredString = (330*(width/960)), (110*(height/500)), "CD:%.1fsec", COOLDOWN_MAINTENANCE


is_sweepwing_fold
;判定减速板/后掠翼是否展开,1为减速板/后掠翼放下,0为减速板/后掠翼开启
;用例如下
If = is_sweepwing_fold==1
DrawTexture = sweepwing_notfold, (380*(width/960)), (140*(height/500)), (80*(width/960)), (40*(height/500)), 0,0,256,128,0
Color = 0xFF00FF00
DrawCenteredString = (350*(width/960)), (150*(height/500)), "Deploy:%s", KEY_SWEEPWING
Endif
;??当减速板/后掠翼处于放下状态（is_sweepwing_fold==1），绘制相关贴图

If = is_sweepwing_fold==0
DrawTexture = sweepwing_fold, (380*(width/960)), (140*(height/500)), (80*(width/960)), (40*(height/500)), 0,0,256,128,0
Color = 0xFFFF4B4B
DrawCenteredString = (350*(width/960)), (150*(height/500)), "Stow:%s", KEY_SWEEPWING
Endif
;??当减速板/后掠翼处于开启状态（is_sweepwing_fold==0），绘制相关贴图

G_FORCE
;过载参数，string类型，使用%s(使用后你可以看到能够承受40G以上过载的史蒂夫超人)
;用例如下
DrawString = (-270*(width/960)), (110*(height/500)), "Over G        %s", G_FORCE

2，判定参数
can_chaff
;能否使用箔条，0表示不能，1表示能
have_chaff
;是否拥有箔条功能，0表示无，1表示有
;结合用例如下

If = (have_chaff==1&&can_chaff==1)
DrawTexture = button_chaff_Ready, (380*(width/960)), (60*(height/500)), (80*(width/960)), (40*(height/500)), 0,0,256,128,0
Color = 0xFF00FF00
DrawCenteredString = (350*(width/960)), (70*(height/500)), "USE:%s", KEY_CHAFF
Endif
If = ((have_chaff==1&&can_chaff==0)||have_chaff==0)
DrawTexture = button_chaff_NotReady, (380*(width/960)), (60*(height/500)), (80*(width/960)), (40*(height/500)), 0,0,256,128,0
Color = 0xFFFF4B4B
DrawCenteredString = (330*(width/960)), (70*(height/500)), "CD:%.1fsec", COOLDOWN_CHAFF
Endif
;就绪状态??：当玩家拥有箔条(have_chaff==1)且系统可用(can_chaff==1)时，显示绿色的"USE"按钮和对应按键提示；
;未就绪状态??：当玩家没有箔条(have_chaff==0)或系统处于冷却状态(have_chaff==1&&can_chaff==0)时，显示红色的冷却倒计时提示。


can_maintenance
;能否使用维修系统，0表示不能，1表示能
have_maintenance
;是否拥有维修系统功能，0表示无，1表示有
;结合用例如下
If = (have_maintenance==1&&can_maintenance==1)&&(cam_zoom==1)
DrawTexture = button_maintence_Ready, (380*(width/960)), (100*(height/500)), (80*(width/960)), (40*(height/500)), 0,0,256,128,0
Color = 0xFF00FF00
DrawCenteredString = (350*(width/960)), (110*(height/500)), "USE:%s", KEY_MAINTENANCE
Endif
If = ((have_maintenance==1&&can_maintenance==0)||have_maintenance==0)&&(cam_zoom==1)
DrawTexture = button_maintence_NotReady, (380*(width/960)), (100*(height/500)), (80*(width/960)), (40*(height/500)), 0,0,256,128,0
Color = 0xFFFF4B4B
DrawCenteredString = (330*(width/960)), (110*(height/500)), "CD:%.1fsec", COOLDOWN_MAINTENANCE
Endif
;就绪状态??：当玩家拥有维护资源(have_maintenance==1)、系统可用(can_maintenance==1)且摄像头处于缩放模式(cam_zoom==1)时，显示绿色的"USE"按钮和对应按键提示；
??;未就绪状态??：当玩家没有维护资源(have_maintenance==0)或系统处于冷却状态(have_maintenance==1&&can_maintenance==0)且摄像头处于缩放模式(cam_zoom==1)时，显示红色的冷却倒计时提示。

can_aps
;能否使用主动防御系统，0表示不能，1表示能
have_aps
;是否拥有主动防御系统功能，0表示无，1表示有
;结合用例
If = (have_aps==1&&can_aps==1)
DrawTexture = button_aps_Ready, (380*(width/960)), (60*(height/500)), (80*(width/960)), (40*(height/500)), 0,0,256,128,0
Color = 0xFFFFE400
DrawCenteredString = (350*(width/960)), (70*(height/500)), "Activate:%s", KEY_APS
Endif
If = ((have_aps==1&&can_aps==0))
DrawTexture = button_aps_NotReady, (380*(width/960)), (60*(height/500)), (80*(width/960)), (40*(height/500)), 0,0,256,128,0
Color = 0xFFFF4B4B
DrawCenteredString = (337*(width/960)), (70*(height/500)), "CD:%.1fs", COOLDOWN_APS
Endif
;就绪状态??：当玩家拥有APS系统(have_aps==1)且系统可用(can_aps==1)时，显示黄色的"Activate"按钮和对应按键提示(KEY_APS)，使用黄色(0xFFFFE400)作为提示颜色；
??;未就绪状态??：当玩家拥有APS系统(have_aps==1)但系统处于冷却状态(can_aps==0)时，显示红色的冷却倒计时提示(COOLDOWN_APS)，使用红色(0xFFFF4B4B)作为警告颜色。

is_engine_shutdown
;判定载具是否处于瘫痪状态，是为1，不是为0
;结合用例
If =is_engine_shutdown==0
	DrawTexture = M1A2BNK,(-600*width/683)-stick_x*5, (-600*height/353)-stick_y*1+pitch*1,1200*width/683,1200*height/353, 0,0, 1960,1960
EndIf
If =is_engine_shutdown==1
	DrawTexture = M1A2BNKLOWHP,(-600*width/683)-stick_x*5, (-600*height/353)-stick_y*1+pitch*1,1200*width/683,1200*height/353, 0,0, 1960,1960
EndIf
;引擎运行状态??：当引擎未关闭(is_engine_shutdown==0)时，显示正常状态的坦克贴图(M1A2BNK)；
??;引擎关闭状态??：当引擎关闭(is_engine_shutdown==1)时，显示低生命值/特殊状态的坦克贴图(M1A2BNKLOWHP)。

hud_type
;与载具参数中的hudtype相对应，属于标识字段。可以通过该参数，仅使用一个一个hud文件，为多个载具实现不同的hud显示效果
;具体用例可以参考HUD文件夹的MBT_ZOOM.txt文件，我用该功能区分了中美俄三类坦克的瞄准镜界面。可以将该文件发给AI让其为您分析其中逻辑

weapon_group_type
;与载具参数中的weapongrouptype相对应，属于标识字段。可以通过该参数，仅使用一个一个hud文件，为多个载具实现不同的hud显示效果
;用途与hud_type相同，但是为了绘制不同hud文件，因此增加该参数，该参数用于配置载具的武器组合
;具体用例可以参考HUD文件夹的MBT_weapon_group.txt,IFV_weapon_group.txt,HELI_weapon_group.txt等文件。可以将该文件发给AI让其为您分析其中逻辑
    
third_person
;是否为第三人称，1为是，0为不是
;用例
If =third_person==0
;第一人称各种hud界面绘制
EndIf
If =third_person==1
;第三人称各种hud界面绘制
EndIf

have_rwr
;判定是否具有RWR，1为拥有，0为无
;用法如下
If =have_rwr==0
;载具无RWR情况下各种hud界面绘制
EndIf
If =have_rwr==1
;有RWR情况下各种hud界面绘制
EndIf

missile_lock_type
;判定追踪自己的导弹的导弹类型:0-未被追踪 1-被半主动雷达导弹追踪 2-被红外导弹追踪 3-被主动雷达导弹追踪 4-被其他武器追踪(手持武器)
vehicle_lock_type
;判定追踪自己的导弹所属者的类型:0-未被追踪 1-被锁定(此时导弹未发射)(有bug，只对手持武器生效) 2-追踪自己的导弹的发射者为地面载具
;3-追踪自己的导弹的发射者为空中载具 4-追踪自己的导弹的发射者为其他(步兵)
missile_lock_dist
;判定追踪自己的导弹距离自己的距离:0-未锁定 1-50米内 2-150米内 3-600米内

;由此我们可以写一个RWR指示灯系统，用例如下

;RWR指示灯参数
;AAM
DrawTexture = AAM_IRM, (-457*(width/960)), (-105*(height/500)), (48*(width/960)), (16*(height/500)), 0,0,256,96,0
DrawTexture = AAM_ARM, (-407*(width/960)), (-105*(height/500)), (48*(width/960)), (16*(height/500)), 0,0,256,96,0
DrawTexture = AAM_SARH, (-357*(width/960)), (-105*(height/500)), (48*(width/960)), (16*(height/500)), 0,0,256,96,0


If = (missile_lock_type==2)&&(vehicle_lock_type==3)
DrawTexture = AAM_IRM_FLASH, (-457*(width/960)), (-105*(height/500)), (48*(width/960)), (16*(height/500)), 0,0,256,96,0
Endif
If = (missile_lock_type==3)&&(vehicle_lock_type==3)
DrawTexture = AAM_ARM_FLASH, (-407*(width/960)), (-105*(height/500)), (48*(width/960)), (16*(height/500)), 0,0,256,96,0
Endif
If = (missile_lock_type==1)&&(vehicle_lock_type==3)
DrawTexture = AAM_SARH_FLASH, (-357*(width/960)), (-105*(height/500)), (48*(width/960)), (16*(height/500)), 0,0,256,96,0
Endif
;在??同时满足导弹类型为红外/主动雷达/半主动雷达（missile_lock_type=2/3/1）且发射源为空中载具（vehicle_lock_type==3）??时，用后缀为_FLASH的贴图覆盖对应静态图标，形成威胁警告。

;SAM
DrawTexture = SAM_IRM, (-457*(width/960)), (-85*(height/500)), (48*(width/960)), (16*(height/500)), 0,0,256,96,0
DrawTexture = SAM_ARM, (-407*(width/960)), (-85*(height/500)), (48*(width/960)), (16*(height/500)), 0,0,256,96,0
DrawTexture = SAM_SARH, (-357*(width/960)), (-85*(height/500)), (48*(width/960)), (16*(height/500)), 0,0,256,96,0

If = (missile_lock_type==2)&&(vehicle_lock_type==2)
DrawTexture = SAM_IRM_FLASH, (-457*(width/960)), (-85*(height/500)), (48*(width/960)), (16*(height/500)), 0,0,256,96,0
Endif
If = (missile_lock_type==3)&&(vehicle_lock_type==2)
DrawTexture = SAM_ARM_FLASH, (-407*(width/960)), (-85*(height/500)), (48*(width/960)), (16*(height/500)), 0,0,256,96,0
Endif
If = (missile_lock_type==1)&&(vehicle_lock_type==2)
DrawTexture = SAM_SARH_FLASH, (-357*(width/960)), (-85*(height/500)), (48*(width/960)), (16*(height/500)), 0,0,256,96,0
Endif
;在??同时满足导弹类型为红外/主动雷达/半主动雷达（missile_lock_type=2/3/1）且发射源为地面载具（vehicle_lock_type==2）??时，用后缀为_FLASH的贴图覆盖对应静态图标，形成地对空导弹威胁警告。


;锁定/跟踪指示灯
DrawTexture = LCK, (-450*(width/960)), (105*(height/500)), (60*(width/960)), (20*(height/500)), 0,0,256,96,0
DrawTexture = TRK, (-380*(width/960)), (105*(height/500)), (60*(width/960)), (20*(height/500)), 0,0,256,96,0
If = vehicle_lock_type==1
DrawTexture = LCK_FLASH, (-450*(width/960)), (105*(height/500)), (60*(width/960)), (20*(height/500)), 0,0,256,96,0
endif
If = ((vehicle_lock_type==2)||(vehicle_lock_type==3)||(vehicle_lock_type==4))==1
DrawTexture = TRK_FLASH, (-380*(width/960)), (105*(height/500)), (60*(width/960)), (20*(height/500)), 0,0,256,96,0
endif
;??锁定警告??：当被武器锁定（vehicle_lock_type==1）时，用后缀为_FLASH的贴图覆盖对应静态图标，形成锁定警告
??;跟踪警告??：当被地面载具、空中载具或步兵跟踪（vehicle_lock_type==2/3/4）时，用后缀为_FLASH的贴图覆盖对应静态图标，形成跟踪警告

