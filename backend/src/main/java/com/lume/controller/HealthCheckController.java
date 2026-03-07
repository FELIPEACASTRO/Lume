package com.lume.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/health")
@Tag(name = "Health Check", description = "Endpoint para verificação de saúde da API")
public class HealthCheckController {

    @GetMapping
    @Operation(summary = "Verificar saúde da API", description = "Retorna o status da aplicação")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "application", "Lume Backend",
                "timestamp", LocalDateTime.now()
        ));
    }
}
