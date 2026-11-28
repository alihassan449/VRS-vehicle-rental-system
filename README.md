# Vehicle Rental Management System

A desktop application for managing a vehicle rental business: customers, vehicles, reservations, rental agreements with return charges, payments, maintenance and reports.
Built in **Java (Swing)** with **SQL Server** storage. Made for the SDA (Software Design & Architecture) course, so the code follows the class diagram exactly.

## Features

* **Login with roles:** each staff member sees only the tabs they are allowed to use.
* **Customers:** register, search, update contact info, rental history, expired-license check.
* **Vehicles:** register, search by registration number / category / status, set INACTIVE, maintenance history.
* **Reservations:** check available vehicles, create (estimated charge), confirm, cancel, expire, no overlapping confirmed reservations.
* **Rentals:** create the rental agreement from a confirmed reservation (handover), record the return, calculate the four charges and the final amount, invoice.
* **Payments:** record payments (partial payments allowed), receipt.
* **Maintenance:** log and update records; a vehicle under maintenance cannot be reserved or rented.
* **Reports:** fleet status, customer history, vehicle history, revenue.
* **Hint text (placeholders)** in every text field show the expected format, e.g. National ID `00000-0000000-0`.

## Roles and tabs

| Role | Username / password | Tabs |
|---|---|---|
| Rental Staff | `rental` / `rental123` | Customers, Reservations, Rentals, Payments, Reports |
| Fleet Staff | `fleet` / `fleet123` | Vehicles, Maintenance, Reports |
| Admin | `admin` / `admin123` | all 7 tabs |

These default accounts are created automatically the first time the program starts. Passwords are stored only as a salted hash (PBKDF2).
The domain classes also check the permission on every action (a wrong action throws `SecurityException`), so hiding tabs is only for convenience.

## Technology

* Java 11 (JDK 11), Swing GUI (Nimbus look and feel)
* SQL Server (Express is fine) through JDBC (Microsoft JDBC driver)
* No other libraries

## Project structure

```
project-root/
  src/com/rental/
    enums/        VehicleCategory, VehicleStatus, ReservationStatus, StaffRole
    model/        Customer, Staff (abstract), RentalStaff, FleetStaff, AdminStaff,
                  Vehicle, Reservation, MaintenanceRecord, RentalAgreement, Payment, StaffFactory
    model/charge/ the four return-charge rules (Strategy pattern)
    service/      CustomerService, VehicleService, ReservationService
    repository/   repository interfaces + in-memory implementations
    repository/sqlserver/   SQL Server implementations (JDBC)
    report/       ReportGenerator, Report
    controller/   RentalManagementController
    util/         IdGenerator, PasswordHasher
    ui/           LoginFrame, MainFrame, 7 panels, Session, FormUtil, UiTheme
    AppContext.java    builds repositories -> services -> controller
    Main.java          starts the GUI (login window)
    ClassesDemo.java   console demo (no GUI, no database)
  lib/                 JDBC driver files (see setup)
  db.properties        your database settings (you create this file)
  DIAGRAM_TO_CODE_MAP.md   class diagram -> code, and FR1-FR31 -> code
  README_DATABASE.md       detailed database setup
```

The code is submitted in two parts:

1. **Classes_code.zip** - enums, model, service, repository, report, controller, util, `AppContext.java`, `ClassesDemo.java`, documents. Compiles and runs alone (no GUI).
2. **GUI_code.zip** - the `ui` folder and `Main.java`. Needs the classes part to run.

Both keep the `src/com/rental/...` structure, so they can simply be merged into one folder.

## Setup

### 1. SQL Server
* SQL Server must be running and **TCP/IP must be enabled** (SQL Server Configuration Manager -> Protocols).
* For a named instance (SQL Server Express = `SQLEXPRESS`) the **SQL Server Browser** service must be running, or use a fixed port (1433).
* Choose one login type:
  * **Windows Authentication** (your Windows account), or
  * **SQL login** (e.g. `sa`) - needs Mixed Mode authentication and an enabled `sa` account.

The database `VehicleRental` and all tables are created automatically on the first start.

### 2. JDBC driver
1. Download the "Microsoft JDBC Driver for SQL Server" (zip) from Microsoft.
2. Create a folder `lib` in the project root (next to `src`).
3. Copy the jar whose name ends with `.jre11.jar` into `lib`.
4. **Only for Windows Authentication:** also copy `mssql-jdbc_auth-<version>.x64.dll` (from the `auth\x64` folder of the same download) into `lib`. The version must be the same as the jar.

### 3. db.properties
Create a file `db.properties` in the project root (next to `src`).

