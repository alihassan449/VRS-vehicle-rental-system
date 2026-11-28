package com.rental;

import java.time.LocalDate;

import com.rental.enums.FuelType;
import com.rental.enums.StaffRole;
import com.rental.enums.TransmissionType;
import com.rental.enums.VehicleCategory;
import com.rental.model.Address;
import com.rental.model.Customer;
import com.rental.model.DrivingLicense;
import com.rental.model.Staff;
import com.rental.model.Vehicle;
import com.rental.repository.CustomerRepository;
import com.rental.repository.VehicleRepository;
import com.rental.service.AuthService;

/** Demo accounts and initial sample data seeded when the database is empty. */
public final class DemoData {
    private DemoData() {
    }

    public static void addUsers(AuthService auth) {
        auth.addUser(new Staff("S-1", "Rental Staff", "rental", "rental123", StaffRole.RENTAL_STAFF), "rental123");
        auth.addUser(new Staff("S-2", "Fleet Staff", "fleet", "fleet123", StaffRole.FLEET_STAFF), "fleet123");
        auth.addUser(new Staff("S-3", "Administrator", "admin", "admin123", StaffRole.ADMIN), "admin123");
    }

    public static void addSampleData(CustomerRepository customers, VehicleRepository vehicles) {
        LocalDate expiry = LocalDate.now().plusYears(3);
        customers.save(new Customer("C-1001", "Ali Khan", new Address("12 Gulberg, Lahore", "", "", "", ""),
                "0300-1234567", "35202-1234567-1", new DrivingLicense("LHR-DL-1001", "", null, expiry)));
        customers.save(new Customer("C-1002", "Sara Ahmed", new Address("5 DHA Phase 5, Lahore", "", "", "", ""),
                "0321-7654321", "35202-7654321-2", new DrivingLicense("LHR-DL-1002", "", null, expiry)));
        vehicles.save(new Vehicle("LEA-1001", "Suzuki", "Alto", 2022, VehicleCategory.ECONOMY,
                TransmissionType.MANUAL, FuelType.PETROL, 15000, 40));
        vehicles.save(new Vehicle("LEB-2002", "Toyota", "Corolla", 2023, VehicleCategory.SEDAN,
                TransmissionType.AUTOMATIC, FuelType.PETROL, 8000, 70));
        vehicles.save(new Vehicle("LEC-3003", "Toyota", "Fortuner", 2022, VehicleCategory.SUV,
                TransmissionType.AUTOMATIC, FuelType.DIESEL, 21000, 130));
    }
}
