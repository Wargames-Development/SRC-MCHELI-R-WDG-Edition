# 座舱模型部件显示系统 —— 总实施文档

## 1. 概述

本文档整合以下三个功能的实施计划：

| 编号 | 功能 | 涉及组件 |
|------|------|---------|
| **F1** | 雷达面板 → `$radardisplay` 模型部件 | 已有（`MCH_RadarDisplayTextureManager`） |
| **F2** | RWR威胁环 → `$rwrdisplay` 模型部件 | 新建（`MCH_RWRDisplayTextureManager`） |
| **F3** | 通用改进：字符集扩充、ECM雪花、雷达开关控制 | 两者均需修改 |

## 2. 系统架构（总图）

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        MCH_RenderAircraft (每帧)                             │
├─────────────────────────────────────────────────────────────────────────────┤
│  renderCommonPart()                                                         │
│    ├─ renderRadarDisplayPart()  ← F1 (已有)                                 │
│    │    ├─ 检查 info.enableRadar && ac.isRadarEnabledRuntime()  ← F3.3      │
│    │    ├─ Pass1: 黑色不透明底                                              │
│    │    └─ Pass2: MCH_RadarDisplayTextureManager.getTexture()               │
│    │              → 含 ECM 雪花叠加  ← F3.2                                 │
│    │              → 扩充字符集     ← F3.1                                   │
│    └─ renderRWRDisplayPart()    ← F2 (新增)                                 │
│         ├─ 检查 ac.getAcInfo().hasRWR                                       │
│         ├─ Pass1: 黑色不透明底                                              │
│         └─ Pass2: MCH_RWRDisplayTextureManager.getTexture()                 │
│                   → 加载 RWR.png 背景     ← F2.2                            │
│                   → 含 ECM 雪花叠加       ← F3.2                            │
│                   → 扩充字符集           ← F3.1                             │
│                                                                             │
│  [颜色约定]                                                                  │
│  雷达面板: 固定翼/直升机 = 绿色(0x00FF00), 坦克/舰船 = 琥珀色(0xFFE400)       │
│  RWR面板:  追踪逻辑同GUI(绿搜索/橙跟踪/红STT/闪烁红导弹)                     │
└─────────────────────────────────────────────────────────────────────────────┘
```

## 3. 详细实施

### F1: 雷达面板 → `$radardisplay`（已有功能）

**状态**：已实现，需修改以下部分。

#### F1.1 修改点

| 修改项 | 文件 | 说明 |
|-------|------|------|
| 雷达开关检查 | `MCH_RenderAircraft.java` → `renderRadarDisplayPart()` | 当前在Pass 2中调用 `getTexture()`，但后者仅检查 `enableRadar`，未检查 `isRadarEnabledRuntime()`。需在Pass 2前加运行时检查 |
| 字符集扩充 | `MCH_RadarDisplayTextureManager.java` → `glyph()` | 扩充完整26字母和常见符号 |
| ECM雪花叠加 | `MCH_RadarDisplayTextureManager.java` → `getTexture()` / `renderGraphicsFrame()` | 在渲染完图形后检测 `ac.jammingTick > 0` 并叠加雪花噪点 |

**当前问题**：
- `renderRadarDisplayPart()` 不检查 `ac.isRadarEnabledRuntime()`。当玩家按 `~` 关闭雷达时，模型部件仍显示雷达画面。

#### F1.2 修复雷达开关控制

```java
// MCH_RenderAircraft.java renderRadarDisplayPart() 修改
private void renderRadarDisplayPart(MCH_EntityAircraft ac, MCH_AircraftInfo info, float tickTime) {
    if (ac == null || info == null || info.model == null || !(info.model instanceof W_ModelCustom)) {
        return;
    }
    // ★ 新增：检查雷达配置和运行时开关
    if (!info.enableRadar || !ac.isRadarEnabledRuntime()) {
        return;  // 雷达关闭或不支持，不渲染
    }
    // ... 后续不变
}
```

#### F1.3 雷达纹理管理器新增：ECM雪花效果

```java
// MCH_RadarDisplayTextureManager.java 修改

// getTexture() 新增参数（传递 jammingTick 信息）
public static ResourceLocation getTexture(MCH_EntityAircraft ac, EntityPlayer player, float partialTicks) {
    // ... 现有逻辑不变 ...

    renderGraphicsFrame(state.pixels, frame, ac);  // ★ 传入 ac 以读取 jammingTick
    // ... 后续不变 ...
}

