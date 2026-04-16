

; ★ 重要 ★
; 配置文件和模型可在不关闭Minecraft的情况下重新加载。
; [ 进入载具 → 按R键打开补给界面 → MOD选项 → 开发 → 重新加载飞行器设置 ]
; 纹理和音效需使用Minecraft原生功能重载，而非通过直升机MOD。
; [ 按Esc打开游戏菜单 → 设置 → 资源包 → 完成 ]

;***********************************************************************************
■ 直升机/战斗机/地面载具/车辆配置文件的通用设置
;***********************************************************************************

DisplayName = AH-6 杀手蛋
; 显示名称 ※请勿使用全角字符，仅限半角英数字及符号

AddDisplayName = ja_JP, AH-6 キラ－エッグ
; 手持物品时显示的名称
; ※ 使用日语全角字符时，文件编码必须为UTF-8

ItemID = 28801
; 物品ID (在Minecraft中实际使用时将+256)
; ※ 1.7.2+版本不再使用ItemID，但若需兼容1.6.4及更早版本则必须设置

Invulnerable = true
; 载具无敌模式
; 建议在多人游戏中用于基地防御武器

CreativeOnly = true
; 仅创造模式玩家可放置或回收物品

AddTexture = sh60-us-1
AddTexture = sh60-jp-1
AddTexture = sh60-jp-2
; 附加纹理（可添加多个）
; 默认使用与配置文件同名的png文件
; 此处添加额外纹理（无需扩展名）

ThirdPersonDist = 12
; 第三人称视角默认距离
; 直升机MOD中可用PageUp/Down以4方块为单位调整距离，建议设为4的倍数

CameraPosition = 0.0, 1.1, 3.7
CameraPosition = 0.0, 1.1, 3.7, false
CameraPosition = 0.0, 1.1, 3.7, true, 30, 45
; 摄像机坐标
; 多重设置时可通过H键切换不同视角
; 第1-3位：坐标
; 第4位设为true时始终锁定摄像机视角
; 第5位：水平角度
; 第6位：垂直角度

CameraZoom = 3
; 摄像机最大缩放倍数

HUD = heli, heli_gnr, none, gunner
; 各座位使用的HUD配置文件名称
; 本例中：驾驶席使用heli.txt，第2席用heli_gnr.txt，第3席无HUD，第4席用gunner.txt
; 若设置数量少于座位数，未指定的座位将使用以下默认配置：
; 未设置时默认配置：
; 直升机：HUD = heli, heli_gnr, gunner, gunner, gunner, gunner...
; 固定翼：HUD = plane, plane, gunner, gunner, gunner, gunner...
; 地面载具：HUD = vehicle
;
; ※ 仅驾驶席在炮手模式下会使用第2席的HUD配置
;    即使单人载具，若启用炮手模式也需设置第2席
;    例如：HUD = heli, heli_gnr

EnableGunnerMode = true
; 是否启用炮手模式切换（true=启用，false=禁用）

EnableNightVision = true
; 是否启用夜视模式切换（true=启用，false=禁用）

EnableEntityRadar = false
; 是否启用雷达（true=启用，false=禁用）

Speed = 0.6
; 载具速度（值越大越快）

MotionFactor = 0.96
; 载具移动减速系数（范围0.0~1.0，值越小减速越强）

Gravity = -0.04
; 载具重力设置（负值表示下落）

GravityInWater = -0.04
; 水中重力设置（负值表示下落）

StepHeight = 2.5
; 可跨越的方块高度

MobilityYaw
MobilityYawOnGround
; 水平转向灵敏度（值越大机动性越强）
; MobilityYawOnGround 仅影响地面，不影响水面
MobilityRoll
; 滚转灵敏度（值越大滚转越快）
MobilityPitch
; 俯仰灵敏度（值越大机动性越强；地面载具表示俯仰角上下限）
MinRotationPitch
MaxRotationPitch
; 范围 MinRotationPitch -80~0
; 范围 MaxRotationPitch 0~80
; 俯仰角限制（最小/最大）
; ※ 直升机/战斗机启用此设置将同时限制滚转

MinRotationRoll
MaxRotationRoll
; 滚转角限制（最小/最大）
; 范围 MinRotationRoll -80~0
; 范围 MaxRotationRoll 0~80
; ※ 直升机/战斗机启用此设置将同时限制俯仰

UnmountPosition = 3.0, 1.0, -2.0
; 下机时的坐标

