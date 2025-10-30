package mcheli.weapon;

import java.util.List;

public class MCH_BulletDecaySegmented implements MCH_IBulletDecay {

    private List<MCH_BulletDecaySegmented.DecaySegment> segments;

    public MCH_BulletDecaySegmented(List<MCH_BulletDecaySegmented.DecaySegment> segments) {
        this.segments = segments;
    }

    public float calculateDecayFactor(float distanceTraveled) {
        float decayFactor = 1.0F;

        for (MCH_BulletDecaySegmented.DecaySegment segment : this.segments) {
            if (distanceTraveled > segment.startDistance) {
                decayFactor = segment.damageMultiplier;
            }
        }

        return decayFactor;
    }

    public float calculateDamage(float initialDamage, float distanceTraveled) {
        return initialDamage * this.calculateDecayFactor(distanceTraveled);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("Segmented ");

        for (MCH_BulletDecaySegmented.DecaySegment segment : this.segments) {
            sb.append(segment.toString());
        }

        return sb.toString();
    }

    public static class DecaySegment {
        float startDistance;
        float damageMultiplier;

        public DecaySegment(float startDistance, float damageMultiplier) {
            this.startDistance = startDistance;
            this.damageMultiplier = damageMultiplier;
        }

        public String toString() {
            return "| >" + this.startDistance + "m-x" + this.damageMultiplier + " ";
        }
    }
}
