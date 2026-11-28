package com.rental.enums;

public enum VehicleStatus {
    AVAILABLE("Available"),
    RESERVED("Reserved"),
    RENTED("Rented"),
    UNDER_MAINTENANCE("Under Maintenance"),
    INACTIVE("Inactive");

    private final String label;

    VehicleStatus(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
