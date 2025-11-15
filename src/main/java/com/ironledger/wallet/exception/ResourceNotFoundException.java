package com.ironledger.wallet.exception;

/**
 * Exception class representing a situation where a requested resource cannot be found.
 *
 * This exception is typically used in scenarios where a client attempts to access a
 * resource (such as a database entity) that does not exist. It extends {@code RuntimeException},
 * allowing runtime propagation of the exception without requiring explicit handling in the method signature.
 *
 * Example usage includes throwing this exception when a service or controller cannot find
 * the requested entity, commonly used in conjunction with a global exception handler to
 * return a user-friendly HTTP 404 Not Found response.
 *
 * Constructor details:
 * - Accepts a message parameter that provides additional information about the specific
 *   resource or condition causing the exception.
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}