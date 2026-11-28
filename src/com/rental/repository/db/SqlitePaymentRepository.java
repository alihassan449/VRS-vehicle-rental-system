package com.rental.repository.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.rental.enums.PaymentMethod;
import com.rental.model.Payment;
import com.rental.model.RentalAgreement;
import com.rental.repository.PaymentRepository;
import com.rental.repository.RentalAgreementRepository;

/** SQLite-backed implementation of {@link PaymentRepository}. */
public class SqlitePaymentRepository implements PaymentRepository {
    private final SqliteDatabase db;
    private final RentalAgreementRepository agreementRepo;

    public SqlitePaymentRepository(SqliteDatabase db, RentalAgreementRepository agreementRepo) {
        this.db = db;
        this.agreementRepo = agreementRepo;
    }

    @Override
    public void save(Payment payment) {
        String sql = "INSERT INTO payments(payment_id, agreement_id, amount_paid, payment_date, method, balance_after) "
                + "VALUES (?, ?, ?, ?, ?, ?) "
                + "ON CONFLICT(payment_id) DO UPDATE SET "
                + "agreement_id = excluded.agreement_id, amount_paid = excluded.amount_paid, "
                + "payment_date = excluded.payment_date, method = excluded.method, "
                + "balance_after = excluded.balance_after";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, payment.getPaymentId());
            ps.setString(2, payment.getRentalAgreement().getAgreementId());
            ps.setDouble(3, payment.getAmountPaid());
            ps.setString(4, payment.getPaymentDate().toString());
            ps.setString(5, payment.getMethod().name());
            ps.setDouble(6, payment.getBalanceAfter());
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException("Database error saving payment: " + ex.getMessage(), ex);
        }
    }

    @Override
    public List<Payment> findByRentalAgreement(RentalAgreement agreement) {
        if (agreement == null) {
            return new ArrayList<>();
        }
        String sql = "SELECT * FROM payments WHERE agreement_id = ? ORDER BY payment_date, payment_id";
        List<Payment> list = new ArrayList<>();
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, agreement.getAgreementId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs, agreement));
                }
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Database error querying payments: " + ex.getMessage(), ex);
        }
        return list;
    }

    @Override
    public List<Payment> findByDateRange(LocalDate from, LocalDate to) {
        String sql = "SELECT * FROM payments WHERE payment_date >= ? AND payment_date <= ? "
                + "ORDER BY payment_date, payment_id";
        List<Payment> list = new ArrayList<>();
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, from.toString());
            ps.setString(2, to.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    RentalAgreement ag = agreementRepo.findById(rs.getString("agreement_id")).orElse(null);
                    list.add(mapRow(rs, ag));
                }
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Database error querying payments by date: " + ex.getMessage(), ex);
        }
        return list;
    }

    @Override
    public List<Payment> findAll() {
        String sql = "SELECT * FROM payments ORDER BY payment_date, payment_id";
        List<Payment> list = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                RentalAgreement ag = agreementRepo.findById(rs.getString("agreement_id")).orElse(null);
                list.add(mapRow(rs, ag));
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Database error listing payments: " + ex.getMessage(), ex);
        }
        return list;
    }

    private static Payment mapRow(ResultSet rs, RentalAgreement ag) throws SQLException {
        return new Payment(
                rs.getString("payment_id"),
                ag,
                rs.getDouble("amount_paid"),
                LocalDate.parse(rs.getString("payment_date")),
                PaymentMethod.valueOf(rs.getString("method")),
                rs.getDouble("balance_after"));
    }
}
