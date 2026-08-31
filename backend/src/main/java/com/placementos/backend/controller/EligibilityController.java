package com.placementos.backend.controller;

import com.placementos.backend.domain.dto.DriveEligibilityEvaluationResponse;
import com.placementos.backend.domain.dto.RoleEligibilityEvaluationResponse;
import com.placementos.backend.domain.service.EligibilityService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for student eligibility evaluation and retrieval.
 */
@RestController
@RequestMapping("/api/v1/eligibility")
public class EligibilityController {

    private final EligibilityService eligibilityService;

    public EligibilityController(EligibilityService eligibilityService) {
        this.eligibilityService = eligibilityService;
    }

    /**
     * Evaluates and idempotently persists eligibility for a student across all roles in a placement drive.
     */
    @PostMapping("/students/{studentId}/drives/{driveId}")
    public ResponseEntity<DriveEligibilityEvaluationResponse> evaluateForDrive(
            @PathVariable Long studentId,
            @PathVariable Long driveId) {
        DriveEligibilityEvaluationResponse response = eligibilityService.evaluateAndPersistForDrive(studentId, driveId);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves persisted eligibility evaluation results for a student and drive.
     */
    @GetMapping("/students/{studentId}/drives/{driveId}")
    public ResponseEntity<DriveEligibilityEvaluationResponse> getPersistedForDrive(
            @PathVariable Long studentId,
            @PathVariable Long driveId) {
        DriveEligibilityEvaluationResponse response = eligibilityService.getPersistedResultsForDrive(studentId, driveId);
        return ResponseEntity.ok(response);
    }

    /**
     * Evaluates and idempotently persists eligibility for a student on a specific role.
     */
    @PostMapping("/students/{studentId}/roles/{roleId}")
    public ResponseEntity<RoleEligibilityEvaluationResponse> evaluateForRole(
            @PathVariable Long studentId,
            @PathVariable Long roleId) {
        RoleEligibilityEvaluationResponse response = eligibilityService.evaluateAndPersistForRole(studentId, roleId);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves persisted eligibility evaluation result for a student and role.
     */
    @GetMapping("/students/{studentId}/roles/{roleId}")
    public ResponseEntity<RoleEligibilityEvaluationResponse> getPersistedForRole(
            @PathVariable Long studentId,
            @PathVariable Long roleId) {
        RoleEligibilityEvaluationResponse response = eligibilityService.getPersistedResultForRole(studentId, roleId);
        return ResponseEntity.ok(response);
    }
}
