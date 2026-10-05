package ru.nsu.masolygin.benchmark;

import java.util.Locale;
import ru.nsu.masolygin.list.StringList;
import ru.nsu.masolygin.sort.SortDelay;
import ru.nsu.masolygin.sort.SortWorkers;

public class BenchmarkRunner {
    public BenchmarkResult run(String name, StringList list, int size, int threads, int passes, SortDelay delay)
            throws InterruptedException {
        if (size < 0 || passes <= 0) {
            throw new IllegalArgumentException("Size must not be negative and passes must be positive");
        }

        for (int index = 0; index < size; index++) {
            list.addFirst(String.format(Locale.ROOT, "%08d", index));
        }

        long theoretical = (long) threads * passes * Math.max(0, size - 1);

        long begin = System.nanoTime();
        try (SortWorkers workers = new SortWorkers(list, delay, threads, passes)) {
            workers.start();
            workers.await();
        }
        double seconds = (System.nanoTime() - begin) / 1_000_000_000.0;

        long actual = list.steps();
        if (actual != theoretical) {
            throw new IllegalStateException("Step count mismatch: expected " + theoretical + ", actual " + actual);
        }

        int parallelInside = name.equals("linked") ? Math.min(threads, (size + 1) / 3) : 1;
        double workBound = theoretical * (delay.insideMillis() + (double) delay.betweenMillis()) / Math.max(1, threads);
        double lockBound = theoretical * (double) delay.insideMillis() / Math.max(1, parallelInside);
        double ideal = Math.max(workBound, lockBound) / 1000.0;
        return new BenchmarkResult(name, theoretical, actual, seconds, ideal);
    }
}
