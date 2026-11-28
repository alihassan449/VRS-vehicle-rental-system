package com.rental.repository;

import java.util.List;
import java.util.Optional;

import com.rental.model.Customer;

/** Storage of Customer objects (FR1, FR2). */
public interface CustomerRepository {
    void save(Customer customer);
    Optional<Customer> findById(String customerID);
    Optional<Customer> findByNationalID(String nationalID);
    List<Customer> findByNameContains(String text);
    List<Customer> findAll();
}
