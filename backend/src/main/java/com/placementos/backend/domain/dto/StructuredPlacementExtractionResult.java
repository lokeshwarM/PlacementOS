package com.placementos.backend.domain.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Stable output contract for placement email classification and extraction.
 * Communicated from Python processing service to Spring Boot.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StructuredPlacementExtractionResult {

    private String messageId;
    private String sourceEmail;
    private ClassificationResultDto classification;
    private String companyName;
    private String driveTitle;
    private String description;
    private Instant applicationDeadline;
    private List<ImportantDateDto> importantDates;
    private StructuredEligibilityDto commonEligibility;
    private List<ExtractedRoleDto> roles;
    private Map<String, String> fieldEvidence;
    private String errorMessage;

    public StructuredPlacementExtractionResult() {}

    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }

    public String getSourceEmail() { return sourceEmail; }
    public void setSourceEmail(String sourceEmail) { this.sourceEmail = sourceEmail; }

    public ClassificationResultDto getClassification() { return classification; }
    public void setClassification(ClassificationResultDto classification) { this.classification = classification; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getDriveTitle() { return driveTitle; }
    public void setDriveTitle(String driveTitle) { this.driveTitle = driveTitle; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Instant getApplicationDeadline() { return applicationDeadline; }
    public void setApplicationDeadline(Instant applicationDeadline) { this.applicationDeadline = applicationDeadline; }

    public List<ImportantDateDto> getImportantDates() { return importantDates; }
    public void setImportantDates(List<ImportantDateDto> importantDates) { this.importantDates = importantDates; }

    public StructuredEligibilityDto getCommonEligibility() { return commonEligibility; }
    public void setCommonEligibility(StructuredEligibilityDto commonEligibility) { this.commonEligibility = commonEligibility; }

    public List<ExtractedRoleDto> getRoles() { return roles; }
    public void setRoles(List<ExtractedRoleDto> roles) { this.roles = roles; }

    public Map<String, String> getFieldEvidence() { return fieldEvidence; }
    public void setFieldEvidence(Map<String, String> fieldEvidence) { this.fieldEvidence = fieldEvidence; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
