# Vehicle Rental System - Test Scenarios (sample runs)

Har step ke saath **Expected** likha hai. Jo expected se alag aaye, wo note kar lo.

**Pehle ye samajh lo**
- Dates ka format: `yyyy-MM-dd` (jaise `2026-11-29`). Date-time ka format: `yyyy-MM-dd HH:mm` (jaise `2026-11-29 10:00`).
- Neeche ki dates is maan par hain ke aaj **2026-11-28** hai. Agar laptop ki date alag ho to sab dates utni hi aage/peeche kar lo (sirf Step 5 ka "2 din late" wala hisaab barqarar rakhna).
- IDs (`CUS-0001`, `RES-0001`...) tab milenge jab database naya ho. Agar pehle test data ban chuka ho to IDs mein agla number aayega. Table se ID dekh kar use karo.
- Fresh start chahiye to SSMS mein `DROP DATABASE VehicleRental;` chalao, program dobara start hoga to naya ban jayega.
- Har tab mein Enter ya Down Arrow dabane se agli field par jana chahiye.

---

## 1. Login aur roles

| # | Kaam | Expected |
|---|---|---|
| 1.1 | `rental` / `wrongpass` se login | "Wrong username or password" |
| 1.2 | Khaali username ya password chhod kar Login | "Please enter username and password" |
| 1.3 | `rental` / `rental123` | Sirf 5 tabs: Customers, Reservations, Rentals, Payments, Reports. Header mein "Ali Hassan (Rental Staff)" |
| 1.4 | Logout, phir `fleet` / `fleet123` | Sirf 3 tabs: Vehicles, Maintenance, Reports |
| 1.5 | Logout, phir `admin` / `admin123` | Sab 7 tabs |
| 1.6 | Capital letters mein `ADMIN` / `admin123` | Login ho jana chahiye (username case-insensitive hai) |

---

## 2. Fleet Staff: Vehicles (FR6 - FR10)

Login: `fleet` / `fleet123` -> **Vehicles** tab.

**Check:** form chhota hai aur neeche table mein vehicles saaf nazar aate hain (aap ka purana masla).

### 2.1 Teen vehicles register karo (FR6, FR7)

| Reg no | Make | Model | Year | Category | Transmission | Fuel | Mileage | Daily rate |
|---|---|---|---|---|---|---|---|---|
| LEA-101 | Toyota | Corolla | 2022 | SEDAN | Automatic | Petrol | 15000 | 100 |
| LEB-202 | Suzuki | Alto | 2021 | ECONOMY | Manual | Petrol | 30000 | 50 |
| LEC-303 | Toyota | Hilux | 2023 | SUV | Manual | Diesel | 8000 | 150 |

**Expected:** teeno table mein status **AVAILABLE** ke saath.

### 2.2 Galat input

| # | Kaam | Expected error |
|---|---|---|
| 2.2a | LEA-101 dobara register | "Vehicle LEA-101 is already registered" |
| 2.2b | Daily rate `0` | "Daily rental rate must be greater than zero" |
| 2.2c | Mileage `-5` | "Mileage cannot be negative" |
| 2.2d | Year mein `abc` | valid model year ka error |
| 2.2e | Registration number khaali | "Registration number is required" |

### 2.3 Search (FR9)

| Search | Expected |
|---|---|
| Reg. No. `LEA` | sirf LEA-101 |
| Category SUV | sirf LEC-303 |
| Status AVAILABLE | teeno |
| Show All | teeno |

### 2.4 Status (FR8)

1. LEC-303 select -> **Mark Selected INACTIVE** -> status **INACTIVE**.
2. Dobara select -> **Reactivate Selected** -> **AVAILABLE**.
3. Search mein Status = INACTIVE -> pehle INACTIVE karke dekho, wo list mein aata hai.
4. Phir wapas Reactivate kar do (aage ke tests ke liye).

---

## 3. Rental Staff: Customers (FR1 - FR5)

Login: `rental` / `rental123` -> **Customers** tab.

### 3.1 Do customers register karo (FR1)

| Field | Customer 1 | Customer 2 (expired license) |
|---|---|---|
| Customer ID | (khaali chhodo) | (khaali chhodo) |
| Name | Ali Raza | Sana Malik |
| Contact number | 0300-1234567 | 0321-7654321 |
| National ID | 35202-1234567-1 | 35201-7654321-2 |
| Address | House 12, Gulberg, Lahore | Flat 4, Model Town, Lahore |
| Driving license no | LIC-2024-001 | LIC-2019-777 |
| License expiry | 2030-12-31 | 2025-01-01 |

