# RWR GUI 渲染调研与 `$rwrdisplay` 移植方案

## 1. 调研目标

分析现有 RWR GUI 的完整渲染链路，明确哪些部分可以用 CPU 软件渲染（`$radardisplay` 模式）复现，哪些部分依赖 OpenGL / FontRenderer 无法直接移植，给出一个**与 RWR GUI 最大视觉同步**的 `$rwrdisplay` 方案。

## 2. RWR GUI 渲染全链路

### 2.1 整体流程

```
MCH_RenderRWR.onRenderOverlay()
  │
  ├─ [前置条件] ac != null, ac.getAcInfo().hasRWR == true
  │
  ├─ [步骤1] 选择 RWR 背景纹理
  │   ├─ Plane            → RWR.png      (180×180 圆形)
  │   ├─ Heli             → RWR_HELI.png
  │   ├─ Tank             → RWR_TANK.png
  │   ├─ FAC/舰船         → RWR_FAC.png   (160×160)
  │   └─ 干扰中            → RWR_jammed.png (覆盖在原图上)
  │
  ├─ [步骤2] 绘制 RWR 圆形背景
  │   └─ drawRWRCircle() — OpenGL 纹理四边形（Tessellator + UV映射）
  │
  ├─ [步骤3] 若被干扰，绘制干扰纹理
  │   └─ drawRWRCircle(RWR_jammed) — 同上，覆盖在原图上
  │
  ├─ [步骤4] 更新 RWR HUD 状态 (updateRwrHudState)
  │   └─ 从 MCH_RWRThreatClientTracker 读取威胁表
  │       解析为 scanEvents / lockSourceName / missileSourceName
  │
  ├─ [步骤5] 渲染威胁环 (renderRwrThreatRing)
  │   └─ 对每个威胁事件:
  │       1. 计算距离映射 → 环半径位置 (innerRadius ~ outerRadius)
  │       2. 计算方位角映射 → 角度位置 (bearingDeg → 圆环角度)
  │       3. 确定颜色 (威胁等级→颜色映射)
  │       4. drawRadarText() — FontRenderer 绘制威胁源标签
  │
  └─ [步骤6] 渲染告警面板 (drawRwrHudAlerts)
      ├─ 半透明底框 (OpenGL四边形)
      ├─ 顶部闪烁扫掠线
      ├─ 导弹告警行：FontRenderer 绘制 "MSL_SOURCE"（红闪）
      ├─ 锁定告警行：FontRenderer 绘制 "LOCK_SOURCE"（红闪）
      └─ 搜索源行：FontRenderer 绘制扫描到的辐射源列表（淡入效果）
```

### 2.2 各渲染元素的实现方式

| 元素 | 实现方式 | 关键代码位置 |
|------|---------|-------------|
| **RWR圆形背景** | OpenGL纹理四边形（Tessellator + UV） | `drawRWRCircle()` L726-L737 |
| **干扰覆盖** | 同上，叠加RWR_jammed.png | L191 |
| **威胁环标签** | FontRenderer.drawString() | `drawRadarText()` L1617-L1629 |
| **告警底框** | OpenGL四边形 + GL_LINE_LOOP边框 + GL_LINES扫掠线 | `drawRwrAlertMask()` L610-L651 |
| **告警文字** | FontRenderer.drawString() | L520-L537 |

### 2.3 坐标映射数学（RWR威胁环）

RWR GUI 的威胁环渲染完全由代码计算，不依赖纹理：

```
环半径范围: 内圈 = halfSize * 0.22, 外圈 = halfSize * 0.88
距离映射:   rangeNorm = (distance - minDist) / (maxDist - minDist)
            ringRadius = innerRadius + (outerRadius - innerRadius) * rangeNorm
方位角映射:  angleRad = Math.toRadians(bearingDeg - 90)
            px = centerX + cos(angleRad) * ringRadius
            py = centerY + sin(angleRad) * ringRadius
```

**文本渲染**：drawRadarText() 使用 Minecraft FontRenderer 在 `(px, py)` 处居中绘制威胁源名称（2~3个字符缩写，如 "29"、"SA6"、"M2000"）。

### 2.4 威胁颜色映射（关键）

