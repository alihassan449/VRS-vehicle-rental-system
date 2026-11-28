package com.rental;

import com.rental.report.ReportService;
import com.rental.service.AuthService;
import com.rental.service.CustomerService;
import com.rental.service.MaintenanceService;
import com.rental.service.PaymentService;
import com.rental.service.RentalService;
import com.rental.service.ReservationService;
import com.rental.service.SimulatedLvaService;
import com.rental.service.VehicleService;

/** Holds the wired-up services shared by all screens. */
public class AppContext {
    public final AuthService authService;
    public final SimulatedLvaService lva;
    public final CustomerService customerService;
    public final VehicleService vehicleService;
    public final ReservationService reservationService;
    public final RentalService rentalService;
    public final MaintenanceService maintenanceService;
    public final PaymentService paymentService;
    public final ReportService reportService;

    public AppContext(AuthService authService, SimulatedLvaService lva, CustomerService customerService,
                      VehicleService vehicleService, ReservationService reservationService,
                      RentalService rentalService, MaintenanceService maintenanceService,
                      PaymentService paymentService, ReportService reportService) {
        this.authService = authService;
        this.lva = lva;
        this.customerService = customerService;
        this.vehicleService = vehicleService;
        this.reservationService = reservationService;
        this.rentalService = rentalService;
        this.maintenanceService = maintenanceService;
        this.paymentService = paymentService;
        this.reportService = reportService;
    }
}
