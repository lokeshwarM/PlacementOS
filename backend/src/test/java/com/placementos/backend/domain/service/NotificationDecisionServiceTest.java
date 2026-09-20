package com.placementos.backend.domain.service;

import com.placementos.backend.domain.entity.*;
import com.placementos.backend.domain.enums.*;
import com.placementos.backend.domain.repository.*;
import com.placementos.backend.domain.service.template.MessageTemplateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class NotificationDecisionServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationOutboxRepository outboxRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private PlacementDriveRepository placementDriveRepository;

    @Mock
    private PlacementRoleRepository placementRoleRepository;

    @Mock
    private StudentEligibilityResultRepository eligibilityResultRepository;

    @Mock
    private ShortlistEntryRepository shortlistEntryRepository;

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private ReminderTaskRepository reminderTaskRepository;

    @Mock
    private MessageTemplateService templateService;

    @Mock
    private TelegramIdentityRepository telegramIdentityRepository;

    private NotificationDecisionService decisionService;

    private Student student;
    private PlacementDrive drive;
    private PlacementRole role1;

    @BeforeEach
    void setUp() {
        decisionService = new NotificationDecisionService(
                notificationRepository,
                outboxRepository,
                studentRepository,
                placementDriveRepository,
                placementRoleRepository,
                eligibilityResultRepository,
                shortlistEntryRepository,
                applicationRepository,
                reminderTaskRepository,
                templateService,
                telegramIdentityRepository
        );

        student = new Student();
        student.setId(1L);
        student.setName("Alice Smith");
        student.setPhoneNumber("+919876543210");

        drive = new PlacementDrive();
        drive.setId(10L);
        drive.setCompanyName("Amazon");
        drive.setApplicationDeadline(Instant.now().plusSeconds(36000));

        role1 = new PlacementRole("SDE 1", 1);
        role1.setId(101L);
        role1.setPlacementDrive(drive);

        lenient().when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(inv -> {
                    Notification n = inv.getArgument(0);
                    n.setId(999L);
                    return n;
                });
    }

    @Test
    void decideEligibilityNotification_generatesAtomically() {
        when(notificationRepository.existsByIdempotencyKey("eligibility:student:1:drive:10")).thenReturn(false);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(placementDriveRepository.findById(10L)).thenReturn(Optional.of(drive));

        StudentEligibilityResult eligResult = new StudentEligibilityResult(
                student, drive, role1, EligibilityDecision.ELIGIBLE, List.of(), "v1"
        );
        when(eligibilityResultRepository.findByStudentIdAndPlacementDriveId(1L, 10L))
                .thenReturn(List.of(eligResult));
        when(templateService.renderEligibilityMessage(any(), any(), any()))
                .thenReturn("Eligible for Amazon SDE 1");

        when(telegramIdentityRepository.findByStudentId(1L))
                .thenReturn(Optional.of(new TelegramIdentity(student, 987654321L, 111L, "alice")));

        Optional<Notification> result = decisionService.decideEligibilityNotification(1L, 10L);

        assertTrue(result.isPresent());
        Notification notification = result.get();
        assertEquals("eligibility:student:1:drive:10", notification.getIdempotencyKey());
        assertEquals(NotificationType.ELIGIBILITY, notification.getNotificationType());
        assertEquals(NotificationChannel.TELEGRAM, notification.getChannel());
        assertEquals(NotificationStatus.PENDING, notification.getStatus());

        verify(notificationRepository).save(any(Notification.class));
        verify(outboxRepository).save(argThat(outbox ->
                outbox.getChannel() == NotificationChannel.TELEGRAM &&
                outbox.getRecipient().equals("987654321") &&
                outbox.getStatus() == OutboxStatus.PENDING
        ));
    }

    @Test
    void decideEligibilityNotification_unlinkedStudent_marksSkippedAndCancelled() {
        when(notificationRepository.existsByIdempotencyKey("eligibility:student:1:drive:10")).thenReturn(false);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(placementDriveRepository.findById(10L)).thenReturn(Optional.of(drive));

        StudentEligibilityResult eligResult = new StudentEligibilityResult(
                student, drive, role1, EligibilityDecision.ELIGIBLE, List.of(), "v1"
        );
        when(eligibilityResultRepository.findByStudentIdAndPlacementDriveId(1L, 10L))
                .thenReturn(List.of(eligResult));
        when(templateService.renderEligibilityMessage(any(), any(), any()))
                .thenReturn("Eligible for Amazon SDE 1");
        when(telegramIdentityRepository.findByStudentId(1L)).thenReturn(Optional.empty());

        Optional<Notification> result = decisionService.decideEligibilityNotification(1L, 10L);

        assertTrue(result.isPresent());
        Notification notification = result.get();
        assertEquals(NotificationChannel.TELEGRAM, notification.getChannel());
        assertEquals(NotificationStatus.SKIPPED, notification.getStatus());

        verify(outboxRepository).save(argThat(outbox ->
                outbox.getChannel() == NotificationChannel.TELEGRAM &&
                outbox.getStatus() == OutboxStatus.CANCELLED &&
                "STUDENT_TELEGRAM_NOT_LINKED".equals(outbox.getLastError())
        ));
    }

    @Test
    void decideEligibilityNotification_alreadyExists_skipsDuplicate() {
        Notification existing = new Notification();
        existing.setId(50L);
        existing.setIdempotencyKey("eligibility:student:1:drive:10");

        when(notificationRepository.existsByIdempotencyKey("eligibility:student:1:drive:10")).thenReturn(true);
        when(notificationRepository.findByIdempotencyKey("eligibility:student:1:drive:10")).thenReturn(Optional.of(existing));

        Optional<Notification> result = decisionService.decideEligibilityNotification(1L, 10L);

        assertTrue(result.isPresent());
        assertEquals(50L, result.get().getId());
        verify(notificationRepository, never()).save(any());
        verify(outboxRepository, never()).save(any());
    }

    @Test
    void decideDeadlineNotification_studentAlreadyApplied_suppressesNotification() {
        Application application = new Application();
        application.setStudent(student);
        application.setPlacementDrive(drive);
        application.setStatus(ApplicationStatus.APPLIED);

        when(notificationRepository.existsByIdempotencyKey("deadline:student:1:drive:10:hours:24")).thenReturn(false);
        when(applicationRepository.findByStudentIdAndPlacementDriveId(1L, 10L)).thenReturn(Optional.of(application));

        Optional<Notification> result = decisionService.decideDeadlineNotification(1L, 10L, 24);

        assertTrue(result.isEmpty());
        verify(outboxRepository, never()).save(any());
    }
}