AddSeat =-0.45,  0.80,  1.20
AddSeat = 0.45, -0.50,  1.20
AddSeat =-0.90, -0.50,  0.20
AddSeat = 0.90, -0.50,  0.20, true
; 添加座位 ※除无人机外必须至少1个座位
; 第1个为驾驶席
; 参数为坐标(X,Y,Z)
; 第4个参数决定座位是否随驾驶员朝向旋转（主要用于坦克炮塔）

AddGunnerSeat = -0.45, 0.80, 1.20,   0.0, 2.00, -1.01,   true
AddGunnerSeat = -0.45, 0.80, 1.20,   0.0, 2.00, -1.01,   true, -60, 78, true
; AddGunnerSeat=座位X,Y,Z,  摄像机X,Y,Z,  可切换视角,  摄像头上限(-90~0), 摄像头下限(0~90), 座位随炮塔旋转
; 添加炮手座位
; 此座位玩家默认为摄像机视角
; 参数含座位坐标和摄像机位置（摄像机位置可省略，默认使用CameraPosition）
; 视角切换设为false时锁定摄像机视角，true时可用H键切回玩家视角
; 第10个参数控制座位是否随驾驶员朝向旋转

AddFixRotSeat = -0.45,  0.80,  1.20, 0.0,2.00,-1.01,  true,  -50, 40
; AddFixRotSeat=座位X,Y,Z,  摄像机X,Y,Z,  可切换视角,  水平固定角度, 垂直固定角度
; 添加固定视角座位
; 与AddGunnerSeat类似，但摄像机角度固定不可调
; 设置固定角度后无法用鼠标调整视角（可用Ctrl键切换自由视角）

; ★★★★★
; 载具搭载功能需满足：
; ・搭载方指定被搭载载具（AddRack）
; ・被搭载方指定搭载方挂架编号（RideRack）
; 任一条件满足即可生效

AddRack = container,                 0.0, 1.4, -4.7,  0.0, 1.0, -16.1
AddRack = container / ah-64,         0.0, 1.4, -4.7,  0.0, 1.0, -16.1,  5.0, 20
AddRack = helicopter/vehicle / t-4,  0.0, 1.4, -4.7,  0.0, 1.0, -16.1,  5.0, 100000,  0.0, 0.0
; ■ 搭载方设置
; AddRack = 
;  参数1：可搭载实体名
;  参数2-4：挂架坐标X,Y,Z（在载具上的位置）
;  参数5-7：出入口坐标X,Y,Z（实体需靠近此坐标才能装载/卸载）
;  参数8：入口半径（可省略）
;  参数9：开伞高度（设极大值可禁用降落伞）
;  参数10：被载实体水平角度
;  参数11：被载实体垂直角度
; 添加可搭载容器/直升机等的挂架
;  实体名可用 container/helicopter/plane/vehicle 或直接指定载具名（如ah-64），用/分隔

RideRack = c5, 1
; ■ 被搭载方设置
; RideRack = 搭载载具名, 挂架编号（1~） 

ExclusionSeat = 15, 17
; 参数数量不限（需≥2）
; 设置互斥座位/挂架
; 例如 ExclusionSeat = 3,4,5 时：
;  3号位有实体时，4/5号位不可载实体
;  4号位有实体时，3/5号位不可载实体
;  5号位有实体时，3/4号位不可载实体
;
; 座位/挂架编号规则：先按所有座位定义顺序编号，再编号挂架
; 示例配置顺序：
;  AddSeat  →1号
;  AddRack  →4号
;  AddGunnerSeat →2号
;  AddRack  →5号
;  AddSeat  →3号
;  AddRack  →6号
; 建议最后定义挂架以便编号清晰

TurretPosition = 0.0, 0.0, 0.25
; 炮塔旋转中心位置（非必要不建议修改）

