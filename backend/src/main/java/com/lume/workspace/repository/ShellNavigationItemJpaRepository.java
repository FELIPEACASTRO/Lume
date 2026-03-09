package com.lume.workspace.repository;

import com.lume.workspace.entity.ShellNavigationItemJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ShellNavigationItemJpaRepository extends JpaRepository<ShellNavigationItemJpaEntity, String> {

    List<ShellNavigationItemJpaEntity> findAllByOrderBySortOrderAsc();

    List<ShellNavigationItemJpaEntity> findByEnabledTrueOrderBySortOrderAsc();

    Optional<ShellNavigationItemJpaEntity> findByPathIgnoreCase(String path);
}
