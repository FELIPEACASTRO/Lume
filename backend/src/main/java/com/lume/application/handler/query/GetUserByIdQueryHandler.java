package com.lume.application.handler.query;

import com.lume.application.dto.response.UserResponseDTO;
import com.lume.application.mapper.UserMapper;
import com.lume.application.port.output.UserRepositoryPort;
import com.lume.application.query.GetUserByIdQuery;
import com.lume.domain.exception.ResourceNotFoundException;
import com.lume.domain.model.User;

/**
 * Handler para a query de busca de usuário por ID.
 *
 * <p><b>Microservices Pattern: CQRS</b> - Handler dedicado à query de leitura.</p>
 * <p><b>Complexidade: O(log n)</b> - busca por chave primária indexada (B-tree).</p>
 */
public class GetUserByIdQueryHandler {

    private final UserRepositoryPort userRepository;

    public GetUserByIdQueryHandler(UserRepositoryPort userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Executa a busca de um usuário por ID.
     *
     * @param query query contendo o ID
     * @return DTO com dados do usuário
     * @throws ResourceNotFoundException se o usuário não for encontrado
     */
    public UserResponseDTO handle(GetUserByIdQuery query) {
        User user = userRepository.findById(query.id())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", query.id()));

        return UserMapper.toResponseDTO(user);
    }
}
