package ru.nsu.masolygin.sort;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import ru.nsu.masolygin.list.StringList;

public class SortWorkers implements AutoCloseable {
    private final ExecutorService workers;
    private final List<Future<?>> results = new ArrayList<>();
    private final CountDownLatch start = new CountDownLatch(1);

    public SortWorkers(StringList list, StepDelay delay, int count, int passes) {
        if (count <= 0 || passes < 0) {
            throw new IllegalArgumentException("Threads must be positive and passes must not be negative");
        }
        workers = Executors.newFixedThreadPool(count);
        for (int worker = 0; worker < count; worker++) {
            results.add(workers.submit(() -> {
                try {
                    start.await();
                    for (int pass = 0; !Thread.currentThread().isInterrupted() && (passes == 0 || pass < passes); pass++) {
                        if (list.sortPass(delay) == 0) {
                            delay.betweenSteps();
                            Thread.sleep(1);
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }));
        }
    }

    public void start() {
        start.countDown();
    }

    public void await() throws InterruptedException {
        for (Future<?> result : results) {
            try {
                result.get();
            } catch (ExecutionException e) {
                throw new IllegalStateException("Sorter failed", e.getCause());
            }
        }
    }

    @Override
    public void close() {
        workers.shutdownNow();
        try {
            if (!workers.awaitTermination(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Sorters did not stop");
            }
            for (Future<?> result : results) {
                if (result.isDone() && !result.isCancelled()) {
                    result.get();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while stopping sorters", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Sorter failed", e.getCause());
        }
    }
}
