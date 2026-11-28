package com.rental.repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.rental.model.Vehicle;

public class InMemoryVehicleRepository implements VehicleRepository {

    private final Map<String, Vehicle> vehicles = new LinkedHashMap<>();

    @Override
    public void save(Vehicle vehicle) {
        vehicles.put(vehicle.getRegistrationNumber().toUpperCase(), vehicle);
    }

    @Override
    public Optional<Vehicle> findByRegistrationNumber(String registrationNumber) {
        return Optional.ofNullable(vehicles.get(registrationNumber.trim().toUpperCase()));
    }

    @Override
    public List<Vehicle> findAll() {
        return new ArrayList<>(vehicles.values());
    }
}
