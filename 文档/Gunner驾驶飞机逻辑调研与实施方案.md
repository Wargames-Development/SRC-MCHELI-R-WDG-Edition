# Gunner 驾驶飞机（空战与对地）逻辑调研与实施方案

## 0. 目标与约束

- 目标：让 gunner 可驾驶飞机，具备巡航、截击、狗斗、对地攻击、脱离与返航能力。
- 高度层：常态作战高度 100~300 米。
- 作战半径：以“上飞机时记录的原点”为中心，采用**水平距离**（XZ 平面）判定，形状为圆柱，不使用三维直线距离。
- 机头指向：飞机武器强依赖机头朝向，攻击策略必须以“指向控制优先”。
- 版本策略：先调研方案，不改核心功能代码。

## 1. 现有代码能力调研（飞机）

### 1.1 飞机控制主链

1. 角度与操纵输入融合  
`mcheli.plane.MCP_EntityPlane#onUpdateAngles(...)`

2. 油门与转向输入生效  
`mcheli.plane.MCP_EntityPlane#onUpdate_Control(...)`  
`mcheli.plane.MCP_EntityPlane#onUpdate_ControlNotHovering(...)`

3. 飞行动力学推进  
`mcheli.plane.MCP_EntityPlane#onUpdate_Server(...)`（速度、升降、阻尼、限速、落地交互）

### 1.2 可直接复用的控制量

- `throttleUp / throttleDown`
- `moveLeft / moveRight`
- `setRotYaw / setRotPitch / setRotRoll`
- `setCurrentThrottle / addCurrentThrottle`

结论：现有飞机已经具备完整“可操纵物理闭环”，缺的是 gunner 侧 AI 驾驶决策层。

## 2. Gunner 驾驶飞机总体状态机（建议）

建议采用两层状态机：

- 一级（任务态）：`CRUISE` / `INTERCEPT` / `DOGFIGHT` / `GROUND_ATTACK` / `DISENGAGE` / `RTB`
- 二级（机动态）：`STRAIGHT` / `ORBIT` / `YOYO_HIGH` / `BREAK_TURN` / `EXTEND`

切换原则：

- 有空中目标：优先 `INTERCEPT`，进入近距窗口后转 `DOGFIGHT`。
- 有地面目标且空域安全：`GROUND_ATTACK`。
- 血量/弹药/姿态风险过高：`DISENGAGE`。
- 距离约束触发强回归：`RTB`（返航到作战圆柱内）。

### 2.1 状态机时间细分（随机区间）

为避免所有 gunner 行为同步、动作机械化，建议每个关键状态使用“基础时长 + 随机抖动”：

- 通用公式：`duration = base + rand[minJitter, maxJitter]`
- 所有时长单位建议使用 tick（20 tick = 1 秒）

建议区间如下：

1. 索敌阶段（`SEARCH`，可视作 `CRUISE` 的子阶段）  
- 推荐范围：`40~120 tick`（2.0~6.0 秒）  
- 含义：维持巡航与轻机动，周期性重算候选目标  
- 动态修正：  
  - 若最近 5 秒内被攻击：收缩到 `20~60 tick`  
  - 若空域威胁低：放宽到 `80~160 tick`

2. 攻击前专注阶段（`FOCUS`）  
- 推荐范围：`20~50 tick`（1.0~2.5 秒）  
- 含义：以机头对准和火控窗口收敛为首要目标  
- 退出条件：  
  - 达到时长上限；或  
  - `yawErr/pitchErr` 已进入开火窗口并稳定若干 tick（如 8~12 tick）

3. 攻击持续阶段（`ATTACK`）  
- 推荐范围：`80~180 tick`（4.0~9.0 秒）  
- 含义：持续压迫、开火、维持机头指向  
- 动态修正：  
  - 对地攻击可取上半区间（`120~220 tick`）  
  - 对空狗斗可取中区间（`80~160 tick`）  
  - 若超出作战半径 600m：上限压缩到 `60~120 tick`

4. 脱战持续阶段（`DISENGAGE`）  
- 推荐范围：`100~220 tick`（5.0~11.0 秒）  
- 含义：抬升、拉开、重置几何关系并回收到可控空域  
- 动态修正：  
  - 若血量低/弹药低：放宽到 `160~300 tick`  
  - 若仍处于高威胁狗斗：最低不低于 `120 tick`，避免“刚脱离就回头”

