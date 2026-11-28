package com.rental.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.rental.enums.AgreementStatus;
import com.rental.enums.FuelLevel;

/**
 * UC-08 / UC-09: rental agreement created at handover from a confirmed
 * reservation and completed at return.
 */
public class RentalAgreement {
    private String agreementId;
    private Reservation reservation;
    /** null while the agreement is only a draft (before 'Authorize Agreement Handover'). */
    private AgreementStatus status;

    // UC-08: handover details
    private LocalDateTime pickupDateTime;
    private double startMileage;
    private FuelLevel startFuelLevel;
    private String existingDamageNotes = "";

    // UC-09: return details
    private LocalDateTime returnDateTime;
    private double endMileage;
    private FuelLevel endFuelLevel;
    private String newDamageNotes = "";

    // UC-16: charges stored against the agreement
    private double baseCharge;
    private final List<Charge> charges = new ArrayList<>();
    private double finalAmount;

    private final List<Payment> payments = new ArrayList<>();

    public RentalAgreement() {
    }

    public RentalAgreement(String agreementId, Reservation reservation) {
        this.agreementId = agreementId;
        this.reservation = reservation;
    }

    /** UC-08: record the handover values; the agreement becomes Active. */
    public void recordHandover(LocalDateTime pickupDateTime, double startMileage,
                               FuelLevel startFuelLevel, String existingDamageNotes) {
        this.pickupDateTime = pickupDateTime;
        this.startMileage = startMileage;
        this.startFuelLevel = startFuelLevel;
        this.existingDamageNotes = existingDamageNotes == null ? "" : existingDamageNotes;
        this.status = AgreementStatus.ACTIVE;
    }

    /** UC-09: record the return values and store the calculated charges (UC-16). */
    public void recordReturn(ReturnDetails details, ChargeBreakdown breakdown) {
        this.returnDateTime = details.getReturnDateTime();
        this.endMileage = details.getEndMileage();
        this.endFuelLevel = details.getEndFuelLevel();
        this.newDamageNotes = details.getNewDamageNotes();
        this.baseCharge = breakdown.getBaseCharge();
        this.charges.clear();
        this.charges.addAll(breakdown.getAdditionalCharges());
        this.finalAmount = breakdown.getFinalAmount();
        this.status = AgreementStatus.RETURNED;
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
        return Math.round(sum * 100.0) / 100.0;
    }

    /** UC-10: outstanding balance = final payable amount - payments made so far. */
    public double balanceDue() {
        return Math.round((finalAmount - totalPaid()) * 100.0) / 100.0;
    }

    public String getAgreementId() { return agreementId; }
    public void setAgreementId(String agreementId) { this.agreementId = agreementId; }
    public Reservation getReservation() { return reservation; }
    public void setReservation(Reservation reservation) { this.reservation = reservation; }
    public AgreementStatus getStatus() { return status; }
    public boolean isActive() { return status == AgreementStatus.ACTIVE; }
    public boolean isReturned() { return status == AgreementStatus.RETURNED; }
    public LocalDateTime getPickupDateTime() { return pickupDateTime; }
    public double getStartMileage() { return startMileage; }
    public FuelLevel getStartFuelLevel() { return startFuelLevel; }
    public String getExistingDamageNotes() { return existingDamageNotes; }
    public LocalDateTime getReturnDateTime() { return returnDateTime; }
    public double getEndMileage() { return endMileage; }
    public FuelLevel getEndFuelLevel() { return endFuelLevel; }
    public String getNewDamageNotes() { return newDamageNotes; }
    public double getBaseCharge() { return baseCharge; }
    public List<Charge> getCharges() { return charges; }
    public double getFinalAmount() { return finalAmount; }
    public List<Payment> getPayments() { return payments; }

    @Override
    public String toString() {
        String reg = reservation != null && reservation.getVehicle() != null
                ? reservation.getVehicle().getRegistrationNumber() : "?";
        return agreementId + " - " + reg + " [" + status + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RentalAgreement)) return false;
        RentalAgreement other = (RentalAgreement) o;
        return agreementId != null && agreementId.equals(other.agreementId);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hashCode(agreementId);
    }
}