AddWeapon = m230,     0.00, 0.90, 2.54,   0.0, 0.0, true, 2
AddWeapon = hydra70,  0.00, 0.90, 2.54,   0.0, 0.0, true, 1, 0,-60,60, 0,25
AddWeapon = m134,     1.48, 0.40, 1.54,   1.0, 0.0
AddWeapon = m134,    -1.48, 0.40, 1.54,  -1.0, 0.0
AddTurretWeapon = hydra70,  0.00, 0.90, 2.54,   0.0, 0.0, true, 1, 0,-60,60, 0,25
; 添加武器（文件名需匹配weapons文件夹内无扩展名文件）
; 连续添加相同武器（如m134）将视为单武器多发射点
; 参数顺序：武器配置名、位置(X,Y,Z)、旋转角(水平,垂直)、驾驶员可用性、座位、默认水平角、最小水平角、最大水平角、最小俯仰角、最大俯仰角
;
; 座位：1=1号位，2=2号位，依此类推
; 参数组合说明：
;  true,2 → 2号位玩家可用，2号位无人时驾驶员可用
;  false,2 → 仅2号位玩家可用
;  false,1 → 仅驾驶员可用（不推荐）
;  true,1 → 仅驾驶员可用
; 省略时默认为 true,1
;
; AddTurretWeapon与AddWeapon区别仅在于发射点随炮塔旋转

AddSearchLight      = 0.71,  -0.02,  0.02,   0x50FFFFFF,   0x10FFFFC0,    60.0, 20.0,       0,   0
AddFixedSearchLight = 0.71,  -0.02,  0.02,   0x50FFFFFF,   0x10FFFFC0,    60.0, 20.0,       0,   0
AddSteeringSearchLight = -0.52,0.90, 1.76,   0x50FFFFFF,   0x00FFFFC0,    27.0, 15.0,       5,   0,     45
;AddSearchLight     = 坐标X,Y,Z,   起点颜色, 终点颜色,  距离, 末端半径, 水平角, 俯仰角, 舵角
; AddSearchLight      ：动态探照灯（随乘员视角旋转）
; AddFixedSearchLight ：固定方向探照灯
; AddSteeringSearchLight ：随轮胎方向旋转的固定灯（舵角建议匹配轮胎转向角）

AddPartLightHatch =  0.32, 0.23, 1.83,   -1,0,-0.024, 90
;AddPartLightHatch= 坐标X,Y,Z, 旋转轴X,Y,Z, 旋转角度 -1800~1800
; 添加探照灯开启时才会展开的部件
; ★重要：必须先设置AddSearchLight或AddFixedSearchLight

AddRecipe = " Y ",  "YXY",  " YD",  X, iron_block, Y, iron_ingot, D,dye,2
AddRecipe ="YXY", X, mcheli:ah-6, Y, redstone
AddShapelessRecipe = iron_block, iron_ingot, dye,2
; 添加合成配方（多行AddRecipe可增加配方）
; ""内3字符对应工作台横向排列
; (格式同Forge的GameRegistry.addRecipe)
; 示例详解：
; X = 铁块名
; Y = 铁锭物品名
; D = 绿色染料物品名（带损伤值的物品需在名称后加损伤值）
; 物品名参考：http://minecraft.gamepedia.com/Data_values
; 原版物品可省略"minecraft:"前缀
; MOD物品需指定MOD名（如 mcheli:ah-6）
; AddShapelessRecipe 添加无序合成配方

FlareType = 1
; 诱饵弹类型：
; 0=无
; 1=普通
; 2=大型机用
; 3=横向抛洒
; 4=向前抛洒
; 5=向下抛洒
; 10=坦克烟雾弹

Float  = true
; 启用漂浮

FloatOffset = -1.0
; 漂浮高度偏移（可为负值）

SubmergedDamageHeight = 2
; 低于此高度的水接触不造成伤害（单位：方块）

MaxHP = 100
; 耐久值

ArmorDamageFactor = 0.5
; 载具受伤系数（1.0=100%，0.5=50%）

ArmorMinDamage = 5
; 最小伤害阈值（低于此值不受伤害）

ArmorMaxDamage = 500
; 最大伤害上限（超出部分按此值计算）

InventorySize = 18
; 载具物品栏大小（需为9的倍数）

DamageFactor = 0.2
; 玩家受伤系数（0.2=承受20%伤害）
; 注：玩家受伤时载具同步受损

Sound = heli
; 油门提升时的音效文件（对应sounds/heli.ogg）

UAV = true
SmallUAV = true
; true=无人机（无法进入驾驶席）
; UAV=true：大型无人机（不可用手持终端控制）
; SmallUAV=true：小型无人机（可用手持终端控制）
; 注：无人机控制站可控制所有类型，手持终端仅限小型机

TargetDrone = true
; 仅战斗机有效：true=无人靶机（无法进入驾驶席）
; 仅可通过无人机控制站生成，生成后自动低空盘旋

