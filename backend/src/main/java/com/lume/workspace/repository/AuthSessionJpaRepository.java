package com.lume.workspace.repository;

import com.lume.workspace.entity.AuthSessionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface AuthSessionJpaRepository extends JpaRepository<AuthSessionJpaEntity, String> {

    Optional<AuthSessionJpaEntity> findByIdAndInvalidatedAtIsNull(String id);

    void deleteByExpiresAtBefore(LocalDateTime threshold);
}
