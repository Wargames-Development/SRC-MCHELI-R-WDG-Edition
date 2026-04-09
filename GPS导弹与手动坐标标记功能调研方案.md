# GPS导弹与手动坐标标记功能调研方案（仅调研，不实施）

## 1. 当前GPS导弹机制结论

- `IsGPSMissile` 配置项存在于武器信息，解析入口在 `MCH_WeaponInfo`。
- GPS链路当前主要挂在 `asmissile`（空地导弹）上，发射类为 `MCH_WeaponASMissile`，实体为 `MCH_EntityASMissile`。
- 现有“生成GPS标记点”的入口是锁定流程：按住当前武器锁定键，锁定完成后在客户端计算视线落点并写入 GPS 点。
- GPS点由客户端通过网络包同步到服务端，服务端按玩家实体ID缓存，发射时再读取并写入导弹目标坐标。

## 2. 现有使用流程（玩家视角）

1. 选择支持 `asmissile` 且 `IsGPSMissile=true` 的武器。  
2. 在载具上按“当前武器锁定键”（`KeyCurrentWeaponLock`）触发锁定。  
3. 锁定完成后，客户端得到一个世界坐标点并写入当前玩家GPS。  
4. 开火时服务端读取该GPS坐标，导弹按该坐标制导。  

补充：
- 客户端会渲染 GPS 目标图标。
- 玩家下载具时会将客户端 GPS 标记状态置为不激活。

## 3. 新需求可行性评估

需求：除了锁定生成GPS点外，再提供一个按键（例如 `K`）打开面板，手动输入 `x/y/z` 生成GPS导航点。

结论：可行，且改动可控。

原因：
- 现有系统已有 `MCH_GPSPosition.set(x,y,z,isActive,owner)` 与 `PacketGPSPositionReset`，可直接复用，不必新造导弹同步协议。
- 手动输入本质是“新增一个GPS点来源”，不改变导弹发射与制导逻辑。

## 4. 推荐实现路径（方案A：最小改动）

- 客户端新增一个“GPS坐标输入面板（GuiScreen）”，包含三个输入框 `x/y/z` 和“确认/取消”按钮。
- 新增按键 `KeyOpenGPSPanel`（默认可设为 `K`，键码 `37`）：
  - 在载具控制输入处理中监听该键；
  - 按下后 `displayGuiScreen(new MCH_GuiGPSInput(...))`。
- 点击确认后调用：
  - `MCH_GPSPosition.set(x, y, z, true, player)`
  - 复用现有 `PacketGPSPositionReset` 同步到服务端。
- 保留原有右键锁定生成GPS标记流程，不做替换；手动输入仅作为新增入口。
- 导弹端无需改动：`MCH_WeaponASMissile.shot()` 发射时会按现有逻辑读取玩家GPS点。

## 5. 需要修改的现有文件（按职责）

### 5.1 键位与配置

- `src/main/java/mcheli/MCH_Config.java`
  - 新增 `MCH_ConfigPrm KeyOpenGPSPanel`
  - 构造中赋默认值（建议 `37`）
  - 加入 `KeyConfig` 数组

- `src/main/java/mcheli/aircraft/MCH_AircraftClientTickHandler.java`
  - 新增 `MCH_Key KeyOpenGPSPanel` 字段
  - `updateKeybind()` 初始化
  - 在 `commonPlayerControl(...)` 中处理按键事件并打开面板

- `src/main/java/mcheli/gui/MCH_ConfigGui.java`
  - 控制页增加一项可改键位条目（数组长度、按钮ID需顺延）

- `src/main/java/mcheli/MCH_I18n.java`
  - 增加键位文案：
    - `gui.mcheli.key.open_gps_panel`（en_US / zh_CN）

### 5.2 GPS逻辑复用（通常无需改）

- `src/main/java/mcheli/weapon/MCH_GPSPosition.java`
  - 复用 `set(...)` 即可

- `src/main/java/mcheli/network/packets/PacketGPSPositionReset.java`
  - 复用当前同步包即可

- `src/main/java/mcheli/weapon/MCH_WeaponASMissile.java`
  - 发射逻辑复用当前读取GPS路径，一般不改

### 5.3 显示层（通常无需改）

- `src/main/java/mcheli/render/MCH_RenderGPSPosition.java`
  - 现有图标渲染可直接显示手动录入点

## 6. 需要新增的文件

- `src/main/java/mcheli/gui/MCH_GuiGPSInput.java`
  - 建议继承 `GuiScreen`
  - 三个 `GuiTextField`：`x/y/z`
  - 解析校验通过后调用 `MCH_GPSPosition.set(...)`
  - `Esc` / 取消关闭，不写入

说明：
- 该面板是客户端输入面板，不一定要走 `openGui` 的容器式GUI注册。
- 若坚持统一走 `openGui` 流程，则还需改 `MCH_GuiCommonHandler` 与 `MCH_PacketIndOpenScreen` 相关分支；但这会增加改动面，不推荐。

## 7. 交互与约束建议

- 仅在玩家乘坐 `MCH_EntityAircraft` / `MCH_EntitySeat` / `MCH_EntityUavStation` 时允许打开面板。
- 输入校验：
  - 三个字段必须是数值；
  - 可设置坐标范围上限（例如世界边界）；
  - 非法输入只提示不提交。
- 覆盖策略：
  - 新坐标覆盖旧GPS点（沿用当前每玩家单GPS点机制）。

## 8. 风险与兼容性

- 风险低：导弹主逻辑不动，仅新增“坐标来源入口”。
- 主要风险在GUI与键位接入：
  - 若 `MCH_ConfigGui` 条目编号/数组长度处理不当，可能导致按键页面异常。
- 与已有锁定流程兼容：
  - 锁定生成点与手动输入点会竞争同一GPS缓存，后写覆盖先写。

## 9. 最终结论

- “K键打开面板手输 `x/y/z` 生成GPS点”方案可行。
- 推荐按“客户端独立输入面板 + 复用现有GPS同步包”落地，改动最小、风险最低。
- 必改文件重点是：`MCH_Config`、`MCH_AircraftClientTickHandler`、`MCH_ConfigGui`、`MCH_I18n`，并新增 `MCH_GuiGPSInput`。

## 10. 开发步骤（保持“双入口并行”，不是替代关系）

1. 先锁定基线：确认当前右键锁定生成GPS点流程可正常使用，作为回归标准。  
2. 增加键位配置：在 `MCH_Config` 增加 `KeyOpenGPSPanel`，默认 `K`（37），并接入 `KeyConfig`。  
3. 接入客户端按键：在 `MCH_AircraftClientTickHandler` 增加该按键实例和触发逻辑，仅负责打开输入面板。  
4. 新增输入面板：实现 `MCH_GuiGPSInput`，完成 `x/y/z` 输入、校验、确认/取消。  
5. 复用GPS写入链路：确认按钮调用 `MCH_GPSPosition.set(...)`，沿用既有 `PacketGPSPositionReset` 同步。  
6. 保持右键锁定逻辑原样：不改 `KeyCurrentWeaponLock -> currentWeaponLock -> MCH_WeaponASMissile.lock/clientLock` 这条链。  
7. 配置界面与文案：在 `MCH_ConfigGui` 增加新键位项，在 `MCH_I18n` 增加中英文键名。  
8. 并行回归验证：  
   - 路径A：右键锁定生成GPS点并发射命中；  
   - 路径B：K键手输坐标生成GPS点并发射命中；  
   - 路径A/B交替执行时，后写GPS点覆盖先写GPS点，导弹读取最新值。  