OnGroundPitch = 角度
; 地面停放时的俯仰角（如零式战斗机地面机头上扬）

AddPartHatch = 位置X,Y,Z, 旋转轴X,Y,Z, 旋转角度0~180
; 添加Z键开闭的舱门
; 模型命名：载具名_hatch?.obj（?从0开始）
; 找不到模型时不显示（若无显示需求可不做模型）

AddPartSlideHatch = 移动量X,Y,Z
; 添加滑动式舱门（模型命名规则同AddPartHatch）

AddPartCamera = 坐标X,Y,Z, 水平联动, 俯仰联动
; 添加始终朝向玩家的部件
; 模型命名：载具名_camera?.obj

AddPartRotation = 0.00, 9.00, -31.17,  0,-1,0,       1.3,      false
; AddPartRotation = 位置X,Y,Z,        旋转轴X,Y,Z,   转速,  是否持续旋转
; 添加周期性旋转部件

AddPartWeapon        = m230,       false, true, true,  -2.51,  1.29,  -1.51
AddPartWeapon        = m102_105mm, false, true, true,  -2.51,  1.29,  -1.51, 1.00
AddPartWeapon        = rehinmetall_apfsds / rehinmetall_he, false, true, false,  0.00, 2.10, 0.00, 0
AddPartTurretWeapon  = mg7_62mm,   false, true, true,  -0.83,  3.39,  -0.57, 0
AddPartRotWeapon     = m134_r50,   false, true, true,  -1.825, 1.475, -0.25, 1,0,0
AddPartWeaponChild   = false, true, 0.00, 0.5, 3.00
AddPartWeaponMissile = aim120,     false, false,false, -2.51,  1.29,  -1.51
; 直升机/战斗机武器部件设置
; AddPartWeapon = 关联武器名(none表示无), 炮手模式隐藏?, 水平联动, 俯仰联动, 旋转坐标X,Y,Z, 后坐距离
; AddPartRotWeapon = 关联武器名, 炮手模式隐藏?, 水平联动, 俯仰联动, 旋转坐标X,Y,Z, 旋转轴X,Y,Z
; AddPartWeaponChild = 水平联动, 俯仰联动, 旋转坐标X,Y,Z
; 随AddWeapon武器角度变化（武器名用/分隔）
; 后坐距离为火炮后坐位移
; AddPartRotWeapon用于转管机枪（开火时旋转）
; 模型命名：载具名_weapon?.obj
;
; AddPartWeaponChild 作为AddPartWeapon的子部件添加
; 必须紧接AddPartWeapon后定义
; 模型命名：载具名_weapon?_0.obj（?为父部件编号）
;
; AddPartWeaponMissile 在武器未就绪时隐藏（如导弹/炸弹）

AddPartWeaponBay = 武器名, 位置X,Y,Z, 旋转轴X,Y,Z, 旋转角度0~180
; 添加旋转开启的武器舱
AddPartSlideWeaponBay = 武器名, 移动量X,Y,Z
; 添加滑动开启的武器舱
; 模型命名：载具名_wb?.obj

AddPartCanopy = 位置X,Y,Z, 旋转轴X,Y,Z, 旋转角度0~180
; 添加旋转式座舱盖
AddPartSlideCanopy = 移动量X,Y,Z
; 添加滑动式座舱盖
; 模型命名：载具名_canopy?.obj（可添加多个）
; 兼容性说明：省略数字时默认使用_canopy0.obj

AddPartThrottle = 位置X,Y,Z,  旋转轴X,Y,Z,  旋转角度0~180,  移动量X,Y,Z
; 添加随油门联动旋转/移动的部件
; 旋转角度前为必填项

AddPartLG = 位置X,Y,Z, 旋转轴X,Y,Z, 旋转角度0~180 [, 旋转轴X,Y,Z, 旋转角度0~180]
AddPartLGRev = 位置X,Y,Z, 旋转轴X,Y,Z, 旋转角度0~180 [, 旋转轴X,Y,Z, 旋转角度0~180]
AddPartSlideRotLG = 移动量X,Y,Z,  位置X,Y,Z, 旋转轴X,Y,Z, 旋转角度0~180
AddPartLGHatch = 位置X,Y,Z, 旋转轴X,Y,Z, 旋转角度0~180 [, 旋转轴X,Y,Z, 旋转角度0~180]
; 添加起落架（起飞时自动收起）
; 模型命名：载具名_lg?.obj
; AddPartLGRev 与AddPartLG动作相反
; AddPartLGHatch 仅在起落架折叠/展开时开启
;
; 动作说明：
; AddPartLG      收起：0°→90°
; AddPartLGRev   收起：90°→0°
; AddPartSlideRotLG 收起：0°→90°
; AddPartLGHatch 收起：0°→90°→0°

