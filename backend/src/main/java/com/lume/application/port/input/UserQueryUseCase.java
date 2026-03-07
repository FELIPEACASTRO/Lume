package com.lume.application.port.input;

import com.lume.application.dto.response.UserResponseDTO;
import com.lume.application.query.GetUserByIdQuery;
import com.lume.application.query.ListUsersQuery;
import org.springframework.data.domain.Page;

/**
 * Port de entrada para operações de leitura (Queries) de usuários.
 *
 * <p><b>Microservices Pattern: CQRS</b> - Interface dedicada exclusivamente
 * a operações de leitura, separada dos comandos de escrita.</p>
 *
 * <p><b>Princípio ISP (Interface Segregation):</b> Clientes que só precisam
 * de leitura dependem apenas desta interface.</p>
 */
public interface UserQueryUseCase {

    /**
     * Busca um usuário por ID.
     * <p><b>Complexidade: O(log n)</b> - busca por chave primária indexada.</p>
     *
     * @param query query contendo o ID do usuário
     * @return DTO com dados do usuário
     */
    UserResponseDTO findById(GetUserByIdQuery query);

    /**
     * Lista usuários de forma paginada.
     * <p><b>Complexidade: O(n/p)</b> onde n é o total de registros e p o tamanho da página,
     * com O(log n) para offset via índice.</p>
     *
     * @param query query contendo parâmetros de paginação
     * @return página de DTOs de usuários
     */
    Page<UserResponseDTO> findAll(ListUsersQuery query);
}
