package com.chat_app.chat.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Global Exception Handler for the Chat Application
 * 
 * This class provides centralized exception handling across all controllers.
 * It ensures consistent error responses and proper HTTP status codes for
 * different types of exceptions that may occur during request processing.
 * 
 * Exception Handling Strategy:
 * - Validation errors: Return field-specific error messages (400 Bad Request)
 * - Runtime exceptions: Return business logic error messages (400 Bad Request)
 * - Generic exceptions: Return generic error message (500 Internal Server Error)
 * 
 * Benefits:
 * - Consistent error response format across all endpoints
 * - Proper HTTP status codes for different error types
 * - Centralized error handling logic
 * - Better client-side error handling and user experience
 * 
 * Usage:
 * - Automatically applied to all @RestController classes
 * - No need to add try-catch blocks in individual controller methods
 * - Exceptions thrown from service layer are automatically handled
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles validation exceptions from @Valid annotations
     * 
     * This method processes validation errors that occur when:
     * - Request body validation fails (@Valid annotation)
     * - Required fields are missing or invalid
     * - Field constraints are violated (e.g., @NotBlank, @Size, @Email)
     * 
     * Response Format:
     * {
     *   "fieldName1": "validation error message",
     *   "fieldName2": "another validation error message"
     * }
     * 
     * Example:
     * - If username is empty: {"username": "Username is required"}
     * - If email is invalid: {"email": "Invalid email format"}
     * 
     * @param ex MethodArgumentNotValidException containing validation errors
     * @return ResponseEntity with field-specific error messages and 400 status
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        
        // Extract all validation errors from the exception
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();  // Get the field name that failed validation
            String errorMessage = error.getDefaultMessage();     // Get the validation error message
            errors.put(fieldName, errorMessage);                // Add to error map
        });
        
        // Return 400 Bad Request with field-specific error messages
        return ResponseEntity.badRequest().body(errors);
    }

    /**
     * Handles runtime exceptions from business logic
     * 
     * This method processes exceptions thrown by service layer methods,
     * such as:
     * - User not found during login
     * - Invalid credentials
     * - Duplicate username during signup
     * - Business rule violations
     * 
     * These exceptions typically contain meaningful error messages
     * that should be returned to the client for user feedback.
     * 
     * @param ex RuntimeException containing business logic error
     * @return ResponseEntity with error message and 400 status
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> handleRuntimeException(RuntimeException ex) {
        // Return 400 Bad Request with the exception message
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    /**
     * Handles generic exceptions and unexpected errors
     * 
     * This method serves as a catch-all for any exceptions that are not
     * handled by more specific exception handlers. It provides a generic
     * error message to avoid exposing internal system details to clients.
     * 
     * Common scenarios:
     * - Database connection errors
     * - Network timeouts
     * - Unexpected system errors
     * - Unhandled exception types
     * 
     * Security Note:
     * - Returns generic message to prevent information leakage
     * - Internal error details should be logged separately
     * 
     * @param ex Generic Exception that wasn't handled by other handlers
     * @return ResponseEntity with generic error message and 500 status
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleGenericException(Exception ex) {
        // Return 500 Internal Server Error with generic message
        // Note: In production, you might want to log the actual exception here
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred");
    }
} 