package mcheli.render;

import java.util.Random;

public final class MCH_TextureRenderUtil {

    public static final int TEX_SIZE = 256;
    public static final int CLEAR_COLOR = 0x00000000;

    private MCH_TextureRenderUtil() {
    }

    public static void fill(int[] pixels, int color) {
        for (int i = 0; i < pixels.length; i++) {
            pixels[i] = color;
        }
    }

    public static void setPixel(int[] pixels, int x, int y, int color) {
        if (x < 0 || y < 0 || x >= TEX_SIZE || y >= TEX_SIZE) {
            return;
        }
        pixels[y * TEX_SIZE + x] = color;
    }

    public static void blendPixel(int[] pixels, int x, int y, int src) {
        if (x < 0 || y < 0 || x >= TEX_SIZE || y >= TEX_SIZE) {
            return;
        }
        int idx = y * TEX_SIZE + x;
        blendPixelByIndex(pixels, idx, src);
    }

    public static void blendPixelByIndex(int[] pixels, int idx, int src) {
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

    public static void overlayTextLayer(int[] dstPixels, int[] textPixels) {
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

    public static int withAlpha(int rgb, int alpha) {
        return ((alpha & 0xFF) << 24) | (rgb & 0x00FFFFFF);
    }

    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public static void drawSquare(int[] pixels, int x, int y, int color, int radius) {
        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                setPixel(pixels, x + dx, y + dy, color);
            }
        }
    }

    public static void drawRing(int[] pixels, int cx, int cy, int r, int color) {
        if (r <= 0) {
            return;
        }
        int samples = Math.max(64, r * 6);
        for (int i = 0; i < samples; i++) {
            double a = Math.PI * 2.0D * (double) i / (double) samples;
            int x = cx + (int) Math.round(Math.cos(a) * r);
            int y = cy + (int) Math.round(Math.sin(a) * r);
            setPixel(pixels, x, y, color);
        }
    }

    public static void drawLine(int[] pixels, int x0, int y0, int x1, int y1, int color) {
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

    public static void drawDashedLine(int[] pixels, int x0, int y0, int x1, int y1, int color, int dash, int gap) {
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
            int sx = x0 + (int) Math.round(ux * s);
            int sy = y0 + (int) Math.round(uy * s);
            int ex = x0 + (int) Math.round(ux * e);
            int ey = y0 + (int) Math.round(uy * e);
            drawLine(pixels, sx, sy, ex, ey, color);
            pos += dash + gap;
        }
    }

    public static void fillCircle(int[] pixels, int cx, int cy, int radius, int color) {
        for (int y = -radius; y <= radius; y++) {
            int xx = (int) Math.sqrt(Math.max(0, radius * radius - y * y));
            for (int x = -xx; x <= xx; x++) {
                blendPixel(pixels, cx + x, cy + y, color);
            }
        }
    }

    public static void fillSector(int[] pixels, int cx, int cy, int radius, double startDeg, double endDeg, int color) {
        int seg = Math.max(24, (int) Math.ceil(Math.abs(endDeg - startDeg) / 3.0D));
        for (int i = 0; i <= seg; i++) {
            double t = (double) i / (double) seg;
            double deg = startDeg + (endDeg - startDeg) * t;
            int x = cx + (int) Math.round(Math.cos(Math.toRadians(deg)) * radius);
            int y = cy + (int) Math.round(Math.sin(Math.toRadians(deg)) * radius);
            drawLineBlend(pixels, cx, cy, x, y, color);
        }
    }

    public static void drawLineBlend(int[] pixels, int x0, int y0, int x1, int y1, int color) {
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

    public static void drawArc(int[] pixels, int cx, int cy, int radius, double startDeg, double endDeg, int color) {
        int seg = Math.max(16, (int) Math.ceil(Math.abs(endDeg - startDeg) / 4.0D));
        int px = 0;
        int py = 0;
        for (int i = 0; i <= seg; i++) {
            double t = (double) i / (double) seg;
            double deg = startDeg + (endDeg - startDeg) * t;
            int x = cx + (int) Math.round(Math.cos(Math.toRadians(deg)) * radius);
            int y = cy + (int) Math.round(Math.sin(Math.toRadians(deg)) * radius);
            if (i > 0) {
                drawLine(pixels, px, py, x, y, color);
            }
            px = x;
            py = y;
        }
    }

    public static void fillRectBlend(int[] pixels, int x, int y, int w, int h, int color) {
        for (int yy = 0; yy < h; yy++) {
            for (int xx = 0; xx < w; xx++) {
                blendPixel(pixels, x + xx, y + yy, color);
            }
        }
    }

    public static void drawText(int[] pixels, String text, int x, int y, int color, int scale) {
        if (text == null || text.isEmpty()) {
            return;
        }
        int cx = x;
        int s = Math.max(1, scale);
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            boolean isLower = Character.isLowerCase(c);
            int[] g = glyph(Character.toUpperCase(c));
            if (g == null) {
                cx += 4 * s;
                continue;
            }
            int rowOffset = isLower ? 1 : 0;
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
    }

    public static int textWidth(String text, int scale) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        int s = Math.max(1, scale);
        return text.length() * 4 * s - s;
    }

    public static int[] glyph(char c) {
        switch (c) {
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
            case 'N': return new int[]{0b101, 0b111, 0b111, 0b111, 0b101};
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

    public static void overlayECMSnow(int[] pixels, long worldTick) {
        if (pixels == null) return;
        double density = 0.15;
        Random rand = new Random(worldTick * 31L);
        for (int i = 0; i < pixels.length; i++) {
            if (rand.nextDouble() >= density) continue;
            int alpha = 120 + rand.nextInt(80);
            int gray = 180 + rand.nextInt(76);
            int noise = (alpha << 24) | (gray << 16) | (gray << 8) | gray;
            blendPixelByIndex(pixels, i, noise);
        }
    }

    private static net.minecraft.client.renderer.texture.DynamicTexture jamTex;
    private static net.minecraft.util.ResourceLocation jamTexLoc;
    private static long lastJamTick = -1L;

    public static net.minecraft.util.ResourceLocation getSharedJamTexture(long worldTick) {
        if (jamTex == null) {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
            jamTex = new net.minecraft.client.renderer.texture.DynamicTexture(TEX_SIZE, TEX_SIZE);
            jamTexLoc = mc.getTextureManager().getDynamicTextureLocation("mcheli_shared_jam", jamTex);
        }
        int[] p = jamTex.getTextureData();
        if (worldTick != lastJamTick) {
            fill(p, CLEAR_COLOR);
            overlayECMSnow(p, worldTick);
            jamTex.updateDynamicTexture();
            lastJamTick = worldTick;
        }
        return jamTexLoc;
    }
}
