package com.rental.repository.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.rental.enums.FuelLevel;
import com.rental.enums.PaymentMethod;
import com.rental.model.Charge;
import com.rental.model.ChargeBreakdown;
import com.rental.model.Payment;
import com.rental.model.RentalAgreement;
import com.rental.model.Reservation;
import com.rental.model.ReturnDetails;
import com.rental.model.Vehicle;
import com.rental.repository.RentalAgreementRepository;
import com.rental.repository.ReservationRepository;

/** SQLite-backed implementation of {@link RentalAgreementRepository}. */
public class SqliteRentalAgreementRepository implements RentalAgreementRepository {
    private final SqliteDatabase db;
    private final ReservationRepository reservationRepo;

    public SqliteRentalAgreementRepository(SqliteDatabase db, ReservationRepository reservationRepo) {
        this.db = db;
        this.reservationRepo = reservationRepo;
    }

    @Override
    public void save(RentalAgreement a) {
        String sql = "INSERT INTO rental_agreements(agreement_id, reservation_id, status, pickup_date_time, "
                + "start_mileage, start_fuel_level, existing_damage_notes, return_date_time, end_mileage, "
                + "end_fuel_level, new_damage_notes, base_charge, final_amount) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) "
                + "ON CONFLICT(agreement_id) DO UPDATE SET "
                + "reservation_id = excluded.reservation_id, status = excluded.status, "
                + "pickup_date_time = excluded.pickup_date_time, start_mileage = excluded.start_mileage, "
                + "start_fuel_level = excluded.start_fuel_level, existing_damage_notes = excluded.existing_damage_notes, "
                + "return_date_time = excluded.return_date_time, end_mileage = excluded.end_mileage, "
                + "end_fuel_level = excluded.end_fuel_level, new_damage_notes = excluded.new_damage_notes, "
                + "base_charge = excluded.base_charge, final_amount = excluded.final_amount";
        try (Connection conn = db.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, a.getAgreementId());
                ps.setString(2, a.getReservation().getReservationId());
                ps.setString(3, a.getStatus().name());
                ps.setString(4, a.getPickupDateTime() == null ? null : a.getPickupDateTime().toString());
                ps.setDouble(5, a.getStartMileage());
                ps.setString(6, a.getStartFuelLevel() == null ? null : a.getStartFuelLevel().name());
                ps.setString(7, a.getExistingDamageNotes());
                if (a.getReturnDateTime() == null) {
                    ps.setNull(8, Types.VARCHAR);
                    ps.setNull(9, Types.REAL);
                    ps.setNull(10, Types.VARCHAR);
                } else {
                    ps.setString(8, a.getReturnDateTime().toString());
                    ps.setDouble(9, a.getEndMileage());
                    ps.setString(10, a.getEndFuelLevel() == null ? null : a.getEndFuelLevel().name());
                }
                ps.setString(11, a.getNewDamageNotes());
                ps.setDouble(12, a.getBaseCharge());
                ps.setDouble(13, a.getFinalAmount());
                ps.executeUpdate();
            }

