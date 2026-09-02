package com.placementos.backend.domain.dto.student;

import com.placementos.backend.domain.enums.ReminderStatus;

import java.time.Instant;

public record StudentReminderResponse(
        Long id,
        Long driveId,
        String companyName,
        Long roleId,
        String roleTitle,
        Instant scheduledFor,
        Integer intervalMinutes,
        Integer remindersSent,
        Integer maxReminders,
        String cancelReason,
        Instant lastReminderAt,
        ReminderStatus status,
        boolean isActive,
        Instant completedAt
) {}
