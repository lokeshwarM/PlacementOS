package com.placementos.backend.domain.service;

import com.placementos.backend.domain.entity.Application;
import com.placementos.backend.domain.entity.PlacementDrive;
import com.placementos.backend.domain.entity.PlacementRole;
import com.placementos.backend.domain.entity.Student;
import com.placementos.backend.domain.enums.ApplicationStatus;
import com.placementos.backend.domain.exception.DuplicateResourceException;
import com.placementos.backend.domain.exception.ResourceNotFoundException;
import com.placementos.backend.domain.repository.ApplicationRepository;
import com.placementos.backend.domain.repository.PlacementDriveRepository;
import com.placementos.backend.domain.repository.PlacementRoleRepository;
import com.placementos.backend.domain.repository.StudentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Business service for student application state operations.
 */
@Service
@Transactional(readOnly = true)
public class ApplicationService {

    private static final Logger log = LoggerFactory.getLogger(ApplicationService.class);

    private final ApplicationRepository applicationRepository;
    private final StudentRepository studentRepository;
    private final PlacementDriveRepository placementDriveRepository;
    private final PlacementRoleRepository placementRoleRepository;
    private final ReminderService reminderService;

    public ApplicationService(ApplicationRepository applicationRepository,
                               StudentRepository studentRepository,
                               PlacementDriveRepository placementDriveRepository,
                               PlacementRoleRepository placementRoleRepository,
                               ReminderService reminderService) {
        this.applicationRepository = applicationRepository;
        this.studentRepository = studentRepository;
        this.placementDriveRepository = placementDriveRepository;
        this.placementRoleRepository = placementRoleRepository;
        this.reminderService = reminderService;
    }

    // -------------------------------------------------------------------------
    // Writes
    // -------------------------------------------------------------------------

    /**
     * Creates an application record for a student and placement drive.
     */
    @Transactional
    public Application createApplication(Long studentId, Long placementDriveId) {
        return createApplication(studentId, placementDriveId, null);
    }

    /**
     * Creates a role-aware application record for a student and placement drive.
     */
    @Transactional
    public Application createApplication(Long studentId, Long placementDriveId, Long placementRoleId) {
        if (applicationRepository.existsByStudentIdAndPlacementDriveId(studentId, placementDriveId)) {
            throw DuplicateResourceException.application(studentId, placementDriveId);
        }

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> ResourceNotFoundException.student(studentId));

        PlacementDrive drive = placementDriveRepository.findById(placementDriveId)
                .orElseThrow(() -> ResourceNotFoundException.placementDrive(placementDriveId));

        Application application = new Application();
        application.setStudent(student);
        application.setPlacementDrive(drive);
        application.setStatus(ApplicationStatus.NOT_STARTED);

        if (placementRoleId != null) {
            PlacementRole role = placementRoleRepository.findById(placementRoleId)
                    .orElseThrow(() -> new ResourceNotFoundException("PlacementRole not found: " + placementRoleId));
            application.setPlacementRole(role);
        }

        return applicationRepository.save(application);
    }

    /**
     * Explicit student action to mark an application as APPLIED.
     * Enforces ownership, records applied timestamp, and stops all active reminders.
     */
    @Transactional
    public Application apply(Long authenticatedStudentId, Long applicationId) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> ResourceNotFoundException.application(applicationId));

        if (!application.getStudent().getId().equals(authenticatedStudentId)) {
            log.warn("Security violation: Student id={} attempted to apply for application id={} owned by student id={}",
                    authenticatedStudentId, applicationId, application.getStudent().getId());
            throw new AccessDeniedException("Access denied: You cannot submit applications for another student");
        }

        application.setStatus(ApplicationStatus.APPLIED);
        application.setAppliedAt(Instant.now());
        Application saved = applicationRepository.save(application);

        // Stop all active reminders for this student & drive
        reminderService.stopRemindersForStudentAndDrive(
                authenticatedStudentId,
                application.getPlacementDrive().getId(),
                "STUDENT_APPLIED"
        );

        log.info("Student id={} explicitly marked application id={} as APPLIED. Stopped active reminders.",
                authenticatedStudentId, applicationId);

        return saved;
    }

    /**
     * Updates the status of an existing application.
     */
    @Transactional
    public Application updateStatus(Long applicationId, ApplicationStatus newStatus) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> ResourceNotFoundException.application(applicationId));
        application.setStatus(newStatus);
        return applicationRepository.save(application);
    }

    // -------------------------------------------------------------------------
    // Reads
    // -------------------------------------------------------------------------

    public Optional<Application> findById(Long id) {
        return applicationRepository.findById(id);
    }

    public Optional<Application> findByStudentAndDrive(Long studentId, Long placementDriveId) {
        return applicationRepository.findByStudentIdAndPlacementDriveId(studentId, placementDriveId);
    }

    public List<Application> findByStudentId(Long studentId) {
        return applicationRepository.findByStudentId(studentId);
    }

    public List<Application> findByPlacementDriveId(Long placementDriveId) {
        return applicationRepository.findByPlacementDriveId(placementDriveId);
    }
}
