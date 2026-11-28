package com.rental.repository.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.rental.enums.FuelType;
import com.rental.enums.TransmissionType;
import com.rental.enums.VehicleCategory;
import com.rental.enums.VehicleStatus;
import com.rental.model.MaintenanceRecord;
import com.rental.model.Vehicle;
import com.rental.repository.VehicleRepository;

/** SQLite-backed implementation of {@link VehicleRepository}. */
public class SqliteVehicleRepository implements VehicleRepository {
    private final SqliteDatabase db;

    public SqliteVehicleRepository(SqliteDatabase db) {
        this.db = db;
    }

    @Override
    public void save(Vehicle vehicle) {
        String sql = "INSERT INTO vehicles(registration_number, make, model, year, category, transmission_type, "
                + "fuel_type, current_mileage, daily_rental_rate, status) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) "
                + "ON CONFLICT(registration_number) DO UPDATE SET "
                + "make = excluded.make, model = excluded.model, year = excluded.year, "
                + "category = excluded.category, transmission_type = excluded.transmission_type, "
                + "fuel_type = excluded.fuel_type, current_mileage = excluded.current_mileage, "
                + "daily_rental_rate = excluded.daily_rental_rate, status = excluded.status";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, vehicle.getRegistrationNumber());
            ps.setString(2, vehicle.getMake());
            ps.setString(3, vehicle.getModel());
            ps.setInt(4, vehicle.getYear());
            ps.setString(5, vehicle.getCategory().name());
            ps.setString(6, vehicle.getTransmissionType().name());
            ps.setString(7, vehicle.getFuelType().name());
            ps.setDouble(8, vehicle.getCurrentMileage());
            ps.setDouble(9, vehicle.getDailyRentalRate());
            ps.setString(10, vehicle.getStatus().name());
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException("Database error saving vehicle: " + ex.getMessage(), ex);
        }
    }

    @Override
    public Optional<Vehicle> findByRegistrationNumber(String registrationNumber) {
        if (registrationNumber == null) {
            return Optional.empty();
        }
        String sql = "SELECT * FROM vehicles WHERE LOWER(registration_number) = LOWER(?)";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, registrationNumber.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                Vehicle v = mapRow(rs);
                hydrateMaintenance(conn, v);
                return Optional.of(v);
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Database error finding vehicle: " + ex.getMessage(), ex);
        }
    }

    @Override
    public List<Vehicle> findByCategory(VehicleCategory category) {
        String sql = "SELECT * FROM vehicles WHERE category = ? ORDER BY registration_number";
        return queryVehicles(sql, category.name());
    }

    @Override
    public List<Vehicle> findByStatus(VehicleStatus status) {
        String sql = "SELECT * FROM vehicles WHERE status = ? ORDER BY registration_number";
        return queryVehicles(sql, status.name());
    }

    @Override
    public List<Vehicle> findAll() {
        String sql = "SELECT * FROM vehicles ORDER BY registration_number";
        List<Vehicle> list = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Vehicle v = mapRow(rs);
                hydrateMaintenance(conn, v);
                list.add(v);
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Database error listing vehicles: " + ex.getMessage(), ex);
        }
        return list;
    }

    private List<Vehicle> queryVehicles(String sql, String param) {
        List<Vehicle> list = new ArrayList<>();
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Vehicle v = mapRow(rs);
                    hydrateMaintenance(conn, v);
                    list.add(v);
                }
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Database error querying vehicles: " + ex.getMessage(), ex);
        }
        return list;
    }

    static void hydrateMaintenance(Connection conn, Vehicle v) throws SQLException {
        String sql = "SELECT * FROM maintenance_records WHERE vehicle_reg = ? ORDER BY record_date, record_id";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, v.getRegistrationNumber());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    MaintenanceRecord rec = new MaintenanceRecord(
                            rs.getString("record_id"),
                            v,
                            rs.getString("maintenance_type"),
                            LocalDate.parse(rs.getString("record_date")),
                            rs.getString("description"),
                            rs.getDouble("cost"),
                            rs.getInt("completed") != 0);
                    v.addMaintenanceRecord(rec);
                }
            }
        }
    }

    static Vehicle mapRow(ResultSet rs) throws SQLException {
        Vehicle v = new Vehicle(
                rs.getString("registration_number"),
                rs.getString("make"),
                rs.getString("model"),
                rs.getInt("year"),
                VehicleCategory.valueOf(rs.getString("category")),
                TransmissionType.valueOf(rs.getString("transmission_type")),
                FuelType.valueOf(rs.getString("fuel_type")),
                rs.getDouble("current_mileage"),
                rs.getDouble("daily_rental_rate"));
        v.setStatus(VehicleStatus.valueOf(rs.getString("status")));
        return v;
    }
}
