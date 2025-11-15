package com.ironledger.wallet.exception;

import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Represents a structured error response intended to be returned by a REST API.
 * The class encapsulates details about an error such as the HTTP status code,
 * an error description, a detailed error message, the associated path, and
 * the timestamp when the error occurred.
 *
 * <ul>
 * - The {@code timestamp} field indicates when the error response was created.
 * - The {@code status} field contains the HTTP status code for the error.
 * - The {@code error} field represents a short description of the error type.
 * - The {@code message} field provides additional details about the error.
 * - The {@code path} field specifies the HTTP request path that triggered the error.
 * </ul>
 *
 * This class is typically used in conjunction with exception handlers to format
 * error responses consistently in a RESTful API.
 */
@Getter
public class ApiErrorResponse {
    private final LocalDateTime timestamp;
    private int status;
    private String error;
    private String message;
    private String path;

    public ApiErrorResponse() {
        this.timestamp = LocalDateTime.now();
    }

    public ApiErrorResponse(int status, String error, String message, String path) {
        this.timestamp = LocalDateTime.now();
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
    }

}
