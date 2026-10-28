package com.axity.course.retailorderservice.exception;

public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String message) {
        super(message);
    }

    public RecursoNoEncontradoException(String resource, Object id) {
        super("No se encontró el " + resource + " con id " + id);
    }
}
