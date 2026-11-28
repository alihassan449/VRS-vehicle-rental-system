package com.rental.model;

import java.time.LocalDate;

/**
 * Diagram class: Payment (FR24, FR25).
 * Association "contains": a RentalAgreement contains 1..* payments.
 */
public class Payment {

    private final String paymentID;
    private final RentalAgreement rentalAgreement;
    private final double amountPaid;
    private final LocalDate paymentDate;
    private final String paymentMethod;

    public Payment(String paymentID, RentalAgreement rentalAgreement, double amountPaid,
                   LocalDate paymentDate, String paymentMethod) {
        this.paymentID = paymentID;
        this.rentalAgreement = rentalAgreement;
        this.amountPaid = amountPaid;
        this.paymentDate = paymentDate;
        this.paymentMethod = paymentMethod;
    }

    /** FR25: printable payment receipt. */
    public String generateReceipt() {
        StringBuilder sb = new StringBuilder();
        sb.append("============== PAYMENT RECEIPT ==============\n");
        sb.append(String.format("Receipt No     : %s%n", paymentID));
        sb.append(String.format("Date           : %s%n", paymentDate));
        sb.append(String.format("Agreement      : %s%n", rentalAgreement.getAgreementID()));
        sb.append(String.format("Customer       : %s%n", rentalAgreement.getCustomer().getName()));
        sb.append(String.format("Vehicle        : %s%n", rentalAgreement.getVehicle().getRegistrationNumber()));
        sb.append(String.format("Payment method : %s%n", paymentMethod));
        sb.append("---------------------------------------------\n");
        sb.append(String.format("AMOUNT PAID    : %.2f%n", amountPaid));
        sb.append(String.format("Total paid     : %.2f%n", rentalAgreement.totalPaid()));
        sb.append(String.format("Total amount   : %.2f%n", rentalAgreement.getTotalAmount()));
        sb.append(String.format("Balance due    : %.2f%n", rentalAgreement.balanceDue()));
        sb.append("=============================================\n");
        return sb.toString();
    }

    public String getPaymentID() { return paymentID; }
    public RentalAgreement getRentalAgreement() { return rentalAgreement; }
    public double getAmountPaid() { return amountPaid; }
    public LocalDate getPaymentDate() { return paymentDate; }
    public String getPaymentMethod() { return paymentMethod; }

    @Override
    public String toString() {
        return paymentID + " - " + String.format("%.2f", amountPaid) + " (" + paymentMethod + ")";
    }
}