// renderGraphicsFrame() 新增 ECM 雪花的叠加步骤
private static void renderGraphicsFrame(int[] pixels, MCH_RenderRWR.RadarDisplayFrame frame, MCH_EntityAircraft ac) {
    fill(pixels, CLEAR_COLOR);
    // ... 现有绘图逻辑不变 ...

    // ★ 最后一步：ECM 雪花叠加
    if (ac != null && ac.jammingTick > 0) {
        overlayECMSnow(pixels, ac.worldObj);
    }
}
```

### F2: RWR 威胁环 → `$rwrdisplay`（新建功能）

#### F2.1 新增文件

| 文件 | 路径 | 功能 |
|------|------|------|
| `MCH_RWRDisplayTextureManager.java` | `src/main/java/mcheli/render/` | RWR动态纹理管理器（256×256） |

#### F2.2 RWR 背景来源

采用**组合策略**，按优先级降序：

| 优先级 | 方案 | 描述 |
|--------|------|------|
| **P1** (主方案) | ImageIO 加载 RWR.png | 通过 `Minecraft.getResourceManager().getResource()` 获取 InputStream，`ImageIO.read()` 解码后缩放到256×256，存储为 `backgroundPixels` |
| **P2** (Fallback) | 纯几何渲染 | ImageIO失败时画黑色圆底、内/外圈环、4条方位基线 |

```java
private static void loadRWRBackground(RwrTexState state) {
    if (state.backgroundLoaded) return;
    try {
        // 根据载具类型选择对应的RWR纹理
        ResourceLocation rwrRes = selectRWRTexture(aircraftType);
        IResource resource = Minecraft.getMinecraft().getResourceManager().getResource(rwrRes);
        BufferedImage image = ImageIO.read(resource.getInputStream());
        BufferedImage scaled = new BufferedImage(TEX_SIZE, TEX_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = scaled.createGraphics();
        g.drawImage(image, 0, 0, TEX_SIZE, TEX_SIZE, null);
        g.dispose();
        state.backgroundPixels = scaled.getRGB(0, 0, TEX_SIZE, TEX_SIZE, null, 0, TEX_SIZE);
        state.backgroundLoaded = true;
    } catch (Exception e) {
        renderFallbackBackground(state);
        state.backgroundLoaded = true;
    }
}
```

#### F2.3 RWR 显示面板 → 模型部件

```java
// MCH_RenderAircraft.java — 新增方法
private void renderRWRDisplayPart(MCH_EntityAircraft ac, MCH_AircraftInfo info, float tickTime) {
    if (ac == null || info == null || info.model == null || !(info.model instanceof W_ModelCustom)) {
        return;
    }
    if (!info.hasRWR) {
        return;
    }
    W_ModelCustom bodyModel = (W_ModelCustom) info.model;
    String partName = "$rwrdisplay";
    if (!bodyModel.containsPart(partName)) {
        return;
    }

    GL11.glPushMatrix();
    GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT
            | GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_POLYGON_BIT | GL11.GL_TEXTURE_BIT);
    try {
        GL11.glDisable(GL11.GL_LIGHTING);

        // Pass 1: 黑色不透明底
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
        GL11.glPolygonOffset(-2.0F, -2.0F);
        GL11.glColor4f(0.0F, 0.0F, 0.0F, 1.0F);
        bodyModel.renderPart(partName);
        GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);

        // Pass 2: RWR 动态纹理
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        ResourceLocation rwrDynamicTex = MCH_RWRDisplayTextureManager.getTexture(ac, player, tickTime);
        if (rwrDynamicTex != null) {
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glDisable(GL11.GL_ALPHA_TEST);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDepthMask(false);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            this.bindTexture(rwrDynamicTex);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            bodyModel.renderPart(partName);
        }
    } finally {
        GL11.glPopAttrib();
        this.bindAircraftTexture(ac);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glPopMatrix();
    }
}
```

#### F2.4 RWR 纹理管理器核心结构

```java
// MCH_RWRDisplayTextureManager.java — 核心架构
public final class MCH_RWRDisplayTextureManager {
    private static final int TEX_SIZE = 256;
    private static final double RWR_UV_DIAMETER_RATIO = 0.70D;
    private static final int WARMUP_CHUNK_PIXELS = 8192;
    private static final int TEXT_UPDATE_INTERVAL_TICK = 10;
    private static final int CLEAR_COLOR = 0x00000000;

    private static final Map<Integer, RwrTexState> CACHE = new HashMap<>();

