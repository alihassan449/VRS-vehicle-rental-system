package com.rental.exception;

/** Invalid or missing input (shown to the user as an inline validation error). */
public class ValidationException extends RuntimeException {
    public ValidationException(String message) {
        super(message);
    }
}
