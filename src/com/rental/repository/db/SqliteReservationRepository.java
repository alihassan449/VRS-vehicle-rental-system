package com.rental.repository.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.rental.enums.ReservationStatus;
import com.rental.model.Customer;
import com.rental.model.Reservation;
import com.rental.model.Vehicle;
import com.rental.repository.CustomerRepository;
import com.rental.repository.ReservationRepository;
import com.rental.repository.VehicleRepository;

/** SQLite-backed implementation of {@link ReservationRepository}. */
public class SqliteReservationRepository implements ReservationRepository {
    private final SqliteDatabase db;
    private final CustomerRepository customerRepo;
    private final VehicleRepository vehicleRepo;

    public SqliteReservationRepository(SqliteDatabase db, CustomerRepository customerRepo,
                                       VehicleRepository vehicleRepo) {
        this.db = db;
        this.customerRepo = customerRepo;
        this.vehicleRepo = vehicleRepo;
    }

    @Override
    public void save(Reservation r) {
        String sql = "INSERT INTO reservations(reservation_id, customer_id, vehicle_reg, pickup_date, "
                + "return_date, status, estimated_charge) VALUES (?, ?, ?, ?, ?, ?, ?) "
                + "ON CONFLICT(reservation_id) DO UPDATE SET "
                + "customer_id = excluded.customer_id, vehicle_reg = excluded.vehicle_reg, "
                + "pickup_date = excluded.pickup_date, return_date = excluded.return_date, "
                + "status = excluded.status, estimated_charge = excluded.estimated_charge";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, r.getReservationId());
            ps.setString(2, r.getCustomer().getCustomerId());
            ps.setString(3, r.getVehicle().getRegistrationNumber());
            ps.setString(4, r.getPickupDate().toString());
            ps.setString(5, r.getReturnDate().toString());
            ps.setString(6, r.getStatus().name());
            ps.setDouble(7, r.getEstimatedCharge());
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException("Database error saving reservation: " + ex.getMessage(), ex);
        }
    }

    @Override
    public Optional<Reservation> findById(String reservationId) {
        if (reservationId == null) {
            return Optional.empty();
        }
        String sql = "SELECT * FROM reservations WHERE reservation_id = ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, reservationId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Database error finding reservation: " + ex.getMessage(), ex);
        }
    }

    @Override
    public List<Reservation> findByVehicle(Vehicle vehicle) {
        if (vehicle == null) {
            return new ArrayList<>();
        }
        String sql = "SELECT * FROM reservations WHERE vehicle_reg = ? ORDER BY pickup_date, reservation_id";
        return queryList(sql, vehicle.getRegistrationNumber());
    }

    @Override
    public List<Reservation> findByCustomerId(String customerId) {
        String sql = "SELECT * FROM reservations WHERE customer_id = ? ORDER BY pickup_date, reservation_id";
        return queryList(sql, customerId);
    }

    @Override
    public List<Reservation> findAll() {
        String sql = "SELECT * FROM reservations ORDER BY reservation_id";
        List<Reservation> list = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Database error listing reservations: " + ex.getMessage(), ex);
        }
        return list;
    }

    private List<Reservation> queryList(String sql, String param) {
        List<Reservation> list = new ArrayList<>();
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Database error querying reservations: " + ex.getMessage(), ex);
        }
        return list;
    }

    private Reservation mapRow(ResultSet rs) throws SQLException {
        String custId = rs.getString("customer_id");
        String regNo = rs.getString("vehicle_reg");
        Customer customer = customerRepo.findById(custId).orElse(null);
        Vehicle vehicle = vehicleRepo.findByRegistrationNumber(regNo).orElse(null);
        Reservation r = new Reservation(
                rs.getString("reservation_id"),
                customer,
                vehicle,
                LocalDate.parse(rs.getString("pickup_date")),
                LocalDate.parse(rs.getString("return_date")));
        r.setStatus(ReservationStatus.valueOf(rs.getString("status")));
        r.setEstimatedCharge(rs.getDouble("estimated_charge"));
        return r;
    }
}
