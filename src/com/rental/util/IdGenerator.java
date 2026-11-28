package com.rental.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Sequential ID generator, e.g. {@code IdGenerator.next("CUS")} -> {@code "CUS-0001"}.
 * Supports persistence-backed counters via {@link CounterStore}.
 */
public class IdGenerator {

    @FunctionalInterface
    public interface CounterStore {
        void saveNext(String prefix, int nextValue);
    }

    private static final Map<String, AtomicInteger> counters = new ConcurrentHashMap<>();
    private static volatile CounterStore counterStore;

    public static void setCounterStore(CounterStore store) {
        counterStore = store;
    }

    public static void initCounter(String prefix, int nextValue) {
        counters.compute(prefix, (p, existing) -> {
            int val = existing == null ? nextValue : Math.max(existing.get(), nextValue);
            return new AtomicInteger(val);
        });
    }

    public static String next(String prefix) {
        AtomicInteger counter = counters.computeIfAbsent(prefix, p -> new AtomicInteger(1));
        int value = counter.getAndIncrement();
        CounterStore store = counterStore;
        if (store != null) {
            store.saveNext(prefix, value + 1);
        }
        return String.format("%s-%04d", prefix, value);
    }
}
