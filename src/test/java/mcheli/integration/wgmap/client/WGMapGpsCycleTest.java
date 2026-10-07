package mcheli.integration.wgmap.client;

import org.junit.Test;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import static org.junit.Assert.assertEquals;

public class WGMapGpsCycleTest {
    private final UUID first = new UUID(0, 1);
    private final UUID second = new UUID(0, 2);
    private final UUID third = new UUID(0, 3);

    @Test public void firstPressSelectsFirstAndEachPressAdvancesWithWraparound() {
        List<UUID> ids = Arrays.asList(first, second, third);
        assertEquals(0, WGMapGpsClientBridge.nextIndex(ids, null));
        assertEquals(1, WGMapGpsClientBridge.nextIndex(ids, first));
        assertEquals(2, WGMapGpsClientBridge.nextIndex(ids, second));
        assertEquals(0, WGMapGpsClientBridge.nextIndex(ids, third));
    }

    @Test public void emptySingleAndDeletedSelectionsAreSafe() {
        assertEquals(-1, WGMapGpsClientBridge.nextIndex(Collections.<UUID>emptyList(), first));
        assertEquals(0, WGMapGpsClientBridge.nextIndex(Collections.singletonList(first), first));
        assertEquals(0, WGMapGpsClientBridge.nextIndex(Arrays.asList(first, third), second));
    }
}
