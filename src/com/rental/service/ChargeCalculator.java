package com.rental.service;

import java.util.ArrayList;
import java.util.List;

import com.rental.exception.BusinessRuleException;
import com.rental.model.Charge;
import com.rental.model.ChargeBreakdown;
import com.rental.model.RentalAgreement;
import com.rental.model.Reservation;
import com.rental.model.ReturnDetails;
import com.rental.service.charge.ChargeRule;
import com.rental.service.charge.DamageCharge;
import com.rental.service.charge.ExcessMileageCharge;
import com.rental.service.charge.FuelShortageCharge;
import com.rental.service.charge.LateReturnCharge;

/**
 * UC-16 Calculate Charges &amp; Fees (included by UC-09).
 */
public class ChargeCalculator {
    private final List<ChargeRule> rules = java.util.Arrays.asList(
            new LateReturnCharge(),
            new ExcessMileageCharge(),
            new FuelShortageCharge(),
            new DamageCharge());

    /**
     * 1.1 base rental charge (rental days x daily rate); 1.2 late-return,
     * excess-mileage and fuel-shortage charges; 1.3 fixed damage fee if new
     * damage was recorded; 1.4 final payable amount.
     */
    public ChargeBreakdown calculate(RentalAgreement agreement, ReturnDetails details) {
        if (agreement == null || agreement.getReservation() == null || details == null
                || details.getReturnDateTime() == null || details.getEndFuelLevel() == null
                || agreement.getStartFuelLevel() == null) {
            throw new BusinessRuleException("Invalid Input: the return data is incomplete or inconsistent, "
                    + "so the charges were not calculated.");
        }
        Reservation reservation = agreement.getReservation();
        double base = reservation.getRentalDays() * reservation.getVehicle().getDailyRentalRate();
        List<Charge> extras = new ArrayList<>();
        for (ChargeRule rule : rules) {
            Charge c = rule.apply(agreement, details);
            if (c.getAmount() > 0) {
                extras.add(c);
            }
        }
        return new ChargeBreakdown(base, extras);
    }
}
