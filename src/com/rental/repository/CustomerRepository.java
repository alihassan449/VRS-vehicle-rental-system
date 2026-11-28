package com.rental.repository;

import java.util.List;
import java.util.Optional;

import com.rental.model.Customer;

/**
 * FR1-FR2: storage and lookup of Customer records.
 */
public interface CustomerRepository {

    void save(Customer customer);

    Optional<Customer> findById(String customerId);

    // FR2: search by customer ID, national ID or name
    List<Customer> findByNationalId(String nationalId);

    List<Customer> findByNameContains(String namePart);

    // UC-01: duplicate checks
    List<Customer> findByDrivingLicenseNumber(String licenseNumber);

    List<Customer> findAll();
}
