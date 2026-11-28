package com.rental.model.charge;

import com.rental.model.RentalAgreement;

/**
 * Fixed rule (FR21): fuel level is stored as a percentage (0-100).
 * If the vehicle comes back with less fuel than it left with, every missing
 * percent costs FEE_PER_MISSING_PERCENT (= 15.00 per quarter tank).
 */
public class FuelShortageCharge implements ChargeRule {

    public static final double FEE_PER_MISSING_PERCENT = 0.60;

    @Override
    public double calculate(RentalAgreement agreement) {
        double missing = agreement.getPickupFuelLevel() - agreement.getReturnFuelLevel();
        return missing > 0 ? missing * FEE_PER_MISSING_PERCENT : 0.0;
    }
}
