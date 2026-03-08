package com.lume.presentation.controller;

import com.lume.application.dto.request.UpdateUserRequestDTO;
import com.lume.application.dto.request.UserRequestDTO;
import com.lume.application.dto.response.UserResponseDTO;
import com.lume.application.mapper.UserMapper;
import com.lume.application.port.input.UserCommandUseCase;
import com.lume.workspace.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller REST para operações de escrita (Commands) de usuários.
 *
 * <p><b>Microservices Pattern: CQRS</b> - Controller dedicado exclusivamente a
 * operações de escrita (POST, PUT, DELETE), separado do controller de leitura.</p>
 *
 * <p><b>Clean Architecture:</b> Pertence à camada de apresentação e depende
 * apenas da abstração UserCommandUseCase (port de entrada).</p>
 *
 * <p><b>Princípios SOLID:</b></p>
 * <p>SRP: responsável apenas por receber requisições HTTP de escrita e delegá-las.</p>
 * <p>DIP: depende da abstração UserCommandUseCase, não da implementação.</p>
 * <p>ISP: depende apenas da interface de comandos, não da de queries.</p>
 */
@RestController
@RequestMapping("/users")
@Tag(name = "Usuários - Comandos", description = "Operações de escrita (criação, atualização, exclusão)")
public class UserCommandController {

    private final UserCommandUseCase commandUseCase;
    private final AuditLogService auditLogService;

    public UserCommandController(UserCommandUseCase commandUseCase, AuditLogService auditLogService) {
        this.commandUseCase = commandUseCase;
        this.auditLogService = auditLogService;
    }

    @PostMapping
    @Operation(summary = "Criar novo usuário", description = "Cria um novo usuário no sistema")
    public ResponseEntity<UserResponseDTO> create(@Valid @RequestBody UserRequestDTO dto) {
        var command = UserMapper.toCreateCommand(dto);
        UserResponseDTO created = commandUseCase.create(command);
        auditLogService.record(
                "user",
                String.valueOf(created.id()),
                "created",
                Map.of("email", created.email(), "active", created.active())
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar usuário", description = "Atualiza os dados de um usuário existente")
    public ResponseEntity<UserResponseDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequestDTO dto) {
        var command = UserMapper.toUpdateCommand(id, dto);
        UserResponseDTO updated = commandUseCase.update(command);
        auditLogService.record(
                "user",
                String.valueOf(updated.id()),
                "updated",
                Map.of("email", updated.email(), "active", updated.active())
        );
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Desativar usuário", description = "Desativa um usuário (soft delete)")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        commandUseCase.delete(id);
        auditLogService.record("user", String.valueOf(id), "deactivated", null);
        return ResponseEntity.noContent().build();
    }
}
