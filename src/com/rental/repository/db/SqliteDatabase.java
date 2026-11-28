package com.rental.repository.db;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import com.rental.util.IdGenerator;

/**
 * Manages the SQLite connection, schema creation, and persistent ID counters.
 * Database file defaults to {@code data/vehicle_rental.db}.
 */
public class SqliteDatabase {
    public static final String DEFAULT_DB_PATH = "data/vehicle_rental.db";
    private final String jdbcUrl;

    public SqliteDatabase() {
        this(DEFAULT_DB_PATH);
    }

    public SqliteDatabase(String dbPath) {
        File file = new File(dbPath);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        this.jdbcUrl = "jdbc:sqlite:" + dbPath.replace('\\', '/');
        initSchema();
        syncIdCounters();
    }

    public Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(jdbcUrl);
        try (Statement st = conn.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON;");
        }
        return conn;
    }

    private void initSchema() {
        String[] ddl = {
            "CREATE TABLE IF NOT EXISTS staff ("
                + "staff_id TEXT PRIMARY KEY, "
                + "name TEXT NOT NULL, "
                + "username TEXT NOT NULL UNIQUE, "
                + "password TEXT NOT NULL, "
                + "role TEXT NOT NULL)",
            "CREATE TABLE IF NOT EXISTS customers ("
                + "customer_id TEXT PRIMARY KEY, "
                + "name TEXT NOT NULL, "
                + "street TEXT, "
                + "city TEXT, "
                + "state TEXT, "
                + "postal_code TEXT, "
                + "country TEXT, "
                + "contact_number TEXT NOT NULL, "
                + "national_id TEXT NOT NULL UNIQUE, "
                + "license_number TEXT NOT NULL UNIQUE, "
                + "issuing_authority TEXT, "
                + "issue_date TEXT, "
                + "expiry_date TEXT NOT NULL)",
            "CREATE TABLE IF NOT EXISTS vehicles ("
                + "registration_number TEXT PRIMARY KEY, "
                + "make TEXT NOT NULL, "
                + "model TEXT NOT NULL, "
                + "year INTEGER NOT NULL, "
                + "category TEXT NOT NULL, "
                + "transmission_type TEXT NOT NULL, "
                + "fuel_type TEXT NOT NULL, "
                + "current_mileage REAL NOT NULL, "
                + "daily_rental_rate REAL NOT NULL, "
                + "status TEXT NOT NULL)",
            "CREATE TABLE IF NOT EXISTS reservations ("
                + "reservation_id TEXT PRIMARY KEY, "
                + "customer_id TEXT NOT NULL, "
                + "vehicle_reg TEXT NOT NULL, "
                + "pickup_date TEXT NOT NULL, "
                + "return_date TEXT NOT NULL, "
                + "status TEXT NOT NULL, "
                + "estimated_charge REAL NOT NULL, "
                + "FOREIGN KEY (customer_id) REFERENCES customers(customer_id), "
                + "FOREIGN KEY (vehicle_reg) REFERENCES vehicles(registration_number))",
            "CREATE TABLE IF NOT EXISTS rental_agreements ("
                + "agreement_id TEXT PRIMARY KEY, "
                + "reservation_id TEXT NOT NULL, "
                + "status TEXT NOT NULL, "
                + "pickup_date_time TEXT, "
                + "start_mileage REAL, "
                + "start_fuel_level TEXT, "
                + "existing_damage_notes TEXT, "
                + "return_date_time TEXT, "
                + "end_mileage REAL, "
                + "end_fuel_level TEXT, "
                + "new_damage_notes TEXT, "
                + "base_charge REAL NOT NULL DEFAULT 0, "
                + "final_amount REAL NOT NULL DEFAULT 0, "
                + "FOREIGN KEY (reservation_id) REFERENCES reservations(reservation_id))",
            "CREATE TABLE IF NOT EXISTS agreement_charges ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "agreement_id TEXT NOT NULL, "
                + "description TEXT NOT NULL, "
                + "amount REAL NOT NULL, "
                + "FOREIGN KEY (agreement_id) REFERENCES rental_agreements(agreement_id) ON DELETE CASCADE)",
            "CREATE TABLE IF NOT EXISTS maintenance_records ("
                + "record_id TEXT PRIMARY KEY, "
                + "vehicle_reg TEXT NOT NULL, "
                + "maintenance_type TEXT NOT NULL, "
                + "record_date TEXT NOT NULL, "
                + "description TEXT NOT NULL, "
                + "cost REAL NOT NULL, "
                + "completed INTEGER NOT NULL, "
                + "FOREIGN KEY (vehicle_reg) REFERENCES vehicles(registration_number))",
            "CREATE TABLE IF NOT EXISTS payments ("
                + "payment_id TEXT PRIMARY KEY, "
                + "agreement_id TEXT NOT NULL, "
                + "amount_paid REAL NOT NULL, "
                + "payment_date TEXT NOT NULL, "
                + "method TEXT NOT NULL, "
                + "balance_after REAL NOT NULL, "
                + "FOREIGN KEY (agreement_id) REFERENCES rental_agreements(agreement_id))",
            "CREATE TABLE IF NOT EXISTS id_counters ("
                + "prefix TEXT PRIMARY KEY, "
                + "next_val INTEGER NOT NULL)"
        };

        try (Connection conn = getConnection(); Statement st = conn.createStatement()) {
            for (String sql : ddl) {
                st.execute(sql);
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to initialize SQLite database schema: " + ex.getMessage(), ex);
        }
    }

    /** Ensures sequential IDs continue cleanly after app restarts without collisions. */
    private void syncIdCounters() {
        syncPrefix("CUS", "customers", "customer_id");
        syncPrefix("C", "customers", "customer_id");
        syncPrefix("RES", "reservations", "reservation_id");
        syncPrefix("AGR", "rental_agreements", "agreement_id");
        syncPrefix("MNT", "maintenance_records", "record_id");
        syncPrefix("PAY", "payments", "payment_id");

        IdGenerator.setCounterStore((prefix, nextValue) -> {
            String sql = "INSERT INTO id_counters(prefix, next_val) VALUES (?, ?) "
                    + "ON CONFLICT(prefix) DO UPDATE SET next_val = excluded.next_val";
            try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, prefix);
                ps.setInt(2, nextValue);
                ps.executeUpdate();
            } catch (SQLException ex) {
                throw new RuntimeException("Failed to persist ID counter for " + prefix, ex);
            }
        });
    }

    private void syncPrefix(String prefix, String table, String column) {
        int max = 0;
        String storedSql = "SELECT next_val FROM id_counters WHERE prefix = ?";
        try (Connection conn = getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(storedSql)) {
                ps.setString(1, prefix);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        max = Math.max(max, rs.getInt(1) - 1);
                    }
                }
            }
            String likePattern = prefix + "-%";
            String scanSql = "SELECT " + column + " FROM " + table + " WHERE " + column + " LIKE ?";
            try (PreparedStatement ps = conn.prepareStatement(scanSql)) {
                ps.setString(1, likePattern);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String id = rs.getString(1);
                        int dash = id.lastIndexOf('-');
                        if (dash >= 0 && dash + 1 < id.length()) {
                            try {
                                int num = Integer.parseInt(id.substring(dash + 1));
                                if (num > max) {
                                    max = num;
                                }
                            } catch (NumberFormatException ignored) {
                            }
                        }
                    }
                }
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to sync ID counter for " + prefix, ex);
        }
        IdGenerator.initCounter(prefix, max + 1);
    }

    public boolean isEmptyDatabase() {
        try (Connection conn = getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT (SELECT COUNT(*) FROM customers) + (SELECT COUNT(*) FROM vehicles)")) {
            return rs.next() && rs.getInt(1) == 0;
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to inspect database state", ex);
        }
    }
}
