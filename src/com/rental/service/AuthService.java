package com.rental.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.rental.model.Staff;
import com.rental.repository.db.SqliteStaffRepository;

/**
 * Pre-condition of every use case: "the user is logged in". Authenticates
 * Rental Staff, Fleet Staff and Admin users (SQLite-backed when configured,
 * with in-memory fallback).
 */
public class AuthService {
    private final List<Staff> users = new ArrayList<>();
    private final SqliteStaffRepository staffRepo;

    public AuthService() {
        this.staffRepo = null;
    }

    public AuthService(SqliteStaffRepository staffRepo) {
        this.staffRepo = staffRepo;
    }

    public void addUser(Staff staff) {
        users.add(staff);
    }

    public void addUser(Staff staff, String rawPassword) {
        if (staffRepo != null) {
            staffRepo.save(staff, rawPassword);
        } else {
            users.add(staff);
        }
    }

    public Optional<Staff> authenticate(String username, String password) {
        if (username == null || password == null) {
            return Optional.empty();
        }
        if (staffRepo != null) {
            return staffRepo.authenticate(username, password);
        }
        for (Staff s : users) {
            if (s.getUsername().equalsIgnoreCase(username.trim()) && s.passwordMatches(password)) {
                return Optional.of(s);
            }
        }
        return Optional.empty();
    }

    public List<Staff> getUsers() {
        if (staffRepo != null) {
            return staffRepo.findAll();
        }
        return new ArrayList<>(users);
    }

    public boolean usernameExists(String username) {
        if (username == null)
            return false;
        for (Staff staff : getUsers()) {
            if (staff.getUsername().equalsIgnoreCase(username.trim()))
                return true;
        }
        return false;
    }

    public void deleteUser(Staff staff) {
        if (staff == null || staff.getRole() == com.rental.enums.StaffRole.ADMIN) {
            throw new IllegalArgumentException("Administrator accounts cannot be deleted.");
        }
        if (staffRepo != null) {
            staffRepo.delete(staff.getStaffId());
        } else {
            users.removeIf(s -> s.getStaffId().equals(staff.getStaffId()));
        }
    }
}
