package com.rental.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.rental.enums.CustomerSearchBy;
import com.rental.enums.LicenseStatus;
import com.rental.exception.BusinessRuleException;
import com.rental.exception.ValidationException;
import com.rental.model.Address;
import com.rental.model.Customer;
import com.rental.model.DrivingLicense;
import com.rental.repository.CustomerRepository;

/**
 * UC-01 Register Customer, UC-02 Search Customer, UC-03 Update Customer Info.
 */
public class CustomerService {
    private final CustomerRepository customerRepository;
    private final LicenseVerificationService licenseVerificationService;

    public CustomerService(CustomerRepository customerRepository,
                           LicenseVerificationService licenseVerificationService) {
        this.customerRepository = customerRepository;
        this.licenseVerificationService = licenseVerificationService;
    }

    /**
     * UC-01 steps 2.1 - 3.3: validate the entered details, verify the license
     * with the LVA (include UC-13), check for duplicates and save.
     */
    public Customer registerCustomer(String customerId, String name, String address, String contactNumber,
                                     String nationalId, String licenseNumber, LocalDate licenseExpiry) {
        // 2.1 validate all details
        customerId = clean(customerId);
        name = clean(name);
        address = clean(address);
        contactNumber = clean(contactNumber);
        nationalId = clean(nationalId);
        licenseNumber = clean(licenseNumber);
        require(customerId, "Customer ID");
        require(name, "Name");
        require(address, "Address");
        require(contactNumber, "Contact number");
        require(nationalId, "National ID");
        require(licenseNumber, "Driving license number");
        if (licenseExpiry == null) {
            throw new ValidationException("Driving license expiry date is required (yyyy-MM-dd).");
        }
        if (!customerId.matches("[A-Za-z0-9-]{2,20}")) {
            throw new ValidationException("Customer ID may contain only letters, digits and hyphens (2-20 characters).");
        }
        if (!name.matches("[\\p{L} .'-]{2,60}")) {
            throw new ValidationException("Name may contain only letters, spaces and . ' - (2-60 characters).");
        }
        validateContactNumber(contactNumber);
        if (!nationalId.matches("[A-Za-z0-9-]{5,20}")) {
            throw new ValidationException("National ID must be 5-20 letters, digits or hyphens.");
        }

        // 3.1 verify the driving license with the LVA (include UC-13)
        DrivingLicense license = new DrivingLicense(licenseNumber, "", null, licenseExpiry);
        LicenseStatus result = licenseVerificationService.verify(license);
        if (result != LicenseStatus.VALID) {
            throw new BusinessRuleException("License Not Valid: the driving license is " + result.toString().toLowerCase()
                    + ". The customer was not registered.");
        }

        // 3.2 check that the National ID / driving license / Customer ID are not already registered
        if (customerRepository.findById(customerId).isPresent()
                || !customerRepository.findByNationalId(nationalId).isEmpty()
                || !customerRepository.findByDrivingLicenseNumber(licenseNumber).isEmpty()) {
            throw new BusinessRuleException("Customer profile with this ID already exists");
        }

        // 3.3 save
        Customer customer = new Customer(customerId, name, new Address(address, "", "", "", ""),
                contactNumber, nationalId, license);
        customerRepository.save(customer);
        return customer;
    }

    /** UC-02: search by one criterion (Customer ID / National ID exact, Name partial). */
    public List<Customer> search(CustomerSearchBy by, String value) {
        List<Customer> results = new ArrayList<>();
        value = clean(value);
        if (value.isEmpty()) {
            throw new ValidationException("Enter a search value.");
        }
        switch (by) {
            case CUSTOMER_ID:
                customerRepository.findById(value).ifPresent(results::add);
                if (results.isEmpty()) {
                    for (Customer c : customerRepository.findAll()) {
                        if (c.getCustomerId().equalsIgnoreCase(value)) {
                            results.add(c);
                        }
                    }
                }
                break;
            case NATIONAL_ID:
                results.addAll(customerRepository.findByNationalId(value));
                break;
            default:
                results.addAll(customerRepository.findByNameContains(value));
                break;
        }
        return results;
    }

    /** UC-02 (used by UC-06 and UC-12.2): match on Customer ID, National ID or Name. */
    public List<Customer> searchCustomers(String keyword) {
        List<Customer> results = new ArrayList<>();
        keyword = clean(keyword);
        if (keyword.isEmpty()) {
            return results;
        }
        for (CustomerSearchBy by : CustomerSearchBy.values()) {
            for (Customer c : search(by, keyword)) {
                if (!results.contains(c)) {
                    results.add(c);
                }
            }
        }
        return results;
    }

    /** UC-03 steps 2.1 - 3.2: validate and save the new contact number / address. */
    public void updateContactInfo(String customerId, String contactNumber, String address) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new BusinessRuleException("No customer with ID " + customerId));
        contactNumber = clean(contactNumber);
        address = clean(address);
        require(contactNumber, "Contact number");
        require(address, "Address");
        validateContactNumber(contactNumber);
        customer.updateContactInfo(contactNumber, new Address(address, "", "", "", ""));
        customerRepository.save(customer);
    }

    public List<Customer> findAll() {
        return customerRepository.findAll();
    }

    public Customer findById(String customerId) {
        return customerRepository.findById(customerId).orElse(null);
    }

    private static void validateContactNumber(String contact) {
        if (!contact.matches("\\+?[0-9][0-9 -]{6,18}[0-9]")) {
            throw new ValidationException("Contact number must contain 8-20 digits (spaces, hyphens and a leading + allowed).");
        }
    }

    private static void require(String value, String field) {
        if (value.isEmpty()) {
            throw new ValidationException(field + " is required.");
        }
    }

    private static String clean(String s) {
        return s == null ? "" : s.trim();
    }
}
