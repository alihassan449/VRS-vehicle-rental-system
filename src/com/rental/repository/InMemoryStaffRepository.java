package com.rental.repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.rental.enums.StaffRole;
import com.rental.model.Staff;
import com.rental.model.StaffFactory;
import com.rental.util.PasswordHasher;

/** Keeps staff accounts in memory. The SQL Server repository extends this class (as its cache). */
public class InMemoryStaffRepository implements StaffRepository {

    /** One login account: the staff member plus the stored password hash. */
    protected static final class Account {
        final Staff staff;
        final String username;
        final String passwordHash;

        Account(Staff staff, String username, String passwordHash) {
            this.staff = staff;
            this.username = username;
            this.passwordHash = passwordHash;
        }
    }

    private final Map<String, Account> accountsByUsername = new LinkedHashMap<>();

    @Override
    public void addAccount(Staff staff, String username, String password) {
        if (username == null || username.trim().isEmpty() || password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Username and password are required");
        }
        String key = username.trim().toLowerCase();
        if (accountsByUsername.containsKey(key)) {
            throw new IllegalArgumentException("Username " + username.trim() + " already exists");
        }
        accountsByUsername.put(key, new Account(staff, key, PasswordHasher.hash(password)));
    }

    /** Used when loading stored accounts (the password is already hashed). */
    protected void putAccount(Staff staff, String username, String passwordHash) {
        accountsByUsername.put(username.toLowerCase(), new Account(staff, username.toLowerCase(), passwordHash));
    }

    @Override
    public Optional<Staff> authenticate(String username, String password) {
        if (username == null || password == null) {
            return Optional.empty();
        }
        Account account = accountsByUsername.get(username.trim().toLowerCase());
        if (account != null && PasswordHasher.verify(password, account.passwordHash)) {
            return Optional.of(account.staff);
        }
        return Optional.empty();
    }

    @Override
    public List<Staff> findAll() {
        List<Staff> result = new ArrayList<>();
        for (Account a : accountsByUsername.values()) {
            result.add(a.staff);
        }
        return result;
    }

    @Override
    public boolean isEmpty() {
        return accountsByUsername.isEmpty();
    }

    /** The three default accounts created on first start (one per role). */
    public static void seedDefaultAccounts(StaffRepository repository) {
        repository.addAccount(StaffFactory.create("STF-0001", "Ali Hassan", StaffRole.RENTAL_STAFF),
                "rental", "rental123");
        repository.addAccount(StaffFactory.create("STF-0002", "Sara Khan", StaffRole.FLEET_STAFF),
                "fleet", "fleet123");
        repository.addAccount(StaffFactory.create("STF-0003", "System Admin", StaffRole.ADMIN),
                "admin", "admin123");
    }
}
