package com.rental.model;

import java.time.LocalDate;

/**
 * Diagram class: Customer (FR1-FR5).
 * UML "Date" is represented by java.time.LocalDate.
 */
public class Customer {

    private final String customerID;
    private final String name;
    private String address;
    private String contactNumber;
    private final String nationalID;
    private final String drivingLicenseNo;
    private final LocalDate licenseExpiryDate;

    public Customer(String customerID, String name, String address, String contactNumber,
                    String nationalID, String drivingLicenseNo, LocalDate licenseExpiryDate) {
        this.customerID = customerID;
        this.name = name;
        this.address = address;
        this.contactNumber = contactNumber;
        this.nationalID = nationalID;
        this.drivingLicenseNo = drivingLicenseNo;
        this.licenseExpiryDate = licenseExpiryDate;
    }

    /** FR4: update contact information (blank values keep the old value). */
    public void updateContactInfo(String address, String contactNumber) {
        if (address != null && !address.trim().isEmpty()) {
            this.address = address.trim();
        }
        if (contactNumber != null && !contactNumber.trim().isEmpty()) {
            this.contactNumber = contactNumber.trim();
        }
    }

    /** FR5: a license is valid only if it has a number and has not expired today. */
    public boolean isLicenseValid() {
        if (drivingLicenseNo == null || drivingLicenseNo.trim().isEmpty() || licenseExpiryDate == null) {
            return false;
        }
        return !LocalDate.now().isAfter(licenseExpiryDate);
    }

    public String getCustomerID() { return customerID; }
    public String getName() { return name; }
    public String getAddress() { return address; }
    public String getContactNumber() { return contactNumber; }
    public String getNationalID() { return nationalID; }
    public String getDrivingLicenseNo() { return drivingLicenseNo; }
    public LocalDate getLicenseExpiryDate() { return licenseExpiryDate; }

    @Override
    public String toString() {
        return customerID + " - " + name;
    }
}
