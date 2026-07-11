# 火箭弹 CCIP（连续计算命中落点 / 投弹圈）调研方案

## 1. 目标

为 `type=rocket` 增加 CCIP 投弹圈能力：

- 持续计算当前发射条件下的预计命中点
- 在玩家视野中渲染落点指示圈（投弹圈）
- 保持与现有弹道参数一致（`Acceleration / Gravity / Drag / SpeedDependsAircraft`）

---

## 2. 结论先行

1. **可实现**：在 Minecraft 1.7.10 + 当前工程结构下可行。
2. **不是从零开始**：项目已有“落点距离预测”能力，可直接升级为“落点坐标预测”。
3. **实现复杂度中等**：核心工作是预测函数产出 `Vec3` 命中点 + 世界渲染层显示。

---

## 3. 现有火箭弹动力学链路（代码定位）

## 3.1 发射初速（基础）

`MCH_WeaponRocket.shot()` 里按发射朝向构造单位方向，再以 `weapon.acceleration` 生成实体初始速度：

- `tX/tY/tZ` 由 `yaw/pitch` 计算
- `new MCH_EntityRocket(..., tX, tY, tZ, ..., acceleration)`

代码位置：

- `src/main/java/mcheli/weapon/MCH_WeaponRocket.java`

## 3.2 acceleration 来源

- `MCH_WeaponCreator` 在武器创建后设置 `weapon.acceleration = info.acceleration`
- `info.acceleration` 由武器配置项 `Acceleration` 解析得到

代码位置：

- `src/main/java/mcheli/weapon/MCH_WeaponCreator.java`
- `src/main/java/mcheli/weapon/MCH_WeaponInfo.java`

## 3.3 每 tick 弹道更新公式

在 `MCH_EntityBaseBullet.onUpdate()` 中，非水下状态每 tick 大致执行：

1. 计算当前速度方向 `dir = motion / |motion|`
2. 可选区间加速（`speedFactorStartTick ~ speedFactorEndTick`）
3. `motionY += gravity`
4. `motionX -= dirX * dragInAir`
5. `motionZ -= dirZ * dragInAir`
6. `pos += motion * accelerationFactor`

代码位置：

- `src/main/java/mcheli/weapon/MCH_EntityBaseBullet.java`

---

## 4. 你特别提到的 SpeedDependsAircraft 调研结论

## 4.1 配置项解析

`SpeedDependsAircraft` 在武器配置里解析到：

- `MCH_WeaponInfo.speedDependsAircraft`（默认 `false`）

代码位置：

- `src/main/java/mcheli/weapon/MCH_WeaponInfo.java`

## 4.2 实际生效点（关键）

不是在 `shot()` 当帧直接叠加，而是在弹体 `onUpdate()` 的服务端分支里，且**只触发一次**：

- 条件：
  - `shootingAircraft instanceof MCH_EntityAircraft`
  - `!speedAddedFromAircraft`
  - `getInfo().speedDependsAircraft == true`
- 计算：
  - `s = sqrt(ac.motionX^2 + ac.motionY^2 + ac.motionZ^2)`（载机速度标量）
  - `acceleration += s`
  - 将当前 `motion` 重新按新的 `acceleration` 归一化放大
  - `speedAddedFromAircraft = true`（防止重复叠加）

代码位置：

- `src/main/java/mcheli/weapon/MCH_EntityBaseBullet.java`

## 4.3 公式含义

在该实现下，初速大小近似为：

- `|V0| = Acceleration + |V_aircraft|`

并且方向仍沿当时弹体的 `motion` 方向（即发射朝向）。

因此你的理解“火箭弹初速 = Acceleration + 当前载机 speed”与现实现一致。

---

## 5. 现有“落点预测”能力（可直接复用）

项目已有用于 HUD 的落点距离预测：

- `MCH_WeaponBase.getLandInDistance(MCH_WeaponParam prm)`
- 通过离散步进 + `rayTraceBlocks` 查找与地形交点

调用链：

- `MCH_EntityAircraft.getLandInDistance()`
- `MCH_WeaponSet.getLandInDistance()`
- `MCH_WeaponBase.getLandInDistance()`

代码位置：

- `src/main/java/mcheli/aircraft/MCH_EntityAircraft.java`
- `src/main/java/mcheli/weapon/MCH_WeaponSet.java`
- `src/main/java/mcheli/weapon/MCH_WeaponBase.java`

---

## 6. CCIP 的推荐实现方案（本项目）

## 6.1 预测层（新增）

在 `MCH_WeaponBase` 新增一个“返回命中点坐标”的函数，例如：

- `Vec3 getPredictedImpactPoint(MCH_WeaponParam prm)`

实现方式：

1. 复用 `getLandInDistance` 的发射向量与步进框架
2. 将返回值从“水平距离”改为“命中点世界坐标（Vec3）”
3. 仿真时纳入：
   - `Acceleration`
   - `Gravity`
   - `DragInAir`
   - `SpeedFactor` 区间加速
   - `SpeedDependsAircraft`（按一次性叠加处理）

