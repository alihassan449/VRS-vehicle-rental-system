package com.rental.enums;

public enum FuelLevel {
    EMPTY("Empty"),
    QUARTER("1/4"),
    HALF("1/2"),
    THREE_QUARTER("3/4"),
    FULL("Full");

    private final String label;

    FuelLevel(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
