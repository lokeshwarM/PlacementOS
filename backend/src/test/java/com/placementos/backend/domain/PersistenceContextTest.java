package com.placementos.backend.domain;

import com.placementos.backend.domain.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test that verifies:
 * 1. The full Spring Boot context loads with all JPA entity mappings.
 * 2. All 8 repositories are wired by Spring and operational.
 * 3. Hibernate ddl-auto=validate passes against the Flyway-managed Neon schema.
 *
 * This test uses the real Neon datasource configured in backend/.env.
 * No test data is inserted or modified. This is a pure context-load and schema
 * validation check.
 *
 * Run only when backend/.env is present and the Neon connection is available.
 */
@SpringBootTest
class PersistenceContextTest {

    @Autowired StudentRepository studentRepository;
    @Autowired PlacementDriveRepository placementDriveRepository;
    @Autowired ProcessedEmailRepository processedEmailRepository;
    @Autowired AttachmentRepository attachmentRepository;
    @Autowired ShortlistEntryRepository shortlistEntryRepository;
    @Autowired ApplicationRepository applicationRepository;
    @Autowired NotificationRepository notificationRepository;
    @Autowired ReminderTaskRepository reminderTaskRepository;

    @Test
    void allRepositoriesLoad() {
        assertThat(studentRepository).isNotNull();
        assertThat(placementDriveRepository).isNotNull();
        assertThat(processedEmailRepository).isNotNull();
        assertThat(attachmentRepository).isNotNull();
        assertThat(shortlistEntryRepository).isNotNull();
        assertThat(applicationRepository).isNotNull();
        assertThat(notificationRepository).isNotNull();
        assertThat(reminderTaskRepository).isNotNull();
    }
}
