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
}