    public static ResourceLocation getTexture(MCH_EntityAircraft ac, EntityPlayer player, float partialTicks) {
        // 1. 前置检查：hasRWR
        if (ac == null || ac.getAcInfo() == null || !ac.getAcInfo().hasRWR) return null;

        RwrTexState state = getOrCreate(ac.getEntityId());
        long worldTick = ac.worldObj != null ? ac.worldObj.getTotalWorldTime() : 0L;

        // 2. 暖机
        if (!state.ready) { ... return null; }
        if (!state.clearUploaded) { ... return null; }

        // 3. 节流：每 tick 最多 1 次
        if (worldTick == state.lastUpdatePhaseKey && state.lastUpdatePhaseKey >= 0) {
            return state.location;
        }

        // 4. 构建帧数据（从 MCH_RWRThreatClientTracker 读取）
        MCH_RenderRWR.RWRDisplayFrame frame = MCH_RenderRWR.buildRWRDisplayFrame(ac, player, partialTicks);

        // 5. 加载背景（一次性的）
        if (!state.backgroundLoaded) {
            loadRWRBackground(state, selectRWRTexture(ac));
        }

        // 6. 合成画面
        renderGraphicsFrame(state.pixels, frame, state.backgroundPixels, worldTick);

        // 7. ECM 雪花叠加
        if (ac.jammingTick > 0) {
            overlayECMSnow(state.pixels, ac.worldObj, worldTick);
        }

        // 8. 文字层（含告警闪烁）
        if (shouldUpdateTextLayer(state, frame, worldTick)) {
            renderTextLayer(state.textPixels, frame, worldTick);
            state.lastTextTick = worldTick;
        }
        overlayTextLayer(state.pixels, state.textPixels);

        // 9. 上传
        state.texture.updateDynamicTexture();
        state.lastUpdatePhaseKey = worldTick;
        return state.location;
    }
}
```

#### F2.5 RWR 帧数据结构（在 MCH_RenderRWR.java 中新增）

```java
public static class RWRDisplayPoint {
    public double ringRadius;       // 归一化环半径 (0.22~0.88)
    public double angleRad;         // 角度（弧度）
    public int color;               // ARGB 颜色
    public String label;            // 威胁源标签
    public byte threatMode;         // 威胁等级
    public boolean isMissile;       // 导弹威胁
    public double distanceMeters;   // 距离（用于排序）
}