## 6.2 渲染层（新增）

新增一个世界渲染器（建议风格参考 GPS 渲染）：

- 使用 `RenderWorldLastEvent`
- 在 predicted impact point 上方渲染一个 billboard 圈/十字
- 资源纹理固定为 `textures/ccip.png`

可参考：

- `src/main/java/mcheli/render/MCH_RenderGPSPosition.java`
- `src/main/java/mcheli/MCH_ClientProxy.java`（事件注册）

## 6.3 显示条件

建议仅在以下条件显示：

1. 玩家在可控载具中
2. 当前武器 `type=rocket`
3. 成功算出命中点

可选开关：

- `MCH_Config` 增加 `EnableRocketCCIP`

---

## 7. 渲染方案对比（GPS vs HUD）

1. **世界空间渲染（推荐）**
   - 优点：直观，目标点“贴地”，和实际场景一致
   - 缺点：受遮挡/地形影响，需要处理深度与可读性
2. **HUD 2D 渲染**
   - 优点：实现更快，状态稳定
   - 缺点：空间感弱，远距离误判感更强

本项目已有成熟世界渲染样例（GPS），所以优先世界渲染。

---

## 8. 技术风险与误差来源

1. **客户端预测 vs 服务端真实轨迹**  
   由于服务端权威，存在轻微偏差是正常的。
2. **SpeedDependsAircraft 时机差异**  
   真实弹体在 `onUpdate()` 才叠加载机速度；预测若在发射前做，要模拟同样时机。
3. **离散步长误差**  
   步进次数/步长过粗会导致落点抖动。
4. **高机动瞬态**  
   载机快速拉升/滚转时，圈会快速移动，需要平滑处理。

---

## 9. 实现难度评估

1. **可实现性**：高
2. **工程复杂度**：中等
3. **联机风险**：低（只做客户端显示，不改伤害与命中判定）
4. **预计工作重点**：
   - 预测函数与真实弹道参数完全对齐
   - 视觉稳定（插值/平滑/可见性）

---

## 10. 最小改动文件建议清单

1. `src/main/java/mcheli/weapon/MCH_WeaponBase.java`
   - 新增 `getPredictedImpactPoint(...)`（或等价函数）
2. `src/main/java/mcheli/aircraft/MCH_EntityAircraft.java`
   - 增加当前武器 CCIP 预测结果缓存（可每 2 tick 更新）
3. `src/main/java/mcheli/render/*`（新增 `MCH_RenderCCIP.java`）
   - 世界渲染圈图标
4. `src/main/java/mcheli/MCH_ClientProxy.java`
   - 注册 CCIP 渲染事件
5. （可选）`src/main/java/mcheli/MCH_Config.java`
   - 可开关配置项

---

## 11. 实施步骤（按开发顺序）

1. **新增预测函数（武器层）**
   - 在 `MCH_WeaponBase` 新增 `getPredictedImpactPoint(MCH_WeaponParam prm)`。
   - 复用 `getLandInDistance` 的离散步进与 `rayTraceBlocks` 流程，返回 `Vec3` 命中点。
   - 参数对齐真实弹道：`Acceleration / Gravity / DragInAir / SpeedFactor / SpeedDependsAircraft`。
2. **接入 SpeedDependsAircraft 计算（预测层）**
   - 当 `speedDependsAircraft=true` 且发射平台为 `MCH_EntityAircraft` 时，预测初速按一次性叠加载机速度标量处理。
   - 与实体逻辑对齐：等效速度幅值 `|V0| = Acceleration + |V_aircraft|`，方向沿发射向量。
3. **在载具层做当前武器预测缓存**
   - 在 `MCH_EntityAircraft` 增加 CCIP 命中点缓存字段（可 2 tick 更新一次）。
   - 仅在当前武器 `type=rocket` 时刷新预测；其余武器清空缓存。
4. **新增 CCIP 渲染器**
   - 新建 `MCH_RenderCCIP`（`RenderWorldLastEvent`）。
   - 读取当前玩家控制载具的 CCIP 命中点并绘制 billboard 圈。
   - 圈贴图固定使用 `ccip.png`。
5. **注册渲染事件**
   - 在 `MCH_ClientProxy.init()` 注册 `MCH_RenderCCIP` 到 `MinecraftForge.EVENT_BUS`。
6. **贴图资源落地**
   - 新增资源文件：`assets/mcheli/textures/ccip.png`。
   - 渲染器内 `ResourceLocation` 使用：`new ResourceLocation("mcheli", "textures/ccip.png")`。
7. **显示条件与保护逻辑**
   - 仅在“玩家在可控载具 + 当前武器为 rocket + 命中点有效”时显示。
   - 当预测失败（无地形交点/超步进）时不显示圈，避免误导。
8. **联机与性能验证**
   - 单机与多人分别验证：圈位置是否稳定、是否与实际落点趋势一致。
   - 检查高机动、高速、低空情况下圈抖动；必要时增加轻量平滑。
