package com.rental.repository.sqlserver;

import com.rental.model.Staff;
import com.rental.repository.InMemoryStaffRepository;
import com.rental.util.PasswordHasher;

/** Staff accounts kept in memory for login and stored in the Staff table (password only as a hash). */
public class SqlServerStaffRepository extends InMemoryStaffRepository {

    private final SqlServerDatabase db;

    public SqlServerStaffRepository(SqlServerDatabase db) {
        this.db = db;
    }

    @Override
    public void addAccount(Staff staff, String username, String password) {
        if (username == null || username.trim().isEmpty() || password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Username and password are required");
        }
        String hash = PasswordHasher.hash(password);
        db.insertStaff(staff, username.trim().toLowerCase(), hash);
        putAccount(staff, username.trim(), hash);
    }

    /** Used while loading from the database (no write back). */
    public void cache(Staff staff, String username, String passwordHash) {
        putAccount(staff, username, passwordHash);
    }
}
