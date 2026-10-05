package ru.nsu.masolygin.benchmark;

public record BenchmarkResult(
        String implementation,
        long theoreticalSteps,
        long actualSteps,
        double seconds,
        double idealSeconds) {
}
