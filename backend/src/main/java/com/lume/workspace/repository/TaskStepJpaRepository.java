package com.lume.workspace.repository;

import com.lume.workspace.entity.TaskStepJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskStepJpaRepository extends JpaRepository<TaskStepJpaEntity, String> {

    List<TaskStepJpaEntity> findByTaskIdOrderByStepOrderAsc(String taskId);
}
