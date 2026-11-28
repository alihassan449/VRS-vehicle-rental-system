package com.rental.repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.rental.model.Customer;

public class InMemoryCustomerRepository implements CustomerRepository {

    private final Map<String, Customer> customers = new LinkedHashMap<>();

    @Override
    public void save(Customer customer) {
        customers.put(customer.getCustomerID(), customer);
    }

    @Override
    public Optional<Customer> findById(String customerID) {
        return Optional.ofNullable(customers.get(customerID));
    }

    @Override
    public Optional<Customer> findByNationalID(String nationalID) {
        for (Customer c : customers.values()) {
            if (c.getNationalID().equalsIgnoreCase(nationalID)) {
                return Optional.of(c);
            }
        }
        return Optional.empty();
    }

    @Override
    public List<Customer> findByNameContains(String text) {
        List<Customer> result = new ArrayList<>();
        String needle = text.toLowerCase();
        for (Customer c : customers.values()) {
            if (c.getName().toLowerCase().contains(needle)) {
                result.add(c);
            }
        }
        return result;
    }

    @Override
    public List<Customer> findAll() {
        return new ArrayList<>(customers.values());
    }
}
