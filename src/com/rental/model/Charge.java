package com.rental.model;

/**
 * FR21: a single line-item charge (e.g. late return, excess mileage,
 * fuel shortage, damage) applied to a RentalAgreement.
 * Produced by classes implementing com.rental.service.charge.ChargeRule.
 */
public class Charge {

    private String description;
    private double amount;

    public Charge() {
    }

    public Charge(String description, double amount) {
        this.description = description;
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    @Override
    public String toString() {
        return description + ": " + String.format("%.2f", amount);
    }
}
