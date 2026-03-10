package com.lume.workspace.service;

import com.lume.workspace.repository.AgentThreadJpaRepository;
import com.lume.workspace.repository.NotificationJpaRepository;
import com.lume.workspace.repository.TaskJpaRepository;
import org.springframework.stereotype.Service;

@Service
public class WorkspaceMeteringService {

    static final int DAILY_CREDITS = 300;

    private final TaskJpaRepository taskRepository;
    private final AgentThreadJpaRepository agentThreadRepository;
    private final NotificationJpaRepository notificationRepository;
    private final WorkspaceContextService workspaceContextService;

    public WorkspaceMeteringService(
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

    public UsageMeteringSnapshot currentSnapshot() {
        Long workspaceId = workspaceContextService.getWorkspaceId();
        long activeTasks = taskRepository.countByWorkspaceId(workspaceId);
        int scheduledTasks = (int) taskRepository.countByWorkspaceIdAndScheduledForIsNotNull(workspaceId);
        int threadCount = (int) agentThreadRepository.countByWorkspaceId(workspaceId);
        int unreadNotifications = (int) notificationRepository.countByWorkspaceIdAndReadFalse(workspaceId);

        int consumedCredits = Math.min(DAILY_CREDITS, (int) (activeTasks * 18 + threadCount * 12));
        int remainingCredits = Math.max(0, DAILY_CREDITS - consumedCredits);

        return new UsageMeteringSnapshot(
                DAILY_CREDITS,
                consumedCredits,
                remainingCredits,
                (int) activeTasks,
                scheduledTasks,
                unreadNotifications
        );
    }

    public record UsageMeteringSnapshot(
            int dailyCredits,
            int consumedCredits,
            int remainingCredits,
            int activeTasks,
            int scheduledTasks,
            int unreadNotifications
    ) {
    }
}
