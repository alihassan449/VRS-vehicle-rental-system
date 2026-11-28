package com.rental.model.charge;

import com.rental.model.RentalAgreement;

/**
 * Strategy interface for FR21. Each implementation calculates ONE kind of
 * return-time charge for a RentalAgreement. RentalAgreement.calculateFinalCharges()
 * uses one rule per charge field shown in the class diagram.
 */
public interface ChargeRule {

    /** @return the charge amount (0 if the rule does not apply). */
    double calculate(RentalAgreement agreement);
}
