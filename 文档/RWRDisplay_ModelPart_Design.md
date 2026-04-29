# RWR 映射到 `$rwrdisplay` 模型部件设计方案

## 1. 目标

将 RWR（雷达告警接收机）的威胁环画面动态渲染到飞机/载具3D模型上名为 `$rwrdisplay` 的部件，实现座舱仪表板上的 RWR 物理显示器。

## 2. 设计原则

完全镜像 `$radardisplay`（雷达面板）的实现模式：
- **数据源共享**：与屏幕HUD RWR使用同一份 `MCH_RWRThreatClientTracker` 数据
- **独立纹理管理**：新建 `MCH_RWRDisplayTextureManager`，不与雷达纹理管理器混用
- **CPU软件渲染**：纯像素数组操作 → `DynamicTexture`，不依赖OpenGL状态机
- **复用渲染管线**：模型集成方式与 `$radardisplay` 完全一致（双Pass、check `containsPart`）

## 3. 整体架构

```
┌──────────────────────────────────────────────────────────────────────────┐
│                           每帧渲染流程                                     │
├──────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  MCH_RenderAircraft.doRender()                                           │
│    └─ renderCommonPart()                                                 │
│         ├─ renderRadarDisplayPart()     ← $radardisplay (已有)           │
│         └─ renderRWRDisplayPart()       ← $rwrdisplay   (新增)           │
│              ├─ 检查模型是否有 "$rwrdisplay" 部件                          │
│              ├─ Pass 1: 渲染不透明黑底                                     │
│              └─ Pass 2: 调用 MCH_RWRDisplayTextureManager.getTexture()    │
│                         获取动态纹理并渲染到部件上                          │
│                                                                          │
│  MCH_RWRDisplayTextureManager.getTexture()   (新增文件)                   │
│    ├─ 调用 MCH_RenderRWR.buildRWRDisplayFrame() 构建帧数据                │
│    ├─ 软件渲染图形层（RWR圆环、威胁标记、标签文字）                          │
│    ├─ 软件渲染告警层（导弹告警闪烁、锁定告警闪烁）                          │
│    ├─ 合成告警层到图形层                                                   │
│    └─ 上传到 DynamicTexture，返回 ResourceLocation                         │
│                                                                          │
│  MCH_RenderRWR.buildRWRDisplayFrame()   (新增方法)                        │
│    └─ 读取 MCH_RWRThreatClientTracker.getEvents()                        │
│        为每个威胁事件生成 RWRDisplayPoint（归一化坐标 + 颜色 + 标签）        │
│                                                                          │
└──────────────────────────────────────────────────────────────────────────┘
```

### 3.1 与 $radardisplay 的关键差异

| 维度 | $radardisplay | $rwrdisplay |
|------|--------------|-------------|
| 数据源 | `radarContactCache`（主动雷达扫描） | `MCH_RWRThreatClientTracker`（被动告警接收） |
| 纹理更新频率 | 每 tick 最多 4 次 | 每 tick 1 次（RWR数据变化慢） |
| 视觉风格 | 扇形 + 扫掠线 + 目标方点 | 圆环 + 威胁标签（文字） |
| 颜色体系 | 单色系（绿/琥珀） | 多色（绿搜索 / 橙跟踪 / 红STT / 闪烁红导弹） |
| 告警闪烁 | 无 | 导弹/锁定告警闪烁效果 |
| 文字渲染 | 硬编码3×5点阵字体 | 硬编码3×5点阵字体（复用） |
| 干扰叠加 | 无 | 被干扰时全屏覆盖干扰纹理 |

## 4. 新增/修改文件清单

### 4.1 新增文件

| 文件 | 路径 | 说明 |
|------|------|------|
| `MCH_RWRDisplayTextureManager.java` | `src/main/java/mcheli/render/` | RWR动态纹理管理器，负责256×256 CPU软件渲染 |

### 4.2 修改文件

