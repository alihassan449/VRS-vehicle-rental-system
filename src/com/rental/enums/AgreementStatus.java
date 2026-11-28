package com.rental.enums;

public enum AgreementStatus {
    ACTIVE("Active"),
    RETURNED("Returned");

    private final String label;

    AgreementStatus(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
