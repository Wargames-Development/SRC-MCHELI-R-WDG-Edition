# 单兵激光制导剥离至 TV 子模式方案调研

## 1. 目标

- 将“单兵激光制导武器”从 `MCH_GPSPosition` 共享链路中剥离。
- 单兵侧改为复用“机载 TV 导弹子模式（TA/Laser）”的激光引导链路。
- 降低当前 GPS 点偶发不生效、被异步清空、状态竞争的问题。

## 2. 结论（可行性）

- 结论：**可行**，且技术路径清晰。
- 当前代码已经具备完整激光引导基础设施：
  - 激光引导系统：`MCH_LaserGuidanceSystem`
  - 激光点服务器同步包：`PacketLaserGuidanceTargeting`
  - TV 导弹激光子模式：`MCH_WeaponTvMissile`（`mode=1`）
- 主要阻塞点不是“有没有激光链路”，而是“单兵仍混用 GPS 状态容器”。

## 3. 当前链路现状（为什么会互相干扰）

### 3.1 单兵链路仍写 GPS 状态

- `MCH_ClientLightWeaponTickHandler#updateLaserPoint` 在激光更新时调用 `MCH_GPSPosition.set(...)`。
- `MCH_WeaponTvMissile#lock` 在 `mode=1` 时也调用 `MCH_GPSPosition.set(...)`。
- `MCH_GuiGPSInput#applyGPS`（K 面板）也写 `MCH_GPSPosition`。

结果：单兵激光、TV 子模式、机载 GPS、K 面板共用同一状态容器。

### 3.2 清理路径多源并发

- `MCH_ClientLightWeaponTickHandler#unlockWeapon` 会在部分模式清空 GPS。
- `MCH_ClientCommonTickHandler#onTick` 有客户端 GPS 清理分支（`isActive=false`）。
- `MCH_WeaponTvMissile#onUnlock` 会关闭激光 targeting（并周期发 `PacketLaserGuidanceTargeting(false,...)`）。

结果：多个 tick 路径可能在同一帧内“一个写入、另一个清空”。

### 3.3 服务器数据通道双轨

- GPS 通道：`PacketGPSPositionReset -> MCH_GPSPosition.currentGPSPositions`。
- 激光通道：`PacketLaserGuidanceTargeting -> 当前载具武器 guidanceSystem`。

结果：单兵如果走 GPS，而 TV/载具走 Laser 包，会出现语义分裂和偶发失步。

## 4. 是否可以“完全改成 TV 子模式激光逻辑”

可以，但建议分阶段：

- **阶段A（低风险过渡）**：单兵保留现有发射逻辑，只把“点位来源/渲染来源”从 GPS 改为激光 guidance 状态。
- **阶段B（彻底剥离）**：单兵不再调用 `MCH_GPSPosition.set`，新增单兵专用 laser state 或扩展 `PacketLaserGuidanceTargeting` 的单兵分支。

## 5. 推荐实施方案（类/方法级）

## 5.1 数据与同步层

- 新增 `MCH_LaserDesignatorState`（建议）：
  - 字段：`ownerId / x / y / z / active / lastUpdateTick`
  - 容器：服务端 `Map<Integer, State>`，客户端 `currentClientLaserState`
- 新增或扩展网络包（建议二选一）：
  - 方案1：扩展 `PacketLaserGuidanceTargeting`，支持“手持单兵上下文”
  - 方案2：新增 `PacketHandheldLaserTargeting`

## 5.2 单兵 Tick 逻辑

- 改 `MCH_ClientLightWeaponTickHandler#updateLaserPoint`：
  - 不再调用 `MCH_GPSPosition.set(...)`
  - 改为写入 LaserState 并发送 laser 包
- 改 `unlockWeapon`：
  - 不再清空 GPS（仅清理 LaserState）

## 5.3 TV 导弹与实体制导

- `MCH_WeaponTvMissile#lock/onUnlock`：
  - 仅维护 `guidanceSystem.targeting` 与 laser 包同步
  - 不再触发 `MCH_GPSPosition.set(...)`
- `MCH_EntityTvMissile#onUpdateMotion`：
  - 非 TV 模式优先读取 LaserState/guidanceSystem
  - 去掉（或降级）对 `MCH_GPSPosition.get(e)` 的依赖

## 5.4 渲染与 HUD

- `MCH_RenderGPSPosition`：
  - 机载 GPS 武器：继续 `GPSPosition.png + [GPS]`
  - 单兵激光：改读 LaserState，显示十字框 + `[LZR]`
- `MCH_GuiLightWeapon`：
  - `isLaserPointLocked(...)` 改由 LaserState 判定，不再直接绑定 `MCH_GPSPosition.currentClientGPSPosition`

## 5.5 GPS 保持独立职责

- `MCH_GuiGPSInput`、`MCH_WeaponASMissile`、`MCH_WeaponCreator` 保持 GPS 导弹专用，不再给单兵激光复用。

## 6. 风险点

- 网络包频率：LaserState 实时同步频率过高会增加带宽。
- 旧兼容：已有脚本/配置可能默认“LZR 依赖 GPS”，需过渡开关。
- 多人一致性：单兵死亡、切物品、换维度时 LaserState 要及时失活。

## 7. 验收清单

1. 单兵 TV 子模式激光引导：右键与发射后 60s 内不丢点。
2. K 面板设置 GPS：只影响 GPS 武器，不影响单兵 LZR。
3. 机载 GPS 与单兵 LZR 同时在线：两种标记互不覆盖。
4. 单兵切枪/收枪/死亡：LZR 状态可正确清理，不误清 GPS。
5. 多人联机（2~4人）：观察者看到的 LZR 与发射者一致，导弹命中与标记一致。

## 8. 建议落地顺序

1. 先加 LaserState + 包同步（不改发射逻辑）。
2. 再改单兵 `updateLaserPoint/unlockWeapon` 去 GPS。
3. 再改 `MCH_EntityTvMissile` 读取源。
4. 最后清理遗留 GPS 依赖并做兼容开关。

---

结论：  
“单兵激光制导剥离出 GPS，并改走 TV 子模式激光链路”是可实施且推荐的方向。当前问题本质是共享状态竞争，不是激光算法本身不足。
