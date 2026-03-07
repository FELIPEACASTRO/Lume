package com.lume.domain.exception;

/**
 * Exceção lançada quando um recurso solicitado não é encontrado.
 *
 * <p>Mapeada para HTTP 404 na camada de apresentação.</p>
 */
public class ResourceNotFoundException extends DomainException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resource, Long id) {
        super(String.format("%s não encontrado(a) com id: %d", resource, id));
    }
}
