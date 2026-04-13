# 单兵武器兼容 ASMissile / TVMissile 调研与实施方案

## 1. 目标

- 在不破坏现有 `AAMissile` / `ATMissile` 单兵玩法的前提下，让单兵武器配置化体系支持 `ASMissile` 与 `TVMissile`。
- 保持现有 `lweapons/*.txt` 与 `lweapon_ammo/*.txt` 配置结构兼容，尽量不改配置格式。
- 提供可灰度上线的方案：先最小兼容，再补齐 HUD 体验。

---

## 2. 现状结论

当前单兵链路对导弹类型的限制并非“类型字符串”，而是“制导系统接口被写死”：

- `MCH_ClientLightWeaponTickHandler` 里将武器制导对象强转为 `MCH_WeaponGuidanceSystem`，并依赖其 `lock`、`lockCount`、`targetEntity` 等能力。
- `TVMissile` 使用 `MCH_LaserGuidanceSystem`，与上述类型不兼容。
- `ASMissile` / `TVMissile` 本身实现了 `weapon.lock(prm)` 和 `weapon.onUnlock(prm)`，但当前单兵逻辑没有统一走这一抽象层。

直接结果：

- 配置成 `ASMissile` / `TVMissile` 时，单兵 HUD、锁定、开火链路容易在 `weapon==null || gs==null` 或强转处提前失效。

---

## 3. 设计原则

- 统一锁定入口：从“依赖具体制导类”改为“依赖 `MCH_WeaponBase` 抽象能力”。
- 类型分支最小化：仅在必要位置区分 `seeker`（AAM/AT）与 `laser`（TV）行为差异。
- 保留向后兼容：旧 FIM/FGM 配置和体验不退化。

---

## 4. 推荐改造点

## 4.1 客户端 Tick（核心）

文件：`src/main/java/mcheli/lweapon/MCH_ClientLightWeaponTickHandler.java`

改造要点：

- 保留 `weapon` 创建逻辑，但去掉“必须存在 `MCH_WeaponGuidanceSystem` 才可继续”的硬条件。
- 增加通用锁定参数构建（`MCH_WeaponParam prm`），统一调用：
  - `weapon.lock(prm)`
  - `weapon.onUnlock(prm)`
- 锁定进度与可发射判定改为优先读取：
  - `weapon.getLockCount()`
  - `weapon.getLockCountMax()`
- 对 `MCH_WeaponGuidanceSystem` 仅做增强显示（目标实体、距离、实体锁框），不再作为主控制前提。

预期效果：

- `AAMissile` / `ATMissile` 行为保持。
- `ASMissile` / `TVMissile` 可进入同一单兵锁定与发射流程。

## 4.2 发射参数打包

文件：`src/main/java/mcheli/lweapon/MCH_ClientLightWeaponTickHandler.java`  
文件：`src/main/java/mcheli/lweapon/MCH_LightWeaponPacketHandler.java`

改造要点：

- 不同导弹类型对 `option1/option2` 语义不同，建议按武器类型填充：
  - `AAM/AT`：保留当前目标实体 ID（如已有逻辑依赖）。
  - `TV`：`option1` 用模式值（TV/导引模式），不要塞实体 ID。
  - `AS`：可使用 `option` 传递模式或保持默认。
- 服务端保持“按 `weapon.use(prm)` 最终解释 option”的模式，不在包处理层写死类型逻辑。

## 4.3 HUD 兼容

文件：`src/main/java/mcheli/lweapon/MCH_GuiLightWeapon.java`

改造要点：

- 在 `HudType` 增加 `asmissile` / `tvmissile` 分支（可先复用 `stinger` UI）。
- 锁定条与提示文本改为通用接口驱动（`getLockCount/getLockCountMax`）。
- 对 seeker 专属元素（目标实体框）做条件显示。

## 4.4 可选增强（建议）

文件：`src/main/java/mcheli/weapon/MCH_WeaponASMissile.java`  
文件：`src/main/java/mcheli/weapon/MCH_WeaponTvMissile.java`

改造要点：

- 若某类型 `getLockCount/getLockCountMax` 不完整，补齐覆盖实现，减少单兵端分支判断。

---

## 5. 分阶段实施方案

## 阶段 A（最小可用，建议先做）

- Tick 逻辑改为 `weapon.lock(prm)` 通用入口。
- 去除 `gs` 强依赖导致的提前 return。
- 发射 option 根据武器类型最小分流（至少处理 TV）。
- 保证 `AS/TV` 可锁定、可发射、可重装。

验收标准：

- `AAM/AT` 回归不变。
- 新增 `AS/TV` 单兵配置后可开火命中，不再“无 HUD/无发射”。

## 阶段 B（体验完善）

- `MCH_GuiLightWeapon` 增加 `asmissile` / `tvmissile` HUD。
- 完善锁定提示、模式提示、准星反馈。
- 补齐音效与模式切换文本。

## 阶段 C（收敛与清理）

- 抽离“单兵锁定适配器”小工具，降低 Tick 主流程复杂度。
- 合并重复条件与旧硬编码分支。

---

## 6. 测试清单

- 武器类型覆盖：
  - `AAMissile`（fim92/fim192）
  - `ATMissile`（fgm148）
  - `ASMissile`（新配置）
  - `TVMissile`（新配置）
- 操作覆盖：
  - 持续锁定、松键解锁、重新锁定
  - 发射、重装、再次发射
  - 模式切换（若 `ModeNum > 1`）
- 网络覆盖：
  - 单机
  - 局域网/专服（客户端包 -> 服务端发射一致）
- 回归覆盖：
  - 旧 FIM/FGM HUD 和音效不退化
  - 无配置缺失导致的 NPE/提前 return

---

## 7. 风险与回退

- 风险 1：`option1` 语义混用导致 TV 行为异常。  
  对策：按武器类型分流 `option` 写法，并打日志验证。

- 风险 2：通用锁定改造影响 AAM/AT 老逻辑。  
  对策：阶段 A 先保留 seeker 分支显示逻辑，只替换控制入口。

- 风险 3：HUD 分支增多导致维护困难。  
  对策：优先复用模板 HUD，后续再抽象共用绘制函数。

回退策略：

- 保留一个开关（例如配置项）决定是否启用“通用锁定入口”，出现问题可快速切回旧 seeker 路径。

---

## 8. 推荐配置示例（新增类型）

`lweapons/hvm-ii.txt` 示例关键字段：

- `WeaponInfoName=hvm2`
- `HudType=tvmissile` 或 `HudType=asmissile`
- `AmmoItemName=hvm-ii_bullet`

`weapons/hvm2.txt` 示例关键字段：

- `Type=TVMissile`（或 `Type=ASMissile`）
- 若有模式：`ModeNum=2` 并定义对应模式参数

说明：

- 上述配置要生效，必须完成本方案的阶段 A 改造；仅靠配置无法绕过当前单兵端对制导系统的硬依赖。
