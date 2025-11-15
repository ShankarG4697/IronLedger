package com.ironledger.wallet.exception;

/**
 * Exception thrown when a user attempts to authenticate with a locked or disabled account.
 *
 * This exception is used when a user's account status is not ACTIVE (e.g., DISABLED or LOCKED),
 * preventing them from accessing the system even with valid credentials.
 *
 * It extends {@code RuntimeException}, allowing runtime propagation of the exception
 * without requiring explicit handling in the method signature.
 *
 * The global exception handler will catch this and return an HTTP 423 Locked response.
 */
public class AccountLockedException extends RuntimeException {
    public AccountLockedException(String message) {
        super(message);
    }
}

