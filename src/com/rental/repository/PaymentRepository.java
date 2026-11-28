package com.rental.repository;

import java.time.LocalDate;
import java.util.List;

import com.rental.model.Payment;
import com.rental.model.RentalAgreement;

/**
 * FR24: storage of Payment records.
 */
public interface PaymentRepository {

    void save(Payment payment);

    List<Payment> findByRentalAgreement(RentalAgreement agreement);

    // FR31: revenue report for a date range
    List<Payment> findByDateRange(LocalDate from, LocalDate to);

    List<Payment> findAll();
}
