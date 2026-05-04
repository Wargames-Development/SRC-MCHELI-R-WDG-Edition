# structure_rules README（参数手册）

> 适用代码：
> `MCH_StructureRule.java` / `MCH_StructureRuleManager.java` / `MCH_WorldStructureGenerator.java`

## 快速开始

1. 在 `config/mcheli/structure_rules/` 新建 `*.txt`
2. 写入最小规则（见下方“最小模板”）
3. 重启游戏/服务端（规则在 `preInit` 加载，不热更新）
4. 用 `/mcheli structdebug here` 看当前点为什么不生成

## 最小模板（能跑）

```txt
Enable = true
RuleId = airbase_debug
Structure = airbase
Dimension = 0
GridSpacingChunk = 1
Chance = 0.2
HeightMin = 1
HeightMax = 255
SlopeMax = 20
ForceSpawnNow = true
```

## 参数说明（按重要度）

### 必填/强相关

- `Structure`
  - 结构资产名，对应：
    - `config/mcheli/structures_runtime/meta/<name>.txt`
    - `config/mcheli/structures_runtime/blob/<name>.nbt`
  - 为空时规则会被跳过

- `Enable`
  - 是否启用该规则
  - 支持：`true/false`、`1/0`、`yes/no`、`on/off`

### 生成密度

- `GridSpacingChunk`（默认 `48`）
  - 网格间距（单位：区块）
  - 先过网格，再抽概率
  - `<=0` 会回退到 `48`
  - 值越小，候选点越密

- `Chance`（默认 `0.18`）
  - 候选点抽中概率，范围 `0~1`
  - 超范围自动夹到 `0~1`

### 地图过滤

- `Dimension` / `Dimensions`
  - 维度白名单（逗号分隔整数），如 `0,-1,1`
  - 留空表示不限制维度

- `Biome` / `Biomes`
  - 群系列表（逗号分隔）
  - 内部会转小写比较 `biomeName`
  - 名称要与当前环境实际群系名一致

- `WorldNameWhitelist`
  - 世界名白名单（小写比较）
  - 常见坑：写了 `world`，但实际世界名不是 `world`

- `WorldNameBlacklist`
  - 世界名黑名单

### 地形过滤

- `HeightMin` / `HeightMax`（默认 `62/90`）
  - 高度范围，内部使用 `getTopSolidOrLiquidBlock`
  - `HeightMin < 1` 会变 `1`
  - `HeightMax > 255` 会变 `255`
  - 若 `HeightMax < HeightMin` 会自动交换

- `SlopeMax`（默认 `4`）
  - 坡度阈值：中心附近采样后 `maxY-minY`
  - `0` 非常严格（超平坦最容易过）

### 行为控制

- `RuleId` / `Id`
  - 规则标识，仅用于日志
  - 不写时默认用文件名（去后缀）

- `ForceSpawnNow`（默认 `true`）
  - 放置后是否对结构内生成器立即强制尝试一次生成

## 规则执行顺序

1. `Enable`
2. `GridSpacingChunk` 网格命中
3. `Chance` 随机命中
4. 世界过滤（`Dimension/WorldName/Biome`）
5. 地形过滤（`Height/Slope`）
6. 放置结构

## 调试命令

- `/mcheli structdebug here`
  - 显示当前点每条规则的 `PASS/FAIL` 原因

- `/mcheli structdebug true`
  - 开启后台统计（每150tick）
  - 日志输出：`logs/mcheli_structure_debug.log`

- `/mcheli structdebug status`
  - 查看后台统计开关状态

## 常见问题

### 为什么一直不生成？

按优先级检查：

1. `WorldNameWhitelist` 是否写错
2. `GridSpacingChunk` 是否太大（常见 `FAIL grid`）
3. `Biome` 名称是否匹配
4. `Height/Slope` 是否太严
5. 是否在新区块触发（世界生成不是原地每tick刷）

### 联调推荐参数

```txt
GridSpacingChunk = 1
Chance = 0.2
HeightMin = 1
HeightMax = 255
SlopeMax = 20
```

确认能生成后再逐步收紧。