**Expected:** `CUS-0001` aur `CUS-0002` table mein aa jayen.

### 3.2 Galat input

| # | Kaam | Expected |
|---|---|---|
| 3.2a | Customer 1 ka National ID dobara kisi aur naam se | "A customer with National ID 35202-1234567-1 already exists" |
| 3.2b | Name khaali | "... is required" |
| 3.2c | License expiry khaali | "Driving license expiry date is required" |
| 3.2d | License expiry `31-12-2030` (galat format) | date format ka error |

### 3.3 Search (FR2)

| Search text | Expected |
|---|---|
| `Ali` | Ali Raza |
| `CUS-0002` | Sana Malik |
| `35202-1234567-1` | Ali Raza |
| `xyz` | khaali list |

### 3.4 Contact update (FR4)

1. Ali Raza row select karo.
2. Form mein **Address** = `House 50, DHA Phase 5, Lahore` aur **Contact number** = `0333-9876543` likho.
3. **Update Contact Info of Selected** dabao.

**Expected:** table mein nayi address aur contact. Baaki fields (name, national ID) wahi rahein.

### 3.5 Rental history (FR3)
Abhi Ali Raza select karke **Rental History of Selected** dabao -> abhi koi agreement nahi (khaali). Step 6 ke baad dobara dekhna.

---

## 4. Rental Staff: Reservations (FR11 - FR16)

**Reservations** tab.

### 4.1 Available vehicles (FR12)
Availability from `2026-11-29`, to `2026-12-02` -> **Show Available Vehicles**.
**Expected:** LEA-101, LEB-202, LEC-303 teeno.

### 4.2 Reservation banao (FR11, FR14)
Customer `CUS-0001`, vehicle `LEA-101`, pickup `2026-11-29`, return `2026-12-02` -> **Create Reservation**.
**Expected:** `RES-0001`, status **PENDING**, estimated charge **300.00** (3 din x 100).

### 4.3 Refuse hone wale cases

| # | Kaam | Expected |
|---|---|---|
| 4.3a | CUS-0002 (expired license) + LEB-202, 2026-12-10 se 2026-12-12 | "Customer's driving license is expired - reservation refused" (FR5) |
| 4.3b | CUS-0001 + LEB-202, pickup 2026-12-05, return 2026-12-02 | "Return date cannot be before the pickup date" |
| 4.3c | Customer ID `CUS-9999` | "No customer with ID CUS-9999" |
| 4.3d | Vehicle `ZZZ-000` | "No vehicle with registration number ZZZ-000" |

### 4.4 Confirm (FR13, FR15)
RES-0001 select -> **Confirm** -> status **CONFIRMED**.
(Fleet/Admin se Vehicles tab dekho: LEA-101 ab **RESERVED** hona chahiye.)

### 4.5 Overlap (FR13)
CUS-0001 + LEA-101, pickup `2026-12-01`, return `2026-12-03` -> Create.
**Expected:** "Vehicle already has a confirmed reservation overlapping these dates".

### 4.6 Cancel (FR16)
1. CUS-0001 + LEB-202, `2026-12-10` se `2026-12-12` -> Create (**RES-0002**, estimated 100.00).
2. Select -> **Cancel** -> status **CANCELLED**.

### 4.7 Expire (FR15)
1. CUS-0001 + LEB-202, `2026-12-15` se `2026-12-16` -> Create (**RES-0003**, estimated 50.00).
2. Select -> **Mark Expired** -> status **EXPIRED**.

---

## 5. Rental Staff: Rentals - handover aur return (FR17 - FR23)

**Rentals** tab.

### 5.1 Handover (FR17, FR18, FR19)

| Field | Value |
|---|---|
| Confirmed reservation ID | RES-0001 |
| Pickup date/time | 2026-11-29 10:00 |
| Starting mileage | 15000 |
| Fuel level | 80 |
| Existing damage | small scratch on rear bumper |

-> **Create Agreement & Record Handover**.
**Expected:** `AGR-0001` table mein; reservation **COLLECTED**; vehicle LEA-101 **RENTED** (Vehicles tab mein dekho).

### 5.2 Galat handover

| # | Kaam | Expected |
|---|---|---|
| 5.2a | RES-0001 dobara handover | "... only be created from a CONFIRMED reservation (status: COLLECTED)" |
| 5.2b | RES-0002 (cancelled) ka handover | "... CONFIRMED reservation (status: CANCELLED)" |
| 5.2c | Fuel level `150` | "Fuel level must be between 0 and 100 (percent)" |
| 5.2d | Starting mileage `14000` (vehicle ke current mileage 15000 se kam) | "Mileage cannot be less than the vehicle's current mileage (15000.0)" |

