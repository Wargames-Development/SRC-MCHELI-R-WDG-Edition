# Gunner 驾驶坦克（可移动+可开火）逻辑调研与实施方案

## 0. 关键前提（必须先明确）

- FMUR 和 MCHR 在代码层面互通非常少，代码结构与实体/控制链路差异很大，几乎不具备直接复用价值。
- 这件事上只能借鉴 FMUR 的“行为树思想与任务分解逻辑”，不能直接搬 FMUR 的行为树代码到 MCHR。
- MCHR 当前没有 FMUR 的 AITask/Selector/Sequence/Parallel/Reverse 那套节点系统，直接移植会造成高耦合和高风险。

## 1. MCHR 现状调研（先看坦克）

### 1.1 坦克“移动”控制链路

1. 客户端输入采集（玩家）  
   `MCH_ClientTankTickHandler.playerControl(...)`  
   - 将按键写入 `MCH_TankPacketPlayerControl`（throttleUp/throttleDown/moveLeft/moveRight 等）

2. 服务端包处理  
   `MCH_TankPacketHandler.onPacket_PlayerControl(...)`  
   - 仅当 `tank.isPilot(player)` 时写入部分控制量（当前明确写入 throttleUp/throttleDown/brake）

3. 载具本体控制  
   `MCH_EntityTank.onUpdate_Control(...)` -> `onUpdate_ControlSub(...)`  
   - 基于 `throttleUp/throttleDown/moveLeft/moveRight` 计算油门、倒车、转向

4. 物理推进  
   `MCH_EntityTank.onUpdate_Server()`  
   - 根据当前油门/姿态生成 `motionX/Y/Z` 并落地到 `moveEntity(...)`

### 1.2 Gunner“开火”控制链路

1. AI 更新入口  
   `MCH_EntityGunner.onUpdate()`

2. 目标选择与射击  
   `MCH_EntityGunner.shotTarget(MCH_EntityAircraft ac)`  
   - 选目标、转炮塔/视角、调用 `ac.useCurrentWeapon(prm)`

3. 载具武器发射  
   `MCH_EntityAircraft.useCurrentWeapon(...)`

### 1.3 结论：为什么现在 Gunner 会开火但不会开车

- Gunner 的射击决策已经存在（`shotTarget`），所以“能开火”是完整链路。
- Gunner 没有“驾驶决策”逻辑，没人去持续设置坦克移动控制量（throttleUp/moveLeft 等），所以“不会移动”。
- 坦克控制逻辑本身在 `MCH_EntityTank` 已具备，只缺“AI 驾驶输入源”。

## 2. 与 FMUR 的可借鉴边界（强调不可复用代码）

## 2.1 可借鉴（逻辑）

- Parallel 思路：移动与火力并行。
- Selector/Sequence 思路：先判断条件，再执行动作，失败回退到次级行为。
- 任务拆分：目标获取、机动、射击、脱离、重选目标。

## 2.2 不可借鉴（代码）

- FMUR 的 `AITask/TaskState/SelectorNode/SequenceNode/ParallelNode/ReverseNode` 代码不能直接搬用。
- FMUR 士兵实体、导航与武器调用接口与 MCHR 坦克实体接口不同，直接复用会造成大面积重构。
- MCHR 现有与 FMUR 的交集主要是 API 级联动（如 `MCH_FMURUtil` 反射调用），不是 AI 架构共用。

## 3. 坦克优先落地方案（Gunner 可移动+可开火）

## 3.1 目标定义

- 仅针对坦克（`MCH_EntityTank`）先做闭环。
- Gunner 坐在坦克驾驶位时，具备：
  - 持续机动能力（前进/转向/必要时减速或倒车）
  - 持续开火能力（复用现有 `shotTarget`）

## 3.2 推荐实施路径（低风险）

### 阶段 A：最小驾驶闭环（FSM 版，不引入新框架）

- 在 `MCH_EntityGunner` 增加坦克驾驶分支（仅服务端生效）：
  - 前置条件：`ac instanceof MCH_EntityTank && ac.isPilot(this)`
  - 行为目标：围绕当前 `targetEntity` 完成“朝向目标机动”
