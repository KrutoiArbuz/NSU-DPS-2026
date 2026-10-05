package ru.nsu.masolygin.list;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import ru.nsu.masolygin.sort.StepDelay;

public class LinkedStringList implements StringList {
    private final Node head = new Node(null, null);
    private final ReentrantReadWriteLock topology = new ReentrantReadWriteLock(true);
    private final AtomicLong completedSteps = new AtomicLong();
    private int size;

    @Override
    public void addFirst(String value) {
        Objects.requireNonNull(value);
        head.lock();
        try {
            topology.writeLock().lock();
            try {
                head.next = new Node(value, head.next);
                size++;
            } finally {
                topology.writeLock().unlock();
            }
        } finally {
            head.unlock();
        }
    }

    @Override
    public long sortPass(StepDelay delay) throws InterruptedException {
        int length = size();
        long count = 0;
        for (int index = 0; index + 1 < length; index++) {
            if (sortStep(index, delay)) {
                count++;
                delay.betweenSteps();
            }
        }
        return count;
    }

    private boolean sortStep(int index, StepDelay delay) throws InterruptedException {
        Node previous = lockPrevious(index);
        try {
            Node left = previous.next;
            if (left == null) {
                return false;
            }
            left.lockInterruptibly();
            try {
                Node right = left.next;
                if (right == null) {
                    return false;
                }
                right.lockInterruptibly();
                try {
                    delay.insideStep();
                    if (left.value.compareTo(right.value) > 0) {
                        topology.writeLock().lockInterruptibly();
                        try {
                            left.next = right.next;
                            right.next = left;
                            previous.next = right;
                        } finally {
                            topology.writeLock().unlock();
                        }
                    }
                    completedSteps.incrementAndGet();
                    return true;
                } finally {
                    right.unlock();
                }
            } finally {
                left.unlock();
            }
        } finally {
            previous.unlock();
        }
    }

    private Node lockPrevious(int index) throws InterruptedException {
        Node previous = head;
        previous.lockInterruptibly();
        try {
            for (int position = 0; position < index && previous.next != null; position++) {
                Node next = previous.next;
                next.lockInterruptibly();
                previous.unlock();
                previous = next;
            }
            return previous;
        } catch (InterruptedException | RuntimeException e) {
            previous.unlock();
            throw e;
        }
    }

    private int size() {
        topology.readLock().lock();
        try {
            return size;
        } finally {
            topology.readLock().unlock();
        }
    }

    @Override
    public Iterator<String> iterator() {
        topology.readLock().lock();
        try {
            List<String> values = new ArrayList<>();
            for (Node node = head.next; node != null; node = node.next) {
                values.add(node.value);
            }
            return List.copyOf(values).iterator();
        } finally {
            topology.readLock().unlock();
        }
    }

    @Override
    public long steps() {
        return completedSteps.get();
    }
}
