package com.rental.report;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.rental.enums.VehicleStatus;
import com.rental.exception.BusinessRuleException;
import com.rental.exception.ValidationException;
import com.rental.model.Customer;
import com.rental.model.MaintenanceRecord;
import com.rental.model.Payment;
import com.rental.model.RentalAgreement;
import com.rental.model.Vehicle;
import com.rental.repository.PaymentRepository;
import com.rental.repository.RentalAgreementRepository;
import com.rental.repository.VehicleRepository;
import com.rental.service.CustomerService;
import com.rental.util.Formats;

/**
 * UC-12 Generate System Reports: UC-12.1 Vehicle Status, UC-12.2 Customer Rental
 * History, UC-12.3 Vehicle History and UC-12.4 Revenue report.
 */
public class ReportService {
    public static final String NO_RECORDS = "No records found.";
    public static final VehicleStatus[] REPORT_STATUSES = {
            VehicleStatus.AVAILABLE, VehicleStatus.RESERVED, VehicleStatus.RENTED, VehicleStatus.UNDER_MAINTENANCE };

    private final VehicleRepository vehicleRepository;
    private final RentalAgreementRepository agreementRepository;
    private final PaymentRepository paymentRepository;
    private final CustomerService customerService;

    public ReportService(VehicleRepository vehicleRepository, RentalAgreementRepository agreementRepository,
                         PaymentRepository paymentRepository, CustomerService customerService) {
        this.vehicleRepository = vehicleRepository;
        this.agreementRepository = agreementRepository;
        this.paymentRepository = paymentRepository;
        this.customerService = customerService;
    }

    /** UC-12.1: vehicles by status; grouped by status when no filter is chosen. */
    public String vehicleStatusReport(VehicleStatus filter) {
        StringBuilder sb = new StringBuilder("VEHICLE STATUS REPORT\n\n");
        int total = 0;
        for (VehicleStatus status : REPORT_STATUSES) {
            if (filter != null && filter != status) {
                continue;
            }
            List<Vehicle> vehicles = vehicleRepository.findByStatus(status);
            if (filter == null && vehicles.isEmpty()) {
                continue;
            }
            sb.append(String.format("== %s (%d) ==%n", status, vehicles.size()));
            for (Vehicle v : vehicles) {
                sb.append(String.format("  %-12s %-12s %-14s %-10s %s%n", v.getRegistrationNumber(), v.getMake(),
                        v.getModel(), v.getCategory(), v.getStatus()));
            }
            sb.append('\n');
            total += vehicles.size();
        }
        return total == 0 ? NO_RECORDS : sb.toString();
    }

    /** UC-12.2: all rental agreements of one customer (ID, National ID or Name). */
    public String customerRentalHistoryReport(String customerKey) {
        if (customerKey == null || customerKey.trim().isEmpty()) {
            throw new ValidationException("Enter a Customer ID, National ID or Name.");
        }
        List<Customer> matches = customerService.searchCustomers(customerKey);
        if (matches.isEmpty()) {
            throw new BusinessRuleException("Record Not Found: the selected customer does not exist. "
                    + "Please enter a valid Customer ID, National ID or Name.");
        }
        if (matches.size() > 1) {
            throw new BusinessRuleException("Several customers match '" + customerKey.trim()
                    + "'. Please enter the Customer ID or National ID.");
        }
        Customer customer = matches.get(0);
        List<RentalAgreement> agreements = agreementRepository.findByCustomerId(customer.getCustomerId());
        if (agreements.isEmpty()) {
            return NO_RECORDS;
        }
        StringBuilder sb = new StringBuilder("CUSTOMER RENTAL HISTORY REPORT\n");
        sb.append(String.format("Customer: %s (%s)%n%n", customer.getName(), customer.getCustomerId()));
        sb.append(String.format("%-10s %-12s %-17s %-17s %12s  %s%n", "Agreement", "Vehicle", "Pickup", "Return",
                "Amount", "Status"));
        for (RentalAgreement a : agreements) {
            sb.append(agreementRow(a, true));
        }
        return sb.toString();
    }

