package ru.nsu.masolygin.list;

import java.util.concurrent.locks.ReentrantLock;

class Node {
    final String value;
    private final ReentrantLock lock = new ReentrantLock();
    Node next;

    Node(String value, Node next) {
        this.value = value;
        this.next = next;
    }

    void lock() {
        lock.lock();
    }

    void lockInterruptibly() throws InterruptedException {
        lock.lockInterruptibly();
    }

    void unlock() {
        lock.unlock();
    }
}
