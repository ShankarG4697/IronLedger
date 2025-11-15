package com.ironledger.wallet.exception;

/**
 * Exception thrown when authentication fails due to invalid credentials.
 *
 * This exception is used during login attempts when the provided email or password
 * does not match any valid user in the system or when the credentials are incorrect.
 *
 * It extends {@code RuntimeException}, allowing runtime propagation of the exception
 * without requiring explicit handling in the method signature.
 *
 * The global exception handler will catch this and return an HTTP 401 Unauthorized response.
 */
public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException(String message) {
        super(message);
    }
}

