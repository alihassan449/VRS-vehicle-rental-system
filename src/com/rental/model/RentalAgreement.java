package com.rental.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.rental.model.charge.ChargeRule;
import com.rental.model.charge.DamageCharge;
import com.rental.model.charge.ExcessMileageCharge;
import com.rental.model.charge.FuelShortageCharge;
import com.rental.model.charge.LateReturnCharge;

/**
 * Diagram class: RentalAgreement (FR17-FR23).
 * UML "DateTime" is java.time.LocalDateTime. Fuel levels are percentages (0-100).
 * Associations: generated from one Reservation, signed by the Customer, executed by Staff,
 * for one Vehicle ("rented in"), and it contains 1..* Payments.
 */
public class RentalAgreement {

    private final ChargeRule lateRule = new LateReturnCharge();
    private final ChargeRule mileageRule = new ExcessMileageCharge();
    private final ChargeRule fuelRule = new FuelShortageCharge();
    private final ChargeRule damageRule = new DamageCharge();

    private final String agreementID;
    private final Reservation reservation;

    // FR18: handover
    private LocalDateTime pickupDateTime;
    private double startingMileage;
    private double pickupFuelLevel;
    private String existingDamage;

    // FR20: return
    private LocalDateTime actualReturnDateTime;
    private double endingMileage;
    private double returnFuelLevel;
    private String newDamage;

    // FR21 / FR22
    private double lateReturnCharge;
    private double excessMileageCharge;
    private double fuelShortageCharge;
    private double damageCharge;
    private double totalAmount;

    private Staff executedBy;
    private final List<Payment> payments = new ArrayList<>();

    public RentalAgreement(String agreementID, Reservation reservation) {
        this.agreementID = agreementID;
        this.reservation = reservation;
    }

    /**
     * FR21 + FR22: calculate late-return, excess-mileage, fuel-shortage and damage
     * charges, then the final payable amount = estimated rental charge + extra charges.
     */
    public double calculateFinalCharges() {
        lateReturnCharge = lateRule.calculate(this);
        excessMileageCharge = mileageRule.calculate(this);
        fuelShortageCharge = fuelRule.calculate(this);
        damageCharge = damageRule.calculate(this);
        totalAmount = reservation.getEstimatedRentalCharge()
                + lateReturnCharge + excessMileageCharge + fuelShortageCharge + damageCharge;
        return totalAmount;
    }

    /**
     * FR22 + FR23: finish the rental after the return details were recorded.
     * Calculates the final amount, updates the vehicle mileage and releases the vehicle
     * (AVAILABLE, or RESERVED if another confirmed reservation exists).
     */
    public void completeReturn() {
        if (actualReturnDateTime == null) {
            throw new IllegalStateException("Return details have not been recorded");
        }
        calculateFinalCharges();
        Vehicle vehicle = getVehicle();
        vehicle.updateMileage(endingMileage);
        vehicle.releaseHold();
    }

    /**
     * Used only when an agreement is loaded from the database: puts back the charges that were
     * calculated at return time (so old invoices never change if the charge rules change later).
     */
    public void restoreCharges(double late, double excessMileage, double fuelShortage,
                               double damage, double total) {
        this.lateReturnCharge = late;
        this.excessMileageCharge = excessMileage;
        this.fuelShortageCharge = fuelShortage;
        this.damageCharge = damage;
        this.totalAmount = total;
    }

