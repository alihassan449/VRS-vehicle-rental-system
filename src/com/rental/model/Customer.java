package com.rental.model;

import java.util.ArrayList;
import java.util.List;

/**
 * FR1-FR5: Customer entity.
 */
public class Customer {

    private String customerId;
    private String name;
    private Address address;
    private String contactNumber;
    private String nationalId;
    private DrivingLicense drivingLicense;

    // FR3: previous rental agreements for this customer
    private List<RentalAgreement> rentalHistory = new ArrayList<>();

    public Customer() {
    }

    public Customer(String customerId, String name, Address address, String contactNumber,
                     String nationalId, DrivingLicense drivingLicense) {
        this.customerId = customerId;
        this.name = name;
        this.address = address;
        this.contactNumber = contactNumber;
        this.nationalId = nationalId;
        this.drivingLicense = drivingLicense;
    }

    /**
     * FR4: allow authorized staff to update contact info.
     * Authorization itself is enforced by the caller (service/UI layer);
     * this method simply performs the update.
     */
    public void updateContactInfo(String newContactNumber, Address newAddress) {
        if (newContactNumber != null && !newContactNumber.trim().isEmpty()) {
            this.contactNumber = newContactNumber;
        }
        if (newAddress != null) {
            this.address = newAddress;
        }
    }

    /** True when a driving license is on file and has not expired. */
    public boolean isLicenseValid() {
        return drivingLicense != null && !drivingLicense.isExpired(java.time.LocalDate.now());
    }

    public void addRentalToHistory(RentalAgreement agreement) {
        if (agreement != null && !rentalHistory.contains(agreement)) {
            rentalHistory.add(agreement);
        }
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public String getNationalId() {
        return nationalId;
    }

    public void setNationalId(String nationalId) {
        this.nationalId = nationalId;
    }

    public DrivingLicense getDrivingLicense() {
        return drivingLicense;
    }

    public void setDrivingLicense(DrivingLicense drivingLicense) {
        this.drivingLicense = drivingLicense;
    }

    public List<RentalAgreement> getRentalHistory() {
        return rentalHistory;
    }

    @Override
    public String toString() {
        return customerId + " - " + name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Customer)) return false;
        Customer other = (Customer) o;
        return customerId != null && customerId.equals(other.customerId);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hashCode(customerId);
    }
}