5. 返航阶段（`RTB`）  
- 推荐范围：`120~280 tick`（6.0~14.0 秒）  
- 含义：在半径约束触发后优先回归作战圆柱  
- 特殊规则：  
  - `d > 600m` 且非狗斗：直接进入 `RTB`，至少保持 `120 tick`  
  - `d > 600m` 且狗斗：允许短宽限 `40~90 tick` 后强制 `RTB`

### 2.2 时间状态机切换模板（建议）

- `SEARCH(40~120)` → 命中目标后 `FOCUS(20~50)`  
- `FOCUS` 成功对准 → `ATTACK(80~180)`  
- `ATTACK` 到时或姿态/风险不利 → `DISENGAGE(100~220)`  
- `DISENGAGE` 结束且空域恢复 → `SEARCH`  
- 任意阶段若 `d > 600m` 且非狗斗 → `RTB(120~280)`，结束后回 `SEARCH`

## 3. 作战半径（圆柱）分段意愿模型

### 3.1 距离定义（只算水平）

- 原点：`originX, originZ`（上飞机时记录）
- 当前：`planeX, planeZ`
- 水平距离：`d = sqrt((planeX-originX)^2 + (planeZ-originZ)^2)`

### 3.2 你给定的分段需求（精确落地）

- `0~50m`：向外飞意愿**很强烈**
- `50~150m`：向外飞意愿**较强烈**
- `150~450m`：向外/向内意愿**相等**
- `450~600m`：向内飞意愿**强烈**
- `>600m`：除非与敌人搏斗，否则**直接向内飞**

### 3.3 建议量化（可直接映射到控制）

定义径向意愿 `R`：

- `R > 0` 表示向外
- `R < 0` 表示向内
- `|R|` 表示强度

建议分段值：

- `0~50m`: `R = +1.00`
- `50~150m`: `R = +0.65`
- `150~450m`: `R = 0.00`
- `450~600m`: `R = -0.75`
- `>600m`:  
  - 若 `DOGFIGHT` 且目标威胁高：`R = -0.35`（允许短时缠斗）  
  - 否则：`R = -1.00`（强制返航）

将 `R` 映射到目标航向偏置：

- 先计算“朝向原点”方位角 `yawToOrigin`
- 向内基准航向：`yawIn = yawToOrigin`
- 向外基准航向：`yawOut = yawToOrigin + 180°`
- 径向航向 `yawRadial = lerp(yawIn, yawOut, (R+1)/2)`
- 最终期望航向 `yawDesired = blend(yawTactical, yawRadial, wR)`，其中 `wR = clamp(|R|, 0.2, 0.9)`

## 4. 高度控制（100~300m）

建议采用“任务态目标高度 + 阻尼”：

- `CRUISE`: 180~240m
- `INTERCEPT`: 200~280m
- `DOGFIGHT`: 150~280m（允许上下机动）
- `GROUND_ATTACK`: 100~180m（进攻后抬升）
- `DISENGAGE/RTB`: 220~300m

控制策略：

- `altErr = targetAlt - currentAlt`
- 使用限幅俯仰命令：`pitchCmd = clamp(kp*altErr - kd*vertSpeed, -pitchMax, +pitchMax)`
- 防止“拉扯振荡”：为高度目标加入 1~2 秒平滑滤波。

## 5. 攻击策略细化（强调机头指向）

## 5.1 对空攻击（机头指向主导）

### 阶段 A：截击（INTERCEPT）

- 目标：先拿到机头指向窗口，再考虑开火。
- 计算：
  - 方位误差 `yawErr`
  - 俯仰误差 `pitchErr`
  - 目标横向角速率 `losRate`
- 策略：
  - 远距（>350m）：优先拉头，限制大过载滚转
  - 中距（150~350m）：允许侧向偏置切入（offset）
  - 近距（<150m）：根据闭合速度触发 `BREAK_TURN` 或 `YOYO_HIGH`

### 阶段 B：狗斗（DOGFIGHT）

