package mcheli.weapon;

import net.minecraft.entity.Entity;
import net.minecraft.profiler.Profiler;
import net.minecraft.world.*;
import net.minecraft.world.chunk.IChunkProvider;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

public class MCH_LaserStateWorldTest {
    private static final int OWNER = -14001;
    @After public void clear() { MCH_LaserStateStore.clearOwner(OWNER); }

    @Test public void anotherWorldCannotConsumeOrExpireTheCurrentDesignation() {
        ClockWorld first = new ClockWorld(), second = new ClockWorld();
        MCH_LaserStateStore.upsertServerState(OWNER, 1, 1000, 64, 0, true, 1, 100, first);
        assertNull(MCH_LaserStateStore.getServerState(OWNER, 1, 10000, second));
        assertNotNull(MCH_LaserStateStore.getServerState(OWNER, 1, 101, first));
    }

    @Test public void worldChangeCanStartANewSequenceWithoutOldMissilesRemovingIt() {
        ClockWorld first = new ClockWorld(), second = new ClockWorld();
        MCH_LaserStateStore.upsertServerState(OWNER, 1, 1000, 64, 0, true, 100, 10000, first);
        MCH_LaserStateStore.upsertServerState(OWNER, 1, 500, 64, 0, true, 1, 100, second);
        assertNull(MCH_LaserStateStore.getServerState(OWNER, 1, 10001, first));
        assertEquals(500, MCH_LaserStateStore.getServerState(OWNER, 1, 101, second).x, 0);
    }

    @Test public void expiryUsesEachOwningWorldsClockAndChannelsRemainSeparate() {
        ClockWorld first = new ClockWorld(), second = new ClockWorld();
        first.tick = 101; second.tick = 10001;
        MCH_LaserStateStore.upsertServerState(OWNER, 1, 1000, 64, 0, true, 1, 100, first);
        MCH_LaserStateStore.upsertServerState(OWNER, 2, 500, 64, 0, true, 1, 10000, second);
        MCH_LaserStateStore.expireServerStates(10001, 40);
        assertNotNull(MCH_LaserStateStore.getServerState(OWNER, 1, 101, first));
        assertNotNull(MCH_LaserStateStore.getServerState(OWNER, 2, 10001, second));
        first.tick = 141;
        MCH_LaserStateStore.expireServerStates(10001, 40);
        assertNull(MCH_LaserStateStore.getServerState(OWNER, 1));
        assertNotNull(MCH_LaserStateStore.getServerState(OWNER, 2));
    }

    private static final class ClockWorld extends World {
        long tick;
        ClockWorld() {
            super(null, "laser_test", new WorldProviderSurface() {
                @Override protected void registerWorldChunkManager() { }
                @Override public void calculateInitialWeather() { }
            }, new WorldSettings(0, WorldSettings.GameType.SURVIVAL, false, false, WorldType.DEFAULT), new Profiler());
        }
        @Override protected IChunkProvider createChunkProvider() { return null; }
        @Override protected int func_152379_p() { return 0; }
        @Override public Entity getEntityByID(int id) { return null; }
        @Override public void calculateInitialSkylight() { }
        @Override public long getTotalWorldTime() { return tick; }
    }
}
