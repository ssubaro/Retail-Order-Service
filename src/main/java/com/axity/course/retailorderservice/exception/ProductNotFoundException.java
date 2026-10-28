package com.axity.course.retailorderservice.exception;

public class ProductNotFoundException extends RecursoNoEncontradoException {

    public ProductNotFoundException(Long id) {
        super("producto", id);
    }
}
