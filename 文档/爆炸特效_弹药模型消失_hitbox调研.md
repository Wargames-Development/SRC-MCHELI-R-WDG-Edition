# 爆炸特效、载具弹药模型发射后消失、HitBox 代码调研

## 1. 调研范围

本次调研聚焦三个问题：

1. 爆炸特效的触发、参数与网络同步链路。  
2. 载具“弹药模型发射后消失”机制的实现位置与判定逻辑。  
3. 碰撞箱（HitBox）相关的数据结构、碰撞判定与伤害系数传递路径。  

---

## 2. 爆炸特效代码链路

### 2.1 服务端爆炸入口（武器/弹体）

主要入口在 `MCH_EntityBaseBullet`：

- 命中后处理：`onImpact(...)`  
  - 根据命中对象（实体/方块）、是否在水中、爆炸威力等，调用 `newExplosion(...)` 或 `playExplosionSound()`。  
- 超时引爆：`onUpdateTimeout()`  
  - 到达 `timeFuse` 或延时条件后引爆。  
- 空爆/近炸：  
  - `onUpdateAirburst()`、`onUpdateProximityFuse()` 中也会调用 `newExplosion(...)`。

对应文件：

- `src/main/java/mcheli/weapon/MCH_EntityBaseBullet.java`

### 2.2 爆炸参数来源（武器配置）

武器配置字段在 `MCH_WeaponInfo` 中定义并解析，例如：

- `explosion`, `explosionBlock`, `flaming`  
- `isFAE`, `explosionType`, `effectYield`, `nukeYield`  
- `trajectoryParticleName`, `disableSmoke`, `smokeSize`  
- `proximityFuseDist`, `rigidityTime`

这些参数最终在 `MCH_EntityBaseBullet.newExplosion(...)` 里打包到 `MCH_ExplosionParam` 并触发。

对应文件：

- `src/main/java/mcheli/weapon/MCH_WeaponInfo.java`  
- `src/main/java/mcheli/weapon/MCH_EntityBaseBullet.java`

### 2.3 爆炸执行与客户端特效

统一爆炸执行类：`MCH_Explosion`

- 服务端：`MCH_Explosion.newExplosion(World, MCH_ExplosionParam)`  
  - 执行 `doExplosionA()` / `doExplosionB(true)`  
  - 构造并广播 `MCH_PacketEffectExplosion`  
- 客户端：根据包参数渲染爆炸粒子  
  - `effectExplosion(...)`（自定义粒子）  
  - `DEF_effectExplosion(...)`（默认爆炸粒子）  
  - `effectExplosionInWater(...)`（水下爆炸）

网络接收入口：

- `MCH_CommonPacketHandler.onPacketEffectExplosion(...)`  
  - 根据 `MCH_Config.DefaultExplosionParticle` 选择自定义或默认效果。

对应文件：

- `src/main/java/mcheli/MCH_Explosion.java`  
- `src/main/java/mcheli/MCH_PacketEffectExplosion.java`  
- `src/main/java/mcheli/MCH_CommonPacketHandler.java`  
- `src/main/java/mcheli/MCH_Config.java`

---

## 3. 载具弹药模型“发射后消失”机制

### 3.1 是否属于“导弹挂载模型”

在载具 info 解析中：

- `AddPartWeaponMissile` 会把该部件标记为 `PartWeapon.isMissile = true`。  
- 普通 `AddPartWeapon` 不会进入该逻辑。

对应文件：

- `src/main/java/mcheli/aircraft/MCH_AircraftInfo.java`

### 3.2 渲染隐藏判定（关键）

在 `MCH_RenderAircraft.renderWeapon(...)` 中：

- 渲染条件是：`!w.isMissile || !ac.isWeaponNotCooldown(ws, weaponIndex)`  
- 含义：  
  - 非导弹部件：始终渲染。  
  - 导弹部件：当该武器槽被标记为“近期发射使用中”时，不渲染模型（表现为弹药发射后消失）。

对应文件：

- `src/main/java/mcheli/aircraft/MCH_RenderAircraft.java`

### 3.3 “近期发射使用中”状态来源

状态由 `MCH_EntityAircraft.useWeaponStat` 维护：

