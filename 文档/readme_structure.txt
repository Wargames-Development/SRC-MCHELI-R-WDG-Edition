2026/04/18

;***********************************************************************************
;■ 结构生成规则文件 structure_rules/***.txt（MCH-Reforged）
;***********************************************************************************

;===================================================================================
; A. 使用前提
;===================================================================================

; 规则目录：config/mcheli/structure_rules/
; 仅加载 .txt 文件，按文件名字典序加载
; 规则在 preInit 加载，修改后必须重启
; 结构资产目录：
;   config/mcheli/structures_runtime/meta/<name>.txt
;   config/mcheli/structures_runtime/blob/<name>.nbt

;===================================================================================
; B. 最小模板（联调）
;===================================================================================

Enable = true
; 是否启用该规则

RuleId = airbase_debug
; 规则ID（日志标识用）

Structure = airbase
; 结构资产名（必须存在同名 meta/blob）

Dimension = 0
; 维度白名单（主世界一般 0）

GridSpacingChunk = 1
; 网格间距（区块），联调建议 1

Chance = 0.2
; 候选点命中概率（0~1）

HeightMin = 1
; 最低地表高度

HeightMax = 255
; 最高地表高度

SlopeMax = 20
; 地形坡度阈值（maxY-minY）

ForceSpawnNow = true
; 结构放置后立即触发一次生成器刷载具尝试

;===================================================================================
; C. 参数说明（每行参数后紧跟注释）
;===================================================================================

Enable = true
; 类型：布尔；可写 true/false/1/0/yes/no/on/off；默认 true

RuleId = airbase_release
; 类型：字符串；别名 Id；不写时默认文件名（去后缀）

Structure = airbase
; 类型：字符串；别名 Name / StructureName；为空会跳过规则

Dimension = 0,-1,1
; 类型：整数列表（逗号分隔）；别名 Dimensions；留空=不限制维度

WorldNameWhitelist = world,world_nether_copy
; 类型：字符串列表（逗号分隔，小写比较）；留空=不限制世界名

WorldNameBlacklist = world_event
; 类型：字符串列表（逗号分隔，小写比较）；留空=不限制

GridSpacingChunk = 48
; 类型：整数；<=0 回退为 48；值越小候选点越密

Chance = 0.18
; 类型：浮点；范围 0~1；越界会自动夹紧

HeightMin = 62
; 类型：整数；<1 会抬到 1

HeightMax = 90
; 类型：整数；>255 会压到 255；若小于 HeightMin 会自动交换

SlopeMax = 4
; 类型：整数；<0 回退 0；0 表示极严格（基本只适合超平坦）

ForceSpawnNow = true
; 类型：布尔；默认 true

Biome = plains,savanna,ocean
; 类型：字符串列表（逗号分隔，小写比较）；别名 Biomes；留空=不限制群系

;===================================================================================
; D. Biome 可填值（1.7.10 原版常用 + 全量）
;===================================================================================

; 写法要求：全部小写，按 biomeName 文本写
; 示例：Biome = ocean,deep ocean

;---- 常用群系 ----
; ocean
; plains
; desert
; forest
; taiga
; swampland
; river
; jungle
; savanna
; mesa
; deep ocean
; beach
; stone beach
; cold beach
; ice plains
; ice mountains
; frozenocean
; frozenriver

;---- 1.7.10 原版全量群系名（biomeName）----
; ocean
; plains
; desert
; extreme hills
; forest
; taiga
; swampland
; river
; hell
; sky
; frozenocean
; frozenriver
; ice plains
; ice mountains
; mushroomisland
; mushroomislandshore
; beach
; deserthills
; foresthills
; taigahills
; extreme hills edge
; jungle
; junglehills
; jungleedge
; deep ocean
; stone beach
; cold beach
; birch forest
; birch forest hills
; roofed forest
; cold taiga
; cold taiga hills
; mega taiga
; mega taiga hills
; extreme hills+
; savanna
; savanna plateau
; mesa
; mesa plateau f
; mesa plateau

;---- 海洋刷航母示例 ----
Biome = ocean,deep ocean
; 海洋生成为主，建议配合 HeightMin/HeightMax 放宽与 SlopeMax 提高

;===================================================================================
; E. 相关命令（结构 + 调试）
;===================================================================================

;---- 结构资产命令 ----
; /mcheli struct capture <name> <x1> <y1> <z1> <x2> <y2> <z2> [captureAir=true|false]
; 捕获结构；captureAir 默认 false（不捕获空气），设 true 可清地形

; /mcheli struct place <name> <x> <y> <z> [rot]
; 手动放置结构；rot=0/90/180/270

; /mcheli struct list
; 列出结构名

; /mcheli struct validate <name>
; 校验结构资产

; /mcheli struct verify <name> <x> <y> <z> [rot]
; 对比结构与落地结果

; /mcheli struct importschem <name> <schemPath>
; 将 schem 导入为 meta+blob

;---- 结构规则调试命令 ----
; /mcheli structdebug here
; /mcheli structdebug <x> <z>
; 显示当前点每条规则 PASS/FAIL 原因

; /mcheli structdebug true
; /mcheli structdebug false
; /mcheli structdebug status
; 开关 150tick 周期统计，日志文件：logs/mcheli_structure_debug.log

;---- 生成器调试命令 ----
; /mcheli spawnerfreeze true|false|status
; 冻结或恢复载具生成器

; /mcheli spawnerdebug here
; /mcheli spawnerdebug <x> <y> <z>
; 查看单个生成器状态（冷却、等待、池配置、跟踪目标等）

;===================================================================================
; F. 常见问题
;===================================================================================

; 1) checked 很高但 placed=0：
;    通常是规则门控没过（grid/worldName/biome/height/slope）

; 2) WorldNameWhitelist 写了 world 但不生成：
;    用 /mcheli structdebug here 看实际 worldName，再改白名单

; 3) Structure 写了 airbase 但不落地：
;    检查：
;    config/mcheli/structures_runtime/meta/airbase.txt
;    config/mcheli/structures_runtime/blob/airbase.nbt

; 4) 放置卡顿：
;    captureAir=true 会增加数据量；联调默认建议 captureAir=false
