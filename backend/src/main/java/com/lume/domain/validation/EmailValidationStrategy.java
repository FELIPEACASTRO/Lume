package com.lume.domain.validation;

import com.lume.domain.exception.BusinessRuleException;

import java.util.regex.Pattern;

/**
 * Estratégia de validação para formato de e-mail.
 *
 * <p><b>Design Pattern: Strategy</b> - Implementação concreta para validação de e-mail.</p>
 * <p><b>Complexidade: O(n)</b> onde n é o comprimento do e-mail (regex matching).</p>
 */
public class EmailValidationStrategy implements ValidationStrategy<String> {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    @Override
    public void validate(String email) {
        if (email == null || email.isBlank()) {
            throw new BusinessRuleException("O e-mail é obrigatório");
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new BusinessRuleException("Formato de e-mail inválido: " + email);
        }
    }
}
