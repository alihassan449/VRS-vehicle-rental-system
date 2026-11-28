# Project Updates Summary — Vehicle Rental Desktop Management System (SDA Final Phase)

This document summarizes all improvements made in the final implementation phase of the **Vehicle Rental Desktop Management System** (Project 3), aligning the codebase with the Software Design and Analysis (SDA) artifacts (`UseCase.drawio.png`, `Vehicle_Rental_Use_Case_Specifications (1).pdf`, `Vehicle_Rental_Sequence_Diagrams.pptx`, and `designclass (1).png`).

---

## 1. Alignment with SDA Phase 1–3 Artifacts

All 16 use cases (**UC-01 to UC-16**, including **UC-12.1 to UC-12.4**) and domain/service abstractions from the design class diagram and sequence diagrams remain faithfully enforced while upgrading the persistence layer and user interface:

| Use Case(s) | SDA Artifact Flow | Final Phase Implementation |
|---|---|---|
| **UC-01** Register Customer + **UC-13** Verify License | Customer details validated, LVA checked, duplicates blocked, saved | Dedicated full-page registration screen in `CustomerPanel` (`PAGE_CREATE`) + `CustomerService` + `SqliteCustomerRepository` |
| **UC-02** Search Customer | Search by Customer ID, National ID, or Name; view profile & agreement subgrid | `CustomerPanel` (`PAGE_DIRECTORY` + dedicated `PAGE_PROFILE`) |
| **UC-03** Update Customer Info | Edit contact number & address on located customer | Dedicated `CustomerPanel` (`PAGE_EDIT`) with customer picker & read-only identity card |
| **UC-04** Register Vehicle | Register under fixed categories with status `Available` | Dedicated `VehiclePanel` (`PAGE_ADD`) + `SqliteVehicleRepository` |
| **UC-05** Search Vehicle | Filter by registration number, category, status; view specs & maintenance history | `VehiclePanel` (`PAGE_DIRECTORY` + dedicated `PAGE_DETAILS`) |
| **UC-06**, **UC-14**, **UC-15** Reservations | Period validation, availability filtering, overlap checks at selection & confirmation | `ReservationPanel` (`PAGE_LIST` + dedicated `PAGE_CREATE`) + `SqliteReservationRepository` |
| **UC-07** Cancel Reservation | Cancel uncollected reservation & release vehicle | `ReservationPanel` (`PAGE_LIST`) + `ReservationService.cancelReservation` |
| **UC-08** Vehicle Handover + **UC-13** | Verify license via LVA, create active agreement, set vehicle `Rented` & reservation `Collected` | `HandoverPanel` (`PAGE_QUEUE` + dedicated `PAGE_PROCESS`) + `RentalService` |
| **UC-09** Vehicle Return + **UC-16** | Return inspection, calculate base + late/mileage/fuel/damage charges, update status | `ReturnPanel` (`PAGE_QUEUE` + dedicated `PAGE_PROCESS`) + `ChargeCalculator` |
| **UC-10** Record Payment & Print Receipt | Partial/full payments, balance update, printable invoice & receipt preview | `PaymentPanel` + `SqlitePaymentRepository` + `Invoice` / `Receipt` |
| **UC-11** Manage Maintenance | Log records, auto-flag damage returns, complete jobs to restore `Available` | `MaintenancePanel` + `SqliteMaintenanceRepository` |
| **UC-12 (.1–.4)** System Reports | Vehicle Status, Customer History, Vehicle History, Revenue Report | `ReportPanel` with a report-catalog sidebar and tabular output for Admin and Rental Staff |
| **Staff Administration** | Administrator creates and deletes Rental/Fleet Staff accounts | Dedicated staff directory and create-staff form in `StaffManagementPanel`, persisted through `AuthService` and `SqliteStaffRepository` |

---

## 2. Persistent SQLite Database Layer

Previously, `Main.java` wired `InMemory*Repository` implementations, causing all created/edited records to disappear when the application closed. The system now persists all operational data permanently in **SQLite** (`data/vehicle_rental.db`).

