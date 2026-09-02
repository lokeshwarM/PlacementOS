package com.placementos.backend.domain.service.template;

import com.placementos.backend.domain.entity.PlacementDrive;
import com.placementos.backend.domain.entity.PlacementRole;
import com.placementos.backend.domain.entity.ShortlistEntry;
import com.placementos.backend.domain.entity.Student;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MessageTemplateServiceTest {

    private MessageTemplateService templateService;
    private Student student;
    private PlacementDrive drive;
    private PlacementRole role;

    @BeforeEach
    void setUp() {
        templateService = new MessageTemplateService();

        student = new Student();
        student.setId(1L);
        student.setName("Alice Smith");

        drive = new PlacementDrive();
        drive.setId(10L);
        drive.setCompanyName("Microsoft");
        drive.setTitle("Campus Hiring 2027");
        drive.setApplicationDeadline(Instant.parse("2026-09-15T18:00:00Z"));

        role = new PlacementRole("Software Engineer", 1);
        role.setId(101L);
        role.setPlacementDrive(drive);
    }

    @Test
    void renderEligibilityMessage_containsExpectedDetails() {
        String msg = templateService.renderEligibilityMessage(student, drive, List.of(role));

        assertNotNull(msg);
        assertTrue(msg.contains("Alice Smith"));
        assertTrue(msg.contains("Microsoft"));
        assertTrue(msg.contains("Software Engineer"));
        assertTrue(msg.contains("ELIGIBLE"));
    }

    @Test
    void renderShortlistMessage_containsShortlistDetails() {
        ShortlistEntry entry = new ShortlistEntry();
        entry.setRegistrationNumber("21BCE1001");

        String msg = templateService.renderShortlistMessage(student, drive, role, entry);

        assertNotNull(msg);
        assertTrue(msg.contains("Alice Smith"));
        assertTrue(msg.contains("SHORTLISTED"));
        assertTrue(msg.contains("Microsoft"));
        assertTrue(msg.contains("21BCE1001"));
    }

    @Test
    void renderDeadlineMessage_containsDeadlineDetails() {
        String msg = templateService.renderDeadlineMessage(student, drive, role, drive.getApplicationDeadline());

        assertNotNull(msg);
        assertTrue(msg.contains("Alice Smith"));
        assertTrue(msg.contains("Microsoft"));
        assertTrue(msg.contains("Deadline"));
    }

    @Test
    void renderReminderMessage_containsReminderNumber() {
        String msg = templateService.renderReminderMessage(student, drive, role, drive.getApplicationDeadline(), 3);

        assertNotNull(msg);
        assertTrue(msg.contains("Alice Smith"));
        assertTrue(msg.contains("Reminder #3"));
        assertTrue(msg.contains("DONE"));
    }
}
