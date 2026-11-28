package com.rental.model;

import java.time.LocalDate;

/**
 * Value object representing a customer's driving license.
 * FR1: stored as part of customer registration.
 * FR5: used to block rental when expired.
 */
public class DrivingLicense {

    private String licenseNumber;
    private String issuingAuthority;
    private LocalDate issueDate;
    private LocalDate expiryDate;

    public DrivingLicense() {
    }

    public DrivingLicense(String licenseNumber, String issuingAuthority, LocalDate issueDate, LocalDate expiryDate) {
        this.licenseNumber = licenseNumber;
        this.issuingAuthority = issuingAuthority;
        this.issueDate = issueDate;
        this.expiryDate = expiryDate;
    }

    /**
     * FR5: A customer with an expired license must not receive a vehicle.
     * Compares expiryDate against the given reference date (normally "today").
     */
    public boolean isExpired(LocalDate onDate) {
        if (expiryDate == null || onDate == null) {
            return true; // no expiry date on file is treated as not eligible
        }
        return onDate.isAfter(expiryDate);
    }

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public void setLicenseNumber(String licenseNumber) {
        this.licenseNumber = licenseNumber;
    }

    public String getIssuingAuthority() {
        return issuingAuthority;
    }

    public void setIssuingAuthority(String issuingAuthority) {
        this.issuingAuthority = issuingAuthority;
    }

    public LocalDate getIssueDate() {
        return issueDate;
    }

    public void setIssueDate(LocalDate issueDate) {
        this.issueDate = issueDate;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    @Override
    public String toString() {
        return licenseNumber + " (expires " + expiryDate + ")";
    }
}
