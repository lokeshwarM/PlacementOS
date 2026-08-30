package com.placementos.backend.domain.service;

import com.placementos.backend.domain.entity.Application;
import com.placementos.backend.domain.entity.PlacementDrive;
import com.placementos.backend.domain.entity.Student;
import com.placementos.backend.domain.enums.ApplicationStatus;
import com.placementos.backend.domain.exception.DuplicateResourceException;
import com.placementos.backend.domain.exception.ResourceNotFoundException;
import com.placementos.backend.domain.repository.ApplicationRepository;
import com.placementos.backend.domain.repository.PlacementDriveRepository;
import com.placementos.backend.domain.repository.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Business service for student application state operations.
 *
 * The database enforces UNIQUE(student_id, placement_drive_id).
 * This service checks the constraint at the business layer for clear error reporting.
 *
 * A full eligibility-to-application state machine is NOT implemented here.
 * Status transitions are accepted if they produce a valid enum value.
 * Full state-machine validation will be added in a later milestone.
 *
 * Transaction strategy:
 *  - @Transactional on create and update because they modify the applications table.
 *  - Reads remain read-only.
 */
@Service
@Transactional(readOnly = true)
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final StudentRepository studentRepository;
    private final PlacementDriveRepository placementDriveRepository;

    public ApplicationService(ApplicationRepository applicationRepository,
                               StudentRepository studentRepository,
                               PlacementDriveRepository placementDriveRepository) {
        this.applicationRepository = applicationRepository;
        this.studentRepository = studentRepository;
        this.placementDriveRepository = placementDriveRepository;
    }

    // -------------------------------------------------------------------------
    // Writes
    // -------------------------------------------------------------------------

    /**
     * Creates an application record for a student and placement drive.
     * Rejects if an application already exists for this combination.
     */
    @Transactional
    public Application createApplication(Long studentId, Long placementDriveId) {
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

        return applicationRepository.save(application);
    }

    /**
     * Updates the status of an existing application.
     * Full state-machine validation is deferred to a later milestone.
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
