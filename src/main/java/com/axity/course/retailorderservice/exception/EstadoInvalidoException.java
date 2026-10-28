package com.axity.course.retailorderservice.exception;

/**
 * Operación rechazada por el estado actual del recurso (transición de pedido no permitida,
 * cancelación/reactivación duplicada, etc.). Se usa tanto para Order como para Product.
 */
public class EstadoInvalidoException extends RuntimeException {

    public EstadoInvalidoException(String message) {
        super(message);
    }
}
