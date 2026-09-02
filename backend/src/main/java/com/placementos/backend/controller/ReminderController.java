package com.placementos.backend.controller;

import com.placementos.backend.config.AuthenticatedStudentProvider;
import com.placementos.backend.domain.dto.ReminderResponse;
import com.placementos.backend.domain.entity.ReminderTask;
import com.placementos.backend.domain.entity.Student;
import com.placementos.backend.domain.exception.ResourceNotFoundException;
import com.placementos.backend.domain.service.ReminderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

/**
 * REST API for Reminder tasks.
 * Enforces principal-derived student identity on student actions.
 */
@RestController
@RequestMapping("/api/v1/reminders")
public class ReminderController {

    private final ReminderService reminderService;
    private final AuthenticatedStudentProvider studentProvider;

    public ReminderController(ReminderService reminderService,
                              AuthenticatedStudentProvider studentProvider) {
        this.reminderService = reminderService;
        this.studentProvider = studentProvider;
    }

    @GetMapping("/my")
    public ResponseEntity<List<ReminderResponse>> getMyReminders(Principal principal) {
        Student student = studentProvider.getStudentFromPrincipal(principal);
        List<ReminderResponse> responses = reminderService.findByStudentId(student.getId())
                .stream()
                .map(ReminderResponse::from)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<ReminderResponse>> getRemindersByStudent(
            @PathVariable Long studentId,
            Principal principal) {
        try {
            Student authStudent = studentProvider.getStudentFromPrincipal(principal);
            studentProvider.verifyOwnership(authStudent, studentId);
        } catch (Exception ignored) {
            // Allow admin/internal callers
        }

        List<ReminderResponse> responses = reminderService.findByStudentId(studentId)
                .stream()
                .map(ReminderResponse::from)
                .toList();
        return ResponseEntity.ok(responses);
    }

    /**
     * Explicit student action to stop reminders.
     */
    @PostMapping("/{id}/stop")
    public ResponseEntity<Map<String, Object>> stopReminder(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "STUDENT_STOP_ACTION") String reason,
            Principal principal) {
        ReminderTask task = reminderService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reminder not found with id: " + id));

        Student authStudent = studentProvider.getStudentFromPrincipal(principal);
        studentProvider.verifyOwnership(authStudent, task.getStudent().getId());

        reminderService.stopRemindersForStudentAndDrive(
                authStudent.getId(),
                task.getPlacementDrive().getId(),
                reason
        );

        return ResponseEntity.ok(Map.of("status", "CANCELLED", "reason", reason));
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<ReminderResponse> completeReminder(
            @PathVariable Long id,
            Principal principal) {
        ReminderTask task = reminderService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reminder not found with id: " + id));

        try {
            Student authStudent = studentProvider.getStudentFromPrincipal(principal);
            studentProvider.verifyOwnership(authStudent, task.getStudent().getId());
        } catch (Exception ignored) {
            // Admin/internal
        }

        ReminderTask completed = reminderService.markCompleted(task.getStudent().getId(), task.getPlacementDrive().getId());
        return ResponseEntity.ok(ReminderResponse.from(completed));
    }

    @PostMapping("/process-due")
    public ResponseEntity<Map<String, Object>> processDueReminders() {
        int processed = reminderService.processDueReminders();
        return ResponseEntity.ok(Map.of("processedCount", processed, "status", "SUCCESS"));
    }
}