    /** UC-12.3: rental agreements and maintenance records of one vehicle. */
    public String vehicleHistoryReport(String registrationNumber) {
        if (registrationNumber == null || registrationNumber.trim().isEmpty()) {
            throw new ValidationException("Enter a vehicle registration number.");
        }
        Vehicle vehicle = vehicleRepository.findByRegistrationNumber(registrationNumber.trim()).orElse(null);
        if (vehicle == null) {
            throw new BusinessRuleException("Record Not Found: the selected vehicle does not exist. "
                    + "Please enter a valid registration number.");
        }
        List<RentalAgreement> agreements = agreementRepository.findByVehicle(vehicle);
        List<MaintenanceRecord> maintenance = vehicle.getMaintenanceHistory();
        if (agreements.isEmpty() && maintenance.isEmpty()) {
            return NO_RECORDS;
        }
        StringBuilder sb = new StringBuilder("VEHICLE HISTORY REPORT\n");
        sb.append(String.format("Vehicle: %s %s %s (%s)%n%n", vehicle.getRegistrationNumber(), vehicle.getMake(),
                vehicle.getModel(), vehicle.getStatus()));
        sb.append("Rental agreements\n");
        if (agreements.isEmpty()) {
            sb.append("  (none)\n");
        } else {
            sb.append(String.format("%-10s %-12s %-17s %-17s %12s  %s%n", "Agreement", "Customer", "Pickup",
                    "Return", "Amount", "Status"));
            for (RentalAgreement a : agreements) {
                sb.append(agreementRow(a, false));
            }
        }
        sb.append("\nMaintenance records\n");
        if (maintenance.isEmpty()) {
            sb.append("  (none)\n");
        } else {
            sb.append(String.format("%-9s %-12s %-12s %10s  %-12s %s%n", "Record", "Type", "Date", "Cost", "Status",
                    "Description"));
            for (MaintenanceRecord m : maintenance) {
                sb.append(String.format("%-9s %-12s %-12s %10.2f  %-12s %s%n", m.getRecordId(), m.getMaintenanceType(),
                        m.getDate(), m.getCost(), m.getCompletionStatus(), m.getDescription()));
            }
        }
        return sb.toString();
    }

