package ru.nsu.masolygin.list;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import ru.nsu.masolygin.sort.SortDelay;
import ru.nsu.masolygin.sort.SortWorkers;
import ru.nsu.masolygin.sort.StepDelay;

@Timeout(15)
class LinkedConcurrencyTest {
    @Test
    void testSnapshotReturnsWhileNodesAreLockedInsideStep() throws Exception {
        LinkedStringList list = new LinkedStringList();
        list.addFirst("a");
        list.addFirst("b");
        CountDownLatch inside = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        StepDelay delay = new StepDelay() {
            @Override
            public void insideStep() throws InterruptedException {
                inside.countDown();
                if (!release.await(5, TimeUnit.SECONDS)) {
                    throw new InterruptedException("Test pause timed out");
                }
            }
            @Override
            public void betweenSteps() {

            }
        };
        try (var tasks = Executors.newFixedThreadPool(2)) {
            try {
                var sorted = tasks.submit(() -> list.sortPass(delay));
                assertTrue(inside.await(5, TimeUnit.SECONDS));
                var snapshot = tasks.submit(() -> values(list));
                assertEquals(List.of("b", "a"), snapshot.get(1, TimeUnit.SECONDS));
                release.countDown();
                assertEquals(1, sorted.get(5, TimeUnit.SECONDS));
                assertEquals(List.of("a", "b"), values(list));
            } finally {
                release.countDown();
            }
        }
    }

    @Test
    void testConcurrentInsertionAndSnapshotsNeverLoseRecords() {
        LinkedStringList list = new LinkedStringList();
        try (SortWorkers workers = new SortWorkers(list, new SortDelay(0, 0), 4, 0)) {
            workers.start();
            List<String> expected = new ArrayList<>();
            for (int index = 0; index < 100; index++) {
                String value = "record-" + index;
                list.addFirst(value);
                expected.add(value);
                List<String> snapshot = values(list);
                Collections.sort(snapshot);
                List<String> orderedExpected = new ArrayList<>(expected);
                Collections.sort(orderedExpected);
                assertEquals(orderedExpected, snapshot);
            }
        }
    }

    private List<String> values(LinkedStringList list) {
        List<String> values = new ArrayList<>();
        for (String value : list) {
            values.add(value);
        }
        return values;
    }
}
