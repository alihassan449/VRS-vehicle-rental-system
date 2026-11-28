package com.rental.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.rental.enums.VehicleCategory;
import com.rental.enums.VehicleStatus;
import com.rental.model.Customer;
import com.rental.model.MaintenanceRecord;
import com.rental.model.Payment;
import com.rental.model.RentalAgreement;
import com.rental.model.Reservation;
import com.rental.model.Staff;
import com.rental.model.Vehicle;
import com.rental.report.Report;
import com.rental.report.ReportGenerator;
import com.rental.service.CustomerService;
import com.rental.service.ReservationService;
import com.rental.service.VehicleService;

/**
 * Diagram class: RentalManagementController.
 *
 * Single entry point used by the Swing screens. It holds the four collaborators shown in the
 * diagram (CustomerService, VehicleService, ReservationService, ReportGenerator) and passes
 * every request to the right one. Every operation that changes data receives the logged-in
 * Staff object, and the staff object decides whether the action is allowed
 * (RentalStaff, FleetStaff or AdminStaff) - so authorisation lives in the domain.
 */
public class RentalManagementController {

    private final CustomerService customerService;
    private final VehicleService vehicleService;
    private final ReservationService reservationService;
    private final ReportGenerator reportGenerator;

    public RentalManagementController(CustomerService customerService, VehicleService vehicleService,
                                      ReservationService reservationService, ReportGenerator reportGenerator) {
        this.customerService = customerService;
        this.vehicleService = vehicleService;
        this.reservationService = reservationService;
        this.reportGenerator = reportGenerator;
    }

    // ================================================================ customers (rental staff)

    public Customer registerCustomer(Staff staff, String customerID, String name, String address,
                                     String contactNumber, String nationalID, String drivingLicenseNo,
                                     LocalDate licenseExpiryDate) {
        staff.requireRentalAccess();
        return customerService.registerCustomer(customerID, name, address, contactNumber, nationalID,
                drivingLicenseNo, licenseExpiryDate);
    }

    public void updateCustomerContact(Staff staff, String customerID, String address, String contactNumber) {
        staff.requireRentalAccess();
        customerService.updateContactInfo(customerID, address, contactNumber);
    }

    public List<Customer> searchCustomers(String query) {
        return customerService.searchCustomer(query);
    }

    public List<Customer> listCustomers() {
        return customerService.findAll();
    }

    public List<RentalAgreement> getRentalHistory(String customerID) {
        return customerService.getRentalHistory(customerID);
    }

    // ================================================================ vehicles (fleet staff)

    public Vehicle registerVehicle(Staff staff, String registrationNumber, String make, String model, int year,
                                   VehicleCategory category, String transmissionType, String fuelType,
                                   double currentMileage, double dailyRentalRate) {
        staff.requireFleetAccess();
        return vehicleService.registerVehicle(registrationNumber, make, model, year, category,
                transmissionType, fuelType, currentMileage, dailyRentalRate);
    }

    public void setVehicleInactive(Staff staff, String registrationNumber, boolean inactive) {
        staff.requireFleetAccess();
        Vehicle vehicle = vehicleService.findByRegistration(registrationNumber);
        staff.setVehicleInactive(vehicle, inactive);
        vehicleService.save(vehicle);
    }

    public List<Vehicle> searchVehicles(String regNo, VehicleCategory category, VehicleStatus status) {
        return vehicleService.searchVehicles(regNo, category, status);
    }

    // ================================================================ maintenance (fleet staff)

    public MaintenanceRecord logMaintenance(Staff staff, String registrationNumber, String type,
                                            String description, double cost, String completionStatus) {
        staff.requireFleetAccess();
        Vehicle vehicle = vehicleService.findByRegistration(registrationNumber);
        MaintenanceRecord record = staff.logMaintenance(vehicle, type, description, cost, completionStatus);
        vehicleService.save(vehicle);
        return record;
    }

    public void updateMaintenanceStatus(Staff staff, String recordID, String completionStatus) {
        staff.requireFleetAccess();
        MaintenanceRecord record = null;
        for (MaintenanceRecord r : vehicleService.findAllMaintenanceRecords()) {
            if (r.getRecordID().equals(recordID)) {
                record = r;
            }
        }
        if (record == null) {
            throw new IllegalArgumentException("No maintenance record with ID " + recordID);
        }
        staff.updateMaintenanceStatus(record, completionStatus);
        vehicleService.save(record.getVehicle());
    }

