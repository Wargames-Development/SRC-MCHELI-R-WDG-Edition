# TVmissile 在 `ModeNum=2` + `FixMode=1` + 非激光制导时失效调研（2026-04-16）

## 1. 问题描述

- 配置条件：
  - `type=tvmissile`
  - `ModeNum=2`
  - `FixMode=1`
  - `LaserGuidance=false`（或未开启激光制导链路）
- 现象：导弹发射后“制导失效/不按预期跟随”，表现为无法正常引导命中。

---

## 2. 结论（先给答案）

该问题在当前实现下是**可复现的配置语义问题**，不是随机异常：

1. `FixMode=1` 会把 TV 导弹固定在“模式1（TA）”，而不是“第1个模式（TV）”。  
2. `tvmissile` 里模式 `0=TV`、`1=TA`。  
3. 当进入 TA（终端攻击/激光点）但又没启用激光制导链路时，导弹拿不到有效引导点，表现为“制导失效”。

---

## 3. 关键代码链路

## 3.1 模式编号是 0/1，不是 1/2

- `MCH_WeaponTvMissile#getName()` 明确：
  - `getCurrentMode()==0 -> [TV]`
  - `getCurrentMode()==1 -> [TA]`
- 代码位置：`mcheli.weapon.MCH_WeaponTvMissile`

## 3.2 `FixMode=1` 的真实含义

- `MCH_WeaponBase#getCurrentMode()`：
  - `fixMode > 0` 时，直接返回 `fixMode`，忽略当前切换状态。
- `MCH_WeaponBase#switchMode()`：
  - `fixMode > 0` 时直接 `return false`，模式不可切换。
- 代码位置：`mcheli.weapon.MCH_WeaponBase`

因此对 TV 导弹来说，`FixMode=1` 不是“固定第一档”，而是**固定 TA 档**。

## 3.3 发射时按 `option1` 决定导弹走 TV 还是 TA

- `MCH_WeaponTvMissile#shot(...)`：
  - `isTVGuided = (prm.option1 == 0)`
  - `e.setTVMissile(isTVGuided)`
- 客户端发包时 `option1` 来自当前武器模式：
  - `PacketUseWeapon(..., lastUsedOptionParameter1, ...)`
  - `lastUsedOptionParameter1` 在 `MCH_WeaponTvMissile#shot` 客户端分支里被设为 `getCurrentMode()`
- 代码位置：
  - `mcheli.weapon.MCH_WeaponTvMissile`
  - `mcheli.aircraft.MCH_AircraftClientTickHandler`
  - `mcheli.network.packets.PacketUseWeapon`

在 `FixMode=1` 下，发射参数恒为 `option1=1`，导弹恒走 TA 分支（非 TV）。

## 3.4 TA 分支需要激光点状态

- `MCH_EntityTvMissile#onUpdateMotion()`：
  - `isTVMissile=true` 才走 TV 跟随视角。
  - 否则走 LaserState 读取（`SOURCE_AIRCRAFT`/`SOURCE_HANDHELD`）并调用 `onLaserGuide(...)`。
- 代码位置：`mcheli.weapon.MCH_EntityTvMissile`

- 但 `MCH_WeaponTvMissile#lock(...)` 里只有 `guidanceSystem != null` 才会发布 aircraft laser state；而 `guidanceSystem` 的创建受 `getInfo().laserGuidance` 控制。
- 代码位置：`mcheli.weapon.MCH_WeaponTvMissile` 构造与 `lock()`

所以在“固定 TA + 非激光制导”时，导弹会进入需要激光点的分支，但通常没有有效激光点可用，最终表现为制导失效。

---

## 4. 为什么 `ModeNum=2` 会放大这个问题

- `MCH_WeaponTvMissile` 构造里默认 `numMode=2`；
- `MCH_WeaponCreator` 又会把 `weapon.numMode = info.modeNum`。
- 当你显式写 `ModeNum=2`，与 TV 导弹模式设计完全一致，`FixMode=1` 就稳定锁在 TA。

代码位置：`mcheli.weapon.MCH_WeaponTvMissile`、`mcheli.weapon.MCH_WeaponCreator`

---

## 5. 复现条件最小集

1. TV 导弹武器配置：`type=tvmissile`
2. `ModeNum=2`
3. `FixMode=1`
4. 未启用激光制导（`LaserGuidance=false` 或链路未产出 LaserState）

结果：导弹固定进入 TA 分支，但缺引导点，导致“制导失效”。

---

## 6. 规避与修复建议

## 6.1 仅改配置（推荐）

- 若你要“固定 TV”：
  - 不要写 `FixMode=1`。
  - 可选方案A：删除 `FixMode`，并在实际使用中保持 TV 档（0）。
  - 可选方案B：把 `ModeNum=1`（仅保留一个模式）并确保该单模式按 TV 路径使用。

- 若你要“固定 TA”：
  - 保留 `FixMode=1`，但必须开启激光制导链路并保证有有效 LaserState 输入。

## 6.2 代码层增强（可选）

- 在 `MCH_WeaponInfo` 或 `MCH_WeaponTvMissile` 增加配置校验：
  - 当 `type=tvmissile && fixMode==1 && laserGuidance=false` 时给出警告日志。
- 或引入更直观的 `FixModeIndex`（1-based）配置并兼容映射，避免“0/1语义误解”。

---

## 7. 最终判断

- 这是当前实现下的**确定性行为**：`FixMode=1` 把 TV 导弹锁到 TA；TA 又依赖激光点，非激光配置自然会出现“制导失效”。
- 因此问题根因在配置语义与模式依赖不匹配，不是网络抖动或随机失效。  
