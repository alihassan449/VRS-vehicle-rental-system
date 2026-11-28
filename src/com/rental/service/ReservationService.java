package com.rental.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.rental.enums.ReservationStatus;
import com.rental.enums.VehicleStatus;
import com.rental.exception.BusinessRuleException;
import com.rental.exception.SchedulingConflictException;
import com.rental.exception.ValidationException;
import com.rental.model.Customer;
import com.rental.model.RentalAgreement;
import com.rental.model.Reservation;
import com.rental.model.Vehicle;
import com.rental.repository.RentalAgreementRepository;
import com.rental.repository.ReservationRepository;
import com.rental.repository.VehicleRepository;
import com.rental.util.IdGenerator;

/**
 * UC-06 Create Reservation (with UC-14 Check Vehicle Availability and
 * UC-15 Check Vehicle Overbooking) and UC-07 Cancel Reservation.
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

    /** UC-06 step 3.1: validate the rental period. */
    public void validatePeriod(LocalDate pickup, LocalDate ret) {
        if (pickup == null || ret == null) {
            throw new ValidationException("Pickup date and return date are required (yyyy-MM-dd).");
        }
        if (ret.isBefore(pickup)) {
            throw new ValidationException("Invalid rental period: the return date is earlier than the pickup date.");
        }
        if (pickup.isBefore(LocalDate.now())) {
            throw new ValidationException("The pickup date cannot be in the past.");
        }
    }

    /**
     * UC-14 Check Vehicle Availability: vehicles that are not Under Maintenance
     * and have no overlapping reservation or rental for the period.
     */
    public List<Vehicle> findAvailableVehicles(LocalDate pickup, LocalDate ret) {
        validatePeriod(pickup, ret);
        List<Vehicle> available = new ArrayList<>();
        for (Vehicle v : vehicleRepository.findAll()) {
            if (isBookable(v) && isFreeOfOverlap(v, pickup, ret, null)) {
                available.add(v);
            }
        }
        return available;
    }

    /**
     * UC-15 Check Vehicle Overbooking: true if the vehicle has no reservation
     * or active rental overlapping the requested period.
     */
    public boolean isFreeOfOverlap(Vehicle vehicle, LocalDate pickup, LocalDate ret, Reservation ignore) {
        for (Reservation existing : reservationRepository.findByVehicle(vehicle)) {
            if (existing.equals(ignore) || !blocksVehicle(existing)) {
                continue;
            }
            LocalDate end = existing.getReturnDate();
            if (existing.getStatus() == ReservationStatus.COLLECTED && end.isBefore(LocalDate.now())) {
                end = LocalDate.now(); // overdue rental: the vehicle is still out
            }
            if (Reservation.periodsOverlap(pickup, ret, existing.getPickupDate(), end)) {
                return false;
            }
        }
        return true;
    }

    /** UC-11 (alt 2): vehicles that are Under Maintenance cannot be reserved. */
    private boolean isBookable(Vehicle v) {
        return v.getStatus() != VehicleStatus.UNDER_MAINTENANCE
                && v.getStatus() != VehicleStatus.INACTIVE
                && !v.hasOpenMaintenance();
    }

    private boolean blocksVehicle(Reservation r) {
        if (r.getStatus() == ReservationStatus.CONFIRMED) {
            return true;
        }
        if (r.getStatus() == ReservationStatus.COLLECTED) {
            for (RentalAgreement a : agreementRepository.findAll()) {
                if (r.equals(a.getReservation()) && a.isActive()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * UC-06 steps 5.1 / 6.1: select a vehicle - rejects vehicles Under Maintenance
     * and overlapping selections, and calculates the estimated rental charge
     * (rental days x daily rate). The draft is NOT saved until confirmed.
     */
    public Reservation buildDraft(Customer customer, Vehicle vehicle, LocalDate pickup, LocalDate ret) {
        if (customer == null) {
            throw new ValidationException("Select a customer first.");
        }
        if (vehicle == null) {
            throw new ValidationException("Select a vehicle first.");
        }
        validatePeriod(pickup, ret);
        if (!isBookable(vehicle)) {
            throw new BusinessRuleException("Vehicle " + vehicle.getRegistrationNumber()
                    + " is Under Maintenance and cannot be reserved. Please select another vehicle.");
        }
        if (!isFreeOfOverlap(vehicle, pickup, ret, null)) {
            throw new BusinessRuleException("Overlapping Reservation: vehicle " + vehicle.getRegistrationNumber()
                    + " already has a reservation or rental overlapping this period. "
                    + "Select another vehicle or change the dates.");
        }
        return new Reservation(null, customer, vehicle, pickup, ret);
    }

    /**
     * UC-06 steps 7.1 - 7.3: re-check for overlaps, save the reservation as
     * Confirmed and set the vehicle to Reserved.
     */
    public synchronized Reservation confirmReservation(Reservation draft) {
        if (draft == null || draft.getReservationId() != null) {
            throw new IllegalStateException("Nothing to confirm.");
        }
        Vehicle vehicle = draft.getVehicle();
        if (!isBookable(vehicle)) {
            throw new BusinessRuleException("Vehicle " + vehicle.getRegistrationNumber()
                    + " is now Under Maintenance and cannot be reserved.");
        }
        if (!isFreeOfOverlap(vehicle, draft.getPickupDate(), draft.getReturnDate(), draft)) {
            throw new SchedulingConflictException("Scheduling Conflict: another user has booked vehicle "
                    + vehicle.getRegistrationNumber() + " for an overlapping period. The reservation was not saved.");
        }
        draft.setReservationId(IdGenerator.next("RES"));
        draft.setStatus(ReservationStatus.CONFIRMED);
        if (vehicle.getStatus() == VehicleStatus.AVAILABLE) {
            vehicle.setStatus(VehicleStatus.RESERVED);
        }
        reservationRepository.save(draft);
        vehicleRepository.save(vehicle);
        return draft;
    }

    /** UC-06: save a new reservation in PENDING state for Rental Staff review. */
    public synchronized Reservation createPendingReservation(Reservation draft) {
        if (draft == null || draft.getReservationId() != null) {
            throw new IllegalStateException("Nothing to save.");
        }
        Vehicle vehicle = draft.getVehicle();
        if (!isBookable(vehicle)) {
            throw new BusinessRuleException("Vehicle " + vehicle.getRegistrationNumber() + " is unavailable for reservation.");
        }
        if (!isFreeOfOverlap(vehicle, draft.getPickupDate(), draft.getReturnDate(), draft)) {
            throw new SchedulingConflictException("Scheduling Conflict: vehicle " + vehicle.getRegistrationNumber()
                    + " has an overlapping reservation.");
        }
        draft.setReservationId(IdGenerator.next("RES"));
        draft.setStatus(ReservationStatus.PENDING);
        reservationRepository.save(draft);
        return draft;
    }

    /** Sequence-controlled status transition with persisted repository updates. */
    public synchronized void setReservationStatus(Reservation reservation, ReservationStatus next) {
        if (reservation == null || next == null) throw new ValidationException("Select a reservation and status.");
        ReservationStatus current = reservation.getStatus();
        boolean allowed =
                (current == ReservationStatus.PENDING &&
                    (next == ReservationStatus.CONFIRMED || next == ReservationStatus.CANCELLED || next == ReservationStatus.EXPIRED)) ||
                (current == ReservationStatus.CONFIRMED &&
                    (next == ReservationStatus.CANCELLED || next == ReservationStatus.EXPIRED));
        if (!allowed) {
            throw new BusinessRuleException("Status transition from " + current + " to " + next + " is not allowed.");
        }
        reservation.setStatus(next);
        reservationRepository.save(reservation);
        if (next == ReservationStatus.CONFIRMED) {
            if (reservation.getVehicle().getStatus() == VehicleStatus.AVAILABLE) {
                reservation.getVehicle().setStatus(VehicleStatus.RESERVED);
                vehicleRepository.save(reservation.getVehicle());
            }
        } else if (next == ReservationStatus.CANCELLED || next == ReservationStatus.EXPIRED) {
            releaseVehicleIfFree(reservation.getVehicle());
        }
    }

    /**
     * UC-07: cancel a reservation that has not been collected; the vehicle is
     * released (Available) unless it has another confirmed reservation.
     */
    public synchronized void cancelReservation(Reservation reservation) {
        if (reservation == null) {
            throw new ValidationException("Select a reservation first.");
        }
        ReservationStatus s = reservation.getStatus();
        if (s == ReservationStatus.COLLECTED) {
            throw new BusinessRuleException("Cancellation Not Allowed: the vehicle has already been collected.");
        }
        if (s == ReservationStatus.CANCELLED || s == ReservationStatus.EXPIRED) {
            throw new BusinessRuleException("Cancellation Not Allowed: the reservation is already " + s + ".");
        }
        reservation.setStatus(ReservationStatus.CANCELLED);
        reservationRepository.save(reservation);
        releaseVehicleIfFree(reservation.getVehicle());
    }

    /** Confirmed reservations whose return date has passed without collection become Expired. */
    public void expireOverdueReservations() {
        for (Reservation r : reservationRepository.findAll()) {
            if (r.getStatus() == ReservationStatus.CONFIRMED && r.getReturnDate().isBefore(LocalDate.now())) {
                r.setStatus(ReservationStatus.EXPIRED);
                reservationRepository.save(r);
                releaseVehicleIfFree(r.getVehicle());
            }
        }
    }

    private void releaseVehicleIfFree(Vehicle vehicle) {
        if (vehicle.getStatus() != VehicleStatus.RESERVED) {
            return;
        }
        for (Reservation other : reservationRepository.findByVehicle(vehicle)) {
            if (other.getStatus() == ReservationStatus.CONFIRMED) {
                return;
            }
        }
        vehicle.setStatus(VehicleStatus.AVAILABLE);
        vehicleRepository.save(vehicle);
    }

    /** Search by reservation ID, customer ID, customer name or vehicle registration number. */
    public List<Reservation> search(String keyword) {
        String k = keyword == null ? "" : keyword.trim().toLowerCase();
        List<Reservation> result = new ArrayList<>();
        for (Reservation r : reservationRepository.findAll()) {
            if (k.isEmpty()
                    || r.getReservationId().equalsIgnoreCase(k)
                    || r.getCustomer().getCustomerId().equalsIgnoreCase(k)
                    || r.getCustomer().getName().toLowerCase().contains(k)
                    || r.getVehicle().getRegistrationNumber().equalsIgnoreCase(k)) {
                result.add(r);
            }
        }
        return result;
    }

    public List<Reservation> findAll() {
        return reservationRepository.findAll();
    }
}
