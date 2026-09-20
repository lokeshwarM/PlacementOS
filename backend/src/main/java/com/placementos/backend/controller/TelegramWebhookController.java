package com.placementos.backend.controller;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.placementos.backend.config.TelegramProperties;
import com.placementos.backend.domain.entity.*;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * Controller receiving inbound updates from the Telegram Bot API webhook.
 *
 * Security:
 * - Validates the {@code X-Telegram-Bot-Api-Secret-Token} header against configured secret.
 * - Idempotently processes updates via {@link TelegramWebhookUpdateRepository}.
 * - Delegates all business operations to domain services (ApplicationService, ReminderService, TelegramLinkingService).
 */
@RestController
@RequestMapping("/api/internal/telegram")
public class TelegramWebhookController {

    private static final Logger log = LoggerFactory.getLogger(TelegramWebhookController.class);

    private final TelegramProperties telegramProperties;
    private final TelegramWebhookUpdateRepository webhookUpdateRepository;
    private final TelegramLinkingService linkingService;
    private final TelegramIdentityRepository identityRepository;
    private final TelegramNotificationProvider notificationProvider;
    private final ApplicationService applicationService;
    private final ReminderService reminderService;
    private final ReminderTaskRepository reminderTaskRepository;
    private final ApplicationRepository applicationRepository;
    private final ObjectMapper objectMapper;

