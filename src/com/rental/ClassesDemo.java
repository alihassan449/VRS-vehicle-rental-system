package com.rental;

import java.time.LocalDate;

import com.rental.controller.RentalManagementController;
import com.rental.enums.VehicleCategory;
import com.rental.enums.VehicleStatus;
import com.rental.model.Customer;
import com.rental.model.FleetStaff;
import com.rental.model.MaintenanceRecord;
import com.rental.model.Payment;
import com.rental.model.RentalAgreement;
import com.rental.model.RentalStaff;
import com.rental.model.Reservation;
import com.rental.model.Vehicle;
import com.rental.report.ReportGenerator;
import com.rental.repository.InMemoryCustomerRepository;
import com.rental.repository.InMemoryRentalAgreementRepository;
import com.rental.repository.InMemoryReservationRepository;
import com.rental.repository.InMemoryVehicleRepository;
import com.rental.repository.RentalAgreementRepository;
import com.rental.repository.VehicleRepository;
import com.rental.service.CustomerService;
import com.rental.service.ReservationService;
import com.rental.service.VehicleService;

/**
 * Console demo of the classes (no GUI needed). It walks through one complete rental and
 * prints what happens, including the cases that must be refused (wrong staff, expired
 * license, overlapping reservation, vehicle under maintenance).
 *
 * Run:  javac -d bin -sourcepath src src/com/rental/ClassesDemo.java
 *       java -cp bin com.rental.ClassesDemo
 */
public class ClassesDemo {

    public static void main(String[] args) {
        VehicleRepository vehicleRepo = new InMemoryVehicleRepository();
        RentalAgreementRepository agreementRepo = new InMemoryRentalAgreementRepository();
        RentalManagementController controller = new RentalManagementController(
                new CustomerService(new InMemoryCustomerRepository(), agreementRepo),
                new VehicleService(vehicleRepo),
                new ReservationService(new InMemoryReservationRepository(), vehicleRepo, agreementRepo),
                new ReportGenerator(vehicleRepo, agreementRepo));

        RentalStaff rental = new RentalStaff("STF-0001", "Ali Hassan");
        FleetStaff fleet = new FleetStaff("STF-0002", "Sara Khan");

        step("1. Rental staff registers customers (FR1)");
        Customer bilal = controller.registerCustomer(rental, "", "Bilal Ahmed", "Lahore", "0300-1111111",
                "35202-1111111-1", "LIC-1", LocalDate.now().plusYears(2));
        Customer expired = controller.registerCustomer(rental, "", "Old License", "Lahore", "0300-2222222",
                "35202-2222222-2", "LIC-2", LocalDate.now().minusDays(1));
        System.out.println("Registered: " + bilal + " and " + expired);

        step("2. Fleet staff registers a vehicle (FR6); rental staff is refused");
        Vehicle car = controller.registerVehicle(fleet, "LEC-123", "Toyota", "Corolla", 2022,
                VehicleCategory.SEDAN, "Automatic", "Petrol", 10000, 100);
        System.out.println("Registered: " + car);
        attempt("Rental staff registers a vehicle",
                () -> controller.registerVehicle(rental, "XYZ-1", "Honda", "Civic", 2021,
                        VehicleCategory.SEDAN, "Manual", "Petrol", 5000, 90));

        step("3. Reservation rules (FR5, FR11, FR13, FR14)");
        LocalDate pickup = LocalDate.now().plusDays(1);
        LocalDate back = LocalDate.now().plusDays(4);
        attempt("Customer with expired license reserves",
                () -> controller.createReservation(rental, expired.getCustomerID(), "LEC-123", pickup, back));
        Reservation reservation = controller.createReservation(rental, bilal.getCustomerID(), "LEC-123", pickup, back);
        System.out.println("Created " + reservation + ", estimated charge " + reservation.getEstimatedRentalCharge());
        controller.confirmReservation(rental, reservation.getReservationID());
        System.out.println("Confirmed. Vehicle status: " + car.getStatus());
        attempt("Second reservation for the same dates",
                () -> controller.createReservation(rental, bilal.getCustomerID(), "LEC-123", pickup, back));

        step("4. Handover (FR17-FR19)");
        RentalAgreement agreement = controller.handoverVehicle(rental, reservation.getReservationID(), null,
                10000, 100, "small scratch on left door");
        System.out.println("Agreement " + agreement.getAgreementID() + " created. Vehicle status: " + car.getStatus());

        step("5. Return 2 days late with extra km, less fuel and new damage (FR20-FR23)");
        String invoice = controller.returnVehicle(rental, agreement.getAgreementID(),
                back.plusDays(2).atTime(10, 0), 11000, 50, "dent; broken mirror", false);
        System.out.println(invoice);
        System.out.println("Vehicle status after return: " + car.getStatus());

        step("6. Payment and receipt (FR24, FR25)");
        Payment payment = controller.recordPayment(rental, agreement.getAgreementID(), 500, "Cash");
        System.out.println(payment.generateReceipt());

        step("7. Maintenance blocks the vehicle (FR26, FR27)");
        MaintenanceRecord record = controller.logMaintenance(fleet, "LEC-123", "Oil change", "regular service",
                50, MaintenanceRecord.IN_PROGRESS);
        System.out.println("Vehicle status: " + car.getStatus());
        attempt("Reserve a vehicle that is under maintenance",
                () -> controller.createReservation(rental, bilal.getCustomerID(), "LEC-123",
                        back.plusDays(10), back.plusDays(12)));
        controller.updateMaintenanceStatus(fleet, record.getRecordID(), MaintenanceRecord.COMPLETED);
        System.out.println("Maintenance completed. Vehicle status: " + car.getStatus());

        step("8. Reports (FR28-FR31)");
        System.out.println(controller.vehicleStatusReport(VehicleStatus.AVAILABLE));
        System.out.println(controller.customerHistoryReport(bilal.getCustomerID()));
        System.out.println(controller.vehicleHistoryReport("LEC-123"));
        System.out.println(controller.revenueReport(LocalDate.now().minusDays(1), LocalDate.now().plusDays(1)));
    }

    private static void step(String title) {
        System.out.println();
        System.out.println("##### " + title);
    }

    /** Runs an action that is expected to be refused and prints the refusal message. */
    private static void attempt(String what, Runnable action) {
        try {
            action.run();
            System.out.println("NOT REFUSED (unexpected): " + what);
        } catch (RuntimeException e) {
            System.out.println("REFUSED -> " + what + ": " + e.getMessage());
        }
    }
}
