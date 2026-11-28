package com.rental.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.rental.enums.ReservationStatus;
import com.rental.enums.VehicleCategory;
import com.rental.enums.VehicleStatus;

/**
 * Diagram class: Vehicle (FR6-FR10, FR27).
 * Association "assigned to": a vehicle knows its reservations.
 * Association "undergoes": a vehicle knows its maintenance history (FR10).
 */
public class Vehicle {

    private final String registrationNumber;
    private String make;
    private String model;
    private int year;
    private VehicleCategory category;
    private String transmissionType;
    private String fuelType;
    private double currentMileage;
    private double dailyRentalRate;
    private VehicleStatus status;

    private final List<MaintenanceRecord> maintenanceHistory = new ArrayList<>();
    private final List<Reservation> reservations = new ArrayList<>();

    public Vehicle(String registrationNumber, String make, String model, int year,
                   VehicleCategory category, String transmissionType, String fuelType,
                   double currentMileage, double dailyRentalRate) {
        this.registrationNumber = registrationNumber;
        this.make = make;
        this.model = model;
        this.year = year;
        this.category = category;
        this.transmissionType = transmissionType;
        this.fuelType = fuelType;
        this.currentMileage = currentMileage;
        this.dailyRentalRate = dailyRentalRate;
        this.status = VehicleStatus.AVAILABLE;
    }

    /** FR8: change the vehicle status. */
    public void updateStatus(VehicleStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("Status is required");
        }
        this.status = status;
    }

    /** Odometer can only go up. */
    public void updateMileage(double mileage) {
        if (mileage < currentMileage) {
            throw new IllegalArgumentException(
                    "Mileage cannot be less than the vehicle's current mileage (" + currentMileage + ")");
        }
        this.currentMileage = mileage;
    }

    /**
     * FR12 / FR13 / FR27: a vehicle is available for a period when it is not under
     * maintenance, not inactive, and has no blocking reservation in that period.
     */
    public boolean isAvailableForPeriod(LocalDate startDate, LocalDate endDate) {
        if (status == VehicleStatus.UNDER_MAINTENANCE || status == VehicleStatus.INACTIVE) {
            return false;
        }
        return !hasOverlappingReservation(startDate, endDate);
    }

    /** FR13: true if a CONFIRMED (or still-out COLLECTED) reservation overlaps the period. */
    public boolean hasOverlappingReservation(LocalDate startDate, LocalDate endDate) {
        for (Reservation r : reservations) {
            if (r.isBlocking() && r.overlaps(startDate, endDate)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasOpenMaintenance() {
        for (MaintenanceRecord record : maintenanceHistory) {
            if (!MaintenanceRecord.COMPLETED.equals(record.getCompletionStatus())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Called when a reservation is cancelled/expired or a rental is returned:
     * the vehicle becomes RESERVED if another confirmed reservation exists,
     * otherwise AVAILABLE. An INACTIVE vehicle is never changed by this method.
     * (Callers decide when it is safe to call it, e.g. maintenance completed.)
     */
    public void releaseHold() {
        if (status == VehicleStatus.INACTIVE) {
            return;
        }
        boolean hasConfirmed = false;
        for (Reservation r : reservations) {
            if (r.getStatus() == ReservationStatus.CONFIRMED) {
                hasConfirmed = true;
                break;
            }
        }
        status = hasConfirmed ? VehicleStatus.RESERVED : VehicleStatus.AVAILABLE;
    }

    public void addMaintenanceRecord(MaintenanceRecord record) {
        if (record != null && !maintenanceHistory.contains(record)) {
            maintenanceHistory.add(record);
        }
    }

    public void addReservation(Reservation reservation) {
        if (reservation != null && !reservations.contains(reservation)) {
            reservations.add(reservation);
        }
    }

    public String getRegistrationNumber() { return registrationNumber; }
    public String getMake() { return make; }
    public String getModel() { return model; }
    public int getYear() { return year; }
    public VehicleCategory getCategory() { return category; }
    public String getTransmissionType() { return transmissionType; }
    public String getFuelType() { return fuelType; }
    public double getCurrentMileage() { return currentMileage; }
    public double getDailyRentalRate() { return dailyRentalRate; }
    public VehicleStatus getStatus() { return status; }

    public List<MaintenanceRecord> getMaintenanceHistory() {
        return Collections.unmodifiableList(maintenanceHistory);
    }

    public List<Reservation> getReservations() {
        return Collections.unmodifiableList(reservations);
    }

    @Override
    public String toString() {
        return registrationNumber + " - " + make + " " + model + " (" + year + ") [" + status + "]";
    }
}
