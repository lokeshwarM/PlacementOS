package com.placementos.backend.controller;

import com.placementos.backend.domain.dto.ReminderResponse;
import com.placementos.backend.domain.entity.ReminderTask;
import com.placementos.backend.domain.exception.ResourceNotFoundException;
import com.placementos.backend.domain.repository.ReminderTaskRepository;
import com.placementos.backend.domain.service.ReminderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API for Reminder tasks.
 */
@RestController
@RequestMapping("/api/v1/reminders")
public class ReminderController {

    private final ReminderService reminderService;
    public ReminderController(ReminderService reminderService) {
        this.reminderService = reminderService;
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<ReminderResponse>> getRemindersByStudent(@PathVariable Long studentId) {
        List<ReminderResponse> responses = reminderService.findByStudentId(studentId)
                .stream()
                .map(ReminderResponse::from)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<ReminderResponse> completeReminder(@PathVariable Long id) {
        ReminderTask task = reminderService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reminder not found with id: " + id));
        
        ReminderTask completed = reminderService.markCompleted(task.getStudent().getId(), task.getPlacementDrive().getId());
        return ResponseEntity.ok(ReminderResponse.from(completed));
    }
}
