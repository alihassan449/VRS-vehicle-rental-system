package com.rental.repository.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.rental.model.MaintenanceRecord;
import com.rental.model.Vehicle;
import com.rental.repository.MaintenanceRepository;
import com.rental.repository.VehicleRepository;

/** SQLite-backed implementation of {@link MaintenanceRepository}. */
public class SqliteMaintenanceRepository implements MaintenanceRepository {
    private final SqliteDatabase db;
    private final VehicleRepository vehicleRepo;

    public SqliteMaintenanceRepository(SqliteDatabase db, VehicleRepository vehicleRepo) {
        this.db = db;
        this.vehicleRepo = vehicleRepo;
    }

    @Override
    public void save(MaintenanceRecord record) {
        String sql = "INSERT INTO maintenance_records(record_id, vehicle_reg, maintenance_type, "
                + "record_date, description, cost, completed) VALUES (?, ?, ?, ?, ?, ?, ?) "
                + "ON CONFLICT(record_id) DO UPDATE SET "
                + "vehicle_reg = excluded.vehicle_reg, maintenance_type = excluded.maintenance_type, "
                + "record_date = excluded.record_date, description = excluded.description, "
                + "cost = excluded.cost, completed = excluded.completed";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, record.getRecordId());
            ps.setString(2, record.getVehicle().getRegistrationNumber());
            ps.setString(3, record.getMaintenanceType());
            ps.setString(4, record.getDate().toString());
            ps.setString(5, record.getDescription());
            ps.setDouble(6, record.getCost());
            ps.setInt(7, record.isCompleted() ? 1 : 0);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException("Database error saving maintenance record: " + ex.getMessage(), ex);
        }
    }

    @Override
    public List<MaintenanceRecord> findByVehicle(Vehicle vehicle) {
        if (vehicle == null) {
            return new ArrayList<>();
        }
        String sql = "SELECT * FROM maintenance_records WHERE vehicle_reg = ? ORDER BY record_date, record_id";
        List<MaintenanceRecord> list = new ArrayList<>();
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, vehicle.getRegistrationNumber());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs, vehicle));
                }
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Database error querying maintenance records: " + ex.getMessage(), ex);
        }
        return list;
    }

    @Override
    public List<MaintenanceRecord> findAll() {
        String sql = "SELECT * FROM maintenance_records ORDER BY record_date, record_id";
        List<MaintenanceRecord> list = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Vehicle v = vehicleRepo.findByRegistrationNumber(rs.getString("vehicle_reg")).orElse(null);
                list.add(mapRow(rs, v));
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Database error listing maintenance records: " + ex.getMessage(), ex);
        }
        return list;
    }

    private static MaintenanceRecord mapRow(ResultSet rs, Vehicle vehicle) throws SQLException {
        return new MaintenanceRecord(
                rs.getString("record_id"),
                vehicle,
                rs.getString("maintenance_type"),
                LocalDate.parse(rs.getString("record_date")),
                rs.getString("description"),
                rs.getDouble("cost"),
                rs.getInt("completed") != 0);
    }
}
