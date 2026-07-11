package mcheli.render;

import mcheli.aircraft.MCH_EntityAircraft;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public final class MCH_RadarDisplayTextureManager {

    private static final double RADAR_UV_DIAMETER_RATIO = 0.70D;
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

        if (ac.jammingTick > 0) {
            return MCH_TextureRenderUtil.getSharedJamTexture(worldTick);
        }

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
        MCH_TextureRenderUtil.overlayTextLayer(state.pixels, state.textPixels);
        state.texture.updateDynamicTexture();
        state.lastUpdateTick = worldTick;
        state.lastUpdatePhaseKey = phaseKey;
        return state.location;
    }

    private static long buildPhaseKey(long worldTick, float partialTicks) {
        int bucket = (int)(MCH_TextureRenderUtil.clamp(partialTicks, 0.0D, 0.9999D) * 4.0D);
        return worldTick * 4L + bucket;
    }

    private static void warmupTexture(RadarTexState state) {
        if (state == null || state.pixels == null || state.ready) {
            return;
        }
        int start = state.warmupCursor;
        int end = Math.min(state.pixels.length, start + WARMUP_CHUNK_PIXELS);
        for (int i = start; i < end; i++) {
            state.pixels[i] = MCH_TextureRenderUtil.CLEAR_COLOR;
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
        DynamicTexture tex = new DynamicTexture(MCH_TextureRenderUtil.TEX_SIZE, MCH_TextureRenderUtil.TEX_SIZE);
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
        MCH_TextureRenderUtil.fill(pixels, MCH_TextureRenderUtil.CLEAR_COLOR);
        int cx = MCH_TextureRenderUtil.TEX_SIZE / 2;
        int cy = MCH_TextureRenderUtil.TEX_SIZE / 2;
        int radius = (int)(MCH_TextureRenderUtil.TEX_SIZE * (RADAR_UV_DIAMETER_RATIO * 0.5D));

        if (frame != null && frame.valid) {
            int sweepColor = MCH_TextureRenderUtil.withAlpha(radarUiColor, 0xA0);
            int sectorEdgeColor = MCH_TextureRenderUtil.withAlpha(radarUiColor, 0x55);
            int fillColor = MCH_TextureRenderUtil.withAlpha(radarUiColor, (int)Math.round(MCH_TextureRenderUtil.clamp(frame.panelFillAlpha, 0.0D, 1.0D) * 255.0D));
            int trackFillColor = MCH_TextureRenderUtil.withAlpha(radarUiColor, (int)Math.round(MCH_TextureRenderUtil.clamp(frame.panelFillAlpha * 1.5D, 0.0D, 1.0D) * 255.0D));
            int contactColor = MCH_TextureRenderUtil.withAlpha(radarUiColor, 0xFF);
            int selectedColor = MCH_TextureRenderUtil.withAlpha(0xFFFFFF, 0xFF);
            int trackingColor = MCH_TextureRenderUtil.withAlpha(0xFF4040, 0xFF);
            double scanAz = MCH_TextureRenderUtil.clamp(frame.scanAzimuthDeg, 0.0D, 360.0D);
            double trackAz = MCH_TextureRenderUtil.clamp(frame.trackAzimuthDeg, 0.0D, scanAz);
            double axisDeg = frame.scanAxisDeg;
            double sweepDeg;
            if (scanAz >= 359.9D) {
                MCH_TextureRenderUtil.fillCircle(pixels, cx, cy, radius, fillColor);
                if (trackAz > 0.0D) {
                    MCH_TextureRenderUtil.fillCircle(pixels, cx, cy, radius, trackFillColor);
                }
                MCH_TextureRenderUtil.drawRing(pixels, cx, cy, radius, sectorEdgeColor);
                sweepDeg = axisDeg + frame.scanPhase * 360.0D;
            } else {
                double startDeg = axisDeg - scanAz * 0.5D;
                double endDeg = startDeg + scanAz;
                MCH_TextureRenderUtil.fillSector(pixels, cx, cy, radius, startDeg, endDeg, fillColor);
                if (trackAz > 0.0D) {
                    double trackStart = axisDeg - trackAz * 0.5D;
                    double trackEnd = trackStart + trackAz;
                    MCH_TextureRenderUtil.fillSector(pixels, cx, cy, radius, trackStart, trackEnd, trackFillColor);
                }
                double p = MCH_TextureRenderUtil.clamp(frame.scanPhase, 0.0D, 1.0D);
                double pingPong = p <= 0.5D ? (p * 2.0D) : (2.0D - p * 2.0D);
                sweepDeg = startDeg + scanAz * pingPong;
                MCH_TextureRenderUtil.drawLine(pixels, cx, cy,
                    cx + (int)Math.round(Math.cos(Math.toRadians(startDeg)) * radius),
                    cy + (int)Math.round(Math.sin(Math.toRadians(startDeg)) * radius),
                    sectorEdgeColor);
                MCH_TextureRenderUtil.drawLine(pixels, cx, cy,
                    cx + (int)Math.round(Math.cos(Math.toRadians(endDeg)) * radius),
                    cy + (int)Math.round(Math.sin(Math.toRadians(endDeg)) * radius),
                    sectorEdgeColor);
                MCH_TextureRenderUtil.drawArc(pixels, cx, cy, radius, startDeg, endDeg, sectorEdgeColor);
            }
            if (frame.showMainSweep) {
                double sweepAngle = Math.toRadians(sweepDeg);
                int sx = cx + (int)Math.round(Math.cos(sweepAngle) * radius);
                int sy = cy + (int)Math.round(Math.sin(sweepAngle) * radius);
                MCH_TextureRenderUtil.drawLine(pixels, cx, cy, sx, sy, sweepColor);
            }

            if (frame.acmMode && frame.acmAzimuthDeg > 0.0D) {
                double acmAz = MCH_TextureRenderUtil.clamp(frame.acmAzimuthDeg, 0.0D, 360.0D);
                double acmAxis = frame.acmAxisDeg;
                double acmStart = acmAxis - acmAz * 0.5D;
                double acmEnd = acmStart + acmAz;
                MCH_TextureRenderUtil.fillSector(pixels, cx, cy, radius, acmStart, acmEnd, MCH_TextureRenderUtil.withAlpha(radarUiColor, 0x40));
                MCH_TextureRenderUtil.drawArc(pixels, cx, cy, radius, acmStart, acmEnd, MCH_TextureRenderUtil.withAlpha(radarUiColor, 0xA0));
                MCH_TextureRenderUtil.drawLine(pixels, cx, cy,
                    cx + (int)Math.round(Math.cos(Math.toRadians(acmStart)) * radius),
                    cy + (int)Math.round(Math.sin(Math.toRadians(acmStart)) * radius),
                    MCH_TextureRenderUtil.withAlpha(radarUiColor, 0x90));
                MCH_TextureRenderUtil.drawLine(pixels, cx, cy,
                    cx + (int)Math.round(Math.cos(Math.toRadians(acmEnd)) * radius),
                    cy + (int)Math.round(Math.sin(Math.toRadians(acmEnd)) * radius),
                    MCH_TextureRenderUtil.withAlpha(radarUiColor, 0x90));
                double p = MCH_TextureRenderUtil.clamp(frame.scanPhase, 0.0D, 1.0D);
                double pingPong = p <= 0.5D ? (p * 2.0D) : (2.0D - p * 2.0D);
                double acmSweep = acmStart + acmAz * pingPong;
                MCH_TextureRenderUtil.drawLine(pixels, cx, cy,
                    cx + (int)Math.round(Math.cos(Math.toRadians(acmSweep)) * radius),
                    cy + (int)Math.round(Math.sin(Math.toRadians(acmSweep)) * radius),
                    MCH_TextureRenderUtil.withAlpha(radarUiColor, 0xC0));
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
                    MCH_TextureRenderUtil.drawLine(pixels, cx, cy, px, py, MCH_TextureRenderUtil.withAlpha(0xFF4040, 0xA0));
                }
                if (frame.twsLikeMode && point.selected && !point.tracking) {
                    MCH_TextureRenderUtil.drawDashedLine(pixels, cx, cy, px, py, MCH_TextureRenderUtil.withAlpha(0xFF4040, 0xA0), 3, 2);
                }
                int pointRadius = Math.max(1, point.pointSize / 2);
                MCH_TextureRenderUtil.drawSquare(pixels, px, py, color, point.tracking ? Math.max(2, pointRadius) : pointRadius);
                if (point.hasVelocity) {
                    int vx = px + (int)Math.round(point.velX * radius * point.velLen);
                    int vy = py + (int)Math.round(point.velY * radius * point.velLen);
                    MCH_TextureRenderUtil.drawLine(pixels, px, py, vx, vy, MCH_TextureRenderUtil.withAlpha(color & 0x00FFFFFF, 0xCC));
                }
                if (point.selected || point.tracking) {
                    MCH_TextureRenderUtil.drawRing(pixels, px, py, 3, color);
                }
            }
        }
    }

    private static void renderTextLayer(int[] textPixels, MCH_RenderRWR.RadarDisplayFrame frame, int radarUiColor) {
        MCH_TextureRenderUtil.fill(textPixels, MCH_TextureRenderUtil.CLEAR_COLOR);
        if (frame == null || !frame.valid) {
            return;
        }
        int cx = MCH_TextureRenderUtil.TEX_SIZE / 2;
        int cy = MCH_TextureRenderUtil.TEX_SIZE / 2;
        int radius = (int)(MCH_TextureRenderUtil.TEX_SIZE * (RADAR_UV_DIAMETER_RATIO * 0.5D));
        String modeText = "RADAR " + (frame.modeLabel == null ? "SRC" : frame.modeLabel.toUpperCase());
        String topText = "-" + frame.halfAzimuthDeg + " " + frame.maxDistanceMeters + "M " + frame.halfAzimuthDeg;
        int modeWidth = MCH_TextureRenderUtil.textWidth(modeText, 1);
        int topWidth = MCH_TextureRenderUtil.textWidth(topText, 1);
        MCH_TextureRenderUtil.drawText(textPixels, modeText, cx - modeWidth / 2, cy - radius - 13, MCH_TextureRenderUtil.withAlpha(radarUiColor, 0xE0), 1);
        MCH_TextureRenderUtil.drawText(textPixels, topText, cx - topWidth / 2, cy - radius - 6, MCH_TextureRenderUtil.withAlpha(radarUiColor, 0xD0), 1);
    }

    private static class RadarTexState {
        public DynamicTexture texture;
        public ResourceLocation location;
        public int[] pixels;
        public int[] textPixels = new int[MCH_TextureRenderUtil.TEX_SIZE * MCH_TextureRenderUtil.TEX_SIZE];
        public long lastUpdateTick = -1L;
        public long lastUpdatePhaseKey = -1L;
        public long lastTextTick = -1L;
        public int lastTextHash = 0;
        public int warmupCursor = 0;
        public boolean ready = false;
        public boolean clearUploaded = false;
        public boolean lastFrameHadPoints = false;
    }
}
