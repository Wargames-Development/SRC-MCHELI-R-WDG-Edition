# 热成像功能与Shader链路调研（含shaders目录复核）

## 1. 结论速览

- 当前项目的热成像能力由三层组成：
  - 相机模式切换（`MCH_Camera.MODE_THERMALVISION`）
  - 后处理 Shader（`thermal` / `nightvision`）
  - 额外渲染分支（锁框、粒子、载具模型在热成像下的着色）
- 你已复制的 Shader 文件现在位于仓库根目录 `shaders/`，文件本体完整。
- 但代码加载路径是资源域 `mcheli:shaders/post/*.json`，不是直接从仓库根目录读取。
- 依据当前构建脚本，`devModJar` 只打包 `build/resources/main/assets/**`，根目录 `shaders/` 不会自动进入模组资源。

---

## 2. 关键代码链路

### 2.1 相机模式定义与切换

- 模式定义：
  - `MODE_NORMAL = 0`
  - `MODE_NIGHTVISION = 1`
  - `MODE_THERMALVISION = 2`
- 切到 1/2 时分别执行：
  - `activateShader("nightvision")`
  - `activateShader("thermal")`

对应代码：
- `src/main/java/mcheli/MCH_Camera.java`

### 2.2 Shader 资源加载路径

- 加载入口：
  - `W_EntityRenderer.activateShader("thermal")`
  - 内部解析到 `new ResourceLocation("mcheli", "shaders/post/" + n + ".json")`
- 这意味着最终查找的是资源域路径：
  - `assets/mcheli/shaders/post/thermal.json`
  - `assets/mcheli/shaders/post/nightvision.json`

对应代码：
- `src/main/java/mcheli/wrapper/W_EntityRenderer.java`

### 2.3 是否允许切换到热成像

- 载具是否能切相机模式由 `EnableNightVision` 控制（夜视/热成像共用入口）。
- 另外需要 `shaderSupport=true`（显卡支持且 `DisableShader=false`）。
- 这部分通过客户端设置包同步到服务端座位相机能力。

对应代码：
- `src/main/java/mcheli/aircraft/MCH_EntityAircraft.java`
- `src/main/java/mcheli/aircraft/MCH_PacketNotifyClientSetting.java`
- `src/main/java/mcheli/MCH_Config.java`

---

## 3. 你当前复制的 shaders 目录复核结果

已检测到以下文件（仓库根目录）：

- `shaders/post/thermal.json`
- `shaders/post/nightvision.json`
- `shaders/program/thermal.json`
- `shaders/program/nightvision.json`
- `shaders/program/thermal.vsh`
- `shaders/program/nightvision.vsh`
- `shaders/program/thermal.fsh`
- `shaders/program/nightvision.fsh`

文件内容结构匹配标准后处理链路：

- `post/*.json` 定义 pass（`thermal`/`nightvision` + `blit`）
- `program/*.json` 定义 `vertex/fragment/samplers/uniforms`
- `program/*.{vsh,fsh}` 为 GLSL 程序

---

## 4. 为什么“有文件但可能仍不生效”

### 4.1 路径域不匹配（最关键）

- 代码按 `assets/mcheli/shaders/...` 资源域查找。
- 你当前文件在仓库根目录 `shaders/...`。
- 若未额外搬运到资源域，运行时仍会报“找不到 shader 资源”或直接表现为热成像无效。

### 4.2 构建打包范围不包含根目录 `shaders/`

- `build.gradle.kts` 的 `devModJar` 仅包含：
  - `build/resources/main/assets/**`
  - `mcmod.info`
  - `META-INF/**`
- 根目录 `shaders/**` 未在打包 include 列表中。

---

## 5. 开发环境与游戏环境建议放置方式

### 5.1 开发环境（IDE/Gradle）推荐

把 Shader 文件放到：

