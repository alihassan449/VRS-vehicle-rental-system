package com.rental.repository;

import java.util.List;
import java.util.Optional;

import com.rental.model.RentalAgreement;

/** Storage of RentalAgreement objects (FR3, FR17-FR23, FR29-FR31). */
public interface RentalAgreementRepository {
    void save(RentalAgreement agreement);
    Optional<RentalAgreement> findById(String agreementID);
    List<RentalAgreement> findByCustomerId(String customerID);
    List<RentalAgreement> findByVehicle(String registrationNumber);
    List<RentalAgreement> findAll();
}
