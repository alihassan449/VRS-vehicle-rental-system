package com.rental.enums;

public enum LicenseStatus {
    VALID("Valid"),
    EXPIRED("Expired"),
    INVALID("Invalid");

    private final String label;

    LicenseStatus(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
