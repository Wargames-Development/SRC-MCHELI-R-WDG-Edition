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

## 11. 四类导弹调研补充（AAmissile / ATmissile / TVmissile / ASmissile）

### 11.1 类映射与入口

- 四类导弹的工厂映射位于 `MCH_WeaponCreator`：
  - `aamissile -> MCH_WeaponAAMissile / MCH_EntityAAMissile`
  - `atmissile -> MCH_WeaponATMissile / MCH_EntityATMissile`
  - `tvmissile -> MCH_WeaponTvMissile / MCH_EntityTvMissile`
  - `asmissile -> MCH_WeaponASMissile / MCH_EntityASMissile`
- 入口分为两层：
  1. 武器实例创建（`createWeapon`）
  2. 弹体实体创建（`createEntity`）

### 11.2 AAmissile（空空导弹）

- 继承 `MCH_WeaponEntitySeeker`，默认 `canLockInAir = true`。
- 支持两类流程：
  - 雷达流程（`passiveRadar / activeRadar / semiActiveRadar`）
  - 常规锁定流程（依赖锁定实体ID）
- 弹体在 `rigidityTime` 后进入制导，若为主动雷达且失去目标会定期 `scanForTargets`。
- 基类扫描逻辑中，AA会优先处理箔条诱骗，再选择合法空中目标。

### 11.3 ATmissile（反坦克导弹）

- 继承 `MCH_WeaponEntitySeeker`，默认 `canLockOnGround = true`。
- 武器模式含 `TA`（Top Attack，攻顶）。
- 弹体核心特征：
  - 普通模式：直接追踪目标。
  - 攻顶模式：先抬升后俯冲，末段会提高威力与爆炸等级。
- 兼容雷达分支与常规锁定分支，目标锁定与发射流程和AA同体系。

### 11.4 TVmissile（电视/拖线导弹）

- 不走 `WeaponEntitySeeker`，而是 `MCH_WeaponTvMissile` 专用链路。
- 模式要点：
  - `TV`：典型拖线/视线导引。
  - 非TV模式：发射加速提升（更快突防）。
- 弹体导引有两条：
  1. 非激光：按射手视角进行拖线制导。
  2. 激光：跟随激光点，并受干扰设备影响（含光电/ECM判定）。

### 11.5 ASmissile（空地导弹）

- 该类承担“坐标点打击”主功能，核心取决于 `isGPSMissile`。
- GPS分支：
  - 发射时读取玩家GPS点写入导弹目标坐标。
- 非GPS分支：
  - 按视线做分段射线检测，求落点后导弹按点制导。
- 若 `lockEntity = true`，会在目标区域附近搜索实体并切换到实体跟踪；否则持续按坐标点制导。

### 11.6 四类导弹共用制导参数（`MCH_WeaponInfo`）

- 距离/角度：`maxLockOnRange`、`maxLockOnAngle`
- 机动与时序：`turningFactor`、`rigidityTime`
- 雷达相关：`passiveRadar`、`activeRadar`、`semiActiveRadar`、`scanInterval`
- 特殊制导：`laserGuidance`、`isGPSMissile`、`lockEntity`
- 目标过滤：`ridableOnly`、`lockMinHeight`
- 近炸相关：`proximityFuseDist`、`proximityFuseTick`

结论：
- AA/AT属于“锁定实体导弹”主线；
- TV属于“手动/激光导引”主线；
- AS属于“点位打击/GPS导引”主线，并可扩展到区域实体锁定。

## 12. Gunner武器应用与攻击欲望增强方案评估（新增）

### 12.1 需求摘要