public static class RWRDisplayFrame {
    public MCH_EntityAircraft aircraft;
    public boolean valid = false;
    public boolean jammed = false;
    public String missileSourceName = "";
    public long missileUntilTick = -1L;
    public String lockSourceName = "";
    public long lockUntilTick = -1L;
    public final List<String> scanSources = new ArrayList<>();
    public final List<RWRDisplayPoint> points = new ArrayList<>();
    public int maxDistanceMeters = 4096;
}
```

#### F2.6 RWR 纹理管理器 —— 图形层渲染（威胁环投影）

威胁环的**坐标投影**与GUI完全一致：

```java
// 归一化映射 (与 renderRwrThreatRing 完全相同的数学)
private static void renderThreatRing(int[] pixels, RWRDisplayFrame frame, long worldTick) {
    int cx = TEX_SIZE / 2, cy = TEX_SIZE / 2;
    int radius = (int) (TEX_SIZE * (RWR_UV_DIAMETER_RATIO * 0.5D));
    double innerRatio = 0.22, outerRatio = 0.88;
    int innerRadius = (int) (radius * innerRatio);
    int outerRadius = (int) (radius * outerRatio);

    for (RWRDisplayPoint point : frame.points) {
        double normR = (point.ringRadius - innerRatio) / (outerRatio - innerRatio);
        normR = Math.max(0, Math.min(1, normR));
        double r = innerRadius + (outerRadius - innerRadius) * normR;
        int px = cx + (int) Math.round(Math.cos(point.angleRad) * r);
        int py = cy + (int) Math.round(Math.sin(point.angleRad) * r);

        // 画威胁点
        drawSquare(pixels, px, py, point.color, 2);

        // 画威胁标签 (用扩充后的点阵字体)
        drawText(pixels, point.label, px - 6, py - 8, point.color, 1);
    }
}
```

### F3.1: 通用字符集扩充

#### F3.1.1 当前状态

当前 `glyph()` 方法仅支持：
```
大写字母: A B C D E G I M R S T W          (12个)
数字:     0 1 2 3 4 5 6 7 8 9              (10个)
符号:     - * 空格                            (3个)
总计: 25 个字符
```

#### F3.1.2 扩充目标

扩充到**完整86个字符**：

```
大写字母: A B C D E F G H I J K L M N O P Q R S T U V W X Y Z   (26个, 新增14个)
小写字母: a b c d e f g h i j k l m n o p q r s t u v w x y z   (26个, 全部新增)
数字:     0 1 2 3 4 5 6 7 8 9                                    (10个, 已有)
符号:     - * _ . , / \ ( ) [ ] { } : ; ! ? @ # $ % ^ & + = < > | ~ ' " 空格  (27个, 大部分新增)
总计: 89 个字符
```

> **设计**：小写字母与对应大写字母采用相同的字形（`toUpperCase` 后再查 glyph，节省工作量）。
> 如果希望有明显的大小写区分，可单独设计小写字形（例如 `a` 为 `011 101 111 101 101`）。

#### F3.1.3 扩充后的 glyph 定义

```java
private static int[] glyph(char c) {
    switch (c) {
        // ====== 原有 26 个大写字母 (已定义为行, 补齐全) ======
        case 'A': return new int[]{0b010, 0b101, 0b111, 0b101, 0b101};
        case 'B': return new int[]{0b110, 0b101, 0b110, 0b101, 0b110};
        case 'C': return new int[]{0b011, 0b100, 0b100, 0b100, 0b011};
        case 'D': return new int[]{0b110, 0b101, 0b101, 0b101, 0b110};
        case 'E': return new int[]{0b111, 0b100, 0b110, 0b100, 0b111};
        case 'F': return new int[]{0b111, 0b100, 0b110, 0b100, 0b100};
        case 'G': return new int[]{0b011, 0b100, 0b101, 0b101, 0b011};
        case 'H': return new int[]{0b101, 0b101, 0b111, 0b101, 0b101};
        case 'I': return new int[]{0b111, 0b010, 0b010, 0b010, 0b111};
        case 'J': return new int[]{0b001, 0b001, 0b001, 0b101, 0b011};
        case 'K': return new int[]{0b101, 0b110, 0b100, 0b110, 0b101};
        case 'L': return new int[]{0b100, 0b100, 0b100, 0b100, 0b111};
        case 'M': return new int[]{0b101, 0b111, 0b111, 0b101, 0b101};
        case 'N': return new int[]{0b101, 0b111, 0b111, 0b111, 0b101}; // 近似：简单版
        case 'O': return new int[]{0b011, 0b101, 0b101, 0b101, 0b011};
        case 'P': return new int[]{0b110, 0b101, 0b110, 0b100, 0b100};
        case 'Q': return new int[]{0b011, 0b101, 0b101, 0b011, 0b001};
        case 'R': return new int[]{0b110, 0b101, 0b110, 0b101, 0b101};
        case 'S': return new int[]{0b011, 0b100, 0b011, 0b001, 0b110};
        case 'T': return new int[]{0b111, 0b010, 0b010, 0b010, 0b010};
        case 'U': return new int[]{0b101, 0b101, 0b101, 0b101, 0b011};
        case 'V': return new int[]{0b101, 0b101, 0b101, 0b010, 0b010};
        case 'W': return new int[]{0b101, 0b101, 0b111, 0b111, 0b101};
        case 'X': return new int[]{0b101, 0b101, 0b010, 0b101, 0b101};
        case 'Y': return new int[]{0b101, 0b101, 0b010, 0b010, 0b010};
        case 'Z': return new int[]{0b111, 0b001, 0b010, 0b100, 0b111};

        // ====== 小写字母: 使用 `toUpperCase` 映射, 也可设计专用字形 ======
        case 'a': return new int[]{0b011, 0b101, 0b111, 0b101, 0b101}; // 同A
        case 'b': return new int[]{0b110, 0b101, 0b110, 0b101, 0b110}; // 同B
        case 'c': return new int[]{0b011, 0b100, 0b100, 0b100, 0b011}; // 同C
        case 'd': return new int[]{0b110, 0b101, 0b101, 0b101, 0b110}; // 同D
        case 'e': return new int[]{0b111, 0b100, 0b110, 0b100, 0b111}; // 同E
        case 'f': return new int[]{0b111, 0b100, 0b110, 0b100, 0b100}; // 同F
        case 'g': return new int[]{0b011, 0b100, 0b101, 0b101, 0b011}; // 同G
        case 'h': return new int[]{0b101, 0b101, 0b111, 0b101, 0b101}; // 同H
        case 'i': return new int[]{0b111, 0b010, 0b010, 0b010, 0b111}; // 同I
        case 'j': return new int[]{0b001, 0b001, 0b001, 0b101, 0b011}; // 同J
        case 'k': return new int[]{0b101, 0b110, 0b100, 0b110, 0b101}; // 同K
        case 'l': return new int[]{0b100, 0b100, 0b100, 0b100, 0b111}; // 同L
        case 'm': return new int[]{0b101, 0b111, 0b111, 0b101, 0b101}; // 同M
        case 'n': return new int[]{0b101, 0b111, 0b111, 0b111, 0b101}; // 同N
        case 'o': return new int[]{0b011, 0b101, 0b101, 0b101, 0b011}; // 同O
        case 'p': return new int[]{0b110, 0b101, 0b110, 0b100, 0b100}; // 同P
        case 'q': return new int[]{0b011, 0b101, 0b101, 0b011, 0b001}; // 同Q
        case 'r': return new int[]{0b110, 0b101, 0b110, 0b101, 0b101}; // 同R
        case 's': return new int[]{0b011, 0b100, 0b011, 0b001, 0b110}; // 同S
        case 't': return new int[]{0b111, 0b010, 0b010, 0b010, 0b010}; // 同T
        case 'u': return new int[]{0b101, 0b101, 0b101, 0b101, 0b011}; // 同U
        case 'v': return new int[]{0b101, 0b101, 0b101, 0b010, 0b010}; // 同V
        case 'w': return new int[]{0b101, 0b101, 0b111, 0b111, 0b101}; // 同W
        case 'x': return new int[]{0b101, 0b101, 0b010, 0b101, 0b101}; // 同X
        case 'y': return new int[]{0b101, 0b101, 0b010, 0b010, 0b010}; // 同Y
        case 'z': return new int[]{0b111, 0b001, 0b010, 0b100, 0b111}; // 同Z

        // ====== 数字 ======
        case '0': return new int[]{0b011, 0b101, 0b101, 0b101, 0b011};
        case '1': return new int[]{0b010, 0b110, 0b010, 0b010, 0b111};
        case '2': return new int[]{0b111, 0b001, 0b111, 0b100, 0b111};
        case '3': return new int[]{0b111, 0b001, 0b111, 0b001, 0b111};
        case '4': return new int[]{0b101, 0b101, 0b111, 0b001, 0b001};
        case '5': return new int[]{0b111, 0b100, 0b111, 0b001, 0b111};
        case '6': return new int[]{0b111, 0b100, 0b111, 0b101, 0b111};
        case '7': return new int[]{0b111, 0b001, 0b010, 0b010, 0b010};
        case '8': return new int[]{0b111, 0b101, 0b111, 0b101, 0b111};
        case '9': return new int[]{0b111, 0b101, 0b111, 0b001, 0b111};

        // ====== 符号 ======
        case ' ':  return new int[]{0b000, 0b000, 0b000, 0b000, 0b000};
        case '-':  return new int[]{0b000, 0b000, 0b111, 0b000, 0b000};
        case '_':  return new int[]{0b000, 0b000, 0b000, 0b000, 0b111};
        case '*':  return new int[]{0b101, 0b010, 0b111, 0b010, 0b101};
        case '.':  return new int[]{0b000, 0b000, 0b000, 0b000, 0b010};
        case ',':  return new int[]{0b000, 0b000, 0b000, 0b010, 0b100};
        case '/':  return new int[]{0b001, 0b001, 0b010, 0b100, 0b100};
        case '\\': return new int[]{0b100, 0b100, 0b010, 0b001, 0b001};
        case '(':  return new int[]{0b001, 0b010, 0b010, 0b010, 0b001};
        case ')':  return new int[]{0b100, 0b010, 0b010, 0b010, 0b100};
        case '[':  return new int[]{0b011, 0b010, 0b010, 0b010, 0b011};
        case ']':  return new int[]{0b110, 0b010, 0b010, 0b010, 0b110};
        case '{':  return new int[]{0b001, 0b010, 0b011, 0b010, 0b001};
        case '}':  return new int[]{0b100, 0b010, 0b110, 0b010, 0b100};
        case ':':  return new int[]{0b000, 0b010, 0b000, 0b010, 0b000};
        case ';':  return new int[]{0b000, 0b010, 0b000, 0b010, 0b100};
        case '!':  return new int[]{0b010, 0b010, 0b010, 0b000, 0b010};
        case '?':  return new int[]{0b111, 0b001, 0b010, 0b000, 0b010};
        case '@':  return new int[]{0b011, 0b101, 0b111, 0b100, 0b011};
        case '#':  return new int[]{0b101, 0b111, 0b101, 0b111, 0b101};
        case '$':  return new int[]{0b010, 0b111, 0b110, 0b011, 0b010};
        case '%':  return new int[]{0b100, 0b101, 0b010, 0b101, 0b001};
        case '^':  return new int[]{0b010, 0b101, 0b000, 0b000, 0b000};
        case '&':  return new int[]{0b010, 0b101, 0b010, 0b101, 0b011};
        case '+':  return new int[]{0b000, 0b010, 0b111, 0b010, 0b000};
        case '=':  return new int[]{0b000, 0b111, 0b000, 0b111, 0b000};
        case '<':  return new int[]{0b001, 0b010, 0b100, 0b010, 0b001};
        case '>':  return new int[]{0b100, 0b010, 0b001, 0b010, 0b100};
        case '|':  return new int[]{0b010, 0b010, 0b010, 0b010, 0b010};
        case '~':  return new int[]{0b000, 0b101, 0b010, 0b101, 0b000};
        case '\'': return new int[]{0b010, 0b010, 0b000, 0b000, 0b000};
        case '"':  return new int[]{0b101, 0b101, 0b000, 0b000, 0b000};
        case '`':  return new int[]{0b100, 0b010, 0b000, 0b000, 0b000};

        default: return null;
    }
}
```

> 字形为 3×5 点阵，每行用一个3位整数表示（二进制从高位到低位对应左→右）。
> 例如 `0b101` 表示该行的第1、3列为填充，第2列为空白。

#### F3.1.3 小写字母的处理策略

在 `drawText()` 中，小写字母可以直接用 `toUpperCase()` 映射，但视觉效果上大写字母偏高、偏大。为了提升可读性可：

**策略A（简单）**：对 `Character.toUpperCase(c)` 后查 glyph。字母 `ac.` 中的 `c` 会显示为大写 `C`，视觉一致但部分场景不自然。

**策略B（推荐）**：在 `drawText()` 中处理小写字母时，将字形下移1行并在第4、5行绘制：

```java
// drawText() 修改
for (int i = 0; i < text.length(); i++) {
    char c = text.charAt(i);
    boolean isLower = Character.isLowerCase(c);
    int[] g = glyph(Character.toUpperCase(c));
    if (g == null) {
        cx += 4 * s;
        continue;
    }
    int rowOffset = isLower ? 1 : 0; // 小写下移1行
    for (int row = 0; row < g.length; row++) {
        int bits = g[row];
        for (int col = 0; col < 3; col++) {
            if (((bits >> (2 - col)) & 1) != 0) {
                fillRectBlend(pixels, cx + col * s, y + (row + rowOffset) * s, s, s, color);
            }
        }
    }
    cx += 4 * s;
}
```

#### F3.1.4 修改范围

| 文件 | 修改内容 |
|------|---------|
| `MCH_RadarDisplayTextureManager.java` | 替换 `glyph()` 方法（扩充到89字符）；`drawText()` 增加小写偏移逻辑 |
| `MCH_RWRDisplayTextureManager.java` | 复用扩充后的 `glyph()` / `drawText()`（两个纹理管理器共享） |

#### F3.1.5 代码复用

建议将 `glyph()` 和 `drawText()` 等相关函数提取到 `MCH_TextureRenderUtil` 公共工具类，两个纹理管理器统一调用。

### F3.2: ECM 干扰雪花屏

#### F3.2.1 需求

当载具受到ECM干扰（`ac.jammingTick > 0`）时，模型部件的雷达和RWR显示叠加**雪花噪点**效果，与屏幕HUD的干扰纹理（RWR_jammed.png）视觉同步。

#### F3.2.2 雪花噪点生成算法

```java
/**
 * 叠加 ECM 雪花噪点到像素数组上
 *
 * @param pixels   目标像素数组（ARGB格式）
 * @param world    世界引用（用于随机种子）
 * @param worldTick 当前 tick（使雪花动态变化）
 */
