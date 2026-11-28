package com.rental;

import java.util.Arrays;
import java.util.List;
import javax.swing.SwingUtilities;

import com.rental.model.Staff;
import com.rental.report.ReportService;
import com.rental.repository.CustomerRepository;
import com.rental.repository.InMemoryCustomerRepository;
import com.rental.repository.InMemoryMaintenanceRepository;
import com.rental.repository.InMemoryPaymentRepository;
import com.rental.repository.InMemoryRentalAgreementRepository;
import com.rental.repository.InMemoryReservationRepository;
import com.rental.repository.InMemoryVehicleRepository;
import com.rental.repository.MaintenanceRepository;
import com.rental.repository.PaymentRepository;
import com.rental.repository.RentalAgreementRepository;
import com.rental.repository.ReservationRepository;
import com.rental.repository.VehicleRepository;
import com.rental.repository.db.SqliteCustomerRepository;
import com.rental.repository.db.SqliteDatabase;
import com.rental.repository.db.SqliteMaintenanceRepository;
import com.rental.repository.db.SqlitePaymentRepository;
import com.rental.repository.db.SqliteRentalAgreementRepository;
import com.rental.repository.db.SqliteReservationRepository;
import com.rental.repository.db.SqliteStaffRepository;
import com.rental.repository.db.SqliteVehicleRepository;
import com.rental.service.AuthService;
import com.rental.service.ChargeCalculator;
import com.rental.service.CustomerService;
import com.rental.service.MaintenanceService;
import com.rental.service.PaymentService;
import com.rental.service.RentalService;
import com.rental.service.ReservationService;
import com.rental.service.SimulatedLvaService;
import com.rental.service.VehicleService;
import com.rental.ui.MainFrame;
import com.rental.ui.auth.LoginDialog;
import com.rental.ui.common.UiTheme;

/**
 * Application entry point and composition root (SQLite DB repositories -> services -> UI).
 * Flags:
 * <ul>
 *   <li>{@code --empty}: skip seeding demo customers/vehicles when the database is empty.</li>
 *   <li>{@code --in-memory}: use transient in-memory repositories instead of SQLite.</li>
 * </ul>
 */
public class Main {
    public static void main(String[] args) {
        List<String> argList = Arrays.asList(args);
        boolean empty = argList.contains("--empty");
        boolean inMemory = argList.contains("--in-memory");

        CustomerRepository customerRepository;
        VehicleRepository vehicleRepository;
        ReservationRepository reservationRepository;
        RentalAgreementRepository agreementRepository;
        MaintenanceRepository maintenanceRepository;
        PaymentRepository paymentRepository;
        AuthService authService;

        if (!inMemory && isSqliteDriverAvailable()) {
            SqliteDatabase db = new SqliteDatabase();
            SqliteStaffRepository staffRepo = new SqliteStaffRepository(db);
            customerRepository = new SqliteCustomerRepository(db);
            vehicleRepository = new SqliteVehicleRepository(db);
            reservationRepository = new SqliteReservationRepository(db, customerRepository, vehicleRepository);
            agreementRepository = new SqliteRentalAgreementRepository(db, reservationRepository);
            maintenanceRepository = new SqliteMaintenanceRepository(db, vehicleRepository);
            paymentRepository = new SqlitePaymentRepository(db, agreementRepository);

            authService = new AuthService(staffRepo);
            if (staffRepo.isEmpty()) {
                DemoData.addUsers(authService);
            }
            if (!empty && db.isEmptyDatabase()) {
                DemoData.addSampleData(customerRepository, vehicleRepository);
            }
        } else {
            if (!inMemory) {
                System.err.println("SQLite JDBC driver not found on classpath; running with in-memory storage. "
                        + "Include lib/* on classpath for persistent SQLite storage.");
            }
            customerRepository = new InMemoryCustomerRepository();
            vehicleRepository = new InMemoryVehicleRepository();
            reservationRepository = new InMemoryReservationRepository();
            agreementRepository = new InMemoryRentalAgreementRepository();
            maintenanceRepository = new InMemoryMaintenanceRepository();
            paymentRepository = new InMemoryPaymentRepository();
            authService = new AuthService();
            DemoData.addUsers(authService);
            if (!empty) {
                DemoData.addSampleData(customerRepository, vehicleRepository);
            }
        }

        SimulatedLvaService lva = new SimulatedLvaService();
        CustomerService customerService = new CustomerService(customerRepository, lva);
        AppContext ctx = new AppContext(
                authService,
                lva,
                customerService,
                new VehicleService(vehicleRepository),
                new ReservationService(reservationRepository, vehicleRepository, agreementRepository),
                new RentalService(agreementRepository, vehicleRepository, reservationRepository, lva, new ChargeCalculator()),
                new MaintenanceService(maintenanceRepository, vehicleRepository, agreementRepository),
                new PaymentService(paymentRepository),
                new ReportService(vehicleRepository, agreementRepository, paymentRepository, customerService));

        UiTheme.applyLookAndFeel();
        SwingUtilities.invokeLater(() -> start(ctx));
    }

    private static boolean isSqliteDriverAvailable() {
        try {
            Class.forName("org.sqlite.JDBC");
            return true;
        } catch (ClassNotFoundException ex) {
            return false;
        }
    }

    /** Shows the login; after a successful login opens the window for the user's role. */
    private static void start(AppContext ctx) {
        Staff user = LoginDialog.showLogin(ctx.authService);
        if (user == null) {
            System.exit(0);
        }
        new MainFrame(ctx, user, () -> start(ctx)).setVisible(true);
    }
}
