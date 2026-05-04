# BFMCCore 与 BFMCCore-NMS 联动调研

## 1. 背景

- `BFMCCore` 与 `BFMCCore-NMS` 均为服务端插件项目。
- 两者本身不是对 `MCH-Reforged` 的直接代码迁移目标，而是通过桥接层联动 `mcheli` / `Flans` / `Soldier` 相关能力。

## 2. 目录结构对比

### 2.1 BFMCCore-NMS（桥接层）

目录位置：`d:\MCHR\BFMCCore-NMS`

特点：

- 体量小，职责集中在“跨层桥接”。
- 主要模块：
  - `FlansModAPI`：对 Flans + MCH + Soldier 能力做统一静态封装。
  - `listeners/*`：监听 Forge/MCH 事件并转发到 Bukkit 事件体系。
  - `models/*`：自定义 Bukkit 事件和模型对象（如载具伤害/摧毁事件）。
  - `ForgeBusHack`：把 Bukkit 插件里的监听器挂到 Forge EVENT_BUS。

核心入口：

- `org.bitterorange.flansmodslain.FlansModSlain`
- `org.bitterorange.flansmodslain.FlansModAPI`
- `org.bitterorange.flansmodslain.ForgeBusHack`

### 2.2 BFMCCore（玩法层）

目录位置：`d:\MCHR\BFMCCore`

特点：

- 体量大，职责是“游戏玩法与业务逻辑”。
- 主要模块：
  - `game/*`：战局管理、指挥官系统、地图逻辑、载具点位逻辑。
  - `listener/*`：奖励、冷却、广播、反作弊、补给、载具事件处理。
  - `helper/*`：阵营、载具类型、MCH 辅助方法等。
  - `command/*`：管理命令与调试命令（含 `mch` 命令）。

核心入口：

- `net.tv90.bfmccore.BFMCCore`

## 3. 依赖关系

- `BFMCCore` 在 `plugin.yml` 中声明依赖 `FlansModSlain`。
- `BFMCCore` 的 `pom.xml` 通过 `systemPath` 依赖 `original-FlansModSlain-1.0-SNAPSHOT.jar`。
- `BFMCCore-NMS` 的 `pom.xml` 直接依赖（systemPath）：
  - Forge 1.7.10
  - Flan's Mod Ultimate
  - `mcheli-reforged-1.0.jar`

结论：

- `BFMCCore-NMS` 是 `BFMCCore` 与 Forge/MCH 之间的“中间层插件”。

## 4. MCH 关键联动入口

### 4.1 API调用入口（最关键）

文件：`BFMCCore-NMS/src/main/java/org/bitterorange/flansmodslain/FlansModAPI.java`

关键能力：

- `MCH_API.getAcName(...)`：读取玩家当前 MCH 载具名。
- `MCH_API.mountPilot(...)`：挂载驾驶位。
- `MCH_API.mountFirstEmptySeat(...)`：挂载第一个空座位。
- `MCH_API.spawnAircraftAndMountPlayer(...)`：生成载具并挂载玩家。
- `MCHeliUtil.getWeapons(...)`：读取载具武器列表。
- `MCHeliUtil.isMCHeliAircraft(...)`：判断实体是否 MCH 载具。
- `MCHeliUtil.sendSpotedEntityListToSameTeam(...)`：同队标记（Spot）。
- `MCHeliUtil.isMCHeliUAV(...) / isEnemyUAV(...) / getUavLastControlAircraft(...)`：UAV 相关能力。

### 4.2 MCH 事件桥接入口

文件：

- `BFMCCore-NMS/.../listeners/AircraftDamageListener.java`
- `BFMCCore-NMS/.../listeners/AircraftDestroyListener.java`

桥接方式：

- 监听 `mcheli.event.AircraftDamageEvent` / `AircraftDestoryEvent`
- 转发成 Bukkit 事件：
  - `BukkitVehicleDamageEvent`
  - `BukkitVehicleDestroyEvent`

### 4.3 Forge 总线注册入口

文件：`BFMCCore-NMS/.../ForgeBusHack.java`

作用：

- 通过反射和 dummy container，把插件监听器安全挂入 Forge `EVENT_BUS`。
- 这是 Bukkit 插件能收到 Forge/MCH 事件的关键技术点。

## 5. BFMCCore 对联动事件的消费点

### 5.1 载具伤害/击毁奖励

文件：`BFMCCore/src/main/java/net/tv90/bfmccore/listener/AntiVehicleListener.java`

行为：

- 消费 `BukkitVehicleDamageEvent`：按伤害比例发放奖励。
- 消费 `BukkitVehicleDestroyEvent`：击毁载具发放更高奖励。

### 5.2 击杀与战斗统计

文件：`BFMCCore/src/main/java/net/tv90/bfmccore/SlainListener.java`

行为：

- 消费 `PlayerSlainEvent` / `SoldierDeathEvent`。
- 调用 `FlansModAPI.getPlayerVehicle()`、`getDisplayedVehicleWeapons()` 等生成击杀信息和 HUD。

