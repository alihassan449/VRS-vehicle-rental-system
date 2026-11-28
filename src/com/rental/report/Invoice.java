package com.rental.report;

import com.rental.model.Customer;
import com.rental.model.Vehicle;
import com.rental.model.Reservation;
import com.rental.model.Charge;
import com.rental.model.Payment;
import com.rental.model.RentalAgreement;
import com.rental.util.Formats;

/** UC-10: printable rental invoice for a returned rental agreement. */
public class Invoice {
    private final RentalAgreement agreement;

    public Invoice(RentalAgreement agreement) {
        this.agreement = agreement;
    }

    public String render() {
        StringBuilder sb = new StringBuilder();
        Reservation reservation = agreement.getReservation();
        Vehicle vehicle = reservation.getVehicle();
        Customer customer = reservation.getCustomer();
        sb.append("================== RENTAL INVOICE ==================\n");
        sb.append(String.format("Agreement ID   : %s%n", agreement.getAgreementId()));
        sb.append(String.format("Customer       : %s (%s)%n", customer.getName(), customer.getCustomerId()));
        sb.append(String.format("Vehicle        : %s %s [%s]%n", vehicle.getMake(), vehicle.getModel(),
                vehicle.getRegistrationNumber()));
        sb.append(String.format("Pickup         : %s%n", Formats.dateTime(agreement.getPickupDateTime())));
        sb.append(String.format("Return         : %s%n", Formats.dateTime(agreement.getReturnDateTime())));
        sb.append("----------------------------------------------------\n");
        sb.append(String.format("%-34s %12.2f%n", "Base rental charge (" + reservation.getRentalDays()
                + " day(s) x " + String.format("%.2f", vehicle.getDailyRentalRate()) + ")", agreement.getBaseCharge()));
        for (Charge c : agreement.getCharges()) {
            sb.append(String.format("%-34s %12.2f%n", c.getDescription(), c.getAmount()));
        }
        sb.append("----------------------------------------------------\n");
        sb.append(String.format("%-34s %12.2f%n", "TOTAL PAYABLE", agreement.getFinalAmount()));
        for (Payment p : agreement.getPayments()) {
            sb.append(String.format("%-34s %12.2f%n", "Paid " + p.getPaymentDate() + " (" + p.getMethod() + ")",
                    p.getAmountPaid()));
        }
        sb.append(String.format("%-34s %12.2f%n", "OUTSTANDING BALANCE", agreement.balanceDue()));
        sb.append("====================================================\n");
        return sb.toString();
    }
}
