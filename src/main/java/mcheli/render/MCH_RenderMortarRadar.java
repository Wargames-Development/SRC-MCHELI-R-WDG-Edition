package mcheli.render;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import mcheli.MCH_EntityInfo;
import mcheli.MCH_EntityInfoClientTracker;
import mcheli.aircraft.MCH_AircraftInfo;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.aircraft.MCH_EntitySeat;
import mcheli.uav.MCH_EntityUavStation;
import mcheli.weapon.MCH_WeaponInfo;
import mcheli.weapon.MCH_WeaponSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

public class MCH_RenderMortarRadar {

    private static final int PANEL_SIZE = 480;
    private static final double SCREEN_HEIGHT_REF = 500.0;

    private static final double MIN_DISTANCE = 20.0;
    private static final double MAX_DISTANCE = 300.0;

    private static final int COLOR_BG = 0xFF001022;
    private static final int COLOR_RING_MAIN = 0xFF006688;
    private static final int COLOR_RING_SUB = 0xFF003355;
    private static final int COLOR_TICK = 0xFF0088AA;
    private static final int COLOR_SWEEP = 0x0600FFCC;
    private static final int COLOR_TEXT = 0xFF00FFCC;
    private static final int COLOR_TARGET_DOT = 0xFF00FF66;
    private static final int COLOR_TARGET_NAME = 0xFFFFFFCC;
    private static final int COLOR_BORDER = 0xFF000810;
    private static final int COLOR_BORDER_FRAME = 0xFF001828;
    private static final int COLOR_BORDER_ACCENT = 0xFF00CCFF;
    private static final int COLOR_IMPACT_GREEN = 0xFF00FF44;
    private static final int COLOR_IMPACT_ORANGE = 0xFFFF8833;
    private static final int COLOR_IMPACT_RED = 0xFFFF3333;
    private static final int COLOR_HUD_BG = 0xCC000810;
    private static final int COLOR_HUD_TEXT = 0xFF00FFCC;
    private static final int COLOR_HUD_VALUE = 0xFFFFFFFF;
    private static final int COLOR_HUD_WARN = 0xFFFF6644;

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return;

        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.thePlayer;
        World world = mc.theWorld;
        if (player == null || world == null) return;

        MCH_EntityAircraft ac = null;
        if (player.ridingEntity instanceof MCH_EntityAircraft) {
            ac = (MCH_EntityAircraft) player.ridingEntity;
        } else if (player.ridingEntity instanceof MCH_EntitySeat) {
            ac = ((MCH_EntitySeat) player.ridingEntity).getParent();
        } else if (player.ridingEntity instanceof MCH_EntityUavStation) {
            ac = ((MCH_EntityUavStation) player.ridingEntity).getControlAircract();
        }

        if (ac == null) return;
        if (!ac.isMortarRadarEnabledRuntime()) return;

        MCH_WeaponSet ws = ac.getCurrentWeapon(player);
        MCH_WeaponInfo wi = ws != null ? ws.getInfo() : null;

        double maxDist = MAX_DISTANCE;
        boolean displayDist = false;
        String weaponName = "";
        if (wi != null && wi.hasMortarRadar) {
            if (wi.mortarRadarMaxDist > 0) {
                maxDist = wi.mortarRadarMaxDist;
            }
            displayDist = wi.displayMortarDistance;
            weaponName = wi.displayName != null ? wi.displayName : wi.name;
        }

        double currentDist = -1.0;
        if (displayDist) {
            currentDist = ac.getLandInDistance(player);
        }

        ScaledResolution sc = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
        double scale = sc.getScaledHeight_double() / SCREEN_HEIGHT_REF;
        double panelHalf = PANEL_SIZE * scale / 2.0;
        double cx = sc.getScaledWidth_double() / 2.0;
        double cy = sc.getScaledHeight_double() / 2.0;
        double diskRadius = panelHalf * 0.82;

        double pX = player.posX + (player.posX - player.lastTickPosX) * event.partialTicks;
        double pY = player.posY + (player.posY - player.lastTickPosY) * event.partialTicks;
        double pZ = player.posZ + (player.posZ - player.lastTickPosZ) * event.partialTicks;

        Vec3 lookVec = getDirection(player, event.partialTicks);

