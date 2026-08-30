package com.placementos.backend.domain.repository;

import com.placementos.backend.domain.entity.ReminderTask;
import com.placementos.backend.domain.enums.ReminderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

/**
 * Persistence repository for {@link ReminderTask}.
 * The composite unique constraint (student_id, placement_drive_id) is enforced by the database.
 */
public interface ReminderTaskRepository extends JpaRepository<ReminderTask, Long> {

    Optional<ReminderTask> findByStudentIdAndPlacementDriveId(Long studentId, Long placementDriveId);

    boolean existsByStudentIdAndPlacementDriveId(Long studentId, Long placementDriveId);

    List<ReminderTask> findByStatus(ReminderStatus status);
}
