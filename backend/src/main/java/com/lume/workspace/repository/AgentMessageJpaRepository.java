package com.lume.workspace.repository;

import com.lume.workspace.entity.AgentMessageJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AgentMessageJpaRepository extends JpaRepository<AgentMessageJpaEntity, String> {

    List<AgentMessageJpaEntity> findByThreadIdOrderByCreatedAtAsc(String threadId);
}