| 文件 | 修改位置 | 说明 |
|------|---------|------|
| `MCH_RenderAircraft.java` | `renderCommonPart()` | 新增 `renderRWRDisplayPart()` 调用 |
| `MCH_RenderAircraft.java` | 新增方法 | 实现 `renderRWRDisplayPart()` 双Pass渲染 |
| `MCH_RenderRWR.java` | 新增方法 | 实现 `buildRWRDisplayFrame()` 构建帧数据 |
| `MCH_RenderRWR.java` | 新增内部类 | `RWRDisplayPoint`、`RWRDisplayFrame` 数据结构 |

## 5. 数据结构设计

### 5.1 RWRDisplayPoint（在 MCH_RenderRWR 中新增）

```java
public static class RWRDisplayPoint {
    /** 归一化坐标：圆环上位置（半径方向 0.22~0.88） */
    public double ringRadius;
    /** 角度（弧度），0=上方，顺时针 */
    public double angleRad;
    /** 威胁颜色（ARGB） */
    public int color;
    /** 威胁源标签（如 "29"、"SA-6"） */
    public String label;
    /** 威胁等级 */
    public byte threatMode;
    /** 是否为导弹威胁（用于闪烁判定） */
    public boolean isMissile;
    /** 距离（米），用于排序 */
    public double distanceMeters;
}
```

### 5.2 RWRDisplayFrame（在 MCH_RenderRWR 中新增）

```java
public static class RWRDisplayFrame {
    public MCH_EntityAircraft aircraft;
    public boolean valid = false;
    /** 是否被干扰 */
    public boolean jammed = false;
    /** 导弹告警激活（有源标签名） */
    public String missileSourceName = "";
    public long missileUntilTick = -1L;
    /** 锁定告警激活 */
    public String lockSourceName = "";
    public long lockUntilTick = -1L;
    /** 搜索扫描事件（最多4行） */
    public final List<String> scanSources = new ArrayList<String>();
    /** 威胁标记列表 */
    public final List<RWRDisplayPoint> points = new ArrayList<RWRDisplayPoint>();
    /** RWR 类型 */
    public String rwrType = "DIGITAL"; // DIGITAL / ANALOG
    /** 最大显示距离 */
    public int maxDistanceMeters = 4096;
}
```

## 6. 核心方法实现

### 6.1 buildRWRDisplayFrame()（MCH_RenderRWR 新增）

