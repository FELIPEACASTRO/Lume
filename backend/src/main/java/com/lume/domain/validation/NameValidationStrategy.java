package com.lume.domain.validation;

import com.lume.domain.exception.BusinessRuleException;

/**
 * Estratégia de validação para nome de usuário.
 *
 * <p><b>Design Pattern: Strategy</b> - Implementação concreta para validação de nome.</p>
 * <p><b>Complexidade: O(1)</b> - verificações de comprimento são constantes.</p>
 */
public class NameValidationStrategy implements ValidationStrategy<String> {

    private static final int MIN_LENGTH = 2;
    private static final int MAX_LENGTH = 150;

    @Override
    public void validate(String name) {
        if (name == null || name.isBlank()) {
            throw new BusinessRuleException("O nome é obrigatório");
        }
        if (name.trim().length() < MIN_LENGTH || name.trim().length() > MAX_LENGTH) {
            throw new BusinessRuleException(
                    String.format("O nome deve ter entre %d e %d caracteres", MIN_LENGTH, MAX_LENGTH)
            );
        }
    }
}