    /** FR25: printable rental invoice. */
    public String generateInvoice() {
        StringBuilder sb = new StringBuilder();
        sb.append("================= RENTAL INVOICE =================\n");
        sb.append(String.format("Agreement No    : %s%n", agreementID));
        sb.append(String.format("Reservation     : %s%n", reservation.getReservationID()));
        sb.append(String.format("Customer        : %s (%s)%n", getCustomer().getName(), getCustomer().getCustomerID()));
        sb.append(String.format("Vehicle         : %s %s [%s]%n", getVehicle().getMake(),
                getVehicle().getModel(), getVehicle().getRegistrationNumber()));
        sb.append(String.format("Planned period  : %s to %s (%d day(s))%n", reservation.getPickupDate(),
                reservation.getReturnDate(), reservation.getRentalDays()));
        sb.append(String.format("Picked up       : %s%n", pickupDateTime == null ? "-" : pickupDateTime));
        sb.append(String.format("Returned        : %s%n", actualReturnDateTime == null ? "-" : actualReturnDateTime));
        sb.append(String.format("Mileage         : %.1f -> %.1f%n", startingMileage, endingMileage));
        sb.append(String.format("Fuel level (%%)  : %.0f -> %.0f%n", pickupFuelLevel, returnFuelLevel));
        sb.append("--------------------------------------------------\n");
        sb.append(String.format("Base rental charge   : %10.2f%n", reservation.getEstimatedRentalCharge()));
        sb.append(String.format("Late return charge   : %10.2f%n", lateReturnCharge));
        sb.append(String.format("Excess mileage charge: %10.2f%n", excessMileageCharge));
        sb.append(String.format("Fuel shortage charge : %10.2f%n", fuelShortageCharge));
        sb.append(String.format("Damage charge        : %10.2f%n", damageCharge));
        sb.append("--------------------------------------------------\n");
        sb.append(String.format("TOTAL PAYABLE        : %10.2f%n", totalAmount));
        sb.append(String.format("Paid so far          : %10.2f%n", totalPaid()));
        sb.append(String.format("BALANCE DUE          : %10.2f%n", balanceDue()));
        sb.append("==================================================\n");
        return sb.toString();
    }

    public void addPayment(Payment payment) {
        if (payment != null) {
            payments.add(payment);
        }
    }

    public double totalPaid() {
        double sum = 0.0;
        for (Payment p : payments) {
            sum += p.getAmountPaid();
        }
        return sum;
    }

    public double balanceDue() {
        return totalAmount - totalPaid();
    }

    public boolean isReturned() {
        return actualReturnDateTime != null;
    }

    // Associations derived from the reservation (no duplicated data)
    public Customer getCustomer() { return reservation.getCustomer(); }
    public Vehicle getVehicle() { return reservation.getVehicle(); }

    public String getAgreementID() { return agreementID; }
    public Reservation getReservation() { return reservation; }
    public LocalDateTime getPickupDateTime() { return pickupDateTime; }
    public void setPickupDateTime(LocalDateTime pickupDateTime) { this.pickupDateTime = pickupDateTime; }
    public double getStartingMileage() { return startingMileage; }
    public void setStartingMileage(double startingMileage) { this.startingMileage = startingMileage; }
    public double getPickupFuelLevel() { return pickupFuelLevel; }
    public void setPickupFuelLevel(double pickupFuelLevel) { this.pickupFuelLevel = pickupFuelLevel; }
    public String getExistingDamage() { return existingDamage; }
    public void setExistingDamage(String existingDamage) { this.existingDamage = existingDamage; }
    public LocalDateTime getActualReturnDateTime() { return actualReturnDateTime; }
    public void setActualReturnDateTime(LocalDateTime actualReturnDateTime) { this.actualReturnDateTime = actualReturnDateTime; }
    public double getEndingMileage() { return endingMileage; }
    public void setEndingMileage(double endingMileage) { this.endingMileage = endingMileage; }
    public double getReturnFuelLevel() { return returnFuelLevel; }
    public void setReturnFuelLevel(double returnFuelLevel) { this.returnFuelLevel = returnFuelLevel; }
    public String getNewDamage() { return newDamage; }
    public void setNewDamage(String newDamage) { this.newDamage = newDamage; }
    public double getLateReturnCharge() { return lateReturnCharge; }
    public double getExcessMileageCharge() { return excessMileageCharge; }
    public double getFuelShortageCharge() { return fuelShortageCharge; }
    public double getDamageCharge() { return damageCharge; }
    public double getTotalAmount() { return totalAmount; }
    public Staff getExecutedBy() { return executedBy; }
    public void setExecutedBy(Staff executedBy) { this.executedBy = executedBy; }

    public List<Payment> getPayments() {
        return Collections.unmodifiableList(payments);
    }

    @Override
    public String toString() {
        return agreementID + " - " + getVehicle().getRegistrationNumber()
                + " [" + (isReturned() ? "Returned" : "Active") + "]";
    }
}
