package com.lume.workspace.service;

import com.lume.workspace.dto.UsageSummaryResponse;
import com.lume.workspace.repository.AgentThreadJpaRepository;
import com.lume.workspace.repository.NotificationJpaRepository;
import com.lume.workspace.repository.TaskJpaRepository;
import org.springframework.stereotype.Service;

@Service
public class UsageService {

    private static final int DAILY_CREDITS = 300;

    private final TaskJpaRepository taskRepository;
    private final AgentThreadJpaRepository agentThreadRepository;
    private final NotificationJpaRepository notificationRepository;
    private final WorkspaceContextService workspaceContextService;

    public UsageService(
            TaskJpaRepository taskRepository,
            AgentThreadJpaRepository agentThreadRepository,
            NotificationJpaRepository notificationRepository,
            WorkspaceContextService workspaceContextService
    ) {
        this.taskRepository = taskRepository;
        this.agentThreadRepository = agentThreadRepository;
        this.notificationRepository = notificationRepository;
        this.workspaceContextService = workspaceContextService;
    }

    public UsageSummaryResponse getSummary() {
        long activeTasks = taskRepository.findByWorkspaceIdOrderByUpdatedAtDesc(workspaceContextService.getWorkspaceId()).size();
        int scheduledTasks = (int) taskRepository.findByWorkspaceIdOrderByUpdatedAtDesc(workspaceContextService.getWorkspaceId()).stream()
                .filter(task -> task.getScheduledFor() != null)
                .count();
        int threadCount = agentThreadRepository.findByWorkspaceIdOrderByUpdatedAtDesc(workspaceContextService.getWorkspaceId()).size();
        int unreadNotifications = (int) notificationRepository.countByWorkspaceIdAndReadFalse(workspaceContextService.getWorkspaceId());

        int consumedCredits = Math.min(DAILY_CREDITS, (int) (activeTasks * 18 + threadCount * 12));
        int remainingCredits = Math.max(0, DAILY_CREDITS - consumedCredits);

        return new UsageSummaryResponse(
                DAILY_CREDITS,
                consumedCredits,
                remainingCredits,
                (int) activeTasks,
                scheduledTasks,
                unreadNotifications,
                "A camada de creditos aqui representa metering operacional do workspace, nao faturamento final."
        );
    }
}
