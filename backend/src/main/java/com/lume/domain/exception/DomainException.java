package com.lume.domain.exception;

/**
 * Exceção base para todas as exceções de domínio.
 *
 * <p>Pertence à camada de domínio e não possui dependências externas.
 * Todas as exceções de negócio devem estender esta classe.</p>
 */
public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }

    protected DomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
