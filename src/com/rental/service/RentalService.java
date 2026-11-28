package com.rental.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.rental.enums.FuelLevel;
import com.rental.enums.LicenseStatus;
import com.rental.enums.ReservationStatus;
import com.rental.enums.VehicleStatus;
import com.rental.exception.BusinessRuleException;
import com.rental.exception.ValidationException;
import com.rental.model.ChargeBreakdown;
import com.rental.model.RentalAgreement;
import com.rental.model.Reservation;
import com.rental.model.ReturnDetails;
import com.rental.model.Vehicle;
import com.rental.repository.RentalAgreementRepository;
import com.rental.repository.ReservationRepository;
import com.rental.repository.VehicleRepository;
import com.rental.util.IdGenerator;

/**
 * UC-08 Process Vehicle Handover and UC-09 Process Vehicle Return.
 */
public class RentalService {
    private final RentalAgreementRepository agreementRepository;
    private final VehicleRepository vehicleRepository;
    private final ReservationRepository reservationRepository;
    private final LicenseVerificationService licenseVerificationService;
    private final ChargeCalculator chargeCalculator;

    public RentalService(RentalAgreementRepository agreementRepository, VehicleRepository vehicleRepository,
                         LicenseVerificationService licenseVerificationService, ChargeCalculator chargeCalculator) {
        this(agreementRepository, vehicleRepository, null, licenseVerificationService, chargeCalculator);
    }

    public RentalService(RentalAgreementRepository agreementRepository, VehicleRepository vehicleRepository,
                         ReservationRepository reservationRepository,
                         LicenseVerificationService licenseVerificationService, ChargeCalculator chargeCalculator) {
        this.agreementRepository = agreementRepository;
        this.vehicleRepository = vehicleRepository;
        this.reservationRepository = reservationRepository;
        this.licenseVerificationService = licenseVerificationService;
        this.chargeCalculator = chargeCalculator;
    }

    // ------------------------------------------------------------------ UC-08

