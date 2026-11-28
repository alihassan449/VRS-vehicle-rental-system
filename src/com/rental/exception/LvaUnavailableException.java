package com.rental.exception;

/** UC-13 "LVA Unavailable": the external Licensing Authority cannot be reached. */
public class LvaUnavailableException extends BusinessRuleException {
    public LvaUnavailableException(String message) {
        super(message);
    }
}
