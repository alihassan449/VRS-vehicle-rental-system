package com.rental.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.rental.enums.VehicleStatus;
import com.rental.exception.BusinessRuleException;
import com.rental.exception.ValidationException;
import com.rental.model.MaintenanceRecord;
import com.rental.model.RentalAgreement;
import com.rental.model.Vehicle;
import com.rental.repository.MaintenanceRepository;
import com.rental.repository.RentalAgreementRepository;
import com.rental.repository.VehicleRepository;
import com.rental.util.IdGenerator;

/**
 * UC-11 Manage Maintenance Records.
 */
public class MaintenanceService {
    private final MaintenanceRepository maintenanceRepository;
    private final VehicleRepository vehicleRepository;
    private final RentalAgreementRepository agreementRepository;

    public MaintenanceService(MaintenanceRepository maintenanceRepository, VehicleRepository vehicleRepository,
                              RentalAgreementRepository agreementRepository) {
        this.maintenanceRepository = maintenanceRepository;
        this.vehicleRepository = vehicleRepository;
        this.agreementRepository = agreementRepository;
    }

    /**
     * UC-11 steps 2.1 - 3.2: validate and save the record. A vehicle with an open
     * record becomes Under Maintenance - unless it is currently Rented or Reserved,
     * in which case its status is not changed until the rental/reservation is done.
     */
    public MaintenanceRecord createRecord(Vehicle vehicle, String type, LocalDate date, String description,
                                          Double cost, boolean completed) {
        if (vehicle == null) {
            throw new ValidationException("Select a vehicle first.");
        }
        type = type == null ? "" : type.trim();
        description = description == null ? "" : description.trim();
        if (type.isEmpty()) {
            throw new ValidationException("Maintenance type is required.");
        }
        if (date == null) {
            throw new ValidationException("Maintenance date is required (yyyy-MM-dd).");
        }
        if (description.isEmpty()) {
            throw new ValidationException("Description is required.");
        }
        if (cost == null) {
            throw new ValidationException("Cost is required.");
        }
        if (cost < 0) {
            throw new ValidationException("Cost cannot be negative.");
        }
        MaintenanceRecord record = new MaintenanceRecord(IdGenerator.next("MNT"), vehicle, type, date,
                description, cost, completed);
        maintenanceRepository.save(record);
        vehicle.addMaintenanceRecord(record);
        if (!completed && (vehicle.getStatus() == VehicleStatus.AVAILABLE)) {
            vehicle.setStatus(VehicleStatus.UNDER_MAINTENANCE);
        }
        vehicleRepository.save(vehicle);
        return record;
    }

    /** UC-11 steps 4.1 - 4.2: mark the record Completed; the vehicle becomes Available. */
    public void completeRecord(MaintenanceRecord record) {
        if (record == null) {
            throw new ValidationException("Select a maintenance record first.");
        }
        if (record.isCompleted()) {
            throw new BusinessRuleException("This maintenance record is already completed.");
        }
        record.setCompleted(true);
        maintenanceRepository.save(record);
        Vehicle vehicle = record.getVehicle();
        if (vehicle.getStatus() == VehicleStatus.UNDER_MAINTENANCE && !vehicle.hasOpenMaintenance()) {
            vehicle.setStatus(VehicleStatus.AVAILABLE);
        }
        vehicleRepository.save(vehicle);
    }

    /**
     * UC-11 alt 1: vehicles flagged for maintenance by UC-09 (Under Maintenance but
     * no maintenance record opened yet).
     */
    public List<Vehicle> findFlaggedVehicles() {
        List<Vehicle> flagged = new ArrayList<>();
        for (Vehicle v : vehicleRepository.findAll()) {
            if (v.getStatus() == VehicleStatus.UNDER_MAINTENANCE && !v.hasOpenMaintenance()) {
                flagged.add(v);
            }
        }
        return flagged;
    }

    /** UC-11 alt 1.1: vehicle and return information used to pre-fill the maintenance form. */
    public String returnInfoFor(Vehicle vehicle) {
        RentalAgreement latest = null;
        for (RentalAgreement a : agreementRepository.findByVehicle(vehicle)) {
            if (a.isReturned() && (latest == null || a.getReturnDateTime().isAfter(latest.getReturnDateTime()))) {
                latest = a;
            }
        }
        if (latest == null) {
            return "";
        }
        return "Returned under " + latest.getAgreementId() + " on " + latest.getReturnDateTime().toLocalDate()
                + (latest.getNewDamageNotes().isEmpty() ? "" : ". New damage: " + latest.getNewDamageNotes());
    }

    public List<MaintenanceRecord> findAll() {
        return maintenanceRepository.findAll();
    }
}