    /**
     * UC-08 steps 1.1 - 2.1: the reservation must be Confirmed; generates a draft
     * rental agreement and verifies the customer's license (include UC-13).
     * Nothing is saved yet.
     */
    public RentalAgreement startHandover(Reservation reservation) {
        if (reservation == null) {
            throw new ValidationException("Select a reservation first.");
        }
        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new BusinessRuleException("Reservation Not Confirmed: the reservation is "
                    + reservation.getStatus() + ", so no rental agreement can be created.");
        }
        Vehicle vehicle = reservation.getVehicle();
        if (vehicle.getStatus() == VehicleStatus.UNDER_MAINTENANCE || vehicle.getStatus() == VehicleStatus.INACTIVE
                || vehicle.hasOpenMaintenance()) {
            throw new BusinessRuleException("Vehicle " + vehicle.getRegistrationNumber()
                    + " is Under Maintenance and is not available for handover.");
        }
        LicenseStatus result = licenseVerificationService.verify(reservation.getCustomer().getDrivingLicense());
        if (result != LicenseStatus.VALID) {
            throw new BusinessRuleException("Expired License: the customer's driving license is "
                    + result.toString().toLowerCase() + ". The handover is rejected and no rental agreement was created.");
        }
        return new RentalAgreement(null, reservation);
    }

    /**
     * UC-08 steps 3.1 - 5.3: validate and record the handover, save the agreement
     * as Active, set the vehicle to Rented and the reservation to Collected.
     */
    public synchronized RentalAgreement authorizeHandover(RentalAgreement draft, LocalDateTime pickupDateTime,
                                                          double startMileage, FuelLevel startFuel,
                                                          boolean noExistingDamage, String existingDamage) {
        if (draft == null || draft.getAgreementId() != null) {
            throw new IllegalStateException("No draft agreement to authorize.");
        }
        Reservation reservation = draft.getReservation();
        Vehicle vehicle = reservation.getVehicle();
        if (pickupDateTime == null) {
            throw new ValidationException("Pickup date and time are required (yyyy-MM-dd HH:mm).");
        }
        if (startMileage < 0) {
            throw new ValidationException("Starting mileage cannot be negative.");
        }
        if (startMileage < vehicle.getCurrentMileage()) {
            throw new ValidationException("Starting mileage cannot be lower than the vehicle's recorded mileage ("
                    + String.format("%.0f", vehicle.getCurrentMileage()) + ").");
        }
        if (startFuel == null) {
            throw new ValidationException("Fuel level is required.");
        }
        String damage = existingDamage == null ? "" : existingDamage.trim();
        if (!noExistingDamage && damage.isEmpty()) {
            throw new ValidationException("Record the existing damage, or tick 'No existing damage'.");
        }
        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new BusinessRuleException("Reservation Not Confirmed: the reservation is "
                    + reservation.getStatus() + ".");
        }
        draft.setAgreementId(IdGenerator.next("AGR"));
        draft.recordHandover(pickupDateTime, startMileage, startFuel, noExistingDamage ? "" : damage);
        reservation.setStatus(ReservationStatus.COLLECTED);
        vehicle.setStatus(VehicleStatus.RENTED);
        vehicle.setCurrentMileage(startMileage);
        if (reservationRepository != null) {
            reservationRepository.save(reservation);
        }
        agreementRepository.save(draft);
        vehicleRepository.save(vehicle);
        reservation.getCustomer().addRentalToHistory(draft);
        return draft;
    }

    // ------------------------------------------------------------------ UC-09

    /** UC-09 step 3.1: validate the entered return data. */
    public void validateReturn(RentalAgreement agreement, ReturnDetails d) {
        if (agreement == null) {
            throw new ValidationException("Select a rental agreement first.");
        }
        if (!agreement.isActive()) {
            throw new BusinessRuleException("Agreement Not Found or Not Active: the return cannot be processed.");
        }
        if (d.getReturnDateTime() == null) {
            throw new ValidationException("Return date and time are required (yyyy-MM-dd HH:mm).");
        }
        if (d.getReturnDateTime().isBefore(agreement.getPickupDateTime())) {
            throw new ValidationException("Invalid return data: the return date/time is before the pickup date/time.");
        }
        if (d.getEndMileage() < agreement.getStartMileage()) {
            throw new ValidationException("Invalid return data: the ending mileage is lower than the starting mileage ("
                    + String.format("%.0f", agreement.getStartMileage()) + ").");
        }
        if (d.getEndFuelLevel() == null) {
            throw new ValidationException("Return fuel level is required.");
        }
    }

    /** UC-09 steps 3.1 - 3.3: validate and calculate (include UC-16) without saving. */
    public ChargeBreakdown previewReturn(RentalAgreement agreement, ReturnDetails details) {
        validateReturn(agreement, details);
        return chargeCalculator.calculate(agreement, details);
    }

    /**
     * UC-09 steps 4.1 - 4.3 (and alt 1.1 - 1.3): save the return record and the charges,
     * and update the vehicle status - Available, or Under Maintenance when new damage
     * was recorded (or the vehicle has an open maintenance record).
     */
    public synchronized ChargeBreakdown finalizeReturn(RentalAgreement agreement, ReturnDetails details) {
        ChargeBreakdown breakdown = previewReturn(agreement, details);
        agreement.recordReturn(details, breakdown);
        Vehicle vehicle = agreement.getReservation().getVehicle();
        vehicle.setCurrentMileage(details.getEndMileage());
        vehicle.setStatus(details.hasNewDamage() || vehicle.hasOpenMaintenance()
                ? VehicleStatus.UNDER_MAINTENANCE : VehicleStatus.AVAILABLE);
        agreementRepository.save(agreement);
        vehicleRepository.save(vehicle);
        return breakdown;
    }

    // ---------------------------------------------------------------- lookups

    /** Search by agreement ID, customer ID, customer name or vehicle registration number. */
    public List<RentalAgreement> search(String keyword, boolean activeOnly) {
        String k = keyword == null ? "" : keyword.trim().toLowerCase();
        List<RentalAgreement> result = new ArrayList<>();
        for (RentalAgreement a : agreementRepository.findAll()) {
            if (activeOnly && !a.isActive()) {
                continue;
            }
            Reservation r = a.getReservation();
            if (k.isEmpty()
                    || a.getAgreementId().equalsIgnoreCase(k)
                    || r.getCustomer().getCustomerId().equalsIgnoreCase(k)
                    || r.getCustomer().getName().toLowerCase().contains(k)
                    || r.getVehicle().getRegistrationNumber().equalsIgnoreCase(k)) {
                result.add(a);
            }
        }
        return result;
    }

    public List<RentalAgreement> findByCustomer(String customerId) {
        return agreementRepository.findByCustomerId(customerId);
    }

    public List<RentalAgreement> findAll() {
        return agreementRepository.findAll();
    }
}
