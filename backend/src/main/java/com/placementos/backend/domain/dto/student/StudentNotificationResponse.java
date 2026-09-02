package com.placementos.backend.domain.dto.student;

import com.placementos.backend.domain.enums.NotificationChannel;
import com.placementos.backend.domain.enums.NotificationStatus;
import com.placementos.backend.domain.enums.NotificationType;

import java.time.Instant;

public record StudentNotificationResponse(
        Long id,
        String idempotencyKey,
        NotificationType notificationType,
        NotificationChannel channel,
        NotificationStatus status,
        Long driveId,
        String companyName,
        Long roleId,
        String roleTitle,
        String messagePayload,
        Instant sentAt,
        Instant createdAt
) {}
