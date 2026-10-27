package mcheli.integration.wgmap;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

public final class WGMapOptionalClassloadingTest {
    @Test public void unavailableSharedTargetFailsClosed() {
        assertFalse(WGMapGpsServerBridge.matchesShared(null, 0, 64, 0));
    }

    @Test public void missingLaserApiFailsClosedWithoutLoadingClientClasses() {
        assertNull(WGMapLaserServerBridge.trace(null, net.minecraft.util.Vec3.createVectorHelper(0, 64, 0),
                net.minecraft.util.Vec3.createVectorHelper(1000, 64, 0)));
    }
}
