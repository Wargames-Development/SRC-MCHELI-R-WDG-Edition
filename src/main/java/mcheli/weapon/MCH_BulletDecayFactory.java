package mcheli.weapon;

import java.util.ArrayList;
import java.util.List;

public class MCH_BulletDecayFactory {

    public static MCH_IBulletDecay createBulletDecay(String type, String[] args) {
        switch (type) {
            case "Segmented":
                List<MCH_BulletDecaySegmented.DecaySegment> segments = new ArrayList<>();

                for(int i = 0; i < args.length; i += 2) {
                    float startDistance = Float.parseFloat(args[i]);
                    float decayFactor = Float.parseFloat(args[i + 1]);
                    MCH_BulletDecaySegmented.DecaySegment segment = new MCH_BulletDecaySegmented.DecaySegment(startDistance, decayFactor);
                    segments.add(segment);
                }
                return new MCH_BulletDecaySegmented(segments);
            default:
                throw new IllegalArgumentException("Invalid bullet decay type: " + type);
        }
    }
}