- `src/main/resources/assets/mcheli/shaders/post/*.json`
- `src/main/resources/assets/mcheli/shaders/program/*.{json,vsh,fsh}`

这样 `processResources` 后会进入 `build/resources/main/assets/...`，并被 `devModJar` 打包。

### 5.2 你当前“解压式运行”环境（若沿用 MCH-R 说明文档）

确保最终运行目录存在：

- `.minecraft/mods/mcheli/assets/mcheli/shaders/post/*.json`
- `.minecraft/mods/mcheli/assets/mcheli/shaders/program/*.{json,vsh,fsh}`

注意：只把文件放在工程根 `shaders/`，不保证游戏能读取。

---

## 6. 与热成像相关的功能点（代码已接入）

- 锁框渲染在热成像下有专门着色分支。
- 载具渲染在热成像模式下走高亮/特殊颜色分支。
- 投掷物与部分粒子（烟雾、爆炸）在热成像下有特殊显示处理。

对应代码目录：

- `src/main/java/mcheli/weapon/`
- `src/main/java/mcheli/aircraft/`
- `src/main/java/mcheli/throwable/`
- `src/main/java/mcheli/particles/`

---

## 7. 建议的下一步（可执行）

1. 将当前根目录 `shaders/` 同步复制到 `src/main/resources/assets/mcheli/shaders/`。
2. 运行一次 `./gradlew compileJava` 与实际客户端验证。
3. 若仍无效，重点检查：
   - `DisableShader=false`
   - 显卡是否支持 `OpenGlHelper.shadersSupported`
   - 载具配置是否 `EnableNightVision=true`
   - 是否被探照灯分支占用 `KeyCameraMode`

---

## 8. Shader 文件具体逻辑解析

### 8.1 `shaders/post/thermal.json`

- 定义一个后处理链：
  1. 把 `minecraft:main` 送入 `thermal` pass，输出到临时 target `swap`
  2. 再用 `blit` 把 `swap` 回写到 `minecraft:main`
- 含义：对整屏做一次 thermal 片元处理，再覆盖回主帧缓冲。

### 8.2 `shaders/post/nightvision.json`

- 结构与 thermal 同型，只是 pass 名改为 `nightvision`。
- 含义：夜视也是整屏后处理，不是局部对象材质替换。

### 8.3 `shaders/program/thermal.json`

- 渲染状态：`srcrgb=one`、`dstrgb=zero`（结果基本由 shader 输出直接决定）。
- 顶点程序：`thermal.vsh`
- 片元程序：`thermal.fsh`
- 输入采样器：`DiffuseSampler`（上一阶段画面）
- Uniform：`ProjMat`、`InSize`、`OutSize`（标准 post shader 参数）

### 8.4 `shaders/program/thermal.fsh`

- 先读取屏幕像素 `centerColor`。
- 计算亮度 `brightness` 和饱和度 `saturation`。
- 特征判定：
  - `isTargetViolet`：红蓝都高且显著压制绿通道（识别“品红目标”）
  - `isNearWhite`：高亮低饱和（识别近白高亮）
- 输出策略：
  - 命中目标特征：输出纯白（白热）
  - 其他像素：转灰并压暗到 `0.45`（背景冷却）
- 直观效果：目标发亮，背景灰暗。

### 8.5 `shaders/program/nightvision.fsh`

- 读取颜色后计算 `yellowness = (r+g)/2 - b`。
- 超过阈值（`0.2`）则将颜色朝“增强黄绿”方向偏移：
  - `+0.1,+0.1,-0.05`
- 否则保持原色。
- 直观效果：偏夜视黄绿增强，而非纯单色覆盖。

### 8.6 顶点程序 `*.vsh`

- `thermal.vsh` 和 `nightvision.vsh` 逻辑一致：
  - 将屏幕空间顶点乘 `ProjMat` 输出
  - 计算 `texCoord` 与 `oneTexel`
- 这是典型全屏后处理顶点模板。

---

