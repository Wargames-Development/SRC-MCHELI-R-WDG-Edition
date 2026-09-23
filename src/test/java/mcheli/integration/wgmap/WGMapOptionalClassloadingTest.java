package mcheli.integration.wgmap;

import org.junit.Test;

import static org.junit.Assert.assertFalse;

public final class WGMapOptionalClassloadingTest {
    @Test public void unavailableSharedTargetFailsClosed() {
        assertFalse(WGMapGpsServerBridge.matchesShared(null, 0, 64, 0));
    }
}
