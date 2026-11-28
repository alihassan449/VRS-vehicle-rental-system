package com.rental.service.charge;

import com.rental.model.Charge;
import com.rental.model.RentalAgreement;
import com.rental.model.ReturnDetails;

/**
 * UC-16: one kind of return-time fee (late return, excess mileage, fuel
 * shortage, damage). Returns a zero-amount Charge if the rule does not apply.
 */
public interface ChargeRule {
    Charge apply(RentalAgreement agreement, ReturnDetails details);
}
