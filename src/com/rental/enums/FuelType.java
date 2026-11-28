package com.rental.enums;

public enum FuelType {
    PETROL("Petrol"),
    DIESEL("Diesel"),
    HYBRID("Hybrid"),
    ELECTRIC("Electric");

    private final String label;

    FuelType(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
