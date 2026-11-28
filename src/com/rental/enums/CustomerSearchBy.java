package com.rental.enums;

public enum CustomerSearchBy {
    CUSTOMER_ID("Customer ID"),
    NATIONAL_ID("National ID"),
    NAME("Name");

    private final String label;

    CustomerSearchBy(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
