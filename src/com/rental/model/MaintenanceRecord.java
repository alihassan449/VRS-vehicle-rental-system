package com.rental.model;

import java.time.LocalDate;

import com.rental.enums.VehicleStatus;

/**
 * Diagram class: MaintenanceRecord (FR10, FR26, FR27).
 * Associations: Vehicle "undergoes" it, Staff (fleet) "logs" it.
 * completionStatus is a String: PENDING, IN_PROGRESS or COMPLETED.
 */
public class MaintenanceRecord {

    public static final String PENDING = "PENDING";
    public static final String IN_PROGRESS = "IN_PROGRESS";
    public static final String COMPLETED = "COMPLETED";
    public static final String[] STATUSES = {PENDING, IN_PROGRESS, COMPLETED};

    private final String recordID;
    private final Vehicle vehicle;
    private final String maintenanceType;
    private final LocalDate date;
    private final String description;
    private final double cost;
    private String completionStatus;
    private Staff loggedBy;

    public MaintenanceRecord(String recordID, Vehicle vehicle, String maintenanceType,
                             LocalDate date, String description, double cost) {
        this.recordID = recordID;
        this.vehicle = vehicle;
        this.maintenanceType = maintenanceType;
        this.date = date;
        this.description = description;
        this.cost = cost;
        this.completionStatus = PENDING;
    }

    /**
     * FR26 / FR27: change the completion status and keep the vehicle status in step.
     * Open maintenance (PENDING / IN_PROGRESS) -> vehicle becomes UNDER_MAINTENANCE.
     * COMPLETED (and nothing else open) -> vehicle is released back to the fleet.
     */
    public void updateStatus(String status) {
        boolean valid = false;
        for (String s : STATUSES) {
            if (s.equals(status)) {
                valid = true;
            }
        }
        if (!valid) {
            throw new IllegalArgumentException("Status must be PENDING, IN_PROGRESS or COMPLETED");
        }
        boolean completing = COMPLETED.equals(status);
        if (!completing && vehicle.getStatus() == VehicleStatus.RENTED) {
            throw new IllegalStateException(
                    "Vehicle is currently rented - maintenance cannot be opened until it is returned");
        }

        this.completionStatus = status;
        if (completing) {
            if (vehicle.getStatus() == VehicleStatus.UNDER_MAINTENANCE && !vehicle.hasOpenMaintenance()) {
                vehicle.releaseHold();
            }
        } else if (vehicle.getStatus() != VehicleStatus.INACTIVE) {
            vehicle.updateStatus(VehicleStatus.UNDER_MAINTENANCE);
        }
    }

    /**
     * Used only when a record is loaded from the database: sets the stored status
     * WITHOUT changing the vehicle status (updateStatus would do that).
     */
    public void restoreCompletionStatus(String status) {
        this.completionStatus = status;
    }

    public String getRecordID() { return recordID; }
    public Vehicle getVehicle() { return vehicle; }
    public String getMaintenanceType() { return maintenanceType; }
    public LocalDate getDate() { return date; }
    public String getDescription() { return description; }
    public double getCost() { return cost; }
    public String getCompletionStatus() { return completionStatus; }
    public Staff getLoggedBy() { return loggedBy; }
    public void setLoggedBy(Staff loggedBy) { this.loggedBy = loggedBy; }

    @Override
    public String toString() {
        return recordID + " - " + maintenanceType + " (" + date + ") " + completionStatus;
    }
}
