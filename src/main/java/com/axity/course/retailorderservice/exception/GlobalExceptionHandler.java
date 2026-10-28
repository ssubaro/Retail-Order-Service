package com.axity.course.retailorderservice.exception;

import java.time.Instant;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.axity.course.retailorderservice.dto.ApiErrorDto;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ApiErrorDto> handleNotFound(RecursoNoEncontradoException exception) {
        return createResponse(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(StockInsuficienteException.class)
    public ResponseEntity<ApiErrorDto> handleStockInsuficiente(StockInsuficienteException exception) {
        return createResponse(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(EstadoInvalidoException.class)
    public ResponseEntity<ApiErrorDto> handleEstadoInvalido(EstadoInvalidoException exception) {
        return createResponse(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(PrecioInvalidoException.class)
    public ResponseEntity<ApiErrorDto> handlePrecioInvalido(PrecioInvalidoException exception) {
        return createResponse(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorDto> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return createResponse(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorDto> handleUnreadable(HttpMessageNotReadableException exception) {
        return createResponse(HttpStatus.BAD_REQUEST, "El cuerpo de la petición es inválido o no se pudo interpretar");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorDto> handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
        return createResponse(HttpStatus.BAD_REQUEST,
                "El parámetro '" + exception.getName() + "' tiene un formato inválido");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorDto> handleDataIntegrity(DataIntegrityViolationException exception) {
        return createResponse(HttpStatus.CONFLICT, "La operación viola una restricción de integridad de datos (por ejemplo, un SKU duplicado)");
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ApiErrorDto> handleOptimisticLock(ObjectOptimisticLockingFailureException exception) {
        return createResponse(HttpStatus.CONFLICT,
                "El recurso fue modificado concurrentemente por otra operación; reintente la solicitud");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorDto> handleUnexpected(Exception exception) {
        return createResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error inesperado al procesar la solicitud");
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