### Added Dependencies (`lib/`)
- `lib/sqlite-jdbc-3.47.1.0.jar`
- `lib/slf4j-api-2.0.16.jar`
- `lib/slf4j-nop-2.0.16.jar`
- Updated `.vscode/settings.json` with `"java.project.referencedLibraries": ["lib/**/*.jar"]`.

### Database Schema & Repositories (`src/com/rental/repository/db/`)
- **`SqliteDatabase.java`**: Initializes `data/vehicle_rental.db`, enforces `PRAGMA foreign_keys = ON;`, creates 9 normalized tables (`staff`, `customers`, `vehicles`, `reservations`, `rental_agreements`, `agreement_charges`, `maintenance_records`, `payments`, `id_counters`), and synchronizes sequential ID counters (`IdGenerator`) with the highest stored IDs on startup so IDs never collide after restarts.
- **`SqliteStaffRepository.java`**: Stores and authenticates staff accounts (`rental`, `fleet`, `admin`).
- **`SqliteCustomerRepository.java`**: Full CRUD and search queries by Customer ID, National ID, Name, and Driving License number.
- **`SqliteVehicleRepository.java`**: Persists fleet vehicles and hydrates their `MaintenanceRecord` history.
- **`SqliteReservationRepository.java`**: Persists reservations with foreign keys to customers and vehicles.
- **`SqliteRentalAgreementRepository.java`**: Persists handover details, return details, itemized `agreement_charges`, and attached `payments` inside transactions.
- **`SqliteMaintenanceRepository.java`**: Persists maintenance records and completion status.
- **`SqlitePaymentRepository.java`**: Persists payments and supports date-range queries for revenue reports.

### Service & Domain Entity Persistence Fixes
- **Entity Equality (`equals` / `hashCode`)**: Added ID-based `equals` and `hashCode` to `Customer`, `Vehicle`, `Reservation`, `RentalAgreement`, `MaintenanceRecord`, and `Payment` so collections and UI selections remain consistent across fresh database queries.
- **`ReservationService.java`**: Replaced reference equality (`==`) in `isFreeOfOverlap` and `blocksVehicle` with `.equals(...)` so database-loaded entities are compared properly.
- **`RentalService.java`**: Wired `ReservationRepository` so `authorizeHandover` persists the reservation's status transition to `COLLECTED` in SQLite.
- **`MaintenanceService.java`**: Updated `completeRecord` to persist the updated `completed` flag via `maintenanceRepository.save(record)`.
- **`AuthService.java` & `DemoData.java` & `Main.java`**: Wired SQLite repositories by default (`Main.java`), seeding demo staff and sample customers/vehicles only when the database is empty (while preserving `--empty` and `--in-memory` CLI flags).

---

## 3. GUI & UX Redesign (Scalable, Multi-Page, Sidebar-Driven)

### Theme, Typography & Unmistakable Buttons (`UiTheme.java`, `UiUtil.java`)
- Added custom-painted rounded button variants (`RoundedButtonUI`) so Nimbus Look & Feel never renders flat or ambiguous buttons:
  - `primaryButton` (solid blue `#1D4ED8`, white bold text)
  - `successButton` (solid green `#15803D`, white bold text)
  - `dangerButton` (solid red `#B91C1C`, white bold text)
  - `warningButton` (solid amber `#B45309`, white bold text)
  - `secondaryButton` (crisp white background, visible slate border `#94A3B8`, dark text)
- Added standardized layout helpers in `UiUtil`: `pageHeader(...)`, `card(...)`, `kpiCard(...)`, `badge(...)`, `addFormRow(...)` (with inline field format hints), and `scrollable(...)` (smooth scrollbars so nothing clips on smaller screens).

### Persistent Role Sidebar & Dashboard (`MainFrame.java`, `DashboardPanel.java`, `Navigator.java`)
- Replaced the single-row `JTabbedPane` with a **245px scrollable dark-slate left sidebar** + **`CardLayout` main workspace** (default window size `1280x780`, minimum `1024x680`).
- Added **`DashboardPanel`**: Role-specific home screen showing 4 live SQLite KPI cards and quick-action navigation cards.
- Added a **Sidebar Footer** displaying DB persistence status (`DB: SQLite Persistent`), a clickable **LVA Status toggle** (`LVA: Online` / `Outage Simulated`), and a prominent **Log Out** button.

