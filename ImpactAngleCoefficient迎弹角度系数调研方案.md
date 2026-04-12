# ImpactAngleCoefficient 迎弹角度系数调研方案

## 1. 需求复述

- 新增载具级参数 `ImpactAngleCoefficient=A1,D1,A2,D2,...,An,-1`
- 对当前载具所有碰撞箱生效
- 角度分段按绝对值对称处理（`+A` 与 `-A` 同段）
- 当命中角度超过最后阈值 `An` 时触发跳弹（`-1` 既是跳弹标志也是参数结束位）
- 跳弹时：
  - 本次命中不对载具造成伤害
  - 弹丸发生物理反射（不是原路返回）
  - 显示“跳弹”文案（复用现有碰撞箱命中文案通路）

## 2. 现状代码链路（已确认）

- 碰撞箱命中和伤害倍率来源：
  - `MCH_AircraftBoundingBox.calculateIntercept(...)` 会在命中时写入 `lastBBDamageFactor / lastBBName / lastBBIndex`
  - `MCH_EntityAircraft.attackEntityFrom(...)` 读取上述字段后套用伤害倍率并发送碰撞箱文案包
- 关键位置：
  - `MCH_AircraftBoundingBox.calculateIntercept`：`src/main/java/mcheli/aircraft/MCH_AircraftBoundingBox.java`
  - `MCH_EntityAircraft.attackEntityFrom`：`src/main/java/mcheli/aircraft/MCH_EntityAircraft.java`
  - 碰撞箱定义解析（TXT）：`MCH_AircraftInfo` 的 `BoundingBox/BoundingERABox` 分支：`src/main/java/mcheli/aircraft/MCH_AircraftInfo.java`
  - 命中文案包：`PacketBoundingBoxHit`：`src/main/java/mcheli/network/packets/PacketBoundingBoxHit.java`
  - 客户端命中信息显示：`MCH_ClientCommonTickHandler`：`src/main/java/mcheli/MCH_ClientCommonTickHandler.java`
  - 弹丸更新与反弹基础逻辑：`MCH_EntityBaseBullet.boundBullet / onUpdateCollided / onImpact`：`src/main/java/mcheli/weapon/MCH_EntityBaseBullet.java`

## 3. 可行性结论

- 方案可行，且与现有结构匹配度高
- 需要补齐两类能力：
  1. 迎弹角三维计算（按命中“面法线”而非仅水平/俯仰）
  2. 跳弹控制链路（阻断当前伤害 + 反射弹丸 + 文案）
- 当前实现里 `MCH_AircraftBoundingBox.calculateIntercept` 构造的 `MovingObjectPosition` 的 `sideHit` 固定为 `0`，无法直接区分 6 个面，必须补充“命中法线”求解

## 4. 三维迎弹角计算方案（六面统一）

### 4.1 核心思想

- 每个 OBB（旋转碰撞箱）已有世界坐标下三条局部轴：
  - `axisX / axisY / axisZ`
- 六个面法线就是这三条轴的正负方向：
  - `±axisX, ±axisY, ±axisZ`
- 命中点已知（`hitPos`），弹丸入射方向已知（`dir`）

### 4.2 面法线求解（推荐）

- 先把命中点转到碰撞箱局部坐标：
  - `rel = hitPos - bb.center`
  - `lx = dot(rel, bb.axisX)`，`ly = dot(rel, bb.axisY)`，`lz = dot(rel, bb.axisZ)`
- 计算离哪个面最近：
  - `dx = | |lx| - halfWidth |`
  - `dy = | |ly| - halfHeight |`
  - `dz = | |lz| - halfDepth |`
- 取最小者对应轴，并按符号决定正负面法线
- 这样自然覆盖六个面，且对滚转/俯仰/偏航都成立

### 4.3 入射角定义

- 设 `v` 为弹丸速度方向单位向量（朝向目标），`n` 为外法线单位向量
- 迎弹角定义为：
  - `theta = acos( clamp( dot(-v, n), -1, 1 ) ) * 180/pi`
