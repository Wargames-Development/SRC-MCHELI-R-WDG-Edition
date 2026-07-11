# 雷达GUI映射到 `$radardisplay` 模型部件功能调研

## 1. 概述

本项目支持将**雷达扫描面板（屏幕GUI）的内容动态渲染到飞机/载具3D模型的指定部件上**，模型部件名称为 `$radardisplay`。这个功能使得飞机座舱仪表板上的一个平面网格，能够在游戏世界中实时显示雷达扫描扇形、扫描线、目标回波点、模式文字等信息，模拟真实的座舱雷达显示器。

## 2. 整体架构与数据流

```
┌──────────────────────────────────────────────────────────────────────────┐
│                           每帧渲染流程                                     │
├──────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  MCH_RenderAircraft.doRender()                                           │
│    └─ renderCommonPart()                                                 │
│         └─ renderRadarDisplayPart()    ← 触发点                           │
│              ├─ 检查模型是否有 "$radardisplay" 部件                         │
│              ├─ Pass 1: 渲染不透明黑色底（消除模型原始纹理）                  │
│              └─ Pass 2: 调用 MCH_RadarDisplayTextureManager.getTexture()   │
│                         获取动态纹理并渲染到部件上                          │
│                                                                          │
│  MCH_RadarDisplayTextureManager.getTexture()                             │
│    ├─ 调用 MCH_RenderRWR.buildRadarDisplayFrame() 构建帧数据               │
│    ├─ 软件渲染图形层（扇形、扫掠线、目标点、ACM叠加层）                       │
│    ├─ 软件渲染文字层（模式标签、距离/角度刻度）                              │
│    ├─ 合成文字层到图形层                                                   │
│    └─ 上传到 DynamicTexture，返回 ResourceLocation                         │
│                                                                          │
│  MCH_RenderRWR.buildRadarDisplayFrame()                                  │
│    └─ 读取 radarContactCache（与屏幕GUI雷达共用同一缓存）                   │
│        为每个接触点生成 RadarDisplayPoint（归一化坐标 + 选中/跟踪状态）      │
│                                                                          │
└──────────────────────────────────────────────────────────────────────────┘
```

### 关键设计原则

- **数据源共享**：`$radardisplay` 动态纹理与屏幕GUI雷达（`MCH_RenderRWR.renderRadarScanPanel`）使用同一份 `radarContactCache`，两者看到的雷达回波完全一致。
- **附加图层分离**：雷达GUI上的导弹追踪线（`renderRadarMissileOverlays`）和BVR锁定框（`MCH_RenderBVRLockBox`）**不**渲染到模型部件上，仅渲染雷达基础图形。
- **渲染与游戏分离**：动态纹理采用纯CPU软件渲染（pixel数组操作），不依赖OpenGL状态机，渲染结果缓存在 `DynamicTexture` 中。

## 3. 核心文件与代码位置

