package com.rental.model.charge;

import com.rental.model.RentalAgreement;

/**
 * Fixed rule (FR21): ALLOWED_KM_PER_DAY free kilometres per rental day.
 * Every kilometre above the allowance costs RATE_PER_EXCESS_KM.
 */
public class ExcessMileageCharge implements ChargeRule {

    public static final double ALLOWED_KM_PER_DAY = 200.0;
    public static final double RATE_PER_EXCESS_KM = 0.50;

    @Override
    public double calculate(RentalAgreement agreement) {
        double distance = agreement.getEndingMileage() - agreement.getStartingMileage();
        double allowed = agreement.getReservation().getRentalDays() * ALLOWED_KM_PER_DAY;
        double excess = distance - allowed;
        return excess > 0 ? excess * RATE_PER_EXCESS_KM : 0.0;
    }
}