private static void overlayECMSnow(int[] pixels, World world, long worldTick) {
    if (pixels == null) return;

    // 雪花密度：约 15% 的像素被噪点覆盖
    final double DENSITY = 0.15;
    // 固定种子基于 tick，使雪花每 tick 变化但同一帧内一致
    Random rand = new Random(worldTick * 31L);

    for (int i = 0; i < pixels.length; i++) {
        if (rand.nextDouble() >= DENSITY) continue;

        // 在目标像素上覆盖半透明 白/灰 噪点
        int srcColor = pixels[i];
        int alpha = 120 + rand.nextInt(80);   // 半透明度: 120~199
        int gray = 180 + rand.nextInt(76);    // 灰度: 180~255
        int noise = (alpha << 24) | (gray << 16) | (gray << 8) | gray;

        // 与原有颜色混合（保留原有颜色的部分亮度）
        blendPixelByIndex(pixels, i, noise);
    }
}
```

#### F3.2.3 与 RWR_jammed.png 的关系

| 维度 | 屏幕HUD (RWR_jammed.png) | 模型部件 (ECM雪花方案) |
|------|--------------------------|----------------------|
| 视觉 | 红色条纹交错 | 白色/灰色雪花点 |
| 颜色 | 红色色调（`0xFF0000` base） | 中性灰度（`0x####`） |
| 动态 | 静态纹理 | 每 tick 随机变化（动画） |
| 适用场景 | RWR专用 | 通用（雷达+RWR均可用） |

