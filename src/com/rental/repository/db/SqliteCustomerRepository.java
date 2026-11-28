package com.rental.repository.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.rental.model.Address;
import com.rental.model.Customer;
import com.rental.model.DrivingLicense;
import com.rental.repository.CustomerRepository;

/** SQLite-backed implementation of {@link CustomerRepository}. */
public class SqliteCustomerRepository implements CustomerRepository {
    private final SqliteDatabase db;

    public SqliteCustomerRepository(SqliteDatabase db) {
        this.db = db;
    }

    @Override
    public void save(Customer customer) {
        String sql = "INSERT INTO customers(customer_id, name, street, city, state, postal_code, country, "
                + "contact_number, national_id, license_number, issuing_authority, issue_date, expiry_date) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) "
                + "ON CONFLICT(customer_id) DO UPDATE SET "
                + "name = excluded.name, street = excluded.street, city = excluded.city, state = excluded.state, "
                + "postal_code = excluded.postal_code, country = excluded.country, "
                + "contact_number = excluded.contact_number, national_id = excluded.national_id, "
                + "license_number = excluded.license_number, issuing_authority = excluded.issuing_authority, "
                + "issue_date = excluded.issue_date, expiry_date = excluded.expiry_date";
        Address a = customer.getAddress();
        DrivingLicense dl = customer.getDrivingLicense();
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, customer.getCustomerId());
            ps.setString(2, customer.getName());
            ps.setString(3, a == null ? "" : nullToEmpty(a.getStreet()));
            ps.setString(4, a == null ? "" : nullToEmpty(a.getCity()));
            ps.setString(5, a == null ? "" : nullToEmpty(a.getState()));
            ps.setString(6, a == null ? "" : nullToEmpty(a.getPostalCode()));
            ps.setString(7, a == null ? "" : nullToEmpty(a.getCountry()));
            ps.setString(8, customer.getContactNumber());
            ps.setString(9, customer.getNationalId());
            ps.setString(10, dl == null ? "" : nullToEmpty(dl.getLicenseNumber()));
            ps.setString(11, dl == null ? "" : nullToEmpty(dl.getIssuingAuthority()));
            ps.setString(12, dl == null || dl.getIssueDate() == null ? null : dl.getIssueDate().toString());
            ps.setString(13, dl == null || dl.getExpiryDate() == null ? "" : dl.getExpiryDate().toString());
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException("Database error saving customer: " + ex.getMessage(), ex);
        }
    }

    @Override
    public Optional<Customer> findById(String customerId) {
        if (customerId == null) {
            return Optional.empty();
        }
        String sql = "SELECT * FROM customers WHERE LOWER(customer_id) = LOWER(?)";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, customerId.trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Database error finding customer: " + ex.getMessage(), ex);
        }
    }

    @Override
    public List<Customer> findByNationalId(String nationalId) {
        String sql = "SELECT * FROM customers WHERE LOWER(national_id) = LOWER(?) ORDER BY customer_id";
        return queryList(sql, nationalId == null ? "" : nationalId.trim());
    }

    @Override
    public List<Customer> findByNameContains(String namePart) {
        if (namePart == null) {
            return new ArrayList<>();
        }
        String sql = "SELECT * FROM customers WHERE LOWER(name) LIKE LOWER(?) ORDER BY customer_id";
        return queryList(sql, "%" + namePart.trim() + "%");
    }

    @Override
    public List<Customer> findByDrivingLicenseNumber(String licenseNumber) {
        String sql = "SELECT * FROM customers WHERE LOWER(license_number) = LOWER(?) ORDER BY customer_id";
        return queryList(sql, licenseNumber == null ? "" : licenseNumber.trim());
    }

    @Override
    public List<Customer> findAll() {
        String sql = "SELECT * FROM customers ORDER BY customer_id";
        List<Customer> list = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Database error listing customers: " + ex.getMessage(), ex);
        }
        return list;
    }

    private List<Customer> queryList(String sql, String param) {
        List<Customer> list = new ArrayList<>();
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Database error querying customers: " + ex.getMessage(), ex);
        }
        return list;
    }

    static Customer mapRow(ResultSet rs) throws SQLException {
        Address addr = new Address(
                rs.getString("street"),
                rs.getString("city"),
                rs.getString("state"),
                rs.getString("postal_code"),
                rs.getString("country"));
        String issueStr = rs.getString("issue_date");
        String expiryStr = rs.getString("expiry_date");
        DrivingLicense dl = new DrivingLicense(
                rs.getString("license_number"),
                rs.getString("issuing_authority"),
                issueStr == null || issueStr.isEmpty() ? null : LocalDate.parse(issueStr),
                expiryStr == null || expiryStr.isEmpty() ? null : LocalDate.parse(expiryStr));
        return new Customer(
                rs.getString("customer_id"),
                rs.getString("name"),
                addr,
                rs.getString("contact_number"),
                rs.getString("national_id"),
                dl);
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
