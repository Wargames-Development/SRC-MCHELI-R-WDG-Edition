package mcheli.flare;

import net.minecraft.util.Vec3;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MCH_FlareEjectionTest {
    @Test
    public void levelFlightUsesWiderHorizontalSpreadAndPreservesDownwardSpeed() {
        Vec3 velocity = MCH_Flare.getPlaneFlareEjection(1.0D, -1.0D, 0.0F, 0.0F, 0.0F);
        assertEquals(0.75D, velocity.xCoord, 0.0001D);
        assertEquals(-0.75D, velocity.yCoord, 0.0001D);
        assertEquals(-0.75D, velocity.zCoord, 0.0001D);
    }

    @Test
    public void bankedInvertedAndVerticalPlanesEjectTowardTheirUnderside() {
        assertVector(0.75D, 0.0D, 0.0D, 0.0F, 0.0F, 90.0F);
        assertVector(-0.75D, 0.0D, 0.0D, 0.0F, 0.0F, -90.0F);
        assertVector(0.0D, 0.75D, 0.0D, 0.0F, 0.0F, 180.0F);
        assertVector(0.0D, 0.0D, -0.75D, 0.0F, 90.0F, 0.0F);
        assertVector(0.0D, 0.0D, 0.75D, 0.0F, -90.0F, 0.0F);
        assertVector(0.0D, 0.0D, 0.75D, 90.0F, 0.0F, 90.0F);
    }

    @Test
    public void spreadCannotRemoveOutwardEjectionDuringCombinedManeuvers() {
        for (float yaw : new float[]{-170.0F, 0.0F, 65.0F}) {
            for (float pitch : new float[]{-80.0F, 0.0F, 75.0F}) {
                for (float roll : new float[]{-135.0F, 45.0F, 180.0F}) {
                    Vec3 down = MCH_Flare.getPlaneFlareEjection(0.0D, 0.0D, yaw, pitch, roll);
                    Vec3 spread = MCH_Flare.getPlaneFlareEjection(1.3D, -1.3D, yaw, pitch, roll);
                    double projection = (spread.xCoord * down.xCoord + spread.yCoord * down.yCoord
                        + spread.zCoord * down.zCoord) / down.lengthVector();
                    assertEquals(0.75D, projection, 0.001D);
                }
            }
        }
    }

    private void assertVector(double x, double y, double z, float yaw, float pitch, float roll) {
        Vec3 velocity = MCH_Flare.getPlaneFlareEjection(0.0D, 0.0D, yaw, pitch, roll);
        assertEquals(x, velocity.xCoord, 0.0002D);
        assertEquals(y, velocity.yCoord, 0.0002D);
        assertEquals(z, velocity.zCoord, 0.0002D);
    }
}
