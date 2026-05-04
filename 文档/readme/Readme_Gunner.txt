
2026/04/16

;***********************************************************************************
;■ Gunner配置文件 gunners/***.txt（MCH-Reforged）
;***********************************************************************************

;★ 重要 ★
;Gunner配置用于：
; 1) 生成gunner道具（ItemName）
; 2) /addgunner profile <name> 命令
; 3) 方块生成器 GunnerProfile 直连
;文件名建议全小写，避免大小写问题（例如 gunner_faction_aa.txt）。

;========================
; 显示名与道具
;========================

DisplayName = Gunner [Faction AA]
; 默认显示名

AddDisplayName = zh_CN,炮手[阵营][防空]
AddDisplayName = ja_JP,対弾薬迎撃 射撃手[陣営]
; 多语言显示名：语言代码,名称

ItemName = gunner_faction_aa
; 物品注册名
; 贴图key默认取注册名（mcheli:gunner_faction_aa）

;========================
; 基础目标与行为
;========================

TargetType = aa
; 目标类型：
; monster / player / aa / enemy

StupidGunner = false
; 是否固定愚人炮手

StupidGunnerChance = 0.15
; 愚人概率（0.0~1.0）

StupidGunnerChanceByRole = aa:0.15|normal:0.05
; 按角色概率覆盖（可选）

FactionRole = aa
; 阵营角色标签（用于策略区分）

;========================
; 物品贴图与颜色
;========================

ApplyItemColorTint = false
; 是否对item贴图应用颜色染色
; false = 保持原白色贴图（推荐）
; true  = 使用PrimaryColor/SecondaryColor染色

PrimaryColor = 0xC0C000
SecondaryColor = 0x00A0FF
; 仅当ApplyItemColorTint=true时才生效

UseLayeredIcon = false
; 是否启用叠层图标（overlay）

;========================
; 阵营与队伍策略
;========================

TeamMode = player
; player = 继承放置者队伍
; fixed  = 固定队伍
; none   = 无队伍

FixedTeamId = RGF
FixedTeamDisplayName = Red Guard Force
FixedTeamColor = 0xD32F2F
AutoCreateTeam = true
RequirePlayerTeamWhenPvp = true
; fixed/player模式下队伍相关参数

;========================
; 挂载与替换策略
;========================

MountSearchRange = 5.0
; 寻找座位/载具范围（1.0~32.0）

AllowMountAircraft = true
AllowMountSeat = true
; 是否允许挂到载具本体/座位

AllowReplaceExistingGunner = false
; 是否替换已有gunner

;========================
; 索敌范围与武器优先级
;========================

SearchRangeGroundHorizontal = 260
SearchRangeGroundVertical   = 120
SearchRangeAirHorizontal    = 520
SearchRangeAirVertical      = 320
SearchRangeFallbackToConfig = true
; 自定义索敌范围（可不写）

AirWeaponPriority = machinegun1:100|machinegun2:100|railgun:100|laser:80|aamissile:60|tvmissile:40
GroundWeaponPriority = machinegun1:100|machinegun2:100|railgun:100|laser:80|aamissile:60|tvmissile:40
; 武器优先级（值越大优先级越高）
; 当前版本AA模式也会按此优先级执行

;========================
; 战斗行为参数
;========================

AllowLeadForAirTarget = true
; 是否允许对空提前量

StupidAttackSectorScaleGround = 1.0
; 愚人模式对地攻击扇区倍率（1.0~2.0），越大打得越不准

EnableShortBurst = false
ShortBurstFireTick = 14
ShortBurstRestTick = 10
; 短点射参数

;***********************************************************************************
;■ 最小可用模板（复制后改名即可）
;***********************************************************************************
;DisplayName = Gunner [Friendly]
;ItemName = spawn_gunner_vs_monster
;TargetType = monster
;StupidGunner = false
;ApplyItemColorTint = false
;UseLayeredIcon = true
;TeamMode = none