### Multi-Page Customer Management (`CustomerPanel.java`)
Replaced the single cramped screen and modal registration popup with **4 dedicated full pages** navigable from both the left sidebar and in-page buttons:
1. **Customer Directory (`PAGE_DIRECTORY`, UC-02)**: Search bar, full customer table with license validity column, `+ Register New Customer` header CTA, and an explicit bottom action bar (`View Profile & History`, `Edit Customer`, `Create Reservation`).
2. **Register New Customer (`PAGE_CREATE`, UC-01 + UC-13)**: Dedicated full scrollable form page with `Auto-Generate ID`, `+3 Years` expiry helper, live LVA status badge, field hints, and `Save & Verify License` CTA (preserving form inputs on validation/LVA errors).
3. **Customer Profile & Rental History (`PAGE_PROFILE`, UC-02 step 4)**: Dedicated profile view with Identity/Contact card, Driving License validity badge (`VALID`/`EXPIRED`), `Edit Customer Info` and `+ Create Reservation` buttons, and a full-width rental agreement history table.
4. **Edit Customer Info (`PAGE_EDIT`, UC-03)**: Dedicated edit page featuring a customer selector (for direct sidebar entry), read-only identity summary card, editable Contact Number & Address form, and `Save Changes` / `Reset` / `Cancel` buttons.

### Multi-Page Upgrades Across All Other Modules
- **Fleet Management (`VehiclePanel.java`)**: Split into 3 dedicated pages — `PAGE_DIRECTORY` (UC-05), `PAGE_ADD` (UC-04 full-page vehicle registration replacing `AddVehicleDialog`), and `PAGE_DETAILS` (vehicle specifications + maintenance history + shortcut to Maintenance Records).
- **Reservations (`ReservationPanel.java`)**: Split into `PAGE_LIST` (search + status filter + cancel + handover shortcut) and `PAGE_CREATE` (full-page 4-step reservation wizard replacing `CreateReservationDialog`, supporting pre-selected customers from Customer Profile).
- **Vehicle Handover (`HandoverPanel.java`)**: Split into `PAGE_QUEUE` (confirmed reservation filter + details) and `PAGE_PROCESS` (dedicated full-page draft agreement & handover authorization replacing `HandoverDialog`).
- **Vehicle Return (`ReturnPanel.java`)**: Split into `PAGE_QUEUE` (active agreements) and `PAGE_PROCESS` (dedicated full-page return inspection + UC-16 charge calculator replacing `ReturnDialog`).
- **Payments & Receipts (`PaymentPanel.java`)**: Added `All Agreements` / `Balance Due` / `Paid in Full` filter, `Fill Full Balance` shortcut, side-by-side summary/payment cards, and prominent `Record Payment` + `View / Print Invoice & Receipt` buttons.
- **Maintenance Records (`MaintenancePanel.java`)**: Added live flagged-vehicles badge (`N Flagged by Returns`), pre-selection support from Fleet Directory, and clear `Save Maintenance Record` + `Mark Selected Record Completed` actions.
- **System Reports (`ReportPanel.java`)**: Added a dedicated left **Report Catalog sidebar** for UC-12.1 through UC-12.4, parameter bar, `Generate Report` CTA, and `Print Current Report` preview button.
- **Login Screen (`LoginDialog.java`)**: Upgraded with card layout and clear primary `Log In` button.

### Run Scripts
- Added `run.bat` and `run.ps1` for one-click compilation and execution with `lib/*` on the classpath.

---

## 4. Verification Performed

- Compiled the entire `src/` tree cleanly with `javac -cp "lib/*" -d bin ...` using OpenJDK 21 (`javac 21.0.12.1`).
- Executed an end-to-end verification suite (`VerifySystem.java`) testing:
  - Staff authentication against SQLite (`rental`, `fleet`, `admin`)
  - UC-01, UC-03, UC-04, UC-06, UC-08, UC-09, UC-10, UC-11, UC-12.1–12.4, UC-13, UC-14, UC-15, UC-16
  - Overlap rejection during reservation booking
  - Persistence across multiple simulated restarts (re-opening fresh `AppContext` instances from the same SQLite DB file)
  - Sequential ID continuity across restarts without ID collisions
  - `MainFrame` instantiation and route navigation across all 3 roles (`RENTAL_STAFF`, `FLEET_STAFF`, `ADMIN`).

