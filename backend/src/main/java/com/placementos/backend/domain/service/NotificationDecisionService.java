package com.placementos.backend.domain.service;

import com.placementos.backend.domain.entity.*;
import com.placementos.backend.domain.enums.ApplicationStatus;
import com.placementos.backend.domain.enums.EligibilityDecision;
import com.placementos.backend.domain.enums.NotificationChannel;
import com.placementos.backend.domain.enums.NotificationStatus;
import com.placementos.backend.domain.enums.NotificationType;
import com.placementos.backend.domain.enums.OutboxStatus;
import com.placementos.backend.domain.exception.ResourceNotFoundException;
import com.placementos.backend.domain.repository.*;
import com.placementos.backend.domain.service.template.MessageTemplateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Service responsible for deterministic notification decisions.
 * Evaluates business rules and writes Notification and NotificationOutbox
 * records atomically within the same database transaction.
 *
 * Enforces:
 * - Deterministic idempotency keys preventing duplicate notification intent.
 * - No external network calls (persists to transactional outbox only).
 * - Suppression of deadline/reminder notifications if student already applied.
 */
@Service
@Transactional(readOnly = true)
public class NotificationDecisionService {

    private static final Logger log = LoggerFactory.getLogger(NotificationDecisionService.class);

    private final NotificationRepository notificationRepository;
    private final NotificationOutboxRepository outboxRepository;
    private final StudentRepository studentRepository;
    private final PlacementDriveRepository placementDriveRepository;
    private final PlacementRoleRepository placementRoleRepository;
    private final StudentEligibilityResultRepository eligibilityResultRepository;
    private final ShortlistEntryRepository shortlistEntryRepository;
    private final ApplicationRepository applicationRepository;
    private final ReminderTaskRepository reminderTaskRepository;
    private final MessageTemplateService templateService;
    private final TelegramIdentityRepository telegramIdentityRepository;

    public NotificationDecisionService(NotificationRepository notificationRepository,
                                     NotificationOutboxRepository outboxRepository,
                                     StudentRepository studentRepository,
                                     PlacementDriveRepository placementDriveRepository,
                                     PlacementRoleRepository placementRoleRepository,
                                     StudentEligibilityResultRepository eligibilityResultRepository,
                                     ShortlistEntryRepository shortlistEntryRepository,
                                     ApplicationRepository applicationRepository,
                                     ReminderTaskRepository reminderTaskRepository,
                                     MessageTemplateService templateService,
                                     TelegramIdentityRepository telegramIdentityRepository) {
        this.notificationRepository = notificationRepository;
        this.outboxRepository = outboxRepository;
        this.studentRepository = studentRepository;
        this.placementDriveRepository = placementDriveRepository;
        this.placementRoleRepository = placementRoleRepository;
        this.eligibilityResultRepository = eligibilityResultRepository;
        this.shortlistEntryRepository = shortlistEntryRepository;
        this.applicationRepository = applicationRepository;
        this.reminderTaskRepository = reminderTaskRepository;
        this.templateService = templateService;
        this.telegramIdentityRepository = telegramIdentityRepository;
    }

    /**
     * Decides and generates an ELIGIBILITY notification if the student has confirmed eligible roles.
     */
    @Transactional
    public Optional<Notification> decideEligibilityNotification(Long studentId, Long driveId) {
        String idempotencyKey = String.format("eligibility:student:%d:drive:%d", studentId, driveId);

        if (notificationRepository.existsByIdempotencyKey(idempotencyKey)) {
            log.info("Eligibility notification already decided for key={}", idempotencyKey);
            return notificationRepository.findByIdempotencyKey(idempotencyKey);
        }

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> ResourceNotFoundException.student(studentId));
        PlacementDrive drive = placementDriveRepository.findById(driveId)
                .orElseThrow(() -> ResourceNotFoundException.placementDrive(driveId));

        List<StudentEligibilityResult> eligibleResults = eligibilityResultRepository
                .findByStudentIdAndPlacementDriveId(studentId, driveId).stream()
                .filter(r -> r.getDecision() == EligibilityDecision.ELIGIBLE)
                .toList();

        if (eligibleResults.isEmpty()) {
            log.info("Student {} has no ELIGIBLE roles for drive {}. Skipping eligibility notification.", studentId, driveId);
            return Optional.empty();
        }

        List<PlacementRole> eligibleRoles = eligibleResults.stream()
                .map(StudentEligibilityResult::getPlacementRole)
                .filter(Objects::nonNull)
                .toList();

