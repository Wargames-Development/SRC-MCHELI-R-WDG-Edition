# 雷达 GainFactor 与 RCS 系数方案调研（2026-04-25）

## 1. 需求目标
- 新增 `GainFactor=near,far`，用于按距离线性调整雷达扫描命中概率。
- 新增 `RCS` 系数体系，用于体现隐身目标的方向特性（正面更隐身、侧面容易被发现）。
- 方案尽量兼容现有配置与行为，避免对老载具产生破坏性变更。

## 2. 现状结论

### 2.1 命中概率主入口
- 当前雷达扫描命中概率计算集中在：
  - `src/main/java/mcheli/render/MCH_RenderRWR.java`
  - 函数：`computeDetectProbabilityStatic(...)`
- 现有公式核心：
  - `p = base * angleFactor * elevFactor * rangeFactor`
  - `rangeFactor = 1.0 - 0.5 * (distance/maxDistance)`（线性衰减到 0.5）

### 2.2 扫描调用点
- 概率函数在 `refreshRadarContactsStatic(...)` 中逐目标调用。
- `base` 来源于 `MCH_AircraftInfo.radarDetectChanceBase`。

### 2.3 角度逻辑可复用性
- 导弹侧 `pdHDNMaxDegree` 相关代码使用了 `dot/acos` 和 `Vector3f.angle(...)` 思路，理论可参考“夹角建模方法”。
- 但该链路存在“弧度/角度混用风险”，不建议直接复用实现细节，建议在雷达侧单独定义清晰的度制计算。

## 3. GainFactor 方案

### 3.1 配置设计
- 新增飞机参数（`MCH_AircraftInfo`）：
  - `radarGainNearFactor`（默认 `1.0`）
  - `radarGainFarFactor`（默认 `0.5`）
- 配置项建议：
  - `RadarGainFactor=1.5,0.5`

### 3.2 计算公式
- 设：
  - `ratio = clamp(distance/maxDistance, 0, 1)`
  - `gain = near + (far - near) * ratio`
- 新概率：
  - `p = base * angleFactor * elevFactor * gain`
  - 结果再 `clamp(0,1)`

### 3.3 兼容策略
- 默认值 `near=1.0, far=0.5`，与当前行为一致。
- 老配置未写 `RadarGainFactor` 时，表现不变。

## 4. RCS 方案（方向相关，载具与武器统一）

### 4.1 设计目标
- 正面（机头朝向雷达）更难被探测。
- 侧面反射更强，更容易被探测。
- 尾向可单独定义（通常介于正面和侧面之间）。

### 4.2 参数设计
- 载具参数（`MCH_AircraftInfo`）：
  - `RadarRCSFactor={front},{side},{rear}`
- 武器参数（`MCH_WeaponInfo`）：
  - `RCSFactor={front},{side},{rear}`
- 两者语义完全一致，仅配置入口不同，方便区分“目标是载具还是武器实体”。
- 数值范围统一：`0.01 ~ 10`。
- 默认值统一：`1.0,1.0,1.0`。
- 典型隐身示例：
  - `front=0.55, side=0.95, rear=0.75`

### 4.3 角度定义与系数插值
- `aspect`：雷达视线与目标机头方向夹角（0~180 度）。
  - `0°`：正面来向
  - `90°`：侧面
  - `180°`：尾向
- 分段线性插值：
  - `0~90°`：`rcs = lerp(front, side, aspect/90)`
  - `90~180°`：`rcs = lerp(side, rear, (aspect-90)/90)`
- 最终概率：
  - `p_final = p_scan * rcs`
  - 最后 `clamp(0,1)`

### 4.4 与 GainFactor 组合
- 推荐顺序：
  - `p_scan = base * angleFactor * elevFactor * gain(distance)`
  - `p_final = p_scan * rcs(aspect)`
- 可选再乘全局平衡参数（后续如需）。