```java
public static RWRDisplayFrame buildRWRDisplayFrame(MCH_EntityAircraft ac, EntityPlayer player, float partialTicks) {
    RWRDisplayFrame frame = new RWRDisplayFrame();
    frame.aircraft = ac;

    if (ac == null || ac.getAcInfo() == null || !ac.getAcInfo().hasRWR) {
        return frame;
    }

    long nowTick = ac.worldObj != null ? ac.worldObj.getTotalWorldTime() : 0L;

    // 1. 获取RWR威胁表数据
    MCH_RWRThreatTable table = MCH_RWRThreatClientTracker.getTable(ac.getEntityId());
    RwrHudState hudState = updateRwrHudState(ac, nowTick);  // 复用现有方法

    // 2. 干扰状态
    frame.jammed = ac.jammingTick > 0;

    // 3. 告警状态
    if (hudState != null) {
        if (hudState.missileUntilTick >= nowTick && !hudState.missileSourceName.isEmpty()) {
            frame.missileSourceName = hudState.missileSourceName;
            frame.missileUntilTick = hudState.missileUntilTick;
        }
        if (hudState.lockUntilTick >= nowTick && !hudState.lockSourceName.isEmpty()) {
            frame.lockSourceName = hudState.lockSourceName;
            frame.lockUntilTick = hudState.lockUntilTick;
        }
        for (Map.Entry<String, Long> e : hudState.scanEvents.entrySet()) {
            if (e.getValue() >= nowTick) {
                frame.scanSources.add(e.getKey());
            }
        }
    }

    // 4. 计算距离范围
    double minDistance = _MIN_DISTANCE;       // 50m
    double maxDistance = _RWR_RING_MAX_DISTANCE; // 4096m
    if (ac.getAcInfo().radarMaxTargetRange > 0.0F) {
        maxDistance = Math.min(maxDistance, ac.getAcInfo().radarMaxTargetRange);
    }
    if (maxDistance <= minDistance) {
        minDistance = Math.max(0.0D, maxDistance - 50.0D);
    }
    frame.maxDistanceMeters = (int) Math.round(maxDistance);

    // 5. 构建威胁标记点列表
    if (table != null && table.events != null) {
        // 环的内外半径比（与屏幕HUD RWR一致: 内圈0.22, 外圈0.88）
        double innerRingRatio = 0.22D;
        double outerRingRatio = 0.88D;

        for (MCH_RWRThreatEvent evt : table.events) {
            if (evt == null || evt.emitterId == ac.getEntityId()) {
                continue;
            }
            String name = normalizeRwrSourceName(evt.sourceName);
            if (name.isEmpty() || "?".equals(name)) {
                continue;
            }

            // 距离映射：近距离→外圈，远距离→内圈
            double distance = evt.distanceMeters > 0.0F ? evt.distanceMeters
                : (maxDistance - MCH_RWRThreatEvent.clamp01(evt.strength) * (maxDistance - minDistance));
            if (distance <= 1.0E-4D) {
                continue;
            }
            distance = Math.max(minDistance, Math.min(maxDistance, distance));
            double rangeNorm = (distance - minDistance) / Math.max(1.0D, maxDistance - minDistance);
            rangeNorm = Math.max(0.0D, Math.min(1.0D, rangeNorm));
            double ringRadius = innerRingRatio + (outerRingRatio - innerRingRatio) * rangeNorm;

            // 方位映射：bearingDeg 是相对于机头的角度，0=正前方，顺时针
            // 显示时上方(0°)对应bearing=0，RWR传统是上北下南
            double angleRad = Math.toRadians(evt.bearingDeg - 90.0D);  // -90°把正前映射到上方

            boolean isMsl = evt.threatMode == MCH_RWRThreatEvent.MODE_MSL_ACTIVE
                         || evt.threatMode == MCH_RWRThreatEvent.MODE_MSL_DATALINK;
            int color = resolveThreatColorForRWR(evt, nowTick, hudState);

            RWRDisplayPoint point = new RWRDisplayPoint();
            point.ringRadius = ringRadius;
            point.angleRad = angleRad;
            point.color = color;
            point.label = name;
            point.threatMode = evt.threatMode;
            point.isMissile = isMsl;
            point.distanceMeters = distance;
            frame.points.add(point);
        }

        // 按距离排序（远的先画，近的覆盖在上面）
        Collections.sort(frame.points, new Comparator<RWRDisplayPoint>() {
            @Override
            public int compare(RWRDisplayPoint a, RWRDisplayPoint b) {
                return Double.compare(b.distanceMeters, a.distanceMeters);
            }
        });

        // 限制最多12个威胁标记
        while (frame.points.size() > 12) {
            frame.points.remove(frame.points.size() - 1);
        }
    }

    frame.valid = true;
    return frame;
}
```

### 6.2 renderRWRDisplayPart()（MCH_RenderAircraft 新增）

```java
private void renderRWRDisplayPart(MCH_EntityAircraft ac, MCH_AircraftInfo info, float tickTime) {
    // 前提检查
    if (ac == null || info == null || info.model == null || !(info.model instanceof W_ModelCustom)) {
        return;
    }
    if (!ac.getAcInfo().hasRWR) {
        return;  // 没有配置RWR的载具跳过
    }
    W_ModelCustom bodyModel = (W_ModelCustom) info.model;
    String partName = "$rwrdisplay";
    if (!bodyModel.containsPart(partName)) {
        return;  // 模型没有此部件，跳过
    }

    GL11.glPushMatrix();
    GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT
                    | GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_POLYGON_BIT | GL11.GL_TEXTURE_BIT);
    try {
        GL11.glDisable(GL11.GL_LIGHTING);

        // Pass 1: 黑色不透明底（覆盖模型原始纹理）
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
        GL11.glPolygonOffset(-2.0F, -2.0F);
        GL11.glColor4f(0.0F, 0.0F, 0.0F, 1.0F);
        bodyModel.renderPart(partName);
        GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);

        // Pass 2: RWR动态纹理叠加
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

### 6.3 MCH_RWRDisplayTextureManager 完整设计

```java
package mcheli.render;

