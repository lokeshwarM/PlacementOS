package com.placementos.backend.domain.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Composite response DTO containing a student's evaluation results across all roles in a drive.
 */
public class DriveEligibilityEvaluationResponse {

    private Long studentId;
    private Long placementDriveId;
    private String companyName;
    private String driveTitle;
    private List<RoleEligibilityEvaluationResponse> roleResults = new ArrayList<>();

    public DriveEligibilityEvaluationResponse() {}

    public DriveEligibilityEvaluationResponse(Long studentId, Long placementDriveId, String companyName, String driveTitle) {
        this.studentId = studentId;
        this.placementDriveId = placementDriveId;
        this.companyName = companyName;
        this.driveTitle = driveTitle;
    }

    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }

    public Long getPlacementDriveId() { return placementDriveId; }
    public void setPlacementDriveId(Long placementDriveId) { this.placementDriveId = placementDriveId; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getDriveTitle() { return driveTitle; }
    public void setDriveTitle(String driveTitle) { this.driveTitle = driveTitle; }

    public List<RoleEligibilityEvaluationResponse> getRoleResults() { return roleResults; }
    public void setRoleResults(List<RoleEligibilityEvaluationResponse> roleResults) { this.roleResults = roleResults; }
}