- 输出控制量：直接写入坦克控制字段
  - `tank.throttleUp / tank.throttleDown`
  - `tank.moveLeft / tank.moveRight`
  - 必要时 `tank.setBrake(...)`

建议状态机：
- `APPROACH`：目标较远，前进并对准
- `TRACK`：中距离，低速转向保持火线
- `BRAKE_ALIGN`：角度误差过大时制动转向
- `IDLE_PATROL`：无目标时低速巡航/定向搜索

### 阶段 B：并行化“移动+射击”

- `shotTarget(ac)` 保留为射击子流程。
- 在同一 tick 中先更新驾驶状态，再执行 `shotTarget(ac)`。
- 实现效果等价于 FMUR 的 Parallel（逻辑借鉴，不复用 FMUR 代码）。

### 阶段 C：局部行为树化（可选）

- 在 MCHR 内做极轻量本地节点（仅 Gunner 用）：
  - DriverSelector
  - DriverSequence
  - DriverParallel
- 仅重构 `MCH_EntityGunner` 内部，不侵入全局载具框架。
- 该阶段不是首发必须项，建议在 A/B 稳定后再做。

## 4. 关键实现点（代码落点）

1. `mcheli.mob.MCH_EntityGunner`
- 新增 `updateTankDrive(MCH_EntityTank tank)`  
- 在 `onUpdate()` 中，若骑乘坦克且自己是 pilot，则先跑驾驶控制，再跑 `shotTarget`

2. `mcheli.tank.MCH_EntityTank`
- 尽量不改核心物理，仅复用现有 `onUpdate_ControlSub` 与 `onUpdate_Server`
- 如需，补一个小的 AI 驾驶辅助接口（例如统一清理控制量）

3. 目标与机动策略
- 目标来自现有 `targetEntity`
- 方向误差使用 `wrapAngleTo180_float` 风格处理
- 速度控制只用当前已有字段，不新增复杂动力学参数

## 5. 风险与约束

- 联机一致性：AI 驾驶应只在服务端推进，避免客户端/服务端双写控制量。
- 座位约束：仅在 Gunner 确实处于 pilot 位时启用驾驶。
- 火力冲突：继续遵守 `MCH_WeaponSet.canUse()` 冷却节奏，避免高频射击回归。
- 兼容性：不改玩家输入链路，不影响玩家手操坦克。

## 6. 验证方案（坦克）

1. 空场验证
- Gunner 上驾驶位后，坦克能持续前进与转向

2. 交战验证
- 有目标时，坦克可机动接近并维持开火
- 无目标时，进入巡航/待机，不出现原地抖动

3. 联机验证
- 专用服下 Gunner 驾驶路径稳定，无瞬移/橡皮筋

4. 回归验证
- 玩家驾驶坦克手感无变化
- Gunner 非驾驶位仍按原逻辑仅炮手行为

## 7. 建议执行顺序（本次给你的调研结论）

1. 先做阶段 A（FSM 最小闭环）  
2. 稳定后做阶段 B（移动+开火并行）  
3. 最后再评估阶段 C（局部行为树化）

这条路线最符合当前代码形态，改动集中、风险可控，也最容易从“坦克”扩展到后续其他载具。

## 8. MCHR 武器类型总览（代码定义）

武器创建入口：`mcheli.weapon.MCH_WeaponCreator#createWeapon(...)`

当前代码内已注册的 type：

- machinegun1
- machinegun2
- railgun
- laser
- tvmissile
- torpedo
- cas
- rocket
- asmissile
- aamissile
- atmissile
- bomb
- mkrocket
- dummy
- smoke
- dispenser
- targetingpod

弹体实体创建映射入口：`mcheli.weapon.MCH_WeaponCreator#createEntity(...)`  
其中弹体实体侧主要覆盖：aamissile / atmissile / asmissile / tvmissile / mkrocket / machinegun1 / machinegun2 / railgun / bomb / rocket。

## 9. Gunner 可用武器范围与 Railgun 不开火问题

### 9.1 Gunner 可用武器范围（机制结论）

Gunner 不是走“固定白名单”，而是走“座位可切换武器 + 武器自身 canUse/use + AI 射击窗口判定”：

