package com.rental.service.charge;

import com.rental.model.Charge;
import com.rental.model.RentalAgreement;
import com.rental.model.ReturnDetails;

/**
 * Fixed rule: FEE_PER_MISSING_STEP for every fuel-gauge step the vehicle is
 * returned below the level it was handed over with.
 */
public class FuelShortageCharge implements ChargeRule {
    public static final double FEE_PER_MISSING_STEP = 15.00;

    @Override
    public Charge apply(RentalAgreement agreement, ReturnDetails details) {
        int missing = agreement.getStartFuelLevel().ordinal() - details.getEndFuelLevel().ordinal();
        if (missing <= 0) {
            return new Charge("Fuel shortage", 0.0);
        }
        return new Charge("Fuel shortage (" + missing + " level(s) short)", missing * FEE_PER_MISSING_STEP);
    }
}
