package com.lume.application.handler.query;

import com.lume.application.dto.response.UserResponseDTO;
import com.lume.application.port.input.UserQueryUseCase;
import com.lume.application.query.GetUserByIdQuery;
import com.lume.application.query.ListUsersQuery;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serviço que implementa o use case de queries de usuário.
 *
 * <p><b>Microservices Pattern: CQRS</b> - Fachada dedicada a operações de leitura,
 * com transações read-only para otimização de performance.</p>
 *
 * <p><b>Princípio SRP:</b> Orquestra queries sem conter lógica de negócio.</p>
 */
public class UserQueryService implements UserQueryUseCase {

    private final GetUserByIdQueryHandler getByIdHandler;
    private final ListUsersQueryHandler listHandler;

    public UserQueryService(GetUserByIdQueryHandler getByIdHandler, ListUsersQueryHandler listHandler) {
        this.getByIdHandler = getByIdHandler;
        this.listHandler = listHandler;
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponseDTO findById(GetUserByIdQuery query) {
        return getByIdHandler.handle(query);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponseDTO> findAll(ListUsersQuery query) {
        return listHandler.handle(query);
    }
}
