package com.rental.repository;

import java.util.List;
import java.util.Optional;

import com.rental.model.Reservation;

/** Storage of Reservation objects (FR11-FR16). */
public interface ReservationRepository {
    void save(Reservation reservation);
    Optional<Reservation> findById(String reservationID);
    List<Reservation> findAll();
}
