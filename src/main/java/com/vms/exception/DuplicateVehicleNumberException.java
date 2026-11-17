package com.vms.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when an operation violates the uniqueness constraint
 * for a vehicle number in business logic.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicateVehicleNumberException extends RuntimeException {

    public DuplicateVehicleNumberException(String message) {
        super(message);
    }
}
