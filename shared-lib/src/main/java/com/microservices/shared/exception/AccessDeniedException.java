package com.microservices.shared.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when access to a resource is denied.
 */
public class AccessDeniedException extends BaseException {

    private static final String ERROR_CODE = "ACCESS_DENIED";

    public AccessDeniedException(String message) {
        super(message, HttpStatus.FORBIDDEN, ERROR_CODE);
    }

    public AccessDeniedException() {
        super("Access denied", HttpStatus.FORBIDDEN, ERROR_CODE);
    }
}
