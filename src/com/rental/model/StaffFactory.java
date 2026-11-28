package com.rental.model;

import com.rental.enums.StaffRole;

/** Creates the right Staff subclass for a role (used when staff are loaded from storage). */
public final class StaffFactory {

    private StaffFactory() {
    }

    public static Staff create(String staffID, String name, StaffRole role) {
        switch (role) {
            case RENTAL_STAFF:
                return new RentalStaff(staffID, name);
            case FLEET_STAFF:
                return new FleetStaff(staffID, name);
            case ADMIN:
                return new AdminStaff(staffID, name);
            default:
                throw new IllegalArgumentException("Unknown staff role: " + role);
        }
    }
}