| 文件 | 路径 | 职责 |
|------|------|------|
| `MCH_RenderAircraft.java` | [L1480-L1547](file:///d:/MCHR/MCH-Reforged/src/main/java/mcheli/aircraft/MCH_RenderAircraft.java#L1480-L1547) | 模型部件触发与双Pass渲染 |
| `MCH_RadarDisplayTextureManager.java` | [全文](file:///d:/MCHR/MCH-Reforged/src/main/java/mcheli/render/MCH_RadarDisplayTextureManager.java) | 动态纹理管理与软件渲染 |
| `MCH_RenderRWR.java` | [L3295-L3492](file:///d:/MCHR/MCH-Reforged/src/main/java/mcheli/render/MCH_RenderRWR.java#L3295-L3492) | `buildRadarDisplayFrame()` 构建帧数据 |
| `MCH_EntityInfoClientTracker.java` | [全文](file:///d:/MCHR/MCH-Reforged/src/main/java/mcheli/MCH_EntityInfoClientTracker.java) | 客户端实体缓存（提供雷达目标数据） |

## 4. 模型部件注册（MCH_RenderAircraft）

### 4.1 触发链路

```
doRender()
  └─ renderCommonPart()                    [L1480]
       ├─ renderRope()
       ├─ renderRadarDisplayPart()         [L1482] ★
       ├─ renderERA()
       ├─ renderWeapon()
       ├─ renderRotPart()
       ├─ renderTurretRotPart()
       ├─ renderHatch()
       └─ ...
```

`renderCommonPart()` 在 `doRender()` 中，**在 `renderAircraft()` 之后立即调用**，因此模型主体渲染完成后才会叠加雷达显示。

### 4.2 renderRadarDisplayPart() 详细逻辑

```java
private void renderRadarDisplayPart(MCH_EntityAircraft ac, MCH_AircraftInfo info, float tickTime) {
    // 1. 前提检查：ac、info、model 都存在且为 W_ModelCustom 类型
    if (ac == null || info == null || info.model == null || !(info.model instanceof W_ModelCustom)) {
        return;
    }
    W_ModelCustom bodyModel = (W_ModelCustom)info.model;
    String partName = "$radardisplay";

    // 2. 检查模型是否包含名为 "$radardisplay" 的部件
    if (!bodyModel.containsPart(partName)) {
        return;  // 模型没有这个部件，跳过
    }

    // 3. Pass 1: 黑色不透明底
    //    关闭纹理、关闭混合、开启PolygonOffset避免Z-fighting
    //    用纯黑色覆盖该部件的UV区域，清除原始贴图
    GL11.glDisable(GL11.GL_TEXTURE_2D);
    GL11.glDisable(GL11.GL_BLEND);
    GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
    GL11.glPolygonOffset(-2.0F, -2.0F);
    GL11.glColor4f(0.0F, 0.0F, 0.0F, 1.0F);
    bodyModel.renderPart(partName);

    // 4. Pass 2: 雷达动态纹理叠加
    //    开启纹理、开启Alpha混合、关闭深度测试让雷达始终可见
    ResourceLocation radarDynamicTex = MCH_RadarDisplayTextureManager.getTexture(ac, player, tickTime);
    if (radarDynamicTex != null) {
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(false);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        this.bindTexture(radarDynamicTex);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        bodyModel.renderPart(partName);
    }
}
```

**渲染特点**：
- **双Pass设计**：先画黑色底让背景不透明，再叠加雷达纹理。这保证了雷达显示区域不会透出飞机机身纹理。
- **关闭深度测试**：Pass 2关闭 `GL_DEPTH_TEST`，确保雷达始终在模型表面可见，不会因为视角原因被遮挡。
- **PolygonOffset**：使用 `-2.0F` 偏移量，防止黑底与模型表面Z-fighting闪烁。

## 5. 动态纹理管理（MCH_RadarDisplayTextureManager）

### 5.1 纹理规格

| 属性 | 值 |
|------|-----|
| 纹理尺寸 | 256 × 256 px |
| 雷达直径占纹理比例 | 70%（`RADAR_UV_DIAMETER_RATIO = 0.70`） |
| 纹理名称格式 | `mcheli_radar_display_{aircraftId}` |
| 纹理类型 | `DynamicTexture`（Minecraft动态纹理，支持每帧更新像素） |

### 5.2 双通道像素图

纹理管理内部维护两个独立的像素缓冲区：

- **`state.pixels`（图形层）**：存储雷达几何图形（扇形、扫掠线、目标点、ACM框）
- **`state.textPixels`（文字层）**：存储雷达文字（模式标签、距离/角度刻度）

最终通过 `overlayTextLayer()` 将文字层Alpha混合叠加到图形层。

### 5.3 纹理更新策略

```
getTexture() 决策树：
├─ !ac.getAcInfo().enableRadar → return null（雷达未启用）
├─ 纹理未就绪 → warmupTexture()（分块初始化，每帧8192像素）
│   └─ 仍未就绪 → return null（黑屏过渡）
├─ phaseKey 未变化 且 非锁定状态 → 复用缓存，不更新
├─ 锁定但目标点在cache中消失 → 保持上一帧纹理（闪烁保护）
└─ 正常更新：
    ├─ renderGraphicsFrame()   → 渲染几何图形
    ├─ shouldUpdateTextLayer() → 每5 tick更新一次文字
    ├─ renderTextLayer()       → 渲染文字
    ├─ overlayTextLayer()      → 合成
    └─ texture.updateDynamicTexture() → 上传到GPU
```

**更新节流机制**：
- `buildPhaseKey()`：将每个tick分为4个`partialTicks`桶（每帧最多4次更新），相同桶内不重复渲染。
- **锁定时强制更新**：当跟踪目标存在时，即使`phaseKey`相同也会更新（保证追踪点位置实时刷新）。
- **文字层独立节流**：文字每秒最多更新4次（`TEXT_UPDATE_INTERVAL_TICK = 5`），避免频繁刷新消耗性能。

### 5.4 图形渲染内容（renderGraphicsFrame）

渲染的几何元素，按层叠顺序：

1. **基础填充**：全图清零（`CLEAR_COLOR = 0x00000000`）
2. **扇形背景**：根据 `scanAzimuthDeg` 绘制雷达扫描扇形
   - 360°时用 `fillCircle()`
   - <360°时用 `fillSector()` + 扇形边界弧线
3. **跟踪扇区叠加**：当 `trackAzimuthDeg > 0`，在扫描扇形上叠加更亮的跟踪角度扇形
4. **扫描线**：根据 `scanPhase` 绘制来回扫描线（非ACM模式下可见）
5. **ACM叠加层**：ACM模式下绘制更小的ACM扇区 + 独立扫掠线
6. **目标回波点**：遍历 `frame.points`，绘制方点
   - 未选中：接触色（`radarUiColor`）
   - 已选中：白色
   - 跟踪中：红色 + 跟踪连线
   - TWS虚线：已选中但非跟踪 → 虚线连接
   - 速度矢量：飞机/直升机显示短速度线
7. **选中/跟踪方框**：选中或跟踪的点额外画3像素方框

### 5.5 文字渲染内容（renderTextLayer）

在圆形雷达上部显示两行文字：

```
RADAR STT            ← 模式标签（例：SRC / STT / TWS / ACM / GMTI等）
-90  4096M  90       ← 方位角/距离刻度（左半角 距离 右半角）
```

文字渲染采用**硬编码3x5点阵字体**，仅支持以下字符：
`A B C D E G I M R S T W 0-9 - * 空格`

### 5.6 软件渲染函数

纹理管理内部实现了完整的2D软件光栅化渲染：

| 函数 | 功能 |
|------|------|
| `fillCircle()` | 圆填充 |
| `fillSector()` | 扇形填充（射线法） |
| `drawRing()` | 圆环线 |
| `drawArc()` | 弧线 |
| `drawLine()` | Bresenham直线 |
| `drawDashedLine()` | 虚线 |
| `drawSquare()` | 方点/方框 |
| `setPixel()` | 单像素写入 |
| `blendPixel()` | Alpha混合单像素 |
| `drawText()` / `glyph()` | 3x5点阵文字 |

## 6. 雷达帧数据构建（buildRadarDisplayFrame）

### 6.1 RadarDisplayFrame 数据结构

```java
public static class RadarDisplayFrame {
    public MCH_EntityAircraft aircraft;   // 所属飞机
    public boolean valid;                 // 数据有效标记
    public boolean acmMode;               // ACM（格斗）模式
    public int selectedTargetId;          // 选中的目标实体ID
    public int trackingTargetId;          // 跟踪中的目标实体ID
    public double scanPhase;              // 扫描线相位 (0.0~1.0)
    public double scanAzimuthDeg;         // 扫描方位角
    public double trackAzimuthDeg;        // 跟踪方位角
    public double panelFillAlpha;         // 面板填充透明度
    public boolean srcLikeMode;           // SRC模式
    public boolean twsLikeMode;           // TWS模式
    public boolean showMainSweep;         // 是否显示扫描线
    public double scanAxisDeg;            // 扫描中心轴角度（默认-90°=屏幕上方）
    public double acmAxisDeg;             // ACM模式中心轴角度
    public double acmAzimuthDeg;          // ACM扇区角度
    public int maxDistanceMeters;         // 最大显示距离（米）
    public int halfAzimuthDeg;            // 半方位角（用于刻度文字）
    public String modeLabel;              // 模式标签（SRC/STT/TWS/ACM等）
    public List<RadarDisplayPoint> points;// 目标回波点列表
}
```

### 6.2 数据来源

`buildRadarDisplayFrame()` 的**数据完全来源于** `radarContactCache`——这与屏幕GUI雷达 `renderRadarScanPanel()` 使用的缓存是**同一个** `Map<Integer, Map<Integer, RadarContact>>`。

同步机制：
1. `isRadarContactVisible()` 或 `renderRadarScanPanel()` 触发 `refreshRadarContactsStatic()` 填充缓存
2. `buildRadarDisplayFrame()` 直接读取 `radarContactCache` 中当前飞机ID对应的缓存
3. 每个接触点通过 `MCH_EntityInfoClientTracker.getEntityInfo(id)` 获取实体位置信息
4. 位置经过 `projectContactStatic()` 映射到方位角/距离，再归一化到 [0,1] 范围

### 6.3 跟踪目标保护

当 `trackingTargetId > 0` 但该目标不在雷达缓存中时（如目标在概率扫描中丢失），`buildRadarDisplayFrame()` 会：
1. 从 `MCH_EntityInfoClientTracker` 直接获取目标实体信息
2. 手动投影并创建一个 `RadarDisplayPoint`
3. 保证STT锁定目标始终在模型雷达上可见

## 7. 与屏幕GUI雷达的区别

| 特性 | 屏幕GUI雷达 | $radardisplay 模型雷达 |
|------|------------|----------------------|
| 渲染方式 | OpenGL即时模式 | CPU软件渲染 → DynamicTexture |
| 渲染目标 | 屏幕HUD | 3D模型UV纹理 |
| 导弹覆盖层 | ✅ 显示导弹位置和追踪线 | ❌ 不显示 |
| BVR锁定框 | ❌ 由 MCH_RenderBVRLockBox 独立渲染 | ❌ 不显示 |
| 数据源 | radarContactCache | radarContactCache（共享） |
| 文字渲染 | Minecraft FontRenderer | 自定义3x5点阵字体 |
| 更新频率 | 每帧 | 最多每tick 4次（phaseKey节流） |
| 视觉效果 | 独立于视角 | 跟随模型旋转/移动 |

## 8. 前置条件

要使 `$radardisplay` 功能正常工作，需满足以下条件：

1. **飞行器配置文件**中 `enableRadar = true`（`MCH_AircraftInfo.enableRadar`）
2. **运行时雷达已开启**（`ac.isRadarEnabledRuntime() == true`）
3. **3D模型文件**（.mqo 或 .obj）中必须包含名为 `$radardisplay` 的独立部件
4. **模型类型**必须为 `W_ModelCustom`（MCH自定义模型格式）
5. **雷达扫描参数**已正确配置：
   - `radarScanAzimuthDeg`：扫描方位角（必须 > 0）
   - `radarScanElevationDeg`：扫描俯仰角（必须 > 0）
   - `radarMaxTargetRange`：雷达最大探测距离
   - `radarScanTick`：扫描刷新间隔（tick）

### 8.1 纹理暖机过程

动态纹理创建时有**分块初始化机制**（`warmupTexture()`）：
- 纹理初次创建时像素未初始化
- 每帧初始化 `WARMUP_CHUNK_PIXELS = 8192` 个像素
- 256×256 = 65536 像素需要约 8 帧初始化完成
- 初始化完成前返回 `null`（模型雷达显示全黑）
- 初始化完成后还有一帧延迟（等待 `clearUploaded = true`），确保先上传清空纹理

## 9. 已知限制与注意事项

1. **单色系渲染**：纹理只支持一种主色（`radarUiColor`），固定翼飞机为绿色（`0x00FF00`），坦克/舰船为琥珀色（`0xFFE400`）。不支持多色雷达回波（如敌我识别不同颜色）。

2. **文字字符集有限**：只支持大写字母子集（`A B C D E G I M R S T W`）、数字 `0-9`、`-`、`*` 和空格。中文、小写字母等其他字符不会显示。

3. **纹理尺寸固定**：256×256分辨率无法通过配置调整。在大型模型部件上可能显得模糊。

4. **无深度写入**：Pass 2渲染时 `GL11.glDepthMask(false)`，雷达纹理不参与深度测试，始终叠加在模型表面。这意味着从模型背面也能看到雷达（如果模型部件法线双面渲染的话）。

5. **雷达关闭时无后备纹理**：当 `enableRadar = false` 或运行时雷达关闭，`getTexture()` 返回 `null`，模型部件显示为黑底（Pass 1黑色）但无任何雷达内容。

6. **多人同步**：雷达数据来源于 `MCH_EntityInfoClientTracker`（客户端实体缓存），因此不同客户端看到的模型雷达内容取决于各自客户端接收到的实体同步数据。服务端不直接控制模型纹理。

7. **纹理缓存不会自动清理**：`CACHE` 静态Map以 `aircraftId` 为键，当飞行器被摧毁/卸载时纹理仍在内存中。如果实体ID复用（极少情况），可能显示残留纹理。

## 10. 扩展建议

如果需要增强 `$radardisplay` 功能，可考虑以下方向：

- **增加灰度/颜色映射**：让 `RadarDisplayPoint` 携带颜色信息，支持敌我识别多色显示
- **纹理分辨率可配置**：将 `TEX_SIZE` 改为可配置参数
- **增加阴影/辉光效果**：在软件渲染中添加简单的发光shader效果
- **文字走Minecraft字体**：用 `FontRenderer` 渲染到FBO再拷贝到DynamicTexture
