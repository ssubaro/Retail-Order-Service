package com.axity.course.retailorderservice.exception;

import java.time.Instant;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.axity.course.retailorderservice.dto.ApiErrorDto;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<ApiErrorDto> handleProductNotFound(ProductNotFoundException exception) {
        return createResponse(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorDto> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return createResponse(HttpStatus.BAD_REQUEST, message);
    }

    private ResponseEntity<ApiErrorDto> createResponse(HttpStatus status, String message) {
        ApiErrorDto error = new ApiErrorDto(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message);
        return ResponseEntity.status(status).body(error);
    }
}
