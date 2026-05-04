# MCH-Reforged-nukeesteve 炮手（Gunner）功能调研报告

## 1. 调研范围

- 代码来源仓库：`d:\MCHR\MCH-Reforged-nukeesteve`
- 重点提交区间：`8aa6f802d5d8d837079b45afdfef457b14286c6c...8897c7857b8728d36ac458e1349001a61661d3db`
- 关联分支：`ai炮手`（远端显示为编码后的分支名）

## 2. 核心结论

- 炮手功能主体在提交 `8aa6f80`（提交信息：`ai炮手`）。
- 你给出的 compare 区间 `8aa6f80...8897c78` 主要是清理缓存和 IDE 文件，不是功能代码主体。
- 另一个相近提交 `0fe87c4` 也包含同类炮手改动，位于其他分支链上。

## 3. 炮手功能改动清单

### 3.1 新增类（核心）

- `src/main/java/mcheli/mob/MCH_EntityGunner.java`
- `src/main/java/mcheli/mob/MCH_ItemSpawnGunner.java`
- `src/main/java/mcheli/mob/MCH_GuiSpawnGunner.java`
- `src/main/java/mcheli/mob/MCH_RenderGunner.java`

### 3.2 入口与注册（MCH_MOD）

- 新增/启用 `itemSpawnGunnerVsPlayer`、`itemSpawnGunnerVsMonster` 字段
- 在 `PreInit` 中调用 `registerItemSpawnGunner()`
- 在实体注册中加入 `MCH_EntityGunner`（`"MCH.E.Gunner"`）
- 增加 `registerItemSpawnGunner()` 实现（两种炮手道具）

### 3.3 客户端层对接

- `MCH_ClientProxy` 注册 `MCH_EntityGunner` 渲染器
- `MCH_ClientCommonTickHandler` 新增 `gui_SwnGnr` 并挂入 `guiTicks`

### 3.4 载具武器逻辑兼容

- `MCH_EntityAircraft` 中多处把“仅玩家可操作”条件扩展为“玩家或炮手实体可操作”
- 涉及座位武器可用性、gunner 模式、武器切换/射击判断等

### 3.5 多人/目标相关

- `MCH_WeaponTargetingPod` 调整了用户类型处理（由 `EntityPlayer` 约束转向更通用的 `EntityLivingBase` 配合）
- `MCH_Multiplay` 保持 `spotEntity(EntityLivingBase, ...)` 兼容路径

## 4. 与当前主线仓库对比（d:\MCHR\MCH-Reforged）

- 主线 `MCH_MOD` 中保留了炮手相关注释痕迹，但未启用：
  - 注释掉 `itemSpawnGunnerVsPlayer` / `itemSpawnGunnerVsMonster`
  - 注释掉 `registerItemSpawnGunner()` 整段
- 主线缺少 `src/main/java/mcheli/mob/` 目录下炮手相关类
- 说明主线曾考虑回归该功能，但迁移未完成

## 5. 建议迁移顺序

1. **最小闭环先行**
   - 迁移 `mob` 下 4 个炮手类
   - 恢复 `MCH_MOD` 的炮手物品与实体注册
2. **客户端可视化补齐**
   - 迁移 `MCH_ClientProxy` 渲染注册
   - 迁移 `MCH_ClientCommonTickHandler` 的 `gui_SwnGnr` 挂载
3. **核心行为补齐**
   - 迁移 `MCH_EntityAircraft` 中与 Gunner 相关的操作判断改动
4. **兼容性收尾**
   - 对齐 `MCH_WeaponTargetingPod` 与 `MCH_Multiplay` 的实体类型处理
   - 编译并进行单机/多人回归测试

### 5.1 分阶段迁移计划表

