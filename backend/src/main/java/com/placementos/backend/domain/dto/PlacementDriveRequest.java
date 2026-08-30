package com.placementos.backend.domain.dto;

import com.placementos.backend.domain.enums.DriveStatus;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.Map;

/**
 * Inbound DTO for creating or updating a PlacementDrive.
 * JPA entity is not exposed directly to API callers.
 */
public class PlacementDriveRequest {

    @NotBlank(message = "Company name is required.")
    @Size(max = 255, message = "Company name must not exceed 255 characters.")
    private String companyName;

    @Size(max = 500, message = "Title must not exceed 500 characters.")
    private String title;

    private String description;

    private Instant receivedAt;

    private Instant applicationDeadline;

    @Size(max = 255, message = "Source email ID must not exceed 255 characters.")
    private String sourceEmailId;

    /**
     * Flexible eligibility criteria as key-value pairs.
     * Example keys: allowedBranches, minCgpa, maxCgpa, batch, gender.
     * No evaluation logic at this stage.
     */
    private Map<String, Object> eligibilityCriteria;

    private DriveStatus status;

    // -------------------------------------------------------------------------
    // Getters and Setters
    // -------------------------------------------------------------------------
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Instant getReceivedAt() { return receivedAt; }
    public void setReceivedAt(Instant receivedAt) { this.receivedAt = receivedAt; }

    public Instant getApplicationDeadline() { return applicationDeadline; }
    public void setApplicationDeadline(Instant applicationDeadline) { this.applicationDeadline = applicationDeadline; }

    public String getSourceEmailId() { return sourceEmailId; }
    public void setSourceEmailId(String sourceEmailId) { this.sourceEmailId = sourceEmailId; }

    public Map<String, Object> getEligibilityCriteria() { return eligibilityCriteria; }
    public void setEligibilityCriteria(Map<String, Object> eligibilityCriteria) { this.eligibilityCriteria = eligibilityCriteria; }

    public DriveStatus getStatus() { return status; }
    public void setStatus(DriveStatus status) { this.status = status; }
}
