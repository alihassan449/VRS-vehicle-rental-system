# SQL Server setup (step by step)

The program stores all data in **SQL Server** (database `VehicleRental`). The database and all
tables are created automatically on the first start. You only need to do the 4 steps below once.

## Step 1 - SQL Server must be reachable over TCP/IP
(Azure Data Studio / SSMS can connect without TCP/IP, but our Java program always uses TCP/IP. `sa` login additionally needs Mixed Mode - skip points 1-2 if you use Windows Authentication.)
1. SSMS -> right-click the server -> Properties -> Security -> choose **SQL Server and Windows Authentication mode**.
2. SSMS -> Security -> Logins -> `sa` -> Properties: set a password, Status = **Enabled**.
3. Start menu -> **SQL Server Configuration Manager** -> SQL Server Network Configuration ->
   Protocols for MSSQLSERVER (or SQLEXPRESS) -> **TCP/IP = Enabled**.
4. Restart the SQL Server service (SQL Server Services -> right-click -> Restart).

(If you use SQL Server Express the instance name is usually `SQLEXPRESS` - see Step 3.)

## Step 2 - Download the JDBC driver (the only extra file needed)
1. Search "Microsoft JDBC Driver for SQL Server" and download it (or take it from Maven Central: `mssql-jdbc`).
2. You need the jar whose name ends with **`.jre11.jar`** (your laptop has JDK 11), e.g. `mssql-jdbc-12.x.x.jre11.jar`.
3. Create a folder `lib` in the project root (next to `src`) and copy the jar into it.

## Step 2b - Only for Windows Authentication (auth=windows): the auth DLL
1. In the same download (the .zip/.tar of the Microsoft JDBC driver) open the folder `auth\x64`.
2. Copy `mssql-jdbc_auth-12.x.x.x64.dll` (same version as your jar) into the project `lib` folder.
3. Use a **64-bit** JDK (normal on Windows). The program must then be started with `-Djava.library.path=lib` (Step 4).
Skip this step if you use `auth=sql` (sa login).

## Step 3 - Create db.properties
Copy `db.properties.example` to `db.properties` (project root, next to `src`) and edit it:
Windows Authentication (same as Azure Data Studio "Windows Authentication"):
```
server=localhost
instance=SQLEXPRESS
database=VehicleRental
auth=windows
```
or SQL login:
```
server=localhost
instance=SQLEXPRESS
database=VehicleRental
auth=sql
user=sa
password=your_sa_password
```
(Default instance instead of Express: leave `instance=` empty and set `port=1433`.)

## Step 4 - Compile and run (from the project root, the folder that contains `src`)
```
javac -d bin -sourcepath src src/com/rental/Main.java
java -Djava.library.path=lib -cp "bin;lib/*" com.rental.Main
```
(`-Djava.library.path=lib` is needed only for `auth=windows`; it does no harm otherwise.)
(The driver is needed only at run time, so the compile command has no `lib`. On Windows the
separator in `-cp` is `;`.)

The title bar of the login window shows **Storage: SQL Server database VehicleRental** when the
connection works. Default accounts (created on first start): `admin/admin123`, `rental/rental123`, `fleet/fleet123`.

## Console demo (no database, no GUI)
```
javac -d bin -sourcepath src src/com/rental/ClassesDemo.java
java -cp bin com.rental.ClassesDemo
```

## Problems
| Message | Reason / fix |
|---|---|
| `No suitable driver found` | the jar is not on the classpath: check `lib` folder and use `-cp "bin;lib/*"` |
| `The TCP/IP connection to the host ... failed` | TCP/IP not enabled or service not restarted (Step 1); for Express set `instance=SQLEXPRESS` and start the "SQL Server Browser" service |
| `This driver is not configured for integrated authentication` | auth DLL missing or not found: do Step 2b and start with `-Djava.library.path=lib` |
| `Login failed for user 'sa'` | wrong password, `sa` disabled, or Mixed Mode not enabled (Step 1) |
| `Cannot read db.properties` | the file must be in the folder from which you run `java` (project root) |

When the database cannot be used, the program asks whether to **continue without database**
(demo only, data is lost on exit).

## Look at the data / start again from zero
* SSMS -> Databases -> VehicleRental -> Tables: Staff, Customer, Vehicle, Reservation, RentalAgreement, Payment, MaintenanceRecord.
* To start from zero: `DROP DATABASE VehicleRental;` in SSMS (it is created again on the next start).
