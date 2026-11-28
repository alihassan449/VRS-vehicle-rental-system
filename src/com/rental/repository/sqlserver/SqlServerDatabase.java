package com.rental.repository.sqlserver;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import com.rental.enums.ReservationStatus;
import com.rental.enums.StaffRole;
import com.rental.enums.VehicleCategory;
import com.rental.enums.VehicleStatus;
import com.rental.model.Customer;
import com.rental.model.MaintenanceRecord;
import com.rental.model.Payment;
import com.rental.model.RentalAgreement;
import com.rental.model.Reservation;
import com.rental.model.Staff;
import com.rental.model.StaffFactory;
import com.rental.model.Vehicle;
import com.rental.util.IdGenerator;

/**
 * All SQL for the application in one place: creates the database and tables, loads everything
 * into the in-memory objects at start-up, and saves objects (UPDATE, or INSERT if the row is new).
 *
 * The program uses only the standard java.sql interfaces. At run time the Microsoft JDBC
 * driver jar (mssql-jdbc-...jre11.jar) must be on the classpath (see README_DATABASE.md).
 *
 * Every save runs in one transaction (all rows or none). A save of a reservation / agreement
 * also saves the customer, vehicle (with its maintenance records) it depends on, so the object
 * changes made before the save (e.g. vehicle status RENTED) are never lost.
 */
public class SqlServerDatabase {

    private final SqlServerConfig config;
    private Connection connection;

    public SqlServerDatabase(SqlServerConfig config) {
        this.config = config;
    }

    // ------------------------------------------------------------------ connection / schema

