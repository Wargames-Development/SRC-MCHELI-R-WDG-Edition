package mcheli.render;

/** Mirrors WGMap CompressedDepthProjection v1 without a required WGMap dependency. */
public final class MCH_CompressedDepthProjection {
    public static final int VERSION = 1;
    public static final double MAX_DISTANCE = 4096.0D;

    private MCH_CompressedDepthProjection() { }

    public static double projected(double distance, double transitionStart) {
        if (!Double.isFinite(distance) || !Double.isFinite(transitionStart)
                || transitionStart <= 0.0D || transitionStart >= MAX_DISTANCE) return Double.NaN;
        if (distance <= transitionStart) return distance;
        double band = Math.min(8.0D, transitionStart / 16.0D);
        double head = band * 0.5D;
        double tail = band - head;
        double logRange = Math.log(MAX_DISTANCE / transitionStart);
        if (logRange <= 0.80D) return Double.NaN;
        // The short exponential shoulder joins normal depth with slope one. The
        // logarithmic tail preserves useful separation throughout the BVR range.
        double shoulder = head / (1.0D - tail / (transitionStart * logRange));
        double capped = Math.min(distance, MAX_DISTANCE);
        return transitionStart + head * -Math.expm1(-(capped - transitionStart) / shoulder)
                + tail * Math.log(capped / transitionStart) / logRange;
    }

    /** Fraction of the available depth-buffer interval after the normal-depth transition. */
    public static double depthFraction(double distance, double transitionStart) {
        if (!Double.isFinite(distance) || !Double.isFinite(transitionStart)
                || transitionStart <= 0.0D || transitionStart >= MAX_DISTANCE) return Double.NaN;
        if (distance <= transitionStart) return 0.0D;
        double logRange = Math.log(MAX_DISTANCE / transitionStart);
        if (logRange <= 0.80D) return Double.NaN;
        double shoulder = 0.05D / (1.0D / transitionStart
                - 0.80D / (transitionStart * logRange));
        double capped = Math.min(distance, MAX_DISTANCE);
        return 0.05D * -Math.expm1(-(capped - transitionStart) / shoulder)
                + 0.80D * Math.log(capped / transitionStart) / logRange;
    }
}
