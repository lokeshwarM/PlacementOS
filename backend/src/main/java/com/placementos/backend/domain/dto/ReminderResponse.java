package com.placementos.backend.domain.dto;

import com.placementos.backend.domain.entity.ReminderTask;
import com.placementos.backend.domain.enums.ReminderStatus;

import java.time.Instant;

public class ReminderResponse {

    private Long id;
    private Long studentId;
    private Long placementDriveId;
    private Instant scheduledFor;
    private ReminderStatus status;
    private Instant completedAt;
    private Instant createdAt;

    public static ReminderResponse from(ReminderTask task) {
        ReminderResponse response = new ReminderResponse();
        response.id = task.getId();
        response.studentId = task.getStudent().getId();
        response.placementDriveId = task.getPlacementDrive().getId();
        response.scheduledFor = task.getScheduledFor();
        response.status = task.getStatus();
        response.completedAt = task.getCompletedAt();
        response.createdAt = task.getCreatedAt();
        return response;
    }

    public Long getId() { return id; }
    public Long getStudentId() { return studentId; }
    public Long getPlacementDriveId() { return placementDriveId; }
    public Instant getScheduledFor() { return scheduledFor; }
    public ReminderStatus getStatus() { return status; }
    public Instant getCompletedAt() { return completedAt; }
    public Instant getCreatedAt() { return createdAt; }
}