TrackRollerRot = 30
; 履带轮转速（负值为反转，但不推荐）

AddTrackRoller = -1.72,  0.77,  5.04
; 添加履带轮（仅需坐标，X负值=右侧，正值=左侧）
; 可独立于履带设置

AddCrawlerTrack = false, 0.37, -2.09,  1.03/-3.41, 0.72/-3.57, 0.37/-3.42, -0.15/-2.55, -0.25/-2.16, -0.25/3.88, -0.13/4.21, 0.52/5.29, 0.78/5.39, 1.03/5.28, 1.10/5.04, 1.15/-3.12
;AddCrawlerTrack = 履带正反,  单节间距, 履带X位, 旋转点Y/Z, ...
; 履带运动异常时可调整正反参数
; 游戏测试模式会以红/蓝点显示设定位置

PartWheelRot = 40
; 轮胎转速（值越大越快）

AddPartWheel     = -1.05, 0.157, 1.965,  30
; 添加轮胎     X,Y,Z坐标,  最大转向角
AddPartWheel     =  0.68,  0.19,  1.20,  30,   0.0, 1.0, 0.2,   0.68, 0.19, 0.70
; 添加轮胎     X,Y,Z坐标,  转向角, 旋转轴X,Y,Z,    旋转位置X,Y,Z
; 省略旋转轴时默认(0,1,0)

AddPartSteeringWheel =  -0.54, 0.88,  0.48,   0.0,     1.0, -1.7,  130
; 添加方向盘        X,Y,Z坐标,  旋转轴X,Y,Z,   最大旋转角度

ThrottleUpDown = 1.0
ThrottleUpDownOnEntity = 2.0
; 油门响应系数（值越小起飞越慢）
;
; ThrottleUpDownOnEntity 为载具搭载在其他实体上时的响应系数（默认2.0）
; 计算公式：
; ThrottleUpDown * 搭载实体速度 * ThrottleUpDownOnEntity → 油门灵敏度
; 例如：ThrottleUpDownOnEntity=2.0时搭载矿车（最高速≈1.7）
;       1.7 * 2.0=3.4 → 仅需1/3距离即可起飞

AutoPilotRot = -0.4
; 自动转向角度（值越大转弯半径越小）
; 0=直行
; 负值=左转，正值=右转

ConcurrentGunnerMode = true
; 允许第2席有玩家时仍可进入炮手模式

Regeneration = true
; 第2席及之后的乘员自动回血

ParticlesScale = 0.1
; 沙尘等粒子效果尺寸（值越大效果越明显）

FuelSupplyRange = 25
; 为其他载具供油的范围（单位：米）
; 供油时自身燃料不减少
; 不可给自身供油

AmmoSupplyRange = 35
; 为其他载具补给弹药的范围（单位：米）
; 补给时自身弹药不减少
; 不可给自身补给

MaxFuel         = 600
; 最大燃料容量
FuelConsumption = 0.5
; 每秒燃料消耗量
; 续航时间(秒) = 最大燃料容量 / 每秒消耗量
; 600 / 0.5 = 1200秒

Stealth = 0.5
; 隐身性（0.0~1.0，默认0.0）
; 值越高越难被导弹锁定（延长锁定时间，缩短锁定距离）

SmoothShading = false
; 平滑着色开关
; false=平面着色（棱角分明）
; true=平滑着色（边缘柔化）
; 注：mcheli.cfg中SmoothShading=false将全局禁用平滑着色

HideEntity = false
; 是否隐藏乘员模型
; true=隐藏
; false=显示

EntityWidth  = 0.9
EntityHeight = 0.9
; 乘员模型渲染尺寸（宽/高，范围-100.0~100.0）
; 0.5=尺寸减半

EntityPitch = 45
EntityRoll  = 20
; 乘员模型渲染角度（-360~360）

CanRide = false
; 是否允许乘坐
; true=允许（默认）
; false=禁止

