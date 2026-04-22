package mcheli;

public class MCH_RWRThreatEvent {

    public static final byte EMITTER_AIRCRAFT = 1;
    public static final byte EMITTER_GROUND_VEHICLE = 2;
    public static final byte EMITTER_MISSILE = 3;

    public static final byte MODE_SEARCH = 1;
    public static final byte MODE_TRACK = 2;
    public static final byte MODE_STT = 3;
    public static final byte MODE_MSL_ACTIVE = 4;
    public static final byte MODE_MSL_DATALINK = 5;

    public int emitterId;
    public byte emitterKind;
    public byte threatMode;
    public float bearingDeg;
    public float strength;
    public int ttlTick;
    public float confidence;
    public float distanceMeters;
    public String sourceName;

    public MCH_RWRThreatEvent() {
        this.sourceName = "?";
    }

    public MCH_RWRThreatEvent(int emitterId, byte emitterKind, byte threatMode,
                              float bearingDeg, float strength, int ttlTick, float confidence,
                              float distanceMeters, String sourceName) {
        this.emitterId = emitterId;
        this.emitterKind = emitterKind;
        this.threatMode = threatMode;
        this.bearingDeg = bearingDeg;
        this.strength = strength;
        this.ttlTick = ttlTick;
        this.confidence = confidence;
        this.distanceMeters = distanceMeters;
        this.sourceName = sourceName != null ? sourceName : "?";
    }

    public static float clamp01(float v) {
        if (v < 0.0F) {
            return 0.0F;
        }
        if (v > 1.0F) {
            return 1.0F;
        }
        return v;
    }
}