> 若希望模型部件雪花与 HUD RWR 的红色干扰条纹一致，可改用红色噪点：
> ```java
> int noise = (alpha << 24) | (gray << 16) | (0 << 8) | 0;  // 红色噪点
> ```

#### F3.2.4 干扰纹理（针对 RWR 背景的专用叠加）

如果 RWR 模型部件背景是通过 ImageIO 加载了 `RWR_jammed.png`，干扰时可以**直接切换背景**：

```java
private static void renderGraphicsFrame(int[] pixels, MCH_RenderRWR.RWRDisplayFrame frame,
                                         int[] backgroundPixels, int[] jammedPixels,
                                         boolean jammed) {
    // 选择背景：正常 or 干扰
    int[] bg = jammed && jammedPixels != null ? jammedPixels : backgroundPixels;
    System.arraycopy(bg, 0, pixels, 0, pixels.length);

    // ... 在背景上绘制威胁点/标签 ...
}
```

此方案要求同时加载 `RWR.png` 和 `RWR_jammed.png` 两张纹理。

#### F3.2.5 修改范围

| 文件 | 修改内容 |
|------|---------|
| `MCH_RadarDisplayTextureManager.java` | `getTexture()` 在最后叠加前检查 `ac.jammingTick > 0`，调用 `overlayECMSnow()` |
| `MCH_RWRDisplayTextureManager.java` | 同上，或直接切换加载 `RWR_jammed.png` 作为干扰背景 |

