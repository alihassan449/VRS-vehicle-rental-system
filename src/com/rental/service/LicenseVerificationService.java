package com.rental.service;

import com.rental.enums.LicenseStatus;
import com.rental.exception.LvaUnavailableException;
import com.rental.model.DrivingLicense;

/**
 * UC-13 Verify License: boundary to the external Licensing Authority (LVA).
 */
public interface LicenseVerificationService {
    /**
     * Sends the license details to the LVA and returns the verification result.
     *
     * @throws LvaUnavailableException if the LVA cannot be reached
     */
    LicenseStatus verify(DrivingLicense license) throws LvaUnavailableException;
}
