# 单兵激光制导剥离阶段B：并行写入观测记录

## 1. 阶段B目标

- 保持原有 GPS 读取链路不变。
- 并行写入 LaserState（handheld/aircraft 双通道）。
- 通过 HUD 调试信息观测 GPS 与 LaserState 的一致性和竞争情况。

## 2. 已完成改动（代码侧）

- `MCH_ClientLightWeaponTickHandler`
  - `updateLaserPoint(...)` 并行写入 handheld LaserState。
  - `unlockWeapon(...)` 发布 handheld inactive。
- `MCH_WeaponTvMissile`
  - `lock(...)` 在 TV 子模式激光引导时并行写入 aircraft LaserState。
  - `onUnlock(...)` 发布 aircraft inactive。
- `MCH_GuiLightWeapon`
  - 增加调试行：`DBG GPS:ON/OFF HH:ON/OFF AC:ON/OFF`。
- `MCH_ClientCommonTickHandler`
  - 每 tick 调用 `MCH_LaserStateStore.expireClientStates(...)` 做 TTL 清理。
- `PacketLaserStateSync`
  - 收包时增加 `expireServerStates/expireClientStates` 兜底清理。

## 3. 自动化验证结果

- `./gradlew compileJava`：通过（exit code 0）。

说明：
- 当前仓库无可直接驱动“单兵/机载激光实机链路”的自动化集成测试。
- 阶段B剩余验证需要手动进入游戏场景执行。

## 4. 手动观测矩阵（最小7条）

1. 单兵激光持续瞄准 60 秒：`GPS=ON HH=ON AC=OFF`，无随机熄灭。
2. 单兵切枪/离手：`HH` 应立即变 `OFF`，且不误改机载状态。
3. 玩家上车后使用机载 TV 子模式：`AC=ON`，不继承旧 `HH` 点位。
4. 机载 GPS 武器（非 TV 激光）使用时：HUD 行为保持 GPS 逻辑，不依赖 `HH/AC`。
5. 双人并发（都用单兵激光）：双方互不覆盖，彼此 ownerId 独立。
6. 玩家死亡后重生：旧 `HH/AC` 状态在 TTL 或显式失活后清空。
7. 高延迟（150~300ms）下快速移动瞄准：旧 sequence 不应覆盖新 sequence。

## 5. 观测记录模板

| 用例ID | 场景 | 预期 | 实际 | 结论 |
|---|---|---|---|---|
| B-01 | 单兵持续瞄准 | GPS=ON HH=ON AC=OFF | 待测 | 待定 |
| B-02 | 单兵切枪 | HH->OFF | 待测 | 待定 |
| B-03 | 上车机载激光 | AC=ON 且不继承HH | 待测 | 待定 |
| B-04 | 机载GPS武器 | GPS链路正常 | 待测 | 待定 |
| B-05 | 双人并发 | 不互相覆盖 | 待测 | 待定 |
| B-06 | 死亡重生 | 旧状态不残留 | 待测 | 待定 |
| B-07 | 高延迟乱序 | sequence 防回退 | 待测 | 待定 |

## 6. 阶段B退出条件

- 上述 7 条最小用例全部通过。
- 未再出现“GPS 与单兵激光点互相抢写/误清空”的可复现案例。
- 可进入阶段C（切读取源到 LaserState）。
