package com.lume.application.handler.command;

import com.lume.application.command.CreateUserCommand;
import com.lume.application.command.UpdateUserCommand;
import com.lume.application.dto.response.UserResponseDTO;
import com.lume.application.port.input.UserCommandUseCase;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serviço que implementa o use case de comandos de usuário.
 *
 * <p><b>Microservices Pattern: CQRS</b> - Fachada que delega para handlers específicos,
 * mantendo a separação de responsabilidades.</p>
 *
 * <p><b>Design Pattern: Facade</b> - Simplifica a interface para o controller,
 * delegando para handlers especializados.</p>
 *
 * <p><b>Princípio SRP:</b> Orquestra comandos sem conter lógica de negócio.</p>
 */
public class UserCommandService implements UserCommandUseCase {

    private final CreateUserCommandHandler createHandler;
    private final UpdateUserCommandHandler updateHandler;
    private final DeleteUserCommandHandler deleteHandler;

    public UserCommandService(
            CreateUserCommandHandler createHandler,
            UpdateUserCommandHandler updateHandler,
            DeleteUserCommandHandler deleteHandler) {
        this.createHandler = createHandler;
        this.updateHandler = updateHandler;
        this.deleteHandler = deleteHandler;
    }

    @Override
    @Transactional
    public UserResponseDTO create(CreateUserCommand command) {
        return createHandler.handle(command);
    }

    @Override
    @Transactional
    public UserResponseDTO update(UpdateUserCommand command) {
        return updateHandler.handle(command);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        deleteHandler.handle(id);
    }
}
