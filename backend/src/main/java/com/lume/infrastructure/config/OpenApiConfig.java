package com.lume.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração do OpenAPI/Swagger para documentação da API.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Lume API")
                        .version("0.1.0")
                        .description("API REST do projeto Lume - Arquitetura Clean Architecture com CQRS")
                        .contact(new Contact()
                                .name("Lume Team")
                                .email("contato@lume.com")));
    }
}
