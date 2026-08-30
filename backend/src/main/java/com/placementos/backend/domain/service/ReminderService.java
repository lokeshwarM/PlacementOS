package com.placementos.backend.domain.service;

import com.placementos.backend.domain.entity.PlacementDrive;
import com.placementos.backend.domain.entity.ReminderTask;
import com.placementos.backend.domain.entity.Student;
import com.placementos.backend.domain.enums.ReminderStatus;
import com.placementos.backend.domain.exception.DuplicateResourceException;
import com.placementos.backend.domain.exception.ResourceNotFoundException;
import com.placementos.backend.domain.repository.PlacementDriveRepository;
import com.placementos.backend.domain.repository.ReminderTaskRepository;
import com.placementos.backend.domain.repository.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Business service establishing the reminder task domain boundary.
 *
 * Manages reminder task records only.
 * No scheduling, no job execution, and no Redis integration is implemented here.
 * The scheduler milestone will use this service to query and mark reminders.
 *
 * The database UNIQUE(student_id, placement_drive_id) ensures only one reminder
 * task exists per student per drive. This service checks the constraint at the
 * business layer for a clean error message.
 */
@Service
@Transactional(readOnly = true)
public class ReminderService {

    private final ReminderTaskRepository reminderTaskRepository;
    private final StudentRepository studentRepository;
    private final PlacementDriveRepository placementDriveRepository;

    public ReminderService(ReminderTaskRepository reminderTaskRepository,
                           StudentRepository studentRepository,
                           PlacementDriveRepository placementDriveRepository) {
        this.reminderTaskRepository = reminderTaskRepository;
        this.studentRepository = studentRepository;
        this.placementDriveRepository = placementDriveRepository;
    }

    // -------------------------------------------------------------------------
    // Writes
    // -------------------------------------------------------------------------

    @Transactional
    public ReminderTask createReminderTask(Long studentId, Long placementDriveId, Instant scheduledFor) {
        if (reminderTaskRepository.existsByStudentIdAndPlacementDriveId(studentId, placementDriveId)) {
            throw DuplicateResourceException.reminderTask(studentId, placementDriveId);
        }

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> ResourceNotFoundException.student(studentId));

        PlacementDrive drive = placementDriveRepository.findById(placementDriveId)
                .orElseThrow(() -> ResourceNotFoundException.placementDrive(placementDriveId));

        ReminderTask task = new ReminderTask();
        task.setStudent(student);
        task.setPlacementDrive(drive);
        task.setScheduledFor(scheduledFor);
        task.setStatus(ReminderStatus.PENDING);

        return reminderTaskRepository.save(task);
    }

    /**
     * Marks a reminder task as completed. Called by the future scheduler after dispatch.
     */
    @Transactional
    public ReminderTask markCompleted(Long studentId, Long placementDriveId) {
        ReminderTask task = reminderTaskRepository
                .findByStudentIdAndPlacementDriveId(studentId, placementDriveId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Reminder task not found for student " + studentId
                        + " and drive " + placementDriveId));

        task.setStatus(ReminderStatus.COMPLETED);
        task.setCompletedAt(Instant.now());
        return reminderTaskRepository.save(task);
    }

    // -------------------------------------------------------------------------
    // Reads
    // -------------------------------------------------------------------------

    public List<ReminderTask> findPending() {
        return reminderTaskRepository.findByStatus(ReminderStatus.PENDING);
    }

    public Optional<ReminderTask> findByStudentAndDrive(Long studentId, Long placementDriveId) {
        return reminderTaskRepository.findByStudentIdAndPlacementDriveId(studentId, placementDriveId);
    }
}
