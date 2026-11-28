package com.rental.enums;

public enum PaymentMethod {
    CASH("Cash"),
    CARD("Card"),
    BANK_TRANSFER("Bank Transfer"),
    MOBILE_WALLET("Mobile Wallet");

    private final String label;

    PaymentMethod(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
