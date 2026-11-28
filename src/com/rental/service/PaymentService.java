package com.rental.service;

import java.time.LocalDate;

import com.rental.enums.PaymentMethod;
import com.rental.exception.BusinessRuleException;
import com.rental.exception.ValidationException;
import com.rental.model.Payment;
import com.rental.model.RentalAgreement;
import com.rental.report.Invoice;
import com.rental.report.Receipt;
import com.rental.repository.PaymentRepository;
import com.rental.util.IdGenerator;

/**
 * UC-10 Record Payment &amp; Print Receipt.
 */
public class PaymentService {
    private final PaymentRepository paymentRepository;

    public PaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    /**
     * UC-10 steps 2.1 - 3.2: validate the payment against the outstanding balance,
     * save it against the agreement and update the balance.
     */
    public synchronized Payment recordPayment(RentalAgreement agreement, Double amount, LocalDate date,
                                              PaymentMethod method) {
        if (agreement == null) {
            throw new ValidationException("Retrieve and select a rental agreement first.");
        }
        if (!agreement.isReturned()) {
            throw new BusinessRuleException("Return Not Processed: the vehicle has not been returned and no final "
                    + "payable amount exists, so payment and invoice generation are not allowed.");
        }
        if (amount == null) {
            throw new ValidationException("Invalid amount: enter the payment amount as a number.");
        }
        amount = Math.round(amount * 100.0) / 100.0;
        if (amount <= 0) {
            throw new ValidationException("Invalid amount: the payment must be greater than zero.");
        }
        if (amount > agreement.balanceDue()) {
            throw new ValidationException("Invalid amount: the payment exceeds the outstanding balance of "
                    + String.format("%.2f", agreement.balanceDue()) + ".");
        }
        if (date == null) {
            throw new ValidationException("Payment date is required (yyyy-MM-dd).");
        }
        if (method == null) {
            throw new ValidationException("Payment method is required.");
        }
        Payment payment = new Payment(IdGenerator.next("PAY"), agreement, amount, date, method, 0);
        agreement.addPayment(payment);
        payment.setBalanceAfter(agreement.balanceDue());
        paymentRepository.save(payment);
        return payment;
    }

    /** UC-10 step 3.3: the printable rental invoice. */
    public String renderInvoice(RentalAgreement agreement) {
        return new Invoice(agreement).render();
    }

    /** UC-10 step 3.3: the printable payment receipt (shows the remaining balance). */
    public String renderReceipt(Payment payment) {
        return new Receipt(payment).render();
    }
}
