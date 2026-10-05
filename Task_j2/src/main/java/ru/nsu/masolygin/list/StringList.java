package ru.nsu.masolygin.list;

import ru.nsu.masolygin.sort.StepDelay;

public interface StringList extends Iterable<String> {
    void addFirst(String value);
    long sortPass(StepDelay delay) throws InterruptedException;
    long steps();
}
