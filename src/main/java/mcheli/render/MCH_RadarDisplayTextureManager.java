package mcheli.render;

import mcheli.aircraft.MCH_EntityAircraft;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public final class MCH_RadarDisplayTextureManager {

    private static final int TEX_SIZE = 256;
    private static final double RADAR_UV_DIAMETER_RATIO = 0.70D;
    private static final int CLEAR_COLOR = 0x00000000;
    private static final int WARMUP_CHUNK_PIXELS = 8192;
    private static final int TEXT_UPDATE_INTERVAL_TICK = 5;
    private static final Map<Integer, RadarTexState> CACHE = new HashMap<Integer, RadarTexState>();

    private MCH_RadarDisplayTextureManager() {
    }

    public static ResourceLocation getTexture(MCH_EntityAircraft ac, EntityPlayer player, float partialTicks) {
        if (ac == null || ac.getAcInfo() == null || !ac.getAcInfo().enableRadar) {
            return null;
        }
        RadarTexState state = getOrCreate(ac.getEntityId());
        long worldTick = ac.worldObj != null ? ac.worldObj.getTotalWorldTime() : 0L;
        if (!state.ready) {
            warmupTexture(state);
            if (!state.ready) {
                return null;
            }
            // One-frame delay after warmup: upload clear texture first, then start radar drawing.
            if (!state.clearUploaded) {
                state.texture.updateDynamicTexture();
                state.clearUploaded = true;
                state.lastUpdateTick = worldTick;
                state.lastUpdatePhaseKey = buildPhaseKey(worldTick, partialTicks);
                return null;
            }
        }
        long phaseKey = buildPhaseKey(worldTick, partialTicks);
        boolean firstUpdate = state.lastUpdatePhaseKey < 0L;
        boolean lockActive = MCH_RenderRWR.getRadarTrackingTargetId(ac) > 0;
        if (!lockActive && !firstUpdate && phaseKey == state.lastUpdatePhaseKey) {
            return state.location;
        }
        MCH_RenderRWR.RadarDisplayFrame frame = MCH_RenderRWR.buildRadarDisplayFrame(ac, player, partialTicks);
        boolean lockNoPoint = frame != null
            && frame.trackingTargetId > 0
            && (frame.points == null || frame.points.isEmpty());
        if (lockNoPoint && state.lastFrameHadPoints) {
            state.lastUpdateTick = worldTick;
            state.lastUpdatePhaseKey = phaseKey;
            return state.location;
        }
        int radarUiColor = MCH_RenderRWR.getEnableRadarUiColor(frame.aircraft);
        renderGraphicsFrame(state.pixels, frame, radarUiColor);
        state.lastFrameHadPoints = frame != null && frame.points != null && !frame.points.isEmpty();
        if (shouldUpdateTextLayer(state, frame, radarUiColor, worldTick)) {
            renderTextLayer(state.textPixels, frame, radarUiColor);
            state.lastTextTick = worldTick;
            state.lastTextHash = computeTextHash(frame, radarUiColor);
        }
        overlayTextLayer(state.pixels, state.textPixels);
        state.texture.updateDynamicTexture();
        state.lastUpdateTick = worldTick;
        state.lastUpdatePhaseKey = phaseKey;
        return state.location;
    }

    private static long buildPhaseKey(long worldTick, float partialTicks) {
        // 4 updates per tick max: improves visual smoothness without full per-frame upload cost.
        int bucket = (int)(clamp(partialTicks, 0.0D, 0.9999D) * 4.0D);
        return worldTick * 4L + bucket;
    }

    private static void warmupTexture(RadarTexState state) {
        if (state == null || state.pixels == null || state.ready) {
            return;
        }
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

    private static RadarTexState getOrCreate(int aircraftId) {
        RadarTexState state = CACHE.get(aircraftId);
        if (state != null) {
            return state;
        }
        Minecraft mc = Minecraft.getMinecraft();
        DynamicTexture tex = new DynamicTexture(TEX_SIZE, TEX_SIZE);
        ResourceLocation location = mc.getTextureManager().getDynamicTextureLocation("mcheli_radar_display_" + aircraftId, tex);
        state = new RadarTexState();
        state.texture = tex;
        state.location = location;
        state.pixels = tex.getTextureData();
        CACHE.put(aircraftId, state);
        return state;
    }

    private static boolean shouldUpdateTextLayer(RadarTexState state, MCH_RenderRWR.RadarDisplayFrame frame, int radarUiColor, long worldTick) {
        if (state == null) {
            return true;
        }
        int hash = computeTextHash(frame, radarUiColor);
        if (state.lastTextHash != hash) {
            return true;
        }
        if (state.lastTextTick < 0L) {
            return true;
        }
        return worldTick - state.lastTextTick >= TEXT_UPDATE_INTERVAL_TICK;
    }

    private static int computeTextHash(MCH_RenderRWR.RadarDisplayFrame frame, int radarUiColor) {
        int h = 17;
        h = h * 31 + radarUiColor;
        h = h * 31 + (frame != null ? frame.maxDistanceMeters : 0);
        h = h * 31 + (frame != null ? frame.halfAzimuthDeg : 0);
        h = h * 31 + (frame != null && frame.modeLabel != null ? frame.modeLabel.hashCode() : 0);
        return h;
    }

    private static void renderGraphicsFrame(int[] pixels, MCH_RenderRWR.RadarDisplayFrame frame, int radarUiColor) {
        fill(pixels, CLEAR_COLOR);
        int cx = TEX_SIZE / 2;
        int cy = TEX_SIZE / 2;
        int radius = (int)(TEX_SIZE * (RADAR_UV_DIAMETER_RATIO * 0.5D));

        if (frame != null && frame.valid) {
            int sweepColor = withAlpha(radarUiColor, 0xA0);
            int sectorEdgeColor = withAlpha(radarUiColor, 0x55);
            int fillColor = withAlpha(radarUiColor, (int)Math.round(clamp(frame.panelFillAlpha, 0.0D, 1.0D) * 255.0D));
            int trackFillColor = withAlpha(radarUiColor, (int)Math.round(clamp(frame.panelFillAlpha * 1.5D, 0.0D, 1.0D) * 255.0D));
            int contactColor = withAlpha(radarUiColor, 0xFF);
            int selectedColor = withAlpha(0xFFFFFF, 0xFF);
            int trackingColor = withAlpha(0xFF4040, 0xFF);
            double scanAz = clamp(frame.scanAzimuthDeg, 0.0D, 360.0D);
            double trackAz = clamp(frame.trackAzimuthDeg, 0.0D, scanAz);
            double axisDeg = frame.scanAxisDeg;
            double sweepDeg;
            if (scanAz >= 359.9D) {
                fillCircle(pixels, cx, cy, radius, fillColor);
                if (trackAz > 0.0D) {
                    fillCircle(pixels, cx, cy, radius, trackFillColor);
                }
                drawRing(pixels, cx, cy, radius, sectorEdgeColor);
                sweepDeg = axisDeg + frame.scanPhase * 360.0D;
            } else {
                double startDeg = axisDeg - scanAz * 0.5D;
                double endDeg = startDeg + scanAz;
                fillSector(pixels, cx, cy, radius, startDeg, endDeg, fillColor);
                if (trackAz > 0.0D) {
                    double trackStart = axisDeg - trackAz * 0.5D;
                    double trackEnd = trackStart + trackAz;
                    fillSector(pixels, cx, cy, radius, trackStart, trackEnd, trackFillColor);
                }
                double p = clamp(frame.scanPhase, 0.0D, 1.0D);
                double pingPong = p <= 0.5D ? (p * 2.0D) : (2.0D - p * 2.0D);
                sweepDeg = startDeg + scanAz * pingPong;
                drawLine(pixels, cx, cy,
                    cx + (int)Math.round(Math.cos(Math.toRadians(startDeg)) * radius),
                    cy + (int)Math.round(Math.sin(Math.toRadians(startDeg)) * radius),
                    sectorEdgeColor);
                drawLine(pixels, cx, cy,
                    cx + (int)Math.round(Math.cos(Math.toRadians(endDeg)) * radius),
                    cy + (int)Math.round(Math.sin(Math.toRadians(endDeg)) * radius),
                    sectorEdgeColor);
                drawArc(pixels, cx, cy, radius, startDeg, endDeg, sectorEdgeColor);
            }
            if (frame.showMainSweep) {
                double sweepAngle = Math.toRadians(sweepDeg);
                int sx = cx + (int)Math.round(Math.cos(sweepAngle) * radius);
                int sy = cy + (int)Math.round(Math.sin(sweepAngle) * radius);
                drawLine(pixels, cx, cy, sx, sy, sweepColor);
            }

            // ACM overlay: keep the large search sector, and draw a small ACM sector on top.
            if (frame.acmMode && frame.acmAzimuthDeg > 0.0D) {
                double acmAz = clamp(frame.acmAzimuthDeg, 0.0D, 360.0D);
                double acmAxis = frame.acmAxisDeg;
                double acmStart = acmAxis - acmAz * 0.5D;
                double acmEnd = acmStart + acmAz;
                fillSector(pixels, cx, cy, radius, acmStart, acmEnd, withAlpha(radarUiColor, 0x40));
                drawArc(pixels, cx, cy, radius, acmStart, acmEnd, withAlpha(radarUiColor, 0xA0));
                drawLine(pixels, cx, cy,
                    cx + (int)Math.round(Math.cos(Math.toRadians(acmStart)) * radius),
                    cy + (int)Math.round(Math.sin(Math.toRadians(acmStart)) * radius),
                    withAlpha(radarUiColor, 0x90));
                drawLine(pixels, cx, cy,
                    cx + (int)Math.round(Math.cos(Math.toRadians(acmEnd)) * radius),
                    cy + (int)Math.round(Math.sin(Math.toRadians(acmEnd)) * radius),
                    withAlpha(radarUiColor, 0x90));
                double p = clamp(frame.scanPhase, 0.0D, 1.0D);
                double pingPong = p <= 0.5D ? (p * 2.0D) : (2.0D - p * 2.0D);
                double acmSweep = acmStart + acmAz * pingPong;
                drawLine(pixels, cx, cy,
                    cx + (int)Math.round(Math.cos(Math.toRadians(acmSweep)) * radius),
                    cy + (int)Math.round(Math.sin(Math.toRadians(acmSweep)) * radius),
                    withAlpha(radarUiColor, 0xC0));
            }

            for (MCH_RenderRWR.RadarDisplayPoint point : frame.points) {
                int px = cx + (int)Math.round(point.x * radius);
                int py = cy + (int)Math.round(point.y * radius);
                int color = contactColor;
                if (point.selected) {
                    color = selectedColor;
                }
                if (point.tracking) {
                    color = trackingColor;
                    drawLine(pixels, cx, cy, px, py, withAlpha(0xFF4040, 0xA0));
                }
                if (frame.twsLikeMode && point.selected && !point.tracking) {
                    drawDashedLine(pixels, cx, cy, px, py, withAlpha(0xFF4040, 0xA0), 3, 2);
                }
                int pointRadius = Math.max(1, point.pointSize / 2);
                drawSquare(pixels, px, py, color, point.tracking ? Math.max(2, pointRadius) : pointRadius);
                if (point.hasVelocity) {
                    int vx = px + (int)Math.round(point.velX * radius * point.velLen);
                    int vy = py + (int)Math.round(point.velY * radius * point.velLen);
                    drawLine(pixels, px, py, vx, vy, withAlpha(color & 0x00FFFFFF, 0xCC));
                }
                if (point.selected || point.tracking) {
                    drawRing(pixels, px, py, 3, color);
                }
            }
        }
    }

    private static void renderTextLayer(int[] textPixels, MCH_RenderRWR.RadarDisplayFrame frame, int radarUiColor) {
        fill(textPixels, CLEAR_COLOR);
        if (frame == null || !frame.valid) {
            return;
        }
        int cx = TEX_SIZE / 2;
        int cy = TEX_SIZE / 2;
        int radius = (int)(TEX_SIZE * (RADAR_UV_DIAMETER_RATIO * 0.5D));
        String modeText = "RADAR " + (frame.modeLabel == null ? "SRC" : frame.modeLabel.toUpperCase());
        String topText = "-" + frame.halfAzimuthDeg + " " + frame.maxDistanceMeters + "M " + frame.halfAzimuthDeg;
        int modeWidth = textWidth(modeText, 1);
        int topWidth = textWidth(topText, 1);
        drawText(textPixels, modeText, cx - modeWidth / 2, cy - radius - 13, withAlpha(radarUiColor, 0xE0), 1);
        drawText(textPixels, topText, cx - topWidth / 2, cy - radius - 6, withAlpha(radarUiColor, 0xD0), 1);
    }

    private static void overlayTextLayer(int[] dstPixels, int[] textPixels) {
        if (dstPixels == null || textPixels == null || dstPixels.length != textPixels.length) {
            return;
        }
        for (int i = 0; i < dstPixels.length; i++) {
            int src = textPixels[i];
            if ((src >>> 24) != 0) {
                blendPixelByIndex(dstPixels, i, src);
            }
        }
    }

    private static void fill(int[] pixels, int color) {
        for (int i = 0; i < pixels.length; i++) {
            pixels[i] = color;
        }
    }

    private static void drawSquare(int[] pixels, int x, int y, int color, int radius) {
        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                setPixel(pixels, x + dx, y + dy, color);
            }
        }
    }

    private static void drawRing(int[] pixels, int cx, int cy, int r, int color) {
        if (r <= 0) {
            return;
        }
        int samples = Math.max(64, r * 6);
        for (int i = 0; i < samples; i++) {
            double a = Math.PI * 2.0D * (double)i / (double)samples;
            int x = cx + (int)Math.round(Math.cos(a) * r);
            int y = cy + (int)Math.round(Math.sin(a) * r);
            setPixel(pixels, x, y, color);
        }
    }

    private static void drawLine(int[] pixels, int x0, int y0, int x1, int y1, int color) {
        int dx = Math.abs(x1 - x0);
        int sx = x0 < x1 ? 1 : -1;
        int dy = -Math.abs(y1 - y0);
        int sy = y0 < y1 ? 1 : -1;
        int err = dx + dy;
        while (true) {
            setPixel(pixels, x0, y0, color);
            if (x0 == x1 && y0 == y1) {
                break;
            }
            int e2 = err << 1;
            if (e2 >= dy) {
                err += dy;
                x0 += sx;
            }
            if (e2 <= dx) {
                err += dx;
                y0 += sy;
            }
        }
    }

    private static void drawDashedLine(int[] pixels, int x0, int y0, int x1, int y1, int color, int dash, int gap) {
        int dx = x1 - x0;
        int dy = y1 - y0;
        double len = Math.sqrt(dx * dx + dy * dy);
        if (len < 1.0D) {
            return;
        }
        double ux = dx / len;
        double uy = dy / len;
        double pos = 0.0D;
        while (pos < len) {
            double s = pos;
            double e = Math.min(len, pos + dash);
            int sx = x0 + (int)Math.round(ux * s);
            int sy = y0 + (int)Math.round(uy * s);
            int ex = x0 + (int)Math.round(ux * e);
            int ey = y0 + (int)Math.round(uy * e);
            drawLine(pixels, sx, sy, ex, ey, color);
            pos += dash + gap;
        }
    }

    private static void fillCircle(int[] pixels, int cx, int cy, int radius, int color) {
        for (int y = -radius; y <= radius; y++) {
            int xx = (int)Math.sqrt(Math.max(0, radius * radius - y * y));
            for (int x = -xx; x <= xx; x++) {
                blendPixel(pixels, cx + x, cy + y, color);
            }
        }
    }

    private static void fillSector(int[] pixels, int cx, int cy, int radius, double startDeg, double endDeg, int color) {
        int seg = Math.max(24, (int)Math.ceil(Math.abs(endDeg - startDeg) / 3.0D));
        for (int i = 0; i <= seg; i++) {
            double t = (double)i / (double)seg;
            double deg = startDeg + (endDeg - startDeg) * t;
            int x = cx + (int)Math.round(Math.cos(Math.toRadians(deg)) * radius);
            int y = cy + (int)Math.round(Math.sin(Math.toRadians(deg)) * radius);
            drawLineBlend(pixels, cx, cy, x, y, color);
        }
    }

    private static void drawLineBlend(int[] pixels, int x0, int y0, int x1, int y1, int color) {
        int dx = Math.abs(x1 - x0);
        int sx = x0 < x1 ? 1 : -1;
        int dy = -Math.abs(y1 - y0);
        int sy = y0 < y1 ? 1 : -1;
        int err = dx + dy;
        while (true) {
            blendPixel(pixels, x0, y0, color);
            if (x0 == x1 && y0 == y1) {
                break;
            }
            int e2 = err << 1;
            if (e2 >= dy) {
                err += dy;
                x0 += sx;
            }
            if (e2 <= dx) {
                err += dx;
                y0 += sy;
            }
        }
    }

    private static void drawArc(int[] pixels, int cx, int cy, int radius, double startDeg, double endDeg, int color) {
        int seg = Math.max(16, (int)Math.ceil(Math.abs(endDeg - startDeg) / 4.0D));
        int px = 0;
        int py = 0;
        for (int i = 0; i <= seg; i++) {
            double t = (double)i / (double)seg;
            double deg = startDeg + (endDeg - startDeg) * t;
            int x = cx + (int)Math.round(Math.cos(Math.toRadians(deg)) * radius);
            int y = cy + (int)Math.round(Math.sin(Math.toRadians(deg)) * radius);
            if (i > 0) {
                drawLine(pixels, px, py, x, y, color);
            }
            px = x;
            py = y;
        }
    }

    private static void setPixel(int[] pixels, int x, int y, int color) {
        if (x < 0 || y < 0 || x >= TEX_SIZE || y >= TEX_SIZE) {
            return;
        }
        pixels[y * TEX_SIZE + x] = color;
    }

    private static void blendPixel(int[] pixels, int x, int y, int src) {
        if (x < 0 || y < 0 || x >= TEX_SIZE || y >= TEX_SIZE) {
            return;
        }
        int idx = y * TEX_SIZE + x;
        blendPixelByIndex(pixels, idx, src);
    }

    private static void blendPixelByIndex(int[] pixels, int idx, int src) {
        if (pixels == null || idx < 0 || idx >= pixels.length) {
            return;
        }
        int dst = pixels[idx];
        int sa = (src >>> 24) & 0xFF;
        if (sa <= 0) {
            return;
        }
        if (sa >= 255) {
            pixels[idx] = src;
            return;
        }
        int sr = (src >>> 16) & 0xFF;
        int sg = (src >>> 8) & 0xFF;
        int sb = src & 0xFF;
        int da = (dst >>> 24) & 0xFF;
        int dr = (dst >>> 16) & 0xFF;
        int dg = (dst >>> 8) & 0xFF;
        int db = dst & 0xFF;
        int inv = 255 - sa;
        int oa = Math.min(255, sa + (da * inv + 127) / 255);
        int or = (sr * sa + dr * inv + 127) / 255;
        int og = (sg * sa + dg * inv + 127) / 255;
        int ob = (sb * sa + db * inv + 127) / 255;
        pixels[idx] = ((oa & 0xFF) << 24) | ((or & 0xFF) << 16) | ((og & 0xFF) << 8) | (ob & 0xFF);
    }

    private static int withAlpha(int rgb, int alpha) {
        return ((alpha & 0xFF) << 24) | (rgb & 0x00FFFFFF);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static class RadarTexState {
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
        public boolean lastFrameHadPoints = false;
    }

    private static void drawText(int[] pixels, String text, int x, int y, int color, int scale) {
        if (text == null || text.isEmpty()) {
            return;
        }
        int cx = x;
        int s = Math.max(1, scale);
        for (int i = 0; i < text.length(); i++) {
            char c = Character.toUpperCase(text.charAt(i));
            int[] g = glyph(c);
            if (g == null) {
                cx += 4 * s;
                continue;
            }
            for (int row = 0; row < g.length; row++) {
                int bits = g[row];
                for (int col = 0; col < 3; col++) {
                    if (((bits >> (2 - col)) & 1) != 0) {
                        fillRectBlend(pixels, cx + col * s, y + row * s, s, s, color);
                    }
                }
            }
            cx += 4 * s;
        }
    }

    private static int textWidth(String text, int scale) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        int s = Math.max(1, scale);
        return text.length() * 4 * s - s;
    }

    private static void fillRectBlend(int[] pixels, int x, int y, int w, int h, int color) {
        for (int yy = 0; yy < h; yy++) {
            for (int xx = 0; xx < w; xx++) {
                blendPixel(pixels, x + xx, y + yy, color);
            }
        }
    }

    private static int[] glyph(char c) {
        switch (c) {
            case 'A': return new int[]{2, 5, 7, 5, 5};
            case 'B': return new int[]{6, 5, 6, 5, 6};
            case 'C': return new int[]{3, 4, 4, 4, 3};
            case 'D': return new int[]{6, 5, 5, 5, 6};
            case 'E': return new int[]{7, 4, 6, 4, 7};
            case 'G': return new int[]{3, 4, 5, 5, 3};
            case 'I': return new int[]{7, 2, 2, 2, 7};
            case 'M': return new int[]{5, 7, 7, 5, 5};
            case 'R': return new int[]{6, 5, 6, 5, 5};
            case 'S': return new int[]{3, 4, 2, 1, 6};
            case 'T': return new int[]{7, 2, 2, 2, 2};
            case 'W': return new int[]{5, 5, 7, 7, 5};
            case ' ': return new int[]{0, 0, 0, 0, 0};
            case '-': return new int[]{0, 0, 7, 0, 0};
            case '*': return new int[]{5, 2, 7, 2, 5};
            case '0': return new int[]{7, 5, 5, 5, 7};
            case '1': return new int[]{2, 6, 2, 2, 7};
            case '2': return new int[]{7, 1, 7, 4, 7};
            case '3': return new int[]{7, 1, 7, 1, 7};
            case '4': return new int[]{5, 5, 7, 1, 1};
            case '5': return new int[]{7, 4, 7, 1, 7};
            case '6': return new int[]{7, 4, 7, 5, 7};
            case '7': return new int[]{7, 1, 2, 2, 2};
            case '8': return new int[]{7, 5, 7, 5, 7};
            case '9': return new int[]{7, 5, 7, 1, 7};
            default: return null;
        }
    }
}
