package com.rental.model;

import com.rental.enums.StaffRole;

/** A system user (Rental Staff, Fleet Staff or Admin). */
public class Staff {
    private final String staffId;
    private final String name;
    private final String username;
    private final String password;
    private final StaffRole role;

    public Staff(String staffId, String name, String username, String password, StaffRole role) {
        this.staffId = staffId;
        this.name = name;
        this.username = username;
        this.password = password;
        this.role = role;
    }

    public String getStaffId() { return staffId; }
    public String getName() { return name; }
    public String getUsername() { return username; }
    public StaffRole getRole() { return role; }

    public boolean passwordMatches(String candidate) {
        return password != null && password.equals(candidate);
    }

    @Override
    public String toString() {
        return name + " (" + role + ")";
    }
}