| 威胁模式 | 常量 | 颜色值 | 闪烁 |
|---------|------|--------|------|
| SEARCH (飞机辐射源) | `EMITTER_AIRCRAFT` | `0x00FF00` 绿 | 无 |
| SEARCH (地面辐射源) | 默认 | `0xFFE400` 琥珀色 | 无 |
| TRACK | `MODE_TRACK` | `0xFF8A3A` 橙 | 无 |
| STT | `MODE_STT` | `0xFF4A4A` 红 | 无 |
| MSL_ACTIVE | `MODE_MSL_ACTIVE` | `0xFF2D2D` ↔ `0xCC3030` | 3 tick |
| MSL_DATALINK | `MODE_MSL_DATALINK` | `0xFF2D2D` ↔ `0xCC3030` | 3 tick |

### 2.5 数据类型流

```
服务端 (MCH_RWRThreatManager)
  └─ 计算扫描/跟踪/导弹威胁
  └─ 打包 → PacketRWRThreatUpdate
       └─ 客户端 (MCH_RWRThreatClientTracker)
            └─ MCH_RWRThreatTable
                 └─ List<MCH_RWRThreatEvent>
                      ├─ emitterId (辐射源实体ID)
                      ├─ emitterKind (飞机/地面/导弹)
                      ├─ threatMode (SEARCH/TRACK/STT/MSL)
                      ├─ bearingDeg (相对于机头的方位角)
                      ├─ strength (信号强度 0~1)
                      ├─ distanceMeters (距离)
                      └─ sourceName (辐射源名称)
                           │
              ┌────────────┴────────────┐
              ↓                         ↓
    RWR GUI (HUD覆盖层)          $rwrdisplay (模型部件)
    MCH_RenderRWR                MCH_RenderRWR
    .onRenderOverlay()            .buildRWRDisplayFrame()
```

## 3. `$radardisplay` 与 `$rwrdisplay` 移植差异分析

### 3.1 `$radardisplay` 为什么容易移植

雷达GUI的渲染全部是**几何图形**：
- 扇形背景：`fillSector()` / `fillCircle()` → 软件绘图 `drawSectorFilled`
- 扫掠线：`GL11.drawLine()` → 软件绘图 `drawLine()`
- 目标方点：OpenGL四边形 → 软件绘图 `drawSquare()`
- 速度矢量线：→ 软件绘图 `drawLine()`
- 刻度文字：Minecraft FontRenderer → 自实现3×5点阵字体

**所有元素都可以用CPU像素操作完美复现**，不丢失视觉信息。

### 3.2 `$rwrdisplay` 的核心差异

RWR GUI 的渲染大量依赖**艺术纹理和外部字体**：

| 元素 | RWR GUI (OpenGL) | `$radardisplay` 方式 (CPU软件) | 能否完美复现 |
|------|-----------------|-------------------------------|------------|
| RWR圆形背景 | RWR.png (美工绘制) | 软件画圆 (纯几何) | ❌ 丢失美术细节 |
| 干扰覆盖 | RWR_jammed.png | 软件生成噪点条纹 | ⚠️ 近似但不同 |
| 威胁标签 | FontRenderer | 3×5点阵字体 | ⚠️ 字体不同 |
| 告警文字 | FontRenderer | 3×5点阵字体 | ⚠️ 字体不同 |
| 告警底框 | OpenGL四边形+扫掠线 | 软件绘制 | ✅ |
| 威胁环坐标映射 | 纯数学计算 | 纯数学计算 | ✅ 完全一致 |
| 颜色映射 | 纯代码 | 纯代码 | ✅ 完全一致 |

### 3.3 关键可移植性结论

| 组件 | 可移植性 | 原因 |
|------|---------|------|
| 威胁环坐标投影（bearing→角度, distance→半径） | ✅ **完全可移植** | 纯数学计算，与纹理/OpenGL无关 |
| 威胁颜色映射 | ✅ **完全可移植** | 纯 if/else 分支 |
| 告警面板底框+扫掠线 | ✅ **完全可移植** | 简单的矩形/线，软件绘图可复现 |
| 告警文字/威胁标签 | ⚠️ **部分丢失** | FontRenderer 有抗锯齿、完整字符集；3×5点阵字体只支持大写字母+数字 |
| RWR.png 背景 | ⚠️ **需要通过 ImageIO 加载到像素数组** | 否则只能画纯几何圆形 |
| RWR_jammed.png 干扰 | ⚠️ **近似模拟** | 软件生成的噪点条纹效果接近但不完全相同 |

## 4. 推荐移植方案（最大视觉同步）

### 4.1 方案总览