## 9. 多人/事件后热成像失效：定向调研结论

### 9.1 现象方向与代码证据匹配度

你描述的“击杀或某事件后热成像失效”与以下设计缺陷高度匹配：

1. `MCH_Camera.setMode()` 会直接操作**全局 shader 开关**（activate/deactivate），并写全局静态 `MCH_Camera.currentCameraMode`。  
2. 该逻辑是 client 侧世界对象执行；当客户端世界里**其他实体**触发相机重置时，也会影响当前玩家的全局 shader。  
3. 多处“重置相机”路径会调用 `setMode(...,0)` 或 `initCamera(...)->setMode(...,0)`，可能在你未主动切模式时把 shader 关掉。

关键代码点：

- `MCH_Camera.setMode()`：模式 0 会 `deactivateShader()`，并改写 `currentCameraMode`。
- `MCH_EntityAircraft.onUnmountPlayerSeat()`：`camera.initCamera(sid, entity)`（内部会 setMode 0）。
- `MCH_EntityAircraft.unmountEntity()`：`camera.initCamera(0, rider)`。
- `MCH_EntityAircraft.destroyAircraft()`：遍历座位玩家并 `switchCameraMode(player,0)`。
- `MCH_ClientCommonTickHandler`：玩家未乘坐载具时还会把 `currentCameraMode` 清 0（但这里不会主动 re-activate）。

### 9.2 为什么会出现“后一位玩家开热成像，前一位失效”

高概率机制：

- Shader 绑定是客户端全局（`Minecraft.entityRenderer.theShaderGroup`）。
- 但相机对象是“每个载具实例一个 camera”。
- 任何一个 camera 的 `setMode(0)` 都会调用全局 `deactivateShader()`。
- 一旦发生在你的客户端（例如你附近载具发生卸载、座位变化、销毁、某玩家事件触发相机重置），你的热成像可能被意外关闭。
- 若你的相机模式内部状态仍是 2，但没有再次触发 `setMode(2)`，就会出现“逻辑仍热成像，实际滤镜丢失”。

这和你描述的“像是某事件把滤镜丢了”非常一致。

### 9.3 额外风险点

- `setShaderSupport(uid,b)` 先 `setMode(uid,0)` 再写 capability，也会关一次全局 shader。
- `MCH_Camera.currentCameraMode` 是静态全局，容易被其他 camera 实例覆盖。
- `mode[]` 长度固定为 2（`new int[]{0,0}`），多座位场景的 uid 合法性与状态一致性也存在潜在隐患。

---

## 10. 建议修复方向（按优先级）

### A. 高优先级（建议先做）

1. **将 shader 激活/关闭绑定到“本地玩家当前视角对应 camera”**，不要让任意 camera 实例直接全局开关。  
2. 引入 `ensureCameraShaderState()`（每 tick）：
   - 若本地应为 thermal/nightvision，但 `theShaderGroup` 为空或类型不符，则自动 re-activate。
   - 若本地应为 normal，才 deactivate。
3. 对 `onUnmount/destroy/initCamera` 路径增加“是否本地视角实体”判断，非本地则不动全局 shader。

### B. 中优先级

1. 将 `MCH_Camera.currentCameraMode` 改为“本地视角来源”的派生值，而非任意 `setMode` 直接覆写。
2. `setShaderSupport` 不要无条件 `setMode(0)`，改为仅更新 capability，必要时在本地视角重算模式。

### C. 验证方案（多人复现）

1. 双人联机，A 打开 thermal，B 进行以下动作：
   - 上下座
   - 切换座位
   - 触发载具销毁/玩家死亡
2. 在 A 客户端打日志：
   - `currentCameraMode`
   - `entityRenderer.theShaderGroup` 是否为空
   - 触发 `deactivateShader()` 的调用点
3. 若出现“mode=2 但 shaderGroup=null”，即可锁定为“滤镜丢失未重绑”。

