# FGM148 / FIM92 单兵武器调研与配置化方案

## 1. 结论（先看）

- 当前项目中 `fgm148` / `fim92` 不是“全硬编码”，而是**半硬编码**：
  - 武器弹道/制导等核心参数走 `MCH_WeaponInfo` 文本反序列化（`assets/mcheli/weapons/*.txt`）。
  - 但单兵物品本体（注册、专用HUD分支、模型预加载、部分音效名）存在硬编码。
- 因此它们**可以配置文件化**，但分两层：
  - **层A（已具备）**：武器参数配置化（`weapons/fim92.txt`、`weapons/fgm148.txt`）；
  - **层B（需改代码）**：把“单兵物品定义+UI资源映射”也数据化，做到像坦克/直升机那样靠配置自动扩展。

---

## 2. 代码证据与现状

## 2.1 已有的反序列化链路（武器参数）

- `MCH_MOD.PreInit` 启动时加载武器文本目录：  
  `MCH_WeaponInfoManager.load(sourcePath + "/assets/mcheli/weapons")`  
  见：`src/main/java/mcheli/MCH_MOD.java`（约 L281）
- `MCH_WeaponInfoManager` 会遍历目录下全部 `.txt` 并逐行 `key=value` 解析到 `MCH_WeaponInfo`：  
  见：`src/main/java/mcheli/weapon/MCH_WeaponInfoManager.java`（L37-L101）
- 单兵开火时，武器名直接取手持物品名（如 `fgm148`/`fim92`），再调用 `MCH_WeaponCreator.createWeapon(...)` 从 `MCH_WeaponInfoManager` 取配置：  
  见：`src/main/java/mcheli/lweapon/MCH_ClientLightWeaponTickHandler.java`（L138-L141）  
  见：`src/main/java/mcheli/lweapon/MCH_LightWeaponPacketHandler.java`（L41-L43）

**结论**：如果运行时存在 `assets/mcheli/weapons/fgm148.txt`、`fim92.txt`，导弹行为本身就是配置驱动，不是写死在 Java 里。

## 2.2 硬编码部分（当前限制）

### A) 物品注册硬编码

- `registerItemLightWeapon()` 直接写死了两个名称：`fim92`、`fgm148`；  
  `registerItemLightWeaponBullet()` 写死了 `fim92_bullet`、`fgm148_bullet`。  
  见：`src/main/java/mcheli/MCH_MOD.java`（L531-L563）

### B) HUD/瞄具渲染硬编码

- `MCH_GuiLightWeapon` 通过字符串分支处理：  
  - `if ("fgm148"... )` 走标枪专属界面  
  - `else if ("fim92"... )` 走刺针界面  
  见：`src/main/java/mcheli/lweapon/MCH_GuiLightWeapon.java`（L75-L106, L167+）

### C) 模型与纹理路径半硬编码

- 客户端模型预加载固定写了：`lweapons/fim92`、`lweapons/fgm148`。  
  见：`src/main/java/mcheli/MCH_ClientProxy.java`（L175-L177）
- 但渲染器本身按物品名拼路径：`model lweapons/<name>`，纹理当前是 `textures/lweapon/<name>.png`（单数目录），这部分具备一定通用性。  
  见：`src/main/java/mcheli/lweapon/MCH_ItemLightWeaponRender.java`（L21, L31, L46）

### D) 特定音效硬编码

- 固定注册 `fim92_snd.ogg`、`fim92_reload.ogg`，重装时也直接播 `fim92_reload`。  
  见：`src/main/java/mcheli/MCH_ClientProxy.java`（L482-L483）  
  见：`src/main/java/mcheli/lweapon/MCH_ClientLightWeaponTickHandler.java`（L185-L187）

### E) 全局锁距共用配置

- `MCH_ClientLightWeaponTickHandler.lockRange` 来自 `StingerLockRange` 服务器设置，FGM/FIM 共用，未按武器区分。  
  见：`src/main/java/mcheli/lweapon/MCH_ClientLightWeaponTickHandler.java`（L40, L149）  
  见：`src/main/java/mcheli/MCH_Config.java`（`StingerLockRange`）

---

## 3. 能否“像载具一样配置文件化”

## 3.1 可以，但分阶段

### 阶段1（低成本，马上可做）

- 保持现有 item 类不变，仅通过 `weapons/fim92.txt` 与 `weapons/fgm148.txt` 调参数。  
- 你可获得：伤害、加速度、锁定时间、最大锁距、导引角、抗干扰、近炸等大部分平衡能力。

### 阶段2（中成本，彻底配置化）

- 新增 `LightWeaponInfoManager`（建议目录：`assets/mcheli/lweapons/*.txt`），用来定义：
  - 物品ID/名称/弹药物品
  - 模型名、贴图名
  - HUD 类型（`fgm`/`fim`/`generic`）
  - 重装音效、发射音效
  - 是否启用 NV、缩放档位策略、锁距覆盖
- `MCH_MOD.registerItemLightWeapon*` 改成遍历配置自动注册，取消固定 `fim92/fgm148` 分支。
- `MCH_GuiLightWeapon` 从 `LightWeaponInfo` 取 HUD 类型而不是按名字 `if/else`。

