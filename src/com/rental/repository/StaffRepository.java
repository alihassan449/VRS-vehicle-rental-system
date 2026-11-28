package com.rental.repository;

import java.util.List;
import java.util.Optional;

import com.rental.model.Staff;

/** Storage of staff members and their login accounts (username + password). */
public interface StaffRepository {

    /** Adds a staff member together with a login account. */
    void addAccount(Staff staff, String username, String password);

    /** Returns the staff member if username and password are correct, otherwise empty. */
    Optional<Staff> authenticate(String username, String password);

    List<Staff> findAll();

    boolean isEmpty();
}