    public TelegramWebhookController(TelegramProperties telegramProperties,
                                     TelegramWebhookUpdateRepository webhookUpdateRepository,
                                     TelegramLinkingService linkingService,
                                     TelegramIdentityRepository identityRepository,
                                     TelegramNotificationProvider notificationProvider,
                                     ApplicationService applicationService,
                                     ReminderService reminderService,
                                     ReminderTaskRepository reminderTaskRepository,
                                     ApplicationRepository applicationRepository,
                                     ObjectMapper objectMapper) {
        this.telegramProperties = telegramProperties;
        this.webhookUpdateRepository = webhookUpdateRepository;
        this.linkingService = linkingService;
        this.identityRepository = identityRepository;
        this.notificationProvider = notificationProvider;
        this.applicationService = applicationService;
        this.reminderService = reminderService;
        this.reminderTaskRepository = reminderTaskRepository;
        this.applicationRepository = applicationRepository;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(
            @RequestHeader(value = "X-Telegram-Bot-Api-Secret-Token", required = false) String secretTokenHeader,
            @RequestBody String rawPayload) {

        // 1. Verify webhook secret if configured
        String configuredSecret = telegramProperties.getWebhookSecret();
        if (configuredSecret != null && !configuredSecret.isBlank()) {
            if (secretTokenHeader == null || !secretTokenHeader.equals(configuredSecret)) {
                log.warn("Unauthorized Telegram webhook request: secret token mismatch or missing");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Unauthorized: Invalid secret token");
            }
        }

        try {
            JsonNode root = objectMapper.readTree(rawPayload);
            long updateId = root.path("update_id").asLong(0);

            if (updateId == 0) {
                log.warn("Invalid Telegram update: missing update_id");
                return ResponseEntity.badRequest().body("Missing update_id");
            }

            // 2. Webhook Idempotency Check
            if (webhookUpdateRepository.existsByUpdateId(updateId)) {
                log.info("Telegram update id={} already processed (duplicate delivery). Skipping.", updateId);
                return ResponseEntity.ok("ALREADY_PROCESSED");
            }

            webhookUpdateRepository.save(new TelegramWebhookUpdate(updateId));

            // 3. Process Update Type
            if (root.has("message")) {
                handleMessageUpdate(root.path("message"));
            } else if (root.has("callback_query")) {
                handleCallbackQueryUpdate(root.path("callback_query"));
            }

            return ResponseEntity.ok("OK");

        } catch (Exception e) {
            log.error("Error processing Telegram webhook update: {}", e.getMessage(), e);
            // Return 200 to prevent Telegram from retrying malformed updates forever
            return ResponseEntity.ok("ERROR_HANDLED");
        }
    }

    private void handleMessageUpdate(JsonNode messageNode) {
        long chatId = messageNode.path("chat").path("id").asLong();
        long userId = messageNode.path("from").path("id").asLong();
        String username = messageNode.path("from").path("username").asText(null);
        String text = messageNode.path("text").asText("").trim();

        if (text.startsWith("/start")) {
            handleStartCommand(chatId, userId, username, text);
        } else if (text.equalsIgnoreCase("/done") || text.equalsIgnoreCase("done") || text.equalsIgnoreCase("applied")) {
            handleDoneCommand(chatId);
        }
    }

    private void handleStartCommand(long chatId, long userId, String username, String text) {
        String[] parts = text.split("\\s+");
        if (parts.length < 2 || parts[1].isBlank()) {
            notificationProvider.sendTelegramMessage(
                    String.valueOf(chatId),
                    "👋 *Welcome to PlacementOS Bot!*\n\nTo link your Telegram account, please click the **Connect Telegram** button in your PlacementOS student portal.",
                    "start-no-token:" + chatId
            );
            return;
        }

        String rawToken = parts[1].trim();
        try {
            Student student = linkingService.verifyAndLink(rawToken, chatId, userId, username);
            String welcomeMsg = String.format(
                    "✅ *PlacementOS Connected!*\n\nHi %s, your Telegram account has been linked to **%s** (%s).\n\nYou will now receive real-time placement alerts, eligibility updates, shortlists, and deadline reminders.",
                    student.getName(), student.getName(), student.getRegistrationNumber()
            );
            notificationProvider.sendTelegramMessage(String.valueOf(chatId), welcomeMsg, "linked:" + student.getId());
        } catch (Exception e) {
            log.warn("Telegram linking failed for chatId={}: {}", chatId, e.getMessage());
            String errorMsg = "❌ *Linking Failed*\n\n" + e.getMessage();
            notificationProvider.sendTelegramMessage(String.valueOf(chatId), errorMsg, "link-fail:" + chatId);
        }
    }

    private void handleDoneCommand(long chatId) {
        Optional<TelegramIdentity> identityOpt = identityRepository.findByTelegramChatId(chatId);
        if (identityOpt.isEmpty()) {
            notificationProvider.sendTelegramMessage(
                    String.valueOf(chatId),
                    "❌ *Account Not Linked*\n\nYour Telegram account is not linked to any PlacementOS student profile. Please log in to PlacementOS to connect your account.",
                    "not-linked:" + chatId
            );
            return;
        }

        Student student = identityOpt.get().getStudent();

        // Check for active reminders for this student
        List<ReminderTask> activeReminders = reminderTaskRepository
                .findByStudentIdAndPlacementDriveIdAndStatus(student.getId(), null, ReminderStatus.PENDING);
        if (activeReminders.isEmpty()) {
            activeReminders = reminderTaskRepository.findByStudentId(student.getId()).stream()
                    .filter(r -> r.getStatus() == ReminderStatus.PENDING)
                    .toList();
        }

        if (activeReminders.isEmpty()) {
            // Check for pending application
            List<Application> pendingApps = applicationRepository.findByStudentId(student.getId()).stream()
                    .filter(a -> a.getStatus() == ApplicationStatus.NOT_STARTED || a.getStatus() == ApplicationStatus.ELIGIBLE)
                    .toList();

            if (pendingApps.isEmpty()) {
                notificationProvider.sendTelegramMessage(
                        String.valueOf(chatId),
                        "ℹ️ *No Pending Reminders*\n\nYou have no active deadline reminders or pending applications right now.",
                        "no-pending:" + chatId
                );
                return;
            }

            // Apply for the first pending application
            Application app = pendingApps.get(0);
            applicationService.apply(student.getId(), app.getId());
            String confirmation = String.format(
                    "✅ Application for *%s* marked as **APPLIED**. Active reminders have been stopped.",
                    app.getPlacementDrive().getCompanyName()
            );
            notificationProvider.sendTelegramMessage(String.valueOf(chatId), confirmation, "done-app:" + app.getId());
            return;
        }

        // Apply for the first active reminder's placement drive
        ReminderTask task = activeReminders.get(0);
        PlacementDrive drive = task.getPlacementDrive();
        Optional<Application> appOpt = applicationRepository.findByStudentIdAndPlacementDriveId(student.getId(), drive.getId());

        Application app;
        if (appOpt.isPresent()) {
            app = appOpt.get();
        } else {
            app = applicationService.createApplication(student.getId(), drive.getId());
        }

        applicationService.apply(student.getId(), app.getId());
        String confirmation = String.format(
                "✅ Application for *%s* marked as **APPLIED**. Active reminders have been stopped.",
                drive.getCompanyName()
        );
        notificationProvider.sendTelegramMessage(String.valueOf(chatId), confirmation, "done-drive:" + drive.getId());
    }

    private void handleCallbackQueryUpdate(JsonNode callbackNode) {
        String callbackQueryId = callbackNode.path("id").asText();
        String data = callbackNode.path("data").asText("");
        long chatId = callbackNode.path("message").path("chat").path("id").asLong();

        if (data.startsWith("DONE:")) {
            try {
                Long applicationId = Long.parseLong(data.substring(5).trim());
                Optional<TelegramIdentity> identityOpt = identityRepository.findByTelegramChatId(chatId);
                if (identityOpt.isPresent()) {
                    Student student = identityOpt.get().getStudent();
                    applicationService.apply(student.getId(), applicationId);
                    notificationProvider.answerCallbackQuery(callbackQueryId, "Application marked as APPLIED!");
                    notificationProvider.sendTelegramMessage(
                            String.valueOf(chatId),
                            "✅ Application marked as **APPLIED**. Active reminders have been stopped.",
                            "cb-done:" + applicationId
                    );
                } else {
                    notificationProvider.answerCallbackQuery(callbackQueryId, "Account not linked");
                }
            } catch (Exception e) {
                log.warn("Failed to process callback query data={}: {}", data, e.getMessage());
                notificationProvider.answerCallbackQuery(callbackQueryId, "Action failed: " + e.getMessage());
            }
        }
    }
}
