package com.rental.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.rental.enums.ReservationStatus;
import com.rental.enums.StaffRole;
import com.rental.enums.VehicleStatus;
import com.rental.util.IdGenerator;

/**
 * Diagram class: Staff (generalisation of RentalStaff and FleetStaff).
 *
 * The diagram shows processReservation(), processHandover() and processReturn() without
 * parameters (abbreviated); in code they take the objects they work on. Every method first
 * checks that the staff member is authorised (RentalStaff only), so a FleetStaff object
 * can never process reservations, handovers or returns.
 */
public abstract class Staff {

    private final String staffID;
    private final String name;
    private final StaffRole role;

    protected Staff(String staffID, String name, StaffRole role) {
        this.staffID = staffID;
        this.name = name;
        this.role = role;
    }

    /** True for staff who serve customers: customers, reservations, rentals, payments. */
    public abstract boolean canHandleRentals();

    /** True for staff who manage the fleet: vehicles and maintenance. */
    public abstract boolean canManageFleet();

    public void requireRentalAccess() {
        if (!canHandleRentals()) {
            throw new SecurityException(getDisplayRole() + " is not authorized for rental operations "
                    + "(customers, reservations, rentals, payments)");
        }
    }

    public void requireFleetAccess() {
        if (!canManageFleet()) {
            throw new SecurityException(getDisplayRole() + " is not authorized for fleet operations "
                    + "(vehicles, maintenance)");
        }
    }