---

## 4. 推荐配置参数（建议值）

## 4.1 FIM-92（防空便携）

推荐用于 `weapons/fim92.txt`：

- `Type = AAMissile`
- `DisplayName = FIM-92 Stinger`
- `Power = 45`
- `Acceleration = 3.2`
- `Explosion = 4`
- `ReloadTime = 70`
- `LockTime = 32`
- `MaxLockOnRange = 260`
- `MaxLockOnAngle = 28`
- `MaxDegreeOfMissile = 85`
- `IsHeatSeekerMissile = true`
- `IsRadarMissile = false`
- `AntiFlareCount = 8`
- `TickEndHoming = 140`
- `TurningFactor = 0.35`
- `EnableOffAxis = true`
- `ProximityFuseDist = 3.0`
- `ProximityFuseTick = 12`

说明：

- FIM-92 建议突出“近中距高机动追尾”，锁定快、角度宽、抗热焰弹中等。

## 4.2 FGM-148（反坦克便携）

推荐用于 `weapons/fgm148.txt`：

- `Type = ATMissile`
- `DisplayName = FGM-148 Javelin`
- `Power = 140`
- `Acceleration = 2.1`
- `Explosion = 8`
- `Piercing = 2`
- `ReloadTime = 90`
- `LockTime = 45`
- `MaxLockOnRange = 420`
- `MaxLockOnAngle = 16`
- `MaxDegreeOfMissile = 60`
- `TurningFactor = 0.22`
- `ModeNum = 2`
- `FixMode = 0`（保留模式切换）
- `IsHeatSeekerMissile = false`
- `IsRadarMissile = false`
- `EnableOffAxis = false`（建议）
- `TickEndHoming = 220`

说明：

- FGM-148 建议突出“锁定慢但高毁伤、较远距、模式切换（直攻/顶攻）”。
- 你现有 `MCH_WeaponATMissile` 已支持 `numMode=2` 与 `option2` 传递导引模式（客户端也有模式切换逻辑）。

---

## 5. 推荐的“单兵物品配置化字段”（阶段2）

建议新增 `assets/mcheli/lweapons/<name>.txt` 字段：

资源目录规范（建议统一）：

- 模型目录：`assets/mcheli/models/lweapons/`
- 贴图目录：`assets/mcheli/textures/lweapons/`

- `DisplayName`
- `WeaponInfoName`（映射 `weapons/*.txt` 名称）
- `AmmoItemName`
- `MaxDurability`
- `ReloadTick`
- `LockRangeOverride`（可选；不填则走全局）
- `HudType`（`fgm148` / `fim92` / `generic`）
- `ModelName`
- `TextureName`
- `ScopeTexture`
- `ScopeOverlayTexture2`（可选）
- `SoundReload`
- `SoundFire`
- `EnableNightVision`
- `ZoomLevels`（如果要脱离 weapon.txt 的 zoom）

---

## 6. 推荐的“弹药物品配置化字段”（新增）

建议新增 `assets/mcheli/lweapon_ammo/<name>.txt`，先做最小可用字段：

- `DisplayName`：弹药显示名
- `ItemID`：物品ID（兼容现有ID体系）
- `StackSize`：最大堆叠（如 2/4/8）
- `TextureName`：图标纹理名（可选，放在 `textures/lweapons/`）
- `AddRecipe` / `AddShapelessRecipe`：合成配方（可选）
- `AddDisplayName`：多语言显示名（可选）

并在 `lweapons/<name>.txt` 里通过 `AmmoItemName` 引用弹药条目。

---

## 7. 实施步骤与方案（建议顺序）

### 步骤1：新增配置管理器（不改玩法）

- 新建 `MCH_LightWeaponInfo` 与 `MCH_LightWeaponInfoManager`，加载 `assets/mcheli/lweapons/*.txt`。
- 新建 `MCH_LightWeaponAmmoInfo` 与 `MCH_LightWeaponAmmoInfoManager`，加载 `assets/mcheli/lweapon_ammo/*.txt`。
- 目标：先完成“反序列化 + 内存索引”，不接入注册逻辑。

### 步骤2：接管物品注册

- 在 `MCH_MOD.PreInit` 中：
  - 加载 `lweapons` 与 `lweapon_ammo` 配置目录；
  - 用遍历配置替代 `registerItemLightWeapon()` / `registerItemLightWeaponBullet()` 的固定分支。
- 让单兵发射器通过 `AmmoItemName` 绑定弹药物品实例。

### 步骤3：接管配方与语言

- 在 `MCH_ItemRecipe.registerCommonItemRecipe()` 中，改为遍历 `lweapons` / `lweapon_ammo` 配置生成配方。
- 语言注册改为读 `DisplayName` + `AddDisplayName`。

### 步骤4：接管 HUD/音效映射

- `MCH_GuiLightWeapon` 改为按 `HudType` 分支，而非按 `fgm148`/`fim92` 字符串分支。
- `MCH_ClientLightWeaponTickHandler` 重装音效改为 `SoundReload` 字段。
- `MCH_ClientProxy.registerModels/registerSounds` 改为配置驱动动态注册。
- `MCH_ItemLightWeaponRender` 纹理路径从 `textures/lweapon/` 统一改为 `textures/lweapons/`。

