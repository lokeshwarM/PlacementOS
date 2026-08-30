package com.placementos.backend.domain.dto;

import com.placementos.backend.domain.entity.Application;
import com.placementos.backend.domain.enums.ApplicationStatus;

import java.time.Instant;

public class ApplicationResponse {

    private Long id;
    private Long studentId;
    private Long placementDriveId;
    private ApplicationStatus status;
    private Instant createdAt;
    private Instant updatedAt;

    public static ApplicationResponse from(Application application) {
        ApplicationResponse response = new ApplicationResponse();
        response.id = application.getId();
        response.studentId = application.getStudent().getId();
        response.placementDriveId = application.getPlacementDrive().getId();
        response.status = application.getStatus();
        response.createdAt = application.getCreatedAt();
        response.updatedAt = application.getUpdatedAt();
        return response;
    }

    public Long getId() { return id; }
    public Long getStudentId() { return studentId; }
    public Long getPlacementDriveId() { return placementDriveId; }
    public ApplicationStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
