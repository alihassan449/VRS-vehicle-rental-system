package com.rental.enums;

public enum StaffRole {
    RENTAL_STAFF("Rental Staff"),
    FLEET_STAFF("Fleet Staff"),
    ADMIN("Admin");

    private final String label;

    StaffRole(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
