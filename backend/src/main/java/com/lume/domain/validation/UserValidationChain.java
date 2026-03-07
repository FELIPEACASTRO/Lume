package com.lume.domain.validation;

import com.lume.domain.model.User;

/**
 * Cadeia de validação para a entidade User, compondo múltiplas estratégias.
 *
 * <p><b>Design Patterns aplicados:</b> Strategy (composição de validadores)
 * e Chain of Responsibility (execução sequencial).</p>
 *
 * <p><b>Complexidade: O(n)</b> onde n é o número de estratégias na cadeia.
 * Cada estratégia individual opera em O(1) ou O(m) onde m é o tamanho do campo.</p>
 */
public class UserValidationChain {

    private final ValidationStrategy<String> nameValidator;
    private final ValidationStrategy<String> emailValidator;
    private final ValidationStrategy<String> passwordValidator;

    public UserValidationChain() {
        this.nameValidator = new NameValidationStrategy();
        this.emailValidator = new EmailValidationStrategy();
        this.passwordValidator = new PasswordValidationStrategy();
    }

    /**
     * Valida todos os campos de criação de um usuário.
     *
     * @param name     nome do usuário
     * @param email    e-mail do usuário
     * @param password senha do usuário
     */
    public void validateForCreation(String name, String email, String password) {
        nameValidator.validate(name);
        emailValidator.validate(email);
        passwordValidator.validate(password);
    }

    /**
     * Valida campos de atualização de um usuário.
     * A senha é opcional na atualização.
     *
     * @param name     nome do usuário
     * @param email    e-mail do usuário
     * @param password senha (pode ser nula para manter a atual)
     */
    public void validateForUpdate(String name, String email, String password) {
        nameValidator.validate(name);
        emailValidator.validate(email);
        if (password != null && !password.isBlank()) {
            passwordValidator.validate(password);
        }
    }
}
