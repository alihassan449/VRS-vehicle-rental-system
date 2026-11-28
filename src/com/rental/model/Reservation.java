package com.rental.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import com.rental.enums.ReservationStatus;

/**
 * FR11-FR16: Reservation entity.
 */
public class Reservation {

    private String reservationId;
    private Customer customer;
    private Vehicle vehicle;
    private LocalDate pickupDate;
    private LocalDate returnDate;
    private ReservationStatus status;
    private double estimatedCharge;

    public Reservation() {
    }

    public Reservation(String reservationId, Customer customer, Vehicle vehicle,
                        LocalDate pickupDate, LocalDate returnDate) {
        this.reservationId = reservationId;
        this.customer = customer;
        this.vehicle = vehicle;
        this.pickupDate = pickupDate;
        this.returnDate = returnDate;
        this.status = ReservationStatus.PENDING;
        this.estimatedCharge = calculateEstimatedCharge();
    }

    /**
     * FR13: two reservations overlap if they share the same vehicle and
     * their [pickupDate, returnDate] ranges intersect. Only CONFIRMED
     * reservations should actually block each other; that filtering is
     * done by the caller (ReservationService), this method only checks dates.
     */
    public boolean overlapsWith(Reservation other) {
        if (other == null || other.vehicle == null || this.vehicle == null) {
            return false;
        }
        if (!this.vehicle.getRegistrationNumber().equals(other.vehicle.getRegistrationNumber())) {
            return false;
        }
        // Ranges [a1,a2] and [b1,b2] overlap unless one ends before the other starts.
        return !this.returnDate.isBefore(other.pickupDate) && !other.returnDate.isBefore(this.pickupDate);
    }

    /**
     * FR14: estimated charge = number of rental days * vehicle's daily rate.
     * A rental of the same pickup and return date counts as 1 day minimum.
     */
    public double calculateEstimatedCharge() {
        if (pickupDate == null || returnDate == null || vehicle == null) {
            return 0.0;
        }
        this.estimatedCharge = getRentalDays() * vehicle.getDailyRentalRate();
        return this.estimatedCharge;
    }

    /** Number of rental days (minimum 1) between pickup and return date. */
    public long getRentalDays() {
        if (pickupDate == null || returnDate == null) {
            return 0;
        }
        long days = ChronoUnit.DAYS.between(pickupDate, returnDate);
        return days < 1 ? 1 : days;
    }

    /** True if the two inclusive date ranges intersect. */
    public static boolean periodsOverlap(LocalDate start1, LocalDate end1, LocalDate start2, LocalDate end2) {
        return !end1.isBefore(start2) && !end2.isBefore(start1);
    }

    public String getReservationId() {
        return reservationId;
    }

    public void setReservationId(String reservationId) {
        this.reservationId = reservationId;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public void setVehicle(Vehicle vehicle) {
        this.vehicle = vehicle;
    }

    public LocalDate getPickupDate() {
        return pickupDate;
    }

    public void setPickupDate(LocalDate pickupDate) {
        this.pickupDate = pickupDate;
    }

    public LocalDate getReturnDate() {
        return returnDate;
    }

    public void setReturnDate(LocalDate returnDate) {
        this.returnDate = returnDate;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public void setStatus(ReservationStatus status) {
        this.status = status;
    }

    public double getEstimatedCharge() {
        return estimatedCharge;
    }

    public void setEstimatedCharge(double estimatedCharge) {
        this.estimatedCharge = estimatedCharge;
    }

    @Override
    public String toString() {
        return reservationId + " - " + (vehicle != null ? vehicle.getRegistrationNumber() : "?")
                + " [" + pickupDate + " to " + returnDate + "] " + status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Reservation)) return false;
        Reservation other = (Reservation) o;
        return reservationId != null && reservationId.equals(other.reservationId);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hashCode(reservationId);
    }
}
