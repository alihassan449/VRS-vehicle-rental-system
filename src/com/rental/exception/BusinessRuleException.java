package com.rental.exception;

/** A business rule from the use case specifications prevents the operation (an "Exception" flow). */
public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) {
        super(message);
    }
}
