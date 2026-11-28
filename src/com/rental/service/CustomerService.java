package com.rental.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.rental.model.Customer;
import com.rental.model.RentalAgreement;
import com.rental.repository.CustomerRepository;
import com.rental.repository.RentalAgreementRepository;
import com.rental.util.IdGenerator;

/**
 * Diagram class: CustomerService (FR1-FR4).
 * registerCustomer(...), searchCustomer(query) and getRentalHistory(customerID) are the
 * diagram methods; the rest are small helpers used by the controller.
 */
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final RentalAgreementRepository agreementRepository;

    public CustomerService(CustomerRepository customerRepository, RentalAgreementRepository agreementRepository) {
        this.customerRepository = customerRepository;
        this.agreementRepository = agreementRepository;
    }

    /** FR1: register a customer. A blank customerID is replaced by a generated one. */
    public Customer registerCustomer(String customerID, String name, String address, String contactNumber,
                                     String nationalID, String drivingLicenseNo, LocalDate licenseExpiryDate) {
        require(name, "Name");
        require(contactNumber, "Contact number");
        require(nationalID, "National ID");
        require(drivingLicenseNo, "Driving license number");
        if (licenseExpiryDate == null) {
            throw new IllegalArgumentException("Driving license expiry date is required");
        }
        String id = (customerID == null || customerID.trim().isEmpty())
                ? IdGenerator.next("CUS") : customerID.trim();
        if (customerRepository.findById(id).isPresent()) {
            throw new IllegalArgumentException("Customer ID " + id + " already exists");
        }
        if (customerRepository.findByNationalID(nationalID.trim()).isPresent()) {
            throw new IllegalArgumentException("A customer with National ID " + nationalID.trim() + " already exists");
        }
        Customer customer = new Customer(id, name.trim(), address == null ? "" : address.trim(),
                contactNumber.trim(), nationalID.trim(), drivingLicenseNo.trim(), licenseExpiryDate);
        customerRepository.save(customer);
        return customer;
    }

    /** FR2: search by customer ID, national ID or (part of) the name. */
    public List<Customer> searchCustomer(String query) {
        List<Customer> results = new ArrayList<>();
        if (query == null || query.trim().isEmpty()) {
            return results;
        }
        String q = query.trim();
        customerRepository.findById(q).ifPresent(results::add);
        customerRepository.findByNationalID(q).ifPresent(c -> {
            if (!results.contains(c)) {
                results.add(c);
            }
        });
        for (Customer c : customerRepository.findByNameContains(q)) {
            if (!results.contains(c)) {
                results.add(c);
            }
        }
        return results;
    }

    /** FR3: all rental agreements of a customer. */
    public List<RentalAgreement> getRentalHistory(String customerID) {
        return agreementRepository.findByCustomerId(customerID);
    }

    /** FR4: update contact information of an existing customer. */
    public void updateContactInfo(String customerID, String address, String contactNumber) {
        Customer customer = findById(customerID);
        customer.updateContactInfo(address, contactNumber);
        customerRepository.save(customer);
    }

    public Customer findById(String customerID) {
        if (customerID == null) {
            throw new IllegalArgumentException("Customer ID is required");
        }
        return customerRepository.findById(customerID.trim())
                .orElseThrow(() -> new IllegalArgumentException("No customer with ID " + customerID.trim()));
    }

    public List<Customer> findAll() {
        return customerRepository.findAll();
    }

    private void require(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
    }
}
