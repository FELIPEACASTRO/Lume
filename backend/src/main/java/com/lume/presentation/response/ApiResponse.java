package com.lume.presentation.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

/**
 * Resposta padronizada da API para erros e mensagens.
 *
 * <p><b>Clean Code:</b> Estrutura consistente para todas as respostas de erro,
 * facilitando o tratamento pelos clientes da API.</p>
 *
 * @param timestamp momento do erro
 * @param status    código HTTP
 * @param error     mensagem de erro principal
 * @param details   detalhes adicionais (validação, etc.)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        Object details
) {

    public static ApiResponse of(int status, String error) {
        return new ApiResponse(LocalDateTime.now(), status, error, null);
    }

    public static ApiResponse of(int status, String error, Object details) {
        return new ApiResponse(LocalDateTime.now(), status, error, details);
    }
}
