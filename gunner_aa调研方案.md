# gunner_aa（仅拦截弹药实体）调研方案

## 目标

新增一个 AI gunner：`gunner_aa`，对应物品贴图 `gunner_aa.PNG`，其行为为：

- 不再攻击怪物/玩家
- 仅锁定并射击以下弹药实体：
  - Rocket
  - ASMissile
  - TVMissile
  - ATMissile
  - Bomb
  - MkRocket

当前代码中 `MkRocket` 对应实体是 `MCH_EntityMarkerRocket`（武器类型字符串为 `mkrocket`）。

---

## 现状结论（当前 gunner 类型）

当前只有一个 AI 实体类 `MCH_EntityGunner`，通过 `targetType` 区分两种物品形态：

- `targetType = 0`：对怪物（IMob）
- `targetType = 1`：对敌队玩家

关键位置：

- 物品注册（仅两种）：`spawn_gunner_vs_monster` / `spawn_gunner_vs_player`  
  [MCH_MOD.java](file:///D:/MCHR/MCH-Reforged/src/main/java/mcheli/MCH_MOD.java#L368-L389)
- targetType 字段与放置逻辑  
  [MCH_ItemSpawnGunner.java](file:///D:/MCHR/MCH-Reforged/src/main/java/mcheli/mob/MCH_ItemSpawnGunner.java#L27-L124)
- AI 目标选择/开火（只扫 EntityLivingBase）  
  [MCH_EntityGunner.java](file:///D:/MCHR/MCH-Reforged/src/main/java/mcheli/mob/MCH_EntityGunner.java#L157-L280)
- 死亡返还物品（仅 0/1 两档）  
  [MCH_EntityGunner.java](file:///D:/MCHR/MCH-Reforged/src/main/java/mcheli/mob/MCH_EntityGunner.java#L362-L369)

---

## 为实现 gunner_aa 需要修改哪些代码

## 1) 物品注册与静态字段

文件：`src/main/java/mcheli/MCH_MOD.java`

需要改动：

- 新增静态字段：
  - `itemSpawnGunnerAA`
- 在 `registerItemSpawnGunner()` 中新增第三个物品注册：
  - 例如内部名：`spawn_gunner_aa`
  - `targetType = 2`
  - 语言名可设为 `Gunner AA`

参考现有注册段：  
[MCH_MOD.java:L368-L389](file:///D:/MCHR/MCH-Reforged/src/main/java/mcheli/MCH_MOD.java#L368-L389)

---

## 2) 放置物品逻辑（targetType=2）

文件：`src/main/java/mcheli/mob/MCH_ItemSpawnGunner.java`

需要改动：

- 保持 `targetType` 可取 0/1/2
- 现有“玩家模式必须有队伍”的限制只应作用于 `targetType==1`（当前已是如此）
- 放置后 `gunner.targetType = this.targetType` 无需改结构（已有）

注意点：

- 物品贴图机制来自 `setTexture(name)`，即纹理名跟注册名走；若要使用 `gunner_aa.PNG`，建议让物品注册名和贴图命名一致或在资源层做映射。
- `registerIcons` 还会额外读取一个 `_overlay` 贴图：`<icon>_overlay`  
  [MCH_ItemSpawnGunner.java:L143-L146](file:///D:/MCHR/MCH-Reforged/src/main/java/mcheli/mob/MCH_ItemSpawnGunner.java#L143-L146)

---

## 3) AI 选目标逻辑（核心）

文件：`src/main/java/mcheli/mob/MCH_EntityGunner.java`

当前问题：

- `shotTarget` 搜敌列表固定为 `EntityLivingBase`，只适配怪物/玩家  
  [MCH_EntityGunner.java:L191-L199](file:///D:/MCHR/MCH-Reforged/src/main/java/mcheli/mob/MCH_EntityGunner.java#L191-L199)
- `canAttackEntity` 参数是 `EntityLivingBase`，同样限制了目标类型  
  [MCH_EntityGunner.java:L157-L172](file:///D:/MCHR/MCH-Reforged/src/main/java/mcheli/mob/MCH_EntityGunner.java#L157-L172)

建议改法（最小侵入）：

1. 新增 AA 目标判定方法，例如：
   - `private boolean isAATarget(Entity e)`
2. AA 目标白名单按实体类判断：
   - `MCH_EntityRocket`
   - `MCH_EntityASMissile`
   - `MCH_EntityTvMissile`
   - `MCH_EntityATMissile`
   - `MCH_EntityBomb`
   - `MCH_EntityMarkerRocket`（对应 MkRocket）
3. 在 `shotTarget` 的选目标分支里加 `targetType==2`：
   - 搜索集合改为 `Entity`（不是 `EntityLivingBase`）
   - 用 `isAATarget` 过滤
   - 加入 150m 球形索敌限制（不是 AABB）
   - 加入“离地高度 > 30m”过滤
   - 其余视线/角度/距离逻辑可复用
4. 将依赖 `EntityLivingBase` 的方法签名抽象到 `Entity`（至少在 AA 分支单独实现）
   - 例如 `isInAttackable` / `checkPitch` 若仅用位置坐标，可以做 `Entity` 重载
5. AA 模式射击俯仰角限制：
   - 强制受载具 `MinRotationPitch/MaxRotationPitch` 限制
   - 相关逻辑应放入 `checkPitch`（或其 AA 重载）统一判定

补充：

- 如果希望“严格按武器 Type 字符串”而非类名，还可从 `MCH_EntityBaseBullet#getInfo().type` 判断 `rocket/asmissile/tvmissile/atmissile/bomb/mkrocket`。  
- 但类名白名单更直观，也更不依赖配置字符串大小写。

新增约束对应代码定位：

- 当前搜敌范围是 AABB（水平/垂直分离配置），需要在此处改为球形半径 150：  
  [MCH_EntityGunner.java:L191-L199](file:///D:/MCHR/MCH-Reforged/src/main/java/mcheli/mob/MCH_EntityGunner.java#L191-L199)
- 当前俯仰限制在 `checkPitch`，且只对 `vehicle + pilot` 分支显式检查载具 `min/maxRotationPitch`，AA 需要改为统一生效：  
  [MCH_EntityGunner.java:L282-L309](file:///D:/MCHR/MCH-Reforged/src/main/java/mcheli/mob/MCH_EntityGunner.java#L282-L309)
- 载具俯仰限制字段定义：  
  [MCH_AircraftInfo.java](file:///D:/MCHR/MCH-Reforged/src/main/java/mcheli/aircraft/MCH_AircraftInfo.java#L115-L118)
- 离地高度计算可复用地面块查询工具（推荐在 gunner 中新增 `getHeightAboveGround(Entity e)` 封装）：  
  [MCH_Lib.java:L316-L369](file:///D:/MCHR/MCH-Reforged/src/main/java/mcheli/MCH_Lib.java#L316-L369)

---

## 4) 死亡掉落逻辑

文件：`src/main/java/mcheli/mob/MCH_EntityGunner.java`

当前：

- `targetType==0` 掉 monster 版
- 否则掉 player 版  
  [MCH_EntityGunner.java:L362-L369](file:///D:/MCHR/MCH-Reforged/src/main/java/mcheli/mob/MCH_EntityGunner.java#L362-L369)

需要改动：

- 增加 `targetType==2` 时掉落 `MCH_MOD.itemSpawnGunnerAA`

---

## 5) 资源文件（items 素材）

按你需求新增：

- `gunner_aa.PNG`

结合当前注册与 icon 规则，建议同时准备：

- `gunner_aa.png`（主图）
- `gunner_aa_overlay.png`（叠层）

放在该物品贴图目录（与现有 item 纹理一致的路径）后，确保 `setTexture("gunner_aa")` 可正确命中。

---

## 6) 推荐 targetType 设计

建议统一为：

- `0` = VS_MONSTER
- `1` = VS_PLAYER_ENEMY_TEAM
- `2` = AA_AMMO

可在 `MCH_EntityGunner` 与 `MCH_ItemSpawnGunner` 内加常量，降低魔法数字风险。

---

## 7) 影响面与风险

1. **目标类型从 Living 扩展到 Entity**
   - 需要避免对 `EntityLivingBase` 专属字段/方法的直接调用。
2. **武器可否命中高速弹药**
   - AI 已有预测逻辑，但对高速导弹可能命中率受武器弹速影响。
3. **队伍判定**
   - AA 模式通常不需要队伍门槛，建议只保留在 `targetType==1`。
4. **高度过滤语义**
   - “离地高度>30m”建议按“目标实体当前位置到其正下方最近可碰撞方块”的垂直差计算，避免仅用海拔绝对值。
5. **球形范围性能**
   - 推荐先用 `expand(150,150,150)` 做粗过滤，再用 `distanceSq <= 150*150` 精过滤，兼顾性能与精度。

---

## 8) 验证清单

1. 创造模式拿到 `gunner_aa`，可正常放置到机体/座位。
2. `gunner_aa` 不会攻击怪物与玩家。
3. 当白名单弹药进入范围时，`gunner_aa` 会转向并开火。
4. 非白名单弹药不被锁定。
5. 白名单弹药若离地高度 `<=30m`，不会被锁定。
6. 白名单弹药若超出 150m 球形范围，不会被锁定。
7. 俯仰角超出载具 `MinRotationPitch/MaxRotationPitch` 时，不开火。
8. 拆除/死亡后掉落正确物品（`gunner_aa`）。

---

## 9) 最小改动文件清单

- `src/main/java/mcheli/MCH_MOD.java`
- `src/main/java/mcheli/mob/MCH_ItemSpawnGunner.java`
- `src/main/java/mcheli/mob/MCH_EntityGunner.java`
- 资源目录下新增 `gunner_aa` 贴图（主图与 overlay）
