# 欧拉角→四元数 全面迁移分析文档

> **状态：仅调研，不做改动。**

---

## 一、当前欧拉角体系全景

### 1.1 欧拉角存储字段

| 字段 | 类型 | 位置 | 含义 |
|---|---|---|---|
| `rotationYaw` | `float` | `Entity` (Minecraft 基类) | MC 原生 yaw |
| `rotationPitch` | `float` | `Entity` (Minecraft 基类) | MC 原生 pitch |
| `rotationRoll` | `float` | `MCH_EntityAircraft` | 载具自定义 roll 角 |
| `prevRotationYaw` | `float` | `Entity` | 上一帧 yaw |
| `prevRotationPitch` | `float` | `Entity` | 上一帧 pitch |
| `prevRotationRoll` | `float` | `MCH_EntityAircraft` | 上一帧 roll |
| `lastRiderYaw` | `float` | `MCH_EntityAircraft` | 炮手/乘员视角 yaw |
| `lastRiderPitch` | `float` | `MCH_EntityAircraft` | 炮手/乘员视角 pitch |
| `prevLastRiderYaw` | `float` | `MCH_EntityAircraft` | 上一帧炮手 yaw |
| `prevLastRiderPitch` | `float` | `MCH_EntityAircraft` | 上一帧炮手 pitch |

### 1.2 欧拉角限制参数（配置文件层）

`MCH_AircraftInfo` 中定义，载具 `.txt` 中配置：

| 参数 | 类型 | 默认值 | 作用 |
|---|---|---|---|
| `minRotationPitch` | `float` | -89.9° | 最小俯仰角（正=上仰） |
| `maxRotationPitch` | `float` | 80° | 最大俯仰角 |
| `minRotationRoll` | `float` | -89.9° | 最小滚转角 |
| `maxRotationRoll` | `float` | 80° | 最大滚转角 |
| `limitRotation` | `boolean` | false | 是否启用姿态限制（设置了任意一个Min/MaxRotation自动为true） |

子类覆盖默认值：

| 类 | minPitch | maxPitch | 说明 |
|---|---|---|---|
| `MCH_HeliInfo` | **-20°** | **20°** | 直升机严格限俯仰 |
| `MCH_VehicleInfo` | **-90°** | **90°** | 车辆全向 |
| `MCH_AircraftInfo`（基类） | **-89.9°** | **80°** | 通用飞机 |

### 1.3 操纵灵敏度参数

| 参数 | 类型 | 默认值 | 含义 |
|---|---|---|---|
| `mobilityYaw` | `float` | 1.0 | 偏航操纵灵敏度系数 |
| `mobilityPitch` | `float` | 1.0 | 俯仰操纵灵敏度系数 |
| `mobilityRoll` | `float` | 1.0 | 滚转操纵灵敏度系数 |
| `mobilityYawOnGround` | `float` | 1.0 | 地面偏航灵敏度（坦克/车辆用） |

### 1.4 运行时旋转限制参数

| 参数 | 作用域 | 位置 |
|---|---|---|
| `playerRotMinPitch` / `playerRotMaxPitch` | 静态全局（客户端） | `MCH_ClientTickHandlerBase` |
| `playerRotLimitPitch` | 静态全局（客户端） | `MCH_ClientTickHandlerBase` |
| `playerRotMinYaw` / `playerRotMaxYaw` | 静态全局（客户端） | `MCH_ClientTickHandlerBase` |
| `playerRotLimitYaw` | 静态全局（客户端） | `MCH_ClientTickHandlerBase` |

这些是**渲染端**限制，在 `setRotLimitPitch()` 中设置，每个载具的 tick handler 在进入时调用。

---

## 二、所有受影响的功能点清单

### 2.1 `MCH_EntityAircraft` — 核心姿态引擎（109 处引用 `getRotYaw/Pitch/Roll`）

