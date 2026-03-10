package com.lume.workspace.repository;

import com.lume.workspace.entity.UserPreferenceJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserPreferenceJpaRepository extends JpaRepository<UserPreferenceJpaEntity, Long> {

    Optional<UserPreferenceJpaEntity> findByUserId(Long userId);
}
