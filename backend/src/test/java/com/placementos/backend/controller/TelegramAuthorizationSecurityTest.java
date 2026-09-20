package com.placementos.backend.controller;

import tools.jackson.databind.ObjectMapper;
import com.placementos.backend.config.TelegramProperties;
import com.placementos.backend.domain.entity.Application;
import com.placementos.backend.domain.entity.PlacementDrive;
import com.placementos.backend.domain.entity.Student;
import com.placementos.backend.domain.entity.TelegramIdentity;
import com.placementos.backend.domain.repository.ApplicationRepository;
import com.placementos.backend.domain.repository.ReminderTaskRepository;
import com.placementos.backend.domain.repository.TelegramIdentityRepository;
import com.placementos.backend.domain.repository.TelegramWebhookUpdateRepository;
import com.placementos.backend.domain.service.ApplicationService;
import com.placementos.backend.domain.service.ReminderService;
import com.placementos.backend.domain.service.TelegramLinkingService;
import com.placementos.backend.domain.service.provider.TelegramNotificationProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TelegramAuthorizationSecurityTest {

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

    private TelegramProperties properties;
    private ObjectMapper objectMapper;
    private TelegramWebhookController controller;

    private Student studentAlice;
    private Student studentBob;

    @BeforeEach
    void setUp() {
        properties = new TelegramProperties();
        properties.setWebhookSecret("correct-secret-token");
        objectMapper = new ObjectMapper();

        controller = new TelegramWebhookController(
                properties,
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

        studentAlice = new Student();
        studentAlice.setId(101L);
        studentAlice.setName("Alice");

        studentBob = new Student();
        studentBob.setId(102L);
        studentBob.setName("Bob");
    }

    @Test
    @DisplayName("Webhook rejects requests without valid X-Telegram-Bot-Api-Secret-Token (403 Forbidden)")
    void webhook_rejectsInvalidSecret() {
        ResponseEntity<String> response = controller.handleWebhook("wrong-token", "{\"update_id\": 1}");
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

        ResponseEntity<String> nullResponse = controller.handleWebhook(null, "{\"update_id\": 1}");
        assertEquals(HttpStatus.FORBIDDEN, nullResponse.getStatusCode());

        verifyNoInteractions(webhookUpdateRepository);
    }

    @Test
    @DisplayName("A student cannot use Telegram callback to mark another student's application as APPLIED")
    void callbackQuery_enforcesOwnershipViaApplicationService() {
        // Alice sends a callback query trying to apply for Bob's application id=202
        String payload = """
                {
                    "update_id": 9999,
                    "callback_query": {
                        "id": "cb-malicious-1",
                        "data": "DONE:202",
                        "message": {
                            "chat": { "id": 111222333 }
                        }
                    }
                }
                """;

        when(webhookUpdateRepository.existsByUpdateId(9999L)).thenReturn(false);

        // Chat 111222333 is linked to Alice
        TelegramIdentity aliceIdentity = new TelegramIdentity(studentAlice, 111222333L, 555L, "alice");
        when(identityRepository.findByTelegramChatId(111222333L)).thenReturn(Optional.of(aliceIdentity));

        // ApplicationService.apply(101, 202) will throw AccessDeniedException because application 202 belongs to Bob (102)
        doThrow(new AccessDeniedException("Access denied: You cannot submit applications for another student"))
                .when(applicationService).apply(101L, 202L);

        ResponseEntity<String> response = controller.handleWebhook("correct-secret-token", payload);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        // Verify answerCallbackQuery receives failure message
        verify(notificationProvider).answerCallbackQuery(eq("cb-malicious-1"), contains("Action failed"));
    }

    @Test
    @DisplayName("Telegram linking rejects claiming a chat ID already linked to another student")
    void linking_rejectsClaimOfExistingChat() {
        String payload = """
                {
                    "update_id": 9998,
                    "message": {
                        "chat": { "id": 111222333 },
                        "from": { "id": 555, "username": "attacker" },
                        "text": "/start forged_token"
                    }
                }
                """;

        when(webhookUpdateRepository.existsByUpdateId(9998L)).thenReturn(false);
        doThrow(new IllegalStateException("This Telegram account is already linked to another student."))
                .when(linkingService).verifyAndLink("forged_token", 111222333L, 555L, "attacker");

        ResponseEntity<String> response = controller.handleWebhook("correct-secret-token", payload);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(notificationProvider).sendTelegramMessage(
                eq("111222333"),
                contains("This Telegram account is already linked to another student"),
                eq("link-fail:111222333")
        );
    }
}
