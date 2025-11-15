package com.ironledger.wallet.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Objects;

/**
 * Global exception handler that provides centralized exception handling for a REST API.
 * This class uses Spring's @RestControllerAdvice to intercept and handle various exceptions
 * and generate uniform error responses for clients.
 *
 * The class is responsible for handling the following exceptions:
 *
 * - {@code ResourceNotFoundException}: Thrown when the requested resource cannot be found.
 *   Results in a 404 Not Found response.
 * - {@code MethodArgumentNotValidException}: Thrown when validation for method arguments fails.
 *   Results in a 400 Bad Request response with details about validation errors.
 * - {@code Exception}: A generic exception handler for all other unhandled exceptions.
 *   Results in a 500 Internal Server Error response.
 *
 * Each handler method constructs an {@code ApiErrorResponse} object to include detailed information
 * about the error, including the HTTP status code, a short description of the error, the specific
 * error message, and the request path that triggered the exception.
 *
 * This class ensures consistent and meaningful error responses across the API, improving debugging
 * and client communication.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        ApiErrorResponse error = new ApiErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                "Not Found",
                ex.getMessage(),
                request.getRequestURI()
        );
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiErrorResponse> handleDuplicate(DuplicateResourceException ex, HttpServletRequest request) {
        ApiErrorResponse error = new ApiErrorResponse(
                HttpStatus.CONFLICT.value(),
                "Resource Already Exists",
                ex.getMessage(),
                request.getRequestURI()
        );
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidCredentials(InvalidCredentialsException ex, HttpServletRequest request) {
        ApiErrorResponse error = new ApiErrorResponse(
                HttpStatus.UNAUTHORIZED.value(),
                "Unauthorized",
                ex.getMessage(),
                request.getRequestURI()
        );
        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccountLocked(AccountLockedException ex, HttpServletRequest request) {
        ApiErrorResponse error = new ApiErrorResponse(
                HttpStatus.LOCKED.value(),
                "Account Locked",
                ex.getMessage(),
                request.getRequestURI()
        );
        return new ResponseEntity<>(error, HttpStatus.LOCKED);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String errorMessage = Objects.requireNonNull(ex.getBindingResult().getFieldError()).getDefaultMessage();
        ApiErrorResponse error = new ApiErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Validation Failed",
                errorMessage,
                request.getRequestURI()
        );
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
        ApiErrorResponse error = new ApiErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                ex.getMessage(),
                request.getRequestURI()
        );
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}

