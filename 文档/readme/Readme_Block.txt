2026/04/16

;***********************************************************************************
;■ 方块参数说明（含载具生成器 + 航路点扩展草案）
;***********************************************************************************

;★ 说明 ★
;1) 本文件为配置示例与字段说明文档（不代表代码已全部实现）。
;2) 参数风格与 blocks/*.txt 一致，使用“参数=值”。
;3) 航路点相关字段用于后续导航功能设计对齐。

;===================================================================================
; A. 通用方块字段（已支持）
;===================================================================================

DisplayName = AA Base [RGF]
; 方块显示名

AddDisplayName = zh_CN,基地防空[RGF]
; 多语言显示名：语言代码,名称

BlockID = 34614
; 方块ID（避免冲突）

Material = iron
; 材质：iron/rock/wood/ground/glass/cloth/anvil/water/lava/ice

Hardness = 10000.0
; 硬度（0.0~10000.0）

Resistance = 10000.0
; 抗爆（0.0~10000.0）

StepSound = metal
; 脚步声：metal/stone/wood/gravel/grass/cloth/sand/glass/ladder/anvil

CreativeTab = block
; 创造页：block/heli/plane/tank/vehicle/misc

TextureActive = active
TextureSleep = sleep
TextureError = error
; 三态贴图

StateSyncTick = 5
; 状态同步tick

;===================================================================================
; B. 载具生成器字段（已支持）
;===================================================================================

EnableSpawner = true
; 启用生成器

CheckRadius = 10.0
CheckIntervalTick = 20
; 检测半径与检测间隔

SpawnMode = interval
CooldownTick = 600
; 生成模式与冷却

SpawnYOffset = 1.0
SpawnYawMode = random
SpawnYaw = 0.0
; 生成位置偏移与朝向

DetectPlayers = false
DetectMobs = false
DetectAnimals = false
DetectGunners = false
DetectVehicles = true
; 生成前占位检测

VehiclePool = vehicle:pantsir-s1|vehicle:mim-23
VehicleWeight = 1|1
SpawnVehicleCount = 1
; 载具池、权重、单次生成数量

SpawnGunner = true
GunnerProfile = gunner_faction_aa
GunnerMode = normal
GunnerSeatIndex = 0
GunnerTargetType = 2
GunnerYaw = 0.0
GunnerPitch = 0.0
; 枪手绑定参数

GunnerFactionId = RGF
GunnerFactionName = RGF
AutoCreateFaction = true
AutoCreateFactionColor = 0xD32F2F
; 阵营参数

;===================================================================================
; C. 航路点导航扩展字段（设计草案）
;===================================================================================

EnableWaypointPatrol = true
; 是否启用航路点导航（生成器级）

InitialWaypointId = wp_a
; 初始目标航路点ID（生成器级）

WaypointSelectPolicy = nearest
; 同ID多点选择策略：nearest/first（建议默认nearest）

NavigateSuppressLargeTurn = true
; NAVIGATE阶段抑制地面载具120-180度周期性大转向

HoldAsFreeState = true
; HOLD阶段恢复自由态（不改常态运动和作战）

HoldCountdownTick = 200
; HOLD倒计时

WaypointFailAction = hold
; 导航失败处理：hold/despawn/return

NavigateTimeoutTick = 2400
; NAVIGATE超时保护

NavigateDrivePriority = avoid>navigate>combat
; 驾驶优先级（建议）：紧急避障 > NAVIGATE赶路 > 战斗机动

;--------------------------
; 航路点方块字段（若采用独立航路点方块）
;--------------------------

EnableWaypoint = true
EnableSpawner = false
; 航路点方块需要开启EnableWaypoint，并关闭EnableSpawner

WaypointId = wp_a
; 当前航路点ID

NextWaypointId = wp_b
; 下一航路点ID

PatrolTimeTick = 200
; 到点驻留时长（可与HoldCountdownTick统一）

Radius = 24.0
Height = 40.0
; 空中圆柱判定：水平半径+高度

IsTerminator = false
; 是否终止点

TerminateAction = free
; 到终止点后动作：free/despawn/hold

TerminatorAfterHold = true
; 终止是否在驻留后触发

TextureName = wp_a
; 航路点贴图建议使用TextureName（单贴图）
; TextureActive/TextureSleep/TextureError 对航路点为可选，不写也可

;===================================================================================
; D. 调试相关扩展字段（设计草案）
;===================================================================================

; /mcheli debug waypoint true|false
; 开启后在航路点方块上方渲染：
; “当前点→下一点”
; 示例：wp_a→wp_b

; 右键航路点方块建议显示：
; 当前点: wp_a (x,y,z)
; 下一点: wp_b (x,y,z)

;===================================================================================
; E. 最小样例（生成器 + 航路点）
;===================================================================================

; [生成器]
;DisplayName = Patrol Spawner
;BlockID = 34630
;Material = iron
;Hardness = 30.0
;Resistance = 1000.0
;CreativeTab = block
;EnableSpawner = true
;VehiclePool = tank:t90
;VehicleWeight = 1
;SpawnVehicleCount = 1
;EnableWaypointPatrol = true
;InitialWaypointId = wp_a
;NavigateSuppressLargeTurn = true
;NavigateDrivePriority = avoid>navigate>combat
;HoldAsFreeState = true
;HoldCountdownTick = 200

; [航路点A]
;DisplayName = Waypoint A
;BlockID = 34631
;EnableWaypoint = true
;EnableSpawner = false
;WaypointId = wp_a
;NextWaypointId = wp_b
;Radius = 24.0
;Height = 40.0
;PatrolTimeTick = 200
;IsTerminator = false
;TextureName = wp_a
