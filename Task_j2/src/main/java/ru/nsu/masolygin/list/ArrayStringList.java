package ru.nsu.masolygin.list;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import ru.nsu.masolygin.sort.StepDelay;

public class ArrayStringList implements StringList {
    private final List<String> values = Collections.synchronizedList(new ArrayList<>());
    private final AtomicLong completedSteps = new AtomicLong();

    @Override
    public void addFirst(String value) {
        values.add(0, Objects.requireNonNull(value));
    }

    @Override
    public long sortPass(StepDelay delay) throws InterruptedException {
        int length = values.size();
        long count = 0;
        for (int index = 0; index + 1 < length; index++) {
            synchronized (values) {
                if (Thread.currentThread().isInterrupted()) {
                    throw new InterruptedException("Sorting interrupted");
                }
                delay.insideStep();
                String left = values.get(index);
                String right = values.get(index + 1);
                if (left.compareTo(right) > 0) {
                    values.set(index, right);
                    values.set(index + 1, left);
                }
                completedSteps.incrementAndGet();
                count++;
            }
            delay.betweenSteps();
        }
        return count;
    }

    @Override
    public Iterator<String> iterator() {
        synchronized (values) {
            return List.copyOf(values).iterator();
        }
    }

    @Override
    public long steps() {
        return completedSteps.get();
    }
}