### 步骤5：兼容与回退

- 若配置缺失，保持旧硬编码兜底（防止旧整合包直接崩）。
- 全部稳定后再移除旧硬编码路径。

---

## 8. 配置文件具体参数（FGM148 / FIM192）

说明：

- 你本次要求使用 `fim192` 名称，下方按 `fim192` 给出示例。
- 若要兼容当前老代码最小改动，也可把文件名与武器名保留为 `fim92`，参数内容不变。

### 8.1 `assets/mcheli/lweapon_ammo/fgm148_bullet.txt`

```txt
DisplayName=FGM-148 Missile
AddDisplayName=zh_CN,FGM-148 弹药
ItemID=28931
StackSize=2
TextureName=fgm148_bullet
AddRecipe=" R "," I "," G ",G,gunpowder,I,iron_ingot,R,redstone
```

### 8.2 `assets/mcheli/lweapon_ammo/fim192_bullet.txt`

```txt
DisplayName=FIM-192 Missile
AddDisplayName=zh_CN,FIM-192 弹药
ItemID=28932
StackSize=2
TextureName=fim192_bullet
AddRecipe="R  "," I ","  G",G,gunpowder,I,iron_ingot,R,redstone
```

### 8.3 `assets/mcheli/lweapons/fgm148.txt`

```txt
DisplayName=FGM-148 Javelin
AddDisplayName=zh_CN,FGM-148 标枪飞弹
ItemID=28921
WeaponInfoName=fgm148
AmmoItemName=fgm148_bullet
MaxDurability=10
ReloadTick=60
LockRangeOverride=420
HudType=fgm148
ModelName=fgm148
TextureName=fgm148
ScopeTexture=javelin
ScopeOverlayTexture2=javelin2
SoundReload=fim92_reload
SoundFire=fim92_snd
EnableNightVision=true
ZoomLevels=1.0,2.0,4.0
AddRecipe="III","GR ",G,glass,I,iron_ingot,R,redstone
```

### 8.4 `assets/mcheli/lweapons/fim192.txt`

```txt
DisplayName=FIM-192 Stinger
AddDisplayName=zh_CN,FIM-192 刺针飞弹
ItemID=28922
WeaponInfoName=fim192
AmmoItemName=fim192_bullet
MaxDurability=10
ReloadTick=60
LockRangeOverride=260
HudType=fim92
ModelName=fim92
TextureName=fim92
ScopeTexture=stinger
SoundReload=fim92_reload
SoundFire=fim92_snd
EnableNightVision=true
ZoomLevels=1.0,2.0
AddRecipe="G  ","III","RI ",G,glass,I,iron_ingot,R,redstone
```

### 8.5 `assets/mcheli/weapons/fgm148.txt`（建议值）

```txt
Type=ATMissile
DisplayName=FGM-148 Javelin
Power=140
Acceleration=2.1
Explosion=8
Piercing=2
ReloadTime=90
LockTime=45
MaxLockOnRange=420
MaxLockOnAngle=16
MaxDegreeOfMissile=60
TurningFactor=0.22
ModeNum=2
FixMode=0
IsHeatSeekerMissile=false
IsRadarMissile=false
EnableOffAxis=false
TickEndHoming=220
```

### 8.6 `assets/mcheli/weapons/fim192.txt`（建议值）

```txt
Type=AAMissile
DisplayName=FIM-192 Stinger
Power=45
Acceleration=3.2
Explosion=4
ReloadTime=70
LockTime=32
MaxLockOnRange=260
MaxLockOnAngle=28
MaxDegreeOfMissile=85
IsHeatSeekerMissile=true
IsRadarMissile=false
AntiFlareCount=8
TickEndHoming=140
TurningFactor=0.35
EnableOffAxis=true
ProximityFuseDist=3.0
ProximityFuseTick=12
```

---

## 9. 风险与注意事项

- 当前代码中 `item ID` 对 FIM/FGM 使用同一组配置项（`ItemID_Stinger`、`ItemID_StingerMissile`），扩展多型号时容易冲突；阶段2需拆分或改自动分配。
- `MCH_ClientProxy.registerSounds()` 中存在对 `MCH_WeaponInfoManager` 迭代器的重复 `while`，后两段不会执行（迭代器已耗尽）；若做音效配置化，建议顺手修复为重新获取迭代器。
- 目前仓库未直接包含 `assets/mcheli/weapons/*.txt` 文件树（运行时由 `sourcePath/assets/mcheli/...` 提供），落地前需确认资源打包路径。

---

## 10. 最终判断

- **是否硬编码**：部分硬编码（item/hud/model/sound），部分配置化（weapon参数）。
- **能否反序列化/配置文件化**：能，且武器参数层已经在用；完全配置化需要补一层 `LightWeaponInfo` 管理器与注册流程改造。
- **建议落地顺序**：先调 `weapons/fim92.txt` / `fgm148.txt` 平衡，再做 `lweapons/*.txt` 彻底解耦硬编码。
