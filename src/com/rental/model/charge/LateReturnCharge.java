package com.rental.model.charge;

import java.time.temporal.ChronoUnit;

import com.rental.model.RentalAgreement;

/**
 * Fixed rule (FR21): LATE_FEE_PER_DAY is charged for every day the vehicle is
 * returned after the reservation's planned return date.
 */
public class LateReturnCharge implements ChargeRule {

    public static final double LATE_FEE_PER_DAY = 20.00;

    @Override
    public double calculate(RentalAgreement agreement) {
        if (agreement.getActualReturnDateTime() == null) {
            return 0.0;
        }
        long lateDays = ChronoUnit.DAYS.between(
                agreement.getReservation().getReturnDate(),
                agreement.getActualReturnDateTime().toLocalDate());
        return lateDays > 0 ? lateDays * LATE_FEE_PER_DAY : 0.0;
    }
}
