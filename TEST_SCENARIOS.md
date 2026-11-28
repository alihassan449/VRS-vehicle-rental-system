# Vehicle Rental System - Test Scenarios (Sample Runs)

Each step has an **Expected** result. Note down anything that differs.

**Before you start**
- Date format: `yyyy-MM-dd` (e.g. `2026-11-29`). Date-time format: `yyyy-MM-dd HH:mm` (e.g. `2026-11-29 10:00`).
- The dates below assume today is **2026-11-28**. If your laptop date is different, shift all dates by the same amount (keep the "2 days late" calculation in Step 5.4 intact).
- IDs (`CUS-0001`, `RES-0001`, ...) are correct only for a fresh database. If test data already exists, the next number is used. Read the ID from the table before using it.
- For a fresh start run `DROP DATABASE VehicleRental;` in SSMS. The program creates it again on the next start.
- In every tab, pressing Enter or Down Arrow in a text field should move to the next field.

---

## 1. Login and roles

| # | Action | Expected |
|---|---|---|
| 1.1 | Login with `rental` / `wrongpass` | "Wrong username or password" |
| 1.2 | Click Login with an empty username or password | "Please enter username and password" |
| 1.3 | `rental` / `rental123` | Only 5 tabs: Customers, Reservations, Rentals, Payments, Reports. Header shows "Ali Hassan (Rental Staff)" |
| 1.4 | Logout, then `fleet` / `fleet123` | Only 3 tabs: Vehicles, Maintenance, Reports |
| 1.5 | Logout, then `admin` / `admin123` | All 7 tabs |
| 1.6 | Login with `ADMIN` (capital letters) / `admin123` | Login works (username is not case-sensitive) |

---

## 2. Fleet Staff: Vehicles (FR6 - FR10)

Login: `fleet` / `fleet123` -> **Vehicles** tab.

**Check:** the form is compact and the vehicle table is clearly visible below it.

### 2.1 Register three vehicles (FR6, FR7)

| Reg no | Make | Model | Year | Category | Transmission | Fuel | Mileage | Daily rate |
|---|---|---|---|---|---|---|---|---|
| LEA-101 | Toyota | Corolla | 2022 | SEDAN | Automatic | Petrol | 15000 | 100 |
| LEB-202 | Suzuki | Alto | 2021 | ECONOMY | Manual | Petrol | 30000 | 50 |
| LEC-303 | Toyota | Hilux | 2023 | SUV | Manual | Diesel | 8000 | 150 |

**Expected:** all three appear in the table with status **AVAILABLE**.

### 2.2 Invalid input

| # | Action | Expected error |
|---|---|---|
| 2.2a | Register LEA-101 again | "Vehicle LEA-101 is already registered" |
| 2.2b | Daily rate `0` | "Daily rental rate must be greater than zero" |
| 2.2c | Mileage `-5` | "Mileage cannot be negative" |
| 2.2d | Year = `abc` | Error about a valid model year |
| 2.2e | Empty registration number | "Registration number is required" |

### 2.3 Search (FR9)

| Search | Expected |
|---|---|
| Reg. No. `LEA` | only LEA-101 |
| Category SUV | only LEC-303 |
| Status AVAILABLE | all three |
| Show All | all three |

### 2.4 Status (FR8)

1. Select LEC-303 -> **Mark Selected INACTIVE** -> status becomes **INACTIVE**.
2. Search with Status = INACTIVE -> LEC-303 is listed.
3. Select it again -> **Reactivate Selected** -> **AVAILABLE**. (Needed for the later tests.)

---

## 3. Rental Staff: Customers (FR1 - FR5)

Login: `rental` / `rental123` -> **Customers** tab.

### 3.1 Register two customers (FR1)

| Field | Customer 1 | Customer 2 (expired license) |
|---|---|---|
| Customer ID | (leave blank) | (leave blank) |
| Name | Ali Raza | Sana Malik |
| Contact number | 0300-1234567 | 0321-7654321 |
| National ID | 35202-1234567-1 | 35201-7654321-2 |
| Address | House 12, Gulberg, Lahore | Flat 4, Model Town, Lahore |
| Driving license no | LIC-2024-001 | LIC-2019-777 |
| License expiry | 2030-12-31 | 2025-01-01 |

**Expected:** `CUS-0001` and `CUS-0002` appear in the table.

### 3.2 Invalid input

