package com.placementos.backend.domain.repository;

import com.placementos.backend.domain.entity.Notification;
import com.placementos.backend.domain.entity.PlacementDrive;
import com.placementos.backend.domain.entity.Student;
import com.placementos.backend.domain.enums.NotificationChannel;
import com.placementos.backend.domain.enums.NotificationType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class NotificationIdempotencyTest {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private PlacementDriveRepository placementDriveRepository;

    @Test
    void duplicateNotification_throwsDataIntegrityViolationException() {
        // Create dummy student
        Student student = new Student();
        student.setRegistrationNumber("TESTREG123");
        student.setName("Test Student");
        student.setBranch("CSE");
        student.setBatch(2025);
        student.setCgpa(new BigDecimal("9.5"));
        student = studentRepository.save(student);

        // Create dummy placement drive
        PlacementDrive drive = new PlacementDrive();
        drive.setCompanyName("Test Company");
        drive = placementDriveRepository.save(drive);

        // First notification should save successfully
        Notification n1 = new Notification();
        n1.setIdempotencyKey("idemp-key-test-1");
        n1.setStudent(student);
        n1.setPlacementDrive(drive);
        n1.setNotificationType(NotificationType.ELIGIBILITY);
        n1.setChannel(NotificationChannel.EMAIL);
        notificationRepository.saveAndFlush(n1);

        // Second duplicate notification with same idempotency key should throw constraint violation
        Notification n2 = new Notification();
        n2.setIdempotencyKey("idemp-key-test-1");
        n2.setStudent(student);
        n2.setPlacementDrive(drive);
        n2.setNotificationType(NotificationType.ELIGIBILITY);
        n2.setChannel(NotificationChannel.EMAIL);

        assertThatThrownBy(() -> notificationRepository.saveAndFlush(n2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
