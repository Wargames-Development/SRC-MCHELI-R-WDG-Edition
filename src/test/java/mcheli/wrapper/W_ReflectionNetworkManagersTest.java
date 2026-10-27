package mcheli.wrapper;

import net.minecraft.network.NetworkManager;
import net.minecraft.network.NetworkSystem;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class W_ReflectionNetworkManagersTest {
    @Test
    public void connectionChangesDoNotInvalidateSnapshotIteration() throws Exception {
        NetworkSystem system = new NetworkSystem(null);
        List<NetworkManager> live = liveManagers(system);
        NetworkManager first = new NetworkManager(false);
        NetworkManager second = new NetworkManager(false);
        live.add(first);
        live.add(second);

        List snapshot = W_Reflection.getNetworkManagers(system);
        Iterator iterator = snapshot.iterator();
        assertSame(first, iterator.next());
        live.remove(first);
        live.add(new NetworkManager(false));

        assertSame(second, iterator.next());
        assertFalse(iterator.hasNext());
        assertEquals(Arrays.asList(first, second), snapshot);
        snapshot.clear();
        assertEquals(2, live.size());
        assertSame(second, live.get(0));
    }

    @Test
    public void snapshotCopyHoldsConnectionListMonitor() throws Exception {
        NetworkSystem system = new NetworkSystem(null);
        List<NetworkManager> guarded = new ArrayList<NetworkManager>() {
            @Override
            public Object[] toArray() {
                assertTrue("Connection list must be locked during the copy", Thread.holdsLock(this));
                return super.toArray();
            }
        };
        guarded.add(new NetworkManager(false));
        Field field = NetworkSystem.class.getDeclaredField("networkManagers");
        field.setAccessible(true);
        field.set(system, guarded);

        assertEquals(guarded, W_Reflection.getNetworkManagers(system));
    }

    @Test(timeout = 10000)
    public void snapshotsRemainStableDuringConcurrentConnectionChurn() throws Exception {
        NetworkSystem system = new NetworkSystem(null);
        List<NetworkManager> live = liveManagers(system);
        NetworkManager persistent = new NetworkManager(false);
        NetworkManager transientConnection = new NetworkManager(false);
        live.add(persistent);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<?> writer = executor.submit(() -> {
            try {
                start.await();
                for (int i = 0; i < 5000; i++) {
                    live.add(transientConnection);
                    live.remove(transientConnection);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AssertionError(e);
            }
        });
        try {
            start.countDown();
            for (int i = 0; i < 5000; i++) {
                List snapshot = W_Reflection.getNetworkManagers(system);
                assertSame(persistent, snapshot.get(0));
                for (Object manager : snapshot) {
                    assertTrue(manager == persistent || manager == transientConnection);
                }
            }
            writer.get(5, TimeUnit.SECONDS);
            assertEquals(Arrays.asList(persistent), live);
        } finally {
            executor.shutdownNow();
        }
    }

    @SuppressWarnings("unchecked")
    private static List<NetworkManager> liveManagers(NetworkSystem system) throws Exception {
        Field field = NetworkSystem.class.getDeclaredField("networkManagers");
        field.setAccessible(true);
        return (List<NetworkManager>) field.get(system);
    }
}
