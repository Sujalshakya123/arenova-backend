package com.arenova.exceptions;

/**
 * Thrown when a non-ACTIVE account tries to sign in or use the API.
 */
public class AccountBlockedException extends RuntimeException {
    public AccountBlockedException(String message) {
        super(message);
    }
}
