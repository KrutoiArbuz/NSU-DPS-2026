package ru.nsu.masolygin.sort;

public interface StepDelay {
    void insideStep() throws InterruptedException;
    void betweenSteps() throws InterruptedException;
}
