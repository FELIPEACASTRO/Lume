package com.lume.workspace.repository;

import com.lume.workspace.entity.OrganizationJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrganizationJpaRepository extends JpaRepository<OrganizationJpaEntity, Long> {

    Optional<OrganizationJpaEntity> findBySlug(String slug);
}
