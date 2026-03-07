package com.lume.domain.factory;

import com.lume.domain.model.User;
import com.lume.domain.service.PasswordEncoder;
import com.lume.domain.validation.UserValidationChain;

/**
 * Factory para criação de entidades User com validação e codificação de senha.
 *
 * <p><b>Design Pattern: Factory</b> - Centraliza a lógica de criação de objetos User,
 * garantindo que toda instância criada passe por validação e tenha a senha codificada.</p>
 *
 * <p><b>Princípios SOLID:</b> SRP (responsável apenas pela criação),
 * DIP (depende das abstrações PasswordEncoder e ValidationStrategy).</p>
 *
 * <p><b>Complexidade: O(1)</b> para criação (desconsiderando o custo do hash de senha).</p>
 */
public class UserFactory {

    private final PasswordEncoder passwordEncoder;
    private final UserValidationChain validationChain;

    public UserFactory(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
        this.validationChain = new UserValidationChain();
    }

    /**
     * Cria uma nova instância de User validada e com senha codificada.
     *
     * @param name     nome do usuário
     * @param email    e-mail do usuário
     * @param password senha em texto plano
     * @return instância de User pronta para persistência
     */
    public User createUser(String name, String email, String password) {
        validationChain.validateForCreation(name, email, password);

        return User.builder()
                .name(name)
                .email(email)
                .password(passwordEncoder.encode(password))
                .active(true)
                .build();
    }
}
