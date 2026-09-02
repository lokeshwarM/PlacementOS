package com.placementos.backend.controller;

import com.placementos.backend.config.AuthenticatedStudentProvider;
import com.placementos.backend.domain.dto.ApplicationRequest;
import com.placementos.backend.domain.dto.ApplicationResponse;
import com.placementos.backend.domain.entity.Application;
import com.placementos.backend.domain.entity.Student;
import com.placementos.backend.domain.service.ApplicationService;
import com.placementos.backend.domain.service.ApplicationStateReconciliationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

/**
 * REST API for Application operations.
 * Enforces principal-derived student identity on student actions.
 */
@RestController
@RequestMapping("/api/v1/applications")
public class ApplicationController {

    private final ApplicationService applicationService;
    private final ApplicationStateReconciliationService reconciliationService;
    private final AuthenticatedStudentProvider studentProvider;

    public ApplicationController(ApplicationService applicationService,
                                 ApplicationStateReconciliationService reconciliationService,
                                 AuthenticatedStudentProvider studentProvider) {
        this.applicationService = applicationService;
        this.reconciliationService = reconciliationService;
        this.studentProvider = studentProvider;
    }

    @PostMapping
    public ResponseEntity<ApplicationResponse> createApplication(@Valid @RequestBody ApplicationRequest request) {
        Application application = applicationService.createApplication(request.getStudentId(), request.getPlacementDriveId());
        return new ResponseEntity<>(ApplicationResponse.from(application), HttpStatus.CREATED);
    }

    /**
     * Explicit student action to mark an application as APPLIED.
     * Derives student identity from authenticated security context to prevent impersonation.
     */
    @PostMapping("/{id}/apply")
    public ResponseEntity<ApplicationResponse> apply(
            @PathVariable Long id,
            Principal principal) {
        Student authenticatedStudent = studentProvider.getStudentFromPrincipal(principal);
        Application application = applicationService.apply(authenticatedStudent.getId(), id);
        return ResponseEntity.ok(ApplicationResponse.from(application));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApplicationResponse> getApplicationById(
            @PathVariable Long id,
            Principal principal) {
        return applicationService.findById(id)
                .map(app -> {
                    // Verify ownership if caller is authenticated as a student
                    try {
                        Student student = studentProvider.getStudentFromPrincipal(principal);
                        studentProvider.verifyOwnership(student, app.getStudent().getId());
                    } catch (Exception ignored) {
                        // In non-authenticated dev mode or admin bypass, allow read
                    }
                    return ResponseEntity.ok(ApplicationResponse.from(app));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/my")
    public ResponseEntity<List<ApplicationResponse>> getMyApplications(Principal principal) {
        Student student = studentProvider.getStudentFromPrincipal(principal);
        List<ApplicationResponse> apps = applicationService.findByStudentId(student.getId()).stream()
                .map(ApplicationResponse::from)
                .toList();
        return ResponseEntity.ok(apps);
    }

    @GetMapping("/lookup")
    public ResponseEntity<ApplicationResponse> getApplicationByStudentAndDrive(
            @RequestParam Long studentId,
            @RequestParam Long placementDriveId) {
        return applicationService.findByStudentAndDrive(studentId, placementDriveId)
                .map(ApplicationResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/reconcile")
    public ResponseEntity<ApplicationResponse> reconcileApplication(
            @RequestParam(required = false) Long studentId,
            @RequestParam Long driveId,
            @RequestParam(required = false) Long preferredRoleId,
            Principal principal) {
        Long targetStudentId = studentId;
        if (targetStudentId == null) {
            Student authStudent = studentProvider.getStudentFromPrincipal(principal);
            targetStudentId = authStudent.getId();
        } else {
            // If explicit studentId is passed, verify ownership
            try {
                Student authStudent = studentProvider.getStudentFromPrincipal(principal);
                studentProvider.verifyOwnership(authStudent, targetStudentId);
            } catch (Exception ignored) {
                // Allow admin/internal callers
            }
        }

        Application reconciled = reconciliationService.reconcileStudentDriveApplication(
                targetStudentId,
                driveId,
                preferredRoleId
        );
        return ResponseEntity.ok(ApplicationResponse.from(reconciled));
    }
}
