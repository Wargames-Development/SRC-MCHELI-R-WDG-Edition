# 单兵激光制导剥离阶段A：协议与状态机草案

## 1. 阶段A已落地内容

- 新增状态存储：`mcheli.weapon.MCH_LaserStateStore`
- 新增协议包：`mcheli.network.packets.PacketLaserStateSync`
- 已接入包注册：`mcheli.network.PacketHandler#initialise`

说明：
- 当前仅落基础设施，不改变现有导弹制导行为。
- 现有 GPS/LZR 逻辑暂不切换读取源（属于阶段C）。

## 2. 协议字段定义（v1）

包名：`PacketLaserStateSync`

- `sourceType:int`
  - `1` = handheld（单兵）
  - `2` = aircraft（机载）
- `sequence:long`
  - 单通道递增序列号
  - 服务端只接受更大序列，丢弃旧包
- `active:boolean`
  - `true` 激活，`false` 显式失活
- `x/y/z:double`
  - 激光点坐标
- `ownerId:int`
  - 客户端发包时可带，但服务端不信任该字段，强制用 `playerEntity.getEntityId()`

## 3. 状态结构（v1）

类：`MCH_LaserStateStore.LaserState`

- `ownerId`
- `sourceType`
- `x/y/z`
- `active`
- `sequence`
- `lastUpdateTick`

Key 规则：
- `key = ownerId + sourceType`（双通道隔离）

超时规则：
- 默认 TTL：`40 tick`（`DEFAULT_TTL_TICKS`）
- 超时即剔除状态（`expireServerStates/expireClientStates`）

## 4. 状态机草案

```text
IDLE
  -> (收到active=true且序列更新) TRACKING

TRACKING
  -> (收到active=false) INACTIVE
  -> (超时TTL) EXPIRED
  -> (收到更高序列active=true) TRACKING

INACTIVE
  -> (收到更高序列active=true) TRACKING
  -> (超时TTL) EXPIRED

EXPIRED
  -> (收到active=true且序列有效) TRACKING
```

## 5. 事件钩子清单（阶段A先定义，不改行为）

单兵通道（handheld）建议挂点：
- `MCH_ClientLightWeaponTickHandler#updateLaserPoint`（active=true 更新）
- `MCH_ClientLightWeaponTickHandler#unlockWeapon`（active=false）
- `MCH_ClientLightWeaponTickHandler` 的“切枪/离手”分支（active=false）

机载通道（aircraft）建议挂点：
- `MCH_WeaponTvMissile#lock`（active=true 更新）
- `MCH_WeaponTvMissile#onUnlock`（active=false）
- 机载武器切换/离开控制载具时（active=false）

通用失活挂点：
- 玩家死亡事件
- 玩家断线事件
- 维度切换事件
- 下车事件

## 6. 阶段B接入原则（提前约束）

1. 仅“并行写入”LaserState，不改现有读取路径。
2. HUD 增加调试输出：同屏显示 GPS 与 LaserState 活跃状态。
3. 如果观测到竞争，先修写入仲裁，再做阶段C读取切换。

## 7. 回退策略

- 回退点1：停发 `PacketLaserStateSync`（保留类）
- 回退点2：不调用 `MCH_LaserStateStore` 写入
- 回退点3：包继续注册不影响旧逻辑

