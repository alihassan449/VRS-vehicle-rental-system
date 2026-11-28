package com.rental.service;

import java.time.LocalDate;

import com.rental.enums.LicenseStatus;
import com.rental.exception.LvaUnavailableException;
import com.rental.model.DrivingLicense;

/**
 * Stand-in for the external LVA (a real deployment would call the
 * Licensing Authority's web service).
 * <ul>
 *   <li>License numbers must be 5-20 letters/digits/hyphens, otherwise INVALID.</li>
 *   <li>License numbers starting with "INV" are reported INVALID by the LVA.</li>
 *   <li>A license past its expiry date is EXPIRED.</li>
 *   <li>{@link #setAvailable(boolean)} simulates an LVA outage.</li>
 * </ul>
 */
public class SimulatedLvaService implements LicenseVerificationService {
    private volatile boolean available = true;

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public boolean isAvailable() {
        return available;
    }

    @Override
    public LicenseStatus verify(DrivingLicense license) {
        if (!available) {
            throw new LvaUnavailableException(
                    "The Licensing Authority (LVA) cannot be reached. Please try again.");
        }
        if (license == null || license.getLicenseNumber() == null
                || !license.getLicenseNumber().matches("[A-Za-z0-9-]{5,20}")
                || license.getLicenseNumber().toUpperCase().startsWith("INV")) {
            return LicenseStatus.INVALID;
        }
        if (license.isExpired(LocalDate.now())) {
            return LicenseStatus.EXPIRED;
        }
        return LicenseStatus.VALID;
    }
}
