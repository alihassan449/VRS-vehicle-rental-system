package com.rental.model.charge;

import com.rental.model.RentalAgreement;

/**
 * Fixed rule (FR21): newly observed damage is entered as a ';' separated list.
 * Every entry costs FEE_PER_DAMAGE_ITEM.
 */
public class DamageCharge implements ChargeRule {

    public static final double FEE_PER_DAMAGE_ITEM = 50.00;

    @Override
    public double calculate(RentalAgreement agreement) {
        String notes = agreement.getNewDamage();
        if (notes == null || notes.trim().isEmpty()) {
            return 0.0;
        }
        int items = 0;
        for (String entry : notes.split(";")) {
            if (!entry.trim().isEmpty()) {
                items++;
            }
        }
        return items * FEE_PER_DAMAGE_ITEM;
    }
}