### 4.5 解析优先级与兼容
- 载具侧推荐优先级：
  - `RadarRCSFactor` > 旧分拆字段（若保留兼容）> 默认值 `1.0,1.0,1.0`
- 武器侧推荐优先级：
  - `RCSFactor` > 默认值 `1.0,1.0,1.0`
- 解析规则建议：
  - 允许空格（如 `0.55, 0.95, 0.75`）。
  - 三个值逐项限幅到 `0.01~10`。
  - 参数缺项或非法时按“逐项回退默认值”处理。

## 5. 数据来源与实现档位

### 5.1 档位 A（低改动，先上线）
- 目标朝向获取策略：
  - 客户端能拿到目标实体：直接用 `entity.rotationYaw`。
  - 拿不到实体：用 `MCH_EntityInfo` 的位移向量估算朝向。
  - 仍不可用：回退 `rcs=1.0`。
- 优点：改动小，不动实体同步协议。
- 风险：BVR 远距时回退较多，RCS 体验不稳定。

### 5.2 档位 B（高一致性，推荐二阶段）
- 扩展实体同步：
  - 在 `MCH_EntityInfo` 增加 yaw/pitch 字段。
  - 在 `PacketEntityInfoSync` 增加对应编解码。
- 优点：远距场景也能稳定计算 `aspect`。
- 风险：涉及协议字段扩展，需要联调与兼容验证。

## 6. 代码接入建议

### 6.1 第一阶段（先做 GainFactor + 基础 RCS）
- `src/main/java/mcheli/aircraft/MCH_AircraftInfo.java`
  - 增加字段、默认值、`loadItemData` 解析。
- `src/main/java/mcheli/weapon/MCH_WeaponInfo.java`
  - 增加 `RCSFactor` 字段与解析。
- `src/main/java/mcheli/render/MCH_RenderRWR.java`
  - 扩展 `computeDetectProbabilityStatic(...)` 参数，接入 `near/far`。
  - 在 `refreshRadarContactsStatic(...)` 中按目标类型（载具/武器）计算并传入 `RCS`。

### 6.2 第二阶段（RCS 稳定化）
- `src/main/java/mcheli/MCH_EntityInfo.java`
  - 增加目标姿态字段。
- `src/main/java/mcheli/network/packets/PacketEntityInfoSync.java`
  - 增加姿态字段编解码。
- `src/main/java/mcheli/MCH_EntityInfoManager.java`
  - 生成快照时写入姿态。

## 7. 风险与注意事项
- 不建议直接复用 `pdHDNMaxDegree` 判定代码，避免弧度/角度语义继承问题。
- RCS 不应影响已锁定目标的“几何有效性判定”（方位/俯仰/距离门），仅影响“扫描命中概率”。
- RCS 统一限幅到 `0.01~10`，避免配置越界导致异常。

## 8. 建议测试用例
- 固定角度（正前/45°/侧向/尾向）与固定距离（近/中/远）组合采样，验证命中率趋势。
- 对比 `GainFactor=1.0,0.5` 与 `1.5,0.5`，确认近距提升明显、远距不变。
- 隐身机对比普通机：正面命中率显著降低，侧向命中率接近或高于正面。
- 多机混战和 BVR 场景验证性能与稳定性（关注回退分支命中率是否异常）。

## 9. 示例配置

```ini
# 飞机雷达基础概率
RadarDetectChanceBase=0.70

# 距离增益：0米=1.5倍，最大距离=0.5倍
RadarGainFactor=1.5,0.5

# 载具 RCS 方向系数（front,side,rear，范围0.01~10）
RadarRCSFactor=0.55,0.95,0.75

# 武器 RCS 方向系数（front,side,rear，范围0.01~10）
RCSFactor=0.70,1.00,0.85
```

---

本方案建议按“两阶段”落地：先上线 GainFactor + 基础 RCS（低改动），验证手感后再做实体姿态同步增强，提升 BVR 场景一致性。