**setAngles()** [(L1614-L1717)](file:///d:/MCHR/MCH-Reforged/src/main/java/mcheli/aircraft/MCH_EntityAircraft.java#L1614)：
```
矩阵 Z→X→Y 增量叠加 → MatrixToEuler → limitRotation 双次钳制
```
这是**最核心的迁移点**。此函数中：
- 用 `getAddRotationYawLimit/PitchLimit/RollLimit()` 限制增量
- 用 `getControlRotYaw/Pitch/Roll()` 获取鼠标/键盘操舵增量
- 用 Z→X→Y 矩阵连续乘法累积
- `MatrixToEuler()` 反解后，**两次 `limitRotation` 钳制**（行 1676-1678 和 1693-1697）
- 最后 `setRotYaw(v.y) / setRotPitch(v.x) / setRotRoll(v.z)` 写入

**玩家视角同步** [(L1723-L1741)](file:///d:/MCHR/MCH-Reforged/src/main/java/mcheli/aircraft/MCH_EntityAircraft.java#L1723)：
- `player.rotationYaw/Pitch = this.getRotYaw()/getRotPitch() + fixYaw/Pitch`
- 含 ±360° 的 wrap 处理

**座位旋转** [(L3824)](file:///d:/MCHR/MCH-Reforged/src/main/java/mcheli/aircraft/MCH_EntityAircraft.java#L3824)：
- `pilotSeat.rotationPitch = this.getRotPitch()`
- `seat.rotationPitch/Yaw = this.getRotPitch()/getRotYaw()`

**后坐力模拟** [(L2174-L2178)](file:///d:/MCHR/MCH-Reforged/src/main/java/mcheli/aircraft/MCH_EntityAircraft.java#L2174)：
- `setRotPitch(getRotPitch() + recoil * pitch * partialTicks)`
- `setRotRoll(getRotRoll() + recoil * roll * partialTicks)`

**网络平滑修正** [(L2673-L2681)](file:///d:/MCHR/MCH-Reforged/src/main/java/mcheli/aircraft/MCH_EntityAircraft.java#L2673)：
- 服务端航向角与客户端插值：`setRotYaw(getRotYaw() + yaw/rpinc)`
- `setRotation(getRotYaw(), getRotPitch())`

**坐标变换（大量）**：
- `MCH_Lib.RotVec3(..., -getRotYaw(), -getRotPitch(), -getRotRoll())` → 55 处调用
- `MCH_Lib.RotVec3(..., -getRotYaw(), -getRotPitch())` → 若干处
- 用于：挂载点计算、武器发射点、瞄具方向、爆炸偏移等

### 2.2 `MCH_EntityTank` — 坦克专用旋转

**setAngles()** [(L1466-L1500)](file:///d:/MCHR/MCH-Reforged/src/main/java/mcheli/tank/MCH_EntityTank.java#L1466)：
- 与基类类似，另外限制了 yaw 方向

**玩家视角 pitch 限制** [(L1571)](file:///d:/MCHR/MCH-Reforged/src/main/java/mcheli/tank/MCH_EntityTank.java#L1571)：
- `player.rotationPitch = RNG(player.rotationPitch, playerPitch + minRotationPitch, playerPitch + maxRotationPitch)`

**武器开火 pitch 绝对钳制**（影响 10+ 武器类）：
```java
pitch = MCH_Lib.RNG(pitch, -90.0F, 90.0F);  // 最后防线
```

### 2.3 `MCH_Lib` — 旋转变换工具库

| 函数 | 签名 | 使用数 | 迁移难度 |
|---|---|---|---|
| `Rot2Vec3` | `(yaw, pitch) → Vec3` | 多处 | 低：用 quaternion 代替 |
| `RotVec3` | `(x,y,z, yaw,pitch) → Vec3` | 多处 | 低：用 quaternion 代替 |
| `RotVec3` | `(x,y,z, yaw,pitch,roll) → Vec3` | 多处 | 低：用 quaternion 代替 |
| `_Rot2Vec3` | `(yaw,pitch,roll) → Vec3` | 少量 | 低 |

这些函数的参数全部是欧拉角，改四元数后需要改为接受 `FQuat` 或内部转换。

### 2.4 `MCH_Math` — 数学库

| 函数 | 现有用途 | 迁移后 |
|---|---|---|
| `EulerToMatrix(yaw, pitch, roll)` | `setAngles()` 增量累积 | 可保留用于转换 |
| `MatrixToEuler(m)` | 反解矩阵→角度 | 保留用于惰性查询 |
| `MatTurnZ/X/Y` | 矩阵旋转操作 | 不再需要 |
| `EulerToQuat(yaw, pitch, roll)` | `motionTest()` 测试用 | 直接用于增量构造 |
| `QuatToEuler(q)` | 已有 | 保留 |
| `QuatMult(a, b)` | 已有 | 成为核心累积操作 |
| `QuatNormalize(q)` | 已有 | 每帧归一化 |
| `QuatToMatrix(q)` | 已有 | 供 OpenGL 渲染 |

### 2.5 网络同步

| 途径 | 数据 | 说明 |
|---|---|---|
| **DataWatcher** | index 26 = `serverRoll` (`float`) | 服务端同步滚转到客户端 |
|| yaw/pitch 由 `Entity.setRotation()` 同步（MC 原生机制） ||
| **NBT 存档** | `AcRoll` (float) | 保存载具 roll 角 |
|| `AcLastRYaw`, `AcLastRPitch` | 保存骑乘乘员视角 |
| **EntityInfo 同步** | `rotationYaw`, `rotationPitch` (float×2) | 服务端→客户端实体追踪 |
| **PacketDummy** | 飞机实体基本状态 | 包含实体 rotation（MC 内置字段） |

### 2.6 渲染系统

| 位置 | 使用 | 说明 |
|---|---|---|
| `MCH_RenderAircraft` | `calcRot(yaw, tick)` 插值 | 计算渲染用旋转角 |
|| `calcRotPitch(tick)` 插值 | 俯仰渲染插值 |
|| `renderAddSigns` | 标识牌投影需要 yaw/pitch/roll |
|| `renderDebugHitBox` | 包围盒旋转需要欧拉角 |
|| `renderBoundingBox` | 包围盒位置随载具姿态变换 |
| `MCH_RenderRWR` | 雷达 HUD 需要 direction 向量 | `getDirection()` 基于 player.rotationYaw/Pitch |
| `MCH_RenderMortarRadar` | 迫击炮雷达需要方向向量 | 同样使用 `player.rotationYaw/Pitch` |
| `MCH_RenderVehicle` | 车辆渲染 | `MCH_Lib.RNG(vehicle.lastRiderPitch, info.minRotationPitch, info.maxRotationPitch)` |
| `MCH_RenderAircraft.renderRiddenEntity` | 渲染骑乘实体 | `renderEntityWithPosYaw(..., yaw, pitch, roll, ...)` |

### 2.7 武器系统

所有武器开火均需要从载具 yaw/pitch 计算发射方向：

| 武器类 | 关键代码 |
|---|---|
| `MCH_WeaponMachineGun1/2` | `yaw = player.rotationYaw`, `pitch = RNG(pitch, -90, 90)` |
| `MCH_WeaponRocket` | 同上模式 |
| `MCH_WeaponRailgun` | 同上 |
| `MCH_WeaponLaser` | 同上 |
| `MCH_WeaponMarkerRocket` | 同上 |
| `MCH_WeaponDispenser` | 同上 |
| `MCH_WeaponAAMissile` | 同上 + seat 级别 minPitch/maxPitch |
| `MCH_WeaponATMissile` | 同上 |
| `MCH_WeaponTvMissile` | 同上 |
| `MCH_WeaponASMissile` | 同上 |
| `MCH_WeaponTorpedo` | `MCH_Lib.RotVec3(..., -yaw, -pitch)` 发射方向 |

### 2.8 AI Gunner

`MCH_EntityGunner` 使用 `acInfo.minRotationPitch` / `acInfo.maxRotationPitch` 限制瞄准俯仰角。

### 2.9 客户端限制

| 函数 | 位置 | 逻辑 |
|---|---|---|
| `setRotLimitPitch(min, max, player)` | `MCH_ClientTickHandlerBase` | 设置全局静态 clamp 范围 |
| 坦克/直升机/车辆/固定翼 clientTickHandler | 各自的 `update()` 中调用 `setRotLimitPitch()` | 每帧限制玩家视角 pitch |

---

## 三、迁移路线图

### Phase 1：仅改增量累积（最小改动，~30 行）

**目标**：四元数只用于每帧姿态增量计算，不改变存储格式。

改动处：
1. `MCH_EntityAircraft.setAngles()` — 将矩阵连续乘法段落改为四元数累积：
```java
// 旧：
MCH_Math.MatTurnZ(m, roll_delta);  // ×6
FVector3D v = MCH_Math.MatrixToEuler(m);

// 新：
FQuat deltaQ = EulerToQuat(yaw_delta, pitch_delta, roll_delta);
accumulatedQ = QuatMult(accumulatedQ, deltaQ);
QuatNormalize(accumulatedQ);
FVector3D v = QuatToEuler(accumulatedQ);
```
2. 同样改 `MCH_EntityTank.setAngles()`

**优势**：消除万向节锁风险，不改接口、不改网络、不改存档。

**风险**：`accumulatedQ` 需要持久化在同一帧内（局部变量即可）。

### Phase 2：FQuat 取代 float×3 存储

**目标**：`rotationYaw/Pitch/Roll` 被 `FQuat rotation` 取代。

**改动量**：大（~100 处直接引用）

| 层面 | 改动 |
|---|---|
| 字段替换 | `getRotYaw/Pitch/Roll()` → `getRotQuat()` + 惰性 `getRotYawFromQuat()` 转欧拉角 |
| DataWatcher | 新增 index（如 32-35）存 `qw,qx,qy,qz` 四个 float，或仍保留 26 存欧拉角转换值 |
| NBT | 存 `qw,qx,qy,qz` 四个 float 代替 `AcRoll` |
| `limitRotation` | 不再需要！四元数不存在 gimbal lock。但需要在 `QuatToEuler` 转换后 clamp 武器俯仰 |
| player 视角同步 | `player.rotationYaw/Pitch` 必须仍是欧拉角（MC 强制），需惰性转换 |
| `MCH_Lib.RotVec3()` | 保留欧拉角版本（向后兼容），新加 `RotVec3ByQuat()` |
| 武器开火 pitch 钳制 | `RNG(pitch, -90, 90)` 保留不变——武器方向钳制是物理限制，不是 gimbal lock 问题 |

### Phase 3：MCH_Lib 旋转工具函数升级

**改动**：
- 新增 `RotVec3ByQuat(Vec3, FQuat)` — 直接用四元数变换向量
- 逐步替换所有 `RotVec3(vin, -getRotYaw(), -getRotPitch(), -getRotRoll())` 为四元数版本
- `Rot2Vec3(yaw, pitch)` 保留兼容旧代码

### Phase 4：渲染系统升级

**改动**：
- `MCH_RenderAircraft.calcRot()/calcRotPitch()` → 从 `QuatToEuler` 惰性获取
- `renderDebugHitBox` 中的 `glRotatef(yaw/pitch/roll)` → 用 `GL11.glMultMatrix(quatToFloatBuffer(q))` 直接传四元数给 OpenGL

### Phase 5：网络/存档/同步升级

**改动**：
- `PacketDummy` 或新增 `PacketAircraftRotation` 同步 `FQuat`（4 个 float）
- NBT 读写 `qw,qx,qy,qz`
- `MCH_EntityInfo.rotationYaw/rotationPitch` → 保持不动或新增 `rotationQuatW/X/Y/Z`

---

## 四、不需要改的部分

| 内容 | 原因 |
|---|---|
| 武器 `.txt` 配置参数 | `minPitch/maxPitch` 等是物理约束，与欧拉角/四元数无关 |
| HUD 方向计算（`getDirection`） | 基于 `player.rotationYaw/Pitch`，MC 强制要求 |
| AI Gunner 瞄准 | 瞄准逻辑本身不需要改，仅需限制输出欧拉角 |
| `MCH_RenderRWR` / `MCH_RenderMortarRadar` | 均基于 `player.rotationYaw/Pitch`，不受影响 |
| 配置文件 `MinRotationPitch` 等 | 可在四元数→欧拉角转换后继续做 clamp |

---

## 五、风险与建议

| 风险 | 缓解措施 |
|---|---|
| 四元数覆盖性（q 和 -q 表示相同旋转） | `QuatToEuler` 转换后统一 wrap |
| `setAngles()` 是热路径 | 先做 Phase 1 验证性能，无退化再推进 |
| MC 原生 `Entity.rotationYaw/Pitch` 无法替换 | 保持 `excessRotation` 只存 roll，yaw/pitch 由 QuatToEuler 惰性回填 |
| 网络协议兼容 | 保持 4 个 float 同步，旧客户端可回退到欧拉角 |
| 测试覆盖 | 需回归：所有载具类型飞行/驾驶手感、武器发射方向、BVR 框、雷达显示 |
