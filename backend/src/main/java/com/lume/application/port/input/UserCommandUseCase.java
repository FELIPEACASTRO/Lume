package com.lume.application.port.input;

import com.lume.application.command.CreateUserCommand;
import com.lume.application.command.UpdateUserCommand;
import com.lume.application.dto.response.UserResponseDTO;

/**
 * Port de entrada para operações de escrita (Commands) de usuários.
 *
 * <p><b>Microservices Pattern: CQRS</b> - Separa operações de escrita (Commands)
 * das operações de leitura (Queries), permitindo otimização independente.</p>
 *
 * <p><b>Princípio ISP (Interface Segregation):</b> Interface dedicada apenas
 * a comandos, sem misturar com queries.</p>
 */
public interface UserCommandUseCase {

    /**
     * Cria um novo usuário.
     * <p><b>Complexidade: O(1)</b> para criação + O(log n) para verificação de unicidade no banco.</p>
     *
     * @param command dados do comando de criação
     * @return DTO com dados do usuário criado
     */
    UserResponseDTO create(CreateUserCommand command);

    /**
     * Atualiza um usuário existente.
     * <p><b>Complexidade: O(log n)</b> para busca por ID + O(log n) para verificação de e-mail.</p>
     *
     * @param command dados do comando de atualização
     * @return DTO com dados do usuário atualizado
     */
    UserResponseDTO update(UpdateUserCommand command);

    /**
     * Desativa um usuário (soft delete).
     * <p><b>Complexidade: O(log n)</b> para busca por ID.</p>
     *
     * @param id identificador do usuário
     */
    void delete(Long id);
}