        String messageText = templateService.renderEligibilityMessage(student, drive, eligibleRoles);
        PlacementRole primaryRole = eligibleRoles.isEmpty() ? null : eligibleRoles.get(0);

        Notification notification = createAndPersistNotification(
                idempotencyKey,
                student,
                drive,
                primaryRole,
                NotificationType.ELIGIBILITY,
                NotificationChannel.TELEGRAM,
                messageText
        );

        return Optional.of(notification);
    }

    /**
     * Decides and generates a SHORTLIST notification for a confirmed shortlist match.
     */
    @Transactional
    public Optional<Notification> decideShortlistNotification(Long studentId, Long driveId, Long shortlistEntryId) {
        String idempotencyKey = String.format("shortlist:student:%d:drive:%d:entry:%d", studentId, driveId, shortlistEntryId);

        if (notificationRepository.existsByIdempotencyKey(idempotencyKey)) {
            log.info("Shortlist notification already decided for key={}", idempotencyKey);
            return notificationRepository.findByIdempotencyKey(idempotencyKey);
        }

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> ResourceNotFoundException.student(studentId));
        PlacementDrive drive = placementDriveRepository.findById(driveId)
                .orElseThrow(() -> ResourceNotFoundException.placementDrive(driveId));

        ShortlistEntry entry = shortlistEntryRepository.findById(shortlistEntryId)
                .orElseThrow(() -> new ResourceNotFoundException("ShortlistEntry not found: " + shortlistEntryId));

        PlacementRole role = entry.getPlacementRole();
        String messageText = templateService.renderShortlistMessage(student, drive, role, entry);

        Notification notification = createAndPersistNotification(
                idempotencyKey,
                student,
                drive,
                role,
                NotificationType.SHORTLIST,
                NotificationChannel.TELEGRAM,
                messageText
        );

        return Optional.of(notification);
    }

    /**
     * Decides and generates a DEADLINE notification if student has not applied yet.
     */
    @Transactional
    public Optional<Notification> decideDeadlineNotification(Long studentId, Long driveId, int hoursBeforeDeadline) {
        String idempotencyKey = String.format("deadline:student:%d:drive:%d:hours:%d", studentId, driveId, hoursBeforeDeadline);

        if (notificationRepository.existsByIdempotencyKey(idempotencyKey)) {
            log.info("Deadline notification already decided for key={}", idempotencyKey);
            return notificationRepository.findByIdempotencyKey(idempotencyKey);
        }

        // Suppress deadline notification if student already applied or is shortlisted/completed
        Optional<Application> appOpt = applicationRepository.findByStudentIdAndPlacementDriveId(studentId, driveId);
        if (appOpt.isPresent()) {
            ApplicationStatus status = appOpt.get().getStatus();
            if (status == ApplicationStatus.APPLIED ||
                status == ApplicationStatus.SHORTLISTED ||
                status == ApplicationStatus.COMPLETED ||
                status == ApplicationStatus.NOT_ELIGIBLE) {
                log.info("Application for student {} drive {} is in non-actionable state {}. Skipping deadline notification.",
                        studentId, driveId, status);
                return Optional.empty();
            }
        }

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> ResourceNotFoundException.student(studentId));
        PlacementDrive drive = placementDriveRepository.findById(driveId)
                .orElseThrow(() -> ResourceNotFoundException.placementDrive(driveId));

        PlacementRole role = appOpt.flatMap(a -> Optional.ofNullable(a.getPlacementRole())).orElse(null);
        String messageText = templateService.renderDeadlineMessage(student, drive, role, drive.getApplicationDeadline());

        Notification notification = createAndPersistNotification(
                idempotencyKey,
                student,
                drive,
                role,
                NotificationType.DEADLINE,
                NotificationChannel.TELEGRAM,
                messageText
        );

        return Optional.of(notification);
    }

    /**
     * Decides and generates a periodic REMINDER notification.
     */
    @Transactional
    public Optional<Notification> decideReminderNotification(Long reminderTaskId, int occurrenceIndex, Instant scheduledSlot) {
        String idempotencyKey = String.format("reminder:task:%d:slot:%d", reminderTaskId, occurrenceIndex);

        if (notificationRepository.existsByIdempotencyKey(idempotencyKey)) {
            log.info("Reminder notification already decided for key={}", idempotencyKey);
            return notificationRepository.findByIdempotencyKey(idempotencyKey);
        }

        ReminderTask task = reminderTaskRepository.findById(reminderTaskId)
                .orElseThrow(() -> new ResourceNotFoundException("ReminderTask not found: " + reminderTaskId));

        Student student = task.getStudent();
        PlacementDrive drive = task.getPlacementDrive();

        // Suppress reminder if student already applied or drive deadline passed
        Optional<Application> appOpt = applicationRepository.findByStudentIdAndPlacementDriveId(student.getId(), drive.getId());
        if (appOpt.isPresent()) {
            ApplicationStatus status = appOpt.get().getStatus();
            if (status == ApplicationStatus.APPLIED ||
                status == ApplicationStatus.SHORTLISTED ||
                status == ApplicationStatus.COMPLETED) {
                log.info("Student {} already applied (status={}). Suppressing reminder task {}.", student.getId(), status, reminderTaskId);
                return Optional.empty();
            }
        }

        if (drive.getApplicationDeadline() != null && Instant.now().isAfter(drive.getApplicationDeadline())) {
            log.info("Drive {} deadline has passed. Suppressing reminder task {}.", drive.getId(), reminderTaskId);
            return Optional.empty();
        }

        PlacementRole role = task.getPlacementRole();
        String messageText = templateService.renderReminderMessage(student, drive, role, drive.getApplicationDeadline(), occurrenceIndex);

        Notification notification = createAndPersistNotification(
                idempotencyKey,
                student,
                drive,
                role,
                NotificationType.REMINDER,
                NotificationChannel.TELEGRAM,
                messageText
        );

        return Optional.of(notification);
    }

    private Notification createAndPersistNotification(String idempotencyKey,
                                                     Student student,
                                                     PlacementDrive drive,
                                                     PlacementRole role,
                                                     NotificationType type,
                                                     NotificationChannel channel,
                                                     String messagePayload) {
        Notification notification = new Notification();
        notification.setIdempotencyKey(idempotencyKey);
        notification.setStudent(student);
        notification.setPlacementDrive(drive);
        notification.setPlacementRole(role);
        notification.setNotificationType(type);
        notification.setChannel(channel);
        notification.setMessagePayload(messagePayload);

        Optional<TelegramIdentity> telegramIdentityOpt = Optional.empty();
        if (channel == NotificationChannel.TELEGRAM) {
            telegramIdentityOpt = telegramIdentityRepository.findByStudentId(student.getId());
        }

        boolean isTelegramLinked = telegramIdentityOpt.isPresent();

        if (channel == NotificationChannel.TELEGRAM && !isTelegramLinked) {
            // Student has not linked Telegram yet
            // Mark notification as SKIPPED to avoid endless retry loops
            notification.setStatus(NotificationStatus.SKIPPED);
        } else {
            notification.setStatus(NotificationStatus.PENDING);
        }

        Notification savedNotification = notificationRepository.save(notification);

        // Transactional Outbox write
        NotificationOutbox outbox = new NotificationOutbox();
        outbox.setNotification(savedNotification);
        outbox.setIdempotencyKey(idempotencyKey);
        outbox.setChannel(channel);

        if (channel == NotificationChannel.TELEGRAM) {
            if (isTelegramLinked) {
                outbox.setRecipient(String.valueOf(telegramIdentityOpt.get().getTelegramChatId()));
                outbox.setStatus(OutboxStatus.PENDING);
            } else {
                outbox.setRecipient("UNLINKED");
                outbox.setStatus(OutboxStatus.CANCELLED);
                outbox.setLastError("STUDENT_TELEGRAM_NOT_LINKED");
                outbox.setProcessedAt(Instant.now());
                log.info("Student {} has not linked Telegram. Notification {} marked SKIPPED, Outbox CANCELLED.",
                        student.getId(), savedNotification.getId());
            }
        } else if (channel == NotificationChannel.WHATSAPP) {
            String recipient = student.getPhoneNumber() != null ? student.getPhoneNumber() : student.getRegistrationNumber();
            outbox.setRecipient(recipient != null ? recipient : "unknown");
            outbox.setStatus(OutboxStatus.PENDING);
        } else {
            outbox.setRecipient(student.getRegistrationNumber() != null ? student.getRegistrationNumber() : "unknown");
            outbox.setStatus(OutboxStatus.PENDING);
        }

        outbox.setPayload(messagePayload);
        outbox.setAttemptCount(0);
        outbox.setMaxAttempts(3);
        outbox.setAvailableAt(Instant.now());

        outboxRepository.save(outbox);

        log.info("Atomically created Notification id={} (status={}) and Outbox record (status={}) for key={}",
                savedNotification.getId(), savedNotification.getStatus(), outbox.getStatus(), idempotencyKey);
        return savedNotification;
    }
}