Windows Authentication:
```
server=localhost
instance=SQLEXPRESS
database=VehicleRental
auth=windows
```

SQL login:
```
server=localhost
instance=SQLEXPRESS
database=VehicleRental
auth=sql
user=sa
password=your_password
```

For a default instance (not Express) leave `instance=` empty and write `port=1433`.

## Compile and run

Run these commands in the project root (the folder that contains `src`):

```
javac -d bin -sourcepath src src/com/rental/Main.java
java -Djava.library.path=lib -cp "bin;lib/*" com.rental.Main
```

(`-Djava.library.path=lib` is needed only for Windows Authentication. On Windows the classpath separator is `;`.)

If the database cannot be reached, the program offers **Continue without database** (demo only, all data is lost when the program closes).

### Console demo (no GUI, no database)
```
javac -d bin -sourcepath src src/com/rental/ClassesDemo.java
java -cp bin com.rental.ClassesDemo
```

### VS Code notes
* Use the commands above (or the `.vscode/settings.json` Code Runner setting). The plain Code Runner play button fails with "package ... does not exist" because it compiles one file only.
* When you copy a new version over an old one, delete the old `src` and `bin` folders first.

## Input formats

| Data | Format | Example |
|---|---|---|
| Date | `yyyy-MM-dd` | `2026-12-31` |
| Date and time | `yyyy-MM-dd HH:mm` (blank = now) | `2026-12-04 10:00` |
| National ID | `00000-0000000-0` | `35202-1234567-1` |
| Mobile number | `0300-0000000` | `0300-1234567` |
| Fuel level | percent, 0 - 100 | `80` |
| Damage items | separated by `;` | `cracked glass;broken mirror` |

## Charge rules

The final amount of a rental = estimated rental charge (days x daily rate) + the four return charges (FR21):

| Charge | Rule |
|---|---|
| Late return | 20.00 per late day |
| Excess mileage | 200 km free per rental day, then 0.50 per km |
| Fuel shortage | 0.60 per missing fuel percent (15.00 per quarter tank) |
| Damage | 50.00 per new damage item |

These values are constants in `model/charge/` and can be changed there if the course handout gives other numbers.

Example: 3 days at 100 per day, returned 2 days late, 800 km driven (600 free), fuel 80% -> 50%, 2 new damage items:
300 + 40 + 100 + 18 + 100 = **558.00**.

## Design overview

* **Layers:** UI -> controller -> services -> repositories -> storage (in memory or SQL Server).
* **Controller** `RentalManagementController` uses the three services and `ReportGenerator`, as in the class diagram.
* **Permissions by polymorphism:** `Staff` is abstract; `RentalStaff`, `FleetStaff` and `AdminStaff` only answer `canHandleRentals()` / `canManageFleet()`. No `if (role == ...)` chains.
* **Strategy pattern:** one class per return charge (`LateReturnCharge`, `ExcessMileageCharge`, `FuelShortageCharge`, `DamageCharge`) used by `RentalAgreement.calculateFinalCharges()`.
* **Repository pattern:** services depend on interfaces; `AppContext` chooses the in-memory or SQL Server implementation.
* **Database:** one table per class (Staff, Customer, Vehicle, Reservation, RentalAgreement, Payment, MaintenanceRecord); foreign keys follow the associations of the class diagram. Every save runs in one transaction.

See `DIAGRAM_TO_CODE_MAP.md` for the full mapping of every diagram class and every functional requirement (FR1 - FR31) to the code.

## Troubleshooting

| Message | Reason / fix |
|---|---|
| `Cannot read db.properties` | The file must be in the folder from which you run `java` (project root). |
| `No suitable driver found` | The jar is not on the classpath: check the `lib` folder and use `-cp "bin;lib/*"`. |
| `This driver is not configured for integrated authentication` | The auth DLL is missing: copy it into `lib` and start with `-Djava.library.path=lib`. |
| `Receive timed out ... UDP port 1434` | Start the **SQL Server Browser** service, or use a fixed port (instance empty, `port=1433`). |
| `The TCP/IP connection to the host ... failed` | Enable TCP/IP for the instance in SQL Server Configuration Manager and restart the service. |
| `Login failed for user 'sa'` | Wrong password, `sa` disabled, or Mixed Mode not enabled. |
| Many red errors in VS Code | Open the project root folder (the one containing `src`), set `java.project.sourcePaths` to `["src"]`, then run "Java: Clean Java Language Server Workspace". |

To start again from zero, run `DROP DATABASE VehicleRental;` in SQL Server; it is created again on the next start.

## Testing

`TEST_SCENARIOS.md` contains step-by-step manual test runs with sample data and the expected result of each step (all roles, all tabs, charges, database persistence).