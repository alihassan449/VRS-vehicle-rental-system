package com.rental;

import com.rental.controller.RentalManagementController;
import com.rental.report.ReportGenerator;
import com.rental.repository.CustomerRepository;
import com.rental.repository.InMemoryCustomerRepository;
import com.rental.repository.InMemoryRentalAgreementRepository;
import com.rental.repository.InMemoryReservationRepository;
import com.rental.repository.InMemoryStaffRepository;
import com.rental.repository.InMemoryVehicleRepository;
import com.rental.repository.RentalAgreementRepository;
import com.rental.repository.ReservationRepository;
import com.rental.repository.StaffRepository;
import com.rental.repository.VehicleRepository;
import com.rental.repository.sqlserver.SqlServerConfig;
import com.rental.repository.sqlserver.SqlServerCustomerRepository;
import com.rental.repository.sqlserver.SqlServerDatabase;
import com.rental.repository.sqlserver.SqlServerRentalAgreementRepository;
import com.rental.repository.sqlserver.SqlServerReservationRepository;
import com.rental.repository.sqlserver.SqlServerStaffRepository;
import com.rental.repository.sqlserver.SqlServerVehicleRepository;
import com.rental.service.CustomerService;
import com.rental.service.ReservationService;
import com.rental.service.VehicleService;

/**
 * Builds the whole application object graph (repositories -> services -> controller) in one
 * place, either on SQL Server or completely in memory (for the console demo / tests).
 * The GUI only needs {@link #getController()} and {@link #getStaffRepository()} (for login).
 */
public class AppContext {

    private final RentalManagementController controller;
    private final StaffRepository staffRepository;
    private final String storageName;

    private AppContext(CustomerRepository customers, VehicleRepository vehicles,
                       ReservationRepository reservations, RentalAgreementRepository agreements,
                       StaffRepository staff, String storageName) {
        CustomerService customerService = new CustomerService(customers, agreements);
        VehicleService vehicleService = new VehicleService(vehicles);
        ReservationService reservationService = new ReservationService(reservations, vehicles, agreements);
        ReportGenerator reportGenerator = new ReportGenerator(vehicles, agreements);
        this.controller = new RentalManagementController(
                customerService, vehicleService, reservationService, reportGenerator);
        this.staffRepository = staff;
        this.storageName = storageName;
    }

    /** All data in memory (lost when the program closes). The three default accounts are created. */
    public static AppContext inMemory() {
        InMemoryStaffRepository staff = new InMemoryStaffRepository();
        InMemoryStaffRepository.seedDefaultAccounts(staff);
        return new AppContext(new InMemoryCustomerRepository(), new InMemoryVehicleRepository(),
                new InMemoryReservationRepository(), new InMemoryRentalAgreementRepository(),
                staff, "In-memory (data is lost on exit)");
    }

    /**
     * Data stored in SQL Server. Creates the database and tables on first use, loads all stored
     * data, and creates the three default accounts if the Staff table is empty.
     * @throws IllegalStateException if the database cannot be reached
     */
    public static AppContext sqlServer(SqlServerConfig config) {
        SqlServerDatabase db = new SqlServerDatabase(config);
        db.connectAndCreateSchema();
        SqlServerDatabase.LoadedData data = db.loadAll();

        SqlServerCustomerRepository customers = new SqlServerCustomerRepository(db);
        SqlServerVehicleRepository vehicles = new SqlServerVehicleRepository(db);
        SqlServerReservationRepository reservations = new SqlServerReservationRepository(db);
        SqlServerRentalAgreementRepository agreements = new SqlServerRentalAgreementRepository(db);
        SqlServerStaffRepository staff = new SqlServerStaffRepository(db);

        data.customers.values().forEach(customers::cache);
        data.vehicles.values().forEach(vehicles::cache);
        data.reservations.values().forEach(reservations::cache);
        data.agreements.values().forEach(agreements::cache);
        data.staff.forEach((id, s) -> staff.cache(s, data.accounts.get(id)[0], data.accounts.get(id)[1]));

        if (staff.isEmpty()) {
            InMemoryStaffRepository.seedDefaultAccounts(staff);
        }
        return new AppContext(customers, vehicles, reservations, agreements, staff,
                "SQL Server database " + config.getDatabase());
    }

    public RentalManagementController getController() {
        return controller;
    }

    public StaffRepository getStaffRepository() {
        return staffRepository;
    }

    /** Text shown in the window (which storage is in use). */
    public String getStorageName() {
        return storageName;
    }
}
