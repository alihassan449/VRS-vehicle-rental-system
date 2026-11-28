package com.rental.repository;

import java.util.List;
import java.util.Optional;

import com.rental.model.Vehicle;

/** Storage of Vehicle objects (FR6, FR9). */
public interface VehicleRepository {
    void save(Vehicle vehicle);
    Optional<Vehicle> findByRegistrationNumber(String registrationNumber);
    List<Vehicle> findAll();
}