### 5.3 载具辅助管理

文件：`BFMCCore/src/main/java/net/tv90/bfmccore/helper/MCHHelper.java`

行为：

- 载具类型识别（玩家当前乘坐 MCH 载具）。
- 清理无主 MCH 载具（定时任务会调用）。
- 载具限额与冷却策略映射（按职业/阵营）。

### 5.4 定时清理与维护

文件：`BFMCCore/src/main/java/net/tv90/bfmccore/task/BFMCMainTask.java`

行为：

- 定时广播“清理废弃载具”提示。
- 到达阈值调用 `MCHHelper.clearMCHEntities()` 清理无主载具。

## 6. 典型调用链（简化）

### 6.1 事件上行链路（MCH -> Bukkit玩法）

1. MCH 模组内触发载具事件（Damage/Destroy）。
2. `BFMCCore-NMS` Forge 监听器收到事件。
3. 转成 Bukkit 自定义事件。
4. `BFMCCore` 监听器消费并执行奖励/统计/提示逻辑。

### 6.2 能力下行链路（Bukkit玩法 -> MCH）

1. `BFMCCore` 逻辑层触发载具操作需求（上车、生成、标记等）。
2. 调用 `FlansModAPI`。
3. `FlansModAPI` 进一步调用 `MCH_API` / `MCHeliUtil`。
4. 作用到 MCH 实体/武器/UAV/标记系统。

## 7. 结论

- 这两个项目是“桥接层 + 玩法层”的分层设计，不是把 MCH 代码迁移到插件。
- 与 `MCH-Reforged` 相关联动最关键的地方集中在：
  - `BFMCCore-NMS` 的 `FlansModAPI` 与 Forge 监听器；
  - `BFMCCore` 对这些桥接能力的消费逻辑（奖励、HUD、载具管控）。
- 若要复现或重建联动，优先复刻的不是全量玩法，而是：
  1. Forge 事件桥接；
  2. `MCH_API/MCHeliUtil` 的稳定调用封装；
  3. Bukkit 侧事件消费规范。

## 8. 载具清理规则改造（新增需求）

目标：

- 在现有“废弃载具清理”基础上增加两条保护规则：
  - 载具上存在 gunner 时不清理；
  - 载具 5m 内存在玩家时不清理。

可行性结论：

- 可行，且改动集中在 `BFMCCore`，不需要改 `MCH-Reforged` 本体。
- 推荐改造点：
  - `BFMCCore/src/main/java/net/tv90/bfmccore/helper/MCHHelper.java`
  - 触发入口仍复用：
    - `BFMCMainTask` 定时清理
    - `/mch clear` 手动清理

### 8.1 建议实现方式

新增保护判定函数（示意）：

1. `hasNearbyPlayer(Entity vehicle, double radius)`  
   - 使用 `vehicle.getNearbyEntities(5, 5, 5)` 检查 `Player`。
   - 发现任意玩家立即返回 `true`。

2. `hasGunnerOnVehicle(Entity vehicle)`  
   - 优先检查该载具的乘员链是否存在 gunner 实体（或插件可识别的 gunner 标记）。
   - 若无法直接拿到 MCH 乘员，可在 NMS 桥接层补一个 `FlansModAPI.hasGunner(vehicle)` 的统一方法。

3. `shouldKeepVehicle(Entity vehicle)`  
   - 若 `hasGunnerOnVehicle(vehicle)` 返回 `true`，保留。
   - 若 `hasNearbyPlayer(vehicle, 5.0)` 返回 `true`，保留。
   - 否则走现有逻辑（UAV/GLTD/SEAT/MIS 与无主判定）。

### 8.2 与现有清理流程的融合

- 当前 `clearMCHEntities()` 在遍历到 `MCHELI*` 实体后执行分类判断。
- 新规则建议放在“可清理前置判断”阶段，优先级高于 `isOwnerless`：
  1. `SEAT/MIS` 直接跳过（保持原逻辑）。
  2. `isOnlineUAV` 为真跳过（保持原逻辑）。
  3. `shouldKeepVehicle` 为真跳过（新增逻辑）。
  4. 最后再执行 `UAVSTATION/GLTD` 与 `isOwnerless` 清理判定。

### 8.3 兼容性和风险点

- 5m 半径建议做成配置项（默认 5），便于服务器调优。
- gunner 判定需明确“哪些实体算 gunner”（仅 MCH gunner 或包含 Soldier AI）。
- 若使用“附近有玩家即保留”，高活跃区域载具清理率会明显下降，建议保留手动 `/mch clear` 兜底。
- 为避免误判，建议在清理日志中输出“被保留原因”（GUNNER / NEARBY_PLAYER / UAV_ONLINE）。

### 8.4 验收建议

最小验证用例：

1. 空载具、无人、无 gunner：应被清理。
2. 空载具、5m 内有玩家：不清理。
3. 载具有 gunner、周围无人：不清理。
4. 在线 UAV：不清理（保持原行为）。
5. GLTD/UAVSTATION 空置：按原规则清理。
