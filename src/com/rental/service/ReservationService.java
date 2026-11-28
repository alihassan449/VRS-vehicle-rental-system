package com.rental.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.rental.enums.VehicleStatus;
import com.rental.model.Customer;
import com.rental.model.RentalAgreement;
import com.rental.model.Reservation;
import com.rental.model.Vehicle;
import com.rental.repository.RentalAgreementRepository;
import com.rental.repository.ReservationRepository;
import com.rental.repository.VehicleRepository;
import com.rental.util.IdGenerator;

/**
 * Diagram class: ReservationService (FR11-FR16).
 * createReservation(...), checkOverlappingReservation(...) and cancelReservation(reservationID)
 * are the diagram methods. Because a Reservation "generates" a RentalAgreement, this service
 * also stores the agreements created from reservations (helper methods at the bottom).
 */
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final VehicleRepository vehicleRepository;
    private final RentalAgreementRepository agreementRepository;

    public ReservationService(ReservationRepository reservationRepository,
                              VehicleRepository vehicleRepository,
                              RentalAgreementRepository agreementRepository) {
        this.reservationRepository = reservationRepository;
        this.vehicleRepository = vehicleRepository;
        this.agreementRepository = agreementRepository;
    }

    /**
     * FR11 + FR14: create a PENDING reservation.
     * FR5  - refused when the customer's driving license is expired.
     * FR27 - refused when the vehicle is under maintenance (or inactive).
     * FR13 - refused when a confirmed reservation already overlaps the period.
     */
    public Reservation createReservation(Customer customer, Vehicle vehicle, LocalDate pickup, LocalDate returnD) {
        if (customer == null || vehicle == null || pickup == null || returnD == null) {
            throw new IllegalArgumentException("Customer, vehicle, pickup date and return date are required");
        }
        if (returnD.isBefore(pickup)) {
            throw new IllegalArgumentException("Return date cannot be before the pickup date");
        }
        if (!customer.isLicenseValid()) {
            throw new IllegalStateException("Customer's driving license is expired - reservation refused");
        }
        VehicleStatus vs = vehicle.getStatus();
        if (vs == VehicleStatus.UNDER_MAINTENANCE || vs == VehicleStatus.INACTIVE) {
            throw new IllegalStateException("Vehicle cannot be reserved (status: " + vs + ")");
        }
        if (checkOverlappingReservation(vehicle, pickup, returnD)) {
            throw new IllegalStateException("Vehicle already has a confirmed reservation overlapping these dates");
        }
        Reservation reservation = new Reservation(IdGenerator.next("RES"), customer, vehicle, pickup, returnD);
        vehicle.addReservation(reservation);
        reservationRepository.save(reservation);
        return reservation;
    }

    /** FR13: true if the vehicle already has a confirmed reservation overlapping the period. */
    public boolean checkOverlappingReservation(Vehicle vehicle, LocalDate pickup, LocalDate returnD) {
        return vehicle.hasOverlappingReservation(pickup, returnD);
    }

    /** FR16: cancel a reservation before the vehicle is collected. */
    public void cancelReservation(String reservationID) {
        Reservation reservation = findById(reservationID);
        reservation.cancelReservation();
        reservationRepository.save(reservation);
    }

    // ------------------------------------------------------------------ helpers

    /** FR12: vehicles available for the requested period. */
    public List<Vehicle> findAvailableVehicles(LocalDate pickup, LocalDate returnD) {
        if (pickup == null || returnD == null || returnD.isBefore(pickup)) {
            throw new IllegalArgumentException("Please enter a valid pickup and return date");
        }
        List<Vehicle> available = new ArrayList<>();
        for (Vehicle v : vehicleRepository.findAll()) {
            if (v.isAvailableForPeriod(pickup, returnD)) {
                available.add(v);
            }
        }
        return available;
    }

    /** FR15: mark a PENDING / CONFIRMED reservation as EXPIRED. */
    public void expireReservation(String reservationID) {
        Reservation reservation = findById(reservationID);
        reservation.markExpired();
        reservationRepository.save(reservation);
    }

    public Reservation findById(String reservationID) {
        if (reservationID == null) {
            throw new IllegalArgumentException("Reservation ID is required");
        }
        return reservationRepository.findById(reservationID.trim())
                .orElseThrow(() -> new IllegalArgumentException("No reservation with ID " + reservationID.trim()));
    }

    public void save(Reservation reservation) {
        reservationRepository.save(reservation);
    }

    public List<Reservation> findAll() {
        return reservationRepository.findAll();
    }

    public void saveAgreement(RentalAgreement agreement) {
        agreementRepository.save(agreement);
    }

    public RentalAgreement findAgreementById(String agreementID) {
        if (agreementID == null) {
            throw new IllegalArgumentException("Agreement ID is required");
        }
        return agreementRepository.findById(agreementID.trim())
                .orElseThrow(() -> new IllegalArgumentException("No rental agreement with ID " + agreementID.trim()));
    }

    public List<RentalAgreement> findAllAgreements() {
        return agreementRepository.findAll();
    }
}