1. `MCH_WeaponSet.use(...)` 里写入 `lastUsedCount[currentWeaponIndex]`。  
2. `MCH_WeaponSet.isUsed(index)` 按 `lastUsedCount` 与 `interval` 判断是否算“使用中”。  
3. `MCH_EntityAircraft.getUsedWeaponStat()` 汇总位图。  
4. `MCH_EntityAircraft.isWeaponNotCooldown(...)` 读取位图供渲染层使用。  
5. `updateWeapons()` 每 tick 同步 `useWeaponStat`（服务端 DataWatcher -> 客户端）。

对应文件：

- `src/main/java/mcheli/weapon/MCH_WeaponSet.java`  
- `src/main/java/mcheli/aircraft/MCH_EntityAircraft.java`  
- `src/main/java/mcheli/aircraft/MCH_RenderAircraft.java`

结论：  
“发射后模型消失”是已有设计，不是渲染 bug，核心由 `PartWeapon.isMissile + useWeaponStat` 共同决定。

---

## 4. HitBox / 碰撞箱代码链路

### 4.1 三类相关结构

1. `MCH_EntityHitBox`  
   - 作为实体存在，主要用于交互/挂载代理（如 pilotSeat）。  
   - 弹丸碰撞过滤里通常会忽略它。  

2. `MCH_BoundingBox`  
   - 旋转包围盒（OBB）数据结构。  
   - 持有中心点、局部轴、旋转角、damageFactor、name 等。  

3. `MCH_AircraftBoundingBox`  
   - 载具整体包围盒包装器（继承 `AxisAlignedBB`）。  
   - 组合机体整体 AABB + 各部件 OBB 判定。  
   - 命中后记录 `ac.lastBBDamageFactor` 与 `ac.lastBBName`。

对应文件：

- `src/main/java/mcheli/aircraft/MCH_EntityHitBox.java`  
- `src/main/java/mcheli/aircraft/MCH_BoundingBox.java`  
- `src/main/java/mcheli/aircraft/MCH_AircraftBoundingBox.java`

### 4.2 载具每 tick 更新附加包围盒

`MCH_EntityAircraft.updateExtraBoundingBox()` 每 tick 调用：

- 使用当前 `pos/yaw/pitch/roll` 更新所有 `extraBoundingBox` 的 OBB 位置与朝向。  
- 命中检测时由 `MCH_AircraftBoundingBox` 执行 OBB-AABB / 射线-OBB 判定。

对应文件：

- `src/main/java/mcheli/aircraft/MCH_EntityAircraft.java`  

### 4.3 弹丸碰撞与伤害系数传递

弹丸碰撞主流程在 `MCH_EntityBaseBullet.onUpdateCollided()`：

- 射线检测实体时使用 `entity.boundingBox.calculateIntercept(...)`。  
- 对载具而言，其 `boundingBox` 已被替换为 `MCH_AircraftBoundingBox`，因此会进入部件 OBB 精细判定。  
- 命中后载具从 `lastBBDamageFactor / lastBBName` 读取命中部位信息，在 `attackEntityFrom(...)` 中参与伤害结算与命中提示包发送。

注意：

- `canBeCollidedEntity(...)` 对 `MCH_EntityHitBox` 返回 `false`，避免子弹直接与交互代理 hitbox 发生命中冲突。  
- 实际受击细分主要依赖载具自身的 `MCH_AircraftBoundingBox + extraBoundingBox`。

对应文件：

- `src/main/java/mcheli/weapon/MCH_EntityBaseBullet.java`  
- `src/main/java/mcheli/aircraft/MCH_EntityAircraft.java`  
- `src/main/java/mcheli/aircraft/MCH_AircraftBoundingBox.java`

---

## 5. 关键结论

1. 爆炸特效是“服务端爆炸计算 + 客户端特效包渲染”双路径，入口集中在弹体 `newExplosion/onImpact`。  
2. 载具导弹模型发射后消失是显式设计：`AddPartWeaponMissile` + `useWeaponStat` 位图驱动渲染隐藏。  
3. 载具 hitbox 并非只靠 `MCH_EntityHitBox`，真正命中细分核心是 `MCH_AircraftBoundingBox` 组合 `extraBoundingBox`（OBB）判定。  
4. 命中部位的伤害倍率通过 `lastBBDamageFactor` 传递到 `attackEntityFrom`，可用于部位化伤害与HUD提示。  

