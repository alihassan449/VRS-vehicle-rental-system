package com.rental.repository.sqlserver;

import com.rental.model.Vehicle;
import com.rental.repository.InMemoryVehicleRepository;

/** Vehicles (with their maintenance records) kept in memory and written to SQL Server on every save. */
public class SqlServerVehicleRepository extends InMemoryVehicleRepository {

    private final SqlServerDatabase db;

    public SqlServerVehicleRepository(SqlServerDatabase db) {
        this.db = db;
    }

    @Override
    public void save(Vehicle vehicle) {
        db.saveVehicle(vehicle);
        super.save(vehicle);
    }

    public void cache(Vehicle vehicle) {
        super.save(vehicle);
    }
}
