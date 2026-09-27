package mcheli.render;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MCH_WGMapOcclusionTest {

    @Test
    public void stableWgmapResultNamesMapWithoutCollapsingUnknown() {
        assertEquals(MCH_WGMapOcclusion.Result.CLEAR, MCH_WGMapOcclusion.mapApiResultName("CLEAR"));
        assertEquals(MCH_WGMapOcclusion.Result.BLOCKED, MCH_WGMapOcclusion.mapApiResultName("BLOCKED"));
        assertEquals(MCH_WGMapOcclusion.Result.UNKNOWN, MCH_WGMapOcclusion.mapApiResultName("UNKNOWN"));
        assertEquals(MCH_WGMapOcclusion.Result.MISSING_DATA, MCH_WGMapOcclusion.mapApiResultName("MISSING_DATA"));
        assertEquals(MCH_WGMapOcclusion.Result.UNCERTAIN_BLOCK, MCH_WGMapOcclusion.mapApiResultName("UNCERTAIN_BLOCK"));
        assertEquals(MCH_WGMapOcclusion.Result.UNKNOWN, MCH_WGMapOcclusion.mapApiResultName("FUTURE_VALUE"));
        assertEquals(MCH_WGMapOcclusion.Result.UNKNOWN, MCH_WGMapOcclusion.mapApiResultName(null));
    }

    @Test
    public void onlyConfirmedTerrainObstructionSuppressesWithoutPriorEvidence() {
        assertTrue(MCH_WGMapOcclusion.shouldSuppress(MCH_WGMapOcclusion.Result.BLOCKED));
        assertFalse(MCH_WGMapOcclusion.shouldSuppress(MCH_WGMapOcclusion.Result.UNKNOWN));
        assertFalse(MCH_WGMapOcclusion.shouldSuppress(MCH_WGMapOcclusion.Result.MISSING_DATA));
        assertFalse(MCH_WGMapOcclusion.shouldSuppress(MCH_WGMapOcclusion.Result.UNCERTAIN_BLOCK));
        assertFalse(MCH_WGMapOcclusion.shouldSuppress(MCH_WGMapOcclusion.Result.CLEAR));
        assertFalse(MCH_WGMapOcclusion.shouldSuppress(MCH_WGMapOcclusion.Result.UNAVAILABLE));
        assertFalse(MCH_WGMapOcclusion.shouldSuppress(null));
    }
}
