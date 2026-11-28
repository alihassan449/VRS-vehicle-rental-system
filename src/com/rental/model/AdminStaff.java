package com.rental.model;

import com.rental.enums.StaffRole;

/**
 * Administrator (role ADMIN): allowed to do everything - both the rental operations
 * (customers, reservations, rentals, payments) and the fleet operations (vehicles, maintenance).
 */
public class AdminStaff extends Staff {

    public AdminStaff(String staffID, String name) {
        super(staffID, name, StaffRole.ADMIN);
    }

    @Override
    public boolean canHandleRentals() {
        return true;
    }

    @Override
    public boolean canManageFleet() {
        return true;
    }
}
