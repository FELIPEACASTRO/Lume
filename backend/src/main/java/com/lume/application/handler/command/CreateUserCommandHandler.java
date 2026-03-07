package com.lume.application.handler.command;

import com.lume.application.command.CreateUserCommand;
import com.lume.application.dto.response.UserResponseDTO;
import com.lume.application.mapper.UserMapper;
import com.lume.application.port.output.UserRepositoryPort;
import com.lume.domain.exception.BusinessRuleException;
import com.lume.domain.factory.UserFactory;
import com.lume.domain.model.User;

/**
 * Handler para o comando de criação de usuário.
 *
 * <p><b>Microservices Pattern: CQRS</b> - Handler dedicado ao processamento
 * do comando de criação, separado dos handlers de query.</p>
 *
 * <p><b>Princípio SRP:</b> Responsável exclusivamente pelo fluxo de criação.</p>
 *
 * <p><b>Complexidade geral: O(log n)</b> dominada pela verificação de unicidade
 * de e-mail no banco de dados (índice B-tree).</p>
 */
public class CreateUserCommandHandler {

    private final UserRepositoryPort userRepository;
    private final UserFactory userFactory;

    public CreateUserCommandHandler(UserRepositoryPort userRepository, UserFactory userFactory) {
        this.userRepository = userRepository;
        this.userFactory = userFactory;
    }

    /**
     * Executa o comando de criação de usuário.
     *
     * @param command dados do novo usuário
     * @return DTO com dados do usuário criado
     * @throws BusinessRuleException se o e-mail já estiver cadastrado
     */
    public UserResponseDTO handle(CreateUserCommand command) {
        ensureEmailIsUnique(command.email());

        User user = userFactory.createUser(command.name(), command.email(), command.password());
        User saved = userRepository.save(user);

        return UserMapper.toResponseDTO(saved);
    }

    private void ensureEmailIsUnique(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new BusinessRuleException("Já existe um usuário cadastrado com o e-mail: " + email);
        }
    }
}