    /** UC-12.4: payments recorded in the date range and the total revenue. */
    public String revenueReport(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new ValidationException("Start date and end date are required (yyyy-MM-dd).");
        }
        if (to.isBefore(from)) {
            throw new ValidationException("Invalid date range: the end date is earlier than the start date.");
        }
        List<Payment> payments = new ArrayList<>(paymentRepository.findByDateRange(from, to));
        if (payments.isEmpty()) {
            return NO_RECORDS;
        }
        StringBuilder sb = new StringBuilder("REVENUE REPORT\n");
        sb.append(String.format("Period: %s to %s%n%n", from, to));
        sb.append(String.format("%-9s %-10s %-12s %-14s %12s%n", "Payment", "Agreement", "Date", "Method", "Amount"));
        double total = 0;
        for (Payment p : payments) {
            sb.append(String.format("%-9s %-10s %-12s %-14s %12.2f%n", p.getPaymentId(),
                    p.getRentalAgreement().getAgreementId(), p.getPaymentDate(), p.getMethod(), p.getAmountPaid()));
            total += p.getAmountPaid();
        }
        sb.append(String.format("%nTOTAL REVENUE: %.2f (%d payment(s))%n", total, payments.size()));
        return sb.toString();
    }

    /** One agreement line; the second column shows the vehicle (customer report) or the customer (vehicle report). */
    private String agreementRow(RentalAgreement a, boolean showVehicle) {
        String second = showVehicle ? a.getReservation().getVehicle().getRegistrationNumber()
                : a.getReservation().getCustomer().getCustomerId();
        return String.format("%-10s %-12s %-17s %-17s %12s  %s%n", a.getAgreementId(), second,
                Formats.dateTime(a.getPickupDateTime()), Formats.dateTime(a.getReturnDateTime()),
                a.isReturned() ? String.format("%.2f", a.getFinalAmount()) : "-", a.getStatus());
    }

    public java.util.List<Object[]> vehicleStatusRows(VehicleStatus filter) {
        java.util.List<Object[]> rows = new java.util.ArrayList<>();
        for (VehicleStatus status : REPORT_STATUSES) {
            if (filter != null && filter != status) continue;
            for (Vehicle v : vehicleRepository.findByStatus(status)) {
                rows.add(new Object[]{v.getRegistrationNumber(), v.getMake(), v.getModel(), v.getCategory(), v.getStatus()});
            }
        }
        return rows;
    }

    public java.util.List<Object[]> customerHistoryRows(String customerKey) {
        java.util.List<Object[]> rows = new java.util.ArrayList<>();
        if (customerKey == null || customerKey.trim().isEmpty()) throw new ValidationException("Enter a Customer ID, National ID or Name.");
        java.util.List<Customer> matches = customerService.searchCustomers(customerKey);
        if (matches.isEmpty()) throw new BusinessRuleException("Customer not found.");
        if (matches.size() > 1) throw new BusinessRuleException("Several customers match. Enter Customer ID or National ID.");
        for (RentalAgreement a : agreementRepository.findByCustomerId(matches.get(0).getCustomerId())) {
            rows.add(new Object[]{a.getAgreementId(), a.getReservation().getVehicle().getRegistrationNumber(),
                    Formats.dateTime(a.getPickupDateTime()), Formats.dateTime(a.getReturnDateTime()),
                    a.isReturned() ? String.format("%.2f", a.getFinalAmount()) : "-", a.getStatus()});
        }
        return rows;
    }

    public java.util.List<Object[]> vehicleHistoryRows(String registrationNumber) {
        java.util.List<Object[]> rows = new java.util.ArrayList<>();
        if (registrationNumber == null || registrationNumber.trim().isEmpty()) throw new ValidationException("Enter a vehicle registration number.");
        Vehicle vehicle = vehicleRepository.findByRegistrationNumber(registrationNumber.trim()).orElse(null);
        if (vehicle == null) throw new BusinessRuleException("Vehicle not found.");
        for (RentalAgreement a : agreementRepository.findByVehicle(vehicle)) {
            rows.add(new Object[]{"Rental", a.getAgreementId(), a.getReservation().getCustomer().getCustomerId(),
                    Formats.dateTime(a.getPickupDateTime()), Formats.dateTime(a.getReturnDateTime()),
                    a.isReturned() ? String.format("%.2f", a.getFinalAmount()) : "-", a.getStatus()});
        }
        for (MaintenanceRecord m : vehicle.getMaintenanceHistory()) {
            rows.add(new Object[]{"Maintenance", m.getRecordId(), "-", m.getDate(), "-",
                    String.format("%.2f", m.getCost()), m.getCompletionStatus() + " · " + m.getMaintenanceType()});
        }
        return rows;
    }

    public java.util.List<Object[]> revenueRows(LocalDate from, LocalDate to) {
        java.util.List<Object[]> rows = new java.util.ArrayList<>();
        if (from == null || to == null) throw new ValidationException("Start date and end date are required.");
        if (to.isBefore(from)) throw new ValidationException("End date is earlier than start date.");
        for (Payment p : paymentRepository.findByDateRange(from, to)) {
            rows.add(new Object[]{p.getPaymentId(), p.getRentalAgreement().getAgreementId(), p.getPaymentDate(),
                    p.getMethod(), String.format("%.2f", p.getAmountPaid())});
        }
        return rows;
    }
}