        double impactWorldX = 0, impactWorldZ = 0;
        boolean hasImpact = currentDist >= MIN_DISTANCE;
        if (hasImpact) {
            double pitchRad = Math.toRadians(player.rotationPitch);
            double horizDist = Math.abs(Math.cos(pitchRad)) * currentDist;
            impactWorldX = pX + lookVec.xCoord * horizDist;
            impactWorldZ = pZ + lookVec.zCoord * horizDist;
        }

        int impactColor = COLOR_IMPACT_GREEN;
        if (hasImpact) {
            double nearestDist = Double.MAX_VALUE;
            for (MCH_EntityInfo entity : getServerLoadedEntity()) {
                if (!isValidEntity(entity, player, ac)) continue;
                double ex = interpolate(entity.posX, entity.lastTickPosX, event.partialTicks);
                double ez = interpolate(entity.posZ, entity.lastTickPosZ, event.partialTicks);
                double dx = impactWorldX - ex;
                double dz = impactWorldZ - ez;
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d < nearestDist) nearestDist = d;
            }
            if (nearestDist < 5.0) {
                impactColor = COLOR_IMPACT_RED;
            } else if (nearestDist < 15.0) {
                impactColor = COLOR_IMPACT_ORANGE;
            }
        }

        List<EntityRecord> entityRecords = new ArrayList<EntityRecord>();
        for (MCH_EntityInfo entity : getServerLoadedEntity()) {
            if (!isValidEntity(entity, player, ac)) continue;
            double xPos = interpolate(entity.posX, entity.lastTickPosX, event.partialTicks);
            double yPos = interpolate(entity.posY, entity.lastTickPosY, event.partialTicks);
            double zPos = interpolate(entity.posZ, entity.lastTickPosZ, event.partialTicks);

            Vec3 delta = Vec3.createVectorHelper(xPos - pX, yPos - pY, zPos - pZ);
            Vec3 dH = Vec3.createVectorHelper(delta.xCoord, 0, delta.zCoord).normalize();
            Vec3 lH = Vec3.createVectorHelper(lookVec.xCoord, 0, lookVec.zCoord).normalize();

            double dot = lH.dotProduct(dH);
            double angle = Math.toDegrees(Math.acos(Math.max(-1, Math.min(1, dot))));
            if (lH.crossProduct(dH).yCoord < 0) angle = -angle;

            double dist = Math.sqrt(delta.xCoord * delta.xCoord + delta.zCoord * delta.zCoord);
            if (dist > maxDist) continue;

            int groundY = ac.worldObj.getHeightValue((int) xPos, (int) zPos);
            if (yPos - groundY > 10.0) continue;

            double ratio = Math.min(Math.max((dist - MIN_DISTANCE) / (maxDist - MIN_DISTANCE), 0), 1);
            double r = ratio * diskRadius;
            double rad = Math.toRadians(angle);
            double mx = cx + r * Math.sin(-rad);
            double my = cy - r * Math.cos(rad);

            String name = ac.getNameOnMyRadar(entity);
            if (name.isEmpty() || "?".equals(name)) {
                name = fallbackRadarName(entity, ac);
            }
            if (name.isEmpty() || "?".equals(name)) {
                name = entity.entityName != null ? entity.entityName : "?";
            }

            EntityRecord rec = new EntityRecord();
            rec.x = mx;
            rec.y = my;
            rec.name = name;
            rec.dist = (int) dist;
            entityRecords.add(rec);
        }

        String impactDistText = hasImpact ? String.valueOf((int) currentDist) : "--";
        String gunElevText = String.format("%.1f", player.rotationPitch) + "\u00b0";

        String heatText = "";
        String reloadText = "";
        if (ws != null) {
            if (ws.currentHeat > 0 && wi != null && wi.maxHeatCount > 0) {
                double pct = (double) ws.currentHeat / (double) wi.maxHeatCount;
                if (pct >= 1.0) {
                    heatText = localized("mortar.overheat", "OVERHEAT");
                } else {
                    heatText = localized("mortar.heat", "HEAT") + " " + (int)(pct * 100) + "%";
                }
            }
            if (ws.getCurrentWeapon() != null) {
                int ammo = ws.getAmmoNum();
                int maxAmmo = ws.getAmmoNumMax();
                if (maxAmmo > 0 && ammo <= 0) {
                    reloadText = localized("mortar.reloading", "RELOD");
                }
            }
        }

        // ========== RENDER ==========

        GL11.glPushMatrix();
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_DEPTH_TEST);

        drawBackground(cx, cy, panelHalf);
        drawBorderFrame(cx, cy, panelHalf);
        drawConcentricRings(cx, cy, diskRadius);
        drawTickMarks(cx, cy, diskRadius, maxDist);
        drawSweep(cx, cy, diskRadius, world.getTotalWorldTime());
        drawBorderAccent(cx, cy, panelHalf);
        drawHudPanel(cx, cy, panelHalf, impactDistText, gunElevText, scale);
        drawWeaponInfoPanel(cx, cy, panelHalf, weaponName, heatText, reloadText, scale);

        for (EntityRecord rec : entityRecords) {
            drawTargetDot1x1(rec.x, rec.y, scale);
            String label = rec.name + " " + rec.dist;
            drawText(label, rec.x, rec.y + 6 * scale, scale, COLOR_TARGET_NAME);
        }

        if (hasImpact) {
            double ratio = Math.min(Math.max((currentDist - MIN_DISTANCE) / (maxDist - MIN_DISTANCE), 0), 1);
            double r = ratio * diskRadius;
            double crx = cx;
            double cry = cy - r;
            drawImpactCrosshair(crx, cry, scale, impactColor);
        }

        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopMatrix();
    }

    private static String localized(String key, String fallback) {
        String translated = I18n.format(key);
        if (translated == null || translated.isEmpty() || translated.equals(key)) {
            return fallback;
        }
        return translated;
    }

    private void drawBackground(double cx, double cy, double half) {
        Tessellator tess = Tessellator.instance;
        tess.startDrawingQuads();
        tess.setColorRGBA_I(COLOR_BG, (COLOR_BG >>> 24) & 0xFF);
        tess.addVertex(cx - half, cy + half, 0);
        tess.addVertex(cx + half, cy + half, 0);
        tess.addVertex(cx + half, cy - half, 0);
        tess.addVertex(cx - half, cy - half, 0);
        tess.draw();
    }

    private void drawBorderFrame(double cx, double cy, double half) {
        Tessellator tess = Tessellator.instance;
        double bw = half * 0.03;
        int alpha = (COLOR_BORDER >>> 24) & 0xFF;

        tess.startDrawingQuads();
        tess.setColorRGBA_I(COLOR_BORDER, alpha);
        tess.addVertex(cx - half - bw, cy + half + bw, 0);
        tess.addVertex(cx + half + bw, cy + half + bw, 0);
        tess.addVertex(cx + half + bw, cy - half - bw, 0);
        tess.addVertex(cx - half - bw, cy - half - bw, 0);
        tess.draw();

        tess.startDrawingQuads();
        tess.setColorRGBA_I(COLOR_BG, alpha);
        tess.addVertex(cx - half, cy + half, 0);
        tess.addVertex(cx + half, cy + half, 0);
        tess.addVertex(cx + half, cy - half, 0);
        tess.addVertex(cx - half, cy - half, 0);
        tess.draw();

        double ib = half * 0.01;
        tess.startDrawingQuads();
        tess.setColorRGBA_I(COLOR_BORDER_FRAME, 0xFF);
        tess.addVertex(cx - half - ib, cy + half + ib, 0);
        tess.addVertex(cx + half + ib, cy + half + ib, 0);
        tess.addVertex(cx + half + ib, cy - half - ib, 0);
        tess.addVertex(cx - half - ib, cy - half - ib, 0);
        tess.draw();
        tess.startDrawingQuads();
        tess.setColorRGBA_I(COLOR_BORDER_FRAME, 0xFF);
        tess.addVertex(cx - half + ib, cy + half - ib, 0);
        tess.addVertex(cx + half - ib, cy + half - ib, 0);
        tess.addVertex(cx + half - ib, cy - half + ib, 0);
        tess.addVertex(cx - half + ib, cy - half + ib, 0);
        tess.draw();
    }

    private void drawBorderAccent(double cx, double cy, double half) {
        Tessellator tess = Tessellator.instance;
        int alpha = (COLOR_BORDER_ACCENT >>> 24) & 0xFF;
        double cl = half * 0.08;
        double bw = half * 0.03;
        double[][] corners = {{-1, -1}, {1, -1}, {1, 1}, {-1, 1}};
        for (double[] corner : corners) {
            double bx = cx + corner[0] * (half + bw * 0.5);
            double by = cy + corner[1] * (half + bw * 0.5);
            tess.startDrawing(GL11.GL_LINE_STRIP);
            tess.setColorRGBA_I(COLOR_BORDER_ACCENT, alpha);
            tess.addVertex(bx + corner[0] * cl, by, 0);
            tess.addVertex(bx, by, 0);
            tess.addVertex(bx, by + corner[1] * cl, 0);
            tess.draw();
        }
    }

    private void drawConcentricRings(double cx, double cy, double radius) {
        Tessellator tess = Tessellator.instance;
        for (int level = 1; level <= 4; level++) {
            double r = radius * level / 4.0;
            int color = level % 2 == 0 ? COLOR_RING_MAIN : COLOR_RING_SUB;
            int segs = 120;
            tess.startDrawing(GL11.GL_LINE_LOOP);
            tess.setColorRGBA_I(color, (color >>> 24) & 0xFF);
            for (int i = 0; i < segs; i++) {
                double a = Math.PI * 2.0 * i / segs;
                tess.addVertex(cx + Math.cos(a) * r, cy + Math.sin(a) * r, 0);
            }
            tess.draw();
        }
    }

    private void drawTickMarks(double cx, double cy, double radius, double maxDist) {
        Tessellator tess = Tessellator.instance;
        FontRenderer fr = Minecraft.getMinecraft().fontRenderer;
        double tickLen = radius * 0.04;
        double labelR = radius + radius * 0.08;

        for (int deg = 0; deg < 360; deg += 15) {
            double a = Math.toRadians(deg - 90);
            double cos = Math.cos(a);
            double sin = Math.sin(a);
            boolean major = deg % 45 == 0;
            double len = major ? tickLen * 2.0 : tickLen;
            int color = major ? COLOR_TICK : COLOR_RING_SUB;

            tess.startDrawing(GL11.GL_LINES);
            tess.setColorRGBA_I(color, (color >>> 24) & 0xFF);
            tess.addVertex(cx + cos * radius, cy + sin * radius, 0);
            tess.addVertex(cx + cos * (radius - len), cy + sin * (radius - len), 0);
            tess.draw();

            if (major) {
                double lx = cx + cos * labelR;
                double ly = cy + sin * labelR;
                String degLabel = String.valueOf(deg) + "\u00b0";
                int tw = fr.getStringWidth(degLabel);
                GL11.glEnable(GL11.GL_TEXTURE_2D);
                fr.drawString(degLabel, (int)(lx - tw / 2.0) - 1, (int)(ly - fr.FONT_HEIGHT / 2.0), COLOR_TEXT, true);
                GL11.glDisable(GL11.GL_TEXTURE_2D);
            }
        }

        GL11.glEnable(GL11.GL_TEXTURE_2D);
        for (int level = 1; level <= 3; level++) {
            double distVal = maxDist * level / 4.0;
            String label = (int) distVal + "m";
            int tw = fr.getStringWidth(label);
            fr.drawString(label, (int)(cx + radius * level / 4.0) + 3, (int)cy + 1, COLOR_TEXT, true);
        }
        GL11.glDisable(GL11.GL_TEXTURE_2D);
    }

    private void drawSweep(double cx, double cy, double radius, long worldTick) {
        Tessellator tess = Tessellator.instance;
        double sweepAngle = Math.toRadians((worldTick * 2) % 360);
        int segs = 40;
        tess.startDrawing(GL11.GL_TRIANGLE_FAN);
        tess.setColorRGBA_I(COLOR_SWEEP, (COLOR_SWEEP >>> 24) & 0xFF);
        tess.addVertex(cx, cy, 0);
        for (int i = 0; i <= segs; i++) {
            double a = sweepAngle - (Math.PI / 8.0) * i / segs;
            double r = radius * (1.0 - i / (double) segs * 0.3);
            tess.addVertex(cx + Math.cos(a) * r, cy + Math.sin(a) * r, 0);
        }
        tess.draw();

        tess.startDrawing(GL11.GL_LINES);
        tess.setColorRGBA_I(COLOR_BORDER_ACCENT, 0x66);
        tess.addVertex(cx, cy, 0);
        tess.addVertex(cx + Math.cos(sweepAngle) * radius, cy + Math.sin(sweepAngle) * radius, 0);
        tess.draw();
    }

    private void drawTargetDot1x1(double x, double y, double scale) {
        Tessellator tess = Tessellator.instance;
        double s = 2.0 * scale;
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        tess.startDrawingQuads();
        tess.setColorRGBA_I(COLOR_TARGET_DOT, 0xFF);
        tess.addVertex(x - s, y + s, 0);
        tess.addVertex(x + s, y + s, 0);
        tess.addVertex(x + s, y - s, 0);
        tess.addVertex(x - s, y - s, 0);
        tess.draw();
    }

    private void drawImpactCrosshair(double x, double y, double scale, int color) {
        Tessellator tess = Tessellator.instance;
        double s = 10.0 * scale;
        double g = 2.5 * scale;

        GL11.glDisable(GL11.GL_TEXTURE_2D);
        tess.startDrawing(GL11.GL_LINES);
        tess.setColorRGBA_I(color, 0xFF);
        tess.addVertex(x - s, y, 0);
        tess.addVertex(x - g, y, 0);
        tess.addVertex(x + g, y, 0);
        tess.addVertex(x + s, y, 0);
        tess.addVertex(x, y - s, 0);
        tess.addVertex(x, y - g, 0);
        tess.addVertex(x, y + g, 0);
        tess.addVertex(x, y + s, 0);
        tess.draw();
    }

    private void drawHudPanel(double cx, double cy, double half, String distText, String elevText, double scale) {
        Tessellator tess = Tessellator.instance;
        double bw = half * 0.03;
        double top = cy - half - bw * 0.5;
        double left = cx - half;
        double right = cx + half;
        FontRenderer fr = Minecraft.getMinecraft().fontRenderer;

        String distLabel = localized("mortar.impact", "IMPACT");
        String distValue = distText + "m";
        double distPanelW = Math.max(fr.getStringWidth(distLabel), fr.getStringWidth(distValue)) + 16 * scale;
        double distPanelH = 28 * scale;
        double distPanelX = left + 8 * scale;
        double distPanelY = top + 4 * scale;

        tess.startDrawingQuads();
        tess.setColorRGBA_I(COLOR_HUD_BG, 0xCC);
        tess.addVertex(distPanelX, distPanelY + distPanelH, 0);
        tess.addVertex(distPanelX + distPanelW, distPanelY + distPanelH, 0);
        tess.addVertex(distPanelX + distPanelW, distPanelY, 0);
        tess.addVertex(distPanelX, distPanelY, 0);
        tess.draw();

        tess.startDrawing(GL11.GL_LINE_LOOP);
        tess.setColorRGBA_I(COLOR_BORDER_ACCENT, 0x99);
        tess.addVertex(distPanelX, distPanelY + distPanelH, 0);
        tess.addVertex(distPanelX + distPanelW, distPanelY + distPanelH, 0);
        tess.addVertex(distPanelX + distPanelW, distPanelY, 0);
        tess.addVertex(distPanelX, distPanelY, 0);
        tess.draw();

        GL11.glEnable(GL11.GL_TEXTURE_2D);
        fr.drawString(distLabel, (int)(distPanelX + 4 * scale), (int)(distPanelY + 2 * scale), COLOR_HUD_TEXT, true);
        fr.drawString(distValue, (int)(distPanelX + 4 * scale), (int)(distPanelY + 14 * scale), COLOR_HUD_VALUE, true);

        String elevLabel = localized("mortar.elev", "ELEV");
        double elevPanelW = Math.max(fr.getStringWidth(elevLabel), fr.getStringWidth(elevText)) + 16 * scale;
        double elevPanelH = 28 * scale;
        double elevPanelX = right - elevPanelW - 8 * scale;
        double elevPanelY = top + 4 * scale;

        tess.startDrawingQuads();
        tess.setColorRGBA_I(COLOR_HUD_BG, 0xCC);
        tess.addVertex(elevPanelX, elevPanelY + elevPanelH, 0);
        tess.addVertex(elevPanelX + elevPanelW, elevPanelY + elevPanelH, 0);
        tess.addVertex(elevPanelX + elevPanelW, elevPanelY, 0);
        tess.addVertex(elevPanelX, elevPanelY, 0);
        tess.draw();

        tess.startDrawing(GL11.GL_LINE_LOOP);
        tess.setColorRGBA_I(COLOR_BORDER_ACCENT, 0x99);
        tess.addVertex(elevPanelX, elevPanelY + elevPanelH, 0);
        tess.addVertex(elevPanelX + elevPanelW, elevPanelY + elevPanelH, 0);
        tess.addVertex(elevPanelX + elevPanelW, elevPanelY, 0);
        tess.addVertex(elevPanelX, elevPanelY, 0);
        tess.draw();

        GL11.glEnable(GL11.GL_TEXTURE_2D);
        fr.drawString(elevLabel, (int)(elevPanelX + 4 * scale), (int)(elevPanelY + 2 * scale), COLOR_HUD_TEXT, true);
        fr.drawString(elevText, (int)(elevPanelX + 4 * scale), (int)(elevPanelY + 14 * scale), COLOR_HUD_VALUE, true);

        GL11.glDisable(GL11.GL_TEXTURE_2D);
    }

    private void drawWeaponInfoPanel(double cx, double cy, double half, String weaponName, String heatText, String reloadText, double scale) {
        Tessellator tess = Tessellator.instance;
        double bw = half * 0.03;
        double top = cy - half - bw * 0.5;
        FontRenderer fr = Minecraft.getMinecraft().fontRenderer;
        GL11.glEnable(GL11.GL_TEXTURE_2D);

        String wpnLabel = localized("mortar.weapon", "WPN");
        double panelW = fr.getStringWidth(wpnLabel + weaponName) + 24 * scale;
        panelW = Math.max(panelW, fr.getStringWidth(heatText) + 24 * scale);
        panelW = Math.max(panelW, fr.getStringWidth(reloadText) + 24 * scale);
        double panelH = 18 * scale;
        int lineCount = 1;
        if (!heatText.isEmpty()) { panelH += 14 * scale; lineCount++; }
        if (!reloadText.isEmpty()) { panelH += 14 * scale; lineCount++; }
        double panelX = cx - panelW / 2.0;
        double panelY = top + 4 * scale;

        tess.startDrawingQuads();
        tess.setColorRGBA_I(COLOR_HUD_BG, 0xCC);
        tess.addVertex(panelX, panelY + panelH, 0);
        tess.addVertex(panelX + panelW, panelY + panelH, 0);
        tess.addVertex(panelX + panelW, panelY, 0);
        tess.addVertex(panelX, panelY, 0);
        tess.draw();

        tess.startDrawing(GL11.GL_LINE_LOOP);
        tess.setColorRGBA_I(COLOR_BORDER_ACCENT, 0x99);
        tess.addVertex(panelX, panelY + panelH, 0);
        tess.addVertex(panelX + panelW, panelY + panelH, 0);
        tess.addVertex(panelX + panelW, panelY, 0);
        tess.addVertex(panelX, panelY, 0);
        tess.draw();

        fr.drawString(wpnLabel + " " + weaponName, (int)(panelX + 4 * scale), (int)(panelY + 3 * scale), COLOR_HUD_TEXT, true);
        double lineY = panelY + 15 * scale;
        if (!heatText.isEmpty()) {
            int heatColor = heatText.contains("OVERHEAT") || heatText.contains("\u8fc7\u70ed") ? COLOR_HUD_WARN : COLOR_HUD_TEXT;
            fr.drawString(heatText, (int)(panelX + 4 * scale), (int)lineY, heatColor, true);
            lineY += 14 * scale;
        }
        if (!reloadText.isEmpty()) {
            fr.drawString(reloadText, (int)(panelX + 4 * scale), (int)lineY, COLOR_HUD_WARN, true);
        }

        GL11.glDisable(GL11.GL_TEXTURE_2D);
    }

    private void drawText(String text, double x, double y, double scale, int color) {
        FontRenderer fr = Minecraft.getMinecraft().fontRenderer;
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glPushMatrix();
        GL11.glTranslated(x, y, 0);
        GL11.glScaled(scale, scale, 1.0);
        int tw = fr.getStringWidth(text);
        fr.drawString(text, -tw / 2, 0, color, true);
        GL11.glPopMatrix();
        GL11.glDisable(GL11.GL_TEXTURE_2D);
    }

    private static String fallbackRadarName(MCH_EntityInfo entity, MCH_EntityAircraft ac) {
        MCH_AircraftInfo info = MCH_AircraftInfo.allAircraftInfo.getOrDefault(entity.entityName, null);
        if (info == null) return "?";
        String[] candidates = {
            info.nameOnModernAARadar, info.nameOnAdvancedAARadar, info.nameOnEarlyAARadar,
            info.nameOnModernASRadar, info.nameOnEarlyASRadar
        };
        for (String s : candidates) {
            if (s != null && !s.isEmpty()) return s;
        }
        return "?";
    }

    private boolean isValidEntity(MCH_EntityInfo entity, EntityPlayer player, MCH_EntityAircraft ac) {
        if (entity == null) return false;
        String cn = entity.entityClassName;
        if (cn == null) return false;
        if (entity.getHorizonalDistanceSqToEntity(player) < MIN_DISTANCE * MIN_DISTANCE) return false;
        if (isSameTeamEntity(entity, player, ac)) return false;
        if (cn.contains("MCH_EntityBaseBullet") || cn.contains("MCH_EntityBullet") || cn.contains("MCH_EntityRocket")
            || cn.contains("MCH_EntityAAMissile") || cn.contains("MCH_EntityATMissile") || cn.contains("MCH_EntityASMissile")
            || cn.contains("MCH_EntityTvMissile") || cn.contains("MCH_EntityTorpedo") || cn.contains("MCH_EntityBomb")
            || cn.contains("MCH_EntityDispensedItem") || cn.contains("MCH_EntityMarkerRocket")
            || cn.contains("MCH_EntityChaff") || cn.contains("MCH_EntityFlare")) return false;
        if (cn.contains("MCH_EntityHeli") || cn.contains("MCP_EntityPlane") || cn.contains("MCH_EntityTank")
             || cn.contains("MCH_EntityVehicle") || cn.contains("EntityPlayer") || cn.contains("EntityPlayerMP")
             || cn.contains("MCH_EntityNPC") || cn.contains("MCH_EntityGunner")
             || cn.contains("EntitySoldier")) return true;
        return false;
    }

    private boolean isSameTeamEntity(MCH_EntityInfo info, EntityPlayer player, MCH_EntityAircraft ac) {
        if (player == null || ac == null || ac.worldObj == null || info == null) return false;
        Entity e = ac.worldObj.getEntityByID(info.entityId);
        if (e == null) return false;
        if (e instanceof MCH_EntityAircraft) {
            return ((MCH_EntityAircraft) e).isMountedSameTeamEntity(player);
        }
        if (e instanceof EntityLivingBase && player.getTeam() != null && ((EntityLivingBase) e).getTeam() != null) {
            return player.isOnSameTeam((EntityLivingBase) e);
        }
        return false;
    }

    public Vec3 getDirection(Entity e, float factor) {
        float f1, f2;
        if (factor == 1.0F) {
            f1 = MathHelper.cos(-e.rotationYaw * 0.017453292F - (float) Math.PI);
            f2 = MathHelper.sin(-e.rotationYaw * 0.017453292F - (float) Math.PI);
            float f3 = -MathHelper.cos(-e.rotationPitch * 0.017453292F);
            float f4 = MathHelper.sin(-e.rotationPitch * 0.017453292F);
            return Vec3.createVectorHelper(f2 * f3, f4, f1 * f3);
        } else {
            f1 = e.prevRotationPitch + (e.rotationPitch - e.prevRotationPitch) * factor;
            f2 = e.prevRotationYaw + (e.rotationYaw - e.prevRotationYaw) * factor;
            float f3 = MathHelper.cos(-f2 * 0.017453292F - (float) Math.PI);
            float f4 = MathHelper.sin(-f2 * 0.017453292F - (float) Math.PI);
            float f5 = -MathHelper.cos(-f1 * 0.017453292F);
            float f6 = MathHelper.sin(-f1 * 0.017453292F);
            return Vec3.createVectorHelper(f4 * f5, f6, f3 * f5);
        }
    }

    private double interpolate(double now, double old, float partialTicks) {
        return old + (now - old) * partialTicks;
    }

    public List<MCH_EntityInfo> getServerLoadedEntity() {
        return new ArrayList<>(MCH_EntityInfoClientTracker.getAllTrackedEntities());
    }

    private static class EntityRecord {
        double x, y;
        String name;
        int dist;
    }
}