### F3.3: 雷达开关控制模型部件渲染

#### F3.3.1 需求

当玩家按 `~` 键（`KeyRadarSwitch`）关闭雷达时，模型部件上的雷达面板不再渲染。

#### F3.3.2 当前行为

```
按键 ~ → ac.setRadarEnabledRuntime(false)
         → MCH_RenderRWR.handleRadarPowerStateChanged(ac, false)
            → 清除跟踪状态
         → 屏幕HUD不再显示雷达面板
         → ❌ 但 $radardisplay 仍在渲染（因为 renderRadarDisplayPart() 未检查 isRadarEnabledRuntime()）
```

#### F3.3.3 修复方案

```java
// MCH_RenderAircraft.java — renderRadarDisplayPart() 修改
private void renderRadarDisplayPart(MCH_EntityAircraft ac, MCH_AircraftInfo info, float tickTime) {
    // ... 前置检查不变 ...

    // ★ 新增运行时检查
    if (!info.enableRadar || !ac.isRadarEnabledRuntime()) {
        return;  // 雷达未开启，不渲染模型部件
    }

    // ... 后续渲染逻辑不变 ...
}
```

#### F3.3.4 `$rwrdisplay` 的开关控制

RWR没有独立的运行时开关。`hasRWR` 是配置属性，不能动态关闭。但 RWR 显示依赖于：
1. 数据来源：`MCH_RWRThreatClientTracker`（客户端缓存，无数据时显示空环）
2. 纹理管理器返回 `null` 时，Pass 2 跳过，仅显示黑底

当无威胁数据时，RWR 显示为空环（背景 + 无威胁点），这是合理行为。

#### F3.3.5 修改范围

| 文件 | 修改内容 |
|------|---------|
| `MCH_RenderAircraft.java` | `renderRadarDisplayPart()` 增加 `ac.isRadarEnabledRuntime()` 检查 |

## 4. 修改文件清单

### 4.1 修改已有文件（3个）

