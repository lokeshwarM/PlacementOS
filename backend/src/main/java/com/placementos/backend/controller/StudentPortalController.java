package com.placementos.backend.controller;

import com.placementos.backend.config.AuthenticatedStudentProvider;
import com.placementos.backend.domain.dto.student.*;
import com.placementos.backend.domain.entity.Application;
import com.placementos.backend.domain.entity.ReminderTask;
import com.placementos.backend.domain.entity.Student;
import com.placementos.backend.domain.entity.UserAccount;
import com.placementos.backend.domain.enums.NotificationType;
import com.placementos.backend.domain.exception.ResourceNotFoundException;
import com.placementos.backend.domain.service.ApplicationService;
import com.placementos.backend.domain.service.ReminderService;
import com.placementos.backend.domain.service.StudentPortalService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

/**
 * REST API for the Student Portal.
 * Strictly derives student identity from the authenticated principal.
 */
@RestController
@RequestMapping("/api/v1/student")
public class StudentPortalController {

    private final StudentPortalService studentPortalService;
    private final ApplicationService applicationService;
    private final ReminderService reminderService;
    private final AuthenticatedStudentProvider studentProvider;
    private final com.placementos.backend.domain.service.TelegramLinkingService telegramLinkingService;

    public StudentPortalController(StudentPortalService studentPortalService,
                                   ApplicationService applicationService,
                                   ReminderService reminderService,
                                   AuthenticatedStudentProvider studentProvider,
                                   com.placementos.backend.domain.service.TelegramLinkingService telegramLinkingService) {
        this.studentPortalService = studentPortalService;
        this.applicationService = applicationService;
        this.reminderService = reminderService;
        this.studentProvider = studentProvider;
        this.telegramLinkingService = telegramLinkingService;
    }

    // -------------------------------------------------------------------------
    // Profile & Onboarding
    // -------------------------------------------------------------------------

    @GetMapping("/profile")
    public ResponseEntity<StudentProfileResponse> getProfile(Principal principal) {
        UserAccount user = studentProvider.getUserAccountFromPrincipal(principal);
        StudentProfileResponse profile = studentPortalService.getStudentProfile(user.getId());
        return ResponseEntity.ok(profile);
    }

    @PostMapping("/onboarding")
    public ResponseEntity<StudentProfileResponse> onboard(
            @Valid @RequestBody StudentOnboardingRequest request,
            Principal principal) {
        UserAccount user = studentProvider.getUserAccountFromPrincipal(principal);
        StudentProfileResponse profile = studentPortalService.onboardStudent(user.getId(), request);
        return ResponseEntity.ok(profile);
    }

    @PutMapping("/profile")
    public ResponseEntity<StudentProfileResponse> updateProfile(
            @RequestBody StudentProfileUpdateRequest request,
            Principal principal) {
        UserAccount user = studentProvider.getUserAccountFromPrincipal(principal);
        StudentProfileResponse profile = studentPortalService.updateStudentProfile(user.getId(), request);
        return ResponseEntity.ok(profile);
    }

    // -------------------------------------------------------------------------
    // Placement Drives & Role Breakdown
    // -------------------------------------------------------------------------