| # | Action | Expected |
|---|---|---|
| 3.2a | Register a new customer with Ali's National ID `35202-1234567-1` | "A customer with National ID 35202-1234567-1 already exists" |
| 3.2b | Empty name | "... is required" |
| 3.2c | Empty license expiry | "Driving license expiry date is required" |
| 3.2d | License expiry `31-12-2030` (wrong format) | Date format error |

### 3.3 Search (FR2)

| Search text | Expected |
|---|---|
| `Ali` | Ali Raza |
| `CUS-0002` | Sana Malik |
| `35202-1234567-1` | Ali Raza |
| `xyz` | empty list |

### 3.4 Update contact info (FR4)

1. Select the Ali Raza row.
2. In the form set **Address** = `House 50, DHA Phase 5, Lahore` and **Contact number** = `0333-9876543`.
3. Click **Update Contact Info of Selected**.

**Expected:** the table shows the new address and contact number. Name and National ID stay unchanged.

### 3.5 Rental history (FR3)
Select Ali Raza and click **Rental History of Selected**. It is empty for now. Check again after Step 6.

---

## 4. Rental Staff: Reservations (FR11 - FR16)

**Reservations** tab.

### 4.1 Available vehicles (FR12)
Availability from `2026-11-29`, to `2026-12-02` -> **Show Available Vehicles**.
**Expected:** LEA-101, LEB-202 and LEC-303 are all listed.

### 4.2 Create a reservation (FR11, FR14)
Customer `CUS-0001`, vehicle `LEA-101`, pickup `2026-11-29`, return `2026-12-02` -> **Create Reservation**.
**Expected:** `RES-0001`, status **PENDING**, estimated charge **300.00** (3 days x 100).

### 4.3 Cases that must be refused

| # | Action | Expected |
|---|---|---|
| 4.3a | CUS-0002 (expired license) + LEB-202, 2026-12-10 to 2026-12-12 | Reservation refused because the license is expired (FR5) |
| 4.3b | CUS-0001 + LEB-202, pickup 2026-12-05, return 2026-12-02 | "Return date cannot be before the pickup date" |
| 4.3c | Customer ID `CUS-9999` | "No customer with ID CUS-9999" |
| 4.3d | Vehicle `ZZZ-000` | "No vehicle with registration number ZZZ-000" |

### 4.4 Confirm (FR13, FR15)
Select RES-0001 -> **Confirm** -> status **CONFIRMED**.
(With a Fleet or Admin login check the Vehicles tab: LEA-101 is now **RESERVED**.)

### 4.5 Overlap (FR13)
CUS-0001 + LEA-101, pickup `2026-12-01`, return `2026-12-03` -> Create.
**Expected:** refused because the vehicle already has a confirmed reservation overlapping these dates.

### 4.6 Cancel (FR16)
1. CUS-0001 + LEB-202, `2026-12-10` to `2026-12-12` -> Create (**RES-0002**, estimated 100.00).
2. Select it -> **Cancel** -> status **CANCELLED**.

### 4.7 Expire (FR15)
1. CUS-0001 + LEB-202, `2026-12-15` to `2026-12-16` -> Create (**RES-0003**, estimated 50.00).
2. Select it -> **Mark Expired** -> status **EXPIRED**.

---

## 5. Rental Staff: Rentals - handover and return (FR17 - FR23)

**Rentals** tab.

### 5.1 Handover (FR17, FR18, FR19)

| Field | Value |
|---|---|
| Confirmed reservation ID | RES-0001 |
| Pickup date/time | 2026-11-29 10:00 |
| Starting mileage | 15000 |
| Fuel level | 80 |
| Existing damage | small scratch on rear bumper |

Click **Create Agreement & Record Handover**.
**Expected:** `AGR-0001` appears; the reservation becomes **COLLECTED**; vehicle LEA-101 becomes **RENTED** (check in the Vehicles tab).

### 5.2 Invalid handover

