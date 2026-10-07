package mcheli.aircraft;

/** Render-time control calculations, independent of Minecraft client classes. */
public final class MCH_AircraftControlMath {
    private MCH_AircraftControlMath() {
    }

    public static float tickDelta(float delta) {
        return Float.isNaN(delta) || Float.isInfinite(delta) ? 0.0F : Math.max(0.0F, Math.min(delta, 1.0F));
    }

    public static float rotationStep(float input, double limit, float factor, float delta) {
        if (Float.isNaN(input) || Float.isInfinite(input)) {
            return 0.0F;
        }
        return (float) (Math.max(-limit, Math.min(input, limit)) * factor * 0.06D * tickDelta(delta));
    }

    public static float retention(float perTick, float delta) {
        return (float) Math.pow(perTick, tickDelta(delta));
    }

    public static float planeRollInput(float mouseX, float keyRoll, float mobility,
            boolean flightSim, boolean vtol) {
        float mouseFactor = flightSim ? 2.0F : (vtol ? 1.0F : 0.5F);
        // Restore the pre-jitter-fix key rate: 0.5 degrees * 3 reference frames
        // per tick. setAngles still integrates it using actual elapsed time once.
        float key = flightSim ? 0.0F : keyRoll * (1.5F / 0.048F);
        return (mouseX * mouseFactor + key) * mobility;
    }

    public static double planeRollLimit(float mobility, boolean flightSim) {
        // Normal-mode keyboard demand reaches about 62.5 at full input. The old
        // 40-unit cap would clip the restored rate; mouse and keys share this cap.
        return (flightSim ? 40.0D : 80.0D) * mobility;
    }
}