---

## 5. UI Accessibility & Presentation Refresh (Final Pass)

This final phase rebuilds the presentation layer around **first-glance usability**: every screen is designed so a first-time user, without training, can tell what to do and where to click. **No use case, service call, domain rule, persistence behaviour, navigation route, or sequence of procedures was changed.** Every change below lives purely in `com.rental.ui.*` and is cosmetic (fonts, spacing, colours, glyphs, layout labels).

### 5.1 Design goals

| Goal | What it means in the app |
|---|---|
| **Big, readable text** | Body 15px, buttons 16px bold, page headers 26px bold, KPI numbers 32px bold, table rows 40px tall. |
| **Obvious click targets** | Every action button is at least **48px tall**; inputs **38px**; sidebar links **46px**. No tiny hit areas. |
| **Self-explaining actions** | Every button carries a leading glyph so its purpose is visible before reading the label (`＋ Register`, `✓ Save`, `✖ Cancel`, `🔍 Search`, `🖨 Print`, `⇄ Handover`, `↩ Return`, `⚙ Maintenance`, `⏻ Log Out`, `▶ Generate`, `↻ Refresh`, `👁 View`, `✎ Edit`). |
| **Clear visual hierarchy** | Green top banner (title + subtitle + signed-in user) → page header (title + subtitle + header CTA) → section cards with visible borders → action footer. |
| **Numbered workflows** | Multi-step flows use ① ② ③ ④ step numbers on section titles so the order is obvious at a glance (Reservation wizard, Maintenance, Handover, Return). |
| **Consistent colour coding** | Blue = primary/safe, Green = confirm/save, Red = destructive/cancel, Amber = edit/warning. Colours are used **only** to signal meaning, never decoratively. |

### 5.2 Palette update (`UiTheme`)

The palette was already high-contrast; this pass adds a single banner colour entry and confirms WCAG-AA contrast (≥ 4.5:1) for all text:

| Token | Value | Use |
|---|---|---|
| `BANNER_GREEN` | `#2E7D32` | Top-of-app banner background (replaces previous dark blue) |
| `BANNER_SUBTITLE` | `#DCFCE7` | Subtitle text on the green banner |
| `TEXT_PRIMARY` | `#0F172A` | Default body text |
| `TEXT_MUTED` | `#475569` | Captions and hints |
| `PRIMARY` | `#1D4ED8` | Primary actions |
| `SUCCESS` | `#15803D` | Confirmation actions |
| `DANGER` | `#B91C1C` | Cancel / destructive |
| `WARNING` | `#B45309` | Edit / maintenance |
| `SIDEBAR_BG` | `#0F172A` | Left navigation panel |
| `SIDEBAR_SECTION` | `#94A3B8` | Section headers in sidebar (brighter for readability) |

### 5.3 Typography scale (`UiTheme`)

| Token | Size / weight | Used for |
|---|---|---|
| `PAGE_TITLE_FONT` | 26px bold | Page headers |
| `HEADER_FONT` | 22px bold | Banner title |
| `TITLE_FONT` | 18px bold | Section titles, cards |
| `BUTTON_FONT` | 16px bold | All action buttons |
| `LABEL_FONT` / `LABEL_BOLD` | 15px / 15px bold | Field labels and read-only values |
| `SUBTITLE_FONT` | 14px | Sub-headings, page subtitles |
| `SMALL_FONT` | 13px | Hints, footer badges |
| `KPI_FONT` | 32px bold | Dashboard KPI numbers |
| `TABLE_ROW_HEIGHT` | 40px | Every table |
| `INPUT_HEIGHT` | 38px | Every text field / combo box |
| `BUTTON_HEIGHT` | 48px | Every action button |

### 5.4 Screen-by-screen summary