| 文件 | 路径 | 修改点 |
|------|------|--------|
| `MCH_RenderAircraft.java` | `src/main/java/mcheli/aircraft/` | ① `renderCommonPart()` 新增 `renderRWRDisplayPart()` 调用；② 修改 `renderRadarDisplayPart()` 增加 `isRadarEnabledRuntime()` 检查；③ 新增 `renderRWRDisplayPart()` 方法 |
| `MCH_RadarDisplayTextureManager.java` | `src/main/java/mcheli/render/` | ① `glyph()` 扩充至89字符；② `drawText()` 增加小写偏移；③ `renderGraphicsFrame()` 增加 ECM 雪花叠加；④ `getTexture()` 中传递 `ac.jammingTick` |
| `MCH_RenderRWR.java` | `src/main/java/mcheli/render/` | ① 新增 `RWRDisplayPoint` 内部类；② 新增 `RWRDisplayFrame` 内部类；③ 新增 `buildRWRDisplayFrame()` 方法 |

### 4.2 新增文件（1个）

| 文件 | 路径 | 内容 |
|------|------|------|
| `MCH_RWRDisplayTextureManager.java` | `src/main/java/mcheli/render/` | 完整RWR纹理管理器，约 400 行 |

### 4.3 建议共用工具类（1个，可选）

| 文件 | 路径 | 内容 |
|------|------|------|
| `MCH_TextureRenderUtil.java` | `src/main/java/mcheli/render/` | 提取共用函数：`glyph()`、`drawText()`、`textWidth()`、`fill()`、`fillCircle()`、`drawRing()`、`drawLine()`、`drawSquare()`、`setPixel()`、`blendPixel()`、`blendPixelByIndex()`、`overlayTextLayer()`、`withAlpha()`、`clamp()`、`fillRectBlend()` 等 |

## 5. 字符集对比总结

| 类别 | 原字符集 L25 | 扩充后 L89 |
|------|-------------|-----------|
| 大写字母 | A B C D E G I M R S T W (12) | A-Z (26) |
| 小写字母 | ❌ 无 | a-z (26, 映射大写) |
| 数字 | 0-9 (10) | 0-9 (10) |
| 符号 | - * (2) | - _ * . , / \ ( ) [ ] { } : ; ! ? @ # $ % ^ & + = < > \| ~ ' " ` 空格 (27) |
| 总计 | 24 | 89 |

## 6. 实施路线图

```
Phase 1: 共用工具提取 (估算: 0.5天)
├─ 创建 MCH_TextureRenderUtil.java
└─ 将 glyph()、drawText() 等底层函数从 MCH_RadarDisplayTextureManager 提取到工具类

Phase 2: 字符集扩充 (估算: 0.5天)
├─ 扩充 MCH_TextureRenderUtil.glyph() 到89字符
├─ 优化 drawText() 支持小写字母偏移
└─ 两个纹理管理器切换为调用工具类

Phase 3: RWR纹理管理器 (估算: 1.5天)
├─ 实现 MCH_RWRDisplayTextureManager 完整功能
├─ ImageIO 加载 RWR.png 背景
├─ 威胁环投影渲染
├─ 告警面板渲染
└─ MCH_RenderRWR.buildRWRDisplayFrame() 实现

Phase 4: 模型集成 (估算: 0.5天)
├─ MCH_RenderAircraft.renderCommonPart() 增加 renderRWRDisplayPart()
├─ renderRadarDisplayPart() 增加 isRadarEnabledRuntime() 检查
└─ 复测

Phase 5: ECM 雪花屏 (估算: 0.5天)
├─ overlayECMSnow() 实现（两纹理管理器均调用）
└─ RWR 还可选加载 RWR_jammed.png 作为干扰背景

总计: 约 3.5 天
```

## 7. 注意事项

1. **字符集兼容性**：扩充 `glyph()` 后，旧的3×5点阵字形定义会被替换。如果原有字符宽度定义发生变化（新旧保持一致），所有文字渲染不受影响。

2. **ImageIO 依赖**：`javax.imageio.ImageIO` 是 Java SE 标准库，MC 1.7.10（Java 8）自带，无需额外依赖。

3. **ECM雪花性能**：`overlayECMSnow()` 每帧遍历 256×256=65536 像素，每像素约 15% 概率写入，平均每帧约 9800 次像素写入操作。性能开销可忽略。

4. **纹理缓存生命周期**：两个纹理管理器的静态 `CACHE` 均以 `aircraftId` 为键。当飞行器被摧毁/卸载时，缓存仍在内存中。建议在 `EntityLeaveWorldEvent` 或定时清理中移除对应条目。

5. **`$rwrdisplay` 模型部件命名**：需与3D模型制作者约定部件名称为 **`$rwrdisplay`**（全小写、美元符号开头），大小写敏感。
