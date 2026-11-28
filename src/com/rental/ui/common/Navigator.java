package com.rental.ui.common;

import com.rental.model.Customer;
import com.rental.model.Vehicle;

/**
 * Allows panels and dashboard cards to navigate to other workspace routes
 * while keeping the sidebar selection synchronized.
 */
public interface Navigator {
    String DASHBOARD = "dashboard";

    // Rental Staff routes
    String CUSTOMERS_DIRECTORY = "customers.directory";
    String CUSTOMERS_CREATE = "customers.create";
    String CUSTOMERS_EDIT = "customers.edit";
    String CUSTOMERS_PROFILE = "customers.profile";
    String RESERVATIONS_LIST = "reservations.list";
    String RESERVATIONS_CREATE = "reservations.create";
    String HANDOVER = "handover";
    String RETURN = "return";
    String PAYMENTS = "payments";

    // Fleet Staff routes
    String FLEET_DIRECTORY = "fleet.directory";
    String FLEET_ADD = "fleet.add";
    String MAINTENANCE = "maintenance";

    // Admin routes
    String STAFF_MANAGEMENT = "staff.management";
    String REPORTS = "reports";

    void navigateTo(String route);

    default void navigateToCreateReservation(Customer preselectedCustomer) {
        navigateTo(RESERVATIONS_CREATE);
    }

    default void navigateToMaintenance(Vehicle preselectedVehicle) {
        navigateTo(MAINTENANCE);
    }
}
