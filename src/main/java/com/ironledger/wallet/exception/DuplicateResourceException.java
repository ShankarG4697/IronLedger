package com.ironledger.wallet.exception;

/**
 * Exception thrown when attempting to create a resource that already exists.
 *
 * This exception is typically used in scenarios where unique constraints would be violated,
 * such as attempting to register a user with an email that already exists in the system.
 *
 * It extends {@code RuntimeException}, allowing runtime propagation of the exception
 * without requiring explicit handling in the method signature.
 *
 * The global exception handler will catch this and return an HTTP 409 Conflict response.
 */
public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String message) {
        super(message);
    }
}

