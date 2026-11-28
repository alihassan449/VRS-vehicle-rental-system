package com.rental.repository;

import java.util.List;
import java.util.Optional;

import com.rental.model.Reservation;
import com.rental.model.Vehicle;

/**
 * FR11-FR16: storage and lookup of Reservation records.
 */
public interface ReservationRepository {

    void save(Reservation reservation);

    Optional<Reservation> findById(String reservationId);

    // FR13: needed to check overlap for a given vehicle
    List<Reservation> findByVehicle(Vehicle vehicle);

    List<Reservation> findByCustomerId(String customerId);

    List<Reservation> findAll();
}
