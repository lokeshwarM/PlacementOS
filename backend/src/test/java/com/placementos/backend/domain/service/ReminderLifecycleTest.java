package com.placementos.backend.domain.service;

import com.placementos.backend.domain.entity.*;
import com.placementos.backend.domain.enums.ApplicationStatus;
import com.placementos.backend.domain.enums.OutboxStatus;
import com.placementos.backend.domain.enums.ReminderStatus;
import com.placementos.backend.domain.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReminderLifecycleTest {

    @Mock
    private ReminderTaskRepository reminderTaskRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private PlacementDriveRepository placementDriveRepository;

    @Mock
    private PlacementRoleRepository placementRoleRepository;

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private NotificationOutboxRepository outboxRepository;

    @Mock
    private NotificationDecisionService notificationDecisionService;

    private ReminderService reminderService;

    private Student student;
    private PlacementDrive drive;
    private ReminderTask reminderTask;

    @BeforeEach
    void setUp() {
        reminderService = new ReminderService(
                reminderTaskRepository,
                studentRepository,
                placementDriveRepository,
                placementRoleRepository,
                applicationRepository,
                outboxRepository,
                notificationDecisionService
        );

        student = new Student();
        student.setId(1L);

        drive = new PlacementDrive();
        drive.setId(10L);
        drive.setApplicationDeadline(Instant.now().plusSeconds(7200));

        reminderTask = new ReminderTask();
        reminderTask.setId(50L);
        reminderTask.setStudent(student);
        reminderTask.setPlacementDrive(drive);
        reminderTask.setStatus(ReminderStatus.PENDING);
        reminderTask.setScheduledFor(Instant.now().minusSeconds(10));
        reminderTask.setRemindersSent(0);
        reminderTask.setMaxReminders(3);
        reminderTask.setIntervalMinutes(60);
    }

    @Test
    void processDueReminders_advancesSchedule() {
        when(reminderTaskRepository.findDueReminders(eq(ReminderStatus.PENDING), any(Instant.class)))
                .thenReturn(List.of(reminderTask));
        when(applicationRepository.findByStudentIdAndPlacementDriveId(1L, 10L))
                .thenReturn(Optional.of(new Application()));

        int processed = reminderService.processDueReminders();

        assertEquals(1, processed);
        assertEquals(1, reminderTask.getRemindersSent());
        assertEquals(ReminderStatus.PENDING, reminderTask.getStatus());
        assertNotNull(reminderTask.getLastReminderAt());

        verify(notificationDecisionService).decideReminderNotification(eq(50L), eq(1), any());
        verify(reminderTaskRepository).save(reminderTask);
    }

    @Test
    void processDueReminders_studentAlreadyApplied_stopsReminder() {
        Application appliedApp = new Application();
        appliedApp.setStatus(ApplicationStatus.APPLIED);

        when(reminderTaskRepository.findDueReminders(eq(ReminderStatus.PENDING), any(Instant.class)))
                .thenReturn(List.of(reminderTask));
        when(applicationRepository.findByStudentIdAndPlacementDriveId(1L, 10L))
                .thenReturn(Optional.of(appliedApp));

        int processed = reminderService.processDueReminders();

        assertEquals(0, processed);
        assertEquals(ReminderStatus.CANCELLED, reminderTask.getStatus());
        assertEquals("STUDENT_ALREADY_APPLIED", reminderTask.getCancelReason());
        verify(notificationDecisionService, never()).decideReminderNotification(any(), anyInt(), any());
    }

    @Test
    void stopRemindersForStudentAndDrive_cancelsActiveTasksAndPendingOutbox() {
        when(reminderTaskRepository.findByStudentIdAndPlacementDriveIdAndStatus(1L, 10L, ReminderStatus.PENDING))
                .thenReturn(List.of(reminderTask));

        Notification notification = new Notification();
        notification.setNotificationType(com.placementos.backend.domain.enums.NotificationType.REMINDER);

        NotificationOutbox outbox = new NotificationOutbox();
        outbox.setNotification(notification);
        outbox.setStatus(OutboxStatus.PENDING);

        when(outboxRepository.findPendingByStudentAndDrive(1L, 10L))
                .thenReturn(List.of(outbox));

        reminderService.stopRemindersForStudentAndDrive(1L, 10L, "STUDENT_APPLIED");

        assertEquals(ReminderStatus.CANCELLED, reminderTask.getStatus());
        assertEquals("STUDENT_APPLIED", reminderTask.getCancelReason());
        assertEquals(OutboxStatus.CANCELLED, outbox.getStatus());

        verify(reminderTaskRepository).save(reminderTask);
        verify(outboxRepository).save(outbox);
    }
}
