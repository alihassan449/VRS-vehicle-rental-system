package com.rental.repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.rental.model.RentalAgreement;

public class InMemoryRentalAgreementRepository implements RentalAgreementRepository {

    private final Map<String, RentalAgreement> agreements = new LinkedHashMap<>();

    @Override
    public void save(RentalAgreement agreement) {
        agreements.put(agreement.getAgreementID(), agreement);
    }

    @Override
    public Optional<RentalAgreement> findById(String agreementID) {
        return Optional.ofNullable(agreements.get(agreementID));
    }

    @Override
    public List<RentalAgreement> findByCustomerId(String customerID) {
        List<RentalAgreement> result = new ArrayList<>();
        for (RentalAgreement a : agreements.values()) {
            if (a.getCustomer().getCustomerID().equals(customerID)) {
                result.add(a);
            }
        }
        return result;
    }

    @Override
    public List<RentalAgreement> findByVehicle(String registrationNumber) {
        List<RentalAgreement> result = new ArrayList<>();
        for (RentalAgreement a : agreements.values()) {
            if (a.getVehicle().getRegistrationNumber().equalsIgnoreCase(registrationNumber)) {
                result.add(a);
            }
        }
        return result;
    }

    @Override
    public List<RentalAgreement> findAll() {
        return new ArrayList<>(agreements.values());
    }
}