1. 载具层：`MCH_EntityAircraft#getNextWeaponID(...)` 决定 gunner 当前座位能切到哪些武器  
2. 武器层：`MCH_EntityAircraft#useCurrentWeapon(...) -> MCH_WeaponSet#canUse/use`  
3. AI 层：`MCH_EntityGunner#shotTarget(...)` 只有在目标筛选与姿态收敛满足时才会触发 `ac.useCurrentWeapon(prm)`

因此：

- 常规可用：machinegun1/2、railgun、rocket、bomb、aamissile、atmissile、asmissile、tvmissile、laser
- 场景依赖：torpedo、cas、targetingpod、mkrocket、dispenser
- 实战不可用：dummy、smoke（`shot` 直接返回 false）

### 9.2 “Gunner 用 railgun 不开火”根因判断

初步结论：**大概率不是“gunner 不会蓄力流程”导致**。

原因：

- `MCH_WeaponRailgun#shot(...)` 在服务端分支是直接 `return railGunShot(prm)`，不走客户端蓄力倒计时。
- 蓄力计数 `lockCount` 主要在客户端分支推进，用于音效/显示和本地触发。
- Gunner AI 的 `shotTarget` 在服务端执行，实际调用的是服务端 `useCurrentWeapon` 链路。

所以 railgun 不开火更可能来自：

1. AI 射击窗口未满足（`rotationYaw/rotationPitch` 收敛判定）  
2. 目标筛选或扇区限制未通过（`checkPitch/isInAttackable`）  
3. 当前座位没有切到 railgun 或该座位权限不可用  
4. 武器状态不可用（冷却、弹药、过热）

### 9.3 后续验证建议（最小排查序）

1. 确认 gunner 当前 `getCurrentWeapon` 确实是 railgun  
2. 确认 `MCH_WeaponSet#canUse()` 为 true（countWait=0）  
3. 确认 `targetEntity` 非空且在 `shotTarget` 的姿态收敛窗口内  
4. 若仍不开火，再加临时日志确认是否进入 `MCH_WeaponRailgun#railGunShot(...)` 服务端分支

## 10. 新增需求调研与实现可行性（规避、图标、命名）

### 10.1 驾驶规避逻辑

需求拆分：

1. 所有 gunner 驾驶载具时，若 10 米内有其他载具，触发紧急转向规避防撞。  
2. 仅“友好炮手（TARGET_MONSTER）”额外规避玩家与中立生物（普通动物、村民等，不含怪物）。  
3. 阵营炮手（TARGET_PLAYER）与反导炮手（TARGET_AA_AMMO）不做第 2 条处理。

可行性：**高**。  
原因：规避只需在 `MCH_EntityGunner` 驾驶逻辑中增加近距实体扫描与覆盖控制量，不需要改动底层载具物理。

已实现要点：

- 新增统一规避扫描函数 `findEmergencyAvoidEntity(...)`。
- 坦克驾驶：检测到威胁后覆盖为紧急转向+减速策略。
- 直升机驾驶：检测到威胁后覆盖为机头背离威胁方向+上推规避策略。

### 10.2 防空炮手 item 图标黑块修复

问题根因：

- `MCH_ItemSpawnGunner` 使用多层渲染（base + overlay），`gunner_aa_overlay` 与颜色层叠导致 icon 发黑。

可行性：**高**。  
修复策略：仅对 `targetType == 2`（反导炮手）关闭 overlay 渲染，直接显示 base 图标 `gunner_aa`。

已实现要点：

- `requiresMultipleRenderPasses()` 对 AA 返回 false。
- `getIconFromDamageForRenderPass(...)` 对 AA 只走 base icon。
- `registerIcons(...)` 对 AA 不注册 overlay。

### 10.3 三类炮手名称调整（中英文）

需求命名：

- 友好炮手：`炮手[友好]`
- 阵营炮手：`炮手[阵营]`
- 反导炮手：`炮手[反导]`

英文翻译已设置为：

- `Gunner [Friendly]`
- `Gunner [Faction]`
- `Gunner [Anti-Missile]`

可行性：**高**。  
原因：直接修改 `MCH_MOD#registerItemSpawnGunner` 的 `W_LanguageRegistry` 注册文案即可。

