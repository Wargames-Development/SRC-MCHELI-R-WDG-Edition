2026/04/27

;***********************************************************************************
;■ 科技树配置说明（仅包含当前版本已实现参数）
;***********************************************************************************

;★ 重要 ★
;本文仅列出“已实现且生效”的参数。
;未出现在本文的参数（例如 FactionId / Layout / Pos / Premium 等）当前版本默认不生效。

;===============================================================================
; 0) 全局主配置（经济相关）
;    路径: config/mcheli.cfg
;===============================================================================

EconomyKeepOnDeath = true
; 死亡是否保留经济货币（已实现）
; true : 玩家死亡后保留 SL / GE / RP
; false: 玩家死亡后不保留（按重生实体默认值）
; 默认值: true

;===============================================================================
; 1) NPC 配置文件
;    路径: config/mcheli/npc/tech_npc_trader.txt
;===============================================================================

DisplayName = TechQuartermaster
; NPC 头顶显示名（已实现）

TechTreeId = nato_tank,nato_heli,premium_vehicle_demo
; 允许显示的科技树 ID 列表（已实现）
; 分隔符支持: , | ;
; 例如: a,b,c  或  a|b|c  或  a;b;c
; 第一个 ID 会作为默认激活树，其余 ID 仍可在界面中显示分组

Skin = minecraft:textures/entity/steve.png
SkinFallback = mcheli:textures/skins/default_tech_npc.png
; NPC 皮肤与回退皮肤（已实现）
; 贴图加载顺序: Skin -> SkinFallback -> 原版 Steve

EnableVillageSpawn = false
; 是否启用村庄低频刷新（已实现）
; true: 启用  false: 关闭

VillageWeight = 20
; 村庄刷新概率权重（已实现，0~100）
; 每次到达村庄刷新判定时，以该概率尝试刷新（100 表示必定通过概率门控）

MinVillagePopulation = 3
; 村庄最小村民数门槛（已实现）
; 当前村庄村民数量小于该值时，不进行刷新

SpawnCooldownTick = 24000
; 同一村庄的刷新冷却 Tick（已实现）
; 默认 24000 Tick 约等于 20 分钟（20TPS）

MaxPerVillage = 1
; 单个村庄内 Tech NPC 最大数量（已实现）
; 达到上限后，该村庄不会继续刷新

MinPlayerDistance = 16
MaxPlayerDistance = 96
; 玩家距离约束（已实现）
; 需要有玩家处于 [MinPlayerDistance, MaxPlayerDistance] 区间内才允许刷新
; 这样可以避免贴脸刷怪，同时避免离玩家过远区域无意义刷新

;===============================================================================
; 2) 科技树配置文件
;    路径: config/mcheli/tech_tree/*.txt
;===============================================================================

TechTreeId = premium_vehicle_demo
; 科技树唯一 ID（已实现）
; NPC 的 TechTreeId 需要包含该值，树才会在 NPC 科技树界面中出现

DisplayName = Premium Vehicle Tech Demo
; 科技树默认显示名（已实现）

AddDisplayName = zh_CN,高级载具科技树示例
AddDisplayName = en_US,Premium Vehicle Tech Demo
; 科技树多语言显示名（已实现）
; 格式: AddDisplayName = 语言代码,文本

Node = premium_tank_2s38,tank,pac_ifv_t2_2s38,2,0,22000,320,true,,1:5
; 节点定义（已实现）
; 当前实现按固定列索引读取，完整写法建议保留10列：
; Node=NodeId,VehicleType,VehicleName,Tier,RPCost,SLCost,GEPrice,Premium,Prerequisite,Pos
;
; 当前版本“已生效”的列：
; 1) NodeId        -> 节点 ID（唯一）
; 2) VehicleName   -> 载具名（用于发放载具与显示名称解析）
; 3) RPCost        -> RP 消耗
; 4) SLCost        -> SL 消耗
; 5) GEPrice       -> GE 消耗
; 6) Prerequisite  -> 前置节点（支持用 & 或 | 分隔多个节点）
;
; 当前版本根据 RPCost/SLCost/GEPrice 自动决定节点类型：
; - RPCost > 0                       -> RP 研发节点（RP_UNLOCK）
; - GEPrice > 0 且 SLCost > 0        -> 高级解锁节点（GE_UNLOCK）
; - RPCost = 0 且 SLCost > 0 且 GE=0 -> SL 购买节点（SL_PURCHASE）
; - RPCost = 0 且 SLCost = 0 且 GE>0 -> GE 兑换节点（GE_EXCHANGE）

;===============================================================================
; 3) 最小可用示例（高级载具线）
;===============================================================================

; 文件: config/mcheli/tech_tree/premium_vehicle_demo.txt
TechTreeId = premium_vehicle_demo
DisplayName = Premium Vehicle Tech Demo
AddDisplayName = zh_CN,高级载具科技树示例
AddDisplayName = en_US,Premium Vehicle Tech Demo

Node = premium_tank_2s38,tank,pac_ifv_t2_2s38,2,0,22000,320,true,,1:1
Node = premium_tank_bmpt,tank,pac_ifv_t3_bmpt,2,0,18000,260,true,,1:2

;===============================================================================
; 4) 常见问题
;===============================================================================

; Q1: 科技树文件写了但 NPC 打开界面看不到？
; A1: 检查 NPC 配置里的 TechTreeId 是否包含该树的 TechTreeId。

; Q2: 改完配置为什么游戏里不更新？
; A2: 该配置当前不是热加载，改完后需要重启服务端/客户端。

; Q3: Node 一些字段写了没反应？
; A3: 请以本文“已生效列”为准，未列出的字段当前版本不参与逻辑。

; Q4: 开了 EnableVillageSpawn 还是不刷？
; A4: 依次检查：
;     1) 村庄村民数是否达到 MinVillagePopulation
;     2) 村内 NPC 是否已达到 MaxPerVillage
;     3) 玩家距离是否在 Min~Max 区间
;     4) 是否还在 SpawnCooldownTick 冷却期
;     5) VillageWeight 概率是否命中
