package com.rental.repository;

import java.util.List;

import com.rental.model.MaintenanceRecord;
import com.rental.model.Vehicle;

/**
 * FR10, FR26: storage of MaintenanceRecord entries.
 */
public interface MaintenanceRepository {

    void save(MaintenanceRecord record);

    List<MaintenanceRecord> findByVehicle(Vehicle vehicle);

    List<MaintenanceRecord> findAll();
}
