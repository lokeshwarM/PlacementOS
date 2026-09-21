package com.placementos.backend.domain.service;

import com.placementos.backend.domain.entity.*;
import com.placementos.backend.domain.enums.ApplicationStatus;
import com.placementos.backend.domain.enums.OutboxStatus;
import com.placementos.backend.domain.enums.ReminderStatus;
import com.placementos.backend.domain.exception.DuplicateResourceException;
import com.placementos.backend.domain.exception.ResourceNotFoundException;
import com.placementos.backend.domain.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Service managing reminder task lifecycle, scheduling, periodic execution,
 * and stop-on-done / stop-on-applied workflows.
 */
@Service
@Transactional(readOnly = true)
public class ReminderService {

    private static final Logger log = LoggerFactory.getLogger(ReminderService.class);

    private final ReminderTaskRepository reminderTaskRepository;
    private final StudentRepository studentRepository;
    private final PlacementDriveRepository placementDriveRepository;
    private final PlacementRoleRepository placementRoleRepository;
    private final ApplicationRepository applicationRepository;
    private final NotificationOutboxRepository outboxRepository;
    private final NotificationDecisionService notificationDecisionService;

    public ReminderService(ReminderTaskRepository reminderTaskRepository,
                           StudentRepository studentRepository,
                           PlacementDriveRepository placementDriveRepository,
                           PlacementRoleRepository placementRoleRepository,
                           ApplicationRepository applicationRepository,
                           NotificationOutboxRepository outboxRepository,
                           NotificationDecisionService notificationDecisionService) {
        this.reminderTaskRepository = reminderTaskRepository;
        this.studentRepository = studentRepository;
        this.placementDriveRepository = placementDriveRepository;
        this.placementRoleRepository = placementRoleRepository;
        this.applicationRepository = applicationRepository;
        this.outboxRepository = outboxRepository;
        this.notificationDecisionService = notificationDecisionService;
    }

    /**
     * Creates a new scheduled or recurring reminder task for a student and drive.
     */
    @Transactional
    public ReminderTask createRecurringReminder(Long studentId,
                                               Long placementDriveId,
                                               Long placementRoleId,
                                               Instant firstScheduledFor,
                                               int intervalMinutes,
                                               int maxReminders) {
        Optional<ReminderTask> existing = reminderTaskRepository
                .findByStudentIdAndPlacementDriveId(studentId, placementDriveId);

        if (existing.isPresent()) {
            ReminderTask task = existing.get();
            task.setScheduledFor(firstScheduledFor);
            task.setIntervalMinutes(intervalMinutes > 0 ? intervalMinutes : 60);
            task.setMaxReminders(maxReminders > 0 ? maxReminders : 5);
            task.setRemindersSent(0);
            task.setStatus(ReminderStatus.PENDING);
            task.setCancelReason(null);
            task.setCompletedAt(null);
            if (placementRoleId != null) {
                placementRoleRepository.findById(placementRoleId).ifPresent(task::setPlacementRole);
            }
            return reminderTaskRepository.save(task);
        }

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> ResourceNotFoundException.student(studentId));

        PlacementDrive drive = placementDriveRepository.findById(placementDriveId)
                .orElseThrow(() -> ResourceNotFoundException.placementDrive(placementDriveId));

        ReminderTask task = new ReminderTask();
        task.setStudent(student);
        task.setPlacementDrive(drive);
        task.setScheduledFor(firstScheduledFor != null ? firstScheduledFor : Instant.now());
        task.setIntervalMinutes(intervalMinutes > 0 ? intervalMinutes : 60);
        task.setMaxReminders(maxReminders > 0 ? maxReminders : 5);
        task.setRemindersSent(0);
        task.setStatus(ReminderStatus.PENDING);

        if (placementRoleId != null) {
            placementRoleRepository.findById(placementRoleId).ifPresent(task::setPlacementRole);
        }

