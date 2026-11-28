package com.rental.enums;

public enum TransmissionType {
    MANUAL("Manual"),
    AUTOMATIC("Automatic");

    private final String label;

    TransmissionType(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
