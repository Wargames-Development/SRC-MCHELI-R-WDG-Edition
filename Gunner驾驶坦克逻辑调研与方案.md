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
