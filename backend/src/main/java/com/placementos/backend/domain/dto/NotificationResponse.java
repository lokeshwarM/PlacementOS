package com.placementos.backend.domain.dto;

import com.placementos.backend.domain.entity.Notification;
import com.placementos.backend.domain.enums.NotificationChannel;
import com.placementos.backend.domain.enums.NotificationStatus;
import com.placementos.backend.domain.enums.NotificationType;

import java.time.Instant;

public class NotificationResponse {

    private Long id;
    private Long studentId;
    private Long placementDriveId;
    private NotificationType notificationType;
    private NotificationChannel channel;
    private NotificationStatus status;
    private Instant createdAt;

    public static NotificationResponse from(Notification notification) {
        NotificationResponse response = new NotificationResponse();
        response.id = notification.getId();
        response.studentId = notification.getStudent().getId();
        response.placementDriveId = notification.getPlacementDrive().getId();
        response.notificationType = notification.getNotificationType();
        response.channel = notification.getChannel();
        response.status = notification.getStatus();
        response.createdAt = notification.getCreatedAt();
        return response;
    }

    public Long getId() { return id; }
    public Long getStudentId() { return studentId; }
    public Long getPlacementDriveId() { return placementDriveId; }
    public NotificationType getNotificationType() { return notificationType; }
    public NotificationChannel getChannel() { return channel; }
    public NotificationStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
}
