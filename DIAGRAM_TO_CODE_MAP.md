# Vehicle Rental System - Class Diagram to Code Map (for evaluation)

All source code is in `src/com/rental/`.

The code is submitted in two separate parts:

* **Classes_code** - all classes of the class diagram (enums, model, service, repository, report, controller, util) plus `ClassesDemo.java`. It compiles and runs on its own (no GUI needed).
* **GUI_code** - the Swing screens (`ui/` folder) and `Main.java`, which starts the GUI. It needs the classes part to run.

## 1. Diagram class -> file

| Class in the diagram | File | Package |
|---|---|---|
| `VehicleCategory` (enum) | `enums/VehicleCategory.java` | com.rental.enums |
| `VehicleStatus` (enum) | `enums/VehicleStatus.java` | com.rental.enums |
| `ReservationStatus` (enum) | `enums/ReservationStatus.java` | com.rental.enums |
| `StaffRole` (enum) | `enums/StaffRole.java` | com.rental.enums |
| `Customer` | `model/Customer.java` | com.rental.model |
| `Staff` | `model/Staff.java` (abstract) | com.rental.model |
| Rental staff (role `RENTAL_STAFF`) | `model/RentalStaff.java` (extends Staff) | com.rental.model |
| Fleet staff (role `FLEET_STAFF`) | `model/FleetStaff.java` (extends Staff) | com.rental.model |
| Admin (role `ADMIN`) | `model/AdminStaff.java` (extends Staff) | com.rental.model |
| `Vehicle` | `model/Vehicle.java` | com.rental.model |
| `Reservation` | `model/Reservation.java` | com.rental.model |
| `MaintenanceRecord` | `model/MaintenanceRecord.java` | com.rental.model |
| `RentalAgreement` | `model/RentalAgreement.java` | com.rental.model |
| `Payment` | `model/Payment.java` | com.rental.model |
| `RentalManagementController` | `controller/RentalManagementController.java` | com.rental.controller |
| `CustomerService` | `service/CustomerService.java` | com.rental.service |
| `VehicleService` | `service/VehicleService.java` | com.rental.service |
| `ReservationService` | `service/ReservationService.java` | com.rental.service |
| `ReportGenerator` | `report/ReportGenerator.java` | com.rental.report |

### Attributes and methods (all match the diagram)

* **Customer**: `customerID, name, address, contactNumber, nationalID, drivingLicenseNo, licenseExpiryDate` - `updateContactInfo(address, contactNumber)`, `isLicenseValid()`
* **Staff**: `staffID, name, role` - `processReservation()`, `processHandover()`, `processReturn()` (plus `recordPayment`, `logMaintenance`, `updateMaintenanceStatus`, `setVehicleInactive`, each protected by `requireRentalAccess()` / `requireFleetAccess()`)
* **Vehicle**: `registrationNumber, make, model, year, category, transmissionType, fuelType, currentMileage, dailyRentalRate, status` - `updateStatus(status)`, `updateMileage(mileage)`, `isAvailableForPeriod(startDate, endDate)`
* **Reservation**: `reservationID, pickupDate, returnDate, estimatedRentalCharge, status` - `calculateEstimatedCharge()`, `confirmReservation()`, `cancelReservation()`
* **MaintenanceRecord**: `recordID, maintenanceType, date, description, cost, completionStatus` - `updateStatus(status)`
* **RentalAgreement**: `agreementID, pickupDateTime, actualReturnDateTime, startingMileage, endingMileage, pickupFuelLevel, returnFuelLevel, existingDamage, newDamage, lateReturnCharge, excessMileageCharge, fuelShortageCharge, damageCharge, totalAmount` - `calculateFinalCharges()`, `completeReturn()`, `generateInvoice()`
* **Payment**: `paymentID, amountPaid, paymentDate, paymentMethod` - `generateReceipt()`
* **RentalManagementController**: `customerService, vehicleService, reservationService, reportGenerator`
* **CustomerService**: `registerCustomer(...)`, `searchCustomer(query)`, `getRentalHistory(customerID)`
* **VehicleService**: `registerVehicle(...)`, `searchVehicles(regNo, category, status)`, `getMaintenanceHistory(registrationNumber)`
* **ReservationService**: `createReservation(customer, vehicle, pickup, returnD)`, `checkOverlappingReservation(vehicle, pickup, returnD)`, `cancelReservation(reservationID)`
* **ReportGenerator**: `generateVehicleStatusReport(status)`, `generateCustomerHistoryReport(customerID)`, `generateVehicleHistoryReport(regNo)`, `generateRevenueReport(startDate, endDate)`

### Associations from the diagram (where they are in code)

