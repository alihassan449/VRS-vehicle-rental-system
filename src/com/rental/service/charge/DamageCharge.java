package com.rental.service.charge;

import com.rental.model.Charge;
import com.rental.model.RentalAgreement;
import com.rental.model.ReturnDetails;

/** Fixed rule: one fixed damage fee when new damage was recorded at return. */
public class DamageCharge implements ChargeRule {
    public static final double FIXED_DAMAGE_FEE = 50.00;

    @Override
    public Charge apply(RentalAgreement agreement, ReturnDetails details) {
        if (!details.hasNewDamage()) {
            return new Charge("Damage fee", 0.0);
        }
        return new Charge("Damage fee (fixed)", FIXED_DAMAGE_FEE);
    }
}
