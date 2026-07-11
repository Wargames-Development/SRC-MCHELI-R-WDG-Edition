# 反导导弹 canLockMissile、TV模式、freelook 与导弹模型渲染问题调研报告

日期：2026-04-16  
范围：仅调研，不改代码

---

## 1. 反导 Gunner 使用 AAMissile：主动/半主动/被动雷达弹是否只跟踪玩家，不跟踪导弹

### 1.1 结论（现状）

- 不是“只跟踪玩家”，但当前实现里**主动/被动雷达弹的自主扫描目标仅限飞机与箔条**，不包含导弹实体。
- `canLockMissile` 虽然存在，但它主要作用在 `MCH_WeaponGuidanceSystem.canLockEntity()` 的“可锁目标判定”；  
  对于雷达弹发射分支，很多情况下并不会把这个锁定结果可靠传给导弹实体。
- 反导 gunner 使用 AAMissile 时，通常是通过 gunner 直接把 `targetEntityId` 填入 `option1`，服务端再 `setTargetEntity()`，因此 gunner 反导可用性高于玩家手操雷达发射。

### 1.2 关键证据

1) `canLockMissile` 字段与解析存在：  
- `MCH_WeaponInfo.canLockMissile`，默认 `false`。  
- `MCH_WeaponEntitySeeker` 构造时把 `wi.canLockMissile` 传给 `guidanceSystem.canLockMissile`。

2) 锁定导弹逻辑确实写在 `MCH_WeaponGuidanceSystem.canLockEntity()`：  
- 当 `canLockMissile=true` 时，可锁 `MCH_EntityAAMissile/ATMissile/ASMissile/TvMissile`（排除自己发射者）。

3) 但 AAMissile 的扫描逻辑在 `MCH_EntityBaseBullet.scanForTargets()` 里：  
- `this instanceof MCH_EntityAAMissile` 分支只处理：
  - `MCH_EntityChaff`
  - `MCH_EntityAircraft`
- 未扫描导弹实体，因此主动/被动雷达弹“自主重捕获导弹”当前不成立。

4) `MCH_WeaponAAMissile.shot()` 的雷达分支（`passive/active/semiActive`）：
- 服务端会尝试按 `prm.option1` 取实体并 `setTargetEntity()`；
- 客户端雷达分支并不会像非雷达分支那样稳定设置 `optionParameter1`（非雷达分支通过 `guidanceSystem.lock()` 完成）。

5) gunner 路径：
- gunner 在 `shotTarget()` 对 `WeaponEntitySeeker` 会直接设置 `prm.option1 = targetEntityId`；
- 这就是 gunner AA 反导可直接追踪导弹目标的主要来源。

### 1.3 对你提出需求的可行性判断

需求A：`canlockmissile=true` 时，让主动雷达弹/被动雷达弹可跟踪导弹。  
- 可行，但需代码改动，至少包含：
  - 在 `scanForTargets()` 的 AAMissile 扫描分支加入导弹实体候选（并做友军过滤、自己弹过滤、角度/距离过滤）。
  - 统一雷达分支对 `option1` 的处理策略，避免仅靠“偶然有 targetEntity”。

需求B：反导 gunner 使用 AAMissile 默认 `canlockmissile=true`。  
- 可行，但 `canLockMissile` 目前是**武器配置级**参数，不是 gunner 级参数。  
- 要实现“gunner 默认 true”可选方案：
  - 方案1（推荐）：gunner 开火时若武器是 `AAMissile` 且 `targetType=AA`，临时允许导弹候选；
  - 方案2：在 weapon info 加载后对指定武器做默认覆写（风险更大，影响玩家手操）。

---

## 2. TV 导弹模式问题：`ModeNum=2 FixMode=1 laserGuidance=false` 应该是指令线，但当前是电视制导

### 2.1 结论（现状）

- 你的判断正确：当前代码里存在“兼容回退”，会把 `laserGuidance=false` 的情况强制按 TV 导弹处理。
- 触发点在 `MCH_WeaponTvMissile.shot()`：
  - `this.isTVGuided = !this.getInfo().laserGuidance || prm.option1 == 0;`
- 这句直接导致：只要 `laserGuidance=false`，就一定 `isTVGuided=true`，即进入电视制导路径。

### 2.2 关键证据

1) `MCH_WeaponBase.getCurrentMode()`：  
- `FixMode>0` 时，返回固定模式（不会切换）。

2) `MCH_WeaponTvMissile.shot()`：  
- 注释里写了“Compatibility fallback”，并把 `laserGuidance=false` 强制 TV。

3) `MCH_EntityTvMissile.onUpdateMotion()`：
- `treatAsTvGuidance = e instanceof MCH_EntityGunner || this.isTVMissile;`
- `isTVMissile=true` 就走 TV（跟随射手视角）；
- `isTVMissile=false` 才走激光/目标点引导链路。

### 2.3 对需求的判断

你的目标是：
- `ModeNum=2 + FixMode=1 + laserGuidance=false` -> 指令线制导（非电视镜头跟随）
- `FixMode=0` 时才电视制导