    public List<MaintenanceRecord> listMaintenanceRecords() {
        return vehicleService.findAllMaintenanceRecords();
    }

    public List<MaintenanceRecord> getMaintenanceHistory(String registrationNumber) {
        return vehicleService.getMaintenanceHistory(registrationNumber);
    }

    // ================================================================ reservations (rental staff)

    public Reservation createReservation(Staff staff, String customerID, String registrationNumber,
                                         LocalDate pickup, LocalDate returnDate) {
        staff.requireRentalAccess();
        Customer customer = customerService.findById(customerID);
        Vehicle vehicle = vehicleService.findByRegistration(registrationNumber);
        return reservationService.createReservation(customer, vehicle, pickup, returnDate);
    }

    public void confirmReservation(Staff staff, String reservationID) {
        Reservation reservation = reservationService.findById(reservationID);
        staff.processReservation(reservation);
        reservationService.save(reservation);
    }

    public void cancelReservation(Staff staff, String reservationID) {
        staff.requireRentalAccess();
        reservationService.cancelReservation(reservationID);
    }

    public void expireReservation(Staff staff, String reservationID) {
        staff.requireRentalAccess();
        reservationService.expireReservation(reservationID);
    }

    public List<Vehicle> findAvailableVehicles(LocalDate pickup, LocalDate returnDate) {
        return reservationService.findAvailableVehicles(pickup, returnDate);
    }

    public List<Reservation> listReservations() {
        return reservationService.findAll();
    }

    // ================================================================ rentals (rental staff)

    /** FR17-FR19. A null pickupDateTime means "now". */
    public RentalAgreement handoverVehicle(Staff staff, String reservationID, LocalDateTime pickupDateTime,
                                           double startingMileage, double pickupFuelLevel, String existingDamage) {
        Reservation reservation = reservationService.findById(reservationID);
        LocalDateTime when = pickupDateTime == null ? LocalDateTime.now() : pickupDateTime;
        staff.processHandover(reservation, when, startingMileage, pickupFuelLevel, existingDamage);
        RentalAgreement agreement = reservation.getRentalAgreement();
        reservationService.saveAgreement(agreement);
        reservationService.save(reservation);
        return agreement;
    }

    /** FR20-FR23. A null returnDateTime means "now". Returns the printable invoice. */
    public String returnVehicle(Staff staff, String agreementID, LocalDateTime returnDateTime,
                                double endingMileage, double returnFuelLevel, String newDamage,
                                boolean maintenanceRequired) {
        RentalAgreement agreement = reservationService.findAgreementById(agreementID);
        LocalDateTime when = returnDateTime == null ? LocalDateTime.now() : returnDateTime;
        staff.processReturn(agreement, when, endingMileage, returnFuelLevel, newDamage, maintenanceRequired);
        reservationService.saveAgreement(agreement);
        return agreement.generateInvoice();
    }

    public String getInvoice(String agreementID) {
        return reservationService.findAgreementById(agreementID).generateInvoice();
    }

    public List<RentalAgreement> listAgreements() {
        return reservationService.findAllAgreements();
    }

    // ================================================================ payments (rental staff)

    public Payment recordPayment(Staff staff, String agreementID, double amount, String paymentMethod) {
        staff.requireRentalAccess();
        RentalAgreement agreement = reservationService.findAgreementById(agreementID);
        Payment payment = staff.recordPayment(agreement, amount, paymentMethod);
        reservationService.saveAgreement(agreement);
        return payment;
    }

    // ================================================================ reports (all staff)

    public Report vehicleStatusReport(VehicleStatus status) {
        return reportGenerator.generateVehicleStatusReport(status);
    }

    public Report customerHistoryReport(String customerID) {
        return reportGenerator.generateCustomerHistoryReport(customerID);
    }

    public Report vehicleHistoryReport(String registrationNumber) {
        return reportGenerator.generateVehicleHistoryReport(registrationNumber);
    }

    public Report revenueReport(LocalDate startDate, LocalDate endDate) {
        return reportGenerator.generateRevenueReport(startDate, endDate);
    }
}
