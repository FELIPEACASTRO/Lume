package com.lume.workspace.repository;

import com.lume.workspace.entity.HomeOverviewBlockJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HomeOverviewBlockJpaRepository extends JpaRepository<HomeOverviewBlockJpaEntity, String> {

    List<HomeOverviewBlockJpaEntity> findAllByOrderBySortOrderAsc();

    Optional<HomeOverviewBlockJpaEntity> findByBlockTypeIgnoreCase(String blockType);
}
