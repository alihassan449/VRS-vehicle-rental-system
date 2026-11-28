package com.rental.model;

import com.rental.enums.StaffRole;

/**
 * Fleet staff (role FLEET_STAFF): registers vehicles and logs / updates maintenance.
 * Cannot process reservations, handovers, returns or payments.
 * (The operations themselves are defined in Staff; this class only decides the permission.)
 */
public class FleetStaff extends Staff {

    public FleetStaff(String staffID, String name) {
        super(staffID, name, StaffRole.FLEET_STAFF);
    }

    @Override
    public boolean canHandleRentals() {
        return false;
    }

    @Override
    public boolean canManageFleet() {
        return true;
    }
}
