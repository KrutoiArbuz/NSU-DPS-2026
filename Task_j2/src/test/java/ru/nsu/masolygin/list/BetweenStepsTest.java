package ru.nsu.masolygin.list;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import ru.nsu.masolygin.sort.StepDelay;

@Timeout(15)
class BetweenStepsTest {
    @ParameterizedTest
    @ValueSource(strings = {"linked", "array"})
    void testPartialSnapshotIsAvailableBetweenSteps(String implementation) throws Exception {
        StringList list = implementation.equals("linked") ? new LinkedStringList() : new ArrayStringList();
        for (String value : List.of("a", "b", "c")) {
            list.addFirst(value);
        }
        CountDownLatch paused = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        StepDelay delay = new StepDelay() {
            @Override
            public void insideStep() {
            }
            @Override
            public void betweenSteps() throws InterruptedException {
                paused.countDown();
                release.await();
            }
        };
        try (var tasks = Executors.newFixedThreadPool(2)) {
            var sorting = tasks.submit(() -> list.sortPass(delay));
            try {
                assertTrue(paused.await(5, TimeUnit.SECONDS));
                var snapshot = tasks.submit(() -> {
                    List<String> values = new ArrayList<>();
                    for (String value : list) {
                        values.add(value);
                    }
                    return values;
                });
                assertEquals(List.of("b", "c", "a"), snapshot.get(1, TimeUnit.SECONDS));
            } finally {
                release.countDown();
            }
            assertEquals(2L, sorting.get(5, TimeUnit.SECONDS));
        }
    }
}