- 动作选择：
  - `YOYO_HIGH`：过冲风险高时减闭合速度
  - `BREAK_TURN`：敌机指向我机头时优先规避
  - `EXTEND`：能量不足时拉开距离再反切
- 开火窗口（建议）：
  - `|yawErr| < 8~12°`
  - `|pitchErr| < 6~10°`
  - `distance` 在武器有效区间内
  - 视线可见 + 武器冷却通过

## 5.2 对地攻击（参考直升机但更快）

- 推荐采用“攻击航线”而非原地盘旋：
  1) `INGRESS`（切入）：降低高度到 120~180m，机头压向目标
  2) `ATTACK_PASS`（攻击航线）：保持机头窗口快速投射
  3) `Egress`（脱离）：抬头爬升到 220m+，横向机动拉开
  4) 再次切入

- 与直升机主要差异：
  - 飞机不适合低速悬停
  - 必须维持较高前向速度
  - 攻击窗口更短，依赖提前对准

## 6. 优先级决策（空对空 vs 空对地）

建议优先级：

1. 近距空中威胁（最高）  
2. 指向窗口已建立的空中目标  
3. 高价值地面目标（AA、载具）  
4. 普通地面目标

动态切换保护：

- 设定最短状态驻留时间（如 40~80 tick）
- 避免每 tick 在空/地目标间抖动切换

### 6.1 索敌半径分离（新增配置建议）

为了满足“对空索敌半径 > 对地索敌半径”，建议将索敌范围拆成独立配置：

- 对空索敌半径（水平圆柱）：`GunnerPlaneSearchRadiusAir = 500`（默认 500m）
- 对地索敌半径（水平圆柱）：`GunnerPlaneSearchRadiusGround = 320`（建议默认 280~350m，可调）

判定方式统一为水平距离：

- `dXZ = sqrt((targetX-planeX)^2 + (targetZ-planeZ)^2)`
- 对空候选判定：`dXZ <= GunnerPlaneSearchRadiusAir`
- 对地候选判定：`dXZ <= GunnerPlaneSearchRadiusGround`

建议补充一个可选高度门限参数，避免过高/过低目标误入：

- `GunnerPlaneSearchAltitudeWindow = 260`（表示目标与本机相对高度差 `|dy| <= 260m`）

策略优先级建议：

- 若对空候选存在且威胁等级达阈值，优先进入 `INTERCEPT/DOGFIGHT`
- 否则才进入对地索敌与 `GROUND_ATTACK`

## 7. 分阶段实施路线（后续开发建议）

### Phase 1（最小闭环）

- 让 gunner 能驱动 `MCP_EntityPlane` 基础巡航
- 完成圆柱作战半径约束（本方案分段）
- 完成高度控制（100~300m）

### Phase 2（战斗闭环）

- 空中目标截击 + 简化狗斗（截击/脱离/再切入）
- 地面目标攻击航线（切入-攻击-脱离）

### Phase 3（机动丰富化）

- 加入 yoyo、break、extend 的动作选择器
- 引入能量状态（速度/高度）约束，避免无效机动

### Phase 4（精调）

- 参数热区调优（阈值、驻留时间、开火角窗口）
- 联机与多机压力测试

## 8. 关键参数建议表（首版）

- 作战半径：600m（水平圆柱）
- 对空索敌半径：500m（水平圆柱，配置项 `GunnerPlaneSearchRadiusAir`）
- 对地索敌半径：320m（水平圆柱，配置项 `GunnerPlaneSearchRadiusGround`）
- 可选高度窗：260m（配置项 `GunnerPlaneSearchAltitudeWindow`）
- 强制返航阈值：>600m
- 高度层：100~300m
- 开火角窗口：Yaw 8~12° / Pitch 6~10°
- 状态驻留：40~80 tick
- 狗斗失稳保护：滚转、俯仰限幅

## 9. 风险与规避

- 风险：飞机速度高，状态切换过于频繁会导致“拉扯”和“绕圈失控”  
  - 规避：驻留时间 + 平滑目标 + 限幅
- 风险：强制返航与当前交战冲突  
  - 规避：`>600m` 时仅对非狗斗态强制回归；狗斗态给予短暂宽限
- 风险：对地攻击时下沉过快  
  - 规避：攻击窗口后强制抬头爬升并进入脱离态