| 阶段 | 目标 | 主要改动文件 | 验收标准 | 风险等级 |
|---|---|---|---|---|
| 阶段 0：基线准备 | 建立可回滚基线，避免混入杂项提交 | `MCH-Reforged` 当前分支、`MCH-Reforged-nukeesteve` 对照目录 | 主线 `clean reobfJar` 通过；确认只迁源码不迁 `.gradle/.idea/eclipse` | 低 |
| 阶段 1：最小闭环 | 炮手实体可生成、可挂载座位、可基础开火 | `mcheli/mob/*`（4类）、`mcheli/MCH_MOD.java` | 游戏内能拿到炮手道具并生成炮手，炮手可挂到载具座位且不崩溃 | 中 |
| 阶段 2：客户端可视化 | 完成炮手渲染与放置辅助GUI | `mcheli/MCH_ClientProxy.java`、`mcheli/MCH_ClientCommonTickHandler.java` | 客户端可见炮手模型；手持炮手道具时出现座位提示GUI | 低 |
| 阶段 3：载具行为融合 | 让 Gunner 参与主线现有武器/座位流程 | `mcheli/aircraft/MCH_EntityAircraft.java`、`mcheli/aircraft/MCH_EntitySeat.java`、`mcheli/weapon/MCH_WeaponSet.java` | 炮手在不同机型（heli/plane/tank/vehicle）下可稳定切武器与射击 | 高 |
| 阶段 4：多人与目标系统 | 修正多人标记、锁定、阵营判定一致性 | `mcheli/multiplay/MCH_Multiplay.java`、`mcheli/weapon/MCH_WeaponTargetingPod.java`、`mcheli/wrapper/W_Reflection.java` | 单机与局域网多人行为一致，无 `ClassCastException` 或阵营误伤异常 | 中 |
| 阶段 5：回归与收口 | 做完整回归并形成可维护补丁集 | 涉及阶段 1~4 的全部改动文件 | 通过构建与实机回归；形成可复用迁移补丁（按阶段可独立回退） | 中 |

### 5.2 阶段 0 执行记录（已完成）

| 检查项 | 结果 | 说明 |
|---|---|---|
| 主线仓库分支与提交 | `master` / `00b6844` | 作为迁移目标基线 |
| 对照仓库分支与提交 | `HEAD(detached)` / `8aa6f80` | 锚定在 `ai炮手` 功能提交点 |
| 主线构建验证 | 已通过 | 执行 `.\gradlew.bat clean reobfJar --rerun-tasks` 返回码为 0 |
| 产物确认 | 已生成 | `build/libs/mcheli-reforged-00b6844-master+00b6844b33-dirty.jar` |
| 杂项文件隔离策略 | 已确认 | 迁移仅处理 `src/main/java` 源码，不迁移 `.gradle/.idea/eclipse` |

阶段 0 结论：基线可回滚、构建链路可用、可进入阶段 1（最小闭环迁移）。

### 5.3 阶段 1 执行记录（已完成）

| 检查项 | 结果 | 说明 |
|---|---|---|
| 新增炮手核心类 | 已完成 | 新增 `mcheli/mob/MCH_EntityGunner.java`、`MCH_ItemSpawnGunner.java`、`MCH_GuiSpawnGunner.java`、`MCH_RenderGunner.java` |
| `MCH_MOD` 物品入口恢复 | 已完成 | 启用 `itemSpawnGunnerVsPlayer`、`itemSpawnGunnerVsMonster` 并恢复 `registerItemSpawnGunner()` |
| `PreInit` 注册流程 | 已完成 | `registerItemRangeFinder()` 后加入 `registerItemSpawnGunner()` 调用 |
| 炮手实体注册 | 已完成 | `registerEntity()` 中加入 `MCH_EntityGunner`（`MCH.E.Gunner`，ID=500） |
| 编译验证 | 已通过 | 执行 `.\gradlew.bat compileJava --no-daemon` 返回码为 0 |
| 重映射验证 | 已通过 | 执行 `.\gradlew.bat reobfJar --no-daemon` 返回码为 0 |
| 产物确认 | 已生成 | `build/libs/mcheli-reforged-00b6844-master+00b6844b33-dirty.jar` |

阶段 1 结论：最小闭环迁移已落地并通过构建，可进入阶段 2（客户端可视化补齐）。

### 5.4 阶段 2 执行记录（已完成）

| 检查项 | 结果 | 说明 |
|---|---|---|
| 客户端渲染注册 | 已完成 | `MCH_ClientProxy.registerRenderer()` 增加 `MCH_EntityGunner -> MCH_RenderGunner` |
| GUI Tick 挂载 | 已完成 | `MCH_ClientCommonTickHandler` 增加 `gui_SwnGnr` 字段、实例化与 `guiTicks` 挂载 |
| 关键文件修改 | 已完成 | `src/main/java/mcheli/MCH_ClientProxy.java`、`src/main/java/mcheli/MCH_ClientCommonTickHandler.java` |
| 编译验证 | 已通过 | 执行 `.\gradlew.bat compileJava --no-daemon` 返回码为 0 |
| 重映射验证 | 已通过 | 执行 `.\gradlew.bat reobfJar --no-daemon` 返回码为 0 |
| 产物确认 | 已生成 | `build/libs/mcheli-reforged-00b6844-master+00b6844b33-dirty.jar` |