```
┌──────────────────────────────────────────────────────────────────┐
│                   $rwrdisplay 纹理生成流程                          │
├──────────────────────────────────────────────────────────────────┤
│                                                                  │
│  [初始化时]                                                       │
│  loadRWRBackground():                                             │
│    ├─ 通过 TextureManager 获取已加载的 RWR.png 的 OpenGL 纹理 ID   │
│    ├─ 通过 glGetTexImage / FBO 回读像素 → int[]                   │
│    └─ 缓存为 backgroundPixels                                     │
│                                                                  │
│  [每帧]                                                           │
│  getTexture():                                                    │
│    ├─ 复制 backgroundPixels → DynamicTexture.pixels               │
│    ├─ [干扰时] overlayJammedEffect() → 叠加干扰噪点               │
│    ├─ renderThreatPlots() → 软件绘制威胁点+标签                   │
│    ├─ renderAlertPanel() → 软件绘制告警底框+告警文字              │
│    ├─ [文字层] renderTextLayer() → 模式/距离文字                  │
│    ├─ 合成文字层 → 图形层                                        │
│    └─ texture.updateDynamicTexture() → 上传GPU                   │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

### 4.2 方案A（推荐）：回读 RWR.png GPU 纹理 → 像素数组 → 叠加动态内容

**原理**：Minecraft 的 `TextureManager` 在首次 `bindTexture` 时已从资源包加载 RWR.png 到 GPU，我们通过 `glGetTexImage` 将像素回读到 CPU，然后作为 DynamicTexture 的背景。

```java
private static void loadRWRBackground(RwrTexState state, ResourceLocation rwrRes) {
    if (state.backgroundLoaded) return;
    
    // 1. 绑定纹理，确保 GPU 已加载
    Minecraft.getMinecraft().getTextureManager().bindTexture(rwrRes);
    
    // 2. 获取纹理尺寸
    int texWidth = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH);
    int texHeight = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT);
    
    // 3. 分配并读取像素
    int[] rawPixels = new int[texWidth * texHeight];
    ByteBuffer buffer = BufferUtils.createByteBuffer(texWidth * texHeight * 4);
    GL11.glGetTexImage(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);
    buffer.asIntBuffer().get(rawPixels);
    
    // 4. 缩放到 TEX_SIZE (256x256) 并存入 state.backgroundPixels
    // ...
    state.backgroundLoaded = true;
}
```

#### 优点
- 背景与 RWR GUI **完全一致**（同样的 RWR.png）
- 不需要手动解析 PNG 文件
- 纹理会自动随资源包变化

#### 缺点
- 需要 OpenGL `GL11.glGetTexImage()` 调用（MC 1.7.8+ 支持）
- 第一帧需要等到纹理加载完成才能读回

### 4.3 方案B（备选）：ImageIO 从资源包加载 PNG

**原理**：通过 Minecraft 的 `ResourceManager.getResource()` 获取 `InputStream`，用 `javax.imageio.ImageIO` 解码为 `BufferedImage`，再提取像素数组。

```java
private static void loadRWRBackgroundFromResource(RwrTexState state, ResourceLocation rwrRes) {
    if (state.backgroundLoaded) return;
    
    try {
        IResource resource = Minecraft.getMinecraft().getResourceManager().getResource(rwrRes);
        BufferedImage image = ImageIO.read(resource.getInputStream());
        
        // 缩放到 TEX_SIZE (256x256)
        BufferedImage scaled = new BufferedImage(TEX_SIZE, TEX_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = scaled.createGraphics();
        g.drawImage(image, 0, 0, TEX_SIZE, TEX_SIZE, null);
        g.dispose();
        
        state.backgroundPixels = new int[TEX_SIZE * TEX_SIZE];
        scaled.getRGB(0, 0, TEX_SIZE, TEX_SIZE, state.backgroundPixels, 0, TEX_SIZE);
        state.backgroundLoaded = true;
    } catch (IOException e) {
        // fallback: 生成纯圆环背景
    }
}
```

#### 优点
- 不依赖 OpenGL，纯 Java 实现
- 支持资源包热重载（加监听即可）

#### 缺点
- `ImageIO` 在 MC 1.7.10 中可能需要额外依赖
- 跨资源包版本兼容性需测试

### 4.4 方案C（最简 Fallback）：纯几何软件绘制

如果背景图片加载失败，退化为类似 `$radardisplay` 的纯几何绘制：

```java
private static void renderFallbackBackground(int[] pixels) {
    int cx = TEX_SIZE / 2;
    int cy = TEX_SIZE / 2;
    int radius = (int)(TEX_SIZE * (RADAR_UV_DIAMETER_RATIO * 0.5D));
    
    // 画黑色圆底
    fillCircle(pixels, cx, cy, radius, 0xFF1A1A2E);
    // 外圈圆环
    drawRing(pixels, cx, cy, radius, 0x80000000);
    // 内圈圆环 (0.22)
    drawRing(pixels, cx, cy, (int)(radius * 0.22), 0x40000000);
    // 4条方位基线
    for (int deg = 0; deg < 360; deg += 90) {
        double rad = Math.toRadians(deg);
        int ex = cx + (int)(Math.cos(rad) * radius);
        int ey = cy + (int)(Math.sin(rad) * radius);
        int ix = cx + (int)(Math.cos(rad) * radius * 0.22);
        int iy = cy + (int)(Math.sin(rad) * radius * 0.22);
        drawLine(pixels, ix, iy, ex, ey, 0x40000000);
    }
}
```

### 4.5 推荐的组合策略

```
getTexture() 初始化时:
  1. try 方案B: ImageIO 加载 RWR.png → 成功，标记 hasArtisticBackground = true
  2. catch: 方案C 纯几何生成 → 标记 hasArtisticBackground = false

getTexture() 每帧:
  1. 复制 backgroundPixels → DynamicTexture.pixels
  2. 威胁环投影 (与 GUI 完全相同的数学):
     for each threatEvent:
       rangeNorm = (distance - minDist) / (maxDist - minDist)
       ringRadius = innerRadius + (outerRadius - innerRadius) * rangeNorm
       angle = Math.toRadians(bearingDeg - 90)
       px = cx + cos(angle) * ringRadius
       py = cy + sin(angle) * ringRadius
       color = resolveThreatColor(evt, worldTick)
       drawText(pixels, name, px, py, color, 1)  // 3×5点阵字
  3. 告警面板 (左上角矩形):
     drawRect() → 半透明底框
     if 导弹告警闪烁 → drawText("MSL")
     if 锁定告警闪烁 → drawText("LOCK")
     for scanSources → drawText(source)
  4. 干扰:
     if jammed:
       叠加噪点/条纹像素 (不透明度模仿 RWR_jammed.png)
  5. texture.updateDynamicTexture()
```

## 5. 与 RWR GUI 的同步程度分析

### 5.1 视觉同步程度（推荐方案）

| 视觉元素 | RWR GUI | $rwrdisplay (推荐方案) | 同步程度 |
|---------|---------|----------------------|---------|
| 圆形背景 | RWR.png（美工绘制） | 同 RWR.png（通过 ImageIO 加载） | ✅ **100%** |
| 干扰效果 | RWR_jammed.png | 读取实际纹理或软件近似 | ✅~⚠️ |
| 威胁环位置 | bearing→角度, distance→半径 | 完全相同的数学 | ✅ **100%** |
| 威胁颜色 | 代码分支（绿/橙/红/闪烁） | 完全相同的代码 | ✅ **100%** |
| 威胁源名称 | FontRenderer (3~5字符) | 3×5点阵字体 | ⚠️ 80%（字符集受限） |
| 告警文字 | FontRenderer | 3×5点阵字体 | ⚠️ 80%（字符集受限） |
| 告警底框 | OpenGL四边形 | 软件矩形填充 | ✅ 95% |
| 导弹闪烁 | 3 tick固定周期 | 同3 tick周期 | ✅ **100%** |
| 告警扫掠线 | GL_LINES | drawLine() | ✅ 95% |
| 搜索源淡入 | FontRenderer透明度 | 像素Alpha | ✅ 95% |

### 5.2 数据同步程度

| 数据源 | RWR GUI | $rwrdisplay | 同步程度 |
|-------|---------|------------|---------|
| 威胁事件列表 | MCH_RWRThreatClientTracker | 同一数据源 | ✅ **100%** |
| 更新频率 | 每帧（60fps） | 每 tick（20fps）× 相位节流 | ⚠️ 约 1/3 频率 |
| 告警闪烁同步 | 依赖 worldTick | 同 worldTick | ✅ **100%** |

### 5.3 不可避免的差异

1. **字体渲染质量**：Minecraft FontRenderer 有抗锯齿和完整 Unicode 支持；3×5 点阵字体只支持 `ABCDEGIMRSTW0123456789-*` 大写子集，字符更宽、更像素化。
2. **渲染分辨率**：GUI HUD 以屏幕像素为基准（ScaledResolution）；模型部件纹理固定 256×256，在模型上再映射一次UV，小部件上会更模糊。
3. **更新频率**：GUI 每帧更新（60fps），模型纹理受 `DynamicTexture.updateDynamicTexture()` 开销限制（每 tick 1~4 次）。

## 6. 实现建议

### 6.1 从 `$radardisplay` 复用代码

直接复制/复用的函数（提取到 `MCH_TextureRenderUtil`）：

| 函数 | 来源 | 用途 |
|------|------|------|
| `fill(int[], int)` | RadarDisplay | 清空像素数组 |
| `drawLine(int[], int, int, int, int, int)` | RadarDisplay | 画基线/扫掠线 |
| `drawRing(int[], int, int, int, int)` | RadarDisplay | 画内圈/外圈环 |
| `drawSquare(int[], int, int, int, int)` | RadarDisplay | 画威胁点 |
| `setPixel(int[], int, int, int)` | RadarDisplay | 写像素 |
| `blendPixel(int[], int, int, int)` | RadarDisplay | 混合像素 |
| `drawText(int[], String, int, int, int, int)` | RadarDisplay | 3×5点阵文字 |
| `glyph(char)` | RadarDisplay | 点阵字形 |
| `fillRectBlend()` | RadarDisplay | 告警底框 |
| `overlayTextLayer()` | RadarDisplay | 合成文字层 |
| `withAlpha(int, int)` | RadarDisplay | 颜色Alpha封装 |

### 6.2 需要新实现的逻辑

```java
// MCH_RenderRWR.java - 新增
public static RWRDisplayFrame buildRWRDisplayFrame(MCH_EntityAircraft ac, EntityPlayer player, float partialTicks) {
    RWRDisplayFrame frame = new RWRDisplayFrame();
    // ... 见 RWRDisplay_ModelPart_Design.md §6.1
}

// MCH_RWRDisplayTextureManager.java 新增
private static void renderThreatPlots(int[] pixels, RWRDisplayFrame frame, long worldTick) {
    for (RWRDisplayPoint point : frame.points) {
        // 投影计算（与GUI完全相同的数学）
        double innerRatio = 0.22, outerRatio = 0.88;
        double ringRadius = innerRatio + (outerRatio - innerRatio) * rangeNorm;
        int px = cx + (int)(cos(point.angleRad) * ringRadius * radius);
        int py = cy + (int)(sin(point.angleRad) * ringRadius * radius);
        // 绘制标签
        drawText(pixels, point.label, px, py, point.color, 1);
    }
}

private static void renderAlertPanel(int[] pixels, RWRDisplayFrame frame, long worldTick) {
    // 告警文字+底框
}
```

### 6.3 关于 `RWR_jammed.png` 的处理

如果选择方案B（ImageIO），可以预加载两张纹理：
- `RWR.png` → `backgroundPixels`（正常背景）
- `RWR_jammed.png` → `jammedPixels`（干扰背景）

干扰时直接复制 `jammedPixels` 替代 `backgroundPixels`，然后在其上叠加威胁点，得到与GUI**完全一致**的干扰效果。

### 6.4 纹理大小匹配

需要确认 RWR 纹理的实际尺寸。通过 `W_TextureUtil.getTextureInfo()` 可以在运行时获取：

```java
W_TextureUtil.TextureParam param = W_TextureUtil.getTextureInfo("mcheli", "textures/RWR.png");
// param.width, param.height
```

如果 RWR.png 不是 256×256，则 ImageIO 加载后需要缩放到 `TEX_SIZE`。缩放用 `BufferedImage.getScaledInstance()` 或 `Graphics2D.drawImage()` 即可。

## 7. 修改方案对比

| 方案 | 背景一致性 | 实现复杂度 | 运行时开销 | 资源包兼容 |
|------|-----------|-----------|-----------|-----------|
| A: glGetTexImage 回读 | ⭐⭐⭐⭐⭐ 最高 | 中（需FBO/回读） | 一次性 | ✅ |
| B: ImageIO 加载 | ⭐⭐⭐⭐⭐ 最高 | 低（标准ImageIO） | 一次性 | ✅ |
| C: 纯几何fallback | ⭐⭐ 差距大 | 低 | 无额外开销 | ✅ 不依赖资源 |
| **组合:B + C**（推荐） | ⭐⭐⭐⭐ | 中 | 一次性加载背景 | ✅ 优雅降级 |

**推荐方案**：**B为主 + C为fallback**。ImageIO 成功后直接显示 RWR.png 背景，失败时用纯几何圆环兜底，保证功能不中断。

## 8. 关键代码引用

| 文件 | 行号 | 内容 |
|------|------|------|
| [MCH_RenderRWR.java](file:///d:/MCHR/MCH-Reforged/src/main/java/mcheli/render/MCH_RenderRWR.java) | L56-L60 | RWR纹理ResourceLocation定义 |
| [MCH_RenderRWR.java](file:///d:/MCHR/MCH-Reforged/src/main/java/mcheli/render/MCH_RenderRWR.java) | L188-L198 | RWR GUI渲染主入口（背景圆环+干扰） |
| [MCH_RenderRWR.java](file:///d:/MCHR/MCH-Reforged/src/main/java/mcheli/render/MCH_RenderRWR.java) | L200-L258 | 威胁环投影与标签渲染（可移植的核心数学） |
| [MCH_RenderRWR.java](file:///d:/MCHR/MCH-Reforged/src/main/java/mcheli/render/MCH_RenderRWR.java) | L262-L278 | 威胁颜色映射（可移植） |
| [MCH_RenderRWR.java](file:///d:/MCHR/MCH-Reforged/src/main/java/mcheli/render/MCH_RenderRWR.java) | L726-L737 | drawRWRCircle() — 背景纹理Quad渲染 |
| [MCH_RenderRWR.java](file:///d:/MCHR/MCH-Reforged/src/main/java/mcheli/render/MCH_RenderRWR.java) | L1617-L1629 | drawRadarText() — FontRenderer文字 |
| [MCH_RenderRWR.java](file:///d:/MCHR/MCH-Reforged/src/main/java/mcheli/render/MCH_RenderRWR.java) | L501-L561 | drawRwrHudAlerts() — 告警面板 |
| [MCH_RenderRWR.java](file:///d:/MCHR/MCH-Reforged/src/main/java/mcheli/render/MCH_RenderRWR.java) | L610-L651 | drawRwrAlertMask() — 告警底框OpenGL渲染 |
| [MCH_RenderRWR.java](file:///d:/MCHR/MCH-Reforged/src/main/java/mcheli/render/MCH_RenderRWR.java) | L3295-L3492 | buildRadarDisplayFrame() — 参考：雷达帧数据构建 |
| [MCH_RadarDisplayTextureManager.java](file:///d:/MCHR/MCH-Reforged/src/main/java/mcheli/render/MCH_RadarDisplayTextureManager.java) | 全文 | 参考：DynamicTexture完整管理模式 |
| [MCH_RWRThreatClientTracker.java](file:///d:/MCHR/MCH-Reforged/src/main/java/mcheli/MCH_RWRThreatClientTracker.java) | 全文 | RWR威胁数据客户端缓存 |
| [MCH_RWRThreatEvent.java](file:///d:/MCHR/MCH-Reforged/src/main/java/mcheli/MCH_RWRThreatEvent.java) | 全文 | 威胁事件数据结构 |
| [W_TextureUtil.java](file:///d:/MCHR/MCH-Reforged/src/main/java/mcheli/wrapper/W_TextureUtil.java) | L14-L19 | 运行时获取纹理尺寸 |
| [MCH_RenderAircraft.java](file:///d:/MCHR/MCH-Reforged/src/main/java/mcheli/aircraft/MCH_RenderAircraft.java) | L1480-L1547 | 参考：$radardisplay 模型部件双Pass渲染 |

## 9. 实施路线图

```
Phase 1: 基础设施 (估算: 1天)
  ├─ 提取 MCH_TextureRenderUtil (公共像素操作函数)
  ├─ 在 MCH_RenderRWR 中新增 buildRWRDisplayFrame()
  └─ 在 MCH_RenderRWR 中新增 RWRDisplayPoint / RWRDisplayFrame 类

Phase 2: 纹理管理器 (估算: 1.5天)
  ├─ 新建 MCH_RWRDisplayTextureManager
  ├─ 实现 ImageIO 加载 RWR.png 到 backgroundPixels
  ├─ 实现图形层渲染 (威胁环投影)
  └─ 实现文字层渲染 (告警面板)

Phase 3: 模型集成 (估算: 0.5天)
  ├─ MCH_RenderAircraft.renderCommonPart() 增加 renderRWRDisplayPart()
  ├─ 实现双Pass渲染
  └─ 联调验证

Phase 4: 异常处理 (估算: 0.5天)
  ├─ ImageIO失败 → 纯几何fallback
  ├─ RWR关闭/无数据 → 空环显示
  └─ 纹理暖机过渡
```
