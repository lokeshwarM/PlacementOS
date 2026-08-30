package com.placementos.backend.controller;

import com.placementos.backend.domain.dto.NotificationResponse;
import com.placementos.backend.domain.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API for internal/admin-facing notification state querying.
 * Does not implement message dispatch.
 */
@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<NotificationResponse>> getNotificationsByStudent(@PathVariable Long studentId) {
        List<NotificationResponse> responses = notificationService.findByStudentId(studentId)
                .stream()
                .map(NotificationResponse::from)
                .toList();
        return ResponseEntity.ok(responses);
    }
}