BoundingBox =  碰撞箱中心X,Y,Z,  宽度, 高度, 伤害倍率
; 添加碰撞箱
; 仅受本MOD机炮/导弹影响
; 不与方块/实体碰撞
; 在MOD选项开启TestMode可显示
; 伤害倍率默认1.0（0.5=半伤，3.0=三倍伤）

Category = W.A
; 载具分类（仅用于创造模式物品栏排序）

CanMoveOnGround = false
CanRotOnGround  = false
; 地面移动/旋转禁令
;  CanMoveOnGround：禁止地面移动
;  CanRotOnGround：禁止地面旋转

EnableParachuting = true
; 启用跳伞功能（仅限第3席及之后玩家按空格跳伞）
MobDropOption  = 0.0, 0.0, -11.5,  10
; 乘员投放设置 = 投放点X,Y,Z, 投放间隔（1/20秒）

RotorSpeed = 50.0
; 旋翼转速（值越大越快，负值为反转但不推荐）

;***********************************************************************************
■ 直升机专属设置
;***********************************************************************************

;需四文件（全小写）：
;  helicopters文件夹：配置文件
;  models/helicopters：模型
;  textures/helicopters：纹理
;  textures/items：物品纹理

EnableFoldBlade = false
; 旋翼折叠功能（true=启用）

AddRotor= 6, 60,  0.00,  3.35,  0.00,  0.0, 1.0, 0.0, true
AddRotor= 2, 60,  0.50,  1.90, -6.55,  1.0, 0.0, 0.0
; 添加旋翼（数量不限）
; 本例第1个主旋翼，第2个尾旋翼
; 仅第1个旋翼可折叠
; 参数：叶片数、叶片间角度、位置X,Y,Z、旋转轴X,Y,Z、是否可折叠
; 模型命名：载具名_rotor?.obj
;
; ※ 旧版AddRotorOld已弃用

AddRepellingHook =  0.60, 2.75, -14.21, 30
; 索降钩设置 = 钩坐标X,Y,Z, 投放间隔

;***********************************************************************************
■ 战斗机专属设置
;***********************************************************************************

;需四文件（全小写）：
;  planes文件夹：配置文件
;  models/planes：模型
;  textures/planes：纹理
;  textures/items：物品纹理

AddPartRotor = 位置X,Y,Z, 旋转轴X,Y,Z, 旋转角度(-180~180)
; 添加旋翼（VTOL时旋转）
; 模型命名：载具名_rotor?.obj
AddBlade = 叶片数, 叶片间角度, 位置X,Y,Z, 旋转轴X,Y,Z
; 必须在AddPartRotor后添加
; 模型命名：载具名_blade?.obj

AddPartWing = 位置X,Y,Z, 旋转轴X,Y,Z, 旋转角度0~180
; 添加可折叠主翼
; 模型命名：载具名_wing?.obj
AddPartPylon = 位置X,Y,Z, 旋转轴X,Y,Z, 旋转角度0~180
; 添加可折叠挂架
; 模型命名：载具名_wing?_pylon?.obj
; 必须在AddPartWing后添加
; 示例：
; AddPartWing  → 模型：载具名_wing0.obj
; AddPartPylon → 模型：载具名_wing0_pylon0.obj / wing0_pylon1.obj

PivotTurnThrottle = 0.0
; 地面转向时的移动量
; 0=原地转向，>0=转向时移动
; 坦克设置建议：
;  超信地旋回=0
;  信地旋回>0

EnableBack = true
; 允许倒车

VariableSweepWing = true
SweepWingSpeed = 1.2
; 变后掠翼设置（需AddPartWing）
; VariableSweepWing=true：空中可调节机翼
; SweepWingSpeed=1.2：机翼折叠时的速度

AddPartNozzle = 位置X,Y,Z, 旋转轴X,Y,Z, 旋转角度0~180
; 添加发动机喷口（VTOL时旋转）
; 模型命名：载具名_nozzle?.obj
; 粒子尺寸受ParticlesScale控制

EnableVtol = true
; 是否启用VTOL功能
DefaultVtol = true
; VTOL启用时的默认状态（true=地面自动启用VTOL）
VtolYaw = 0.3
; VTOL状态水平转向量
VtolPitch = 0.3
; VTOL状态俯仰转向量

EnableEjectionSeat = true
; 弹射座椅开关
; true=在GUI添加弹射座椅按钮
; 1座位载具支持1个，2座位载具支持2个

