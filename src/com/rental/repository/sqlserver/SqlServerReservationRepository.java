package com.rental.repository.sqlserver;

import com.rental.model.Reservation;
import com.rental.repository.InMemoryReservationRepository;

/** Reservations kept in memory and written to SQL Server on every save. */
public class SqlServerReservationRepository extends InMemoryReservationRepository {

    private final SqlServerDatabase db;

    public SqlServerReservationRepository(SqlServerDatabase db) {
        this.db = db;
    }

    @Override
    public void save(Reservation reservation) {
        db.saveReservation(reservation);
        super.save(reservation);
    }

    public void cache(Reservation reservation) {
        super.save(reservation);
    }
}
