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

