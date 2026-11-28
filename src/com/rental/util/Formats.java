package com.rental.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Shared date/time/money formats. */
public final class Formats {
    public static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private Formats() {
    }

    public static String dateTime(LocalDateTime dt) {
        return dt == null ? "-" : dt.format(DATE_TIME);
    }

    public static String money(double amount) {
        return String.format("%.2f", amount);
    }
}
