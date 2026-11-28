package com.rental.enums;

public enum ReservationStatus {
    PENDING("Pending"),
    CONFIRMED("Confirmed"),
    CANCELLED("Cancelled"),
    COLLECTED("Collected"),
    EXPIRED("Expired");

    private final String label;

    ReservationStatus(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
