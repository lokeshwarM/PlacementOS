package com.placementos.backend.domain.service;

import com.placementos.backend.domain.entity.*;
import com.placementos.backend.domain.enums.ApplicationStatus;
import com.placementos.backend.domain.enums.EligibilityDecision;
import com.placementos.backend.domain.enums.ShortlistMatchStatus;
import com.placementos.backend.domain.exception.ResourceNotFoundException;
import com.placementos.backend.domain.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Service responsible for deterministic application state reconciliation combining:
 * 1. Student eligibility evaluation results
 * 2. Shortlist match outcomes
 * 3. Current application lifecycle state
 *
 * Enforces key rules:
 * - Never auto-marks APPLIED (requires explicit student action).
 * - Never regresses authoritative forward states (APPLIED, SHORTLISTED, COMPLETED, REJECTED).
 * - Reconciles ELIGIBLE / NOT_ELIGIBLE / SHORTLISTED idempotently.
 */
@Service
@Transactional(readOnly = true)
public class ApplicationStateReconciliationService {

    private static final Logger log = LoggerFactory.getLogger(ApplicationStateReconciliationService.class);

    private final ApplicationRepository applicationRepository;
    private final StudentRepository studentRepository;
    private final PlacementDriveRepository placementDriveRepository;
    private final PlacementRoleRepository placementRoleRepository;
    private final StudentEligibilityResultRepository eligibilityResultRepository;
    private final ShortlistEntryRepository shortlistEntryRepository;

    public ApplicationStateReconciliationService(ApplicationRepository applicationRepository,
                                                StudentRepository studentRepository,
                                                PlacementDriveRepository placementDriveRepository,
                                                PlacementRoleRepository placementRoleRepository,
                                                StudentEligibilityResultRepository eligibilityResultRepository,
                                                ShortlistEntryRepository shortlistEntryRepository) {
        this.applicationRepository = applicationRepository;
        this.studentRepository = studentRepository;
        this.placementDriveRepository = placementDriveRepository;
        this.placementRoleRepository = placementRoleRepository;
        this.eligibilityResultRepository = eligibilityResultRepository;
        this.shortlistEntryRepository = shortlistEntryRepository;
    }

    /**
     * Reconciles application state for a student and placement drive.
     */
    @Transactional
    public Application reconcileStudentDriveApplication(Long studentId, Long driveId, Long preferredRoleId) {
        Objects.requireNonNull(studentId, "studentId must not be null");
        Objects.requireNonNull(driveId, "driveId must not be null");

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> ResourceNotFoundException.student(studentId));

        PlacementDrive drive = placementDriveRepository.findById(driveId)
                .orElseThrow(() -> ResourceNotFoundException.placementDrive(driveId));

        Application application = applicationRepository.findByStudentIdAndPlacementDriveId(studentId, driveId)
                .orElseGet(() -> {
                    Application newApp = new Application();
                    newApp.setStudent(student);
                    newApp.setPlacementDrive(drive);
                    newApp.setStatus(ApplicationStatus.NOT_STARTED);
                    return newApp;
                });

        // Resolve preferred or specific role if provided
        if (preferredRoleId != null) {
            placementRoleRepository.findById(preferredRoleId).ifPresent(application::setPlacementRole);
        }

        ApplicationStatus currentStatus = application.getStatus();

        // 1. Check if student is shortlisted via deterministic shortlist matching
        List<ShortlistEntry> matchedShortlists = shortlistEntryRepository
                .findByPlacementDriveIdAndMatchStatus(driveId, ShortlistMatchStatus.MATCHED);

        boolean isShortlisted = matchedShortlists.stream()
                .anyMatch(entry -> entry.getStudent() != null && entry.getStudent().getId().equals(studentId));

        if (isShortlisted) {
            // Shortlist match is authoritative forward transition
            if (currentStatus != ApplicationStatus.COMPLETED && currentStatus != ApplicationStatus.REJECTED) {
                application.setStatus(ApplicationStatus.SHORTLISTED);
            }
            return applicationRepository.save(application);
        }

        // 2. If student has already applied, completed, or been rejected, do NOT regress
        if (currentStatus == ApplicationStatus.APPLIED ||
            currentStatus == ApplicationStatus.SHORTLISTED ||
            currentStatus == ApplicationStatus.COMPLETED ||
            currentStatus == ApplicationStatus.REJECTED) {
            log.info("Application id={} for student {} drive {} is already in forward state {}. Preserving state.",
                    application.getId(), studentId, driveId, currentStatus);
            return applicationRepository.save(application);
        }

        // 3. Evaluate eligibility results across drive roles
        List<StudentEligibilityResult> eligibilityResults = eligibilityResultRepository
                .findByStudentIdAndPlacementDriveId(studentId, driveId);

        if (!eligibilityResults.isEmpty()) {
            boolean hasEligibleRole = eligibilityResults.stream()
                    .anyMatch(r -> r.getDecision() == EligibilityDecision.ELIGIBLE);

            boolean allNotEligible = eligibilityResults.stream()
                    .allMatch(r -> r.getDecision() == EligibilityDecision.NOT_ELIGIBLE);

            if (hasEligibleRole) {
                application.setStatus(ApplicationStatus.ELIGIBLE);
                // If application has no role set yet, link to first eligible role
                if (application.getPlacementRole() == null) {
                    eligibilityResults.stream()
                            .filter(r -> r.getDecision() == EligibilityDecision.ELIGIBLE)
                            .findFirst()
                            .ifPresent(r -> application.setPlacementRole(r.getPlacementRole()));
                }
            } else if (allNotEligible) {
                application.setStatus(ApplicationStatus.NOT_ELIGIBLE);
            } else {
                // Some are REVIEW_REQUIRED without any confirmed ELIGIBLE
                application.setStatus(ApplicationStatus.NOT_STARTED);
            }
        }

        return applicationRepository.save(application);
    }
}
