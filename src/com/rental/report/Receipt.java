package com.rental.report;

import com.rental.model.RentalAgreement;
import com.rental.model.Payment;

/** UC-10: printable payment receipt (shows the remaining balance for partial payments). */
public class Receipt {
    private final Payment payment;

    public Receipt(Payment payment) {
        this.payment = payment;
    }

    public String render() {
        StringBuilder sb = new StringBuilder();
        RentalAgreement agreement = payment.getRentalAgreement();
        sb.append("================== PAYMENT RECEIPT =================\n");
        sb.append(String.format("Receipt ID     : %s%n", payment.getPaymentId()));
        sb.append(String.format("Agreement ID   : %s%n", agreement.getAgreementId()));
        sb.append(String.format("Customer       : %s%n", agreement.getReservation().getCustomer().getName()));
        sb.append(String.format("Date           : %s%n", payment.getPaymentDate()));
        sb.append(String.format("Method         : %s%n", payment.getMethod()));
        sb.append(String.format("Amount paid    : %.2f%n", payment.getAmountPaid()));
        sb.append(String.format("Total payable  : %.2f%n", agreement.getFinalAmount()));
        sb.append(String.format("Remaining      : %.2f%s%n", payment.getBalanceAfter(),
                payment.getBalanceAfter() > 0 ? "  (partial payment)" : "  (paid in full)"));
        sb.append("====================================================\n");
        return sb.toString();
    }
}
