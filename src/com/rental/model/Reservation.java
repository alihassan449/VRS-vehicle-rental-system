package com.rental.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import com.rental.enums.ReservationStatus;
import com.rental.enums.VehicleStatus;

/**
 * Diagram class: Reservation (FR11-FR16).
 * Associations: Customer "places" it, Vehicle "assigned to" it, Staff "processes" it,
 * and it "generates" one RentalAgreement.
 */
public class Reservation {

    private final String reservationID;
    private final Customer customer;
    private final Vehicle vehicle;
    private final LocalDate pickupDate;
    private final LocalDate returnDate;
    private double estimatedRentalCharge;
    private ReservationStatus status;

    private Staff processedBy;
    private RentalAgreement rentalAgreement;

    public Reservation(String reservationID, Customer customer, Vehicle vehicle,
                       LocalDate pickupDate, LocalDate returnDate) {
        this.reservationID = reservationID;
        this.customer = customer;
        this.vehicle = vehicle;
        this.pickupDate = pickupDate;
        this.returnDate = returnDate;
        this.status = ReservationStatus.PENDING;
        calculateEstimatedCharge();
    }

    /** Number of rental days (same-day rental counts as 1 day). */
    public long getRentalDays() {
        long days = ChronoUnit.DAYS.between(pickupDate, returnDate);
        return days < 1 ? 1 : days;
    }

    /** FR14: estimated charge = rental days x daily rate. */
    public double calculateEstimatedCharge() {
        estimatedRentalCharge = getRentalDays() * vehicle.getDailyRentalRate();
        return estimatedRentalCharge;
    }

    /** FR13 helper: do the date ranges intersect (end dates inclusive)? */
    public boolean overlaps(LocalDate startDate, LocalDate endDate) {
        return !returnDate.isBefore(startDate) && !endDate.isBefore(pickupDate);
    }

    /** A reservation blocks the vehicle while CONFIRMED, or COLLECTED and not yet returned. */
    public boolean isBlocking() {
        if (status == ReservationStatus.CONFIRMED) {
            return true;
        }
        return status == ReservationStatus.COLLECTED
                && (rentalAgreement == null || rentalAgreement.getActualReturnDateTime() == null);
    }

    /** FR15: PENDING -> CONFIRMED. The vehicle is marked RESERVED if it was AVAILABLE. */
    public void confirmReservation() {
        if (status != ReservationStatus.PENDING) {
            throw new IllegalStateException("Only a PENDING reservation can be confirmed (status: " + status + ")");
        }
        status = ReservationStatus.CONFIRMED;
        if (vehicle.getStatus() == VehicleStatus.AVAILABLE) {
            vehicle.updateStatus(VehicleStatus.RESERVED);
        }
    }

    /** FR16: a reservation can be cancelled any time before the vehicle is collected. */
    public void cancelReservation() {
        if (status == ReservationStatus.COLLECTED) {
            throw new IllegalStateException("Cannot cancel: the vehicle has already been collected");
        }
        if (status == ReservationStatus.CANCELLED || status == ReservationStatus.EXPIRED) {
            throw new IllegalStateException("Reservation is already " + status);
        }
        status = ReservationStatus.CANCELLED;
        releaseVehicle();
    }

    /** FR15: PENDING/CONFIRMED -> EXPIRED (customer never collected the vehicle). */
    public void markExpired() {
        if (status != ReservationStatus.PENDING && status != ReservationStatus.CONFIRMED) {
            throw new IllegalStateException("Only PENDING or CONFIRMED reservations can expire (status: " + status + ")");
        }
        status = ReservationStatus.EXPIRED;
        releaseVehicle();
    }

    private void releaseVehicle() {
        if (vehicle.getStatus() == VehicleStatus.RESERVED) {
            vehicle.releaseHold();
        }
    }

    public String getReservationID() { return reservationID; }
    public Customer getCustomer() { return customer; }
    public Vehicle getVehicle() { return vehicle; }
    public LocalDate getPickupDate() { return pickupDate; }
    public LocalDate getReturnDate() { return returnDate; }
    public double getEstimatedRentalCharge() { return estimatedRentalCharge; }
    public ReservationStatus getStatus() { return status; }
    public void setStatus(ReservationStatus status) { this.status = status; }
    public Staff getProcessedBy() { return processedBy; }
    public void setProcessedBy(Staff processedBy) { this.processedBy = processedBy; }
    public RentalAgreement getRentalAgreement() { return rentalAgreement; }
    public void setRentalAgreement(RentalAgreement rentalAgreement) { this.rentalAgreement = rentalAgreement; }

    @Override
    public String toString() {
        return reservationID + " - " + vehicle.getRegistrationNumber() + " ["
                + pickupDate + " to " + returnDate + "] " + status;
    }
}
