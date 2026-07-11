# 科技树参数实现现状清单（2026-04-27）

## 1. 范围
- 目录：`config/mcheli/tech_tree/*.txt`
- 目录：`config/mcheli/economy/economy_tech.txt`
- 目录：`config/mcheli/npc/tech_npc_trader.txt`
- 代码：`mcheli.economy.*`、`mcheli.mob.MCH_TechNpcConfig`、`mcheli.mob.MCH_RenderNPC`

## 2. tech_tree 参数状态
### 2.1 已实现（已解析并生效）
- `TechTreeId`：生效（树 ID）
- `DisplayName`：生效（树名）
- `AddDisplayName`：生效（树名多语言）
- `Node`：生效（节点定义入口）
- `Node` 字段：`NodeId`、`VehicleName`、`RPCost`、`SLCost`、`GEPrice`、`Prerequisite`

### 2.2 未实现或部分实现
- `FactionId`：未实现（解析器未读取）
- `DefaultUnlockedNode`：未实现（未写入玩家初始解锁）
- `Layout`：未实现（前端布局未按该字段切换）
- `EnableCycleCheck`：未实现（未做开关化环检测）
- `AllowResearchRepeat`：未实现（研发重复策略固定）
- `ResearchFailOnMissingNode`：未实现（未按该参数控制行为）
- `Node.VehicleType`：未实现（当前主要靠 `VehicleName` 解析）
- `Node.Tier`：未实现（UI 层级由依赖深度推导，不用该字段）
- `Node.Premium`：未实现（未按该标记控制 GE 直购策略）
- `Node.Pos`：未实现（坐标字段未用于布局）

## 3. economy_tech 参数状态
### 3.1 当前未接入
- `CurrencyNameSL/GE/RP`
- `DefaultSL/GE/RP`
- `MaxSL/GE/RP`
- `Exchange_GE_to_SL`
- `Exchange_GE_to_RP`
- `ExchangeFeeRate`
- `EnableDailyReward`
- `DailyRewardSL/RP/GE`
- `ServerAuditLog`

### 3.2 现状说明
- 当前经济奖励主要来自：
- `mob_rewards.properties`
- `vehicle_rewards.properties`
- GE 兑换节点值来自代码内置节点，不读取 `economy_tech.txt`。

## 4. tech_npc_trader 参数状态
### 4.1 已实现
- `DisplayName`
- `TechTreeId`（支持多 ID 串）
- `Skin`
- `SkinFallback`
- `ModelType`（字段已读取）

### 4.2 备注
- NPC 渲染已支持按 `Skin -> SkinFallback -> Steve` 回退。
- `ModelType` 已读取，当前模型切换能力可继续增强（如强制 Steve/Alex 臂宽差异）。

## 5. 当前状态机（修正后）
- `RP_UNLOCK` 节点未研发：只能 `RP研发`，`SL购买` 禁用。
- `GE_UNLOCK` 节点未解锁：只能 `购买高级载具`（消耗 GE），`SL购买` 禁用。
- `RP_UNLOCK` 节点已研发且配置 `purchaseCostSL>0`：允许 `SL购买`。
- `GE_UNLOCK` 节点已解锁且配置 `purchaseCostSL>0`：允许 `SL购买`。
- `SL_PURCHASE` 节点：直接 `SL购买`。
- `GE_EXCHANGE` 节点：仅 `GE兑换`。
- 服务端兜底：`RP_UNLOCK/GE_UNLOCK` 未解锁时收到 `ACTION_PURCHASE_SL` 会拒绝。

## 6. 下一步建议
- P0：接入 `economy_tech.txt`（先做汇率、默认余额、货币名）。
- P1：接入 `Premium` 与 `GEPrice` 的直购规则。
- P1：接入 `DefaultUnlockedNode`。
- P2：接入 `Pos/Layout/Tier/FactionId` 的前端布局和筛选。