## 10. 本文档对应你的新增要求（映射确认）

- 已按你的分段细化“向外/向内飞意愿”。
- 已改为“仅水平距离”的圆柱作战半径模型。
- 已细化机头指向性攻击策略（对空/对地分流）。
- 本文档为独立文件，可作为后续飞机 gunner 实装蓝图。

## 11. 具体实施步骤（可直接开工）

### Step 1：配置参数接入（先做）

- 在配置层新增飞机 gunner 参数：
  - `GunnerPlaneSearchRadiusAir`（默认 500）
  - `GunnerPlaneSearchRadiusGround`（默认 320）
  - `GunnerPlaneSearchAltitudeWindow`（默认 260，可选）
  - `GunnerPlaneStateSearchMin/Max`
  - `GunnerPlaneStateFocusMin/Max`
  - `GunnerPlaneStateAttackMin/Max`
  - `GunnerPlaneStateDisengageMin/Max`
- 参数命名保持与现有 gunner 范围参数风格一致，避免后续维护混乱。
- 验收：配置文件可生成、可读取、默认值正确。

### Step 2：飞机状态机骨架接入 Gunner

- 在 `MCH_EntityGunner` 新增飞机任务态：
  - `PLANE_SEARCH`
  - `PLANE_FOCUS`
  - `PLANE_ATTACK`
  - `PLANE_DISENGAGE`
  - `PLANE_RTB`
- 增加状态计时字段：
  - `planeState`
  - `planeStateTicks`
  - `planeStateDuration`
- 在 `onUpdate()` 中为 `MCP_EntityPlane` 增加专用分支，与坦克/直升机并列。
- 验收：上飞机后状态可切换，不影响坦克和直升机逻辑。

### Step 3：原点记录与圆柱作战半径

- 上飞机时记录作战原点：
  - `originX`
  - `originZ`
- 距离统一使用水平距离：
  - `dXZ = sqrt((x-originX)^2 + (z-originZ)^2)`
- 按分段意愿模型输出向内/向外偏置（0~50 强外，50~150 较外，150~450 平，450~600 强内，>600 强制回归）。
- 验收：飞行轨迹满足“近原点偏外、远原点偏内”。

### Step 4：对空/对地索敌分离

- 索敌先拆两池：
  - 空中池：`dXZ <= GunnerPlaneSearchRadiusAir` 且可选 `|dy| <= AltWindow`
  - 地面池：`dXZ <= GunnerPlaneSearchRadiusGround`
- 目标优先级：
  1. 近距空中威胁
  2. 已建立指向窗口的空中目标
  3. 高价值地面目标（AA/载具）
  4. 普通地面目标
- 验收：同场景下优先空战，且空中索敌范围明显大于对地。

### Step 5：机头指向窗口与开火门限

- 抽象统一指向窗口判定：
  - `|yawErr|`
  - `|pitchErr|`
  - `distance`
  - `canSee`
  - 武器可用状态
- `FOCUS` 只做“收敛机头”，窗口稳定若干 tick 再进入 `ATTACK`。
- 验收：避免“未对准乱开火”，先对准后攻击。

### Step 6：机动动作分阶段落地

- 第一批动作：
  - `STRAIGHT`（直飞压迫）
  - `ORBIT`（环绕保持）
  - `EXTEND`（脱离拉开）
- 第二批动作（狗斗增强）：
  - `BREAK_TURN`
  - `YOYO_HIGH`
- 验收：动作可复现，且状态切换不抖动。

### Step 7：时间状态机接入随机区间

- 将 `SEARCH/FOCUS/ATTACK/DISENGAGE/RTB` 全部接入随机时长。
- 在风险或威胁条件下动态收缩/放宽时长区间。
- 验收：多机行为不同步，节奏不机械。

### Step 8：联机验证与参数调优

- 单机验证：空中目标、地面目标、混合目标三组。
- 联机验证：多机并发与长时间运行稳定性。
- 热区参数优先调优：
  - 对空/对地索敌半径
  - 高度窗
  - 状态时长区间
  - 开火角窗口
- 通过标准：
  - 可持续巡航
  - 可稳定截击
  - 超半径可回归
  - 对地可完成切入-攻击-脱离。
