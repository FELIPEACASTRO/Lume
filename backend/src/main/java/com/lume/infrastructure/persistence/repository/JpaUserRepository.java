package com.lume.infrastructure.persistence.repository;

import com.lume.infrastructure.persistence.entity.UserJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Repositório JPA para acesso direto ao banco de dados PostgreSQL.
 *
 * <p><b>Clean Architecture:</b> Pertence à camada de infraestrutura.
 * Não é exposto diretamente às camadas superiores; o acesso é feito
 * através do {@link UserRepositoryAdapter}.</p>
 */
@Repository
public interface JpaUserRepository extends JpaRepository<UserJpaEntity, Long> {

    Optional<UserJpaEntity> findByEmail(String email);

    Optional<UserJpaEntity> findByEmailAndActiveTrue(String email);

    boolean existsByEmail(String email);

    Optional<UserJpaEntity> findByIdAndActiveTrue(Long id);

    Optional<UserJpaEntity> findTopByActiveTrueOrderByCreatedAtAsc();

    List<UserJpaEntity> findByIdInOrderByNameAsc(Collection<Long> ids);

    List<UserJpaEntity> findTop5ByWorkspaceIdOrderByUpdatedAtDesc(Long workspaceId);
}
