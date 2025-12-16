package com.microservices.shared.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a JWT token has expired.
 */
public class TokenExpiredException extends BaseException {

    private static final String ERROR_CODE = "TOKEN_EXPIRED";

    public TokenExpiredException(String message) {
        super(message, HttpStatus.UNAUTHORIZED, ERROR_CODE);
    }

    public TokenExpiredException() {
        super("Token has expired", HttpStatus.UNAUTHORIZED, ERROR_CODE);
    }
}
