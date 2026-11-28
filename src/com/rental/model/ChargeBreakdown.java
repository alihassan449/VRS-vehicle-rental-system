package com.rental.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** UC-16: base rental charge, additional fees and the final payable amount. */
public class ChargeBreakdown {
    private final double baseCharge;
    private final List<Charge> additionalCharges;

    public ChargeBreakdown(double baseCharge, List<Charge> additionalCharges) {
        this.baseCharge = baseCharge;
        this.additionalCharges = new ArrayList<>(additionalCharges);
    }

    public double getBaseCharge() { return baseCharge; }

    public List<Charge> getAdditionalCharges() {
        return Collections.unmodifiableList(additionalCharges);
    }

    public double getFinalAmount() {
        double sum = baseCharge;
        for (Charge c : additionalCharges) {
            sum += c.getAmount();
        }
        return Math.round(sum * 100.0) / 100.0;
    }
}
