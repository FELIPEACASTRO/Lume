package com.lume.domain.exception;

/**
 * Excecao lancada quando o usuario nao possui permissao
 * para acessar o recurso solicitado.
 */
public class AccessDeniedException extends DomainException {

    public AccessDeniedException(String message) {
        super(message);
    }
}
