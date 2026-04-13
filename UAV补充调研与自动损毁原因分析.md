# UAV 补充调研与自动损毁原因分析

## 结论摘要

- 你已有的 UAV 闭环结论是对的，主链路完整且已落地。
- 代码里还存在一些你清单外的“外围联动能力”，主要集中在武器/火控渲染接管、GUI接线、持久化恢复、限制项。
- “飞过一定区块后自动损毁”的直接原因是服务端失联距离硬阈值：水平距离超过 1024 方块即立即以 `outOfWorld` 伤害击毁。
- 1024 方块折算约 64 个区块（chunk，16方块/区块），所以体感会像“飞过一段区块后就炸”。

## 你清单外的补充点

### 1) 武器输入与火控渲染链路对 UAV 站控是通的

- 武器使用包会把“骑在 UAV 站上的玩家”映射为被控机体来执行 `useCurrentWeapon`。  
  `src/main/java/mcheli/network/packets/PacketUseWeapon.java:59-73`
- 三套驾驶输入包（飞机/直升机/坦克）都支持通过 `MCH_EntityUavStation#getControlAircract()` 接管。  
  `src/main/java/mcheli/plane/MCP_PlanePacketHandler.java:21-29`  
  `src/main/java/mcheli/helicopter/MCH_HeliPacketHandler.java:26-34`  
  `src/main/java/mcheli/tank/MCH_TankPacketHandler.java:22-30`
- 锁框、CCIP、RWR、GPS 标记等渲染器也都把 UAV 站上的玩家视角映射到被控机体。  
  例：`src/main/java/mcheli/render/MCH_RenderGPSPosition.java:31-39`  
  例：`src/main/java/mcheli/render/MCH_RenderCCIP.java:45-46`

### 2) GUI 接线是双入口（UAV站GUI + 载具GUI）

- GUI ID 0 专用于 UAV 站容器与界面。  
  `src/main/java/mcheli/gui/MCH_GuiCommonHandler.java:23-37,75-78`
- GUI ID 1 在“骑乘 UAV 站时”会跳转到被控机体 GUI。  
  `src/main/java/mcheli/gui/MCH_GuiCommonHandler.java:39-49,80-90`

### 3) “继续连接上次 UAV”有两套恢复路径

- 快速路径：DataWatcher 28 存实体 ID。  
  `src/main/java/mcheli/uav/MCH_EntityUavStation.java:95,386-403,405-408`
- 持久化路径：NBT 保存 `LastCtrlAc`（机体唯一 GUID），每 40 tick 在周围 500 范围检索恢复。  
  `src/main/java/mcheli/uav/MCH_EntityUavStation.java:160-188,412-425,473-475`

### 4) UAV 站本体行为细节

- 便携站支持开盖状态，开关影响可交互性与渲染贴图。  
  `src/main/java/mcheli/uav/MCH_EntityUavStation.java:122-128,594-603`  
  `src/main/java/mcheli/uav/MCH_RenderUavStation.java:48-55,64-69`
- UAV 站被破坏会掉回对应站控物品（非创造），并触发小爆炸。  
  `src/main/java/mcheli/uav/MCH_EntityUavStation.java:237-260`

### 5) 两个限制项

- `/addgunner` 明确跳过 UAV。  
  `src/main/java/mcheli/command/MCH_CommandAddGunner.java:96-99`
- 牵引链条物品明确不作用于 UAV 站实体。  
  `src/main/java/mcheli/chain/MCH_ItemChain.java:45-47`

## “飞过一定区块后自动损毁”的根因

### 直接触发条件（核心）

`MCH_EntityAircraft#updateUAV()` 服务端逻辑中，若 UAV 与其站控实体存在关联，计算水平距离：

- `udx = this.posX - this.uavStation.posX`
- `udz = this.posZ - this.uavStation.posZ`
- 当 `udx*udx + udz*udz > 1024*1024` 时：
  - 断开站控 `setControlAircract(null)`
  - 清空机体站控 `setUavStation(null)`
  - 直接 `attackEntityFrom(DamageSource.outOfWorld, this.getMaxHP() + 10)` 击毁

代码定位：  
`src/main/java/mcheli/aircraft/MCH_EntityAircraft.java:5945-5952`

换算：

- 1024 方块 = 64 个区块（chunk）
- 判定只看 XZ 水平距离，不看高度 Y

### 另一个“自动毁伤”来源（常被混淆）

- 目标靶机 `TargetDrone` 在没油后会触发销毁+爆炸。  
  `src/main/java/mcheli/aircraft/MCH_EntityAircraft.java:1803-1819`
- 对应坦克/飞机实体也有 `isTargetDrone()` 下的行为分支。  
  `src/main/java/mcheli/tank/MCH_EntityTank.java:507`  
  `src/main/java/mcheli/plane/MCP_EntityPlane.java:351,710`

## 与你现象的对应解释

- 如果你是普通 UAV（非 TargetDrone），且每次大约飞到“几十个区块”后固定损毁，基本就是 1024 方块失联阈值触发。
- 如果是靶机且飞行时间长后损毁，也可能是燃油耗尽触发的靶机自毁。

## 后续可改入口（若你要改玩法）

- 失联阈值/惩罚策略：`src/main/java/mcheli/aircraft/MCH_EntityAircraft.java:5929-5958`
- 投放规则与初始油量：`src/main/java/mcheli/uav/MCH_EntityUavStation.java:509-573`
- GUI 偏移范围与继续连接交互：`src/main/java/mcheli/uav/MCH_GuiUavStation.java:75-125,145-149`