        return reminderTaskRepository.save(task);
    }

    /**
     * Legacy create reminder task method.
     */
    @Transactional
    public ReminderTask createReminderTask(Long studentId, Long placementDriveId, Instant scheduledFor) {
        return createRecurringReminder(studentId, placementDriveId, null, scheduledFor, 60, 5);
    }

    /**
     * Processes due reminder tasks, evaluating stop conditions and triggering notifications.
     */
    @Transactional
    public int processDueReminders() {
        Instant now = Instant.now();
        List<ReminderTask> dueTasks = reminderTaskRepository.findDueReminders(ReminderStatus.PENDING, now);

        if (dueTasks.isEmpty()) {
            return 0;
        }

        int processedCount = 0;

        for (ReminderTask task : dueTasks) {
            Student student = task.getStudent();
            PlacementDrive drive = task.getPlacementDrive();

            // 1. Check if student already applied -> Stop immediately
            Optional<Application> appOpt = applicationRepository
                    .findByStudentIdAndPlacementDriveId(student.getId(), drive.getId());

            if (appOpt.isPresent()) {
                ApplicationStatus status = appOpt.get().getStatus();
                if (status == ApplicationStatus.APPLIED ||
                    status == ApplicationStatus.SHORTLISTED ||
                    status == ApplicationStatus.COMPLETED) {
                    task.setStatus(ReminderStatus.CANCELLED);
                    task.setCancelReason("STUDENT_ALREADY_APPLIED");
                    task.setCompletedAt(now);
                    reminderTaskRepository.save(task);
                    log.info("Reminder task id={} stopped: student {} already in status {}", task.getId(), student.getId(), status);
                    continue;
                }
            }

            // 2. Check if deadline has passed -> Stop
            if (drive.getApplicationDeadline() != null && now.isAfter(drive.getApplicationDeadline())) {
                task.setStatus(ReminderStatus.COMPLETED);
                task.setCancelReason("DEADLINE_PASSED");
                task.setCompletedAt(now);
                reminderTaskRepository.save(task);
                log.info("Reminder task id={} completed: drive {} deadline passed", task.getId(), drive.getId());
                continue;
            }

            // 3. Trigger reminder notification decision
            int nextOccurrence = task.getRemindersSent() + 1;
            notificationDecisionService.decideReminderNotification(task.getId(), nextOccurrence, task.getScheduledFor());

            task.setRemindersSent(nextOccurrence);
            task.setLastReminderAt(now);

            // 4. Advance schedule or complete
            if (task.getRemindersSent() >= task.getMaxReminders()) {
                task.setStatus(ReminderStatus.COMPLETED);
                task.setCompletedAt(now);
                log.info("Reminder task id={} completed all {} reminder cycles", task.getId(), task.getMaxReminders());
            } else {
                task.setScheduledFor(now.plus(Duration.ofMinutes(task.getIntervalMinutes())));
                log.info("Reminder task id={} scheduled next occurrence {} at {}",
                        task.getId(), nextOccurrence + 1, task.getScheduledFor());
            }

            reminderTaskRepository.save(task);
            processedCount++;
        }

        return processedCount;
    }

    /**
     * Deactivates all active reminder tasks and cancels pending outbox reminders
     * for a student and drive upon explicit apply or stop request.
     */
    @Transactional
    public void stopRemindersForStudentAndDrive(Long studentId, Long placementDriveId, String reason) {
        List<ReminderTask> activeTasks = reminderTaskRepository
                .findByStudentIdAndPlacementDriveIdAndStatus(studentId, placementDriveId, ReminderStatus.PENDING);

        Instant now = Instant.now();
        for (ReminderTask task : activeTasks) {
            task.setStatus(ReminderStatus.CANCELLED);
            task.setCancelReason(reason != null ? reason : "STUDENT_STOP_ACTION");
            task.setCompletedAt(now);
            reminderTaskRepository.save(task);
            log.info("Cancelled reminder task id={} for student {} drive {}: reason={}",
                    task.getId(), studentId, placementDriveId, task.getCancelReason());
        }

        // Cancel any pending outbox rows for this student and drive
        List<NotificationOutbox> pendingOutbox = outboxRepository
                .findPendingByStudentAndDrive(studentId, placementDriveId);

        for (NotificationOutbox outbox : pendingOutbox) {
            if (outbox.getNotification() != null &&
                outbox.getNotification().getNotificationType() == com.placementos.backend.domain.enums.NotificationType.REMINDER) {
                outbox.setStatus(OutboxStatus.CANCELLED);
                outbox.setProcessedAt(now);
                outboxRepository.save(outbox);
                log.info("Cancelled pending reminder outbox id={} for student {} drive {}",
                        outbox.getId(), studentId, placementDriveId);
            }
        }
    }

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

    public List<ReminderTask> findPending() {
        return reminderTaskRepository.findByStatus(ReminderStatus.PENDING);
    }

    public Optional<ReminderTask> findByStudentAndDrive(Long studentId, Long placementDriveId) {
        return reminderTaskRepository.findByStudentIdAndPlacementDriveId(studentId, placementDriveId);
    }

    public List<ReminderTask> findByStudentId(Long studentId) {
        return reminderTaskRepository.findByStudentId(studentId);
    }

    public Optional<ReminderTask> findById(Long id) {
        return reminderTaskRepository.findById(id);
    }

    /**
     * Cancels ALL active reminder tasks for a student across all drives.
     * Used during account deletion to stop all pending deliveries.
     */
    @Transactional
    public void cancelAllRemindersForStudent(Long studentId, String reason) {
        List<ReminderTask> tasks = reminderTaskRepository.findByStudentId(studentId);
        for (ReminderTask task : tasks) {
            if (task.getStatus() == ReminderStatus.PENDING) {
                task.setStatus(ReminderStatus.CANCELLED);
                task.setCancelReason(reason);
                task.setCompletedAt(Instant.now());
                reminderTaskRepository.save(task);
                log.info("Cancelled reminder task id={} for student id={}: reason={}", task.getId(), studentId, reason);
            }
        }
    }
}
