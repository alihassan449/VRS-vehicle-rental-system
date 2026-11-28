package com.rental.model;

import java.time.LocalDateTime;

import com.rental.enums.FuelLevel;

/** UC-09: the return values entered by Rental Staff. */
public class ReturnDetails {
    private final LocalDateTime returnDateTime;
    private final double endMileage;
    private final FuelLevel endFuelLevel;
    private final String newDamageNotes;

    public ReturnDetails(LocalDateTime returnDateTime, double endMileage, FuelLevel endFuelLevel,
                         String newDamageNotes) {
        this.returnDateTime = returnDateTime;
        this.endMileage = endMileage;
        this.endFuelLevel = endFuelLevel;
        this.newDamageNotes = newDamageNotes == null ? "" : newDamageNotes.trim();
    }

    public LocalDateTime getReturnDateTime() { return returnDateTime; }
    public double getEndMileage() { return endMileage; }
    public FuelLevel getEndFuelLevel() { return endFuelLevel; }
    public String getNewDamageNotes() { return newDamageNotes; }

    /** True when newly observed damage was recorded (triggers the fixed damage fee and maintenance). */
    public boolean hasNewDamage() {
        return !newDamageNotes.trim().isEmpty();
    }
}
