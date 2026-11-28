package com.rental.report;

import java.time.LocalDate;
import java.util.List;

import com.rental.enums.VehicleStatus;
import com.rental.model.MaintenanceRecord;
import com.rental.model.Payment;
import com.rental.model.RentalAgreement;
import com.rental.model.Vehicle;
import com.rental.repository.RentalAgreementRepository;
import com.rental.repository.VehicleRepository;

/**
 * Diagram class: ReportGenerator (FR28-FR31).
 * Dates are java.time.LocalDate (UML "Date").
 */
public class ReportGenerator {

    private final VehicleRepository vehicleRepository;
    private final RentalAgreementRepository agreementRepository;

    public ReportGenerator(VehicleRepository vehicleRepository, RentalAgreementRepository agreementRepository) {
        this.vehicleRepository = vehicleRepository;
        this.agreementRepository = agreementRepository;
    }

    /** FR28: vehicles by status. A null status gives all four required groups. */
    public Report generateVehicleStatusReport(VehicleStatus status) {
        VehicleStatus[] groups = status == null
                ? new VehicleStatus[]{VehicleStatus.AVAILABLE, VehicleStatus.RESERVED,
                        VehicleStatus.RENTED, VehicleStatus.UNDER_MAINTENANCE}
                : new VehicleStatus[]{status};
        Report report = new Report("VEHICLE STATUS REPORT");
        for (VehicleStatus group : groups) {
            int count = 0;
            report.addLine("--- " + group + " ---");
            for (Vehicle v : vehicleRepository.findAll()) {
                if (v.getStatus() == group) {
                    report.addLine(String.format("%s | %s %s (%d) | %s | %.1f km | %.2f/day",
                            v.getRegistrationNumber(), v.getMake(), v.getModel(), v.getYear(),
                            v.getCategory(), v.getCurrentMileage(), v.getDailyRentalRate()));
                    count++;
                }
            }
            if (count == 0) {
                report.addLine("(no vehicles)");
            }
            report.addLine("Total " + group + ": " + count);
            report.addLine("");
        }
        return report;
    }

    /** FR29: rental history of one customer. */
    public Report generateCustomerHistoryReport(String customerID) {
        Report report = new Report("CUSTOMER RENTAL HISTORY - " + customerID);
        List<RentalAgreement> agreements = agreementRepository.findByCustomerId(customerID);
        if (agreements.isEmpty()) {
            report.addLine("No rental agreements found for this customer.");
            return report;
        }
        report.addLine("Customer: " + agreements.get(0).getCustomer().getName());
        report.addLine("");
        double total = 0;
        for (RentalAgreement a : agreements) {
            report.addLine(String.format("%s | %s | picked up %s | returned %s | total %.2f | paid %.2f",
                    a.getAgreementID(), a.getVehicle().getRegistrationNumber(),
                    a.getPickupDateTime(), a.getActualReturnDateTime() == null ? "-" : a.getActualReturnDateTime(),
                    a.getTotalAmount(), a.totalPaid()));
            total += a.getTotalAmount();
        }
        report.addLine("");
        report.addLine("Agreements: " + agreements.size() + " | Total billed: " + String.format("%.2f", total));
        return report;
    }

    /** FR30: rental history and maintenance history of one vehicle. */
    public Report generateVehicleHistoryReport(String regNo) {
        Report report = new Report("VEHICLE HISTORY - " + regNo);
        Vehicle vehicle = vehicleRepository.findByRegistrationNumber(regNo).orElse(null);
        if (vehicle == null) {
            report.addLine("No vehicle found with this registration number.");
            return report;
        }
        report.addLine(vehicle.toString());
        report.addLine("");
        report.addLine("--- Rental history ---");
        List<RentalAgreement> agreements = agreementRepository.findByVehicle(regNo);
        if (agreements.isEmpty()) {
            report.addLine("(no rentals)");
        }
        for (RentalAgreement a : agreements) {
            report.addLine(String.format("%s | %s | picked up %s | returned %s | total %.2f",
                    a.getAgreementID(), a.getCustomer().getName(), a.getPickupDateTime(),
                    a.getActualReturnDateTime() == null ? "-" : a.getActualReturnDateTime(), a.getTotalAmount()));
        }
        report.addLine("");
        report.addLine("--- Maintenance history ---");
        double maintenanceCost = 0;
        if (vehicle.getMaintenanceHistory().isEmpty()) {
            report.addLine("(no maintenance records)");
        }
        for (MaintenanceRecord m : vehicle.getMaintenanceHistory()) {
            report.addLine(String.format("%s | %s | %s | %s | cost %.2f | %s", m.getRecordID(), m.getDate(),
                    m.getMaintenanceType(), m.getDescription(), m.getCost(), m.getCompletionStatus()));
            maintenanceCost += m.getCost();
        }
        report.addLine("");
        report.addLine("Total maintenance cost: " + String.format("%.2f", maintenanceCost));
        return report;
    }

    /** FR31: revenue (payments received) between two dates, both inclusive. */
    public Report generateRevenueReport(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("Please enter a valid date range");
        }
        Report report = new Report("REVENUE REPORT " + startDate + " to " + endDate);
        double total = 0;
        int count = 0;
        for (RentalAgreement a : agreementRepository.findAll()) {
            for (Payment p : a.getPayments()) {
                if (!p.getPaymentDate().isBefore(startDate) && !p.getPaymentDate().isAfter(endDate)) {
                    report.addLine(String.format("%s | %s | agreement %s | %s | %s | %.2f", p.getPaymentID(),
                            p.getPaymentDate(), a.getAgreementID(), a.getCustomer().getName(),
                            p.getPaymentMethod(), p.getAmountPaid()));
                    total += p.getAmountPaid();
                    count++;
                }
            }
        }
        if (count == 0) {
            report.addLine("(no payments in this period)");
        }
        report.addLine("");
        report.addLine("Payments: " + count);
        report.addLine("TOTAL REVENUE: " + String.format("%.2f", total));
        return report;
    }
}
