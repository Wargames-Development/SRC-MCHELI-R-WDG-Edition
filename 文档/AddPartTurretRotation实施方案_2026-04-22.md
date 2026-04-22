# AddPartTurretRotation 实施方案（2026-04-22）

## 1. 功能概述
`AddPartTurretRotation` 是为载机添加的一种特殊装饰部件，旨在模拟安装在炮塔上的旋转雷达或传感器。该部件具有双重运动逻辑：
1. **自身旋转**：绕指定的轴心进行持续旋转，旋转速度与雷达扫描周期（`RadarScanTick`）强制同步。
2. **跟随旋转**：模型位置与朝向实时跟随玩家操作的炮塔偏航角（Yaw）进行移动和转动。

## 2. 配置参数格式
在 `aircraft.info` 配置文件中使用以下格式：
```text
AddPartTurretRotation = X, Y, Z, RotX, RotY, RotZ, [AlwaysRotation]
```
- **X, Y, Z**: 部件在模型中的安装点坐标（相对于载机原点）。
- **RotX, RotY, RotZ**: 部件自身旋转的轴向量（如 `0, 1, 0` 表示绕 Y 轴旋转）。
- **AlwaysRotation**: (布尔值，可选)
    - `true` (默认): 始终旋转。
    - `false`: 仅在引擎启动/雷达开启时旋转。

## 3. 模型关联约定
系统将自动按顺序匹配模型中的子对象：
- 配置文件中第 1 个 `AddPartTurretRotation` 对应模型中的 `$weaponrotpart0`。
- 配置文件中第 2 个 `AddPartTurretRotation` 对应模型中的 `$weaponrotpart1`。
- 依此类推。

## 4. 旋转速度同步逻辑
旋转速度由 `RadarScanTick` 参数驱动，实现视觉与逻辑的完美同步：
- **旋转步长**：每 tick 旋转角度 = `360.0 / RadarScanTick`。
- **默认值**：若配置文件中未定义 `RadarScanTick`，则默认按 `40 tick` 转动一圈（即每 tick 旋转 9 度）。

## 5. 技术实施要点

### 5.1 数据结构扩展 (MCH_AircraftInfo.java)
- 在 `MCH_AircraftInfo` 类中新增 `TurretRotPart` 内部类，继承自 `DrawnPart`。
- 在 `loadItem` 逻辑中解析 `AddPartTurretRotation` 并存入 `partTurretRotPart` 列表。

### 5.2 渲染管线修改 (MCH_RenderAircraft.java)
在渲染流程中，执行以下坐标变换序列：
1. `glPushMatrix()`: 开启新堆栈。
2. **应用炮塔偏航**：调用 `glRotatef(ac.getWeaponYaw(), 0, 1, 0)`。这确保了部件整体跟随炮塔转动。
3. **定位安装点**：调用 `glTranslated(pos.x, pos.y, pos.z)`。
4. **执行自身扫描**：根据 `(ticksExisted % RadarScanTick) / RadarScanTick * 360` 计算角度，并调用 `glRotatef(angle, rot.x, rot.y, rot.z)`。
5. **渲染模型**：调用 `renderPart` 渲染对应的 `$weaponrotpart[n]`。
6. `glPopMatrix()`: 恢复堆栈。

## 6. 预期效果
- 当玩家转动炮塔时，雷达天线会保持在炮塔的固定位置并随之转向。
- 雷达天线会以恒定的速度自转，且旋转一圈的时间精确等于雷达在后台完成一次扫描的时间。
