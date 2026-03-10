package com.lume.infrastructure.persistence.repository;

import com.lume.infrastructure.persistence.entity.UserJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @Query("""
            select count(u)
            from UserJpaEntity u
            where u.active = true
              and exists (
                    select m.id
                    from MembershipJpaEntity m
                    where m.userId = u.id
                      and m.workspaceId = :workspaceId
                      and m.active = true
              )
            """)
    long countActiveMembersByWorkspaceId(@Param("workspaceId") Long workspaceId);

    @Query("""
            select u
            from UserJpaEntity u
            where u.active = true
              and exists (
                    select m.id
                    from MembershipJpaEntity m
                    where m.userId = u.id
                      and m.workspaceId = :workspaceId
                      and m.active = true
              )
            order by u.name asc
            """)
    List<UserJpaEntity> findActiveMembersByWorkspaceId(@Param("workspaceId") Long workspaceId, Pageable pageable);

    @Query("""
            select u
            from UserJpaEntity u
            where u.active = true
              and exists (
                    select m.id
                    from MembershipJpaEntity m
                    where m.userId = u.id
                      and m.workspaceId = :workspaceId
                      and m.active = true
              )
              and (
                    lower(u.name) like lower(concat('%', :query, '%'))
                 or lower(u.email) like lower(concat('%', :query, '%'))
              )
            order by u.name asc
            """)
    List<UserJpaEntity> searchActiveMembersByWorkspaceId(
            @Param("workspaceId") Long workspaceId,
            @Param("query") String query,
            Pageable pageable
    );
}