- 实际分段用 `abs(theta)` 即可满足 `±A` 对称
- 该定义是完整三维夹角，不依赖水平/竖直拆分

## 5. 参数设计与解析建议

### 5.1 数据结构

- 在 `MCH_AircraftInfo` 增加：
  - `List<Float> impactAngleDeg`
  - `List<Float> impactAngleCoeff`
  - `boolean impactAngleEnable`
  - `float impactRicochetStartDeg`
- 解析后要求角度递增

### 5.2 文本语法

- `ImpactAngleCoefficient=0,1,60,0.8,67,0.7,72,0.6,78,0.4,81,-1`
- 约束：
  - 成对读取 `A,D`
  - `D==-1` 表示结束并标记跳弹阈值 `A`
  - 支持最后一个 `A,-1`，此前段用线性分段常值

### 5.3 计算输出

- `theta < A1`：`coeff=D1`
- `Ai <= theta < A(i+1)`：`coeff=D(i+1)`（阶梯）
- `theta >= ricochetStartDeg`：`ricochet=true`

## 6. 跳弹链路落点设计

### 6.1 伤害侧

- 在 `MCH_EntityAircraft.attackEntityFrom(...)` 中，在常规 `damageFactor` 生效前插入迎弹角判定
- 若跳弹：
  - 直接返回 `false`（不扣血）
  - 记录“本次命中为跳弹”的状态给弹丸侧（见 6.2）
  - 发送“跳弹”命中文案包

### 6.2 弹丸侧

- 仅在 `damageSource.getSourceOfDamage()` 是 `MCH_EntityBaseBullet` 时做反射
- 反射公式：
  - `v_reflect = v_in - 2 * dot(v_in, n) * n`
- 速度衰减建议：
  - `v_reflect *= ricochetSpeedFactor`（例如 `0.55~0.8`）
- 位置推出：
  - `pos = hitPos + n * epsilon`（避免再次立即命中同面）

### 6.3 文案复用注意点

- `PacketBoundingBoxHit` 已可承载名字字符串
- 但当前客户端渲染过滤条件是 `hitDamage > 0` 才显示
- 跳弹若伤害为 0，默认不会出现在列表，需要二选一：
  1. 放宽渲染条件（推荐，按 `damageType==ricochet` 也显示）
  2. 发送极小假伤害（不推荐）

## 7. 与“六面坐标系不同”的问题对应

- 不需要为每个面单独写坐标系
- 只要基于 `axisX/Y/Z + center + halfExtents` 做局部投影，就天然统一六个面
- 当前 `MCH_BoundingBox` 已维护这些量，可直接复用

## 8. 风险与边界

- 角点/棱边命中时，最近面可能在两个面间抖动，建议加 `epsilon` 稳定判定
- 高速弹丸可能“穿透一帧多面”，建议以首次命中点为准
- 爆炸类伤害是否参与迎弹角需单独开关（建议仅对弹体直击生效）
- 需要明确主外包围盒（非 extraBoundingBox）是否参与迎弹角；建议默认也参与但法线取 AABB 面法线

## 9. 最小改动实施清单（后续编码时）

- `MCH_AircraftInfo`：新增字段与 `ImpactAngleCoefficient` 解析
- `MCH_AircraftBoundingBox`：补充“命中法线/命中点/命中箱索引”写回 aircraft 临时字段
- `MCH_EntityAircraft.attackEntityFrom`：迎弹角分段缩放 + 跳弹分支 + 文案包发送
- `MCH_EntityBaseBullet`：跳弹时反射并继续飞行（不爆炸、不销毁）
- `PacketBoundingBoxHit` + `MCH_ClientCommonTickHandler`：支持 `damageType=ricochet` 的“跳弹”显示

## 10. 结论

- 该方案在当前代码架构下可实现，且能正确处理你强调的三维入射角与六面差异
- 关键技术点不在“是否能算角”，而在“命中法线提取 + 跳弹时弹丸生命周期改写”
- 若按上面的落点拆分实施，改动可控，且不会破坏现有碰撞箱伤害倍率体系