阶段 2 结论：客户端可视化接入已完成并通过构建，可进入阶段 3（载具行为融合）。

### 5.5 阶段 3 执行记录（已完成）

| 检查项 | 结果 | 说明 |
|---|---|---|
| `MCH_EntityAircraft` Gunner 融合 | 已完成 | 引入 `MCH_EntityGunner`，将多处“仅 `EntityPlayer`”判断扩展为“`EntityPlayer` 或 `MCH_EntityGunner`” |
| 关键行为点覆盖 | 已完成 | 覆盖 `isCreative`、`onMountPlayerSeat`、`isRidePlayer`、`checkTeam`、`initCurrentWeapon/getCurrentWeaponID/getNextWeaponID`、武器旋转更新流程 |
| 非玩家下绳/下机兼容 | 已完成 | `onUpdate_UnmountCrewRepelling` 与 `unmountCrew` 增加对 `MCH_EntityGunner` 的保护分支 |
| `MCH_EntitySeat` 交互打通 | 已完成 | `interactFirst` 新增 `MCH_ItemSpawnGunner` 转发至载具交互入口 |
| `MCH_WeaponSet` 改动 | 无需改动 | 当前主线 `MCH_WeaponSet` 未包含玩家类型硬编码分支，阶段3未触发必要差异 |
| 编译验证 | 已通过 | 执行 `.\gradlew.bat compileJava --no-daemon` 返回码为 0 |
| 重映射验证 | 已通过 | 执行 `.\gradlew.bat reobfJar --no-daemon` 返回码为 0 |
| 产物确认 | 已生成 | `build/libs/mcheli-reforged-00b6844-master+00b6844b33-dirty.jar` |

阶段 3 结论：载具行为融合已完成并通过构建，可进入阶段 4（多人与目标系统兼容收口）。

### 5.6 阶段 4 执行记录（已完成）

| 检查项 | 结果 | 说明 |
|---|---|---|
| `MCH_WeaponTargetingPod` 用户类型兼容 | 已完成 | `shot()` 中 `spotEntity`/`markPoint` 调用从 `EntityPlayer` 改为 `EntityLivingBase`，并补齐客户端间隔微调 |
| `MCH_Multiplay` 标记链路兼容 | 已完成 | `markPoint` 与 `sendMarkPointToSameTeam` 统一使用 `EntityLivingBase`，移除对 `EntityPlayer` 的强制转换 |
| `W_Reflection` 对齐评估 | 完成评估，无需改动 | 当前主线实现与阶段4目标无冲突，不存在 Gunner 特化依赖 |
| 关键文件修改 | 已完成 | `src/main/java/mcheli/weapon/MCH_WeaponTargetingPod.java`、`src/main/java/mcheli/multiplay/MCH_Multiplay.java` |
| 编译验证 | 已通过 | 执行 `.\gradlew.bat compileJava --no-daemon` 返回码为 0 |
| 重映射验证 | 已通过 | 执行 `.\gradlew.bat reobfJar --no-daemon` 返回码为 0 |
| 产物确认 | 已生成 | `build/libs/mcheli-reforged-00b6844-master+00b6844b33-dirty.jar` |

阶段 4 结论：多人/目标系统兼容收口已完成，可进入阶段 5（回归与收口测试）。

## 6. 风险点

- `MCH_EntityAircraft` 改动面较大，直接整体拷贝冲突风险高
- 目标分支里含有缓存与工程杂项提交，迁移时要仅挑源码文件
- 需要检查主线近年来在武器、锁定、网络包上的改动，避免炮手逻辑覆盖新机制

## 7. 可执行结论

- 可确认：`nukeesteve` 仓库确实实现过可用的 Gunner 功能链路。
- 建议按“分阶段 cherry-pick 代码块”迁移，不要一把全量覆盖。
- 下一步建议执行阶段 5：开展单机/局域网回归与目录版实装验证，形成可回退补丁集。

## 8. Gunner 无视 Delay 问题专项调研与解决方案（仅方案，不改代码）

### 8.1 现象与问题定义

- 现象：AI 炮手（`MCH_EntityGunner`）在可射击状态下接近“每 tick 一次”触发开火。
- 预期：应受武器配置 `Delay` 限制，遵循武器冷却节奏。
- 范围：仅针对 AI 炮手，玩家手动开火链路保持现状。