    @GetMapping("/placements")
    public ResponseEntity<PageResponse<StudentPlacementDriveCardResponse>> getPlacements(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Principal principal) {
        Student student = studentProvider.getStudentFromPrincipal(principal);
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), Sort.by(Sort.Direction.DESC, "createdAt"));
        PageResponse<StudentPlacementDriveCardResponse> response = studentPortalService.getStudentPlacements(student.getId(), pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/placements/{driveId}")
    public ResponseEntity<StudentPlacementDetailResponse> getPlacementDetail(
            @PathVariable Long driveId,
            Principal principal) {
        Student student = studentProvider.getStudentFromPrincipal(principal);
        StudentPlacementDetailResponse response = studentPortalService.getStudentPlacementDetail(student.getId(), driveId);
        return ResponseEntity.ok(response);
    }

    // -------------------------------------------------------------------------
    // Applications & Apply Action
    // -------------------------------------------------------------------------

    @GetMapping("/applications")
    public ResponseEntity<PageResponse<StudentApplicationResponse>> getApplications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Principal principal) {
        Student student = studentProvider.getStudentFromPrincipal(principal);
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), Sort.by(Sort.Direction.DESC, "createdAt"));
        PageResponse<StudentApplicationResponse> response = studentPortalService.getStudentApplications(student.getId(), pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/applications/{applicationId}")
    public ResponseEntity<StudentApplicationResponse> getApplication(
            @PathVariable Long applicationId,
            Principal principal) {
        Student student = studentProvider.getStudentFromPrincipal(principal);
        Application app = applicationService.findById(applicationId)
                .orElseThrow(() -> ResourceNotFoundException.application(applicationId));

        studentProvider.verifyOwnership(student, app.getStudent().getId());
        return ResponseEntity.ok(new StudentApplicationResponse(
                app.getId(),
                app.getPlacementDrive().getId(),
                app.getPlacementDrive().getCompanyName(),
                app.getPlacementDrive().getTitle(),
                app.getPlacementRole() != null ? app.getPlacementRole().getId() : null,
                app.getPlacementRole() != null ? app.getPlacementRole().getRoleTitle() : null,
                app.getStatus(),
                app.getAppliedAt(),
                null,
                app.getPlacementDrive().getApplicationDeadline(),
                app.getCreatedAt()
        ));
    }

    /**
     * Explicit student action to mark application as APPLIED.
     * Enforces ownership, records appliedAt timestamp, and stops active reminders.
     */
    @PostMapping("/applications/{applicationId}/apply")
    public ResponseEntity<Map<String, Object>> apply(
            @PathVariable Long applicationId,
            Principal principal) {
        Student student = studentProvider.getStudentFromPrincipal(principal);
        Application app = applicationService.apply(student.getId(), applicationId);

        return ResponseEntity.ok(Map.of(
                "applicationId", app.getId(),
                "status", app.getStatus().name(),
                "appliedAt", app.getAppliedAt() != null ? app.getAppliedAt().toString() : ""
        ));
    }

    // -------------------------------------------------------------------------
    // Notifications & Reminders
    // -------------------------------------------------------------------------

    @GetMapping("/notifications")
    public ResponseEntity<PageResponse<StudentNotificationResponse>> getNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) NotificationType type,
            Principal principal) {
        Student student = studentProvider.getStudentFromPrincipal(principal);
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), Sort.by(Sort.Direction.DESC, "createdAt"));
        PageResponse<StudentNotificationResponse> response = studentPortalService.getStudentNotifications(student.getId(), type, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/reminders")
    public ResponseEntity<PageResponse<StudentReminderResponse>> getReminders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Principal principal) {
        Student student = studentProvider.getStudentFromPrincipal(principal);
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), Sort.by(Sort.Direction.ASC, "scheduledFor"));
        PageResponse<StudentReminderResponse> response = studentPortalService.getStudentReminders(student.getId(), pageable);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reminders/{reminderId}/stop")
    public ResponseEntity<Map<String, Object>> stopReminder(
            @PathVariable Long reminderId,
            @RequestParam(required = false, defaultValue = "STUDENT_PORTAL_STOP") String reason,
            Principal principal) {
        Student student = studentProvider.getStudentFromPrincipal(principal);
        ReminderTask task = reminderService.findById(reminderId)
                .orElseThrow(() -> new ResourceNotFoundException("Reminder not found: " + reminderId));

        studentProvider.verifyOwnership(student, task.getStudent().getId());
        reminderService.stopRemindersForStudentAndDrive(student.getId(), task.getPlacementDrive().getId(), reason);

        return ResponseEntity.ok(Map.of("status", "CANCELLED", "reason", reason));
    }

    // -------------------------------------------------------------------------
    // Telegram Connection
    // -------------------------------------------------------------------------

    @PostMapping("/telegram/link-token")
    public ResponseEntity<TelegramLinkResponse> createTelegramLinkToken(Principal principal) {
        Student student = studentProvider.getStudentFromPrincipal(principal);
        TelegramLinkResponse response = telegramLinkingService.createLinkToken(student);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/telegram/status")
    public ResponseEntity<TelegramStatusResponse> getTelegramStatus(Principal principal) {
        Student student = studentProvider.getStudentFromPrincipal(principal);
        TelegramStatusResponse response = telegramLinkingService.getStatus(student);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/telegram/unlink")
    public ResponseEntity<Map<String, String>> unlinkTelegram(Principal principal) {
        Student student = studentProvider.getStudentFromPrincipal(principal);
        telegramLinkingService.unlink(student);
        return ResponseEntity.ok(Map.of("status", "UNLINKED"));
    }
}

