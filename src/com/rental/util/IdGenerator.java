package com.rental.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Generates readable sequential IDs per prefix, e.g. next("CUS") -> CUS-0001, CUS-0002, ...
 * Each prefix (CUS, RES, AGR, PAY, MNT, ...) has its own counter.
 */
public final class IdGenerator {

    private static final Map<String, Integer> counters = new HashMap<>();

    private IdGenerator() {
    }

    /**
     * Called while loading data from the database: makes sure the next generated ID is higher
     * than every ID that already exists (e.g. after loading CUS-0007 the next one is CUS-0008).
     */
    public static synchronized void observe(String id) {
        if (id == null) {
            return;
        }
        int dash = id.lastIndexOf('-');
        if (dash <= 0) {
            return;
        }
        try {
            int number = Integer.parseInt(id.substring(dash + 1));
            String prefix = id.substring(0, dash);
            if (number > counters.getOrDefault(prefix, 0)) {
                counters.put(prefix, number);
            }
        } catch (NumberFormatException e) {
            // not one of our generated IDs - ignore
        }
    }

    public static synchronized String next(String prefix) {
        int value = counters.getOrDefault(prefix, 0) + 1;
        counters.put(prefix, value);
        return String.format("%s-%04d", prefix, value);
    }
}
