package ru.nsu.masolygin.list;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;
import ru.nsu.masolygin.benchmark.BenchmarkResult;
import ru.nsu.masolygin.benchmark.BenchmarkRunner;
import ru.nsu.masolygin.cli.InputSession;
import ru.nsu.masolygin.cli.Arguments;
import ru.nsu.masolygin.sort.SortDelay;
import ru.nsu.masolygin.sort.SortWorkers;

public class ListMain {
    public static void main(String[] args) {
        try {
            Arguments options = new Arguments(args,
                    Set.of("mode", "list", "threads", "inside-ms", "between-ms", "size", "passes"));
            int threads = Integer.parseInt(options.optional("threads", "2"));
            SortDelay delay = new SortDelay(Long.parseLong(options.optional("inside-ms", "1000")),
                    Long.parseLong(options.optional("between-ms", "1000")));
            String mode = options.optional("mode", "interactive");

            if (mode.equals("benchmark")) {
                int size = Integer.parseInt(options.optional("size", "20"));
                int passes = Integer.parseInt(options.optional("passes", "3"));
                BenchmarkRunner benchmark = new BenchmarkRunner();

                for (String implementation : new String[] { "linked", "array" }) {
                    StringList list = implementation.equals("linked") ? new LinkedStringList() : new ArrayStringList();
                    BenchmarkResult result = benchmark.run(implementation, list, size, threads, passes, delay);
                    System.out.printf(Locale.ROOT, "%s: theoretical=%d actual=%d seconds=%.3f ideal_lower_bound=%.3f%n",
                            result.implementation(), result.theoreticalSteps(), result.actualSteps(), result.seconds(),
                            result.idealSeconds());
                }
            } else if (mode.equals("interactive")) {
                String implementation = options.optional("list", "linked");
                if (!implementation.equals("linked") && !implementation.equals("array")) {
                    throw new IllegalArgumentException("List must be linked or array");
                }
                StringList list = implementation.equals("linked") ? new LinkedStringList() : new ArrayStringList();
                try (SortWorkers workers = new SortWorkers(list, delay, threads, 0)) {
                    workers.start();
                    new InputSession().run(new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8)),
                            System.out, list);
                }
                System.err.println("Completed steps: " + list.steps());
            } else {
                throw new IllegalArgumentException("Mode must be interactive or benchmark");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Experiment interrupted");
        } catch (IOException | IllegalArgumentException | IllegalStateException | ArithmeticException e) {
            System.err.println("List lab error: " + e.getMessage());
        }
    }
}
