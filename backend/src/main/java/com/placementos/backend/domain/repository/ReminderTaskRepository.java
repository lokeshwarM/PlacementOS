package com.placementos.backend.domain.repository;

import com.placementos.backend.domain.entity.ReminderTask;
import com.placementos.backend.domain.enums.ReminderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Persistence repository for {@link ReminderTask}.
 */
public interface ReminderTaskRepository extends JpaRepository<ReminderTask, Long> {

    Optional<ReminderTask> findByStudentIdAndPlacementDriveId(Long studentId, Long placementDriveId);

    boolean existsByStudentIdAndPlacementDriveId(Long studentId, Long placementDriveId);

    List<ReminderTask> findByStudentId(Long studentId);

    org.springframework.data.domain.Page<ReminderTask> findByStudentId(Long studentId, org.springframework.data.domain.Pageable pageable);

    List<ReminderTask> findByStatus(ReminderStatus status);

    @Query("SELECT r FROM ReminderTask r WHERE r.status = :status AND r.scheduledFor <= :now ORDER BY r.scheduledFor ASC")
    List<ReminderTask> findDueReminders(@Param("status") ReminderStatus status, @Param("now") Instant now);

    List<ReminderTask> findByStudentIdAndPlacementDriveIdAndStatus(Long studentId, Long placementDriveId, ReminderStatus status);
}