---

## 6. ERA 方案疑问点确认（按最新约束）

### 6.1 模型消失实现原则

- ERA 模型消失将走 **ERA 专用渲染部件链路**。  
- 参考武器挂载件“发射后隐藏”的思路，但 **不直接复用 weapon 逻辑**（不复用 `useWeaponStat/isWeaponNotCooldown`）。  
- 建议新增 ERA 部件定义（例如 `AddPartERA`）并绑定模型 `$ERA0..$ERAn`，渲染时按 ERA 状态位决定显示/隐藏。

### 6.2 mindamage 判定口径

- 采用 **最终伤害值** 判定是否触发 ERA 失效：  
  - `finalDamage > mindamage`：ERA 失效并触发 ERA 爆炸。  
  - `finalDamage <= mindamage`：ERA 保持激活。

### 6.3 ERA 对伤害的作用定位

- ERA **不吸收本次伤害**，仅作为特殊碰撞箱与触发器。  
- 高额免伤效果由配置实现：通常给 ERA box 设置更低 `multiplier`（damageFactor）来模拟。

### 6.4 调试贴图命名

- 采用：  
  - 激活：`bounding_era_active`  
  - 失效：`bounding_era_negative`  
- 原 `nagative` 拼写统一更正为 `negative`。

---

## 7. ERA 实施步骤（不改现有武器隐藏链路）

1. **扩展数据结构**  
   - 在 `MCH_BoundingBox` 扩展 ERA 字段：`isERA`, `eraExplosion`, `eraMinDamage`, `eraActive`。  
   - `copy()` 需完整复制 ERA 字段。

2. **扩展配置解析**  
   - 在 `MCH_AircraftInfo` 新增 `BoundingERABox` 解析分支：  
     `center_x, center_y, center_z, width, height, length, multiplier, explosion, type, name, mindamage`。  
   - 兼容 `DEFAULT/TURRET`，并写入 `extraBoundingBox`。

3. **接入命中判定过滤**  
   - 在 `MCH_AircraftBoundingBox.intersectsWith/calculateIntercept` 遍历时：  
     - 普通 box：按现有逻辑。  
     - ERA box：仅 `eraActive==true` 参与碰撞判定。

4. **接入伤害阶段触发**  
   - 在 `MCH_EntityAircraft.attackEntityFrom(...)` 中，拿到 `bbName` 后定位命中的 ERA box。  
   - 按最终伤害判定：`damage > eraMinDamage` 触发失效：  
     - `eraActive=false`  
     - 触发一次 ERA 爆炸（半径用 `eraExplosion`）。  
   - 不修改本次伤害结算值（符合“ERA 不吸收本次伤害”）。

5. **新增 ERA 渲染链路**  
   - 在 `MCH_AircraftInfo` 增加 ERA 部件列表（如 `partERA`）及解析（如 `AddPartERA`）。  
   - 在 `MCH_RenderAircraft` 增加 `renderERA(...)`：  
     - 通过 ERA 部件顺序与 ERA box 状态位关联；  
     - `eraActive=true` 渲染 `$ERAx`，`false` 不渲染。  
   - 该链路独立于武器挂载件渲染。

6. **调试可视化（TestMode）**  
   - 在 `renderDebugHitBox(...)` 中对 ERA box 分支绘制：  
     - `eraActive=true`：`bounding_era_active`  
     - `eraActive=false`：`bounding_era_negative`  
   - 普通 box 继续使用 `bounding_box`。

7. **状态同步与存档**  
   - 服务端权威维护 ERA 状态；客户端只渲染。  
   - ERA 状态加入 NBT（存档/读档恢复），必要时加同步包或 DataWatcher 字段。

8. **验证清单**  
   - 命中伤害高于/低于 `mindamage` 两组验证。  
   - `DEFAULT` 与 `TURRET` 旋转部位都验证。  
   - ERA 失效后：  
     - 不再碰撞；  
     - 模型隐藏；  
     - 调试贴图切到 `negative`；  
     - 重进世界状态保持一致。  
