package com.rental.model;

import java.time.LocalDate;

import com.rental.enums.PaymentMethod;

/**
 * UC-10: payment (amount, date, method) recorded against a rental agreement.
 */
public class Payment {
    private String paymentId;
    private RentalAgreement rentalAgreement;
    private double amountPaid;
    private LocalDate paymentDate;
    private PaymentMethod method;
    /** Outstanding balance of the agreement right after this payment (shown on the receipt). */
    private double balanceAfter;

    public Payment() {
    }

    public Payment(String paymentId, RentalAgreement rentalAgreement, double amountPaid,
                   LocalDate paymentDate, PaymentMethod method, double balanceAfter) {
        this.paymentId = paymentId;
        this.rentalAgreement = rentalAgreement;
        this.amountPaid = amountPaid;
        this.paymentDate = paymentDate;
        this.method = method;
        this.balanceAfter = balanceAfter;
    }

    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }
    public RentalAgreement getRentalAgreement() { return rentalAgreement; }
    public void setRentalAgreement(RentalAgreement rentalAgreement) { this.rentalAgreement = rentalAgreement; }
    public double getAmountPaid() { return amountPaid; }
    public void setAmountPaid(double amountPaid) { this.amountPaid = amountPaid; }
    public LocalDate getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDate paymentDate) { this.paymentDate = paymentDate; }
    public PaymentMethod getMethod() { return method; }
    public void setMethod(PaymentMethod method) { this.method = method; }
    public double getBalanceAfter() { return balanceAfter; }
    public void setBalanceAfter(double balanceAfter) { this.balanceAfter = balanceAfter; }

    @Override
    public String toString() {
        return paymentId + " - " + String.format("%.2f", amountPaid) + " (" + method + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Payment)) return false;
        Payment other = (Payment) o;
        return paymentId != null && paymentId.equals(other.paymentId);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hashCode(paymentId);
    }
}
