package com.lume.domain.exception;

/**
 * Exceção lançada quando uma regra de negócio é violada.
 *
 * <p>Mapeada para HTTP 422 (Unprocessable Entity) na camada de apresentação.</p>
 */
public class BusinessRuleException extends DomainException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