import mcheli.aircraft.MCH_EntityAircraft;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

import java.util.*;

public final class MCH_RWRDisplayTextureManager {

    // ============ 常量 ============
    private static final int TEX_SIZE = 256;
    /** RWR圆环直径占纹理比例 */
    private static final double RWR_UV_DIAMETER_RATIO = 0.70D;
    /** RWR内圈半径比（最远距离） */
    private static final double RWR_INNER_RING_RATIO = 0.22D;
    /** RWR外圈半径比（最近距离） */
    private static final double RWR_OUTER_RING_RATIO = 0.88D;
    /** 威胁标记点大小（像素） */
    private static final int THREAT_POINT_RADIUS = 2;
    private static final int CLEAR_COLOR = 0x00000000;
    /** 文字更新间隔 tick */
    private static final int TEXT_UPDATE_INTERVAL_TICK = 10;
    /** 威胁纹理暖机每帧处理像素数 */
    private static final int WARMUP_CHUNK_PIXELS = 8192;

    // ============ 颜色定义 ============
    /** RWR圆环底色 */
    private static final int RING_BG_COLOR = 0x1A000000;     // 极暗透黑
    /** RWR圆环线颜色 */
    private static final int RING_LINE_COLOR = 0x80000000;   // 半透明灰
    /** RWR基线颜色（0°/180°/270°） */
    private static final int RING_AXIS_COLOR = 0x40000000;
    /** 威胁标签文字透明度 */
    private static final int LABEL_ALPHA = 0xD0;

    // ============ 缓存 ============
    private static final Map<Integer, RwrTexState> CACHE = new HashMap<Integer, RwrTexState>();

    private MCH_RWRDisplayTextureManager() {}

    // ============ 公共接口 ============
    public static ResourceLocation getTexture(MCH_EntityAircraft ac, EntityPlayer player, float partialTicks) {
        if (ac == null || ac.getAcInfo() == null || !ac.getAcInfo().hasRWR) {
            return null;
        }

        RwrTexState state = getOrCreate(ac.getEntityId());
        long worldTick = ac.worldObj != null ? ac.worldObj.getTotalWorldTime() : 0L;

        // 暖机
        if (!state.ready) {
            warmupTexture(state);
            if (!state.ready) return null;
            if (!state.clearUploaded) {
                state.texture.updateDynamicTexture();
                state.clearUploaded = true;
                state.lastUpdateTick = worldTick;
                return null;
            }
        }

        // 每tick最多更新1次（RWR数据变化慢）
        long effectivePhaseKey = worldTick;
        boolean firstUpdate = state.lastUpdatePhaseKey < 0L;
        boolean hasActiveAlerts = (frame_has_alerts);  // TODO: 传参

        if (!hasActiveAlerts && !firstUpdate && effectivePhaseKey == state.lastUpdatePhaseKey) {
            return state.location;
        }

        MCH_RenderRWR.RWRDisplayFrame frame = MCH_RenderRWR.buildRWRDisplayFrame(ac, player, partialTicks);

        renderGraphicsFrame(state.pixels, frame);

        // 告警闪烁时文字需更频繁更新
        boolean hasBlink = frame != null
            && ((frame.missileUntilTick >= worldTick && !frame.missileSourceName.isEmpty())
             || (frame.lockUntilTick >= worldTick && !frame.lockSourceName.isEmpty()));

        if (shouldUpdateTextLayer(state, frame, worldTick, hasBlink)) {
            renderTextLayer(state.textPixels, frame, worldTick);
            state.lastTextTick = worldTick;
            state.lastTextHash = computeTextHash(frame);
        }
        overlayTextLayer(state.pixels, state.textPixels);
        state.texture.updateDynamicTexture();
        state.lastUpdateTick = worldTick;
        state.lastUpdatePhaseKey = effectivePhaseKey;
        return state.location;
    }

