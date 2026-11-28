package com.rental.exception;

/** UC-06 "Scheduling Conflict": another user booked the vehicle while this reservation was processed. */
public class SchedulingConflictException extends BusinessRuleException {
    public SchedulingConflictException(String message) {
        super(message);
    }
}
