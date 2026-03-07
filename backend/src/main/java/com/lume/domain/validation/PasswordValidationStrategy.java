package com.lume.domain.validation;

import com.lume.domain.exception.BusinessRuleException;

/**
 * Estratégia de validação para senha de usuário.
 *
 * <p><b>Design Pattern: Strategy</b> - Implementação concreta para validação de senha.</p>
 * <p><b>Complexidade: O(1)</b> - verificações de comprimento são constantes.</p>
 */
public class PasswordValidationStrategy implements ValidationStrategy<String> {

    private static final int MIN_LENGTH = 6;
    private static final int MAX_LENGTH = 100;

    @Override
    public void validate(String password) {
        if (password == null || password.isBlank()) {
            throw new BusinessRuleException("A senha é obrigatória");
        }
        if (password.length() < MIN_LENGTH || password.length() > MAX_LENGTH) {
            throw new BusinessRuleException(
                    String.format("A senha deve ter entre %d e %d caracteres", MIN_LENGTH, MAX_LENGTH)
            );
        }
    }
}
