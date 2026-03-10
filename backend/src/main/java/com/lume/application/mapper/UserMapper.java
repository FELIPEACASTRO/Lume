package com.lume.application.mapper;

import com.lume.application.command.CreateUserCommand;
import com.lume.application.command.UpdateUserCommand;
import com.lume.application.dto.request.UpdateUserRequestDTO;
import com.lume.application.dto.request.UserRequestDTO;
import com.lume.application.dto.response.UserResponseDTO;
import com.lume.domain.model.User;

/**
 * Mapper responsável pela conversão entre objetos de domínio, DTOs e Commands.
 *
 * <p><b>Microservices Pattern: ACL (Anti-Corruption Layer)</b> - Centraliza toda
 * a lógica de tradução entre camadas, evitando que o domínio seja contaminado
 * por representações externas.</p>
 *
 * <p><b>Princípio SRP:</b> Responsável exclusivamente por mapeamento de objetos.</p>
 * <p><b>Complexidade: O(1)</b> para todas as operações de mapeamento.</p>
 */
public final class UserMapper {

    private UserMapper() {
        // Classe utilitária - não deve ser instanciada
    }

    /**
     * Converte uma entidade de domínio User para UserResponseDTO.
     */
    public static UserResponseDTO toResponseDTO(User user) {
        return new UserResponseDTO(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.isActive(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    /**
     * Converte um UserRequestDTO em CreateUserCommand.
     */
    public static CreateUserCommand toCreateCommand(UserRequestDTO dto) {
        return new CreateUserCommand(dto.name(), dto.email(), dto.password());
    }

    /**
     * Converte um UpdateUserRequestDTO em UpdateUserCommand com o ID fornecido.
     */
    public static UpdateUserCommand toUpdateCommand(Long id, UpdateUserRequestDTO dto) {
        return new UpdateUserCommand(id, dto.name(), dto.email(), dto.password());
    }
}
