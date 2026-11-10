package com.pawcare.exception;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resource, Object id) {
        super(String.format("%s con id %s no fue encontrado", resource, id));
    }
}