### 5.3 Galat return (pehle ye try karo, phir sahi wala)

| # | Kaam | Expected |
|---|---|---|
| 5.3a | Ending mileage `14000` (start se kam) | "Ending mileage cannot be less than the starting mileage" |
| 5.3b | Return date/time `2026-11-28 09:00` (pickup se pehle) | "Return date/time cannot be before the pickup date/time" |
| 5.3c | Fuel `-5` | "Fuel level must be between 0 and 100 (percent)" |

### 5.4 Return aur charges (FR20, FR21, FR22, FR23)

| Field | Value |
|---|---|
| Agreement ID | AGR-0001 |
| Return date/time | 2026-12-04 10:00 |
| Ending mileage | 15800 |
| Fuel level | 50 |
| New damage | cracked windshield;broken side mirror |
| "Vehicle needs maintenance" | tick **nahi** karna |

-> **Record Return & Calculate Charges**.

**Expected invoice (har hisaab check karo):**

| Item | Hisaab | Amount |
|---|---|---|
| Base rental | 3 din x 100 | 300.00 |
| Late return | wapas 2026-12-04, due 2026-12-02 = 2 din x 20 | 40.00 |
| Excess mileage | 800 km chale, free 3 x 200 = 600, 200 km x 0.50 | 100.00 |
| Fuel shortage | 80 -> 50 = 30 percent x 0.60 | 18.00 |
| Damage | 2 items x 50 | 100.00 |
| **TOTAL** | | **558.00** |

Vehicle LEA-101 ab **AVAILABLE**, mileage **15800**.

### 5.5 Invoice dobara (FR25)
AGR-0001 select -> **View Invoice of Selected** -> wahi 558.00 wala invoice.

### 5.6 Maintenance option (FR23)
Ek aur chhota rental karo jisme "Vehicle needs maintenance" tick ho:
1. CUS-0001 + LEB-202, `2026-12-20` se `2026-12-21` -> Create -> Confirm.
2. Handover: pickup `2026-12-20 09:00`, mileage 30000, fuel 100, damage khaali.
3. Return: `2026-12-21 09:00`, mileage 30100, fuel 100, damage khaali, **tick "Vehicle needs maintenance"**.

**Expected:** total = 50.00 (koi extra charge nahi), vehicle LEB-202 **UNDER_MAINTENANCE**.
Dhyan do: return par sirf vehicle ka status UNDER_MAINTENANCE hota hai, maintenance **record khud nahi banta**. Fleet staff ko Maintenance tab mein LEB-202 ke liye record banana hoga (jaise type `Brake check`, cost `40`, status `IN_PROGRESS`), aur kaam ho jaye to status `COMPLETED` karne se vehicle wapas AVAILABLE ho jata hai.

---

## 6. Rental Staff: Payments (FR24, FR25)

**Payments** tab, Agreement `AGR-0001`.

| # | Amount | Method | Expected |
|---|---|---|---|
| 6.1 | 300 | Cash | receipt `PAY-0001` dikhe; balance 258.00 |
| 6.2 | 500 | Card | refuse: amount balance (258.00) se zyada |
| 6.3 | 0 ya -10 | Cash | "must be greater than zero" |
| 6.4 | 258 | Card | receipt `PAY-0002`; balance **0** |

Select karke **View Invoice** -> invoice mein dono payments aur Balance 0.00.

Ab Customers tab -> Ali Raza select -> **Rental History** -> AGR-0001 (aur 5.6 wala) dikhna chahiye (FR3).

---

## 7. Fleet Staff: Maintenance (FR26, FR27)

Login: `fleet` / `fleet123` -> **Maintenance** tab.

### 7.1 Record banao (FR26)

| Field | Value |
|---|---|
| Vehicle registration no | LEC-303 |
| Maintenance type | Oil change |
| Description | Engine oil and filter replacement |
| Cost | 85 |
| Completion status | PENDING |

-> **Create Maintenance Record**.
**Expected:** `MNT-0001` table mein; Vehicles tab mein LEC-303 **UNDER_MAINTENANCE**.

### 7.2 Maintenance vehicle block (FR27)
Rental Staff se login karke Reservations mein CUS-0001 + LEC-303 reserve karo.
**Expected:** "Vehicle cannot be reserved (status: UNDER_MAINTENANCE)". Availability list (4.1 jaisi) mein LEC-303 nahi aana chahiye.

### 7.3 Status update
1. Fleet -> MNT-0001 select -> status `IN_PROGRESS` -> **Set Status** -> vehicle abhi bhi **UNDER_MAINTENANCE**.
2. Status `COMPLETED` -> **Set Status** -> vehicle **AVAILABLE**.