| # | Action | Expected |
|---|---|---|
| 5.2a | Handover RES-0001 again | Error: only a CONFIRMED reservation can be handed over (status: COLLECTED) |
| 5.2b | Handover RES-0002 (cancelled) | Same error (status: CANCELLED) |
| 5.2c | Fuel level `150` | "Fuel level must be between 0 and 100 (percent)" |
| 5.2d | Starting mileage `14000` (less than the vehicle's current mileage 15000) | "Mileage cannot be less than the vehicle's current mileage (15000.0)" |

### 5.3 Invalid return (try these first, then the correct one)

| # | Action | Expected |
|---|---|---|
| 5.3a | Ending mileage `14000` (less than start) | "Ending mileage cannot be less than the starting mileage" |
| 5.3b | Return date/time `2026-11-28 09:00` (before pickup) | "Return date/time cannot be before the pickup date/time" |
| 5.3c | Fuel `-5` | "Fuel level must be between 0 and 100 (percent)" |

### 5.4 Return and charges (FR20, FR21, FR22, FR23)

| Field | Value |
|---|---|
| Agreement ID | AGR-0001 |
| Return date/time | 2026-12-04 10:00 |
| Ending mileage | 15800 |
| Fuel level | 50 |
| New damage | cracked windshield;broken side mirror |
| "Vehicle needs maintenance" | leave **unticked** |

Click **Record Return & Calculate Charges**.

**Expected invoice (check every line):**

| Item | Calculation | Amount |
|---|---|---|
| Base rental | 3 days x 100 | 300.00 |
| Late return | returned 2026-12-04, due 2026-12-02 = 2 days x 20 | 40.00 |
| Excess mileage | 800 km driven, 3 x 200 = 600 free, 200 km x 0.50 | 100.00 |
| Fuel shortage | 80 -> 50 = 30 percent x 0.60 | 18.00 |
| Damage | 2 items x 50 | 100.00 |
| **TOTAL** | | **558.00** |

Vehicle LEA-101 is now **AVAILABLE** and its mileage is **15800**.

### 5.5 Invoice again (FR25)
Select AGR-0001 -> **View Invoice of Selected** -> the same 558.00 invoice.

### 5.6 Maintenance option (FR23)
Do one more short rental with the "Vehicle needs maintenance" box ticked:
1. CUS-0001 + LEB-202, `2026-12-20` to `2026-12-21` -> Create -> Confirm.
2. Handover: pickup `2026-12-20 09:00`, mileage 30000, fuel 100, damage empty.
3. Return: `2026-12-21 09:00`, mileage 30100, fuel 100, damage empty, **tick "Vehicle needs maintenance"**.

**Expected:** total = 50.00 (no extra charges) and vehicle LEB-202 becomes **UNDER_MAINTENANCE**.
Note: the return only changes the vehicle status. A maintenance **record is not created automatically**. Fleet staff must create a record for LEB-202 in the Maintenance tab (e.g. type `Brake check`, cost `40`, status `IN_PROGRESS`). When the work is done, set the record to `COMPLETED` and the vehicle becomes AVAILABLE again.

---

## 6. Rental Staff: Payments (FR24, FR25)

**Payments** tab, Agreement `AGR-0001`.

| # | Amount | Method | Expected |
|---|---|---|---|
| 6.1 | 300 | Cash | Receipt `PAY-0001`; balance 258.00 |
| 6.2 | 500 | Card | Refused: amount is more than the balance (258.00) |
| 6.3 | 0 or -10 | Cash | "must be greater than zero" |
| 6.4 | 258 | Card | Receipt `PAY-0002`; balance **0** |

Select the agreement and click **View Invoice**: both payments are listed and the balance is 0.00.

Now Customers tab -> select Ali Raza -> **Rental History**: AGR-0001 (and the one from 5.6) must be listed (FR3).

---

## 7. Fleet Staff: Maintenance (FR26, FR27)

Login: `fleet` / `fleet123` -> **Maintenance** tab.

### 7.1 Create a record (FR26)

| Field | Value |
|---|---|
| Vehicle registration no | LEC-303 |
| Maintenance type | Oil change |
| Description | Engine oil and filter replacement |
| Cost | 85 |
| Completion status | PENDING |

Click **Create Maintenance Record**.
**Expected:** `MNT-0001` appears; in the Vehicles tab LEC-303 is **UNDER_MAINTENANCE**.

### 7.2 Maintenance blocks reservations (FR27)
Login as Rental Staff and try to reserve LEC-303 for CUS-0001.
**Expected:** "Vehicle cannot be reserved (status: UNDER_MAINTENANCE)". LEC-303 must also be missing from the "Show Available Vehicles" list (like Step 4.1).

### 7.3 Status update
1. Fleet -> select MNT-0001 -> status `IN_PROGRESS` -> set status -> vehicle stays **UNDER_MAINTENANCE**.
2. Set status `COMPLETED` -> vehicle becomes **AVAILABLE**.

### 7.4 Maintenance on a rented vehicle
While a vehicle is RENTED (after 5.1, before the return) try to create a PENDING maintenance record for it.
**Expected:** "Vehicle is currently rented - it cannot be sent to maintenance now".

### 7.5 Invalid input
Cost `-10` -> "Cost cannot be negative". Empty maintenance type -> "Maintenance type is required".

### 7.6 History (FR10)
Check the maintenance history of LEC-303 in the Vehicles tab or in the report (Step 8.3).

---

## 8. Reports (FR28 - FR31)

The **Reports** tab is visible to Rental Staff, Fleet Staff and Admin.

| # | Report | Input | Expected |
|---|---|---|---|
| 8.1 | Fleet status (FR28) | Status: Any, then RENTED / AVAILABLE | Every vehicle with its status; the filter works |
| 8.2 | Customer history (FR29) | `CUS-0001` | AGR-0001 (total 558.00, return date, payments) and AGR-0002 |
| 8.3 | Vehicle history (FR30) | `LEA-101`, then `LEC-303` | LEA-101: AGR-0001. LEC-303: maintenance MNT-0001 (cost 85.00) |
| 8.4 | Revenue (FR31) | From: a date before today, To: a date after today | `PAY-0001` 300.00, `PAY-0002` 258.00, **TOTAL REVENUE: 558.00** |
| 8.5 | Revenue for a future range (2030-01-01 to 2030-12-31) | | No payments, total 0.00 |
| 8.6 | Customer history for `CUS-9999` | | "customer not found" error |

(A payment's date is the day you recorded it, i.e. today, so the revenue range must include today.)

---

## 9. Admin: everything with one login

Login: `admin` / `admin123`.
1. Customers tab: register a new customer (e.g. Bilal Ahmed, 0345-1112223, 35202-5550001-3, LIC-2025-10, expiry 2031-06-30) -> `CUS-0003`.
2. Vehicles tab: register a new vehicle (LED-404, Honda, Civic, 2023, SEDAN, Automatic, Petrol, 5000, 120).
3. Reservations tab: create a reservation and confirm it.
4. Maintenance tab: create a record.

**Expected:** the admin can do both the rental work and the fleet work. No "not authorized" error appears.

---

## 10. Database persistence (most important)

1. Close the program window.
2. Start it again and login.
3. In every tab click **Show All / Refresh**.

**Expected:** all customers, vehicles, reservations, agreements, payments and maintenance records are still there, with the same statuses (LEA-101 AVAILABLE, mileage 15800, AGR-0001 total 558.00).

4. Register one more customer. Its ID continues after the existing ones (e.g. `CUS-0004`) with no clash.
5. Open SSMS (or Azure Data Studio), select the database `VehicleRental` and run:
```sql
SELECT * FROM Customer;
SELECT * FROM Vehicle;
SELECT * FROM Reservation;
SELECT * FROM RentalAgreement;
SELECT * FROM Payment;
SELECT * FROM MaintenanceRecord;
SELECT staffID, name, role, username FROM Staff;   -- passwords are stored only as a hash
```
**Expected:** each table contains the same data as the GUI. The Staff table has no plain-text password, only `passwordHash`.

---

## 11. Placeholders (hint text)

On every tab, an empty text field shows a grey italic hint (for example National ID: `00000-0000000-0`, dates: `yyyy-MM-dd`). Check that:
- the hint disappears as soon as you type and comes back when the field is empty again,
- after **Clear Form** the hints are visible again,
- clicking on the hint places the cursor in the field.

---

## 5-minute demo for the evaluation

1. Login as `rental` -> Customers: register Ali Raza.
2. Logout, login as `fleet` -> Vehicles: register LEA-101 (show that only 3 tabs are visible).
3. Logout, login as `rental` -> Reservations: create and confirm (estimated 300).
4. Rentals: handover, then return (invoice 558.00 with all four charges).
5. Payments: pay 300 + 258, show the receipt.
6. Login as `fleet` -> Maintenance: create a record, the vehicle becomes UNDER_MAINTENANCE.
7. Reports: revenue 558.00.
8. Close and reopen the program: the data is still there (database).

---

## Result sheet (tick as you go)

| Section | Pass? | Notes / problem |
|---|---|---|
| 1 Login and roles | | |
| 2 Vehicles | | |
| 3 Customers | | |
| 4 Reservations | | |
| 5 Rentals | | |
| 6 Payments | | |
| 7 Maintenance | | |
| 8 Reports | | |
| 9 Admin | | |
| 10 Database persistence | | |
| 11 Placeholders | | |