package ru.nsu.masolygin.sort;

public record SortDelay(long insideMillis, long betweenMillis) implements StepDelay {
    public SortDelay {
        if (insideMillis < 0 || betweenMillis < 0) {
            throw new IllegalArgumentException("Delays must not be negative");
        }
    }

    @Override
    public void insideStep() throws InterruptedException {
        Thread.sleep(insideMillis);
    }

    @Override
    public void betweenSteps() throws InterruptedException {
        Thread.sleep(betweenMillis);
    }
}
