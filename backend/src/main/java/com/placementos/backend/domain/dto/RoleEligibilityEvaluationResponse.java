package com.placementos.backend.domain.dto;

import com.placementos.backend.domain.enums.EligibilityDecision;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Response DTO representing the evaluation result of a student against a specific role.
 */
public class RoleEligibilityEvaluationResponse {

    private Long studentId;
    private Long placementDriveId;
    private Long placementRoleId;
    private String roleTitle;
    private EligibilityDecision decision;
    private List<CriterionEvaluationResult> criteriaResults = new ArrayList<>();
    private String evaluatorVersion;
    private Instant evaluatedAt;

    public RoleEligibilityEvaluationResponse() {}

    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }

    public Long getPlacementDriveId() { return placementDriveId; }
    public void setPlacementDriveId(Long placementDriveId) { this.placementDriveId = placementDriveId; }

    public Long getPlacementRoleId() { return placementRoleId; }
    public void setPlacementRoleId(Long placementRoleId) { this.placementRoleId = placementRoleId; }

    public String getRoleTitle() { return roleTitle; }
    public void setRoleTitle(String roleTitle) { this.roleTitle = roleTitle; }

    public EligibilityDecision getDecision() { return decision; }
    public void setDecision(EligibilityDecision decision) { this.decision = decision; }

    public List<CriterionEvaluationResult> getCriteriaResults() { return criteriaResults; }
    public void setCriteriaResults(List<CriterionEvaluationResult> criteriaResults) { this.criteriaResults = criteriaResults; }

    public String getEvaluatorVersion() { return evaluatorVersion; }
    public void setEvaluatorVersion(String evaluatorVersion) { this.evaluatorVersion = evaluatorVersion; }

    public Instant getEvaluatedAt() { return evaluatedAt; }
    public void setEvaluatedAt(Instant evaluatedAt) { this.evaluatedAt = evaluatedAt; }
}