            try (PreparedStatement del = conn.prepareStatement("DELETE FROM agreement_charges WHERE agreement_id = ?")) {
                del.setString(1, a.getAgreementId());
                del.executeUpdate();
            }
            if (!a.getCharges().isEmpty()) {
                try (PreparedStatement ins = conn.prepareStatement(
                        "INSERT INTO agreement_charges(agreement_id, description, amount) VALUES (?, ?, ?)")) {
                    for (Charge c : a.getCharges()) {
                        ins.setString(1, a.getAgreementId());
                        ins.setString(2, c.getDescription());
                        ins.setDouble(3, c.getAmount());
                        ins.addBatch();
                    }
                    ins.executeBatch();
                }
            }
            conn.commit();
        } catch (SQLException ex) {
            throw new RuntimeException("Database error saving rental agreement: " + ex.getMessage(), ex);
        }
    }

    @Override
    public Optional<RentalAgreement> findById(String agreementId) {
        if (agreementId == null) {
            return Optional.empty();
        }
        String sql = "SELECT * FROM rental_agreements WHERE agreement_id = ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, agreementId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(conn, rs)) : Optional.empty();
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Database error finding rental agreement: " + ex.getMessage(), ex);
        }
    }

    @Override
    public List<RentalAgreement> findByVehicle(Vehicle vehicle) {
        if (vehicle == null) {
            return new ArrayList<>();
        }
        String sql = "SELECT ra.* FROM rental_agreements ra "
                + "JOIN reservations r ON ra.reservation_id = r.reservation_id "
                + "WHERE r.vehicle_reg = ? ORDER BY ra.agreement_id";
        return queryList(sql, vehicle.getRegistrationNumber());
    }

    @Override
    public List<RentalAgreement> findByCustomerId(String customerId) {
        String sql = "SELECT ra.* FROM rental_agreements ra "
                + "JOIN reservations r ON ra.reservation_id = r.reservation_id "
                + "WHERE r.customer_id = ? ORDER BY ra.agreement_id";
        return queryList(sql, customerId);
    }

    @Override
    public List<RentalAgreement> findAll() {
        String sql = "SELECT * FROM rental_agreements ORDER BY agreement_id";
        List<RentalAgreement> list = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(conn, rs));
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Database error listing rental agreements: " + ex.getMessage(), ex);
        }
        return list;
    }

    private List<RentalAgreement> queryList(String sql, String param) {
        List<RentalAgreement> list = new ArrayList<>();
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(conn, rs));
                }
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Database error querying rental agreements: " + ex.getMessage(), ex);
        }
        return list;
    }

    private RentalAgreement mapRow(Connection conn, ResultSet rs) throws SQLException {
        String resId = rs.getString("reservation_id");
        Reservation res = reservationRepo.findById(resId).orElse(null);
        RentalAgreement a = new RentalAgreement(rs.getString("agreement_id"), res);

        String pickupStr = rs.getString("pickup_date_time");
        String startFuelStr = rs.getString("start_fuel_level");
        if (pickupStr != null && startFuelStr != null) {
            a.recordHandover(
                    LocalDateTime.parse(pickupStr),
                    rs.getDouble("start_mileage"),
                    FuelLevel.valueOf(startFuelStr),
                    rs.getString("existing_damage_notes"));
        }

        String retStr = rs.getString("return_date_time");
        String endFuelStr = rs.getString("end_fuel_level");
        if (retStr != null && !retStr.isEmpty() && endFuelStr != null) {
            List<Charge> charges = loadCharges(conn, a.getAgreementId());
            double baseCharge = rs.getDouble("base_charge");
            ReturnDetails details = new ReturnDetails(
                    LocalDateTime.parse(retStr),
                    rs.getDouble("end_mileage"),
                    FuelLevel.valueOf(endFuelStr),
                    rs.getString("new_damage_notes"));
            a.recordReturn(details, new ChargeBreakdown(baseCharge, charges));
        }

        loadPayments(conn, a);
        return a;
    }

    private List<Charge> loadCharges(Connection conn, String agreementId) throws SQLException {
        List<Charge> charges = new ArrayList<>();
        String sql = "SELECT description, amount FROM agreement_charges WHERE agreement_id = ? ORDER BY id";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, agreementId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    charges.add(new Charge(rs.getString("description"), rs.getDouble("amount")));
                }
            }
        }
        return charges;
    }

    private void loadPayments(Connection conn, RentalAgreement a) throws SQLException {
        String sql = "SELECT * FROM payments WHERE agreement_id = ? ORDER BY payment_date, payment_id";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, a.getAgreementId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    a.addPayment(new Payment(
                            rs.getString("payment_id"),
                            a,
                            rs.getDouble("amount_paid"),
                            LocalDate.parse(rs.getString("payment_date")),
                            PaymentMethod.valueOf(rs.getString("method")),
                            rs.getDouble("balance_after")));
                }
            }
        }
    }
}
