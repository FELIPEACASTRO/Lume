package com.lume.application.handler.command;

import com.lume.application.port.output.UserRepositoryPort;
import com.lume.domain.exception.ResourceNotFoundException;
import com.lume.domain.model.User;

/**
 * Handler para o comando de exclusão (desativação) de usuário.
 *
 * <p><b>Microservices Pattern: CQRS</b> - Handler dedicado ao comando de exclusão.</p>
 * <p><b>Complexidade: O(log n)</b> para busca por ID + O(log n) para atualização.</p>
 */
public class DeleteUserCommandHandler {

    private final UserRepositoryPort userRepository;

    public DeleteUserCommandHandler(UserRepositoryPort userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Executa a desativação (soft delete) de um usuário.
     *
     * @param id identificador do usuário
     * @throws ResourceNotFoundException se o usuário não for encontrado
     */
    public void handle(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", id));

        user.deactivate();
        userRepository.save(user);
    }
}
