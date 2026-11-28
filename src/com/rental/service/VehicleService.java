package com.rental.service;

import java.util.ArrayList;
import java.util.List;

import com.rental.enums.VehicleCategory;
import com.rental.enums.VehicleStatus;
import com.rental.model.MaintenanceRecord;
import com.rental.model.Vehicle;
import com.rental.repository.VehicleRepository;

/**
 * Diagram class: VehicleService (FR6-FR10).
 * registerVehicle(...), searchVehicles(regNo, category, status) and
 * getMaintenanceHistory(registrationNumber) are the diagram methods.
 */
public class VehicleService {

    private final VehicleRepository vehicleRepository;

    public VehicleService(VehicleRepository vehicleRepository) {
        this.vehicleRepository = vehicleRepository;
    }

    /** FR6 + FR7: register a vehicle in one of the fixed categories. */
    public Vehicle registerVehicle(String registrationNumber, String make, String model, int year,
                                   VehicleCategory category, String transmissionType, String fuelType,
                                   double currentMileage, double dailyRentalRate) {
        if (registrationNumber == null || registrationNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Registration number is required");
        }
        if (make == null || make.trim().isEmpty() || model == null || model.trim().isEmpty()) {
            throw new IllegalArgumentException("Make and model are required");
        }
        if (category == null) {
            throw new IllegalArgumentException("Category is required");
        }
        if (year < 1900 || year > 2100) {
            throw new IllegalArgumentException("Please enter a valid model year");
        }
        if (currentMileage < 0) {
            throw new IllegalArgumentException("Mileage cannot be negative");
        }
        if (dailyRentalRate <= 0) {
            throw new IllegalArgumentException("Daily rental rate must be greater than zero");
        }
        if (vehicleRepository.findByRegistrationNumber(registrationNumber).isPresent()) {
            throw new IllegalArgumentException("Vehicle " + registrationNumber.trim() + " is already registered");
        }
        Vehicle vehicle = new Vehicle(registrationNumber.trim(), make.trim(), model.trim(), year, category,
                transmissionType, fuelType, currentMileage, dailyRentalRate);
        vehicleRepository.save(vehicle);
        return vehicle;
    }

    /**
     * FR9: search by registration number (partial match), category and/or status.
     * A null / blank argument means "any".
     */
    public List<Vehicle> searchVehicles(String regNo, VehicleCategory category, VehicleStatus status) {
        List<Vehicle> result = new ArrayList<>();
        for (Vehicle v : vehicleRepository.findAll()) {
            boolean regOk = regNo == null || regNo.trim().isEmpty()
                    || v.getRegistrationNumber().toLowerCase().contains(regNo.trim().toLowerCase());
            boolean categoryOk = category == null || v.getCategory() == category;
            boolean statusOk = status == null || v.getStatus() == status;
            if (regOk && categoryOk && statusOk) {
                result.add(v);
            }
        }
        return result;
    }

    /** FR10: service and maintenance history of one vehicle. */
    public List<MaintenanceRecord> getMaintenanceHistory(String registrationNumber) {
        return new ArrayList<>(findByRegistration(registrationNumber).getMaintenanceHistory());
    }

    public Vehicle findByRegistration(String registrationNumber) {
        if (registrationNumber == null) {
            throw new IllegalArgumentException("Registration number is required");
        }
        return vehicleRepository.findByRegistrationNumber(registrationNumber)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No vehicle with registration number " + registrationNumber.trim()));
    }

    /** All maintenance records of the whole fleet. */
    public List<MaintenanceRecord> findAllMaintenanceRecords() {
        List<MaintenanceRecord> all = new ArrayList<>();
        for (Vehicle v : vehicleRepository.findAll()) {
            all.addAll(v.getMaintenanceHistory());
        }
        return all;
    }

    /** Stores the changes made to a vehicle (status, mileage, maintenance records). */
    public void save(Vehicle vehicle) {
        vehicleRepository.save(vehicle);
    }

    public List<Vehicle> findAll() {
        return vehicleRepository.findAll();
    }
}