## 11. 敌对炮手（Enemy Gunner）新增需求调研

### 11.1 目标行为定义（按你的需求归纳）

新增“敌对炮手”，行为与友好炮手相反：

1. 敌对炮手驾驶载具时，主要攻击：中立生物 + 玩家 + 友好炮手所驾驶载具。  
2. 对玩家的攻击条件：  
   - 玩家创造模式且难度非困难：不攻击  
   - 困难难度：可攻击玩家  
3. 对怪物不攻击，改为规避（行为形态参考当前友好炮手规避中立生物的逻辑）。  
4. 友好炮手与敌对炮手互为死敌：互相优先攻击对方载具。  
5. 仇恨优先级最高：  
   - 敌对炮手：若同范围内同时有玩家/中立生物/友好炮手，优先友好炮手。  
   - 友好炮手：若同范围内同时有怪物/敌对炮手，优先敌对炮手。  
6. 敌对炮手 item 图标：`gunner_enemy.png`，不走叠层。

### 11.2 实现可行性评估

可行性：**高**，属于现有 gunner 体系的增量扩展，不需要重写载具或武器底层。

原因：

- 当前 gunner 已经由 `targetType` 驱动主要分支，易于增加新枚举值。
- 现有“驾驶规避”逻辑已抽象为函数，可扩展到敌对炮手的怪物规避。
- item 与语言注册集中在 `MCH_MOD#registerItemSpawnGunner`，新增一类成本较低。

### 11.3 建议代码改造点

1. `MCH_EntityGunner`  
- 新增 `TARGET_ENEMY = 3`。  
- 扩展 `canAttackEntity(...)`：加入敌对炮手对玩家创造模式/难度条件判定。  
- 扩展 `updateTargetForWeapon(...)`：加入“友好/敌对炮手互相最高优先级”逻辑。  
- 扩展 `findEmergencyAvoidEntity(...)`：  
  - 友好炮手：保留现有“玩家/中立生物规避”  
  - 敌对炮手：改为“怪物规避”

2. `MCH_ItemSpawnGunner`  
- 支持 `targetType = 3` 敌对炮手。  
- 对敌对炮手 icon 禁用 overlay，仅显示 `gunner_enemy` base 图。

3. `MCH_MOD#registerItemSpawnGunner`  
- 新增 `itemSpawnGunnerEnemy` 注册。  
- 语言名新增：  
  - 中文建议：`炮手[敌对]`  
  - 英文建议：`Gunner [Hostile]`

4. `MCH_EntityGunner#setDead()`  
- 新增 `targetType=3` 掉落对应敌对炮手 item。

### 11.4 仇恨优先级实现建议

为避免“最近距离目标”覆盖死敌优先级，建议在目标选择时采用“两阶段选择”：

1. 先筛选“死敌候选集合”（友好↔敌对）。  
2. 若集合非空，直接在集合内按最近目标选。  
3. 仅在集合为空时，回落到当前默认最近目标策略。

这样可严格满足“死敌优先级最高”的行为定义。

### 11.5 风险点与注意事项

1. 玩家攻击条件的“困难模式”判定需统一服务端口径，避免客户端显示与服务端行为不一致。  
2. 中立生物定义建议使用 `EntityLivingBase && !(IMob)` 作为基线，并排除 gunner/载具相关实体。  
3. 新增 item 后需确保贴图资源存在：`textures/items/gunner_enemy.png`。  
4. 新增类型后需要同步 GUI 提示与投放文案，避免玩家误解 targetType 行为。

### 11.6 本次实现落地状态

已完成：

1. 新增敌对炮手类型（`TARGET_ENEMY = 3`）并接入目标筛选与攻击判定。  
2. 实装友好↔敌对“死敌优先级最高”索敌逻辑（优先于最近距离）。  
3. 实装敌对炮手玩家攻击条件（创造模式仅困难难度可攻击）。  
4. 实装敌对炮手怪物规避与现有载具避撞逻辑融合。  
5. 新增敌对炮手 item 注册与中英文名称（`Gunner [Hostile]` / `炮手[敌对]`）。  
6. 敌对炮手图标使用 `gunner_enemy` 且禁用 overlay 叠层。
