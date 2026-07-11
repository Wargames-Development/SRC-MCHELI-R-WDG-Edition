# BVR火控预瞄圈（MachineGun 与 Railgun）调研方案

## 1. 目标

基于现有 BVR 锁框显示，新增一个火控预瞄圈（Lead Circle）用于提示射击提前量。  
当前范围仅支持以下武器类型：

- `machinegun1`
- `machinegun2`
- `railgun`

不对导弹类、火箭类、炸弹类启用该预瞄圈。

---

## 2. 可行性结论

结论：**可实现，且与当前架构契合度高。**

原因：

1. 已有目标位置数据源（BVR/RWR 使用的实体同步快照）可用于估算目标速度向量。  
2. 已有武器发射参数（初速、重力、阻力、速度因子）可用于计算拦截点。  
3. 已有渲染入口（BVR 锁框渲染事件、世界点转屏幕）可直接复用。  

---

## 3. 相关代码定位

### 3.1 BVR目标与渲染入口

- BVR 锁框渲染与目标集合：`mcheli.render.MCH_RenderBVRLockBox`
  - `currentLockedEntities`：当前可锁定显示目标集合
  - `onRenderWorldLast(...)`：渲染入口
  - `worldToScreen(...)`：世界坐标转屏幕坐标

### 3.2 目标位置与速度来源

- 网络同步实体结构：`mcheli.MCH_EntityInfo`
  - `posX/Y/Z`
  - `lastTickPosX/Y/Z`
- 服务端全量快照：`mcheli.MCH_EntityInfoManager`
- 客户端缓存：`mcheli.MCH_EntityInfoClientTracker`

速度估算方式（客户端）：

- `Vt = (pos - lastTickPos) * 20.0`（单位约为 m/s）

### 3.3 武器类型与发射链路

- 武器类型映射与实例化：`mcheli.weapon.MCH_WeaponCreator`
  - `machinegun1` -> `MCH_WeaponMachineGun1`
  - `machinegun2` -> `MCH_WeaponMachineGun2`
  - `railgun` -> `MCH_WeaponRailgun`
- 三类武器实际发射均创建 `MCH_EntityBullet`：
  - `MCH_WeaponMachineGun1#shot`
  - `MCH_WeaponMachineGun2#shot`
  - `MCH_WeaponRailgun#railGunShot`

### 3.4 发射点/姿态与弹道参数

- 发射点与姿态：
  - `MCH_WeaponBase#getShotPos`
  - `MCH_WeaponSet#getPredictedImpactPoint`（姿态、roll、炮口偏移处理可复用）
- 弹道参数来源：
  - `MCH_WeaponInfo` 字段：`acceleration / gravity / dragInAir / speedFactor / speedDependsAircraft`
  - `MCH_WeaponCreator` 会把 `info.acceleration` 写入 `weapon.acceleration`

---

## 4. 三类武器的适配性差异

### 4.1 machinegun1

- 默认 `acceleration=4.0`
- 发射实体：`MCH_EntityBullet`
- 支持 `canister` 散布，若启用散射会导致“单一预瞄点”与实际散布中心有偏差。
- 结论：适配良好，建议预瞄按“中心弹道”计算。

### 4.2 machinegun2

- 默认 `acceleration=4.0`
- 发射实体：`MCH_EntityBullet`
- 可切换爆炸模式（HE），但这不影响飞行初速解算。
- 结论：适配良好。

### 4.3 railgun

- 默认 `acceleration=10.0`
- 发射实体：`MCH_EntityBullet`
- 客户端有蓄力流程（`lockTime`）后才发射。
- 结论：适配良好，但建议在蓄力阶段持续更新预瞄圈，发射瞬间最准确。

---

## 5. 预瞄圈数学模型建议

第一阶段使用“等速拦截解析解”（性能好、工程风险低）：

1. 取发射点 `Ps`、目标点 `Pt`、目标速度 `Vt`、子弹速度标量 `s`。  
2. 解方程：`|Pt + Vt*t - Ps| = s*t`。  
3. 得到最小正根 `t` 后计算 `Plead = Pt + Vt*t`。  
4. 将 `Plead` 投影到屏幕并渲染预瞄圈。

第二阶段再做弹道修正（可选）：

- 将 `gravity / dragInAir / speedFactor / speedDependsAircraft` 纳入迭代求解，提高远距离精度。

---

## 6. 接入点建议（最小改动）

新增独立渲染器（建议）：

- 新建示例类：`mcheli.render.MCH_RenderLeadCircle`
- 注册到 `MCH_ClientProxy.init()` 的事件总线

渲染流程：

1. 仅在玩家乘坐载具且有当前武器时执行。  
2. 判断武器 `info.type` 是否为 `machinegun1/machinegun2/railgun`。  
3. 从 BVR 目标集合中选主目标（建议按“距离准星角度最小 + 距离合规”）。  
4. 解算 `Plead`。  
5. 将 `Plead` 以 2D 圈（屏幕坐标）或 3D billboard（世界坐标）方式绘制。  
6. 预瞄圈贴图固定为 `textures/pre-aim_circle.png`。  
7. 在预瞄圈与 BVR 锁框中心点之间渲染绿色虚线连接。  

---

## 7. 类型限制策略

为避免误启用，建议双重限制：

1. 渲染时按 `info.type` 白名单：
   - `machinegun1`
   - `machinegun2`
   - `railgun`
2. 可选新增配置开关（例如 `EnableLeadCircle`），默认 false，仅这三类武器允许为 true。

---

## 8. 风险与边界

1. BVR实体同步是离散快照，低速/机动剧烈目标会导致速度估算噪声。  
2. 散弹（canister）模式下，圈与弹着分布天然不完全一致。  
3. 预瞄圈是“命中概率提示”而非命中保证，尤其在高机动、姿态突变、网络抖动时。  

---

## 9. 实施顺序建议

1. 做第一版等速拦截预瞄（仅三类型武器）。  
2. 接入主目标选择与显示稳定（低通滤波）。  
3. 再做弹道修正迭代，提高远距离精度。  

---

## 10. 最终结论

在当前系统中，基于 BVR 锁框制作 `machinegun1 / machinegun2 / railgun` 的火控预瞄圈是可行的，  
且可以以较小改动先完成可用版本，再迭代到高精度版本。

---

## 11. 火控锁定按钮规则（CAP）

新增火控级锁定按钮：

1. 按下 `CapsLock`（CAP）时进行切换：  
   - 若当前无火控锁定，则尝试建立锁定。  
   - 若当前已有火控锁定，则直接解除锁定。
2. 建立锁定时的目标筛选条件：  
   - 鼠标指向 `FOV <= 5°`  
   - 距离 `<= 1000m`  
   - 在满足条件的目标中取最近目标
3. 仅当当前武器满足以下条件时可建立锁定：  
   - `enableBVR = true`  
   - `type in {machinegun1, machinegun2, railgun}`
4. 锁定成功后表现：  
   - 目标对应 BVR 锁定框显示为红色  
   - 同时显示该目标对应的预瞄圈
