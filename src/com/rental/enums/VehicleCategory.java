package com.rental.enums;

public enum VehicleCategory {
    ECONOMY("Economy"),
    SEDAN("Sedan"),
    SUV("SUV"),
    LUXURY("Luxury"),
    VAN("Van");

    private final String label;

    VehicleCategory(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
