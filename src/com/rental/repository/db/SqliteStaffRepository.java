package com.rental.repository.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.rental.enums.StaffRole;
import com.rental.model.Staff;

/** SQLite-backed store for {@link Staff} accounts. */
public class SqliteStaffRepository {
    private final SqliteDatabase db;

    public SqliteStaffRepository(SqliteDatabase db) {
        this.db = db;
    }

    public boolean isEmpty() {
        String sql = "SELECT COUNT(*) FROM staff";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() && rs.getInt(1) == 0;
        } catch (SQLException ex) {
            throw new RuntimeException("Database error checking staff: " + ex.getMessage(), ex);
        }
    }

    public void save(Staff staff, String rawPassword) {
        String sql = "INSERT INTO staff(staff_id, name, username, password, role) VALUES (?, ?, ?, ?, ?) "
                + "ON CONFLICT(staff_id) DO UPDATE SET name = excluded.name, username = excluded.username, "
                + "password = excluded.password, role = excluded.role";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, staff.getStaffId());
            ps.setString(2, staff.getName());
            ps.setString(3, staff.getUsername());
            ps.setString(4, rawPassword);
            ps.setString(5, staff.getRole().name());
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException("Database error saving staff: " + ex.getMessage(), ex);
        }
    }

    public Optional<Staff> authenticate(String username, String password) {
        if (username == null || password == null) {
            return Optional.empty();
        }
        String sql = "SELECT * FROM staff WHERE LOWER(username) = LOWER(?) AND password = ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username.trim());
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Database error authenticating staff: " + ex.getMessage(), ex);
        }
    }

    public List<Staff> findAll() {
        String sql = "SELECT * FROM staff ORDER BY staff_id";
        List<Staff> list = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Database error listing staff: " + ex.getMessage(), ex);
        }
        return list;
    }

    public void delete(String staffId) {
        String sql = "DELETE FROM staff WHERE staff_id = ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, staffId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException("Database error deleting staff: " + ex.getMessage(), ex);
        }
    }

    private static Staff mapRow(ResultSet rs) throws SQLException {
        return new Staff(
                rs.getString("staff_id"),
                rs.getString("name"),
                rs.getString("username"),
                rs.getString("password"),
                StaffRole.valueOf(rs.getString("role")));
    }
}
