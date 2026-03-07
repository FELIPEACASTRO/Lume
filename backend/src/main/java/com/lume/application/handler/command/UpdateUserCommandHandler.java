package com.lume.application.handler.command;

import com.lume.application.command.UpdateUserCommand;
import com.lume.application.dto.response.UserResponseDTO;
import com.lume.application.mapper.UserMapper;
import com.lume.application.port.output.UserRepositoryPort;
import com.lume.domain.exception.BusinessRuleException;
import com.lume.domain.exception.ResourceNotFoundException;
import com.lume.domain.model.User;
import com.lume.domain.service.PasswordEncoder;
import com.lume.domain.validation.UserValidationChain;

/**
 * Handler para o comando de atualização de usuário.
 *
 * <p><b>Microservices Pattern: CQRS</b> - Handler dedicado ao comando de atualização.</p>
 *
 * <p><b>Complexidade geral: O(log n)</b> dominada pelas buscas por ID e e-mail
 * no banco de dados.</p>
 */
public class UpdateUserCommandHandler {

    private final UserRepositoryPort userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserValidationChain validationChain;

    public UpdateUserCommandHandler(UserRepositoryPort userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.validationChain = new UserValidationChain();
    }

    /**
     * Executa o comando de atualização de usuário.
     *
     * @param command dados de atualização
     * @return DTO com dados do usuário atualizado
     * @throws ResourceNotFoundException se o usuário não for encontrado
     * @throws BusinessRuleException     se o e-mail já pertencer a outro usuário
     */
    public UserResponseDTO handle(UpdateUserCommand command) {
        validationChain.validateForUpdate(command.name(), command.email(), command.password());

        User user = userRepository.findById(command.id())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", command.id()));

        ensureEmailIsUniqueForOtherUser(command.email(), command.id());

        user.updateName(command.name());
        user.updateEmail(command.email());

        if (command.password() != null && !command.password().isBlank()) {
            user.updatePassword(passwordEncoder.encode(command.password()));
        }

        User saved = userRepository.save(user);
        return UserMapper.toResponseDTO(saved);
    }

    private void ensureEmailIsUniqueForOtherUser(String email, Long userId) {
        userRepository.findByEmail(email)
                .filter(existing -> !existing.getId().equals(userId))
                .ifPresent(existing -> {
                    throw new BusinessRuleException("Já existe um usuário cadastrado com o e-mail: " + email);
                });
    }
}
