package com.placementos.backend.domain.dto;

import com.placementos.backend.domain.entity.PlacementDrive;
import com.placementos.backend.domain.enums.DriveStatus;
import java.time.Instant;
import java.util.Map;

/**
 * Outbound DTO for returning PlacementDrive data to API callers.
 * The JPA entity is never returned directly.
 */
public class PlacementDriveResponse {

    private Long id;
    private String companyName;
    private String title;
    private String description;
    private Instant receivedAt;
    private Instant applicationDeadline;
    private String sourceEmailId;
    private Map<String, Object> eligibilityCriteria;
    private DriveStatus status;
    private Instant createdAt;
    private Instant updatedAt;

    public static PlacementDriveResponse from(PlacementDrive drive) {
        PlacementDriveResponse response = new PlacementDriveResponse();
        response.id = drive.getId();
        response.companyName = drive.getCompanyName();
        response.title = drive.getTitle();
        response.description = drive.getDescription();
        response.receivedAt = drive.getReceivedAt();
        response.applicationDeadline = drive.getApplicationDeadline();
        response.sourceEmailId = drive.getSourceEmailId();
        response.eligibilityCriteria = drive.getEligibilityCriteria();
        response.status = drive.getStatus();
        response.createdAt = drive.getCreatedAt();
        response.updatedAt = drive.getUpdatedAt();
        return response;
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------
    public Long getId() { return id; }
    public String getCompanyName() { return companyName; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Instant getReceivedAt() { return receivedAt; }
    public Instant getApplicationDeadline() { return applicationDeadline; }
    public String getSourceEmailId() { return sourceEmailId; }
    public Map<String, Object> getEligibilityCriteria() { return eligibilityCriteria; }
    public DriveStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
