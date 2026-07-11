# Explosion 上限与新式破坏机制调研报告（2026-04-15）

## 1. 需求摘要

- 将 `Explosion` 最大值从 `50` 提升到 `200`。
- 新增参数 `IsNewExplosionBreak`，默认 `true`。
- 当 `IsNewExplosionBreak=true` 时，按 `ExplosionBlock`（若未配置则按 `Explosion`）决定“向下破坏层数上限”：
  - `<5`：向下 `0` 层（不破坏爆点下方）
  - `5~15`：向下最多 `1` 层
  - `16~35`：向下最多 `2` 层
  - `36~65`：向下最多 `3` 层
  - `66~100`：向下最多 `4` 层
  - `>100`：向下最多 `5` 层

---

## 2. 可行性结论

- **可行**，并且主要改动集中在：
  - `MCH_WeaponInfo` 参数解析
  - `MCH_Explosion#doExplosionA` 的受影响方块筛选阶段
- 不需要改网络协议（爆炸破坏逻辑是服务端执行），不需要改存档结构。

---

## 3. 现状关键点（代码链路）

## 3.1 Explosion 上限现状

- `MCH_WeaponInfo#loadItemData` 中：
  - `Explosion` 当前是 `toInt(data, 0, 50)`
  - `ExplosionBlock` 当前是 `toInt(data, 0, 100)`
- 现有默认联动：
  - `explosionBlock = -1` 时，在 `checkData()` 内会回填为 `explosion`

这意味着：即使 `ExplosionBlock` 解析上限是 100，只要没写该参数，`explosionBlock` 仍可跟随更大的 `explosion` 值。

## 3.2 方块破坏现状

- `MCH_Explosion#doExplosionA`：
  - 16x16x16 外壳方向射线采样
  - 每条射线按抗爆和固定步进衰减，累计到 `affectedBlockPositions`
- `MCH_Explosion#doExplosionB`：
  - 遍历 `affectedBlockPositions` 实际破坏方块

因此“新式向下分层限制”最合适的接入点是：**在 doExplosionA 收集完受影响方块后，进入 doExplosionB 前做一次过滤**。

---

## 4. 方案落地设计（建议）

## 4.1 参数设计

- 新增 `IsNewExplosionBreak`（默认 `true`）。
- 位置：`MCH_WeaponInfo` 字段 + `loadItemData` 解析。
- 默认值直接在字段初始化为 `true`，保持“默认开启新机制”。

## 4.2 Explosion 最大值提到 200

- 将 `Explosion` 解析改为：`toInt(data, 0, 200)`。
- `ExplosionBlock` 是否同步提上限：
  - 你的规则已包含 `>100` 档位。
  - 即便不改 `ExplosionBlock` 解析上限，未配置时也可因联动拿到 `>100`。
  - **建议仍同步将 `ExplosionBlock` 上限提到 200**，让显式配置也能进入 `>100` 档位，减少认知歧义。

## 4.3 新式破坏筛选算法（核心）

- 计算有效破坏基准值 `breakPower`：
  - 若武器显式配置了 `ExplosionBlock`，用其值；
  - 若未配置，则用 `Explosion`（现有联动已可满足）。
- 由 `breakPower` 映射 `maxDownLayers`（0~5）。
- 设 `baseY = floor(explosionY)`。
- 对 `affectedBlockPositions` 中每个方块 `(x,y,z)`：
  - 若 `y >= baseY`：保留（上方/同层不受限）
  - 若 `y < baseY`：仅当 `(baseY - y) <= maxDownLayers` 才保留，否则剔除

该方法能直接满足你给的“3x3x3 泥土只破坏最上层/前几层”的目标。

---

## 5. 风险与边界

## 5.1 Explosion=200 的性能风险（高）

- 实体伤害检索盒由 `explosionSize*2` 驱动，半径会非常大，实体遍历与遮挡计算显著变重。
- 粒子/特效规模也会放大，客户端掉帧风险上升。
- 结论：功能可行，但建议服务端仅给少量高端武器使用，避免高频触发。

## 5.2 地形表现风险（中）

- 新机制会明显抑制“深坑”，更像“面杀伤、浅破坏”。
- 优点是可控、反地形穿透；缺点是降低“钻地/挖壕”战术价值。

## 5.3 规则感知风险（中）

- 玩家可能感觉“爆炸很大但坑不深”不直观。
- 建议在武器说明中增加提示，例如“浅层面破坏弹头”。

---

## 6. 游戏性意义评估

## 6.1 正向价值

- **反滥炸地形**：减少深坑破坏，降低服务器地形修复压力。
- **战术分工更清晰**：高 `Explosion` 可保持范围压制；`ExplosionBlock` 档位决定“是否掘地”能力。
- **PVP/PVE体验更稳**：降低“被一发打穿多层掩体地基”的极端挫败感。

## 6.2 负向影响

- 对偏“攻坚掩体/打地堡”玩法是削弱。
- 大威力武器的视觉冲击与地形结果可能不一致，需要文案引导。

## 6.3 综合建议

- 该方案适合“多人服稳定性优先、地形保护优先”的定位。
- 若你想保留少数“重型破坏弹”，可单独给其 `IsNewExplosionBreak=false` 作为特例。

---

## 7. 预计改动点清单（类/方法级）

1. `mcheli.weapon.MCH_WeaponInfo`
- 字段：新增 `isNewExplosionBreak = true`
- 解析：新增 `IsNewExplosionBreak`
- 上限：`Explosion` 由 `0~50` 改为 `0~200`
- 可选：`ExplosionBlock` 由 `0~100` 改为 `0~200`

2. `mcheli.MCH_ExplosionParam`
- 新增字段：`isNewExplosionBreak`（默认 `true`）

3. `mcheli.weapon.MCH_EntityBaseBullet`
- 构建 `MCH_ExplosionParam` 的各入口补传 `isNewExplosionBreak`

4. `mcheli.MCH_Explosion#doExplosionA`
- 在 `affected` 收集后增加“向下层数上限过滤”
- 过滤逻辑仅在 `param.isNewExplosionBreak=true` 时生效

---

## 8. 结论

- 该方案在当前代码架构下**可落地**。
- 改动成本中等、风险主要在 `Explosion=200` 的性能与平衡，不在技术实现本身。
- 若目标是“压制地形破坏深度、保留面伤范围”，该方案有明确正向价值。  
