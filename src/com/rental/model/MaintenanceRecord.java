package com.rental.model;

import java.time.LocalDate;

/**
 * FR26: maintenance record for a vehicle.
 */
public class MaintenanceRecord {

    private String recordId;
    private Vehicle vehicle;
    private String maintenanceType;
    private LocalDate date;
    private String description;
    private double cost;
    private boolean completed;

    public MaintenanceRecord() {
    }

    public MaintenanceRecord(String recordId, Vehicle vehicle, String maintenanceType,
                              LocalDate date, String description, double cost, boolean completed) {
        this.recordId = recordId;
        this.vehicle = vehicle;
        this.maintenanceType = maintenanceType;
        this.date = date;
        this.description = description;
        this.cost = cost;
        this.completed = completed;
    }

    public String getRecordId() {
        return recordId;
    }

    public void setRecordId(String recordId) {
        this.recordId = recordId;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public void setVehicle(Vehicle vehicle) {
        this.vehicle = vehicle;
    }

    public String getMaintenanceType() {
        return maintenanceType;
    }

    public void setMaintenanceType(String maintenanceType) {
        this.maintenanceType = maintenanceType;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getCost() {
        return cost;
    }

    public void setCost(double cost) {
        this.cost = cost;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    /** UC-11 completion status: "In Progress" or "Completed". */
    public String getCompletionStatus() {
        return completed ? "Completed" : "In Progress";
    }

    @Override
    public String toString() {
        return recordId + " - " + maintenanceType + " (" + date + ") " + getCompletionStatus();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MaintenanceRecord)) return false;
        MaintenanceRecord other = (MaintenanceRecord) o;
        return recordId != null && recordId.equals(other.recordId);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hashCode(recordId);
    }
}
