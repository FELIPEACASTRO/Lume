package com.lume.workspace.service;

import com.lume.workspace.dto.NotificationResponse;
import com.lume.workspace.repository.NotificationJpaRepository;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class NotificationService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    private final NotificationJpaRepository notificationRepository;
    private final WorkspaceContextService workspaceContextService;

    public NotificationService(
            NotificationJpaRepository notificationRepository,
            WorkspaceContextService workspaceContextService
    ) {
        this.notificationRepository = notificationRepository;
        this.workspaceContextService = workspaceContextService;
    }

    public List<NotificationResponse> listNotifications() {
        return notificationRepository.findByWorkspaceIdOrderByCreatedAtDesc(workspaceContextService.getWorkspaceId()).stream()
                .map(notification -> new NotificationResponse(
                        notification.getId(),
                        notification.getKind(),
                        notification.getTitle(),
                        notification.getBody(),
                        notification.getPath(),
                        notification.isRead(),
                        notification.getCreatedAt().format(DATE_TIME_FORMATTER)
                ))
                .toList();
    }

    public int unreadCount() {
        return (int) notificationRepository.countByWorkspaceIdAndReadFalse(workspaceContextService.getWorkspaceId());
    }
}
