package com.lume.presentation.controller;

import com.lume.application.dto.response.UserResponseDTO;
import com.lume.application.port.input.UserQueryUseCase;
import com.lume.application.query.GetUserByIdQuery;
import com.lume.application.query.ListUsersQuery;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller REST para operações de leitura (Queries) de usuários.
 *
 * <p><b>Microservices Pattern: CQRS</b> - Controller dedicado exclusivamente a
 * operações de leitura (GET), separado do controller de escrita.</p>
 *
 * <p><b>Clean Architecture:</b> Pertence à camada de apresentação e depende
 * apenas da abstração UserQueryUseCase (port de entrada).</p>
 *
 * <p><b>Princípios SOLID:</b></p>
 * <p>SRP: responsável apenas por receber requisições HTTP de leitura.</p>
 * <p>DIP: depende da abstração UserQueryUseCase.</p>
 * <p>ISP: depende apenas da interface de queries, não da de comandos.</p>
 */
@RestController
@RequestMapping("/users")
@Tag(name = "Usuários - Consultas", description = "Operações de leitura (listagem, busca)")
public class UserQueryController {

    private final UserQueryUseCase queryUseCase;

    public UserQueryController(UserQueryUseCase queryUseCase) {
        this.queryUseCase = queryUseCase;
    }

    @GetMapping
    @Operation(summary = "Listar usuários", description = "Retorna uma lista paginada de usuários")
    public ResponseEntity<Page<UserResponseDTO>> findAll(
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        var query = new ListUsersQuery(pageable);
        return ResponseEntity.ok(queryUseCase.findAll(query));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar usuário por ID", description = "Retorna um usuário específico")
    public ResponseEntity<UserResponseDTO> findById(@PathVariable Long id) {
        var query = new GetUserByIdQuery(id);
        return ResponseEntity.ok(queryUseCase.findById(query));
    }
}
