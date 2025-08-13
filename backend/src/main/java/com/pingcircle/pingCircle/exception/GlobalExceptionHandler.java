package com.pingcircle.pingCircle.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;


@RestControllerAdvice
public class GlobalExceptionHandler {

    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        
        
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();  
            String errorMessage = error.getDefaultMessage();     
            errors.put(fieldName, errorMessage);                
        });
        
       
        return ResponseEntity.badRequest().body(errors);
    }

    
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleBusinessError(RuntimeException exception) {
        Map<String, String> error = new HashMap<>();
        error.put("error", exception.getMessage());
        error.put("type", "business_error");
        
        
        return ResponseEntity.badRequest().body(error);
    }

    
   
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleSystemError(Exception exception) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "An unexpected error occurred");
        error.put("type", "system_error");
        error.put("details", exception.getMessage());
        
      
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
} 