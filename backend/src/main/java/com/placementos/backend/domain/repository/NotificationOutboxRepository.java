package com.placementos.backend.domain.repository;

import com.placementos.backend.domain.entity.NotificationOutbox;
import com.placementos.backend.domain.enums.OutboxStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Persistence repository for {@link NotificationOutbox}.
 */
public interface NotificationOutboxRepository extends JpaRepository<NotificationOutbox, Long> {

    Optional<NotificationOutbox> findByIdempotencyKey(String idempotencyKey);

    List<NotificationOutbox> findByStatus(OutboxStatus status);

    @Query("SELECT o FROM NotificationOutbox o WHERE (o.status = 'PENDING' OR o.status = 'RETRYING') AND o.availableAt <= :now ORDER BY o.availableAt ASC")
    List<NotificationOutbox> findDueOutboxRecords(@Param("now") Instant now, Pageable pageable);

    @Query("SELECT o FROM NotificationOutbox o WHERE o.notification.student.id = :studentId AND o.notification.placementDrive.id = :driveId AND (o.status = 'PENDING' OR o.status = 'RETRYING')")
    List<NotificationOutbox> findPendingByStudentAndDrive(@Param("studentId") Long studentId, @Param("driveId") Long driveId);

    /**
     * Bulk-cancels all pending or retrying outbox entries for a student.
     * Used during account deletion to prevent delivery after account is anonymised.
     */
    @Modifying
    @Query("UPDATE NotificationOutbox o SET o.status = com.placementos.backend.domain.enums.OutboxStatus.CANCELLED, o.lastError = :reason, o.processedAt = :now WHERE o.notification.student.id = :studentId AND (o.status = 'PENDING' OR o.status = 'RETRYING')")
    int cancelPendingByStudentId(@Param("studentId") Long studentId, @Param("reason") String reason, @Param("now") Instant now);
}