| Association | Implemented as |
|---|---|
| Customer "places" Reservation (1 - 0..*) | `Reservation.customer` |
| Customer "signs" RentalAgreement (1 - 0..*) | `RentalAgreement.getCustomer()` (through its reservation); `RentalAgreementRepository.findByCustomerId` |
| Staff "processes" Reservation | `Reservation.processedBy` (set in `Staff.processReservation`) |
| Staff "executes" RentalAgreement | `RentalAgreement.executedBy` (set in `Staff.processHandover`) |
| Staff "logs" MaintenanceRecord | `MaintenanceRecord.loggedBy` (set in `FleetStaff.logMaintenance`) |
| Reservation "assigned to" Vehicle (0..* - 1) | `Reservation.vehicle` and `Vehicle.reservations` |
| Vehicle "undergoes" MaintenanceRecord (1 - 0..*) | `Vehicle.maintenanceHistory`, `MaintenanceRecord.vehicle` |
| Vehicle "rented in" RentalAgreement (1 - 0..*) | `RentalAgreement.getVehicle()`; `RentalAgreementRepository.findByVehicle` |
| Reservation "generates" RentalAgreement (0..1 - 1) | `Reservation.rentalAgreement`, `RentalAgreement.reservation` |
| RentalAgreement "contains" Payment (1 - 1..*) | `RentalAgreement.payments`, `Payment.rentalAgreement` |
| Controller uses the 4 services | the 4 fields of `RentalManagementController` |

## 2. Three staff types (Rental Staff, Fleet Staff, Admin) and login

`Staff` is abstract and has three subclasses. All operations are written once in `Staff`; each operation starts with `requireRentalAccess()` or `requireFleetAccess()`. The subclass only answers two questions, so the permission is decided by polymorphism - no `if (role == ...)` chains:

| | `RentalStaff` | `FleetStaff` | `AdminStaff` |
|---|---|---|---|
| `canHandleRentals()` | true | false | true |
| `canManageFleet()` | false | true | true |
| Customers, reservations, handover, return, payments | yes | no | yes |
| Vehicles, vehicle status, maintenance | no | yes | yes |
| Reports (all four) | yes | yes | yes |

A wrong action throws `SecurityException` ("... is not authorized ..."), which the screen shows as an error message.

**Login:** `ui/LoginFrame` asks for username and password; `StaffRepository.authenticate(...)` checks them (passwords are stored only as a PBKDF2 hash, see `util/PasswordHasher`). After login `ui/MainFrame` creates only the tabs the staff member may use:

| Staff | Tabs shown |
|---|---|
| Rental Staff | Customers, Reservations, Rentals, Payments, Reports |
| Fleet Staff | Vehicles, Maintenance, Reports |
| Admin | all 7 tabs |

Default accounts (created the first time): `rental / rental123` (Ali Hassan), `fleet / fleet123` (Sara Khan), `admin / admin123` (System Admin).

## 3. Supporting classes that are not in the class diagram

These are implementation details that every layered application needs:

| File | Why it exists |
|---|---|
| `repository/*Repository.java` + `InMemory*Repository.java` (5 interfaces + 5 implementations, incl. `StaffRepository`) | storage of objects (in memory); services depend on interfaces |
| `repository/sqlserver/*` (`SqlServerConfig`, `SqlServerDatabase`, 5 `SqlServer*Repository`) | SQL Server storage (JDBC). Same interfaces as the in-memory repositories, so services and controller are unchanged |
| `AppContext.java` | builds repositories -> services -> controller, either on SQL Server or in memory |
| `model/StaffFactory.java`, `util/PasswordHasher.java` | creates the right Staff subclass for a role; password hashing for login |
| `model/charge/ChargeRule.java`, `LateReturnCharge`, `ExcessMileageCharge`, `FuelShortageCharge`, `DamageCharge` | Strategy pattern used by `RentalAgreement.calculateFinalCharges()` (one class per charge field) |
| `report/Report.java` | the `Report` return type of `ReportGenerator` |
| `util/IdGenerator.java` | generates IDs like `CUS-0001`, `RES-0001`, `AGR-0001`, `PAY-0001`, `MNT-0001` |
| `ui/*` (`LoginFrame`, `MainFrame`, 7 panels, `Session`, `FormUtil`, `UiTheme`) | Swing desktop screens (login + role-based tabs) |
| `Main.java` | connects to the database, shows the login window (belongs to the GUI part) |
| `ClassesDemo.java` | console demo that runs the classes without the GUI (belongs to the classes part) |

## 4. Differences from the diagram text (and why)

1. **Types**: UML `Date` = `java.time.LocalDate`, `DateTime` = `java.time.LocalDateTime`, `Boolean` = `boolean`.
2. **`Staff` methods have parameters** in code (the diagram shows them empty): `processReservation(Reservation)`, `processHandover(Reservation, pickupDateTime, startingMileage, pickupFuelLevel, existingDamage)`, `processReturn(RentalAgreement, returnDateTime, endingMileage, returnFuelLevel, newDamage, maintenanceRequired)`. They need the objects they work on.
3. **`registerCustomer(...)` / `registerVehicle(...)`** are written out with all fields required by FR1 / FR6 (including `currentMileage`).
4. **`searchVehicles`** treats a `null` argument as "any", so any combination of registration number / category / status works.
5. **Fuel level** is a `double` percentage (0-100) as in the diagram. Fuel shortage = 0.60 per missing percent (= 15.00 per quarter tank).
6. **`MaintenanceRecord.completionStatus`** is a `String` with the values `PENDING`, `IN_PROGRESS`, `COMPLETED`.
7. **Fixed charge rules (FR21)** are constants in `model/charge/` - adjust them to the project handout if it gives different numbers:
   * late return: 20.00 per late day
   * excess mileage: 200 km free per rental day, then 0.50 per km
   * fuel shortage: 0.60 per missing percent
   * damage: 50.00 per new damage item (items separated by `;`)
