package com.microservices.shared.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a JWT token is invalid.
 */
public class InvalidTokenException extends BaseException {

    private static final String ERROR_CODE = "INVALID_TOKEN";

    public InvalidTokenException(String message) {
        super(message, HttpStatus.UNAUTHORIZED, ERROR_CODE);
    }

    public InvalidTokenException(String message, Throwable cause) {
        super(message, HttpStatus.UNAUTHORIZED, ERROR_CODE, cause);
    }
}