| Screen | Key accessibility changes |
|---|---|
| **Login** | Bigger card (min 540px), 38px fields, `🔓 Log In` CTA, larger demo-accounts hint box, green banner preserved via `ui.preserveBg`. |
| **Main frame** | Green banner (title, subtitle, signed-in user); sidebar widened to **260px** with 46px links and `⌂ / ⌕ / ＋ / ✎ / ☰ / ⇄ / ↩ / ＄ / ⚙ / ▤` glyphs. Footer status `✓ DB: SQLite Persistent`, `✓ LVA: Online` / `⚠ LVA: Outage`, `⏻ Log Out`. |
| **Dashboard** | Four 32px KPI cards; each action tile has a 20px title, 14px description, and a glyph-prefixed CTA. Section header `Quick Actions & Workflows` at 20px. |
| **Customer module** | Directory: full-width filter bar (`Search by` + `Keyword` + `🔍 Search` + `↻ Show All`), 40px rows, big bottom actions (`👁 View Profile & History`, `✎ Edit Customer`, `＋ Create Reservation`). Register (UC-01): form per-field hints with `⚡ Auto-Generate ID` and `📅 +3 Years` helpers. Profile: two-column identity/license cards with `✓ VALID` / `✖ EXPIRED` badges. Edit (UC-03): read-only identity card + editable contact/address card. |
| **Fleet module** | Directory: labeled filters (`Registration No. / Category / Status`), `🔍 Search` / `↻ Show All`, 40px rows, big bottom actions. Add (UC-04): all 9 fields with hints. Details (UC-05): two-column spec card + maintenance history table. |
| **Reservations** | Sidebar list and detail panel. UC-06 creates a **Pending** reservation; selected reservations expose Confirm, Cancel, or Expire status actions. Confirmed reservations proceed to UC-08 Handover. |
| **Maintenance** | Dedicated vehicle list, UC-11 maintenance form, and maintenance-history pages. The form is scrollable and uses placeholders, helper text, Clear/Save actions, and separate completion controls. |
| **Staff Management** | Administrator directory plus dedicated Add/Create Staff page with Staff Pages sidebar, generated staff ID, placeholders, password validation, role selection, save, and delete controls. |
| **Reports** | 280px catalog sidebar with `▤ UC-12.1` / `👤 UC-12.2` / `🚗 UC-12.3` / `＄ UC-12.4` buttons; `▶ Generate Report`; larger output text area; `🖨 Print Current Report`. |

### 5.5 Technical notes

- **New theme constants** added to `UiTheme`: `BANNER_GREEN`, `BANNER_SUBTITLE`, `KPI_FONT`, `INPUT_HEIGHT`, `BUTTON_HEIGHT`, `TABLE_ROW_HEIGHT`. All existing constants (names and values) preserved.
- **`UiTheme.banner(...)` signature unchanged** (`String, String, Component`); only the internal colours changed. Banner now sets `ui.preserveBg` so `UiTheme.style(...)` cannot overwrite it.
- **`UiTheme.style(Container)` behavior preserved**: buttons without a variant become `SECONDARY`; opaque `JPanel`s without `ui.preserveBg` get `BACKGROUND`; `TitledBorder` fonts/colours reapplied; keyboard `ENTER`/`DOWN` navigation preserved.
- **`UiUtil`** gained no breaking changes; `showPrintPreview(...)` and `printText(...)` signatures preserved. `kpiCard(...)`, `titled(...)`, `card(...)`, `pageHeader(...)`, `badge(...)`, `flow(...)` / `flowRight(...)` all keep their existing signatures.
- **Zero service / domain changes**: every `*Service`, `*Repository`, enum, model and exception class is untouched. Every `Navigator` route constant and every `showPage(...)` / `refresh()` contract is identical.
- **Legacy dialogs** (`RegisterCustomerDialog`, `AddVehicleDialog`, `CreateReservationDialog`) were retained and restyled for compatibility; the full-page flows in `CustomerPanel`, `VehiclePanel`, and `ReservationPanel` remain the primary UI.
- **Compilation target:** unchanged (`javac -cp "lib/*" -d bin src/...`) — no new dependencies, no new imports beyond standard Swing + already-present project classes.