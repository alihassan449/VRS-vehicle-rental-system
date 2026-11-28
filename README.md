# Vehicle Rental Desktop Management System

A Java Swing and SQLite vehicle-rental application implementing **UC-01 to UC-16** from the supplied Software Design and Analysis artifacts, including **UC-12.1 to UC-12.4**.

## Requirements implemented

- UC-01 / UC-13: register customers and verify driving licenses.
- UC-02: search customers and view profiles and rental history.
- UC-03: update customer contact details and address.
- UC-04 / UC-05: register and search fleet vehicles.
- UC-06 / UC-14 / UC-15: create reservations, check availability, and reject overlaps.
- UC-07: cancel active, uncollected reservations.
- UC-08 / UC-13: process vehicle handover and verify licenses.
- UC-09 / UC-16: process returns and calculate charges.
- UC-10: record payments and print invoice/receipt previews.
- UC-11: create, view, and complete maintenance records.
- UC-12: generate tabular system reports.
- UC-12.1: vehicle status report.
- UC-12.2: customer rental history report.
- UC-12.3: vehicle history report.
- UC-12.4: revenue report.
- Administrator staff management: create, list, and delete Rental/Fleet staff accounts.

## Updated user interface

- Role-specific navigation for Rental Staff, Fleet Staff, and Administrator users.
- Every operational page displays its SDA use-case reference in the application banner.
- Human-readable forms use clear labels, instructions, placeholders, examples, and inline hints.
- Maintenance is split into a vehicle directory, maintenance form, and maintenance history page.
- Staff Management is split into a staff directory and dedicated Create Staff form.
- Reports use a report-catalog sidebar and tabular output instead of fixed-width text.
- Reservation creation saves a **Pending** reservation. Rental Staff can then select the reservation and apply the allowed Confirmed, Cancelled, or Expired status.

## Staff accounts

| Username | Password | Role |
|---|---|---|
| `rental` | `rental123` | Rental Staff |
| `fleet` | `fleet123` | Fleet Staff |
| `admin` | `admin123` | Administrator |

Administrators can create additional Rental Staff or Fleet Staff accounts from **Staff Management → + Add Staff**. Administrator accounts cannot be deleted.

## Run the application

### Windows

```powershell
.\run.ps1
```

or double-click:

```text
run.bat
```

### SQLite mode

```powershell
java -cp "bin;lib/*" com.rental.Main
```

SQLite data is stored at:

```text
data/vehicle_rental.db
```

### In-memory mode

```powershell
java -cp "bin;lib/*" com.rental.Main --in-memory
```

### Manual compilation

```powershell
javac -cp "lib/*" -d bin (Get-ChildItem -Recurse src -Filter *.java).FullName
java -cp "bin;lib/*" com.rental.Main
```

## Database and persistence

The application persists staff, customers, vehicles, reservations, rental agreements, charges, maintenance records, payments, and sequential ID counters in SQLite.

## SDA documentation

- `Vehicle_Rental_Use_Case_Specifications (1).pdf`
- `Vehicle_Rental_Sequence_Diagrams.pptx`
- `UseCase.drawio.png`
- `designclass (1).png`
