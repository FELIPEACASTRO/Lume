package com.lume.domain.exception;

/**
 * Excecao lancada quando um recurso solicitado nao e encontrado.
 */
public class ResourceNotFoundException extends DomainException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resource, Long id) {
        super(String.format("%s nao encontrado(a) com id: %d", resource, id));
    }

    public ResourceNotFoundException(String resource, String id) {
        super(String.format("%s nao encontrado(a) com id: %s", resource, id));
    }
}