    // ============ 纹理创建与暖机 ============
    private static RwrTexState getOrCreate(int aircraftId) {
        RwrTexState state = CACHE.get(aircraftId);
        if (state != null) return state;

        Minecraft mc = Minecraft.getMinecraft();
        DynamicTexture tex = new DynamicTexture(TEX_SIZE, TEX_SIZE);
        ResourceLocation location = mc.getTextureManager()
            .getDynamicTextureLocation("mcheli_rwr_display_" + aircraftId, tex);

        state = new RwrTexState();
        state.texture = tex;
        state.location = location;
        state.pixels = tex.getTextureData();
        CACHE.put(aircraftId, state);
        return state;
    }

    private static void warmupTexture(RwrTexState state) {
        if (state == null || state.pixels == null || state.ready) return;
        int start = state.warmupCursor;
        int end = Math.min(state.pixels.length, start + WARMUP_CHUNK_PIXELS);
        for (int i = start; i < end; i++) {
            state.pixels[i] = CLEAR_COLOR;
        }
        state.warmupCursor = end;
        if (end >= state.pixels.length) {
            state.ready = true;
        }
    }

    // ============ 图形层渲染 ============
    private static void renderGraphicsFrame(int[] pixels, MCH_RenderRWR.RWRDisplayFrame frame) {
        fill(pixels, CLEAR_COLOR);

        int cx = TEX_SIZE / 2;
        int cy = TEX_SIZE / 2;
        int radius = (int) (TEX_SIZE * (RWR_UV_DIAMETER_RATIO * 0.5D));
        int innerRadius = (int) (radius * RWR_INNER_RING_RATIO);
        int outerRadius = (int) (radius * RWR_OUTER_RING_RATIO);

        if (frame == null || !frame.valid) {
            // 即使无数据也要画空圆环骨架
            drawRWRBaseline(pixels, radius, cx, cy, innerRadius, outerRadius);
            return;
        }

        // 1. 圆环背景填充
        fillCircle(pixels, cx, cy, outerRadius, RING_BG_COLOR);

        // 2. 画RWR圆环基线（刻度线: 0°、90°、180°、270°）
        drawRWRBaseline(pixels, radius, cx, cy, innerRadius, outerRadius);

        // 3. 画内圈和外圈圆环线
        drawRing(pixels, cx, cy, innerRadius, RING_LINE_COLOR);
        drawRing(pixels, cx, cy, outerRadius, RING_LINE_COLOR);

        // 4. 干扰叠加（全屏干扰纹理效果：用半透明条纹模拟）
        if (frame.jammed) {
            drawJammedOverlay(pixels, radius, cx, cy);
        }

        // 5. 画威胁标记点
        for (MCH_RenderRWR.RWRDisplayPoint point : frame.points) {
            int px = cx + (int) Math.round(Math.cos(point.angleRad) * point.ringRadius * radius);
            int py = cy + (int) Math.round(Math.sin(point.angleRad) * point.ringRadius * radius);

            // 威胁点：小方块
            int pointColor = (point.color & 0x00FFFFFF) | ((int)(LABEL_ALPHA) << 24);
            drawSquare(pixels, px, py, pointColor, THREAT_POINT_RADIUS);

            // 导弹威胁额外画小菱形/十字
            if (point.isMissile) {
                drawSquare(pixels, px, py, 0xFF2D2D,
                           THREAT_POINT_RADIUS + 1);
            }
        }

        // 6. 威胁标签按距离排序后再绘制（近的覆盖远的）
        //    标签直接绘制在威胁点旁边（偏移）
        for (MCH_RenderRWR.RWRDisplayPoint point : frame.points) {
            // TODO: 文字标签绘制 - 需要用drawText在威胁点附近偏移绘制
        }
    }

