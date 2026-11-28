package com.rental.service.charge;

import com.rental.model.Charge;
import com.rental.model.RentalAgreement;
import com.rental.model.ReturnDetails;

/**
 * Fixed rule: ALLOWED_KM_PER_DAY free kilometres per rental day; every
 * kilometre beyond that costs RATE_PER_EXCESS_KM.
 */
public class ExcessMileageCharge implements ChargeRule {
    public static final double ALLOWED_KM_PER_DAY = 200.0;
    public static final double RATE_PER_EXCESS_KM = 0.50;

    @Override
    public Charge apply(RentalAgreement agreement, ReturnDetails details) {
        double driven = details.getEndMileage() - agreement.getStartMileage();
        double allowed = agreement.getReservation().getRentalDays() * ALLOWED_KM_PER_DAY;
        double excess = driven - allowed;
        if (excess <= 0) {
            return new Charge("Excess mileage", 0.0);
        }
        return new Charge(String.format("Excess mileage (%.0f km over)", excess), excess * RATE_PER_EXCESS_KM);
    }
}
