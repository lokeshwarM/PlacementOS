package com.placementos.backend.controller;

import tools.jackson.databind.ObjectMapper;
import com.placementos.backend.config.TelegramProperties;
import com.placementos.backend.domain.entity.Application;
import com.placementos.backend.domain.entity.PlacementDrive;
import com.placementos.backend.domain.entity.ReminderTask;
import com.placementos.backend.domain.entity.Student;
import com.placementos.backend.domain.entity.TelegramIdentity;
import com.placementos.backend.domain.enums.ApplicationStatus;
import com.placementos.backend.domain.enums.ReminderStatus;
import com.placementos.backend.domain.repository.ApplicationRepository;
import com.placementos.backend.domain.repository.ReminderTaskRepository;
import com.placementos.backend.domain.repository.TelegramIdentityRepository;
import com.placementos.backend.domain.repository.TelegramWebhookUpdateRepository;
import com.placementos.backend.domain.service.ApplicationService;
import com.placementos.backend.domain.service.ReminderService;
import com.placementos.backend.domain.service.TelegramLinkingService;
import com.placementos.backend.domain.service.provider.TelegramNotificationProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TelegramWebhookControllerTest {

    @Mock
    private TelegramWebhookUpdateRepository webhookUpdateRepository;

    @Mock
    private TelegramLinkingService linkingService;

    @Mock
    private TelegramIdentityRepository identityRepository;

    @Mock
    private TelegramNotificationProvider notificationProvider;

    @Mock
    private ApplicationService applicationService;

    @Mock
    private ReminderService reminderService;

    @Mock
    private ReminderTaskRepository reminderTaskRepository;

    @Mock
    private ApplicationRepository applicationRepository;

    private TelegramProperties telegramProperties;
    private ObjectMapper objectMapper;
    private TelegramWebhookController controller;

    private Student student;
    private PlacementDrive drive;

    @BeforeEach
    void setUp() {
        telegramProperties = new TelegramProperties();
        telegramProperties.setWebhookSecret("my-secret-webhook-token");
        objectMapper = new ObjectMapper();

        controller = new TelegramWebhookController(
                telegramProperties,
                webhookUpdateRepository,
                linkingService,
                identityRepository,
                notificationProvider,
                applicationService,
                reminderService,
                reminderTaskRepository,
                applicationRepository,
                objectMapper
        );

        student = new Student();
        student.setId(1L);
        student.setName("Alice");
        student.setRegistrationNumber("RA2111003010001");

        drive = new PlacementDrive();
        drive.setId(10L);
        drive.setCompanyName("Google");
    }

    @Test
    void handleWebhook_secretTokenMismatch_returns403() {
        String payload = "{\"update_id\": 100}";
        ResponseEntity<String> response = controller.handleWebhook("wrong-token", payload);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verifyNoInteractions(webhookUpdateRepository);
    }

    @Test
    void handleWebhook_missingUpdateId_returnsBadRequest() {
        String payload = "{\"message\": {}}";
        ResponseEntity<String> response = controller.handleWebhook("my-secret-webhook-token", payload);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void handleWebhook_duplicateUpdateId_returnsAlreadyProcessed() {
        String payload = "{\"update_id\": 1001}";
        when(webhookUpdateRepository.existsByUpdateId(1001L)).thenReturn(true);

        ResponseEntity<String> response = controller.handleWebhook("my-secret-webhook-token", payload);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("ALREADY_PROCESSED", response.getBody());
        verify(webhookUpdateRepository, never()).save(any());
        verifyNoInteractions(linkingService);
    }

    @Test
    void handleWebhook_startCommand_linksStudentSuccessfully() {
        String payload = """
                {
                    "update_id": 1002,
                    "message": {
                        "chat": { "id": 123456789 },
                        "from": { "id": 98765, "username": "alice_tg" },
                        "text": "/start abcdef123456"
                    }
                }
                """;

        when(webhookUpdateRepository.existsByUpdateId(1002L)).thenReturn(false);
        when(linkingService.verifyAndLink("abcdef123456", 123456789L, 98765L, "alice_tg"))
                .thenReturn(student);

        ResponseEntity<String> response = controller.handleWebhook("my-secret-webhook-token", payload);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("OK", response.getBody());
        verify(webhookUpdateRepository).save(any());
        verify(notificationProvider).sendTelegramMessage(
                eq("123456789"),
                contains("PlacementOS Connected"),
                eq("linked:1")
        );
    }

    @Test
    void handleWebhook_doneCommand_appliesToPendingDriveAndStopsReminders() {
        String payload = """
                {
                    "update_id": 1003,
                    "message": {
                        "chat": { "id": 123456789 },
                        "from": { "id": 98765, "username": "alice_tg" },
                        "text": "/done"
                    }
                }
                """;

        when(webhookUpdateRepository.existsByUpdateId(1003L)).thenReturn(false);

        TelegramIdentity identity = new TelegramIdentity(student, 123456789L, 98765L, "alice_tg");
        when(identityRepository.findByTelegramChatId(123456789L)).thenReturn(Optional.of(identity));

        ReminderTask reminder = new ReminderTask();
        reminder.setId(55L);
        reminder.setStudent(student);
        reminder.setPlacementDrive(drive);
        reminder.setStatus(ReminderStatus.PENDING);

        when(reminderTaskRepository.findByStudentIdAndPlacementDriveIdAndStatus(1L, null, ReminderStatus.PENDING))
                .thenReturn(List.of(reminder));

        Application app = new Application();
        app.setId(77L);
        app.setStudent(student);
        app.setPlacementDrive(drive);
        app.setStatus(ApplicationStatus.NOT_STARTED);

        when(applicationRepository.findByStudentIdAndPlacementDriveId(1L, 10L)).thenReturn(Optional.of(app));

        ResponseEntity<String> response = controller.handleWebhook("my-secret-webhook-token", payload);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(applicationService).apply(1L, 77L);
        verify(notificationProvider).sendTelegramMessage(
                eq("123456789"),
                contains("marked as **APPLIED**"),
                eq("done-drive:10")
        );
    }

    @Test
    void handleWebhook_callbackQueryDone_appliesAndAnswersCallback() {
        String payload = """
                {
                    "update_id": 1004,
                    "callback_query": {
                        "id": "cb-query-999",
                        "data": "DONE:77",
                        "message": {
                            "chat": { "id": 123456789 }
                        }
                    }
                }
                """;

        when(webhookUpdateRepository.existsByUpdateId(1004L)).thenReturn(false);

        TelegramIdentity identity = new TelegramIdentity(student, 123456789L, 98765L, "alice_tg");
        when(identityRepository.findByTelegramChatId(123456789L)).thenReturn(Optional.of(identity));

        ResponseEntity<String> response = controller.handleWebhook("my-secret-webhook-token", payload);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(applicationService).apply(1L, 77L);
        verify(notificationProvider).answerCallbackQuery("cb-query-999", "Application marked as APPLIED!");
        verify(notificationProvider).sendTelegramMessage(
                eq("123456789"),
                contains("marked as **APPLIED**"),
                eq("cb-done:77")
        );
    }
}
