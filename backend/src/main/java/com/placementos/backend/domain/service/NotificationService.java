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
import java.util.Optional;

/**
 * Business service for querying and managing notification entity records.
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

    /**
     * Legacy creation method for tests/manual recording.
     */
    @Transactional
    public Notification createNotification(Long studentId,
                                            Long placementDriveId,
                                            NotificationType type,
                                            NotificationChannel channel) {
        String key = String.format("legacy:%d:%d:%s:%s", studentId, placementDriveId, type, channel);
        return createNotificationWithKey(studentId, placementDriveId, type, channel, key, null);
    }

    @Transactional
    public Notification createNotificationWithKey(Long studentId,
                                                  Long placementDriveId,
                                                  NotificationType type,
                                                  NotificationChannel channel,
                                                  String idempotencyKey,
                                                  String messagePayload) {
        Optional<Notification> existing = notificationRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return existing.get();
        }

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> ResourceNotFoundException.student(studentId));

        PlacementDrive drive = placementDriveRepository.findById(placementDriveId)
                .orElseThrow(() -> ResourceNotFoundException.placementDrive(placementDriveId));

        Notification notification = new Notification();
        notification.setIdempotencyKey(idempotencyKey);
        notification.setStudent(student);
        notification.setPlacementDrive(drive);
        notification.setNotificationType(type);
        notification.setChannel(channel);
        notification.setMessagePayload(messagePayload);
        notification.setStatus(NotificationStatus.PENDING);

        return notificationRepository.save(notification);
    }

    public List<Notification> findByStudentId(Long studentId) {
        return notificationRepository.findByStudentId(studentId);
    }

    public List<Notification> findByPlacementDriveId(Long placementDriveId) {
        return notificationRepository.findByPlacementDriveId(placementDriveId);
    }

    public Optional<Notification> findByIdempotencyKey(String key) {
        return notificationRepository.findByIdempotencyKey(key);
    }

    public boolean hasEquivalentNotification(String idempotencyKey) {
        return notificationRepository.existsByIdempotencyKey(idempotencyKey);
    }
}
