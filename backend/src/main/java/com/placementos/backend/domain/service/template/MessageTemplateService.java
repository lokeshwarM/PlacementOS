package com.placementos.backend.domain.service.template;

import com.placementos.backend.domain.entity.PlacementDrive;
import com.placementos.backend.domain.entity.PlacementRole;
import com.placementos.backend.domain.entity.ShortlistEntry;
import com.placementos.backend.domain.entity.Student;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service responsible for deterministic message rendering across all notification types.
 * Pure rendering logic: no external side effects and no decision logic.
 */
@Service
public class MessageTemplateService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter
            .ofPattern("dd MMM yyyy, hh:mm a (z)")
            .withZone(ZoneId.of("Asia/Kolkata"));

    /**
     * Renders notification for confirmed eligibility.
     */
    public String renderEligibilityMessage(Student student, PlacementDrive drive, List<PlacementRole> eligibleRoles) {
        StringBuilder sb = new StringBuilder();
        sb.append("🎯 *Placement Opportunity Alert*\n\n");
        sb.append("Hi ").append(student.getName()).append(",\n\n");
        sb.append("You have been verified as *ELIGIBLE* for the upcoming placement drive:\n");
        sb.append("🏢 *Company:* ").append(drive.getCompanyName()).append("\n");
        if (drive.getTitle() != null && !drive.getTitle().isBlank()) {
            sb.append("📋 *Drive:* ").append(drive.getTitle()).append("\n");
        }

        if (eligibleRoles != null && !eligibleRoles.isEmpty()) {
            String roleTitles = eligibleRoles.stream()
                    .map(PlacementRole::getRoleTitle)
                    .collect(Collectors.joining(", "));
            sb.append("💼 *Eligible Role(s):* ").append(roleTitles).append("\n");
        }

        if (drive.getApplicationDeadline() != null) {
            sb.append("⏰ *Application Deadline:* ")
              .append(DATE_TIME_FORMATTER.format(drive.getApplicationDeadline())).append("\n");
        }

        sb.append("\n👉 Please log in to PlacementOS to review criteria and submit your application.");
        return sb.toString();
    }

    /**
     * Renders notification for candidate shortlist.
     */
    public String renderShortlistMessage(Student student, PlacementDrive drive, PlacementRole role, ShortlistEntry entry) {
        StringBuilder sb = new StringBuilder();
        sb.append("🎉 *Congratulations! Shortlist Update*\n\n");
        sb.append("Hi ").append(student.getName()).append(",\n\n");
        sb.append("You have been *SHORTLISTED* for:\n");
        sb.append("🏢 *Company:* ").append(drive.getCompanyName()).append("\n");

        if (role != null) {
            sb.append("💼 *Role:* ").append(role.getRoleTitle()).append("\n");
        }

        if (entry != null && entry.getRegistrationNumber() != null) {
            sb.append("🆔 *Registration No:* ").append(entry.getRegistrationNumber()).append("\n");
        }

        sb.append("\n👉 Please check PlacementOS and your college email for upcoming interview/assessment schedules.");
        return sb.toString();
    }

    /**
     * Renders notification for approaching application deadline.
     */
    public String renderDeadlineMessage(Student student, PlacementDrive drive, PlacementRole role, Instant deadline) {
        StringBuilder sb = new StringBuilder();
        sb.append("⏳ *Upcoming Application Deadline Alert*\n\n");
        sb.append("Hi ").append(student.getName()).append(",\n\n");
        sb.append("The application window for *").append(drive.getCompanyName()).append("* is closing soon:\n");

        if (role != null) {
            sb.append("💼 *Role:* ").append(role.getRoleTitle()).append("\n");
        }

        if (deadline != null) {
            sb.append("⏰ *Deadline:* ").append(DATE_TIME_FORMATTER.format(deadline)).append("\n");
        }

        sb.append("\n👉 If you intend to apply, please submit your application before the deadline.");
        return sb.toString();
    }

    /**
     * Renders periodic reminder notification.
     */
    public String renderReminderMessage(Student student, PlacementDrive drive, PlacementRole role, Instant deadline, int reminderNumber) {
        StringBuilder sb = new StringBuilder();
        sb.append("🔔 *Placement Application Reminder #").append(reminderNumber).append("*\n\n");
        sb.append("Hi ").append(student.getName()).append(",\n\n");
        sb.append("You have a pending application for *").append(drive.getCompanyName()).append("*.\n");

        if (role != null) {
            sb.append("💼 *Role:* ").append(role.getRoleTitle()).append("\n");
        }

        if (deadline != null) {
            sb.append("⏰ *Deadline:* ").append(DATE_TIME_FORMATTER.format(deadline)).append("\n");
        }

        sb.append("\n👉 Reply *DONE* or click *Apply* in PlacementOS once you have submitted your application to stop further reminders.");
        return sb.toString();
    }
}
