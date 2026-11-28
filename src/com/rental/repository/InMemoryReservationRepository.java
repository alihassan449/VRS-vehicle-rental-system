package com.rental.repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.rental.model.Reservation;

public class InMemoryReservationRepository implements ReservationRepository {

    private final Map<String, Reservation> reservations = new LinkedHashMap<>();

    @Override
    public void save(Reservation reservation) {
        reservations.put(reservation.getReservationID(), reservation);
    }

    @Override
    public Optional<Reservation> findById(String reservationID) {
        return Optional.ofNullable(reservations.get(reservationID));
    }

    @Override
    public List<Reservation> findAll() {
        return new ArrayList<>(reservations.values());
    }
}