- 提升战斗机 gunner 攻击欲望。
- 空战时仅允许使用：`machinegun1/2`、`railgun`、`rocket`、`AAmissile`，且 `AAmissile` 使用频率最高、`rocket` 最低。
- 对地时仅允许使用：`machinegun1/2`、`railgun`、`rocket`、`bomb`、`ATmissile`、`ASmissile`、`TVmissile`，且权重满足：`AT/AS/TV > rocket > bomb > machinegun/railgun`。
- GPS导弹不依赖玩家建点，直接使用 gunner 当前索敌目标坐标。
- 激光导弹在 gunner 使用时退化为普通 TV 引导逻辑。
- 导弹类武器应有更大索敌与攻击扇区。
- 上述武器应用逻辑应统一到坦克/直升机/飞机。
- 增加战斗机俯冲频率，降低“飞太高导致难以对地精确索敌”的问题。
- 将所有 gunner 载具对空索敌距离统一为 `GunnerPlaneSearchRadiusAir`。

### 12.2 与当前实现的匹配度

- 当前 gunner 武器切换是固定轮换周期，不区分空战/对地场景，入口在 `MCH_EntityGunner.updateWeaponRotation`。
- 当前索敌与可攻击判定与“当前武器”绑定，入口在 `MCH_EntityGunner.updateTargetForWeapon`，因此武器选择策略会直接影响索敌表现。
- 当前战斗机状态机已具备 `SEARCH/FOCUS/ATTACK/DISENGAGE/RTB`，但攻击阶段占比与俯冲触发阈值仍偏保守。
- 非飞机 gunner 目前仍大量使用 `RangeOfGunner_*` 配置；飞机单独使用 `GunnerPlaneSearchRadiusAir/Ground`。

结论：
- 该方案整体可行，且主改动可集中在 `MCH_EntityGunner`，无需重构武器底层框架。

### 12.3 可行性判定（逐项）

- 战斗机攻击欲望增强：可行，通过缩短 `SEARCH/FOCUS` 持续时间、提升 `ATTACK` 占比实现。
- 空战/对地武器白名单与权重：可行，建议以“加权选武器”替代“固定轮换”。
- GPS导弹目标来源改为 gunner 目标坐标：可行，且与现有 AS 导弹逻辑兼容。
- 激光导弹退化为 TV 逻辑：可行，gunner 场景可按 TV 目标链路驱动，不依赖玩家激光点维护。
- 导弹武器扇区放宽：可行，可在 `checkPitch/isInAttackable` 里按导弹类型给更宽阈值。
- 普及到坦克/直升机/飞机：可行，三类载具都经过 `updateTargetForWeapon -> shotTarget` 主链路。
- 统一对空索敌距离为 `GunnerPlaneSearchRadiusAir`：可行，但需注意性能与行为外溢。
- 增加俯冲频率：可行，建议前移高空压低触发阈值并提升攻击态下对地俯冲权重。

### 12.4 风险评估

- 性能风险：统一大半径对空扫描会增加实体遍历量，联机大规模战斗场景更明显。
- 行为风险：权重过于刚性可能导致“长期只用少数武器”，需要保留最小探索概率。
- 兼容风险：部分载具武器位角限制严格，若场景选中武器但角度不满足，可能出现短暂“想打打不出去”。

### 12.5 推荐实施顺序

1. 先改武器选择策略：把固定轮换改为“目标上下文驱动的白名单+权重抽样”，并覆盖坦克/直升机/飞机。  
2. 再改战斗机攻击欲望：提高 `ATTACK` 进入概率和持续时间，降低 `FOCUS` 门槛。  
3. 再调俯冲行为：在攻击地面目标时提高俯冲频次，并提前高空压低触发。  
4. 最后统一对空索敌半径到 `GunnerPlaneSearchRadiusAir`，并加扫描保护（如更新间隔/候选裁剪）。  

### 12.6 参数建议（初始值）

- 空战权重建议：`AAmissile=100`、`railgun=45`、`machinegun1/2=35`、`rocket=15`。
- 对地权重建议：`AT/AS/TV=90`、`rocket=60`、`bomb=40`、`machinegun1/2/railgun=25`。
- 战斗机攻击态建议：`ATTACK` 最短时长上调，`FOCUS` 最短时长下调，稳定后优先进入攻击。
- 俯冲触发建议：高空压低阈值可从当前高阈值下移，并在“对地攻击目标”时附加额外负俯仰偏置。