    private static void drawRWRBaseline(int[] pixels, int radius, int cx, int cy,
                                         int innerRadius, int outerRadius) {
        // 画4条主刻度线：0°(上)、90°(右)、180°(下)、270°(左)
        for (int deg = 0; deg < 360; deg += 90) {
            double rad = Math.toRadians(deg);
            int x = cx + (int) Math.round(Math.cos(rad) * radius);
            int y = cy + (int) Math.round(Math.sin(rad) * radius);
            int ix = cx + (int) Math.round(Math.cos(rad) * innerRadius);
            int iy = cy + (int) Math.round(Math.sin(rad) * innerRadius);
            int ox = cx + (int) Math.round(Math.cos(rad) * outerRadius);
            int oy = cy + (int) Math.round(Math.sin(rad) * outerRadius);
            // 大刻度：从内圈稍外到外圈
            drawLine(pixels, ix, iy, ox, oy,
                     deg == 0 || deg == 180 ? RING_AXIS_COLOR
                                            : withAlpha(RING_AXIS_COLOR, 0x40));
        }
    }

    private static void drawJammedOverlay(int[] pixels, int radius, int cx, int cy) {
        // 用随机噪点条纹模拟干扰效果
        int stripeWidth = 4;
        int gapWidth = 3;
        Random rand = new Random(42); // 固定种子避免每帧闪烁太剧烈
        for (int y = -radius; y <= radius; y++) {
            int absY = Math.abs(y);
            int mod = absY % (stripeWidth + gapWidth);
            if (mod >= stripeWidth) continue;

            int xx = (int) Math.sqrt(Math.max(0, radius * radius - y * y));
            for (int x = -xx; x <= xx; x++) {
                if (rand.nextInt(3) == 0) {
                    int alpha = 0x40 + rand.nextInt(0x30);
                    int color = (alpha << 24) | 0xFF0000; // 红色噪点
                    setPixel(pixels, cx + x, cy + y, color);
                }
            }
        }
    }

    // ============ 文字层渲染 ============
    private static void renderTextLayer(int[] textPixels, MCH_RenderRWR.RWRDisplayFrame frame,
                                         long worldTick) {
        fill(textPixels, CLEAR_COLOR);
        if (frame == null || !frame.valid) return;

        int cx = TEX_SIZE / 2;
        int cy = TEX_SIZE / 2;
        int radius = (int) (TEX_SIZE * (RWR_UV_DIAMETER_RATIO * 0.5D));
        int outerRadius = (int) (radius * RWR_OUTER_RING_RATIO);

        // RWR 标题/模式文字（上方圆环外）
        String titleText = "RWR";
        int titleWidth = textWidth(titleText, 1);
        drawText(textPixels, titleText, cx - titleWidth / 2, cy - outerRadius - 13, 0xE0FFE400, 1);

        // 距离刻度（上方圆环外）
        String rangeText = frame.maxDistanceMeters + "M";
        int rangeWidth = textWidth(rangeText, 1);
        drawText(textPixels, rangeText, cx - rangeWidth / 2, cy - outerRadius - 6, 0xD000FF00, 1);

        // 导弹告警（左上角，闪烁）
        if (frame.missileUntilTick >= worldTick && !frame.missileSourceName.isEmpty()) {
            boolean blinkStrong = ((worldTick / 3) & 1L) == 0L;
            int mslColor = blinkStrong ? 0xFFFF2D2D : 0x8CFF2D2D;
            int x = cx - outerRadius + 4;
            int y = cy - outerRadius + 4;
            drawText(textPixels, "M", x, y, mslColor, 1);
            drawText(textPixels, frame.missileSourceName, x + 6, y, mslColor, 1);
        }

        // 锁定告警（中上）
        if (frame.lockUntilTick >= worldTick && !frame.lockSourceName.isEmpty()) {
            boolean blinkStrong = ((worldTick / 5) & 1L) == 0L;
            int lockColor = blinkStrong ? 0xFFFF4A4A : 0x90FF4A4A;
            int y = cy - outerRadius + (frame.missileUntilTick >= worldTick ? 14 : 4);
            drawText(textPixels, "L", cx - outerRadius + 4, y, lockColor, 1);
            drawText(textPixels, frame.lockSourceName, cx - outerRadius + 10, y, lockColor, 1);
        }

        // 搜索扫描源（左上往下排列，最多3个）
        int searchY = cy - outerRadius + 4
            + (frame.missileUntilTick >= worldTick ? 10 : 0)
            + (frame.lockUntilTick >= worldTick ? 10 : 0);
        int count = 0;
        for (String source : frame.scanSources) {
            if (count >= 3) break;
            // 淡入效果（用固定颜色，不随时间衰减——纹理层面不支持逐字TTL）
            drawText(textPixels, source, cx - outerRadius + 4, searchY + count * 10, 0xC000FF00, 1);
            count++;
        }
    }

