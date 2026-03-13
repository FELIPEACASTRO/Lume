package com.lume.infrastructure.config;

import com.lume.application.handler.command.*;
import com.lume.application.handler.query.*;
import com.lume.application.port.input.UserCommandUseCase;
import com.lume.application.port.input.UserQueryUseCase;
import com.lume.application.port.output.UserRepositoryPort;
import com.lume.domain.factory.UserFactory;
import com.lume.domain.service.PasswordEncoder;
import com.lume.infrastructure.security.BCryptPasswordEncoderAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Configuração central de beans da aplicação.
 *
 * <p><b>Design Pattern: Singleton</b> - Todos os beans gerenciados pelo Spring IoC Container
 * são Singletons por padrão (scope = singleton), garantindo uma única instância por contexto.</p>
 *
 * <p><b>Clean Architecture:</b> Esta classe pertence à camada de infraestrutura e é
 * responsável por conectar (wiring) todas as camadas, respeitando a regra de dependência:
 * as camadas internas (Domain, Application) não conhecem as externas (Infrastructure).</p>
 *
 * <p><b>Princípio DIP:</b> Os beans são registrados por suas interfaces (abstrações),
 * permitindo que as camadas superiores dependam apenas de contratos.</p>
 */
@Configuration
public class BeanConfig {

    // ─── Infrastructure Beans ────────────────────────────────────────────

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }

    // ─── Domain Layer Beans ─────────────────────────────────────────────

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoderAdapter();
    }

    @Bean
    public UserFactory userFactory(PasswordEncoder passwordEncoder) {
        return new UserFactory(passwordEncoder);
    }

    // ─── Application Layer - Command Handlers ───────────────────────────

    @Bean
    public CreateUserCommandHandler createUserCommandHandler(
            UserRepositoryPort userRepository, UserFactory userFactory) {
        return new CreateUserCommandHandler(userRepository, userFactory);
    }

    @Bean
    public UpdateUserCommandHandler updateUserCommandHandler(
            UserRepositoryPort userRepository, PasswordEncoder passwordEncoder) {
        return new UpdateUserCommandHandler(userRepository, passwordEncoder);
    }

    @Bean
    public DeleteUserCommandHandler deleteUserCommandHandler(UserRepositoryPort userRepository) {
        return new DeleteUserCommandHandler(userRepository);
    }

    // ─── Application Layer - Query Handlers ─────────────────────────────

    @Bean
    public GetUserByIdQueryHandler getUserByIdQueryHandler(UserRepositoryPort userRepository) {
        return new GetUserByIdQueryHandler(userRepository);
    }

    @Bean
    public ListUsersQueryHandler listUsersQueryHandler(UserRepositoryPort userRepository) {
        return new ListUsersQueryHandler(userRepository);
    }

    // ─── Application Layer - Use Case Facades ───────────────────────────

    @Bean
    public UserCommandUseCase userCommandUseCase(
            CreateUserCommandHandler createHandler,
            UpdateUserCommandHandler updateHandler,
            DeleteUserCommandHandler deleteHandler) {
        return new UserCommandService(createHandler, updateHandler, deleteHandler);
    }

    @Bean
    public UserQueryUseCase userQueryUseCase(
            GetUserByIdQueryHandler getByIdHandler,
            ListUsersQueryHandler listHandler) {
        return new UserQueryService(getByIdHandler, listHandler);
    }
}