该目标与现有“兼容回退逻辑”冲突，属于明确代码问题，不是配置问题。

---

## 3. 服务器环境下，飞机连续射击过热后有概率自动进入 freelook（单人不复现）

### 3.1 结论（当前证据强度）

- 源码中**未发现“过热 -> 自动切 freelook”的直接调用链**。
- `freelook` 的显式切换入口只有：
  - 客户端按键设置 `pc.switchFreeLook`（`MCH_AircraftClientTickHandler`）
  - 服务端包处理执行 `switchFreeLookMode(...)`（Plane/Heli/Tank PacketHandler）
  - 首次骑乘时客户端按 `defaultFreelook` 初始化（`onRidePilotFirstUpdate()`）
- 因此更像是**网络状态/重挂载时序问题**或**输入状态误触发**，而非“过热逻辑本身”直接触发。

### 3.2 关键证据

1) `MCH_WeaponSet` 过热逻辑：  
- `currentHeat >= maxHeatCount` 后增加冷却等待，不包含 freelook 调用。

2) freelook 开关路径：
- 客户端：`MCH_AircraftClientTickHandler.commonPlayerControl()` 内 `KeyFreeLook` 才写 `pc.switchFreeLook`。
- 服务端：`MCP_PlanePacketHandler` / `MCH_HeliPacketHandler` / `MCH_TankPacketHandler` 读取包后才切换。
- 首次骑乘：`MCH_EntityAircraft.onRidePilotFirstUpdate()` 会执行 `switchFreeLookModeClient(defaultFreelook)`。

### 3.3 研判

- 与“仅服务器复现”一致的高可疑点：
  - 乘员状态重同步触发 `onRidePilotFirstUpdate` 的时序差异（尤其高频射击、网络抖动时）。
  - 控制包中 `switchFreeLook` 被误置位（需要抓包/日志验证）。

### 3.4 建议验证方法（不改代码）

- 在以下点加 debug 日志（本次未改）：
  - 客户端发送 `pc.switchFreeLook` 前后；
  - 服务端三类 `PacketHandler` 收包时打印 `switchFreeLook`；
  - `onRidePilotFirstUpdate()` 触发时打印实体ID/世界tick/默认值。

---

## 4. 载具刚放下时导弹模型不渲染，切到该武器后才出现

### 4.1 结论（现状）

- 这是渲染判定与弹药同步时序叠加导致的高概率问题，**更偏代码行为设计/时序问题，不是配置错误**。
- `MCH_RenderAircraft.renderWeapon()` 对导弹部件有两层隐藏条件：
  - `hideByCooldown = w.isMissile && ac.isWeaponNotCooldown(ws, weaponIndex)`
  - `hideByAmmo = w.isMissile && weaponIndex >= ws.getAmmoNum()`
- 如果客户端在初始时刻 `ws.getAmmoNum()` 还是 0，就会被 `hideByAmmo` 全隐藏；  
  切换到对应武器后，常常触发弹药同步，模型才显现。

### 4.2 关键证据

1) 渲染隐藏逻辑在 `MCH_RenderAircraft.renderWeapon()`。  
2) 客户端弹药同步依赖 `MCH_PacketNotifyAmmoNum`，初始同步是异步请求/响应：
  - `initCurrentWeapon()` 客户端才发 `MCH_PacketIndNotifyAmmoNum.send(this, -1)`。
  - 服务端返回 `sendAllAmmoNum` 给座位玩家。

### 4.3 研判

- “刚放下”时如果尚未建立完整座位/武器同步，或当前视角并非已请求到对应弹药，容易出现导弹模型暂隐。  
- 切到对应武器后触发同步/状态刷新，模型出现，与你描述一致。

---

## 5. 总结与优先级建议

### P0（明确代码问题）

1) TV 导弹模式回退逻辑导致 `FixMode=1 + laserGuidance=false` 被强制 TV。  
2) `canLockMissile` 对雷达弹“跟踪导弹”链路不完整（扫描候选缺导弹，雷达分支 target 传递不统一）。

### P1（高概率时序问题）

3) 服务器环境 freelook 自动启用：当前更像网络同步/重挂载时序问题，需加日志确认。  
4) 载具初次放置导弹模型不显示：高概率是弹药同步与渲染隐藏条件叠加。

---

## 6. 与你需求的对应结论（直接答复）

- “主动雷达弹和半主动雷达弹是不是只会跟踪玩家，不会跟踪导弹？”  
  - 不是只跟踪玩家；但当前主动/被动雷达自主扫描不含导弹，半主动路径也不完整，确实会表现为“导弹目标支持不足”。

- “`canlockmissile=true` 能不能让主动雷达和被动雷达跟踪导弹？”  
  - 能做，但需要补全扫描与目标传递链路，当前仅参数不足以完全生效。

- “反导gunner使用AAmissile默认 canlockmissile=true 能不能做？”  
  - 能做，建议做成 gunner AA 场景下的局部覆写，避免全局影响玩家武器。