    // ============ 文字更新策略 ============
    private static boolean shouldUpdateTextLayer(RwrTexState state, MCH_RenderRWR.RWRDisplayFrame frame,
                                                  long worldTick, boolean hasBlink) {
        if (state == null) return true;
        if (hasBlink) return true; // 告警闪烁时每帧更新
        int hash = computeTextHash(frame);
        if (state.lastTextHash != hash) return true;
        if (state.lastTextTick < 0L) return true;
        return worldTick - state.lastTextTick >= TEXT_UPDATE_INTERVAL_TICK;
    }

    private static int computeTextHash(MCH_RenderRWR.RWRDisplayFrame frame) {
        int h = 17;
        if (frame != null) {
            h = h * 31 + (frame.jammed ? 1 : 0);
            h = h * 31 + (frame.missileSourceName != null ? frame.missileSourceName.hashCode() : 0);
            h = h * 31 + (frame.lockSourceName != null ? frame.lockSourceName.hashCode() : 0);
            h = h * 31 + frame.maxDistanceMeters;
            for (String s : frame.scanSources) {
                h = h * 31 + (s != null ? s.hashCode() : 0);
            }
        }
        return h;
    }

    // ============ 复用MCH_RadarDisplayTextureManager的底层绘制函数 ============
    // 以下函数建议提取到公共工具类 MCH_TextureRenderUtil，避免代码重复：
    // - fill() / fillCircle() / drawRing() / drawLine() / drawSquare() / setPixel()
    // - blendPixel() / blendPixelByIndex() / overlayTextLayer()
    // - withAlpha() / textWidth() / drawText() / glyph() / fillRectBlend()
    //
    // 如果暂时不提取，则直接在此文件中复制一份（约300行）。

