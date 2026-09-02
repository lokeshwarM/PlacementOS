package com.placementos.backend.domain.repository;

import com.placementos.backend.domain.entity.Notification;
import com.placementos.backend.domain.enums.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

/**
 * Persistence repository for {@link Notification}.
 */
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Optional<Notification> findByIdempotencyKey(String idempotencyKey);

    boolean existsByIdempotencyKey(String idempotencyKey);

    List<Notification> findByStudentId(Long studentId);

    org.springframework.data.domain.Page<Notification> findByStudentId(Long studentId, org.springframework.data.domain.Pageable pageable);

    org.springframework.data.domain.Page<Notification> findByStudentIdAndNotificationType(
            Long studentId, com.placementos.backend.domain.enums.NotificationType notificationType, org.springframework.data.domain.Pageable pageable);

    List<Notification> findByPlacementDriveId(Long placementDriveId);

    List<Notification> findByStudentIdAndPlacementDriveId(Long studentId, Long placementDriveId);

    List<Notification> findByStatus(NotificationStatus status);
}