AddParticleSplash  =  1.0,  0.97,   13.19,      3,     9.0,   1.1,        20, 0.30, -0.03
;AddParticleSplash = 坐标X,Y,Z,  粒子数量,  尺寸,  速度,  持续时间, 上升速度, 重力
; 水面移动时生成水花粒子
; 与EnableSeaSurfaceParticle无关

EnableSeaSurfaceParticle = true
; 海面飞行时是否生成水花
; 尺寸受ParticlesScale影响（推荐0.7）
; 注：与AddParticleSplash无关

;***********************************************************************************
■ 地面载具专属设置
;***********************************************************************************

;需四文件（全小写）：
;  vehicles文件夹：配置文件
;  models/vehicles：模型
;  textures/vehicles：纹理
;  textures/items：物品纹理

AddPart = 参数1, 参数2, 参数3, 参数4, 位置X,Y,Z
; 添加随玩家旋转的部件
; 参数1：第一人称是否隐藏（true=显示）
; 参数2：是否水平联动（true=联动）
; 参数3：是否俯仰联动（true=联动）
; 参数4：部件类型（0=普通,1=开火时旋转,2=开火时后坐）
; 模型命名：载具名_part?.obj
AddChildPart = 参数1, 参数2, 参数3, 参数4, 位置X,Y,Z
; 添加子部件（需在AddPart后）
; 模型命名：载具名_part?_#.obj（#从0开始）
; 示例：
; AddPart     → 载具名_part0.obj
; AddChildPart → 载具名_part0_0.obj / part0_1.obj

; RotationPitchMax/Min为旧参数，请勿使用

;***********************************************************************************
■ 车辆专属设置
;***********************************************************************************

;需四文件（全小写）：
;  tanks文件夹：配置文件
;  models/tanks：模型
;  textures/tanks：纹理
;  textures/items：物品纹理

DefaultFreelook = true
; 上车后立即启用自由视角（主要用于坦克）

OnGroundPitchFactor = 2.0
OnGroundRollFactor  = 1.3
; 地形适应倾斜速度
; 值越大倾斜越快
; 高速车辆建议调高，低速车辆调低
; 过高会导致画面抖动，过低会卡进方块

CameraRotationSpeed = 25
; 摄像机旋转速度（坦克可用于限制炮塔转速）

WeightType = Tank
; 重量类型：Tank（坦克） / Car（汽车） / Unknown（未知）
; Tank：撞击生物时自身无伤，破坏更多方块
; Car：撞击生物时自身受伤，破坏较少方块
; 方块破坏规则在mcheli.cfg设置

WeightedCenterZ = 0.0
; 重心Z坐标（影响地形适应倾斜）
; ※ 效果不稳定，如不适建议禁用

SetWheelPos =  1.75,  -0.24,  4.85, 3.02, 1.44, -1.54, -2.91
;SetWheelPos =  X坐标, Y坐标,  Z坐标1, Z坐标2...
; 设置接地点（载具据此倾斜）
; X负值无需设置
; ★ Y坐标强烈建议固定为-0.24

======================================2025.10.8更新，以下为MCHeli-Reforged参数=====================================
    /**
     * 雷达种类
     */
radarType = ModernAARadar
;现代对空雷达
radarType = EarlyAARadar
;早期对空雷达
radarType = ModernASRadar
;现代对地雷达
radarType = EarlyASRadar
;早期对地雷达
;默认值=无

nameOnModernAARadar = "?"
;当前载具在现代对空雷达中显示的名字
;默认值=？

nameOnEarlyAARadar = "?"
;当前载具在早期对空雷达中显示的名字
;默认值=？

nameOnModernASRadar = "?"
;当前载具在现代对地雷达中显示的名字
 ;默认值=？

nameOnEarlyASRadar = "?"
;当前载具在早期对地雷达中显示的名字
 ;默认值=？

explosionSizeByCrash = 5
;载具被摧毁时爆炸范围
 ;默认值=5

throttleDownFactor = 1
;倒车速度倍率(推荐值为3,这样倒车速度大概为前进速度的一半。但倒车速度还受motionfactor影响)
;默认值=1

haschaff = false
;是否有箔条
;默认值=false

chaffUseTime = 100
;箔条生效时长
;默认值=无
 
chaffWaitTime = 400
;箔条冷却时长
;默认值=无

hasmaintenance = false
;是否有维修系统
;默认值=false

