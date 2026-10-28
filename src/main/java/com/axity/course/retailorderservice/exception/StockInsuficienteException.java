package com.axity.course.retailorderservice.exception;

public class StockInsuficienteException extends RuntimeException {

    public StockInsuficienteException(Long productId, int solicitado, int disponible) {
        super("Stock insuficiente para el producto " + productId
                + ": solicitado " + solicitado + ", disponible " + disponible);
    }
}
