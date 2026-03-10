package com.lume.workspace.repository;

import com.lume.workspace.entity.ShellTaskTypeJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShellTaskTypeJpaRepository extends JpaRepository<ShellTaskTypeJpaEntity, String> {

    List<ShellTaskTypeJpaEntity> findAllByOrderBySortOrderAsc();

    List<ShellTaskTypeJpaEntity> findByEnabledTrueOrderBySortOrderAsc();
}
