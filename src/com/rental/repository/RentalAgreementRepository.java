package com.rental.repository;

import java.util.List;
import java.util.Optional;

import com.rental.model.RentalAgreement;
import com.rental.model.Vehicle;

/**
 * FR17-FR23: storage and lookup of RentalAgreement records.
 */
public interface RentalAgreementRepository {

    void save(RentalAgreement agreement);

    Optional<RentalAgreement> findById(String agreementId);

    // FR30: vehicle-wise rental history report
    List<RentalAgreement> findByVehicle(Vehicle vehicle);

    // FR29: customer rental-history report
    List<RentalAgreement> findByCustomerId(String customerId);

    List<RentalAgreement> findAll();
}
