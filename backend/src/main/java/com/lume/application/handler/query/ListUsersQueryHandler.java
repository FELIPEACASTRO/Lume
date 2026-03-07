package com.lume.application.handler.query;

import com.lume.application.dto.response.UserResponseDTO;
import com.lume.application.mapper.UserMapper;
import com.lume.application.port.output.UserRepositoryPort;
import com.lume.application.query.ListUsersQuery;
import org.springframework.data.domain.Page;

/**
 * Handler para a query de listagem paginada de usuários.
 *
 * <p><b>Microservices Pattern: CQRS</b> - Handler dedicado à query de listagem.</p>
 *
 * <p><b>Complexidade: O(p + log n)</b> onde p é o tamanho da página e n o total
 * de registros. O banco utiliza OFFSET/LIMIT com índice para paginação eficiente.</p>
 */
public class ListUsersQueryHandler {

    private final UserRepositoryPort userRepository;

    public ListUsersQueryHandler(UserRepositoryPort userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Executa a listagem paginada de usuários.
     *
     * @param query query contendo parâmetros de paginação
     * @return página de DTOs de usuários
     */
    public Page<UserResponseDTO> handle(ListUsersQuery query) {
        return userRepository.findAll(query.pageable())
                .map(UserMapper::toResponseDTO);
    }
}