    /** Creates the database (if missing), opens the connection and creates the tables (if missing). */
    public void connectAndCreateSchema() {
        try {
            try (Connection master = open(config.masterUrl());
                 Statement st = master.createStatement()) {
                st.execute("IF DB_ID(N'" + config.getDatabase() + "') IS NULL CREATE DATABASE ["
                        + config.getDatabase() + "]");
            }
            connection = open(config.url());
            createTables();
        } catch (SQLException e) {
            String hint = "";
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("integrated authentication")) {
                hint = "\n\nWindows login needs the file mssql-jdbc_auth-...x64.dll in the lib folder "
                        + "and the program must be started with -Djava.library.path=lib (see README_DATABASE.md).";
            }
            throw new IllegalStateException("Cannot connect to SQL Server: " + e.getMessage() + hint, e);
        }
    }

    private synchronized Connection conn() throws SQLException {
        if (connection == null || connection.isClosed() || !connection.isValid(2)) {
            connection = open(config.url());
        }
        return connection;
    }

    /** Opens a connection: with the Windows login, or with the user name / password from db.properties. */
    private Connection open(String url) throws SQLException {
        if (config.isWindowsAuth()) {
            return DriverManager.getConnection(url);
        }
        return DriverManager.getConnection(url, config.getUser(), config.getPassword());
    }

    private void createTables() throws SQLException {
        String[] ddl = {
            "IF OBJECT_ID(N'dbo.Staff', N'U') IS NULL CREATE TABLE dbo.Staff ("
                + "staffID NVARCHAR(20) NOT NULL PRIMARY KEY, name NVARCHAR(100) NOT NULL, "
                + "role NVARCHAR(20) NOT NULL, username NVARCHAR(50) NOT NULL UNIQUE, "
                + "passwordHash NVARCHAR(200) NOT NULL)",
            "IF OBJECT_ID(N'dbo.Customer', N'U') IS NULL CREATE TABLE dbo.Customer ("
                + "customerID NVARCHAR(20) NOT NULL PRIMARY KEY, name NVARCHAR(100) NOT NULL, "
                + "address NVARCHAR(200) NULL, contactNumber NVARCHAR(30) NOT NULL, "
                + "nationalID NVARCHAR(30) NOT NULL UNIQUE, drivingLicenseNo NVARCHAR(30) NOT NULL, "
                + "licenseExpiryDate DATE NOT NULL)",
            "IF OBJECT_ID(N'dbo.Vehicle', N'U') IS NULL CREATE TABLE dbo.Vehicle ("
                + "registrationNumber NVARCHAR(20) NOT NULL PRIMARY KEY, make NVARCHAR(50) NOT NULL, "
                + "model NVARCHAR(50) NOT NULL, modelYear INT NOT NULL, category NVARCHAR(20) NOT NULL, "
                + "transmissionType NVARCHAR(20) NULL, fuelType NVARCHAR(20) NULL, "
                + "currentMileage FLOAT NOT NULL, dailyRentalRate FLOAT NOT NULL, status NVARCHAR(20) NOT NULL)",
            "IF OBJECT_ID(N'dbo.Reservation', N'U') IS NULL CREATE TABLE dbo.Reservation ("
                + "reservationID NVARCHAR(20) NOT NULL PRIMARY KEY, "
                + "customerID NVARCHAR(20) NOT NULL REFERENCES dbo.Customer(customerID), "
                + "registrationNumber NVARCHAR(20) NOT NULL REFERENCES dbo.Vehicle(registrationNumber), "
                + "pickupDate DATE NOT NULL, returnDate DATE NOT NULL, estimatedRentalCharge FLOAT NOT NULL, "
                + "status NVARCHAR(20) NOT NULL, processedBy NVARCHAR(20) NULL REFERENCES dbo.Staff(staffID))",
            "IF OBJECT_ID(N'dbo.RentalAgreement', N'U') IS NULL CREATE TABLE dbo.RentalAgreement ("
                + "agreementID NVARCHAR(20) NOT NULL PRIMARY KEY, "
                + "reservationID NVARCHAR(20) NOT NULL UNIQUE REFERENCES dbo.Reservation(reservationID), "
                + "pickupDateTime DATETIME2 NULL, actualReturnDateTime DATETIME2 NULL, "
                + "startingMileage FLOAT NOT NULL, endingMileage FLOAT NOT NULL, "
                + "pickupFuelLevel FLOAT NOT NULL, returnFuelLevel FLOAT NOT NULL, "
                + "existingDamage NVARCHAR(500) NULL, newDamage NVARCHAR(500) NULL, "
                + "lateReturnCharge FLOAT NOT NULL, excessMileageCharge FLOAT NOT NULL, "
                + "fuelShortageCharge FLOAT NOT NULL, damageCharge FLOAT NOT NULL, totalAmount FLOAT NOT NULL, "
                + "executedBy NVARCHAR(20) NULL REFERENCES dbo.Staff(staffID))",
            "IF OBJECT_ID(N'dbo.Payment', N'U') IS NULL CREATE TABLE dbo.Payment ("
                + "paymentID NVARCHAR(20) NOT NULL PRIMARY KEY, "
                + "agreementID NVARCHAR(20) NOT NULL REFERENCES dbo.RentalAgreement(agreementID), "
                + "amountPaid FLOAT NOT NULL, paymentDate DATE NOT NULL, paymentMethod NVARCHAR(30) NOT NULL)",
            "IF OBJECT_ID(N'dbo.MaintenanceRecord', N'U') IS NULL CREATE TABLE dbo.MaintenanceRecord ("
                + "recordID NVARCHAR(20) NOT NULL PRIMARY KEY, "
                + "registrationNumber NVARCHAR(20) NOT NULL REFERENCES dbo.Vehicle(registrationNumber), "
                + "maintenanceType NVARCHAR(100) NOT NULL, maintenanceDate DATE NOT NULL, "
                + "description NVARCHAR(500) NULL, cost FLOAT NOT NULL, completionStatus NVARCHAR(20) NOT NULL, "
                + "loggedBy NVARCHAR(20) NULL REFERENCES dbo.Staff(staffID))"
        };
        try (Statement st = conn().createStatement()) {
            for (String sql : ddl) {
                st.execute(sql);
            }
        }
    }

    // ------------------------------------------------------------------ loading

    /** Result of loading: every object, already linked to each other. */
    public static class LoadedData {
        public final Map<String, Staff> staff = new LinkedHashMap<>();
        public final Map<String, String[]> accounts = new LinkedHashMap<>(); // staffID -> {username, hash}
        public final Map<String, Customer> customers = new LinkedHashMap<>();
        public final Map<String, Vehicle> vehicles = new LinkedHashMap<>();
        public final Map<String, Reservation> reservations = new LinkedHashMap<>();
        public final Map<String, RentalAgreement> agreements = new LinkedHashMap<>();
    }

    public LoadedData loadAll() {
        LoadedData d = new LoadedData();
        try {
            Connection c = conn();
            try (Statement st = c.createStatement()) {
                try (ResultSet rs = st.executeQuery("SELECT staffID, name, role, username, passwordHash FROM dbo.Staff")) {
                    while (rs.next()) {
                        Staff s = StaffFactory.create(rs.getString(1), rs.getString(2),
                                StaffRole.valueOf(rs.getString(3)));
                        d.staff.put(s.getStaffID(), s);
                        d.accounts.put(s.getStaffID(), new String[]{rs.getString(4), rs.getString(5)});
                    }
                }
                try (ResultSet rs = st.executeQuery("SELECT customerID, name, address, contactNumber, nationalID, "
                        + "drivingLicenseNo, licenseExpiryDate FROM dbo.Customer ORDER BY customerID")) {
                    while (rs.next()) {
                        Customer cu = new Customer(rs.getString(1), rs.getString(2), nz(rs.getString(3)),
                                rs.getString(4), rs.getString(5), rs.getString(6), rs.getDate(7).toLocalDate());
                        d.customers.put(cu.getCustomerID(), cu);
                        IdGenerator.observe(cu.getCustomerID());
                    }
                }
                try (ResultSet rs = st.executeQuery("SELECT registrationNumber, make, model, modelYear, category, "
                        + "transmissionType, fuelType, currentMileage, dailyRentalRate, status "
                        + "FROM dbo.Vehicle ORDER BY registrationNumber")) {
                    while (rs.next()) {
                        Vehicle v = new Vehicle(rs.getString(1), rs.getString(2), rs.getString(3), rs.getInt(4),
                                VehicleCategory.valueOf(rs.getString(5)), rs.getString(6), rs.getString(7),
                                rs.getDouble(8), rs.getDouble(9));
                        v.updateStatus(VehicleStatus.valueOf(rs.getString(10)));
                        d.vehicles.put(v.getRegistrationNumber().toUpperCase(), v);
                    }
                }
                try (ResultSet rs = st.executeQuery("SELECT reservationID, customerID, registrationNumber, pickupDate, "
                        + "returnDate, status, processedBy FROM dbo.Reservation ORDER BY reservationID")) {
                    while (rs.next()) {
                        Reservation r = new Reservation(rs.getString(1), d.customers.get(rs.getString(2)),
                                d.vehicles.get(rs.getString(3).toUpperCase()),
                                rs.getDate(4).toLocalDate(), rs.getDate(5).toLocalDate());
                        r.setStatus(ReservationStatus.valueOf(rs.getString(6)));
                        r.setProcessedBy(d.staff.get(rs.getString(7)));
                        r.getVehicle().addReservation(r);
                        d.reservations.put(r.getReservationID(), r);
                        IdGenerator.observe(r.getReservationID());
                    }
                }
                try (ResultSet rs = st.executeQuery("SELECT agreementID, reservationID, pickupDateTime, "
                        + "actualReturnDateTime, startingMileage, endingMileage, pickupFuelLevel, returnFuelLevel, "
                        + "existingDamage, newDamage, lateReturnCharge, excessMileageCharge, fuelShortageCharge, "
                        + "damageCharge, totalAmount, executedBy FROM dbo.RentalAgreement ORDER BY agreementID")) {
                    while (rs.next()) {
                        Reservation r = d.reservations.get(rs.getString(2));
                        RentalAgreement a = new RentalAgreement(rs.getString(1), r);
                        a.setPickupDateTime(ldt(rs.getTimestamp(3)));
                        a.setActualReturnDateTime(ldt(rs.getTimestamp(4)));
                        a.setStartingMileage(rs.getDouble(5));
                        a.setEndingMileage(rs.getDouble(6));
                        a.setPickupFuelLevel(rs.getDouble(7));
                        a.setReturnFuelLevel(rs.getDouble(8));
                        a.setExistingDamage(rs.getString(9));
                        a.setNewDamage(rs.getString(10));
                        a.restoreCharges(rs.getDouble(11), rs.getDouble(12), rs.getDouble(13),
                                rs.getDouble(14), rs.getDouble(15));
                        a.setExecutedBy(d.staff.get(rs.getString(16)));
                        r.setRentalAgreement(a);
                        d.agreements.put(a.getAgreementID(), a);
                        IdGenerator.observe(a.getAgreementID());
                    }
                }
                try (ResultSet rs = st.executeQuery("SELECT paymentID, agreementID, amountPaid, paymentDate, "
                        + "paymentMethod FROM dbo.Payment ORDER BY paymentID")) {
                    while (rs.next()) {
                        RentalAgreement a = d.agreements.get(rs.getString(2));
                        a.addPayment(new Payment(rs.getString(1), a, rs.getDouble(3),
                                rs.getDate(4).toLocalDate(), rs.getString(5)));
                        IdGenerator.observe(rs.getString(1));
                    }
                }
                try (ResultSet rs = st.executeQuery("SELECT recordID, registrationNumber, maintenanceType, "
                        + "maintenanceDate, description, cost, completionStatus, loggedBy "
                        + "FROM dbo.MaintenanceRecord ORDER BY recordID")) {
                    while (rs.next()) {
                        Vehicle v = d.vehicles.get(rs.getString(2).toUpperCase());
                        MaintenanceRecord m = new MaintenanceRecord(rs.getString(1), v, rs.getString(3),
                                rs.getDate(4).toLocalDate(), rs.getString(5), rs.getDouble(6));
                        m.restoreCompletionStatus(rs.getString(7));
                        m.setLoggedBy(d.staff.get(rs.getString(8)));
                        v.addMaintenanceRecord(m);
                        IdGenerator.observe(m.getRecordID());
                    }
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot load data from SQL Server: " + e.getMessage(), e);
        }
        return d;
    }

    // ------------------------------------------------------------------ saving

    public void insertStaff(Staff staff, String username, String passwordHash) {
        inTransaction(c -> run(c, "INSERT INTO dbo.Staff (staffID, name, role, username, passwordHash) "
                        + "VALUES (?,?,?,?,?)",
                staff.getStaffID(), staff.getName(), staff.getRole().name(), username, passwordHash));
    }

    public void saveCustomer(Customer cu) {
        inTransaction(c -> upsertCustomer(c, cu));
    }

    /** Saves the vehicle and all of its maintenance records. */
    public void saveVehicle(Vehicle v) {
        inTransaction(c -> upsertVehicle(c, v));
    }

    /** Saves the reservation, its customer and vehicle, and its agreement (with payments) if any. */
    public void saveReservation(Reservation r) {
        inTransaction(c -> {
            upsertReservationChain(c, r);
            if (r.getRentalAgreement() != null) {
                upsertAgreement(c, r.getRentalAgreement());
            }
        });
    }

    /** Saves the agreement with its payments, and the reservation / customer / vehicle it belongs to. */
    public void saveAgreement(RentalAgreement a) {
        inTransaction(c -> {
            upsertReservationChain(c, a.getReservation());
            upsertAgreement(c, a);
        });
    }

    private void upsertCustomer(Connection c, Customer cu) throws SQLException {
        int n = run(c, "UPDATE dbo.Customer SET name=?, address=?, contactNumber=?, nationalID=?, "
                        + "drivingLicenseNo=?, licenseExpiryDate=? WHERE customerID=?",
                cu.getName(), cu.getAddress(), cu.getContactNumber(), cu.getNationalID(),
                cu.getDrivingLicenseNo(), cu.getLicenseExpiryDate(), cu.getCustomerID());
        if (n == 0) {
            run(c, "INSERT INTO dbo.Customer (customerID, name, address, contactNumber, nationalID, "
                            + "drivingLicenseNo, licenseExpiryDate) VALUES (?,?,?,?,?,?,?)",
                    cu.getCustomerID(), cu.getName(), cu.getAddress(), cu.getContactNumber(), cu.getNationalID(),
                    cu.getDrivingLicenseNo(), cu.getLicenseExpiryDate());
        }
    }

    private void upsertVehicle(Connection c, Vehicle v) throws SQLException {
        int n = run(c, "UPDATE dbo.Vehicle SET make=?, model=?, modelYear=?, category=?, transmissionType=?, "
                        + "fuelType=?, currentMileage=?, dailyRentalRate=?, status=? WHERE registrationNumber=?",
                v.getMake(), v.getModel(), v.getYear(), v.getCategory().name(), v.getTransmissionType(),
                v.getFuelType(), v.getCurrentMileage(), v.getDailyRentalRate(), v.getStatus().name(),
                v.getRegistrationNumber());
        if (n == 0) {
            run(c, "INSERT INTO dbo.Vehicle (registrationNumber, make, model, modelYear, category, "
                            + "transmissionType, fuelType, currentMileage, dailyRentalRate, status) "
                            + "VALUES (?,?,?,?,?,?,?,?,?,?)",
                    v.getRegistrationNumber(), v.getMake(), v.getModel(), v.getYear(), v.getCategory().name(),
                    v.getTransmissionType(), v.getFuelType(), v.getCurrentMileage(), v.getDailyRentalRate(),
                    v.getStatus().name());
        }
        for (MaintenanceRecord m : v.getMaintenanceHistory()) {
            String logged = m.getLoggedBy() == null ? null : m.getLoggedBy().getStaffID();
            int k = run(c, "UPDATE dbo.MaintenanceRecord SET completionStatus=? WHERE recordID=?",
                    m.getCompletionStatus(), m.getRecordID());
            if (k == 0) {
                run(c, "INSERT INTO dbo.MaintenanceRecord (recordID, registrationNumber, maintenanceType, "
                                + "maintenanceDate, description, cost, completionStatus, loggedBy) "
                                + "VALUES (?,?,?,?,?,?,?,?)",
                        m.getRecordID(), v.getRegistrationNumber(), m.getMaintenanceType(), m.getDate(),
                        m.getDescription(), m.getCost(), m.getCompletionStatus(), logged);
            }
        }
    }

    private void upsertReservationChain(Connection c, Reservation r) throws SQLException {
        upsertCustomer(c, r.getCustomer());
        upsertVehicle(c, r.getVehicle());
        String processed = r.getProcessedBy() == null ? null : r.getProcessedBy().getStaffID();
        int n = run(c, "UPDATE dbo.Reservation SET status=?, processedBy=?, estimatedRentalCharge=? "
                        + "WHERE reservationID=?",
                r.getStatus().name(), processed, r.getEstimatedRentalCharge(), r.getReservationID());
        if (n == 0) {
            run(c, "INSERT INTO dbo.Reservation (reservationID, customerID, registrationNumber, pickupDate, "
                            + "returnDate, estimatedRentalCharge, status, processedBy) VALUES (?,?,?,?,?,?,?,?)",
                    r.getReservationID(), r.getCustomer().getCustomerID(), r.getVehicle().getRegistrationNumber(),
                    r.getPickupDate(), r.getReturnDate(), r.getEstimatedRentalCharge(), r.getStatus().name(),
                    processed);
        }
    }

    private void upsertAgreement(Connection c, RentalAgreement a) throws SQLException {
        String executed = a.getExecutedBy() == null ? null : a.getExecutedBy().getStaffID();
        int n = run(c, "UPDATE dbo.RentalAgreement SET pickupDateTime=?, actualReturnDateTime=?, startingMileage=?, "
                        + "endingMileage=?, pickupFuelLevel=?, returnFuelLevel=?, existingDamage=?, newDamage=?, "
                        + "lateReturnCharge=?, excessMileageCharge=?, fuelShortageCharge=?, damageCharge=?, "
                        + "totalAmount=?, executedBy=? WHERE agreementID=?",
                a.getPickupDateTime(), a.getActualReturnDateTime(), a.getStartingMileage(), a.getEndingMileage(),
                a.getPickupFuelLevel(), a.getReturnFuelLevel(), a.getExistingDamage(), a.getNewDamage(),
                a.getLateReturnCharge(), a.getExcessMileageCharge(), a.getFuelShortageCharge(),
                a.getDamageCharge(), a.getTotalAmount(), executed, a.getAgreementID());
        if (n == 0) {
            run(c, "INSERT INTO dbo.RentalAgreement (agreementID, reservationID, pickupDateTime, "
                            + "actualReturnDateTime, startingMileage, endingMileage, pickupFuelLevel, returnFuelLevel, "
                            + "existingDamage, newDamage, lateReturnCharge, excessMileageCharge, fuelShortageCharge, "
                            + "damageCharge, totalAmount, executedBy) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                    a.getAgreementID(), a.getReservation().getReservationID(), a.getPickupDateTime(),
                    a.getActualReturnDateTime(), a.getStartingMileage(), a.getEndingMileage(),
                    a.getPickupFuelLevel(), a.getReturnFuelLevel(), a.getExistingDamage(), a.getNewDamage(),
                    a.getLateReturnCharge(), a.getExcessMileageCharge(), a.getFuelShortageCharge(),
                    a.getDamageCharge(), a.getTotalAmount(), executed);
        }
        for (Payment p : a.getPayments()) {
            int k = run(c, "UPDATE dbo.Payment SET amountPaid=?, paymentDate=?, paymentMethod=? WHERE paymentID=?",
                    p.getAmountPaid(), p.getPaymentDate(), p.getPaymentMethod(), p.getPaymentID());
            if (k == 0) {
                run(c, "INSERT INTO dbo.Payment (paymentID, agreementID, amountPaid, paymentDate, paymentMethod) "
                                + "VALUES (?,?,?,?,?)",
                        p.getPaymentID(), a.getAgreementID(), p.getAmountPaid(), p.getPaymentDate(),
                        p.getPaymentMethod());
            }
        }
    }

    // ------------------------------------------------------------------ helpers

    private interface SqlWork {
        void execute(Connection c) throws SQLException;
    }

    /** Runs the work in one transaction: commit if everything worked, rollback otherwise. */
    private synchronized void inTransaction(SqlWork work) {
        Connection c = null;
        try {
            c = conn();
            c.setAutoCommit(false);
            work.execute(c);
            c.commit();
        } catch (SQLException e) {
            try {
                if (c != null) {
                    c.rollback();
                }
            } catch (SQLException ignored) {
                // nothing more to do
            }
            throw new IllegalStateException("Database error: " + e.getMessage(), e);
        } finally {
            try {
                if (c != null) {
                    c.setAutoCommit(true);
                }
            } catch (SQLException ignored) {
                // nothing more to do
            }
        }
    }

    /** Executes one INSERT / UPDATE with the given parameters and returns the number of rows changed. */
    private static int run(Connection c, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                Object p = params[i];
                int index = i + 1;
                if (p == null) {
                    ps.setNull(index, Types.VARCHAR);
                } else if (p instanceof LocalDate) {
                    ps.setDate(index, java.sql.Date.valueOf((LocalDate) p));
                } else if (p instanceof LocalDateTime) {
                    ps.setTimestamp(index, Timestamp.valueOf((LocalDateTime) p));
                } else {
                    ps.setObject(index, p);
                }
            }
            return ps.executeUpdate();
        }
    }

    private static LocalDateTime ldt(Timestamp t) {
        return t == null ? null : t.toLocalDateTime();
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
