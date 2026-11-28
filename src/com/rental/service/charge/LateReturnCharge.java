package com.rental.service.charge;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import com.rental.model.Charge;
import com.rental.model.RentalAgreement;
import com.rental.model.ReturnDetails;

/**
 * Fixed rule: LATE_FEE_PER_DAY for every day the vehicle is returned after the
 * reservation's return date (any part of a day counts as a full late day).
 */
public class LateReturnCharge implements ChargeRule {
    public static final double LATE_FEE_PER_DAY = 20.00;

    @Override
    public Charge apply(RentalAgreement agreement, ReturnDetails details) {
        LocalDate planned = agreement.getReservation().getReturnDate();
        LocalDate actual = details.getReturnDateTime().toLocalDate();
        long lateDays = ChronoUnit.DAYS.between(planned, actual);
        if (lateDays <= 0) {
            return new Charge("Late return", 0.0);
        }
        return new Charge("Late return (" + lateDays + " day(s) late)", lateDays * LATE_FEE_PER_DAY);
    }
}
