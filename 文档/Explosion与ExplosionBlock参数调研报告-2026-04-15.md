# Explosion 与 ExplosionBlock 参数调研报告（2026-04-15）

## 1. 调研目标

- `Explosion` 的最大值是多少？
- `ExplosionBlock` 的最大值是多少？
- 两者对应的“杀伤半径 / 方块破坏半径”大致是多少？
- 方块破坏机制是如何工作的？

---

## 2. 参数定义与最大值

## 2.1 Explosion

- 配置解析：`MCH_WeaponInfo#loadItemData`
- 解析约束：`this.explosion = this.toInt(data, 0, 50);`
- 结论：`Explosion` 取值范围 `0 ~ 50`，最大值是 **50**。

## 2.2 ExplosionBlock

- 配置解析：`MCH_WeaponInfo#loadItemData`
- 解析约束：`this.explosionBlock = this.toInt(data, 0, 100);`
- 结论：`ExplosionBlock` 取值范围 `0 ~ 100`，最大值是 **100**。

## 2.3 默认联动规则

- 构造默认：`explosionBlock = -1`
- 在 `checkData()` 中：若 `explosionBlock < 0`，则 `explosionBlock = explosion`
- 结论：未显式配置 `ExplosionBlock` 时，它会跟随 `Explosion`。

---

## 3. Explosion（实体伤害）半径与伤害模型

## 3.1 作用范围半径（影响判定半径）

`MCH_Explosion#doExplosionA()` 中会先执行：

- `this.explosionSize *= 2.0F`
- 按 `distance / explosionSize <= 1` 判定实体是否进入伤害流程

因此**实体伤害影响半径**近似为：

- `R_damage = 2 * Explosion`

示例：

- `Explosion=10` -> 影响半径约 `20` 格
- `Explosion=50` -> 影响半径约 `100` 格（理论最大）

## 3.2 伤害不是固定“秒杀半径”

普通实体伤害核心（简化）：

- `rDist = distance / explosionSize`
- `atten = (1 - rDist) * density`（其中 `density` 与遮挡有关）
- `damage ~ ((atten^2 + atten)/2) * 8 * explosionSize + 1`

结论：

- “杀伤半径”不是单一常数，取决于：
  - 距离
  - 遮挡（墙体）
  - 实体类型伤害倍率（VsPlayer / VsLiving / VsTank 等）
  - 其他伤害修正（配置、护甲、抗性）
- 只能稳定给出“进入伤害计算的影响半径”=`2*Explosion`。

---

## 4. ExplosionBlock（方块破坏）半径与机制

## 4.1 作用方式（不是简单球形半径）

方块破坏不是“按固定球半径直接清除”，而是：

1. 从爆心向立方体表面方向发射采样射线（16x16x16 外壳点）。
2. 每条射线有初始爆轰强度：`blast = sizeBlock * (0.7 ~ 1.3)`（含随机）。
3. 射线步进（STEP≈0.3），每步衰减固定值与方块抗爆值。
4. `blast > 0` 时将该格标记为受影响方块。
5. 在 `doExplosionB` 阶段真正执行掉落与破坏。

所以 `ExplosionBlock` 对应的是“射线能量预算”，不是严格几何半径。

## 4.2 理论最大破坏外延（近似上界）

在“几乎无阻挡/低抗性”的理想上界中，单条射线可达距离可粗估为：

- `R_block_max_ray ≈ sizeBlock * 1.73`

当 `ExplosionBlock=100` 时，极限上界约可到 `173` 格量级（理论值，实战通常远小于此值）。

> 实战中大量实心方块会迅速消耗爆轰强度，因此真实破坏半径通常明显小于理论上界。

## 4.3 方块破坏生效条件（重要）

即使 `ExplosionBlock > 0`，还要同时满足：

1. `mobGriefing = true`
2. `param.isDestroyBlock = true`（通常由 `getInfo().explosionBlock > 0` 决定）
3. `MCH_Config.Explosion_DestroyBlock = true`
4. 该格最终是非空气方块（`blockId > 0`）

否则只会有爆炸特效/实体伤害，不会破坏方块。

---

## 5. 关键代码位置

- 参数解析与上限：
  - `mcheli.weapon.MCH_WeaponInfo#loadItemData`
  - `Explosion -> toInt(data, 0, 50)`
  - `ExplosionBlock -> toInt(data, 0, 100)`
- 默认联动：
  - `mcheli.weapon.MCH_WeaponInfo#checkData`
- 实体伤害流程：
  - `mcheli.MCH_Explosion#doExplosionA`
- 方块破坏流程：
  - `mcheli.MCH_Explosion#doExplosionA`（收集受影响方块）
  - `mcheli.MCH_Explosion#doExplosionB`（真正破坏）
- 子弹/武器传参入口：
  - `mcheli.weapon.MCH_EntityBaseBullet#newExplosion(...)`

---

## 6. 结论汇总

1. `Explosion` 最大值：**50**（影响半径理论最大约 `100` 格）。  
2. `ExplosionBlock` 最大值：**100**（射线预算上界非常高，理论极限外延可达百格量级）。  
3. “杀伤半径”严格来说不是固定值；可稳定使用 `2*Explosion` 作为“进入伤害计算的范围半径”。  
4. 方块破坏是“射线衰减 + 抗爆阻尼 + 多重开关”机制，不是简单球形清除。  