### 7.4 Rented vehicle par maintenance
Jab koi vehicle RENTED ho (5.1 ke baad, return se pehle) us par PENDING record banane ki koshish karo.
**Expected:** "Vehicle is currently rented - it cannot be sent to maintenance now".

### 7.5 Galat input
Cost `-10` -> "Cost cannot be negative". Maintenance type khaali -> "Maintenance type is required".

### 7.6 History (FR10)
Vehicles ya Reports mein LEC-303 ki maintenance history dekho (Step 8.3).

---

## 8. Reports (FR28 - FR31)

Rental, Fleet aur Admin teeno ko **Reports** tab dikhta hai.

| # | Report | Input | Expected |
|---|---|---|---|
| 8.1 | Fleet status (FR28) | Status: Any, phir RENTED/AVAILABLE | har vehicle ka status ke saath list; filter sahi chale |
| 8.2 | Customer history (FR29) | `CUS-0001` | AGR-0001 (total 558.00, return date, payments) aur AGR-0002 |
| 8.3 | Vehicle history (FR30) | `LEA-101` | AGR-0001 aur is vehicle ki maintenance history; `LEC-303` par MNT-0001 (cost 85.00) |
| 8.4 | Revenue (FR31) | From: aaj se pehle ki date, To: aaj se baad ki date | `PAY-0001` 300.00, `PAY-0002` 258.00, **TOTAL REVENUE: 558.00** |
| 8.5 | Revenue, range bhavishya mein (From 2030-01-01 To 2030-12-31) | | koi payment nahi, total 0.00 |
| 8.6 | Customer history `CUS-9999` | | customer nahi mila ka error |

(Payment ki date wo hoti hai jis din aap ne payment record ki, yani aaj ki date.)

---

## 9. Admin: sab kuch ek login se

Login: `admin` / `admin123`.
1. Customers tab se naya customer register karo (jaise Bilal Ahmed, 0345-1112223, 35202-5550001-3, LIC-2025-10, expiry 2031-06-30) -> `CUS-0003`.
2. Vehicles tab se naya vehicle register karo (LED-404, Honda, Civic, 2023, SEDAN, Automatic, Petrol, 5000, 120).
3. Reservations tab se reservation banao aur confirm karo.
4. Maintenance tab se record banao.

**Expected:** admin dono taraf ke kaam kar sakta hai, koi "not authorized" nahi.

---

## 10. Database persistence (sab se zaroori)

1. Program band karo (window close).
2. Dobara run karo, login karo.
3. Har tab mein **Show All / Refresh** dabao.

**Expected:** customers, vehicles, reservations, agreements, payments, maintenance records sab wahin hon, aur statuses bhi (LEA-101 AVAILABLE, mileage 15800, AGR-0001 ka total 558.00 wahi).

4. Ek naya customer register karo -> ID `CUS-0004` (purane IDs se aage), takrao nahi.
5. SSMS (ya Azure Data Studio) mein dekho, database `VehicleRental`, aur chalao:
```sql
SELECT * FROM Customer;
SELECT * FROM Vehicle;
SELECT * FROM Reservation;
SELECT * FROM RentalAgreement;
SELECT * FROM Payment;
SELECT * FROM MaintenanceRecord;
SELECT staffID, name, role, username FROM Staff;   -- password sirf hash hota hai
```
**Expected:** har table mein wohi data jo GUI mein hai. Staff table mein password plain text nahi, sirf `passwordHash` hota hai.

---

## Evaluation ke liye 5-minute short demo

1. `rental` login -> Customers: register Ali Raza.
2. Logout, `fleet` login -> Vehicles: register LEA-101 (dikhao ke sirf 3 tabs hain).
3. Logout, `rental` login -> Reservations: create + Confirm (estimated 300).
4. Rentals: handover, phir return (558.00 ka invoice, 4 charges dikhao).
5. Payments: 300 + 258, receipt.
6. `fleet` -> Maintenance: record banao, vehicle UNDER_MAINTENANCE.
7. Reports: revenue 558.00.
8. Program band karke dobara kholo -> data wahin (database).

---

## Result sheet (tick karte jao)

| Section | Pass? | Notes / problem |
|---|---|---|
| 1 Login aur roles | | |
| 2 Vehicles | | |
| 3 Customers | | |
| 4 Reservations | | |
| 5 Rentals | | |
| 6 Payments | | |
| 7 Maintenance | | |
| 8 Reports | | |
| 9 Admin | | |
| 10 Database persistence | | |
