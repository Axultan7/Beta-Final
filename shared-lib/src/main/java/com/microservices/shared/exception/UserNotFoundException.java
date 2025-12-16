package com.microservices.shared.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a user is not found.
 */
public class UserNotFoundException extends BaseException {

    private static final String ERROR_CODE = "USER_NOT_FOUND";

    public UserNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND, ERROR_CODE);
    }

    public UserNotFoundException(String userId, boolean isId) {
        super(String.format("User not found with %s: %s", isId ? "id" : "email", userId),
                HttpStatus.NOT_FOUND, ERROR_CODE);
    }
}
