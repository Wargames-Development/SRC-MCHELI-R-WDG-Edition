package mcheli.render;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MCH_GpsMarkerProjectionTest {
    @Test public void nearbyMarkerKeepsItsPhysicalPosition() {
        assertEquals(1.0D, MCH_RenderGPSPosition.markerProjectionScale(50, 8), 0.0D);
        assertEquals(1.0D, MCH_RenderGPSPosition.markerProjectionScale(112, 8), 0.0D);
    }

    @Test public void distantMarkerKeepsDirectionAndPixelSizeInsideFarPlane() {
        for (int chunks : new int[]{1, 2, 8, 16, 32, 128}) {
            double safe = Math.max(16, chunks * 16 - 16);
            for (double distance : new double[]{4_096, 50_000, 80_000_000}) {
                double scale = MCH_RenderGPSPosition.markerProjectionScale(distance, chunks);
                assertTrue(scale > 0 && scale <= 1);
                assertTrue(distance * scale <= safe + 8.000001D);
                // Applying the same factor to position and glyph size preserves angular size.
                assertEquals(24.0D, (24 * distance * scale) / (distance * scale), 0.000001D);
                assertEquals(0.6D, (distance * 0.6D * scale) / (distance * scale), 0.000001D);
            }
        }
    }

    @Test public void invalidDistancesRejectAndMissingSettingsUseDefault() {
        assertTrue(Double.isNaN(MCH_RenderGPSPosition.markerProjectionScale(Double.NaN, 8)));
        assertTrue(Double.isNaN(MCH_RenderGPSPosition.markerProjectionScale(Double.POSITIVE_INFINITY, 8)));
        assertTrue(Double.isNaN(MCH_RenderGPSPosition.markerProjectionScale(0, 8)));
        assertEquals(MCH_RenderGPSPosition.markerProjectionScale(5_000, 8),
                MCH_RenderGPSPosition.markerProjectionScale(5_000, 0), 0.0D);
    }
}