    // ============ 状态类 ============
    private static class RwrTexState {
        public DynamicTexture texture;
        public ResourceLocation location;
        public int[] pixels;
        public int[] textPixels = new int[TEX_SIZE * TEX_SIZE];
        public long lastUpdateTick = -1L;
        public long lastUpdatePhaseKey = -1L;
        public long lastTextTick = -1L;
        public int lastTextHash = 0;
        public int warmupCursor = 0;
        public boolean ready = false;
        public boolean clearUploaded = false;
    }
}
```

## 7. 威胁颜色映射规则

与屏幕HUD RWR完全一致：

| 威胁等级 | 颜色 | 闪烁 |
|---------|------|------|
| SEARCH（搜索） | `0x00FF00`（绿，飞机辐射源）/ `0xFFE400`（琥珀，地面辐射源） | 无 |
| TRACK（跟踪） | `0xFF8A3A`（橙色） | 无 |
| STT（单目标跟踪） | `0xFF4A4A`（红色） | 无 |
| MSL_ACTIVE（主动弹） | `0xFF2D2D` ↔ `0xCC3030` | 3 tick 周期 |
| MSL_DATALINK（数据链导弹） | `0xFF2D2D` ↔ `0xCC3030` | 3 tick 周期 |

## 8. 修改汇总

### 8.1 MCH_RenderAircraft.java

**renderCommonPart() 新增一行调用：**

```java
public void renderCommonPart(MCH_EntityAircraft ac, MCH_AircraftInfo info, double x, double y, double z, float tickTime) {
    renderRope(ac, info, x, y, z, tickTime);
    this.renderRadarDisplayPart(ac, info, tickTime);
    this.renderRWRDisplayPart(ac, info, tickTime);   // ★ 新增
    renderERA(ac, info);
    // ... 其余不变
}
```

**新增 renderRWRDisplayPart() 方法**（见 6.2 节完整代码）。

### 8.2 MCH_RenderRWR.java

**新增5个元素：**

1. `RWRDisplayPoint` 内部类（数据结构）
2. `RWRDisplayFrame` 内部类（数据结构）
3. `buildRWRDisplayFrame()` 静态方法（核心逻辑）
4. `resolveThreatColorForRWR()` 辅助方法（颜色映射，复用 `resolveThreatColor` 的部分逻辑）

### 8.3 MCH_RWRDisplayTextureManager.java（新建）

完整文件，约 400 行，包含：
- 动态纹理生命周期管理（getOrCreate、warmupTexture）
- 图形层软件渲染（圆环、威胁标记、干扰叠加）
- 文字层软件渲染（标题、刻度、告警文字、搜索源列表）
- 更新节流与闪烁逻辑
- 底层绘制函数（复用或复制自 MCH_RadarDisplayTextureManager）

## 9. 纹理视觉效果示意

```
┌─────────────────────────────────┐
│        RWR                       │  ← 标题（琥珀色）
│       4096M                      │  ← 距离刻度（绿色）
│                                  │
│              ╭──────╮            │
│    M  SA-6   │      │            │  ← 导弹告警（红色闪烁）
│    L  29     │  ◎   │  *  AA     │  ← 锁定告警（红色闪烁）
│  searchSrc1  │      │            │  ← 搜索源列表（绿色浅色）
│  searchSrc2  │      │            │
│              ╰──────╯            │
│        ──── 内圈 ────            │
│        ──── 外圈 ────            │
│                                  │
│        ○ 威胁标记（STT/红色）      │  ← 在外圈=距离近
│     ● 威胁标记（MSL/红色闪烁）     │  ← 导弹威胁
│  * 威胁标记（搜索/绿色）           │  ← 在内圈=距离远
│                                  │
│  [干扰时：红色条纹噪点覆盖全屏]      │
└─────────────────────────────────┘
```

## 10. 前置条件

与 `$radardisplay` 类似，需要：

1. **飞行器配置**中 `hasRWR = true`
2. **3D模型**（.mqo）中必须包含名为 `$rwrdisplay` 的独立部件
3. **模型类型**必须为 `W_ModelCustom`
4. 敌方雷达正在扫描/跟踪/锁定本机（否则RWR为空环）

**注意**：`hasRWR` 和 `enableRadar` 是两个独立开关。一架飞机可以：
- 只有 `hasRWR = true`（有$rwrdisplay，无$radardisplay）
- 只有 `enableRadar = true`（有$radardisplay，无$rwrdisplay）
- 两者都有（两个部件共存）
- 两者都没有

## 11. 代码复用建议

`MCH_RadarDisplayTextureManager` 和 `MCH_RWRDisplayTextureManager` 有大量相同的底层像素操作函数（`fill`、`fillCircle`、`drawRing`、`drawLine`、`drawSquare`、`setPixel`、`blendPixel`、`glyph`、`drawText` 等约 250 行）。

**建议**：提取公共工具类 `MCH_TextureRenderUtil`，将以下函数放入：
- `fill(int[], int)` / `fillCircle(int[], int, int, int, int)`
- `drawRing(int[], int, int, int, int)` / `drawLine(int[], int, int, int, int, int)`
- `drawSquare(int[], int, int, int, int)` / `setPixel(int[], int, int, int)`
- `blendPixel(int[], int, int, int)` / `blendPixelByIndex(int[], int, int)`
- `overlayTextLayer(int[], int[])`
- `withAlpha(int, int)` / `clamp(double, double, double)`
- `drawText(int[], String, int, int, int, int)` / `glyph(char)` / `textWidth(String, int)` / `fillRectBlend(int[], int, int, int, int, int)`

这样两个纹理管理器各自只保留业务逻辑（约150行）。

## 12. 扩展方向

- **RWR环带距离环**：在内圈和外圈之间绘制多条等距刻度环，类似真实RWR的距离环
- **辐射源分类图标**：不同类型的辐射源用不同形状标记（三角形=战斗机，菱形=地空导弹等）
- **信号强度条**：在威胁标签旁画信号强度柱状图
- **RWR音量/灵敏度旋钮纹理**：固定纹理叠加层显示旋钮位置
- **多色告警等级**：新增 `WARNING` 和 `CRITICAL` 等级的闪烁颜色
