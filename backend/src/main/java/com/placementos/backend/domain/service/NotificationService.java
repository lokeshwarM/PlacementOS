package com.placementos.backend.domain.service;

import com.placementos.backend.domain.entity.Notification;
import com.placementos.backend.domain.entity.PlacementDrive;
import com.placementos.backend.domain.entity.Student;
import com.placementos.backend.domain.enums.NotificationChannel;
import com.placementos.backend.domain.enums.NotificationStatus;
import com.placementos.backend.domain.enums.NotificationType;
import com.placementos.backend.domain.exception.ResourceNotFoundException;
import com.placementos.backend.domain.repository.NotificationRepository;
import com.placementos.backend.domain.repository.PlacementDriveRepository;
import com.placementos.backend.domain.repository.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Business service establishing the notification domain boundary.
 *
 * This service manages notification records in the database only.
 * No external notification delivery (WhatsApp, Telegram, Email, In-App) is implemented here.
 * Delivery will be added in a later milestone.
 *
 * Idempotency: the service checks for an existing equivalent notification before
 * creating a new record, using the combination of
 * (student_id, placement_drive_id, notification_type, channel).
 */
@Service
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final StudentRepository studentRepository;
    private final PlacementDriveRepository placementDriveRepository;

    public NotificationService(NotificationRepository notificationRepository,
                                StudentRepository studentRepository,
                                PlacementDriveRepository placementDriveRepository) {
        this.notificationRepository = notificationRepository;
        this.studentRepository = studentRepository;
        this.placementDriveRepository = placementDriveRepository;
    }

    // -------------------------------------------------------------------------
    // Writes
    // -------------------------------------------------------------------------

    /**
     * Creates a notification record for a student and placement drive.
     * Does not send anything externally.
     *
     * Callers should check {@link #hasEquivalentNotification} before calling this
     * to avoid duplicate notifications for the same event.
     */
    @Transactional
    public Notification createNotification(Long studentId,
                                            Long placementDriveId,
                                            NotificationType type,
                                            NotificationChannel channel) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> ResourceNotFoundException.student(studentId));

        PlacementDrive drive = placementDriveRepository.findById(placementDriveId)
                .orElseThrow(() -> ResourceNotFoundException.placementDrive(placementDriveId));

        Notification notification = new Notification();
        notification.setStudent(student);
        notification.setPlacementDrive(drive);
        notification.setNotificationType(type);
        notification.setChannel(channel);
        notification.setStatus(NotificationStatus.PENDING);

        return notificationRepository.save(notification);
    }

    // -------------------------------------------------------------------------
    // Reads
    // -------------------------------------------------------------------------

    public List<Notification> findByStudentId(Long studentId) {
        return notificationRepository.findByStudentId(studentId);
    }

    public List<Notification> findByPlacementDriveId(Long placementDriveId) {
        return notificationRepository.findByPlacementDriveId(placementDriveId);
    }

    /**
     * Checks whether an equivalent notification (same student, drive, type, and channel)
     * already exists, regardless of status. Used to prevent duplicate dispatches.
     */
    public boolean hasEquivalentNotification(Long studentId,
                                              Long placementDriveId,
                                              NotificationType type,
                                              NotificationChannel channel) {
        return notificationRepository.findByStudentId(studentId)
                .stream()
                .anyMatch(n -> n.getPlacementDrive().getId().equals(placementDriveId)
                            && n.getNotificationType() == type
                            && n.getChannel() == channel);
    }
}
