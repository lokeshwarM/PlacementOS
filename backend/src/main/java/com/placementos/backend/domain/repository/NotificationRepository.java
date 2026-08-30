package com.placementos.backend.domain.repository;

import com.placementos.backend.domain.entity.Notification;
import com.placementos.backend.domain.enums.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/**
 * Persistence repository for {@link Notification}.
 */
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByStudentId(Long studentId);

    List<Notification> findByPlacementDriveId(Long placementDriveId);

    List<Notification> findByStatus(NotificationStatus status);
}
