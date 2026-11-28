package com.rental.model;

import com.rental.enums.StaffRole;

/**
 * Rental staff (role RENTAL_STAFF): works with customers, reservations, rental
 * agreements (handover / return) and payments. Cannot touch fleet or maintenance.
 * (The operations themselves are defined in Staff; this class only decides the permission.)
 */
public class RentalStaff extends Staff {

    public RentalStaff(String staffID, String name) {
        super(staffID, name, StaffRole.RENTAL_STAFF);
    }

    @Override
    public boolean canHandleRentals() {
        return true;
    }

    @Override
    public boolean canManageFleet() {
        return false;
    }
}
