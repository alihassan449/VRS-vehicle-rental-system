package com.rental.service;

import java.time.Year;
import java.util.ArrayList;
import java.util.List;

import com.rental.enums.FuelType;
import com.rental.enums.TransmissionType;
import com.rental.enums.VehicleCategory;
import com.rental.enums.VehicleStatus;
import com.rental.exception.BusinessRuleException;
import com.rental.exception.ValidationException;
import com.rental.model.Vehicle;
import com.rental.repository.VehicleRepository;

/**
 * UC-04 Register Vehicle and UC-05 Search Vehicle.
 */
public class VehicleService {
    private final VehicleRepository vehicleRepository;

    public VehicleService(VehicleRepository vehicleRepository) {
        this.vehicleRepository = vehicleRepository;
    }

    /** UC-04 steps 3.1 - 3.3: validate, save with status Available. */
    public Vehicle registerVehicle(String registrationNumber, String make, String model, Integer year,
                                   VehicleCategory category, TransmissionType transmissionType,
                                   FuelType fuelType, Double currentMileage, Double dailyRentalRate) {
        registrationNumber = registrationNumber == null ? "" : registrationNumber.trim();
        make = make == null ? "" : make.trim();
        model = model == null ? "" : model.trim();
        if (registrationNumber.isEmpty()) {
            throw new ValidationException("Registration number is required.");
        }
        if (make.isEmpty() || model.isEmpty()) {
            throw new ValidationException("Make and model are required.");
        }
        if (year == null || year < 1950 || year > Year.now().getValue() + 1) {
            throw new ValidationException("Year must be a valid model year (1950 - " + (Year.now().getValue() + 1) + ").");
        }
        if (category == null || transmissionType == null || fuelType == null) {
            throw new ValidationException("Category, transmission type and fuel type are required.");
        }
        if (currentMileage == null) {
            throw new ValidationException("Current mileage is required.");
        }
        if (currentMileage < 0) {
            throw new ValidationException("Invalid mileage: the current mileage cannot be below 0.");
        }
        if (dailyRentalRate == null || dailyRentalRate <= 0) {
            throw new ValidationException("Daily rental rate must be greater than 0.");
        }
        if (vehicleRepository.findByRegistrationNumber(registrationNumber).isPresent()) {
            throw new BusinessRuleException("Duplicate registration number: vehicle "
                    + registrationNumber + " is already registered.");
        }
        Vehicle vehicle = new Vehicle(registrationNumber, make, model, year, category,
                transmissionType, fuelType, currentMileage, dailyRentalRate);
        vehicleRepository.save(vehicle); // status Available
        return vehicle;
    }

    /**
     * UC-05: filter by registration number (partial, case-insensitive), category
     * and/or status. Any null/blank filter is ignored.
     */
    public List<Vehicle> search(String registrationNumber, VehicleCategory category, VehicleStatus status) {
        String reg = registrationNumber == null ? "" : registrationNumber.trim().toLowerCase();
        List<Vehicle> result = new ArrayList<>();
        for (Vehicle v : vehicleRepository.findAll()) {
            if (!reg.isEmpty() && !v.getRegistrationNumber().toLowerCase().contains(reg)) {
                continue;
            }
            if (category != null && v.getCategory() != category) {
                continue;
            }
            if (status != null && v.getStatus() != status) {
                continue;
            }
            result.add(v);
        }
        return result;
    }

    public List<Vehicle> findAll() {
        return vehicleRepository.findAll();
    }

    public Vehicle findByRegistrationNumber(String registrationNumber) {
        return vehicleRepository.findByRegistrationNumber(registrationNumber == null ? "" : registrationNumber.trim())
                .orElse(null);
    }
}
