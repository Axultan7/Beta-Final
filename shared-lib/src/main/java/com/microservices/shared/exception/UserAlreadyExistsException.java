package com.microservices.shared.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when attempting to create a user that already exists.
 */
public class UserAlreadyExistsException extends BaseException {

    private static final String ERROR_CODE = "USER_ALREADY_EXISTS";

    public UserAlreadyExistsException(String message) {
        super(message, HttpStatus.CONFLICT, ERROR_CODE);
    }

    public UserAlreadyExistsException(String email, boolean dummy) {
        super(String.format("User with email '%s' already exists", email),
                HttpStatus.CONFLICT, ERROR_CODE);
    }
}
