# 欧拉角万向节锁与四元数迁移分析

## 1. 梳理：MCH 项目中的旋转架构

### 1.1 当前体系：Yaw→Pitch→Roll 欧拉角

载具的旋转姿态由 3 个角度描述：

| 属性 | 含义 | 存储位置 |
|---|---|---|
| `rotYaw` | 航向角（绕 Y 轴） | `MCH_EntityAircraft` |
| `rotPitch` | 俯仰角（绕 X 轴） | `MCH_EntityAircraft` |
| `rotRoll` | 滚转角（绕 Z 轴） | `MCH_EntityAircraft` |

每帧 `setAngles()` （[MCH_EntityAircraft.java:L1669-L1715](file:///d:/MCHR/MCH-Reforged/src/main/java/mcheli/aircraft/MCH_EntityAircraft.java#L1669-L1715)）通过**矩阵乘法串联旋转**：

```
新姿态矩阵 = (roll增量) × (pitch增量) × (yaw增量) × (当前roll) × (当前pitch) × (当前yaw)
```

具体对应——**旋转顺序固定为 Z→X→Y（欧拉角标准航空顺序）**：

```java
MCH_Math.MatTurnZ(m, roll_delta);   // 绕 Z 轴旋转（增量滚转）
MCH_Math.MatTurnX(m, pitch_delta);  // 绕 X 轴旋转（增量俯仰）
MCH_Math.MatTurnY(m, yaw_delta);    // 绕 Y 轴旋转（增量偏航）
MCH_Math.MatTurnZ(m, cur_roll);     // 累加当前滚转
MCH_Math.MatTurnX(m, cur_pitch);    // 累加当前俯仰
MCH_Math.MatTurnY(m, cur_yaw);      // 累加当前偏航
```

然后用 `MatrixToEuler()` 将矩阵**反解**回欧拉角。

### 1.2 `MatrixToEuler()` 实现细节

[MCH_Math.java:L57-L94](file:///d:/MCHR/MCH-Reforged/src/main/java/mcheli/MCH_Math.java#L57-L94) 关键段：

```java
float b = (float)(-Math.asin(zy));   // pitch = -asin(m[2][1])
float cosB = Cos(b);

if (Math.abs(cosB) >= 1e-4) {
    c = Atan2(zx, zz);               // yaw = atan2(m[2][0], m[2][2])
    a = (float)Math.asin(xy / cosB); // roll = asin(m[0][1] / cosB)
} else {
    c = Atan2(-xz, xx);              // gimbal lock：yaw 退化
    a = 0.0F;                        // roll 强制归零
}
```

**当 `cos(pitch) = 0`**（pitch = ±90°），进入 else 分支，**roll 被强制设为 0.0F**，丧失数据完整性。

### 1.3 `RNG()` 钳制函数

[MCH_Lib.java:L88-L90](file:///d:/MCHR/MCH-Reforged/src/main/java/mcheli/MCH_Lib.java#L88-L90)：

```java
public static float RNG(float a, float min, float max) {
    return a < min ? min : (a > max ? max : a);
}
```

仅是简单的 clamp 函数，不涉及变换逻辑。

---

## 2. MinRotationPitch / MaxRotationPitch 为何不能超过 ±90°

### 2.1 理论根源：万向节锁（Gimbal Lock）

欧拉角旋转系统（按 Yaw→Pitch→Roll 顺序）中：

**当 pitch = ±90° 时，Yaw 轴与 Roll 轴完全重合。**

此时有无穷多组 (yaw, pitch, roll) 对应相同的最终旋转矩阵。例如：

```
(yaw=0,   pitch=90, roll=0)   → 指向正下方
(yaw=30,  pitch=90, roll=-30) → 同样指向正下方
```

从矩阵反解回欧拉角时，`MatrixToEuler()` 的 **else 分支将 roll 强制设为 0.0F**。如果载具原本有非零 roll，数据会"被丢弃"。

### 2.2 代码层面的多重防护

**第一层：默认值避开 ±90°**

| 载具类型 | minRotationPitch | maxRotationPitch |
|---|---|---|
| `MCH_AircraftInfo`（默认） | -89.9° | 80° |
| `MCH_HeliInfo`（直升机） | -20° | 20° |
| `MCH_VehicleInfo`（车辆） | -90° | 90° |

注意：`getMinRotationPitch()` 返回 `-89.9°`，刻意不到 -90°；`getMaxRotationPitch()` 返回 `80°`，留 10° 安全边界。

**第二层：`limitRotation` 钳制**

在 `setAngles()` 中，如果设置了 MinRotationPitch/MaxRotationPitch 会自动开启 `limitRotation = true`，之后**每帧两次**将 pitch clamp：

```java
// 第一次：矩阵反解后立即钳制
v.x = MCH_Lib.RNG(v.x, ai.minRotationPitch, ai.maxRotationPitch);
this.setRotPitch(v.x);

// 第二次：再次确认
v.x = MCH_Lib.RNG(this.getRotPitch(), ai.minRotationPitch, ai.maxRotationPitch);
this.setRotPitch(v.x);
```

**第三层：武器级别的绝对钳制**

所有坦克武器开火时：

```java
pitch = MCH_Lib.RNG(pitch, -90.0F, 90.0F);
```

最后一道防线，确保武器发射矢量不触碰 gimbal lock 边界。

**第四层：仅日志的兜底检测**

```java
if (MathHelper.abs(this.getRotPitch()) > 90.0F) {
    MCH_Lib.DbgLog(true, "MCH_EntityAircraft.setAngles Error:Pitch=%.1f", ...);
}
```

只打印日志，不自动修复。pitch 超过 ±90° 后需要等待下一帧的 `limitRotation` 双次钳制被动恢复。

---

## 3. 受影响的功能全景

| 功能 | 位置 | 限制机制 |
|---|---|---|
| 飞机/直升机飞行姿态 | `setAngles()` 双次钳制 | `minRotationPitch` / `maxRotationPitch` |
| 坦克武器开火方向 | `MCH_WeaponMachineGun1/2`, `MCH_WeaponRocket` 等 10+ 武器类 | `RNG(pitch, -90, 90)` |
| 坦克第一人称视角 | `MCH_EntityTank:L1571` | `playerPitch + minRotationPitch` ~ `playerPitch + maxRotationPitch` |
| AI Gunner 瞄准 | `MCH_EntityGunner:L606` | `minRotationPitch` / `maxRotationPitch` |
| 火控雷达解锁限制 | `MCH_RenderRWR:L2642` 等 | 间接依赖方向向量的合法性 |

---

## 4. MCH_Math 中已有的四元数基础设施

好消息：**四元数库已经存在！**只是没有被核心姿态系统使用。

### 4.1 已有的四元数类型和方法

[MCH_Math.java](file:///d:/MCHR/MCH-Reforged/src/main/java/mcheli/MCH_Math.java)：

```java
// 类型
class FQuat { float w, x, y, z; }    // L695
class FMatrix { float m00..m33; }    // L704

// 四元数 → 矩阵
QuatToMatrix(FQuat)                  // L555

// 矩阵 → 四元数  
MatrixToQuat(FQuat, FMatrix)         // L121

// 欧拉角 → 四元数（通过矩阵中转）
EulerToQuat(yaw, pitch, roll)        // L46
  → EulerToMatrix(yaw, pitch, roll)  // L40
  → MatrixToQuat(q, m)               // L121

// 四元数 → 欧拉角（通过矩阵中转）
QuatToEuler(q)                       // L52
  → QuatToMatrix(q)                  // L555
  → MatrixToEuler(m)                 // L57

// 四元数运算
QuatMult(a, b)                       // 四元数乘法 L525
QuatRotation(q, rad, ax, ay, az)     // 从轴角构造四元数 L582
QuatNormalize(q)                     // 归一化 L110
QuatIdentity(q)                      // 单位四元数 L592
QuatCopy(dst, src)                   // 复制 L596
QuatAdd(out, q)                      // 累加 L538
```

### 4.2 已有工具的问题

1. **欧拉 ↔ 四元数转换都经过矩阵中转**（效率稍低但功能完整）
2. `EulerToQuat()` 中用 `MatrixToQuat()` 转换，后者有 `correctQuat()` 做 NaN/Inf 安全防护
3. `motionTest()` 是唯一直接使用四元数的函数——但仅用于测试，不接入核心循环
4. **`setAngles()` 完全没用四元数**，始终是矩阵→欧拉角回路

---

## 5. 四元数迁移方案（框架级建议）

### 5.1 目标架构

```
每帧增量 → 四元数乘法（累积姿态）
          → QuatToMatrix（供 OpenGL 渲染）
          → （需要欧拉角时）QuatToEuler → 欧拉角（用于 HUD/网络/配置文件）
```

### 5.2 需要修改的核心位置

| 位置 | 当前 | 改为 |
|---|---|---|
| `MCH_EntityAircraft.setAngles()` | `MatTurnZ/X/Y` 增量 × 6 → `MatrixToEuler` | `EulerToQuat(增量)` → `QuatMult` 累积 → `QuatToEuler`（仅当需要） |
| `MCH_EntityTank.setAngles()` | 同上 | 同上 |
| `limitRotation` 钳制 | 在欧拉角空间 clamp | 改为在武器开火时 clamp 最终方向 |
| 网络同步 | 当前同步 `rotYaw/Pitch/Roll` (float × 3) | 可保持不变（网络包兼容），或用 `QuatToEuler` 转换 |
| `getRotYaw/Pitch/Roll()` | 直接返回字段 | 如果改用 `FQuat` 存储，加 `QuatToEuler` 惰性转换 |

### 5.3 优势

|  | 欧拉角（当前） | 四元数（建议） |
|---|---|---|
| Pitch 范围 | 不能到 ±90° | **不受限**，全向旋转 |
| 姿态累积精度 | 矩阵链误差积累 | 正交保持好（归一化即可） |
| 插值（slerp） | 复杂 | **slerp 直接可用** |
| 内存/同步量 | 3 个 float | 4 个 float |
| 对配置文件/用户可见性 | `minRotationPitch` 等可直接理解 | 只需在转换时 clamp |

### 5.4 最小改动方案（推荐先行验证）

**Phase 1：仅改 `setAngles()` 的增量累积**

将旋转增量从矩阵乘法改为四元数累积：

```java
// 旧：矩阵 × 矩阵 → MatrixToEuler
MCH_Math.MatTurnZ(m, roll_delta);
MCH_Math.MatTurnX(m, pitch_delta);
MCH_Math.MatTurnY(m, yaw_delta);
...
FVector3D v = MatrixToEuler(m);

// 新：四元数累积 → QuatToEuler
FQuat incrementalQ = EulerToQuat(yaw_delta, pitch_delta, roll_delta);
accumulatedQ = QuatMult(accumulatedQ, incrementalQ);
QuatNormalize(accumulatedQ);
FVector3D v = QuatToEuler(accumulatedQ);
```

**Phase 2：改为 `FQuat` 持久化存储（替换 `rotYaw/Pitch/Roll` 字段）**

需同步更改网络同步（`DataWatcher` 格式）、实体保存等。如果只改渲染层，Phase 1 足够。

### 5.5 潜在风险

1. **四元数覆盖性**：同一个方向有两个四元数对映（q 和 -q），`QuatToEuler` 可能在不同 tick 得到 ±180° 翻转的 yaw，需要在转换后统一归一
2. **`limitRotation` 语义**：不再需要了——四元数不会 gimbal lock，但武器仍需限制最小值
3. **向后兼容**：如果保留 `rotYaw/Pitch/Roll` 字段并惰性转换，则现有网络协议/存档不动

---

## 6. 总结

| 问题 | 答案 |
|---|---|
| 为什么范围不能超过 ±90°？ | 欧拉角万向节锁——pitch=±90° 时 yaw 和 roll 退化 |
| MCH_Math 有四元数支持吗？ | **有**，`FQuat` + 全套运算函数已存在，只是未被核心姿态系统使用 |
| 改动难度？ | Phase 1（仅改增量累积）约 30 行变动，风险可控 |
