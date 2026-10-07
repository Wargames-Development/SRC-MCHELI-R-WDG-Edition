package mcheli;

import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MCH_AircraftRenderStateTest {
    private final UUID uuid = UUID.fromString("8a7211b2-9609-443b-8dcb-b7833bfd1d53");
    private final MCH_EntityInfoClientTracker.AircraftRenderState state =
        new MCH_EntityInfoClientTracker.AircraftRenderState();

    private MCH_EntityInfo contact(boolean destroyed) {
        MCH_EntityInfo info = new MCH_EntityInfo(37, "world", "tank", "mcheli.tank.MCH_EntityTank",
            10, 64, 20, 10, 64, 20);
        info.aircraftUuid = uuid;
        info.destroyed = destroyed;
        return info;
    }

    @Test public void destructionCannotBeOverriddenByAliveClientCopyOrExpiredTombstone() {
        assertFalse(state.shouldSuppress(contact(false), true, false, 10, 0));
        assertTrue(state.shouldSuppress(contact(true), true, false, 12, 100));
        assertTrue(state.shouldSuppress(null, false, false, 14, 200));
        assertTrue(state.shouldSuppress(null, false, false, 100, 10_000));
    }

    @Test public void realWreckRemainsUntilServerPresenceIsLost() {
        assertFalse(state.shouldSuppress(contact(true), true, true, 10, 0));
        assertFalse(state.shouldSuppress(null, false, true, 12, 100));
        assertTrue(state.shouldSuppress(null, false, true, 72, 3_100));
    }

    @Test public void missingDestroyPacketStillExpiresWithContinuingSnapshots() {
        MCH_EntityInfo stale = contact(false);
        assertFalse(state.shouldSuppress(stale, true, false, 10, 0));
        assertFalse(state.shouldSuppress(stale, false, false, 12, 100));
        assertFalse(state.shouldSuppress(stale, false, false, 70, 3_099));
        assertTrue(state.shouldSuppress(stale, false, false, 72, 3_100));
        assertTrue(state.shouldSuppress(null, false, false, 74, 3_200));
    }

    @Test public void spawnAndChunkHandoffRecoverWithoutChangingLiveVisibility() {
        assertFalse(state.shouldSuppress(null, false, false, 10, 0));
        assertFalse(state.shouldSuppress(null, false, false, 12, 100));
        assertFalse(state.shouldSuppress(contact(false), true, false, 14, 200));
        assertFalse(state.shouldSuppress(null, false, false, 16, 250));
        assertFalse(state.shouldSuppress(contact(false), true, false, 100, 5_000));
    }

    @Test public void stalledOrUnavailableSnapshotsDoNotInventRemoval() {
        assertFalse(state.shouldSuppress(null, false, false, -1, 0));
        assertFalse(state.shouldSuppress(null, false, false, -1, 10_000));
        assertFalse(state.shouldSuppress(null, false, false, 10, 20_000));
        assertFalse(state.shouldSuppress(null, false, false, 10, 30_000));
    }

    @Test public void reusedIdCannotValidateOldUuidAndNewUuidCanRender() {
        assertFalse(state.shouldSuppress(contact(false), true, false, 8, 0));
        MCH_EntityInfo replacement = contact(false);
        replacement.aircraftUuid = UUID.fromString("646762ac-451a-47ba-951d-2334f757c236");
        assertTrue(state.shouldSuppress(replacement, true, false, 10, 0));
        assertFalse(new MCH_EntityInfoClientTracker.AircraftRenderState().shouldSuppress(
            replacement, true, false, 10, 0));
    }

    @Test public void legacySnapshotWithoutUuidStillConfirmsPresence() {
        MCH_EntityInfo legacy = contact(false);
        legacy.aircraftUuid = null;
        assertFalse(state.shouldSuppress(legacy, true, false, 10, 0));
    }
}