8. `StaffRole.ADMIN` is implemented by `AdminStaff` (all permissions).
9. **Database:** data is stored in SQL Server (tables: Staff, Customer, Vehicle, Reservation, RentalAgreement, Payment, MaintenanceRecord; one table per diagram class, foreign keys = the diagram associations). See `README_DATABASE.md`.

## 5. Functional requirement -> where it is implemented

| FR | Implemented in |
|---|---|
| FR1 register customer | `CustomerService.registerCustomer`, `Customer` |
| FR2 search customer | `CustomerService.searchCustomer` |
| FR3 previous agreements | `CustomerService.getRentalHistory`, `ReportGenerator.generateCustomerHistoryReport` |
| FR4 update contact info | `Customer.updateContactInfo`, `CustomerService.updateContactInfo` (Rental Staff only) |
| FR5 expired license blocked | `Customer.isLicenseValid`, checked in `ReservationService.createReservation` and `Staff.processHandover` |
| FR6 register vehicle | `VehicleService.registerVehicle` (Fleet Staff only) |
| FR7 fixed categories | `VehicleCategory` enum |
| FR8 vehicle status | `VehicleStatus`, `Vehicle.updateStatus`, `Staff.setVehicleInactive` (fleet access) |
| FR9 search vehicles | `VehicleService.searchVehicles` |
| FR10 maintenance history | `Vehicle.maintenanceHistory`, `VehicleService.getMaintenanceHistory` |
| FR11 create reservation | `ReservationService.createReservation` |
| FR12 available vehicles | `ReservationService.findAvailableVehicles`, `Vehicle.isAvailableForPeriod` |
| FR13 no overlapping confirmed reservations | `Vehicle.hasOverlappingReservation`, `ReservationService.checkOverlappingReservation`, `Staff.processReservation` |
| FR14 estimated charge | `Reservation.calculateEstimatedCharge` |
| FR15 reservation status | `ReservationStatus`, `Reservation.confirmReservation / cancelReservation / markExpired` |
| FR16 cancel before collection | `Reservation.cancelReservation`, `ReservationService.cancelReservation` |
| FR17 agreement from confirmed reservation | `Staff.processHandover` (creates the `RentalAgreement`) |
| FR18 handover details | `Staff.processHandover`, fields of `RentalAgreement` |
| FR19 vehicle becomes Rented | `Staff.processHandover` |
| FR20 return details | `Staff.processReturn` |
| FR21 four charges | `RentalAgreement.calculateFinalCharges` + `model/charge/*` |
| FR22 final amount | `RentalAgreement.calculateFinalCharges` (`totalAmount`) |
| FR23 vehicle Available after return | `RentalAgreement.completeReturn`, `Vehicle.releaseHold`; maintenance option in `Staff.processReturn` |
| FR24 record payment | `Staff.recordPayment` (rental access), `Payment` |
| FR25 invoice and receipt | `RentalAgreement.generateInvoice`, `Payment.generateReceipt` |
| FR26 maintenance record | `Staff.logMaintenance / updateMaintenanceStatus` (fleet access), `MaintenanceRecord` |
| FR27 maintenance blocks reserve / rent | `ReservationService.createReservation`, `Staff.processReservation`, `Staff.processHandover`, `MaintenanceRecord.updateStatus` |
| FR28 fleet status report | `ReportGenerator.generateVehicleStatusReport` |
| FR29 customer history report | `ReportGenerator.generateCustomerHistoryReport` |
| FR30 vehicle history report | `ReportGenerator.generateVehicleHistoryReport` |
| FR31 revenue report | `ReportGenerator.generateRevenueReport` |

## 6. Quick demo script for the evaluation

1. Start the program -> login as **rental / rental123** -> only 5 tabs are visible. Customers tab -> register a customer (license expiry in the future).
2. Logout -> login as **fleet / fleet123** -> only Vehicles, Maintenance, Reports. Vehicles tab -> register a vehicle.
3. Logout -> login as **admin / admin123** -> all 7 tabs.
4. Rental staff -> Reservations tab -> create a reservation -> select it -> **Confirm**.
5. Rentals tab -> reservation ID, starting mileage, fuel % -> **Create Agreement & Record Handover** (vehicle becomes RENTED).
6. Rentals tab -> return with a later date/time, higher mileage, lower fuel %, some damage -> invoice shows all four charges.
7. Payments tab -> record a payment -> receipt is shown.
8. Fleet staff -> Maintenance tab -> create an open record (vehicle becomes UNDER_MAINTENANCE) -> try to reserve it as Rental Staff (refused) -> set the record to COMPLETED.
9. Reports tab -> run the four reports.
10. Close the program, start it again, login -> the data is still there (SQL Server). Show the tables in SSMS.

Keyboard: press **Enter** or **Down Arrow** in a text field to jump to the next field.