### 8.2 参数链路确认（Delay 本身无问题）

- `Delay` 定义：`MCH_WeaponInfo.delay`。
- `Delay` 解析：武器配置读取后写入 `delay`。
- 运行时映射：`MCH_WeaponCreator` 将 `info.delay` 赋给 `weapon.interval`。
- 结论：`Delay -> interval` 映射链路正常，不是配置项失效。

### 8.3 根因定位

- Gunner 开火发生在服务端 AI 更新：
  - `MCH_EntityGunner.onUpdate()` -> `shotTarget(...)` -> `ac.useCurrentWeapon(prm)`。
- 武器是否可用由 `MCH_WeaponSet.canUse()` 决定，门控条件是 `countWait == 0`。
- 当前主线 `MCH_WeaponSet.use()` 在成功开火后：
  - 客户端分支会写 `countWait = crtWpn.interval`，并推进 `currentHeat`、弹药、装填等状态。
  - 服务端分支仅处理少量状态，不完整推进 `countWait/currentHeat/ammo/reload`。
- 由于 Gunner 是服务端驱动，服务端冷却状态未完整推进，导致 `canUse()` 频繁为真，表现为高频射击。

### 8.4 与历史实现对照

- 在 `MCH-Reforged-nukeesteve` 对照版本中，`MCH_WeaponSet.use()` 成功开火后会统一设置 `countWait = crtWpn.interval`，不依赖客户端分支。
- 该差异与当前问题现象一致，说明这是一次“客户端预测迁移后，旧服务端 AI 链路未同步演进”的兼容性缺口。

### 8.5 目标方案（采纳原作者建议）

- 设计原则：
  - 玩家射击链路不改，避免影响现有手感和联机行为。
  - 仅对 AI 炮手启用服务端权威冷却/过热/装填状态推进。
- 方案摘要：
  - 在 `MCH_WeaponSet.use()` 成功开火后，增加“AI 炮手服务端状态提交”分支。
  - 分支条件：`!worldObj.isRemote && prm.user instanceof MCH_EntityGunner`。
  - 提交内容：`countWait`、`currentHeat`、`lastUsedCount`、`ammo/reload`、`optionParameter` 等与冷却相关状态。
- 预期效果：
  - Gunner 严格遵循 `Delay` 与过热规则。
  - 玩家维持原有客户端预测链路，不引入额外操作延迟感。

### 8.6 备选方案对比

| 方案 | 说明 | 优点 | 风险 |
|---|---|---|---|
| A（推荐）AI 炮手服务端冷却专用分支 | 仅对 `MCH_EntityGunner` 在服务端补齐状态推进 | 改动小、影响面可控、与当前诉求完全一致 | 维护上存在“玩家/AI 双轨”逻辑 |
| B 全量回归服务端权威 | 玩家与 AI 都统一服务端结算 | 逻辑最统一，长期一致性最好 | 改动面大，可能影响玩家操作手感与现有包同步 |
| C Gunner 侧额外 tick 门控 | 在 `MCH_EntityGunner` 里自建冷却计数 | 修改点集中在 AI 类 | 与武器系统双重冷却，后续维护复杂 |

### 8.7 风险与防护

- 风险1：同一发射事件重复扣弹或重复叠加热量。
  - 防护：AI 服务端分支仅在 `crtWpn.use(prm)` 成功后单次提交状态。
- 风险2：组武器（group）联动节奏异常。
  - 防护：保持 `waitAndReloadByOther(...)` 现有逻辑不变，仅补齐当前武器状态。
- 风险3：多人同步显示与服务器判定不一致。
  - 防护：服务端以 `countWait/currentHeat` 为唯一权威，客户端仅显示同步结果。

### 8.8 验证计划

- 用例1：同武器不同 `Delay`（1/5/20）下 Gunner 射击间隔应明显变化并与配置一致。
- 用例2：高热武器触发过热后，Gunner 停火并按冷却恢复。
- 用例3：弹药耗尽后触发装填，装填窗口内 Gunner 不可射击。
- 用例4：玩家手动射击行为与手感无回归变化。
- 用例5：单机与局域网多人均无“服务端高频射击、客户端显示异常”的分叉。

### 8.9 实施建议

- 按方案 A 先做最小改动验证。
- 若验证通过，再评估是否进入方案 B 的长期重构。
- 保留回退点：仅单文件局部修改，失败可快速回滚。

