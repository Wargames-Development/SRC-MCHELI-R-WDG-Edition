package mcheli.hud;

import mcheli.MCH_Vector2;

import java.util.ArrayList;

public class MCH_HudItemRadar extends MCH_HudItem {

    private final String rot;
    private final String left;
    private final String top;
    private final String width;
    private final String height;
    private final boolean isEntityRadar;
    private double[] pointBuffer = new double[0];


    public MCH_HudItemRadar(int fileLine, boolean isEntityRadar, String rot, String left, String top, String width, String height) {
        super(fileLine);
        this.isEntityRadar = isEntityRadar;
        this.rot = toFormula(rot);
        this.left = toFormula(left);
        this.top = toFormula(top);
        this.width = toFormula(width);
        this.height = toFormula(height);
    }

    public void execute() {
        if (this.isEntityRadar) {
            if (MCH_HudItem.EntityList != null && MCH_HudItem.EntityList.size() > 0) {
                this.drawEntityList(MCH_HudItem.EntityList, (float) calc(this.rot), MCH_HudItem.centerX + calc(this.left), MCH_HudItem.centerY + calc(this.top), calc(this.width), calc(this.height));
            }
        } else if (MCH_HudItem.EnemyList != null && MCH_HudItem.EnemyList.size() > 0) {
            this.drawEntityList(MCH_HudItem.EnemyList, (float) calc(this.rot), MCH_HudItem.centerX + calc(this.left), MCH_HudItem.centerY + calc(this.top), calc(this.width), calc(this.height));
        }

    }

    protected void drawEntityList(ArrayList src, float r, double left, double top, double w, double h) {
        double w1 = -w / 2.0D;
        double w2 = w / 2.0D;
        double h1 = -h / 2.0D;
        double h2 = h / 2.0D;
        double w_factor = w / 64.0D;
        double h_factor = h / 64.0D;
        int requiredLength = src.size() * 2;
        if (this.pointBuffer.length < requiredLength) {
            this.pointBuffer = new double[requiredLength];
        }
        int idx = 0;

        for (Object point : src) {
            MCH_Vector2 radarPoint = (MCH_Vector2) point;
            this.pointBuffer[idx++] = radarPoint.x / 2.0D * w_factor;
            this.pointBuffer[idx++] = radarPoint.y / 2.0D * h_factor;
        }

        double radians = Math.toRadians(r);
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        for (int i = 0; i + 1 < requiredLength; i += 2) {
            double x = this.pointBuffer[i];
            double y = this.pointBuffer[i + 1];
            this.pointBuffer[i] = x * cos - y * sin;
            this.pointBuffer[i + 1] = x * sin + y * cos;
        }
        int visiblePointCount = 0;

        for (int i = 0; i + 1 < requiredLength; i += 2) {
            if (this.pointBuffer[i] > w1 && this.pointBuffer[i] < w2 && this.pointBuffer[i + 1] > h1 && this.pointBuffer[i + 1] < h2) {
                this.pointBuffer[visiblePointCount++] = this.pointBuffer[i] + left + w / 2.0D;
                this.pointBuffer[visiblePointCount++] = this.pointBuffer[i + 1] + top + h / 2.0D;
            }
        }

        this.drawPoints(this.pointBuffer, visiblePointCount, MCH_HudItem.colorSetting, MCH_HudItem.scaleFactor * 2);
    }
}