maintenanceUseTime = 20
;维修系统生效时长 （时长即为回血百分比）
;默认值=无
 
maintenanceWaitTime = 300
;维修系统冷却时长
;默认值=无

engineShutdownThreshold = 20
;载具瘫痪阈值，血量低于此百分比将关闭载具引擎
;默认值=无
 
hasaps = false
;是否有主动防御系统
;默认值=false

apsUseTime = 100
;APS生效时长(生效时能拦截Rocket和missile类型武器)
;默认值=100
 
apsWaitTime = 400
;APS冷却时长
;默认值=400
 
apsRange = 8
;APS范围
;默认值=8
 
hasRWR = false
;是否有RWR
;默认值=false
 
hudType = 0
;hud自定义字段，用于指示载具hud
;默认值=无
 
weaponGroupType = 0
;hud自定义字段，用于指示载具weaponGroupType
;默认值=无
 
armorExplosionDamageMultiplier = 1.0
;载具爆炸倍率，最终的爆炸伤害=爆炸伤害*爆炸倍率
;默认值=1

;目前MCH-R除了支持mch原版的碰撞箱外，还新增了两种碰撞箱，以下是写法
;第一种为BoundingBox = {center_x}, {center_y}, {center_z}, {width}, {height}, {length}, {multiplier}, DEFAULT, {name}
;它表示在坐标center_x,center_y,center_z的位置生成一个以其为中心，宽高长分别为width,height,length，受到伤害倍率为multiplier，被打击时命中显示名称为name的DEFAULT碰撞箱
;DEFAULT类型碰撞箱不会随炮塔转动，以下是例子
BoundingBox = 0.0, 1.21, 3.6, 4.684, 0.8871, 1.5, 0.4, DEFAULT, Upper Glacis
;该例子代表在0.0, 1.21, 3.6位置，生成宽高长分别为4.684, 0.8871, 1.5，伤害倍率为0.4，被攻击后显示部位为Upper Glacis的普通碰撞箱

;第二种碰撞箱为BoundingBox = {center_x}, {center_y}, {center_z}, {width}, {height}, {length}, {multiplier}, TURRET, {name}
;它表示在坐标center_x,center_y,center_z的位置生成一个以其为中心，宽高长分别为width,height,length，受到伤害倍率为multiplier，被打击时命中显示名称为TURRET的DEFAULT碰撞箱
;TURRET类型碰撞箱会随着炮塔绕0,y,0进行转动(我们推荐将炮塔旋转位置写为0,y,0)，以下是例子
BoundingBox = 0.0, 2.16, -0.44, 4.1, 1, 5.4, 0.4, TURRET, Turret Front
;该例子代表在0.0, 2.16, -0.44位置，生成宽高长分别为4.1, 1, 5.4，伤害倍率为0.4，被攻击后显示部位为Turret Front的随着炮塔旋转的炮塔碰撞箱


======================================2026.3.19更新，以下为MCHeli-Reforged 2.0参数=====================================
radarType = ModernAdvancedAARadar
;雷达类型为高级雷达
nameOnAdvancedAARadar = ""
;当前载具在现代对空雷达中显示的名字,建议做为隐身战机雷达

hasEcmJammer=true
;是否拥有电子战功能

ecmJammerType = 0
; 电子干扰类型，0为机载电子干扰，1为机载电子攻击，2为陆载电子战防御
;机载电子干扰：开启后短暂关闭RWR与超视距功能，自身雷达隐身并释放一个诱饵在地方RWR和超视距界面进行惯性前进的显示，同时还能使得地方激光制导系统的锁定失效
;机载电子攻击：开启后对场上的所有敌人与友军目标进行全频带阻塞干扰，使得他们的超视距雷达与RWR失效
;陆载电子防御:  开启后禁用RWR和超视距，使得反辐射导弹和落点在附件的GPS弹药失去制导，同时短暂干扰激光锁定。后期雷达制导导弹的干扰措施可能也会从烟雾弹改为陆载电子防御（现在没改）

ecmJammerUseTime = 100
;电子干扰生效时长

ecmJammerWaitTime = 400
;电子干扰冷却时长

hasDIRCM = false
;是否拥有DIRCM，需要配合FlareType = 9使用，DIRCM会顶替掉热焰弹的位置，启动的持续时间内能够使得周围所有红外制导导弹，激光制导导弹，指令线制导导弹和电视制导导弹失去制导


