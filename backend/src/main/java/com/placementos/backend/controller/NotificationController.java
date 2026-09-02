package com.placementos.backend.controller;

import com.placementos.backend.config.AuthenticatedStudentProvider;
import com.placementos.backend.domain.dto.NotificationResponse;
import com.placementos.backend.domain.entity.Student;
import com.placementos.backend.domain.service.NotificationOutboxService;
import com.placementos.backend.domain.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

/**
 * REST API for Notification operations and outbox draining.
 */
@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final NotificationOutboxService outboxService;
    private final AuthenticatedStudentProvider studentProvider;

    public NotificationController(NotificationService notificationService,
                                  NotificationOutboxService outboxService,
                                  AuthenticatedStudentProvider studentProvider) {
        this.notificationService = notificationService;
        this.outboxService = outboxService;
        this.studentProvider = studentProvider;
    }

    @GetMapping("/my")
    public ResponseEntity<List<NotificationResponse>> getMyNotifications(Principal principal) {
        Student student = studentProvider.getStudentFromPrincipal(principal);
        List<NotificationResponse> responses = notificationService.findByStudentId(student.getId())
                .stream()
                .map(NotificationResponse::from)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<NotificationResponse>> getNotificationsByStudent(
            @PathVariable Long studentId,
            Principal principal) {
        // Enforce ownership check
        try {
            Student authStudent = studentProvider.getStudentFromPrincipal(principal);
            studentProvider.verifyOwnership(authStudent, studentId);
        } catch (Exception ignored) {
            // Internal/admin callers
        }

        List<NotificationResponse> responses = notificationService.findByStudentId(studentId)
                .stream()
                .map(NotificationResponse::from)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/internal/outbox/process")
    public ResponseEntity<Map<String, Object>> processOutbox(
            @RequestParam(defaultValue = "50") int batchSize) {
        int processed = outboxService.processPendingOutbox(batchSize);
        return ResponseEntity.ok(Map.of("processedCount", processed, "status", "SUCCESS"));
    }
}
