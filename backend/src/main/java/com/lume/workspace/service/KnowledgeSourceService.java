package com.lume.workspace.service;

import com.lume.workspace.repository.KnowledgeSourceJpaRepository;
import org.springframework.stereotype.Service;

@Service
public class KnowledgeSourceService {

    private final KnowledgeSourceJpaRepository knowledgeSourceRepository;
    private final WorkspaceContextService workspaceContextService;

    public KnowledgeSourceService(
            KnowledgeSourceJpaRepository knowledgeSourceRepository,
            WorkspaceContextService workspaceContextService
    ) {
        this.knowledgeSourceRepository = knowledgeSourceRepository;
        this.workspaceContextService = workspaceContextService;
    }

    public int countSources() {
        return knowledgeSourceRepository.findByWorkspaceIdOrderByUpdatedAtDesc(workspaceContextService.getWorkspaceId()).size();
    }
}