    /**
     * FR13 / FR15 / FR27: confirm a PENDING reservation. Refused when the vehicle is under
     * maintenance / inactive or already has an overlapping confirmed reservation.
     */
    public void processReservation(Reservation reservation) {
        requireRentalAccess();
        if (reservation == null) {
            throw new IllegalArgumentException("Reservation is required");
        }
        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new IllegalStateException("Only a PENDING reservation can be confirmed (status: "
                    + reservation.getStatus() + ")");
        }
        Vehicle vehicle = reservation.getVehicle();
        if (!vehicle.isAvailableForPeriod(reservation.getPickupDate(), reservation.getReturnDate())) {
            throw new IllegalStateException("Vehicle " + vehicle.getRegistrationNumber()
                    + " is not available for this period (overlapping confirmed reservation, "
                    + "under maintenance or inactive)");
        }
        reservation.confirmReservation();
        reservation.setProcessedBy(this);
    }

    /**
     * FR5, FR17, FR18, FR19, FR27: create the rental agreement from a CONFIRMED reservation,
     * record the handover details and mark the vehicle RENTED.
     * The new agreement is attached to the reservation ("generates" association).
     */
    public void processHandover(Reservation reservation, LocalDateTime pickupDateTime,
                                double startingMileage, double pickupFuelLevel, String existingDamage) {
        requireRentalAccess();
        if (reservation == null) {
            throw new IllegalArgumentException("Reservation is required");
        }
        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new IllegalStateException("A rental agreement can only be created from a CONFIRMED "
                    + "reservation (status: " + reservation.getStatus() + ")");
        }
        if (!reservation.getCustomer().isLicenseValid()) {
            throw new IllegalStateException("Customer's driving license is expired - "
                    + "the vehicle cannot be handed over");
        }
        Vehicle vehicle = reservation.getVehicle();
        VehicleStatus vs = vehicle.getStatus();
        if (vs == VehicleStatus.UNDER_MAINTENANCE || vs == VehicleStatus.INACTIVE || vs == VehicleStatus.RENTED) {
            throw new IllegalStateException("Vehicle cannot be handed over (status: " + vs + ")");
        }
        validateFuel(pickupFuelLevel);

        vehicle.updateMileage(startingMileage); // throws if lower than current odometer

        RentalAgreement agreement = new RentalAgreement(IdGenerator.next("AGR"), reservation);
        agreement.setPickupDateTime(pickupDateTime);
        agreement.setStartingMileage(startingMileage);
        agreement.setPickupFuelLevel(pickupFuelLevel);
        agreement.setExistingDamage(existingDamage);
        agreement.setExecutedBy(this);

        reservation.setRentalAgreement(agreement);
        reservation.setStatus(ReservationStatus.COLLECTED);
        vehicle.updateStatus(VehicleStatus.RENTED);
    }

    /**
     * FR20, FR21, FR22, FR23: record the return details, calculate all charges and the
     * final amount, and release the vehicle (or send it to maintenance if required).
     */
    public void processReturn(RentalAgreement agreement, LocalDateTime actualReturnDateTime,
                              double endingMileage, double returnFuelLevel, String newDamage,
                              boolean maintenanceRequired) {
        requireRentalAccess();
        if (agreement == null) {
            throw new IllegalArgumentException("Rental agreement is required");
        }
        if (agreement.isReturned()) {
            throw new IllegalStateException("This vehicle has already been returned");
        }
        if (endingMileage < agreement.getStartingMileage()) {
            throw new IllegalArgumentException("Ending mileage cannot be less than the starting mileage ("
                    + agreement.getStartingMileage() + ")");
        }
        if (actualReturnDateTime.isBefore(agreement.getPickupDateTime())) {
            throw new IllegalArgumentException("Return date/time cannot be before the pickup date/time");
        }
        validateFuel(returnFuelLevel);

        agreement.setActualReturnDateTime(actualReturnDateTime);
        agreement.setEndingMileage(endingMileage);
        agreement.setReturnFuelLevel(returnFuelLevel);
        agreement.setNewDamage(newDamage);
        agreement.completeReturn();

        if (maintenanceRequired) {
            agreement.getVehicle().updateStatus(VehicleStatus.UNDER_MAINTENANCE);
        }
    }

    // ------------------------------------------------------------------ payments (rental access)

    /** FR24: record a payment against a rental agreement. */
    public Payment recordPayment(RentalAgreement agreement, double amount, String paymentMethod) {
        requireRentalAccess();
        if (agreement == null) {
            throw new IllegalArgumentException("Rental agreement is required");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than zero");
        }
        if (paymentMethod == null || paymentMethod.trim().isEmpty()) {
            throw new IllegalArgumentException("Payment method is required");
        }
        if (agreement.isReturned() && amount > agreement.balanceDue() + 0.005) {
            throw new IllegalArgumentException(String.format(
                    "Amount exceeds the balance due (%.2f)", agreement.balanceDue()));
        }
        Payment payment = new Payment(IdGenerator.next("PAY"), agreement, amount, LocalDate.now(), paymentMethod);
        agreement.addPayment(payment);
        return payment;
    }

    // ------------------------------------------------------------------ maintenance (fleet access)

    /**
     * FR26: log a maintenance record ("Staff logs MaintenanceRecord").
     * Unless the record is already COMPLETED the vehicle becomes UNDER_MAINTENANCE (FR27).
     */
    public MaintenanceRecord logMaintenance(Vehicle vehicle, String maintenanceType, String description,
                                            double cost, String completionStatus) {
        requireFleetAccess();
        if (vehicle == null) {
            throw new IllegalArgumentException("Vehicle is required");
        }
        if (maintenanceType == null || maintenanceType.trim().isEmpty()) {
            throw new IllegalArgumentException("Maintenance type is required");
        }
        if (cost < 0) {
            throw new IllegalArgumentException("Cost cannot be negative");
        }
        if (!MaintenanceRecord.COMPLETED.equals(completionStatus) && vehicle.getStatus() == VehicleStatus.RENTED) {
            throw new IllegalStateException("Vehicle is currently rented - it cannot be sent to maintenance now");
        }
        MaintenanceRecord record = new MaintenanceRecord(IdGenerator.next("MNT"), vehicle,
                maintenanceType.trim(), LocalDate.now(), description, cost);
        record.setLoggedBy(this);
        vehicle.addMaintenanceRecord(record);
        record.updateStatus(completionStatus);
        return record;
    }

    /** FR26: update the completion status of an existing maintenance record. */
    public void updateMaintenanceStatus(MaintenanceRecord record, String completionStatus) {
        requireFleetAccess();
        if (record == null) {
            throw new IllegalArgumentException("Maintenance record is required");
        }
        record.updateStatus(completionStatus);
    }

    /** FR8: take a vehicle out of service (INACTIVE) or put it back (AVAILABLE). */
    public void setVehicleInactive(Vehicle vehicle, boolean inactive) {
        requireFleetAccess();
        if (vehicle == null) {
            throw new IllegalArgumentException("Vehicle is required");
        }
        if (inactive) {
            if (vehicle.getStatus() != VehicleStatus.AVAILABLE) {
                throw new IllegalStateException("Only an AVAILABLE vehicle can be made INACTIVE (status: "
                        + vehicle.getStatus() + ")");
            }
            vehicle.updateStatus(VehicleStatus.INACTIVE);
        } else {
            if (vehicle.getStatus() != VehicleStatus.INACTIVE) {
                throw new IllegalStateException("Vehicle is not INACTIVE");
            }
            vehicle.updateStatus(VehicleStatus.AVAILABLE);
            if (vehicle.hasOpenMaintenance()) {
                vehicle.updateStatus(VehicleStatus.UNDER_MAINTENANCE);
            } else {
                vehicle.releaseHold();
            }
        }
    }

    private void validateFuel(double fuelLevel) {
        if (fuelLevel < 0 || fuelLevel > 100) {
            throw new IllegalArgumentException("Fuel level must be between 0 and 100 (percent)");
        }
    }

    public String getStaffID() { return staffID; }
    public String getName() { return name; }
    public StaffRole getRole() { return role; }

    public String getDisplayRole() {
        if (role == StaffRole.RENTAL_STAFF) {
            return "Rental Staff";
        }
        if (role == StaffRole.FLEET_STAFF) {
            return "Fleet Staff";
        }
        return "Admin";
    }

    @Override
    public String toString() {
        return staffID + " - " + name + " (" + getDisplayRole() + ")";
    }
}
